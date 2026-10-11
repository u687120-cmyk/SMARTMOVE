package com.smartmove.ai.ui.alerts

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.components.SmartMoveOutlinedButton
import com.smartmove.ai.ui.theme.IndicatorPillShape
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveSurfaceVariant
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import com.smartmove.ai.ui.theme.SmartMoveWarning
import kotlinx.coroutines.launch

/**
 * Hazard Details & Community Verification Screen (Phase 9).
 *
 * Shows full hazard intelligence and provides community confirmation actions
 * that drive deterministic confidence & lifecycle transitions.
 */
@Composable
fun HazardDetailsScreen(
    hazardId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hazard by remember {
        mutableStateOf(
            HazardRepository.getHazardById(hazardId)
                ?: HazardRepository.getActiveHazards().firstOrNull()
                ?: HazardItem(
                    id = "unknown",
                    type = HazardType.POTHOLE,
                    title = "Pothole detected",
                    severity = com.smartmove.ai.ui.components.RiskLevel.MEDIUM,
                )
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

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
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SmartMoveTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SmartMoveTextPrimary,
                    )
                }

                Text(
                    text = "Hazard Details",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = SmartMoveTextPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // 1. HERO HAZARD CARD
            SmartMoveCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = SmartMoveTheme.spacing.large,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = hazard.type.icon, fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = hazard.type.title.uppercase(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                                color = SmartMoveTextPrimary,
                            )
                            Text(
                                text = hazard.distanceDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SmartMoveTextSecondary,
                            )
                        }
                    }

                    // Severity Pill
                    Row(
                        modifier = Modifier
                            .clip(IndicatorPillShape)
                            .background(hazard.severity.containerColor.copy(alpha = 0.45f))
                            .border(1.dp, hazard.severity.color.copy(alpha = 0.6f), IndicatorPillShape)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(hazard.severity.color),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hazard.severity.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = hazard.severity.color,
                        )
                    }
                }

                hazard.description?.let { desc ->
                    Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))
                    Text(
                        text = "\"$desc\"",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = SmartMoveTextPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // 2. CONFIDENCE & LIFECYCLE
            SectionTitle(
                title = "CONFIDENCE & STATUS",
                subtitle = "Algorithmic validation based on community observations",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Confidence Level",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                    Text(
                        text = "${hazard.confidence}%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = SmartMovePrimary,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Confidence Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(SmartMoveSurfaceVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(hazard.confidence / 100f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(SmartMovePrimary),
                    )
                }

                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

                DetailItemRow(label = "Lifecycle Status", value = hazard.status.label.uppercase())
                DividerLine()
                DetailItemRow(label = "Location", value = hazard.locationName)
                DividerLine()
                DetailItemRow(label = "Reported", value = hazard.reportedAtDisplay)
                DividerLine()
                DetailItemRow(label = "Last Confirmed", value = hazard.lastConfirmedAtDisplay)
                DividerLine()
                DetailItemRow(label = "Confirmations", value = "${hazard.confirmationCount}")
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraLarge))

            // 3. COMMUNITY CONFIRMATION ACTION
            SectionTitle(
                title = "COMMUNITY CONFIRMATION",
                subtitle = "Is this hazard still present on the road?",
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            if (hazard.status == HazardStatus.RESOLVED) {
                SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "✓ This hazard has been resolved and is no longer broadcast to active drivers.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SmartMoveSafe,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.medium),
                ) {
                    // YES, STILL THERE
                    SmartMoveButton(
                        text = "YES, STILL THERE",
                        onClick = {
                            coroutineScope.launch {
                                val updated = HazardRepository.confirmHazardRemote(context, hazard.id, true)
                                if (updated != null) {
                                    hazard = updated
                                    snackbarHostState.showSnackbar("Thank you! Hazard confidence updated to ${updated.confidence}%.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )

                    // FIXED / NO LONGER THERE
                    SmartMoveOutlinedButton(
                        text = "FIXED / GONE",
                        onClick = {
                            coroutineScope.launch {
                                val updated = HazardRepository.confirmHazardRemote(context, hazard.id, false)
                                if (updated != null) {
                                    hazard = updated
                                    val statusMsg = if (updated.status == HazardStatus.RESOLVED) {
                                        "Marked as RESOLVED. Thank you!"
                                    } else {
                                        "Confidence decreased to ${updated.confidence}%."
                                    }
                                    snackbarHostState.showSnackbar(statusMsg)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun DetailItemRow(
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
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(1.dp)
            .background(SmartMoveBorder),
    )
}
