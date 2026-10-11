package com.smartmove.ai.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveOutlinedButton
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sign Up / Registration Screen for SMARTMOVE AI.
 * Captures user details, vehicle profile, and terms agreement with complete validation.
 */
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedVehicleType by remember { mutableStateOf("Car") }
    var vehicleNumber by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val authRepo = remember { com.smartmove.ai.data.repository.AuthRepository(context) }
    val userRepo = remember { com.smartmove.ai.data.repository.UserRepository(context) }

    val passwordStrength = remember(password) {
        AuthValidation.calculatePasswordStrength(password)
    }

    val vehicleTypes = listOf("Car", "Motorcycle", "SUV", "Truck", "Commercial")

    fun performSignup() {
        keyboardController?.hide()
        serverError = null

        val nameErr = AuthValidation.validateFullName(fullName)
        val emailErr = AuthValidation.validateEmail(email)
        val phoneErr = AuthValidation.validatePhone(phone)
        val passErr = AuthValidation.validatePassword(password)
        val confirmErr = AuthValidation.validateConfirmPassword(password, confirmPassword)
        val termsErr = AuthValidation.validateTerms(termsAccepted)

        fullNameError = nameErr
        emailError = emailErr
        phoneError = phoneErr
        passwordError = passErr
        confirmPasswordError = confirmErr
        termsError = termsErr

        if (nameErr != null || emailErr != null || phoneErr != null ||
            passErr != null || confirmErr != null || termsErr != null
        ) {
            return
        }

        isLoading = true
        coroutineScope.launch {
            val result = authRepo.signup(
                fullName = fullName.trim(),
                email = email.trim(),
                phone = phone.takeIf { it.isNotBlank() },
                password = password,
            )
            if (result.isSuccess) {
                try {
                    userRepo.addVehicle(selectedVehicleType, vehicleNumber.takeIf { it.isNotBlank() })
                } catch (_: Exception) {}
                isLoading = false
                onSignupSuccess()
            } else {
                isLoading = false
                serverError = result.exceptionOrNull()?.message ?: "Registration failed."
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
        ) {
            // Header
            SmartMoveAuthHeader(
                title = "Create Account",
                subtitle = "Join SMARTMOVE AI for intelligent road safety.",
            )

            if (serverError != null) {
                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))
                Text(
                    text = serverError!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.smartmove.ai.ui.theme.SmartMoveDanger,
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // 1. Full Name
            SmartMoveTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    if (fullNameError != null) fullNameError = null
                },
                label = "Full Name",
                placeholder = "John Doe",
                errorMessage = fullNameError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // 2. Email Address
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
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // 3. Phone Number (Optional)
            SmartMoveTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    if (phoneError != null) phoneError = null
                },
                label = "Phone Number (Optional)",
                placeholder = "+1 234 567 8900",
                errorMessage = phoneError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next,
                ),
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // 4. Password
            SmartMovePasswordField(
                value = password,
                onValueChange = {
                    password = it
                    if (passwordError != null) passwordError = null
                },
                label = "Password",
                placeholder = "Minimum 8 characters",
                errorMessage = passwordError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                enabled = !isLoading,
            )

            // Password Strength Indicator
            PasswordStrengthIndicator(strength = passwordStrength)

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            // 5. Confirm Password
            SmartMovePasswordField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (confirmPasswordError != null) confirmPasswordError = null
                },
                label = "Confirm Password",
                placeholder = "Re-enter your password",
                errorMessage = confirmPasswordError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

            // Vehicle Section
            Text(
                text = "Vehicle Information",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SmartMoveTextPrimary,
            )
            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            // Vehicle Type Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vehicleTypes.take(3).forEach { type ->
                    val isSelected = selectedVehicleType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (!isLoading) selectedVehicleType = type },
                        label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmartMovePrimary,
                            selectedLabelColor = SmartMoveTextPrimary,
                            containerColor = SmartMoveSurface,
                            labelColor = SmartMoveTextSecondary,
                        ),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vehicleTypes.drop(3).forEach { type ->
                    val isSelected = selectedVehicleType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (!isLoading) selectedVehicleType = type },
                        label = { Text(type, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SmartMovePrimary,
                            selectedLabelColor = SmartMoveTextPrimary,
                            containerColor = SmartMoveSurface,
                            labelColor = SmartMoveTextSecondary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            // Vehicle Number (Optional)
            SmartMoveTextField(
                value = vehicleNumber,
                onValueChange = { vehicleNumber = it },
                label = "Vehicle Registration / Plate (Optional)",
                placeholder = "e.g. ABC-1234",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { performSignup() }
                ),
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // Terms & Privacy Agreement Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) {
                        termsAccepted = !termsAccepted
                        if (termsAccepted) termsError = null
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = {
                        termsAccepted = it
                        if (termsAccepted) termsError = null
                    },
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(
                        checkedColor = SmartMovePrimary,
                        uncheckedColor = SmartMoveBorder,
                        checkmarkColor = SmartMoveTextPrimary,
                    ),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "I agree to the Terms of Service & Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = if (termsError != null) SmartMoveDanger else SmartMoveTextSecondary,
                )
            }

            AnimatedVisibility(visible = termsError != null) {
                if (termsError != null) {
                    Text(
                        text = termsError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveDanger,
                        modifier = Modifier.padding(start = 48.dp, bottom = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

            // CREATE ACCOUNT Button
            SmartMoveButton(
                text = "CREATE ACCOUNT",
                onClick = { performSignup() },
                loading = isLoading,
                loadingText = "Creating account...",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

            // Bottom Switch to Login
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartMoveTextSecondary,
                )

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

                SmartMoveOutlinedButton(
                    text = "LOGIN",
                    onClick = onNavigateToLogin,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.default))
        }
    }
}
