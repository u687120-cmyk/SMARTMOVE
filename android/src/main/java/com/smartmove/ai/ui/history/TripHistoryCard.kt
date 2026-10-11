package com.smartmove.ai.ui.history

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
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Reusable Card for completed trips in the History list.
 *
 * Displays route, distance, duration, date/time, accessible risk badge,
 * safety score, and warnings count.
 */
@Composable
fun TripHistoryCard(
    trip: TripHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val a11yDescription = "Trip from ${trip.startLocation} to ${trip.destination}, on ${trip.dateDisplay} at ${trip.startTime}. " +
            "Distance ${trip.distanceKm} km, Duration ${trip.durationMinutes} minutes. " +
            "Risk ${trip.riskLevel.label} ${trip.averageRisk} out of 100, Safety score ${trip.safetyScore} out of 100."

    SmartMoveCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = a11yDescription },
        contentPadding = SmartMoveTheme.spacing.default,
        onClick = onClick,
    ) {
        // Route and Date Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Origin -> Destination
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = trip.startLocation,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = SmartMoveTextPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = SmartMovePrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = SmartMoveTextPrimary,
                )
            }

            // Date / Time
            Text(
                text = "${trip.dateDisplay} • ${trip.startTime}",
                style = MaterialTheme.typography.labelSmall,
                color = SmartMoveTextMuted,
            )
        }

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

        // Distance & Duration
        Text(
            text = "${trip.distanceKm} km • ${trip.durationMinutes} min",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

        // Bottom Metrics (Risk Badge, Safety Score, Warnings)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Risk Badge with accessible text label
            Row(
                modifier = Modifier
                    .clip(IndicatorPillShape)
                    .background(trip.riskLevel.containerColor.copy(alpha = 0.45f))
                    .border(1.dp, trip.riskLevel.color.copy(alpha = 0.6f), IndicatorPillShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
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
                    text = "${trip.riskLevel.label} ${trip.averageRisk}/100",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    ),
                    color = trip.riskLevel.color,
                )
            }

            // Safety Score
            Text(
                text = "Safety: ${trip.safetyScore}/100",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = SmartMoveTextPrimary,
            )

            // Warnings Count
            Text(
                text = "⚠️ ${trip.warningCount}",
                style = MaterialTheme.typography.labelSmall,
                color = if (trip.warningCount > 0) SmartMoveTextSecondary else SmartMoveTextMuted,
            )
        }
    }
}
