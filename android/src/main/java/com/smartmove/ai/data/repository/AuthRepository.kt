package com.smartmove.ai.data.repository

import android.content.Context
import com.google.gson.Gson
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.local.TokenManager
import com.smartmove.ai.data.model.ApiResponse
import com.smartmove.ai.data.model.AuthResponse
import com.smartmove.ai.data.model.LoginRequest
import com.smartmove.ai.data.model.SignupRequest
import com.smartmove.ai.data.model.UserDto
import retrofit2.Response

/**
 * Authentication Repository.
 * Handles driver registration, login credential verification,
 * secure token storage, and session validation.
 */
class AuthRepository(private val context: Context) {

    private val authApi = ApiClient.getAuthApi(context)
    private val tokenManager = TokenManager.getInstance(context)
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

    suspend fun signup(fullName: String, email: String, phone: String?, password: String): Result<AuthResponse> {
        return try {
            val response = authApi.signup(SignupRequest(fullName, email, password, phone))
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                tokenManager.saveToken(data.token)
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response, "Registration failed")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        }
    }

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            val response = authApi.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                tokenManager.saveToken(data.token)
                Result.success(data)
            } else {
                val errorMsg = parseErrorMessage(response, "Invalid email or password")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        }
    }

    suspend fun checkSession(): Result<UserDto> {
        if (!tokenManager.hasToken()) {
            return Result.failure(Exception("No active session found"))
        }

        return try {
            val response = authApi.getMe()
            if (response.isSuccessful && response.body()?.data != null) {
                val user = response.body()!!.data!!["user"]
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("Empty user data"))
                }
            } else {
                tokenManager.clearToken()
                val errorMsg = parseErrorMessage(response, "Session expired")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network connection error"))
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            authApi.logout()
            tokenManager.clearToken()
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearToken()
            Result.success(Unit)
        }
    }

    fun hasSavedToken(): Boolean = tokenManager.hasToken()
}
