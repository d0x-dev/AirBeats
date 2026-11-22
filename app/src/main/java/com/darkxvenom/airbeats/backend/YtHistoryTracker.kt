package com.darkxvenom.airbeats.backend

import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.utils.YTPlayerUtils
import kotlinx.coroutines.delay

/** Real-time YouTube Music history: pings videostats the moment a track starts (not when it ends). */
object YtHistoryTracker {
    @Volatile private var lastId: String? = null
    @Volatile private var lastAt = 0L

    suspend fun onTrackStarted(videoId: String) {
        if (videoId.startsWith("JS:") || YouTube.cookie.isNullOrEmpty()) return
        val now = System.currentTimeMillis()
        if (videoId == lastId && now - lastAt < 30_000L) return
        lastId = videoId; lastAt = now
        repeat(3) { attempt ->
            val url = YTPlayerUtils.playerResponseForMetadata(videoId).getOrNull()
                ?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
            if (url != null && YouTube.registerPlayback(null, url).isSuccess) return
            delay(1_500L * (attempt + 1))
        }
    }
}
