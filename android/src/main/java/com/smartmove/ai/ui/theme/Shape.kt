package com.smartmove.ai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * SMARTMOVE AI Shape System.
 * Consistent rounded corners tailored for automotive modern design.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),       // Badges, chips
    medium = RoundedCornerShape(12.dp),     // Buttons, input fields
    large = RoundedCornerShape(16.dp),      // Cards, dialogs
    extraLarge = RoundedCornerShape(24.dp), // Elevated bottom sheets, hero cards
)

val CardShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(12.dp)
val BadgeShape = RoundedCornerShape(8.dp)
val IndicatorPillShape = RoundedCornerShape(20.dp)
