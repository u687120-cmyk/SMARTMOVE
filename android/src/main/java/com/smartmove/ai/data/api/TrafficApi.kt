package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.TrafficDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for SMARTMOVE AI Traffic endpoints (Phase 15).
 */
interface TrafficApi {

    @GET("api/traffic/current")
    suspend fun getCurrentTraffic(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("scenario") scenario: String? = null,
    ): Response<ApiResponse<TrafficDto>>
}
