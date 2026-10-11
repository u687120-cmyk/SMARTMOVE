package com.smartmove.ai.data.repository

import android.content.Context
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.api.PlaceDetailsDto
import com.smartmove.ai.data.api.PlacePredictionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for Google Places Autocomplete and Details.
 */
class PlacesRepository(private val context: Context) {

    private val api = ApiClient.getPlacesApi(context)

    suspend fun searchPlaces(query: String): List<PlacePredictionDto> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val response = api.autocomplete(query)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getPlaceDetails(placeId: String): PlaceDetailsDto? = withContext(Dispatchers.IO) {
        try {
            val response = api.getDetails(placeId)
            if (response.isSuccessful && response.body()?.success == true) {
                response.body()?.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
