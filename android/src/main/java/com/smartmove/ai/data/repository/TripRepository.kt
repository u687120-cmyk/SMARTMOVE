package com.smartmove.ai.data.repository

import android.content.Context
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.model.CreateTripRequest
import com.smartmove.ai.data.model.TripDto
import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.history.HistoryFilter
import com.smartmove.ai.ui.history.TripHistoryDataProvider
import com.smartmove.ai.ui.history.TripHistoryItem

/**
 * Trip Summary DTO for active journey completion (Phase 18).
 */
data class CompletedTripSummary(
    val origin: String,
    val destination: String,
    val routeName: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val averageRisk: Int,
    val riskLevel: RiskLevel,
    val safetyScore: Int,
    val warningsCount: Int,
    val hazardsCount: Int,
    val hardBrakingCount: Int,
    val reroutesCount: Int,
    val suggestion: String,
)

/**
 * Trip Repository.
 * Orchestrates remote MySQL trip queries via Retrofit with seamless
 * fallback to local structured data if backend is offline.
 */
class TripRepository(context: Context) {

    private val tripApi = ApiClient.getTripApi(context)

    companion object {
        @Volatile
        private var lastCompletedTrip: CompletedTripSummary? = null

        fun setLastCompletedTrip(summary: CompletedTripSummary) {
            lastCompletedTrip = summary
        }

        fun getLastCompletedTrip(): CompletedTripSummary? = lastCompletedTrip

        fun clearLastCompletedTrip() {
            lastCompletedTrip = null
        }
    }

    suspend fun getTrips(filter: HistoryFilter): List<TripHistoryItem> {
        val periodParam = when (filter) {
            HistoryFilter.ALL -> "all"
            HistoryFilter.TODAY -> "today"
            HistoryFilter.WEEK -> "week"
            HistoryFilter.MONTH -> "month"
        }

        return try {
            val response = tripApi.getTrips(periodParam)
            if (response.isSuccessful && response.body()?.data != null) {
                val dtoList = response.body()!!.data!!
                if (dtoList.isNotEmpty()) {
                    dtoList.map { it.toTripHistoryItem() }
                } else {
                    // Empty list returned from backend
                    emptyList()
                }
            } else {
                TripHistoryDataProvider.getTrips(filter)
            }
        } catch (e: Exception) {
            // Graceful fallback for offline demo / preview
            TripHistoryDataProvider.getTrips(filter)
        }
    }

    suspend fun getTripById(tripId: String): TripHistoryItem {
        return try {
            val response = tripApi.getTripDetails(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                response.body()!!.data!!.toTripHistoryItem()
            } else {
                TripHistoryDataProvider.getDemoTrips().find { it.id == tripId }
                    ?: TripHistoryDataProvider.getDemoTrips().first()
            }
        } catch (e: Exception) {
            TripHistoryDataProvider.getDemoTrips().find { it.id == tripId }
                ?: TripHistoryDataProvider.getDemoTrips().first()
        }
    }

    suspend fun createTrip(request: CreateTripRequest): Result<TripDto> {
        return try {
            val response = tripApi.createTrip(request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Trip could not be synced."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun TripDto.toTripHistoryItem(): TripHistoryItem {
        val riskLvl = when {
            averageRisk < 35 -> RiskLevel.LOW
            averageRisk <= 65 -> RiskLevel.MEDIUM
            else -> RiskLevel.HIGH
        }

        return TripHistoryItem(
            id = id.toString(),
            startLocation = startLocation,
            destination = destinationLocation,
            dateDisplay = startedAt.take(10),
            daysAgo = 0,
            startTime = if (startedAt.length >= 16) startedAt.substring(11, 16) else "10:00",
            endTime = if (!endedAt.isNullOrBlank() && endedAt.length >= 16) endedAt.substring(11, 16) else "10:38",
            durationMinutes = durationMinutes,
            distanceKm = distanceKm.toDouble(),
            averageRisk = averageRisk,
            riskLevel = riskLvl,
            safetyScore = safetyScore,
            warningCount = warningCount,
            hazardCount = hazardCount,
            hardBrakingCount = hardBrakingCount,
            trafficCondition = trafficCondition ?: "Moderate",
            weatherCondition = weatherCondition ?: "Clear",
        )
    }
}
