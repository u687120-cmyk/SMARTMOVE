package com.smartmove.ai.ui.alerts

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.components.ActiveAlertCard
import com.smartmove.ai.ui.components.SectionTitle
import com.smartmove.ai.ui.components.SmartMoveButton
import com.smartmove.ai.ui.components.SmartMoveCard
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

/**
 * Safety Alerts Screen (Phase 9).
 *
 * Displays prioritized active hazard warnings, user-submitted reports,
 * and entry points to submit new hazard reports.
 */
@Composable
fun AlertsScreen(
    onBackClick: () -> Unit,
    onHazardClick: (String) -> Unit,
    onReportHazardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        HazardRepository.syncWithBackend(context)
    }

    val activeHazards = HazardRepository.getActiveHazards()
    val myReports = HazardRepository.getMyReports()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .statusBarsPadding()
            .padding(horizontal = SmartMoveTheme.spacing.default),
    ) {
        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SmartMoveTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = SmartMoveTextPrimary,
                )
            }

            Text(
                text = "ALERTS & HAZARDS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = SmartMovePrimary,
            )

            // Report action
            Text(
                text = "+ REPORT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = SmartMovePrimary,
                ),
                modifier = Modifier
                    .clickable(onClick = onReportHazardClick)
                    .padding(8.dp),
            )
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        // Screen Titles
        Text(
            text = "Road Alerts",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = SmartMoveTextPrimary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Road conditions that may need your attention",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SmartMoveTheme.spacing.medium),
        ) {
            // 1. ACTIVE ALERTS SECTION
            item {
                SectionTitle(
                    title = "ACTIVE ALERTS",
                    subtitle = "Prioritized by hazard risk severity",
                )
            }

            if (activeHazards.isEmpty()) {
                item {
                    ActiveAlertCard() // Safe state: "✓ NO ACTIVE ALERTS"
                }
            } else {
                items(
                    items = activeHazards,
                    key = { it.id },
                ) { hazard ->
                    HazardAlertCard(
                        hazard = hazard,
                        onClick = { onHazardClick(hazard.id) },
                    )
                }
            }

            // 2. MY REPORTS SECTION
            item {
                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.large))
                SectionTitle(
                    title = "MY REPORTS",
                    subtitle = "Hazards submitted by you",
                )
            }

            if (myReports.isEmpty()) {
                item {
                    SmartMoveCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "You haven't reported any hazards yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmartMoveTextSecondary,
                        )
                    }
                }
            } else {
                items(
                    items = myReports,
                    key = { "my-${it.id}" },
                ) { report ->
                    MyReportCard(
                        hazard = report,
                        onClick = { onHazardClick(report.id) },
                    )
                }
            }

            // Report Hazard Floating Button at Bottom
            item {
                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))
                SmartMoveButton(
                    text = "REPORT NEW HAZARD",
                    onClick = onReportHazardClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.huge))
            }
        }
    }
}

/**
 * Compact representation of a user's own submitted report.
 */
@Composable
private fun MyReportCard(
    hazard: HazardItem,
    onClick: () -> Unit,
) {
    SmartMoveCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = SmartMoveTheme.spacing.default,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = hazard.type.icon, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = hazard.type.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = SmartMoveTextPrimary,
                    )
                    Text(
                        text = "${hazard.confidence}% confidence • Reported ${hazard.reportedAtDisplay}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SmartMoveTextMuted,
                    )
                }
            }

            // Status Badge
            val statusColor = when (hazard.status) {
                HazardStatus.ACTIVE -> SmartMoveDanger
                HazardStatus.UNDER_REVIEW -> SmartMoveWarning
                HazardStatus.RESOLVED -> SmartMoveSafe
            }

            Box(
                modifier = Modifier
                    .clip(IndicatorPillShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor.copy(alpha = 0.5f), IndicatorPillShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = hazard.status.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                    ),
                    color = statusColor,
                )
            }
        }
    }
}
