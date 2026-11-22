/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 * Originally created by DxV Studio ~~ Dark. Credits: DxV Studio, AirBeats, and Dark.
 * Licensed under GPL-3.0. Retain the original attribution and license terms.
 */
package com.darkxvenom.itune.ui.artist.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact explicit-content mark matching the iTune shelf and mini-player treatment. */
@Composable
fun ExplicitBadge(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = 12.dp, height = 13.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF303034)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "E",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 10.sp,
        )
    }
}

/** Shimmer placeholder used by the live-radio shelf while stations load. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
        .compositeOver(base)
    val sweep = rememberInfiniteTransition(label = "ituneShimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = LinearEasing),
        ),
        label = "ituneShimmerSweep",
    )
    Box(
        modifier
            .clip(shape)
            .drawWithCache {
                val band = size.width * 0.5f
                val startX = -band + sweep.value * (size.width + band * 2)
                val brush = Brush.horizontalGradient(
                    colors = listOf(base, highlight, base),
                    startX = startX,
                    endX = startX + band,
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}
