package com.darkxvenom.airbeats.innertube.models.body

import com.darkxvenom.airbeats.innertube.models.Context
import com.darkxvenom.airbeats.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
