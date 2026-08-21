package com.darkxvenom.airbeats.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.darkxvenom.airbeats.innertube.SongStreamExtractor
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.YouTubeClient
import com.darkxvenom.airbeats.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.darkxvenom.airbeats.innertube.models.YouTubeClient.Companion.IOS
import com.darkxvenom.airbeats.innertube.models.YouTubeClient.Companion.MOBILE
import com.darkxvenom.airbeats.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.darkxvenom.airbeats.innertube.models.YouTubeClient.Companion.WEB
import com.darkxvenom.airbeats.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.darkxvenom.airbeats.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.darkxvenom.airbeats.innertube.models.response.PlayerResponse
import com.darkxvenom.airbeats.constants.AudioQuality
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    /**
     * The main client is used for metadata and initial streams. Do not use
     * other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets
     * (loudnessDb). Creditos completos de el commit completo a metrolist.
     *
     * [com.metrolist.innertube.models.YouTubeClient.WEB_REMIX] should be
     * preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX

    /**
     * Clients used for fallback streams in case the streams of the main client
     * do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_VR_NO_AUTH,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        WEB,
        WEB_CREATOR,
        MOBILE
    )

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    /**
     * Custom player response intended to use for playback. Metadata like
     * audioConfig and videoDetails are from [MAIN_CLIENT]. Format & stream can
     * be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag)
            .d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        /**
         * This is required for some clients to get working streams however it
         * should not be forced for the [MAIN_CLIENT] because the response of the
         * [MAIN_CLIENT] is required even if the streams won't work from this
         * client. This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            }
        Timber.tag(logTag)
            .d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        Timber.tag(logTag)
            .d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrNull()

        var audioConfig = mainPlayerResponse?.playerConfig?.audioConfig
        var videoDetails = mainPlayerResponse?.videoDetails
        var selectedFormat: PlayerResponse.StreamingData.Format? = null
        var selectedStreamUrl: String? = null
        var selectedExpiresInSeconds: Int? = null

        var fallbackCandidateFormat: PlayerResponse.StreamingData.Format? = null
        var fallbackCandidateUrl: String? = null
        var fallbackCandidateExpires: Int? = null

        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            val client: YouTubeClient
            val streamPlayerResponse: PlayerResponse?

            if (clientIndex == -1) {
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
                Timber.tag(logTag).d("Trying stream from MAIN_CLIENT: ${client.clientName}")
            } else {
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                Timber.tag(logTag)
                    .d("Trying fallback client ${clientIndex + 1}/${STREAM_FALLBACK_CLIENTS.size}: ${client.clientName}")

                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    Timber.tag(logTag)
                        .d("Skipping client ${client.clientName} - requires login but user is not logged in")
                    continue
                }

                Timber.tag(logTag)
                    .d("Fetching player response for fallback client: ${client.clientName}")
                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }

            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                if (audioConfig == null) {
                    audioConfig = streamPlayerResponse.playerConfig?.audioConfig
                }
                if (videoDetails == null) {
                    videoDetails = streamPlayerResponse.videoDetails
                }

                val format = findFormat(
                    streamPlayerResponse,
                    audioQuality,
                    connectivityManager,
                )

                if (format == null) {
                    Timber.tag(logTag)
                        .d("No suitable format found for client: ${client.clientName}")
                    continue
                }

                val streamUrl = findUrlOrNull(format, videoId, client)
                if (streamUrl == null) {
                    Timber.tag(logTag).d("Stream URL not found for format")
                    continue
                }

                val streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds ?: 21600

                if (fallbackCandidateUrl == null) {
                    fallbackCandidateFormat = format
                    fallbackCandidateUrl = streamUrl
                    fallbackCandidateExpires = streamExpiresInSeconds
                }

                if (validateStatus(streamUrl, client.userAgent)) {
                    Timber.tag(logTag)
                        .d("Stream validated successfully with client: ${client.clientName}")
                    selectedFormat = format
                    selectedStreamUrl = streamUrl
                    selectedExpiresInSeconds = streamExpiresInSeconds
                    break
                } else {
                    Timber.tag(logTag)
                        .d("Stream validation failed for client: ${client.clientName}")
                }
            } else {
                Timber.tag(logTag)
                    .d("Player response status not OK: ${streamPlayerResponse?.playabilityStatus?.status}, reason: ${streamPlayerResponse?.playabilityStatus?.reason}")
            }
        }

        val finalFormat = selectedFormat ?: fallbackCandidateFormat
        val finalStreamUrl = selectedStreamUrl ?: fallbackCandidateUrl
        val finalExpires = selectedExpiresInSeconds ?: fallbackCandidateExpires

        if (finalFormat == null || finalStreamUrl == null || finalExpires == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        Timber.tag(logTag)
            .d("Successfully obtained playback data with format: ${finalFormat.mimeType}, bitrate: ${finalFormat.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            finalFormat,
            finalStreamUrl,
            finalExpires,
        )
    }

    /**
     * Simple player response intended to use for metadata only. Stream URLs of
     * this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag)
            .d("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = MAIN_CLIENT)
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag)
            .d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }

    /**
     * Checks if the stream url returns a successful status. If this returns
     * true the url is likely to work. If this returns false the url might
     * cause an error during playback.
     */
    private fun validateStatus(url: String, userAgent: String): Boolean {
        Timber.tag(logTag).v("Validating stream URL status")
        try {
            val httpUrl = url.toHttpUrlOrNull()
            val clientParam = httpUrl?.queryParameter("c")?.trim().orEmpty()

            val resolvedUserAgent = com.darkxvenom.airbeats.utils.StreamClientUtils.resolveUserAgent(clientParam).ifEmpty { userAgent }
            val originReferer = com.darkxvenom.airbeats.utils.StreamClientUtils.resolveOriginReferer(clientParam)

            val probeRanges =
                if (com.darkxvenom.airbeats.utils.StreamClientUtils.isWebClient(clientParam)) {
                    listOf("bytes=0-0", "bytes=262144-262145", "bytes=1048576-1048577")
                } else {
                    listOf("bytes=0-0")
                }

            for (range in probeRanges) {
                val rangeRequest =
                    okhttp3.Request.Builder()
                        .get()
                        .header("User-Agent", resolvedUserAgent)
                        .header("Range", range)
                        .apply {
                            originReferer.origin?.let { header("Origin", it) }
                            originReferer.referer?.let { header("Referer", it) }
                        }.url(url)
                        .build()

                val code = httpClient.newCall(rangeRequest).execute().use { response -> response.code }
                if (code == 403) return false
                if (code !in 200..399 && code != 416) return false
            }

            return true
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }

    /**
     * Wrapper around the [SongStreamExtractor.getSignatureTimestamp] function which
     * reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        return SongStreamExtractor.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [SongStreamExtractor.getStreamUrl] function which reports
     * exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: com.darkxvenom.airbeats.innertube.models.YouTubeClient? = null,
    ): String? {
        Timber.tag(logTag).i("Finding stream URL for format: , videoId: ")
        var url = SongStreamExtractor.getStreamUrl(format, videoId, client)
            .onSuccess { Timber.tag(logTag).i("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull() ?: return null

        if (client != null) {
            url = com.darkxvenom.airbeats.utils.StreamClientUtils.patchClientVersion(url, client.clientVersion)
        }

        return url
    }



    private fun isBotDetectionError(reason: String): Boolean {
        val lower = reason.lowercase(java.util.Locale.US)
        return "bot" in lower ||
            "unusual traffic" in lower ||
            "automated" in lower ||
            "confirm" in lower && "not a" in lower ||
            "not a robot" in lower ||
            "verify" in lower && "human" in lower
    }

    fun isBotDetectionException(error: androidx.media3.common.PlaybackException): Boolean {
        val message = error.message.orEmpty()
        if (isBotDetectionError(message)) return true
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (isBotDetectionError(cause.message.orEmpty())) return true
            cause = cause.cause
        }
        return false
    }
}
