package com.darkxvenom.airbeats.models

import com.darkxvenom.airbeats.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
