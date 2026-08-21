/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.darkxvenom.airbeats.innertube.pages

import com.darkxvenom.airbeats.innertube.models.Album
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.Artist
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.MusicResponsiveListItemRenderer
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.Run
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.innertube.models.clean
import com.darkxvenom.airbeats.innertube.models.oddElements
import com.darkxvenom.airbeats.innertube.models.splitBySeparator
import com.darkxvenom.airbeats.innertube.utils.parseTime

data class SearchResult(
    val items: List<YTItem>,
    val continuation: String? = null,
)

object SearchPage {
    fun toYTItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        val title = renderer.titleText ?: return null
        val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
        val metadata = renderer.metadataGroups()
        return when {
            renderer.isSong -> {
                val endpoint = renderer.watchEndpoint()
                SongItem(
                    id = renderer.playlistItemData?.videoId ?: endpoint?.videoId ?: return null,
                    title = title,
                    artists = metadata.getOrNull(0).toArtists(),
                    album =
                        metadata.getOrNull(1)?.firstOrNull()
                            ?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                            )
                        },
                    duration = metadata.duration(),
                    thumbnail = thumbnail ?: return null,
                    explicit = renderer.isExplicit,
                    endpoint = endpoint,
                )
            }
            renderer.isArtist -> {
                ArtistItem(
                    id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                    title = title,
                    thumbnail = thumbnail,
                    playEndpoint = renderer.watchEndpoint(),
                    shuffleEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                    radioEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                )
            }
            renderer.isAlbum -> {
                AlbumItem(
                    browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                    playlistId =
                        renderer.watchEndpoint()
                            ?.playlistId
                            ?: return null,
                    title = title,
                    artists = metadata.getOrNull(0).toArtists().takeIf { it.isNotEmpty() },
                    year = metadata.year(),
                    thumbnail = thumbnail ?: return null,
                    explicit = renderer.isExplicit,
                )
            }
            renderer.isPlaylist -> {
                val playlistMetadata = renderer.metadataGroups(clean = false)
                PlaylistItem(
                    id =
                        renderer.navigationEndpoint?.browseEndpoint?.browseId?.removePrefix("VL")
                            ?: renderer.watchEndpoint()?.playlistId?.removePrefix("VL")
                            ?: return null,
                    title = title,
                    author = playlistMetadata.playlistAuthor(),
                    songCountText = playlistMetadata.lastText(),
                    thumbnail = thumbnail,
                    playEndpoint =
                        renderer.watchEndpoint(),
                    shuffleEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                    radioEndpoint =
                        renderer.menu
                            ?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                )
            }
            else -> null
        }
    }
}

private val MusicResponsiveListItemRenderer.titleText: String?
    get() =
        flexColumns
            .firstOrNull()
            ?.musicResponsiveListItemFlexColumnRenderer
            ?.text
            ?.runs
            ?.joinToString(separator = "") { it.text }
            ?.takeIf { it.isNotBlank() }

private val MusicResponsiveListItemRenderer.isExplicit: Boolean
    get() =
        badges?.any {
            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
        } == true

private fun MusicResponsiveListItemRenderer.metadataGroups(clean: Boolean = true): List<List<Run>> {
    val groups =
        flexColumns
            .drop(1)
            .flatMap {
                it.musicResponsiveListItemFlexColumnRenderer.text?.runs?.splitBySeparator()
                    .orEmpty()
            }
    return if (clean) groups.clean() else groups
}

private fun MusicResponsiveListItemRenderer.watchEndpoint(): WatchEndpoint? =
    navigationEndpoint?.anyWatchEndpoint
        ?: overlay
            ?.musicItemThumbnailOverlayRenderer
            ?.content
            ?.musicPlayButtonRenderer
            ?.playNavigationEndpoint
            ?.anyWatchEndpoint

private fun List<Run>?.toArtists(): List<Artist> =
    this
        ?.oddElements()
        ?.map {
            Artist(
                name = it.text,
                id = it.navigationEndpoint?.browseEndpoint?.browseId,
            )
        }
        .orEmpty()

private fun List<List<Run>>.duration(): Int? {
    for (group in asReversed()) {
        for (run in group.asReversed()) {
            run.text.parseTime()?.let { return it }
        }
    }
    return null
}

private fun List<List<Run>>.year(): Int? {
    for (group in asReversed()) {
        for (run in group.asReversed()) {
            run.text.toIntOrNull()?.let { return it }
        }
    }
    return null
}

private fun List<List<Run>>.playlistAuthor(): Artist? {
    val authorIndex = if (size >= 3) 1 else 0
    return getOrNull(authorIndex)
        ?.firstOrNull()
        ?.let {
            Artist(
                name = it.text,
                id = it.navigationEndpoint?.browseEndpoint?.browseId,
            )
        }
}

private fun List<List<Run>>.lastText(): String? =
    lastOrNull()
        ?.joinToString(separator = "") { it.text }
        ?.takeIf { it.isNotBlank() }
