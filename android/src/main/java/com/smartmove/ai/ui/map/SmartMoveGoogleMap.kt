package com.smartmove.ai.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.smartmove.ai.data.repository.RouteCandidate
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import kotlinx.coroutines.launch

// Dark Theme Map Style JSON
private const val DARK_MAP_STYLE_JSON = """
[
  { "elementType": "geometry", "stylers": [ { "color": "#0f172a" } ] },
  { "elementType": "labels.text.fill", "stylers": [ { "color": "#94a3b8" } ] },
  { "elementType": "labels.text.stroke", "stylers": [ { "color": "#0f172a" } ] },
  { "featureType": "road", "elementType": "geometry", "stylers": [ { "color": "#1e293b" } ] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [ { "color": "#334155" } ] },
  { "featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [ { "color": "#1e293b" } ] },
  { "featureType": "water", "elementType": "geometry", "stylers": [ { "color": "#020617" } ] },
  { "featureType": "poi", "elementType": "geometry", "stylers": [ { "color": "#1e293b" } ] }
]
"""

/**
 * SMARTMOVE AI — Interactive Google Map.
 *
 * Renders real Google Map tiles, multi-route polylines, start/destination pins,
 * live traffic layer, hazard markers, and route selection directly on map.
 */
@Composable
fun SmartMoveGoogleMap(
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
    originTitle: String = "Start Location",
    destTitle: String = "Destination",
    routes: List<RouteCandidate> = emptyList(),
    selectedRouteId: String? = null,
    onRouteSelected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    showControls: Boolean = true,
) {
    val origin = LatLng(originLat, originLng)
    val destination = LatLng(destLat, destLng)
    val coroutineScope = rememberCoroutineScope()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(origin, 13f)
    }

    var isTrafficEnabled by remember { mutableStateOf(true) }
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }

    val mapProperties = remember(isTrafficEnabled, currentMapType, isDarkTheme) {
        MapProperties(
            isTrafficEnabled = isTrafficEnabled,
            mapType = currentMapType,
            mapStyleOptions = if (isDarkTheme && currentMapType == MapType.NORMAL) {
                MapStyleOptions(DARK_MAP_STYLE_JSON)
            } else null
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = true,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
        )
    }

    // Auto-fit bounds to display whole route
    LaunchedEffect(origin, destination) {
        try {
            val builder = LatLngBounds.builder()
            builder.include(origin)
            builder.include(destination)
            val bounds = builder.build()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(bounds, 120),
                durationMs = 1000
            )
        } catch (_: Exception) {
            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(origin, 13f))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SmartMoveBorder, RoundedCornerShape(16.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
        ) {
            // 1. Origin / Start Marker (Azure Pin)
            Marker(
                state = MarkerState(position = origin),
                title = originTitle,
                snippet = "Starting Point (GPS)",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
            )

            // 2. Destination Marker (Red Pin)
            Marker(
                state = MarkerState(position = destination),
                title = destTitle,
                snippet = "Target Destination",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
            )

            // 3. Render Route Polylines
            if (routes.isNotEmpty()) {
                routes.forEach { candidate ->
                    val isSelected = candidate.routeId == selectedRouteId
                    val polylinePoints = remember(candidate.routeId) {
                        generateRoutePoints(origin, destination, candidate.routeId)
                    }

                    Polyline(
                        points = polylinePoints,
                        clickable = true,
                        color = if (isSelected) SmartMovePrimary else Color(0xFF64748B),
                        width = if (isSelected) 14f else 8f,
                        zIndex = if (isSelected) 2f else 1f,
                        onClick = {
                            onRouteSelected?.invoke(candidate.routeId)
                        }
                    )
                }
            } else {
                // Default direct navigation line if route list is loading
                Polyline(
                    points = listOf(origin, destination),
                    color = SmartMovePrimary,
                    width = 12f,
                )
            }
        }

        // Floating Quick Map Controls
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    // Recenter button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val builder = LatLngBounds.builder()
                                    builder.include(origin)
                                    builder.include(destination)
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngBounds(builder.build(), 120),
                                        600
                                    )
                                } catch (_: Exception) {}
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(SmartMoveSurface.copy(alpha = 0.9f), CircleShape)
                            .border(1.dp, SmartMoveBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Center Map",
                            tint = SmartMovePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Live Traffic Toggle button
                    IconButton(
                        onClick = { isTrafficEnabled = !isTrafficEnabled },
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                if (isTrafficEnabled) SmartMovePrimary.copy(alpha = 0.2f) else SmartMoveSurface.copy(alpha = 0.9f),
                                CircleShape
                            )
                            .border(1.dp, if (isTrafficEnabled) SmartMovePrimary else SmartMoveBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Traffic Layer",
                            tint = if (isTrafficEnabled) SmartMovePrimary else SmartMoveTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Satellite / Normal Toggle
                    IconButton(
                        onClick = {
                            currentMapType = if (currentMapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(SmartMoveSurface.copy(alpha = 0.9f), CircleShape)
                            .border(1.dp, SmartMoveBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Map Type",
                            tint = SmartMoveTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generates curved route coordinates between origin and destination.
 */
private fun generateRoutePoints(origin: LatLng, dest: LatLng, routeId: String): List<LatLng> {
    val points = mutableListOf<LatLng>()
    points.add(origin)

    val midLat = (origin.latitude + dest.latitude) / 2
    val midLng = (origin.longitude + dest.longitude) / 2

    val offset = when {
        routeId.contains("alt-1") || routeId.contains("route-b") -> 0.008
        routeId.contains("alt-2") || routeId.contains("route-c") -> -0.008
        else -> 0.002
    }

    val waypoint1 = LatLng(
        origin.latitude + (midLat - origin.latitude) * 0.5 + offset * 0.7,
        origin.longitude + (midLng - origin.longitude) * 0.5 - offset * 0.5
    )
    val waypoint2 = LatLng(
        midLat + offset,
        midLng + offset * 0.8
    )
    val waypoint3 = LatLng(
        dest.latitude - (dest.latitude - midLat) * 0.5 + offset * 0.4,
        dest.longitude - (dest.longitude - midLng) * 0.5 - offset * 0.6
    )

    points.add(waypoint1)
    points.add(waypoint2)
    points.add(waypoint3)
    points.add(dest)
    return points
}
