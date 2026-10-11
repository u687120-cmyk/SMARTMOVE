package com.smartmove.ai.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.components.SmartMoveOutlinedButton
import com.smartmove.ai.ui.theme.CardShape
import com.smartmove.ai.ui.theme.IndicatorPillShape
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import com.smartmove.ai.ui.theme.SmartMoveWarning
import kotlinx.coroutines.launch

import com.smartmove.ai.data.location.LocationProvider

/**
 * SMARTMOVE AI Active Trip & Smart Map Screen (Phase 8, 13 & 14).
 *
 * Provides real-time journey telemetry, dynamic road-risk updates,
 * simulated map visualization, geo-located weather conditions, and end trip flow.
 */
@Composable
fun TripScreen(
    onBackClick: () -> Unit,
    onEndTripConfirm: () -> Unit,
    onNotificationsClick: () -> Unit,
    onReportHazardClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    initialState: TripUiState = TripUiState(),
) {
    var state by remember { mutableStateOf(initialState) }
    var showEndTripDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val riskRepo = remember { com.smartmove.ai.data.repository.RiskRepository(context) }
    val routeRepo = remember { com.smartmove.ai.data.repository.RouteRepository(context) }
    val coords = remember { LocationProvider.getLastKnownCoordinates(context) }
    var weatherIcon by remember { mutableStateOf("☀️") }
    var routeRecommendation by remember { mutableStateOf<com.smartmove.ai.data.repository.RouteRecommendationResult?>(null) }
    var selectedRouteId by remember { mutableStateOf("route-b") }

    // Phase 17 & 18: Dynamic En-Route Rerouting & Trip Telemetry State
    var activeSafetyPrompt by remember { mutableStateOf<com.smartmove.ai.data.repository.SafetyPromptModel?>(null) }
    var lastPromptTimestamp by remember { mutableStateOf<Long?>(null) }
    var lastPromptRouteId by remember { mutableStateOf<String?>(null) }
    var reroutesCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val liveRisk = riskRepo.getCurrentRisk(latitude = coords.first, longitude = coords.second)
        liveRisk.weatherModel?.let {
            weatherIcon = it.conditionIcon
        }
        val isHeavyTraffic = liveRisk.trafficModel?.isHeavy == true
        val updatedRoadAhead = if (isHeavyTraffic && state.roadAhead.none { it.title.contains("Traffic", ignoreCase = true) }) {
            listOf(
                RoadAheadItem(
                    id = "traffic-alert-01",
                    icon = "🚗",
                    title = "Heavy Traffic",
                    distanceDisplay = "700 m",
                    severity = RiskLevel.HIGH,
                )
            ) + state.roadAhead
        } else {
            state.roadAhead
        }

        val routes = routeRepo.getRecommendedRoutes(
            originLat = coords.first,
            originLng = coords.second,
        )
        routeRecommendation = routes
        selectedRouteId = routes.recommendedRouteId

        state = state.copy(
            riskScore = liveRisk.score,
            riskLevel = liveRisk.level,
            riskRecommendation = liveRisk.recommendation,
            weatherStatus = liveRisk.weatherModel?.let { "${it.conditionDisplay} ${it.temperatureC.toInt()}°C" } ?: state.weatherStatus,
            trafficStatus = liveRisk.trafficModel?.congestionDisplay ?: state.trafficStatus,
            currentSpeedKmh = liveRisk.trafficModel?.currentSpeedKmh?.toInt() ?: state.currentSpeedKmh,
            roadAhead = updatedRoadAhead,
        )
    }

    // Phase 17: Periodic en-route monitoring loop during active trip (45s cycle)
    LaunchedEffect(state.isTripActive, selectedRouteId) {
        if (!state.isTripActive) return@LaunchedEffect
        while (state.isTripActive) {
            kotlinx.coroutines.delay(45_000L)
            val rerouteResult = routeRepo.evaluateEnRoute(
                currentRouteId = selectedRouteId,
                currentLat = coords.first,
                currentLng = coords.second,
                lastPromptTimestamp = lastPromptTimestamp,
                lastPromptRouteId = lastPromptRouteId,
            )
            if (rerouteResult.shouldReroute && rerouteResult.safetyPrompt != null) {
                activeSafetyPrompt = rerouteResult.safetyPrompt
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = SmartMoveTheme.spacing.default),
        ) {
            // --------------------------------------------------
            // 1. TOP BAR
            // --------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SmartMoveTheme.spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        if (state.isTripActive) {
                            showExitConfirmDialog = true
                        } else {
                            onBackClick()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = SmartMoveTextPrimary,
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SMARTMOVE AI",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = SmartMovePrimary,
                    )
                    Text(
                        text = "ACTIVE TRIP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SmartMoveTextMuted,
                    )
                }

                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Active Notifications",
                        tint = SmartMoveTextPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            // --------------------------------------------------
            // 2. INTERACTIVE GOOGLE MAP (Real Tiles & Multi-Route)
            // --------------------------------------------------
            com.smartmove.ai.ui.map.SmartMoveGoogleMap(
                originLat = state.originLat,
                originLng = state.originLng,
                destLat = state.destLat,
                destLng = state.destLng,
                originTitle = state.origin,
                destTitle = state.destination,
                routes = routeRecommendation?.routes ?: emptyList(),
                selectedRouteId = selectedRouteId,
                onRouteSelected = { newRouteId ->
                    selectedRouteId = newRouteId
                    val chosen = routeRecommendation?.routes?.find { it.routeId == newRouteId }
                    if (chosen != null) {
                        state = state.copy(
                            currentRoad = chosen.name,
                            remainingDistanceKm = chosen.distanceKm,
                            etaMinutes = chosen.durationMinutes,
                            riskScore = chosen.riskScore,
                            riskLevel = chosen.riskLevel,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // --------------------------------------------------
            // 3. CURRENT ROAD & RISK (Hero Section)
            // --------------------------------------------------
            SmartMoveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = SmartMoveTheme.spacing.default,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT ROAD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = SmartMoveTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.currentRoad,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = SmartMoveTextPrimary,
                        )
                    }

                    // Dynamic Risk Pill
                    Row(
                        modifier = Modifier
                            .clip(IndicatorPillShape)
                            .background(state.riskLevel.containerColor.copy(alpha = 0.45f))
                            .border(1.dp, state.riskLevel.color.copy(alpha = 0.6f), IndicatorPillShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(state.riskLevel.color),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.riskLevel.label} ${state.riskScore}/100",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = state.riskLevel.color,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

                // Recommendation
                Text(
                    text = state.riskRecommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartMoveTextSecondary,
                )
            }

            // High-Risk Road Alert Banner
            if (state.riskLevel == RiskLevel.HIGH) {
                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
                SmartMoveCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = SmartMoveDanger.copy(alpha = 0.15f),
                    borderColor = SmartMoveDanger,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "🔴", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.small))
                        Column {
                            Text(
                                text = "HIGH RISK CONDITIONS AHEAD",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = SmartMoveDanger,
                                ),
                            )
                            Text(
                                text = state.riskRecommendation,
                                style = MaterialTheme.typography.bodySmall,
                                color = SmartMoveTextPrimary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // --------------------------------------------------
            // 4. TRIP METRICS (Speed, ETA, Remaining Distance)
            // --------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.small),
            ) {
                MetricTile(
                    label = "SPEED",
                    value = "${state.currentSpeedKmh}",
                    unit = "km/h",
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "ETA",
                    value = "${state.etaMinutes}",
                    unit = "min",
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = "REMAINING",
                    value = "${state.remainingDistanceKm}",
                    unit = "km",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // --------------------------------------------------
            // 5. CURRENT CONDITIONS
            // --------------------------------------------------
            SmartMoveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = SmartMoveTheme.spacing.default,
            ) {
                Text(
                    text = "CURRENT CONDITIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = SmartMoveTextSecondary,
                )
                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ConditionItem(icon = "🚗", label = "Traffic", status = state.trafficStatus)
                    ConditionItem(icon = weatherIcon, label = "Weather", status = state.weatherStatus)
                    ConditionItem(icon = "⚠️", label = "Hazards", status = "${state.nearbyHazardsCount} nearby")
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // --------------------------------------------------
            // 5.2 WHO IS TRAVELING (Trip Participants)
            // --------------------------------------------------
            if (state.participants.isNotEmpty()) {
                SmartMoveCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = SmartMoveTheme.spacing.default,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "WHO IS TRAVELING?",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                            color = SmartMoveTextSecondary,
                        )
                        Text(
                            text = "${state.participants.size} Active Traveler${if (state.participants.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = SmartMovePrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.participants.forEach { person ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (person.role == com.smartmove.ai.data.model.ParticipantRole.DRIVER) SmartMovePrimary.copy(alpha = 0.15f) else SmartMoveSurfaceVariant)
                                    .border(1.dp, if (person.role == com.smartmove.ai.data.model.ParticipantRole.DRIVER) SmartMovePrimary else SmartMoveBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (person.role == com.smartmove.ai.data.model.ParticipantRole.DRIVER) "🚗" else "👤",
                                        fontSize = 12.sp,
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${person.name} (${if (person.role == com.smartmove.ai.data.model.ParticipantRole.DRIVER) "Driver" else "Passenger"})",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (person.role == com.smartmove.ai.data.model.ParticipantRole.DRIVER) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = SmartMoveTextPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

            // --------------------------------------------------
            // 5.5 ROUTE OPTIONS (Phase 16)
            // --------------------------------------------------
            SectionTitle(
                title = "ROUTE OPTIONS",
                subtitle = "Multi-route safety comparison & recommendation",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            routeRecommendation?.routes?.forEach { route ->
                val isSelected = route.routeId == selectedRouteId
                val isRecommended = route.routeId == routeRecommendation?.recommendedRouteId
                val isSafest = route.routeId == routeRecommendation?.safestRouteId
                val isFastest = route.routeId == routeRecommendation?.fastestRouteId

                SmartMoveCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    borderColor = if (isSelected) SmartMovePrimary else SmartMoveBorder,
                    containerColor = if (isSelected) SmartMovePrimary.copy(alpha = 0.08f) else SmartMoveSurface,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (isRecommended) {
                                    Box(
                                        modifier = Modifier
                                            .clip(IndicatorPillShape)
                                            .background(SmartMovePrimary.copy(alpha = 0.15f))
                                            .border(1.dp, SmartMovePrimary, IndicatorPillShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "🟢 SMARTMOVE RECOMMENDS",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = SmartMovePrimary,
                                        )
                                    }
                                } else if (isSafest) {
                                    Box(
                                        modifier = Modifier
                                            .clip(IndicatorPillShape)
                                            .background(SmartMovePrimary.copy(alpha = 0.15f))
                                            .border(1.dp, SmartMovePrimary, IndicatorPillShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "🛡️ SAFEST",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = SmartMovePrimary,
                                        )
                                    }
                                }
                                if (isFastest && !isRecommended) {
                                    Box(
                                        modifier = Modifier
                                            .clip(IndicatorPillShape)
                                            .background(Color(0xFFEAB308).copy(alpha = 0.15f))
                                            .border(1.dp, Color(0xFFEAB308), IndicatorPillShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "⚡ FASTEST",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                            ),
                                            color = Color(0xFFEAB308),
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .clip(IndicatorPillShape)
                                    .background(route.riskLevel.containerColor.copy(alpha = 0.45f))
                                    .border(1.dp, route.riskLevel.color.copy(alpha = 0.6f), IndicatorPillShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(route.riskLevel.color),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${route.riskLevel.label} ${route.riskScore}/100",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                    ),
                                    color = route.riskLevel.color,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = route.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SmartMoveTextPrimary,
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${route.distanceKm} km • ${route.durationMinutes} min",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SmartMoveTextSecondary,
                            )
                            Text(
                                text = "Safety: ${route.safetyScore}/100",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (route.safetyScore >= 70) SmartMovePrimary else SmartMoveTextSecondary,
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = route.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = SmartMoveTextMuted,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "🚗 Traffic: ${route.traffic} • ⚠️ ${route.hazards} Hazards",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = SmartMoveTextSecondary,
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .clip(IndicatorPillShape)
                                        .background(SmartMovePrimary.copy(alpha = 0.2f))
                                        .border(1.dp, SmartMovePrimary, IndicatorPillShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = "✓ ROUTE SELECTED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                        ),
                                        color = SmartMovePrimary,
                                    )
                                }
                            } else {
                                SmartMoveButton(
                                    text = "SELECT ROUTE",
                                    onClick = {
                                        selectedRouteId = route.routeId
                                        state = state.copy(
                                            remainingDistanceKm = route.distanceKm,
                                            etaMinutes = route.durationMinutes,
                                            riskScore = route.riskScore,
                                            riskLevel = route.riskLevel,
                                            trafficStatus = route.traffic.lowercase().replaceFirstChar { it.uppercase() },
                                            nearbyHazardsCount = route.hazards,
                                        )
                                    },
                                    fullWidth = false,
                                    modifier = Modifier.height(36.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

            // --------------------------------------------------
            // 6. ROAD AHEAD
            // --------------------------------------------------
            SectionTitle(
                title = "ROAD AHEAD",
                subtitle = "Upcoming checkpoints and detected road events",
                action = {
                    Text(
                        text = "+ REPORT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmartMovePrimary,
                        ),
                        modifier = Modifier
                            .clickable(onClick = onReportHazardClick)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                },
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                state.roadAhead.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = item.icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = SmartMoveTextPrimary,
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.distanceDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SmartMoveTextSecondary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(item.severity.color),
                            )
                        }
                    }

                    if (index < state.roadAhead.size - 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(1.dp)
                                .background(SmartMoveBorder),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

            // --------------------------------------------------
            // 7. SIMULATE RISK CHANGE (Interactive Demo Control)
            // --------------------------------------------------
            SmartMoveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = SmartMoveTheme.spacing.small,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DEMO: SIMULATE ROAD CHANGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                            color = SmartMoveTextSecondary,
                        )
                        Text(
                            text = "Observe dynamic risk update",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SmartMoveTextMuted,
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SmartMoveOutlinedButton(
                            text = "CHANGE RISK",
                            onClick = {
                                state = when (state.riskLevel) {
                                    RiskLevel.LOW -> state.copy(
                                        riskScore = 48,
                                        riskLevel = RiskLevel.MEDIUM,
                                        riskRecommendation = "Drive carefully. Conditions ahead may increase risk.",
                                        currentSpeedKmh = 34,
                                        trafficStatus = "Moderate",
                                        weatherStatus = "Overcast",
                                    )
                                    RiskLevel.MEDIUM -> state.copy(
                                        riskScore = 72,
                                        riskLevel = RiskLevel.HIGH,
                                        riskRecommendation = "High-risk conditions detected ahead. Consider a safer route.",
                                        currentSpeedKmh = 20,
                                        trafficStatus = "Heavy",
                                        weatherStatus = "Rain",
                                    )
                                    RiskLevel.HIGH -> state.copy(
                                        riskScore = 20,
                                        riskLevel = RiskLevel.LOW,
                                        riskRecommendation = "Road conditions look safe right now.",
                                        currentSpeedKmh = 45,
                                        trafficStatus = "Low",
                                        weatherStatus = "Clear",
                                    )
                                }
                            },
                        )

                        SmartMoveButton(
                            text = "SIMULATE REROUTE",
                            onClick = {
                                val demoAlternative = if (selectedRouteId == "route-a") "route-b" else "route-c"
                                val demoName = if (selectedRouteId == "route-a") "Via SB Road Bypass" else "Via Outer University Link"
                                activeSafetyPrompt = com.smartmove.ai.data.repository.SafetyPromptModel(
                                    title = "SAFETY UPDATE",
                                    headline = "Heavy Traffic Reported Ahead",
                                    message = "Severe congestion 800m ahead (+8 min delay). Current route risk: HIGH (74/100). A safer alternative is available via $demoName.",
                                    summary = "+3 min • Risk: 34 (MEDIUM)",
                                    delayMinutes = 3,
                                    currentRisk = 74,
                                    currentRiskLevel = RiskLevel.HIGH,
                                    currentRouteId = selectedRouteId,
                                    alternativeRouteId = demoAlternative,
                                    alternativeRouteName = demoName,
                                    alternativeRisk = 34,
                                    alternativeRiskLevel = RiskLevel.MEDIUM,
                                    alternativeDurationMinutes = 27,
                                    alternativeDistanceKm = 9.1,
                                )
                                state = state.copy(
                                    riskScore = 74,
                                    riskLevel = RiskLevel.HIGH,
                                    trafficStatus = "Heavy",
                                    currentSpeedKmh = 18,
                                )
                            },
                            fullWidth = false,
                            modifier = Modifier.height(34.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 8. END TRIP BUTTON
            // --------------------------------------------------
            SmartMoveButton(
                text = "END TRIP",
                onClick = { showEndTripDialog = true },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
        }
    }

    // --------------------------------------------------
    // DIALOG: DYNAMIC EN-ROUTE SAFETY PROMPT (Phase 17)
    // --------------------------------------------------
    if (activeSafetyPrompt != null) {
        val prompt = activeSafetyPrompt!!
        AlertDialog(
            onDismissRequest = {
                lastPromptTimestamp = System.currentTimeMillis()
                lastPromptRouteId = prompt.alternativeRouteId
                activeSafetyPrompt = null
            },
            containerColor = SmartMoveSurface,
            shape = CardShape,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚠️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = prompt.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = SmartMoveWarning,
                        ),
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = prompt.headline,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = prompt.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(SmartMoveBackground)
                            .border(1.dp, SmartMoveBorder, CardShape)
                            .padding(10.dp),
                    ) {
                        Column {
                            Text(
                                text = "Alternative: ${prompt.alternativeRouteName}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SmartMovePrimary,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = prompt.summary,
                                style = MaterialTheme.typography.labelSmall,
                                color = SmartMoveTextSecondary,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                SmartMoveButton(
                    text = prompt.actionLabel,
                    onClick = {
                        coroutineScope.launch {
                            routeRepo.switchRoute(
                                tripId = state.tripId,
                                previousRouteId = prompt.currentRouteId,
                                newRouteId = prompt.alternativeRouteId,
                                reason = "Driver accepted safer route prompt",
                                oldRisk = prompt.currentRisk,
                                newRisk = prompt.alternativeRisk,
                            )
                        }
                        selectedRouteId = prompt.alternativeRouteId
                        reroutesCount++
                        state = state.copy(
                            riskScore = prompt.alternativeRisk,
                            riskLevel = prompt.alternativeRiskLevel,
                            etaMinutes = prompt.alternativeDurationMinutes,
                            remainingDistanceKm = prompt.alternativeDistanceKm,
                            trafficStatus = "Low",
                            currentSpeedKmh = 45,
                        )
                        activeSafetyPrompt = null
                    },
                    fullWidth = false,
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        lastPromptTimestamp = System.currentTimeMillis()
                        lastPromptRouteId = prompt.alternativeRouteId
                        activeSafetyPrompt = null
                    },
                ) {
                    Text(
                        text = prompt.dismissLabel,
                        color = SmartMoveTextSecondary,
                    )
                }
            },
        )
    }

    // --------------------------------------------------
    // DIALOG: END TRIP CONFIRMATION
    // --------------------------------------------------
    if (showEndTripDialog) {
        AlertDialog(
            onDismissRequest = { showEndTripDialog = false },
            containerColor = SmartMoveSurface,
            shape = CardShape,
            title = {
                Text(
                    text = "End this trip?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SmartMoveTextPrimary,
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to conclude your active journey? A summary of your safety metrics will be displayed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartMoveTextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEndTripDialog = false
                        val finalSafetyScore = (100 - state.riskScore).coerceIn(0, 100)
                        val selectedRouteName = routeRecommendation?.routes?.find { it.routeId == selectedRouteId }?.name ?: "Recommended Route"
                        val suggestion = when {
                            state.riskScore >= 60 -> "Your recent trips show increased safety events. Consider reducing speed in high-risk areas."
                            finalSafetyScore >= 90 -> "Your recent trips show improved safety performance."
                            else -> "Smooth driving throughout this corridor."
                        }

                        val summary = com.smartmove.ai.data.repository.CompletedTripSummary(
                            origin = state.origin,
                            destination = state.destination,
                            routeName = selectedRouteName,
                            distanceKm = state.remainingDistanceKm.coerceAtLeast(1.0),
                            durationMinutes = state.etaMinutes.coerceAtLeast(5),
                            averageRisk = state.riskScore,
                            riskLevel = state.riskLevel,
                            safetyScore = finalSafetyScore,
                            warningsCount = state.roadAhead.size,
                            hazardsCount = state.nearbyHazardsCount,
                            hardBrakingCount = 0,
                            reroutesCount = reroutesCount,
                            suggestion = suggestion,
                        )
                        com.smartmove.ai.data.repository.TripRepository.setLastCompletedTrip(summary)

                        coroutineScope.launch {
                            try {
                                val tripRepo = com.smartmove.ai.data.repository.TripRepository(context)
                                tripRepo.createTrip(
                                    com.smartmove.ai.data.model.CreateTripRequest(
                                        startLocation = summary.origin,
                                        destinationLocation = summary.destination,
                                        distanceKm = summary.distanceKm.toFloat(),
                                        durationMinutes = summary.durationMinutes,
                                        averageRisk = summary.averageRisk,
                                        safetyScore = summary.safetyScore,
                                        warningCount = summary.warningsCount,
                                        hazardCount = summary.hazardsCount,
                                        hardBrakingCount = summary.hardBrakingCount,
                                        trafficCondition = state.trafficStatus,
                                        weatherCondition = state.weatherStatus,
                                    )
                                )
                            } catch (e: Exception) {
                                // Graceful offline handling
                            }
                        }

                        onEndTripConfirm()
                    }
                ) {
                    Text("END TRIP", color = SmartMoveDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTripDialog = false }) {
                    Text("CANCEL", color = SmartMoveTextSecondary)
                }
            },
        )
    }

    // --------------------------------------------------
    // DIALOG: EXIT ACTIVE TRIP BACK CONFIRMATION
    // --------------------------------------------------
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            containerColor = SmartMoveSurface,
            shape = CardShape,
            title = {
                Text(
                    text = "Leave Active Navigation?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = SmartMoveTextPrimary,
                )
            },
            text = {
                Text(
                    text = "Your trip is currently active. Leaving will return to the dashboard while your journey continues in the background.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartMoveTextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        onBackClick()
                    }
                ) {
                    Text("LEAVE", color = SmartMovePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("KEEP ACTIVE", color = SmartMoveTextSecondary)
                }
            },
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier,
        contentPadding = SmartMoveTheme.spacing.small,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = SmartMoveTextSecondary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = SmartMoveTextPrimary,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = SmartMoveTextMuted,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ConditionItem(
    icon: String,
    label: String,
    status: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = SmartMoveTextMuted,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = SmartMoveTextPrimary,
            )
        }
    }
}
