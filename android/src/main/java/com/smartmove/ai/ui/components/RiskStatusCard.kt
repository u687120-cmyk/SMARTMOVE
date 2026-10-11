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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * High-level classification for current road risk.
 * Never labels roads permanently; represents real-time dynamic conditions.
 */
enum class RiskLevel(
    val label: String,
    val indicatorSymbol: String,
    val color: Color,
    val containerColor: Color,
    val defaultDescription: String,
) {
    LOW(
        label = "LOW RISK",
        indicatorSymbol = "🟢",
        color = SmartMoveSafe,
        containerColor = SmartMoveSafeBg,
        defaultDescription = "Road conditions look safe right now.",
    ),
    MEDIUM(
        label = "MEDIUM RISK",
        indicatorSymbol = "🟡",
        color = SmartMoveWarning,
        containerColor = SmartMoveWarningBg,
        defaultDescription = "Moderate caution advised. Watch for traffic or braking events.",
    ),
    HIGH(
        label = "HIGH RISK",
        indicatorSymbol = "🔴",
        color = SmartMoveDanger,
        containerColor = SmartMoveDangerBg,
        defaultDescription = "Elevated danger detected. Reduced speed and high alert required.",
    );

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score <= 30 -> LOW
            score <= 60 -> MEDIUM
            else -> HIGH
        }
    }
}

/**
 * Hero Safety-focused card displaying the current dynamic risk status.
 *
 * Visual Hierarchy:
 * - Header: CURRENT SAFETY
 * - Indicator badge (🟢 / 🟡 / 🔴)
 * - Text label (LOW RISK / MEDIUM RISK / HIGH RISK)
 * - Score (24 / 100)
 * - Recommendation / message
 *
 * Accessibility:
 * - Does NOT rely solely on color: explicitly presents text label and numeric score
 * - Includes a semantic content description for screen readers
 */
@Composable
fun RiskStatusCard(
    riskLevel: RiskLevel,
    score: Int,
    description: String = riskLevel.defaultDescription,
    modifier: Modifier = Modifier,
    riskFactors: List<String> = emptyList(),
    onClick: (() -> Unit)? = null,
) {
    val a11yDescription = "Current Safety status: ${riskLevel.label}, Score: $score out of 100. $description"

    SmartMoveCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = a11yDescription },
        contentPadding = SmartMoveTheme.spacing.large,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Label
            Text(
                text = "CURRENT SAFETY",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = SmartMoveTextSecondary,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))

            // Indicator Icon / Glowing Badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(riskLevel.containerColor.copy(alpha = 0.5f))
                    .border(2.dp, riskLevel.color.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = riskLevel.indicatorSymbol,
                    fontSize = 24.sp,
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            // Risk Text Label (Accessible text in addition to color)
            Text(
                text = riskLevel.label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                ),
                color = riskLevel.color,
            )

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            // Risk Score Presentation
            Row(
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 42.sp,
                        lineHeight = 46.sp,
                    ),
                    color = SmartMoveTextPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "/ 100",
                    style = MaterialTheme.typography.titleMedium,
                    color = SmartMoveTextMuted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))

            // Recommendation Message
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SmartMoveTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = SmartMoveTheme.spacing.small),
            )
        }
    }
}
