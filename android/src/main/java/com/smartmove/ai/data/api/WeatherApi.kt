package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.WeatherDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for SMARTMOVE AI Dynamic Weather API (Phase 14).
 */
interface WeatherApi {
    @GET("api/weather/current")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): Response<ApiResponse<WeatherDto>>
}
