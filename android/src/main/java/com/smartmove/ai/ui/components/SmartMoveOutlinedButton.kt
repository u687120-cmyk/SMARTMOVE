package com.smartmove.ai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartmove.ai.ui.theme.ButtonShape
import com.smartmove.ai.ui.theme.SmartMoveBorderLight
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Secondary outlined action button for SMARTMOVE AI.
 */
@Composable
fun SmartMoveOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    fullWidth: Boolean = true,
) {
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth().height(52.dp)
    } else {
        modifier.defaultMinSize(minHeight = 52.dp)
    }

    OutlinedButton(
        onClick = onClick,
        modifier = buttonModifier,
        enabled = enabled,
        shape = ButtonShape,
        border = BorderStroke(1.dp, SmartMoveBorderLight),
        contentPadding = androidx.compose.material3.ButtonDefaults.ContentPadding,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.small))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = SmartMoveTextPrimary,
            )
        }
    }
}
