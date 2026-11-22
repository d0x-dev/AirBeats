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

package com.darkxvenom.airbeats.ui.home.itune

import com.darkxvenom.itune.backend.MusicPlaylistRef
import com.darkxvenom.itune.backend.NewReleaseAlbum
import com.darkxvenom.airbeats.data.model.itune.HomeFeedItem
import com.darkxvenom.airbeats.data.model.itune.HomeFeedShelf
import com.darkxvenom.airbeats.data.model.itune.HomeItemKind
import com.darkxvenom.airbeats.data.model.itune.HomeShelfGroup

internal fun List<HomeFeedShelf>.withoutExplicit(hide: Boolean): List<HomeFeedShelf> {
    if (!hide) return this
    return mapNotNull { shelf ->
        val kept = shelf.items.filterNot { it.isExplicit }
        if (kept.isEmpty()) null else shelf.copy(items = kept)
    }
}

internal fun NewReleaseAlbum.toFeedItem(): HomeFeedItem = HomeFeedItem(
    kind = HomeItemKind.ALBUM,
    title = title,
    subtitle = subtitleLine,
    thumbnailUrl = thumbnailUrl,
    browseId = browseId,
    artistName = artist,
    artistId = artistId,
    isExplicit = isExplicit,
)
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

internal fun MusicPlaylistRef.toFeedItem(): HomeFeedItem = HomeFeedItem(
    kind = if (isAlbum) HomeItemKind.ALBUM else HomeItemKind.PLAYLIST,
    title = title,
    subtitle = author.ifBlank { if (trackCount > 0) "$trackCount tracks" else "Playlist" },
    thumbnailUrl = thumbnailUrl,
    browseId = id,
    watchPlaylistId = watchPlaylistId,
    isExplicit = isExplicit,
)

/**
 * Final top-to-bottom shelf order for Home (everything above the radio stations).
 *
 * Signed in : Quick picks → your listening (recent, top artists, because-you-listened) → the rest of
 *             your YouTube Music feed → podcasts → playlists.
 * Signed out: Trending songs → new releases → your listening (if any) → popular artists & charts →
 *             default feed → podcasts → playlists.
 */
internal fun orderHomeShelves(
    remote: List<HomeFeedShelf>,
    personal: List<HomeFeedShelf>,
    releases: List<NewReleaseAlbum>,
    playlists: List<MusicPlaylistRef>,
    signedIn: Boolean,
): List<HomeFeedShelf> {
    val out = mutableListOf<HomeFeedShelf>()
    val used = HashSet<String>()
    fun add(shelf: HomeFeedShelf?) {
        if (shelf != null && shelf.items.isNotEmpty() && used.add(shelf.key)) out += shelf
    }

    val releaseShelf = releases.takeIf { it.isNotEmpty() }?.let {
        HomeFeedShelf(
            key = "new_releases",
            title = "New Releases",
            items = it.take(16).map(NewReleaseAlbum::toFeedItem),
            group = HomeShelfGroup.TRENDING,
        )
    }
    val playlistShelf = playlists.takeIf { it.isNotEmpty() }?.let {
        HomeFeedShelf(
            key = "featured_playlists",
            title = if (signedIn) "Playlists for You" else "Featured Playlists",
            items = it.take(16).map(MusicPlaylistRef::toFeedItem),
            group = HomeShelfGroup.FEED,
        )
    }

    val quickPicks = remote.firstOrNull { it.isSongShelf && it.title.contains("quick picks", ignoreCase = true) }
    val trendingSongs = remote.firstOrNull { it.group == HomeShelfGroup.TRENDING && it.isSongShelf }
    val firstSongShelf = remote.firstOrNull { it.isSongShelf }

    if (signedIn) {
        add(quickPicks ?: firstSongShelf)
        personal.forEach(::add)
    } else {
        add(trendingSongs ?: quickPicks ?: firstSongShelf)
        add(releaseShelf)
        personal.forEach(::add)
    }

    val rest = remote.filter { it.key !in used }
    val order = listOf(
        HomeShelfGroup.FEED, HomeShelfGroup.TRENDING, HomeShelfGroup.CHARTS, HomeShelfGroup.PERSONAL,
    )
    order.forEach { g -> rest.filter { it.group == g }.forEach(::add) }
    if (signedIn) add(releaseShelf)
    rest.filter { it.group == HomeShelfGroup.PODCASTS }.forEach(::add)
    add(playlistShelf)
    return out.take(30)
}
