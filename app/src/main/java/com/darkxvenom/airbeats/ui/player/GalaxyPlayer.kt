package com.darkxvenom.airbeats.ui.player

import androidx.compose.ui.res.stringResource

import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.ui.component.PlayerSliderTrack
import com.darkxvenom.airbeats.constants.SliderStyle
import com.darkxvenom.airbeats.constants.SliderStyleKey
import me.saket.squiggles.SquigglySlider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.lyrics.LyricsEntry
import com.darkxvenom.airbeats.lyrics.LyricsUtils.parseLyrics
import com.darkxvenom.airbeats.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sign

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import com.darkxvenom.airbeats.constants.ShowGalaxySliderKey
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.utils.makeTimeString

@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalaxyPlayer(
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCollapse: () -> Unit,
    onMenuClick: () -> Unit,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onQueueClick: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit = {},
    onShareClick: () -> Unit,
    shuffleModeEnabled: Boolean = false,
    onShuffleClick: () -> Unit = {},
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    onRepeatClick: () -> Unit = {},
    onOpenFullscreenLyrics: () -> Unit = {},
    queueWindows: List<MediaItem> = emptyList(),
    currentWindowIndex: Int = 0,
    lyrics: String? = null,
    state: BottomSheetState
) {
    val scope = rememberCoroutineScope()
    val showGalaxySlider by rememberPreference(ShowGalaxySliderKey, defaultValue = true)
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)
    var isDraggingSlider by remember { mutableStateOf(false) }
    var draggingSliderValue by remember { mutableStateOf(0f) }
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Lyrics, 1: Browse, 2: Queue
    var visibleTab by remember { mutableIntStateOf(1) }
    var browseExpanded by remember { mutableStateOf(true) }
    val browseSpread by animateFloatAsState(
        targetValue = if (visibleTab == 1 && browseExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "browseSpread"
    )

    fun selectTab(tab: Int) {
        if (tab == selectedTab) return
        selectedTab = tab
        scope.launch {
            if (visibleTab == 1 && tab != 1) {
                browseExpanded = false
                delay(260)
            }
            visibleTab = tab
            if (tab == 1) {
                browseExpanded = false
                delay(40)
                browseExpanded = true
            }
        }
    }

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

    LaunchedEffect(activeLineIndex, lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(activeLineIndex.coerceAtMost(lines.lastIndex))
        }
    }

    // Twinkling fluid background
    val infiniteTransition = rememberInfiniteTransition(label = "fluid")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = -240f,
        targetValue = 1120f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "xOffset"
    )
    val yOffset by infiniteTransition.animateFloat(
        initialValue = 140f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(6900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "yOffset"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Black,
            Color(0xFF020914),
            Color.Black
        )
    )

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush).bottomSheetDraggable(state)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF5AF6FF).copy(alpha = 0.26f * glowPulse),
                            Color(0xFF006DFF).copy(alpha = 0.18f * glowPulse),
                            Color.Transparent
                        ),
                        center = Offset(xOffset, yOffset),
                        radius = 820f
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF7A1A).copy(alpha = 0.18f * (1.15f - glowPulse)),
                            Color(0xFF7D2DFF).copy(alpha = 0.16f * glowPulse),
                            Color.Transparent
                        ),
                        center = Offset(980f - xOffset, 760f - yOffset),
                        radius = 720f
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            GalaxyTopPill(
                selectedTab = selectedTab,
                onSelectTab = ::selectTab,
            )

            AnimatedContent(
                targetState = visibleTab,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        initialOffsetX = { it * direction }
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                        targetOffsetX = { -it * direction }
                    )
                },
                label = "galaxyTabSlide",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { tab ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (tab) {
                        1 -> GalaxyBrowseCarousel(
                            mediaMetadata = mediaMetadata,
                            queueWindows = queueWindows,
                            currentWindowIndex = currentWindowIndex,
                            spread = browseSpread,
                            onPlayQueueIndex = onPlayQueueIndex
                        )
                        2 -> GalaxyQueueTab(
                            queueWindows = queueWindows,
                            currentWindowIndex = currentWindowIndex,
                            onPlayQueueIndex = onPlayQueueIndex
                        )
                        else -> GalaxyLyricsTab(
                            lines = lines,
                            activeLineIndex = activeLineIndex,
                            listState = listState,
                            onSeek = onSeek,
                            onSeekFinished = onSeekFinished
                        )
                    }
                }
            }

        // Slider above pill
            if (showGalaxySlider) {
                val safeDuration = duration.takeIf { it > 0 } ?: 1L
                val sliderValue = position.coerceIn(0L, safeDuration).toFloat()
                val displayValue = if (isDraggingSlider) draggingSliderValue else sliderValue
                val valueRange = 0f..safeDuration.toFloat().coerceAtLeast(1f)

                val glassSliderColors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.28f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp)
                ) {
                    when (sliderStyle) {
                        SliderStyle.SQUIGGLY -> SquigglySlider(
                            value = displayValue,
                            valueRange = valueRange,
                            onValueChange = {
                                isDraggingSlider = true
                                draggingSliderValue = it
                                onSeek(it.toLong())
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                onSeekFinished()
                            },
                            colors = glassSliderColors,
                            modifier = Modifier.fillMaxWidth(),
                            squigglesSpec = SquigglySlider.SquigglesSpec(
                                amplitude = if (isPlaying) 2.dp else 0.dp,
                                strokeWidth = 3.dp
                            )
                        )
                        SliderStyle.SLIM -> Slider(
                            value = displayValue,
                            valueRange = valueRange,
                            onValueChange = {
                                isDraggingSlider = true
                                draggingSliderValue = it
                                onSeek(it.toLong())
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                onSeekFinished()
                            },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = glassSliderColors
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SliderStyle.DEFAULT -> Slider(
                            value = displayValue,
                            valueRange = valueRange,
                            onValueChange = {
                                isDraggingSlider = true
                                draggingSliderValue = it
                                onSeek(it.toLong())
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                onSeekFinished()
                            },
                            colors = glassSliderColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = makeTimeString(displayValue.toLong()),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = makeTimeString(safeDuration),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

        // Bottom Pill (Controls)
            LiquidGlassPill(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = stringResource(R.string.ui_a11y_previous),
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(enabled = canSkipPrevious, onClick = onPrevious)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(if (isPlaying && !isLoading) R.drawable.pause else R.drawable.play),
                            contentDescription = stringResource(R.string.ui_a11y_play_pause),
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.ui_a11y_next),
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(enabled = canSkipNext, onClick = onNext)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        painter = painterResource(if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                        tint = if (shuffleModeEnabled) Color.Green else Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onShuffleClick)
                    )

                    Icon(
                        painter = painterResource(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = stringResource(R.string.ui_a11y_repeat),
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color.Green else Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(onClick = onRepeatClick)
                    )
                }
            }
        }
    }
}

@Composable
private fun GalaxyLyricsTab(
    lines: List<LyricsEntry>,
    activeLineIndex: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lines.isEmpty()) {
            Text(
                text = stringResource(R.string.ui_text_no_lyrics_available),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(26.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(lines.size) { index ->
                    val isCurrent = index == activeLineIndex
                    val isPast = index < activeLineIndex
                    Text(
                        text = lines[index].text,
                        style = if (isCurrent) {
                            MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        },
                        color = when {
                            isCurrent -> Color.White
                            isPast -> Color.White.copy(alpha = 0.72f)
                            else -> Color.White.copy(alpha = 0.34f)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = lines[index].time > 0) {
                                onSeek(lines[index].time)
                                onSeekFinished()
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun GalaxyQueueTab(
    queueWindows: List<MediaItem>,
    currentWindowIndex: Int,
    onPlayQueueIndex: (Int) -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 26.dp, vertical = 24.dp)
    ) {
        items(queueWindows.size) { index ->
            val item = queueWindows[index]
            val selected = index == currentWindowIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = if (selected) 0.16f else 0.07f))
                    .border(1.dp, Color.White.copy(alpha = if (selected) 0.20f else 0.08f), RoundedCornerShape(18.dp))
                    .clickable { onPlayQueueIndex(index) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = item.mediaMetadata.artworkUri?.toString(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.mediaMetadata.title?.toString().orEmpty(),
                        color = Color.White,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.mediaMetadata.artist?.toString().orEmpty(),
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun GalaxyBrowseCarousel(
    mediaMetadata: MediaMetadata?,
    queueWindows: List<MediaItem>,
    currentWindowIndex: Int,
    spread: Float,
    onPlayQueueIndex: (Int) -> Unit,
) {
    val animatedIndex by animateFloatAsState(
        targetValue = currentWindowIndex.toFloat(),
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow, dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy),
        label = "carouselIndex"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        val windowRadius = 3
        val startIndex = (currentWindowIndex - windowRadius).coerceAtLeast(0)
        val endIndex = (currentWindowIndex + windowRadius).coerceAtMost(queueWindows.lastIndex)
        
        val itemsToRender = (startIndex..endIndex).sortedByDescending { kotlin.math.abs(it - animatedIndex) }

        val density = LocalDensity.current

        itemsToRender.forEach { index ->
            val item = queueWindows[index]
            val distance = index - animatedIndex
            val absDistance = kotlin.math.abs(distance)
            
            if (absDistance <= 2.5f) {
                val sign = kotlin.math.sign(distance)
                
                val translationXBase = with(density) { sign * (86.dp.toPx() * absDistance.coerceAtMost(1f) + 54.dp.toPx() * (absDistance - 1f).coerceAtLeast(0f)) }
                val rotationYBase = sign * (24f * absDistance.coerceAtMost(1f) + 12f * (absDistance - 1f).coerceAtLeast(0f))
                val scaleBase = 1f - (0.16f * absDistance)
                val alphaBase = 1f - (0.24f * absDistance)
                
                Box(
                    modifier = Modifier.graphicsLayer {
                        translationX = translationXBase * spread
                        rotationY = rotationYBase * spread
                        scaleX = scaleBase.coerceAtLeast(0.5f) + (1f - scaleBase.coerceAtLeast(0.5f)) * (1f - spread)
                        scaleY = scaleBase.coerceAtLeast(0.5f) + (1f - scaleBase.coerceAtLeast(0.5f)) * (1f - spread)
                        alpha = alphaBase.coerceIn(0f, 1f) * spread + (1f - spread) * (if (index == currentWindowIndex) 1f else 0f)
                    }
                ) {
                    val centerAlpha = (1f - (absDistance * 1.5f)).coerceIn(0f, 1f)
                    val sideAlpha = 1f - centerAlpha
                    
                    if (sideAlpha > 0f) {
                        GalaxySideCover(
                            mediaItem = item,
                            onClick = { onPlayQueueIndex(index) },
                            modifier = Modifier.alpha(sideAlpha)
                        )
                    }
                    if (centerAlpha > 0f) {
                        GalaxyCurrentCover(
                            thumbnailUrl = item.mediaMetadata.artworkUri?.toString() ?: mediaMetadata?.thumbnailUrl,
                            title = item.mediaMetadata.title?.toString() ?: mediaMetadata?.title.orEmpty(),
                            artist = item.mediaMetadata.artist?.toString() ?: mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                            modifier = Modifier.alpha(centerAlpha).clickable { onPlayQueueIndex(index) }
                        )
                    }
                }
            }
        }
        
        if (queueWindows.isEmpty()) {
             GalaxyCurrentCover(
                thumbnailUrl = mediaMetadata?.thumbnailUrl,
                title = mediaMetadata?.title.orEmpty(),
                artist = mediaMetadata?.artists?.joinToString { it.name }.orEmpty()
            )
        }
    }
}

@Composable
private fun GalaxySideCover(
    mediaItem: MediaItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(150.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .clickable(enabled = mediaItem != null, onClick = onClick)
    ) {
        AsyncImage(
            model = mediaItem?.mediaMetadata?.artworkUri?.toString(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun GalaxyCurrentCover(
    thumbnailUrl: String?,
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 184.dp, height = 218.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Color.White.copy(alpha = 0.11f))
            .border(1.dp, Color.White.copy(alpha = 0.34f), RoundedCornerShape(34.dp))
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .clip(RoundedCornerShape(26.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .background(Color.Black.copy(alpha = 0.44f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun GalaxyTopPill(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
) {
    val indicatorPosition by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "topPillIndicator"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(width = 88.dp, height = 34.dp)
                .graphicsLayer {
                    translationX = indicatorPosition * 88.dp.toPx()
                }
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.24f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TopPillButton(
                icon = R.drawable.lyrics,
                text = stringResource(R.string.lyrics),
                isSelected = selectedTab == 0,
                onClick = { onSelectTab(0) }
            )
            TopPillButton(
                icon = R.drawable.explore,
                text = stringResource(R.string.ui_sweep_browse),
                isSelected = selectedTab == 1,
                onClick = { onSelectTab(1) }
            )
            TopPillButton(
                icon = R.drawable.queue_music,
                text = stringResource(R.string.queue),
                isSelected = selectedTab == 2,
                onClick = { onSelectTab(2) }
            )
        }
    }
}

@Composable
fun TopPillButton(icon: Int, text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .size(width = 88.dp, height = 34.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = text,
            tint = Color.White.copy(alpha = if (isSelected) 1f else 0.78f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = if (isSelected) 1f else 0.78f),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}


