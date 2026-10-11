package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.ConfirmHazardRequest
import com.smartmove.ai.data.model.HazardDto
import com.smartmove.ai.data.model.ReportHazardRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface HazardApi {
    @GET("api/hazards/active")
    suspend fun getActiveHazards(): Response<ApiResponse<List<HazardDto>>>

    @GET("api/hazards/my-reports")
    suspend fun getMyReports(): Response<ApiResponse<List<HazardDto>>>

    @GET("api/hazards/{id}")
    suspend fun getHazardDetails(@Path("id") id: String): Response<ApiResponse<HazardDto>>

    @POST("api/hazards")
    suspend fun reportHazard(@Body request: ReportHazardRequest): Response<ApiResponse<HazardDto>>

    @POST("api/hazards/{id}/confirm")
    suspend fun confirmHazard(
        @Path("id") id: String,
        @Body request: ConfirmHazardRequest = ConfirmHazardRequest("STILL_PRESENT")
    ): Response<ApiResponse<HazardDto>>

    @POST("api/hazards/{id}/fixed")
    suspend fun reportFixed(@Path("id") id: String): Response<ApiResponse<HazardDto>>
}
