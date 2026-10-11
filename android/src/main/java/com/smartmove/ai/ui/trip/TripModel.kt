package com.smartmove.ai.ui.trip

import com.smartmove.ai.data.model.ParticipantRole
import com.smartmove.ai.data.model.TripParticipant
import com.smartmove.ai.ui.components.RiskLevel

/**
 * Normalized coordinate point along a simulated route (0.0f to 1.0f).
 * Ready for future replacement by Lat/Lng geopoints.
 */
data class RoutePoint(
    val xRatio: Float,
    val yRatio: Float,
    val label: String? = null,
)

/**
 * Hazard marker placed on the simulated map.
 */
data class MapHazard(
    val id: String,
    val title: String,
    val type: String,               // Pothole, Accident, Road Blockage, Construction, Traffic
    val xRatio: Float,
    val yRatio: Float,
    val severity: RiskLevel,
    val distanceMeters: Int,
    val isActive: Boolean = true,
)

/**
 * Upcoming hazard or event on the road ahead.
 */
data class RoadAheadItem(
    val id: String,
    val icon: String,
    val title: String,
    val distanceDisplay: String,
    val severity: RiskLevel,
)

/**
 * UI State for the Active Trip and Smart Map screen.
 */
data class TripUiState(
    val tripId: String? = null,
    val isTripActive: Boolean = true,
    val origin: String = "MIT-WPU, Pune",
    val destination: String = "Hinjewadi Phase 1",
    val currentRoad: String = "Paud Road → Hinjewadi Highway",
    val currentSpeedKmh: Int = 0,
    val remainingDistanceKm: Double = 14.7,
    val etaMinutes: Int = 32,
    val riskScore: Int = 24,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val riskRecommendation: String = "Safe driving conditions. Follow speed limits.",
    val trafficStatus: String = "Moderate",
    val weatherStatus: String = "Clear 24°C",
    val nearbyHazardsCount: Int = 1,
    val originLat: Double = 18.5178,
    val originLng: Double = 73.8151,
    val destLat: Double = 18.5913,
    val destLng: Double = 73.7389,
    val overviewPolyline: String? = null,
    val participants: List<TripParticipant> = listOf(
        TripParticipant(id = "p-user", name = "You", role = ParticipantRole.DRIVER, isSelected = true),
        TripParticipant(id = "p-1", name = "Rahul", role = ParticipantRole.PASSENGER, isSelected = true)
    ),
    val vehicleXRatio: Float = 0.38f,
    val vehicleYRatio: Float = 0.48f,
    val destinationXRatio: Float = 0.85f,
    val destinationYRatio: Float = 0.22f,
    val hazardsOnMap: List<MapHazard> = defaultHazards,
    val roadAhead: List<RoadAheadItem> = defaultRoadAhead,
    val routePoints: List<RoutePoint> = defaultRoutePoints,
) {
    companion object {
        val defaultRoutePoints = listOf(
            RoutePoint(0.12f, 0.78f, "Origin"),
            RoutePoint(0.24f, 0.62f),
            RoutePoint(0.38f, 0.48f, "Current"),
            RoutePoint(0.55f, 0.42f),
            RoutePoint(0.70f, 0.30f),
            RoutePoint(0.85f, 0.22f, "Destination"),
        )

        val defaultHazards = listOf(
            MapHazard(
                id = "hz-1",
                title = "Pothole",
                type = "Pothole",
                xRatio = 0.52f,
                yRatio = 0.43f,
                severity = RiskLevel.MEDIUM,
                distanceMeters = 400,
            ),
            MapHazard(
                id = "hz-2",
                title = "Road Work",
                type = "Construction",
                xRatio = 0.72f,
                yRatio = 0.28f,
                severity = RiskLevel.HIGH,
                distanceMeters = 1200,
            ),
        )

        val defaultRoadAhead = listOf(
            RoadAheadItem(
                id = "ra-1",
                icon = "⚠️",
                title = "Pothole",
                distanceDisplay = "400 m",
                severity = RiskLevel.MEDIUM,
            ),
            RoadAheadItem(
                id = "ra-2",
                icon = "🚦",
                title = "Traffic Signal",
                distanceDisplay = "700 m",
                severity = RiskLevel.LOW,
            ),
            RoadAheadItem(
                id = "ra-3",
                icon = "🚧",
                title = "Road Work",
                distanceDisplay = "1.2 km",
                severity = RiskLevel.HIGH,
            ),
        )
    }
}
