package com.example.thetest1.presentation.settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Speed
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thetest1.R
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.auth.AuthViewModel
import com.example.thetest1.presentation.main.ThemeMode
import com.example.thetest1.presentation.main.ThemeViewModel
import com.example.thetest1.presentation.main.TabDisplayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModelFactory: ViewModelFactory,
    themeViewModel: ThemeViewModel
) {
    val uiState by themeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showAccountSheet by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ─── Profile / Auth ───────────────────────────────────────
        item {
            AccountEntryCard(
                onOpen = { showAccountSheet = true }
            )
        }

        // ─── Theme ────────────────────────────────────────────────
        item {
            SettingsSection(title = stringResource(R.string.settings_theme_title)) {
                listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
                ).forEach { (mode, label) ->
                    SettingsOptionsRow(label, uiState.themeMode == mode) { themeViewModel.setThemeMode(mode) }
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsIconOptionRow(
                        label = stringResource(R.string.tab_display_mode_tab_and_notes),
                        selected = uiState.tabDisplayMode == TabDisplayMode.TAB_AND_NOTES,
                        icon = Icons.Default.LibraryMusic,
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

    if (showAccountSheet) {
        val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
        val authState by authViewModel.uiState.collectAsStateWithLifecycle()
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false }
        ) {
            val user = authState.user
            if (user != null) {
                ProfileCard(
                    displayName = user.displayName,
                    email = user.email,
                    onSignOut = { authViewModel.signOut(context) {} }
                )
            } else {
                AuthCard(
                    authViewModel = authViewModel,
                    isLoading = authState.isLoading,
                    error = authState.error,
                    onGoogleSignIn = {
                        authViewModel.signInWithGoogleCredentialManager(context) {}
                    }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccountEntryCard(
    onOpen: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Акаунт",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Увійти, профіль і безпека",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpen) {
                Text("Відкрити")
            }
        }
    }
}

private fun speedString(speed: Float): String {
    return String.format("%.1f", speed).replace(',', '.')
}

private fun stepSpeed(value: Float, delta: Float): Float {
    val stepped = ((value + delta) * 10f).toInt() / 10f
    return stepped.coerceIn(0.1f, 2.5f)
}

private fun scaleString(scale: Float): String {
    return String.format("%.1f", scale).replace(',', '.')
}

private fun stepScale(value: Float, delta: Float): Float {
    val stepped = ((value + delta) * 10f).toInt() / 10f
    return stepped.coerceIn(0.5f, 2.0f)
}

@Composable
private fun CompactSpeedScale(
    speed: Float,
    scale: Float,
    onSpeedChange: (Float) -> Unit,
    onScaleChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text(text = stringResource(R.string.speed_value_format, speedString(speed)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(speed, -0.1f)) },
                    contentDescription = stringResource(R.string.speed_decrease),
                    icon = Icons.Default.Remove
                )
                HoldableIconButton(
                    onClick = { onSpeedChange(stepSpeed(speed, 0.1f)) },
                    contentDescription = stringResource(R.string.speed_increase),
                    icon = Icons.Default.Add
                )
            }
        }
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
                Text(text = stringResource(R.string.scale_value_format, scaleString(scale)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(scale, -0.1f)) },
                    contentDescription = stringResource(R.string.scale_decrease),
                    icon = Icons.Default.Remove
                )
                HoldableIconButton(
                    onClick = { onScaleChange(stepScale(scale, 0.1f)) },
                    contentDescription = stringResource(R.string.scale_increase),
                    icon = Icons.Default.Add
                )
            }
        }
    }
}

@Composable
private fun HoldableIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

// ── Logged-in profile ─────────────────────────────────────────────────────

@Composable
private fun ProfileCard(displayName: String?, email: String?, onSignOut: () -> Unit) {
    val initials = (displayName ?: email ?: "?")
        .split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("")

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials.ifEmpty { "?" },
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = displayName ?: email ?: "Користувач",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            if (displayName != null && email != null) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вийти з профілю")
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
    onGoogleSignIn: () -> Unit
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                    text = { Text("Вхід", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; authViewModel.clearError() },
                    text = { Text("Реєстрація", fontWeight = FontWeight.SemiBold) }
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
                        label = { Text("Ім'я") },
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
                    label = { Text("Пароль") },
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
                        label = { Text("Повторіть пароль") },
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
                        supportingText = if (passwordMismatch) {{ Text("Паролі не збігаються") }} else null,
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
                        shape = RoundedCornerShape(8.dp)
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
                            authViewModel.signInWithEmail(email, password) {}
                        } else {
                            authViewModel.signUpWithEmail(email, password, displayName) {}
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
                            text = if (selectedTab == 0) "Увійти" else "Створити акаунт",
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
                        "  або  ",
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
                        Text("Продовжити з Google", style = MaterialTheme.typography.labelLarge)
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
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsOptionsRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
fun SettingsIconOptionRow(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
