package com.smartmove.ai.data.repository

import android.content.Context
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.model.AnalyticsDto
import com.smartmove.ai.ui.analytics.AnalyticsCalculator
import com.smartmove.ai.ui.analytics.AnalyticsPeriod
import com.smartmove.ai.ui.analytics.AnalyticsUiState
import com.smartmove.ai.ui.analytics.RiskBreakdown
import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.history.TripHistoryDataProvider
import java.util.Locale

/**
 * Analytics Repository.
 * Queries real-time SQL aggregated metrics from MySQL with local calculation fallback.
 */
class AnalyticsRepository(context: Context) {

    private val analyticsApi = ApiClient.getAnalyticsApi(context)

    suspend fun getAnalytics(period: AnalyticsPeriod): AnalyticsUiState {
        val periodParam = when (period) {
            AnalyticsPeriod.TODAY -> "today"
            AnalyticsPeriod.WEEK -> "week"
            AnalyticsPeriod.MONTH -> "month"
            AnalyticsPeriod.YEAR -> "year"
        }

        return try {
            val response = analyticsApi.getAnalytics(periodParam)
            if (response.isSuccessful && response.body()?.data != null) {
                response.body()!!.data!!.toUiState(period)
            } else {
                AnalyticsCalculator.calculate(TripHistoryDataProvider.getDemoTrips(), period)
            }
        } catch (e: Exception) {
            AnalyticsCalculator.calculate(TripHistoryDataProvider.getDemoTrips(), period)
        }
    }

    private fun AnalyticsDto.toUiState(period: AnalyticsPeriod): AnalyticsUiState {
        val riskLvl = when {
            averageRisk < 35 -> RiskLevel.LOW
            averageRisk <= 65 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }

        val suggestions = mutableListOf<String>()
        if (totalTrips == 0) {
            suggestions.add("Not enough trip data yet. Complete trips to generate personalized safety insights.")
        } else {
            if (totalHardBraking >= totalTrips * 2) {
                suggestions.add("Try maintaining a greater following distance to reduce sudden braking.")
            } else if (totalHardBraking == 0 && totalTrips >= 2) {
                suggestions.add("Smooth deceleration! Zero hard braking events recorded in this period.")
            }

            if (averageRisk >= 60) {
                suggestions.add("Your recent trips show increased safety events. Consider reducing speed in high-risk areas.")
            } else if (averageRisk <= 25 && totalTrips >= 1) {
                suggestions.add("Your recent trips show improved safety performance with consistently low risk.")
            }

            if (suggestions.isEmpty()) {
                suggestions.add("Drive attentively and maintain safe following distances in varying road conditions.")
            }
        }

        return AnalyticsUiState(
            selectedPeriod = period,
            totalDistanceKm = totalDistanceKm.toDouble(),
            formattedDistance = String.format(Locale.US, "%.1f km", totalDistanceKm),
            averageRisk = averageRisk,
            riskLevel = riskLvl,
            averageSafetyScore = averageSafetyScore,
            totalTrips = totalTrips,
            totalWarnings = totalWarnings,
            totalHazards = totalHazards,
            totalHardBraking = totalHardBraking,
            suggestions = suggestions,
            riskBreakdown = RiskBreakdown(
                lowCount = riskBreakdown.low,
                mediumCount = riskBreakdown.medium,
                highCount = riskBreakdown.high,
            ),
            isLoading = false,
        )
    }
}
