package com.darkxvenom.airbeats.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily

/**
 * Apple-styled segmented switch with smooth sliding thumb and spring physics.
 */
@Composable
fun AppleSegmentedSwitch(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier.width(160.dp),
    height: Dp = 34.dp
) {
    val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.76f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "appleSegmentedSwitchThumb",
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(
                if (isDark) Color.White.copy(alpha = 0.12f)
                else Color.Black.copy(alpha = 0.08f)
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                shape = RoundedCornerShape(height / 2),
            )
            .padding(2.5.dp),
    ) {
        val segmentWidth = maxWidth / options.size.coerceAtLeast(1)

        // Sliding thumb
        Box(
            modifier = Modifier
                .offset(x = segmentWidth * animatedIndex)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape((height - 5.dp) / 2),
                    ambientColor = Color.Black.copy(alpha = 0.25f),
                    spotColor = Color.Black.copy(alpha = 0.25f),
                )
                .clip(RoundedCornerShape((height - 5.dp) / 2))
                .background(
                    if (isDark) Color.White
                    else MaterialTheme.colorScheme.surface
                ),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (option != selectedOption) {
                                onSelect(option)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = (-0.2).sp,
                        ),
                        color = if (isSelected) {
                            if (isDark) Color(0xFF1C1C1E)
                            else MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        },
                    )
                }
            }
        }
    }
}
