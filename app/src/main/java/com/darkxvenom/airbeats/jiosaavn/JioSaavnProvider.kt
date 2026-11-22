/*
 * Adapted from iTune's JioSaavnProvider by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 * This adaptation follows the source project's GPL-3.0 licensing terms.
 */
package com.darkxvenom.airbeats.jiosaavn

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * iTune-style JioSaavn lookup: split all credited artists, search each one
 * separately, require an exact normalized title + artist match, then make
 * iTune's first-word artist pass. Null means the caller should use YouTube.
 */
object JioSaavnProvider {
    private const val TAG = "JioSaavnProvider"
    private const val API = "https://www.jiosaavn.com/api.php"
    private const val TTL_MS = 20 * 60_000L
    private const val MAX_ARTISTS = 4

    private data class Hit(
        val id: String,
        val title: String,
        val artists: List<String>,
        val encryptedUrl: String,
        val has320: Boolean,
    )
    private data class Cached(val url: String?, val at: Long)
    private val cache = ConcurrentHashMap<String, Cached>()
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun resolve(
        trackId: String,
        title: String,
        artist: String,
        excludedUrls: Set<String> = emptySet(),
        force: Boolean = false,
    ): String? = withContext(Dispatchers.IO) {
        if (title.isBlank() || artist.isBlank()) return@withContext null
        val cached = cache[trackId]
        if (!force && cached != null && System.currentTimeMillis() - cached.at < TTL_MS) {
            return@withContext cached.url?.takeIf { it !in excludedUrls }
        }
        val result = runCatching { find(title, artist) }
            .onFailure { Log.w(TAG, "Lookup failed for '$title': ${it.message}", it) }
            .getOrNull()
            ?.takeIf { it !in excludedUrls }
        cache[trackId] = Cached(result, System.currentTimeMillis())
        if (result == null) Log.d(TAG, "No matching JioSaavn stream for '$title'; falling back to YouTube")
        result
    }

    fun invalidate(trackId: String) { cache.remove(trackId) }

    private fun find(title: String, artist: String): String? {
        val artists = splitArtists(artist).take(MAX_ARTISTS)
        if (artists.isEmpty()) return null
        val fetched = mutableListOf<Pair<String, List<Hit>>>()

        // Pass 1: exact normalized title and exact individual artist.
        for (name in artists) {
            val hits = search("$title $name")
            fetched += name to hits
            val hit = hits.firstOrNull { candidate ->
                norm(candidate.title) == norm(title) &&
                    candidate.artists.flatMap(::splitArtists).any { compact(it) == compact(name) }
            } ?: continue
            streamFor(hit)?.let { return it }
        }

        // Pass 2: exact title and first word of the individual artist.
        for ((name, hits) in fetched) {
            val first = norm(name).substringBefore(' ').takeIf { it.isNotBlank() } ?: continue
            val hit = hits.firstOrNull { candidate ->
                norm(candidate.title) == norm(title) &&
                    candidate.artists.flatMap { norm(it).split(' ') }.any { it == first }
            } ?: continue
            streamFor(hit)?.let { return it }
        }
        return null
    }

    private fun norm(value: String): String = value.lowercase()
        .replace(Regex("\\(.*?\\)|\\[.*?]"), " ")
        .replace(Regex("\\s[-–]\\s(from|feat|ft|with)\\b.*"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()

    private fun compact(value: String): String = norm(value).replace(" ", "")

    private val separators = Regex(
        "\\s*(?:,|&|;|\\+|/|\\||•|·|×|\\band\\b|\\bx\\b|\\bfeat\\.?|\\bft\\.?|\\bfeaturing\\b|\\bwith\\b|\\bvs\\.?)\\s*",
        RegexOption.IGNORE_CASE,
    )
    private val statNoise = Regex(
        "^\\d[\\d.,]*\\s*[kmb]?\\s*(views?|plays?|likes?|subscribers?|listeners?|streams?)$",
        RegexOption.IGNORE_CASE,
    )
    private fun splitArtists(value: String): List<String> = value.split(separators)
        .map { it.replace(Regex("\\(.*?\\)|\\[.*?]"), " ").replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotBlank() && !statNoise.matches(it) && norm(it).isNotBlank() }
        .distinctBy(::compact)

    private fun call(params: Map<String, String>): String? {
        val url = API.toHttpUrl().newBuilder().apply {
            params.forEach { (key, value) -> addQueryParameter(key, value) }
            addQueryParameter("_format", "json")
            addQueryParameter("_marker", "0")
            addQueryParameter("api_version", "4")
            addQueryParameter("ctx", "android")
        }.build()
        val request = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
            .header("Accept-Language", "en-IN,en;q=0.9")
            .header("Cookie", "explicit_content=1")
            .build()
        return http.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() else null
        }
    }

    private fun search(query: String): List<Hit> {
        val body = call(mapOf("__call" to "search.getResults", "q" to query, "p" to "1", "n" to "10"))
            ?: return emptyList()
        val results: JSONArray = JSONObject(body).optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { results.optJSONObject(it)?.let(::parseHit) }
    }

    private fun parseHit(song: JSONObject): Hit? {
        val id = song.optString("id").takeIf { it.isNotBlank() } ?: return null
        val more = song.optJSONObject("more_info") ?: return null
        val map = more.optJSONObject("artistMap")
        val artists = listOf("primary_artists", "featured_artists", "artists").flatMap { key ->
            map?.optJSONArray(key)?.let { array ->
                (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) }
            }.orEmpty()
        }.distinct()
        return Hit(id, song.optString("title"), artists, more.optString("encrypted_media_url"),
            more.optString("320kbps").equals("true", true))
    }

    private fun streamFor(hit: Hit): String? {
        if (hit.encryptedUrl.isNotBlank()) return decryptStream(hit.encryptedUrl, hit.has320)
        val body = call(mapOf("__call" to "song.getDetails", "pids" to hit.id)) ?: return null
        val root = JSONObject(body)
        val song = root.optJSONArray("songs")?.optJSONObject(0)
            ?: root.optJSONObject(hit.id)
            ?: root.keys().asSequence().mapNotNull { root.optJSONObject(it) }.firstOrNull()
            ?: return null
        val detail = parseHit(song) ?: return null
        return decryptStream(detail.encryptedUrl, detail.has320)
    }

    private fun decryptStream(encrypted: String, has320: Boolean): String? {
        if (encrypted.isBlank()) return null
        val raw = runCatching {
            val key = SecretKeySpec("38346591".toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding").apply { init(Cipher.DECRYPT_MODE, key) }
            String(cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT)), Charsets.UTF_8).trim()
        }.getOrDefault("")
        if (raw.isBlank()) return null
        val rendition = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)(?=[?#]|$)", RegexOption.IGNORE_CASE).find(raw)
            ?: return raw
        val offered = rendition.groupValues[1].toInt()
        val quality = if (has320) 320 else offered
        if (quality <= 96) return null
        return if (has320) raw.replaceRange(rendition.range, "_320.${rendition.groupValues[2]}") else raw
    }
}
