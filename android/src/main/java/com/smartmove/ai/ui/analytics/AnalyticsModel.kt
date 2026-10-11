package com.smartmove.ai.ui.analytics

import androidx.compose.ui.graphics.Color
import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.history.TripHistoryItem

/**
 * Period selector for Safety Analytics.
 */
enum class AnalyticsPeriod(val label: String) {
    TODAY("TODAY"),
    WEEK("WEEK"),
    MONTH("MONTH"),
    YEAR("YEAR");
}

/**
 * Distribution of trips across safety risk tiers.
 */
data class RiskBreakdown(
    val lowCount: Int = 0,
    val mediumCount: Int = 0,
    val highCount: Int = 0,
) {
    val total: Int get() = lowCount + mediumCount + highCount
    val lowRatio: Float get() = if (total > 0) lowCount.toFloat() / total else 0f
    val mediumRatio: Float get() = if (total > 0) mediumCount.toFloat() / total else 0f
    val highRatio: Float get() = if (total > 0) highCount.toFloat() / total else 0f
}

/**
 * A data point for rendering simple trends (risk or distance).
 */
data class AnalyticsTrendPoint(
    val label: String,             // e.g. "10:32 AM" or "Tue"
    val primaryValue: Float,       // e.g. 28f (risk) or 24.6f (distance)
    val displayValue: String,      // e.g. "28/100" or "24.6 km"
    val color: Color,
    val note: String? = null,
)

/**
 * Complete UI State for the Safety Analytics Screen.
 * Contains aggregated metrics derived dynamically from the trip dataset.
 */
data class AnalyticsUiState(
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.TODAY,
    val filteredTrips: List<TripHistoryItem> = emptyList(),
    val totalDistanceKm: Double = 0.0,
    val formattedDistance: String = "0.0 km",
    val averageRisk: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val averageSafetyScore: Int = 0,
    val totalTrips: Int = 0,
    val totalWarnings: Int = 0,
    val totalHazards: Int = 0,
    val totalHardBraking: Int = 0,
    val reroutesCount: Int = 0,
    val suggestions: List<String> = emptyList(),
    val riskBreakdown: RiskBreakdown = RiskBreakdown(),
    val riskTrend: List<AnalyticsTrendPoint> = emptyList(),
    val distanceTrend: List<AnalyticsTrendPoint> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean get() = totalTrips == 0 && !isLoading && errorMessage == null
    val hasSufficientData: Boolean get() = totalTrips >= 2
}
