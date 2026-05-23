package com.darkxvenom.airbeats.innertube.models.body

import com.darkxvenom.airbeats.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
