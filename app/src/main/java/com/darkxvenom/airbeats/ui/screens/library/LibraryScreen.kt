package com.darkxvenom.airbeats.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.LibraryFilter
import com.darkxvenom.airbeats.ui.component.ChipsRow
import com.darkxvenom.airbeats.ui.component.VerticalFastScroller

@Composable
fun LibraryScreen(navController: NavController) {

    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var filterType by remember { mutableStateOf(LibraryFilter.LIBRARY) }
    val lazyListState = rememberLazyListState()
    var showSpotifyDialog by remember { mutableStateOf(false) }

    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                    LibraryFilter.LOCAL to stringResource(R.string.filter_local),
                ),
                currentValue = filterType,
                onValueUpdate = {
                    filterType =
                        if (filterType == it) {
                            LibraryFilter.LIBRARY
                        } else {
                            it
                        }
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🎵 Blur background
        val artworkUrl = mediaMetadata?.thumbnailUrl

        artworkUrl?.let { imageUrl ->

            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(90.dp)
            )

            val isDarkTheme =
                MaterialTheme.colorScheme.background.luminance() < 0.5f

            val overlayBrush = if (isDarkTheme) {
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )
        }

        // 🔝 Top Header
        Text(
            text = "Library",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .align(Alignment.TopStart)
        )

        // 📚 Content
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = 80.dp,
            endContentPadding = 0.dp
        ) {
            when (filterType) {
                LibraryFilter.LIBRARY ->
                    LibraryMixScreen(
                        navController = navController,
                        filterContent = filterContent,
                        onLocalClick = { filterType = LibraryFilter.LOCAL },
                        onImportPlaylistClick = { showSpotifyDialog = true }
                    )

                LibraryFilter.PLAYLISTS ->
                    LibraryPlaylistsScreen(
                        navController = navController,
                        filterContent = filterContent,
                        onLocalClick = { filterType = LibraryFilter.LOCAL }
                    )

                LibraryFilter.SONGS ->
                    LibrarySongsScreen(
                        navController,
                        { filterType = LibraryFilter.LIBRARY }
                    )

                LibraryFilter.ALBUMS ->
                    LibraryAlbumsScreen(
                        navController,
                        { filterType = LibraryFilter.LIBRARY }
                    )

                LibraryFilter.ARTISTS ->
                    LibraryArtistsScreen(
                        navController,
                        { filterType = LibraryFilter.LIBRARY }
                    )

                // ── NEW: Local songs filter ───────────────────────────────────
                LibraryFilter.LOCAL ->
                    LocalSongsScreen(navController = navController)
            }
        }

        if (showSpotifyDialog) {
            SpotifyImportDialog(onDismiss = { showSpotifyDialog = false })
        }
    }
}