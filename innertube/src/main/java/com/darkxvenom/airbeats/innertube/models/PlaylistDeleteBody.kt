package com.darkxvenom.airbeats.innertube.models.body

import com.darkxvenom.airbeats.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)
