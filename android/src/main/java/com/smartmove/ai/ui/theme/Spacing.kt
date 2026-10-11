package com.smartmove.ai.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * SMARTMOVE AI Spacing System.
 * Consistent padding and margin values to ensure visual harmony and predictable layouts.
 */
@Immutable
data class SmartMoveSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val default: Dp = 16.dp,       // Standard screen padding / card inner padding
    val large: Dp = 20.dp,
    val extraLarge: Dp = 24.dp,    // Spacing between major sections
    val section: Dp = 28.dp,
    val huge: Dp = 32.dp,
    val touchTargetMin: Dp = 48.dp // Accessibility minimum touch target
)

val LocalSmartMoveSpacing = staticCompositionLocalOf {
    SmartMoveSpacing()
}
