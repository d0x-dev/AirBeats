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

import android.os.Build
import androidx.compose.ui.zIndex
import com.darkxvenom.itune.ui.artist.components.backdrop.Backdrop
import com.darkxvenom.itune.ui.artist.components.backdrop.backdrops.layerBackdrop
import com.darkxvenom.itune.ui.artist.components.backdrop.backdrops.rememberCombinedBackdrop
import com.darkxvenom.itune.ui.artist.components.backdrop.backdrops.rememberLayerBackdrop
import com.darkxvenom.itune.ui.artist.components.backdrop.drawBackdrop
import com.darkxvenom.itune.ui.artist.components.backdrop.effects.colorControls
import com.darkxvenom.itune.ui.artist.components.backdrop.effects.lens
import com.darkxvenom.itune.ui.artist.components.backdrop.highlight.Highlight
import com.darkxvenom.itune.ui.artist.components.backdrop.shadow.Shadow
import com.darkxvenom.itune.ui.artist.components.liquidGlass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkxvenom.itune.R
import com.darkxvenom.itune.ui.theme.AppleGlassBorder
import com.darkxvenom.itune.ui.theme.AppleGlassDock
import com.darkxvenom.itune.ui.theme.AppleMusicRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class NavTab {
    HOME,
    NEW,
    RADIO,
    LIBRARY
}
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

@Composable
fun FloatingNavBar(
    selectedTab: NavTab?,
    isSearchActive: Boolean = false,
    onTabSelected: (NavTab) -> Unit,
    onSearchClick: () -> Unit,
    liquidGlassBackdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val dockHeight = 58.dp
    val containerHeight = 66.dp

    val density = LocalDensity.current

    // Liquid glass mode: the dock and search button are real glass, and the hold-bubble is a lens that
    // refracts and magnifies what is under it (the app content AND the tab icons).
    val glass = liquidGlassBackdrop != null
    val tabsBackdrop = rememberLayerBackdrop()
    val bubbleBackdrop = rememberCombinedBackdrop(liquidGlassBackdrop ?: tabsBackdrop, tabsBackdrop)
    val lensHeightPx = with(density) { 16.dp.toPx() }
    val lensAmountPx = with(density) { 30.dp.toPx() }
    val coroutineScope = rememberCoroutineScope()

    val currentSelectedTab by rememberUpdatedState(selectedTab)
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)

    var dockWidthPx by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var dragFingerX by remember { mutableFloatStateOf(0f) }
    var hoveredIndex by remember { mutableIntStateOf(selectedTab?.ordinal ?: 0) }

    LaunchedEffect(selectedTab, isHolding, isSearchActive) {
        if (!isHolding && selectedTab != null) {
            hoveredIndex = selectedTab.ordinal
        }
    }

    // Dynamic morphing:
    // 1. Normal resting shade: subtle inside-dock oval capsule (56dp x 42dp) with red text/icon
    // 2. Tap-and-hold glassy bubble: sleek, compact, organic liquid lens sized to fit comfortably inside the dock
    val currentBubbleWidthDp by animateDpAsState(
        targetValue = if (isHolding) 66.dp else 56.dp,
        animationSpec = tween(durationMillis = 160),
        label = "BubbleWidth"
    )

    val currentBubbleHeightDp by animateDpAsState(
        targetValue = if (isHolding) 48.dp else 42.dp,
        animationSpec = tween(durationMillis = 160),
        label = "BubbleHeight"
    )

    val holdingProgress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "HoldingProgress"
    )

    val tabCount = 4
    val slotWidthPx = if (dockWidthPx > 0f) dockWidthPx / tabCount else 0f
    val bubbleWidthPx = with(density) { currentBubbleWidthDp.toPx() }

    val activeSlot = if (isHolding) hoveredIndex else (selectedTab?.ordinal ?: 0)
    val slotCenterX = if (slotWidthPx > 0f) activeSlot * slotWidthPx + slotWidthPx / 2f else 0f
    val restingX = slotCenterX - bubbleWidthPx / 2f

    val targetBubbleX = if (isHolding) {
        (dragFingerX - bubbleWidthPx / 2f).coerceIn(0f, dockWidthPx - bubbleWidthPx)
    } else {
        restingX
    }

    val currentBubbleX by animateFloatAsState(
        targetValue = targetBubbleX,
        animationSpec = if (isHolding) {
            spring(dampingRatio = 0.85f, stiffness = 750f)
        } else {
            spring(dampingRatio = 0.65f, stiffness = 380f)
        },
        label = "BubbleX"
    )

    val targetAlpha = when {
        isSearchActive -> 0.0f
        selectedTab != null || isHolding -> 1.0f
        else -> 0.0f
    }

    val bubbleAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 150),
        label = "BubbleAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 4.dp)
            .height(containerHeight)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Main Navigation Dock Area
        Box(
            modifier = Modifier
                .weight(1f)
                .height(containerHeight)
                .onSizeChanged { size ->
                    dockWidthPx = size.width.toFloat()
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val sWidth = size.width.toFloat() / tabCount
                        val startSlot = (down.position.x / sWidth).toInt().coerceIn(0, tabCount - 1)

                        dragFingerX = down.position.x
                        hoveredIndex = startSlot

                        var isHoldActive = false
                        var isDragActive = false

                        // Hold timer (260ms). If user lifts finger before 260ms, it is a normal TAP:
                        // holdJob is cancelled, isHolding remains FALSE, glassy bubble NEVER appears!
                        val holdJob = coroutineScope.launch {
                            delay(260)
                            isHoldActive = true
                            isHolding = true
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                // FINGER LIFTED (RELEASED)
                                holdJob.cancel()
                                if (isHoldActive || isDragActive) {
                                    // User held/slid and released: select destination tab, immediately convert back to normal shade
                                    val finalSlot = (dragFingerX / sWidth).toInt().coerceIn(0, tabCount - 1)
                                    isHolding = false
                                    hoveredIndex = finalSlot
                                    currentOnTabSelected(NavTab.entries[finalSlot])
                                } else {
                                    // Simple one-tap: IMMEDIATELY select tab, stay in normal resting shade!
                                    isHolding = false
                                    hoveredIndex = startSlot
                                    currentOnTabSelected(NavTab.entries[startSlot])
                                }
                                change.consume()
                                break
                            }

                            // Pointer moving
                            val moveDistance = abs(change.position.x - down.position.x)
                            if (moveDistance > 12f) {
                                isDragActive = true
                                dragFingerX = change.position.x
                                val currentSlot = (change.position.x / sWidth).toInt().coerceIn(0, tabCount - 1)
                                hoveredIndex = currentSlot

                                if (!isHoldActive) {
                                    holdJob.cancel()
                                    isHoldActive = true
                                    isHolding = true
                                }
                                change.consume()
                            }
                        }

                        // Guaranteed safety cleanup on any pointer exit
                        holdJob.cancel()
                        isHolding = false
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Layer 1: Dock Capsule Background (58.dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dockHeight)
                    .align(Alignment.Center)
                    .then(
                        if (glass) {
                            Modifier.liquidGlass(RoundedCornerShape(percent = 50), backdropOverride = liquidGlassBackdrop)
                        } else {
                            Modifier
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(percent = 50),
                                    ambientColor = Color.Black,
                                    spotColor = Color.Black
                                )
                                .background(AppleGlassDock, shape = RoundedCornerShape(percent = 50))
                                .border(0.75.dp, AppleGlassBorder, shape = RoundedCornerShape(percent = 50))
                        }
                    )
            )

            // Layer 2: Selected Indicator (Normal resting shade by default; sleek glassy bubble ONLY on hold)
            if (dockWidthPx > 0f && (selectedTab != null || isHolding)) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(currentBubbleX.roundToInt(), 0) }
                        .align(Alignment.CenterStart)
                        .zIndex(if (glass && holdingProgress > 0.01f) 1f else 0f)
                        .width(currentBubbleWidthDp)
                        .height(currentBubbleHeightDp)
                        .graphicsLayer {
                            alpha = bubbleAlpha
                        }
                        .then(
                            if (glass && holdingProgress > 0.01f) {
                                // Water-bubble lens: refracts + magnifies the content under it while you hold and drag.
                                Modifier.drawBackdrop(
                                    backdrop = bubbleBackdrop,
                                    shape = { RoundedCornerShape(percent = 50) },
                                    effects = {
                                        colorControls(saturation = 1.35f)
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            lens(
                                                refractionHeight = lensHeightPx,
                                                refractionAmount = lensAmountPx,
                                                depthEffect = true,
                                                chromaticAberration = true,
                                            )
                                        }
                                    },
                                    highlight = { Highlight.Default.copy(alpha = holdingProgress) },
                                    shadow = { Shadow(alpha = holdingProgress) },
                                    layerBlock = {
                                        scaleX = 1f + 0.10f * holdingProgress
                                        scaleY = 1f + 0.10f * holdingProgress
                                    },
                                    onDrawSurface = { drawRect(Color.White.copy(alpha = 0.05f * holdingProgress)) },
                                )
                            } else {
                                Modifier
                                    .shadow(
                                        elevation = (12.dp * holdingProgress),
                                        shape = RoundedCornerShape(percent = 50),
                                        ambientColor = Color.Black.copy(alpha = 0.50f * holdingProgress),
                                        spotColor = Color.Black.copy(alpha = 0.70f * holdingProgress)
                                    )
                                    .clip(RoundedCornerShape(percent = 50))
                                    // Resting Normal Shade (subtle dark oval)
                                    .background(
                                        Color.White.copy(alpha = 0.12f * (1f - holdingProgress * 0.4f)),
                                        shape = RoundedCornerShape(percent = 50)
                                    )
                            }
                        )
                ) {
                    // Glassy Water Droplet Overlay (Comes alive ONLY on tap-and-hold, height 48dp)
                    if (!glass && holdingProgress > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = holdingProgress }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.28f),
                                            Color.White.copy(alpha = 0.10f),
                                            Color(0x35000000),
                                            Color.White.copy(alpha = 0.15f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(percent = 50)
                                )
                                .border(
                                    width = 1.1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.85f),
                                            Color.White.copy(alpha = 0.45f),
                                            Color.White.copy(alpha = 0.18f),
                                            Color.White.copy(alpha = 0.38f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(percent = 50)
                                )
                        ) {
                            // Top Specular Lens Glare Arc
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .align(Alignment.TopCenter)
                                    .width(32.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.45f),
                                                Color.White.copy(alpha = 0.0f)
                                            )
                                        )
                                    )
                            )

                            // Bottom Ambient Meniscus Light Bounce
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 2.dp)
                                    .align(Alignment.BottomCenter)
                                    .width(24.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.0f),
                                                Color.White.copy(alpha = 0.22f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            // Layer 3: Tabs Row (Red on normal shade, White inside glassy bubble when holding)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dockHeight)
                    .align(Alignment.Center)
                    .then(if (glass) Modifier.layerBackdrop(tabsBackdrop) else Modifier),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppleNavTabItem(
                    label = "Home",
                    iconRes = R.drawable.ic_nav_home,
                    isHighlighted = !isSearchActive && (if (isHolding) hoveredIndex == 0 else selectedTab == NavTab.HOME),
                    isHolding = isHolding,
                    modifier = Modifier.weight(1f)
                )

                AppleNavTabItem(
                    label = "New",
                    iconRes = R.drawable.ic_nav_new,
                    isHighlighted = !isSearchActive && (if (isHolding) hoveredIndex == 1 else selectedTab == NavTab.NEW),
                    isHolding = isHolding,
                    modifier = Modifier.weight(1f)
                )

                AppleNavTabItem(
                    label = "Radio",
                    iconRes = R.drawable.ic_nav_radio,
                    isHighlighted = !isSearchActive && (if (isHolding) hoveredIndex == 2 else selectedTab == NavTab.RADIO),
                    isHolding = isHolding,
                    modifier = Modifier.weight(1f)
                )

                AppleNavTabItem(
                    label = "Library",
                    iconRes = R.drawable.ic_nav_library,
                    isHighlighted = !isSearchActive && (if (isHolding) hoveredIndex == 3 else selectedTab == NavTab.LIBRARY),
                    isHolding = isHolding,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Circular Apple Search Button
        Box(
            modifier = Modifier
                .size(dockHeight)
                .then(
                    if (glass) {
                        Modifier
                            .liquidGlass(CircleShape, backdropOverride = liquidGlassBackdrop)
                            .then(
                                if (isSearchActive) Modifier.border(0.75.dp, AppleMusicRed.copy(alpha = 0.60f), CircleShape) else Modifier
                            )
                    } else {
                        Modifier
                            .shadow(
                                elevation = 16.dp,
                                shape = CircleShape,
                                ambientColor = if (isSearchActive) AppleMusicRed.copy(alpha = 0.35f) else Color.Black,
                                spotColor = if (isSearchActive) AppleMusicRed.copy(alpha = 0.35f) else Color.Black
                            )
                            .clip(CircleShape)
                            .background(
                                if (isSearchActive) Color(0x38FFFFFF) else AppleGlassDock
                            )
                            .border(
                                width = 0.75.dp,
                                color = if (isSearchActive) AppleMusicRed.copy(alpha = 0.60f) else AppleGlassBorder,
                                shape = CircleShape
                            )
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSearchClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_search),
                    contentDescription = "Search",
                    tint = if (isSearchActive) AppleMusicRed else Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(if (isSearchActive) 20.dp else 23.dp)
                )
                if (isSearchActive) {
                    Spacer(modifier = Modifier.height(1.dp))
                    @Suppress("DEPRECATION")
                    Text(
                        text = "Search",
                        color = AppleMusicRed,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleNavTabItem(
    label: String,
    iconRes: Int,
    isHighlighted: Boolean,
    isHolding: Boolean,
    modifier: Modifier = Modifier
) {
    val targetColor = when {
        isHighlighted && isHolding -> Color.White
        isHighlighted -> AppleMusicRed
        else -> Color.White.copy(alpha = 0.85f)
    }

    val contentColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 140),
        label = "TabContentColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            @Suppress("DEPRECATION")
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = (-0.2).sp,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                )
            )
        }
    }
}
