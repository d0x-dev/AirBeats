/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of AirBeats and is licensed under GPL-3.0.
 */

package com.darkxvenom.airbeats.data.auth

import android.content.Context
import android.util.Log
import com.darkxvenom.airbeats.data.artist.ArtistInnertubeClient
import com.darkxvenom.airbeats.innertube.YouTube
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Registers audible plays against the signed-in account's YouTube Music history
 * using the three-ping sequence (`videostatsPlaybackUrl`, `atrUrl`, `videostatsWatchtimeUrl`)
 * so `FEmusic_history` and personalized recommendations reflect what was played in AirBeats.
 */
object YouTubePlaybackTracker {
    private const val TAG = "YtmPlaybackTracker"
    private const val REPORT_INTERVAL_SECONDS = 30L
    private const val OPEN_ATTEMPTS = 3
    private const val OPEN_RETRY_DELAY_MS = 2_000L
    private val VIDEO_ID = Regex("""[A-Za-z0-9_-]{11}""")

    private class Session(
        var videoId: String,
        val cpn: String,
        val tracking: ArtistInnertubeClient.PlaybackTracking,
    ) {
        var reportedSeconds = 0L
        var flushingTo = 0L
        var atrSent = false
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val lock = Mutex()

    @Volatile
    private var session: Session? = null

    @Volatile
    private var opening: String? = null

    fun onPlaying(context: Context, videoId: String) {
        if (!VIDEO_ID.matches(videoId)) return
        if (YouTube.cookie.isNullOrBlank()) return
        if (session?.videoId == videoId || opening == videoId) return

        opening = videoId
        val appContext = context.applicationContext
        scope.launch {
            try {
                openWithRetries(appContext, videoId)
            } finally {
                if (opening == videoId) opening = null
            }
        }
    }

    fun onTrackChanged(positionSeconds: Long) {
        val closing = session ?: return
        session = null
        scope.launch {
            runCatching { flush(closing, positionSeconds, final = true) }
                .onFailure { Log.w(TAG, "final watchtime ping failed: ${it.message}") }
        }
    }

    fun onProgress(videoId: String, positionSeconds: Long) {
        val current = session ?: return
        if (current.videoId != videoId) return
        if (!current.atrSent && positionSeconds >= current.tracking.atrAfterSeconds) {
            current.atrSent = true
            current.tracking.atrUrl?.let { atrUrl ->
                scope.launch {
                    runCatching {
                        val code = ArtistInnertubeClient.pingAtr(atrUrl, current.cpn)
                        Log.d(TAG, "atr ping for $videoId -> HTTP $code")
                    }.onFailure {
                        Log.w(TAG, "atr ping failed for $videoId: ${it.message}")
                    }
                }
            }
        }
        if (positionSeconds - maxOf(current.reportedSeconds, current.flushingTo) < REPORT_INTERVAL_SECONDS) return
        scope.launch {
            runCatching { flush(current, positionSeconds) }
                .onFailure { Log.w(TAG, "watchtime ping failed for $videoId: ${it.message}") }
        }
    }

    fun onPlaybackFinished(positionSeconds: Long) {
        val closing = session ?: return
        session = null
        scope.launch {
            withContext(NonCancellable) {
                runCatching { flush(closing, positionSeconds, final = true) }
                    .onFailure { Log.w(TAG, "closing watchtime failed: ${it.message}") }
            }
        }
    }

    private suspend fun openWithRetries(appContext: Context, videoId: String) {
        repeat(OPEN_ATTEMPTS) { attempt ->
            if (opening != videoId) return
            val settled = try {
                open(appContext, videoId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "playback tracking failed (${attempt + 1}/$OPEN_ATTEMPTS): ${e.message}")
                false
            }
            if (settled) return
            if (attempt < OPEN_ATTEMPTS - 1) delay(OPEN_RETRY_DELAY_MS)
        }
    }

    private suspend fun open(appContext: Context, videoId: String): Boolean = lock.withLock {
        val sts = ArtistInnertubeClient.signatureTimestamp(videoId)
        val tracking = ArtistInnertubeClient.playbackTracking(videoId, sts) ?: return@withLock true
        val cpn = ArtistInnertubeClient.newCpn()
        val status = ArtistInnertubeClient.pingPlayback(tracking.playbackUrl, cpn)
        session = Session(videoId, cpn, tracking)
        Log.d(TAG, "YTM history playback ping for $videoId -> HTTP $status")
        status in 200..299
    }

    private suspend fun flush(s: Session, positionSeconds: Long, final: Boolean = false) {
        val watchtimeUrl = s.tracking.watchtimeUrl ?: return
        val reportTo = maxOf(positionSeconds, s.reportedSeconds)
        s.flushingTo = reportTo
        val code = ArtistInnertubeClient.pingWatchtime(
            baseUrl = watchtimeUrl,
            cpn = s.cpn,
            seconds = reportTo,
            final = final
        )
        s.reportedSeconds = reportTo
        Log.d(TAG, "watchtime ping (${if (final) "final" else "tick"}) for ${s.videoId} at ${reportTo}s -> HTTP $code")
    }
}
