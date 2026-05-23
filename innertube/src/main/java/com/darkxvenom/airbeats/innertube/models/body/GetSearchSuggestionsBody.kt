package com.darkxvenom.airbeats.innertube.models.body

import com.darkxvenom.airbeats.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context,
    val input: String,
)
