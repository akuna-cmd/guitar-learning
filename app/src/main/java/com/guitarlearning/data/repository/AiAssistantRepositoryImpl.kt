package com.guitarlearning.data.repository

import android.content.Context
import android.util.Log
import com.guitarlearning.R
import com.guitarlearning.domain.settings.AiProvider
import com.guitarlearning.data.remote.AiAssistantPromptBuilder
import com.guitarlearning.data.remote.AiAssistantConfigProvider
import com.guitarlearning.domain.model.AiAssistantRequest
import com.guitarlearning.domain.repository.AiAnswerResult
import com.guitarlearning.domain.repository.AiAssistantRepository
import com.guitarlearning.domain.repository.AppSettingsRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ConnectException
import java.net.URI
import java.net.SocketTimeoutException
import java.net.URL
import java.util.Collections
import javax.net.ssl.SSLException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAssistantRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val promptBuilder: AiAssistantPromptBuilder,
    private val configProvider: AiAssistantConfigProvider,
    private val appSettingsRepository: AppSettingsRepository,
    private val firebaseAuth: FirebaseAuth
) : AiAssistantRepository {

    @Volatile
    private var cachedLocalServerInfo: CachedLocalServerInfo? = null
    private val activeTestConnections = Collections.synchronizedSet(mutableSetOf<HttpURLConnection>())

    private companion object {
        const val Tag = "AiAssistantRepo"
        const val EmptyResponseError = "empty_response"
        const val ConnectTimeoutMs = 4_000
        const val ReadTimeoutMs = 120_000
        const val LocalMaxTokens = 2048
    }

    override suspend fun generateAnswer(request: AiAssistantRequest): AiAnswerResult {
        val settings = appSettingsRepository.getSettings()
        val prompt = promptBuilder.build(request)
        return when (settings.aiProvider) {
            AiProvider.GEMINI -> {
                val text = generateWithGemini(prompt)
                AiAnswerResult(
                    text = text,
                    backendLabel = "Gemini"
                )
            }

            AiProvider.LOCAL_LLAMA_CPP -> {
                val result = generateWithLocalLlama(
                    prompt = prompt,
                    rawBaseUrl = settings.localAiServerUrl,
                    options = LocalGenerationOptions(maxTokens = LocalMaxTokens)
                )
                val responseModel = result.responseModel ?: cachedLocalServerInfo?.modelId
                AiAnswerResult(
                    text = result.text,
                    backendLabel = responseModel?.let { "$it via llama.cpp" } ?: "Local model via llama.cpp"
                )
            }
        }
    }

    override suspend fun testConnection(provider: AiProvider, localServerUrl: String): String {
        val prompt = "Привітайтеся та коротко скажіть, яка це модель. У 1-2 речення."
        return when (provider) {
            AiProvider.GEMINI -> generateWithGemini(prompt, trackAsTest = true)

            AiProvider.LOCAL_LLAMA_CPP -> generateWithLocalLlama(
                prompt = prompt,
                rawBaseUrl = localServerUrl,
                options = LocalGenerationOptions(maxTokens = LocalMaxTokens),
                trackAsTest = true
            ).text
        }
    }

    override fun cancelTestConnection() {
        val connections = synchronized(activeTestConnections) { activeTestConnections.toList() }
        connections.forEach { connection ->
            runCatching { connection.disconnect() }
        }
        activeTestConnections.clear()
    }

    private suspend fun generateWithGemini(prompt: String, trackAsTest: Boolean = false): String {
        return withContext(Dispatchers.IO) {
            val workerUrl = configProvider.getWorkerUrl()
                .takeIf { it.isNotBlank() }
                ?: throw IllegalStateException(context.getString(R.string.ai_error_gemini_worker_missing))
            val currentUser = firebaseAuth.currentUser
                ?: throw IllegalStateException(context.getString(R.string.ai_error_gemini_auth_required))
            val idToken = currentUser.getIdToken(false).await().token
                ?.trim()
                .takeIf { !it.isNullOrBlank() }
                ?: throw IllegalStateException(context.getString(R.string.ai_error_gemini_auth_required))
            val requestedModel = configProvider.getModelName()
            val body = JSONObject().apply {
                put("prompt", prompt)
                put("model", requestedModel)
            }.toString()

            runCatching {
                val responseBody = postJson(
                    urlString = workerUrl,
                    body = body,
                    headers = mapOf("Authorization" to "Bearer $idToken"),
                    trackAsTest = trackAsTest
                )
                val payload = JSONObject(responseBody)
                val text = payload.optString("text").trim()
                if (text.isBlank()) {
                    throw IllegalStateException(EmptyResponseError)
                }
                text
            }.getOrElse { error ->
                throw IllegalStateException(explainGeminiWorkerError(error))
            }
        }
    }

    private fun explainGeminiWorkerError(error: Throwable): String {
        val detail = error.localizedMessage?.takeIf { it.isNotBlank() }
        return when (error) {
            is ConnectException, is SocketTimeoutException, is SSLException -> {
                detail ?: context.getString(R.string.ai_error_gemini_worker_unavailable)
            }

            else -> detail ?: context.getString(R.string.ai_error_gemini_worker_failed)
        }
    }

    private suspend fun generateWithLocalLlama(
        prompt: String,
        rawBaseUrl: String,
        options: LocalGenerationOptions = LocalGenerationOptions(),
        trackAsTest: Boolean = false
    ): LocalGenerationResult = withContext(Dispatchers.IO) {
        val primaryBaseUrl = normalizeBaseUrl(rawBaseUrl)
            ?: throw IllegalStateException(context.getString(R.string.ai_error_local_server_missing))
        validateLocalBaseUrl(primaryBaseUrl)
        val cachedInfo = cachedLocalServerInfo?.takeIf { it.baseUrl == primaryBaseUrl }
        val baseUrl = primaryBaseUrl

        val modelId = cachedInfo?.modelId

        val attempt = EndpointAttempt(
            url = "$baseUrl/v1/chat/completions",
            body = JSONObject().apply {
                put("model", modelId ?: "local-model")
                put("stream", false)
                options.maxTokens?.let { put("max_tokens", it) }
                put(
                    "messages",
                    JSONArray().put(
                        JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        }
                    )
                )
            }
        )

        var lastError: Exception? = null
        runCatching {
            postJson(attempt.url, attempt.body.toString(), trackAsTest = trackAsTest)
        }.onSuccess { responseBody ->
            val parsed = parseLocalLlamaResponse(responseBody)
            val text = parsed.text
            if (text.isNotBlank()) {
                cachedLocalServerInfo = CachedLocalServerInfo(
                    baseUrl = baseUrl,
                    modelId = parsed.model ?: modelId
                )
                return@withContext LocalGenerationResult(
                    text = text,
                    responseModel = parsed.model
                )
            }
            lastError = IllegalStateException(EmptyResponseError)
        }.onFailure { error ->
            Log.w(Tag, "Local AI endpoint failed ${attempt.url}: ${error.message}")
            lastError = error as? Exception ?: IllegalStateException(error.message)
        }

        throw IllegalStateException(
            explainLocalServerError(primaryBaseUrl, lastError)
        )
    }

    private fun normalizeBaseUrl(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return withScheme
    }

    private fun explainLocalServerError(baseUrl: String, error: Exception?): String {
        val detail = error?.localizedMessage?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.ai_error_generic)
        return when (error) {
            is ConnectException -> context.getString(
                R.string.ai_error_local_server_connect_hint,
                baseUrl,
                detail
            )

            is SocketTimeoutException -> context.getString(
                R.string.ai_error_local_server_timeout_hint,
                baseUrl,
                detail
            )

            is SSLException -> context.getString(
                R.string.ai_error_local_server_ssl_hint,
                baseUrl,
                detail
            )

            else -> context.getString(
                R.string.ai_error_local_server_unreachable,
                baseUrl,
                detail
            )
        }
    }

    private fun validateLocalBaseUrl(baseUrl: String) {
        val host = runCatching { URI(baseUrl).host?.lowercase() }.getOrNull().orEmpty()
        if (host == "localhost" || host == "127.0.0.1" || host == "0.0.0.0") {
            throw IllegalStateException(
                context.getString(R.string.ai_error_local_server_loopback, baseUrl)
            )
        }
    }

    private fun postJson(
        urlString: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        trackAsTest: Boolean = false
    ): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = ConnectTimeoutMs
            readTimeout = ReadTimeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        if (trackAsTest) {
            activeTestConnections.add(connection)
        }
        return try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }.orEmpty()
            if (responseCode !in 200..299) {
                val message = runCatching {
                    JSONObject(responseText).optString("error").ifBlank { responseText }
                }.getOrDefault(responseText)
                throw IllegalStateException("HTTP $responseCode: $message")
            }
            responseText
        } finally {
            if (trackAsTest) {
                activeTestConnections.remove(connection)
            }
            connection.disconnect()
        }
    }

    private fun parseLocalLlamaResponse(responseBody: String): ParsedLocalResponse {
        val json = JSONObject(responseBody)
        val responseModel = json.optString("model").takeIf { it.isNotBlank() }
        json.optJSONArray("choices")?.let { choices ->
            if (choices.length() > 0) {
                val first = choices.optJSONObject(0)
                val messageContent = first?.optJSONObject("message")?.optString("content").orEmpty().trim()
                if (messageContent.isNotBlank()) {
                    return ParsedLocalResponse(messageContent, responseModel)
                }
                val text = first?.optString("text").orEmpty().trim()
                if (text.isNotBlank()) {
                    return ParsedLocalResponse(text, responseModel)
                }
            }
        }
        val content = json.optString("content").trim()
        if (content.isNotBlank()) return ParsedLocalResponse(content, responseModel)
        val result = json.optString("response").trim()
        if (result.isNotBlank()) return ParsedLocalResponse(result, responseModel)
        throw IllegalStateException(EmptyResponseError)
    }

    private data class EndpointAttempt(
        val url: String,
        val body: JSONObject
    )

    private data class LocalGenerationOptions(
        val maxTokens: Int? = null
    )

    private data class ParsedLocalResponse(
        val text: String,
        val model: String?
    )

    private data class LocalGenerationResult(
        val text: String,
        val responseModel: String?
    )

    private data class CachedLocalServerInfo(
        val baseUrl: String,
        val modelId: String?
    )
}
