package com.darkxvenom.airbeats.ui.screens.search

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.ui.screens.NeonDarkBg
import com.darkxvenom.airbeats.ui.screens.NeonPurple
import com.darkxvenom.airbeats.viewmodels.OnlineSearchSuggestionViewModel
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun NeonSearchScreen(
    navController: NavController,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel()
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val bgColor = if (isDarkTheme) NeonDarkBg else MaterialTheme.colorScheme.background
    val textColor = if (isDarkTheme) Color.White else Color.Black
    
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val viewState by viewModel.viewState.collectAsState()
    
    var query by remember { mutableStateOf("") }
    
    LaunchedEffect(query) {
        viewModel.query.value = query
    }
    
    val onSearch = {
        if (query.isNotBlank()) {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            navController.navigate("search/$encodedQuery")
            keyboardController?.hide()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        .asPaddingValues()
                )
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Search",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search Input Field
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = if (isDarkTheme) Color(0xFF1E1E24) else Color(0xFFF0F0F0),
                    unfocusedContainerColor = if (isDarkTheme) Color(0xFF1E1E24) else Color(0xFFF0F0F0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = NeonPurple
                ),
                placeholder = {
                    Text(
                        text = "Search songs, artists, albums...",
                        color = if (isDarkTheme) Color.Gray else Color.DarkGray,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = "Search",
                        tint = if (isDarkTheme) Color.Gray else Color.DarkGray,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Clear",
                                    tint = if (isDarkTheme) Color.Gray else Color.DarkGray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        IconButton(onClick = { navController.navigate(com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionRoute) }) {
                            Icon(
                                painter = painterResource(R.drawable.mic),
                                contentDescription = "Music Recognition",
                                tint = if (isDarkTheme) Color.Gray else Color.DarkGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (query.isNotEmpty() || viewState.history.isNotEmpty() || viewState.suggestions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(viewState.history, key = { "history_${it.query}" }) { history ->
                        NeonSuggestionItem(
                            query = history.query,
                            online = false,
                            onClick = {
                                query = history.query
                                onSearch()
                            },
                            onDelete = {
                                database.query { delete(history) }
                            },
                            onFillTextField = { query = history.query },
                            isDarkTheme = isDarkTheme,
                            textColor = textColor
                        )
                    }

                    items(viewState.suggestions, key = { "suggestion_$it" }) { suggestion ->
                        NeonSuggestionItem(
                            query = suggestion,
                            online = true,
                            onClick = {
                                query = suggestion
                                onSearch()
                            },
                            onFillTextField = { query = suggestion },
                            isDarkTheme = isDarkTheme,
                            textColor = textColor
                        )
                    }
                }
            } else {
                // Default Explore Screen for Search
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                ) {
                    item {
                        Text(
                            text = "Browse all",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
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
                        
                        genres.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (genre, color) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(96.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(color)
                                            .clickable { 
                                                query = genre
                                                onSearch()
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun NeonSuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    isDarkTheme: Boolean,
    textColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isDarkTheme) Color(0xFF1E1E24) else Color(0xFFF0F0F0),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                painterResource(if (online) R.drawable.search else R.drawable.history),
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = query,
            fontSize = 16.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!online) {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = if (isDarkTheme) Color.Gray else Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(onClick = onFillTextField) {
            Icon(
                painter = painterResource(R.drawable.arrow_top_left),
                contentDescription = null,
                tint = if (isDarkTheme) Color.Gray else Color.DarkGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ExploreCategoryCard(modifier: Modifier, title: String, iconRes: Int, bgColor: Color) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { /* Navigate */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
