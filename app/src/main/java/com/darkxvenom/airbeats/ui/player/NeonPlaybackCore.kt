package com.darkxvenom.airbeats.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * NeonPlaybackCore is a legacy component that is not used in the active layout.
 * We keep it as a simple placeholder Box to prevent compile errors on unused references.
 */
@Composable
fun NeonPlaybackCore(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier)
}
