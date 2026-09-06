package com.darkxvenom.airbeats.lyrics.providers

import android.content.Context
import com.darkxvenom.airbeats.constants.EnableUnisonLyricsKey
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

object AirBeatsUnisonLyricsProvider : LyricsProvider {
    override val name: String = "Unison"

    private const val BASE_URL = "https://unison.boidu.dev/"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableUnisonLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Try search by title and artist
            val query = "$title $artist".trim()
            if (query.isNotBlank()) {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                var searchUrl = "${BASE_URL}search?q=$encodedQuery"
                if (duration > 0) {
                    searchUrl += "&d=$duration"
                }

                val searchRequest = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", "AirBeats/1.0")
                    .header("Accept", "application/json")
                    .build()

                val searchResp = client.newCall(searchRequest).execute()
                if (searchResp.isSuccessful) {
                    val searchBody = searchResp.body?.string()
                    if (!searchBody.isNullOrBlank()) {
                        val searchJson = JSONObject(searchBody)
                        if (searchJson.optBoolean("success", false)) {
                            val dataArr = searchJson.optJSONArray("data")
                            if (dataArr != null && dataArr.length() > 0) {
                                for (i in 0 until dataArr.length()) {
                                    val item = dataArr.getJSONObject(i)
                                    val lrc = item.optString("lyrics", "").trim()
                                    if (lrc.isNotBlank()) {
                                        return@runCatching lrc
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Try by videoId
            val cleanId = if (id.startsWith("JS:")) "" else id.trim()
            if (cleanId.isNotBlank()) {
                val videoUrl = "${BASE_URL}lyrics?v=$cleanId"
                val videoRequest = Request.Builder()
                    .url(videoUrl)
                    .header("User-Agent", "AirBeats/1.0")
                    .header("Accept", "application/json")
                    .build()

                val videoResp = client.newCall(videoRequest).execute()
                if (videoResp.isSuccessful) {
                    val videoBody = videoResp.body?.string()
                    if (!videoBody.isNullOrBlank()) {
                        val videoJson = JSONObject(videoBody)
                        if (videoJson.optBoolean("success", false)) {
                            val dataObj = videoJson.optJSONObject("data")
                            val lrc = dataObj?.optString("lyrics", "")?.trim()
                            if (!lrc.isNullOrBlank()) {
                                return@runCatching lrc
                            }
                        }
                    }
                }
            }

            throw IllegalStateException("Lyrics not found on Unison")
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
