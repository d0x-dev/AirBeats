import re

file_path = 'app/src/main/java/com/darkxvenom/airbeats/utils/YTPlayerUtils.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix findUrlOrNull
old_findUrl = '''    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: , videoId: ")
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onSuccess { Timber.tag(logTag).d("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull()
    }'''

new_findUrl = '''    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
    ): String? {
        Timber.tag(logTag).i("Finding stream URL for format: , videoId: ")
        var url = NewPipeUtils.getStreamUrl(format, videoId, client)
            .onSuccess { Timber.tag(logTag).i("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull() ?: return null

        if (client != null) {
            url = StreamClientUtils.patchClientVersion(url, client.clientVersion)
        }

        return url
    }'''

content = content.replace(old_findUrl, new_findUrl)

# Fix validateStatus
old_validateStatus = '''    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val request = okhttp3.Request.Builder()
                .head()
                .url(url)
                .build()
            val response = httpClient.newCall(request).execute()
            response.use {
                val isSuccessful = it.isSuccessful || it.code == 206 || it.code == 200
                Timber.tag(logTag)
                    .d("Stream URL validation result:  ()")
                return isSuccessful
            }
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
        }
        return false
    }'''

new_validateStatus = '''    private fun validateStatus(url: String, userAgent: String): Boolean {
        Timber.tag(logTag).v("Validating stream URL status")
        try {
            val httpUrl = okhttp3.HttpUrl.Companion.toHttpUrlOrNull(url)
            val clientParam = httpUrl?.queryParameter("c")?.trim().orEmpty()

            val resolvedUserAgent = StreamClientUtils.resolveUserAgent(clientParam).ifEmpty { userAgent }
            val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

            val probeRanges =
                if (StreamClientUtils.isWebClient(clientParam)) {
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
    }'''

content = content.replace(old_validateStatus, new_validateStatus)

# Fix playerResponseForPlayback calling validateStatus and findUrlOrNull
content = content.replace('validateStatus(streamUrl)', 'validateStatus(streamUrl, client.userAgent)')
content = content.replace('findUrlOrNull(format, videoId)', 'findUrlOrNull(format, videoId, client)')

# Add bot detection
bot_detection = '''    private fun isBotDetectionError(reason: String): Boolean {
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
'''

content = re.sub(r'\}$', bot_detection + '}', content.strip())

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
