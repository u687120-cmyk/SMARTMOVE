package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.AnalyticsDto
import com.smartmove.ai.data.model.ApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AnalyticsApi {
    @GET("api/analytics")
    suspend fun getAnalytics(@Query("period") period: String): Response<ApiResponse<AnalyticsDto>>
}
