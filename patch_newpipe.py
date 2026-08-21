import re

file_path = 'innertube/src/main/java/com/darkxvenom/airbeats/innertube/pages/NewPipe.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

new_get_stream_url = '''    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
        authState: com.darkxvenom.airbeats.innertube.PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<String> =
        runCatching {
            val directUrl = format.url
            if (directUrl != null) {
                val resolvedDirectUrl =
                    if (directUrl.contains("n=")) {
                        runCatching {
                            YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                                videoId,
                                directUrl
                            )
                        }.getOrElse { directUrl }
                    } else {
                        directUrl
                    }

                return@runCatching YouTube.appendGvsPoToken(
                    url = resolvedDirectUrl,
                    client = client,
                    authState = authState,
                )
            }

            val url = run {
                val cipherString = format.signatureCipher ?: format.cipher
                if (cipherString == null) throw ParsingException("Could not find format url")

                val params = parseQueryString(cipherString)
                val obfuscatedSignature = params["s"]
                    ?: throw ParsingException("Could not parse cipher signature")
                val signatureParam = params["sp"]
                    ?: throw ParsingException("Could not parse cipher signature parameter")
                val url = params["url"]?.let { URLBuilder(it) }
                    ?: throw ParsingException("Could not parse cipher url")
                url.parameters[signatureParam] =
                    YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                        videoId,
                        obfuscatedSignature
                    )
                url.toString()
            }

            val resolvedUrl = runCatching {
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
            }.getOrElse { url }

            YouTube.appendGvsPoToken(
                url = resolvedUrl,
                client = client,
                authState = authState,
            )
        }'''

import re
content = re.sub(r'    fun getStreamUrl\(.*?\).*?        }', new_get_stream_url, content, flags=re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
