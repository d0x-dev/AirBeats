/*
 * AirBeats - Universal home. Every home style renders THIS screen, fed by HomeBackend.buildUniversal
 * (same shelf structure and ordering as iTune). Only one backend, one layout, identical content.
 */
package com.darkxvenom.airbeats.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.backend.HomeBackend
import com.darkxvenom.airbeats.constants.DefaultHiddenHomeSections
import com.darkxvenom.airbeats.constants.HiddenHomeSectionsKey
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.constants.MaterialHomeSection
import com.darkxvenom.airbeats.constants.UseLocalOnlyKey
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.ModernPullToRefreshBox
import com.darkxvenom.airbeats.ui.menu.YouTubeAlbumMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeArtistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubePlaylistMenu
import com.darkxvenom.airbeats.ui.menu.YouTubeSongMenu
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.viewmodels.HomeViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalHomeScreen(
    navController: NavController,
    onSearchClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val homePage by viewModel.homePage.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val (cookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(cookie) { "SAPISID" in parseCookieString(cookie) }
    val (useLocal, setUseLocal) = rememberPreference(UseLocalOnlyKey, false)
    val hiddenSections by rememberPreference(HiddenHomeSectionsKey, defaultValue = DefaultHiddenHomeSections)

    val home = remember(homePage, accountPlaylists, isLoggedIn) {
        HomeBackend.buildUniversal(homePage, accountPlaylists.orEmpty(), isLoggedIn)
    }

    fun open(item: YTItem) {
        when (item) {
            is SongItem -> playerConnection.playQueue(
                YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata())
            )
            is AlbumItem -> navController.navigate("album/${item.id}")
            is ArtistItem -> navController.navigate("artist/${item.id}")
            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
        }
    }

    fun menu(item: YTItem) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            when (item) {
                is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
            }
        }
    }

    ModernPullToRefreshBox(isRefreshing = isRefreshing, onRefresh = viewModel::refresh) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 100.dp,
            ),
        ) {
            item(key = "top_header") {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("AirBeats", fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSearchClick) { Icon(Icons.Default.Search, contentDescription = "Search") }
                    TextButton(onClick = { navController.navigate("account") }) { Text(if (isLoggedIn) "Account" else "Sign in") }
                }
            }

            // Login choice: Google (YouTube Music sync) or local-only.
            if (!isLoggedIn && !useLocal) {
                item(key = "login_choice") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Continue with Google", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Sync your YouTube Music playlists, liked songs and history, and personalise your feed in real time.",
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { navController.navigate("login") }) { Text("Continue with Google") }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { setUseLocal(true) }) { Text("Use locally") }
                            }
                        }
                    }
                }
            }

            if (MaterialHomeSection.HERO.id !in hiddenSections && home.hero.isNotEmpty()) {
                item(key = "featured_hero") {
                    SectionTitle("Top Picks for You")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(home.hero, key = { "hero_${it.id}" }) { card ->
                            Column(
                                Modifier.width(320.dp).combinedClickable(
                                    onClick = { open(card.item) },
                                    onLongClick = { menu(card.item) },
                                )
                            ) {
                                Text(card.category.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(card.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(card.subtitle, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Art(card.imageUrl, Modifier.fillMaxWidth().height(210.dp), RoundedCornerShape(16.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (home.shelves.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        if (isLoading || isRefreshing) CircularProgressIndicator()
                        else Text("Couldn't load music right now. Pull down to try again.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            items(home.shelves, key = { it.key }) { shelf ->
                Column {
                    SectionTitle(shelf.title, shelf.strapline)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(shelf.items, key = { "${shelf.key}_${it.id}" }) { item ->
                            val isArtist = item is ArtistItem
                            Column(
                                Modifier.width(if (isArtist) 124.dp else 150.dp).combinedClickable(
                                    onClick = { open(item) },
                                    onLongClick = { menu(item) },
                                ),
                                horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
                            ) {
                                Art(
                                    item.thumbnail,
                                    Modifier.size(if (isArtist) 124.dp else 150.dp),
                                    if (isArtist) CircleShape else RoundedCornerShape(12.dp),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (!isArtist) {
                                    Text(
                                        when (item) {
                                            is SongItem -> item.artists.joinToString { it.name }
                                            is AlbumItem -> item.artists?.joinToString { it.name }.orEmpty()
                                            is PlaylistItem -> item.author?.name.orEmpty()
                                            else -> ""
                                        },
                                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, strapline: String = "") {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        if (strapline.isNotBlank()) {
            Text(strapline.uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Art(url: String?, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
    )
}
