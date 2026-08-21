import re

file_path = 'app/src/main/java/com/darkxvenom/airbeats/utils/YTPlayerUtils.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

findUrl_pattern = r'    private fun findUrlOrNull\(\s*format: PlayerResponse\.StreamingData\.Format,\s*videoId: String\s*\): String\? \{.*?    \}'
new_findUrl = '''    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: com.darkxvenom.airbeats.innertube.models.YouTubeClient? = null,
    ): String? {
        Timber.tag(logTag).i("Finding stream URL for format: , videoId: ")
        var url = com.darkxvenom.airbeats.innertube.NewPipeUtils.getStreamUrl(format, videoId, client)
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
    }'''

content = re.sub(findUrl_pattern, new_findUrl, content, flags=re.DOTALL)

validateStatus_pattern = r'    private fun validateStatus\(url: String\): Boolean \{.*?    \}'
new_validateStatus = '''    private fun validateStatus(url: String, userAgent: String): Boolean {
        Timber.tag(logTag).v("Validating stream URL status")
        try {
            val httpUrl = okhttp3.HttpUrl.Companion.toHttpUrlOrNull(url)
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
    }'''

content = re.sub(validateStatus_pattern, new_validateStatus, content, flags=re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
