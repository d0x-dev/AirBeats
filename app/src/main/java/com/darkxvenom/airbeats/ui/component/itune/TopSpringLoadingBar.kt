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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp


/**
 * A sleek, razor-thin glowing loading bar that streams constantly from left to right
 * whenever the user pulls down into top overscroll spring.
 */
@Composable
fun TopSpringLoadingBar(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "TopLoadingAlpha"
    )
    // Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

    if (alpha > 0.01f) {
        val infiniteTransition = rememberInfiniteTransition(label = "TopLoadingInfinite")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 950, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "TopLoadingProgress"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(2.5.dp)
                .graphicsLayer { this.alpha = alpha }
        ) {
            // Faint track guide
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x28FA2D48))
            )

            // Constantly moving glowing beam from left to right
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Beam width is approx 35% of screen width
                val beamWidth = w * 0.35f
                val travelSpan = w + beamWidth * 2f
                val headX = -beamWidth + (progress * travelSpan)
                val tailX = headX + beamWidth

                val beamBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFA243C).copy(alpha = 0.40f),
                        Color(0xFFFA243C),
                        Color.White,
                        Color(0xFFFA243C),
                        Color(0xFFFA243C).copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    startX = headX,
                    endX = tailX
                )

                drawRect(
                    brush = beamBrush,
                    topLeft = Offset(headX, 0f),
                    size = Size(beamWidth, h)
                )
            }
        }
    }
}
