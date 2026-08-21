package com.darkxvenom.airbeats.innertube

import com.darkxvenom.airbeats.innertube.models.YouTubeClient
import com.darkxvenom.airbeats.innertube.models.response.PlayerResponse
import dev.maxrave.pipepipe.extractor.NewPipe
import dev.maxrave.pipepipe.extractor.ServiceList
import dev.maxrave.pipepipe.extractor.downloader.CancellableCall
import dev.maxrave.pipepipe.extractor.downloader.Downloader
import dev.maxrave.pipepipe.extractor.downloader.Request
import dev.maxrave.pipepipe.extractor.downloader.Response
import dev.maxrave.pipepipe.extractor.exceptions.ParsingException
import dev.maxrave.pipepipe.extractor.exceptions.ReCaptchaException
import dev.maxrave.pipepipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import dev.maxrave.pipepipe.extractor.stream.StreamInfo
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap
import org.schabi.newpipe.extractor.NewPipe as BraveNewPipe
import org.schabi.newpipe.extractor.ServiceList as BraveServiceList
import org.schabi.newpipe.extractor.downloader.Downloader as BraveDownloader
import org.schabi.newpipe.extractor.downloader.Request as BraveRequest
import org.schabi.newpipe.extractor.downloader.Response as BraveResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException as BraveReCaptchaException
import org.schabi.newpipe.extractor.stream.StreamInfo as BraveStreamInfo

private val requiredAudioItags = setOf(250, 251, 774, 141)

private class PipePipeDownloader(proxy: Proxy?) : Downloader() {
    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val response = client.newCall(request.toOkHttpRequest()).execute()
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }
        return response.toPipePipeResponse()
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun executeAsync(request: Request, callback: AsyncCallback?): CancellableCall {
        val call = client.newCall(request.toOkHttpRequest())
        val cancellableCall = CancellableCall(call)
        call.enqueue(
            object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    cancellableCall.setFinished()
                    callback?.onError(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        if (response.code == 429) {
                            response.close()
                            callback?.onError(
                                ReCaptchaException("reCaptcha Challenge requested", request.url())
                            )
                            return
                        }
                        callback?.onSuccess(response.toPipePipeResponse())
                    } catch (e: Exception) {
                        callback?.onError(e)
                    } finally {
                        cancellableCall.setFinished()
                    }
                }
            }
        )
        return cancellableCall
    }

    private fun Request.toOkHttpRequest(): okhttp3.Request {
        val builder = okhttp3.Request.Builder()
            .method(httpMethod(), dataToSend()?.toRequestBody())
            .url(url())
            .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers().forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                builder.removeHeader(headerName)
                headerValueList.forEach { builder.addHeader(headerName, it) }
            } else if (headerValueList.size == 1) {
                builder.header(headerName, headerValueList[0])
            }
        }
        return builder.build()
    }

    private fun okhttp3.Response.toPipePipeResponse(): Response {
        val rawBytes = body?.bytes() ?: ByteArray(0)
        return Response(
            code,
            message,
            headers.toMultimap(),
            rawBytes.toString(Charsets.UTF_8),
            rawBytes,
            request.url.toString(),
        )
    }
}

private class BravePipeDownloader(proxy: Proxy?) : BraveDownloader() {
    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .build()

    @Throws(IOException::class, BraveReCaptchaException::class)
    override fun execute(request: BraveRequest): BraveResponse {
        val response = client.newCall(request.toOkHttpRequest()).execute()
        if (response.code == 429) {
            response.close()
            throw BraveReCaptchaException("reCaptcha Challenge requested", request.url())
        }

        val responseBody = response.body?.string()
        return BraveResponse(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseBody,
            response.request.url.toString(),
        )
    }

    private fun BraveRequest.toOkHttpRequest(): okhttp3.Request {
        val builder = okhttp3.Request.Builder()
            .method(httpMethod(), dataToSend()?.toRequestBody())
            .url(url())
            .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers().forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                builder.removeHeader(headerName)
                headerValueList.forEach { builder.addHeader(headerName, it) }
            } else if (headerValueList.size == 1) {
                builder.header(headerName, headerValueList[0])
            }
        }
        return builder.build()
    }
}

object SongStreamExtractor {
    private val streamCache = ConcurrentHashMap<String, List<Pair<Int, String>>>()
    private val healthCheckClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    init {
        NewPipe.init(PipePipeDownloader(YouTube.proxy))
        BraveNewPipe.init(BravePipeDownloader(YouTube.proxy))
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
        authState: PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<String> = runCatching {
        val extractorUrl = getExtractorStreamUrl(videoId, format.itag)
        val resolvedUrl = extractorUrl ?: resolvePlayerFormatUrl(format, videoId)

        YouTube.appendGvsPoToken(
            url = resolvedUrl,
            client = client,
            authState = authState,
        )
    }

    fun getExtractorStreamUrl(videoId: String, itag: Int): String? {
        return getExtractorStreams(videoId).firstOrNull { it.first == itag }?.second
    }

    private fun resolvePlayerFormatUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
    ): String {
        val url = format.url ?: format.signatureCipher?.let { signatureCipher ->
            val params = parseQueryString(signatureCipher)
            val obfuscatedSignature = params["s"]
                ?: throw ParsingException("Could not parse cipher signature")
            val signatureParam = params["sp"]
                ?: throw ParsingException("Could not parse cipher signature parameter")
            val url = params["url"]?.let { URLBuilder(it) }
                ?: throw ParsingException("Could not parse cipher url")
            url.parameters[signatureParam] =
                YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscatedSignature)
            url.toString()
        } ?: throw ParsingException("Could not find format url")

        return YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
    }

    private fun getExtractorStreams(videoId: String): List<Pair<Int, String>> {
        streamCache[videoId]?.let { return it }

        val streams = loadPipePipeStreams(videoId)
            .takeIf { it.hasRequiredAudioItag() && it.headCheckRandomAudioStream() }
            ?: loadBravePipeStreams(videoId)

        streamCache[videoId] = streams
        return streams
    }

    private fun loadPipePipeStreams(videoId: String): List<Pair<Int, String>> {
        return runCatching {
            ServiceList.YouTube.tokens = YouTube.cookie.orEmpty()
            val streamInfo =
                StreamInfo.getInfo(ServiceList.YouTube, "https://music.youtube.com/watch?v=$videoId")
            (streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams)
                .mapNotNull { stream ->
                    (stream.itagItem?.id ?: return@mapNotNull null) to stream.content
                }
        }.getOrElse { emptyList() }
    }

    private fun loadBravePipeStreams(videoId: String): List<Pair<Int, String>> {
        return runCatching {
            val streamInfo =
                BraveStreamInfo.getInfo(BraveServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
            (streamInfo.audioStreams + streamInfo.videoStreams + streamInfo.videoOnlyStreams)
                .mapNotNull { stream ->
                    (stream.itagItem?.id ?: return@mapNotNull null) to stream.content
                }
        }.getOrElse { emptyList() }
    }

    private fun List<Pair<Int, String>>.hasRequiredAudioItag(): Boolean {
        val itags = mapTo(HashSet()) { it.first }
        return requiredAudioItags.any { it in itags }
    }

    private fun List<Pair<Int, String>>.headCheckRandomAudioStream(): Boolean {
        val candidate = filter { it.first in requiredAudioItags }.randomOrNull() ?: return false
        return runCatching {
            val request = okhttp3.Request.Builder()
                .head()
                .url(candidate.second)
                .build()
            healthCheckClient.newCall(request).execute().use { response ->
                response.code in 200..299
            }
        }.getOrDefault(false)
    }
}
