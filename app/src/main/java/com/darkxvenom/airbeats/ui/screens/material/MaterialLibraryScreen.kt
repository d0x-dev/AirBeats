package com.darkxvenom.airbeats.ui.screens.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.LibraryFilter
import com.darkxvenom.airbeats.ui.component.CreatePlaylistDialog
import com.darkxvenom.airbeats.ui.screens.library.LibraryAlbumsScreen
import com.darkxvenom.airbeats.ui.screens.library.LibraryArtistsScreen
import com.darkxvenom.airbeats.ui.screens.library.LibraryPlaylistsScreen
import com.darkxvenom.airbeats.ui.screens.library.LibrarySongsScreen

/**
 * Material 3 Expressive Library Screen companion.
 */
@Composable
fun MaterialLibraryScreen(
    navController: NavController,
) {
    var filterType by remember { mutableStateOf(LibraryFilter.PLAYLISTS) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val playerInsets = LocalPlayerAwareWindowInsets.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val contentInsets = WindowInsets(
        playerInsets.getLeft(density, layoutDirection),
        0,
        playerInsets.getRight(density, layoutDirection),
        playerInsets.getBottom(density),
    )

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false }
        )
    }

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        com.darkxvenom.airbeats.ui.component.ScreenAdaptiveBackground(
            artworkUrl = mediaMetadata?.thumbnailUrl
        )
        CompositionLocalProvider(LocalPlayerAwareWindowInsets provides contentInsets) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 115.dp,
                    )
            ) {
                when (filterType) {
                    LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(
                        navController = navController,
                        filterContent = {}
                    )
                    LibraryFilter.SONGS -> LibrarySongsScreen(
                        navController = navController,
                        onDeselect = { filterType = LibraryFilter.PLAYLISTS }
                    )
                    LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                        navController = navController,
                        onDeselect = { filterType = LibraryFilter.PLAYLISTS }
                    )
                    LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                        navController = navController,
                        onDeselect = { filterType = LibraryFilter.PLAYLISTS }
                    )
                    else -> Unit
                }
            }
        }

        // Top Header with Title and Filter Chips
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
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.library_music_filled),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.android_auto_library),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(onClick = { navController.navigate("generator") }) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(R.string.generator),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.ui_a11y_create_playlist),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf(
                        LibraryFilter.PLAYLISTS to "Playlists",
                        LibraryFilter.SONGS to "Songs",
                        LibraryFilter.ALBUMS to "Albums",
                        LibraryFilter.ARTISTS to "Artists"
                    )
                    items(filters) { (type, label) ->
                        val isSelected = filterType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterType = type },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    }
}
