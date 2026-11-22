/*
 * AirBeats Project (2026) - GPL-3.0
 * Thin facade over [StreamBackend]. The old multi-client InnerTube/PoToken extraction was removed.
 */
package com.darkxvenom.airbeats.utils

import android.net.ConnectivityManager
import com.darkxvenom.airbeats.backend.StreamBackend
import com.darkxvenom.airbeats.constants.AudioQuality
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.YouTubeClient
import com.darkxvenom.airbeats.innertube.models.response.PlayerResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

object YTPlayerUtils {
    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    fun invalidateCachedStreamUrls(videoId: String) = StreamBackend.invalidate(videoId)

    @Suppress("UNUSED_PARAMETER")
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        networkMetered: Boolean? = null,
        avoidCodecs: Set<String> = emptySet(),
    ): Result<PlaybackData> = runCatching { coroutineScope {
        // Metadata (loudness, history-tracking URLs) runs IN PARALLEL with stream resolving and is
        // only awaited briefly, so it can never delay the first audio byte.
        val metaJob = async {
            if (videoId.startsWith("JS:")) null
            else withTimeoutOrNull(3_000L) { playerResponseForMetadata(videoId, playlistId).getOrNull() }
        }
        val r = StreamBackend.resolve(videoId, audioQuality, avoidCodecs).getOrThrow()
        val meta = withTimeoutOrNull(800L) { metaJob.await() }
        if (meta == null) metaJob.cancel()
        val mime = if (r.codec != null) "${r.mimeType}; codecs=\"${r.codec}\"" else r.mimeType
        PlaybackData(
            audioConfig = meta?.playerConfig?.audioConfig,
            videoDetails = meta?.videoDetails,
            playbackTracking = meta?.playbackTracking,
            format = PlayerResponse.StreamingData.Format(
                itag = r.itag, url = r.url, mimeType = mime, bitrate = r.bitrateBps,
                width = null, height = null, contentLength = r.contentLength, quality = "tiny",
                fps = null, qualityLabel = null, averageBitrate = r.bitrateBps, audioQuality = null,
                approxDurationMs = null, audioSampleRate = null, audioChannels = null,
                loudnessDb = meta?.playerConfig?.audioConfig?.loudnessDb, lastModified = null,
                signatureCipher = null, cipher = null,
            ),
            streamUrl = r.url,
            streamExpiresInSeconds = r.expiresInSeconds,
        )
    } }

    /** Metadata only: stream URLs of this response must not be used. */
    suspend fun playerResponseForMetadata(videoId: String, playlistId: String? = null): Result<PlayerResponse> =
        YouTube.player(videoId, playlistId, client = YouTubeClient.WEB_REMIX)

    internal fun targetBitrateBps(quality: AudioQuality): Int? = when (quality) {
        AudioQuality.LOW -> 70_000
        AudioQuality.MEDIUM -> 128_000
        AudioQuality.HIGH -> 160_000
        AudioQuality.AUTO -> null
    }

    internal fun nearestQuality(bitrateBps: Int): AudioQuality =
        listOf(AudioQuality.LOW, AudioQuality.MEDIUM, AudioQuality.HIGH)
            .minBy { kotlin.math.abs(targetBitrateBps(it)!! - bitrateBps) }
}
