package com.darkxvenom.airbeats.lyrics

import android.content.Context
import com.darkxvenom.airbeats.constants.EnableYouTubeSubtitleLyricsKey
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get

object YouTubeSubtitleLyricsProvider : LyricsProvider {
    override val name = "YouTube Subtitle"

    override fun isEnabled(context: Context) = context.dataStore[EnableYouTubeSubtitleLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = YouTube.transcript(id)
}
