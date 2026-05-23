package com.darkxvenom.airbeats.innertube.pages

import com.darkxvenom.airbeats.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
