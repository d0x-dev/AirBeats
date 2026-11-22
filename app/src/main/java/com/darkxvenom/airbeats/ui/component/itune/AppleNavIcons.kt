/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of iTune and is licensed under the GNU General Public License v3.0 (GPL-3.0).
 * Any copying, modification, or redistribution of this code must strictly comply with the
 * terms of the project LICENSE file and retain full author credit.
 */

package com.darkxvenom.airbeats.ui.component.itune

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pixel-accurate vector implementations of Apple Music's bottom navigation icons:
 * 1. Home (Pitched solid roof with right chimney and house base)
 * 2. New (2x2 grid of smooth rounded squares)
 * 3. Radio (Center audio dot flanked by dual broadcasting concentric sound arcs)
 * 4. Library (Stacked album sheets with cutout eighth music note)
 * 5. Search (Clean circular lens with 45-degree angled handle)
 */

@Composable
fun AppleHomeIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Chimney on right roof
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.68f, h * 0.16f),
            size = Size(w * 0.11f, h * 0.28f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )

        // Solid pitched roof and base
        val housePath = Path().apply {
            // Apex
            moveTo(w * 0.50f, h * 0.13f)
            // Right eaves
            lineTo(w * 0.89f, h * 0.44f)
            lineTo(w * 0.81f, h * 0.48f)
            // Right wall
            lineTo(w * 0.81f, h * 0.86f)
            // Floor
            lineTo(w * 0.19f, h * 0.86f)
            // Left wall
            lineTo(w * 0.19f, h * 0.48f)
            // Left eaves
            lineTo(w * 0.11f, h * 0.44f)
            close()
        }
        drawPath(housePath, color = tint)
    }
}

@Composable
fun AppleNewIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sq = w * 0.38f
        val gap = w * 0.10f
        val cr = CornerRadius(w * 0.11f, w * 0.11f)

        val x0 = (w - (sq * 2f + gap)) / 2f
        val y0 = (h - (sq * 2f + gap)) / 2f

        // Top-left square
        drawRoundRect(
            color = tint,
            topLeft = Offset(x0, y0),
            size = Size(sq, sq),
            cornerRadius = cr
        )
        // Top-right square
        drawRoundRect(
            color = tint,
            topLeft = Offset(x0 + sq + gap, y0),
            size = Size(sq, sq),
            cornerRadius = cr
        )
        // Bottom-left square
        drawRoundRect(
            color = tint,
            topLeft = Offset(x0, y0 + sq + gap),
            size = Size(sq, sq),
            cornerRadius = cr
        )
        // Bottom-right square
        drawRoundRect(
            color = tint,
            topLeft = Offset(x0 + sq + gap, y0 + sq + gap),
            size = Size(sq, sq),
            cornerRadius = cr
        )
    }
}
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

@Composable
fun AppleRadioIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w * 0.5f, h * 0.5f)

        // Center solid broadcast circle
        drawCircle(
            color = tint,
            radius = w * 0.11f,
            center = center
        )

        val sw = w * 0.082f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round)

        // Inner left arc
        drawArc(
            color = tint,
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - w * 0.25f, center.y - h * 0.25f),
            size = Size(w * 0.50f, h * 0.50f),
            style = stroke
        )
        // Inner right arc
        drawArc(
            color = tint,
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - w * 0.25f, center.y - h * 0.25f),
            size = Size(w * 0.50f, h * 0.50f),
            style = stroke
        )

        // Outer left arc
        drawArc(
            color = tint,
            startAngle = 130f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(center.x - w * 0.40f, center.y - h * 0.40f),
            size = Size(w * 0.80f, h * 0.80f),
            style = stroke
        )
        // Outer right arc
        drawArc(
            color = tint,
            startAngle = -50f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(center.x - w * 0.40f, center.y - h * 0.40f),
            size = Size(w * 0.80f, h * 0.80f),
            style = stroke
        )
    }
}

@Composable
fun AppleLibraryIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Top stacked sheet (smallest)
        drawRoundRect(
            color = tint.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.35f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.065f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // Middle stacked sheet (medium)
        drawRoundRect(
            color = tint.copy(alpha = 0.92f),
            topLeft = Offset(w * 0.24f, h * 0.21f),
            size = Size(w * 0.52f, h * 0.065f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // Main solid library album card
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.30f),
            size = Size(w * 0.70f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )

        // Cutout musical note inside (White)
        val noteHeadCenter = Offset(w * 0.42f, h * 0.70f)
        drawOval(
            color = Color.White,
            topLeft = Offset(noteHeadCenter.x - w * 0.11f, noteHeadCenter.y - h * 0.08f),
            size = Size(w * 0.20f, h * 0.14f)
        )

        // Note stem
        drawLine(
            color = Color.White,
            start = Offset(w * 0.50f, h * 0.70f),
            end = Offset(w * 0.50f, h * 0.42f),
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )

        // Note flag
        val flagPath = Path().apply {
            moveTo(w * 0.50f, h * 0.42f)
            quadraticTo(
                w * 0.64f, h * 0.46f,
                w * 0.65f, h * 0.58f
            )
            lineTo(w * 0.57f, h * 0.58f)
            quadraticTo(
                w * 0.57f, h * 0.49f,
                w * 0.50f, h * 0.46f
            )
            close()
        }
        drawPath(flagPath, color = Color.White)
    }
}

@Composable
fun AppleSearchIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val strokeWidth = w * 0.09f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

        // Lens circle
        val lensRadius = w * 0.28f
        val lensCenter = Offset(w * 0.42f, h * 0.42f)
        drawCircle(
            color = tint,
            radius = lensRadius,
            center = lensCenter,
            style = stroke
        )

        // Handle at ~45 degrees
        val handleStart = Offset(
            lensCenter.x + lensRadius * 0.707f,
            lensCenter.y + lensRadius * 0.707f
        )
        val handleEnd = Offset(w * 0.84f, h * 0.84f)
        drawLine(
            color = tint,
            start = handleStart,
            end = handleEnd,
            strokeWidth = strokeWidth * 1.15f,
            cap = StrokeCap.Round
        )
    }
}
