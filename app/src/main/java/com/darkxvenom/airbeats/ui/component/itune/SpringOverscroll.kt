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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * State holder for spring overscroll to allow external components (like loading indicators)
 * to observe current pull displacement and top overscroll condition.
 */
class SpringOverscrollState(
    val offset: Animatable<Float, AnimationVector1D> = Animatable(0f)
) {
    val value: Float get() = offset.value
    val isPullingTop: Boolean get() = offset.value > 10f
}
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

@Composable
fun rememberSpringOverscrollState(): SpringOverscrollState {
    return remember { SpringOverscrollState() }
}

/**
 * Creates a relaxed, slower, ultra-smooth Apple iOS-style spring rubber-band overscroll modifier.
 * Features unlocked deep travel distance, soft floaty recovery speed, and gentle organic settling.
 */
@Composable
fun Modifier.springOverscroll(
    state: SpringOverscrollState = rememberSpringOverscrollState()
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    val overscrollOffset = state.offset

    // Relaxed, slower spring physics: gentle floaty bounce with slower recovery speed
    val springDamping = 0.52f
    val springStiffness = 60f

    fun springBack(initialVel: Float = 0f) {
        if (overscrollOffset.value != 0f) {
            coroutineScope.launch {
                overscrollOffset.animateTo(
                    targetValue = 0f,
                    initialVelocity = initialVel.coerceIn(-350f, 350f),
                    animationSpec = spring(
                        dampingRatio = springDamping,
                        stiffness = springStiffness
                    )
                )
            }
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val current = overscrollOffset.value

                // If stretched at top and user pushes up: immediately consume with 1:1 responsive return
                if (current > 0f && available.y < 0f) {
                    val consumed = available.y * 0.95f
                    val next = (current + consumed).coerceAtLeast(0f)
                    coroutineScope.launch { overscrollOffset.snapTo(next) }
                    return Offset(0f, available.y)
                }

                // If stretched at bottom and user pulls down: immediately consume with 1:1 responsive return
                if (current < 0f && available.y > 0f) {
                    val consumed = available.y * 0.95f
                    val next = (current + consumed).coerceAtMost(0f)
                    coroutineScope.launch { overscrollOffset.snapTo(next) }
                    return Offset(0f, available.y)
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    val current = overscrollOffset.value
                    // Unlocked spacious travel distance with smooth progressive elasticity
                    val factor = 0.78f / (1f + (abs(current) / 1000f))
                    val newOffset = (current + available.y * factor).coerceIn(-1800f, 1800f)
                    coroutineScope.launch { overscrollOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                springBack(initialVel = (available.y * 0.10f).coerceIn(-350f, 350f))
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y != 0f) {
                    // Soft momentum impulse that transitions into a slow, relaxed bounce
                    val impulse = (available.y * 0.08f).coerceIn(-400f, 400f)
                    overscrollOffset.snapTo(impulse)
                    springBack(initialVel = (available.y * 0.15f).coerceIn(-400f, 400f))
                } else {
                    springBack()
                }
                return Velocity.Zero
            }
        }
    }

    return this
        .nestedScroll(nestedScrollConnection)
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                waitForUpOrCancellation()
                springBack()
            }
        }
        .graphicsLayer {
            translationY = overscrollOffset.value

            // Organic physical stretch scaling
            val stretch = abs(overscrollOffset.value)
            scaleY = 1f + (stretch / 4500f)
            scaleX = 1f + (stretch / 9000f)
            transformOrigin = if (overscrollOffset.value >= 0f) {
                TransformOrigin(0.5f, 0f)
            } else {
                TransformOrigin(0.5f, 1f)
            }
        }
}
