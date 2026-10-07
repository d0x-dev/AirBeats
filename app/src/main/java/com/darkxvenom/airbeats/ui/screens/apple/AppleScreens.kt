package com.darkxvenom.airbeats.ui.screens.apple

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.darkxvenom.airbeats.ui.component.AvatarDisplay
import com.darkxvenom.airbeats.ui.screens.search.airbeatsChartsItems
import com.darkxvenom.airbeats.ui.screens.search.recentSearchesItems
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.core.net.toUri
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.DarkModeKey
import com.darkxvenom.airbeats.constants.LibraryFilter
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.screens.library.LibraryAlbumsScreen
import com.darkxvenom.airbeats.ui.screens.library.LibraryArtistsScreen
import com.darkxvenom.airbeats.ui.screens.library.LibraryPlaylistsScreen
import com.darkxvenom.airbeats.ui.screens.library.LibrarySongsScreen
import com.darkxvenom.airbeats.ui.screens.library.LocalSongsScreen
import com.darkxvenom.airbeats.constants.HiddenHomeSectionsKey
import com.darkxvenom.airbeats.constants.MaterialHomeSection
import com.darkxvenom.airbeats.ui.component.HomeFloatingActions
import com.darkxvenom.airbeats.ui.component.HomeTasteStrip
import com.darkxvenom.airbeats.ui.component.UniversalHomeHeroBanner
import com.darkxvenom.airbeats.ui.component.UniversalArtistSpotlightCard
import com.darkxvenom.airbeats.ui.component.UniversalTopArtistsRow
import com.darkxvenom.airbeats.ui.component.UniversalQuickAccessTiles
import com.darkxvenom.airbeats.ui.component.HomeThemeStyle
import com.darkxvenom.airbeats.ui.screens.settings.DarkMode
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import com.darkxvenom.airbeats.viewmodels.MoodAndGenresViewModel
import com.darkxvenom.airbeats.viewmodels.OnlineSearchSuggestionViewModel
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import java.net.URLEncoder

@Composable
fun isAppInDarkTheme(): Boolean {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    return remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
}

val AppleBg @Composable get() = if (isAppInDarkTheme()) Color(0xFF000000) else Color(0xFFF2F2F7)
val AppleText @Composable get() = if (isAppInDarkTheme()) Color.White else Color.Black
val AppleRed = Color(0xFFFA233B)

@Composable
fun AppleMeshBackground() {
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }

    com.darkxvenom.airbeats.ui.component.ScreenAdaptiveBackground(
        artworkUrl = mediaMetadata?.thumbnailUrl
    )
}

@Composable
fun AppleHeader(
    title: String,
    modifier: Modifier = Modifier,
    isAtTop: Boolean = true,
    hazeState: HazeState? = null,
    profileUrl: String? = null,
    onProfileClick: () -> Unit,
    onNewReleaseClick: (() -> Unit)? = null,
    onDeveloperNewsClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager
        .getAvatarSelection
        .collectAsState(initial = AvatarSelection.Default)
    val isDark = isAppInDarkTheme()

    val blurAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isAtTop) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "AppleHeaderBlurAlpha"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        if (hazeState != null) {
            com.darkxvenom.airbeats.ui.component.TopFadeBlur(
                hazeState = hazeState,
                pageColor = Color.Transparent,
                scrimColor = Color.Transparent,
                alpha = blurAlpha,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        } else {
            androidx.compose.animation.AnimatedVisibility(
                visible = !isAtTop,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.matchParentSize()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(AppleBg.copy(alpha = 0.95f)))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = AppleText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onDeveloperNewsClick != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.12f)
                                else Color.Black.copy(alpha = 0.06f)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onDeveloperNewsClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.newspaper),
                            contentDescription = "News from Developer",
                            tint = AppleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onNewReleaseClick != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.12f)
                                else Color.Black.copy(alpha = 0.06f)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = onNewReleaseClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.notification_on),
                            contentDescription = "New Releases",
                            tint = AppleText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onProfileClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (val selection = currentSelection) {
                        is AvatarSelection.Custom -> {
                            AsyncImage(
                                model = selection.uri.toUri(),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        is AvatarSelection.DiceBear -> {
                            AsyncImage(
                                model = selection.url,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            if (profileUrl != null) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(profileUrl)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .diskCacheKey(profileUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.person),
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleScaffold(
    title: String,
    navController: NavController,
    profileUrl: String? = null,
    isRefreshing: Boolean? = null,
    onRefresh: (() -> Unit)? = null,
    onNewReleaseClick: (() -> Unit)? = { navController.navigate("new_release") },
    onDeveloperNewsClick: (() -> Unit)? = { navController.navigate("settings/developer_news") },
    floatingActionButton: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val lazyListState = rememberLazyListState()
    val hazeState = remember { HazeState() }
    val pullRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppleBg)
                .let { modifier ->
                    if (isRefreshing != null && onRefresh != null) {
                        modifier.pullToRefresh(
                            state = pullRefreshState,
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh
                        )
                    } else modifier
                }
                .haze(state = hazeState)
        ) {
            AppleMeshBackground()
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 90.dp,
                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 40.dp,
                    start = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateStartPadding(LayoutDirection.Ltr),
                    end = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateEndPadding(LayoutDirection.Ltr)
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize(),
                content = content,
            )
            
            if (isRefreshing != null && onRefresh != null) {
                androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 90.dp),
                    isRefreshing = isRefreshing,
                    state = pullRefreshState
                )
            }
        } 
        val isAtTop by remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            }
        }
        AppleHeader(
            title = title,
            modifier = Modifier.align(Alignment.TopCenter),
            isAtTop = isAtTop,
            hazeState = hazeState,
            profileUrl = profileUrl,
            onProfileClick = { navController.navigate("settings") },
            onNewReleaseClick = onNewReleaseClick,
            onDeveloperNewsClick = onDeveloperNewsClick
        )

        floatingActionButton?.invoke(this)
    }
}

@Composable
fun AppleTile(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier
        .width(160.dp)
        .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            color = AppleText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                color = AppleText.copy(alpha = 0.6f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppleLocalRow(items: List<Song>, playerConnection: com.darkxvenom.airbeats.playback.PlayerConnection) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { song ->
            AppleTile(
                title = song.title,
                subtitle = song.artists.joinToString { it.name },
                thumbnailUrl = song.thumbnailUrl?.highQualityThumbnail(),
                onClick = {
                    playerConnection.playQueue(
                        com.darkxvenom.airbeats.playback.queues.ListQueue(
                            title = "Local",
                            items = items.map { it.toMediaMetadata().toMediaItem() },
                            startIndex = items.indexOf(song)
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun AppleYtRow(items: List<YTItem>, navController: NavController, playerConnection: com.darkxvenom.airbeats.playback.PlayerConnection) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            AppleTile(
                title = item.title,
                subtitle = when(item) {
                    is SongItem -> item.artists.joinToString { it.name }
                    is AlbumItem -> item.artists?.joinToString { it.name } ?: "Album"
                    is PlaylistItem -> item.author?.name ?: "Playlist"
                    is ArtistItem -> "Artist"
                    else -> ""
                },
                thumbnailUrl = item.thumbnail?.highQualityThumbnail() ?: "",
                onClick = {
                    when (item) {
                        is SongItem -> {
                            playerConnection.playQueue(YouTubeQueue(item.endpoint ?: return@AppleTile, item.toMediaMetadata()))
                        }
                        is AlbumItem -> {
                            navController.navigate("album/${item.id}")
                        }
                        is PlaylistItem -> {
                            navController.navigate("online_playlist/${item.id}")
                        }
                        is ArtistItem -> {
                            navController.navigate("artist/${item.id}")
                        }
                        else -> {}
                    }
                }
            )
        }
    }
}

@Composable
fun AppleSectionTitle(title: String) {
    Text(
        text = title,
        color = AppleText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppleHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by com.darkxvenom.airbeats.utils.rememberPreference(com.darkxvenom.airbeats.constants.InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in com.darkxvenom.airbeats.innertube.utils.parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val isContentEmpty = (quickPicks == null || quickPicks?.isEmpty() == true) &&
        (accountPlaylists == null || accountPlaylists?.isEmpty() == true) &&
        (forgottenFavorites == null || forgottenFavorites?.isEmpty() == true) &&
        similarRecommendations.isNullOrEmpty() &&
        homePage?.sections.isNullOrEmpty()

    val hiddenSections by com.darkxvenom.airbeats.utils.rememberPreference(HiddenHomeSectionsKey, defaultValue = emptySet())
    fun isSectionVisible(section: MaterialHomeSection): Boolean = section.id !in hiddenSections

    val freshPicks = remember(quickPicks) { quickPicks?.drop(6).orEmpty().take(12) }
    val artistTriples = remember(quickPicks) {
        quickPicks?.mapNotNull { pick ->
            pick.artists.firstOrNull()?.let { artist ->
                Triple(artist, artist.thumbnailUrl, pick.song.thumbnailUrl)
            }
        }?.distinctBy { it.first.id }?.take(10).orEmpty()
    }
    
    AppleScaffold(
        title = "Listen Now",
        navController = navController,
        profileUrl = url,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        floatingActionButton = {
            HomeFloatingActions(
                navController = navController
            )
        }
    ) {
        if (isLoading && isContentEmpty) {
            item(key = "apple_home_center_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(0.6f),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }

        if (isSectionVisible(MaterialHomeSection.HERO)) {
            item(key = "apple_hero") {
                UniversalHomeHeroBanner(
                    title = "Listen Now",
                    subtitle = "Curated radio tailored to your musical tastes",
                    onPlayRadio = {
                        com.darkxvenom.airbeats.ui.component.InfiniteRadioHelper.playShuffledRadio(
                            playerConnection = playerConnection,
                            currentSongId = playerConnection.mediaMetadata.value?.id,
                            quickPicks = quickPicks,
                            forgottenFavorites = forgottenFavorites,
                            keepListening = keepListening,
                            homeSongs = homePage?.sections?.flatMap { it.items }?.filterIsInstance<com.darkxvenom.airbeats.innertube.models.SongItem>()
                        )
                    },
                    style = HomeThemeStyle.APPLE,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        if (isSectionVisible(MaterialHomeSection.QUICK_TILES)) {
            item(key = "apple_quick_tiles") {
                UniversalQuickAccessTiles(
                    onLikedClick = { navController.navigate("auto_playlist/liked") },
                    onMixClick = {
                        quickPicks?.firstOrNull()?.let {
                            playerConnection.playQueue(YouTubeQueue.radio(it.toMediaMetadata()))
                        }
                    },
                    onHistoryClick = { navController.navigate("history") },
                    onStatsClick = { navController.navigate("stats") },
                    style = HomeThemeStyle.APPLE,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        if (isSectionVisible(MaterialHomeSection.TASTE_STRIP)) {
            item(key = "apple_taste_strip") {
                HomeTasteStrip(
                    onTagClick = { tag ->
                        val encoded = java.net.URLEncoder.encode(tag, "UTF-8")
                        navController.navigate("search/$encoded")
                    },
                    style = HomeThemeStyle.APPLE,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (isSectionVisible(MaterialHomeSection.QUICK_PICKS)) {
            quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                item {
                    AppleSectionTitle("Made for You")
                    AppleLocalRow(picks.take(12), playerConnection)
                }
            }
        }

        if (isSectionVisible(MaterialHomeSection.FRESH_FINDS)) {
            if (freshPicks.isNotEmpty()) {
                item(key = "apple_fresh_finds") {
                    AppleSectionTitle("Fresh Finds")
                    AppleLocalRow(freshPicks, playerConnection)
                }
            }
        }

        if (isSectionVisible(MaterialHomeSection.JUMP_BACK_IN)) {
            keepListening?.filterIsInstance<Song>()?.takeIf { it.isNotEmpty() }?.let { items ->
                item(key = "apple_jump_back_in") {
                    AppleSectionTitle("Recently Played")
                    AppleLocalRow(items.take(12), playerConnection)
                }
            }
        }

        if (isSectionVisible(MaterialHomeSection.MIXES)) {
            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                item {
                    AppleSectionTitle("Your Playlists")
                    AppleYtRow(playlists.take(12), navController, playerConnection)
                }
            }
        }

        if (isSectionVisible(MaterialHomeSection.HEAVY_ROTATION)) {
            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                item {
                    AppleSectionTitle("Forgotten Favorites")
                    AppleLocalRow(favorites.take(12), playerConnection)
                }
            }
        }

        if (isSectionVisible(MaterialHomeSection.SPOTLIGHT)) {
            quickPicks?.firstOrNull()?.let { pick ->
                val artist = pick.artists.firstOrNull()
                if (artist != null) {
                    item(key = "apple_spotlight") {
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
                            style = HomeThemeStyle.APPLE,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        if (isSectionVisible(MaterialHomeSection.TOP_ARTISTS)) {
            if (artistTriples.isNotEmpty()) {
                item(key = "apple_top_artists") {
                    AppleSectionTitle("Artists We Love")
                    UniversalTopArtistsRow(
                        artists = artistTriples,
                        onArtistClick = { artistId ->
                            artistId.takeIf { it.isNotBlank() }?.let { navController.navigate("artist/$it") }
                        },
                        style = HomeThemeStyle.APPLE
                    )
                }
            }
        }

        if (isSectionVisible(MaterialHomeSection.BECAUSE_YOU_LISTEN_TO)) {
            similarRecommendations?.forEach { recommendation ->
                item {
                    AppleSectionTitle("Similar to ${recommendation.title.title}")
                    AppleYtRow(recommendation.items.take(12), navController, playerConnection)
                }
            }
        }

        homePage?.sections?.forEach { section ->
            val isNewRelease = section.title.contains("New", ignoreCase = true) || section.title.contains("Release", ignoreCase = true)
            val isChart = section.title.contains("Chart", ignoreCase = true) || section.title.contains("Top", ignoreCase = true)
            val isAlbum = section.title.contains("Album", ignoreCase = true)

            val shouldRender = when {
                isNewRelease -> isSectionVisible(MaterialHomeSection.NEW_RELEASES)
                isChart -> isSectionVisible(MaterialHomeSection.CHARTS)
                isAlbum -> isSectionVisible(MaterialHomeSection.ALBUMS)
                else -> true
            }

            if (shouldRender) {
                item {
                    AppleSectionTitle(section.title)
                    AppleYtRow(section.items.take(12), navController, playerConnection)
                }
            }
        }

        if (isLoading && !isContentEmpty) {
            item(key = "apple_home_bottom_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppleExploreScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val moodAndGenres by viewModel.moodAndGenres.collectAsState()
    val explorePage by homeViewModel.explorePage.collectAsState()
    val playerConnection = LocalPlayerConnection.current ?: return

    val isExploringLoading = (explorePage == null || explorePage?.newReleaseAlbums.isNullOrEmpty()) && moodAndGenres.isNullOrEmpty()

    AppleScaffold(
        title = "Browse",
        navController = navController
    ) {
        if (isExploringLoading) {
            item(key = "apple_explore_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(0.6f),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            }
        }

        item {
            AppleSectionTitle("New releases")
            AppleYtRow(explorePage?.newReleaseAlbums.orEmpty(), navController, playerConnection)
        }
        
        moodAndGenres?.forEach { group ->
            item {
                AppleSectionTitle(group.title)
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp).padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val itemsPerRow = 2
                    group.items.chunked(itemsPerRow).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AppleBg.copy(alpha = 0.5f))
                                        .clickable {
                                            navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                                        }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = item.title,
                                        color = AppleText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            repeat(itemsPerRow - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppleLibraryScreen(navController: NavController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var filterType by remember { mutableStateOf(LibraryFilter.PLAYLISTS) }

    AppleScaffold(
        title = stringResource(R.string.library),
        navController = navController
    ) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val filters = listOf(
                    context.getString(R.string.playlists) to LibraryFilter.PLAYLISTS,
                    context.getString(R.string.songs) to LibraryFilter.SONGS,
                    context.getString(R.string.albums) to LibraryFilter.ALBUMS,
                    context.getString(R.string.artists) to LibraryFilter.ARTISTS,
                    context.getString(R.string.local_files) to LibraryFilter.LOCAL
                )
                items(filters) { (label, filter) ->
                    val isSelected = filterType == filter
                    val isFrosted = com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled()
                    val chipShape = RoundedCornerShape(8.dp)
                    Box(
                        modifier = Modifier
                            .clip(chipShape)
                            .then(
                                if (isFrosted) {
                                    Modifier.border(
                                        1.dp,
                                        if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                                        chipShape
                                    )
                                } else Modifier
                            )
                            .background(
                                if (isSelected) {
                                    AppleRed
                                } else {
                                    if (isFrosted) Color.White.copy(alpha = 0.08f) else AppleBg.copy(alpha = 0.3f)
                                }
                            )
                            .clickable { filterType = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else AppleText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        item {
            CompositionLocalProvider(
                LocalPlayerAwareWindowInsets provides WindowInsets(0, 0, 0, 0)
            ) {
                Box(Modifier.fillParentMaxSize()) {
                    when (filterType) {
                        LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController = navController, filterContent = {}, onLocalClick = { filterType = LibraryFilter.LOCAL })
                        LibraryFilter.SONGS -> LibrarySongsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.PLAYLISTS })
                        LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.PLAYLISTS })
                        LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.PLAYLISTS })
                        LibraryFilter.LOCAL -> LocalSongsScreen(navController = navController)
                        else -> {}
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleSearchScreen(
    navController: NavController,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val viewState by viewModel.viewState.collectAsState()
    val database = LocalDatabase.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    var selectedTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.darkxvenom.airbeats.ui.component.SearchTab.BROWSE_ALL) }
    val chartsViewModel: com.darkxvenom.airbeats.viewmodels.ChartsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val appleTextColor = AppleText
    val appleBgColor = AppleBg

    AppleScaffold(
        title = "Search",
        navController = navController
    ) {
        item {
            androidx.compose.material3.OutlinedTextField(
                value = query,
                onValueChange = { viewModel.query.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                placeholder = { Text(stringResource(R.string.search_everything_placeholder), color = appleTextColor.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(painterResource(R.drawable.search), contentDescription = null, tint = appleTextColor.copy(alpha=0.5f)) },
                trailingIcon = {
                    IconButton(onClick = { navController.navigate(com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionRoute) }) {
                        Icon(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = "Music Recognition",
                            tint = appleTextColor.copy(alpha = 0.5f)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = appleBgColor.copy(alpha = 0.3f),
                    unfocusedContainerColor = appleBgColor.copy(alpha = 0.3f),
                    focusedTextColor = appleTextColor,
                    unfocusedTextColor = appleTextColor
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = {
                        val encoded = URLEncoder.encode(query, "UTF-8")
                        navController.navigate("search/$encoded")
                        keyboardController?.hide()
                    }
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                )
            )
        }

        if (query.isBlank()) {
            item {
                com.darkxvenom.airbeats.ui.component.SearchPillSwitcher(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    containerColor = appleBgColor.copy(alpha = 0.35f),
                    selectedColor = AppleRed,
                    selectedTextColor = Color.White,
                    unselectedTextColor = appleTextColor.copy(alpha = 0.7f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
        
        if (query.isNotBlank() && (viewState.history.isNotEmpty() || viewState.suggestions.isNotEmpty())) {
            items(viewState.history, key = { "history_${it.query}" }) { history ->
                com.darkxvenom.airbeats.ui.screens.search.SuggestionItem(
                    query = history.query,
                    online = false,
                    onClick = {
                        val encoded = URLEncoder.encode(history.query, "UTF-8")
                        navController.navigate("search/$encoded")
                        keyboardController?.hide()
                    },
                    onDelete = {
                        database.query {
                            delete(history)
                        }
                    },
                    onFillTextField = {
                        viewModel.query.value = history.query
                    },
                    pureBlack = false
                )
            }
            items(viewState.suggestions, key = { "suggestion_$it" }) { suggestion ->
                com.darkxvenom.airbeats.ui.screens.search.SuggestionItem(
                    query = suggestion,
                    online = true,
                    onClick = {
                        val encoded = URLEncoder.encode(suggestion, "UTF-8")
                        navController.navigate("search/$encoded")
                        keyboardController?.hide()
                    },
                    onFillTextField = {
                        viewModel.query.value = suggestion
                    },
                    pureBlack = false
                )
            }
        } else {
            when (selectedTab) {
                com.darkxvenom.airbeats.ui.component.SearchTab.BROWSE_ALL -> {
                    item {
                        AppleSectionTitle("Browse Categories")
                        Spacer(modifier = Modifier.height(10.dp))
                        val genres = listOf(
                            "Pop" to Color(0xFFFF4632),
                            "Hip-Hop" to Color(0xFFBC5900),
                            "Rock" to Color(0xFFE1118C),
                            "Latin" to Color(0xFFE1118C),
                            "Educational" to Color(0xFF477D95),
                            "Documentary" to Color(0xFF509BF5),
                            "Comedy" to Color(0xFFE13300),
                            "Charts" to Color(0xFF8D67AB),
                            "Dance" to Color(0xFFD84000),
                            "Mood" to Color(0xFFE1118C),
                            "Indie" to Color(0xFFE91429),
                            "Workout" to Color(0xFF777777),
                            "K-pop" to Color(0xFF148A08),
                            "Chill" to Color(0xFFD84000),
                            "Sleep" to Color(0xFF1E3264),
                            "Party" to Color(0xFF537AA1),
                            "Decades" to Color(0xFFBA5D07)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 24.dp)) {
                            genres.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    row.forEach { (chip, color) ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(color)
                                                .clickable {
                                                    navController.navigate("search/${URLEncoder.encode(chip, "UTF-8")}")
                                                }
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = chip,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                com.darkxvenom.airbeats.ui.component.SearchTab.AIRBEATS_CHARTS -> {
                    airbeatsChartsItems(
                        navController = navController,
                        viewModel = chartsViewModel
                    )
                }
                com.darkxvenom.airbeats.ui.component.SearchTab.RECENT_SEARCHES -> {
                    recentSearchesItems(
                        onSearch = { queryText: String ->
                            val encoded = URLEncoder.encode(queryText, "UTF-8")
                            navController.navigate("search/$encoded")
                            keyboardController?.hide()
                        },
                        onFillQuery = { queryText: String -> viewModel.query.value = queryText },
                        itemTextColor = appleTextColor
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun AppleStatsScreen(
    navController: NavController,
    viewModel: com.darkxvenom.airbeats.viewmodels.StatsViewModel = hiltViewModel(),
) {
    val globalStats by viewModel.globalStats.collectAsState()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val playerConnection = LocalPlayerConnection.current ?: return

    AppleScaffold(
        title = "Stats",
        navController = navController
    ) {
        val topUser = globalStats.board.users.firstOrNull()
        val currentUser = globalStats.board.users.firstOrNull { it.id == globalStats.currentUserId }

        item {
            AppleSectionTitle("Global Rankings")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppleBg.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.leaderboard), color = AppleText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.top_users, globalStats.board.users.size), color = AppleText.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                    androidx.compose.material3.Button(
                        onClick = { viewModel.refreshGlobalStats() },
                        enabled = !globalStats.isLoading,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFFA233B), contentColor = Color.White)
                    ) {
                        Text(if (globalStats.isLoading) "Syncing" else "Refresh")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(AppleBg.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(stringResource(R.string.top_listener), color = AppleText.copy(alpha = 0.7f), fontSize = 12.sp)
                        val topHours = topUser?.totalListenMs?.let { it / (3600.0 * 1000.0) }?.toInt()
                        Text(if (topUser != null) "${topHours}h" else "--", color = AppleText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(AppleBg.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(stringResource(R.string.your_rank), color = AppleText.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(if (currentUser != null) "#${currentUser.rank}" else "--", color = AppleText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    items(globalStats.board.users, key = { it.id }) { user ->
                        val isCurrentUser = user.id == globalStats.currentUserId
                        val userHours = user.totalListenMs.toDouble() / (3600.0 * 1000.0)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isCurrentUser) Color(0xFFFA233B).copy(alpha = 0.2f) else AppleBg.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${user.rank}",
                                modifier = Modifier.width(36.dp),
                                color = AppleText,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            if (isCurrentUser) {
                                AvatarDisplay(size = 32.dp)
                            } else {
                                coil.compose.AsyncImage(
                                    model = user.profileUrl ?: R.drawable.person,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray.copy(alpha=0.3f)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = user.name,
                                color = AppleText,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                fontWeight = if (isCurrentUser) FontWeight.Black else FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${userHours.toInt()}h",
                                color = AppleText.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        if (mostPlayedSongsStats.isNotEmpty()) {
            item {
                AppleSectionTitle("Your Top Songs")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mostPlayedSongsStats.take(12).size) { index ->
                        val songStat = mostPlayedSongsStats[index]
                        val song = mostPlayedSongs.getOrNull(index)
                        AppleTile(
                            title = songStat.title,
                            subtitle = "${songStat.songCountListened} plays",
                            thumbnailUrl = songStat.thumbnailUrl?.highQualityThumbnail(),
                            onClick = {
                                if (song != null) {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            endpoint = com.darkxvenom.airbeats.innertube.models.WatchEndpoint(videoId = songStat.id),
                                            preloadItem = song.toMediaMetadata(),
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
        
        if (mostPlayedArtists.isNotEmpty()) {
            item {
                AppleSectionTitle("Your Top Artists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mostPlayedArtists.take(12)) { artist ->
                        AppleTile(
                            title = artist.artist.name,
                            subtitle = "${artist.songCount} plays",
                            thumbnailUrl = artist.artist.thumbnailUrl?.highQualityThumbnail(),
                            onClick = {
                                navController.navigate("artist/${artist.id}")
                            }
                        )
                    }
                }
            }
        }
        
        if (mostPlayedAlbums.isNotEmpty()) {
            item {
                AppleSectionTitle("Your Top Albums")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mostPlayedAlbums.take(12)) { album ->
                        AppleTile(
                            title = album.album.title,
                            subtitle = "${album.songCountListened} plays",
                            thumbnailUrl = album.album.thumbnailUrl?.highQualityThumbnail(),
                            onClick = {
                                navController.navigate("album/${album.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}
