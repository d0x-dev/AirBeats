/*
 * AirBeats Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.darkxvenom.airbeats.innertube

import com.darkxvenom.airbeats.innertube.PlaybackAuthState
import com.darkxvenom.airbeats.innertube.models.YouTubeClient
import com.darkxvenom.airbeats.innertube.models.response.PlayerResponse
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.io.IOException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

private class NewPipeDownloaderImpl(proxy: Proxy?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)

        var hasUserAgent = false
        headers.forEach { (headerName, headerValueList) ->
            if (headerName.equals("User-Agent", ignoreCase = true) && headerValueList.isNotEmpty()) {
                hasUserAgent = true
            }
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        if (!hasUserAgent) {
            requestBuilder.header("User-Agent", YouTubeClient.USER_AGENT_WEB)
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body.string()

        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }

}

private class PipePipeDownloaderImpl(proxy: Proxy?) :
    dev.maxrave.pipepipe.extractor.downloader.Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    override fun execute(
        request: dev.maxrave.pipepipe.extractor.downloader.Request,
    ): dev.maxrave.pipepipe.extractor.downloader.Response {
        val response = client.newCall(buildRequest(request)).execute()
        if (response.code == 429) {
            response.close()
            throw dev.maxrave.pipepipe.extractor.exceptions.ReCaptchaException(
                "reCaptcha Challenge requested",
                request.url(),
            )
        }
        return toPipePipeResponse(response)
    }

    override fun executeAsync(
        request: dev.maxrave.pipepipe.extractor.downloader.Request,
        callback: dev.maxrave.pipepipe.extractor.downloader.Downloader.AsyncCallback,
    ): dev.maxrave.pipepipe.extractor.downloader.CancellableCall {
        val call = client.newCall(buildRequest(request))
        val cancellableCall = dev.maxrave.pipepipe.extractor.downloader.CancellableCall(call)
        call.enqueue(
            object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    cancellableCall.setFinished()
                    callback.onError(e)
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    try {
                        if (response.code == 429) {
                            callback.onError(
                                dev.maxrave.pipepipe.extractor.exceptions.ReCaptchaException(
                                    "reCaptcha Challenge requested",
                                    request.url(),
                                ),
                            )
                        } else {
                            callback.onSuccess(toPipePipeResponse(response))
                        }
                    } catch (e: Exception) {
                        callback.onError(e)
                    } finally {
                        response.close()
                        cancellableCall.setFinished()
                    }
                }
            },
        )
        return cancellableCall
    }

    private fun buildRequest(
        request: dev.maxrave.pipepipe.extractor.downloader.Request,
    ): okhttp3.Request {
        val builder = okhttp3.Request.Builder()
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
            .url(request.url())

        var hasUserAgent = false
        request.headers().forEach { (headerName, values) ->
            if (headerName.equals("User-Agent", ignoreCase = true) && values.isNotEmpty()) {
                hasUserAgent = true
            }
            when (values.size) {
                0 -> Unit
                1 -> builder.header(headerName, values.first())
                else -> {
                    builder.removeHeader(headerName)
                    values.forEach { value -> builder.addHeader(headerName, value) }
                }
            }
        }

        if (!hasUserAgent) {
            builder.header("User-Agent", YouTubeClient.USER_AGENT_WEB)
        }
        return builder.build()
    }

    private fun toPipePipeResponse(
        response: okhttp3.Response,
    ): dev.maxrave.pipepipe.extractor.downloader.Response {
        val body = response.body.bytes()
        return dev.maxrave.pipepipe.extractor.downloader.Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            String(body, Charsets.UTF_8),
            body,
            response.request.url.toString(),
        )
    }
}

object NewPipeUtils {

    init {
        NewPipe.init(NewPipeDownloaderImpl(YouTube.proxy))
        dev.maxrave.pipepipe.extractor.NewPipe.init(PipePipeDownloaderImpl(YouTube.proxy))
    }

    @Volatile private var cachedSignatureTimestamp: Pair<Long, Int>? = null

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        val cached = cachedSignatureTimestamp
        if (cached != null && System.currentTimeMillis() - cached.first < 6 * 3600 * 1000L) {
            return@runCatching cached.second
        }
        val sts = YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        cachedSignatureTimestamp = System.currentTimeMillis() to sts
        sts
    }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
        authState: PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<String> =
        runCatching {
            val directUrl = format.url
            if (directUrl != null) {
                val resolvedDirectUrl =
                    if (directUrl.toHttpUrlOrNull()?.queryParameter("n")?.isNotBlank() == true) {
                        runCatching {
                            retryWithBackoff(
                                maxAttempts = 3,
                                initialDelayMs = 250L,
                                maxDelayMs = 2_000L
                            ) {
                                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                                    videoId,
                                    directUrl
                                )
                            }
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
                retryWithBackoff(
                    maxAttempts = 3,
                    initialDelayMs = 250L,
                    maxDelayMs = 2_000L
                ) {
                    YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
                }
            }.getOrElse { url }

            YouTube.appendGvsPoToken(
                url = resolvedUrl,
                client = client,
                authState = authState,
            )
        }

    /**
     * Secondary decoder for ciphered streams when NewPipe's local player-script parser fails.
     * PipePipe's decoder service is only contacted by this explicit fallback path.
     */
    fun getStreamUrlWithPipePipeDecoder(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
        authState: PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<String> = runCatching {
        val cipherString = format.signatureCipher ?: format.cipher
            ?: throw ParsingException("Could not find ciphered format url")
        val params = parseQueryString(cipherString)
        val obfuscatedSignature = params["s"]
            ?: throw ParsingException("Could not parse cipher signature")
        val signatureParam = params["sp"]
            ?: throw ParsingException("Could not parse cipher signature parameter")
        val url = params["url"]?.let(::URLBuilder)
            ?: throw ParsingException("Could not parse cipher url")

        url.parameters[signatureParam] =
            dev.maxrave.pipepipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
                .deobfuscateSignature(videoId, obfuscatedSignature)

        val signedUrl = url.toString()
        val resolvedUrl = runCatching {
            dev.maxrave.pipepipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
                .getUrlWithThrottlingParameterDeobfuscated(videoId, signedUrl)
        }.getOrElse { signedUrl }

        YouTube.appendGvsPoToken(
            url = resolvedUrl,
            client = client,
            authState = authState,
        )
    }

    private inline fun <T> retryWithBackoff(
        maxAttempts: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        block: () -> T
    ): T {
        var attempt = 0
        var delayMs = initialDelayMs
        var lastError: Throwable? = null
        while (attempt < maxAttempts) {
            try {
                return block()
            } catch (e: Throwable) {
                val isRetryable =
                    e is SocketTimeoutException ||
                        e is IOException ||
                        e.cause is SocketTimeoutException ||
                        e.cause is IOException
                if (!isRetryable || attempt == maxAttempts - 1) throw e
                lastError = e
                try {
                    Thread.sleep(delayMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }
                delayMs = (delayMs * 2).coerceAtMost(maxDelayMs)
                attempt++
            }
        }
        throw lastError ?: IllegalStateException("Retry attempts exhausted")
    }

}
