package com.darkxvenom.airbeats.models

import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
