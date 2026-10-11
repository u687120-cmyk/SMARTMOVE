package com.smartmove.ai.ui.trip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartmove.ai.ui.theme.CardShape
import com.smartmove.ai.ui.theme.IndicatorPillShape
import com.smartmove.ai.ui.theme.SmartMoveBorder
import com.smartmove.ai.ui.theme.SmartMovePrimary
import com.smartmove.ai.ui.theme.SmartMoveSurface
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary

/**
 * Lightweight, high-performance Simulated Map Canvas for Phase 8.
 *
 * Renders stylized automotive roads, active route polyline, destination marker,
 * hazard checkpoints, and the current vehicle position.
 * Decoupled from any external Map SDK for clean future plug-and-play replacement.
 */
@Composable
fun SimulatedMap(
    uiState: TripUiState,
    modifier: Modifier = Modifier,
    selectedRouteId: String = "route-b",
) {
    val a11yDesc = "Simulated road map showing active route from ${uiState.origin} to ${uiState.destination}. " +
            "Current vehicle at ${uiState.currentRoad}, with ${uiState.hazardsOnMap.size} visible road hazards ahead."

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(CardShape)
            .background(Color(0xFF0F172A))
            .border(1.dp, SmartMoveBorder, CardShape)
            .semantics { contentDescription = a11yDesc },
    ) {
        // Canvas Road Network & Route
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Background Grid Roads (Simulated City Arterials)
            val secondaryRoadColor = Color(0xFF1E293B)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            // Horizontal cross arterial
            drawLine(
                color = secondaryRoadColor,
                start = Offset(0f, height * 0.55f),
                end = Offset(width, height * 0.45f),
                strokeWidth = 14f,
                cap = StrokeCap.Round,
            )

            // Diagonal secondary road
            drawLine(
                color = secondaryRoadColor,
                start = Offset(width * 0.15f, 0f),
                end = Offset(width * 0.75f, height),
                strokeWidth = 12f,
                cap = StrokeCap.Round,
            )

            // Curving feeder road
            val feederPath = Path().apply {
                moveTo(width * 0.35f, 0f)
                quadraticBezierTo(width * 0.5f, height * 0.5f, width * 0.95f, height * 0.8f)
            }
            drawPath(
                path = feederPath,
                color = secondaryRoadColor,
                style = Stroke(width = 10f, cap = StrokeCap.Round),
            )

            // 2. Candidate Route Alternatives (Phase 16)
            // Route A: Direct Arterial (Red / Higher Risk)
            val isRouteA = selectedRouteId == "route-a"
            val pathA = Path().apply {
                moveTo(width * 0.15f, height * 0.78f)
                quadraticBezierTo(width * 0.45f, height * 0.50f, width * 0.82f, height * 0.22f)
            }
            drawPath(
                path = pathA,
                color = Color(0xFFEF4444).copy(alpha = if (isRouteA) 0.85f else 0.35f),
                style = Stroke(
                    width = if (isRouteA) 8f else 4f,
                    pathEffect = if (!isRouteA) dashEffect else null,
                    cap = StrokeCap.Round,
                ),
            )

            // Route C: Outer Ring Link (Cyan / Low Risk but longer)
            val isRouteC = selectedRouteId == "route-c"
            val pathC = Path().apply {
                moveTo(width * 0.15f, height * 0.78f)
                quadraticBezierTo(width * 0.15f, height * 0.20f, width * 0.82f, height * 0.22f)
            }
            drawPath(
                path = pathC,
                color = Color(0xFF06B6D4).copy(alpha = if (isRouteC) 0.85f else 0.35f),
                style = Stroke(
                    width = if (isRouteC) 8f else 4f,
                    pathEffect = if (!isRouteC) dashEffect else null,
                    cap = StrokeCap.Round,
                ),
            )

            // Route B / Active Base Route (Emerald Green / Recommended)
            val isRouteB = selectedRouteId == "route-b"
            val routePoints = uiState.routePoints
            if (routePoints.size >= 2) {
                val routePath = Path().apply {
                    val first = routePoints.first()
                    moveTo(first.xRatio * width, first.yRatio * height)
                    for (i in 1 until routePoints.size) {
                        val pt = routePoints[i]
                        val prev = routePoints[i - 1]
                        val midX = (prev.xRatio + pt.xRatio) / 2f * width
                        val midY = (prev.yRatio + pt.yRatio) / 2f * height
                        quadraticBezierTo(prev.xRatio * width, prev.yRatio * height, midX, midY)
                    }
                    val last = routePoints.last()
                    lineTo(last.xRatio * width, last.yRatio * height)
                }

                // Outer glow for selected route
                val activeColor = when (selectedRouteId) {
                    "route-a" -> Color(0xFFEF4444)
                    "route-c" -> Color(0xFF06B6D4)
                    else -> Color(0xFF10B981)
                }

                drawPath(
                    path = routePath,
                    color = activeColor.copy(alpha = 0.25f),
                    style = Stroke(width = 22f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                drawPath(
                    path = routePath,
                    color = activeColor,
                    style = Stroke(width = if (isRouteB) 8f else 5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )

                // Route Center Dashed Guide
                drawPath(
                    path = routePath,
                    color = Color.White.copy(alpha = 0.6f),
                    style = Stroke(width = 2f, pathEffect = dashEffect, cap = StrokeCap.Round),
                )
            }
        }

        // Overlay 1: Destination Pin
        val destX = uiState.destinationXRatio
        val destY = uiState.destinationYRatio
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = (destX * 320).dp,
                    y = (destY * 200).dp,
                )
                .clip(IndicatorPillShape)
                .background(SmartMoveSurface.copy(alpha = 0.9f))
                .border(1.dp, SmartMoveBorder, IndicatorPillShape)
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🏁 ${uiState.destination}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                ),
                color = SmartMoveTextPrimary,
            )
        }

        // Overlay 2: Hazard Markers on Map
        uiState.hazardsOnMap.forEach { hazard ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (hazard.xRatio * 320).dp,
                        y = (hazard.yRatio * 200).dp,
                    )
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(hazard.severity.containerColor)
                    .border(1.5.dp, hazard.severity.color, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (hazard.type.lowercase()) {
                        "pothole" -> "⚠️"
                        "construction" -> "🚧"
                        else -> "⚠️"
                    },
                    fontSize = 11.sp,
                )
            }
        }

        // Overlay 3: Current Vehicle Icon (Visually Distinct)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = (uiState.vehicleXRatio * 320).dp,
                    y = (uiState.vehicleYRatio * 200).dp,
                )
                .size(36.dp)
                .clip(CircleShape)
                .background(SmartMovePrimary.copy(alpha = 0.25f))
                .border(2.dp, SmartMovePrimary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🚗", fontSize = 18.sp)
        }

        // Overlay 4: Compact Traffic HUD Indicator (Phase 15)
        val isHeavyTraffic = uiState.trafficStatus.equals("Heavy", ignoreCase = true)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(IndicatorPillShape)
                .background(
                    if (isHeavyTraffic) Color(0xFFEF4444).copy(alpha = 0.85f)
                    else Color.Black.copy(alpha = 0.65f)
                )
                .border(
                    1.dp,
                    if (isHeavyTraffic) Color(0xFFEF4444) else Color(0xFF334155),
                    IndicatorPillShape,
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                text = if (isHeavyTraffic) "🚗 HEAVY TRAFFIC AHEAD" else "🚗 TRAFFIC: ${uiState.trafficStatus.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (isHeavyTraffic) Color.White else Color(0xFFE2E8F0),
            )
        }

        // Overlay 5: Route Risk Legend (Phase 16)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(IndicatorPillShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .border(1.dp, Color(0xFF334155), IndicatorPillShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "🟢 Lower Risk  🟡 Medium  🔴 Higher",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Color(0xFFCBD5E1),
            )
        }

        // Map Watermark / Mode Badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(IndicatorPillShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = "SMART MAP • SIMULATED",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color(0xFF94A3B8),
            )
        }
    }
}
