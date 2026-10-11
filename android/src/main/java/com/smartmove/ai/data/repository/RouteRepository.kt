package com.smartmove.ai.data.repository

import android.content.Context
import com.smartmove.ai.data.api.ApiClient
import com.smartmove.ai.data.api.RouteApi
import com.smartmove.ai.data.model.GeoLocationDto
import com.smartmove.ai.data.model.RouteCandidateDto
import com.smartmove.ai.data.model.RouteRecommendationRequest
import com.smartmove.ai.ui.components.RiskLevel

/**
 * Domain model representing a candidate route option (Phase 16).
 */
data class RouteCandidate(
    val routeId: String,
    val name: String,
    val description: String?,
    val distanceKm: Double,
    val durationMinutes: Int,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val safetyScore: Int,
    val traffic: String,
    val hazards: Int,
    val recommendationScore: Int,
    val reason: String,
    val source: String,
)

/**
 * Domain model representing route comparison results.
 */
data class RouteRecommendationResult(
    val recommendedRouteId: String,
    val safestRouteId: String,
    val fastestRouteId: String,
    val shortestRouteId: String,
    val recommendationReason: String,
    val routes: List<RouteCandidate>,
)

/**
 * Domain model representing a dynamic en-route safety prompt (Phase 17).
 */
data class SafetyPromptModel(
    val title: String,
    val headline: String,
    val message: String,
    val summary: String,
    val delayMinutes: Int,
    val currentRisk: Int,
    val currentRiskLevel: RiskLevel,
    val currentRouteId: String,
    val alternativeRouteId: String,
    val alternativeRouteName: String,
    val alternativeRisk: Int,
    val alternativeRiskLevel: RiskLevel,
    val alternativeDurationMinutes: Int,
    val alternativeDistanceKm: Double,
    val actionLabel: String = "SWITCH ROUTE",
    val dismissLabel: String = "KEEP CURRENT ROUTE",
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Domain model representing result of periodic en-route trip monitoring (Phase 17).
 */
data class EnRouteRerouteResult(
    val shouldReroute: Boolean,
    val isPromptSuppressed: Boolean,
    val currentRoute: RouteCandidate?,
    val alternativeRoute: RouteCandidate?,
    val safetyPrompt: SafetyPromptModel?,
    val allRoutes: List<RouteCandidate>,
    val reason: String?,
)

/**
 * Repository responsible for multi-route comparison and safer route recommendations (Phase 16).
 */
class RouteRepository(
    context: Context,
    private val routeApi: RouteApi = ApiClient.getRouteApi(context),
) {

    /**
     * Fetches candidate routes and recommendations for specified coordinates.
     * Gracefully falls back to structured offline candidate routes if backend is unreachable.
     */
    suspend fun getRecommendedRoutes(
        originLat: Double = 18.5204,
        originLng: Double = 73.8567,
        destLat: Double = 18.5314,
        destLng: Double = 73.8446,
    ): RouteRecommendationResult {
        return try {
            val response = routeApi.recommendRoutes(
                RouteRecommendationRequest(
                    origin = GeoLocationDto(latitude = originLat, longitude = originLng),
                    destination = GeoLocationDto(latitude = destLat, longitude = destLng),
                )
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                RouteRecommendationResult(
                    recommendedRouteId = data.recommendedRouteId,
                    safestRouteId = data.safestRouteId,
                    fastestRouteId = data.fastestRouteId,
                    shortestRouteId = data.shortestRouteId,
                    recommendationReason = data.recommendationReason,
                    routes = data.routes.map { mapDtoToDomain(it) },
                )
            } else {
                getFallbackRoutes()
            }
        } catch (_: Exception) {
            getFallbackRoutes()
        }
    }

    private fun mapDtoToDomain(dto: RouteCandidateDto): RouteCandidate {
        val level = when (dto.riskLevel.uppercase()) {
            "LOW" -> RiskLevel.LOW
            "HIGH" -> RiskLevel.HIGH
            else -> RiskLevel.MEDIUM
        }
        return RouteCandidate(
            routeId = dto.routeId,
            name = dto.name,
            description = dto.description,
            distanceKm = dto.distanceKm,
            durationMinutes = dto.durationMinutes,
            riskScore = dto.riskScore,
            riskLevel = level,
            safetyScore = dto.safetyScore,
            traffic = dto.traffic,
            hazards = dto.hazards,
            recommendationScore = dto.recommendationScore,
            reason = dto.reason,
            source = dto.source,
        )
    }

    private fun getFallbackRoutes(): RouteRecommendationResult {
        val routes = listOf(
            RouteCandidate(
                routeId = "route-a",
                name = "Via FC Road Arterial",
                description = "Central urban corridor with high commercial activity",
                distanceKm = 8.2,
                durationMinutes = 24,
                riskScore = 72,
                riskLevel = RiskLevel.HIGH,
                safetyScore = 28,
                traffic = "HIGH",
                hazards = 3,
                recommendationScore = 65,
                reason = "Higher current risk due to active road hazards and traffic congestion.",
                source = "SIMULATED",
            ),
            RouteCandidate(
                routeId = "route-b",
                name = "Via SB Road Bypass",
                description = "Intermediate arterial bypass avoiding core city congestion",
                distanceKm = 9.1,
                durationMinutes = 27,
                riskScore = 34,
                riskLevel = RiskLevel.MEDIUM,
                safetyScore = 66,
                traffic = "LOW",
                hazards = 1,
                recommendationScore = 84,
                reason = "Recommended because it avoids high-risk hazards with only 3 extra minutes.",
                source = "SIMULATED",
            ),
            RouteCandidate(
                routeId = "route-c",
                name = "Via Outer University Link",
                description = "Perimeter ring road with free-flow traffic",
                distanceKm = 10.5,
                durationMinutes = 29,
                riskScore = 21,
                riskLevel = RiskLevel.LOW,
                safetyScore = 79,
                traffic = "LOW",
                hazards = 0,
                recommendationScore = 80,
                reason = "Lowest current risk but requires extra travel time.",
                source = "SIMULATED",
            ),
        )

        return RouteRecommendationResult(
            recommendedRouteId = "route-b",
            safestRouteId = "route-c",
            fastestRouteId = "route-a",
            shortestRouteId = "route-a",
            recommendationReason = "Recommended because it avoids high-risk hazards with only 3 extra minutes.",
            routes = routes,
        )
    }

    /**
     * Periodically evaluates active trip corridor for en-route deterioration (Phase 17).
     * Compares the current route against alternatives and generates a driver-friendly safety prompt if warranted.
     */
    suspend fun evaluateEnRoute(
        tripId: String? = null,
        currentRouteId: String,
        currentLat: Double = 18.5204,
        currentLng: Double = 73.8567,
        lastPromptTimestamp: Long? = null,
        lastPromptRouteId: String? = null,
    ): EnRouteRerouteResult {
        return try {
            val response = routeApi.recalculateEnRoute(
                com.smartmove.ai.data.model.RerouteEvaluationRequest(
                    tripId = tripId,
                    currentRouteId = currentRouteId,
                    currentCoordinates = com.smartmove.ai.data.model.GeoLocationDto(currentLat, currentLng),
                    lastPromptTimestamp = lastPromptTimestamp,
                    lastPromptRouteId = lastPromptRouteId,
                )
            )

            if (response.isSuccessful && response.body()?.data != null) {
                val data = response.body()!!.data!!
                val mappedPrompt = data.safetyPrompt?.let { p ->
                    SafetyPromptModel(
                        title = p.title,
                        headline = p.headline,
                        message = p.message,
                        summary = p.summary,
                        delayMinutes = p.delayMinutes,
                        currentRisk = p.currentRisk,
                        currentRiskLevel = mapRiskLevel(p.currentRiskLevel),
                        currentRouteId = p.currentRouteId,
                        alternativeRouteId = p.alternativeRouteId,
                        alternativeRouteName = p.alternativeRouteName,
                        alternativeRisk = p.alternativeRisk,
                        alternativeRiskLevel = mapRiskLevel(p.alternativeRiskLevel),
                        alternativeDurationMinutes = p.alternativeDurationMinutes,
                        alternativeDistanceKm = p.alternativeDistanceKm,
                        actionLabel = p.actionLabel,
                        dismissLabel = p.dismissLabel,
                        timestamp = p.timestamp,
                    )
                }

                EnRouteRerouteResult(
                    shouldReroute = data.shouldReroute,
                    isPromptSuppressed = data.isPromptSuppressed,
                    currentRoute = data.currentRoute?.let { mapDtoToDomain(it) },
                    alternativeRoute = data.alternativeRoute?.let { mapDtoToDomain(it) },
                    safetyPrompt = mappedPrompt,
                    allRoutes = data.allRoutes.map { mapDtoToDomain(it) },
                    reason = data.reason,
                )
            } else {
                getOfflineEnRouteFallback(currentRouteId)
            }
        } catch (e: Exception) {
            getOfflineEnRouteFallback(currentRouteId)
        }
    }

    private fun mapRiskLevel(levelStr: String): RiskLevel {
        return when (levelStr.uppercase()) {
            "LOW" -> RiskLevel.LOW
            "HIGH" -> RiskLevel.HIGH
            else -> RiskLevel.MEDIUM
        }
    }

    /**
     * Confirms a route switch during an active trip and records the telemetry event (Phase 17).
     */
    suspend fun switchRoute(
        tripId: String? = null,
        previousRouteId: String,
        newRouteId: String,
        reason: String? = null,
        oldRisk: Int? = null,
        newRisk: Int? = null,
    ): Boolean {
        return try {
            val response = routeApi.switchRoute(
                com.smartmove.ai.data.model.RouteSwitchRequest(
                    tripId = tripId,
                    previousRouteId = previousRouteId,
                    newRouteId = newRouteId,
                    reason = reason,
                    oldRisk = oldRisk,
                    newRisk = newRisk,
                )
            )
            response.isSuccessful
        } catch (e: Exception) {
            true // Graceful offline continuation
        }
    }

    private fun getOfflineEnRouteFallback(currentRouteId: String): EnRouteRerouteResult {
        val offlineRoutes = getFallbackRoutes().routes
        val current = offlineRoutes.find { it.routeId == currentRouteId } ?: offlineRoutes[0]
        val alternative = offlineRoutes.find { it.routeId != currentRouteId && it.routeId == "route-b" } ?: offlineRoutes.last()

        val shouldReroute = current.routeId == "route-a" && current.riskScore >= 60
        val safetyPrompt = if (shouldReroute) {
            SafetyPromptModel(
                title = "SAFETY UPDATE",
                headline = "Congestion & Hazards Detected Ahead",
                message = "Current route risk is HIGH (68/100). A safer alternative is available via ${alternative.name}.",
                summary = "+3 min • Risk: ${alternative.riskScore} (${alternative.riskLevel})",
                delayMinutes = 3,
                currentRisk = current.riskScore,
                currentRiskLevel = current.riskLevel,
                currentRouteId = current.routeId,
                alternativeRouteId = alternative.routeId,
                alternativeRouteName = alternative.name,
                alternativeRisk = alternative.riskScore,
                alternativeRiskLevel = alternative.riskLevel,
                alternativeDurationMinutes = alternative.durationMinutes,
                alternativeDistanceKm = alternative.distanceKm,
            )
        } else {
            null
        }

        return EnRouteRerouteResult(
            shouldReroute = shouldReroute,
            isPromptSuppressed = false,
            currentRoute = current,
            alternativeRoute = alternative,
            safetyPrompt = safetyPrompt,
            allRoutes = offlineRoutes,
            reason = if (shouldReroute) "Safer alternative available." else "Current route remains safe.",
        )
    }
}
