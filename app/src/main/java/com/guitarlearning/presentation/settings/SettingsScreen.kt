package com.guitarlearning.presentation.settings
import com.guitarlearning.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guitarlearning.R
import com.guitarlearning.core.locale.AppLocaleManager
import com.guitarlearning.domain.settings.AiProvider
import com.guitarlearning.domain.settings.AppLanguage
import com.guitarlearning.domain.settings.TabDisplayMode
import com.guitarlearning.domain.settings.ThemeMode
import com.guitarlearning.presentation.main.ThemeViewModel
import com.guitarlearning.presentation.ui.HoldableIconButton
import com.guitarlearning.presentation.ui.formatScale
import com.guitarlearning.presentation.ui.formatSpeed
import com.guitarlearning.presentation.ui.stepScale
import com.guitarlearning.presentation.ui.stepSpeed
import com.guitarlearning.presentation.ui.theme.appBlockBorder
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val activity = LocalContext.current as ComponentActivity
    val themeViewModel: ThemeViewModel = hiltViewModel(activity)
    val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var aiProviderDraft by remember(uiState.aiProvider) { mutableStateOf(uiState.aiProvider) }
    var aiServerUrlDraft by remember(uiState.localAiServerUrl) { mutableStateOf(uiState.localAiServerUrl) }
    val isGeminiKeyMissing = BuildConfig.GEMINI_API_KEY.isBlank()

    LaunchedEffect(uiState.aiProvider, uiState.localAiServerUrl) {
        aiProviderDraft = uiState.aiProvider
        aiServerUrlDraft = uiState.localAiServerUrl
    }

    LaunchedEffect(uiState.appLanguage) {
        val savedLanguageTag = AppLocaleManager.getSavedLanguageTag(context)
        val targetLanguageTag = uiState.appLanguage.languageTag
        if (savedLanguageTag != targetLanguageTag) {
            AppLocaleManager.persistLanguage(context, targetLanguageTag)
            activity.overridePendingTransition(0, 0)
            activity.recreate()
            activity.overridePendingTransition(0, 0)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ─── Profile / Auth ───────────────────────────────────────
        item {
            val user = authState.user
            if (user != null) {
                ProfileCard(
                    displayName = user.displayName,
                    email = user.email,
                    isSyncing = settingsUiState.isSyncing,
                    lastSyncedTime = settingsUiState.lastSyncedTime,
                    message = settingsUiState.message,
                    onSync = settingsViewModel::syncCloud,
                    onSignOut = { authViewModel.signOut(context) }
                )
            } else {
                AuthCard(
                    authViewModel = authViewModel,
                    isLoading = authState.isLoading,
                    error = authState.error,
                    onGoogleSignIn = {
                        authViewModel.signInWithGoogleCredentialManager(context) {
                            settingsViewModel.syncCloud()
                        }
                    },
                    onEmailSignIn = { email, password ->
                        authViewModel.signInWithEmail(email, password) {
                            settingsViewModel.syncCloud()
                        }
                    },
                    onEmailSignUp = { email, password, displayName ->
                        authViewModel.signUpWithEmail(email, password, displayName) {
                            settingsViewModel.syncCloud()
                        }
                    }
                )
            }
        }

        if (!settingsUiState.message.isNullOrBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    border = appBlockBorder(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = settingsUiState.message.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(onClick = settingsViewModel::clearMessage) {
                            Text(
                                text = stringResource(R.string.ok),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // ─── Theme ────────────────────────────────────────────────
        item {
            SettingsSection(title = stringResource(R.string.settings_theme_title)) {
                listOf(
                    Triple(ThemeMode.SYSTEM, stringResource(R.string.settings_theme_system), Icons.Default.BrightnessAuto),
                    Triple(ThemeMode.LIGHT, stringResource(R.string.settings_theme_light), Icons.Default.Brightness5),
                    Triple(ThemeMode.DARK, stringResource(R.string.settings_theme_dark), Icons.Default.Brightness4)
                ).forEachIndexed { index, (mode, label, icon) ->
                    SettingsOptionsRow(
                        label = label,
                        selected = uiState.themeMode == mode,
                        icon = icon,
                        showTopDivider = index != 0
                    ) { themeViewModel.setThemeMode(mode) }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_language_title)) {
                listOf(
                    AppLanguage.UKRAINIAN to stringResource(R.string.settings_language_ukrainian),
                    AppLanguage.ENGLISH to stringResource(R.string.settings_language_english)
                ).forEachIndexed { index, (language, label) ->
                    SettingsOptionsRow(
                        label = label,
                        selected = uiState.appLanguage == language,
                        icon = Icons.Default.Language,
                        showTopDivider = index != 0
                    ) {
                        themeViewModel.setAppLanguage(language)
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_ai_provider_title)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_ai_provider_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val currentBackendLabel = when (uiState.aiProvider) {
                        AiProvider.GEMINI -> stringResource(R.string.settings_ai_provider_gemini)
                        AiProvider.LOCAL_LLAMA_CPP -> stringResource(R.string.settings_ai_provider_local)
                    }

                    Text(
                        text = stringResource(
                            R.string.settings_ai_current_backend,
                            currentBackendLabel
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SettingsOptionsRow(
                        label = stringResource(R.string.settings_ai_provider_gemini),
                        selected = aiProviderDraft == AiProvider.GEMINI,
                        icon = Icons.Default.AutoAwesome,
                        onClick = { aiProviderDraft = AiProvider.GEMINI }
                    )
                    SettingsOptionsRow(
                        label = stringResource(R.string.settings_ai_provider_local),
                        selected = aiProviderDraft == AiProvider.LOCAL_LLAMA_CPP,
                        icon = Icons.Default.Memory,
                        onClick = { aiProviderDraft = AiProvider.LOCAL_LLAMA_CPP }
                    )

                    if (aiProviderDraft == AiProvider.GEMINI && isGeminiKeyMissing) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp),
                            border = appBlockBorder()
                        ) {
                            Text(
                                text = stringResource(R.string.settings_ai_gemini_key_missing),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    if (aiProviderDraft == AiProvider.LOCAL_LLAMA_CPP) {
                        OutlinedTextField(
                            value = aiServerUrlDraft,
                            onValueChange = { aiServerUrlDraft = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            label = { Text(stringResource(R.string.settings_ai_server_label)) },
                            placeholder = { Text(stringResource(R.string.settings_ai_server_placeholder)) }
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (aiProviderDraft == AiProvider.LOCAL_LLAMA_CPP) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                border = appBlockBorder()
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_ai_local_setup_full),
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.settings_ai_test_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val hasAiChanges = aiProviderDraft != uiState.aiProvider ||
                        aiServerUrlDraft != uiState.localAiServerUrl

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isLocalWithoutUrl =
                            aiProviderDraft == AiProvider.LOCAL_LLAMA_CPP && aiServerUrlDraft.trim().isBlank()
                        val isGeminiWithoutKey =
                            aiProviderDraft == AiProvider.GEMINI && isGeminiKeyMissing

                        Button(
                            onClick = {
                                themeViewModel.saveAiSettings(aiProviderDraft, aiServerUrlDraft.trim())
                                settingsViewModel.clearMessage()
                                settingsViewModel.clearAiTestMessage()
                            },
                            enabled = hasAiChanges,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.settings_ai_save))
                        }

                        OutlinedButton(
                            onClick = {
                                themeViewModel.saveAiSettings(aiProviderDraft, aiServerUrlDraft.trim())
                                settingsViewModel.testAiConnection(
                                    provider = aiProviderDraft,
                                    localServerUrl = aiServerUrlDraft.trim()
                                )
                            },
                            enabled = !settingsUiState.isTestingAi && !isLocalWithoutUrl && !isGeminiWithoutKey,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (settingsUiState.isTestingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.settings_ai_test))
                            }
                        }
                    }

                    Text(
                        text = if (hasAiChanges) {
                            stringResource(R.string.settings_ai_status_unsaved)
                        } else {
                            stringResource(R.string.settings_ai_status_saved)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!settingsUiState.aiTestMessage.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            border = appBlockBorder(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = settingsUiState.aiTestMessage.orEmpty(),
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                TextButton(
                                    onClick = settingsViewModel::clearAiTestMessage,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(stringResource(R.string.ok))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_mode_normal)) {
                CompactSpeedScale(
                    speed = uiState.normalSpeed,
                    scale = uiState.normalTabScale,
                    onSpeedChange = themeViewModel::setNormalSpeed,
                    onScaleChange = themeViewModel::setNormalTabScale
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_mode_practice)) {
                CompactSpeedScale(
                    speed = uiState.practiceSpeed,
                    scale = uiState.practiceTabScale,
                    onSpeedChange = themeViewModel::setPracticeSpeed,
                    onScaleChange = themeViewModel::setPracticeTabScale
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.tab_display_mode_title)) {
                SettingsIconOptionRow(
                    label = stringResource(R.string.tab_display_mode_tab_and_notes),
                    selected = uiState.tabDisplayMode == TabDisplayMode.TAB_AND_NOTES,
                    icon = Icons.Default.LibraryMusic,
                    showTopDivider = false,
                    onClick = { themeViewModel.setTabDisplayMode(TabDisplayMode.TAB_AND_NOTES) }
                )
                SettingsIconOptionRow(
                    label = stringResource(R.string.tab_display_mode_notes_only),
                    selected = uiState.tabDisplayMode == TabDisplayMode.NOTES_ONLY,
                    icon = Icons.Default.MusicNote,
                    onClick = { themeViewModel.setTabDisplayMode(TabDisplayMode.NOTES_ONLY) }
                )
                SettingsIconOptionRow(
                    label = stringResource(R.string.tab_display_mode_tab_only),
                    selected = uiState.tabDisplayMode == TabDisplayMode.TAB_ONLY,
                    icon = Icons.Default.QueueMusic,
                    onClick = { themeViewModel.setTabDisplayMode(TabDisplayMode.TAB_ONLY) }
                )
            }
        }

    }
}

