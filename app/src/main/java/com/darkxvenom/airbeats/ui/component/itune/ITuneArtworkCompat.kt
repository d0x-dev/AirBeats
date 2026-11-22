/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 * Originally created by DxV Studio ~~ Dark. Credits: DxV Studio, AirBeats, and Dark.
 * Licensed under GPL-3.0. Retain the original attribution and license terms.
 */
package com.darkxvenom.airbeats.ui.component.itune

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkxvenom.itune.data.model.CoverArtType
import com.darkxvenom.itune.ui.components.AlbumArtwork as OriginalAlbumArtwork

/** Preserves the iTune artwork API while keeping it scoped to the AirBeats UI port. */
@Composable
fun AlbumArtwork(
    type: CoverArtType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 8.dp,
) {
    OriginalAlbumArtwork(type = type, modifier = modifier, size = size, cornerRadius = cornerRadius)
}
