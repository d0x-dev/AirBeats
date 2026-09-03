package com.darkxvenom.airbeats.ui.component

import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.entities.LyricsEntity
import com.darkxvenom.airbeats.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.lyrics.LyricsEntry
import com.darkxvenom.airbeats.lyrics.LyricsUtils.parseLyrics
import com.darkxvenom.airbeats.ui.menu.LyricsMenu
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.makeTimeString
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private val SpotifyLyricsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

@Composable
fun SpotifyLyrics(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val context = LocalContext.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val serviceLyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    var fetchedLyricsEntity by remember { mutableStateOf<LyricsEntity?>(null) }
    val currentLyricsEntity = fetchedLyricsEntity ?: serviceLyricsEntity
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var lyricsSurfaceColor by remember { mutableStateOf(Color(0xFF311000)) }

    val lyrics = currentLyricsEntity?.lyrics?.trim()
    val lines = remember(lyrics) {
        when {
            lyrics.isNullOrBlank() || lyrics == LYRICS_NOT_FOUND -> emptyList()
            lyrics.startsWith("[") -> parseLyrics(lyrics)
            else -> lyrics.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapIndexed { index, line -> LyricsEntry(index * 2500L, line) }
        }
    }
    val activeLineIndex = remember(lines, position) {
        lines.indexOfLast { it.time <= position }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()
    val safeDuration = duration.takeIf { it > 0 } ?: 0L
    val shownPosition = sliderPosition ?: position

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(mediaMetadata?.thumbnailUrl) {
        val thumbnailUrl = mediaMetadata?.thumbnailUrl?.highQualityThumbnail() ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val bitmap = runCatching {
                (ImageLoader(context).execute(
                    ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .allowHardware(false)
                        .build()
                ).drawable as? BitmapDrawable)?.bitmap
            }.getOrNull() ?: return@withContext

            val palette = Palette.from(bitmap).generate()
            val swatch = palette.darkVibrantSwatch
                ?: palette.darkMutedSwatch
                ?: palette.vibrantSwatch
                ?: palette.dominantSwatch
            val color = swatch?.rgb?.let(::Color) ?: Color(0xFF311000)
            withContext(Dispatchers.Main) {
                lyricsSurfaceColor = color
            }
        }
    }

    LaunchedEffect(mediaMetadata?.id, serviceLyricsEntity) {
        val metadata = mediaMetadata ?: return@LaunchedEffect
        if (!serviceLyricsEntity?.lyrics.isNullOrBlank() && serviceLyricsEntity?.lyrics != LYRICS_NOT_FOUND) {
            fetchedLyricsEntity = serviceLyricsEntity
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val entity = runCatching { database.getLyrics(metadata.id) }.getOrNull()
                ?.takeUnless { it.lyrics.isBlank() || it.lyrics == LYRICS_NOT_FOUND }
                ?: runCatching {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.darkxvenom.airbeats.di.LyricsHelperEntryPoint::class.java,
                    )
                    val fetchedLyrics = entryPoint.lyricsHelper().getLyrics(metadata)
                    LyricsEntity(
                        id = metadata.id,
                        lyrics = fetchedLyrics.takeUnless { it.isBlank() } ?: LYRICS_NOT_FOUND,
                    ).also { fetched ->
                        runCatching {
                            database.query {
                                upsert(fetched)
                            }
                        }
                    }
                }.getOrNull()

            withContext(Dispatchers.Main) {
                fetchedLyricsEntity = entity
            }
        }
    }

    LaunchedEffect(playbackState) {
        while (isActive) {
            position = sliderPosition ?: playerConnection.player.currentPosition
            duration = playerConnection.player.duration
            delay(16)
        }
    }

    LaunchedEffect(activeLineIndex, lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(activeLineIndex.coerceAtMost(lines.lastIndex))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = mediaMetadata?.thumbnailUrl?.highQualityThumbnail(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(120.dp)
                .alpha(0.34f)
                .scale(1.16f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            lyricsSurfaceColor.copy(alpha = 0.38f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom))
                .padding(bottom = 44.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title.orEmpty(),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = SpotifyLyricsFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpotifyLyricsFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal),
                        color = Color.White.copy(alpha = 0.68f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SpotifyLyricsIconButton(R.drawable.favorite_border) {}
                SpotifyLyricsIconButton(R.drawable.more_vert) {
                    mediaMetadata?.let { metadata ->
                        menuState.show {
                            LyricsMenu(
                                lyricsProvider = { currentLyricsEntity },
                                mediaMetadataProvider = { metadata },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                }
            }

            Crossfade(targetState = lines.isNotEmpty(), animationSpec = tween(220), label = "spotifyLyrics") { hasLyrics ->
                if (hasLyrics) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(26.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 50.dp)
                    ) {
                        items(lines.size) { index ->
                            val isCurrent = index == activeLineIndex
                            val isPast = index < activeLineIndex
                            Text(
                                text = lines[index].text,
                                style = if (isCurrent) {
                                    MaterialTheme.typography.displaySmall.copy(fontFamily = SpotifyLyricsFontFamily, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    MaterialTheme.typography.headlineMedium.copy(fontFamily = SpotifyLyricsFontFamily, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                                },
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    isCurrent -> Color.White
                                    isPast -> Color.White.copy(alpha = 0.72f)
                                    else -> Color.White.copy(alpha = 0.34f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = lines[index].time > 0) {
                                        playerConnection.player.seekTo(lines[index].time)
                                    }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.lyrics_not_found),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.72f),
                        )
                    }
                }
            }

            Slider(
                value = shownPosition.coerceIn(0L, safeDuration).toFloat(),
                valueRange = 0f..safeDuration.coerceAtLeast(1L).toFloat(),
                onValueChange = { sliderPosition = it.toLong() },
                onValueChangeFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                ),
                modifier = Modifier.padding(horizontal = 40.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
            ) {
                Text(
                    text = makeTimeString(shownPosition.coerceAtLeast(0L)),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = SpotifyLyricsFontFamily, fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.64f),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = makeTimeString(safeDuration),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = SpotifyLyricsFontFamily, fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.64f),
                )
            }

        }
    }
}

@Composable
private fun SpotifyLyricsIconButton(
    icon: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White.copy(alpha = if (enabled) 1f else 0.35f)),
            modifier = Modifier.size(25.dp)
        )
    }
}





