package com.guitarlearning.presentation.settings

import android.content.Context
import android.net.Uri
import com.guitarlearning.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitarlearning.domain.model.PracticedTab
import com.guitarlearning.domain.model.Session
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.util.Date
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncedTime: Long? = null,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    
    val uiState: StateFlow<SettingsUiState> = combine(
        _uiState,
        syncRepository.isSyncing(),
        syncRepository.getLastSyncedTime()
    ) { state, isSyncing, lastSynced ->
        state.copy(isSyncing = isSyncing, lastSyncedTime = lastSynced)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun exportHistory(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, message = null)
            try {
                val sessions = sessionRepository.getAllSessionsSync()
                val jsonArray = JSONArray()
                for (session in sessions) {
                    val sessionObj = JSONObject().apply {
                        put("id", session.id)
                        put("startTime", session.startTime.time)
                        put("endTime", session.endTime.time)
                        put("duration", session.duration)
                        val tabsArray = JSONArray()
                        for (tab in session.practicedTabs) {
                            val tabObj = JSONObject().apply {
                                put("tabId", tab.tabId)
                                put("tabName", tab.tabName)
                                put("duration", tab.duration)
                            }
                            tabsArray.put(tabObj)
                        }
                        put("practicedTabs", tabsArray)
                    }
                    jsonArray.put(sessionObj)
                }
                
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonArray.toString(2).toByteArray())
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

    fun importHistory(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, message = null)
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).readText()
                } ?: ""
                val jsonArray = JSONArray(content)
                val sessions = mutableListOf<Session>()
                
                for (i in 0 until jsonArray.length()) {
                    val sessionObj = jsonArray.getJSONObject(i)
                    val tabsArray = sessionObj.optJSONArray("practicedTabs")
                    val practicedTabs = mutableListOf<PracticedTab>()
                    
                    if (tabsArray != null) {
                        for (j in 0 until tabsArray.length()) {
                            val tabObj = tabsArray.getJSONObject(j)
                            practicedTabs.add(
                                PracticedTab(
                                    tabId = tabObj.getString("tabId"),
                                    tabName = tabObj.getString("tabName"),
                                    duration = tabObj.getLong("duration")
                                )
                            )
                        }
                    }
                    
                    sessions.add(
                        Session(
                            id = sessionObj.optInt("id", 0),
                            startTime = Date(sessionObj.getLong("startTime")),
                            endTime = Date(sessionObj.getLong("endTime")),
                            duration = sessionObj.getLong("duration"),
                            practicedTabs = practicedTabs
                        )
                    )
                }
                
                sessionRepository.importHistory(sessions)
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
                _uiState.value = _uiState.value.copy(message = appContext.getString(R.string.settings_message_sync_success))
            } else {
                _uiState.value = _uiState.value.copy(
                    message = appContext.getString(
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

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
