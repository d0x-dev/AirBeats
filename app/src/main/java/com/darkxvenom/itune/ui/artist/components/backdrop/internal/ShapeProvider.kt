/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
package com.darkxvenom.itune.ui.artist.components.backdrop.internal

// Adapted & maintained by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0 / Apache-2.0.

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@Immutable
internal class ShapeProvider(val shapeBlock: () -> Shape) {

    private var _shape: Shape? = null
    private var _outline: Outline? = null
    private var _size: Size = Size.Unspecified
    private var _layoutDirection: LayoutDirection? = null
    private var _density: Float? = null
    // Integrated into iTune by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark

    val innerShape
        get() = shapeBlock()

    val shape = object : Shape {

        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            val shape = shapeBlock()
            if (_shape != shape) {
                _shape = shape
                _outline = null
            }
            if (_outline == null || _size != size || _layoutDirection != layoutDirection || _density != density.density) {
                _size = size
                _layoutDirection = layoutDirection
                _density = density.density
                _outline = shape.createOutline(size, layoutDirection, density)
            }

            return _outline!!
        }
    }
}
