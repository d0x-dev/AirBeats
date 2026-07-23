package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.viewmodels.MoodAndGenresViewModel
import kotlin.math.absoluteValue

val vibrantGradients = listOf(
    listOf(Color(0xFF833AB4), Color(0xFFFD1D1D)),
    listOf(Color(0xFF4568DC), Color(0xFFB06AB3)),
    listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
    listOf(Color(0xFF3A1C71), Color(0xFFD76D77)),
    listOf(Color(0xFF1CB5E0), Color(0xFF000851)),
    listOf(Color(0xFF141E30), Color(0xFF243B55)),
    listOf(Color(0xFFDA22FF), Color(0xFF9733EE)),
    listOf(Color(0xFFED213A), Color(0xFF93291E)),
    listOf(Color(0xFF0052D4), Color(0xFF65C7F7)),
    listOf(Color(0xFF0F2027), Color(0xFF203A43)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D))
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun NeonExploreScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val bgColor = if (isDarkTheme) NeonDarkBg else MaterialTheme.colorScheme.background
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()
    
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
                    text = "Explore",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(
                        painter = painterResource(R.drawable.settings),
                        contentDescription = "Settings",
                        tint = textColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Bar Navigates to Search Screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkTheme) Color(0xFF1E1E24) else Color(0xFFF0F0F0))
                    .clickable { navController.navigate("search") }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search",
                    tint = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search songs, artists, albums, podcasts...",
                    color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Real Moods & Genres
            if (moodAndGenresList == null) {
                // Loading Placeholder
                Text(text = "Loading Explore...", color = textColor)
            } else {
                moodAndGenresList?.forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        category.items.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowItems.forEach { item ->
                                    val hash = item.title.hashCode().absoluteValue
                                    val gradient = vibrantGradients[hash % vibrantGradients.size]
                                    MoodCard(
                                        modifier = Modifier.weight(1f),
                                        title = item.title,
                                        subtitle = "Tap to browse",
                                        iconRes = R.drawable.album,
                                        gradientColors = gradient,
                                        onClick = {
                                            navController.navigate(
                                                "youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}"
                                            )
                                        }
                                    )
                                }
                                // Handle odd item at end of list
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun MoodCard(
    modifier: Modifier, 
    title: String, 
    subtitle: String, 
    iconRes: Int, 
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
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
