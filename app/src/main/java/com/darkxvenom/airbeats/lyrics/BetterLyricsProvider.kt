/*
 * AirBeats Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.darkxvenom.airbeats.lyrics

import android.content.Context
import com.darkxvenom.airbeats.betterlyrics.BetterLyrics
import android.util.Log

object BetterLyricsProvider : LyricsProvider {
    init {
        BetterLyrics.logger = { message ->
            Log.i("BetterLyrics", message)
        }
    }

    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title = title, artist = artist, album = null, durationSeconds = duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(
            title = title,
            artist = artist,
            album = null,
            durationSeconds = duration,
            callback = callback,
        )
    }
}
