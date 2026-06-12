package com.darkxvenom.airbeats.ui.screens.apple

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = AppleBg

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
fun AppleHeader(
    title: String,
    modifier: Modifier = Modifier,
    isAtTop: Boolean = true,
    hazeState: HazeState? = null,
    profileUrl: String? = null,
    onProfileClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager
        .getAvatarSelection
        .collectAsState(initial = AvatarSelection.Default)

    Box(modifier = modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = isAtTop,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
            modifier = Modifier.matchParentSize(),
            label = "AppleHeaderBackground"
        ) { isAtTopState ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isAtTopState) {
                            Modifier.background(Color.Transparent)
                        } else if (hazeState != null) {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = dev.chrisbanes.haze.materials.HazeMaterials.ultraThin()
                            )
                        } else {
                            Modifier.background(AppleBg.copy(alpha = 0.95f))
                        }
                    )
            )
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
            Text(title, color = AppleText, fontSize = 28.sp, fontWeight = FontWeight.Black)

            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
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

@Composable
fun AppleScaffold(
    title: String,
    navController: NavController,
    profileUrl: String? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    val lazyListState = rememberLazyListState()
    val hazeState = remember { HazeState() }
    
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppleBg)
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
            onProfileClick = { navController.navigate("settings") }
        )
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
                thumbnailUrl = item.thumbnail.highQualityThumbnail(),
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

@Composable
fun AppleHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by com.darkxvenom.airbeats.utils.rememberPreference(com.darkxvenom.airbeats.constants.InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in com.darkxvenom.airbeats.innertube.utils.parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null
    
    AppleScaffold(
        title = "Listen Now",
        navController = navController,
        profileUrl = url
    ) {
        quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
            item {
                AppleSectionTitle("Made for You")
                AppleLocalRow(picks.take(12), playerConnection)
            }
        }

        accountPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
            item {
                AppleSectionTitle("Your Playlists")
                AppleYtRow(playlists.take(12), navController, playerConnection)
            }
        }

        forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
            item {
                AppleSectionTitle("Forgotten Favorites")
                AppleLocalRow(favorites.take(12), playerConnection)
            }
        }

        similarRecommendations?.forEach { recommendation ->
            item {
                AppleSectionTitle("Similar to ${recommendation.title.title}")
                AppleYtRow(recommendation.items.take(12), navController, playerConnection)
            }
        }

        homePage?.sections?.forEach { section ->
            item {
                AppleSectionTitle(section.title)
                AppleYtRow(section.items.take(12), navController, playerConnection)
            }
        }
    }
}

@Composable
fun AppleExploreScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val moodAndGenres by viewModel.moodAndGenres.collectAsState()
    val explorePage by homeViewModel.explorePage.collectAsState()
    val playerConnection = LocalPlayerConnection.current ?: return

    AppleScaffold(
        title = "Browse",
        navController = navController
    ) {
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
    var filterType by remember { mutableStateOf(LibraryFilter.PLAYLISTS) }

    AppleScaffold(
        title = "Library",
        navController = navController
    ) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                val filters = listOf(
                    "Playlists" to LibraryFilter.PLAYLISTS,
                    "Songs" to LibraryFilter.SONGS,
                    "Albums" to LibraryFilter.ALBUMS,
                    "Artists" to LibraryFilter.ARTISTS,
                    "Local Files" to LibraryFilter.LOCAL
                )
                items(filters) { (label, filter) ->
                    val isSelected = filterType == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AppleRed else AppleBg.copy(alpha = 0.3f))
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
                placeholder = { Text("Artists, Songs, Lyrics, and More", color = AppleText.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(painterResource(R.drawable.search), contentDescription = null, tint = AppleText.copy(alpha=0.5f)) },
                trailingIcon = {
                    IconButton(onClick = { navController.navigate(com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionRoute) }) {
                        Icon(
                            painter = painterResource(R.drawable.mic),
                            contentDescription = "Music Recognition",
                            tint = AppleText.copy(alpha = 0.5f)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = AppleBg.copy(alpha = 0.3f),
                    unfocusedContainerColor = AppleBg.copy(alpha = 0.3f),
                    focusedTextColor = AppleText,
                    unfocusedTextColor = AppleText
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
                        Text("Leaderboard", color = AppleText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Top Users: ${globalStats.board.users.size}", color = AppleText.copy(alpha = 0.7f), fontSize = 14.sp)
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
                        Text("Top listener", color = AppleText.copy(alpha = 0.7f), fontSize = 12.sp)
                        val topHours = topUser?.totalListenMs?.let { it / (3600.0 * 1000.0) }?.toInt()
                        Text(if (topUser != null) "${topHours}h" else "--", color = AppleText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(AppleBg.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("Your rank", color = AppleText.copy(alpha = 0.7f), fontSize = 12.sp)
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
                            coil.compose.AsyncImage(
                                model = user.profileUrl ?: R.drawable.person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray.copy(alpha=0.3f)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
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
