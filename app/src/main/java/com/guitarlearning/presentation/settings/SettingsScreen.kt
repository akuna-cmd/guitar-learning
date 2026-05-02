package com.guitarlearning.presentation.settings
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
import com.guitarlearning.core.AppLocaleManager
import com.guitarlearning.presentation.main.AiProvider
import com.guitarlearning.presentation.main.AppLanguage
import com.guitarlearning.presentation.main.ThemeMode
import com.guitarlearning.presentation.main.ThemeViewModel
import com.guitarlearning.presentation.main.TabDisplayMode
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
                            enabled = !settingsUiState.isTestingAi && !isLocalWithoutUrl,
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

@Composable
private fun CompactSpeedScale(
    speed: Float,
    scale: Float,
    onSpeedChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.speed_value_format, formatSpeed(speed)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(speed, -0.1f)) },
                    contentDescription = stringResource(R.string.speed_decrease),
                    icon = Icons.Default.Remove,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(speed, 0.1f)) },
                    contentDescription = stringResource(R.string.speed_increase),
                    icon = Icons.Default.Add,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.scale_value_format, formatScale(scale)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(scale, -0.1f)) },
                    contentDescription = stringResource(R.string.scale_decrease),
                    icon = Icons.Default.Remove,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(scale, 0.1f)) },
                    contentDescription = stringResource(R.string.scale_increase),
                    icon = Icons.Default.Add,
                    buttonSize = 36.dp,
                    iconSize = 20.dp
                )
            }
        }
    }
}

// ── Logged-in profile ─────────────────────────────────────────────────────

@Composable
private fun ProfileCard(
    displayName: String?,
    email: String?,
    isSyncing: Boolean,
    lastSyncedTime: Long?,
    message: String?,
    onSync: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val initials = (displayName ?: email ?: "?")
        .split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")
    val syncText = lastSyncedTime?.let {
        context.getString(
            R.string.settings_last_sync_format,
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
        )
    } ?: context.getString(R.string.settings_last_sync_never)

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = appBlockBorder()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifEmpty { "?" },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName ?: email ?: stringResource(R.string.settings_profile_user),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (displayName != null && email != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = syncText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onSync,
                enabled = !isSyncing,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.settings_sync_now))
                }
            }
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.settings_sign_out))
            }
        }
    }
}

// ── Auth Card with proper Sign In / Sign Up tabs ──────────────────────────

@Composable
private fun AuthCard(
    authViewModel: AuthViewModel,
    isLoading: Boolean,
    error: String?,
    onGoogleSignIn: () -> Unit,
    onEmailSignIn: (String, String) -> Unit,
    onEmailSignUp: (String, String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }  // 0=Sign In, 1=Sign Up

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm  by remember { mutableStateOf(false) }

    val passwordMismatch = selectedTab == 1 && confirmPassword.isNotEmpty() && password != confirmPassword

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = appBlockBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Tab row ──────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {},
                modifier = Modifier.clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; authViewModel.clearError() },
                    text = { Text(stringResource(R.string.settings_auth_sign_in), fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; authViewModel.clearError() },
                    text = { Text(stringResource(R.string.settings_auth_sign_up), fontWeight = FontWeight.SemiBold) }
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Sign Up extras ───────────────────────────────
                if (selectedTab == 1) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text(stringResource(R.string.settings_auth_name)) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }

                // ── Email ────────────────────────────────────────
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Password ─────────────────────────────────────
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.settings_auth_password)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Confirm password (sign-up only) ───────────────
                if (selectedTab == 1) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.settings_auth_repeat_password)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(
                                    if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        singleLine = true,
                        isError = passwordMismatch,
                        supportingText = if (passwordMismatch) {{
                            Text(stringResource(R.string.settings_auth_password_mismatch))
                        }} else null,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Error ─────────────────────────────────────────
                if (error != null) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(8.dp),
            border = appBlockBorder()
        ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(10.dp)
                        )
                    }
                }

                // ── Main action button ────────────────────────────
                val canSubmit = !isLoading && !passwordMismatch &&
                        email.isNotBlank() && password.length >= 6 &&
                        (selectedTab == 0 || confirmPassword == password)

                Button(
                    onClick = {
                        authViewModel.clearError()
                        if (selectedTab == 0) {
                            onEmailSignIn(email, password)
                        } else {
                            onEmailSignUp(email, password, displayName)
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (selectedTab == 0) {
                                stringResource(R.string.settings_auth_sign_in_action)
                            } else {
                                stringResource(R.string.settings_auth_create_account)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // ── Divider + Google ──────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.settings_auth_or),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null)
                        Text(
                            stringResource(R.string.settings_auth_continue_with_google),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

// ── Reusable Settings Components ───────────────────────────────────────────

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = appBlockBorder()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 2.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsOptionsRow(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showTopDivider: Boolean = true,
    onClick: () -> Unit
) {
    SettingsRadioRow(
        label = label,
        selected = selected,
        icon = icon,
        showTopDivider = showTopDivider,
        onClick = onClick
    )
}

@Composable
fun SettingsIconOptionRow(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showTopDivider: Boolean = true,
    onClick: () -> Unit
) {
    SettingsRadioRow(
        label = label,
        selected = selected,
        icon = icon,
        showTopDivider = showTopDivider,
        onClick = onClick
    )
}

@Composable
private fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showTopDivider: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            RadioButton(selected = selected, onClick = null)
        }
    }
}
