package com.smartmove.ai.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.smartmove.ai.ui.theme.ButtonShape
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Primary action button for SMARTMOVE AI.
 * Meets accessibility touch targets (min 48dp) and uses the primary brand blue.
 */
@Composable
fun SmartMoveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    fullWidth: Boolean = true,
) {
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth().height(52.dp)
    } else {
        modifier.defaultMinSize(minHeight = 52.dp)
    }

    Button(
        onClick = onClick,
        modifier = buttonModifier,
        enabled = enabled && !loading,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = SmartMovePrimary,
            contentColor = SmartMoveTextPrimary,
            disabledContainerColor = SmartMovePrimary.copy(alpha = 0.45f),
            disabledContentColor = SmartMoveTextPrimary.copy(alpha = 0.6f),
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = SmartMoveTextPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.medium))
                Text(
                    text = loadingText ?: "Please wait...",
                    style = MaterialTheme.typography.labelLarge,
                )
            } else {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(SmartMoveTheme.spacing.small))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
