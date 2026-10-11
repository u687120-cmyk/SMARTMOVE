package com.smartmove.ai.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.components.SmartMoveTopBar
import com.smartmove.ai.ui.theme.IndicatorPillShape
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Trip Details Screen for SMARTMOVE AI (Phase 6).
 *
 * Displays full journey metrics, road events, conditions, and the dynamic risk timeline.
 */
@Composable
fun TripDetailsScreen(
    tripId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tripRepo = remember { com.smartmove.ai.data.repository.TripRepository(context) }
    var trip by remember {
        mutableStateOf(
            TripHistoryDataProvider.getDemoTrips().find { it.id == tripId }
                ?: TripHistoryDataProvider.getDemoTrips().first()
        )
    }

    LaunchedEffect(tripId) {
        trip = tripRepo.getTripById(tripId)
    }

    val scrollState = rememberScrollState()
    val fuelCost = (trip.distanceKm * 7.5).toInt()
    val safetySuggestion = when {
        trip.hardBrakingCount > 2 -> "Try maintaining a greater following distance to reduce sudden braking."
        trip.averageRisk >= 60 -> "Your trip showed elevated road risk. Consider alternative bypass routes during peak hours."
        trip.safetyScore >= 90 -> "Your trip showed improved safety performance with optimal road safety."
        else -> "Smooth driving throughout this corridor."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground),
    ) {
        // Top Navigation Bar
        SmartMoveTopBar(
            title = "Trip Details",
            subtitle = "${trip.startLocation} → ${trip.destination}",
            onBackClick = onBackClick,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = SmartMoveTheme.spacing.default, vertical = SmartMoveTheme.spacing.medium),
        ) {
            // --------------------------------------------------
            // 1. TRIP SUMMARY & ROUTE INFORMATION
            // --------------------------------------------------
            SectionTitle(
                title = "TRIP INFORMATION",
                subtitle = "Route, time, distance, and cost overview",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow(label = "Route", value = "Recommended Route")
                DetailDivider()
                DetailRow(label = "Start", value = trip.startLocation)
                DetailDivider()
                DetailRow(label = "Destination", value = trip.destination)
                DetailDivider()
                DetailRow(label = "Date", value = trip.dateDisplay)
                DetailDivider()
                DetailRow(label = "Start Time", value = trip.startTime)
                DetailDivider()
                DetailRow(label = "End Time", value = trip.endTime)
                DetailDivider()
                DetailRow(label = "Distance", value = "${trip.distanceKm} km")
                DetailDivider()
                DetailRow(label = "Duration", value = "${trip.durationMinutes} min")
                DetailDivider()
                DetailRow(label = "Estimated Fuel Cost", value = "₹$fuelCost")
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 2. DYNAMIC RISK TIMELINE
            // --------------------------------------------------
            SectionTitle(
                title = "DYNAMIC RISK TIMELINE",
                subtitle = "How road risk changed during this journey",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            RiskTimelineCard(timeline = trip.timeline)

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 3. SAFETY
            // --------------------------------------------------
            SectionTitle(
                title = "SAFETY",
                subtitle = "Aggregate journey risk intelligence",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow(
                    label = "Average Risk",
                    value = "${trip.averageRisk} / 100",
                )
                DetailDivider()
                DetailRow(
                    label = "Safety Score",
                    value = "${trip.safetyScore} / 100",
                )
                DetailDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Risk Level",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                    Row(
                        modifier = Modifier
                            .clip(IndicatorPillShape)
                            .background(trip.riskLevel.containerColor.copy(alpha = 0.45f))
                            .border(1.dp, trip.riskLevel.color.copy(alpha = 0.6f), IndicatorPillShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(trip.riskLevel.color),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = trip.riskLevel.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = trip.riskLevel.color,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 4. ROAD EVENTS
            // --------------------------------------------------
            SectionTitle(
                title = "ROAD EVENTS",
                subtitle = "Hazards and vehicle telemetry events",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow(label = "Safety Events", value = "${trip.warningCount + trip.hardBrakingCount}")
                DetailDivider()
                DetailRow(label = "Hazards Encountered", value = "${trip.hazardCount}")
                DetailDivider()
                DetailRow(label = "Hard Braking Events", value = "${trip.hardBrakingCount}")
                DetailDivider()
                DetailRow(label = "Rerouting Events", value = "0")
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 5. SAFETY INSIGHT (Phase 18 Step 8)
            // --------------------------------------------------
            SectionTitle(
                title = "SAFETY INSIGHT",
                subtitle = "Tailored driving feedback for this journey",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "💡", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Journey Analysis",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SmartMoveTextPrimary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = safetySuggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmartMoveTextSecondary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 6. CONDITIONS
            // --------------------------------------------------
            SectionTitle(
                title = "CONDITIONS",
                subtitle = "Ambient environment during trip",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow(label = "Traffic", value = "🚗 ${trip.trafficCondition}")
                DetailDivider()
                DetailRow(label = "Weather", value = "☀️ ${trip.weatherCondition}")
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
        }
    }
}

/**
 * Horizontal Timeline visualizer communicating how risk dynamically changed during a trip.
 */
@Composable
private fun RiskTimelineCard(
    timeline: List<RiskTimelinePoint>,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        if (timeline.isEmpty()) {
            Text(
                text = "No timeline telemetry available for this trip.",
                style = MaterialTheme.typography.bodyMedium,
                color = SmartMoveTextSecondary,
            )
        } else {
            val timelineScrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(timelineScrollState)
                    .padding(vertical = SmartMoveTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                timeline.forEachIndexed { index, point ->
                    // Timeline Point
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(110.dp),
                    ) {
                        // Stage Name & Time
                        Text(
                            text = point.stage,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = SmartMoveTextPrimary,
                        )
                        Text(
                            text = point.time,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SmartMoveTextMuted,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Colored Risk Indicator Node
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(point.riskLevel.containerColor)
                                .border(2.dp, point.riskLevel.color, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(point.riskLevel.color),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Risk Score & Level Text
                        Text(
                            text = point.riskLevel.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = point.riskLevel.color,
                        )
                        Text(
                            text = "${point.riskScore}/100",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SmartMoveTextSecondary,
                        )
                    }

                    // Connector Line (except after the last item)
                    if (index < timeline.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(2.dp)
                                .background(SmartMoveBorder),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = SmartMoveTextPrimary,
        )
    }
}

@Composable
private fun DetailDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(1.dp)
            .background(SmartMoveBorder),
    )
}
