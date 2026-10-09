package com.darkxvenom.airbeats.ui.screens.material

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.res.painterResource
import com.darkxvenom.airbeats.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.darkxvenom.airbeats.ui.screens.search.airbeatsChartsItems
import com.darkxvenom.airbeats.ui.screens.search.recentSearchesItems
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.constants.PauseSearchHistoryKey
import com.darkxvenom.airbeats.db.entities.SearchHistory
import com.darkxvenom.airbeats.viewmodels.OnlineSearchSuggestionViewModel
import java.net.URLEncoder
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

data class GenreCategory(
    val title: String,
    val color: Color,
    val imageUrl: String
)

private val GENRE_CATEGORIES = listOf(
    GenreCategory("Chill", Color(0xFF5A758D), "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Community", Color(0xFF8F7523), "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Energize", Color(0xFFA89547), "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Feel good", Color(0xFF5B8E60), "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Focus", Color(0xFF6B7A7A), "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Gaming", Color(0xFF383E44), "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Party", Color(0xFF74528E), "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Romance", Color(0xFF9E422D), "https://images.unsplash.com/photo-1518199266791-5375a83190b7?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Sad", Color(0xFF535D5C), "https://images.unsplash.com/photo-1516589178581-6cd7833ae3b2?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Sleep", Color(0xFF4C3A7A), "https://images.unsplash.com/photo-1511295742362-92c96b124e52?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Workout", Color(0xFFA35728), "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("African", Color(0xFF10722C), "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Arabic", Color(0xFF984617), "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Bengali", Color(0xFF837E43), "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Pop", Color(0xFFAD3869), "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Rock", Color(0xFF8F2929), "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Hip-Hop", Color(0xFFBA5A20), "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?auto=format&fit=crop&w=200&q=80"),
    GenreCategory("Bollywood", Color(0xFF8B2F57), "https://images.unsplash.com/photo-1533174072545-7a4b6ad7a6c3?auto=format&fit=crop&w=200&q=80")
)

/**
 * Material 3 Expressive Search Screen companion.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MaterialSearchScreen(
    navController: NavController,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsState()
    val viewState by viewModel.viewState.collectAsState()
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val (pauseSearchHistory) = com.darkxvenom.airbeats.utils.rememberPreference(
        key = PauseSearchHistoryKey,
        defaultValue = false
    )

    val onPerformSearch: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            keyboardController?.hide()
            navController.navigate("search/${URLEncoder.encode(text.trim(), "UTF-8")}")
            if (!pauseSearchHistory) {
                database.query {
                    insert(SearchHistory(query = text.trim()))
                }
            }
        }
    }

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }

    val (pureBlack, _) = com.darkxvenom.airbeats.utils.rememberPreference(
        com.darkxvenom.airbeats.constants.PureBlackKey,
        defaultValue = false
    )
    val plainBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
    var selectedTab by remember { mutableStateOf(com.darkxvenom.airbeats.ui.component.SearchTab.RECENT_SEARCHES) }
    val chartsViewModel: com.darkxvenom.airbeats.viewmodels.ChartsViewModel = hiltViewModel()
    var showRegionSheet by remember { mutableStateOf(false) }
    var regionCode by com.darkxvenom.airbeats.utils.rememberPreference(
        com.darkxvenom.airbeats.charts.ChartRegionKey,
        defaultValue = "system"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(plainBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp,
                    bottom = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding(),
                )
        ) {
            if (query.isBlank()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        com.darkxvenom.airbeats.ui.component.SearchPillSwitcher(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        )
                    }

                    when (selectedTab) {
                        com.darkxvenom.airbeats.ui.component.SearchTab.BROWSE_ALL -> {
                            item {
                                Text(
                                    text = "Explore Categories",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            items(GENRE_CATEGORIES.chunked(2)) { pair ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    pair.forEach { category ->
                                        GenreCard(
                                            category = category,
                                            onClick = { onPerformSearch(category.title) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(Modifier.weight(1f))
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
                                onSearch = onPerformSearch,
                                onFillQuery = { queryText: String -> viewModel.query.value = queryText }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewState.history) { historyItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPerformSearch(historyItem.query) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = historyItem.query,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.NorthWest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    items(viewState.suggestions) { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPerformSearch(suggestion) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.NorthWest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Search Header
        Surface(
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                TextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    placeholder = {
                        Text(
                            text = "Search songs, artists, albums...",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.query.value = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (selectedTab == com.darkxvenom.airbeats.ui.component.SearchTab.AIRBEATS_CHARTS) {
                            IconButton(onClick = { showRegionSheet = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.globe_search),
                                    contentDescription = "Select Region",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onPerformSearch(query) }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showRegionSheet) {
            com.darkxvenom.airbeats.charts.ChartRegionSheet(
                currentRegionSlug = regionCode,
                onRegionSelected = { selected ->
                    regionCode = selected
                    showRegionSheet = false
                },
                onDismiss = { showRegionSheet = false }
            )
        }
    }
}

@Composable
private fun GenreCard(
    category: GenreCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = category.color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = category.imageUrl,
                    contentDescription = category.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
