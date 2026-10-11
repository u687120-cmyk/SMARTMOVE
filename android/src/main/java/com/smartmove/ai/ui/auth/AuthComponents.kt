package com.smartmove.ai.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.theme.ButtonShape
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMoveBorderLight
import com.smartmove.ai.ui.theme.SmartMoveDanger
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * Built-in vector icon for Visibility (Eye open)
 */
val VisibilityIcon: ImageVector
    get() {
        if (_visibilityIcon != null) return _visibilityIcon!!
        _visibilityIcon = ImageVector.Builder(
            name = "Visibility",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 4.5f)
                curveTo(7f, 4.5f, 2.73f, 7.61f, 1f, 12f)
                curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
                reflectiveCurveTo(21.27f, 16.39f, 23f, 12f)
                curveTo(21.27f, 7.61f, 17f, 4.5f, 12f, 4.5f)
                close()
                moveTo(12f, 17f)
                curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
                reflectiveCurveTo(9.24f, 7f, 12f, 7f)
                reflectiveCurveTo(17f, 9.24f, 17f, 12f)
                reflectiveCurveTo(14.76f, 17f, 12f, 17f)
                close()
                moveTo(12f, 9f)
                curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
                reflectiveCurveTo(10.34f, 15f, 12f, 15f)
                reflectiveCurveTo(15f, 13.66f, 15f, 12f)
                reflectiveCurveTo(13.66f, 9f, 12f, 9f)
                close()
            }
        }.build()
        return _visibilityIcon!!
    }
private var _visibilityIcon: ImageVector? = null

/**
 * Built-in vector icon for VisibilityOff (Eye closed / slashed)
 */
val VisibilityOffIcon: ImageVector
    get() {
        if (_visibilityOffIcon != null) return _visibilityOffIcon!!
        _visibilityOffIcon = ImageVector.Builder(
            name = "VisibilityOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 7f)
                curveTo(13.66f, 7f, 15f, 8.34f, 15f, 10f)
                verticalLineTo(10.46f)
                lineTo(9.54f, 5f)
                curveTo(10.33f, 4.68f, 11.15f, 4.5f, 12f, 4.5f)
                curveTo(17f, 4.5f, 21.27f, 7.61f, 23f, 12f)
                curveTo(22.18f, 14.08f, 20.79f, 15.83f, 19f, 17.09f)
                lineTo(17.56f, 15.65f)
                curveTo(18.91f, 14.67f, 20.03f, 13.43f, 20.82f, 12f)
                curveTo(19.17f, 8.68f, 15.83f, 6.5f, 12f, 6.5f)
                curveTo(11.45f, 6.5f, 10.91f, 6.57f, 10.39f, 6.7f)
                lineTo(8.84f, 5.15f)
                curveTo(9.83f, 4.73f, 10.89f, 4.5f, 12f, 4.5f)
                close()
                moveTo(2.81f, 2.81f)
                lineTo(1.39f, 4.22f)
                lineTo(4.46f, 7.29f)
                curveTo(2.97f, 8.58f, 1.8f, 10.18f, 1f, 12f)
                curveTo(2.73f, 16.39f, 7f, 19.5f, 12f, 19.5f)
                curveTo(13.55f, 19.5f, 15.03f, 19.15f, 16.38f, 18.52f)
                lineTo(19.78f, 21.92f)
                lineTo(21.19f, 20.51f)
                lineTo(2.81f, 2.81f)
                close()
                moveTo(12f, 17.5f)
                curveTo(8.17f, 17.5f, 4.83f, 15.32f, 3.18f, 12f)
                curveTo(3.88f, 10.59f, 4.89f, 9.4f, 6.13f, 8.52f)
                lineTo(8.21f, 10.6f)
                curveTo(7.46f, 11.02f, 7f, 11.45f, 7f, 12f)
                curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
                curveTo(12.55f, 17f, 13f, 16.54f, 13.4f, 15.79f)
                lineTo(14.79f, 17.18f)
                curveTo(13.91f, 17.39f, 12.97f, 17.5f, 12f, 17.5f)
                close()
            }
        }.build()
        return _visibilityOffIcon!!
    }
private var _visibilityOffIcon: ImageVector? = null

/**
 * Clean reusable Text Field for SMARTMOVE AI authentication forms.
 * Integrates error states, clear accessible messages, and dark automotive styling.
 */
@Composable
fun SmartMoveTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    val isError = errorMessage != null

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = label) },
            placeholder = if (placeholder != null) {
                { Text(text = placeholder, color = SmartMoveTextMuted) }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            enabled = enabled,
            shape = ButtonShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SmartMoveSurface,
                unfocusedContainerColor = SmartMoveSurface,
                disabledContainerColor = SmartMoveSurface.copy(alpha = 0.5f),
                errorContainerColor = SmartMoveSurface,
                focusedBorderColor = SmartMovePrimary,
                unfocusedBorderColor = SmartMoveBorder,
                errorBorderColor = SmartMoveDanger,
                focusedLabelColor = SmartMovePrimary,
                unfocusedLabelColor = SmartMoveTextSecondary,
                errorLabelColor = SmartMoveDanger,
                focusedTextColor = SmartMoveTextPrimary,
                unfocusedTextColor = SmartMoveTextPrimary,
                cursorColor = SmartMovePrimary,
            ),
        )

        AnimatedVisibility(visible = isError) {
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = SmartMoveDanger,
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                )
            }
        }
    }
}

/**
 * Reusable Password Field with visibility toggle icon and accessible error state.
 */
@Composable
fun SmartMovePasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Password",
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
) {
    var passwordVisible by remember { mutableStateOf(false) }

    SmartMoveTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        placeholder = placeholder,
        errorMessage = errorMessage,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        enabled = enabled,
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible },
                modifier = Modifier.size(SmartMoveTheme.spacing.touchTargetMin),
            ) {
                Icon(
                    imageVector = if (passwordVisible) VisibilityIcon else VisibilityOffIcon,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = if (passwordVisible) SmartMovePrimary else SmartMoveTextSecondary,
                )
            }
        },
    )
}

/**
 * Top Auth Branding Header showing:
 * SMARTMOVE AI
 * "Smarter Roads. Safer Journeys."
 * Screen Title & Subtitle
 */
@Composable
fun SmartMoveAuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // App Identity
        Text(
            text = "SMARTMOVE AI",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
            ),
            color = SmartMoveTextPrimary,
        )
        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraSmall))
        Text(
            text = "Smarter Roads. Safer Journeys.",
            style = MaterialTheme.typography.bodyMedium.copy(
                letterSpacing = 0.5.sp,
            ),
            color = SmartMoveTextSecondary,
        )

        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.section))

        // Screen Heading
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = SmartMoveTextPrimary,
        )
        Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.extraSmall))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = SmartMoveTextSecondary,
        )
    }
}

/**
 * Visual strength indicator for Password fields during Registration.
 */
@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier,
) {
    if (strength == PasswordStrength.EMPTY) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Password Strength: ",
                style = MaterialTheme.typography.labelSmall,
                color = SmartMoveTextSecondary,
            )
            Text(
                text = strength.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = strength.color,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3-Segment Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val totalBars = 3
            val activeBars = when (strength) {
                PasswordStrength.WEAK -> 1
                PasswordStrength.MEDIUM -> 2
                PasswordStrength.STRONG -> 3
                PasswordStrength.EMPTY -> 0
            }

            for (i in 0 until totalBars) {
                val isActive = i < activeBars
                val barColor = if (isActive) strength.color else SmartMoveBorder
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(barColor),
                )
                if (i < totalBars - 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }
    }
}
