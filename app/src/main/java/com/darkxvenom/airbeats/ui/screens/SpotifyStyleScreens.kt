package com.darkxvenom.airbeats.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.launch
import com.valentinilk.shimmer.shimmer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.darkxvenom.airbeats.ui.component.drawBackdropCustomShape
import com.darkxvenom.airbeats.ui.component.layerBackdrop
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.entities.LocalItem
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.ChipsRow
import com.darkxvenom.airbeats.ui.component.NavigationTitle
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import com.darkxvenom.airbeats.viewmodels.MoodAndGenresViewModel
import java.net.URLEncoder
import com.darkxvenom.airbeats.ui.screens.settings.DarkMode
import com.darkxvenom.airbeats.constants.DarkModeKey
import com.darkxvenom.airbeats.utils.rememberEnumPreference

@Composable
fun isAppInDarkTheme(): Boolean {
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    return androidx.compose.runtime.remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
}

private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyBg @Composable get() = if (isAppInDarkTheme()) Color(0xFF050505) else Color(0xFFF9F9F9)
private val SpotifyCard @Composable get() = if (isAppInDarkTheme()) Color(0xFF181818) else Color(0xFFFFFFFF)
private val SpotifyPill @Composable get() = if (isAppInDarkTheme()) Color(0xFF2A2A2A) else Color(0xFFE5E5E5)
private val SpotifyText @Composable get() = if (isAppInDarkTheme()) Color.White else Color.Black

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi::class)
@Composable
fun SpotifyHomeScreen(
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val namePrefMgr = androidx.compose.runtime.remember { com.darkxvenom.airbeats.ui.component.NamePreferenceManager(context) }
    val rawUserName by namePrefMgr.userName.collectAsState(initial = "")
    val accountName = rawUserName.takeIf { it.isNotBlank() } ?: "Guest"
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val hazeState = androidx.compose.runtime.remember { dev.chrisbanes.haze.HazeState() }

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBg)
                .haze(state = hazeState)
        ) {
            SimpMusicMeshBackground()

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                ),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 145.dp,
                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                    start = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {

                quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                    item {
                        NavigationTitle(title = "Quick picks")
                        SpotifyLocalRow(picks.take(12), playerConnection)
                    }
                }

                keepListening?.filterIsInstance<Song>()?.takeIf { it.isNotEmpty() }?.let { items ->
                    item {
                        NavigationTitle(title = "Keep listening")
                        SpotifyLocalRow(items.take(12), playerConnection)
                    }
                }

                accountPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item {
                        NavigationTitle(title = "$accountName's playlists")
                        SpotifyYtRow(playlists.take(12), navController, playerConnection)
                    }
                }

                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                    item {
                        NavigationTitle(title = "Forgotten favorites")
                        SpotifyLocalRow(favorites.take(12), playerConnection)
                    }
                }

                similarRecommendations?.forEach { recommendation ->
                    item {
                        NavigationTitle(title = "Similar to ${recommendation.title.title}")
                        SpotifyYtRow(recommendation.items.take(12), navController, playerConnection)
                    }
                }

                homePage?.sections?.forEach { section ->
                    item {
                        NavigationTitle(
                            title = section.title,
                            label = section.label,
                            onClick = section.endpoint?.let {
                                {
                                    navController.navigate(
                                        "youtube_browse/${it.browseId}?params=${it.params.orEmpty()}",
                                    )
                                }
                            },
                        )
                        SpotifyYtRow(section.items.take(12), navController, playerConnection)
                    }
                }

                if (isLoading || homePage == null) {
                    item { SpotifyHomeLoadingShimmer() }
                }
            }

            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                containerColor = Color(0xFFEAEAEA),
                color = Color(0xFF333333),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 145.dp),
            )
        } // Close BoxWithConstraints
        } // Close hazeSource Box

        val enableLiquidGlass by com.darkxvenom.airbeats.utils.rememberPreference(
            com.darkxvenom.airbeats.constants.LiquidGlassKey,
            defaultValue = false
        )
        val backdrop = com.darkxvenom.airbeats.ui.component.LocalBackdrop.current

        val isAtTop by androidx.compose.runtime.remember {
            androidx.compose.runtime.derivedStateOf {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            }
        }

        SpotifyHeader(
            title = "AirBeats",
            subtitle = greeting(accountName),
            isAtTop = isAtTop,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.TopCenter),
            bottomContent = {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val chipItems = listOf(
                        "history" to "History",
                        "liked" to "Liked",
                        "offline" to "Offline",
                        "stats" to "Stats",
                        "search" to "Search"
                    )
                    items(chipItems) { (route, label) ->
                        androidx.compose.material3.ElevatedFilterChip(
                            selected = false,
                            onClick = {
                                when (route) {
                                    "history" -> navController.navigate("history")
                                    "liked" -> navController.navigate("auto_playlist/liked")
                                    "offline" -> navController.navigate("auto_playlist/downloaded")
                                    "stats" -> navController.navigate("stats")
                                    "search" -> navController.navigate(Screens.Search.route)
                                }
                            },
                            label = { Text(label, maxLines = 1) },
                            shape = CircleShape,
                            colors = androidx.compose.material3.FilterChipDefaults.elevatedFilterChipColors(
                                containerColor = Color.Transparent,
                                labelColor = Color.LightGray,
                            ),
                            border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = Color.Gray.copy(alpha = 0.8f),
                            ),
                        )
                    }
                }
            }
        ) {
            androidx.compose.material3.IconButton(onClick = { navController.navigate("new_release") }) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.notification_on), contentDescription = null, tint = SpotifyText, modifier = Modifier.size(24.dp))
            }
            androidx.compose.material3.IconButton(onClick = { navController.navigate("history") }) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.history), contentDescription = null, tint = SpotifyText, modifier = Modifier.size(24.dp))
            }
            androidx.compose.material3.IconButton(onClick = { navController.navigate("settings") }) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.settings), contentDescription = null, tint = SpotifyText, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun SpotifySearchScreen(
    navController: NavController,
    viewModel: com.darkxvenom.airbeats.viewmodels.OnlineSearchSuggestionViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val viewState by viewModel.viewState.collectAsState()
    val database = com.darkxvenom.airbeats.LocalDatabase.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    SpotifyScaffold(
        title = "Search",
        subtitle = "Find your favorite music",
        actions = {}
    ) {
        item {
            SpotifySearchInput(
                query = query,
                onQueryChange = { viewModel.query.value = it },
                onSearch = { q ->
                    val encoded = URLEncoder.encode(q, "UTF-8")
                    navController.navigate("search/$encoded")
                },
                onMicClick = {
                    navController.navigate(com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionRoute)
                }
            )
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
            item {
                SpotifySectionTitle("Browse all")
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
                    "Dance/Electronic" to Color(0xFFD84000),
                    "Mood" to Color(0xFFE1118C),
                    "Indie" to Color(0xFFE91429),
                    "Workout" to Color(0xFF777777),
                    "K-pop" to Color(0xFF148A08),
                    "Chill" to Color(0xFFD84000),
                    "Sleep" to Color(0xFF1E3264),
                    "Party" to Color(0xFF537AA1),
                    "At Home" to Color(0xFF5179A1),
                    "Decades" to Color(0xFFBA5D07)
                )
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 16.dp)) {
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
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun SpotifyExploreScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val moodAndGenres by viewModel.moodAndGenres.collectAsState()
    val explorePage by homeViewModel.explorePage.collectAsState()
    val playerConnection = LocalPlayerConnection.current ?: return

    SpotifyScaffold(
        title = "Explore",
        subtitle = "Fresh music and moods",
        actions = {
            androidx.compose.material3.IconButton(onClick = { navController.navigate(Screens.Search.route) }) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.search), contentDescription = null, tint = SpotifyText, modifier = Modifier.size(24.dp))
            }
        }
    ) {
        item {
            SpotifySectionTitle("New releases")
            SpotifyYtRow(explorePage?.newReleaseAlbums.orEmpty(), navController, playerConnection)
        }
        moodAndGenres?.forEach { group ->
            item {
                SpotifySectionTitle(group.title)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val localConfiguration = androidx.compose.ui.platform.LocalConfiguration.current
                    val itemsPerRow = if (localConfiguration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
                    group.items.chunked(itemsPerRow).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { item ->
                                SpotifyBrowseTile(
                                    text = item.title,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                                    },
                                )
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
fun SpotifyLibraryScreen(navController: NavController) {
    var filterType by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.darkxvenom.airbeats.constants.LibraryFilter.PLAYLISTS) }

    SpotifyScaffold(
        title = "Your Library",
        subtitle = "Saved music in AirBeats",
        actions = {
            androidx.compose.material3.IconButton(onClick = { navController.navigate(Screens.Search.route) }) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.search), contentDescription = null, tint = SpotifyText, modifier = Modifier.size(24.dp))
            }
            androidx.compose.material3.IconButton(onClick = { navController.navigate("settings") }) {
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.settings), contentDescription = null, tint = SpotifyText, modifier = Modifier.size(24.dp))
            }
        }
    ) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                item {
                    SpotifyChip(text = "Playlists", isSelected = filterType == com.darkxvenom.airbeats.constants.LibraryFilter.PLAYLISTS) {
                        filterType = com.darkxvenom.airbeats.constants.LibraryFilter.PLAYLISTS
                    }
                }
                item {
                    SpotifyChip(text = "Songs", isSelected = filterType == com.darkxvenom.airbeats.constants.LibraryFilter.SONGS) {
                        filterType = com.darkxvenom.airbeats.constants.LibraryFilter.SONGS
                    }
                }
                item {
                    SpotifyChip(text = "Albums", isSelected = filterType == com.darkxvenom.airbeats.constants.LibraryFilter.ALBUMS) {
                        filterType = com.darkxvenom.airbeats.constants.LibraryFilter.ALBUMS
                    }
                }
                item {
                    SpotifyChip(text = "Artists", isSelected = filterType == com.darkxvenom.airbeats.constants.LibraryFilter.ARTISTS) {
                        filterType = com.darkxvenom.airbeats.constants.LibraryFilter.ARTISTS
                    }
                }
                item {
                    SpotifyChip(text = "Local Files", isSelected = filterType == com.darkxvenom.airbeats.constants.LibraryFilter.LOCAL) {
                        filterType = com.darkxvenom.airbeats.constants.LibraryFilter.LOCAL
                    }
                }
                item {
                    SpotifyChip(text = "History", isSelected = false) {
                        navController.navigate("history")
                    }
                }
            }
        }
        item {
            val insets = com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets.current
            val density = androidx.compose.ui.platform.LocalDensity.current
            val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
            val bottom = insets.getBottom(density)
            val left = insets.getLeft(density, layoutDirection)
            val right = insets.getRight(density, layoutDirection)
            val customInsets = androidx.compose.foundation.layout.WindowInsets(left, 0, right, bottom)
            
            androidx.compose.runtime.CompositionLocalProvider(
                com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets provides customInsets
            ) {
                Box(Modifier.fillParentMaxSize()) {
                    when (filterType) {
                        com.darkxvenom.airbeats.constants.LibraryFilter.PLAYLISTS ->
                            com.darkxvenom.airbeats.ui.screens.library.LibraryPlaylistsScreen(navController = navController, filterContent = {}, onLocalClick = { filterType = com.darkxvenom.airbeats.constants.LibraryFilter.LOCAL })
                        com.darkxvenom.airbeats.constants.LibraryFilter.SONGS ->
                            com.darkxvenom.airbeats.ui.screens.library.LibrarySongsScreen(navController = navController, onDeselect = { filterType = com.darkxvenom.airbeats.constants.LibraryFilter.PLAYLISTS })
                        com.darkxvenom.airbeats.constants.LibraryFilter.ALBUMS ->
                            com.darkxvenom.airbeats.ui.screens.library.LibraryAlbumsScreen(navController = navController, onDeselect = { filterType = com.darkxvenom.airbeats.constants.LibraryFilter.PLAYLISTS })
                        com.darkxvenom.airbeats.constants.LibraryFilter.ARTISTS ->
                            com.darkxvenom.airbeats.ui.screens.library.LibraryArtistsScreen(navController = navController, onDeselect = { filterType = com.darkxvenom.airbeats.constants.LibraryFilter.PLAYLISTS })
                        com.darkxvenom.airbeats.constants.LibraryFilter.LOCAL ->
                            com.darkxvenom.airbeats.ui.screens.library.LocalSongsScreen(navController = navController)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotifyScaffold(
    title: String,
    subtitle: String,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val hazeState = androidx.compose.runtime.remember { dev.chrisbanes.haze.HazeState() }
    
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBg)
                .haze(state = hazeState)
        ) {
            SimpMusicMeshBackground()
            LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + 90.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                start = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                end = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
        } // Close inner Box for hazeSource
        val isAtTop by androidx.compose.runtime.remember {
            androidx.compose.runtime.derivedStateOf {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            }
        }
        SpotifyHeader(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.align(Alignment.TopCenter),
            isAtTop = isAtTop,
            actions = actions
        )
    }
}

@Composable
private fun SimpMusicMeshBackground() {
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = SpotifyBg

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize(0.7f)
            .drawWithCache {
                val width = size.width
                val height = size.height

                val brush1 = Brush.radialGradient(
                    colors = listOf(color1.copy(alpha = 0.38f), color1.copy(alpha = 0.24f), color1.copy(alpha = 0.14f), color1.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(width * 0.15f, height * 0.1f),
                    radius = width * 0.55f,
                )
                val brush2 = Brush.radialGradient(
                    colors = listOf(color2.copy(alpha = 0.34f), color2.copy(alpha = 0.2f), color2.copy(alpha = 0.11f), color2.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(width * 0.85f, height * 0.2f),
                    radius = width * 0.65f,
                )
                val brush3 = Brush.radialGradient(
                    colors = listOf(color3.copy(alpha = 0.3f), color3.copy(alpha = 0.17f), color3.copy(alpha = 0.09f), color3.copy(alpha = 0.04f), Color.Transparent),
                    center = Offset(width * 0.3f, height * 0.45f),
                    radius = width * 0.6f,
                )
                val brush4 = Brush.radialGradient(
                    colors = listOf(color4.copy(alpha = 0.26f), color4.copy(alpha = 0.14f), color4.copy(alpha = 0.08f), color4.copy(alpha = 0.03f), Color.Transparent),
                    center = Offset(width * 0.7f, height * 0.5f),
                    radius = width * 0.7f,
                )
                val brush5 = Brush.radialGradient(
                    colors = listOf(color5.copy(alpha = 0.22f), color5.copy(alpha = 0.12f), color5.copy(alpha = 0.06f), color5.copy(alpha = 0.02f), Color.Transparent),
                    center = Offset(width * 0.5f, height * 0.75f),
                    radius = width * 0.8f,
                )
                val overlayBrush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(alpha = 0.22f), surfaceColor.copy(alpha = 0.55f), surfaceColor),
                    startY = height * 0.4f,
                    endY = height,
                )

                onDrawBehind {
                    drawRect(brush1)
                    drawRect(brush2)
                    drawRect(brush3)
                    drawRect(brush4)
                    drawRect(brush5)
                    drawRect(overlayBrush)
                }
            },
    )
}

@Composable
private fun SpotifyHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isAtTop: Boolean = true,
    hazeState: dev.chrisbanes.haze.HazeState? = null,
    bottomContent: @Composable () -> Unit = {},
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}
) {
    Box(modifier = modifier.fillMaxWidth()) {
        androidx.compose.animation.AnimatedContent(
            targetState = isAtTop,
            transitionSpec = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)).togetherWith(androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300)))
            },
            modifier = Modifier.matchParentSize()
        ) { isAtTop ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isAtTop) {
                            Modifier.background(Color.Transparent)
                        } else if (hazeState != null) {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = dev.chrisbanes.haze.materials.HazeMaterials.ultraThin()
                            )
                        } else {
                            Modifier.background(SpotifyBg.copy(alpha = 0.95f))
                        }
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    androidx.compose.material3.Text(title, color = SpotifyText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    if (subtitle.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var showNameDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val namePrefMgr = androidx.compose.runtime.remember { com.darkxvenom.airbeats.ui.component.NamePreferenceManager(context) }
                            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

                            androidx.compose.material3.Text(
                                text = subtitle, 
                                color = SpotifyText.copy(alpha = 0.7f), 
                                fontSize = 14.sp, 
                                maxLines = 1,
                                modifier = if (subtitle.startsWith("Good ")) Modifier.clickable { showNameDialog = true } else Modifier
                            )
                            
                            if (showNameDialog) {
                                com.darkxvenom.airbeats.ui.component.NameSetupDialog(
                                    onNameConfirmed = { newName ->
                                        if (newName.isNotBlank()) {
                                            coroutineScope.launch {
                                                namePrefMgr.saveUserName(newName)
                                                try {
                                                    com.darkxvenom.airbeats.utils.AirBeatsStatsCloudSync.syncDaily(
                                                        context = com.darkxvenom.airbeats.App.instance,
                                                        database = com.darkxvenom.airbeats.App.instance.database,
                                                        namePreferenceManager = namePrefMgr,
                                                    )?.onFailure {
                                                        timber.log.Timber.e(it, "Failed to sync stats after name confirmation")
                                                    }
                                                } catch (e: Exception) {
                                                    timber.log.Timber.e(e, "Exception syncing stats after name confirmation")
                                                }
                                            }
                                        }
                                        showNameDialog = false
                                    }
                                )
                            }
                            if (subtitle.startsWith("Good ")) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val statsViewModel = androidx.hilt.navigation.compose.hiltViewModel<com.darkxvenom.airbeats.viewmodels.StatsViewModel>()
                                val totalHours by statsViewModel.totalListenHours.collectAsState(initial = 0.0)
                                val currentRank = androidx.compose.runtime.remember(totalHours) {
                                    if (totalHours >= 1.0) com.darkxvenom.airbeats.ui.component.AirBeatsRank.fromHours(totalHours.toInt()) else null
                                }
                                val rankPrefMgr = androidx.compose.runtime.remember { com.darkxvenom.airbeats.ui.component.RankPreferenceManager(context) }
                                val displayedRank by rankPrefMgr.displayedRank.collectAsState(initial = null)

                                currentRank?.let { rank ->
                                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                                    var showBadgeSelector by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    com.darkxvenom.airbeats.ui.component.RankBadge(
                                        rank = rank, 
                                        displayedRank = displayedRank, 
                                        size = 16.dp,
                                        modifier = Modifier.clickable { showBadgeSelector = true }
                                    )
                                    if (showBadgeSelector) {
                                        val unlocked = com.darkxvenom.airbeats.ui.component.unlockedRanksFromHours(totalHours)
                                        com.darkxvenom.airbeats.ui.component.BadgeSelector(
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
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = actions
                )
            }
            bottomContent()
        }
    }
}

@Composable
private fun SpotifySearchPill(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.search), null, tint = Color.Black, modifier = Modifier.size(24.dp))
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SpotifySearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onMicClick: () -> Unit
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    androidx.compose.material3.TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { 
            Text(
                text = "What do you want to listen to?", 
                color = Color.Black.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            ) 
        },
        leadingIcon = { Icon(painterResource(R.drawable.search), contentDescription = null, tint = Color.Black) },
        trailingIcon = {
            androidx.compose.material3.IconButton(onClick = onMicClick) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = "Music Recognition",
                    tint = Color.Black
                )
            }
        },
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = SpotifyGreen
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = {
                if (query.isNotBlank()) {
                    onSearch(query.trim())
                    keyboardController?.hide()
                }
            }
        ),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun SpotifySectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun SpotifyLocalRow(items: List<Song>, playerConnection: com.darkxvenom.airbeats.playback.PlayerConnection) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { song ->
            SpotifyCoverCard(
                title = song.song.title,
                subtitle = song.artists.joinToString { it.name },
                thumbnail = song.song.thumbnailUrl,
                onClick = { playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata())) },
            )
        }
    }
}

@Composable
private fun SpotifyYtRow(items: List<YTItem>, navController: NavController, playerConnection: com.darkxvenom.airbeats.playback.PlayerConnection) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { item ->
            SpotifyCoverCard(
                title = item.title,
                subtitle = when (item) {
                    is SongItem -> item.artists.joinToString { it.name }
                    is AlbumItem -> item.artists.orEmpty().joinToString { it.name }
                    is PlaylistItem -> item.author?.name.orEmpty()
                    is ArtistItem -> "Artist"
                },
                thumbnail = item.thumbnail,
                onClick = {
                    when (item) {
                        is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id)))
                        is AlbumItem -> navController.navigate("album/${item.browseId}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        is ArtistItem -> navController.navigate("artist/${item.id}")
                    }
                },
            )
        }
    }
}

@Composable
private fun SpotifyCoverCard(title: String, subtitle: String, thumbnail: String?, onClick: () -> Unit) {
    Column(modifier = Modifier.width(142.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = thumbnail?.highQualityThumbnail(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(142.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SpotifyCard),
        )
        Text(title, color = SpotifyText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(subtitle, color = SpotifyText.copy(alpha = 0.58f), fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SpotifyChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(if (isSelected) SpotifyGreen else SpotifyText.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else SpotifyText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SpotifyBrowseTile(
    text: String, 
    modifier: Modifier = Modifier, 
    @androidx.annotation.DrawableRes icon: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SpotifyText.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(id = icon),
                contentDescription = null,
                tint = SpotifyText.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
            )
        }
        Text(
            text = text, 
            color = SpotifyText, 
            fontWeight = FontWeight.ExtraBold, 
            maxLines = 2,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun SpotifyHomeLoadingShimmer() {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        SpotifyQuickPicksShimmer()
        repeat(5) {
            SpotifyHomeItemShimmer()
        }
    }
}

@Composable
private fun SpotifyQuickPicksShimmer() {
    Column {
        Box(
            Modifier
                .width(150.dp)
                .height(36.dp)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SpotifyText.copy(alpha = 0.08f))
                .shimmer(),
        )
        Column {
            repeat(4) {
                SpotifyQuickPicksShimmerItem()
            }
        }
    }
}

@Composable
private fun SpotifyQuickPicksShimmerItem() {
    Row(
        Modifier
            .height(70.dp)
            .padding(vertical = 10.dp),
    ) {
        Box(
            Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SpotifyText.copy(alpha = 0.08f))
                .shimmer(),
        )
        Column(
            Modifier
                .padding(start = 10.dp)
                .wrapContentHeight(align = Alignment.CenterVertically)
                .align(Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .width(300.dp)
                    .height(21.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpotifyText.copy(alpha = 0.08f))
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                Modifier
                    .width(260.dp)
                    .height(21.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpotifyText.copy(alpha = 0.08f))
                    .shimmer(),
            )
        }
    }
}

@Composable
private fun SpotifyHomeItemShimmer() {
    Column {
        Box(
            Modifier
                .width(150.dp)
                .height(36.dp)
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SpotifyText.copy(alpha = 0.08f))
                .shimmer(),
        )
        LazyRow(userScrollEnabled = false, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(10) {
                SpotifyPlaylistShimmer()
            }
        }
    }
}

@Composable
private fun SpotifyPlaylistShimmer() {
    Column(
        Modifier
            .height(270.dp)
            .padding(bottom = 10.dp),
    ) {
        Box(
            Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SpotifyText.copy(alpha = 0.08f))
                .shimmer(),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Box(
            Modifier
                .width(130.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SpotifyText.copy(alpha = 0.08f))
                .shimmer(),
        )
        Spacer(modifier = Modifier.size(10.dp))
        Box(
            Modifier
                .width(130.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SpotifyText.copy(alpha = 0.08f))
                .shimmer(),
        )
    }
}

private fun greeting(name: String): String {
    val cleanName = name.takeIf { it.isNotBlank() } ?: "there"
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greetingText = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    return "$greetingText $cleanName"
}
