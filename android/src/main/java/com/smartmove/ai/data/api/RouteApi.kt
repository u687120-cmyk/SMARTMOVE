package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.RouteRecommendationDataDto
import com.smartmove.ai.data.model.RouteRecommendationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for SMARTMOVE AI Route Comparison & Recommendation (Phase 16).
 */
interface RouteApi {

    @POST("api/routes/recommend")
    suspend fun recommendRoutes(
        @Body request: RouteRecommendationRequest,
    ): Response<ApiResponse<RouteRecommendationDataDto>>

    @POST("api/routes/recalculate")
    suspend fun recalculateEnRoute(
        @Body request: com.smartmove.ai.data.model.RerouteEvaluationRequest,
    ): Response<ApiResponse<com.smartmove.ai.data.model.RerouteEvaluationResponseDto>>

    @POST("api/routes/switch")
    suspend fun switchRoute(
        @Body request: com.smartmove.ai.data.model.RouteSwitchRequest,
    ): Response<ApiResponse<com.smartmove.ai.data.model.RouteSwitchResponseDto>>
}
