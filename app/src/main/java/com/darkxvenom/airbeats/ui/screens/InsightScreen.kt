package com.darkxvenom.airbeats.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.viewmodels.StatsViewModel
import kotlinx.coroutines.launch
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.db.entities.SongWithStats
import com.darkxvenom.airbeats.db.entities.Artist
import com.darkxvenom.airbeats.db.entities.Album
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val totalHours by viewModel.totalListenHours.collectAsState(initial = 0.0)

    val pages = listOf(
        InsightPage.Intro,
        InsightPage.TopSong,
        InsightPage.TopArtist,
        InsightPage.TopAlbum,
        InsightPage.ListeningTime,
        InsightPage.Summary
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    var isPaused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            coroutineScope.launch {
                                if (offset.x < size.width / 3f) {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                } else {
                                    if (pagerState.currentPage < pages.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    } else {
                                        navController.navigateUp()
                                    }
                                }
                            }
                        }
                    )
                },
            userScrollEnabled = false
        ) { page ->
            when (pages[page]) {
                InsightPage.Intro -> IntroPage()
                InsightPage.TopSong -> TopSongPage(
                    mostPlayedSongs.firstOrNull(),
                    mostPlayedSongsStats.firstOrNull()
                )
                InsightPage.TopArtist -> TopArtistPage(mostPlayedArtists.firstOrNull())
                InsightPage.TopAlbum -> TopAlbumPage(mostPlayedAlbums.firstOrNull())
                InsightPage.ListeningTime -> ListeningTimePage(totalHours)
                InsightPage.Summary -> SummaryPage(mostPlayedSongs.take(5), navController)
            }
        }

        // Story Progress Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            pages.forEachIndexed { index, _ ->
                StoryIndicator(
                    modifier = Modifier.weight(1f),
                    isActive = index == pagerState.currentPage,
                    isCompleted = index < pagerState.currentPage,
                    isPaused = isPaused,
                    onFinish = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < pages.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                navController.navigateUp()
                            }
                        }
                    }
                )
            }
        }

        // Close Button
        IconButton(
            onClick = { navController.navigateUp() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp, end = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.close),
                contentDescription = stringResource(R.string.close),
                tint = Color.White
            )
        }
    }
}

@Composable
fun StoryIndicator(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    isCompleted: Boolean,
    isPaused: Boolean,
    onFinish: () -> Unit
) {
    val progress = remember { Animatable(if (isCompleted) 1f else 0f) }

    LaunchedEffect(isActive, isCompleted, isPaused) {
        if (isCompleted) {
            progress.snapTo(1f)
        } else if (isActive) {
            if (isPaused) {
                progress.stop()
            } else {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = (5000 * (1f - progress.value)).toInt(),
                        easing = LinearEasing
                    )
                )
                onFinish()
            }
        } else {
            progress.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .height(4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

enum class InsightPage { Intro, TopSong, TopArtist, TopAlbum, ListeningTime, Summary }

@Composable
fun IntroPage() {
    val currentYear = LocalDateTime.now().year
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1DB954), Color(0xFF191414))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.auto_awesome),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
            )
            Text(
                text = stringResource(R.string.ui_dynamic_current_year, currentYear),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = stringResource(R.string.ui_sweep_wrapped_up),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun TopSongPage(song: Song?, songStats: SongWithStats?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF8D67AB)),
        contentAlignment = Alignment.Center
    ) {
        if (song != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_sweep_one_song_ruled_them_all),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = song.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                if (songStats != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "Played ${songStats.songCountListened} times",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        } else {
            Text(stringResource(R.string.keep_listening_top_song), color = Color.White)
        }
    }
}

@Composable
fun TopArtistPage(artist: Artist?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE1118C)),
        contentAlignment = Alignment.Center
    ) {
        if (artist != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_sweep_you_couldn_t_get_enough_of),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                AsyncImage(
                    model = artist.artist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = artist.artist.name,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "${artist.songCount} streams",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            Text(stringResource(R.string.keep_listening_top_artist), color = Color.White)
        }
    }
}

@Composable
fun TopAlbumPage(album: Album?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFBC5900)),
        contentAlignment = Alignment.Center
    ) {
        if (album != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_text_your_album_on_repeat),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                AsyncImage(
                    model = album.album.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = album.album.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(32.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "${album.songCountListened ?: 0} streams",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        } else {
            Text(stringResource(R.string.keep_listening_top_album), color = Color.White)
        }
    }
}

@Composable
fun ListeningTimePage(hours: Double) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E3264)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.ui_text_you_spent),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = "${hours.toInt()}",
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1DB954)
            )
            Text(
                text = stringResource(R.string.ui_text_hours),
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.ui_sweep_listening_to_music_on_airbeats),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SummaryPage(topSongs: List<Song>, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFD84000))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 64.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.ui_sweep_your_top_5_songs),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            topSongs.forEachIndexed { index, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.width(32.dp)
                    )
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = song.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = song.artists.joinToString(", ") { it.name },
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = { navController.navigateUp() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
            ) {
                Text(stringResource(R.string.done), color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
