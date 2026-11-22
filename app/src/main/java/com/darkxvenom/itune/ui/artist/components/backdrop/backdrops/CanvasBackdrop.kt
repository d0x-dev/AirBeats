/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
package com.darkxvenom.itune.ui.artist.components.backdrop.backdrops

// Adapted & maintained by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0 / Apache-2.0.

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.darkxvenom.itune.ui.artist.components.backdrop.Backdrop

@Composable
fun rememberCanvasBackdrop(
    onDraw: DrawScope.() -> Unit
): Backdrop {
    return remember(onDraw) {
        CanvasBackdrop(onDraw)
    }
}
// Integrated into iTune by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark

@Immutable
private class CanvasBackdrop(
    val onDraw: DrawScope.() -> Unit
) : Backdrop {

    override val isCoordinatesDependent: Boolean = false

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        onDraw()
    }
}
