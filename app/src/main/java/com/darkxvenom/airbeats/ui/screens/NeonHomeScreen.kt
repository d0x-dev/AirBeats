package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.LocalUserName
import com.darkxvenom.airbeats.ui.component.RankPreferenceManager
import com.darkxvenom.airbeats.ui.component.RankBadge
import com.darkxvenom.airbeats.ui.component.BadgeSelector
import com.darkxvenom.airbeats.ui.component.unlockedRanksFromHours
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.viewmodels.HomeViewModel
import com.darkxvenom.airbeats.viewmodels.StatsViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem

val NeonPurple = Color(0xFFA259FF)
val NeonDarkBg = Color(0xFF0F0F14)
val NeonCardBg = Color(0xFF1C1C24)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonHomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    
    val playerConnection = LocalPlayerConnection.current ?: return
    val statsViewModel = hiltViewModel<StatsViewModel>()
    val coroutineScope = rememberCoroutineScope()
    
    val context = LocalContext.current
    val rankPrefMgr = remember { RankPreferenceManager(context) }
    val displayedRank by rankPrefMgr.displayedRank.collectAsState(initial = null)
    val currentRank by statsViewModel.currentRank.collectAsState(initial = null)
    val totalHours by statsViewModel.totalListenHours.collectAsState(initial = 0.0)
    
    val userName = LocalUserName.current
    val displayName = if (userName.isNotEmpty()) userName else "Friend"
    
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingText = when {
        hour in 0..11 -> "Good Morning \uD83D\uDC4B"
        hour in 12..16 -> "Good Afternoon \uD83C\uDF1E"
        else -> "Good Evening \uD83C\uDF19"
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f 
    val bgColor = if (isDarkTheme) NeonDarkBg else MaterialTheme.colorScheme.background
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                )
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.music_note), 
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AirBeats",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings",
                        tint = textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Greeting & Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$greetingText $displayName",
                    fontSize = 16.sp,
                    color = if (isDarkTheme) Color.LightGray else Color.DarkGray
                )
                
                currentRank?.let { rank ->
                    Spacer(modifier = Modifier.width(8.dp))
                    var showBadgeSelector by remember { mutableStateOf(false) }
                    RankBadge(
                        rank = rank,
                        displayedRank = displayedRank,
                        size = 20.dp,
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
            
            Text(
                text = buildAnnotatedString {
                    append("Feel the ")
                    withStyle(style = SpanStyle(color = NeonPurple)) {
                        append("music")
                    }
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // New Release Featured Card
            val topPick = quickPicks?.firstOrNull()
            if (topPick != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF231242), NeonPurple.copy(alpha = 0.6f))
                            )
                        )
                        .clickable { 
                            playerConnection.playQueue(YouTubeQueue.radio(topPick.toMediaMetadata()))
                        }
                ) {
                    AsyncImage(
                        model = topPick.thumbnailUrl?.highQualityThumbnail(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().padding(start = 120.dp)
                    )
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF231242), Color.Transparent)
                        )
                    ))
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "TOP PICK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = topPick.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = topPick.artists.joinToString { it.name },
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Recently Played
            val recentlyPlayedItems = quickPicks?.drop(1)?.take(10) ?: emptyList()
            if (recentlyPlayedItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listen Again",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = "See all",
                        fontSize = 12.sp,
                        color = NeonPurple,
                        modifier = Modifier.clickable { navController.navigate("history") }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(recentlyPlayedItems) { song ->
                        Column(
                            modifier = Modifier.width(100.dp).clickable {
                                playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                            }
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl?.highQualityThumbnail(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = song.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                maxLines = 1
                            )
                            Text(
                                text = song.artists.joinToString { it.name },
                                fontSize = 12.sp,
                                color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Dynamic Home Page Sections
            homePage?.sections?.forEach { section ->
                Text(
                    text = section.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(section.items) { item ->
                        val imageUrl = item.thumbnail
                        val title = item.title
                        
                        val subtitle = when (item) {
                            is SongItem -> item.artists.joinToString { it.name }
                            is AlbumItem -> item.artists?.joinToString { it.name } ?: ""
                            is PlaylistItem -> item.author?.name ?: ""
                            else -> ""
                        }

                        Column(
                            modifier = Modifier.width(140.dp).clickable {
                                when (item) {
                                    is SongItem -> playerConnection.playQueue(YouTubeQueue(
                                        item.endpoint ?: com.darkxvenom.airbeats.innertube.models.WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                }
                            }
                        ) {
                            AsyncImage(
                                model = imageUrl?.highQualityThumbnail(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                maxLines = 1
                            )
                            if (subtitle.isNotEmpty()) {
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            Spacer(modifier = Modifier.height(100.dp)) // space for mini player and bottom nav
        }
        
        Indicator(
            modifier = Modifier.align(Alignment.TopCenter),
            isRefreshing = isRefreshing,
            state = pullToRefreshState
        )
    }
}
