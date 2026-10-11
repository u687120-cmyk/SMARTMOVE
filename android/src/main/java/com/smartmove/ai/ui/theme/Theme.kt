package com.smartmove.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val SmartMoveDarkColorScheme = darkColorScheme(
    primary = SmartMovePrimary,
    onPrimary = SmartMoveTextPrimary,
    primaryContainer = SmartMovePrimaryVariant,
    onPrimaryContainer = SmartMoveTextPrimary,
    secondary = SmartMoveTextSecondary,
    onSecondary = SmartMoveTextPrimary,
    background = SmartMoveBackground,
    onBackground = SmartMoveTextPrimary,
    surface = SmartMoveSurface,
    onSurface = SmartMoveTextPrimary,
    surfaceVariant = SmartMoveSurfaceVariant,
    onSurfaceVariant = SmartMoveTextSecondary,
    outline = SmartMoveBorder,
    outlineVariant = SmartMoveBorderLight,
    error = SmartMoveDanger,
    onError = SmartMoveTextPrimary,
)

/**
 * Main application theme wrapper for SMARTMOVE AI.
 * Sets up Material 3 dark color scheme, typography, shapes, and custom design tokens.
 */
@Composable
fun SmartMoveTheme(
    content: @Composable () -> Unit
) {
    val customColors = SmartMoveCustomColors()
    val spacing = SmartMoveSpacing()

    CompositionLocalProvider(
        LocalSmartMoveCustomColors provides customColors,
        LocalSmartMoveSpacing provides spacing,
    ) {
        MaterialTheme(
            colorScheme = SmartMoveDarkColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}

/**
 * Access object for SMARTMOVE custom design tokens.
 */
object SmartMoveTheme {
    val colors: SmartMoveCustomColors
        @Composable
        @ReadOnlyComposable
        get() = LocalSmartMoveCustomColors.current

    val spacing: SmartMoveSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSmartMoveSpacing.current
}
