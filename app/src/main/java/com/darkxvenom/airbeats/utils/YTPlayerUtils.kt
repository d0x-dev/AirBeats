/*
 * AirBeats Project (2026) - GPL-3.0
 * Thin facade over [StreamBackend]. The old multi-client InnerTube/PoToken extraction was removed.
 */
package com.darkxvenom.airbeats.utils

import android.net.ConnectivityManager
import com.darkxvenom.airbeats.backend.StreamBackend
import com.darkxvenom.airbeats.constants.AudioQuality
import com.darkxvenom.airbeats.constants.EnableJioSaavnKey
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.YouTubeClient
import com.darkxvenom.airbeats.innertube.models.response.PlayerResponse
import com.darkxvenom.airbeats.utils.dataStoreCache
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
    ): Result<PlaybackData> = runCatching {
        val jioSaavnEnabled = dataStoreCache[EnableJioSaavnKey.name] as? Boolean ?: true
        val r = StreamBackend.resolve(videoId, audioQuality, avoidCodecs, jioSaavnEnabled = jioSaavnEnabled).getOrThrow()
        // Metadata (title, loudness, history-tracking URLs) is best effort and never blocks playback.
        val meta = withTimeoutOrNull(4_000L) { playerResponseForMetadata(videoId, playlistId).getOrNull() }
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
    }

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
