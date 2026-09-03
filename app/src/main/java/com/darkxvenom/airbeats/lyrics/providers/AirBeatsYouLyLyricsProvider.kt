package com.darkxvenom.airbeats.lyrics.providers

import android.content.Context
import com.darkxvenom.airbeats.constants.EnableYouLyLyricsKey
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

object AirBeatsYouLyLyricsProvider : LyricsProvider {
    override val name: String = "YouLyPlus"

    private val baseUrls = listOf(
        "https://lyricsplus.binimum.org/",
        "https://lyricsplus.prjktla.my.id/",
        "https://lyricsplus.atomix.one/",
        "https://lyricsplus-seven.vercel.app/"
    )

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableYouLyLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanTitle = title.trim()
            val cleanArtist = artist.trim()
            if (cleanTitle.isBlank() || cleanArtist.isBlank()) {
                throw IllegalArgumentException("Title and artist required")
            }

            val encTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val encArtist = URLEncoder.encode(cleanArtist, "UTF-8")

            for (baseUrl in baseUrls) {
                try {
                    // Try TTML first
                    val ttmlUrl = "${baseUrl}v1/ttml/get?title=$encTitle&artist=$encArtist" + (if (duration > 0) "&duration=$duration" else "")
                    val ttmlReq = Request.Builder()
                        .url(ttmlUrl)
                        .header("User-Agent", "AirBeats/1.0")
                        .header("Accept", "application/json")
                        .build()

                    val ttmlResp = client.newCall(ttmlReq).execute()
                    if (ttmlResp.isSuccessful) {
                        val body = ttmlResp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val ttml = json.optString("ttml", "").trim()
                            if (ttml.isNotBlank()) return@runCatching ttml
                        }
                    }

                    // Fallback to LRC
                    val lrcUrl = "${baseUrl}v2/lyrics/get?title=$encTitle&artist=$encArtist" + (if (duration > 0) "&duration=$duration" else "")
                    val lrcReq = Request.Builder()
                        .url(lrcUrl)
                        .header("User-Agent", "AirBeats/1.0")
                        .header("Accept", "application/json")
                        .build()

                    val lrcResp = client.newCall(lrcReq).execute()
                    if (lrcResp.isSuccessful) {
                        val body = lrcResp.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val lrc = json.optString("lyrics", "").trim()
                            if (lrc.isNotBlank()) return@runCatching lrc
                        }
                    }
                } catch (_: Exception) {
                    continue
                }
            }

            throw IllegalStateException("Lyrics not found on YouLyPlus")
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
