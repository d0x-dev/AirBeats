package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.chrisbanes.haze.haze
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import androidx.compose.ui.graphics.Shape
import com.darkxvenom.airbeats.constants.AccountNameKey
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.db.entities.Album
import com.darkxvenom.airbeats.db.entities.Artist
import com.darkxvenom.airbeats.db.entities.LocalItem
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.ListQueue
import com.darkxvenom.airbeats.playback.queues.YouTubeAlbumRadio
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.ui.menu.AlbumMenu
import com.darkxvenom.airbeats.ui.menu.ArtistMenu
import com.darkxvenom.airbeats.ui.menu.SongMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeAlbumMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeArtistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubePlaylistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeSongMenu
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.viewmodels.HeroPlaylistData
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun NewClassicHomeScreen(
    navController: NavController,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val heroPlaylist by viewModel.heroPlaylist.collectAsState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val aiRecommendedPlaylist by viewModel.aiRecommendedPlaylist.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val accountName by rememberPreference(AccountNameKey, "")
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val userAvatarUrl = if (isLoggedIn) accountImageUrl else null

    LaunchedEffect(innerTubeCookie) {
        viewModel.onAccountChanged(innerTubeCookie)
    }

    val listState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val effectiveHero: HeroPlaylistData = remember(heroPlaylist, quickPicks) {
        heroPlaylist ?: quickPicks?.firstOrNull()?.let { firstSong ->
            val topArtist = firstSong.artists.firstOrNull()?.name ?: "Top Hits"
            HeroPlaylistData(
                title = firstSong.song.title.ifBlank { "$topArtist Soundtracks" },
                subtitle = firstSong.artists.joinToString { it.name }.ifEmpty { "Based on your last listening habits and artists..." },
                tag = "RECOMMENDED",
                thumbnailUrl = firstSong.thumbnailUrl,
                songs = quickPicks.orEmpty(),
                playlistId = null
            )
        } ?: HeroPlaylistData(
            title = "90s HipHop Soundtracks",
            subtitle = "Based on your last listening habits and artists...",
            tag = "HIPHOP",
            thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800",
            songs = emptyList(),
            playlistId = null
        )
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val screenBg = if (isDark) Color.Black else MaterialTheme.colorScheme.background
    val hazeState = remember { dev.chrisbanes.haze.HazeState() }
    val isAtTop by remember {
        androidx.compose.runtime.derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    val blurAlpha by animateFloatAsState(
        targetValue = if (isAtTop) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "NewClassicBlurAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .pullToRefresh(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                onRefresh = viewModel::refresh
            )
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .haze(state = hazeState),
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 110.dp
            )
        ) {
            item(key = "hero_section") {
                NewClassicHeroSection(
                    heroData = effectiveHero,
                    onNewReleaseClick = { navController.navigate("new_release") },
                    onDeveloperNewsClick = { navController.navigate("settings/developer_news") },
                    onSettingsClick = { navController.navigate("settings") },
                    onPlayNowClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (effectiveHero.songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = effectiveHero.title,
                                    items = effectiveHero.songs.map { it.toMediaItem() }
                                )
                            )
                        } else if (!effectiveHero.playlistId.isNullOrBlank()) {
                            playerConnection.playQueue(
                                YouTubeAlbumRadio(effectiveHero.playlistId)
                            )
                        } else {
                            quickPicks?.firstOrNull()?.let { song ->
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                        }
                    }
                )
            }

            val trendingSongs = quickPicks.orEmpty().ifEmpty {
                homePage?.sections?.firstOrNull()?.items?.filterIsInstance<SongItem>().orEmpty().map { item ->
                    Song(
                        song = item.toMediaMetadata().toSongEntity(),
                        artists = item.artists.map { a ->
                            com.darkxvenom.airbeats.db.entities.ArtistEntity(id = a.id ?: "", name = a.name)
                        },
                        album = item.album?.let { a ->
                            com.darkxvenom.airbeats.db.entities.AlbumEntity(id = a.id, title = a.name, songCount = 0, duration = 0)
                        }
                    )
                }
            }

            // 1. On Trending
            if (trendingSongs.isNotEmpty()) {
                item(key = "section_trending") {
                    NewClassicSectionHeader(
                        title = "On Trending",
                        onSeeAllClick = {
                            navController.navigate("explore")
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(trendingSongs, key = { it.id }) { song ->
                            NewClassicSongCard(
                                title = song.title,
                                subtitle = song.artists.joinToString { it.name }.ifEmpty { "AirBeats" },
                                thumbnailUrl = song.thumbnailUrl,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. Keep Listening
            keepListening?.takeIf { it.isNotEmpty() }?.let { keepList ->
                item(key = "section_keep_listening") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = stringResource(R.string.keep_listening),
                        onSeeAllClick = {
                            navController.navigate("history")
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(keepList, key = { it.id }) { item ->
                            when (item) {
                                is Song -> NewClassicSongCard(
                                    title = item.title,
                                    subtitle = item.artists.joinToString { it.name }.ifEmpty { "Song" },
                                    thumbnailUrl = item.thumbnailUrl,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            SongMenu(
                                                originalSong = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                is Album -> NewClassicSongCard(
                                    title = item.title,
                                    subtitle = item.artists.joinToString { it.name }.ifEmpty { "Album" },
                                    thumbnailUrl = item.thumbnailUrl,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navController.navigate("album/${item.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            AlbumMenu(
                                                originalAlbum = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                is Artist -> NewClassicSongCard(
                                    title = item.title,
                                    subtitle = "Artist",
                                    thumbnailUrl = item.thumbnailUrl,
                                    shape = CircleShape,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        navController.navigate("artist/${item.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            ArtistMenu(
                                                originalArtist = item,
                                                coroutineScope = scope,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                else -> {}
                            }
                        }
                    }
                }
            }

            // 3. Your YouTube Account / Playlists
            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                item(key = "section_yt_account") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = accountName.ifBlank { stringResource(R.string.your_ytb_playlists) },
                        subtitle = if (accountName.isNotBlank()) stringResource(R.string.your_ytb_playlists) else null,
                        thumbnail = {
                            if (userAvatarUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(userAvatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.person),
                                        contentDescription = null,
                                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onSeeAllClick = {
                            navController.navigate("account")
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlists, key = { it.id }) { item ->
                            NewClassicSongCard(
                                title = item.title,
                                subtitle = item.author?.name ?: "Playlist",
                                thumbnailUrl = item.thumbnail,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navController.navigate("online_playlist/${item.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = scope,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 4. Similar to <...>
            similarRecommendations?.forEach { rec ->
                item(key = "section_similar_${rec.title.id}") {
                    Spacer(Modifier.height(20.dp))
                    val headerTitle = when (val title = rec.title) {
                        is Song -> "${stringResource(R.string.similar_to)} ${title.title}"
                        is Artist -> "More from ${title.title}"
                        is Album -> "More from ${title.title}"
                        else -> "${stringResource(R.string.similar_to)} ${title.title}"
                    }
                    NewClassicSectionHeader(
                        title = headerTitle,
                        onSeeAllClick = {
                            when (rec.title) {
                                is Song -> rec.title.album?.let { navController.navigate("album/${it.id}") } ?: navController.navigate("explore")
                                is Album -> navController.navigate("album/${rec.title.id}")
                                is Artist -> navController.navigate("artist/${rec.title.id}")
                                else -> navController.navigate("explore")
                            }
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(rec.items, key = { it.id }) { item ->
                            val isArtist = item is ArtistItem
                            val sub = when (item) {
                                is SongItem -> item.artists.joinToString { it.name }
                                is AlbumItem -> item.artists?.joinToString { it.name } ?: "Album"
                                is ArtistItem -> "Artist"
                                is PlaylistItem -> item.author?.name ?: "Playlist"
                            }
                            NewClassicSongCard(
                                title = item.title,
                                subtitle = sub,
                                thumbnailUrl = item.thumbnail,
                                shape = if (isArtist) CircleShape else RoundedCornerShape(16.dp),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    when (item) {
                                        is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                        is AlbumItem -> navController.navigate("album/${item.id}")
                                        is ArtistItem -> navController.navigate("artist/${item.id}")
                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    when (item) {
                                        is SongItem -> menuState.show {
                                            YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                        }
                                        is AlbumItem -> menuState.show {
                                            YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                        }
                                        is ArtistItem -> menuState.show {
                                            YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                        }
                                        is PlaylistItem -> menuState.show {
                                            YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 5. YouTube Music Sections (Listen Again, Mixed for you, From community, etc.)
            homePage?.sections?.forEachIndexed { index, section ->
                if (section.items.isNotEmpty()) {
                    item(key = "section_yt_home_${section.title}_$index") {
                        Spacer(Modifier.height(20.dp))
                        NewClassicSectionHeader(
                            title = section.title,
                            subtitle = section.label,
                            onSeeAllClick = {
                                navController.navigate("explore")
                            }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(section.items, key = { "${it.id}_$index" }) { item ->
                                val isArtist = item is ArtistItem
                                val sub = when (item) {
                                    is SongItem -> item.artists.joinToString { it.name }
                                    is AlbumItem -> item.artists?.joinToString { it.name } ?: "Album"
                                    is ArtistItem -> "Artist"
                                    is PlaylistItem -> item.author?.name ?: "Playlist"
                                }
                                NewClassicSongCard(
                                    title = item.title,
                                    subtitle = sub,
                                    thumbnailUrl = item.thumbnail,
                                    shape = if (isArtist) CircleShape else RoundedCornerShape(16.dp),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        when (item) {
                                            is SongItem -> playerConnection.playQueue(
                                                YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata())
                                            )
                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        when (item) {
                                            is SongItem -> menuState.show {
                                                YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                            }
                                            is AlbumItem -> menuState.show {
                                                YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                            }
                                            is ArtistItem -> menuState.show {
                                                YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                            }
                                            is PlaylistItem -> menuState.show {
                                                YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 6. New Releases
            explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { newReleases ->
                item(key = "section_new_releases") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = stringResource(R.string.new_release_albums),
                        onSeeAllClick = {
                            navController.navigate("new_release")
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(newReleases, key = { it.id }) { album ->
                            NewClassicSongCard(
                                title = album.title,
                                subtitle = album.artists?.joinToString { it.name } ?: "Album",
                                thumbnailUrl = album.thumbnail,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    navController.navigate("album/${album.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeAlbumMenu(
                                            albumItem = album,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 7. Forgotten Favourites
            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favSongs ->
                item(key = "section_forgotten_favorites") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = stringResource(R.string.forgotten_favorites),
                        onSeeAllClick = {
                            navController.navigate("auto_playlist/liked")
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(favSongs, key = { it.id }) { song ->
                            NewClassicSongCard(
                                title = song.title,
                                subtitle = song.artists.joinToString { it.name }.ifEmpty { "AirBeats" },
                                thumbnailUrl = song.thumbnailUrl,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 8. Recommended by AI
            if (aiRecommendedPlaylist != null && aiRecommendedPlaylist!!.second.isNotEmpty()) {
                val (playlist, songs) = aiRecommendedPlaylist!!
                item(key = "section_ai_recommended") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = "Recommended by AI",
                        onSeeAllClick = {
                            navController.navigate("local_playlist/${playlist.id}")
                        }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(songs, key = { it.id }) { song ->
                            NewClassicSongCard(
                                title = song.title,
                                subtitle = song.artists.joinToString { it.name },
                                thumbnailUrl = song.thumbnailUrl,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "Recommended by AI",
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = songs.indexOf(song).coerceAtLeast(0)
                                        )
                                    )
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(40.dp))
            }
        }

        com.darkxvenom.airbeats.ui.component.TopFadeBlur(
            hazeState = hazeState,
            pageColor = Color.Transparent,
            scrimColor = Color.Transparent,
            alpha = blurAlpha,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        PullToRefreshDefaults.LoadingIndicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

@Composable
private fun NewClassicHeroSection(
    heroData: HeroPlaylistData,
    onNewReleaseClick: () -> Unit,
    onDeveloperNewsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlayNowClick: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val heroHeight = (configuration.screenHeightDp.dp * 0.58f).coerceAtLeast(490.dp) + statusBarHeight
    val isFrosted = isFrostedGlassUiEnabled()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val blendColor = if (isDark) Color.Black else MaterialTheme.colorScheme.background
    val heroTextPrimary = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground
    val heroTextSecondary = if (isDark) Color.White.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clipToBounds()
            .background(blendColor)
    ) {
        // Crisp high-resolution hero image with immersive zoom
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(heroData.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = heroData.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.15f)
        )

        // Continuous organic gradient blend dissolving seamlessly into pure OLED black or clean light theme surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to (if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.45f)),
                            0.12f to (if (isDark) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.15f)),
                            0.24f to Color.Transparent,
                            0.38f to Color.Transparent,
                            0.50f to (if (isDark) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.20f)),
                            0.62f to (if (isDark) Color.Black.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.45f)),
                            0.74f to (if (isDark) Color.Black.copy(alpha = 0.68f) else Color.White.copy(alpha = 0.75f)),
                            0.86f to (if (isDark) Color.Black.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f)),
                            0.94f to blendColor,
                            1.0f to blendColor
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarHeight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                val buttonBg = if (isDark) {
                    if (isFrosted) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
                } else {
                    if (isFrosted) Color.Black.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.10f)
                }
                val buttonBorder = if (isFrosted) {
                    BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))
                } else null
                val buttonTint = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface

                IconButton(
                    onClick = onNewReleaseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .then(
                            if (buttonBorder != null) Modifier.border(buttonBorder, CircleShape)
                            else Modifier
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.notification_on),
                        contentDescription = "New Releases",
                        tint = buttonTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                IconButton(
                    onClick = onDeveloperNewsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .then(
                            if (buttonBorder != null) Modifier.border(buttonBorder, CircleShape)
                            else Modifier
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.newspaper),
                        contentDescription = "News from Developer",
                        tint = buttonTint,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .then(
                            if (buttonBorder != null) Modifier.border(buttonBorder, CircleShape)
                            else Modifier
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings",
                        tint = buttonTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }


            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = heroData.tag.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp
                    )
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = heroData.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = heroTextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = heroData.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = heroTextSecondary,
                        fontSize = 12.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(16.dp))

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val buttonScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.93f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "playNowScale"
                )

                val playContainerColor = if (isFrosted) {
                    if (isDark) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.primary
                }
                val playContentColor = if (isFrosted) {
                    if (isDark) Color.White else MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onPrimary
                }
                val playBorder = if (isFrosted) {
                    BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                } else null

                Button(
                    onClick = onPlayNowClick,
                    interactionSource = interactionSource,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = playContainerColor,
                        contentColor = playContentColor
                    ),
                    border = playBorder,
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 11.dp),
                    modifier = Modifier
                        .scale(buttonScale)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = playContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "PLAY NOW",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = playContentColor
                            )
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun NewClassicSectionHeader(
    title: String,
    subtitle: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.onBackground
    val textSecondary = if (isDark) Color(0xFF909AA8) else MaterialTheme.colorScheme.onSurfaceVariant
    val seeAllBg = if (isDark) Color(0xFF1C1C1C) else MaterialTheme.colorScheme.surfaceVariant
    val seeAllTextColor = if (isDark) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (thumbnail != null) {
                thumbnail()
                Spacer(Modifier.width(10.dp))
            }
            Column {
                if (subtitle != null) {
                    Text(
                        text = subtitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = textSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onSeeAllClick != null) {
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(seeAllBg)
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SEE ALL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = seeAllTextColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 10.5.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun NewClassicSongCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    shape: Shape = RoundedCornerShape(16.dp),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val surfaceColor = if (isDark) Color(0xFF141414) else MaterialTheme.colorScheme.surfaceContainer
    val textPrimary = if (isDark) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.onBackground
    val textSecondary = if (isDark) Color(0xFF909AA8) else MaterialTheme.colorScheme.onSurfaceVariant

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cardScale"
    )

    Column(
        modifier = modifier
            .width(140.dp)
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(shape)
                .background(surfaceColor)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = textSecondary,
                fontSize = 12.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
