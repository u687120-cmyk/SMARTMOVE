package com.smartmove.ai.data.api

import com.smartmove.ai.data.model.ApiResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class PlacePredictionDto(
    val placeId: String,
    val description: String,
    val mainText: String,
    val secondaryText: String?
)

data class PlaceDetailsDto(
    val placeId: String,
    val name: String,
    val formattedAddress: String,
    val lat: Double,
    val lng: Double
)

/**
 * Retrofit interface for Google Places & Autocomplete API.
 */
interface PlacesApi {

    @GET("api/places/autocomplete")
    suspend fun autocomplete(
        @Query("input") input: String,
        @Query("sessionToken") sessionToken: String? = null
    ): Response<ApiResponse<List<PlacePredictionDto>>>

    @GET("api/places/details")
    suspend fun getDetails(
        @Query("placeId") placeId: String
    ): Response<ApiResponse<PlaceDetailsDto>>
}
