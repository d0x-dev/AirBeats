/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of iTune and is licensed under the GNU General Public License v3.0 (GPL-3.0).
 * Any copying, modification, or redistribution of this code must strictly comply with the
 * terms of the project LICENSE file and retain full author credit.
 */

package com.darkxvenom.itune.backend

import androidx.compose.runtime.Immutable

@Immutable
data class MusicTrack(
    val id: String,
    val title: String,
    val artists: String,
    val artistId: String? = null,
    val album: String? = null,
    val albumId: String? = null,
    val thumbnailUrl: String? = null,
    val durationSec: Long = 0,
    val liked: Boolean = false,
    val isVideo: Boolean = false,
    val isExplicit: Boolean = false,
) {
    val duration: String
        get() = if (durationSec > 0) formatMusicDuration(durationSec) else ""
}

@Immutable
data class MusicArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val subscribers: String? = null,
)

@Immutable
data class MusicPlaylistRef(
    val id: String,
    val title: String,
    val author: String = "",
    val authorId: String? = null,
    val thumbnailUrl: String? = null,
    val isAlbum: Boolean = false,
    val trackCount: Int = 0,
    val year: String? = null,
    val watchPlaylistId: String? = null,
    val isExplicit: Boolean = false,
)

@Immutable
data class UnifiedSearchResults(
    val songs: List<MusicTrack> = emptyList(),
    val artists: List<MusicArtist> = emptyList(),
    val albums: List<MusicPlaylistRef> = emptyList(),
    val playlists: List<MusicPlaylistRef> = emptyList(),
) {
    val isEmpty: Boolean
        get() = songs.isEmpty() && artists.isEmpty() && albums.isEmpty() && playlists.isEmpty()
}

enum class ReleaseType(val label: String) {
    ALBUM("Album"),
    SINGLE("Single"),
    EP("EP");

    companion object {
        fun fromLabel(label: String?): ReleaseType = when (label?.trim()?.lowercase()) {
            "single", "singles" -> SINGLE
            "ep", "eps" -> EP
            else -> ALBUM
        }
    }
}

@Immutable
data class NewReleaseAlbum(
    val browseId: String,
    val playlistId: String? = null,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val year: Int? = null,
    val releaseType: ReleaseType = ReleaseType.ALBUM,
    val thumbnailUrl: String? = null,
    val isExplicit: Boolean = false,
) {
    val subtitleLine: String
        get() = buildString {
            append(artist.ifBlank { releaseType.label })
            if (year != null) {
                append(" • ")
                append(year)
            }
        }
}

@Immutable
data class MoodAndMomentRef(
    val title: String,
    val browseId: String,
    val params: String? = null,
)

@Immutable
data class MoodsAndGenresCatalog(
    val moods: List<MoodAndMomentRef> = emptyList(),
    val genres: List<MoodAndMomentRef> = emptyList(),
)
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

enum class MoodCategoryItemType {
    PLAYLIST,
    ALBUM,
    ARTIST,
    SONG
}

@Immutable
data class MoodCategoryItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val type: MoodCategoryItemType = MoodCategoryItemType.PLAYLIST,
    val browseId: String? = null,
    val videoId: String? = null,
    val playlistId: String? = null,
    val isExplicit: Boolean = false,
)

@Immutable
data class MoodCategorySection(
    val title: String,
    val items: List<MoodCategoryItem>,
    val isGrid: Boolean = false,
)

@Immutable
data class MoodCategoryPage(
    val title: String,
    val sections: List<MoodCategorySection>,
)

@Immutable
data class LyricsLine(
    val timeMs: Long,
    val text: String,
    val words: List<WordTiming> = emptyList(),
)

@Immutable
data class WordTiming(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/** Upgrades YouTube and Google CDN album artwork to ultra high resolution (1440p QHD / Original master upload). */
fun String.highQualityThumbnail(): String {
    var url = this
    if (url.contains("googleusercontent.com") || url.contains("ggpht.com")) {
        // Request '=w1440-h1440-l90-rj': 1440p QHD master retina clarity with 90% quality, fast ~400KB load time
        url = if (url.contains("=")) {
            url.substringBeforeLast("=") + "=w1440-h1440-l90-rj"
        } else {
            "$url=w1440-h1440-l90-rj"
        }
    } else if (url.contains("i.ytimg.com/vi/")) {
        // Strip query parameters like ?sqp=... that constrain size and crop
        val cleanUrl = url.substringBefore("?")
        url = if (cleanUrl.contains(Regex("/(default|mqdefault|hqdefault|sddefault|hq720)\\.jpg"))) {
            cleanUrl.replace(Regex("/(default|mqdefault|hqdefault|sddefault|hq720)\\.jpg"), "/maxresdefault.jpg")
        } else {
            cleanUrl
        }
    }
    return url
}

/** Formats seconds into MM:SS or HH:MM:SS string. */
fun formatMusicDuration(seconds: Long): String {
    if (seconds <= 0) return ""
    val m = seconds / 60
    val s = seconds % 60
    return if (seconds >= 3600) {
        val h = seconds / 3600
        val remM = (seconds % 3600) / 60
        "%d:%02d:%02d".format(h, remM, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

/** Formats milliseconds into MM:SS string. */
fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Parses time string such as "3:45" or "1:15:20" to seconds. */
fun parseTimeString(timeStr: String): Long {
    val parts = timeStr.trim().split(":")
    return when (parts.size) {
        1 -> parts[0].toLongOrNull() ?: 0L
        2 -> (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
        3 -> (parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)
        else -> 0L
    }
}
