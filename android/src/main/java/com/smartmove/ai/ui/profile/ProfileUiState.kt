package com.smartmove.ai.ui.profile

/**
 * Distance measurement units.
 */
enum class DistanceUnit(val label: String, val shortLabel: String) {
    KILOMETERS("Kilometers (km)", "km"),
    MILES("Miles (mi)", "mi");
}

/**
 * UI State model for the SMARTMOVE AI Profile Screen.
 *
 * Manages user profile information, vehicle details, safety preferences,
 * and app metadata with clean separation for future backend integration.
 */
data class ProfileUiState(
    val userName: String = "Ujjwal",
    val email: String = "user@example.com",
    val phone: String = "+91 98765 43210",
    val vehicleType: String = "Car",
    val vehicleNumber: String = "MH12AB1234",
    val notificationsEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val appVersion: String = "1.0.0",
)
