package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.LocalUserName
import com.darkxvenom.airbeats.constants.AccountNameKey
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.db.entities.Album
import com.darkxvenom.airbeats.db.entities.Artist
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.ListQueue
import com.darkxvenom.airbeats.playback.queues.YouTubeAlbumRadio
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.TopFadeBlur
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.ui.menu.AlbumMenu
import com.darkxvenom.airbeats.ui.menu.ArtistMenu
import com.darkxvenom.airbeats.ui.menu.SongMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeAlbumMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeArtistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubePlaylistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeSongMenu
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.viewmodels.HeroPlaylistData
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import java.time.LocalTime

// Signature accent lime color from Better home screen mockup
private val BetterLimeAccent = Color(0xFFC6F24D)
private val BetterDarkCard = Color(0xFF161619)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun BetterHomeScreen(
    navController: NavController,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
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
    val userName = LocalUserName.current
    val displayName = if (userName.isNotBlank()) {
        userName
    } else if (accountName.isNotBlank()) {
        accountName
    } else {
        stringResource(R.string.friend)
    }

    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentAvatarSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)

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

    val emptyHeroTitle = stringResource(R.string.ui_heading_midnight_air)
    val emptyHeroSubtitle = stringResource(R.string.ui_heading_editorial_12_tracks_41_min)
    val effectiveHero: HeroPlaylistData = remember(heroPlaylist, quickPicks, emptyHeroTitle, emptyHeroSubtitle) {
        heroPlaylist ?: quickPicks?.firstOrNull()?.let { firstSong ->
            val topArtist = firstSong.artists.firstOrNull()?.name ?: "Top Hits"
            HeroPlaylistData(
                title = firstSong.song.title.ifBlank { "Midnight Air" },
                subtitle = firstSong.artists.joinToString { it.name }.ifEmpty { "Editorial · 12 tracks · 41 min" },
                tag = "FEATURED MIX",
                thumbnailUrl = firstSong.thumbnailUrl,
                songs = quickPicks.orEmpty(),
                playlistId = null
            )
        } ?: HeroPlaylistData(
            title = emptyHeroTitle,
            subtitle = emptyHeroSubtitle,
            tag = "FEATURED MIX",
            thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800",
            songs = emptyList(),
            playlistId = null
        )
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val screenBg = if (isDark) Color.Black else MaterialTheme.colorScheme.background
    val hazeState = remember { dev.chrisbanes.haze.HazeState() }
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    val blurAlpha by animateFloatAsState(
        targetValue = if (isAtTop) 0f else 1f,
        animationSpec = tween(300),
        label = "BetterBlurAlpha"
    )

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..16 -> stringResource(R.string.greeting_afternoon)
        in 17..21 -> stringResource(R.string.greeting_evening)
        else -> stringResource(R.string.greeting_night)
    }

    val defaultMood = stringResource(R.string.mood_late_night)
    var selectedMood by remember(defaultMood) { mutableStateOf(defaultMood) }
    val moodsList = listOf(
        stringResource(R.string.mood_late_night),
        stringResource(R.string.mood_focus),
        stringResource(R.string.mood_workout),
        stringResource(R.string.mood_jazz),
        stringResource(R.string.mood_chill),
        stringResource(R.string.mood_party),
        stringResource(R.string.mood_electronic),
        stringResource(R.string.mood_acoustic)
    )

    val currentPlayingMedia by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()

    val trendingSongs = remember(quickPicks, homePage) {
        quickPicks.orEmpty().ifEmpty {
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
    }

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
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 100.dp
            )
        ) {
            // 1. Hero Section with Top Bar & Nebula Glow
            item(key = "better_hero_section") {
                BetterHeroSection(
                    heroData = effectiveHero,
                    displayName = displayName,
                    currentAvatarSelection = currentAvatarSelection,
                    userAvatarUrl = userAvatarUrl,
                    greeting = greeting,
                    onProfileClick = { navController.navigate("settings/account") },
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
                    },
                    onLikeClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playerConnection.toggleLike()
                    }
                )
            }

            // 01. Continue listening
            item(key = "section_01_continue_listening") {
                Spacer(Modifier.height(16.dp))
                BetterSectionHeader(
                    number = "01",
                    title = stringResource(R.string.keep_listening),
                    onSeeAllClick = { navController.navigate("history") }
                )

                val firstKeepSong = keepListening?.firstOrNull()
                val displayTitle = currentPlayingMedia?.title
                    ?: (firstKeepSong as? Song)?.title
                    ?: trendingSongs.firstOrNull()?.title
                    ?: "Glasshouse"
                val displayArtist = currentPlayingMedia?.artists?.joinToString { it.name }
                    ?: (firstKeepSong as? Song)?.artists?.joinToString { it.name }
                    ?: trendingSongs.firstOrNull()?.artists?.joinToString { it.name }
                    ?: "Nova Sirens · AirBeats Sessions"
                val displayThumb = currentPlayingMedia?.thumbnailUrl
                    ?: (firstKeepSong as? Song)?.thumbnailUrl
                    ?: trendingSongs.firstOrNull()?.thumbnailUrl
                    ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400"

                BetterContinueListeningCard(
                    title = displayTitle,
                    subtitle = displayArtist,
                    thumbnailUrl = displayThumb,
                    isPlaying = isPlaying,
                    progress = if (isPlaying) 0.42f else 0.28f,
                    onPlayPauseClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (currentPlayingMedia != null) {
                            playerConnection.player.togglePlayPause()
                        } else if (trendingSongs.isNotEmpty()) {
                            playerConnection.playQueue(YouTubeQueue.radio(trendingSongs.first().toMediaMetadata()))
                        }
                    },
                    onCardClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (currentPlayingMedia != null) {
                            // Already playing
                        } else if (trendingSongs.isNotEmpty()) {
                            playerConnection.playQueue(YouTubeQueue.radio(trendingSongs.first().toMediaMetadata()))
                        }
                    }
                )
            }

            // 02. Fresh mixes
            if (trendingSongs.isNotEmpty()) {
                item(key = "section_02_fresh_mixes") {
                    Spacer(Modifier.height(24.dp))
                    BetterSectionHeader(
                        number = "02",
                        title = stringResource(R.string.fresh_mixes),
                        onSeeAllClick = { navController.navigate("explore") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(trendingSongs.take(8), key = { "fresh_${it.id}" }) { song ->
                            BetterMixCard(
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

            // 03. Moods and genres
            item(key = "section_03_moods_and_genres") {
                Spacer(Modifier.height(24.dp))
                BetterSectionHeader(
                    number = "03",
                    title = stringResource(R.string.mood_and_genres),
                    onSeeAllClick = { navController.navigate("explore") }
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moodsList) { mood ->
                        val isSelected = mood == selectedMood
                        BetterMoodChip(
                            title = mood,
                            isSelected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedMood = mood
                            }
                        )
                    }
                }
            }

            // 04. Recently
            val recentList = keepListening?.filterIsInstance<Song>().orEmpty().ifEmpty { trendingSongs.drop(1) }
            if (recentList.isNotEmpty()) {
                item(key = "section_04_recently") {
                    Spacer(Modifier.height(24.dp))
                    BetterSectionHeader(
                        number = "04",
                        title = stringResource(R.string.recently),
                        onSeeAllClick = { navController.navigate("history") }
                    )
                }

                items(recentList.take(6), key = { "recent_${it.id}" }) { song ->
                    BetterSongItemRow(
                        song = song,
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

            // YouTube Music Sections (Listen Again, Mixed for you, etc.)
            homePage?.sections?.forEachIndexed { index, section ->
                if (section.items.isNotEmpty()) {
                    item(key = "section_yt_home_${section.title}_$index") {
                        Spacer(Modifier.height(24.dp))
                        val sectionNum = String.format("%02d", (5 + index))
                        BetterSectionHeader(
                            number = sectionNum,
                            title = section.title,
                            onSeeAllClick = { navController.navigate("explore") }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(section.items, key = { "yt_${it.id}_$index" }) { item ->
                                val isArtist = item is ArtistItem
                                val sub = when (item) {
                                    is SongItem -> item.artists.joinToString { it.name }
                                    is AlbumItem -> item.artists?.joinToString { it.name } ?: "Album"
                                    is ArtistItem -> "Artist"
                                    is PlaylistItem -> item.author?.name ?: "Playlist"
                                }
                                BetterMixCard(
                                    title = item.title,
                                    subtitle = sub,
                                    thumbnailUrl = item.thumbnail,
                                    shape = if (isArtist) CircleShape else RoundedCornerShape(18.dp),
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

            // New Releases Albums
            explorePage?.newReleaseAlbums?.takeIf { it.isNotEmpty() }?.let { newReleases ->
                item(key = "section_better_new_releases") {
                    Spacer(Modifier.height(24.dp))
                    BetterSectionHeader(
                        number = "06",
                        title = stringResource(R.string.new_release_albums),
                        onSeeAllClick = { navController.navigate("new_release") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(newReleases, key = { "nr_${it.id}" }) { album ->
                            BetterMixCard(
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

            item(key = "better_bottom_spacer") {
                Spacer(Modifier.height(40.dp))
            }
        }


        TopFadeBlur(
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

/**
 * Top Hero Section featuring profile avatar, greeting, top bar action buttons (New Releases, News from Dev, Settings),
 * artistic artwork background effect like New Classic, and featured mix details with play/like actions.
 */
@Composable
private fun BetterHeroSection(
    heroData: HeroPlaylistData,
    displayName: String,
    currentAvatarSelection: AvatarSelection,
    userAvatarUrl: String?,
    greeting: String,
    onProfileClick: () -> Unit,
    onNewReleaseClick: () -> Unit,
    onDeveloperNewsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlayNowClick: () -> Unit,
    onLikeClick: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val heroHeight = (configuration.screenHeightDp.dp * 0.54f).coerceAtLeast(460.dp) + statusBarHeight
    val isFrosted = isFrostedGlassUiEnabled()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val blendColor = if (isDark) Color.Black else MaterialTheme.colorScheme.background

    val heroImageRequest = remember(heroData.thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(heroData.thumbnailUrl)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .clipToBounds()
            .background(blendColor)
    ) {
        // High resolution hero artwork image
        AsyncImage(
            model = heroImageRequest,
            contentDescription = heroData.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.15f)
        )

        // Dreamy vibrant nebula / colorful smoke glow layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.28f),
                            Color(0xFFD500F9).copy(alpha = 0.22f),
                            Color(0xFF651FFF).copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = Offset(x = 650f, y = 380f),
                        radius = 850f
                    )
                )
        )

        // Continuous organic gradient blend dissolving seamlessly into OLED black (same like New Classic)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to (if (isDark) Color.Black.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.45f)),
                            0.12f to (if (isDark) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f)),
                            0.25f to Color.Transparent,
                            0.40f to Color.Transparent,
                            0.52f to (if (isDark) Color.Black.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.20f)),
                            0.65f to (if (isDark) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.45f)),
                            0.78f to (if (isDark) Color.Black.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f)),
                            0.88f to (if (isDark) Color.Black.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.94f)),
                            0.96f to blendColor,
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
            // Top Bar: Left = Profile avatar & greeting name; Right = New Releases, Developer News, Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Avatar + Greeting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onProfileClick() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when (currentAvatarSelection) {
                            is AvatarSelection.Custom -> {
                                AsyncImage(
                                    model = (currentAvatarSelection as AvatarSelection.Custom).uri.toUri(),
                                    contentDescription = stringResource(R.string.ui_a11y_profile),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            is AvatarSelection.DiceBear -> {
                                AsyncImage(
                                    model = (currentAvatarSelection as AvatarSelection.DiceBear).url,
                                    contentDescription = stringResource(R.string.ui_a11y_profile),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                if (!userAvatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = userAvatarUrl,
                                        contentDescription = stringResource(R.string.ui_a11y_profile),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.person),
                                            contentDescription = stringResource(R.string.ui_a11y_profile),
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.greeting_user, displayName),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = greeting,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Right: 3 pill/circular action buttons
                val buttonBg = if (isDark) {
                    if (isFrosted) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
                } else {
                    if (isFrosted) Color.Black.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.10f)
                }
                val buttonBorder = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f))

                // New Releases
                IconButton(
                    onClick = onNewReleaseClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .border(buttonBorder, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.notification_on),
                        contentDescription = stringResource(R.string.ui_text_new_releases),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // News From Developer
                IconButton(
                    onClick = onDeveloperNewsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .border(buttonBorder, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.newspaper),
                        contentDescription = stringResource(R.string.ui_a11y_news_from_developer),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Settings
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(buttonBg)
                        .border(buttonBorder, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = stringResource(R.string.settings),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Hero details & action buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = heroData.tag.ifBlank { stringResource(R.string.featured_mix) }.uppercase(),
                    color = BetterLimeAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = heroData.title.ifBlank { "Midnight Air" },
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = heroData.subtitle.ifBlank { "Editorial · 12 tracks · 41 min" },
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(18.dp))

                // Actions: Lime Play Pill + Heart button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Play pill button
                    Surface(
                        onClick = onPlayNowClick,
                        shape = RoundedCornerShape(24.dp),
                        color = BetterLimeAccent,
                        contentColor = Color.Black,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.music_recognition_overlay_play),
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.play),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Heart button
                    IconButton(
                        onClick = onLikeClick,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(R.string.ui_a11y_favorite),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section Header formatted with lime green number (e.g. "01", "02") and white title
 */
@Composable
private fun BetterSectionHeader(
    number: String,
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number,
            color = BetterLimeAccent,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(Modifier.weight(1f))

        if (onSeeAllClick != null) {
            Text(
                text = stringResource(R.string.see_all),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Section 01: Continue listening card with track info, lime-green progress indicator, and circular play/pause button
 */
@Composable
private fun BetterContinueListeningCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onCardClick: () -> Unit,
) {
    Surface(
        onClick = onCardClick,
        shape = RoundedCornerShape(20.dp),
        color = BetterDarkCard,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Square album thumbnail
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                // Lime green progress indicator bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0.05f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(BetterLimeAccent)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Lime circular play/pause button
            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BetterLimeAccent)
            ) {
                if (isPlaying) {
                    Icon(
                        painter = painterResource(R.drawable.pause),
                        contentDescription = stringResource(R.string.ui_a11y_pause),
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.music_recognition_overlay_play),
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Square mix card for Fresh mixes & YouTube playlists
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BetterMixCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    shape: Shape = RoundedCornerShape(18.dp),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(135.dp)
                .clip(shape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), shape)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Interactive Mood/Genre Chip with lime accent when selected
 */
@Composable
private fun BetterMoodChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isSelected) BetterLimeAccent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.07f)
    val textColor = if (isSelected) BetterLimeAccent else Color.White.copy(alpha = 0.75f)
    val border = if (isSelected) BorderStroke(1.5.dp, BetterLimeAccent) else BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = border
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

/**
 * Song item row for Section 04: Recently
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BetterSongItemRow(
    song: Song,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = song.artists.joinToString { it.name }.ifEmpty { "AirBeats" },
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


