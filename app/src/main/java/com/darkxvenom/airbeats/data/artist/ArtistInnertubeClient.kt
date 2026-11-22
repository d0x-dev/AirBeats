/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of AirBeats and is licensed under GPL-3.0.
 */

package com.darkxvenom.airbeats.data.artist

import android.util.Log
import com.darkxvenom.airbeats.backend.Net
import com.darkxvenom.airbeats.data.auth.GoogleClock
import com.darkxvenom.airbeats.data.auth.LikeStatus
import com.darkxvenom.airbeats.data.auth.buildFullSapisidAuthorization
import com.darkxvenom.airbeats.data.auth.buildSapisidHash
import com.darkxvenom.airbeats.data.auth.delegatedSessionId
import com.darkxvenom.airbeats.data.auth.mergeSetCookies
import com.darkxvenom.airbeats.data.auth.normalizeDataSyncId
import com.darkxvenom.airbeats.data.auth.sapisidFrom
import com.darkxvenom.airbeats.innertube.YouTube
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.io.IOException
import java.util.Locale

object ArtistInnertubeClient {
    private const val TAG = "ArtistInnertube"
    private const val MUSIC_BASE = "https://music.youtube.com/youtubei/v1"
    private const val MUSIC_ORIGIN = "https://music.youtube.com"
    private const val WEB_REMIX_VERSION = "1.20261104.00.00"
    private const val WEB_VERSION = "2.20261104.00.00"
    private const val WEB_REMIX_CLIENT_ID = "67"
    private const val WEB_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
    private const val DEFAULT_ATR_SECONDS = 5L
    private const val CPN_ALPHABET =
        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val CONFIG_LOGGED_IN = Regex(""""LOGGED_IN"\s*:\s*(true|false)""")
    private val CONFIG_DATASYNC_ID = Regex(""""DATASYNC_ID"\s*:\s*"([^"]+)"""")
    private val CONFIG_PAGE_ID = Regex(""""DELEGATED_SESSION_ID"\s*:\s*"([^"]+)"""")
    private val CONFIG_SESSION_INDEX = Regex(""""SESSION_INDEX"\s*:\s*"?(\d+)""")
    private val CONFIG_VISITOR_DATA = Regex(""""VISITOR_DATA"\s*:\s*"([^"]+)"""")
    private val CONFIG_CLIENT_VERSION = Regex(""""INNERTUBE_CLIENT_VERSION"\s*:\s*"([^"]+)"""")

    private data class SessionScope(
        val dataSyncId: String?,
        val pageId: String?,
        val authUser: String,
        val clientVersion: String?,
    )

    private class HttpStatusException(
        val code: Int,
        val endpoint: String,
        val responseSnippet: String,
    ) : IOException("Innertube $endpoint HTTP $code${responseSnippet.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()}")

    data class PlaybackTracking(
        val playbackUrl: String,
        val watchtimeUrl: String?,
        val atrUrl: String?,
        val atrAfterSeconds: Long,
    )

    @Volatile
    private var scope: SessionScope? = null

    @Volatile
    private var cachedSignatureTimestamp: Int? = null

    private val scopeLock = Mutex()

    var cookie: String?
        get() = YouTube.cookie
        set(value) {
            YouTube.cookie = value
        }

    var activeCookie: String?
        get() = cookie
        set(value) {
            cookie = value
        }

    var visitorData: String?
        get() = YouTube.visitorData
        set(value) {
            YouTube.visitorData = value
        }

    var dataSyncId: String?
        get() = YouTube.dataSyncId
        set(value) {
            YouTube.dataSyncId = value
        }

    val currentLanguage: String
        get() = YouTube.locale.hl.ifBlank { "en" }

    val currentRegion: String
        get() = YouTube.locale.gl.ifBlank { "US" }

    private val acceptLanguageHeader: String
        get() = "$currentLanguage,$currentLanguage;q=0.9,en;q=0.8"

    fun absorbSetCookies(setCookieHeaders: List<String>) {
        if (setCookieHeaders.isEmpty()) return
        val currentCookie = cookie ?: return
        val merged = mergeSetCookies(currentCookie, setCookieHeaders) ?: return
        cookie = merged
    }

    suspend fun browse(
        browseId: String,
        params: String? = null,
        anonymous: Boolean = false,
        persistScopeRecovery: Boolean = true,
    ): JSONObject = postMusic(
        endpoint = "browse",
        anonymous = anonymous,
        persistScopeRecovery = persistScopeRecovery,
        allowAnonymousFallback = !browseId.startsWith("FE"),
    ) {
        put("browseId", browseId)
        if (params != null) put("params", params)
    }

    suspend fun browseContinuation(
        token: String,
        anonymous: Boolean = false,
        allowAnonymousFallback: Boolean = false,
    ): JSONObject = postMusic(
        endpoint = "browse",
        anonymous = anonymous,
        allowAnonymousFallback = allowAnonymousFallback,
    ) {
        put("continuation", token)
    }

    suspend fun next(
        videoId: String? = null,
        playlistId: String? = null,
        playlistSetVideoId: String? = null,
        index: Int? = null,
        params: String? = null,
        continuation: String? = null,
    ): JSONObject = postMusic(
        endpoint = "next",
        allowAnonymousFallback = true,
    ) {
        if (videoId != null) put("videoId", videoId)
        if (playlistId != null) put("playlistId", playlistId)
        if (playlistSetVideoId != null) put("playlistSetVideoId", playlistSetVideoId)
        if (index != null) put("index", index)
        if (params != null) put("params", params)
        if (continuation != null) put("continuation", continuation)
        put("isAudioOnly", true)
    }

    suspend fun search(
        query: String,
        params: String? = null,
        anonymous: Boolean = true,
    ): JSONObject = postMusic(
        endpoint = "search",
        region = currentRegion,
        anonymous = anonymous,
        allowAnonymousFallback = true,
    ) {
        put("query", query)
        if (params != null) put("params", params)
    }

    suspend fun rateVideo(videoId: String, status: LikeStatus) {
        requireSession()
        val endpoint = when (status) {
            LikeStatus.LIKE -> "like/like"
            LikeStatus.DISLIKE -> "like/dislike"
            LikeStatus.INDIFFERENT -> "like/removelike"
        }
        val response = postMusicWithWebFallback(endpoint) {
            put("target", JSONObject().put("videoId", videoId))
        }
        checkInnertubeError(response, "YouTube Music refused rating")
    }

    suspend fun ratePlaylist(playlistId: String, saved: Boolean) {
        requireSession()
        val endpoint = if (saved) "like/like" else "like/removelike"
        val response = postMusicWithWebFallback(endpoint) {
            put("target", JSONObject().put("playlistId", playlistId.removePrefix("VL")))
        }
        checkInnertubeError(response, "YouTube Music refused playlist bookmark")
    }

    suspend fun setSubscribed(channelId: String, subscribed: Boolean) {
        requireSession()
        val endpoint = if (subscribed) "subscription/subscribe" else "subscription/unsubscribe"
        val response = postMusicWithWebFallback(endpoint) {
            put("channelIds", JSONArray().put(channelId))
        }
        checkInnertubeError(response, "YouTube Music refused subscription change")
    }

    suspend fun createPlaylist(
        title: String,
        privacyStatus: String = "PRIVATE",
        description: String? = null,
        videoIds: List<String> = emptyList(),
    ): String {
        requireSession()
        val response = postMusicWithWebFallback("playlist/create") {
            put("title", title)
            if (!description.isNullOrBlank()) put("description", description)
            put("privacyStatus", privacyStatus)
            if (videoIds.isNotEmpty()) {
                put("videoIds", JSONArray().apply { videoIds.forEach(::put) })
            }
        }
        checkInnertubeError(response, "YouTube Music refused playlist creation")
        val pid = response.optString("playlistId").takeIf { it.isNotBlank() }
            ?: findStringDeep(response, "playlistId")
            ?: error("Playlist created on YouTube Music but no playlistId returned")
        return pid
    }

    suspend fun deletePlaylist(playlistId: String) {
        requireSession()
        val response = postMusicWithWebFallback("playlist/delete") {
            put("playlistId", playlistId.removePrefix("VL"))
        }
        checkInnertubeError(response, "YouTube Music refused playlist deletion")
    }

    suspend fun renamePlaylist(playlistId: String, title: String) {
        editPlaylist(playlistId) {
            put(
                JSONObject()
                    .put("action", "ACTION_SET_PLAYLIST_NAME")
                    .put("playlistName", title),
            )
        }
    }

    suspend fun addToPlaylist(playlistId: String, videoIds: List<String>): Map<String, String> {
        if (videoIds.isEmpty()) return emptyMap()
        val response = editPlaylist(playlistId) {
            videoIds.forEach { vid ->
                put(
                    JSONObject()
                        .put("action", "ACTION_ADD_VIDEO")
                        .put("addedVideoId", vid),
                )
            }
        }
        val arr = response.optJSONArray("playlistEditResults")
            ?: error("YouTube Music did not confirm the playlist add operation")
        val out = linkedMapOf<String, String>()
        for (i in 0 until arr.length()) {
            val added = arr.optJSONObject(i)?.optJSONObject("playlistEditVideoAddedResultData") ?: continue
            val vid = added.optString("videoId").takeIf { it.isNotBlank() } ?: continue
            val setId = added.optString("setVideoId").takeIf { it.isNotBlank() } ?: continue
            out[vid] = setId
        }
        return out
    }

    suspend fun removeFromPlaylist(playlistId: String, entries: List<Pair<String, String>>) {
        if (entries.isEmpty()) return
        editPlaylist(playlistId) {
            entries.forEach { (setVideoId, videoId) ->
                put(
                    JSONObject()
                        .put("action", "ACTION_REMOVE_VIDEO")
                        .put("setVideoId", setVideoId)
                        .put("removedVideoId", videoId),
                )
            }
        }
    }

    suspend fun signatureTimestamp(videoId: String): Int? = withContext(Dispatchers.IO) {
        cachedSignatureTimestamp?.let { return@withContext it }
        runCatching {
            YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
        }
            .onFailure { Log.w(TAG, "signatureTimestamp lookup failed: ${it.message}") }
            .getOrNull()
            ?.also { cachedSignatureTimestamp = it }
    }

    suspend fun playbackTracking(videoId: String, signatureTimestamp: Int?): PlaybackTracking? {
        if (cookie == null) return null
        ensureSessionScope()
        val response = postMusic("player") {
            put("videoId", videoId)
            put("contentCheckOk", true)
            put("racyCheckOk", true)
            put(
                "playbackContext",
                JSONObject().put(
                    "contentPlaybackContext",
                    JSONObject().apply {
                        put("html5Preference", "HTML5_PREF_WANTS")
                        put("referer", "$MUSIC_ORIGIN/watch?v=$videoId")
                        if (signatureTimestamp != null) put("signatureTimestamp", signatureTimestamp)
                    },
                ),
            )
        }
        val tracking = response.optJSONObject("playbackTracking") ?: return null
        val playbackUrl = tracking.optJSONObject("videostatsPlaybackUrl")
            ?.optString("baseUrl")?.takeIf { it.isNotBlank() } ?: return null
        val watchtimeUrl = tracking.optJSONObject("videostatsWatchtimeUrl")
            ?.optString("baseUrl")?.takeIf { it.isNotBlank() }
        val atrObj = tracking.optJSONObject("atrUrl")
        val atrUrl = atrObj?.optString("baseUrl")?.takeIf { it.isNotBlank() }
        val atrAfter = atrObj?.optLong("elapsedMediaTimeSeconds", DEFAULT_ATR_SECONDS)
            ?.takeIf { it > 0L } ?: DEFAULT_ATR_SECONDS
        return PlaybackTracking(
            playbackUrl = playbackUrl,
            watchtimeUrl = watchtimeUrl,
            atrUrl = atrUrl,
            atrAfterSeconds = atrAfter,
        )
    }

    suspend fun pingPlayback(baseUrl: String, cpn: String): Int =
        pingStats(baseUrl, cpn) {}

    suspend fun pingWatchtime(baseUrl: String, cpn: String, seconds: Long, final: Boolean = false): Int =
        pingStats(baseUrl, cpn) {
            addQueryParameter("st", "0")
            addQueryParameter("et", seconds.toString())
            addQueryParameter("cmt", seconds.toString())
            addQueryParameter("state", if (final) "paused" else "playing")
        }

    suspend fun pingAtr(baseUrl: String, cpn: String): Int = withContext(Dispatchers.IO) {
        if (cookie != null) ensureSessionScope()
        val session = scope
        val url = baseUrl.toHttpUrl().newBuilder()
            .addQueryParameter("cpn", cpn)
            .build()
        val builder = Request.Builder()
            .url(url)
            .get()
        applyStatsHeaders(builder, session)
        Net.client.newCall(builder.build()).execute().use { it.code }
    }

    fun newCpn(): String = (1..16).map { CPN_ALPHABET.random() }.joinToString("")

    private suspend fun pingStats(
        baseUrl: String,
        cpn: String,
        extras: okhttp3.HttpUrl.Builder.() -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        if (cookie != null) ensureSessionScope()
        val session = scope
        val url = baseUrl.toHttpUrl().newBuilder()
            .addQueryParameter("ver", "2")
            .addQueryParameter("c", "WEB_REMIX")
            .addQueryParameter("cver", currentClientVersion())
            .addQueryParameter("cpn", cpn)
            .addQueryParameter("cplayer", "UNIPLAYER")
            .addQueryParameter("cbr", "Chrome")
            .addQueryParameter("cbrver", "141.0.0.0")
            .addQueryParameter("cos", "Windows")
            .addQueryParameter("cosver", "10.0")
            .addQueryParameter("hl", currentLanguage.replace('-', '_'))
            .addQueryParameter("cr", currentRegion)
            .apply(extras)
            .build()
        val builder = Request.Builder().url(url).get()
        applyStatsHeaders(builder, session)
        Net.client.newCall(builder.build()).execute().use { it.code }
    }

    private fun applyStatsHeaders(builder: Request.Builder, session: SessionScope?) {
        builder.header("X-Origin", MUSIC_ORIGIN)
            .header("Origin", MUSIC_ORIGIN)
            .header("Referer", "$MUSIC_ORIGIN/")
            .header("User-Agent", WEB_USER_AGENT)
            .header("X-Youtube-Bootstrap-Logged-In", "true")
        visitorData?.let { builder.header("X-Goog-Visitor-Id", it) }
        cookie?.let { c ->
            builder.header("Cookie", c)
            builder.header("X-Goog-AuthUser", authUserFor(session))
            pageIdFor(session)?.let { builder.header("X-Goog-PageId", it) }
            buildFullSapisidAuthorization(
                cookieHeader = c,
                origin = MUSIC_ORIGIN,
                userSessionId = dataSyncIdFor(session),
            )?.let { auth ->
                builder.header("Authorization", auth)
            }
        }
    }

    private suspend fun postMusicWithWebFallback(
        endpoint: String,
        bodyExtras: JSONObject.() -> Unit,
    ): JSONObject {
        val primaryError = try {
            return postMusic(endpoint, bodyExtras = bodyExtras)
        } catch (e: HttpStatusException) {
            Log.w(TAG, "WEB_REMIX $endpoint failed (${e.code}), retrying on WEB: ${e.responseSnippet}")
            e
        }
        return try {
            postMusicRawUrl(
                url = "https://www.youtube.com/youtubei/v1/$endpoint",
                clientName = "WEB",
                clientVersion = WEB_VERSION,
                clientId = "1",
                origin = "https://www.youtube.com",
                anonymous = false,
                allowAnonymousFallback = false,
                bodyExtras = bodyExtras,
            )
        } catch (webErr: Throwable) {
            Log.w(TAG, "WEB fallback for $endpoint also failed: ${webErr.message}")
            throw primaryError
        }
    }

    private suspend fun editPlaylist(
        playlistId: String,
        actions: JSONArray.() -> Unit,
    ): JSONObject {
        requireSession()
        val response = postMusicWithWebFallback("browse/edit_playlist") {
            put("playlistId", playlistId.removePrefix("VL"))
            put("actions", JSONArray().apply(actions))
        }
        val status = response.optString("status")
        if (status.isNotBlank() && status != "STATUS_SUCCEEDED") {
            error("YouTube Music refused edit ($status)")
        }
        checkInnertubeError(response, "YouTube Music refused edit")
        return response
    }

    private fun checkInnertubeError(response: JSONObject, fallbackMessage: String) {
        val err = response.optJSONObject("error") ?: return
        val msg = err.optString("message").takeIf { it.isNotBlank() } ?: fallbackMessage
        error(msg)
    }

    private fun requireSession() {
        if (cookie.isNullOrBlank()) error("Sign in with Google to sync with YouTube Music")
    }

    private suspend fun ensureSessionScope() {
        val activeCookie = cookie ?: return
        val existing = scope
        if (existing != null && !existing.clientVersion.isNullOrBlank()) return
        scopeLock.withLock {
            val lockedExisting = scope
            if ((lockedExisting != null && !lockedExisting.clientVersion.isNullOrBlank()) || cookie != activeCookie) return
            runCatching { fetchSessionScope(activeCookie) }
                .onFailure { Log.w(TAG, "could not read session scope: ${it.message}") }
                .getOrNull()
                ?.let { fresh ->
                    scope = fresh
                }
        }
    }

    private suspend fun fetchSessionScope(sessionCookie: String): SessionScope? = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url("$MUSIC_ORIGIN/")
            .get()
            .header("User-Agent", WEB_USER_AGENT)
            .header("Accept-Language", acceptLanguageHeader)
            .header("Cookie", sessionCookie)
            .header("X-Youtube-Bootstrap-Logged-In", "true")
        val html = Net.client.newCall(builder.build()).execute().use { res ->
            absorbSetCookies(res.headers("Set-Cookie"))
            res.body?.string().orEmpty()
        }
        val signedIn = CONFIG_LOGGED_IN.find(html)?.groupValues?.get(1) == "true"
        val clientVersion = CONFIG_CLIENT_VERSION.find(html)?.groupValues?.get(1)
        if (!signedIn) {
            return@withContext clientVersion?.let { SessionScope(null, null, "0", it) }
        }
        val rawDataSync = CONFIG_DATASYNC_ID.find(html)?.groupValues?.get(1)?.trim()
        val pageId = CONFIG_PAGE_ID.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
            ?: delegatedSessionId(rawDataSync)
        val dataSyncId = normalizeDataSyncId(rawDataSync)
        val authUser = CONFIG_SESSION_INDEX.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: "0"
        CONFIG_VISITOR_DATA.find(html)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let {
            visitorData = it
        }
        SessionScope(dataSyncId, pageId, authUser, clientVersion)
    }

    private fun currentClientVersion(): String = scope?.clientVersion ?: WEB_REMIX_VERSION

    private fun pageIdFor(session: SessionScope?): String? = session?.pageId

    private fun dataSyncIdFor(session: SessionScope?): String? = session?.dataSyncId ?: dataSyncId

    private fun authUserFor(session: SessionScope?): String = session?.authUser ?: "0"

    private suspend fun postMusic(
        endpoint: String,
        region: String? = null,
        anonymous: Boolean = false,
        allowAnonymousFallback: Boolean = false,
        persistScopeRecovery: Boolean = true,
        bodyExtras: JSONObject.() -> Unit = {},
    ): JSONObject = postMusicRawUrl(
        url = "$MUSIC_BASE/$endpoint",
        clientName = "WEB_REMIX",
        clientVersion = currentClientVersion(),
        clientId = WEB_REMIX_CLIENT_ID,
        origin = MUSIC_ORIGIN,
        region = region,
        anonymous = anonymous,
        allowAnonymousFallback = allowAnonymousFallback,
        persistScopeRecovery = persistScopeRecovery,
        bodyExtras = bodyExtras,
    )

    private suspend fun postMusicRawUrl(
        url: String,
        clientName: String,
        clientVersion: String,
        clientId: String,
        origin: String,
        region: String? = null,
        anonymous: Boolean = false,
        allowAnonymousFallback: Boolean = false,
        persistScopeRecovery: Boolean = true,
        bodyExtras: JSONObject.() -> Unit = {},
    ): JSONObject {
        if (!anonymous && cookie != null) {
            ensureSessionScope()
        }
        val session = scope
        val finalUrl = "$url?prettyPrint=false"
        val resolvedVersion = if (clientName == "WEB_REMIX") (session?.clientVersion ?: clientVersion) else clientVersion

        fun buildPayload(includeVisitorData: Boolean = true): String = JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", clientName)
                    put("clientVersion", resolvedVersion)
                    put("hl", currentLanguage)
                    if (region != null) put("gl", region)
                    if (includeVisitorData) visitorData?.let { put("visitorData", it) }
                })
                put("user", JSONObject().put("lockedSafetyMode", false))
                put("request", JSONObject().apply {
                    put("useSsl", true)
                })
            })
            bodyExtras()
        }.toString()

        val activeCookie = if (anonymous) null else cookie
        val activeDataSyncId = if (anonymous) null else dataSyncIdFor(session)
        val activePageId = if (anonymous) null else pageIdFor(session)
        val activeAuthUser = authUserFor(session)

        return executeInnertubePost(
            finalUrl = finalUrl,
            payload = buildPayload(includeVisitorData = true),
            origin = origin,
            clientId = clientId,
            clientVersion = resolvedVersion,
            activeCookie = activeCookie,
            authUser = activeAuthUser,
            pageId = activePageId,
            userSessionId = activeDataSyncId,
        )
    }

    private suspend fun executeInnertubePost(
        finalUrl: String,
        payload: String,
        origin: String,
        clientId: String,
        clientVersion: String,
        activeCookie: String?,
        authUser: String?,
        pageId: String?,
        userSessionId: String?,
    ): JSONObject {
        val builder = Request.Builder()
            .url(finalUrl)
            .header("Accept-Language", acceptLanguageHeader)
            .header("X-Origin", origin)
            .header("Origin", origin)
            .header("Referer", "$origin/")
            .header("X-YouTube-Client-Name", clientId)
            .header("X-YouTube-Client-Version", clientVersion)
            .header("User-Agent", WEB_USER_AGENT)
        visitorData?.let { builder.header("X-Goog-Visitor-Id", it) }
        if (!activeCookie.isNullOrBlank()) {
            builder.header("Cookie", activeCookie)
            builder.header("X-Youtube-Bootstrap-Logged-In", "true")
            authUser?.takeIf { it.isNotBlank() }?.let { builder.header("X-Goog-AuthUser", it) }
            pageId?.let { builder.header("X-Goog-PageId", it) }
            buildFullSapisidAuthorization(
                cookieHeader = activeCookie,
                origin = origin,
                userSessionId = userSessionId,
            )?.let { builder.header("Authorization", it) }
        }
        val request = builder.post(payload.toRequestBody(JSON_MEDIA_TYPE)).build()
        return withContext(Dispatchers.IO) {
            Net.client.newCall(request).execute().use { res ->
                absorbSetCookies(res.headers("Set-Cookie"))
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    throw HttpStatusException(res.code, finalUrl, text.take(240))
                }
                val json = JSONObject(text)
                val apiError = json.optJSONObject("error")
                if (apiError != null) {
                    val apiCode = apiError.optInt("code", 500).takeIf { it in 400..599 } ?: 500
                    val message = apiError.optString("message").ifBlank { "YouTube API error" }
                    throw HttpStatusException(apiCode, finalUrl, message.take(240))
                }
                json
            }
        }
    }

    fun findStringDeep(root: JSONObject, targetKey: String): String? {
        val iterator = root.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            if (key == targetKey) {
                val str = root.optString(key)
                if (str.isNotBlank()) return str
            }
            when (val v = root.opt(key)) {
                is JSONObject -> {
                    val found = findStringDeep(v, targetKey)
                    if (found != null) return found
                }
                is JSONArray -> {
                    for (i in 0 until v.length()) {
                        val item = v.optJSONObject(i) ?: continue
                        val found = findStringDeep(item, targetKey)
                        if (found != null) return found
                    }
                }
            }
        }
        return null
    }
}
