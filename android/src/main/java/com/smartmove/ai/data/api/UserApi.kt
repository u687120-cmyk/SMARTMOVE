package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.CreateVehicleRequest
import com.smartmove.ai.data.model.UpdateProfileRequest
import com.smartmove.ai.data.model.UserDto
import com.smartmove.ai.data.model.VehicleDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApi {
    @GET("api/users/me")
    suspend fun getProfile(): Response<ApiResponse<UserDto>>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<UserDto>>

    @GET("api/users/me/vehicles")
    suspend fun getVehicles(): Response<ApiResponse<List<VehicleDto>>>

    @POST("api/users/me/vehicles")
    suspend fun addVehicle(@Body request: CreateVehicleRequest): Response<ApiResponse<VehicleDto>>

    @DELETE("api/users/me/vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Long): Response<ApiResponse<Unit>>
}
