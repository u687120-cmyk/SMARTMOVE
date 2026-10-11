package com.smartmove.ai.data.repository

import android.content.Context
import com.google.gson.Gson
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.CreateVehicleRequest
import com.smartmove.ai.data.model.UpdateProfileRequest
import com.smartmove.ai.data.model.UserDto
import com.smartmove.ai.data.model.VehicleDto
import retrofit2.Response

/**
 * User & Vehicle Repository.
 * Handles Driver profile queries and vehicle specifications over HTTP.
 */
class UserRepository(context: Context) {

    private val userApi = ApiClient.getUserApi(context)
    private val gson = Gson()

    private fun <T> parseErrorMessage(response: Response<T>, defaultMsg: String): String {
        return try {
            val errorJson = response.errorBody()?.string()
            if (!errorJson.isNullOrBlank()) {
                val parsed = gson.fromJson(errorJson, ApiResponse::class.java)
                parsed.message.takeIf { !it.isNullOrBlank() } ?: "HTTP ${response.code()}: $defaultMsg"
            } else {
                "HTTP ${response.code()}: $defaultMsg"
            }
        } catch (_: Exception) {
            "HTTP ${response.code()}: $defaultMsg"
        }
    }

    suspend fun getProfile(): Result<UserDto> {
        return try {
            val response = userApi.getProfile()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Failed to fetch profile")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        }
    }

    suspend fun updateProfile(fullName: String, phone: String?): Result<UserDto> {
        return try {
            val response = userApi.updateProfile(UpdateProfileRequest(fullName, phone))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Failed to update profile")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        }
    }

    suspend fun getVehicles(): Result<List<VehicleDto>> {
        return try {
            val response = userApi.getVehicles()
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Failed to fetch vehicles")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        }
    }

    suspend fun addVehicle(vehicleType: String, vehicleNumber: String?): Result<VehicleDto> {
        return try {
            val response = userApi.addVehicle(CreateVehicleRequest(vehicleType, vehicleNumber))
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Failed to add vehicle")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        }
    }

    suspend fun deleteVehicle(vehicleId: Long): Result<Unit> {
        return try {
            val response = userApi.deleteVehicle(vehicleId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Failed to delete vehicle")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        }
    }
}
