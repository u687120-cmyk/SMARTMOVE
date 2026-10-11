package com.smartmove.ai.ui.alerts

import com.smartmove.ai.ui.components.RiskLevel

/**
 * Standard hazard types supported by SMARTMOVE AI.
 */
enum class HazardType(val icon: String, val title: String) {
    POTHOLE("🕳️", "Pothole"),
    ACCIDENT("💥", "Accident"),
    ROAD_BLOCKAGE("🚧", "Road Blockage"),
    CONSTRUCTION("🏗️", "Construction"),
    HEAVY_TRAFFIC("🚗", "Heavy Traffic"),
    OTHER("⚠️", "Other");
}

/**
 * Lifecycle status of a road hazard.
 * A hazard is not permanent — it moves from ACTIVE -> UNDER_REVIEW -> RESOLVED.
 */
enum class HazardStatus(val label: String) {
    ACTIVE("Active"),
    UNDER_REVIEW("Under Review"),
    RESOLVED("Resolved");
}

/**
 * Complete data model for a reported road hazard.
 */
data class HazardItem(
    val id: String,
    val type: HazardType,
    val title: String,
    val severity: RiskLevel,
    val locationName: String = "Pune - Hinjewadi Road",
    val latitude: Double = 18.5204,
    val longitude: Double = 73.8567,
    val distanceMeters: Int = 300,
    val description: String? = null,
    val confidence: Int = 60,                   // Clamped 0-100%
    val status: HazardStatus = HazardStatus.ACTIVE,
    val reportedAtDisplay: String = "Just now",
    val lastConfirmedAtDisplay: String = "Just now",
    val reportCount: Int = 1,
    val confirmationCount: Int = 0,
    val fixedReportCount: Int = 0,
    val isReportedByCurrentUser: Boolean = false,
) {
    val distanceDisplay: String
        get() = if (distanceMeters >= 1000) {
            String.format(java.util.Locale.US, "%.1f km ahead", distanceMeters / 1000f)
        } else {
            "$distanceMeters m ahead"
        }
}

/**
 * UI State for the Alerts & Hazards Screen.
 */
data class HazardUiState(
    val activeHazards: List<HazardItem> = emptyList(),
    val myReports: List<HazardItem> = emptyList(),
    val selectedHazard: HazardItem? = null,
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
)
