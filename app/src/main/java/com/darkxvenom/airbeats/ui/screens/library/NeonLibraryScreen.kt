package com.darkxvenom.airbeats.ui.screens.library

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.ui.component.CreatePlaylistDialog
import com.darkxvenom.airbeats.ui.screens.NeonDarkBg
import com.darkxvenom.airbeats.ui.screens.NeonPurple
import com.darkxvenom.airbeats.viewmodels.LibraryPlaylistsViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun NeonLibraryScreen(
    navController: NavController,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val bgColor = if (isDarkTheme) NeonDarkBg else MaterialTheme.colorScheme.background
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    val playlists by viewModel.allPlaylists.collectAsState()
    val topSize by viewModel.topValue.collectAsState(initial = 50)
    
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showSpotifyImportDialog by remember { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = "",
            allowSyncing = true
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
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
                Text(
                    text = "Library",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                Row {
                    IconButton(onClick = { navController.navigate("neon_search") }) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Search",
                            tint = textColor
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
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter Chips
            val filters = listOf("Playlists", "Songs", "Albums", "Artists")
            var selectedFilter by remember { mutableStateOf("Playlists") }
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    val isSelected = filter == selectedFilter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                1.dp,
                                if (isSelected) NeonPurple else Color.Gray.copy(alpha = 0.5f),
                                RoundedCornerShape(20.dp)
                            )
                            .background(if (isSelected) NeonPurple.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) textColor else Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Create New Playlist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isDarkTheme) Color(0xFF2C2C35) else Color.LightGray,
                        RoundedCornerShape(12.dp)
                    )
                    .background(Color.Transparent)
                    .clickable { showCreatePlaylistDialog = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Transparent, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = "Add",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Create New Playlist",
                    color = NeonPurple,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Big Cards Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryBigCard(Modifier.weight(1f), "Liked Songs", "auto", R.drawable.favorite, listOf(Color(0xFF833AB4), Color(0xFFFD1D1D))) {
                    navController.navigate("auto_playlist/liked")
                }
                LibraryBigCard(Modifier.weight(1f), "Downloaded", "auto", R.drawable.download, listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))) {
                    navController.navigate("auto_playlist/downloaded")
                }
                LibraryBigCard(Modifier.weight(1f), "My Top $topSize", "auto", R.drawable.trending_up, listOf(Color(0xFFDA22FF), Color(0xFF9733EE))) {
                    navController.navigate("top_playlist/$topSize")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibraryBigCard(Modifier.weight(1f), "Cached", "auto", R.drawable.sync, listOf(Color(0xFF11998E), Color(0xFF38EF7D))) {
                    navController.navigate("cache_playlist/cached")
                }
                LibraryBigCard(
                    modifier = Modifier.weight(1f),
                    title = "Import Playlist",
                    subtitle = "Spotify URL",
                    iconRes = R.drawable.playlist_import,
                    gradientColors = listOf(Color(0xFF1DB954), Color(0xFF191414)),
                    onClick = { showSpotifyImportDialog = true }
                )
                // Placeholder to align
                Box(modifier = Modifier.weight(1f).height(120.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Playlists",
                        color = NeonPurple,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // List items
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                playlists.forEach { playlist ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            navController.navigate("local_playlist/${playlist.id}")
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (playlist.thumbnails.isNotEmpty()) {
                                    AsyncImage(
                                        model = playlist.thumbnails.firstOrNull(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.queue_music),
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.playlist.name,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Text(
                                    text = "${playlist.songCount} songs",
                                    color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    if (showSpotifyImportDialog) {
        SpotifyImportDialog(onDismiss = { showSpotifyImportDialog = false })
    }
}

@Composable
fun LibraryBigCard(
    modifier: Modifier, 
    title: String, 
    subtitle: String, 
    iconRes: Int, 
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
