package com.darkxvenom.airbeats.spotify.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single entry in the `/me/player/recently-played` response.
 * `played_at` is an ISO-8601 timestamp for when the track was played.
 */
@Serializable
data class SpotifyPlayHistoryItem(
    val track: SpotifyTrack? = null,
    @SerialName("played_at") val playedAt: String? = null,
)

/**
 * Cursor-paginated wrapper returned by `/me/player/recently-played`.
 * We only care about `items`; cursors/next are unused for our home-feed use case.
 */
@Serializable
data class SpotifyRecentlyPlayedResponse(
    val items: List<SpotifyPlayHistoryItem> = emptyList(),
    val next: String? = null,
    val limit: Int = 20,
    val href: String? = null,
)
