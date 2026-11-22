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

package com.darkxvenom.airbeats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.playback.queues.ListQueue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.constants.AccountNameKey
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.LocalUserName
import com.darkxvenom.airbeats.ui.component.itune.ArtistCircleShelf
import com.darkxvenom.airbeats.ui.component.itune.FloatingMiniPlayer
import com.darkxvenom.airbeats.ui.component.itune.FloatingNavBar
import com.darkxvenom.airbeats.ui.component.itune.LatestSongsSection
import com.darkxvenom.airbeats.ui.component.itune.LiveFeaturedHeroCarousel
import com.darkxvenom.airbeats.ui.component.itune.NavTab
import com.darkxvenom.airbeats.ui.component.itune.SquareCardShelf
import com.darkxvenom.airbeats.ui.component.itune.TopHeaderBar
import com.darkxvenom.airbeats.ui.component.itune.TopSpringLoadingBar
import com.darkxvenom.airbeats.ui.component.itune.HomeShelfSkeleton
import com.darkxvenom.airbeats.ui.component.itune.WideCardShelf
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import com.darkxvenom.itune.backend.MusicTrack
import com.darkxvenom.airbeats.data.model.itune.HomeFeedItem
import com.darkxvenom.airbeats.data.model.itune.HomeFeedShelf
import com.darkxvenom.airbeats.data.model.itune.HomeHeroCard
import com.darkxvenom.airbeats.data.model.itune.HomeItemKind
import com.darkxvenom.airbeats.data.model.itune.HomeShelfGroup
import com.darkxvenom.airbeats.ui.component.itune.rememberSpringOverscrollState
import com.darkxvenom.airbeats.ui.component.itune.springOverscroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ITuneHomeScreen(
    navController: NavHostController,
    onSearchClick: () -> Unit,
    playerBottomSheetState: BottomSheetState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val accountName by rememberPreference(AccountNameKey, "")
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val userName = LocalUserName.current
    val displayName = userName.ifBlank { accountName }

    LaunchedEffect(innerTubeCookie) {
        viewModel.onAccountChanged(innerTubeCookie)
    }

    val shelves = remember(homePage, accountPlaylists) {
        val remote = homePage?.sections.orEmpty().mapIndexedNotNull { index, section ->
            val feedItems = section.items.mapNotNull { item ->
                when (item) {
                    is SongItem -> HomeFeedItem(
                        kind = HomeItemKind.SONG,
                        title = item.title,
                        subtitle = item.artists.joinToString(" • ") { it.name },
                        thumbnailUrl = item.thumbnail,
                        videoId = item.id,
                        artistName = item.artists.joinToString(", ") { it.name },
                        artistId = item.artists.firstOrNull()?.id,
                        albumName = item.album?.name,
                        albumId = item.album?.id,
                        isExplicit = item.explicit,
                    )
                    is AlbumItem -> HomeFeedItem(
                        kind = HomeItemKind.ALBUM,
                        title = item.title,
                        subtitle = item.artists.orEmpty().joinToString(" • ") { it.name }
                            .ifBlank { item.year?.toString().orEmpty() },
                        thumbnailUrl = item.thumbnail,
                        browseId = item.browseId,
                        watchPlaylistId = item.playlistId,
                        artistName = item.artists.orEmpty().joinToString(", ") { it.name },
                        artistId = item.artists?.firstOrNull()?.id,
                        albumName = item.title,
                        albumId = item.browseId,
                        isExplicit = item.explicit,
                    )
                    is PlaylistItem -> HomeFeedItem(
                        kind = HomeItemKind.PLAYLIST,
                        title = item.title,
                        subtitle = item.author?.name.orEmpty().ifBlank { item.songCountText.orEmpty() },
                        thumbnailUrl = item.thumbnail,
                        browseId = item.id,
                        watchPlaylistId = item.playEndpoint?.playlistId ?: item.id,
                        artistName = item.author?.name.orEmpty(),
                    )
                    is ArtistItem -> HomeFeedItem(
                        kind = HomeItemKind.ARTIST,
                        title = item.title,
                        thumbnailUrl = item.thumbnail,
                        browseId = item.id,
                    )
                    else -> null
                }
            }
            feedItems.takeIf { it.isNotEmpty() }?.let { items ->
                HomeFeedShelf(
                    key = "remote_${index}_${section.title}",
                    title = section.title,
                    strapline = section.label.orEmpty(),
                    items = items,
                    group = when {
                        section.title.contains("personal", ignoreCase = true) ||
                            section.title.contains("listen", ignoreCase = true) -> HomeShelfGroup.PERSONAL
                        section.title.contains("chart", ignoreCase = true) -> HomeShelfGroup.CHARTS
                        section.title.contains("podcast", ignoreCase = true) -> HomeShelfGroup.PODCASTS
                        section.title.contains("release", ignoreCase = true) ||
                            section.title.contains("trending", ignoreCase = true) -> HomeShelfGroup.TRENDING
                        else -> HomeShelfGroup.FEED
                    },
                    moreBrowseId = section.endpoint?.browseId,
                )
            }
        }

        val playlists = accountPlaylists.orEmpty().map { item ->
            HomeFeedItem(
                kind = HomeItemKind.PLAYLIST,
                title = item.title,
                subtitle = item.author?.name.orEmpty().ifBlank { item.songCountText.orEmpty() },
                thumbnailUrl = item.thumbnail,
                browseId = item.id,
                watchPlaylistId = item.playEndpoint?.playlistId ?: item.id,
                artistName = item.author?.name.orEmpty(),
            )
        }
        if (playlists.isNotEmpty()) {
            remote + HomeFeedShelf(
                key = "account_playlists",
                title = "Your YouTube Music Playlists",
                strapline = "YOUR LIBRARY",
                items = playlists,
                group = HomeShelfGroup.PERSONAL,
            )
        } else {
            remote
        }
    }

    val heroCards = remember(shelves) {
        shelves.flatMap { shelf ->
            shelf.items.take(2).map { item ->
                HomeHeroCard(
                    id = item.stableKey,
                    category = item.kind.name,
                    title = item.title,
                    subtitle = item.subtitle,
                    caption = shelf.title,
                    imageUrl = item.thumbnailUrl,
                    item = item,
                    queue = shelf.items,
                )
            }
        }.take(8)
    }

    val currentTrack = remember(mediaMetadata) {
        mediaMetadata?.let { metadata ->
            MusicTrack(
                id = metadata.id,
                title = metadata.title,
                artists = metadata.artists.joinToString(", ") { it.name },
                artistId = metadata.artists.firstOrNull()?.id,
                album = metadata.album?.title,
                albumId = metadata.album?.id,
                thumbnailUrl = metadata.thumbnailUrl,
                durationSec = metadata.duration.coerceAtLeast(0).toLong(),
                liked = metadata.liked,
                isExplicit = metadata.explicit,
            )
        }
    }

    val listState = rememberLazyListState()
    val overscrollState = rememberSpringOverscrollState()
    var selectedTab by remember { mutableStateOf<NavTab?>(NavTab.HOME) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(currentRoute) {
        selectedTab = when (currentRoute) {
            Screens.Explore.route -> NavTab.NEW
            Screens.Library.route -> NavTab.LIBRARY
            Screens.Home.route -> NavTab.HOME
            else -> selectedTab
        }
    }

    fun playHomeItem(item: HomeFeedItem, queue: List<HomeFeedItem> = listOf(item)) {
        when {
            item.isPlayableTrack -> {
                val videoId = item.videoId ?: return
                val metadataQueue = queue
                    .filter { it.isPlayableTrack }
                    .mapNotNull { it.toMediaMetadata() }
                val playableQueue = metadataQueue.ifEmpty { listOfNotNull(item.toMediaMetadata()) }
                if (playableQueue.isNotEmpty()) {
                    val startIndex = playableQueue.indexOfFirst { it.id == videoId }.coerceAtLeast(0)
                    playerConnection.playQueue(
                        ListQueue(
                            title = item.title,
                            items = playableQueue.map { it.toMediaItem() },
                            startIndex = startIndex,
                        )
                    )
                }
            }
            item.kind == HomeItemKind.ALBUM -> {
                item.browseId?.let { navController.navigate("album/$it") }
            }
            item.kind == HomeItemKind.PLAYLIST -> {
                val playlistId = item.watchPlaylistId ?: item.browseId ?: return
                navController.navigate("online_playlist/$playlistId")
            }
            item.kind == HomeItemKind.ARTIST -> {
                item.browseId?.let { navController.navigate("artist/$it") }
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .springOverscroll(overscrollState)
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                ),
            contentPadding = PaddingValues(top = 4.dp, bottom = 158.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "itune_header") {
                TopHeaderBar(
                    displayName = displayName,
                    onProfileClick = { navController.navigate("account") },
                )
            }

            item(key = "itune_hero") {
                LiveFeaturedHeroCarousel(
                    cards = heroCards,
                    isLoading = isLoading,
                    isApple = true,
                    sectionTitle = "Made for You",
                    onCardClick = { card -> playHomeItem(card.item, card.queue) },
                )
            }

            if (shelves.isEmpty()) {
                if (isLoading) {
                    item(key = "itune_loading_skeleton") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            repeat(3) { HomeShelfSkeleton() }
                        }
                    }
                } else {
                    item(key = "itune_feed_error") {
                        Text(
                            text = "Couldn't load music right now. Pull down to try again.",
                            color = Color.White.copy(alpha = 0.68f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                        )
                    }
                }
            } else {
                items(shelves, key = { it.key }) { shelf ->
                    when {
                        shelf.isSongShelf -> {
                            LatestSongsSection(
                                songColumns = emptyList(),
                                personalizedTracks = shelf.items.mapNotNull { it.toMusicTrack() },
                                sectionTitle = shelf.title,
                                onPersonalizedTrackClick = { track ->
                                    val feedItem = shelf.items.firstOrNull { it.videoId == track.id }
                                    if (feedItem != null) {
                                        playHomeItem(feedItem, shelf.items)
                                    } else {
                                        val metadata = track.toMediaMetadata()
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = track.title,
                                                items = listOf(metadata.toMediaItem()),
                                            )
                                        )
                                    }
                                },
                            )
                        }
                        shelf.isArtistShelf -> ArtistCircleShelf(
                            shelf = shelf,
                            onItemClick = { item -> playHomeItem(item, shelf.items) },
                        )
                        shelf.isVideoShelf -> WideCardShelf(
                            shelf = shelf,
                            onItemClick = { item -> playHomeItem(item, shelf.items) },
                        )
                        else -> SquareCardShelf(
                            shelf = shelf,
                            onItemClick = { item -> playHomeItem(item, shelf.items) },
                        )
                    }
                }
            }

            item(key = "itune_end_spacer") {
                Spacer(Modifier.height(12.dp))
            }
        }

        TopSpringLoadingBar(
            isVisible = isLoading || isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FloatingMiniPlayer(
                currentTrack = null,
                realTrack = currentTrack,
                isPlaying = isPlaying,
                onPlayPauseClick = { playerConnection.togglePlayPause() },
                onNextClick = { playerConnection.seekToNext() },
                onClick = { playerBottomSheetState.expandSoft() },
                showEmptyPlaceholder = mediaMetadata == null,
            )

            FloatingNavBar(
                selectedTab = selectedTab,
                onSearchClick = onSearchClick,
                onTabSelected = { tab ->
                    selectedTab = tab
                    when (tab) {
                        NavTab.HOME -> {
                            if (currentRoute != Screens.Home.route) {
                                navController.navigate(Screens.Home.route)
                            }
                        }
                        NavTab.NEW -> navController.navigate(Screens.Explore.route)
                        NavTab.LIBRARY -> navController.navigate(Screens.Library.route)
                        NavTab.RADIO -> {
                            val metadata = mediaMetadata ?: homePage?.sections
                                ?.flatMap { it.items }
                                ?.filterIsInstance<SongItem>()
                                ?.firstOrNull()
                                ?.toMediaMetadata()
                            if (metadata != null) {
                                playerConnection.playQueue(YouTubeQueue.radio(metadata))
                            } else {
                                navController.navigate(Screens.Explore.route)
                            }
                        }
                    }
                },
            )
        }
    }
}

private fun HomeFeedItem.toMediaMetadata(): MediaMetadata? {
    val videoId = videoId?.takeIf { it.isNotBlank() } ?: return null
    val artists = artistName.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapIndexed { index, name ->
            MediaMetadata.Artist(
                id = if (index == 0) artistId else null,
                name = name,
            )
        }
    return MediaMetadata(
        id = videoId,
        title = title,
        artists = artists,
        duration = 0,
        thumbnailUrl = thumbnailUrl,
        album = if (!albumId.isNullOrBlank() || !albumName.isNullOrBlank()) {
            MediaMetadata.Album(albumId.orEmpty(), albumName.orEmpty())
        } else {
            null
        },
        explicit = isExplicit,
    )
}

private fun MusicTrack.toMediaMetadata(): MediaMetadata {
    val artists = artists.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapIndexed { index, name ->
            MediaMetadata.Artist(id = if (index == 0) artistId else null, name = name)
        }
    return MediaMetadata(
        id = id,
        title = title,
        artists = artists,
        duration = durationSec.coerceAtLeast(0).toInt(),
        thumbnailUrl = thumbnailUrl,
        album = if (!albumId.isNullOrBlank() || !album.isNullOrBlank()) {
            MediaMetadata.Album(albumId.orEmpty(), album.orEmpty())
        } else {
            null
        },
        explicit = isExplicit,
        liked = liked,
    )
}
