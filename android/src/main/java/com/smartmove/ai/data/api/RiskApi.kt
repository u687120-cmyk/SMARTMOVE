package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.RiskResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for SMARTMOVE AI Dynamic Risk Engine (Phase 13).
 */
interface RiskApi {
    @GET("api/risk/current")
    suspend fun getCurrentRisk(
        @Query("traffic") traffic: String? = null,
        @Query("weather") weather: String? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): Response<ApiResponse<RiskResponseDto>>
}
