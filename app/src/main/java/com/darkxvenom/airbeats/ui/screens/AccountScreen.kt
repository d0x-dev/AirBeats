package com.darkxvenom.airbeats.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.GridThumbnailHeight
import com.darkxvenom.airbeats.constants.SpotifyCookieKey
import com.darkxvenom.airbeats.spotify.models.SpotifyHomeFeedItem
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.YouTubeGridItem
import com.darkxvenom.airbeats.ui.component.SpotifyGridItem
import com.darkxvenom.airbeats.ui.component.shimmer.GridItemPlaceHolder
import com.darkxvenom.airbeats.ui.component.shimmer.ShimmerHost
import com.darkxvenom.airbeats.ui.menu.YouTubeAlbumMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeArtistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubePlaylistMenu
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.viewmodels.AccountViewModel
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AccountScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AccountViewModel = hiltViewModel(),
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("YouTube", "Spotify")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.account)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> YouTubeAccountContent(navController, viewModel)
                1 -> SpotifyAccountContent(navController, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YouTubeAccountContent(
    navController: NavController,
    viewModel: AccountViewModel
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val playlists by viewModel.playlists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        items(items = playlists.orEmpty(), key = { it.id }) { item ->
            YouTubeGridItem(
                item = item,
                fillMaxWidth = true,
                modifier = Modifier.combinedClickable(
                    onClick = { navController.navigate("online_playlist/${item.id}") },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            YouTubePlaylistMenu(
                                playlist = item,
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                ),
            )
        }

        items(items = albums.orEmpty(), key = { it.id }) { item ->
            YouTubeGridItem(
                item = item,
                fillMaxWidth = true,
                modifier = Modifier.combinedClickable(
                    onClick = { navController.navigate("album/${item.id}") },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            YouTubeAlbumMenu(
                                albumItem = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                )
            )
        }

        items(items = artists.orEmpty(), key = { it.id }) { item ->
            YouTubeGridItem(
                item = item,
                fillMaxWidth = true,
                modifier = Modifier.combinedClickable(
                    onClick = { navController.navigate("artist/${item.id}") },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            YouTubeArtistMenu(
                                artist = item,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                )
            )
        }

        if (playlists == null) {
            items(8) {
                ShimmerHost { GridItemPlaceHolder(fillMaxWidth = true) }
            }
        }
    }
}

@Composable
fun SpotifyAccountContent(
    navController: NavController,
    viewModel: AccountViewModel
) {
    val context = LocalContext.current
    val hasSpotifyCookie by remember {
        context.dataStore.data.map { it.contains(SpotifyCookieKey) }
    }.collectAsState(initial = false)

    val spotifyUser by viewModel.spotifyUser.collectAsState()
    val spotifyFeed by viewModel.spotifyFeed.collectAsState()
    val isSpotifyLoading by viewModel.isSpotifyLoading.collectAsState()

    LaunchedEffect(hasSpotifyCookie) {
        if (hasSpotifyCookie) {
            viewModel.loadSpotifyData()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!hasSpotifyCookie) {
            Button(onClick = { navController.navigate("spotify_login") }) {
                Text("Connect to Spotify")
            }
        } else if (isSpotifyLoading) {
            CircularProgressIndicator()
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                modifier = Modifier.fillMaxSize()
            ) {
                if (spotifyUser != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "Welcome back, ${spotifyUser!!.displayName}!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                spotifyFeed?.sections?.forEach { section ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = section.title ?: "Recommended",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(items = section.items, key = { it.uri }) { item ->
                        SpotifyGridItem(
                            item = item,
                            fillMaxWidth = true,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    when (item) {
                                        is SpotifyHomeFeedItem.Playlist -> navController.navigate("online_playlist/sp:${item.id}")
                                        is SpotifyHomeFeedItem.Album -> navController.navigate("album/sp:${item.id}")
                                        is SpotifyHomeFeedItem.Artist -> navController.navigate("artist/sp:${item.id}")
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
