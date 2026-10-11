package com.smartmove.ai.data.repository

import android.content.Context
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.model.RiskResponseDto
import com.smartmove.ai.ui.components.RiskLevel

/**
 * Domain model representing real-time calculated road risk.
 */
data class LiveRoadRisk(
    val score: Int,
    val level: RiskLevel,
    val recommendation: String,
    val hazardFactor: Int,
    val trafficFactor: Int,
    val weatherFactor: Int,
    val brakingFactor: Int,
    val eventFactor: Int,
    val reasons: List<String>,
    val weatherAvailable: Boolean = true,
    val weatherModel: WeatherModel? = null,
    val trafficAvailable: Boolean = true,
    val trafficModel: TrafficModel? = null,
    val isDemoFallback: Boolean = false,
)

/**
 * Risk Repository (Phase 13, 14 & 15).
 * Queries the backend Dynamic Risk Engine for live 0-100 road risk scores,
 * classification, and contributing factors, with clearly separated demo fallback.
 */
class RiskRepository(context: Context) {

    private val riskApi = ApiClient.getRiskApi(context)

    suspend fun getCurrentRisk(
        traffic: String? = null,
        weather: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ): LiveRoadRisk {
        return try {
            val response = riskApi.getCurrentRisk(traffic, weather, latitude, longitude)
            if (response.isSuccessful && response.body()?.data != null) {
                response.body()!!.data!!.toLiveRoadRisk(isDemoFallback = false)
            } else {
                getDemoFallbackRisk()
            }
        } catch (e: Exception) {
            getDemoFallbackRisk()
        }
    }

    private fun getDemoFallbackRisk(): LiveRoadRisk {
        return LiveRoadRisk(
            score = 24,
            level = RiskLevel.LOW,
            recommendation = "Road conditions look safe right now.",
            hazardFactor = 20,
            trafficFactor = 30,
            weatherFactor = 10,
            brakingFactor = 15,
            eventFactor = 10,
            reasons = listOf("Clear driving conditions and optimal road safety"),
            weatherAvailable = true,
            weatherModel = WeatherModel(
                temperatureC = 25f,
                condition = "CLEAR",
                conditionDisplay = "Clear",
                conditionIcon = "☀️",
                precipitationMm = 0f,
                humidity = 50,
                windSpeedKmh = 10f,
                visibilityKm = 10f,
                observedAt = "Default",
                isSevere = false,
                isFallback = true,
            ),
            trafficAvailable = true,
            trafficModel = TrafficModel(
                roadSegmentId = "seg-default-01",
                currentSpeedKmh = 55.0,
                freeFlowSpeedKmh = 60.0,
                speedRatio = 0.92,
                congestionLevel = "LOW",
                travelDelaySeconds = 0,
                source = "SIMULATED",
                isFallback = true,
            ),
            isDemoFallback = true,
        )
    }

    private fun RiskResponseDto.toLiveRoadRisk(isDemoFallback: Boolean): LiveRoadRisk {
        val lvl = when (level.uppercase()) {
            "HIGH" -> RiskLevel.HIGH
            "MEDIUM" -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val wModel = weather?.let { w ->
            val (display, icon) = when (w.condition.uppercase()) {
                "CLOUDY" -> Pair("Cloudy", "⛅")
                "RAIN" -> Pair("Rain", "🌧️")
                "HEAVY_RAIN" -> Pair("Heavy Rain", "⛈️")
                "FOG" -> Pair("Fog", "🌫️")
                else -> Pair("Clear", "☀️")
            }
            val severe = w.condition.uppercase() == "HEAVY_RAIN" ||
                    w.condition.uppercase() == "FOG" ||
                    w.visibilityKm < 2.0f

            WeatherModel(
                temperatureC = w.temperatureC,
                condition = w.condition.uppercase(),
                conditionDisplay = display,
                conditionIcon = icon,
                precipitationMm = w.precipitationMm,
                humidity = w.humidity,
                windSpeedKmh = w.windSpeedKmh,
                visibilityKm = w.visibilityKm,
                observedAt = w.observedAt ?: "",
                isSevere = severe,
                isCached = w.isCached,
                isFallback = false,
            )
        }

        val tModel = traffic?.let { t ->
            TrafficModel(
                roadSegmentId = t.roadSegmentId ?: "seg-default-01",
                currentSpeedKmh = t.currentSpeedKmh,
                freeFlowSpeedKmh = t.freeFlowSpeedKmh,
                speedRatio = t.speedRatio,
                congestionLevel = t.congestionLevel.uppercase(),
                travelDelaySeconds = t.travelDelaySeconds,
                observedAt = t.observedAt ?: "",
                source = t.source,
                isFallback = false,
            )
        }

        return LiveRoadRisk(
            score = score,
            level = lvl,
            recommendation = recommendation,
            hazardFactor = factors.hazards,
            trafficFactor = factors.traffic,
            weatherFactor = factors.weather,
            brakingFactor = factors.hardBraking,
            eventFactor = factors.events,
            reasons = reasons,
            weatherAvailable = weatherAvailable ?: true,
            weatherModel = wModel,
            trafficAvailable = trafficAvailable ?: true,
            trafficModel = tModel,
            isDemoFallback = isDemoFallback,
        )
    }
}
