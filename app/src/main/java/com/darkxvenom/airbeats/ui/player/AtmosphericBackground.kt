package com.darkxvenom.airbeats.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * AtmosphericBackground — white paper theme.
 *
 * Previously: animated dark neon glow.
 * Now: clean off-white paper surface that the white player sits on.
 *
 * [dynamicColor] kept in the signature so all existing call sites compile
 * without change — it is no longer used in this theme.
 */
@Composable
fun AtmosphericBackground(
    modifier: Modifier = Modifier,
    dynamicColor: Color? = null          // kept for API compatibility
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))  // off-white paper
    )
}
