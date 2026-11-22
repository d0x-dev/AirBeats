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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.ui.component.AvatarDisplay

/**
 * Renders the user profile avatar with Apple iOS styling:
 * Automatically synchronizes with AvatarManager's persistent selection and falls back to
 * the signature Apple Memoji canvas if network is unavailable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleMemojiAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    avatarId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF141416))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x66FFFFFF), Color(0x22FFFFFF))
                ),
                shape = CircleShape
            )
            .then(
                if (onLongClick != null && onClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick,
                    )
                } else if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AvatarDisplay(
            size = size,
            showBorder = false,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "User Profile Avatar",
        )
    }
}
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

@Composable
private fun AppleMemojiFallback(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val width = size.toPx()
        val height = size.toPx()
        val center = Offset(width / 2f, height / 2f)

        // 1. Soft lavender-purple gradient background matching screenshot
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF9E9BD4), Color(0xFF726FA8)),
                center = center,
                radius = width / 2f
            )
        )

        // 2. Shoulders/Neck
        val skinColor = Color(0xFF4A2F1E)
        val skinHighlight = Color(0xFF5E3C27)
        val dreadColor = Color(0xFF1E1A17)
        val glassesColor = Color(0xFF121214)

        // Neck
        drawRect(
            color = skinColor,
            topLeft = Offset(width * 0.40f, height * 0.65f),
            size = Size(width * 0.20f, height * 0.25f)
        )

        // Head (Oval)
        drawOval(
            color = skinColor,
            topLeft = Offset(width * 0.26f, height * 0.26f),
            size = Size(width * 0.48f, height * 0.52f)
        )

        // Cheeks highlight
        drawCircle(
            color = skinHighlight,
            radius = width * 0.12f,
            center = Offset(width * 0.50f, height * 0.50f)
        )

        // 3. Dreadlocks / Hair (Top & Sides)
        // Top hair puffs
        drawOval(
            color = dreadColor,
            topLeft = Offset(width * 0.20f, height * 0.14f),
            size = Size(width * 0.60f, height * 0.30f)
        )
        // Left lock
        drawRoundRect(
            color = dreadColor,
            topLeft = Offset(width * 0.18f, height * 0.26f),
            size = Size(width * 0.14f, height * 0.42f),
            cornerRadius = CornerRadius(width * 0.07f, width * 0.07f)
        )
        // Right lock
        drawRoundRect(
            color = dreadColor,
            topLeft = Offset(width * 0.68f, height * 0.26f),
            size = Size(width * 0.14f, height * 0.42f),
            cornerRadius = CornerRadius(width * 0.07f, width * 0.07f)
        )

        // 4. Glasses (Apple Memoji signature)
        val frameStroke = Stroke(width = width * 0.045f)
        // Left lens
        drawRoundRect(
            color = glassesColor,
            topLeft = Offset(width * 0.30f, height * 0.44f),
            size = Size(width * 0.17f, height * 0.15f),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f),
            style = frameStroke
        )
        // Right lens
        drawRoundRect(
            color = glassesColor,
            topLeft = Offset(width * 0.53f, height * 0.44f),
            size = Size(width * 0.17f, height * 0.15f),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f),
            style = frameStroke
        )
        // Glasses bridge
        drawLine(
            color = glassesColor,
            start = Offset(width * 0.47f, height * 0.50f),
            end = Offset(width * 0.53f, height * 0.50f),
            strokeWidth = width * 0.04f
        )

        // Eyes inside glasses
        drawCircle(
            color = Color.White,
            radius = width * 0.025f,
            center = Offset(width * 0.38f, height * 0.51f)
        )
        drawCircle(
            color = Color.White,
            radius = width * 0.025f,
            center = Offset(width * 0.62f, height * 0.51f)
        )

        // Friendly smile
        val smilePath = Path().apply {
            moveTo(width * 0.42f, height * 0.66f)
            quadraticTo(
                width * 0.50f, height * 0.72f,
                width * 0.58f, height * 0.66f
            )
        }
        drawPath(
            path = smilePath,
            color = Color(0xFF2B1608),
            style = Stroke(width = width * 0.03f)
        )
    }
}
