package com.darkxvenom.airbeats.innertube.pages

import com.darkxvenom.airbeats.innertube.models.AlbumItem

data class ExplorePage(
    val newReleaseAlbums: List<AlbumItem>,
    val moodAndGenres: List<MoodAndGenres.Item>,
)
