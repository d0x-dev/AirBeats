package com.darkxvenom.airbeats.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.constants.DynamicBackgroundKey
import com.darkxvenom.airbeats.ui.player.FluidBackground
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.rememberPreference

@Composable
fun BlurredBackground(
    model: Any?,
    modifier: Modifier = Modifier,
    blurRadius: androidx.compose.ui.unit.Dp = 90.dp
) {
    if (model != null) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
                    .fillMaxSize()
                    .blur(blurRadius)
            )
        } else {
            FluidBackground(modifier = modifier.fillMaxSize())
        }
    } else {
        LibraryMeshBackground(modifier = modifier)
    }
}

/**
 * Renders the exact background from the Library screen
 * used when no song is playing.
 */
@Composable
fun LibraryMeshBackground(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) Color(0xFF050505) else Color(0xFFF9F9F9)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        val color1 = MaterialTheme.colorScheme.primary
        val color2 = MaterialTheme.colorScheme.secondary
        val color3 = MaterialTheme.colorScheme.tertiary
        val color4 = MaterialTheme.colorScheme.primaryContainer
        val color5 = MaterialTheme.colorScheme.secondaryContainer

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize(0.7f)
                .drawWithCache {
                    val width = size.width
                    val height = size.height

                    val brush1 = Brush.radialGradient(
                        colors = listOf(color1.copy(alpha = 0.38f), color1.copy(alpha = 0.24f), color1.copy(alpha = 0.14f), color1.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(width * 0.15f, height * 0.1f),
                        radius = width * 0.55f,
                    )
                    val brush2 = Brush.radialGradient(
                        colors = listOf(color2.copy(alpha = 0.34f), color2.copy(alpha = 0.2f), color2.copy(alpha = 0.11f), color2.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(width * 0.85f, height * 0.2f),
                        radius = width * 0.65f,
                    )
                    val brush3 = Brush.radialGradient(
                        colors = listOf(color3.copy(alpha = 0.3f), color3.copy(alpha = 0.17f), color3.copy(alpha = 0.09f), color3.copy(alpha = 0.04f), Color.Transparent),
                        center = Offset(width * 0.3f, height * 0.45f),
                        radius = width * 0.6f,
                    )
                    val brush4 = Brush.radialGradient(
                        colors = listOf(color4.copy(alpha = 0.26f), color4.copy(alpha = 0.14f), color4.copy(alpha = 0.08f), color4.copy(alpha = 0.03f), Color.Transparent),
                        center = Offset(width * 0.7f, height * 0.5f),
                        radius = width * 0.7f,
                    )
                    val brush5 = Brush.radialGradient(
                        colors = listOf(color5.copy(alpha = 0.22f), color5.copy(alpha = 0.12f), color5.copy(alpha = 0.06f), color5.copy(alpha = 0.02f), Color.Transparent),
                        center = Offset(width * 0.5f, height * 0.75f),
                        radius = width * 0.8f,
                    )
                    val overlayBrush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = 0.22f), surfaceColor.copy(alpha = 0.55f), surfaceColor),
                        startY = height * 0.4f,
                        endY = height,
                    )

                    onDrawBehind {
                        drawRect(brush1)
                        drawRect(brush2)
                        drawRect(brush3)
                        drawRect(brush4)
                        drawRect(brush5)
                        drawRect(overlayBrush)
                    }
                }
        )
    }
}

/**
 * Adaptive background for all main screens (Home, Explore, Settings, About, Appearance, Stats, etc.):
 * - When a song is playing (artworkUrl != null): displays the blurred song thumbnail with smooth overlay.
 * - When no song is playing (artworkUrl == null): displays the Library screen mesh background.
 */
@Composable
fun ScreenAdaptiveBackground(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    val isLite = com.darkxvenom.airbeats.utils.isLiteModeActive()
    val (dynamicBackground, _) = rememberPreference(DynamicBackgroundKey, defaultValue = true)

    if (isLite || !dynamicBackground) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    Crossfade(
        targetState = artworkUrl?.takeIf { it.isNotBlank() },
        animationSpec = tween(500),
        modifier = modifier.fillMaxSize(),
        label = "ScreenAdaptiveBackgroundCrossfade"
    ) { currentArtworkUrl ->
        if (currentArtworkUrl != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                BlurredBackground(
                    model = currentArtworkUrl.highQualityThumbnail()
                )

                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val overlayBrush = if (isDarkTheme) {
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayBrush)
                )
            }
        } else {
            LibraryMeshBackground()
        }
    }
}
