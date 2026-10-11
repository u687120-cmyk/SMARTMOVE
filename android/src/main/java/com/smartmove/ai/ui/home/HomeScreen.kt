package com.smartmove.ai.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.data.location.LocationProvider
import com.smartmove.ai.data.repository.TrafficModel
import com.smartmove.ai.data.repository.TrafficRepository
import com.smartmove.ai.data.repository.WeatherModel
import com.smartmove.ai.data.repository.WeatherRepository
import com.smartmove.ai.ui.alerts.HazardRepository
import com.smartmove.ai.ui.components.ActiveAlertCard
import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.components.RiskStatusCard
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.components.SmartMoveOutlinedButton
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * REAL SMARTMOVE AI Home Dashboard (Phase 4, 13, 14 & 15).
 *
 * Fully integrated with dynamic road risk engine, real-time geo-weather,
 * dynamic traffic flow, and context-sensitive road alerts.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: HomeSafetyUiState = HomeSafetyUiState(),
    onStartTripClick: () -> Unit = {},
    onViewAnalyticsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onReportHazardClick: () -> Unit = {},
    onOpenWebMapClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val userRepo = remember { com.smartmove.ai.data.repository.UserRepository(context) }
    val riskRepo = remember { com.smartmove.ai.data.repository.RiskRepository(context) }
    val weatherRepo = remember { WeatherRepository(context) }
    val trafficRepo = remember { TrafficRepository(context) }
    var currentUserName by remember { mutableStateOf(uiState.userName) }
    var locationDeniedMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            locationDeniedMessage = "Location permission is needed for local weather and traffic."
        }
    }

    var liveRisk by remember {
        mutableStateOf(
            com.smartmove.ai.data.repository.LiveRoadRisk(
                score = uiState.riskScore,
                level = uiState.currentRiskLevel,
                recommendation = uiState.riskRecommendation,
                hazardFactor = 20,
                trafficFactor = 30,
                weatherFactor = 10,
                brakingFactor = 15,
                eventFactor = 10,
                reasons = uiState.riskFactors,
            )
        )
    }

    var liveWeather by remember {
        mutableStateOf<WeatherModel?>(null)
    }

    var liveTraffic by remember {
        mutableStateOf<TrafficModel?>(null)
    }

    LaunchedEffect(Unit) {
        if (!LocationProvider.hasLocationPermission(context)) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }

        HazardRepository.syncWithBackend(context)
        val pRes = userRepo.getProfile()
        if (pRes.isSuccess) {
            currentUserName = pRes.getOrNull()!!.fullName
        }

        val coords = LocationProvider.getLastKnownCoordinates(context)
        val weatherData = weatherRepo.getCurrentWeather(coords.first, coords.second)
        liveWeather = weatherData

        val trafficData = trafficRepo.getCurrentTraffic(coords.first, coords.second)
        liveTraffic = trafficData

        val riskData = riskRepo.getCurrentRisk(latitude = coords.first, longitude = coords.second)
        liveRisk = riskData
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .verticalScroll(scrollState)
            .statusBarsPadding()
            .padding(horizontal = SmartMoveTheme.spacing.default, vertical = SmartMoveTheme.spacing.medium),
    ) {
        // --------------------------------------------------
        // 1. TOP HEADER
        // --------------------------------------------------
        HomeTopHeader(
            userName = currentUserName,
            greeting = uiState.greeting,
            hasUnreadNotifications = uiState.hasUnreadNotifications,
            onNotificationsClick = onNotificationsClick,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

        // --------------------------------------------------
        // 2. CURRENT SAFETY (Hero Risk Card - Dynamic Risk Engine)
        // --------------------------------------------------
        RiskStatusCard(
            riskLevel = liveRisk.level,
            score = liveRisk.score,
            description = liveRisk.recommendation,
            riskFactors = liveRisk.reasons,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

        // --------------------------------------------------
        // 3. TODAY'S OVERVIEW
        // --------------------------------------------------
        SectionTitle(
            title = "TODAY'S OVERVIEW",
            subtitle = "Daily driving intelligence & risk metrics",
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // Two Primary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.medium),
        ) {
            PrimaryOverviewCard(
                label = "TOTAL DISTANCE",
                value = uiState.overview.totalDistance,
                modifier = Modifier.weight(1f),
            )
            PrimaryOverviewCard(
                label = "AVERAGE RISK",
                value = uiState.overview.averageRisk,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        // Smaller Secondary Statistics Row
        SecondaryStatsCard(
            trips = uiState.overview.tripsCount,
            warnings = uiState.overview.warningsCount,
            safetyScore = uiState.overview.safetyScore,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

        // --------------------------------------------------
        // 4. CURRENT CONDITIONS
        // --------------------------------------------------
        SectionTitle(
            title = "CURRENT CONDITIONS",
            subtitle = "Live road environment estimates",
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        val currentTraffic = liveRisk.trafficModel ?: liveTraffic
        val dynamicConditions = uiState.conditions.copy(
            trafficLabel = currentTraffic?.congestionDisplay ?: uiState.conditions.trafficLabel,
            weatherEmoji = liveWeather?.conditionIcon ?: uiState.conditions.weatherEmoji,
            weatherLabel = liveWeather?.let { "${it.conditionDisplay} ${it.temperatureC.toInt()}°C" } ?: uiState.conditions.weatherLabel,
        )
        CurrentConditionsCard(conditions = dynamicConditions)

        if (locationDeniedMessage != null) {
            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
            Text(
                text = locationDeniedMessage!!,
                style = MaterialTheme.typography.bodySmall,
                color = SmartMoveTextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

        // --------------------------------------------------
        // 5. START TRIP & FIND SAFER ROUTE
        // --------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.small),
        ) {
            SmartMoveButton(
                text = "🗺️ GOOGLE MAP",
                onClick = onOpenWebMapClick,
                modifier = Modifier.weight(1.1f),
            )
            SmartMoveOutlinedButton(
                text = "⚠️ REPORT HAZARD",
                onClick = onReportHazardClick,
                modifier = Modifier.weight(0.9f),
            )
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.small),
        ) {
            SmartMoveButton(
                text = "PLAN ROUTE & TRAVELERS",
                onClick = onStartTripClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

        // --------------------------------------------------
        // 6. ACTIVE ALERT
        // --------------------------------------------------
        SectionTitle(
            title = "ROAD ALERTS",
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        val activeHazards = remember { HazardRepository.getActiveHazards() }
        val topHazard = activeHazards.firstOrNull()
        val dynamicAlert = if (topHazard != null) {
            ActiveAlertData(
                title = if (activeHazards.size > 1) "${activeHazards.size} ACTIVE ALERTS • ${topHazard.title.uppercase()}" else topHazard.title.uppercase(),
                message = "${topHazard.locationName} • ${topHazard.distanceDisplay}",
                severity = when (topHazard.severity) {
                    RiskLevel.HIGH -> AlertSeverity.DANGER
                    RiskLevel.MEDIUM -> AlertSeverity.WARNING
                    RiskLevel.LOW -> AlertSeverity.INFO
                },
                distance = topHazard.distanceDisplay,
            )
        } else {
            null
        }

        val heavyTrafficAlert = currentTraffic?.takeIf { it.isHeavy }?.let {
            ActiveAlertData(
                title = "TRAFFIC ALERT • HEAVY TRAFFIC",
                message = "Traffic speed is significantly below normal (${it.speedDisplay} / ${it.freeFlowDisplay}). Delays expected.",
                severity = AlertSeverity.WARNING,
                distance = "Current Corridor",
            )
        }

        val severeWeather = liveWeather?.takeIf { it.isSevere }
        val weatherAlert = severeWeather?.let {
            ActiveAlertData(
                title = "WEATHER ALERT • ${it.conditionDisplay.uppercase()}",
                message = "Reduced visibility (${it.visibilityKm} km) and wet road conditions ahead. Drive cautiously.",
                severity = AlertSeverity.WARNING,
                distance = "Current Area",
            )
        }
        val displayAlert = heavyTrafficAlert ?: weatherAlert ?: dynamicAlert

        ActiveAlertCard(
            alert = displayAlert,
            onClick = onNotificationsClick,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

        // --------------------------------------------------
        // 7. QUICK ANALYTICS
        // --------------------------------------------------
        QuickAnalyticsCard(
            onViewAnalyticsClick = onViewAnalyticsClick,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
    }
}

/**
 * Top Header component with greeting, user name, and notification bell.
 */
@Composable
private fun HomeTopHeader(
    userName: String,
    greeting: String,
    hasUnreadNotifications: Boolean,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "SMARTMOVE AI",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                ),
                color = SmartMovePrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$greeting, $userName",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = SmartMoveTextPrimary,
            )
        }

        // Notification Bell Icon with touch target and optional unread badge
        Box {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.size(SmartMoveTheme.spacing.touchTargetMin),
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = SmartMoveTextPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }

            if (hasUnreadNotifications) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SmartMoveSafe)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                )
            }
        }
    }
}

/**
 * Primary overview cards (Distance & Average Risk).
 */
@Composable
private fun PrimaryOverviewCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier,
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
            color = SmartMoveTextSecondary,
        )
        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
            ),
            color = SmartMoveTextPrimary,
        )
    }
}

/**
 * Secondary statistics card displaying Trips, Warnings, and Safety Score.
 */
@Composable
private fun SecondaryStatsCard(
    trips: Int,
    warnings: Int,
    safetyScore: String,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(label = "Trips", value = "$trips")
            DividerDot()
            StatItem(label = "Warnings", value = "$warnings")
            DividerDot()
            StatItem(label = "Safety Score", value = safetyScore)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SmartMoveTextSecondary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = SmartMoveTextPrimary,
        )
    }
}

@Composable
private fun DividerDot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(SmartMoveBorder),
    )
}

/**
 * Card displaying Current Conditions (Traffic, Weather, Hazards).
 */
@Composable
private fun CurrentConditionsCard(
    conditions: CurrentConditionsData,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConditionColumn(
                emoji = conditions.trafficEmoji,
                title = "Traffic",
                value = conditions.trafficLabel,
            )
            ConditionDivider()
            ConditionColumn(
                emoji = conditions.weatherEmoji,
                title = "Weather",
                value = conditions.weatherLabel,
            )
            ConditionDivider()
            ConditionColumn(
                emoji = conditions.hazardsEmoji,
                title = "Hazards",
                value = conditions.hazardsLabel,
            )
        }
    }
}

@Composable
private fun ConditionColumn(
    emoji: String,
    title: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.small))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = SmartMoveTextSecondary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = SmartMoveTextPrimary,
            )
        }
    }
}

@Composable
private fun ConditionDivider() {
    Box(
        modifier = Modifier
            .height(28.dp)
            .width(1.dp)
            .background(SmartMoveBorder),
    )
}

/**
 * Quick Analytics entry card leading to the Analytics screen.
 */
@Composable
private fun QuickAnalyticsCard(
    onViewAnalyticsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SAFETY ANALYTICS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = SmartMoveTextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "View your driving statistics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SmartMoveTextSecondary,
                )
            }

            Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.default))

            SmartMoveOutlinedButton(
                text = "VIEW",
                onClick = onViewAnalyticsClick,
                fullWidth = false,
            )
        }
    }
}
