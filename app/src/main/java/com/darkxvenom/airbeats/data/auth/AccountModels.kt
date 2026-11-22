/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of AirBeats and is licensed under GPL-3.0.
 */

package com.darkxvenom.airbeats.data.auth

import androidx.compose.runtime.Immutable
import java.security.MessageDigest
import java.util.Locale

private const val MUSIC_ORIGIN = "https://music.youtube.com"
private val API_SID_NAMES = listOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")

enum class LikeStatus { LIKE, DISLIKE, INDIFFERENT }

enum class WebSessionMode {
    SIGN_IN,
    SWITCH_CHANNEL,
}

enum class PlaylistStorageTarget {
    LOCAL,
    GOOGLE_ACCOUNT,
    SPOTIFY_ACCOUNT,
}

@Immutable
data class YouTubeAccountInfo(
    val name: String,
    val email: String = "",
    val handle: String = "",
    val avatarUrl: String? = null,
)

@Immutable
data class AccountChannel(
    val name: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val pageId: String? = null,
    val dataSyncId: String? = null,
    val activeOnWeb: Boolean = false,
) {
    val key: String
        get() = pageId ?: dataSyncId ?: name
}

@Immutable
data class YouTubeProfile(
    val profileId: String,
    val name: String,
    val handle: String = "",
    val avatar: String? = null,
    val pageId: String? = null,
    val dataSyncId: String? = null,
    val authUser: String? = null,
    val isBrandAccount: Boolean = false,
)

@Immutable
data class GoogleAccountSession(
    val accountId: String,
    val cookie: String,
    val name: String = "",
    val email: String = "",
    val profiles: List<YouTubeProfile> = emptyList(),
    val activeProfileId: String? = null,
    val visitorData: String? = null,
)

@Immutable
data class YouTubeAccountUiState(
    val isSignedIn: Boolean = false,
    val activeAccountId: String? = null,
    val activeProfileId: String? = null,
    val accountName: String = "",
    val accountEmail: String = "",
    val accountHandle: String = "",
    val avatarUrl: String? = null,
    val channelName: String? = null,
    val isSyncing: Boolean = false,
    val lastSyncedAtMs: Long = 0L,
    val lastSyncError: String? = null,
    val channels: List<AccountChannel> = emptyList(),
    val accounts: List<GoogleAccountSession> = emptyList(),
) {
    val activeSessionKey: String?
        get() = activeAccountId?.let { "$it:${activeProfileId.orEmpty()}" }
}

/** Checks cookie entries by exact name for a SAPISID signing secret. */
fun hasApiSid(cookieHeader: String?): Boolean {
    if (cookieHeader.isNullOrBlank()) return false
    return parseCookieHeader(cookieHeader).keys.any { it in API_SID_NAMES }
}

/**
 * Checks that the cookie header contains both a SAPISID signing secret and a
 * real Google session cookie (`LOGIN_INFO`, `SID`, or `__Secure-*PSID`), avoiding
 * premature capture of half-signed redirect cookies.
 */
fun looksSignedInCookies(cookieHeader: String?): Boolean {
    if (cookieHeader.isNullOrBlank()) return false
    val jar = parseCookieHeader(cookieHeader)
    val hasSidSecret = jar.keys.any { it in API_SID_NAMES }
    if (!hasSidSecret) return false
    return jar.containsKey("LOGIN_INFO") ||
        jar.containsKey("SID") ||
        jar.containsKey("__Secure-1PSID") ||
        jar.containsKey("__Secure-3PSID")
}

/** Parses a raw Cookie header into a map preserving non-empty entries. */
fun parseCookieHeader(cookieHeader: String): Map<String, String> {
    if (cookieHeader.isBlank()) return emptyMap()
    val out = linkedMapOf<String, String>()
    cookieHeader.split(';').forEach { entry ->
        val sep = entry.indexOf('=')
        if (sep <= 0) return@forEach
        val name = entry.substring(0, sep).trim()
        val value = entry.substring(sep + 1).trim()
        if (name.isNotEmpty() && value.isNotEmpty()) {
            out[name] = value
        }
    }
    return out
}

/** Extracts the highest-priority signing secret from a YouTube cookie header. */
fun sapisidFrom(cookieHeader: String): String? {
    val jar = parseCookieHeader(cookieHeader)
    return API_SID_NAMES.firstNotNullOfOrNull { jar[it] }
}

fun normalizeDataSyncId(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (!value.contains("||")) return value
    return value.substringAfter("||").trim().takeIf { it.isNotBlank() }
        ?: value.substringBefore("||").trim().takeIf { it.isNotBlank() }
}

fun delegatedSessionId(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (!value.contains("||")) return null
    val first = value.substringBefore("||").trim()
    val second = value.substringAfter("||").trim()
    return if (first.isNotBlank() && second.isNotBlank()) first else null
}

/**
 * Builds a Google Innertube SAPISID authorization token. When a user session id is
 * available, it is prefixed to the timestamp in the SHA-1 input and the trailing
 * `_u` marker is added, matching YouTube's current cookie-auth format.
 */
fun buildSapisidHash(
    sapisid: String,
    origin: String = MUSIC_ORIGIN,
    timestampSeconds: Long = GoogleClock.nowSeconds(),
    scheme: String = "SAPISIDHASH",
    userSessionId: String? = null,
): String {
    val userId = userSessionId?.takeIf { it.isNotBlank() }
    val hashInput = buildString {
        if (userId != null) append(userId).append(' ')
        append(timestampSeconds).append(' ').append(sapisid).append(' ').append(origin)
    }
    val digest = MessageDigest.getInstance("SHA-1")
        .digest(hashInput.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(Locale.ROOT, it) }
    val userMarker = if (userId != null) "_u" else ""
    return "$scheme ${timestampSeconds}_$digest$userMarker"
}

/**
 * Builds the full Google Innertube Authorization header for every available SAPISID cookie.
 * The optional user session id must come from DATASYNC_ID's user-session component; the
 * delegated session id is sent separately as X-Goog-PageId.
 */
fun buildFullSapisidAuthorization(
    cookieHeader: String,
    origin: String = MUSIC_ORIGIN,
    timestampSeconds: Long = GoogleClock.nowSeconds(),
    userSessionId: String? = null,
): String? {
    val jar = parseCookieHeader(cookieHeader)
    val baseSecret = jar["SAPISID"] ?: jar["__Secure-3PAPISID"] ?: jar["__Secure-1PAPISID"] ?: return null
    val parts = linkedSetOf<String>()
    parts.add(buildSapisidHash(baseSecret, origin, timestampSeconds, "SAPISIDHASH", userSessionId))
    jar["__Secure-1PAPISID"]?.let { secret1p ->
        parts.add(buildSapisidHash(secret1p, origin, timestampSeconds, "SAPISID1PHASH", userSessionId))
    }
    jar["__Secure-3PAPISID"]?.let { secret3p ->
        parts.add(buildSapisidHash(secret3p, origin, timestampSeconds, "SAPISID3PHASH", userSessionId))
    }
    return parts.joinToString(" ")
}

/**
 * Merges HTTP `Set-Cookie` response headers into an existing serialized cookie header,
 * preserving updated rotating tokens like `__Secure-1PSIDTS`, `__Secure-3PSIDTS`, and `SIDCC`.
 * Returns `null` if no effective changes were made.
 */
fun mergeSetCookies(existingCookieHeader: String?, setCookieHeaders: List<String>): String? {
    if (existingCookieHeader.isNullOrBlank() || setCookieHeaders.isEmpty()) return null
    val jar = parseCookieHeader(existingCookieHeader).toMutableMap()
    var changed = false
    setCookieHeaders.forEach { raw ->
        val firstPart = raw.substringBefore(';').trim()
        val eq = firstPart.indexOf('=')
        if (eq <= 0) return@forEach
        val name = firstPart.substring(0, eq).trim()
        val value = firstPart.substring(eq + 1).trim()
        val deleted = value.isEmpty() || raw.contains("max-age=0", ignoreCase = true)
        if (deleted) {
            if (jar.remove(name) != null) changed = true
        } else if (jar[name] != value) {
            jar[name] = value
            changed = true
        }
    }
    if (!changed || jar.isEmpty()) return null
    return jar.entries.joinToString("; ") { (k, v) -> "$k=$v" }
}
