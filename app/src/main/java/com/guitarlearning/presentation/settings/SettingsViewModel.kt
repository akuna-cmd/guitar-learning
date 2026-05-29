package com.guitarlearning.presentation.settings

import android.content.Context
import android.net.Uri
import com.guitarlearning.R
import com.guitarlearning.core.locale.AppLocaleManager
import com.guitarlearning.core.preferences.AiProvider
import com.guitarlearning.domain.session.SessionHistoryTransfer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.domain.repository.AiAssistantRepository
import com.guitarlearning.domain.repository.SessionRepository
import com.guitarlearning.domain.repository.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isSyncing: Boolean = false,
    val isTestingAi: Boolean = false,
    val lastSyncedTime: Long? = null,
    val message: String? = null,
    val aiTestMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionHistoryTransfer: SessionHistoryTransfer,
    private val sessionRepository: SessionRepository,
    private val syncRepository: SyncRepository,
    private val aiAssistantRepository: AiAssistantRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    
    val uiState: StateFlow<SettingsUiState> = combine(
        _uiState,
        syncRepository.isSyncing(),
        syncRepository.getLastSyncedTime()
    ) { state, isSyncing, lastSynced ->
        state.copy(isSyncing = isSyncing, lastSyncedTime = lastSynced)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun exportHistory(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            try {
                val content = sessionHistoryTransfer.exportHistory()
                appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
                _uiState.value = _uiState.value.copy(message = appContext.getString(R.string.settings_message_export_success))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = appContext.getString(R.string.settings_message_export_error, e.localizedMessage ?: "")
                )
            } finally {
                _uiState.value = _uiState.value.copy(isExporting = false)
            }
        }
    }

    fun importHistory(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, message = null)
            try {
                val content = appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                sessionHistoryTransfer.importHistory(content)
                _uiState.value = _uiState.value.copy(message = appContext.getString(R.string.settings_message_import_success))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    message = appContext.getString(R.string.settings_message_import_error, e.localizedMessage ?: "")
                )
            } finally {
                _uiState.value = _uiState.value.copy(isImporting = false)
            }
        }
    }

    fun syncCloud() {
        viewModelScope.launch {
            val result = syncRepository.syncData()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(message = localizedString(R.string.settings_message_sync_success))
            } else {
                _uiState.value = _uiState.value.copy(
                    message = localizedString(
                        R.string.settings_message_sync_error,
                        result.exceptionOrNull()?.localizedMessage ?: ""
                    )
                )
            }
        }
    }

    fun resetHistory() {
        viewModelScope.launch {
            sessionRepository.clearHistory()
            _uiState.value = _uiState.value.copy(message = appContext.getString(R.string.settings_message_history_cleared))
        }
    }

    fun testAiConnection(provider: AiProvider, localServerUrl: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTestingAi = true,
                aiTestMessage = appContext.getString(R.string.settings_ai_test_running)
            )
            val result = runCatching {
                aiAssistantRepository.testConnection(provider, localServerUrl)
            }
            _uiState.value = _uiState.value.copy(
                isTestingAi = false,
                aiTestMessage = result.getOrElse { error ->
                    error.localizedMessage ?: appContext.getString(R.string.ai_error_generic)
                }
            )
        }
    }

    fun clearAiTestMessage() {
        _uiState.value = _uiState.value.copy(aiTestMessage = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun localizedString(resId: Int, vararg formatArgs: Any): String {
        return AppLocaleManager.wrap(appContext).getString(resId, *formatArgs)
    }
}
