package com.smartmove.ai.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveOutlinedButton
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import kotlinx.coroutines.launch

/**
 * Authentication State for the Login screen.
 */
enum class LoginState {
    IDLE,
    LOADING,
    ERROR,
    SUCCESS,
}

/**
 * Login Screen for SMARTMOVE AI.
 * Implements frontend validation, loading indicator, demo authentication, and server host configuration.
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var loginState by remember { mutableStateOf(LoginState.IDLE) }

    var showServerDialog by remember { mutableStateOf(false) }
    var customServerUrl by remember { mutableStateOf(ApiClient.getBaseUrl(context)) }

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val authRepo = remember { com.smartmove.ai.data.repository.AuthRepository(context) }
    val scrollState = rememberScrollState()

    fun performLogin() {
        keyboardController?.hide()
        generalError = null

        val emailErr = AuthValidation.validateEmail(email)
        val passErr = AuthValidation.validatePassword(password)

        emailError = emailErr
        passwordError = passErr

        if (emailErr != null || passErr != null) {
            loginState = LoginState.ERROR
            return
        }

        loginState = LoginState.LOADING
        coroutineScope.launch {
            val result = authRepo.login(email.trim(), password)
            if (result.isSuccess) {
                loginState = LoginState.SUCCESS
                onLoginSuccess()
            } else {
                loginState = LoginState.ERROR
                generalError = result.exceptionOrNull()?.message ?: "Unable to connect to SMARTMOVE backend."
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = SmartMoveTheme.spacing.default, vertical = SmartMoveTheme.spacing.large),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                // Branding and Screen Header
                SmartMoveAuthHeader(
                    title = "Welcome Back",
                    subtitle = "Sign in to continue your safer journey.",
                )

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

                // General Error Banner (if any)
                AnimatedVisibility(visible = generalError != null) {
                    if (generalError != null) {
                        Text(
                            text = generalError!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmartMoveDanger,
                            modifier = Modifier.padding(bottom = SmartMoveTheme.spacing.medium),
                        )
                    }
                }

                // Email Field
                SmartMoveTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) emailError = null
                    },
                    label = "Email Address",
                    placeholder = "name@example.com",
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    enabled = loginState != LoginState.LOADING,
                )

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

                // Password Field
                SmartMovePasswordField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (passwordError != null) passwordError = null
                    },
                    label = "Password",
                    placeholder = "Enter your password",
                    errorMessage = passwordError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { performLogin() }
                    ),
                    enabled = loginState != LoginState.LOADING,
                )

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

                // Forgot Password Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SmartMovePrimary,
                        modifier = Modifier
                            .clickable(enabled = loginState != LoginState.LOADING) {
                                onNavigateToForgotPassword()
                            }
                            .padding(vertical = 8.dp),
                    )
                }

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

                // LOGIN Button
                SmartMoveButton(
                    text = "LOGIN",
                    onClick = { performLogin() },
                    loading = loginState == LoginState.LOADING,
                    loadingText = "Signing in...",
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Bottom Sign Up Switcher & Server Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SmartMoveTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartMoveTextSecondary,
                )

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

                SmartMoveOutlinedButton(
                    text = "SIGN UP",
                    onClick = onNavigateToSignup,
                    enabled = loginState != LoginState.LOADING,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

                // Server Status / Config Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SmartMoveSurfaceVariant)
                        .border(1.dp, SmartMoveBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            customServerUrl = ApiClient.getBaseUrl(context)
                            showServerDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Server Settings",
                        tint = SmartMoveTextMuted,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Server: ${ApiClient.getBaseUrl(context)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = SmartMoveTextSecondary,
                    )
                }
            }
        }
    }

    // Server Configuration Dialog
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = {
                Text("Backend Server Connection", color = SmartMoveTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Choose connection mode for your physical device or emulator:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = { customServerUrl = ApiClient.DEFAULT_PC_LAN_IP },
                            modifier = Modifier
                                .weight(1f)
                                .background(SmartMoveSurface, RoundedCornerShape(6.dp))
                                .border(1.dp, SmartMoveBorder, RoundedCornerShape(6.dp)),
                        ) {
                            Text("Wi-Fi LAN\n(10.21.143.187)", fontSize = 10.sp, color = SmartMoveSafe)
                        }

                        TextButton(
                            onClick = { customServerUrl = ApiClient.DEFAULT_USB_REVERSE_IP },
                            modifier = Modifier
                                .weight(1f)
                                .background(SmartMoveSurface, RoundedCornerShape(6.dp))
                                .border(1.dp, SmartMoveBorder, RoundedCornerShape(6.dp)),
                        ) {
                            Text("USB Cable\n(127.0.0.1)", fontSize = 10.sp, color = SmartMovePrimary)
                        }

                        TextButton(
                            onClick = { customServerUrl = ApiClient.DEFAULT_EMULATOR_IP },
                            modifier = Modifier
                                .weight(1f)
                                .background(SmartMoveSurface, RoundedCornerShape(6.dp))
                                .border(1.dp, SmartMoveBorder, RoundedCornerShape(6.dp)),
                        ) {
                            Text("Emulator\n(10.0.2.2)", fontSize = 10.sp, color = SmartMoveTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customServerUrl,
                        onValueChange = { customServerUrl = it },
                        label = { Text("Server Base URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customServerUrl.isNotBlank()) {
                            ApiClient.setBaseUrl(customServerUrl.trim(), context)
                            showServerDialog = false
                        }
                    }
                ) {
                    Text("Save & Connect", color = SmartMovePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel", color = SmartMoveTextSecondary)
                }
            }
        )
    }
}
