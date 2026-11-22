/*
 * AirBeats Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.darkxvenom.airbeats.innertube

import com.darkxvenom.airbeats.innertube.models.YouTubeClient
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

private class NewPipeDownloaderImpl : Downloader() {
    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
            .url(request.url())
        var hasUserAgent = false
        request.headers().forEach { (name, values) ->
            if (name.equals("User-Agent", ignoreCase = true) && values.isNotEmpty()) hasUserAgent = true
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }
        if (!hasUserAgent) builder.header("User-Agent", YouTubeClient.USER_AGENT_WEB)
        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 429) throw ReCaptchaException("reCaptcha challenge requested", request.url())
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body?.string(),
                response.request.url.toString(),
            )
        }
    }
}

/**
 * Single entry point to NewPipeExtractor. Blocking; call from Dispatchers.IO.
 */
object NewPipeUtils {
    private val ready by lazy {
        NewPipe.init(NewPipeDownloaderImpl())
        org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor.setFetchIosClient(true)
        true
    }

    fun streamInfo(videoId: String): org.schabi.newpipe.extractor.stream.StreamInfo {
        check(ready)
        return org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
            org.schabi.newpipe.extractor.ServiceList.YouTube,
            "https://www.youtube.com/watch?v=$videoId",
        )
    }
}
