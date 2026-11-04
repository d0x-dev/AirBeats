package com.darkxvenom.airbeats.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.extensions.togglePlayPause
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.LibraryFilter
import com.darkxvenom.airbeats.ui.component.ChipsRow
import com.darkxvenom.airbeats.ui.component.VerticalFastScroller

@Composable
fun PlayfulLibraryScreen(
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    onSearchClick: () -> Unit
) {

    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    var filterType by remember { mutableStateOf(LibraryFilter.LIBRARY) }
    val lazyListState = rememberLazyListState()

    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    LibraryFilter.LOCAL to stringResource(R.string.filter_local),
                ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType =
                        if (filterType == it) {
                            LibraryFilter.LIBRARY
                        } else {
                            it
                        }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            onBackground = Color.Black,
            onSurface = Color.Black,
            surface = Color.White
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.Black) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFD54F))
            ) {
                // 🔝 Top Header
                Text(
                    text = stringResource(R.string.android_auto_library),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .align(Alignment.TopStart)
                )

                // 📚 Content
                VerticalFastScroller(
                    listState = lazyListState,
                    topContentPadding = 80.dp,
                    endContentPadding = 250.dp
                ) {
                    when (filterType) {
                        LibraryFilter.LIBRARY ->
                            LibraryMixScreen(
                                navController = navController,
                                filterContent = filterContent,
                                onLocalClick = { filterType = LibraryFilter.LOCAL }
                            )

                        LibraryFilter.PLAYLISTS ->
                            LibraryPlaylistsScreen(
                                navController = navController,
                                filterContent = filterContent,
                                onLocalClick = { filterType = LibraryFilter.LOCAL }
                            )

                        LibraryFilter.SONGS ->
                            LibrarySongsScreen(
                                navController,
                                { filterType = LibraryFilter.LIBRARY }
                            )

                        LibraryFilter.ALBUMS ->
                            LibraryAlbumsScreen(
                                navController,
                                { filterType = LibraryFilter.LIBRARY }
                            )

                        LibraryFilter.ARTISTS ->
                            LibraryArtistsScreen(
                                navController,
                                { filterType = LibraryFilter.LIBRARY }
                            )

                        LibraryFilter.LOCAL ->
                            LocalSongsScreen(navController = navController)
                    }
                }
                val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val bottomPadding = if (navBarBottom > 40.dp) navBarBottom + 8.dp else 16.dp

                // Custom White Bottom Nav / Mini Player Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomPadding, start = 16.dp, end = 16.dp)
                        .background(Color.White, RoundedCornerShape(32.dp))
                ) {
                    Column {
                        // Mini Player
                        if (mediaMetadata != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerBottomSheetState.expandSoft()
                                    }
                                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { playerConnection.player.togglePlayPause() }
                                ) {
                                    AsyncImage(
                                        model = mediaMetadata?.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    )
                                    Icon(
                                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                        contentDescription = stringResource(R.string.ui_a11y_play_pause),
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mediaMetadata?.title ?: "",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                                        fontSize = 14.sp,
                                        color = Color.Black.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { playerConnection.toggleLike() }) {
                                    Icon(
                                        painter = painterResource(
                                            if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border
                                        ),
                                        contentDescription = stringResource(R.string.action_like),
                                        tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else Color.Black
                                    )
                                }
                                IconButton(onClick = { playerConnection.player.seekToNext() }) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_next),
                                        contentDescription = stringResource(R.string.ui_a11y_skip_next),
                                        tint = Color.Black
                                    )
                                }
                            }
                        }

                        // Bottom Navigation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navController.navigate("home") { launchSingleTop = true } }) {
                                Icon(painterResource(R.drawable.home_outlined), null, tint = Color.Black)
                            }
                            IconButton(onClick = { navController.navigate("explore") { launchSingleTop = true } }) {
                                Icon(painterResource(R.drawable.explore_outlined), null, tint = Color.Black)
                            }
                            IconButton(onClick = onSearchClick) {
                                Icon(painterResource(R.drawable.search), null, tint = Color.Black)
                            }
                            IconButton(onClick = { navController.navigate("library") { launchSingleTop = true } }) {
                                Icon(painterResource(R.drawable.library_music), null, tint = Color(0xFFE5A93D))
                            }
                        }
                    }
                }
            }
        }
    }
}
