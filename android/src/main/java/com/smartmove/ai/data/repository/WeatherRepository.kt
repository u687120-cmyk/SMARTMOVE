package com.smartmove.ai.data.repository

import android.content.Context
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.model.WeatherDto

/**
 * Domain model representing current meteorological conditions.
 */
data class WeatherModel(
    val temperatureC: Float,
    val condition: String,
    val conditionDisplay: String,
    val conditionIcon: String,
    val precipitationMm: Float,
    val humidity: Int,
    val windSpeedKmh: Float,
    val visibilityKm: Float,
    val observedAt: String,
    val isSevere: Boolean,
    val isCached: Boolean = false,
    val isFallback: Boolean = false,
)

/**
 * SMARTMOVE AI Weather Repository (Phase 14).
 * Fetches geo-located meteorological data from the backend weather service.
 */
class WeatherRepository(context: Context) {

    private val weatherApi = ApiClient.getWeatherApi(context)

    suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherModel {
        return try {
            val response = weatherApi.getCurrentWeather(latitude, longitude)
            if (response.isSuccessful && response.body()?.data != null) {
                response.body()!!.data!!.toWeatherModel(isFallback = false)
            } else {
                getFallbackWeather()
            }
        } catch (e: Exception) {
            getFallbackWeather()
        }
    }

    private fun getFallbackWeather(): WeatherModel {
        return WeatherModel(
            temperatureC = 25.0f,
            condition = "CLEAR",
            conditionDisplay = "Clear",
            conditionIcon = "☀️",
            precipitationMm = 0.0f,
            humidity = 50,
            windSpeedKmh = 10.0f,
            visibilityKm = 10.0f,
            observedAt = "Live unavailable",
            isSevere = false,
            isFallback = true,
        )
    }

    private fun WeatherDto.toWeatherModel(isFallback: Boolean): WeatherModel {
        val (display, icon) = when (condition.uppercase()) {
            "CLOUDY" -> Pair("Cloudy", "⛅")
            "RAIN" -> Pair("Rain", "🌧️")
            "HEAVY_RAIN" -> Pair("Heavy Rain", "⛈️")
            "FOG" -> Pair("Fog", "🌫️")
            else -> Pair("Clear", "☀️")
        }

        val severe = condition.uppercase() == "HEAVY_RAIN" ||
                condition.uppercase() == "FOG" ||
                visibilityKm < 2.0f

        return WeatherModel(
            temperatureC = temperatureC,
            condition = condition.uppercase(),
            conditionDisplay = display,
            conditionIcon = icon,
            precipitationMm = precipitationMm,
            humidity = humidity,
            windSpeedKmh = windSpeedKmh,
            visibilityKm = visibilityKm,
            observedAt = observedAt ?: "",
            isSevere = severe,
            isCached = isCached,
            isFallback = isFallback,
        )
    }
}
