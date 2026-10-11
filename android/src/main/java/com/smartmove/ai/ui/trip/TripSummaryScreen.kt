package com.smartmove.ai.ui.trip

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.components.RiskLevel
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.theme.IndicatorPillShape
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Trip Summary Screen presented immediately after completing a journey (Phase 8).
 */
@Composable
fun TripSummaryScreen(
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
    origin: String = "Pune",
    destination: String = "Hinjewadi",
    distanceKm: Double = 24.6,
    durationMinutes: Int = 32,
    averageRisk: Int = 24,
    riskLevel: RiskLevel = RiskLevel.LOW,
    safetyScore: Int = 92,
    warningsCount: Int = 1,
) {
    val scrollState = rememberScrollState()
    val liveSummary = remember { com.smartmove.ai.data.repository.TripRepository.getLastCompletedTrip() }

    val finalOrigin = liveSummary?.origin ?: origin
    val finalDest = liveSummary?.destination ?: destination
    val finalRoute = liveSummary?.routeName ?: "Recommended Route"
    val finalDist = liveSummary?.distanceKm ?: distanceKm
    val finalDur = liveSummary?.durationMinutes ?: durationMinutes
    val finalRisk = liveSummary?.averageRisk ?: averageRisk
    val finalRiskLvl = liveSummary?.riskLevel ?: riskLevel
    val finalSafety = liveSummary?.safetyScore ?: safetyScore
    val finalEvents = (liveSummary?.warningsCount ?: warningsCount) + (liveSummary?.hardBrakingCount ?: 0)
    val finalReroutes = liveSummary?.reroutesCount ?: 0
    val finalSuggestion = liveSummary?.suggestion ?: "Smooth driving throughout this corridor."

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = SmartMoveTheme.spacing.default),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

        // Success Checkmark Badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SmartMoveSafe.copy(alpha = 0.15f))
                .border(2.dp, SmartMoveSafe, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🏁", fontSize = 32.sp)
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        Text(
            text = "TRIP COMPLETED",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            ),
            color = SmartMoveTextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Your journey is complete.",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        // Origin -> Destination
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = finalOrigin,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SmartMoveTextPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SmartMovePrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = finalDest,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SmartMoveTextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Route Name Badge
        Box(
            modifier = Modifier
                .clip(IndicatorPillShape)
                .background(SmartMoveSurfaceVariant)
                .border(1.dp, SmartMoveBorder, IndicatorPillShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = "Route: $finalRoute",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SmartMovePrimary,
            )
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

        // Primary Metrics Card
        SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DISTANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$finalDist km",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveTextPrimary,
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(SmartMoveBorder),
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$finalDur min",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveTextPrimary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // Safety Performance Breakdown
        SectionTitle(title = "SAFETY PERFORMANCE")

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
            SummaryDetailRow(
                label = "Risk",
                value = "$finalRisk / 100",
                badgeText = finalRiskLvl.label,
                badgeColor = finalRiskLvl.color,
            )
            SummaryDivider()
            SummaryDetailRow(
                label = "Safety Score",
                value = "$finalSafety",
            )
            SummaryDivider()
            SummaryDetailRow(
                label = "Safety Events",
                value = "$finalEvents",
            )
            SummaryDivider()
            SummaryDetailRow(
                label = "Reroutes",
                value = "$finalReroutes",
            )
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // Safety Suggestions & Insights (Phase 18 Step 8)
        SectionTitle(title = "SAFETY INSIGHT")

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
                        text = "Driving Suggestion",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = finalSuggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

        // Done Button (Returns to Home)
        SmartMoveButton(
            text = "DONE",
            onClick = {
                com.smartmove.ai.data.repository.TripRepository.clearLastCompletedTrip()
                onDoneClick()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
    }
}

@Composable
private fun SummaryDetailRow(
    label: String,
    value: String,
    badgeText: String? = null,
    badgeColor: androidx.compose.ui.graphics.Color? = null,
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = SmartMoveTextPrimary,
            )

            if (badgeText != null && badgeColor != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(IndicatorPillShape)
                        .background(badgeColor.copy(alpha = 0.2f))
                        .border(1.dp, badgeColor, IndicatorPillShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        ),
                        color = badgeColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(1.dp)
            .background(SmartMoveBorder),
    )
}
