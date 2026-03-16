package com.example.thetest1.presentation.settings

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.thetest1.R
import com.example.thetest1.di.ViewModelFactory
import com.example.thetest1.presentation.auth.AuthViewModel
import com.example.thetest1.presentation.main.ThemeMode
import com.example.thetest1.presentation.main.ThemeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModelFactory: ViewModelFactory) {
    val themeViewModel: ThemeViewModel = viewModel(factory = viewModelFactory)
    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
    val uiState by themeViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ─── Profile / Auth ───────────────────────────────────────
        item {
            val user = authState.user
            AnimatedContent(targetState = user != null, label = "auth_state") { isLoggedIn ->
                if (isLoggedIn && user != null) {
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
            }
        }

        // ─── Theme ────────────────────────────────────────────────
        item {
            SettingsSection(title = "Тема застосунку") {
                listOf(ThemeMode.SYSTEM to "Системна", ThemeMode.LIGHT to "Світла", ThemeMode.DARK to "Темна")
                    .forEach { (mode, label) ->
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
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        job?.cancel()
                        job = scope.launch {
                            delay(250)
                            while (down.pressed) {
                                onClick()
                                delay(80)
                            }
                        }
                        waitForUpOrCancellation()
                        job?.cancel()
                    }
                }
            }
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
                AnimatedContent(targetState = selectedTab, label = "tab_content") { tab ->
                    if (tab == 1) {
                        // Name field only shown for sign-up
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
                        Box(modifier = Modifier.fillMaxWidth())
                    }
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
