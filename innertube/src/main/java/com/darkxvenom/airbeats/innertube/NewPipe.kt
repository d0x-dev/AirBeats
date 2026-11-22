/*
 * AirBeats Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.darkxvenom.airbeats.innertube

import com.darkxvenom.airbeats.innertube.models.YouTubeClient
import com.darkxvenom.airbeats.backend.NewPipeDownloader
import org.schabi.newpipe.extractor.NewPipe

/**
 * Single entry point to NewPipeExtractor (same library + version iTune uses).
 * Initialised once; [streamInfo] is blocking, call it from Dispatchers.IO.
 */
object NewPipeUtils {
    private val ready by lazy { NewPipe.init(NewPipeDownloader.instance); true }

    fun streamInfo(videoId: String): org.schabi.newpipe.extractor.stream.StreamInfo {
        check(ready)
        return org.schabi.newpipe.extractor.stream.StreamInfo.getInfo(
            org.schabi.newpipe.extractor.ServiceList.YouTube,
            "https://www.youtube.com/watch?v=$videoId",
        )
    }
}
