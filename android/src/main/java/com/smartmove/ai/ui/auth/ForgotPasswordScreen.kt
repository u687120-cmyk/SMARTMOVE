package com.smartmove.ai.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.components.SmartMoveOutlinedButton
import com.smartmove.ai.ui.components.SmartMoveTopBar
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveSafeBg
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Forgot Password Screen for SMARTMOVE AI.
 * Handles reset link request and demonstrates the success confirmation state.
 */
@Composable
fun ForgotPasswordScreen(
    onNavigateBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    fun performSendResetLink() {
        keyboardController?.hide()
        val err = AuthValidation.validateEmail(email)
        emailError = err

        if (err != null) return

        isLoading = true
        coroutineScope.launch {
            // Simulated network delay
            delay(1000L)
            isLoading = false
            isSuccess = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground),
    ) {
        SmartMoveTopBar(
            title = "Password Recovery",
            onBackClick = onNavigateBackToLogin,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = SmartMoveTheme.spacing.default, vertical = SmartMoveTheme.spacing.large),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!isSuccess) {
                    // Entry Form State
                    Column {
                        SmartMoveAuthHeader(
                            title = "Forgot Password?",
                            subtitle = "Enter your email and we'll help you reset your password.",
                        )

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

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
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { performSendResetLink() }
                            ),
                            enabled = !isLoading,
                        )

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

                        SmartMoveButton(
                            text = "SEND RESET LINK",
                            onClick = { performSendResetLink() },
                            loading = isLoading,
                            loadingText = "Sending reset link...",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    // Success Confirmation State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = SmartMoveTheme.spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Success Badge
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SmartMoveSafeBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = SmartMoveSafe,
                                modifier = Modifier.size(36.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

                        Text(
                            text = "Reset link sent",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = SmartMoveTextPrimary,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

                        Text(
                            text = "Check your email ($email) for password reset instructions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmartMoveTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = SmartMoveTheme.spacing.medium),
                        )

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

                        SmartMoveCard(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = SmartMoveTheme.spacing.default,
                        ) {
                            Text(
                                text = "Did not receive the email?",
                                style = MaterialTheme.typography.titleMedium,
                                color = SmartMoveTextPrimary,
                            )
                            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraSmall))
                            Text(
                                text = "Make sure to check your spam folder, or try resending.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SmartMoveTextSecondary,
                            )
                        }

                        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

                        SmartMoveButton(
                            text = "BACK TO LOGIN",
                            onClick = onNavigateBackToLogin,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Bottom Back to Login Link
                if (!isSuccess) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = SmartMoveTheme.spacing.huge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SmartMoveOutlinedButton(
                            text = "BACK TO LOGIN",
                            onClick = onNavigateBackToLogin,
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
