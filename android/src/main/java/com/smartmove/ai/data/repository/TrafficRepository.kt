package com.smartmove.ai.data.repository

import android.content.Context
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.model.TrafficDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain model representing dynamic road traffic flow and congestion (Phase 15).
 */
data class TrafficModel(
    val roadSegmentId: String = "seg-default-01",
    val currentSpeedKmh: Double = 55.0,
    val freeFlowSpeedKmh: Double = 60.0,
    val speedRatio: Double = 0.92,
    val congestionLevel: String = "LOW",
    val travelDelaySeconds: Int = 0,
    val observedAt: String = "",
    val source: String = "SIMULATED",
    val isFallback: Boolean = false,
) {
    val isHeavy: Boolean
        get() = congestionLevel.equals("HIGH", ignoreCase = true)

    val isModerate: Boolean
        get() = congestionLevel.equals("MODERATE", ignoreCase = true)

    val isLight: Boolean
        get() = congestionLevel.equals("LOW", ignoreCase = true)

    val congestionDisplay: String
        get() = when (congestionLevel.uppercase()) {
            "HIGH" -> "Heavy"
            "MODERATE" -> "Moderate"
            else -> "Light"
        }

    val trafficIcon: String
        get() = "🚗"

    val speedDisplay: String
        get() = "${currentSpeedKmh.toInt()} km/h"

    val freeFlowDisplay: String
        get() = "${freeFlowSpeedKmh.toInt()} km/h"
}

/**
 * Repository for fetching dynamic road segment traffic observations.
 */
class TrafficRepository(private val context: Context) {

    private val trafficApi = ApiClient.getTrafficApi(context)

    suspend fun getCurrentTraffic(
        latitude: Double,
        longitude: Double,
        scenario: String? = null,
    ): TrafficModel = withContext(Dispatchers.IO) {
        try {
            val response = trafficApi.getCurrentTraffic(latitude, longitude, scenario)
            if (response.isSuccessful && response.body()?.success == true) {
                val dto = response.body()!!.data
                if (dto != null) {
                    return@withContext mapDtoToModel(dto)
                }
            }
            createFallbackModel()
        } catch (e: Exception) {
            createFallbackModel()
        }
    }

    fun mapDtoToModel(dto: TrafficDto): TrafficModel {
        return TrafficModel(
            roadSegmentId = dto.roadSegmentId ?: "seg-default-01",
            currentSpeedKmh = dto.currentSpeedKmh,
            freeFlowSpeedKmh = dto.freeFlowSpeedKmh,
            speedRatio = dto.speedRatio,
            congestionLevel = dto.congestionLevel.uppercase(),
            travelDelaySeconds = dto.travelDelaySeconds,
            observedAt = dto.observedAt ?: "",
            source = dto.source,
            isFallback = false,
        )
    }

    private fun createFallbackModel(): TrafficModel {
        return TrafficModel(
            congestionLevel = "LOW",
            currentSpeedKmh = 55.0,
            freeFlowSpeedKmh = 60.0,
            speedRatio = 0.92,
            travelDelaySeconds = 0,
            isFallback = true,
        )
    }
}
