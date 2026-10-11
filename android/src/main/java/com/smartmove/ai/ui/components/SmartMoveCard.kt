package com.smartmove.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartmove.ai.ui.theme.CardShape
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Standard container card for SMARTMOVE AI.
 * Uses secondary background (#111827) with a subtle slate border and 16dp rounded corners.
 */
@Composable
fun SmartMoveCard(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = SmartMoveSurface,
    borderColor: androidx.compose.ui.graphics.Color = SmartMoveBorder,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardColors = CardDefaults.cardColors(
        containerColor = containerColor,
    )
    val border = BorderStroke(1.dp, borderColor)

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = CardShape,
            colors = cardColors,
            border = border,
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = CardShape,
            colors = cardColors,
            border = border,
        ) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}
