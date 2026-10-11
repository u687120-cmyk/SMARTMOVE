package com.smartmove.ai.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.theme.IndicatorPillShape
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Reusable Card for Active Road Hazard Alerts.
 */
@Composable
fun HazardAlertCard(
    hazard: HazardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val a11yDesc = "${hazard.severity.label} hazard: ${hazard.type.title} at ${hazard.distanceDisplay}. " +
            "Confidence ${hazard.confidence} percent. Confirmed ${hazard.lastConfirmedAtDisplay}."

    SmartMoveCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = a11yDesc },
        contentPadding = SmartMoveTheme.spacing.default,
        onClick = onClick,
    ) {
        // Top Row: Icon + Type and Severity Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = hazard.type.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = hazard.type.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = SmartMoveTextPrimary,
                )
            }

            // Severity Chip
            Row(
                modifier = Modifier
                    .clip(IndicatorPillShape)
                    .background(hazard.severity.containerColor.copy(alpha = 0.45f))
                    .border(1.dp, hazard.severity.color.copy(alpha = 0.6f), IndicatorPillShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
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
                        fontSize = 10.sp,
                    ),
                    color = hazard.severity.color,
                )
            }
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        // Distance & Location
        Text(
            text = hazard.distanceDisplay,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = SmartMoveTextPrimary,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = hazard.locationName,
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // Bottom Row: Confidence & Recency
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${hazard.confidence}% confidence",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = SmartMovePrimary,
                ),
            )

            Text(
                text = "Confirmed ${hazard.lastConfirmedAtDisplay}",
                style = MaterialTheme.typography.labelSmall,
                color = SmartMoveTextMuted,
            )
        }
    }
}
