/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of AirBeats and is licensed under GPL-3.0.
 */

package com.darkxvenom.airbeats.data.auth

import com.darkxvenom.airbeats.data.library.HomeFeedParser
import com.darkxvenom.airbeats.data.model.HomeFeedItem
import com.darkxvenom.airbeats.data.model.HomeItemKind
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses authenticated YouTube Music responses (`account_menu`, `accounts_list`,
 * `FEmusic_liked_*`, `FEmusic_library_corpus_artists`, `FEmusic_history`, and `FEmusic_home`)
 * using Innertube parser rules.
 */
object YouTubeAccountParsers {

    private val SYSTEM_AUTO_PLAYLIST_IDS = setOf("LM", "SE", "NE", "SS", "SR", "LL", "WL")

    fun parseAccountInfo(response: JSONObject): YouTubeAccountInfo? {
        val header = HomeFeedParser.collectRenderers(response, "activeAccountHeaderRenderer").firstOrNull()
        if (header != null) {
            val name = header.optJSONObject("accountName").runsText()
                .ifBlank { header.optJSONObject("accountName")?.optString("simpleText").orEmpty() }
            if (name.isNotBlank()) {
                val email = header.optJSONObject("email").runsText()
                    .ifBlank { header.optJSONObject("email")?.optString("simpleText").orEmpty() }
                val handle = header.optJSONObject("channelHandle").runsText()
                    .ifBlank { header.optJSONObject("channelHandle")?.optString("simpleText").orEmpty() }
                val avatar = header.optJSONObject("accountPhoto")?.optJSONArray("thumbnails").bestThumbnail()
                return YouTubeAccountInfo(
                    name = name,
                    email = email.ifBlank { handle },
                    handle = handle,
                    avatarUrl = avatar,
                )
            }
        }
        val firstChannel = parseAccountChannels(response).firstOrNull() ?: return null
        return YouTubeAccountInfo(
            name = firstChannel.name,
            email = firstChannel.subtitle,
            handle = firstChannel.subtitle,
            avatarUrl = firstChannel.thumbnailUrl,
        )
    }

    fun parseAccountChannels(root: JSONObject): List<AccountChannel> =
        HomeFeedParser.collectRenderers(root, "accountItem").mapNotNull { item ->
            val name = item.optJSONObject("accountName").runsText()
                .ifBlank { item.optJSONObject("accountName")?.optString("simpleText").orEmpty() }
            if (name.isBlank()) return@mapNotNull null
            val rawDataSyncId = findStringDeep(item, "datasyncIdToken")
            val pageId = findStringDeep(item, "pageId") ?: delegatedSessionId(rawDataSyncId)
            val dataSyncId = normalizeDataSyncId(rawDataSyncId)
            if (pageId == null && dataSyncId == null) return@mapNotNull null
            val subtitle = item.optJSONObject("channelHandle").runsText()
                .ifBlank { item.optJSONObject("channelHandle")?.optString("simpleText").orEmpty() }
                .ifBlank { item.optJSONObject("accountByline").runsText() }
                .ifBlank { item.optJSONObject("accountByline")?.optString("simpleText").orEmpty() }
            val thumb = item.optJSONObject("accountPhoto")?.optJSONArray("thumbnails").bestThumbnail()
            AccountChannel(
                name = name,
                subtitle = subtitle,
                thumbnailUrl = thumb,
                pageId = pageId,
                dataSyncId = dataSyncId,
                activeOnWeb = item.optBoolean("isSelected", false),
            )
        }.distinctBy { it.key }

    fun parseLibraryPlaylists(root: JSONObject): Pair<List<HomeFeedItem>, String?> {
        val out = linkedMapOf<String, HomeFeedItem>()

        HomeFeedParser.collectRenderers(root, "musicTwoRowItemRenderer").forEach { renderer ->
            val title = renderer.optJSONObject("title").runsText().trim()
            val nav = renderer.optJSONObject("navigationEndpoint")
            val browseId = nav?.optJSONObject("browseEndpoint")?.optString("browseId")?.takeIf { it.isNotBlank() }
                ?: nav?.optJSONObject("watchPlaylistEndpoint")?.optString("playlistId")?.takeIf { it.isNotBlank() }?.let { "VL$it" }
            if (title.isNotBlank() && browseId != null) {
                val clean = browseId.removePrefix("VL")
                if (clean !in SYSTEM_AUTO_PLAYLIST_IDS && !clean.startsWith("FE") && !clean.startsWith("UC")) {
                    val subtitle = renderer.optJSONObject("subtitle").runsText().trim()
                    val thumb = renderer.optJSONObject("thumbnailRenderer")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                        .bestThumbnail()
                    val item = HomeFeedItem(
                        kind = HomeItemKind.PLAYLIST,
                        title = title,
                        subtitle = subtitle.ifBlank { "Playlist" },
                        thumbnailUrl = thumb,
                        browseId = browseId,
                        watchPlaylistId = clean,
                        isExplicit = renderer.hasExplicitBadge(),
                    )
                    out.putIfAbsent(item.stableKey, item)
                }
            }
        }

        HomeFeedParser.collectRenderers(root, "musicResponsiveListItemRenderer").forEach { renderer ->
            val nav = renderer.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val browseId = nav?.optString("browseId")?.takeIf { it.isNotBlank() }
            val title = renderer.columnText(0)
            if (title.isNotBlank() && browseId != null) {
                val clean = browseId.removePrefix("VL")
                if (clean !in SYSTEM_AUTO_PLAYLIST_IDS && !clean.startsWith("FE") && !clean.startsWith("UC")) {
                    val subtitle = renderer.columnText(1)
                    val thumb = renderer.optJSONObject("thumbnail")
                        ?.optJSONObject("musicThumbnailRenderer")
                        ?.optJSONObject("thumbnail")
                        ?.optJSONArray("thumbnails")
                        .bestThumbnail()
                    val item = HomeFeedItem(
                        kind = HomeItemKind.PLAYLIST,
                        title = title,
                        subtitle = subtitle.ifBlank { "Playlist" },
                        thumbnailUrl = thumb,
                        browseId = browseId,
                        watchPlaylistId = clean,
                        isExplicit = renderer.hasExplicitBadge(),
                    )
                    out.putIfAbsent(item.stableKey, item)
                }
            }
        }

        return out.values.toList() to HomeFeedParser.continuationToken(root)
    }

    fun parseLibraryArtists(root: JSONObject): Pair<List<HomeFeedItem>, String?> {
        val out = linkedMapOf<String, HomeFeedItem>()

        HomeFeedParser.collectRenderers(root, "musicTwoRowItemRenderer").forEach { renderer ->
            val title = renderer.optJSONObject("title").runsText().trim()
            val browseId = renderer.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")?.optString("browseId")?.takeIf { it.startsWith("UC") }
            if (title.isNotBlank() && browseId != null) {
                val subtitle = renderer.optJSONObject("subtitle").runsText().trim()
                val thumb = renderer.optJSONObject("thumbnailRenderer")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails")
                    .bestThumbnail()
                val item = HomeFeedItem(
                    kind = HomeItemKind.ARTIST,
                    title = title,
                    subtitle = subtitle.ifBlank { "Artist" },
                    thumbnailUrl = thumb,
                    browseId = browseId,
                    artistName = title,
                    artistId = browseId,
                )
                out.putIfAbsent(item.stableKey, item)
            }
        }

        HomeFeedParser.collectRenderers(root, "musicResponsiveListItemRenderer").forEach { renderer ->
            val title = renderer.columnText(0)
            val browseId = renderer.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")?.optString("browseId")?.takeIf { it.startsWith("UC") }
                ?: renderer.optJSONArray("flexColumns")?.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)
                    ?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
                    ?.optString("browseId")?.takeIf { it.startsWith("UC") }
            if (title.isNotBlank() && browseId != null) {
                val subtitle = renderer.columnText(1)
                val thumb = renderer.optJSONObject("thumbnail")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails")
                    .bestThumbnail()
                val item = HomeFeedItem(
                    kind = HomeItemKind.ARTIST,
                    title = title,
                    subtitle = subtitle.ifBlank { "Artist" },
                    thumbnailUrl = thumb,
                    browseId = browseId,
                    artistName = title,
                    artistId = browseId,
                )
                out.putIfAbsent(item.stableKey, item)
            }
        }

        return out.values.toList() to HomeFeedParser.continuationToken(root)
    }

    fun parseTracks(root: JSONObject): Pair<List<HomeFeedItem>, String?> {
        val out = linkedMapOf<String, HomeFeedItem>()

        HomeFeedParser.collectRenderers(root, "musicResponsiveListItemRenderer").forEach { renderer ->
            val vid = renderer.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
                ?: renderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
                ?: renderer.optJSONObject("overlay")?.optJSONObject("musicItemThumbnailOverlayRenderer")
                    ?.optJSONObject("content")?.optJSONObject("musicPlayButtonRenderer")
                    ?.optJSONObject("playNavigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
            val title = renderer.columnText(0)
            if (vid != null && title.isNotBlank()) {
                val subtitle = renderer.columnText(1)
                val thumb = renderer.optJSONObject("thumbnail")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails")
                    .bestThumbnail()
                val item = HomeFeedItem(
                    kind = HomeItemKind.SONG,
                    videoId = vid,
                    title = title,
                    subtitle = subtitle,
                    artistName = subtitle.split("•", "·", ",").firstOrNull()?.trim() ?: subtitle,
                    thumbnailUrl = thumb,
                    isExplicit = renderer.hasExplicitBadge(),
                )
                out.putIfAbsent(item.stableKey, item)
            }
        }

        HomeFeedParser.collectRenderers(root, "musicTwoRowItemRenderer").forEach { renderer ->
            val vid = renderer.optJSONObject("navigationEndpoint")?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
            val title = renderer.optJSONObject("title").runsText().trim()
            if (vid != null && title.isNotBlank()) {
                val subtitle = renderer.optJSONObject("subtitle").runsText().trim()
                val thumb = renderer.optJSONObject("thumbnailRenderer")
                    ?.optJSONObject("musicThumbnailRenderer")
                    ?.optJSONObject("thumbnail")
                    ?.optJSONArray("thumbnails")
                    .bestThumbnail()
                val item = HomeFeedItem(
                    kind = HomeItemKind.SONG,
                    videoId = vid,
                    title = title,
                    subtitle = subtitle,
                    artistName = subtitle.split("•", "·", ",").firstOrNull()?.trim() ?: subtitle,
                    thumbnailUrl = thumb,
                    isExplicit = renderer.hasExplicitBadge(),
                )
                out.putIfAbsent(item.stableKey, item)
            }
        }

        return out.values.toList() to HomeFeedParser.continuationToken(root)
    }

    private fun JSONObject?.columnText(col: Int): String {
        if (this == null) return ""
        val runs = optJSONArray("flexColumns")?.optJSONObject(col)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")
        return runs.runsText()
    }

    private fun JSONObject?.runsText(): String {
        if (this == null) return ""
        val runs = optJSONArray("runs") ?: return optString("simpleText")
        return runs.runsText()
    }

    private fun JSONArray?.runsText(): String {
        if (this == null) return ""
        return buildString {
            for (i in 0 until length()) append(optJSONObject(i)?.optString("text").orEmpty())
        }.trim()
    }

    private fun JSONArray?.bestThumbnail(): String? {
        if (this == null || length() == 0) return null
        return optJSONObject(length() - 1)?.optString("url")?.takeIf { it.isNotBlank() }?.highQualityThumbnail()
    }

    private fun JSONObject.hasExplicitBadge(): Boolean {
        val badges = optJSONArray("badges") ?: optJSONArray("subtitleBadges")
        if (badges != null) {
            for (i in 0 until badges.length()) {
                val icon = badges.optJSONObject(i)?.optJSONObject("musicInlineBadgeRenderer")
                    ?.optJSONObject("icon")?.optString("iconType")
                if (icon == "MUSIC_EXPLICIT_BADGE") return true
            }
        }
        return false
    }

    private fun findStringDeep(node: Any?, key: String): String? {
        when (node) {
            is JSONObject -> {
                val str = node.optString(key)
                if (str.isNotBlank()) return str
                val it = node.keys()
                while (it.hasNext()) {
                    val found = findStringDeep(node.opt(it.next()), key)
                    if (found != null) return found
                }
            }
            is JSONArray -> {
                for (i in 0 until node.length()) {
                    val found = findStringDeep(node.opt(i), key)
                    if (found != null) return found
                }
            }
        }
        return null
    }
}
