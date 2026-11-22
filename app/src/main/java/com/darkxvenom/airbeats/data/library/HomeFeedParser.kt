/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of AirBeats and is licensed under GPL-3.0.
 */

package com.darkxvenom.airbeats.data.library

import com.darkxvenom.airbeats.data.model.HomeFeedItem
import com.darkxvenom.airbeats.data.model.HomeFeedShelf
import com.darkxvenom.airbeats.data.model.HomeItemKind
import com.darkxvenom.airbeats.data.model.HomeShelfGroup
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses YouTube Music `browse` responses (home, explore, charts, podcasts) into typed shelves.
 * Works for both anonymous and signed-in responses; the shape is identical, only the content differs.
 */
object HomeFeedParser {

    data class Page(val shelves: List<HomeFeedShelf>, val continuation: String?)

    private val typeWords = setOf(
        "song", "album", "single", "ep", "playlist", "video", "artist", "podcast", "episode", "show",
    )

    fun parse(root: JSONObject, group: HomeShelfGroup, keyPrefix: String): Page {
        val sectionList = findFirst(root, "sectionListRenderer") ?: findFirst(root, "sectionListContinuation")
        val contents = sectionList?.optJSONArray("contents") ?: return Page(emptyList(), null)
        val shelves = mutableListOf<HomeFeedShelf>()
        for (i in 0 until contents.length()) {
            val section = contents.optJSONObject(i) ?: continue
            val shelf = parseSection(section, group, "$keyPrefix:$i") ?: continue
            shelves.add(shelf)
        }
        val token = sectionList.optJSONArray("continuations")
            ?.optJSONObject(0)
            ?.optJSONObject("nextContinuationData")
            ?.optString("continuation")
            ?.takeIf { it.isNotBlank() }
            ?: continuationToken(root)
        return Page(shelves, token)
    }

    private fun parseSection(section: JSONObject, group: HomeShelfGroup, key: String): HomeFeedShelf? {
        val carousel = section.optJSONObject("musicCarouselShelfRenderer")
            ?: section.optJSONObject("musicImmersiveCarouselShelfRenderer")
        if (carousel != null) {
            val header = carousel.optJSONObject("header")?.let {
                it.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                    ?: it.optJSONObject("musicImmersiveCarouselShelfBasicHeaderRenderer")
            }
            val title = header?.optJSONObject("title").runsText()
            val strap = header?.optJSONObject("strapline").runsText()
            val more = header?.optJSONObject("moreContentButton")
                ?.optJSONObject("buttonRenderer")
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("browseEndpoint")
                ?.optString("browseId")?.takeIf { it.isNotBlank() }
            val items = parseItems(carousel.optJSONArray("contents"))
            if (title.isBlank() || items.isEmpty()) return null
            return HomeFeedShelf(key, title, strap, items, group, more)
        }
        val shelf = section.optJSONObject("musicShelfRenderer")
        if (shelf != null) {
            val title = shelf.optJSONObject("title").runsText()
            val items = parseItems(shelf.optJSONArray("contents"))
            if (title.isBlank() || items.isEmpty()) return null
            return HomeFeedShelf(key, title, "", items, group, null)
        }
        return null
    }

    private fun parseItems(arr: JSONArray?): List<HomeFeedItem> {
        if (arr == null) return emptyList()
        val out = mutableListOf<HomeFeedItem>()
        val seen = HashSet<String>()
        for (i in 0 until arr.length()) {
            val e = arr.optJSONObject(i) ?: continue
            val item = e.optJSONObject("musicTwoRowItemRenderer")?.let(::parseTwoRow)
                ?: e.optJSONObject("musicResponsiveListItemRenderer")?.let(::parseResponsive)
                ?: e.optJSONObject("musicMultiRowListItemRenderer")?.let(::parseMultiRow)
            if (item != null && seen.add(item.stableKey)) out.add(item)
        }
        return out
    }

    // ── musicTwoRowItemRenderer: albums, playlists, artists, mixes, podcasts, videos, songs ───────

    private fun parseTwoRow(r: JSONObject): HomeFeedItem? {
        val title = r.optJSONObject("title").runsText()
        if (title.isBlank()) return null
        val subRuns = r.optJSONObject("subtitle")?.optJSONArray("runs")
        val thumb = r.optJSONObject("thumbnailRenderer")?.thumbnailUrl()
        val nav = r.optJSONObject("navigationEndpoint")
        val explicit = r.optJSONArray("subtitleBadges").hasExplicit()
        val meta = subtitleMeta(subRuns)

        val browse = nav?.optJSONObject("browseEndpoint")
        val watch = nav?.optJSONObject("watchEndpoint")
        val watchPl = nav?.optJSONObject("watchPlaylistEndpoint")

        if (browse != null) {
            val id = browse.optString("browseId").takeIf { it.isNotBlank() } ?: return null
            val pageType = browse.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType").orEmpty()
            if (id.startsWith("MPED")) {
                return HomeFeedItem(
                    kind = HomeItemKind.EPISODE, title = title, subtitle = meta.text, thumbnailUrl = thumb,
                    videoId = id.removePrefix("MPED"), browseId = id, artistName = meta.text, isExplicit = explicit,
                )
            }
            val kind = kindFromBrowse(id, pageType) ?: return null
            return HomeFeedItem(
                kind = kind, title = title, subtitle = meta.text, thumbnailUrl = thumb, browseId = id,
                artistName = meta.artistName, artistId = meta.artistId, isExplicit = explicit,
            )
        }
        if (watch != null) {
            val vid = watch.optString("videoId").takeIf { it.isNotBlank() } ?: return null
            val type = watch.optJSONObject("watchEndpointMusicSupportedConfigs")
                ?.optJSONObject("watchEndpointMusicConfig")?.optString("musicVideoType").orEmpty()
            val kind = if (type == "MUSIC_VIDEO_TYPE_OMV" || type == "MUSIC_VIDEO_TYPE_UGC") {
                HomeItemKind.VIDEO
            } else HomeItemKind.SONG
            return HomeFeedItem(
                kind = kind, title = title, subtitle = meta.text, thumbnailUrl = thumb, videoId = vid,
                watchPlaylistId = watch.optString("playlistId").takeIf { it.isNotBlank() },
                artistName = meta.artistName, artistId = meta.artistId, views = meta.views,
                albumName = meta.albumName, albumId = meta.albumId, isExplicit = explicit,
            )
        }
        if (watchPl != null) {
            val pid = watchPl.optString("playlistId").takeIf { it.isNotBlank() } ?: return null
            return HomeFeedItem(
                kind = HomeItemKind.PLAYLIST, title = title, subtitle = meta.text, thumbnailUrl = thumb,
                watchPlaylistId = pid, isExplicit = explicit,
            )
        }
        return null
    }

    // ── musicResponsiveListItemRenderer: songs, and chart/“top artists” rows ──────────────────────

    private fun parseResponsive(r: JSONObject): HomeFeedItem? {
        val thumb = r.optJSONObject("thumbnail")?.thumbnailUrl()
        val titleRuns = r.flexRuns(0)
        val title = titleRuns.joinText()
        if (title.isBlank()) return null
        val explicit = r.optJSONArray("badges").hasExplicit()
        val vid = r.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }
            ?: titleRuns?.optJSONObject(0)?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
            ?: findFirst(r.optJSONObject("overlay"), "watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
        val meta = subtitleMeta(r.flexRuns(1))

        if (vid != null) {
            return HomeFeedItem(
                kind = HomeItemKind.SONG, title = title, subtitle = meta.text, thumbnailUrl = thumb, videoId = vid,
                artistName = meta.artistName, artistId = meta.artistId, views = meta.views,
                albumName = meta.albumName, albumId = meta.albumId, isExplicit = explicit,
            )
        }
        val browse = r.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            ?: titleRuns?.optJSONObject(0)?.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
        val id = browse?.optString("browseId")?.takeIf { it.isNotBlank() } ?: return null
        val pageType = browse.optJSONObject("browseEndpointContextSupportedConfigs")
            ?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType").orEmpty()
        val kind = kindFromBrowse(id, pageType) ?: return null
        return HomeFeedItem(
            kind = kind, title = title, subtitle = meta.text, thumbnailUrl = thumb, browseId = id,
            artistName = meta.artistName, artistId = meta.artistId, isExplicit = explicit,
        )
    }

    // ── musicMultiRowListItemRenderer: podcast episodes ────────────────────────────────────────────

    private fun parseMultiRow(r: JSONObject): HomeFeedItem? {
        val title = r.optJSONObject("title").runsText()
        if (title.isBlank()) return null
        val vid = findFirst(r.optJSONObject("onTap"), "watchEndpoint")?.optString("videoId")
            ?.takeIf { it.isNotBlank() } ?: return null
        val sub = r.optJSONObject("subtitle").runsText()
        return HomeFeedItem(
            kind = HomeItemKind.EPISODE, title = title, subtitle = sub,
            thumbnailUrl = r.optJSONObject("thumbnail")?.thumbnailUrl(), videoId = vid, artistName = sub,
        )
    }

    private fun kindFromBrowse(id: String, pageType: String): HomeItemKind? = when {
        pageType.contains("ALBUM") -> HomeItemKind.ALBUM
        pageType.contains("ARTIST") || pageType.contains("USER_CHANNEL") -> HomeItemKind.ARTIST
        pageType.contains("PODCAST") -> HomeItemKind.PODCAST
        pageType.contains("PLAYLIST") -> HomeItemKind.PLAYLIST
        id.startsWith("MPRE") || id.startsWith("OLAK") -> HomeItemKind.ALBUM
        id.startsWith("UC") -> HomeItemKind.ARTIST
        id.startsWith("MPSP") -> HomeItemKind.PODCAST
        id.startsWith("VL") || id.startsWith("PL") || id.startsWith("RD") -> HomeItemKind.PLAYLIST
        else -> null
    }

    // ── Radio / “up next” panel tracks (used for mixes and “because you listened”) ────────────────

    fun parsePanelTracks(root: JSONObject): List<HomeFeedItem> {
        val out = mutableListOf<HomeFeedItem>()
        val seen = HashSet<String>()
        walk(root) { key, obj ->
            if (key != "playlistPanelVideoRenderer") return@walk
            val vid = obj.optString("videoId").takeIf { it.isNotBlank() } ?: return@walk
            if (!seen.add(vid)) return@walk
            val meta = subtitleMeta(
                obj.optJSONObject("longBylineText")?.optJSONArray("runs")
                    ?: obj.optJSONObject("shortBylineText")?.optJSONArray("runs"),
            )
            out.add(
                HomeFeedItem(
                    kind = HomeItemKind.SONG,
                    videoId = vid,
                    title = obj.optJSONObject("title").runsText(),
                    artistName = meta.artistName,
                    artistId = meta.artistId,
                    albumName = meta.albumName,
                    albumId = meta.albumId,
                    thumbnailUrl = obj.optJSONObject("thumbnail")?.optJSONArray("thumbnails").bestThumb(),
                    isExplicit = obj.optJSONArray("badges").hasExplicit(),
                ),
            )
        }
        return out
    }

    /** Circular artist photo from an artist page header. */
    fun parseArtistPhoto(root: JSONObject): String? {
        val header = findFirst(root, "musicImmersiveHeaderRenderer") ?: findFirst(root, "musicVisualHeaderRenderer")
        return header?.optJSONObject("thumbnail")?.thumbnailUrl()
    }

    fun continuationToken(root: Any): String? {
        val endpoint = collectRenderers(root, "continuationItemRenderer").firstOrNull()
            ?.optJSONObject("continuationEndpoint")
        val cmd = endpoint?.optJSONObject("continuationCommand")
            ?: endpoint?.optJSONObject("commandExecutorCommand")?.optJSONArray("commands")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optJSONObject("continuationCommand") }.firstOrNull()
            }
        cmd?.optString("token")?.takeIf { it.isNotBlank() }?.let { return it }
        return collectRenderers(root, "nextContinuationData").firstOrNull()
            ?.optString("continuation")?.takeIf { it.isNotBlank() }
    }

    fun collectRenderers(node: Any?, key: String): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        walk(node) { k, obj ->
            if (k == key) out.add(obj)
        }
        return out
    }

    // ── Subtitle runs → artist / album / readable text ────────────────────────────────────────────

    private data class Meta(val text: String, val artistName: String, val artistId: String?, val albumName: String?, val albumId: String?, val views: String?)
    private class SubRun(val raw: String, val browseId: String?, val pageType: String?) {
        val text: String get() = raw.trim()
        val isArtist: Boolean get() = pageType?.contains("ARTIST") == true || browseId?.startsWith("UC") == true
        val isAlbum: Boolean get() = pageType?.contains("ALBUM") == true || browseId?.startsWith("MPRE") == true
    }
    private val viewsRegex = Regex("(?i)\\d[\\d.,]*\\s*[kmbt]?\\s*(?:views?|plays?|listeners?|subscribers?|streams?)\\b")
    private fun segmentText(seg: List<SubRun>): String = seg.joinToString("") { it.raw }.replace(Regex("\\s+"), " ").trim()
    private fun subtitleMeta(runs: JSONArray?): Meta {
        if (runs == null) return Meta("", "", null, null, null, null)
        val segments = mutableListOf<MutableList<SubRun>>(mutableListOf())
        for (i in 0 until runs.length()) {
            val run = runs.optJSONObject(i) ?: continue
            val raw = run.optString("text"); val t = raw.trim()
            if (t == "•" || t == "·") { if (segments.last().isNotEmpty()) segments.add(mutableListOf()); continue }
            if (t.isBlank()) continue
            val be = run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")
            val pt = be?.optJSONObject("browseEndpointContextSupportedConfigs")?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType")
            segments.last().add(SubRun(raw, be?.optString("browseId")?.takeIf { it.isNotBlank() }, pt))
        }
        segments.removeAll { it.isEmpty() }
        segments.firstOrNull()?.singleOrNull()?.let { r -> if (r.browseId == null && r.text.lowercase() in typeWords) segments.removeAt(0) }
        var views: String? = null
        val rest = mutableListOf<List<SubRun>>()
        for (seg in segments) {
            val text = segmentText(seg)
            if (views == null && seg.none { it.browseId != null } && viewsRegex.containsMatchIn(text)) views = text
            else if (!(seg.none { it.browseId != null } && viewsRegex.containsMatchIn(text))) rest.add(seg)
        }
        val unlinked = rest.filter { seg -> seg.all { it.browseId == null } }
        val artistSeg = rest.firstOrNull { seg -> seg.any { it.isArtist } } ?: unlinked.firstOrNull()
        val albumSeg = rest.firstOrNull { seg -> seg.any { it.isAlbum } } ?: unlinked.firstOrNull { it !== artistSeg && !segmentText(it).all { c -> c.isDigit() || c.code == 32 } }
        val artistRuns = artistSeg?.filter { it.isArtist }.orEmpty()
        val artistName = if (artistRuns.isNotEmpty()) artistRuns.joinToString(", ") { it.text } else artistSeg?.let(::segmentText).orEmpty()
        val albumRun = albumSeg?.firstOrNull { it.isAlbum }
        return Meta(segments.joinToString(" • ") { segmentText(it) }, artistName, artistRuns.firstOrNull()?.browseId, albumRun?.text ?: albumSeg?.let(::segmentText), albumRun?.browseId, views)
    }

    // ── JSON helpers ───────────────────────────────────────────────────────────────────────────────

    private fun JSONObject?.runsText(): String {
        if (this == null) return ""
        val runs = optJSONArray("runs") ?: return optString("simpleText")
        return runs.joinText()
    }

    private fun JSONArray?.joinText(): String {
        if (this == null) return ""
        return buildString {
            for (i in 0 until length()) append(optJSONObject(i)?.optString("text").orEmpty())
        }.trim()
    }

    private fun JSONObject.flexRuns(col: Int): JSONArray? =
        optJSONArray("flexColumns")?.optJSONObject(col)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")?.optJSONArray("runs")

    private fun JSONObject.thumbnailUrl(): String? =
        (optJSONObject("musicThumbnailRenderer") ?: optJSONObject("croppedSquareThumbnailRenderer"))
            ?.optJSONObject("thumbnail")?.optJSONArray("thumbnails").bestThumb()

    private fun JSONArray?.bestThumb(): String? {
        if (this == null || length() == 0) return null
        return optJSONObject(length() - 1)?.optString("url")?.takeIf { it.isNotBlank() }?.highQualityThumbnail()
    }

    private fun JSONArray?.hasExplicit(): Boolean {
        if (this == null) return false
        for (i in 0 until length()) {
            val icon = optJSONObject(i)?.optJSONObject("musicInlineBadgeRenderer")
                ?.optJSONObject("icon")?.optString("iconType")
            if (icon == "MUSIC_EXPLICIT_BADGE") return true
        }
        return false
    }

    private fun findFirst(node: Any?, key: String): JSONObject? {
        when (node) {
            is JSONObject -> {
                val direct = node.optJSONObject(key)
                if (direct != null) return direct
                val it = node.keys()
                while (it.hasNext()) findFirst(node.opt(it.next()), key)?.let { found -> return found }
            }
            is JSONArray -> for (i in 0 until node.length()) findFirst(node.opt(i), key)?.let { found -> return found }
        }
        return null
    }

    private fun walk(node: Any?, visit: (String, JSONObject) -> Unit) {
        when (node) {
            is JSONObject -> {
                val it = node.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    val v = node.opt(k)
                    if (v is JSONObject) visit(k, v)
                    walk(v, visit)
                }
            }
            is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i), visit)
        }
    }
}
