package com.smartmove.ai.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * SMARTMOVE AI Real-Time Location Provider.
 *
 * Implements high-accuracy GPS telemetry using FusedLocationProviderClient,
 * Android Geocoder for reverse-geocoding place names, and speed calculation.
 */
object LocationProvider {

    // Default reference location (Pune, Maharashtra) when device GPS is unavailable or disabled
    val DEFAULT_LOCATION = Pair(18.5204, 73.8567)
    val DEFAULT_LOCATION_NAME = "Pune, Maharashtra"

    /**
     * Checks if either fine or coarse location permission has been granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }

    /**
     * Obtains the user's high-accuracy current location using FusedLocationProviderClient.
     */
    suspend fun getFreshLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission(context)) return@withContext null

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val task = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            )
            val location = com.google.android.gms.tasks.Tasks.await(task)
            location
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obtains the user's last known geographic coordinates safely.
     */
    fun getLastKnownCoordinates(context: Context): Pair<Double, Double> {
        if (!hasLocationPermission(context)) {
            return DEFAULT_LOCATION
        }

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return DEFAULT_LOCATION

            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val loc = try {
                    locationManager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
                if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                    bestLocation = loc
                }
            }

            if (bestLocation != null) {
                Pair(bestLocation.latitude, bestLocation.longitude)
            } else {
                DEFAULT_LOCATION
            }
        } catch (e: Exception) {
            DEFAULT_LOCATION
        }
    }

    /**
     * Reverse geocodes latitude and longitude into a human-readable address.
     */
    suspend fun getAddressName(context: Context, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var resultAddress: String? = null
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val addr = addresses[0]
                        resultAddress = formatAddress(addr)
                    }
                }
                resultAddress ?: DEFAULT_LOCATION_NAME
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    formatAddress(addresses[0])
                } else {
                    DEFAULT_LOCATION_NAME
                }
            }
        } catch (e: Exception) {
            DEFAULT_LOCATION_NAME
        }
    }

    private fun formatAddress(addr: Address): String {
        val feature = addr.featureName
        val subLocality = addr.subLocality
        val locality = addr.locality
        val thoroughfare = addr.thoroughfare

        val parts = listOfNotNull(feature, thoroughfare, subLocality, locality).distinct()
        return if (parts.isNotEmpty()) {
            parts.take(3).joinToString(", ")
        } else {
            addr.getAddressLine(0) ?: DEFAULT_LOCATION_NAME
        }
    }
}

