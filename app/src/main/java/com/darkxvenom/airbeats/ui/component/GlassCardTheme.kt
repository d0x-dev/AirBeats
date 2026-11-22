package com.darkxvenom.airbeats.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.constants.FrostedGlassCardsButtonsKey
import com.darkxvenom.airbeats.utils.isLiteModeActive
import com.darkxvenom.airbeats.utils.rememberPreference

/**
 * Checks whether global frosted glass styling for cards, buttons, and popups is enabled.
 * Defaults to true, but disabled in Lite mode.
 */
@Composable
fun isFrostedGlassUiEnabled(): Boolean {
    if (com.darkxvenom.airbeats.utils.isBlurDisabled()) return false
    val (enabled) = rememberPreference(FrostedGlassCardsButtonsKey, defaultValue = true)
    return enabled
}

/**
 * Resolves standard card container color based on whether frosted glass UI is active.
 */
@Composable
fun settingsCardContainerColor(frosted: Boolean = isFrostedGlassUiEnabled()): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (frosted) {
        if (isDark) {
            Color.White.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        }
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
    }
}

/**
 * Resolves popup and bottom sheet glass container color with enhanced opacity and depth
 * to prevent background content bleeding through while preserving the frosted aesthetic.
 */
@Composable
fun popupGlassContainerColor(frosted: Boolean = isFrostedGlassUiEnabled()): Color {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (frosted) {
        if (isDark) {
            Color(0xFF16161B).copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
        }
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
}

/**
 * Resolves card border stroke based on whether frosted glass UI is active.
 */
@Composable
fun settingsCardBorder(frosted: Boolean = isFrostedGlassUiEnabled()): BorderStroke? {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (frosted) {
        BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        )
    } else {
        null
    }
}

/**
 * Unified Card container that automatically switches between frosted glass styling
 * and the default opaque Material 3 container.
 */
@Composable
fun SettingsGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = settingsCardContainerColor(),
    border: BorderStroke? = settingsCardBorder(),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}
