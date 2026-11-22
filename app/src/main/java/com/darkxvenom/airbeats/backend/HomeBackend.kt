package com.darkxvenom.airbeats.backend

import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.innertube.pages.HomePage
import com.darkxvenom.airbeats.innertube.utils.completedLibraryPage

/**
 * Shared YouTube Music home source for every AirBeats home presentation.
 *
 * This repository deliberately does not consult the selected playback provider:
 * JioSaavn may still be used by the player, but it must never replace YouTube
 * Music's home shelves or account playlists.
 */
object HomeBackend {
    data class Remote(
        val page: HomePage?,
        val playlists: List<PlaylistItem>,
    )

    fun isLoggedIn(): Boolean {
        val cookie = YouTube.cookie.orEmpty()
        val jar = cookie.split(';').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null
            else part.substring(0, index).trim() to part.substring(index + 1).trim()
        }.toMap()

        val hasApiSid = listOf(
            "SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID"
        ).any { !jar[it].isNullOrBlank() }
        val hasSession = listOf(
            "SID", "__Secure-1PSID", "__Secure-3PSID", "LOGIN_INFO"
        ).any { !jar[it].isNullOrBlank() }
        return hasApiSid && hasSession
    }

    suspend fun accountPlaylists(): List<PlaylistItem> {
        if (!isLoggedIn()) return emptyList()
        return YouTube.library("FEmusic_liked_playlists")
            .completedLibraryPage()
            .getOrThrow()
            .items
            .filterIsInstance<PlaylistItem>()
            .filterNot { it.id in SYSTEM_PLAYLIST_IDS }
            .distinctBy { it.id }
    }

    /**
     * Fetches YouTube Music's own FEmusic_home feed, follows its continuations,
     * filters excluded tracks, and attaches the signed-in account's playlists.
     */
    suspend fun load(excluded: Set<String>): Remote {
        val pageResult = YouTube.home()
        val page = pageResult.getOrThrow()
        val sections = page.sections.mapNotNull { section ->
            val items = section.items.filterNot { it.id in excluded }
            items.takeIf { it.isNotEmpty() }?.let { section.copy(items = it) }
        }

        // The feed may already contain mixes/playlists. Also expose the actual
        // account library playlists so all screen variants can render them.
        val playlists = accountPlaylists()
        return Remote(page.copy(sections = sections), playlists)
    }

    private val SYSTEM_PLAYLIST_IDS = setOf(
        "LM", "SE", "NE", "SS", "SR", "LL", "WL"
    )

    // ── Universal home model: the ONE structure every home style renders (mirrors iTune) ──────
    enum class ShelfGroup { PERSONAL, FEED, TRENDING, CHARTS, PODCASTS }

    data class UniversalShelf(
        val key: String,
        val title: String,
        val strapline: String = "",
        val items: List<YTItem>,
        val group: ShelfGroup = ShelfGroup.FEED,
    ) {
        val isSongShelf get() = items.isNotEmpty() && items.all { it is SongItem }
        val isArtistShelf get() = items.isNotEmpty() && items.all { it is ArtistItem }
    }

    data class UniversalHero(
        val id: String,
        val category: String,
        val title: String,
        val subtitle: String,
        val imageUrl: String?,
        val item: YTItem,
    )

    data class UniversalHome(val hero: List<UniversalHero>, val shelves: List<UniversalShelf>)

    private fun groupOf(title: String): ShelfGroup {
        val t = title.lowercase()
        return when {
            "podcast" in t -> ShelfGroup.PODCASTS
            "chart" in t || t.startsWith("top ") -> ShelfGroup.CHARTS
            "trending" in t || "new release" in t || "popular" in t -> ShelfGroup.TRENDING
            "quick picks" in t || "listen again" in t || "forgotten" in t ||
                "your " in t || "for you" in t || "mixed" in t -> ShelfGroup.PERSONAL
            else -> ShelfGroup.FEED
        }
    }

    private fun YTItem.subtitleText(): String = when (this) {
        is SongItem -> artists.joinToString { it.name }
        is AlbumItem -> artists?.joinToString { it.name }.orEmpty()
        is PlaylistItem -> author?.name.orEmpty()
        is ArtistItem -> "Artist"
        else -> ""
    }

    /**
     * Same ordering rules as iTune's orderHomeShelves:
     * signed in  -> Quick picks, personal, feed, trending, charts, new releases, podcasts, Playlists for You
     * signed out -> Trending songs, new releases, personal, feed, charts, podcasts
     */
    fun buildUniversal(page: HomePage?, accountPlaylists: List<PlaylistItem>, signedIn: Boolean): UniversalHome {
        val remote = page?.sections.orEmpty().mapIndexedNotNull { i, sec ->
            sec.items.takeIf { it.isNotEmpty() }?.let {
                UniversalShelf("feed_${i}_${sec.title}", sec.title, sec.label.orEmpty(), it, groupOf(sec.title))
            }
        }
        val out = mutableListOf<UniversalShelf>()
        val used = HashSet<String>()
        fun add(s: UniversalShelf?) { if (s != null && s.items.isNotEmpty() && used.add(s.key)) out += s }

        val quick = remote.firstOrNull { it.isSongShelf && it.title.contains("quick picks", true) }
        val trending = remote.firstOrNull { it.group == ShelfGroup.TRENDING && it.isSongShelf }
        val firstSong = remote.firstOrNull { it.isSongShelf }
        val releases = remote.flatMap { it.items }.filterIsInstance<AlbumItem>().distinctBy { it.id }.take(16)
            .takeIf { it.isNotEmpty() }?.let { UniversalShelf("new_releases", "New Releases", items = it, group = ShelfGroup.TRENDING) }
        val playlists = (if (signedIn) accountPlaylists else emptyList()).take(24)
            .takeIf { it.isNotEmpty() }?.let { UniversalShelf("playlists_for_you", "Playlists for You", items = it) }

        if (signedIn) add(quick ?: firstSong) else { add(trending ?: quick ?: firstSong); add(releases) }
        remote.filter { it.group == ShelfGroup.PERSONAL && it.key !in used }.forEach(::add)
        val rest = remote.filter { it.key !in used }
        listOf(ShelfGroup.FEED, ShelfGroup.TRENDING, ShelfGroup.CHARTS, ShelfGroup.PERSONAL)
            .forEach { g -> rest.filter { it.group == g }.forEach(::add) }
        if (signedIn) add(releases)
        rest.filter { it.group == ShelfGroup.PODCASTS }.forEach(::add)
        add(playlists)

        val all = out.flatMap { it.items }
        val hero = listOfNotNull(
            all.firstOrNull { it is SongItem }?.let { "Top Pick" to it },
            all.firstOrNull { it is AlbumItem }?.let { "New Release" to it },
            (trending?.items?.getOrNull(1) ?: all.filterIsInstance<SongItem>().getOrNull(1))?.let { "Trending" to it },
            all.firstOrNull { it is ArtistItem }?.let { "Top Artist" to it },
            all.firstOrNull { it is PlaylistItem }?.let { "Playlist" to it },
        ).distinctBy { it.second.id }.map { (cat, item) ->
            UniversalHero(item.id, cat, item.title, item.subtitleText(), item.thumbnail, item)
        }
        return UniversalHome(hero, out.take(30))
    }
}
