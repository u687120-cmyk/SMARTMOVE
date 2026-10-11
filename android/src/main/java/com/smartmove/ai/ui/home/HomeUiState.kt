package com.smartmove.ai.ui.home

import com.smartmove.ai.ui.components.RiskLevel

/**
 * Severity levels for road alerts.
 */
enum class AlertSeverity {
    INFO,
    WARNING,
    DANGER,
}

/**
 * Model representing a real-time safety alert.
 */
data class ActiveAlertData(
    val id: String = "alert-001",
    val title: String = "NO ACTIVE ALERTS",
    val message: String = "You're good to go.",
    val severity: AlertSeverity = AlertSeverity.INFO,
    val distance: String? = null,
    val timestamp: String = "Just now",
)

/**
 * Model representing dynamic ambient conditions.
 */
data class CurrentConditionsData(
    val trafficLabel: String = "Low",
    val trafficEmoji: String = "🚗",
    val weatherLabel: String = "Clear",
    val weatherEmoji: String = "☀️",
    val hazardsLabel: String = "None",
    val hazardsEmoji: String = "✓",
)

/**
 * Model representing daily driving statistics.
 */
data class TodayOverviewData(
    val totalDistance: String = "128.4 km",
    val averageRisk: String = "24 / 100",
    val tripsCount: Int = 8,
    val warningsCount: Int = 3,
    val safetyScore: String = "92 / 100",
)

/**
 * Complete UI State model for the REAL SMARTMOVE AI Home Dashboard.
 *
 * Designed for future replacement by ViewModel / repository / backend data
 * without rewriting the UI layout.
 */
data class HomeSafetyUiState(
    val userName: String = "Ujjwal",
    val greeting: String = "Good Morning",
    val currentRiskLevel: RiskLevel = RiskLevel.LOW,
    val riskScore: Int = 24,
    val riskRecommendation: String = "Road conditions look safe right now.",
    val riskFactors: List<String> = listOf("Low traffic volume", "Clear dry roads", "Normal braking patterns"),
    val overview: TodayOverviewData = TodayOverviewData(),
    val conditions: CurrentConditionsData = CurrentConditionsData(),
    val activeAlert: ActiveAlertData? = null, // null indicates "NO ACTIVE ALERTS"
    val hasUnreadNotifications: Boolean = false,
)
