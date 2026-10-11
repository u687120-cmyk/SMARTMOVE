package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.CreateTripRequest
import com.smartmove.ai.data.model.TripDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TripApi {
    @POST("api/trips")
    suspend fun createTrip(@Body request: CreateTripRequest): Response<ApiResponse<TripDto>>

    @GET("api/trips")
    suspend fun getTrips(@Query("period") period: String? = null): Response<ApiResponse<List<TripDto>>>

    @GET("api/trips/{id}")
    suspend fun getTripDetails(@Path("id") id: String): Response<ApiResponse<TripDto>>
}
