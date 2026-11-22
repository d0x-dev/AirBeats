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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TopHeaderBar(
    modifier: Modifier = Modifier,
    displayName: String = "",
    showBrandLogoAndTitle: Boolean = true,
    onBrandBoundsChanged: ((iconBounds: Rect, titleBounds: Rect) -> Unit)? = null,
    onProfileClick: () -> Unit = {},
    onProfileLongClick: (() -> Unit)? = null,
) {

    var iconBounds by remember { mutableStateOf<Rect?>(null) }
    var titleBounds by remember { mutableStateOf<Rect?>(null) }

    fun publishBounds() {
        val ib = iconBounds
        val tb = titleBounds
        if (ib != null && tb != null) {
            onBrandBoundsChanged?.invoke(ib, tb)
        }
    }

    // Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(44.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top Left: The requested ic_itune_waves.xml icon & brand/festival text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f, fill = false)
                .graphicsLayer {
                    alpha = if (showBrandLogoAndTitle) 1f else 0f
                },
        ) {
            // Existing XML icon with subtle matching glow when festival active (never hidden)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .onGloballyPositioned { coords ->
                        iconBounds = coords.boundsInRoot()
                        publishBounds()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "♫",
                    color = Color.White,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "iTune",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                style = LocalTextStyle.current,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.onGloballyPositioned { coords ->
                    titleBounds = coords.boundsInRoot()
                    publishBounds()
                },
            )
        }

        // Top Right: Personalized name pill + Apple-style profile icon
        val cleanName = displayName.trim()
        if (cleanName.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF141417))
                    .border(0.6.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                    .then(
                        if (onProfileLongClick != null) {
                            Modifier.combinedClickable(
                                onClick = onProfileClick,
                                onLongClick = onProfileLongClick,
                            )
                        } else {
                            Modifier.clickable(onClick = onProfileClick)
                        }
                    )
                    .padding(start = 12.dp, end = 3.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = cleanName,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 136.dp),
                )
                AppleMemojiAvatar(
                    size = 32.dp,
                    onClick = onProfileClick,
                    onLongClick = onProfileLongClick,
                )
            }
        } else {
            AppleMemojiAvatar(
                size = 38.dp,
                avatarId = resolvedAvatarId,
                onClick = onProfileClick,
                onLongClick = onProfileLongClick,
            )
        }
    }
}
