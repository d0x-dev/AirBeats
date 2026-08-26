/**
 * MuSicX Project (C) 2026
 * Licensed under GPL-3.0
 *
 * Shared coordination point for prioritising *playback-critical* Spotify API
 * calls over *background* ones.
 *
 * Background: v13.8.10 introduced a per-playlist track-count hydrator that
 * issues ~20 back-to-back `Spotify.playlist(id)` GQL calls right after the
 * library loads. Even at a 350 ms throttle that saturates Spotify's rate
 * limiter and OkHttp's dispatcher, so when the user taps play on an
 * uncached Spotify song the playback pipeline's `Spotify.search()` /
 * `playlistTracks()` calls queue behind the hydrator — the user sees a
 * multi-second stall before the first note plays.
 *
 * The fix is a lightweight priority gate:
 *  * Playback-critical call sites (`SpotifyYouTubeMapper.matchOnSpotify`,
 *    each queue's Spotify page fetch) wrap their calls in
 *    `withPlaybackPriority { … }`. This bumps an in-flight counter for the
 *    duration of the call.
 *  * Background jobs (the hydrator, prefetchers, etc.) call `awaitIdle()`
 *    before every request. If any playback call is in-flight, the
 *    background job suspends until the counter hits zero — playback wins
 *    the Spotify lane by default.
 *
 * This is intentionally *cooperative*, not a semaphore. Background jobs
 * still make their own calls; they just yield the API bandwidth to
 * playback whenever the user is actively starting something.
 */
package com.darkxvenom.airbeats.spotify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

object SpotifyPriorityGate {
    private val playbackInFlight = MutableStateFlow(0)

    /**
     * Increment the playback-critical in-flight counter for the duration
     * of [block]. Guaranteed to decrement even on cancellation / thrown
     * exceptions so a crash inside the wrapped call can never permanently
     * block background jobs.
     */
    suspend inline fun <T> withPlaybackPriority(block: () -> T): T {
        beginPlayback()
        try {
            return block()
        } finally {
            endPlayback()
        }
    }

    fun beginPlayback() {
        playbackInFlight.value = playbackInFlight.value + 1
    }

    fun endPlayback() {
        playbackInFlight.value = (playbackInFlight.value - 1).coerceAtLeast(0)
    }

    /** Suspends until every playback-critical call has returned. */
    suspend fun awaitIdle() {
        playbackInFlight.first { it == 0 }
    }
}
