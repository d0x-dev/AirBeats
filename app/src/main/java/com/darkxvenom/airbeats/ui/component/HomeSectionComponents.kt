package com.darkxvenom.airbeats.ui.component

import com.darkxvenom.airbeats.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.db.entities.LocalItem
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.PlayerConnection
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.net.URLEncoder

val HOME_TASTE_TAGS = listOf(
    "Pop", "Rock", "Hip-Hop", "Chill", "Electronic", "Indie",
    "Lo-Fi", "Bollywood", "Acoustic", "Jazz", "Metal", "Classical", "R&B"
)

enum class HomeThemeStyle {
    CLASSIC, SPOTIFY, APPLE, NEW_CLASSIC, MATERIAL
}

/**
 * Shared helper for Infinite Radio that shuffles and picks a fresh random seed on every click,
 * avoiding the currently playing song and recent radio seeds.
 */
object InfiniteRadioHelper {
    private val recentSeedIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun playShuffledRadio(
        playerConnection: PlayerConnection,
        currentSongId: String?,
        quickPicks: List<Song>?,
        forgottenFavorites: List<Song>? = null,
        keepListening: List<LocalItem>? = null,
        homeSongs: List<SongItem>? = null,
    ) {
        val pool = mutableListOf<MediaMetadata>()
        quickPicks?.forEach { pool.add(it.toMediaMetadata()) }
        forgottenFavorites?.forEach { pool.add(it.toMediaMetadata()) }
        keepListening?.filterIsInstance<Song>()?.forEach { pool.add(it.toMediaMetadata()) }
        homeSongs?.forEach { pool.add(it.toMediaMetadata()) }

        if (pool.isEmpty()) return

        val candidates = pool
            .filter { it.id != currentSongId && it.id !in recentSeedIds }
            .ifEmpty { pool.filter { it.id != currentSongId } }
            .ifEmpty { pool }

        val seed = candidates.shuffled().firstOrNull() ?: return

        recentSeedIds.add(seed.id)
        if (recentSeedIds.size > 20) {
            recentSeedIds.clear()
            recentSeedIds.add(seed.id)
        }

        playerConnection.playQueue(YouTubeQueue.radio(seed))
    }
}

/**
 * Reusable Taste Strip for genre / mood exploration across all home screen styles.
 */
@Composable
fun HomeTasteStrip(
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: HomeThemeStyle = HomeThemeStyle.CLASSIC
) {
    val containerColor = when (style) {
        HomeThemeStyle.SPOTIFY -> Color(0xFF282828)
        HomeThemeStyle.APPLE -> Color.White.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val labelColor = when (style) {
        HomeThemeStyle.SPOTIFY -> Color.White
        HomeThemeStyle.APPLE -> Color.White.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(HOME_TASTE_TAGS) { tag ->
            FilterChip(
                selected = false,
                onClick = { onTagClick(tag) },
                label = {
                    Text(
                        text = tag,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = containerColor,
                    labelColor = labelColor
                ),
                border = when (style) {
                    HomeThemeStyle.APPLE -> FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = Color.White.copy(alpha = 0.15f)
                    )
                    HomeThemeStyle.SPOTIFY -> FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = Color.Gray.copy(alpha = 0.4f)
                    )
                    else -> null
                }
            )
        }
    }
}

/**
 * Universal Hero Greeting Banner with instant infinite radio.
 */
@Composable
fun UniversalHomeHeroBanner(
    title: String,
    subtitle: String,
    onPlayRadio: () -> Unit,
    modifier: Modifier = Modifier,
    style: HomeThemeStyle = HomeThemeStyle.CLASSIC,
) {
    val cardShape = RoundedCornerShape(22.dp)

    val backgroundModifier = when (style) {
        HomeThemeStyle.SPOTIFY -> Modifier.background(
            Brush.horizontalGradient(
                listOf(Color(0xFF1E3224), Color(0xFF121212))
            )
        )
        HomeThemeStyle.APPLE -> Modifier.background(
            Brush.horizontalGradient(
                listOf(Color(0xFF2C1B33), Color(0xFF16141D))
            )
        )
        HomeThemeStyle.NEW_CLASSIC -> Modifier.background(
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                )
            )
        )
        else -> Modifier.background(
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer
                )
            )
        )
    }

    val accentColor = when (style) {
        HomeThemeStyle.SPOTIFY -> Color(0xFF1ED760)
        HomeThemeStyle.APPLE -> Color(0xFFFA2D48)
        else -> MaterialTheme.colorScheme.primary
    }

    val onAccentColor = when (style) {
        HomeThemeStyle.SPOTIFY -> Color.Black
        HomeThemeStyle.APPLE -> Color.White
        else -> MaterialTheme.colorScheme.onPrimary
    }

    val textColor = when (style) {
        HomeThemeStyle.SPOTIFY, HomeThemeStyle.APPLE -> Color.White
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .then(backgroundModifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Radio,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ui_text_infinite_radio),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            FilledIconButton(
                onClick = onPlayRadio,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accentColor,
                    contentColor = onAccentColor
                ),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.play_radio),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/**
 * Universal Artist Spotlight Card.
 */
@Composable
fun UniversalArtistSpotlightCard(
    artistName: String,
    artistId: String?,
    thumbnailUrl: String?,
    fallbackThumbnail: String? = null,
    onOpenArtist: () -> Unit,
    onPlayRadio: () -> Unit,
    modifier: Modifier = Modifier,
    style: HomeThemeStyle = HomeThemeStyle.CLASSIC
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

    val cardColor = when (style) {
        HomeThemeStyle.SPOTIFY -> Color(0xFF1E1E1E)
        HomeThemeStyle.APPLE -> Color.White.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val primaryColor = when (style) {
        HomeThemeStyle.SPOTIFY -> Color(0xFF1ED760)
        HomeThemeStyle.APPLE -> Color(0xFFFA2D48)
        else -> MaterialTheme.colorScheme.primary
    }

    val textColor = when (style) {
        HomeThemeStyle.SPOTIFY, HomeThemeStyle.APPLE -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.artist_spotlight),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.ui_text_endless_radio_tuned_to_this_artist),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primaryColor,
                        modifier = Modifier.clickable(onClick = onPlayRadio)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = if (style == HomeThemeStyle.SPOTIFY) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.ui_text_start_radio),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (style == HomeThemeStyle.SPOTIFY) Color.Black else Color.White
                            )
                        }
                    }

                    if (artistId != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = textColor.copy(alpha = 0.12f),
                            modifier = Modifier.clickable(onClick = onOpenArtist)
                        ) {
                            Text(
                                text = stringResource(R.string.ui_text_view_artist),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                shape = CircleShape,
                color = textColor.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(76.dp)
                    .clickable(onClick = onOpenArtist)
            ) {
                if (!finalThumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = finalThumbnail.highQualityThumbnail(),
                        contentDescription = artistName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Universal Top Artists Row.
 */
@Composable
fun UniversalTopArtistsRow(
    artists: List<Triple<com.darkxvenom.airbeats.db.entities.ArtistEntity, String?, String?>>,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: HomeThemeStyle = HomeThemeStyle.CLASSIC
) {
    val textColor = when (style) {
        HomeThemeStyle.SPOTIFY, HomeThemeStyle.APPLE -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        itemsIndexed(artists, key = { _, item -> item.first.id }) { index, (artist, thumb, fallbackThumb) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(82.dp)
                    .clickable { onArtistClick(artist.id) }
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Surface(
                        shape = CircleShape,
                        color = textColor.copy(alpha = 0.08f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        val finalThumb = thumb ?: fallbackThumb
                        if (!finalThumb.isNullOrBlank()) {
                            AsyncImage(
                                model = finalThumb.highQualityThumbnail(),
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.People,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    if (index < 3) {
                        Surface(
                            shape = CircleShape,
                            color = when (style) {
                                HomeThemeStyle.SPOTIFY -> Color(0xFF1ED760)
                                HomeThemeStyle.APPLE -> Color(0xFFFA2D48)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (style == HomeThemeStyle.SPOTIFY) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Universal Quick Access Tiles for Liked Songs, Mix, History, and Stats.
 */
@Composable
fun UniversalQuickAccessTiles(
    onLikedClick: () -> Unit,
    onMixClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onStatsClick: () -> Unit,
    isGeneratingMix: Boolean = false,
    modifier: Modifier = Modifier,
    style: HomeThemeStyle = HomeThemeStyle.CLASSIC
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickAccessTileItem(
                title = stringResource(R.string.ui_heading_liked_songs),
                icon = Icons.Filled.Favorite,
                containerColor = when (style) {
                    HomeThemeStyle.SPOTIFY -> Color(0xFF282828)
                    HomeThemeStyle.APPLE -> Color.White.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = when (style) {
                    HomeThemeStyle.SPOTIFY, HomeThemeStyle.APPLE -> Color.White
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                },
                onClick = onLikedClick,
                modifier = Modifier.weight(1f)
            )
            QuickAccessTileItem(
                title = if (isGeneratingMix) "Mixing..." else "Endless Mix",
                icon = Icons.Filled.AutoAwesome,
                containerColor = when (style) {
                    HomeThemeStyle.SPOTIFY -> Color(0xFF1E3224)
                    HomeThemeStyle.APPLE -> Color(0xFF3B1E28)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = when (style) {
                    HomeThemeStyle.SPOTIFY -> Color(0xFF1ED760)
                    HomeThemeStyle.APPLE -> Color(0xFFFA2D48)
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                },
                onClick = onMixClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickAccessTileItem(
                title = stringResource(R.string.history),
                icon = Icons.Filled.History,
                containerColor = when (style) {
                    HomeThemeStyle.SPOTIFY -> Color(0xFF282828)
                    HomeThemeStyle.APPLE -> Color.White.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                },
                contentColor = when (style) {
                    HomeThemeStyle.SPOTIFY, HomeThemeStyle.APPLE -> Color.White
                    else -> MaterialTheme.colorScheme.onTertiaryContainer
                },
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f)
            )
            QuickAccessTileItem(
                title = stringResource(R.string.stats),
                icon = Icons.Filled.TrendingUp,
                containerColor = when (style) {
                    HomeThemeStyle.SPOTIFY -> Color(0xFF282828)
                    HomeThemeStyle.APPLE -> Color.White.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                contentColor = when (style) {
                    HomeThemeStyle.SPOTIFY, HomeThemeStyle.APPLE -> Color.White
                    else -> MaterialTheme.colorScheme.onSurface
                },
                onClick = onStatsClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickAccessTileItem(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.height(54.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
