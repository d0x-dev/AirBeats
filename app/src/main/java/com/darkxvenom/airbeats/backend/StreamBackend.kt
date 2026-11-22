/*
 * AirBeats - unified stream backend (JioSaavn + YouTube).
 * Mirrors iTune: YouTube via NewPipeExtractor StreamInfo, JioSaavn via api.php (v4) + DES decode.
 */
package com.darkxvenom.airbeats.backend

import android.util.Base64
import com.darkxvenom.airbeats.constants.AudioQuality
import com.darkxvenom.airbeats.innertube.NewPipeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object StreamBackend {
    data class Resolved(
        val url: String,
        val mimeType: String,
        val codec: String?,
        val bitrateBps: Int,
        val itag: Int,
        val contentLength: Long?,
        val expiresInSeconds: Int,
        val source: String,
    )

    private const val TTL_MS = 20 * 60_000L
    private val cache = ConcurrentHashMap<String, Pair<Long, Resolved>>()
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    fun invalidate(id: String) { cache.keys.removeAll { it.startsWith("$id|") } }

    /** One call for every provider: "JS:<id>" -> JioSaavn, anything else -> YouTube. */
    suspend fun resolve(
        id: String,
        quality: AudioQuality = AudioQuality.AUTO,
        avoidCodecs: Set<String> = emptySet(),
        force: Boolean = false,
    ): Result<Resolved> = withContext(Dispatchers.IO) {
        val key = "$id|$quality"
        if (!force) cache[key]?.let { (at, r) -> if (System.currentTimeMillis() - at < TTL_MS) return@withContext Result.success(r) }
        runCatching {
            if (id.startsWith("JS:")) saavn(id.removePrefix("JS:"))
            else youtube(id, quality, avoidCodecs)
        }.onSuccess { cache[key] = System.currentTimeMillis() to it }
    }

    // ── YouTube (NewPipeExtractor, same as iTune) ───────────────────────────
    private fun youtube(id: String, quality: AudioQuality, avoid: Set<String>): Resolved {
        val info = NewPipeUtils.streamInfo(id)
        val all = info.audioStreams.orEmpty().filter {
            !it.content.isNullOrBlank() && it.audioTrackType != AudioTrackType.DUBBED
        }
        val usable = all.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }.ifEmpty { all }
        val noAvoid = usable.filter { s -> avoid.none { a -> s.codec.orEmpty().contains(a, true) } }
        val cands = noAvoid.ifEmpty { usable }
        check(cands.isNotEmpty()) { "No audio streams for $id" }
        val target = when (quality) {
            AudioQuality.LOW -> 70_000; AudioQuality.MEDIUM -> 128_000
            AudioQuality.HIGH, AudioQuality.AUTO -> null
        }
        val best = if (target == null) {
            cands.maxWith(compareBy({ it.averageBitrate }, { if (it.format == MediaFormat.M4A) 1 else 0 }))
        } else {
            cands.minWith(compareBy({ kotlin.math.abs(it.averageBitrate * 1000 - target) }, { if (it.format == MediaFormat.M4A) 0 else 1 }))
        }
        val url = best.content
        val mime = best.format?.mimeType ?: "audio/mp4"
        val expire = url.toHttpUrlOrNull()?.queryParameter("expire")?.toLongOrNull()
        val ttl = expire?.let { (it - System.currentTimeMillis() / 1000).toInt() }?.takeIf { it > 60 } ?: 5 * 3600
        return Resolved(
            url = url, mimeType = mime, codec = best.codec,
            bitrateBps = best.averageBitrate * 1000,
            itag = runCatching { best.itagItem?.id }.getOrNull() ?: 0,
            contentLength = runCatching { best.itagItem?.contentLength?.takeIf { it > 0 } }.getOrNull(),
            expiresInSeconds = ttl, source = "YouTube",
        )
    }

    // ── JioSaavn (api.php v4 + DES, same as iTune) ──────────────────────────
    /** Plain URL lookup for a JioSaavn id (no "JS:" prefix). */
    fun saavnStreamUrl(id: String): String? = runCatching { saavn(id.removePrefix("JS:")).url }.getOrNull()

    private fun saavn(id: String): Resolved {
        val url = "https://www.jiosaavn.com/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("__call", "song.getDetails").addQueryParameter("pids", id)
            .addQueryParameter("_format", "json").addQueryParameter("_marker", "0")
            .addQueryParameter("api_version", "4").addQueryParameter("ctx", "android").build()
        val req = Request.Builder().url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
            .header("Accept-Language", "en-IN,en;q=0.9").header("Cookie", "explicit_content=1").build()
        val body = http.newCall(req).execute().use { check(it.isSuccessful) { "JioSaavn HTTP ${it.code}" }; it.body!!.string() }
        val root = JSONObject(body)
        val song = root.optJSONArray("songs")?.optJSONObject(0) ?: root.optJSONObject(id)
            ?: root.keys().asSequence().mapNotNull { root.optJSONObject(it) }.first()
        val more = song.optJSONObject("more_info") ?: song
        val enc = more.optString("encrypted_media_url").ifBlank { error("No media url") }
        var stream = decrypt(enc).ifBlank { error("Decrypt failed") }
        var kbps = 160
        Regex("_(48|96|160|320)\\.(mp4|aac|mp3)(?=[?#]|$)", RegexOption.IGNORE_CASE).find(stream)?.let { m ->
            kbps = m.groupValues[1].toInt()
            if (more.optString("320kbps").equals("true", true)) {
                stream = stream.replaceRange(m.range, "_320.${m.groupValues[2]}"); kbps = 320
            }
        }
        return Resolved(stream, "audio/mp4", "mp4a.40.2", kbps * 1000, 0, null, 3600, "JioSaavn")
    }

    private fun decrypt(enc: String): String = runCatching {
        val key = SecretKeySpec("38346591".toByteArray(Charsets.UTF_8), "DES")
        val c = Cipher.getInstance("DES/ECB/PKCS5Padding").apply { init(Cipher.DECRYPT_MODE, key) }
        String(c.doFinal(Base64.decode(enc, Base64.DEFAULT)), Charsets.UTF_8).trim()
    }.getOrDefault("")
}
