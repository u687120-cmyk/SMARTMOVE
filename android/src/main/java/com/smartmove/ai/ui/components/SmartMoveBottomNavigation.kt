package com.smartmove.ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary

/**
 * Bottom navigation destinations for SMARTMOVE AI.
 */
enum class BottomNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "Home", Icons.Default.Home),
    MAP("web_map", "Google Map", MapIcon),
    HISTORY("history", "History", Icons.Default.DateRange),
    ANALYTICS("analytics", "Analytics", AnalyticsIcon),
    PROFILE("profile", "Profile", Icons.Default.AccountCircle);
}

/**
 * Built-in vector for Map Navigation icon.
 */
val MapIcon: ImageVector
    get() {
        if (_mapIcon != null) return _mapIcon!!
        _mapIcon = ImageVector.Builder(
            name = "Map",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
            ) {
                moveTo(20.5f, 3f)
                lineTo(14f, 5.5f)
                lineTo(10f, 3.5f)
                lineTo(3.5f, 6.2f)
                lineTo(3.5f, 20.5f)
                lineTo(10f, 18f)
                lineTo(14f, 20f)
                lineTo(20.5f, 17.5f)
                close()
            }
        }.build()
        return _mapIcon!!
    }

private var _mapIcon: ImageVector? = null

/**
 * Built-in vector for Analytics (Bar Chart) to avoid external dependency issues.
 */
val AnalyticsIcon: ImageVector
    get() {
        if (_analyticsIcon != null) return _analyticsIcon!!
        _analyticsIcon = ImageVector.Builder(
            name = "Analytics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter,
                strokeLineMiter = 4.0f,
                pathFillType = PathFillType.NonZero,
            ) {
                // Bar 1 (left)
                moveTo(4f, 13f)
                horizontalLineTo(7f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                close()
                // Bar 2 (center)
                moveTo(10f, 9f)
                horizontalLineTo(13f)
                verticalLineTo(20f)
                horizontalLineTo(10f)
                close()
                // Bar 3 (right)
                moveTo(16f, 4f)
                horizontalLineTo(19f)
                verticalLineTo(20f)
                horizontalLineTo(16f)
                close()
            }
        }.build()
        return _analyticsIcon!!
    }

private var _analyticsIcon: ImageVector? = null

/**
 * Bottom Navigation Bar for SMARTMOVE AI.
 * Uses #111827 container, #2563EB for selected destination, #94A3B8 for unselected items.
 */
@Composable
fun SmartMoveBottomNavigation(
    currentRoute: String,
    onNavigateToDestination: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        // Top border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SmartMoveBorder),
        )

        NavigationBar(
            containerColor = SmartMoveSurface,
            contentColor = SmartMoveTextPrimary,
            tonalElevation = 0.dp,
        ) {
            BottomNavDestination.entries.forEach { destination ->
                val isSelected = currentRoute == destination.route

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onNavigateToDestination(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                            ),
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SmartMovePrimary,
                        selectedTextColor = SmartMovePrimary,
                        unselectedIconColor = SmartMoveTextSecondary,
                        unselectedTextColor = SmartMoveTextSecondary,
                        indicatorColor = SmartMovePrimary.copy(alpha = 0.15f),
                    ),
                )
            }
        }
    }
}
