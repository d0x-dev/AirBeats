package com.darkxvenom.airbeats.ui.screens.material

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.HiddenHomeSectionsKey
import com.darkxvenom.airbeats.constants.MaterialHomeSection
import com.darkxvenom.airbeats.ui.component.HomeFloatingActions
import com.darkxvenom.airbeats.constants.SongSortType
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.ui.component.TopFadeBlur
import com.darkxvenom.airbeats.ui.screens.Screens
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.utils.reportException
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val CardShape = RoundedCornerShape(20.dp)
private val MediaShape = RoundedCornerShape(14.dp)

private val TASTE_TAGS = listOf(
    "Pop", "Rock", "Hip-Hop", "Chill", "Electronic", "Indie",
    "Lo-Fi", "Bollywood", "Acoustic", "Jazz", "Metal", "Classical", "R&B"
)

/**
 * Material 3 Expressive Home Screen ported from the reference design.
 * Features dynamic greeting, personalized quick tiles, taste tags,
 * and 14 sections toggled via [HiddenHomeSectionsKey].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialHomeScreen(
    navController: NavController,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingMix by remember { mutableStateOf(false) }
    val namePrefMgr = remember { NamePreferenceManager(context) }
    val rawUserName by namePrefMgr.userName.collectAsState(initial = "")
    val displayName = rawUserName.takeIf { it.isNotBlank() } ?: stringResource(R.string.guest)

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val pullRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    val hazeState = remember { HazeState() }
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }
    val blurAlpha by animateFloatAsState(
        targetValue = if (isAtTop) 0f else 1f,
        animationSpec = tween(300),
        label = "MaterialHomeBlurAlpha"
    )
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerHeightDp = if (headerHeightPx > 0) with(density) { headerHeightPx.toDp() } else statusBarTop + 72.dp

    val hiddenSections by rememberPreference(HiddenHomeSectionsKey, defaultValue = emptySet())
    fun isSectionVisible(section: MaterialHomeSection): Boolean = section.id !in hiddenSections

    suspend fun generateAndPlayMix() {
        if (isGeneratingMix) return
        isGeneratingMix = true
        try {
            // 1. YouTube Home official personalized Mix / Supermix
            val ytMixPlaylist = homePage?.sections
                ?.flatMap { it.items }
                ?.filterIsInstance<PlaylistItem>()
                ?.firstOrNull {
                    it.title.contains("Mix", ignoreCase = true) ||
                    it.title.contains("Supermix", ignoreCase = true)
                }

            if (ytMixPlaylist != null) {
                val endpoint = ytMixPlaylist.playEndpoint ?: ytMixPlaylist.radioEndpoint ?: WatchEndpoint(playlistId = ytMixPlaylist.id)
                playerConnection.playQueue(YouTubeQueue(endpoint))
                return
            }

            // 2. Personal taste-driven radio mix from user's history / favorites
            val candidateSeeds = mutableListOf<com.darkxvenom.airbeats.models.MediaMetadata>()
            quickPicks?.take(15)?.forEach { candidateSeeds.add(it.toMediaMetadata()) }
            forgottenFavorites?.take(15)?.forEach { candidateSeeds.add(it.toMediaMetadata()) }

            if (candidateSeeds.isEmpty()) {
                withContext(Dispatchers.IO) {
                    val dbLiked = runCatching { viewModel.database.likedSongs(SongSortType.CREATE_DATE, true).first() }.getOrDefault(emptyList())
                    dbLiked.take(20).forEach { candidateSeeds.add(it.toMediaMetadata()) }
                    if (candidateSeeds.isEmpty()) {
                        val dbRecent = runCatching { viewModel.database.recentSongs(20).first() }.getOrDefault(emptyList())
                        dbRecent.forEach { candidateSeeds.add(it.toMediaMetadata()) }
                    }
                }
            }

            // 3. Fallback for new installs with no local playback history
            if (candidateSeeds.isEmpty()) {
                homePage?.sections?.flatMap { it.items }?.filterIsInstance<SongItem>()?.take(15)?.forEach {
                    candidateSeeds.add(it.toMediaMetadata())
                }
            }

            val seed = candidateSeeds.shuffled().firstOrNull()
            if (seed != null) {
                val radioEndpoint = WatchEndpoint(
                    videoId = seed.id,
                    playlistId = "RDAMVM${seed.id}"
                )
                playerConnection.playQueue(YouTubeQueue(radioEndpoint, preloadItem = seed))
            }
        } catch (e: Exception) {
            reportException(e)
        } finally {
            isGeneratingMix = false
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            listState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> stringResource(R.string.greeting_morning)
        in 12..16 -> stringResource(R.string.greeting_afternoon)
        in 17..21 -> stringResource(R.string.greeting_evening)
        else -> stringResource(R.string.greeting_night)
    }

    val formattedDate = remember {
        try {
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        } catch (_: Exception) { "" }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        com.darkxvenom.airbeats.ui.component.ScreenAdaptiveBackground(
            artworkUrl = mediaMetadata?.thumbnailUrl
        )
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
                state = listState,
                contentPadding = PaddingValues(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp,
                    bottom = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding() + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = hazeState),
            ) {
                // 1. HERO SECTION
                if (isSectionVisible(MaterialHomeSection.HERO)) {
                    item(key = "hero") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.greeting_with_name, greeting, displayName),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.3).sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                if (formattedDate.isNotBlank()) {
                                    Text(
                                        text = formattedDate,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    )
                                }
                            }

                            MaterialHeroBanner(
                                onPlayRadio = {
                                    com.darkxvenom.airbeats.ui.component.InfiniteRadioHelper.playShuffledRadio(
                                        playerConnection = playerConnection,
                                        currentSongId = mediaMetadata?.id,
                                        quickPicks = quickPicks,
                                        forgottenFavorites = forgottenFavorites,
                                        keepListening = keepListening,
                                        homeSongs = homePage?.sections?.flatMap { it.items }?.filterIsInstance<com.darkxvenom.airbeats.innertube.models.SongItem>()
                                    )
                                }
                            )
                        }
                    }
                }

                // 2. QUICK TILES SECTION
                if (isSectionVisible(MaterialHomeSection.QUICK_TILES)) {
                    item(key = "quick_tiles") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MaterialSectionHeader(title = stringResource(R.string.ui_heading_quick_access))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MaterialQuickTile(
                                    title = stringResource(R.string.ui_heading_liked_songs),
                                    subtitle = stringResource(R.string.ui_heading_your_collection),
                                    icon = Icons.Filled.Favorite,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navController.navigate("auto_playlist/liked") }
                                )
                                MaterialQuickTile(
                                    title = "Mix",
                                    subtitle = if (isGeneratingMix) "Generating..." else "Made for you",
                                    icon = Icons.Filled.AutoAwesome,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    isLoading = isGeneratingMix,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!isGeneratingMix) {
                                            coroutineScope.launch {
                                                generateAndPlayMix()
                                            }
                                        }
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MaterialQuickTile(
                                    title = stringResource(R.string.history),
                                    subtitle = stringResource(R.string.ui_heading_recent_listens),
                                    icon = Icons.Filled.History,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navController.navigate("history") }
                                )
                                MaterialQuickTile(
                                    title = stringResource(R.string.stats),
                                    subtitle = stringResource(R.string.ui_heading_listening_stats),
                                    icon = Icons.Filled.TrendingUp,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navController.navigate(Screens.Stats.route) }
                                )
                            }
                        }
                    }
                }

                // 3. TASTE STRIP SECTION
                if (isSectionVisible(MaterialHomeSection.TASTE_STRIP)) {
                    item(key = "taste_strip") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MaterialSectionHeader(
                                title = stringResource(R.string.ui_heading_explore_by_vibe),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(TASTE_TAGS) { tag ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            navController.navigate("search/${URLEncoder.encode(tag, "UTF-8")}")
                                        },
                                        label = {
                                            Text(
                                                text = tag,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        },
                                        shape = CircleShape,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. QUICK PICKS SECTION
                if (isSectionVisible(MaterialHomeSection.QUICK_PICKS)) {
                    quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                        item(key = "quick_picks") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MaterialSectionHeader(
                                    title = stringResource(R.string.ui_text_quick_picks),
                                    subtitle = stringResource(R.string.ui_heading_trending_songs_picked_for_you),
                                    actionText = stringResource(R.string.ui_extra_play_all),
                                    actionIcon = Icons.Filled.PlayArrow,
                                    onActionClick = {
                                        picks.firstOrNull()?.let {
                                            playerConnection.playQueue(YouTubeQueue.radio(it.toMediaMetadata()))
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(picks.take(12)) { item ->
                                        MaterialMediaCard(
                                            title = item.song.title,
                                            subtitle = item.artists.joinToString { it.name },
                                            thumbnailUrl = item.song.thumbnailUrl,
                                            onClick = {
                                                playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. BECAUSE YOU LISTEN TO
                if (isSectionVisible(MaterialHomeSection.BECAUSE_YOU_LISTEN_TO)) {
                    similarRecommendations?.firstOrNull()?.let { recommendation ->
                        item(key = "because_you_listen_to") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MaterialSectionHeader(
                                    title = stringResource(R.string.ui_heading_because_you_listen_to),
                                    subtitle = recommendation.title.title,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recommendation.items.take(10)) { item ->
                                        MaterialMediaCard(
                                            title = item.title,
                                            subtitle = when (item) {
                                                is SongItem -> item.artists.joinToString { it.name }
                                                is AlbumItem -> item.artists.orEmpty().joinToString { it.name }
                                                is PlaylistItem -> item.author?.name.orEmpty()
                                                is ArtistItem -> "Artist"
                                            },
                                            thumbnailUrl = item.thumbnail,
                                            onClick = {
                                                when (item) {
                                                    is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id)))
                                                    is AlbumItem -> navController.navigate("album/${item.browseId}")
                                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. FRESH FINDS SECTION
                if (isSectionVisible(MaterialHomeSection.FRESH_FINDS)) {
                    quickPicks?.drop(6)?.takeIf { it.isNotEmpty() }?.let { freshTracks ->
                        item(key = "fresh_finds") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = CardShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
                            ) {
                                Column(modifier = Modifier.padding(vertical = 14.dp)) {
                                    MaterialSectionHeader(
                                        title = stringResource(R.string.fresh_finds),
                                        subtitle = stringResource(R.string.fresh_tracks_desc),
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(freshTracks.take(8)) { item ->
                                            MaterialMediaCard(
                                                title = item.song.title,
                                                subtitle = item.artists.joinToString { it.name },
                                                thumbnailUrl = item.song.thumbnailUrl,
                                                badgeText = "NEW",
                                                onClick = {
                                                    playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. JUMP BACK IN
                if (isSectionVisible(MaterialHomeSection.JUMP_BACK_IN)) {
                    keepListening?.filterIsInstance<Song>()?.takeIf { it.isNotEmpty() }?.let { items ->
                        item(key = "jump_back_in") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MaterialSectionHeader(
                                    title = stringResource(R.string.ui_text_jump_back_in),
                                    subtitle = stringResource(R.string.ui_heading_from_your_recent_listening_history),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(items.take(12)) { song ->
                                        MaterialMediaCard(
                                            title = song.song.title,
                                            subtitle = song.artists.joinToString { it.name },
                                            thumbnailUrl = song.song.thumbnailUrl,
                                            onClick = {
                                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 8. MIXES SECTION
                if (isSectionVisible(MaterialHomeSection.MIXES)) {
                    accountPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                        item(key = "mixes") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MaterialSectionHeader(
                                    title = stringResource(R.string.ui_heading_mixes_playlists),
                                    subtitle = stringResource(R.string.ui_heading_familiar_favorites_continuous_playback),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(playlists.take(10)) { playlist ->
                                        MaterialMediaCard(
                                            title = playlist.title,
                                            subtitle = playlist.author?.name ?: "Playlist",
                                            thumbnailUrl = playlist.thumbnail,
                                            onClick = {
                                                navController.navigate("online_playlist/${playlist.id}")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 9. SPOTLIGHT HERO SECTION
                if (isSectionVisible(MaterialHomeSection.SPOTLIGHT)) {
                    quickPicks?.firstOrNull()?.let { pick ->
                        val artist = pick.artists.firstOrNull()
                        if (artist != null) {
                            item(key = "spotlight") {
                                MaterialSpotlightCard(
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
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }

                // 10. TOP ARTISTS SECTION
                if (isSectionVisible(MaterialHomeSection.TOP_ARTISTS)) {
                    quickPicks?.mapNotNull { pick ->
                        pick.artists.firstOrNull()?.let { artist ->
                            Triple(artist, artist.thumbnailUrl, pick.song.thumbnailUrl)
                        }
                    }?.distinctBy { it.first.id }?.takeIf { it.isNotEmpty() }?.let { artistTriples ->
                        item(key = "top_artists") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MaterialSectionHeader(
                                    title = stringResource(R.string.artists_for_you),
                                    subtitle = stringResource(R.string.artists_for_you_desc),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    itemsIndexed(artistTriples.take(10)) { index, (artist, thumb, songThumb) ->
                                        MaterialArtistCard(
                                            artistId = artist.id,
                                            name = artist.name,
                                            thumbnailUrl = thumb,
                                            fallbackThumbnail = songThumb,
                                            rank = index + 1,
                                            onClick = {
                                                artist.id.takeIf { it.isNotBlank() }?.let { navController.navigate("artist/$it") }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 11. HEAVY ROTATION
                if (isSectionVisible(MaterialHomeSection.HEAVY_ROTATION)) {
                    forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                        item(key = "heavy_rotation") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MaterialSectionHeader(
                                    title = stringResource(R.string.heavy_rotation),
                                    subtitle = stringResource(R.string.heavy_rotation_desc),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(favorites.take(10)) { item ->
                                        MaterialMediaCard(
                                            title = item.song.title,
                                            subtitle = item.artists.joinToString { it.name },
                                            thumbnailUrl = item.song.thumbnailUrl,
                                            onClick = {
                                                playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata()))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 12. ALBUMS SECTION & 13. CHARTS & 14. NEW RELEASES from homePage
                homePage?.sections?.forEachIndexed { index, section ->
                    val isNewRelease = section.title.contains("New", ignoreCase = true) || section.title.contains("Release", ignoreCase = true)
                    val isChart = section.title.contains("Chart", ignoreCase = true) || section.title.contains("Top", ignoreCase = true)
                    val isAlbum = section.title.contains("Album", ignoreCase = true)

                    val shouldRender = when {
                        isNewRelease -> isSectionVisible(MaterialHomeSection.NEW_RELEASES)
                        isChart -> isSectionVisible(MaterialHomeSection.CHARTS)
                        isAlbum -> isSectionVisible(MaterialHomeSection.ALBUMS)
                        else -> true
                    }

                    if (shouldRender && section.items.isNotEmpty()) {
                        item(key = "section_${section.title}_$index") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                MaterialSectionHeader(
                                    title = section.title,
                                    subtitle = section.label,
                                    actionText = if (section.endpoint != null) stringResource(R.string.see_all) else null,
                                    actionIcon = if (section.endpoint != null) Icons.AutoMirrored.Filled.ArrowForwardIos else null,
                                    onActionClick = section.endpoint?.let { endpoint ->
                                        {
                                            navController.navigate(
                                                "youtube_browse/${endpoint.browseId}?params=${endpoint.params.orEmpty()}"
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(section.items.take(12)) { rankIndex, item ->
                                        if (isChart) {
                                            MaterialChartCard(
                                                rank = rankIndex + 1,
                                                title = item.title,
                                                subtitle = when (item) {
                                                    is SongItem -> item.artists.joinToString { it.name }
                                                    is AlbumItem -> item.artists.orEmpty().joinToString { it.name }
                                                    else -> ""
                                                },
                                                thumbnailUrl = item.thumbnail,
                                                onClick = {
                                                    when (item) {
                                                        is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id)))
                                                        is AlbumItem -> navController.navigate("album/${item.browseId}")
                                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                        is ArtistItem -> navController.navigate("artist/${item.id}")
                                                    }
                                                }
                                            )
                                        } else {
                                            MaterialMediaCard(
                                                title = item.title,
                                                subtitle = when (item) {
                                                    is SongItem -> item.artists.joinToString { it.name }
                                                    is AlbumItem -> item.artists.orEmpty().joinToString { it.name }
                                                    is PlaylistItem -> item.author?.name.orEmpty()
                                                    is ArtistItem -> "Artist"
                                                },
                                                thumbnailUrl = item.thumbnail,
                                                badgeText = if (isNewRelease) "NEW" else null,
                                                onClick = {
                                                    when (item) {
                                                        is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id)))
                                                        is AlbumItem -> navController.navigate("album/${item.browseId}")
                                                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                        is ArtistItem -> navController.navigate("artist/${item.id}")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PullToRefreshDefaults.LoadingIndicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp),
            )
        }

        // TopFadeBlur progressive blur effect just below edge line of top header
        TopFadeBlur(
            hazeState = hazeState,
            pageColor = Color.Transparent,
            scrimColor = Color.Transparent,
            height = 68.dp,
            alpha = blurAlpha,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (headerHeightDp - 20.dp).coerceAtLeast(0.dp)),
        )

        // Top Header Bar
        MaterialTopHeader(
            onNewReleasesClick = { navController.navigate("new_release") },
            onDevNewsClick = { navController.navigate("settings/developer_news") },
            onSettingsClick = { navController.navigate("settings") },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height }
        )

        // Floating 3-dot FAB
        HomeFloatingActions(
            navController = navController,
            lazyListState = listState
        )
    }
}

@Composable
private fun MaterialTopHeader(
    onNewReleasesClick: () -> Unit,
    onDevNewsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.ui_text_airbeats),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onNewReleasesClick) {
                    Icon(
                        imageVector = Icons.Filled.NewReleases,
                        contentDescription = stringResource(R.string.ui_text_new_releases),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDevNewsClick) {
                    Icon(
                        painter = painterResource(R.drawable.newspaper),
                        contentDescription = stringResource(R.string.ui_a11y_news_from_developer),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialHeroBanner(
    onPlayRadio: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Radio,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.smart_radio),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.smart_radio_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                )
            }

            Spacer(Modifier.width(12.dp))

            FilledIconButton(
                onClick = onPlayRadio,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.play_radio),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun MaterialQuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = contentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MaterialSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MaterialMediaCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
) {
    Column(
        modifier = modifier
            .width(148.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(MediaShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            AsyncImage(
                model = thumbnailUrl?.highQualityThumbnail(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MaterialChartCard(
    rank: Int,
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.width(220.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (rank <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AsyncImage(
                model = thumbnailUrl?.highQualityThumbnail(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MaterialArtistCard(
    artistId: String?,
    name: String,
    thumbnailUrl: String?,
    fallbackThumbnail: String? = null,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val dbArtist by remember(artistId) {
        if (artistId != null) {
            database.artist(artistId)
        } else {
            flowOf(null)
        }
    }.collectAsState(initial = null)

    var remoteThumbnail by remember(artistId) { mutableStateOf<String?>(null) }
    LaunchedEffect(artistId, dbArtist?.artist?.thumbnailUrl) {
        val currentThumb = dbArtist?.artist?.thumbnailUrl ?: thumbnailUrl
        if (artistId != null && currentThumb == null && artistId.startsWith("UC")) {
            withContext(Dispatchers.IO) {
                YouTube.artist(artistId).onSuccess { page ->
                    remoteThumbnail = page.artist.thumbnail
                    database.query {
                        dbArtist?.artist?.let { update(it, page) }
                    }
                }
            }
        }
    }

    val finalThumbnail = dbArtist?.artist?.thumbnailUrl
        ?: remoteThumbnail
        ?: thumbnailUrl
        ?: fallbackThumbnail

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(88.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(76.dp)
            ) {
                if (!finalThumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = finalThumbnail.highQualityThumbnail(),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            if (rank <= 3) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$rank",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MaterialSpotlightCard(
    artistName: String,
    artistId: String?,
    thumbnailUrl: String?,
    fallbackThumbnail: String? = null,
    onOpenArtist: () -> Unit,
    onPlayRadio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val dbArtist by remember(artistId) {
        if (artistId != null) {
            database.artist(artistId)
        } else {
            flowOf(null)
        }
    }.collectAsState(initial = null)

    var remoteThumbnail by remember(artistId) { mutableStateOf<String?>(null) }
    LaunchedEffect(artistId, dbArtist?.artist?.thumbnailUrl) {
        val currentThumb = dbArtist?.artist?.thumbnailUrl ?: thumbnailUrl
        if (artistId != null && currentThumb == null && artistId.startsWith("UC")) {
            withContext(Dispatchers.IO) {
                YouTube.artist(artistId).onSuccess { page ->
                    remoteThumbnail = page.artist.thumbnail
                    database.query {
                        dbArtist?.artist?.let { update(it, page) }
                    }
                }
            }
        }
    }

    val finalThumbnail = dbArtist?.artist?.thumbnailUrl
        ?: remoteThumbnail
        ?: thumbnailUrl
        ?: fallbackThumbnail

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.artist_spotlight),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = artistName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledIconButton(
                        onClick = onPlayRadio,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.play_radio), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (artistId != null) {
                        TextButton(onClick = onOpenArtist) {
                            Text(stringResource(R.string.view_profile), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .size(76.dp)
                    .clickable(enabled = artistId != null, onClick = onOpenArtist)
            ) {
                if (!finalThumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = finalThumbnail.highQualityThumbnail(),
                        contentDescription = artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}
