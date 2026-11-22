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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.darkxvenom.itune.R
import com.darkxvenom.itune.data.account.MultiAccountManager
import com.darkxvenom.itune.data.festival.FestivalManager
import com.darkxvenom.itune.data.repository.AvatarManager
import com.darkxvenom.itune.ui.theme.AppleTextPrimary
import kotlinx.coroutines.delay

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
    val activeAccount by MultiAccountManager.activeAccount.collectAsStateWithLifecycle()
    val resolvedAvatarId = activeAccount?.avatarId ?: AvatarManager.currentAvatarId.value
    val festivalConfig by FestivalManager.config.collectAsStateWithLifecycle()
    val festivalActive = festivalConfig.enabled && festivalConfig.color != Color.Unspecified && festivalConfig.text.isNotBlank()
    val festivalColor = festivalConfig.color

    var iconBounds by remember { mutableStateOf<Rect?>(null) }
    var titleBounds by remember { mutableStateOf<Rect?>(null) }

    fun publishBounds() {
        val ib = iconBounds
        val tb = titleBounds
        if (ib != null && tb != null) {
            onBrandBoundsChanged?.invoke(ib, tb)
        }
    }

    var displayedText by remember(festivalActive, festivalConfig.text) {
        mutableStateOf("iTune")
    }

    // Execute the festival animation sequence only after the splash-to-top-bar transition has finished
    // Keeps typing and erasing iTune and the festival text infinitely while festival mode is active.
    // The ic_itune_waves XML icon is NEVER hidden once reached.
    LaunchedEffect(showBrandLogoAndTitle, festivalActive, festivalConfig.text) {
        if (!showBrandLogoAndTitle) return@LaunchedEffect

        if (!festivalActive) {
            displayedText = "iTune"
            return@LaunchedEffect
        }

        while (true) {
            val festText = festivalConfig.text.trim().ifEmpty { "iTune" }

            // 1. Display iTune in JSON colour with subtle glow
            displayedText = "iTune"
            delay(2000L)

            // 2. Delete iTune character by character
            for (i in ("iTune".length - 1) downTo 0) {
                displayedText = "iTune".substring(0, i)
                delay(60L)
            }
            displayedText = ""
            delay(180L)

            // 3. Type the Festival Message character by character
            for (i in 1..festText.length) {
                displayedText = festText.substring(0, i)
                delay(75L)
            }
            delay(2400L)

            // 4. Clear the Festival Message character by character
            for (i in (festText.length - 1) downTo 0) {
                displayedText = festText.substring(0, i)
                delay(50L)
            }
            displayedText = ""
            delay(180L)

            // 5. Type iTune again character by character
            for (i in 1.."iTune".length) {
                displayedText = "iTune".substring(0, i)
                delay(75L)
            }
            delay(200L)
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
                    .drawBehind {
                        if (festivalActive) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        festivalColor.copy(alpha = 0.40f),
                                        festivalColor.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.maxDimension * 0.75f
                                )
                            )
                        }
                    }
                    .onGloballyPositioned { coords ->
                        iconBounds = coords.boundsInRoot()
                        publishBounds()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_itune_waves),
                    contentDescription = "iTune Brand Waves",
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(if (festivalActive) festivalColor else Color.White)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = displayedText,
                color = if (festivalActive) festivalColor else AppleTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                style = if (festivalActive) {
                    LocalTextStyle.current.copy(
                        shadow = Shadow(
                            color = festivalColor.copy(alpha = 0.52f),
                            offset = Offset(0f, 0f),
                            blurRadius = 14f
                        )
                    )
                } else {
                    LocalTextStyle.current
                },
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
                    avatarId = resolvedAvatarId,
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
