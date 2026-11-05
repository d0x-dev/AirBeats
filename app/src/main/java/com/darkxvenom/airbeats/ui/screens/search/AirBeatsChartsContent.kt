package com.darkxvenom.airbeats.ui.screens.search

import androidx.compose.ui.res.stringResource

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.charts.ChartAlbum
import com.darkxvenom.airbeats.charts.ChartArtist
import com.darkxvenom.airbeats.charts.ChartRegionKey
import com.darkxvenom.airbeats.charts.ChartRegionSheet
import com.darkxvenom.airbeats.charts.ChartRegionSlugToName
import com.darkxvenom.airbeats.charts.ChartTrack
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.viewmodels.ChartsViewModel
import kotlinx.coroutines.launch
import java.util.Locale

fun LazyListScope.airbeatsChartsItems(
    navController: NavController,
    viewModel: ChartsViewModel,
) {
    item(key = "airbeats_charts_core") {
        AirBeatsChartsEmbedView(
            navController = navController,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AirBeatsChartsEmbedView(
    navController: NavController,
    viewModel: ChartsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val chartTracks by viewModel.chartTracks.collectAsState()
    val chartArtists by viewModel.chartArtists.collectAsState()
    val chartAlbums by viewModel.chartAlbums.collectAsState()
    val chartVideos by viewModel.chartVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isManualLoading by viewModel.isManualLoading.collectAsState()

    val uriHandler = LocalUriHandler.current
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current

    var regionCode by rememberPreference(key = ChartRegionKey, defaultValue = "system")
    var showRegionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(regionCode) {
        viewModel.refresh(regionCode)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if ((isLoading || isManualLoading) && chartTracks == null && chartArtists == null && chartAlbums == null && chartVideos == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        }

        chartTracks?.let { tracks ->
            TrendingAppleMusicSection(
                tracks = tracks,
                countryCode = regionCode,
                onRegionClick = { showRegionSheet = true },
                onTrackClick = { track ->
                    Toast.makeText(context, "Loading ${track.title}...", Toast.LENGTH_SHORT).show()
                    viewModel.playTrack(track, playerConnection)
                },
                onMoreClick = {
                    val code = if (regionCode == "system") {
                        Locale.getDefault().country.lowercase().ifEmpty { "us" }
                    } else {
                        regionCode.lowercase()
                    }
                    uriHandler.openUri("https://music.apple.com/$code/charts")
                }
            )
        }

        chartArtists?.let { artists ->
            TopArtistsSection(
                artists = artists,
                onArtistClick = { artist ->
                    Toast.makeText(context, "Loading ${artist.name}...", Toast.LENGTH_SHORT).show()
                    viewModel.navigateToArtist(artist, navController)
                }
            )
        }

        chartAlbums?.let { albums ->
            TrendingAlbumsSection(
                albums = albums,
                onAlbumClick = { album ->
                    Toast.makeText(context, "Loading ${album.title}...", Toast.LENGTH_SHORT).show()
                    viewModel.navigateToAlbum(album, navController)
                },
                onMoreClick = {
                    val code = if (regionCode == "system") {
                        Locale.getDefault().country.lowercase().ifEmpty { "us" }
                    } else {
                        regionCode.lowercase()
                    }
                    uriHandler.openUri("https://music.apple.com/$code/charts/albums")
                }
            )
        }

        chartVideos?.let { videos ->
            TrendingVideosSection(
                videos = videos,
                onVideoClick = { video ->
                    Toast.makeText(context, "Loading video ${video.title}...", Toast.LENGTH_SHORT).show()
                    viewModel.playVideo(video, playerConnection)
                },
                onMoreClick = {
                    val code = if (regionCode == "system") {
                        Locale.getDefault().country.lowercase().ifEmpty { "us" }
                    } else {
                        regionCode.lowercase()
                    }
                    uriHandler.openUri("https://music.apple.com/$code/charts/videos")
                }
            )
        }

        if (chartTracks == null && chartArtists == null && chartAlbums == null && chartVideos == null && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp, bottom = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.ui_sweep_no_charts_available_at_the_moment),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = { viewModel.refresh(regionCode, force = true) }) {
                        Text(stringResource(R.string.ui_text_refresh))
                    }
                }
            }
        }

        if (chartTracks != null || chartArtists != null || chartAlbums != null || chartVideos != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.ui_text_data_from_apple_music),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = stringResource(R.string.ui_text_airbeats),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showRegionSheet) {
        ChartRegionSheet(
            currentRegionSlug = regionCode,
            onRegionSelected = { selected ->
                regionCode = selected
                showRegionSheet = false
            },
            onDismiss = { showRegionSheet = false }
        )
    }
}

@Composable
fun TrendingAppleMusicSection(
    tracks: List<ChartTrack>,
    countryCode: String,
    onRegionClick: () -> Unit,
    onTrackClick: (ChartTrack) -> Unit,
    onMoreClick: () -> Unit,
) {
    if (tracks.isEmpty()) return
    val displayTracks = tracks.take(29)
    val totalItems = displayTracks.size + 1
    val pagerState = rememberPagerState(pageCount = { (totalItems + 4) / 5 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ui_sweep_apple_music_top_100),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onRegionClick() }
        ) {
            Text(
                text = ChartRegionSlugToName[countryCode] ?: "System Default",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.globe_search),
                contentDescription = stringResource(R.string.ui_a11y_select_region),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(tween(300, easing = FastOutSlowInEasing))
        ) { page ->
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val startIdx = page * 5
                val endIdx = minOf(startIdx + 5, totalItems)

                for (i in startIdx until endIdx) {
                    val isMoreCard = i == 29
                    val isTop = i == startIdx
                    val isBottom = i == endIdx - 1

                    val shape = when {
                        isTop && isBottom -> RoundedCornerShape(24.dp)
                        isTop -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        isBottom -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(4.dp)
                    }

                    if (isMoreCard) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onMoreClick() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.globe_search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.ui_text_view_more_on_apple_music),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (i < displayTracks.size) {
                        val track = displayTracks[i]

                        Surface(
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .clickable { onTrackClick(track) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 14.dp)
                                ) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = track.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "#${track.rank}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val playCount = remember(track.rank) {
                                            val base = 2_500_000 / (track.rank + 2)
                                            if (base >= 1_000_000) String.format("%.1fM plays", base / 1_000_000f)
                                            else String.format("%dk plays", base / 1_000)
                                        }
                                        Text(
                                            text = playCount,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                if (track.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = track.thumbnailUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(painterResource(R.drawable.chevron_leftpx), "Previous")
                }
                Text(
                    text = "${pagerState.currentPage + 1} of ${pagerState.pageCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    enabled = pagerState.currentPage < pagerState.pageCount - 1
                ) {
                    Icon(painterResource(R.drawable.chevron_right_px), "Next")
                }
            }
        }
    }
}

@Composable
fun TopArtistsSection(
    artists: List<ChartArtist>,
    onArtistClick: (ChartArtist) -> Unit,
) {
    if (artists.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ui_text_trending_artists),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            items(artists) { artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onArtistClick(artist) }
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = artist.thumbnailUrl,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .offset((-4).dp, (-4).dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = artist.rank.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.surface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val playCount = remember(artist.rank) {
                        val base = 15_000_000 / (artist.rank + 8)
                        if (base >= 1_000_000) String.format("%.1fM plays", base / 1_000_000f)
                        else String.format("%dk plays", base / 1_000)
                    }
                    Text(
                        text = playCount,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingAlbumsSection(
    albums: List<ChartAlbum>,
    onAlbumClick: (ChartAlbum) -> Unit,
    onMoreClick: () -> Unit,
) {
    if (albums.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ui_text_trending_albums),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 28.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp)
        ) {
            items(albums) { album ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAlbumClick(album) }
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        AsyncImage(
                            model = album.thumbnailUrl,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Surface(
                            modifier = Modifier
                                .size(28.dp)
                                .offset((-4).dp, (-4).dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = album.rank.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.surface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(100.dp)
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onMoreClick() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.globe_search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.ui_text_more),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingVideosSection(
    videos: List<ChartTrack>,
    onVideoClick: (ChartTrack) -> Unit,
    onMoreClick: () -> Unit,
) {
    if (videos.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ui_text_trending_music_videos),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.ui_text_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onMoreClick() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(videos) { video ->
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onVideoClick(video) }
                ) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                    startY = 180f
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = video.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
