/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of AirBeats and is licensed under GPL-3.0.
 */

package com.darkxvenom.airbeats.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.models.MediaMetadata

enum class HomeItemKind { SONG, VIDEO, ALBUM, ARTIST, PLAYLIST, PODCAST, EPISODE }

enum class HomeShelfGroup { PERSONAL, FEED, TRENDING, CHARTS, PODCASTS }

@Immutable
data class HomeFeedItem(
    val kind: HomeItemKind,
    val title: String,
    val subtitle: String = "",
    val thumbnailUrl: String? = null,
    val videoId: String? = null,
    val browseId: String? = null,
    val watchPlaylistId: String? = null,
    val artistName: String = "",
    val artistId: String? = null,
    val albumName: String? = null,
    val albumId: String? = null,
    val isExplicit: Boolean = false,
    val views: String? = null,
) {
    val stableKey: String
        get() = "${kind.name}:${videoId ?: browseId ?: watchPlaylistId ?: title}"

    val isPlayableTrack: Boolean
        get() = !videoId.isNullOrBlank() &&
            (kind == HomeItemKind.SONG || kind == HomeItemKind.VIDEO || kind == HomeItemKind.EPISODE)

    fun toMediaMetadata(): MediaMetadata {
        return MediaMetadata(
            id = videoId ?: browseId ?: "",
            title = title,
            artists = listOf(
                MediaMetadata.Artist(
                    id = artistId,
                    name = artistName.ifBlank { subtitle }
                )
            ),
            duration = 0,
            thumbnailUrl = thumbnailUrl
        )
    }

    fun toSongItem(): SongItem {
        return SongItem(
            id = videoId ?: browseId ?: "",
            title = title,
            artists = listOf(
                com.darkxvenom.airbeats.innertube.models.Artist(
                    name = artistName.ifBlank { subtitle },
                    id = artistId
                )
            ),
            album = albumName?.let {
                com.darkxvenom.airbeats.innertube.models.Album(
                    name = it,
                    id = albumId ?: ""
                )
            },
            duration = null,
            thumbnail = thumbnailUrl.orEmpty(),
            endpoint = videoId?.let { WatchEndpoint(videoId = it) }
        )
    }

    fun toPlaylistItem(): PlaylistItem = PlaylistItem(
        id = watchPlaylistId ?: browseId?.removePrefix("VL") ?: "",
        title = title,
        author = if (artistName.isNotBlank()) com.darkxvenom.airbeats.innertube.models.Artist(name = artistName, id = artistId) else null,
        songCountText = subtitle,
        thumbnail = thumbnailUrl,
        playEndpoint = null,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )

    fun toArtistItem(): ArtistItem = ArtistItem(
        id = browseId ?: artistId ?: "",
        title = title,
        thumbnail = thumbnailUrl,
        shuffleEndpoint = null,
        radioEndpoint = null,
        subscriberCountText = subtitle.ifBlank { null }
    )

    fun toAlbumItem(): AlbumItem = AlbumItem(
        browseId = browseId ?: albumId ?: "",
        playlistId = watchPlaylistId ?: "",
        title = title,
        artists = if (artistName.isNotBlank()) listOf(com.darkxvenom.airbeats.innertube.models.Artist(name = artistName, id = artistId)) else null,
        year = null,
        thumbnail = thumbnailUrl.orEmpty(),
        explicit = isExplicit
    )

    fun toYTItem(): YTItem = when (kind) {
        HomeItemKind.SONG, HomeItemKind.VIDEO, HomeItemKind.EPISODE -> toSongItem()
        HomeItemKind.PLAYLIST -> toPlaylistItem()
        HomeItemKind.ARTIST -> toArtistItem()
        HomeItemKind.ALBUM -> toAlbumItem()
        HomeItemKind.PODCAST -> toPlaylistItem()
    }
}

@Immutable
data class HomeFeedShelf(
    val key: String,
    val title: String,
    val strapline: String = "",
    val items: List<HomeFeedItem>,
    val group: HomeShelfGroup = HomeShelfGroup.FEED,
    val moreBrowseId: String? = null,
) {
    val isSongShelf: Boolean get() = items.isNotEmpty() && items.all { it.kind == HomeItemKind.SONG }
    val isArtistShelf: Boolean get() = items.isNotEmpty() && items.all { it.kind == HomeItemKind.ARTIST }
    val isVideoShelf: Boolean
        get() = items.isNotEmpty() && items.all { it.kind == HomeItemKind.VIDEO || it.kind == HomeItemKind.EPISODE }
}

@Immutable
data class HomeHeroCard(
    val id: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val caption: String,
    val imageUrl: String?,
    val item: HomeFeedItem,
    val queue: List<HomeFeedItem> = emptyList(),
    val accentColor: Color? = null,
    val gradientColors: List<Color> = emptyList(),
)
