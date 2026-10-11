package com.smartmove.ai.data.model

import com.google.gson.annotations.SerializedName

/**
 * Standard backend API response envelope.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
)

// --------------------------------------------------
// AUTH & USER MODELS
// --------------------------------------------------

data class SignupRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("phone") val phone: String? = null,
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto,
)

data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone") val phone: String? = null,
)

data class UpdateProfileRequest(
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String? = null,
)

data class VehicleDto(
    @SerializedName("id") val id: Long,
    @SerializedName("vehicleType") val vehicleType: String,
    @SerializedName("vehicleNumber") val vehicleNumber: String? = null,
)

data class CreateVehicleRequest(
    @SerializedName("vehicleType") val vehicleType: String,
    @SerializedName("vehicleNumber") val vehicleNumber: String? = null,
)

// --------------------------------------------------
// TRIP & ANALYTICS MODELS
// --------------------------------------------------

data class CreateTripRequest(
    @SerializedName("vehicleId") val vehicleId: Long? = null,
    @SerializedName("startLatitude") val startLatitude: Double = 18.5204,
    @SerializedName("startLongitude") val startLongitude: Double = 73.8567,
    @SerializedName("startLocation") val startLocation: String,
    @SerializedName("destinationLatitude") val destinationLatitude: Double = 18.5912,
    @SerializedName("destinationLongitude") val destinationLongitude: Double = 73.7389,
    @SerializedName("destinationLocation") val destinationLocation: String,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("distanceKm") val distanceKm: Float,
    @SerializedName("durationMinutes") val durationMinutes: Int,
    @SerializedName("averageRisk") val averageRisk: Int,
    @SerializedName("safetyScore") val safetyScore: Int,
    @SerializedName("warningCount") val warningCount: Int = 0,
    @SerializedName("hazardCount") val hazardCount: Int = 0,
    @SerializedName("hardBrakingCount") val hardBrakingCount: Int = 0,
    @SerializedName("trafficCondition") val trafficCondition: String = "Moderate",
    @SerializedName("weatherCondition") val weatherCondition: String = "Clear",
)

data class TripDto(
    @SerializedName("id") val id: Long,
    @SerializedName("startLocation") val startLocation: String,
    @SerializedName("destinationLocation") val destinationLocation: String,
    @SerializedName("startedAt") val startedAt: String,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("distanceKm") val distanceKm: Float,
    @SerializedName("durationMinutes") val durationMinutes: Int,
    @SerializedName("averageRisk") val averageRisk: Int,
    @SerializedName("safetyScore") val safetyScore: Int,
    @SerializedName("warningCount") val warningCount: Int,
    @SerializedName("hazardCount") val hazardCount: Int,
    @SerializedName("hardBrakingCount") val hardBrakingCount: Int,
    @SerializedName("trafficCondition") val trafficCondition: String? = null,
    @SerializedName("weatherCondition") val weatherCondition: String? = null,
)

data class AnalyticsDto(
    @SerializedName("period") val period: String,
    @SerializedName("totalDistanceKm") val totalDistanceKm: Float,
    @SerializedName("averageRisk") val averageRisk: Int,
    @SerializedName("averageSafetyScore") val averageSafetyScore: Int,
    @SerializedName("totalTrips") val totalTrips: Int,
    @SerializedName("totalWarnings") val totalWarnings: Int,
    @SerializedName("totalHazards") val totalHazards: Int,
    @SerializedName("totalHardBraking") val totalHardBraking: Int,
    @SerializedName("riskBreakdown") val riskBreakdown: RiskBreakdownDto,
)

data class RiskBreakdownDto(
    @SerializedName("low") val low: Int = 0,
    @SerializedName("medium") val medium: Int = 0,
    @SerializedName("high") val high: Int = 0,
)

// --------------------------------------------------
// HAZARD MODELS
// --------------------------------------------------

data class HazardDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("locationName") val locationName: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("confidence") val confidence: Int,
    @SerializedName("reportedAt") val reportedAt: String,
    @SerializedName("lastConfirmedAt") val lastConfirmedAt: String,
)

data class ReportHazardRequest(
    @SerializedName("type") val type: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("locationName") val locationName: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String? = null,
)

data class ConfirmHazardRequest(
    @SerializedName("confirmationType") val confirmationType: String,
)

// --------------------------------------------------
// RISK ENGINE MODELS (Phase 13)
// --------------------------------------------------

data class RiskResponseDto(
    @SerializedName("score") val score: Int,
    @SerializedName("level") val level: String,
    @SerializedName("recommendation") val recommendation: String,
    @SerializedName("factors") val factors: RiskFactorsDto,
    @SerializedName("reasons") val reasons: List<String> = emptyList(),
    @SerializedName("weatherAvailable") val weatherAvailable: Boolean? = true,
    @SerializedName("weather") val weather: WeatherDto? = null,
    @SerializedName("trafficAvailable") val trafficAvailable: Boolean? = true,
    @SerializedName("traffic") val traffic: TrafficDto? = null,
)

data class RiskFactorsDto(
    @SerializedName("hazards") val hazards: Int = 0,
    @SerializedName("traffic") val traffic: Int = 0,
    @SerializedName("weather") val weather: Int = 0,
    @SerializedName("hardBraking") val hardBraking: Int = 0,
    @SerializedName("events") val events: Int = 0,
)

// --------------------------------------------------
// WEATHER MODELS (Phase 14)
// --------------------------------------------------

data class WeatherDto(
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("temperatureC") val temperatureC: Float = 25f,
    @SerializedName("condition") val condition: String = "CLEAR",
    @SerializedName("precipitationMm") val precipitationMm: Float = 0f,
    @SerializedName("humidity") val humidity: Int = 50,
    @SerializedName("windSpeedKmh") val windSpeedKmh: Float = 10f,
    @SerializedName("visibilityKm") val visibilityKm: Float = 10f,
    @SerializedName("observedAt") val observedAt: String? = null,
    @SerializedName("isCached") val isCached: Boolean = false,
)

// --------------------------------------------------
// TRAFFIC MODELS (Phase 15)
// --------------------------------------------------

data class TrafficDto(
    @SerializedName("roadSegmentId") val roadSegmentId: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("currentSpeedKmh") val currentSpeedKmh: Double = 55.0,
    @SerializedName("freeFlowSpeedKmh") val freeFlowSpeedKmh: Double = 60.0,
    @SerializedName("speedRatio") val speedRatio: Double = 0.92,
    @SerializedName("congestionLevel") val congestionLevel: String = "LOW",
    @SerializedName("travelDelaySeconds") val travelDelaySeconds: Int = 0,
    @SerializedName("observedAt") val observedAt: String? = null,
    @SerializedName("source") val source: String = "SIMULATED",
    @SerializedName("isCached") val isCached: Boolean = false,
)

// --------------------------------------------------
// ROUTE RECOMMENDATION MODELS (Phase 16)
// --------------------------------------------------

data class GeoLocationDto(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
)

data class RouteRecommendationRequest(
    @SerializedName("origin") val origin: GeoLocationDto,
    @SerializedName("destination") val destination: GeoLocationDto,
    @SerializedName("weatherCondition") val weatherCondition: String? = null,
)

data class RouteRecommendationDataDto(
    @SerializedName("recommendedRouteId") val recommendedRouteId: String,
    @SerializedName("safestRouteId") val safestRouteId: String,
    @SerializedName("fastestRouteId") val fastestRouteId: String,
    @SerializedName("shortestRouteId") val shortestRouteId: String,
    @SerializedName("recommendationReason") val recommendationReason: String,
    @SerializedName("routes") val routes: List<RouteCandidateDto> = emptyList(),
)

data class RouteCandidateDto(
    @SerializedName("routeId") val routeId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("distanceKm") val distanceKm: Double,
    @SerializedName("durationMinutes") val durationMinutes: Int,
    @SerializedName("riskScore") val riskScore: Int,
    @SerializedName("riskLevel") val riskLevel: String,
    @SerializedName("safetyScore") val safetyScore: Int,
    @SerializedName("traffic") val traffic: String,
    @SerializedName("hazards") val hazards: Int,
    @SerializedName("recommendationScore") val recommendationScore: Int,
    @SerializedName("reason") val reason: String,
    @SerializedName("source") val source: String = "SIMULATED",
)

// --------------------------------------------------
// EN-ROUTE REROUTING & SAFETY PROMPT MODELS (Phase 17)
// --------------------------------------------------

data class RerouteEvaluationRequest(
    @SerializedName("tripId") val tripId: String? = null,
    @SerializedName("currentRouteId") val currentRouteId: String,
    @SerializedName("origin") val origin: GeoLocationDto? = null,
    @SerializedName("destination") val destination: GeoLocationDto? = null,
    @SerializedName("currentCoordinates") val currentCoordinates: GeoLocationDto? = null,
    @SerializedName("lastPromptTimestamp") val lastPromptTimestamp: Long? = null,
    @SerializedName("lastPromptRouteId") val lastPromptRouteId: String? = null,
)

data class SafetyPromptDto(
    @SerializedName("title") val title: String,
    @SerializedName("headline") val headline: String,
    @SerializedName("message") val message: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("delayMinutes") val delayMinutes: Int = 0,
    @SerializedName("currentRisk") val currentRisk: Int,
    @SerializedName("currentRiskLevel") val currentRiskLevel: String,
    @SerializedName("currentRouteId") val currentRouteId: String,
    @SerializedName("alternativeRouteId") val alternativeRouteId: String,
    @SerializedName("alternativeRouteName") val alternativeRouteName: String,
    @SerializedName("alternativeRisk") val alternativeRisk: Int,
    @SerializedName("alternativeRiskLevel") val alternativeRiskLevel: String,
    @SerializedName("alternativeDurationMinutes") val alternativeDurationMinutes: Int,
    @SerializedName("alternativeDistanceKm") val alternativeDistanceKm: Double,
    @SerializedName("actionLabel") val actionLabel: String = "SWITCH ROUTE",
    @SerializedName("dismissLabel") val dismissLabel: String = "KEEP CURRENT ROUTE",
    @SerializedName("timestamp") val timestamp: Long = 0L,
)

data class RerouteEvaluationResponseDto(
    @SerializedName("shouldReroute") val shouldReroute: Boolean,
    @SerializedName("isPromptSuppressed") val isPromptSuppressed: Boolean = false,
    @SerializedName("currentRoute") val currentRoute: RouteCandidateDto? = null,
    @SerializedName("alternativeRoute") val alternativeRoute: RouteCandidateDto? = null,
    @SerializedName("safetyPrompt") val safetyPrompt: SafetyPromptDto? = null,
    @SerializedName("allRoutes") val allRoutes: List<RouteCandidateDto> = emptyList(),
    @SerializedName("reason") val reason: String? = null,
)

data class RouteSwitchRequest(
    @SerializedName("tripId") val tripId: String? = null,
    @SerializedName("previousRouteId") val previousRouteId: String,
    @SerializedName("newRouteId") val newRouteId: String,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("oldRisk") val oldRisk: Int? = null,
    @SerializedName("newRisk") val newRisk: Int? = null,
    @SerializedName("oldEtaMinutes") val oldEtaMinutes: Int? = null,
    @SerializedName("newEtaMinutes") val newEtaMinutes: Int? = null,
)

data class RouteSwitchResponseDto(
    @SerializedName("selectedRouteId") val selectedRouteId: String,
    @SerializedName("switchedAt") val switchedAt: String? = null,
)
