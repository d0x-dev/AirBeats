package com.darkxvenom.airbeats.kizzy.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    @SerialName("id")
    val id: String,
)
