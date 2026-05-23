package com.darkxvenom.airbeats.lyrics

import android.content.Context
import com.darkxvenom.airbeats.lrclib.LrcLib
import com.darkxvenom.airbeats.constants.EnableLrcLibKey
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = LrcLib.getLyrics(title, artist, duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        LrcLib.getAllLyrics(title, artist, duration, null, callback)
    }
}
