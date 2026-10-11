package com.smartmove.ai.ui.analytics

import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.history.TripHistoryItem
import com.smartmove.ai.ui.theme.SmartMovePrimary
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure calculation and aggregation layer for Safety Analytics.
 *
 * Designed to cleanly mirror future backend SQL aggregations:
 * - SUM(distanceKm)
 * - AVG(averageRisk)
 * - AVG(safetyScore)
 * - COUNT(trips)
 * - SUM(warningCount)
 * - SUM(hazardCount)
 * - SUM(hardBrakingCount)
 */
object AnalyticsCalculator {

    fun calculate(
        allTrips: List<TripHistoryItem>,
        period: AnalyticsPeriod,
    ): AnalyticsUiState {
        val filtered = when (period) {
            AnalyticsPeriod.TODAY -> allTrips.filter { it.daysAgo == 0 }
            AnalyticsPeriod.WEEK -> allTrips.filter { it.daysAgo <= 6 }
            AnalyticsPeriod.MONTH -> allTrips.filter { it.daysAgo <= 29 }
            AnalyticsPeriod.YEAR -> allTrips.filter { it.daysAgo <= 365 }
        }

        if (filtered.isEmpty()) {
            return AnalyticsUiState(
                selectedPeriod = period,
                filteredTrips = emptyList(),
                totalDistanceKm = 0.0,
                formattedDistance = "0.0 km",
                averageRisk = 0,
                riskLevel = RiskLevel.LOW,
                averageSafetyScore = 0,
                totalTrips = 0,
                totalWarnings = 0,
                totalHazards = 0,
                totalHardBraking = 0,
                riskBreakdown = RiskBreakdown(),
                riskTrend = emptyList(),
                distanceTrend = emptyList(),
            )
        }

        // 1. Total Distance: SUM(distanceKm)
        val totalDist = filtered.sumOf { it.distanceKm }
        val formattedDist = String.format(Locale.US, "%.1f km", totalDist)

        // 2. Average Risk: SUM(averageRisk) / COUNT(trips)
        val avgRisk = (filtered.sumOf { it.averageRisk }.toDouble() / filtered.size).roundToInt()
        val riskLvl = RiskLevel.fromScore(avgRisk)

        // 3. Safety Score: SUM(safetyScore) / COUNT(trips)
        val avgSafety = (filtered.sumOf { it.safetyScore }.toDouble() / filtered.size).roundToInt()

        // 4. Totals
        val totalTrips = filtered.size
        val totalWarn = filtered.sumOf { it.warningCount }
        val totalHaz = filtered.sumOf { it.hazardCount }
        val totalBraking = filtered.sumOf { it.hardBrakingCount }

        // 5. Risk Breakdown
        val lowCount = filtered.count { it.riskLevel == RiskLevel.LOW }
        val medCount = filtered.count { it.riskLevel == RiskLevel.MEDIUM }
        val highCount = filtered.count { it.riskLevel == RiskLevel.HIGH }
        val breakdown = RiskBreakdown(lowCount = lowCount, mediumCount = medCount, highCount = highCount)

        // 6. Risk Trend Points (ordered chronologically from oldest to newest in period)
        val chronologicalTrips = filtered.sortedByDescending { it.daysAgo }
        val riskTrendPoints = chronologicalTrips.map { trip ->
            AnalyticsTrendPoint(
                label = if (period == AnalyticsPeriod.TODAY) trip.startTime else trip.dateDisplay,
                primaryValue = trip.averageRisk.toFloat(),
                displayValue = "${trip.averageRisk}/100",
                color = trip.riskLevel.color,
                note = "${trip.startLocation} → ${trip.destination}",
            )
        }

        // 7. Distance Trend Points
        val distanceTrendPoints = chronologicalTrips.map { trip ->
            AnalyticsTrendPoint(
                label = if (period == AnalyticsPeriod.TODAY) trip.startTime else trip.dateDisplay,
                primaryValue = trip.distanceKm.toFloat(),
                displayValue = String.format(Locale.US, "%.1f km", trip.distanceKm),
                color = SmartMovePrimary,
                note = trip.destination,
            )
        }

        // 8. Dynamic Data-Driven Safety Suggestions (Phase 18 Step 8)
        val suggestions = mutableListOf<String>()
        if (totalBraking >= totalTrips * 2) {
            suggestions.add("Try maintaining a greater following distance to reduce sudden braking.")
        } else if (totalBraking == 0 && totalTrips >= 2) {
            suggestions.add("Smooth deceleration! Zero hard braking events recorded in this period.")
        }

        if (avgRisk >= 60) {
            suggestions.add("Your recent trips show increased safety events. Consider reducing speed in high-risk areas.")
        } else if (avgRisk <= 25 && totalTrips >= 1) {
            suggestions.add("Your recent trips show improved safety performance with consistently low risk.")
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Drive attentively and maintain safe following distances in varying road conditions.")
        }

        return AnalyticsUiState(
            selectedPeriod = period,
            filteredTrips = filtered,
            totalDistanceKm = totalDist,
            formattedDistance = formattedDist,
            averageRisk = avgRisk,
            riskLevel = riskLvl,
            averageSafetyScore = avgSafety,
            totalTrips = totalTrips,
            totalWarnings = totalWarn,
            totalHazards = totalHaz,
            totalHardBraking = totalBraking,
            suggestions = suggestions,
            riskBreakdown = breakdown,
            riskTrend = riskTrendPoints,
            distanceTrend = distanceTrendPoints,
        )
    }
}
