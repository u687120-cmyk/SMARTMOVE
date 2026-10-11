package com.smartmove.ai.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.home.ActiveAlertData
import com.smartmove.ai.ui.home.AlertSeverity
import com.smartmove.ai.ui.theme.IndicatorPillShape
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMoveDangerBg
import com.smartmove.ai.ui.theme.SmartMoveSafe
import com.smartmove.ai.ui.theme.SmartMoveSafeBg
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme
import com.smartmove.ai.ui.theme.SmartMoveWarning
import com.smartmove.ai.ui.theme.SmartMoveWarningBg

/**
 * Reusable Active Alert Card for SMARTMOVE AI.
 * Displays real-time road hazard notifications or safe confirmation when no hazards are present.
 */
@Composable
fun ActiveAlertCard(
    alert: ActiveAlertData? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (alert == null) {
        // Safe State: No Active Alerts
        SmartMoveCard(
            modifier = modifier.fillMaxWidth(),
            contentPadding = SmartMoveTheme.spacing.default,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Check Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SmartMoveSafeBg)
                        .border(1.dp, SmartMoveSafe.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SmartMoveSafe,
                    )
                }

                Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.default))

                Column {
                    Text(
                        text = "NO ACTIVE ALERTS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = SmartMoveTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "You're good to go.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartMoveTextSecondary,
                    )
                }
            }
        }
    } else {
        // Active Hazard State
        val (badgeColor, badgeBg, iconSymbol) = when (alert.severity) {
            AlertSeverity.DANGER -> Triple(SmartMoveDanger, SmartMoveDangerBg, "🚨")
            AlertSeverity.WARNING -> Triple(SmartMoveWarning, SmartMoveWarningBg, "⚠️")
            AlertSeverity.INFO -> Triple(SmartMoveSafe, SmartMoveSafeBg, "ℹ️")
        }

        SmartMoveCard(
            modifier = modifier.fillMaxWidth(),
            contentPadding = SmartMoveTheme.spacing.default,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(badgeBg)
                            .border(1.dp, badgeColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = iconSymbol, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.default))

                    Column {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = badgeColor,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SmartMoveTextSecondary,
                        )
                    }
                }

                if (alert.distance != null) {
                    Text(
                        text = alert.distance,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = SmartMoveTextMuted,
                    )
                }
            }
        }
    }
}
