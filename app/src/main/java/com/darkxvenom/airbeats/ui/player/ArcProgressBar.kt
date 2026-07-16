package com.darkxvenom.airbeats.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Wide arc progress bar matching the white paper player screenshot.
 * The circle centre is placed BELOW the composable so only the top arc is
 * visible — sweeping from bottom-left up and across to bottom-right.
 *
 *   startAngle = 200°  (Compose: 0°=right, 90°=down → 200° ≈ lower-left)
 *   sweepAngle = −160° (negative = counter-clockwise)
 *
 * At progress 0 → dot sits at the left arm (~200°).
 * At progress 1 → dot sits at the right arm (~40°).
 *
 * Color params default to the white-paper theme (dark ink on white).
 */
@Composable
fun ArcProgressBarWide(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color  = Color(0xFF111111).copy(alpha = 0.10f),
    activeColor: Color = Color(0xFF111111),
    dotColor: Color    = Color(0xFF111111),
) {
    val progress = remember(position, duration) {
        if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val startAngle = 200f
    val sweepAngle = -160f

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    val activeProgress = if (isDragging) dragProgress else progress

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onPress = { offset ->
                    isDragging = true
                    val center = Offset(size.width / 2f, size.height + 80.dp.toPx())
                    val frac = arcFraction(offset, center, startAngle, sweepAngle)
                    dragProgress = frac
                    onSeek((frac * duration).toLong())
                    tryAwaitRelease()
                    isDragging = false
                    onSeekFinished()
                })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val center = Offset(size.width / 2f, size.height + 80.dp.toPx())
                        dragProgress = arcFraction(offset, center, startAngle, sweepAngle)
                        onSeek((dragProgress * duration).toLong())
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height + 80.dp.toPx())
                        dragProgress = arcFraction(change.position, center, startAngle, sweepAngle)
                        onSeek((dragProgress * duration).toLong())
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekFinished()
                    },
                    onDragCancel = { isDragging = false }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val centerX = w / 2f
            val centerY = h + 80.dp.toPx()
            val center  = Offset(centerX, centerY)

            val radius = w * 0.52f
            val sw     = 2.5.dp.toPx()

            val topLeft = Offset(centerX - radius, centerY - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Inactive track
            drawArc(
                color      = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(width = sw, cap = StrokeCap.Round)
            )

            // Active progress
            val activeSweep = sweepAngle * activeProgress
            if (activeProgress > 0.001f) {
                drawArc(
                    color      = activeColor,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = sw, cap = StrokeCap.Round)
                )
            }

            // Scrubber dot
            val dotDeg = startAngle + activeSweep
            val dotRad = Math.toRadians(dotDeg.toDouble())
            val dotX   = center.x + radius * cos(dotRad).toFloat()
            val dotY   = center.y + radius * sin(dotRad).toFloat()
            val dot    = Offset(dotX, dotY)

            // Subtle glow ring around dot
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        dotColor.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = dot,
                    radius = 14.dp.toPx()
                ),
                center = dot,
                radius = 14.dp.toPx()
            )
            // Solid dot core
            drawCircle(color = dotColor, radius = 5.5.dp.toPx(), center = dot)
            // White inner highlight
            drawCircle(
                color  = Color.White.copy(alpha = 0.6f),
                radius = 2.dp.toPx(),
                center = dot
            )
        }
    }
}

private fun arcFraction(
    offset: Offset,
    center: Offset,
    startAngle: Float,
    sweepAngle: Float
): Float {
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (deg < 0f) deg += 360f
    val absSweep = abs(sweepAngle)
    var relative = startAngle - deg
    if (relative < 0f) relative += 360f
    return if (relative > absSweep) {
        if (relative - absSweep > (360f - absSweep) / 2f) 0f else 1f
    } else {
        (relative / absSweep).coerceIn(0f, 1f)
    }
}
