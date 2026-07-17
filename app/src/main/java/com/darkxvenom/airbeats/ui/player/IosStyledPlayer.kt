package com.darkxvenom.airbeats.ui.player

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.darkxvenom.airbeats.ui.component.SongDetailsDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.PlayerHorizontalPadding
import com.darkxvenom.airbeats.db.entities.FormatEntity
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.playback.PlayerConnection
import com.darkxvenom.airbeats.ui.component.BottomSheetPage
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.MenuState
import com.darkxvenom.airbeats.ui.menu.PlayerMenu
import com.darkxvenom.airbeats.utils.makeTimeString
import com.skydoves.cloudy.cloudy

private fun String?.highRes(): String = this?.replace("w54-h54", "w512-h512") ?: ""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosStyledPlayer(
    state: BottomSheetState,
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
    onShareClick: () -> Unit,
    shuffleModeEnabled: Boolean = false,
    onShuffleClick: () -> Unit = {},
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    onRepeatClick: () -> Unit = {},
    onOpenFullscreenLyrics: () -> Unit = {},
    playerConnection: PlayerConnection,
    navController: NavController,
    menuState: MenuState,
    
    nextUpMetadata: MediaMetadata? = null,
    currentFormat: FormatEntity? = null,
    playerVolume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        V8PlayerBackdrop(
            thumbnailUrl = mediaMetadata?.thumbnailUrl,
            disableBlur = false,
            label = "v8BackdropPortrait",
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
        ) {
            mediaMetadata?.let {
                V8PlayerControlsContent(
                    mediaMetadata = it,
                    playbackState = if (isPlaying) Player.STATE_READY else Player.STATE_IDLE,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    repeatMode = repeatMode,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    textBackgroundColor = Color.White,
                    sliderPosition = position,
                    position = position,
                    duration = duration,
                    isLiked = isLiked,
                    onLikeClick = onLikeClick,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onRepeatClick = onRepeatClick,
                    onShuffleClick = onShuffleClick,
                    shuffleModeEnabled = shuffleModeEnabled,
                    navController = navController,
                    state = state,
                    menuState = menuState,
                    onSliderValueChange = onSeek,
                    onSliderValueChangeFinished = onSeekFinished,
                    nextUpMetadata = nextUpMetadata,
                    currentFormat = currentFormat,
                    onExpandQueue = onQueueClick,
                    onShowLyrics = onOpenFullscreenLyrics,
                    playerVolume = playerVolume,
                    onVolumeChange = onVolumeChange,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun V8PlayerBackdrop(
    thumbnailUrl: String?,
    disableBlur: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val cloudyRadius = 100
    val blurMaskStart = 0.42f
    val blurMaskMid = 0.55f
    val blurMaskSolid = 0.72f
    val baseArtworkScale = if (disableBlur) 1.02f else 1.05f
    val baseArtworkAlpha = if (disableBlur) 0.65f else 0.75f

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
            label = label,
        ) { artworkUrl ->
            if (artworkUrl != null) {
                var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = artworkUrl.highRes(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = baseArtworkScale
                                scaleY = baseArtworkScale
                                alpha = baseArtworkAlpha
                            },
                    )

                    if (!disableBlur) {
                        AsyncImage(
                            model = artworkUrl.highRes(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .cloudy(radius = cloudyRadius)
                                .drawWithCache {
                                    val blurMask = Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Transparent,
                                            blurMaskStart to Color.Transparent,
                                            blurMaskMid to Color.Black.copy(alpha = 0.5f),
                                            blurMaskSolid to Color.Black,
                                            1f to Color.Black,
                                        )
                                    )

                                    onDrawWithContent {
                                        drawContent()
                                        drawRect(brush = blurMask, blendMode = BlendMode.DstIn)
                                    }
                                },
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.15f to Color.Black.copy(alpha = 0.05f),
                            0.45f to Color.Black.copy(alpha = 0.25f),
                            0.70f to Color.Black.copy(alpha = 0.50f),
                            1f to Color.Black.copy(alpha = 0.85f),
                        )
                    )
                ),
        )
    }
}

@Composable
private fun V8PlayerControlsContent(
    mediaMetadata: MediaMetadata,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textBackgroundColor: Color,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit,
    shuffleModeEnabled: Boolean,
    navController: NavController,
    state: BottomSheetState,
    menuState: MenuState,
    
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    nextUpMetadata: MediaMetadata? = null,
    currentFormat: FormatEntity? = null,
    onExpandQueue: () -> Unit = {},
    onShowLyrics: () -> Unit = {},
    playerVolume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = mediaMetadata.title,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "v8_title",
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textBackgroundColor,
                        modifier = Modifier.basicMarquee(),
                    )
                }

                Spacer(Modifier.height(6.dp))

                val artistsText = mediaMetadata.artists.joinToString(separator = ", ") { it.name }
                AnimatedContent(
                    targetState = artistsText,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "v8_artist",
                ) { artists ->
                    Text(
                        text = artists,
                        style = MaterialTheme.typography.titleMedium,
                        color = textBackgroundColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLiked) textBackgroundColor.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .clickable { onLikeClick() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = if (isLiked) 1f else 0.7f),
                        modifier = Modifier.size(30.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = state,
                                    onShowDetailsDialog = {
                                        showDetailsDialog = true
                                    },
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val safeDuration = if (duration <= 0L) 0f else duration.toFloat()
        val safeValue = (sliderPosition ?: position).toFloat().coerceIn(0f, maxOf(0f, safeDuration))
        Slider(
            value = safeValue,
            valueRange = 0f..maxOf(1f, safeDuration),
            onValueChange = { onSliderValueChange(it.toLong()) },
            onValueChangeFinished = onSliderValueChangeFinished,
            colors = thickSliderColors(textBackgroundColor),
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                val fraction = ((sliderState.value - sliderState.valueRange.start) /
                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                    .coerceIn(0f, 1f)
                GlassTrack(
                    fraction = fraction,
                    trackHeight = 10.dp,
                    tint = textBackgroundColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding),
        )

        Spacer(Modifier.height(4.dp))

        PlayerTimeLabel(
            sliderPosition = sliderPosition,
            position = position,
            duration = duration,
            textBackgroundColor = textBackgroundColor,
            showRemainingTime = true,
            centerContent = currentFormat?.let { format ->
                {
                    val codec = format.mimeType.substringAfter("/").uppercase()
                    val label = when {
                        codec.contains("FLAC") || codec.contains("ALAC") -> "Lossless"
                        codec.contains("OPUS") -> codec
                        codec.contains("AAC") -> codec
                        codec.contains("MP4A") -> "AAC"
                        codec.contains("VORBIS") -> "Vorbis"
                        else -> codec
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = textBackgroundColor.copy(alpha = 0.12f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.graphic_eq),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = textBackgroundColor.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = textBackgroundColor.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            },
        )

        if (nextUpMetadata != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExpandQueue() }
                    .padding(horizontal = PlayerHorizontalPadding, vertical = 2.dp),
            ) {
                AsyncImage(
                    model = nextUpMetadata.thumbnailUrl.highRes(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
                Text(
                    text = "Up next: ${nextUpMetadata.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textBackgroundColor.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        V8PlaybackControls(
            playbackState = playbackState,
            isPlaying = isPlaying,
            isLoading = isLoading,
            repeatMode = repeatMode,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            textBackgroundColor = textBackgroundColor,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onRepeatClick = onRepeatClick,
            onShuffleClick = onShuffleClick,
            shuffleModeEnabled = shuffleModeEnabled,
        )

        Spacer(Modifier.height(14.dp))

        V8VolumeSlider(
            volume = playerVolume,
            onVolumeChange = onVolumeChange,
            activeColor = textBackgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding + 8.dp),
        )

        Spacer(Modifier.height(26.dp))

        QueueCollapsedContentV8(
            textBackgroundColor = textBackgroundColor,
            onShowLyrics = onShowLyrics,
            onExpandQueue = onExpandQueue,
        )
    }
}

@Composable
private fun PlayerTimeLabel(
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    textBackgroundColor: Color,
    showRemainingTime: Boolean = false,
    centerContent: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding + 4.dp),
    ) {
        Text(
            text = makeTimeString(sliderPosition ?: position),
            color = textBackgroundColor.copy(alpha = 0.65f),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (centerContent != null) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                centerContent()
            }
        }
        Text(
            text = if (duration > 0L) {
                if (showRemainingTime) {
                    "-${makeTimeString((duration - (sliderPosition ?: position)).coerceAtLeast(0))}"
                } else {
                    makeTimeString(duration)
                }
            } else {
                ""
            },
            color = textBackgroundColor.copy(alpha = 0.65f),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun QueueCollapsedContentV8(
    textBackgroundColor: Color,
    onShowLyrics: () -> Unit,
    onExpandQueue: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding),
    ) {
        Surface(
            onClick = onShowLyrics,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.lyrics_apple),
                    contentDescription = "Lyrics",
                    tint = textBackgroundColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            V8DeviceSelector(
                textBackgroundColor = textBackgroundColor,
                modifier = Modifier.size(36.dp),
            )
        }

        Surface(
            onClick = onExpandQueue,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = "Queue",
                    tint = textBackgroundColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun V8DeviceSelector(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDeviceSheet by remember { mutableStateOf(false) }
    val availableDevices = remember { getAvailableDevices(context) }
    val activeDevice = remember(availableDevices) { getActiveDevice(availableDevices) }
    val isBluetooth = activeDevice?.isBluetoothOutput() == true
    val deviceIcon = if (isBluetooth) R.drawable.ic_bluetooth else R.drawable.airplay

    Surface(
        onClick = { showDeviceSheet = true },
        shape = CircleShape,
        color = if (isBluetooth) textBackgroundColor.copy(alpha = 0.15f) else Color.Transparent,
        modifier = modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(deviceIcon),
                contentDescription = "AirPlay",
                tint = textBackgroundColor.copy(alpha = if (isBluetooth) 1f else 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
    }

    if (showDeviceSheet) {
        DeviceSelectionBottomSheet(
            onDismiss = { showDeviceSheet = false },
            availableDevices = availableDevices,
            activeDevice = activeDevice,
            textBackgroundColor = textBackgroundColor,
        )
    }
}

@Composable
private fun DeviceSelectionBottomSheet(
    onDismiss: () -> Unit,
    availableDevices: List<AudioDeviceInfo>,
    activeDevice: AudioDeviceInfo?,
    textBackgroundColor: Color,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally),
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Select device",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                availableDevices.forEach { device ->
                    val isActive = device.id == activeDevice?.id
                    val iconRes = if (device.isBluetoothOutput()) R.drawable.ic_bluetooth else R.drawable.airplay
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) textBackgroundColor.copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isActive) 1f else 0.65f),
                                modifier = Modifier.size(24.dp),
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.outputName(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isActive) 1f else 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (isActive) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    )
                                }
                            }

                            if (isActive) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun V8VolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.volume_off),
            contentDescription = null,
            tint = activeColor.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )

        Slider(
            value = volume.coerceIn(0f, 1f),
            valueRange = 0f..1f,
            onValueChange = onVolumeChange,
            colors = thickSliderColors(activeColor),
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                val fraction = ((sliderState.value - sliderState.valueRange.start) /
                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                    .coerceIn(0f, 1f)
                GlassTrack(
                    fraction = fraction,
                    trackHeight = 10.dp,
                    tint = activeColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            modifier = Modifier.weight(1f),
        )

        Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = null,
            tint = activeColor.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun V8PlaybackControls(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textBackgroundColor: Color,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit,
    shuffleModeEnabled: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding),
    ) {
        Surface(
            onClick = onPrevious,
            enabled = canSkipPrevious,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                    modifier = Modifier.size(38.dp),
                )
            }
        }

        Surface(
            onClick = onPlayPause,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = textBackgroundColor,
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            when {
                                playbackState == Player.STATE_ENDED -> R.drawable.replay
                                isPlaying -> R.drawable.pause
                                else -> R.drawable.play
                            }
                        ),
                        contentDescription = null,
                        tint = textBackgroundColor,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
        }

        Surface(
            onClick = onNext,
            enabled = canSkipNext,
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipNext) 1f else 0.4f),
                    modifier = Modifier.size(38.dp),
                )
            }
        }
    }
}

@Composable
private fun GlassTrack(
    fraction: Float,
    trackHeight: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(trackHeight)
            .clip(shape),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 6.dp)
                .background(tint.copy(alpha = 0.16f)),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 0.75.dp, color = tint.copy(alpha = 0.3f), shape = shape),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.95f),
                            tint.copy(alpha = 0.75f),
                        )
                    )
                ),
        )
    }
}

@Composable
private fun thickSliderColors(activeColor: Color) = SliderDefaults.colors(
    thumbColor = activeColor,
    activeTrackColor = activeColor,
    inactiveTrackColor = activeColor.copy(alpha = 0.24f),
)

private fun getAvailableDevices(context: Context): List<AudioDeviceInfo> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { device ->
            device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                isBleHeadset(device) ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        }
        .sortedBy { device ->
            when {
                device.isBluetoothOutput() -> 0
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET -> 1
                device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 2
                else -> 3
            }
        }
}

private fun getActiveDevice(devices: List<AudioDeviceInfo>): AudioDeviceInfo? {
    return devices.firstOrNull { it.isBluetoothOutput() }
        ?: devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        }
        ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
}

private fun AudioDeviceInfo.isBluetoothOutput(): Boolean {
    return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        isBleHeadset(this)
}

private fun isBleHeadset(device: AudioDeviceInfo): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
}

private fun AudioDeviceInfo.outputName(): String {
    productName?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        else -> if (isBleHeadset(this)) "BLE headset" else "Device"
    }
}
