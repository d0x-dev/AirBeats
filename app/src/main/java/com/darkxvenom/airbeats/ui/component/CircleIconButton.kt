package com.darkxvenom.airbeats.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Animated version (used when scroll offset exists) with sleek glass styling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CircleIconButton(
    icon: Int,
    onClick: () -> Unit,
    scrollOffset: Float = 0f,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassBg = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val glassBorder = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val contentColor = if (isFrosted) {
        if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && isFrosted) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassCircleIconPress"
    )

    val bgModifier = if (isFrosted) {
        Modifier
            .background(glassBg.copy(alpha = (glassBg.alpha * (1f - (scrollOffset * 0.3f).coerceIn(0f, 0.3f)))))
            .border(BorderStroke(1.dp, glassBorder), CircleShape)
    } else {
        Modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                alpha = 1f - (scrollOffset * 0.3f).coerceIn(0f, 0.3f)
            )
        )
    }

    Box(
        modifier = modifier
            .size(52.dp - (8 * scrollOffset).dp)
            .scale(pressScale * (1f - (scrollOffset * 0.1f)))
            .clip(CircleShape)
            .then(bgModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp - (4 * scrollOffset).dp).then(iconModifier),
            tint = contentColor
        )
    }
}

/**
 * Normal version (default use) with sleek glass styling matching New Classic top buttons
 */
@Composable
fun CircleIconButton(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassBg = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val glassBorder = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val contentColor = if (isFrosted) {
        if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && isFrosted) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glassCircleIconPress"
    )

    val bgModifier = if (isFrosted) {
        Modifier
            .background(glassBg)
            .border(BorderStroke(1.dp, glassBorder), CircleShape)
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
    }

    Box(
        modifier = modifier
            .size(52.dp)
            .scale(pressScale)
            .clip(CircleShape)
            .then(bgModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp).then(iconModifier),
            tint = contentColor
        )
    }
}
