package com.smartmove.ai.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// SMARTMOVE AI Core Brand Colors
val SmartMoveBackground = Color(0xFF0B1220)       // Primary background
val SmartMoveSurface = Color(0xFF111827)          // Secondary background / Card surface
val SmartMoveSurfaceVariant = Color(0xFF1E293B)   // Elevated cards / subtle surface
val SmartMovePrimary = Color(0xFF2563EB)          // Primary blue accent
val SmartMovePrimaryVariant = Color(0xFF1D4ED8)   // Darker blue for pressed states

// Safety and Risk Indicators
val SmartMoveSafe = Color(0xFF22C55E)             // Safe / Low Risk (Green)
val SmartMoveSafeBg = Color(0xFF064E3B)           // Safe badge background
val SmartMoveWarning = Color(0xFFF59E0B)          // Warning / Medium Risk (Amber)
val SmartMoveWarningBg = Color(0xFF78350F)        // Warning badge background
val SmartMoveDanger = Color(0xFFEF4444)           // Danger / High Risk (Red)
val SmartMoveDangerBg = Color(0xFF7F1D1D)         // Danger badge background

// Typography Colors
val SmartMoveTextPrimary = Color(0xFFF8FAFC)      // Primary text
val SmartMoveTextSecondary = Color(0xFF94A3B8)    // Secondary text / subtitles
val SmartMoveTextMuted = Color(0xFF64748B)        // Tertiary / hint text

// Borders & Outlines
val SmartMoveBorder = Color(0xFF1E293B)           // Subtle card border
val SmartMoveBorderLight = Color(0xFF334155)      // Highlight border

@Immutable
data class SmartMoveCustomColors(
    val background: Color = SmartMoveBackground,
    val surface: Color = SmartMoveSurface,
    val surfaceVariant: Color = SmartMoveSurfaceVariant,
    val primary: Color = SmartMovePrimary,
    val safe: Color = SmartMoveSafe,
    val safeBg: Color = SmartMoveSafeBg,
    val warning: Color = SmartMoveWarning,
    val warningBg: Color = SmartMoveWarningBg,
    val danger: Color = SmartMoveDanger,
    val dangerBg: Color = SmartMoveDangerBg,
    val textPrimary: Color = SmartMoveTextPrimary,
    val textSecondary: Color = SmartMoveTextSecondary,
    val border: Color = SmartMoveBorder,
)

val LocalSmartMoveCustomColors = staticCompositionLocalOf {
    SmartMoveCustomColors()
}
