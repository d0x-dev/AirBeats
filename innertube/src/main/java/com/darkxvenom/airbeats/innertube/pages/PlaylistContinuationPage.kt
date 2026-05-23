package com.darkxvenom.airbeats.innertube.pages

import com.darkxvenom.airbeats.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
