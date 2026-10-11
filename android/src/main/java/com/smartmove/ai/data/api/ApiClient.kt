package com.smartmove.ai.data.api

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.smartmove.ai.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton HTTP Network Client for SMARTMOVE AI Android app.
 *
 * Supports seamless communication across:
 * 1. Physical Phone via Wi-Fi LAN (e.g. 10.21.143.187)
 * 2. Physical Phone via USB Cable (127.0.0.1 with adb reverse)
 * 3. Android Studio Emulator (10.0.2.2)
 *
 * Includes persistent host preference and runtime server switching.
 */
object ApiClient {

    private const val PREFS_NAME = "smartmove_network_prefs"
    private const val KEY_SERVER_URL = "custom_server_url"

    // Primary default Wi-Fi LAN address of development PC
    const val DEFAULT_PC_LAN_IP = "http://10.21.143.187:5000/"
    const val DEFAULT_EMULATOR_IP = "http://10.0.2.2:5000/"
    const val DEFAULT_USB_REVERSE_IP = "http://127.0.0.1:5000/"

    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("google/sdk_gphone")
                || Build.FINGERPRINT.startsWith("generic/google_sdk")
                || Build.FINGERPRINT.startsWith("generic_x86")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86"))
    }

    private var activeBaseUrl: String? = null
    private var retrofit: Retrofit? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getBaseUrl(context: Context? = null): String {
        if (activeBaseUrl != null) return activeBaseUrl!!

        if (context != null) {
            val savedUrl = getPrefs(context).getString(KEY_SERVER_URL, null)
            if (!savedUrl.isNullOrBlank()) {
                activeBaseUrl = if (savedUrl.endsWith("/")) savedUrl else "$savedUrl/"
                return activeBaseUrl!!
            }
        }

        activeBaseUrl = if (isEmulator()) {
            DEFAULT_EMULATOR_IP
        } else {
            // Default to PC Wi-Fi LAN IP for physical phones, works over same Wi-Fi network
            DEFAULT_PC_LAN_IP
        }
        return activeBaseUrl!!
    }

    fun setBaseUrl(newUrl: String, context: Context) {
        val sanitized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        activeBaseUrl = sanitized
        getPrefs(context).edit().putString(KEY_SERVER_URL, sanitized).apply()
        retrofit = buildRetrofit(context)
    }

    private fun buildRetrofit(context: Context): Retrofit {
        val url = getBaseUrl(context)
        val tokenManager = TokenManager.getInstance(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun getRetrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: buildRetrofit(context).also { retrofit = it }
        }
    }

    fun getAuthApi(context: Context): AuthApi = getRetrofit(context).create(AuthApi::class.java)
    fun getUserApi(context: Context): UserApi = getRetrofit(context).create(UserApi::class.java)
    fun getTripApi(context: Context): TripApi = getRetrofit(context).create(TripApi::class.java)
    fun getAnalyticsApi(context: Context): AnalyticsApi = getRetrofit(context).create(AnalyticsApi::class.java)
    fun getHazardApi(context: Context): HazardApi = getRetrofit(context).create(HazardApi::class.java)
    fun getRiskApi(context: Context): RiskApi = getRetrofit(context).create(RiskApi::class.java)
    fun getWeatherApi(context: Context): WeatherApi = getRetrofit(context).create(WeatherApi::class.java)
    fun getTrafficApi(context: Context): TrafficApi = getRetrofit(context).create(TrafficApi::class.java)
    fun getRouteApi(context: Context): RouteApi = getRetrofit(context).create(RouteApi::class.java)
    fun getPlacesApi(context: Context): PlacesApi = getRetrofit(context).create(PlacesApi::class.java)
}
