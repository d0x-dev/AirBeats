package com.darkxvenom.airbeats.lyrics.providers

import android.content.Context
import com.darkxvenom.airbeats.constants.EnablePaxsenixLyricsKey
import com.darkxvenom.airbeats.lyrics.LyricsProvider
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AirBeatsPaxsenixLyricsProvider : LyricsProvider {
    override val name: String = "Paxsenix (Multi-Source)"

    private const val BASE_URL = "https://lyrics.paxsenix.org/"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnablePaxsenixLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.trim()
            val cleanArtist = artist.trim()
            val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")

            // Multi-backend fallback order: Apple Music -> Spotify -> Musixmatch -> Netease -> YouTube ID
            val endpoints = listOf(
                "apple-music/lyrics?title=$encTitle&artist=$encArtist",
                "spotify/lyrics?title=$encTitle&artist=$encArtist",
                "musixmatch/lyrics?title=$encTitle&artist=$encArtist",
                "netease/lyrics?title=$encTitle&artist=$encArtist"
            )

            for (path in endpoints) {
                try {
                    val req = Request.Builder()
                        .url(BASE_URL + path)
                        .header("User-Agent", "AirBeats/1.0")
                        .header("Accept", "application/json, text/plain, */*")
                        .build()

                    val resp = client.newCall(req).execute()
                    if (resp.isSuccessful) {
                        val body = resp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val lrc = extractLyricsText(body)
                            if (!lrc.isNullOrBlank()) {
                                return@runCatching lrc
                            }
                        }
                    }
                } catch (_: Exception) {
                    continue
                }
            }

            // Fallback by video ID if available
            val cleanId = if (id.startsWith("JS:")) "" else id.trim()
            if (cleanId.isNotBlank()) {
                try {
                    val ytReq = Request.Builder()
                        .url("${BASE_URL}youtube/lyrics?id=$cleanId")
                        .header("User-Agent", "AirBeats/1.0")
                        .header("Accept", "application/json")
                        .build()
                    val ytResp = client.newCall(ytReq).execute()
                    if (ytResp.isSuccessful) {
                        val body = ytResp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val lrc = extractLyricsText(body)
                            if (!lrc.isNullOrBlank()) return@runCatching lrc
                        }
                    }
                } catch (_: Exception) {}
            }

            throw IllegalStateException("Lyrics not found on Paxsenix")
        }
    }

    private fun extractLyricsText(body: String): String? {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return trimmed

        return try {
            val json = JSONObject(trimmed)
            for (key in listOf("lyrics", "lrc", "content", "text", "plainLyrics", "syncedLyrics")) {
                val value = json.optString(key, "").trim()
                if (value.isNotEmpty() && value != "null") return value
            }
            null
        } catch (_: Exception) {
            trimmed
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        getLyrics(id, title, artist, duration).onSuccess(callback)
    }
}
