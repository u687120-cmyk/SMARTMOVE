package com.smartmove.ai.ui.auth

import androidx.compose.ui.graphics.Color
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveWarning

/**
 * Validation rules and password evaluation for SMARTMOVE AI authentication.
 * Pure Kotlin functions for testability and clarity.
 */
object AuthValidation {

    private val EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "Email is required"
            !EMAIL_REGEX.matches(trimmed) -> "Enter a valid email address"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "Password is required"
            password.length < 8 -> "Password must contain at least 8 characters"
            else -> null
        }
    }

    fun validateFullName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Name is required"
            trimmed.length < 2 -> "Please enter your full name"
            else -> null
        }
    }

    fun validatePhone(phone: String): String? {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return null // Optional field
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (digitsOnly.length < 7 || digitsOnly.length > 15) {
            "Enter a valid phone number"
        } else {
            null
        }
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Please confirm your password"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
    }

    fun validateTerms(accepted: Boolean): String? {
        return if (!accepted) {
            "Please accept the Terms & Privacy Policy"
        } else {
            null
        }
    }

    fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.EMPTY
        if (password.length < 8) return PasswordStrength.WEAK

        var score = 0
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        if (password.length >= 12) score++

        return when {
            score >= 4 -> PasswordStrength.STRONG
            score >= 2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}

enum class PasswordStrength(
    val label: String,
    val color: Color,
    val progress: Float,
) {
    EMPTY("Password required", Color.Transparent, 0f),
    WEAK("Weak", SmartMoveDanger, 0.33f),
    MEDIUM("Medium", SmartMoveWarning, 0.66f),
    STRONG("Strong", SmartMoveSafe, 1.0f);
}
