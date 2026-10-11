package com.smartmove.ai.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.history.TripHistoryDataProvider
import com.smartmove.ai.ui.history.TripHistoryItem
import com.smartmove.ai.ui.theme.IndicatorPillShape
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import com.smartmove.ai.ui.theme.SmartMoveWarning

/**
 * Safety Analytics Screen for SMARTMOVE AI (Phase 7).
 *
 * Implements dynamic period selection (TODAY, WEEK, MONTH, YEAR), calculated
 * aggregate metrics, risk tier distributions, and lightweight Compose trend charts.
 */
@Composable
fun AnalyticsScreen(
    onStartTripClick: () -> Unit,
    modifier: Modifier = Modifier,
    allTrips: List<TripHistoryItem> = remember { TripHistoryDataProvider.getDemoTrips() },
) {
    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.TODAY) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val analyticsRepo = remember { com.smartmove.ai.data.repository.AnalyticsRepository(context) }

    var state by remember {
        mutableStateOf(AnalyticsCalculator.calculate(allTrips, selectedPeriod))
    }

    LaunchedEffect(selectedPeriod) {
        state = analyticsRepo.getAnalytics(selectedPeriod)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = SmartMoveTheme.spacing.default),
    ) {
        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // Screen Header
        Text(
            text = "Safety Analytics",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = SmartMoveTextPrimary,
        )
        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraSmall))
        Text(
            text = "Understand your driving and road safety patterns",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

        // Period Filter Selector: [ TODAY ] [ WEEK ] [ MONTH ] [ YEAR ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnalyticsPeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedPeriod = period },
                    label = {
                        Text(
                            text = period.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            ),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SmartMovePrimary,
                        selectedLabelColor = SmartMoveTextPrimary,
                        containerColor = SmartMoveSurface,
                        labelColor = SmartMoveTextSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

        if (state.isEmpty) {
            // Empty State
            EmptyAnalyticsView(onStartTripClick = onStartTripClick)
        } else {
            // --------------------------------------------------
            // 1. PRIMARY METRICS (Prominent Cards)
            // --------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.medium),
            ) {
                // Total Distance Card
                SmartMoveCard(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOTAL DISTANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
                    Text(
                        text = state.formattedDistance,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                        ),
                        color = SmartMoveTextPrimary,
                    )
                }

                // Average Risk Card with Risk Pill
                SmartMoveCard(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AVERAGE RISK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "${state.averageRisk} / 100",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                            ),
                            color = SmartMoveTextPrimary,
                        )

                        // Accessible Text Badge
                        Box(
                            modifier = Modifier
                                .clip(IndicatorPillShape)
                                .background(state.riskLevel.containerColor.copy(alpha = 0.5f))
                                .border(1.dp, state.riskLevel.color.copy(alpha = 0.6f), IndicatorPillShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = state.riskLevel.label.replace(" RISK", ""),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                                color = state.riskLevel.color,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // --------------------------------------------------
            // 2. SECONDARY OVERVIEW METRICS
            // --------------------------------------------------
            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetricColumn(label = "TRIPS", value = "${state.totalTrips}")
                    DividerDot()
                    MetricColumn(label = "WARNINGS", value = "${state.totalWarnings}")
                    DividerDot()
                    MetricColumn(label = "SAFETY SCORE", value = "${state.averageSafetyScore}/100")
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            // Secondary Telemetry Events
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.medium),
            ) {
                SmallStatCard(
                    label = "HAZARDS ENCOUNTERED",
                    value = "${state.totalHazards}",
                    modifier = Modifier.weight(1f),
                )
                SmallStatCard(
                    label = "HARD BRAKING EVENTS",
                    value = "${state.totalHardBraking}",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 3. RISK BREAKDOWN
            // --------------------------------------------------
            SectionTitle(
                title = "RISK BREAKDOWN",
                subtitle = "Trip distribution across road risk categories",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            RiskBreakdownCard(breakdown = state.riskBreakdown)

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 4. RISK TREND VISUALIZATION
            // --------------------------------------------------
            SectionTitle(
                title = "RISK TREND",
                subtitle = "Average risk across journeys in this period",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            if (!state.hasSufficientData) {
                SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Not enough trip data yet to calculate trend patterns.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                }
            } else {
                RiskTrendCard(trendPoints = state.riskTrend)
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 5. DISTANCE TREND VISUALIZATION
            // --------------------------------------------------
            SectionTitle(
                title = "DISTANCE TREND",
                subtitle = "Kilometers traveled per trip in this period",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            if (!state.hasSufficientData) {
                SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Not enough trip data yet to calculate trend patterns.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                }
            } else {
                DistanceTrendCard(trendPoints = state.distanceTrend)
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // --------------------------------------------------
            // 6. SAFETY INSIGHTS & SUGGESTIONS (Phase 18 Step 8)
            // --------------------------------------------------
            SectionTitle(
                title = "SAFETY INSIGHTS & SUGGESTIONS",
                subtitle = "Actionable intelligence based on actual trip telemetry",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.suggestions.forEach { suggestion ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "💡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SmartMoveTextSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
        }
    }
}

/**
 * Breakdown Card with visual horizontal progress bars.
 */
@Composable
private fun RiskBreakdownCard(
    breakdown: RiskBreakdown,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        BreakdownRow(
            label = "Low Risk",
            count = breakdown.lowCount,
            ratio = breakdown.lowRatio,
            color = SmartMoveSafe,
            symbol = "🟢",
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        BreakdownRow(
            label = "Medium Risk",
            count = breakdown.mediumCount,
            ratio = breakdown.mediumRatio,
            color = SmartMoveWarning,
            symbol = "🟡",
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        BreakdownRow(
            label = "High Risk",
            count = breakdown.highCount,
            ratio = breakdown.highRatio,
            color = SmartMoveDanger,
            symbol = "🔴",
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    count: Int,
    ratio: Float,
    color: Color,
    symbol: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = symbol, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SmartMoveTextPrimary,
                )
            }
            Text(
                text = if (count == 1) "1 trip" else "$count trips",
                style = MaterialTheme.typography.labelSmall,
                color = SmartMoveTextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(SmartMoveSurfaceVariant),
        ) {
            if (ratio > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

/**
 * Risk Trend Visualizer showing sequence of risk scores across journeys.
 */
@Composable
private fun RiskTrendCard(
    trendPoints: List<AnalyticsTrendPoint>,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        if (trendPoints.isEmpty()) {
            Text(
                text = "No trend data available for this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = SmartMoveTextSecondary,
            )
        } else {
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = SmartMoveTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                trendPoints.forEachIndexed { index, point ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(90.dp),
                    ) {
                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = SmartMoveTextSecondary,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Node Circle with Risk Color
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(point.color.copy(alpha = 0.25f))
                                .border(2.dp, point.color, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(point.color),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = point.displayValue,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = point.color,
                            ),
                        )
                    }

                    if (index < trendPoints.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(SmartMoveBorder),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Distance Trend Visualizer showing proportional bar heights for each journey.
 */
@Composable
private fun DistanceTrendCard(
    trendPoints: List<AnalyticsTrendPoint>,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        if (trendPoints.isEmpty()) {
            Text(
                text = "No distance data available for this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = SmartMoveTextSecondary,
            )
        } else {
            val maxDistance = trendPoints.maxOfOrNull { it.primaryValue } ?: 1f
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .height(130.dp)
                    .padding(vertical = SmartMoveTheme.spacing.small),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                trendPoints.forEach { point ->
                    val ratio = (point.primaryValue / maxDistance).coerceIn(0.15f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(64.dp),
                    ) {
                        Text(
                            text = point.displayValue,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            ),
                            color = SmartMoveTextPrimary,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Scaled Bar
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height((60 * ratio).dp)
                                .clip(CircleShape)
                                .background(SmartMovePrimary),
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = point.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SmartMoveTextSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
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
private fun SmallStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    SmartMoveCard(
        modifier = modifier,
        contentPadding = SmartMoveTheme.spacing.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
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
 * Empty View when no trips match the selected period.
 */
@Composable
private fun EmptyAnalyticsView(
    onStartTripClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = SmartMoveTheme.spacing.large),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SmartMoveSurfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "📊", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

            Text(
                text = "NO TRIPS YET",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = SmartMoveTextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            Text(
                text = "There are no completed trips for this period.",
                style = MaterialTheme.typography.bodyMedium,
                color = SmartMoveTextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            SmartMoveButton(
                text = "START A TRIP",
                onClick = onStartTripClick,
                fullWidth = false,
            )
        }
    }
}
