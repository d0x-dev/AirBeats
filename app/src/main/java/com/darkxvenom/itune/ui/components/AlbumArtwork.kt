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

package com.darkxvenom.itune.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkxvenom.itune.data.model.CoverArtType

@Composable
fun AlbumArtwork(
    type: CoverArtType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            CoverArtType.DIS_BADMAN -> DisBadmanArtwork()
            CoverArtType.VICTORY_LAP -> VictoryLapArtwork()
            CoverArtType.ESPIRITU -> EspirituArtwork()
            CoverArtType.SCHIZO -> SchizoArtwork()
            CoverArtType.BLINDING_LIGHTS -> BlindingLightsArtwork()
            CoverArtType.PAINT_THE_TOWN -> PaintTheTownArtwork()
            CoverArtType.STARBOY -> StarboyArtwork()
            CoverArtType.VAMPIRE -> VampireArtwork()
            CoverArtType.TODAYS_HITS_HERO -> TodaysHitsHeroArtwork()
            CoverArtType.NEVER_ENOUGH_HERO -> NeverEnoughHeroArtwork()
            CoverArtType.ALIST_POP_HERO -> AListPopHeroArtwork()
        }
    }
}

@Composable
private fun DisBadmanArtwork() {
    // Retro comic/pixel blue cover with red mascot character
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3B82B8), Color(0xFF235A85))
            )
        )

        // Mascot boombox/character in red
        drawRoundRect(
            color = Color(0xFFE23D32),
            topLeft = Offset(w * 0.28f, h * 0.28f),
            size = Size(w * 0.44f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )

        // Speakers
        drawCircle(
            color = Color(0xFF1E252D),
            radius = w * 0.09f,
            center = Offset(w * 0.50f, h * 0.52f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.035f,
            center = Offset(w * 0.50f, h * 0.52f)
        )

        // Antenna
        drawLine(
            color = Color.White,
            start = Offset(w * 0.42f, h * 0.28f),
            end = Offset(w * 0.32f, h * 0.16f),
            strokeWidth = w * 0.04f
        )
    }
}

@Composable
private fun VictoryLapArtwork() {
    // Gritty nighttime street scene with golden headlights and urban bokeh
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF1C1A18), Color(0xFF0F0E0D), Color(0xFF060606))
            )
        )

        // Golden streetlights bokeh
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFB347), Color.Transparent),
                center = Offset(w * 0.35f, h * 0.45f),
                radius = w * 0.35f
            )
        )

        // Red tail light streak
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE63946), Color.Transparent),
                center = Offset(w * 0.72f, h * 0.60f),
                radius = w * 0.25f
            )
        )

        // Road perspective lines
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(w * 0.5f, h * 0.4f),
            end = Offset(w * 0.2f, h),
            strokeWidth = w * 0.03f
        )
        drawLine(
            color = Color(0x66FFFFFF),
            start = Offset(w * 0.5f, h * 0.4f),
            end = Offset(w * 0.8f, h),
            strokeWidth = w * 0.03f
        )
    }
}

@Composable
private fun EspirituArtwork() {
    // Night road dusk with car headlights glowing
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF3A1C0F), Color(0xFF1B0D07), Color(0xFF080402))
            )
        )

        // Twin headlight beams
        val beamPath = Path().apply {
            moveTo(w * 0.3f, h * 0.5f)
            lineTo(w * 0.1f, h)
            lineTo(w * 0.5f, h)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x99FFDE59), Color(0x11FFDE59))
            )
        )

        val beamPath2 = Path().apply {
            moveTo(w * 0.7f, h * 0.5f)
            lineTo(w * 0.5f, h)
            lineTo(w * 0.9f, h)
            close()
        }
        drawPath(
            path = beamPath2,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x99FFDE59), Color(0x11FFDE59))
            )
        )

        // Glowing car front
        drawRoundRect(
            color = Color(0xFF1A1512),
            topLeft = Offset(w * 0.25f, h * 0.42f),
            size = Size(w * 0.5f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
    }
}

@Composable
private fun SchizoArtwork() {
    // Bright yellow retro comic poster artwork
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5B824)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Vintage poster shapes
            drawCircle(
                color = Color(0xFFC72828),
                radius = w * 0.22f,
                center = Offset(w * 0.65f, h * 0.45f)
            )

            // Comic stencil figure
            val trianglePath = Path().apply {
                moveTo(w * 0.4f, h * 0.25f)
                lineTo(w * 0.2f, h * 0.75f)
                lineTo(w * 0.6f, h * 0.75f)
                close()
            }
            drawPath(
                path = trianglePath,
                color = Color(0xFF1F1C16)
            )
        }

        Text(
            text = "SCHI\nZO",
            color = Color(0xFF1F1C16),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            lineHeight = 10.sp
        )
    }
}
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

@Composable
private fun BlindingLightsArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF1E38), Color(0xFF880E1C), Color(0xFF100305)),
                center = Offset(w * 0.5f, h * 0.4f),
                radius = w * 0.65f
            )
        )

        // Silhouette head with sunglasses
        drawCircle(
            color = Color(0xFF0A0405),
            radius = w * 0.24f,
            center = Offset(w * 0.5f, h * 0.55f)
        )
    }
}

@Composable
private fun PaintTheTownArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF9E0B0F), Color(0xFF4A0306), Color(0xFF170102))
            )
        )

        // Horn silhouette
        drawCircle(
            color = Color(0xFF120304),
            radius = w * 0.26f,
            center = Offset(w * 0.5f, h * 0.6f)
        )
    }
}

@Composable
private fun StarboyArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F325E), Color(0xFF051226))
            )
        )

        // Glowing neon pink cross
        drawLine(
            color = Color(0xFFFF2D87),
            start = Offset(w * 0.5f, h * 0.2f),
            end = Offset(w * 0.5f, h * 0.8f),
            strokeWidth = w * 0.08f
        )
        drawLine(
            color = Color(0xFFFF2D87),
            start = Offset(w * 0.28f, h * 0.42f),
            end = Offset(w * 0.72f, h * 0.42f),
            strokeWidth = w * 0.08f
        )
    }
}

@Composable
private fun VampireArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF4C2A58), Color(0xFF231329), Color(0xFF0E0710))
            )
        )

        // Band-aid graphic
        drawRoundRect(
            color = Color(0xFFF3E7DC),
            topLeft = Offset(w * 0.35f, h * 0.45f),
            size = Size(w * 0.30f, h * 0.12f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
    }
}

@Composable
fun TodaysHitsHeroArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Luxurious studio dark warm gradient
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF221E19),
                    Color(0xFF332D27),
                    Color(0xFF1F1C18)
                )
            )
        )

        // Warm golden ambient glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66FFD159), Color.Transparent),
                center = Offset(w * 0.25f, h * 0.35f),
                radius = w * 0.45f
            )
        )

        // Artist silhouette with warm studio lighting
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF5D3F2C), Color(0xFF291B13)),
                center = Offset(w * 0.65f, h * 0.45f),
                radius = w * 0.35f
            ),
            topLeft = Offset(w * 0.50f, h * 0.18f),
            size = Size(w * 0.36f, h * 0.65f)
        )

        // Long warm amber hair waves
        drawOval(
            color = Color(0xFF8B5A2B),
            topLeft = Offset(w * 0.46f, h * 0.15f),
            size = Size(w * 0.44f, h * 0.45f)
        )

        // Subtle dark bottom vignette
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xCC000000), Color(0xF5000000)),
                startY = h * 0.5f,
                endY = h
            )
        )
    }
}

@Composable
fun NeverEnoughHeroArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF3D0C0C), Color(0xFF1E0505), Color(0xFF0F0202))
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x88FF3B30), Color.Transparent),
                center = Offset(w * 0.7f, h * 0.4f),
                radius = w * 0.5f
            )
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xF5000000)),
                startY = h * 0.5f,
                endY = h
            )
        )
    }
}

@Composable
fun AListPopHeroArtwork() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF0B2B47), Color(0xFF051727), Color(0xFF020A12))
            )
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x885AC8FA), Color.Transparent),
                center = Offset(w * 0.65f, h * 0.4f),
                radius = w * 0.5f
            )
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xF5000000)),
                startY = h * 0.5f,
                endY = h
            )
        )
    }
}
