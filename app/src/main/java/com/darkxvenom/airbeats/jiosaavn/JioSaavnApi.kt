package com.darkxvenom.airbeats.jiosaavn

import com.darkxvenom.airbeats.innertube.models.Artist
import com.darkxvenom.airbeats.innertube.models.Album
import com.darkxvenom.airbeats.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object JioSaavnApi {
    private val client = OkHttpClient.Builder().build()
    private const val BASE_URL = "https://www.jiosaavn.com/api.php"
    private const val DES_KEY = "38346591"

    private val headers = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .add("Accept", "application/json, text/plain, */*")
        .add("Accept-Language", "en-US,en;q=0.9")
        .build()

    val streamUrlCache = ConcurrentHashMap<String, String>()

    suspend fun getTrendingSongs(): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        runCatching {
            // First attempt: JioSaavn's official Trending Today editorial chart playlist (listid=110858205)
            val request = Request.Builder()
                .url("$BASE_URL?__call=playlist.getDetails&listid=110858205&_format=json&_marker=0&cc=in")
                .headers(headers)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty body")
            val json = JSONObject(body)
            val songsArray = json.optJSONArray("songs")
            val songs = mutableListOf<SongItem>()

            if (songsArray != null && songsArray.length() > 0) {
                for (i in 0 until songsArray.length()) {
                    val item = songsArray.optJSONObject(i) ?: continue
                    parseSong(item)?.let { songs.add(it) }
                }
            }

            // Fallback: If playlist was empty, query content.getTrending
            if (songs.isEmpty()) {
                val fallbackReq = Request.Builder()
                    .url("$BASE_URL?__call=content.getTrending&_format=json&_marker=0&cc=in")
                    .headers(headers)
                    .build()
                val fallbackResp = client.newCall(fallbackReq).execute()
                val fallbackBody = fallbackResp.body?.string()
                if (!fallbackBody.isNullOrBlank()) {
                    val trendingArray = JSONArray(fallbackBody)
                    for (i in 0 until trendingArray.length()) {
                        val entry = trendingArray.optJSONObject(i) ?: continue
                        if (entry.optString("type") == "song") {
                            val details = entry.optJSONObject("details") ?: continue
                            parseSong(details)?.let { songs.add(it) }
                        }
                    }
                }
            }

            if (songs.isEmpty()) throw Exception("No trending songs found")
            songs
        }
    }

    suspend fun searchSongs(query: String): Result<List<SongItem>> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("$BASE_URL?__call=search.getResults&_format=json&_marker=0&cc=in&p=1&n=30&q=$encodedQuery")
                .headers(headers)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: throw Exception("Empty body")
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: throw Exception("No search results")
            val songs = mutableListOf<SongItem>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                parseSong(item)?.let { songs.add(it) }
            }
            songs
        }
    }

    suspend fun getStreamUrl(id: String): String? = withContext(Dispatchers.IO) {
        val mappedId = if (id.startsWith("JS:")) id else "JS:$id"
        streamUrlCache[mappedId]?.let { return@withContext it }
        val originalId = mappedId.removePrefix("JS:")
        com.darkxvenom.airbeats.backend.StreamBackend.saavnStreamUrl(originalId)?.let {
            streamUrlCache[mappedId] = it
            return@withContext it
        }

        try {
            val request = Request.Builder()
                .url("$BASE_URL?__call=song.getDetails&cc=in&_marker=0%3F_marker%3D0&_format=json&pids=$originalId")
                .headers(headers)
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val songObj = json.optJSONObject(originalId)
                ?: json.optJSONArray("songs")?.optJSONObject(0)
            if (songObj != null) {
                val encryptedUrl = songObj.optString("encrypted_media_url")
                if (encryptedUrl.isNotBlank()) {
                    val supports320 = songObj.optString("320kbps").equals("true", ignoreCase = true) ||
                        songObj.optBoolean("320kbps", false)
                    val streamUrl = decryptMediaUrl(encryptedUrl, supports320)
                    if (streamUrl != null) {
                        streamUrlCache[mappedId] = streamUrl
                        return@withContext streamUrl
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    suspend fun findMatch(title: String, artist: String? = null, durationSec: Int? = null): SongItem? = withContext(Dispatchers.IO) {
        val queries = TrackMatcher.queries(title, artist.orEmpty())
        for (q in queries) {
            val candidates = searchSongs(q).getOrNull() ?: continue
            val bestMatch = TrackMatcher.best(candidates, title, artist.orEmpty(), durationSec)
            if (bestMatch != null) return@withContext bestMatch
        }
        val rawCandidates = searchSongs("$title ${artist.orEmpty()}".trim()).getOrNull() ?: return@withContext null
        TrackMatcher.best(rawCandidates, title, artist.orEmpty(), durationSec)
    }

    suspend fun findMatchAndStreamUrl(title: String, artist: String? = null, durationSec: Int? = null): String? = withContext(Dispatchers.IO) {
        val match = findMatch(title, artist, durationSec) ?: return@withContext null
        getStreamUrl(match.id)
    }

    /**
     * Decrypts JioSaavn's DES-ECB encrypted media URL and converts it to high-fidelity 320kbps MP4 audio
     * when supported by the catalogue.
     */
    private fun decryptMediaUrl(encryptedUrl: String, supports320: Boolean = true): String? {
        if (encryptedUrl.isBlank()) return null
        return try {
            val keySpec = SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decoded = android.util.Base64.decode(encryptedUrl.trim(), android.util.Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)
            val rawUrl = String(decrypted, Charsets.UTF_8).trim()
            val suffix = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)$").find(rawUrl)
            if (suffix != null) {
                val ext = suffix.groupValues[2]
                if (supports320) {
                    rawUrl.replaceRange(suffix.range, "_320.$ext")
                } else {
                    rawUrl
                }
            } else {
                if (supports320) {
                    rawUrl.replace("_96.mp4", "_320.mp4").replace("_160.mp4", "_320.mp4")
                } else {
                    rawUrl
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSong(item: JSONObject): SongItem? {
        val rawId = item.optString("id").ifEmpty { return null }
        val id = "JS:$rawId"
        val rawName = item.optString("song").ifEmpty { item.optString("title").ifEmpty { item.optString("name") } }
        if (rawName.isEmpty()) return null
        val name = unescapeHtml(rawName)

        val durationStr = item.optString("duration")
        val duration = durationStr.toIntOrNull() ?: item.optInt("duration", 0)

        val albumRaw = item.optString("album").ifEmpty { item.optJSONObject("album")?.optString("name") ?: "" }
        val album = if (albumRaw.isNotBlank()) Album(unescapeHtml(albumRaw), "") else null

        val artists = mutableListOf<Artist>()
        val artistMap = item.optJSONObject("artistMap")
        if (artistMap != null) {
            val keys = artistMap.keys()
            while (keys.hasNext()) {
                val aName = keys.next()
                if (aName.isNotBlank() && aName != "primary_artists" && aName != "featured_artists") {
                    artists.add(Artist(name = unescapeHtml(aName), id = null))
                }
            }
        }
        if (artists.isEmpty()) {
            val primary = item.optString("primary_artists")
            if (primary.isNotBlank()) {
                primary.split(",").forEach {
                    val trimmed = unescapeHtml(it.trim())
                    if (trimmed.isNotBlank()) {
                        artists.add(Artist(name = trimmed, id = null))
                    }
                }
            } else {
                artists.add(Artist(name = "Unknown Artist", id = null))
            }
        }

        var thumbnailUrl = item.optString("image")
        if (thumbnailUrl.isEmpty()) {
            val images = item.optJSONArray("image")
            if (images != null && images.length() > 0) {
                thumbnailUrl = images.getJSONObject(images.length() - 1).optString("url")
            }
        }
        // Upgrade to high-resolution 500x500 album art
        thumbnailUrl = getHighQualityImage(thumbnailUrl)

        // Pre-decrypt and cache stream URL if present in response
        val encryptedUrl = item.optString("encrypted_media_url")
        if (encryptedUrl.isNotBlank()) {
            val supports320 = item.optString("320kbps").equals("true", ignoreCase = true) ||
                item.optBoolean("320kbps", false)
            decryptMediaUrl(encryptedUrl, supports320)?.let { streamUrl ->
                streamUrlCache[id] = streamUrl
            }
        }

        return SongItem(
            id = id,
            title = name,
            artists = artists,
            album = album,
            duration = duration,
            thumbnail = thumbnailUrl,
            explicit = item.optInt("explicit_content", 0) == 1 || item.optBoolean("explicit_content", false)
        )
    }

    private fun getHighQualityImage(url: String): String {
        return url.replace("150x150", "500x500")
            .replace("50x50", "500x500")
    }

    private fun unescapeHtml(text: String): String {
        return text.replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
