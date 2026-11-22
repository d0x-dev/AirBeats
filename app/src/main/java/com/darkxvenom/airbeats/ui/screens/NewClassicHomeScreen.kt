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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.darkxvenom.airbeats.data.auth.hasApiSid
import com.darkxvenom.airbeats.data.model.HomeFeedItem
import com.darkxvenom.airbeats.data.model.HomeFeedShelf
import com.darkxvenom.airbeats.data.model.HomeItemKind
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

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val remoteShelves by viewModel.remoteShelves.collectAsState()
    val accountPlaylistsFeed by viewModel.accountPlaylistsFeed.collectAsState()
    val subscribedArtistsFeed by viewModel.subscribedArtistsFeed.collectAsState()
    val likedSongsFeed by viewModel.likedSongsFeed.collectAsState()
    val historyItemsFeed by viewModel.historyItemsFeed.collectAsState()
    val heroCardsFeed by viewModel.heroCardsFeed.collectAsState()

    val accountName by rememberPreference(AccountNameKey, "")
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        hasApiSid(innerTubeCookie)
    }

    LaunchedEffect(innerTubeCookie) {
        viewModel.onAccountChanged(innerTubeCookie)
    }

    val listState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    val currentRoute = backStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        if (currentRoute == Screens.Home.route || currentRoute == "home") {
            isSettingsOpen = false
        }
    }
    val gearRotation by animateFloatAsState(
        targetValue = if (isSettingsOpen) 360f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "newClassicSettingsGearRotation"
    )
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val midnightAirTitle = stringResource(R.string.ui_heading_midnight_air)
    val effectiveHero: HeroPlaylistData = remember(heroCardsFeed, remoteShelves, midnightAirTitle) {
        val topHero = heroCardsFeed.firstOrNull()
        if (topHero != null) {
            HeroPlaylistData(
                title = topHero.title,
                subtitle = topHero.subtitle.ifBlank { topHero.caption },
                tag = topHero.category.uppercase(),
                thumbnailUrl = topHero.imageUrl,
                songs = emptyList(),
                playlistId = topHero.item.browseId ?: topHero.item.watchPlaylistId,
            )
        } else {
            val firstSection = remoteShelves.firstOrNull()
            val firstItem = firstSection?.items?.firstOrNull()
            HeroPlaylistData(
                title = firstItem?.title ?: firstSection?.title ?: midnightAirTitle,
                subtitle = firstItem?.subtitle ?: firstItem?.artistName.orEmpty(),
                tag = "YOUTUBE MUSIC",
                thumbnailUrl = firstItem?.thumbnailUrl,
                songs = emptyList(),
                playlistId = null,
            )
        }
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

    fun handleFeedItemClick(item: HomeFeedItem) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when (item.kind) {
            HomeItemKind.SONG, HomeItemKind.VIDEO, HomeItemKind.EPISODE -> {
                val metadata = item.toMediaMetadata()
                val endpoint = item.videoId?.let { WatchEndpoint(videoId = it) }
                    ?: WatchEndpoint(videoId = item.browseId ?: "")
                playerConnection.playQueue(YouTubeQueue(endpoint, metadata))
            }
            HomeItemKind.ALBUM -> {
                navController.navigate("album/${item.browseId}")
            }
            HomeItemKind.ARTIST -> {
                navController.navigate("artist/${item.browseId}")
            }
            HomeItemKind.PLAYLIST, HomeItemKind.PODCAST -> {
                val id = item.watchPlaylistId ?: item.browseId?.removePrefix("VL") ?: item.browseId.orEmpty()
                navController.navigate("online_playlist/$id")
            }
        }
    }

    fun handleFeedItemLongClick(item: HomeFeedItem) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when (item.kind) {
            HomeItemKind.SONG, HomeItemKind.VIDEO, HomeItemKind.EPISODE -> {
                menuState.show {
                    YouTubeSongMenu(song = item.toSongItem(), navController = navController, onDismiss = menuState::dismiss)
                }
            }
            HomeItemKind.ALBUM -> {
                menuState.show {
                    YouTubeAlbumMenu(
                        albumItem = AlbumItem(
                            browseId = item.browseId ?: "",
                            playlistId = item.watchPlaylistId ?: item.browseId ?: "",
                            title = item.title,
                            artists = item.artistName.takeIf { it.isNotBlank() }?.let {
                                listOf(com.darkxvenom.airbeats.innertube.models.Artist(name = it, id = item.artistId))
                            },
                            year = null,
                            thumbnail = item.thumbnailUrl.orEmpty()
                        ),
                        navController = navController,
                        onDismiss = menuState::dismiss
                    )
                }
            }
            HomeItemKind.ARTIST -> {
                menuState.show {
                    YouTubeArtistMenu(
                        artist = ArtistItem(
                            id = item.browseId ?: "",
                            title = item.title,
                            thumbnail = item.thumbnailUrl,
                            subscriberCountText = item.subtitle,
                            shuffleEndpoint = null,
                            radioEndpoint = null
                        ),
                        onDismiss = menuState::dismiss
                    )
                }
            }
            HomeItemKind.PLAYLIST, HomeItemKind.PODCAST -> {
                menuState.show {
                    YouTubePlaylistMenu(
                        playlist = PlaylistItem(
                            id = item.watchPlaylistId ?: item.browseId?.removePrefix("VL") ?: "",
                            title = item.title,
                            author = null,
                            songCountText = item.subtitle,
                            thumbnail = item.thumbnailUrl,
                            playEndpoint = null,
                            shuffleEndpoint = null,
                            radioEndpoint = null
                        ),
                        coroutineScope = scope,
                        onDismiss = menuState::dismiss
                    )
                }
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
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 110.dp
            )
        ) {
            // Hero Section
            item(key = "hero_section") {
                NewClassicHeroSection(
                    heroData = effectiveHero,
                    onNewReleaseClick = { navController.navigate("new_release") },
                    onDeveloperNewsClick = { navController.navigate("settings/developer_news") },
                    onSettingsClick = {
                        isSettingsOpen = true
                        navController.navigate("settings")
                    },
                    gearRotation = gearRotation,
                    onPlayNowClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val topHero = heroCardsFeed.firstOrNull()
                        if (topHero != null) {
                            val item = topHero.item
                            if (item.isPlayableTrack) {
                                playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                            } else if (!item.browseId.isNullOrBlank()) {
                                playerConnection.playQueue(YouTubeAlbumRadio(item.browseId))
                            } else if (!item.watchPlaylistId.isNullOrBlank()) {
                                playerConnection.playQueue(YouTubeAlbumRadio(item.watchPlaylistId))
                            }
                        } else {
                            val firstSong = remoteShelves.firstOrNull { it.isSongShelf }?.items?.firstOrNull()
                            if (firstSong != null) {
                                playerConnection.playQueue(YouTubeQueue.radio(firstSong.toMediaMetadata()))
                            }
                        }
                    }
                )
            }

            // Quick Picks / Top Songs
            val firstSongShelf = remoteShelves.firstOrNull { it.isSongShelf }
            if (firstSongShelf != null && firstSongShelf.items.isNotEmpty()) {
                item(key = "section_quick_picks") {
                    NewClassicSectionHeader(
                        title = if (isLoggedIn) (firstSongShelf.title.ifBlank { stringResource(R.string.quick_picks) }) else stringResource(R.string.ui_heading_on_trending),
                        subtitle = firstSongShelf.strapline.takeIf { it.isNotBlank() },
                        onSeeAllClick = { navController.navigate("explore") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(firstSongShelf.items, key = { it.stableKey }) { songItem ->
                            NewClassicSongCard(
                                title = songItem.title,
                                subtitle = songItem.artistName.ifBlank { songItem.subtitle },
                                thumbnailUrl = songItem.thumbnailUrl,
                                onClick = { handleFeedItemClick(songItem) },
                                onLongClick = { handleFeedItemLongClick(songItem) }
                            )
                        }
                    }
                }
            }

            // After Login: Your YouTube Playlists
            if (isLoggedIn && accountPlaylistsFeed.isNotEmpty()) {
                item(key = "section_account_playlists") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = "Your Playlists",
                        subtitle = "YOUTUBE MUSIC",
                        onSeeAllClick = { navController.navigate("library/playlists") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(accountPlaylistsFeed, key = { it.stableKey }) { playlist ->
                            NewClassicSongCard(
                                title = playlist.title,
                                subtitle = playlist.subtitle,
                                thumbnailUrl = playlist.thumbnailUrl,
                                onClick = { handleFeedItemClick(playlist) },
                                onLongClick = { handleFeedItemLongClick(playlist) }
                            )
                        }
                    }
                }
            }

            // After Login: Subscribed Artists (CircleShape cards)
            if (isLoggedIn && subscribedArtistsFeed.isNotEmpty()) {
                item(key = "section_subscribed_artists") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = "Subscribed Artists",
                        subtitle = "CHANNELS YOU FOLLOW",
                        onSeeAllClick = { navController.navigate("library/artists") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subscribedArtistsFeed, key = { it.stableKey }) { artist ->
                            NewClassicSongCard(
                                title = artist.title,
                                subtitle = artist.subtitle,
                                thumbnailUrl = artist.thumbnailUrl,
                                shape = CircleShape,
                                onClick = { handleFeedItemClick(artist) },
                                onLongClick = { handleFeedItemLongClick(artist) }
                            )
                        }
                    }
                }
            }

            // After Login: Liked Songs
            if (isLoggedIn && likedSongsFeed.isNotEmpty()) {
                item(key = "section_liked_songs") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = "Liked Songs",
                        subtitle = "YOUTUBE MUSIC",
                        onSeeAllClick = { navController.navigate("library/songs") }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(likedSongsFeed.take(25), key = { it.stableKey }) { song ->
                            NewClassicSongCard(
                                title = song.title,
                                subtitle = song.artistName.ifBlank { song.subtitle },
                                thumbnailUrl = song.thumbnailUrl,
                                onClick = { handleFeedItemClick(song) },
                                onLongClick = { handleFeedItemLongClick(song) }
                            )
                        }
                    }
                }
            }

            // After Login: Recently Played / History
            if (isLoggedIn && historyItemsFeed.isNotEmpty()) {
                item(key = "section_history_tracks") {
                    Spacer(Modifier.height(20.dp))
                    NewClassicSectionHeader(
                        title = "Recently Played",
                        subtitle = "YOUTUBE HISTORY",
                        onSeeAllClick = null
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(historyItemsFeed.take(25), key = { it.stableKey }) { song ->
                            NewClassicSongCard(
                                title = song.title,
                                subtitle = song.artistName.ifBlank { song.subtitle },
                                thumbnailUrl = song.thumbnailUrl,
                                onClick = { handleFeedItemClick(song) },
                                onLongClick = { handleFeedItemLongClick(song) }
                            )
                        }
                    }
                }
            }

            // All remaining YouTube Music Shelves (Mixed for you, Listen again, Trending, New Releases, Charts, Podcasts)
            val remainingShelves = remoteShelves.filter { it.key != firstSongShelf?.key }
            remainingShelves.forEachIndexed { index, shelf ->
                if (shelf.items.isNotEmpty()) {
                    item(key = "shelf_${shelf.key}_$index") {
                        Spacer(Modifier.height(20.dp))
                        NewClassicSectionHeader(
                            title = shelf.title,
                            subtitle = shelf.strapline.takeIf { it.isNotBlank() },
                            onSeeAllClick = shelf.moreBrowseId?.let { browseId ->
                                { navController.navigate("browse/$browseId") }
                            } ?: { navController.navigate("explore") }
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(shelf.items, key = { "${it.stableKey}_$index" }) { item ->
                                val isArtist = item.kind == HomeItemKind.ARTIST
                                NewClassicSongCard(
                                    title = item.title,
                                    subtitle = item.artistName.ifBlank { item.subtitle },
                                    thumbnailUrl = item.thumbnailUrl,
                                    shape = if (isArtist) CircleShape else RoundedCornerShape(16.dp),
                                    onClick = { handleFeedItemClick(item) },
                                    onLongClick = { handleFeedItemLongClick(item) }
                                )
                            }
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
    gearRotation: Float = 0f,
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
        AsyncImage(
            model = heroImageRequest,
            contentDescription = heroData.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.15f)
        )

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
                        contentDescription = stringResource(R.string.ui_text_new_releases),
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
                        contentDescription = stringResource(R.string.ui_a11y_news_from_developer),
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
                        contentDescription = stringResource(R.string.settings),
                        tint = buttonTint,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { rotationZ = gearRotation }
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
                    modifier = Modifier.scale(buttonScale)
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
                            text = stringResource(R.string.ui_text_play_now),
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
                    text = stringResource(R.string.ui_text_see_all),
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

    val thumbnailRequest = remember(thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .crossfade(true)
            .build()
    }

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
                model = thumbnailRequest,
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
