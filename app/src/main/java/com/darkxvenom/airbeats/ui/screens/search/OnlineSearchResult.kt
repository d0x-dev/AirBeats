package com.darkxvenom.airbeats.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

import com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO

import com.darkxvenom.airbeats.innertube.models.*
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AppBarHeight
import com.darkxvenom.airbeats.constants.SearchFilterHeight
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.*
import com.darkxvenom.airbeats.ui.component.shimmer.*
import com.darkxvenom.airbeats.ui.menu.*
import com.darkxvenom.airbeats.viewmodels.OnlineSearchViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {

    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    // 🌗 THEME AWARE OVERLAY
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val gradientColors =
        if (isDarkTheme) {
            listOf(
                Color.Black.copy(alpha = 0.25f),
                Color.Black.copy(alpha = 0.55f),
                Color.Black.copy(alpha = 0.85f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.45f),
                Color.White.copy(alpha = 0.65f)
            )
        }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🔥 BLUR BACKGROUND
        mediaMetadata?.thumbnailUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(90.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(gradientColors)
                    )
            )
        }

        // 📜 ORIGINAL CONTENT (UNCHANGED)
        LazyColumn(
            state = lazyListState,
            contentPadding =
                LocalPlayerAwareWindowInsets.current
                    .add(WindowInsets(top = SearchFilterHeight))
                    .add(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .asPaddingValues(),
        ) {

            val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item ->

                val longClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        when (item) {
                            is SongItem ->
                                YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )

                            is AlbumItem ->
                                YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )

                            is ArtistItem ->
                                YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss,
                                )

                            is PlaylistItem ->
                                YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = coroutineScope,
                                    onDismiss = menuState::dismiss,
                                )
                        }
                    }
                }

                YouTubeListItem(
                    item = item,
                    isActive =
                        when (item) {
                            is SongItem -> mediaMetadata?.id == item.id
                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                            else -> false
                        },
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(onClick = longClick) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .combinedClickable(
                                onClick = {
                                    when (item) {
                                        is SongItem -> {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        WatchEndpoint(videoId = item.id),
                                                        item.toMediaMetadata()
                                                    )
                                                )
                                            }
                                        }

                                        is AlbumItem -> navController.navigate("album/${item.id}")
                                        is ArtistItem -> navController.navigate("artist/${item.id}")
                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                    }
                                },
                                onLongClick = longClick,
                            )
                            .animateItem(),
                )
            }

            if (searchFilter == null) {
                searchSummary?.summaries?.forEach { summary ->
                    item { NavigationTitle(summary.title) }

                    items(
                        items = summary.items,
                        key = { "${summary.title}/${it.id}" },
                        itemContent = ytItemContent,
                    )
                }

                if (searchSummary?.summaries?.isEmpty() == true) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            } else {
                items(
                    items = itemsPage?.items.orEmpty().distinctBy { it.id },
                    key = { it.id },
                    itemContent = ytItemContent,
                )

                if (itemsPage?.continuation != null) {
                    item(key = "loading") {
                        ShimmerHost {
                            repeat(3) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                }

                if (itemsPage?.items?.isEmpty() == true) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            }
        }

        // 🔥 FILTER BAR (BLENDS IN LIGHT MODE)
        ChipsRow(
            chips =
                listOf(
                    null to stringResource(R.string.filter_all),
                    FILTER_SONG to stringResource(R.string.filter_songs),
                    FILTER_VIDEO to stringResource(R.string.filter_videos),
                    FILTER_ALBUM to stringResource(R.string.filter_albums),
                    FILTER_ARTIST to stringResource(R.string.filter_artists),
                    FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                    FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
                ),
            currentValue = searchFilter,
            onValueUpdate = {
                if (viewModel.filter.value != it) {
                    viewModel.filter.value = it
                }
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            },
            modifier =
                Modifier
                    .background(Color.Transparent)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                            .add(WindowInsets(top = AppBarHeight))
                    )
                    .fillMaxWidth()
        )
    }
}
