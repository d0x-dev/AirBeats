package com.darkxvenom.airbeats.ui.component

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

val FADE_RUN = 56.dp
private const val PEAK = 0.75f
private const val SCRIM_PEAK = 0.42f
private const val SCRIM_STOPS = 12

@Composable
fun defaultTopFadeBlurHeight(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp + FADE_RUN

/**
 * Progressive top blur effect with smooth vertical fade.
 * Blends scrolling content smoothly into the top area without hard seams or milky white color cast.
 */
@OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalHazeApi::class)
@Composable
fun TopFadeBlur(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    pageColor: Color = Color.Transparent,
    scrimColor: Color = Color.Transparent,
    height: Dp = defaultTopFadeBlurHeight(),
    alpha: Float = 1f,
) {
    if (com.darkxvenom.airbeats.utils.isBlurDisabled() || alpha <= 0.001f) return

    val hazeStyle = if (pageColor == Color.Transparent || pageColor.alpha <= 0.001f) {
        dev.chrisbanes.haze.HazeStyle(
            backgroundColor = Color.Transparent,
            tints = emptyList(),
            blurRadius = 24.dp,
            noiseFactor = 0f,
        )
    } else {
        HazeMaterials.ultraThin(pageColor)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { this.alpha = alpha }
            .hazeEffect(
                state = hazeState,
                style = hazeStyle,
            ) {
                inputScale = HazeInputScale.Fixed(0.33f)
                progressive = HazeProgressive.verticalGradient(
                    easing = EaseOutCubic,
                    startIntensity = PEAK,
                    endIntensity = 0f,
                )
                noiseFactor = 0f
            },
    )

    if (scrimColor != Color.Transparent && scrimColor.alpha > 0.001f) {
        val scrim = remember(scrimColor) {
            Brush.verticalGradient(
                colorStops = Array(SCRIM_STOPS) { i ->
                    val t = i / (SCRIM_STOPS - 1f)
                    t to scrimColor.copy(alpha = SCRIM_PEAK * (1f - EaseOutCubic.transform(t)))
                },
            )
        }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer { this.alpha = alpha }
                .background(scrim),
        )
    }
}
