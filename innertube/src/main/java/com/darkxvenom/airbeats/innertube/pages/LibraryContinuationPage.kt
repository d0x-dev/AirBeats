package com.darkxvenom.airbeats.innertube.pages

import com.darkxvenom.airbeats.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
