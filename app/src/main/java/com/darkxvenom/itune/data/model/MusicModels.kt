/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of iTune and is licensed under the GNU General Public License v3.0 (GPL-3.0).
 * Any copying, modification, or redistribution of this code must strictly comply with the
 * terms of the project LICENSE file and retain full author credit.
 */

package com.darkxvenom.itune.data.model

import androidx.compose.ui.graphics.Color

enum class CoverArtType {
    DIS_BADMAN,
    VICTORY_LAP,
    ESPIRITU,
    SCHIZO,
    BLINDING_LIGHTS,
    PAINT_THE_TOWN,
    STARBOY,
    VAMPIRE,
    TODAYS_HITS_HERO,
    NEVER_ENOUGH_HERO,
    ALIST_POP_HERO
}

data class FeaturedCard(
    val id: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val caption: String,
    val heroType: CoverArtType,
    val accentColor: Color = Color(0xFFFFD159),
    val gradientColors: List<Color> = listOf(Color(0xFF2E2922), Color(0xFF151412), Color(0xFF0A0A0A)),
    val imageUrl: String? = null,
    val destinationBrowseId: String? = null,
    val destinationIsAlbum: Boolean = false
)
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val isExplicit: Boolean = false,
    val coverType: CoverArtType,
    val duration: String = "3:24"
)

data class Playlist(
    val id: String,
    val title: String,
    val curator: String,
    val description: String,
    val gradientColors: List<Color>
)

data class RadioStation(
    val id: String,
    val name: String,
    val host: String,
    val showName: String,
    val isLive: Boolean = true,
    val badgeColor: Color = Color(0xFFFA2D48),
    val backgroundGradient: List<Color>
)
