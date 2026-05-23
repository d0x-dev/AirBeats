package com.darkxvenom.airbeats.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.luminance

@Composable
fun CurvedBottomNavigationBar(
    modifier: Modifier = Modifier,
    items: List<CurvedBottomNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = surfaceColor.luminance() < 0.5f

    val glassBackgroundBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f), // Top highlight
                    Color(0xFF121212).copy(alpha = 0.75f),
                    Color(0xFF000000).copy(alpha = 0.85f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.55f)
                )
            )
        }
    }

    val rimBorderBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White,
                    Color.White.copy(alpha = 0.5f)
                )
            )
        }
    }

    val unselectedIconColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.7f)
    } else {
        Color.Black.copy(alpha = 0.5f)
    }

    val indicatorBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.2f),
                    Color(0xFF222222),
                    Color(0xFF111111)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFEEEEEE),
                    Color(0xFFDDDDDD)
                )
            )
        }
    }

    val indicatorBorderColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.3f)
    } else {
        Color.White.copy(alpha = 0.9f)
    }
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth.value
        val itemWidth = width / items.size

        // Animate the offset of the cutout based on selectedIndex
        val animatedOffset by animateFloatAsState(
            targetValue = (itemWidth * selectedIndex) + (itemWidth / 2f),
            animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "offset"
        )

        // Draw the curved background with liquid glass effect
        val density = LocalDensity.current
        val barShape = CurvedBottomBarShape(
            cutoutCenterX = with(density) { animatedOffset.dp.toPx() },
            cutoutCenterY = with(density) { 24.dp.toPx() },
            cutoutRadius = with(density) { 30.dp.toPx() }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    shape = barShape
                    clip = true
                    shadowElevation = 12.dp.toPx()
                }
                .background(brush = glassBackgroundBrush)
                .border(
                    width = 1.dp,
                    brush = rimBorderBrush,
                    shape = barShape
                )
        )

        // Draw the floating circle (indicator)
        Box(
            modifier = Modifier
                .offset(x = (animatedOffset - 24).dp, y = 0.dp)
                .size(48.dp)
                .graphicsLayer {
                    shadowElevation = 8.dp.toPx()
                    shape = CircleShape
                    clip = true
                }
                .background(brush = indicatorBrush)
                .border(
                    width = 1.dp,
                    color = indicatorBorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // The selected icon
            Icon(
                painter = painterResource(id = items[selectedIndex].iconActive),
                contentDescription = null,
                tint = Color(0xFFF9A825), // Amber/Orange to match screenshot
                modifier = Modifier.size(24.dp)
            )
        }

        // Draw the unselected icons in a row
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (index != selectedIndex) {
                            Icon(
                                painter = painterResource(id = item.iconInactive),
                                contentDescription = null,
                                tint = unselectedIconColor,
                                modifier = Modifier.size(24.dp)
                            )
                    }
                }
            }
        }
    }
}

data class CurvedBottomNavigationItem(
    val iconInactive: Int,
    val iconActive: Int,
    val titleId: Int = 0
)

class CurvedBottomBarShape(
    private val cutoutCenterX: Float,
    private val cutoutCenterY: Float,
    private val cutoutRadius: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cornerRadius = size.height / 2f // Proper pill shape

        val pillPath = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
                )
            )
        }

        val cutoutPath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    center = androidx.compose.ui.geometry.Offset(cutoutCenterX, cutoutCenterY),
                    radius = cutoutRadius
                )
            )
        }

        val resultPath = Path()
        resultPath.op(pillPath, cutoutPath, androidx.compose.ui.graphics.PathOperation.Difference)

        return Outline.Generic(resultPath)
    }
}
