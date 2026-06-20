package com.darkxvenom.airbeats.ui.screens
import android.annotation.SuppressLint
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.ui.component.NavigationTitle
import com.darkxvenom.airbeats.ui.component.YouTubeGridItem
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.menu.YouTubeSongMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeAlbumMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeArtistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubePlaylistMenu
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

import com.darkxvenom.airbeats.ui.component.BottomSheetState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayfulHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    playerBottomSheetState: BottomSheetState,
    onSearchClick: () -> Unit
) {
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableStateOf("History") }

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

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            onBackground = Color.Black,
            onSurface = Color.Black,
            surface = Color.White
        )
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.Black) {
            Scaffold(
                containerColor = Color(0xFFFFD54F) // Bright yellow background
            ) { padding ->
                Box(modifier = Modifier
                    .fillMaxSize()
                    .pullToRefresh(
                        state = pullRefreshState,
                        isRefreshing = isRefreshing,
                        onRefresh = viewModel::refresh
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = padding.calculateTopPadding())
                    ) {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val userName = com.darkxvenom.airbeats.ui.component.LocalUserName.current
                            val displayName = if (userName.isNotEmpty()) userName else "Friend"
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val rankPrefMgr = remember { com.darkxvenom.airbeats.ui.component.RankPreferenceManager(context) }
                            val displayedRank by rankPrefMgr.displayedRank.collectAsState(initial = null)
                            val statsViewModel = hiltViewModel<com.darkxvenom.airbeats.viewmodels.StatsViewModel>()
                            val currentRank by statsViewModel.currentRank.collectAsState(initial = null)
                            val totalHours by statsViewModel.totalListenHours.collectAsState(initial = 0.0)
                            val coroutineScope = rememberCoroutineScope()

                            val greatVibesFontFamily = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(com.darkxvenom.airbeats.R.font.great_vibes))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hi, ",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = greatVibesFontFamily,
                                    color = Color.Black
                                )
                                Text(
                                    text = displayName,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = greatVibesFontFamily,
                                    color = Color.Black
                                )

                                currentRank?.let { rank ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    var showBadgeSelector by remember { mutableStateOf(false) }
                                    com.darkxvenom.airbeats.ui.component.RankBadge(
                                        rank = rank,
                                        displayedRank = displayedRank,
                                        size = 28.dp,
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
                            IconButton(onClick = { navController.navigate("settings") }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Settings",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Vertical Tabs
                            Column(
                                modifier = Modifier
                                    .width(60.dp)
                                    .fillMaxHeight()
                                    .padding(bottom = 250.dp),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val tabs = listOf("History", "Stats", "Liked", "Downloaded")
                                tabs.reversed().forEach { tab ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clickable {
                                                selectedTab = tab
                                                when (tab) {
                                                    "History" -> navController.navigate("history")
                                                    "Stats" -> navController.navigate("stats")
                                                    "Liked" -> navController.navigate("auto_playlist/liked")
                                                    "Downloaded" -> navController.navigate("auto_playlist/downloaded")
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tab,
                                            fontSize = 16.sp,
                                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == tab) Color.Black else Color.Black.copy(alpha = 0.5f),
                                            softWrap = false,
                                            maxLines = 1,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier
                                                .requiredWidth(120.dp)
                                                .rotate(-90f)
                                        )
                                        if (selectedTab == tab) {
                                            Box(
                                                modifier = Modifier
                                                    .offset(x = (-30).dp)
                                                    .size(6.dp)
                                                    .background(Color.Black, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }

                            // Content Area
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                                    .padding(end = 16.dp),
                                contentPadding = PaddingValues(bottom = 250.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    // Featured Card (Carousel)
                                    val featuredItems = quickPicks ?: emptyList()
                                    val featuredItem = featuredItems.firstOrNull()

                                    if (featuredItem != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(300.dp)
                                                .background(Color.White, RoundedCornerShape(32.dp))
                                                .clickable {
                                                    playerConnection.playQueue(YouTubeQueue.radio(featuredItem.toMediaMetadata()))
                                                }
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                AsyncImage(
                                                    model = featuredItem.thumbnailUrl?.highQualityThumbnail(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(180.dp)
                                                        .clip(CircleShape)
                                                )
                                                Spacer(modifier = Modifier.height(24.dp))
                                                Text(
                                                    text = featuredItem.title,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(300.dp))
                                    }
                                }

                                // Vertical List of Songs with Thumbnails
                                items(quickPicks?.drop(1)?.take(10) ?: emptyList()) { song ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = song.thumbnailUrl?.highQualityThumbnail(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = song.artists.joinToString { it.name },
                                                fontSize = 14.sp,
                                                color = Color.Black.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                
                                // Extra Sections matching Classic Home Screen
                                homePage?.sections?.forEach { section ->
                                    item {
                                        NavigationTitle(
                                            title = section.title,
                                            label = section.label,
                                            modifier = Modifier.padding(top = 16.dp)
                                        )
                                    }
                                    item {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(section.items) { item ->
                                                ytGridItem(item)
                                            }
                                        }
                                    }
                                }
                            }
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
                                            contentDescription = "Play/Pause",
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
                                            contentDescription = "Like",
                                            tint = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else Color.Black
                                        )
                                    }
                                    IconButton(onClick = { playerConnection.player.seekToNext() }) {
                                        Icon(
                                            painter = painterResource(R.drawable.skip_next),
                                            contentDescription = "Skip Next",
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
                                    Icon(painterResource(R.drawable.home), null, tint = Color(0xFFE5A93D))
                                }
                                IconButton(onClick = { navController.navigate("explore") { launchSingleTop = true } }) {
                                    Icon(painterResource(R.drawable.explore_outlined), null, tint = Color.Black)
                                }
                                IconButton(onClick = onSearchClick) {
                                    Icon(painterResource(R.drawable.search), null, tint = Color.Black)
                                }
                                IconButton(onClick = { navController.navigate("library") { launchSingleTop = true } }) {
                                    Icon(painterResource(R.drawable.library_music), null, tint = Color.Black)
                                }
                            }
                        }
                    }
                    
                    Indicator(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding()),
                        isRefreshing = isRefreshing,
                        state = pullRefreshState
                    )
                }
            }
        }
    }
}
