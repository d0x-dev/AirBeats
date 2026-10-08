package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import com.darkxvenom.airbeats.BuildConfig
import com.darkxvenom.airbeats.checkForUpdates
import com.darkxvenom.airbeats.isNewerVersion
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.darkxvenom.airbeats.ui.component.ChipsRow
import com.darkxvenom.airbeats.ui.component.TopFadeBlur
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.ui.component.HomeTasteStrip
import com.darkxvenom.airbeats.ui.component.UniversalHomeHeroBanner
import com.darkxvenom.airbeats.ui.component.UniversalArtistSpotlightCard
import com.darkxvenom.airbeats.ui.component.UniversalTopArtistsRow
import com.darkxvenom.airbeats.ui.component.HomeThemeStyle
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AccountNameKey
import com.darkxvenom.airbeats.constants.GridThumbnailHeight
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.constants.ListItemHeight
import com.darkxvenom.airbeats.constants.ListThumbnailSize
import com.darkxvenom.airbeats.constants.ThumbnailCornerRadius
import com.darkxvenom.airbeats.db.entities.Album
import com.darkxvenom.airbeats.db.entities.Artist
import com.darkxvenom.airbeats.db.entities.LocalItem
import com.darkxvenom.airbeats.db.entities.Playlist
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.LocalAlbumRadio
import com.darkxvenom.airbeats.playback.queues.YouTubeAlbumRadio
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.AlbumGridItem
import com.darkxvenom.airbeats.ui.component.ArtistGridItem
import com.darkxvenom.airbeats.ui.component.ChipsRow
import com.darkxvenom.airbeats.ui.component.HideOnScrollFAB
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.NavigationTitle
import com.darkxvenom.airbeats.ui.component.SongGridItem
import com.darkxvenom.airbeats.ui.component.SongListItem
import com.darkxvenom.airbeats.ui.component.YouTubeGridItem
import com.darkxvenom.airbeats.ui.component.shimmer.GridItemPlaceHolder
import com.darkxvenom.airbeats.ui.component.shimmer.ShimmerHost
import com.darkxvenom.airbeats.ui.component.shimmer.TextPlaceholder
import com.darkxvenom.airbeats.ui.menu.AlbumMenu
import com.darkxvenom.airbeats.ui.menu.ArtistMenu
import com.darkxvenom.airbeats.ui.menu.SongMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeAlbumMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeArtistMenu
import com.darkxvenom.airbeats.ui.component.CircleIconButton
import com.darkxvenom.airbeats.ui.menu.YouTubePlaylistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeSongMenu
import com.darkxvenom.airbeats.ui.utils.SnapLayoutInfoProvider
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.random.Random
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.core.net.toUri
import com.darkxvenom.airbeats.ui.component.LocalUserName
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.RankPreferenceManager
import com.darkxvenom.airbeats.ui.component.RankBadge
import com.darkxvenom.airbeats.ui.component.BadgeSelector
import com.darkxvenom.airbeats.ui.component.unlockedRanksFromHours
import com.darkxvenom.airbeats.viewmodels.StatsViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
)
{
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val aiRecommendedPlaylist by viewModel.aiRecommendedPlaylist.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by rememberPreference(AccountNameKey, "")
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    // Returning from YouTube login used to leave the already-created Home
    // ViewModel showing the anonymous feed until the user manually refreshed.
    LaunchedEffect(innerTubeCookie) {
        viewModel.onAccountChanged(innerTubeCookie)
    }

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val hazeState = remember { HazeState() }
    val isAtTop by remember {
        derivedStateOf {
            lazylistState.firstVisibleItemIndex == 0 && lazylistState.firstVisibleItemScrollOffset == 0
        }
    }
    val blurAlpha by animateFloatAsState(
        targetValue = if (isAtTop) 0f else 1f,
        animationSpec = tween(300),
        label = "HomeScreenBlurAlpha"
    )

    val hiddenSections by rememberPreference(com.darkxvenom.airbeats.constants.HiddenHomeSectionsKey, defaultValue = emptySet())
    fun isSectionVisible(section: com.darkxvenom.airbeats.constants.MaterialHomeSection): Boolean = section.id !in hiddenSections

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(
                                        videoId = item.id
                                    ), item.toMediaMetadata()
                                )
                            )

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            quickPicksLazyGridState.scrollToItem(0)
            forgottenFavoritesLazyGridState.scrollToItem(0)
        }
    }

    // Main container Box that holds everything (same pattern as ExploreScreen)
    Box(modifier = Modifier.fillMaxSize()) {

        // Adaptive background: blurred song thumbnail when playing, Library mesh when no song playing
        val artworkUrl = mediaMetadata?.thumbnailUrl
        com.darkxvenom.airbeats.ui.component.ScreenAdaptiveBackground(
            artworkUrl = artworkUrl
        )

        // Content
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
                ),
            contentAlignment = Alignment.TopStart
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = quickPicksLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = forgottenFavoritesLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }
            val freshTracks = remember(aiRecommendedPlaylist, quickPicks) {
                val aiSongs = aiRecommendedPlaylist?.second.orEmpty()
                if (aiSongs.isNotEmpty()) aiSongs else quickPicks?.drop(6).orEmpty()
            }
            val artistTriples = remember(quickPicks) {
                quickPicks?.mapNotNull { pick ->
                    pick.artists.firstOrNull()?.let { artist ->
                        Triple(artist, artist.thumbnailUrl, pick.song.thumbnailUrl)
                    }
                }?.distinctBy { it.first.id }?.take(10).orEmpty()
            }

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState)
            ) {
                // ModernHomeTopBarInline is now inside the LazyColumn
                item(key = "home_top_bar") {
                    ModernHomeTopBarInline(
                        navController = navController,
                        onSearchClick = onSearchClick
                    )
                }

                if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.HERO)) {
                    item(key = "classic_hero") {
                        UniversalHomeHeroBanner(
                            title = "Welcome back, $accountName",
                            subtitle = "Continuous radio tuned to your favorites",
                            onPlayRadio = {
                                com.darkxvenom.airbeats.ui.component.InfiniteRadioHelper.playShuffledRadio(
                                    playerConnection = playerConnection,
                                    currentSongId = mediaMetadata?.id,
                                    quickPicks = quickPicks,
                                    forgottenFavorites = forgottenFavorites,
                                    keepListening = keepListening,
                                    homeSongs = homePage?.sections?.flatMap { it.items }?.filterIsInstance<com.darkxvenom.airbeats.innertube.models.SongItem>()
                                )
                            },
                            style = HomeThemeStyle.CLASSIC,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .animateItem()
                        )
                    }
                }

                if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.QUICK_TILES)) {
                    item(key = "home_chips") {
                        val isFrosted = isFrostedGlassUiEnabled()
                        if (isFrosted) {
                            GlassHomeTagsRow(
                                navController = navController,
                                isLoggedIn = isLoggedIn,
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                    .animateItem()
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                ChipsRow(
                                    chips = listOfNotNull(
                                        Pair("history", stringResource(R.string.history)),
                                        Pair("stats", stringResource(R.string.stats)),
                                        Pair("liked", stringResource(R.string.liked)),
                                        Pair("downloads", stringResource(R.string.offline)),
                                        if (isLoggedIn) Pair(
                                            "account",
                                            stringResource(R.string.account)
                                        ) else null
                                    ),
                                    currentValue = "",
                                    onValueUpdate = { value ->
                                        when (value) {
                                            "history" -> navController.navigate("history")
                                            "stats" -> navController.navigate("stats")
                                            "liked" -> navController.navigate("auto_playlist/liked")
                                            "downloads" -> navController.navigate("auto_playlist/downloaded")
                                            "account" -> if (isLoggedIn) navController.navigate("account")
                                        }
                                    },
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            }
                        }
                    }
                }

                if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.TASTE_STRIP)) {
                    item(key = "classic_taste_strip") {
                        HomeTasteStrip(
                            onTagClick = { tag ->
                                navController.navigate("search/${java.net.URLEncoder.encode(tag, "UTF-8")}")
                            },
                            style = HomeThemeStyle.CLASSIC,
                            modifier = Modifier.padding(vertical = 4.dp).animateItem()
                        )
                    }
                }

                if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.QUICK_PICKS)) {
                    quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                    item(key = "quick_picks_title") {
                        NavigationTitle(
                            title = stringResource(R.string.quick_picks),
                            modifier = Modifier.animateItem()
                        )
                    }
                    item(key = "quick_picks_carousel") {
                        val distinctPicks = remember(picks) { picks.distinctBy { it.id } }
                        HorizontalCenteredHeroCarousel(
                            state = rememberCarouselState { distinctPicks.size },
                            maxItemWidth = 250.dp,
                            itemSpacing = 8.dp,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                                .padding(top = 10.dp, bottom = 14.dp)
                                .animateItem()
                        ) { index ->
                            val currentSong = distinctPicks[index]
                            val isActive = currentSong.id == mediaMetadata?.id

                            val thumbnailUrl = remember(currentSong.thumbnailUrl) {
                                currentSong.thumbnailUrl?.highQualityThumbnail()
                            }
                            val thumbnailRequest = remember(thumbnailUrl, context) {
                                ImageRequest.Builder(context)
                                    .data(thumbnailUrl)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .diskCacheKey(thumbnailUrl)
                                    .build()
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .maskClip(MaterialTheme.shapes.extraLarge)
                                    .maskBorder(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        MaterialTheme.shapes.extraLarge,
                                    )
                                    .combinedClickable(
                                        onClick = {
                                            if (isActive) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(YouTubeQueue.radio(currentSong.toMediaMetadata()))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = currentSong,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                            ) {
                                AsyncImage(
                                    model = thumbnailRequest,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.7f)
                                                )
                                            )
                                        )
                                )

                                if (isActive && isPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .size(32.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.volume_up),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = currentSong.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentSong.artists.joinToString { it.name },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.FRESH_FINDS)) {
                if (freshTracks.isNotEmpty()) {
                    val isAi = aiRecommendedPlaylist?.second?.isNotEmpty() == true
                    item(key = "fresh_finds_title") {
                        NavigationTitle(
                            title = if (isAi) stringResource(R.string.recommended_by_ai) else "Fresh Finds",
                            label = if (isAi) aiRecommendedPlaylist?.first?.playlist?.lastUpdateTime?.let {
                                "Updated: " + it.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, h:mm a"))
                            } else "Discover new gems",
                            onClick = if (isAi) {
                                { navController.navigate("local_playlist/${aiRecommendedPlaylist?.first?.id}") }
                            } else null,
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "fresh_finds_list") {
                        val distinctSongs = remember(freshTracks) { freshTracks.distinctBy { it.id } }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            items(distinctSongs, key = { it.id }) { song ->
                                Box(modifier = Modifier.width(140.dp)) {
                                    localGridItem(song)
                                }
                            }
                        }
                    }
                }
            }

            if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.JUMP_BACK_IN)) {
                keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.keep_listening),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        val rows = if (keepListening.size > 6) 2 else 1
                        LazyHorizontalGrid(
                            state = rememberLazyGridState(),
                            rows = GridCells.Fixed(rows),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((GridThumbnailHeight + with(LocalDensity.current) {
                                    MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                            MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                                }) * rows)
                                .animateItem()
                        ) {
                            items(keepListening) {
                                localGridItem(it)
                            }
                        }
                    }
                }
            }

            if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.MIXES)) {
                accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                    item {
                        NavigationTitle(
                            label = stringResource(R.string.your_ytb_playlists),
                            title = accountName,
                            thumbnail = {
                                if (url != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(url)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(url)
                                            .crossfade(true)
                                            .build(),
                                        placeholder = painterResource(id = R.drawable.person),
                                        error = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        modifier = Modifier.size(ListThumbnailSize)
                                    )
                                }
                            },
                            onClick = {
                                navController.navigate("account")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }


                    item {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(
                                items = accountPlaylists,
                                key = { it.id },
                            ) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.SPOTLIGHT)) {
                quickPicks?.firstOrNull()?.let { pick ->
                    val artist = pick.artists.firstOrNull()
                    if (artist != null) {
                        item(key = "classic_spotlight") {
                            UniversalArtistSpotlightCard(
                                artistName = artist.name,
                                artistId = artist.id,
                                thumbnailUrl = artist.thumbnailUrl,
                                fallbackThumbnail = pick.song.thumbnailUrl,
                                onOpenArtist = {
                                    artist.id.takeIf { it.isNotBlank() }?.let { navController.navigate("artist/$it") }
                                },
                                onPlayRadio = {
                                    playerConnection.playQueue(YouTubeQueue.radio(pick.toMediaMetadata()))
                                },
                                style = HomeThemeStyle.CLASSIC,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .animateItem()
                            )
                        }
                    }
                }
            }

            if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.TOP_ARTISTS)) {
                if (artistTriples.isNotEmpty()) {
                    item(key = "classic_top_artists_title") {
                        NavigationTitle(
                            title = "Artists For You",
                            modifier = Modifier.animateItem()
                        )
                    }
                    item(key = "classic_top_artists_row") {
                        UniversalTopArtistsRow(
                            artists = artistTriples,
                            onArtistClick = { artistId ->
                                artistId.takeIf { it.isNotBlank() }?.let { navController.navigate("artist/$it") }
                            },
                            style = HomeThemeStyle.CLASSIC,
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.BECAUSE_YOU_LISTEN_TO)) {
                similarRecommendations?.forEach { recommendation ->
                    item(key = "similar_title_${recommendation.title.id}") {
                        NavigationTitle(
                            label = stringResource(R.string.similar_to),
                            title = recommendation.title.title,
                            thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
                                {
                                    val shape =
                                        if (recommendation.title is Artist) CircleShape else RoundedCornerShape(
                                            ThumbnailCornerRadius
                                        )
                                    AsyncImage(
                                        model = thumbnailUrl.highQualityThumbnail(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(shape)
                                    )
                                }
                            },
                            onClick = {
                                when (recommendation.title) {
                                    is Song -> navController.navigate("album/${recommendation.title.album!!.id}")
                                    is Album -> navController.navigate("album/${recommendation.title.id}")
                                    is Artist -> navController.navigate("artist/${recommendation.title.id}")
                                    is Playlist -> {}
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "similar_row_${recommendation.title.id}") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(recommendation.items, key = { item -> item.id }) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

                homePage?.sections?.forEach { section ->
                    val isNewRelease = section.title.contains("New", ignoreCase = true) || section.title.contains("Release", ignoreCase = true)
                    val isChart = section.title.contains("Chart", ignoreCase = true) || section.title.contains("Top", ignoreCase = true)
                    val isAlbum = section.title.contains("Album", ignoreCase = true)

                    val shouldRender = when {
                        isNewRelease -> isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.NEW_RELEASES)
                        isChart -> isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.CHARTS)
                        isAlbum -> isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.ALBUMS)
                        else -> true
                    }

                    if (shouldRender) {
                        item(key = "yt_home_title_${section.title}_${section.endpoint}") {
                            NavigationTitle(
                                title = section.title,
                                label = section.label,
                                thumbnail = section.thumbnail?.let { thumbnailUrl ->
                                    {
                                        val shape =
                                            if (section.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                                ThumbnailCornerRadius
                                            )
                                        AsyncImage(
                                            model = thumbnailUrl.highQualityThumbnail(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(ListThumbnailSize)
                                                .clip(shape)
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }

                        item(key = "yt_home_row_${section.title}_${section.endpoint}") {
                            LazyRow(
                                contentPadding = WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                                modifier = Modifier.animateItem()
                            ) {
                                items(section.items, key = { item -> item.id }) { item ->
                                    ytGridItem(item)
                                }
                            }
                        }
                    }
                }

                if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.NEW_RELEASES)) {
                    explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
                        item {
                            NavigationTitle(
                                title = stringResource(R.string.new_release_albums),
                                onClick = {
                                    navController.navigate("new_release")
                                },
                                modifier = Modifier.animateItem()
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding = WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                                modifier = Modifier.animateItem()
                            ) {
                                items(
                                    items = newReleaseAlbums,
                                    key = { it.id }
                                ) { album ->
                                    YouTubeGridItem(
                                        item = album,
                                        isActive = mediaMetadata?.album?.id == album.id,
                                        isPlaying = isPlaying,
                                        coroutineScope = scope,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
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
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item(key = "home_loading_indicator") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                                .animateItem(),
                        ) {
                            LoadingIndicator()
                        }
                    }
                }

                if (isSectionVisible(com.darkxvenom.airbeats.constants.MaterialHomeSection.HEAVY_ROTATION)) {
                    forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                        item {
                            NavigationTitle(
                                title = stringResource(R.string.forgotten_favorites),
                                modifier = Modifier.animateItem()
                            )
                        }

                        item {
                            // take min in case list size is less than 4
                            val rows = min(4, forgottenFavorites.size)
                            LazyHorizontalGrid(
                                state = forgottenFavoritesLazyGridState,
                                rows = GridCells.Fixed(rows),
                                flingBehavior = rememberSnapFlingBehavior(
                                    forgottenFavoritesSnapLayoutInfoProvider
                                ),
                                contentPadding = WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight * rows)
                                    .animateItem()
                            ) {
                                items(
                                    items = forgottenFavorites,
                                    key = { it.id }
                                ) { originalSong ->
                                    val currentSong = originalSong

                                    SongListItem(
                                        song = currentSong,
                                        showInLibraryIcon = true,
                                        isActive = currentSong.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        modifier = Modifier
                                            .width(horizontalLazyGridItemWidth)
                                            .combinedClickable(
                                                onClick = {
                                                    if (currentSong.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(YouTubeQueue.radio(currentSong.toMediaMetadata()))
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = currentSong,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
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

            var fabMenuExpanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                HideOnScrollFAB(
                    visible = true,
                    lazyListState = lazylistState,
                    icon = R.drawable.more_vert,
                    onClick = {
                        fabMenuExpanded = true
                    }
                )

                androidx.compose.material3.DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.shuffle)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            fabMenuExpanded = false
                            scope.launch(Dispatchers.Main) {
                                val localCandidates = allLocalItems
                                val ytCandidates = allYtItems

                                val chooseLocal = when {
                                    localCandidates.isNotEmpty() && ytCandidates.isNotEmpty() -> Random.nextFloat() < 0.5
                                    localCandidates.isNotEmpty() -> true
                                    ytCandidates.isNotEmpty() -> false
                                    else -> null
                                }

                                if (chooseLocal == true) {
                                    when (val luckyItem = localCandidates.randomOrNull()) {
                                        is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                        is Album -> {
                                            val albumWithSongs = withContext(Dispatchers.IO) {
                                                runCatching { database.albumWithSongs(luckyItem.id).first() }.getOrNull()
                                            }
                                            albumWithSongs?.let {
                                                playerConnection.playQueue(LocalAlbumRadio(it))
                                            }
                                        }

                                        is Artist -> {}
                                        is Playlist -> {}
                                        null -> {}
                                    }
                                } else if (chooseLocal == false) {
                                    when (val luckyItem = ytCandidates.randomOrNull()) {
                                        is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                        is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                                        is ArtistItem -> luckyItem.radioEndpoint?.let {
                                            playerConnection.playQueue(YouTubeQueue(it))
                                        }

                                        is PlaylistItem -> (luckyItem.playEndpoint ?: luckyItem.radioEndpoint)?.let {
                                            playerConnection.playQueue(YouTubeQueue(it))
                                        }
                                        null -> {}
                                    }
                                } else {
                                    // Both local and remote collections are empty (initial launch / offline)
                                    // Gracefully attempt to play a recent song or quick pick from the database without crashing
                                    val fallbackSong = withContext(Dispatchers.IO) {
                                        runCatching { database.recentSongs(1).first().firstOrNull() }.getOrNull()
                                    }
                                    if (fallbackSong != null) {
                                        playerConnection.playQueue(YouTubeQueue.radio(fallbackSong.toMediaMetadata()))
                                    }
                                }
                            }
                        }
                    )
                    
                    androidx.compose.material3.DropdownMenuItem(
                        text = { androidx.compose.material3.Text(stringResource(R.string.music_recognition)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.mic),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            fabMenuExpanded = false
                            navController.navigate(com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionRoute)
                        }
                    )

                    androidx.compose.material3.DropdownMenuItem(
                        text = { androidx.compose.material3.Text("AirBeats Charts") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.trending_up),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            fabMenuExpanded = false
                            navController.navigate("charts")
                        }
                    )
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
}
@Composable
fun ModernHomeTopBarInline(
    navController: NavController,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager
        .getAvatarSelection
        .collectAsState(initial = AvatarSelection.Default)

    val playerConnection = LocalPlayerConnection.current
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

    val userName = LocalUserName.current
    val displayName = if (userName.isNotEmpty()) userName else "Friend"

    val currentVersion = BuildConfig.VERSION_NAME
    var showUpdateIcon by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val latestVersion = withContext(Dispatchers.IO) { checkForUpdates() }
            showUpdateIcon =
                latestVersion?.let { isNewerVersion(it, currentVersion) } ?: false
        } catch (_: Exception) {
            showUpdateIcon = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Top)
            )
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 12.dp)
    ) {

        // Top row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Avatar and animated ring
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {

                val playerConnection = LocalPlayerConnection.current
                val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) }

// Simple colorful gradient (you can later extract from artwork)
                val songColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                )

                AnimatedBeatsRing(
                    isPlaying = isPlaying,
                    songColors = songColors,
                    modifier = Modifier.matchParentSize()
                )

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .combinedClickable {
                            navController.navigate("settings/account")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (currentSelection) {

                        is AvatarSelection.Custom -> {
                            AsyncImage(
                                model = (currentSelection as AvatarSelection.Custom).uri.toUri(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        is AvatarSelection.DiceBear -> {
                            AsyncImage(
                                model = (currentSelection as AvatarSelection.DiceBear).url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF8E2DE2),
                                                Color(0xFF4A00E0)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                CircleIconButton(
                    icon = R.drawable.notification_on,
                    onClick = { navController.navigate("new_release") }
                )

                CircleIconButton(
                    icon = R.drawable.newspaper,
                    onClick = {
                        navController.navigate("settings/developer_news")
                    }
                )

                CircleIconButton(
                    icon = if (showUpdateIcon)
                        R.drawable.update
                    else
                        R.drawable.settings,
                    onClick = { navController.navigate("settings") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Hi, $displayName",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            val context = LocalContext.current
            val rankPrefMgr = remember { RankPreferenceManager(context) }
            val displayedRank by rankPrefMgr.displayedRank.collectAsState(initial = null)
            val viewModel = com.darkxvenom.airbeats.ui.utils.safeHiltViewModel<StatsViewModel>()
            val currentRank by (viewModel?.currentRank ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
            val totalHours by (viewModel?.totalListenHours ?: kotlinx.coroutines.flow.flowOf(0.0)).collectAsState(initial = 0.0)
            val coroutineScope = rememberCoroutineScope()

            currentRank?.let { rank ->
                Spacer(modifier = Modifier.width(12.dp))
                var showBadgeSelector by remember { mutableStateOf(false) }
                RankBadge(
                    rank = rank,
                    displayedRank = displayedRank,
                    size = 28.dp,
                    modifier = Modifier.clickable { showBadgeSelector = true }
                )
                if (showBadgeSelector) {
                    val unlocked = unlockedRanksFromHours(totalHours)
                    BadgeSelector(
                        unlockedRanks = unlocked,
                        currentDisplayed = displayedRank,
                        onSelect = { selectedRank ->
                            coroutineScope.launch {
                                rankPrefMgr.saveDisplayedRank(selectedRank)
                            }
                            showBadgeSelector = false
                        },
                        onDismiss = { showBadgeSelector = false }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedBeatsRing(
    isPlaying: Boolean,
    songColors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) return

    val infiniteTransition = rememberInfiniteTransition(label = "beats_anim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val barAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_height"
    )

    Canvas(
        modifier = modifier
            .size(72.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val radius = size.minDimension / 2
        val barCount = 40
        val angleStep = 360f / barCount

        for (i in 0 until barCount) {

            val angle = Math.toRadians((i * angleStep).toDouble())
            val dynamicHeight = radius * barAnim * (0.5f + (i % 5) * 0.1f)

            val startX = center.x + (radius - 12f) * cos(angle).toFloat()
            val startY = center.y + (radius - 12f) * sin(angle).toFloat()

            val endX = center.x + (radius - 12f + dynamicHeight * 0.25f) * cos(angle).toFloat()
            val endY = center.y + (radius - 12f + dynamicHeight * 0.25f) * sin(angle).toFloat()

            drawLine(
                brush = Brush.linearGradient(songColors),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun GlassHomeTagsRow(
    navController: NavController,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassBg = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val glassBorder = if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val contentColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    val haptic = LocalHapticFeedback.current

    val tags = remember(isLoggedIn) {
        listOfNotNull(
            Triple("history", R.string.history, R.drawable.history),
            Triple("stats", R.string.stats, R.drawable.trending_up),
            Triple("liked", R.string.liked, R.drawable.favorite),
            Triple("downloads", R.string.offline, R.drawable.download),
            if (isLoggedIn) Triple("account", R.string.account, R.drawable.person) else null
        )
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(tags, key = { it.first }) { (id, stringRes, iconRes) ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.93f else 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "glassTagScale"
            )

            Box(
                modifier = Modifier
                    .scale(pressScale)
                    .clip(CircleShape)
                    .background(glassBg)
                    .border(BorderStroke(1.dp, glassBorder), CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (id) {
                                "history" -> navController.navigate("history")
                                "stats" -> navController.navigate("stats")
                                "liked" -> navController.navigate("auto_playlist/liked")
                                "downloads" -> navController.navigate("auto_playlist/downloaded")
                                "account" -> if (isLoggedIn) navController.navigate("account")
                            }
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = contentColor.copy(alpha = 0.90f)
                    )
                    Text(
                        text = stringResource(stringRes),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = contentColor
                    )
                }
            }
        }
    }
}


