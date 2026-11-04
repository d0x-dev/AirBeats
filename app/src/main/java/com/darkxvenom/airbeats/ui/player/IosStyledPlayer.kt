package com.darkxvenom.airbeats.ui.player

import androidx.compose.ui.res.stringResource

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.collectAsState
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.ui.component.PlayerCastMenuRow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Switch
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
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
import com.darkxvenom.airbeats.ui.component.AudioPipelineDialog
import com.darkxvenom.airbeats.ui.component.AudioQualityTag
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

private fun String?.highRes(): String = this?.replace(Regex("w\\d+-h\\d+"), "w8192-h8192") ?: ""

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
                    onShowDetailsDialog = { showDetailsDialog = true },
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
    onShowDetailsDialog: () -> Unit,
    
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
                                    onShowDetailsDialog = onShowDetailsDialog,
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

        var showAudioPipelineDialog by rememberSaveable { mutableStateOf(false) }

        if (showAudioPipelineDialog) {
            AudioPipelineDialog(
                currentFormat = currentFormat,
                mediaMetadata = mediaMetadata,
                onDismiss = { showAudioPipelineDialog = false },
            )
        }

        PlayerTimeLabel(
            sliderPosition = sliderPosition,
            position = position,
            duration = duration,
            textBackgroundColor = textBackgroundColor,
            showRemainingTime = true,
            centerContent = {
                AudioQualityTag(
                    currentFormat = currentFormat,
                    mediaMetadata = mediaMetadata,
                    tint = textBackgroundColor,
                    onClick = { showAudioPipelineDialog = true },
                )
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
                    contentDescription = stringResource(R.string.lyrics),
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
                    contentDescription = stringResource(R.string.queue),
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
    val playerConnection = LocalPlayerConnection.current
    var showDeviceSheet by remember { mutableStateOf(false) }

    val preferredAudioDevice by (playerConnection?.service?.preferredAudioDevice?.collectAsState() ?: remember { mutableStateOf(null) })
    val availableAudioDevices by (playerConnection?.service?.availableAudioDevices?.collectAsState() ?: remember { mutableStateOf(emptyList()) })

    val fallbackDevices = remember { getAvailableDevices(context) }
    val displayDevices = if (availableAudioDevices.isNotEmpty()) availableAudioDevices else fallbackDevices
    val activeDevice = preferredAudioDevice ?: remember(displayDevices) { getActiveDevice(displayDevices) }
    val isCasting by (playerConnection?.service?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) })
    val isBluetooth = activeDevice?.isBluetoothOutput() == true
    val deviceIcon = when {
        isCasting -> R.drawable.ic_cast_connected
        isBluetooth -> R.drawable.ic_bluetooth
        else -> R.drawable.airplay
    }

    val hasBtPerm = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    var btPermGranted by remember { mutableStateOf(hasBtPerm) }
    val btLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        btPermGranted = granted
        playerConnection?.service?.refreshAudioOutputDevices()
        showDeviceSheet = true
    }

    Surface(
        onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !btPermGranted) {
                btLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                showDeviceSheet = true
            }
        },
        shape = CircleShape,
        color = if (isCasting || isBluetooth) textBackgroundColor.copy(alpha = 0.15f) else Color.Transparent,
        modifier = modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(deviceIcon),
                contentDescription = stringResource(R.string.ui_a11y_playback_device),
                tint = textBackgroundColor.copy(alpha = if (isCasting || isBluetooth) 1f else 0.7f),
                modifier = Modifier.size(22.dp),
            )
        }
    }

    if (showDeviceSheet) {
        DeviceSelectionBottomSheet(
            onDismiss = { showDeviceSheet = false },
            availableDevices = displayDevices,
            activeDevice = activeDevice,
            preferredDevice = preferredAudioDevice,
            onSelectDevice = { device ->
                playerConnection?.service?.setPreferredOutputDevice(device)
            },
            textBackgroundColor = textBackgroundColor,
            hasBluetoothPermission = btPermGranted,
            onRequestBluetoothPermission = {
                btLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        )
    }
}

@Composable
internal fun DeviceSelectionBottomSheet(
    onDismiss: () -> Unit,
    availableDevices: List<AudioDeviceInfo>,
    activeDevice: AudioDeviceInfo?,
    preferredDevice: AudioDeviceInfo? = null,
    onSelectDevice: (AudioDeviceInfo?) -> Unit = {},
    textBackgroundColor: Color = MaterialTheme.colorScheme.onSurface,
    hasBluetoothPermission: Boolean = true,
    onRequestBluetoothPermission: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.88f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally),
                )

                Spacer(Modifier.height(16.dp))

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ui_text_playback_destination),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.ui_text_choose_audio_route_or_stream_to_multiple_devices),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Scrollable Content Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {

                // Permission Warning Banner if missing Bluetooth permission on Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBluetoothPermission && onRequestBluetoothPermission != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bluetooth),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ui_text_bluetooth_permission_required),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.ui_sweep_needed_to_detect_connected_bluetooth_earbuds_and_speakers),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = onRequestBluetoothPermission,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                            ) {
                                Text(
                                    text = stringResource(R.string.ui_text_grant),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                // Google Cast / Chromecast Streaming
                PlayerCastMenuRow(
                    onDismiss = onDismiss,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                // 1. Automatic (System Default) Option
                val isAutoSelected = preferredDevice == null
                Surface(
                    onClick = {
                        onSelectDevice(null)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAutoSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.airplay),
                            contentDescription = null,
                            tint = if (isAutoSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ui_sweep_system_default_automatic),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isAutoSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAutoSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (activeDevice != null) "Routes to ${activeDevice.outputName()}" else "Automatic routing based on plugged/paired devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        if (isAutoSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.ui_text_selected),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Available Physical Devices
                availableDevices.forEach { device ->
                    val isSelected = (preferredDevice != null && preferredDevice.id == device.id) ||
                        (preferredDevice == null && activeDevice?.id == device.id)
                    val iconVector = when {
                        device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> Icons.Default.PhoneAndroid
                        device.type == AudioDeviceInfo.TYPE_USB_DEVICE || device.type == AudioDeviceInfo.TYPE_USB_HEADSET -> Icons.Default.Usb
                        device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET -> Icons.Default.Headphones
                        else -> null
                    }

                    Surface(
                        onClick = {
                            onSelectDevice(device)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                        ) {
                            if (device.isBluetoothOutput()) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_bluetooth),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = iconVector ?: Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp),
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.outputName(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val subtitleText = when {
                                    device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Internal phone speaker"
                                    device.isBluetoothOutput() -> "Bluetooth wireless audio"
                                    device.type == AudioDeviceInfo.TYPE_USB_DEVICE || device.type == AudioDeviceInfo.TYPE_USB_HEADSET -> "USB DAC / Bit-perfect audio"
                                    else -> "Wired output"
                                }
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                )
                            }

                            if (isSelected) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = stringResource(R.string.ui_text_active),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.ui_text_active),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // In-App Simultaneous Multi-Device Playback (Dual Output)
                val isDualAudioEnabled by (playerConnection?.service?.isDualAudioEnabled?.collectAsState() ?: remember { mutableStateOf(false) })
                val dualAudioSecondaryDeviceId by (playerConnection?.service?.dualAudioSecondaryDeviceId?.collectAsState() ?: remember { mutableStateOf(null) })
                val dualAudioSecondaryVolume by (playerConnection?.service?.dualAudioSecondaryVolume?.collectAsState() ?: remember { mutableStateOf(1.0f) })

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDualAudioEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDualAudioEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                modifier = Modifier.size(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SpeakerGroup,
                                        contentDescription = null,
                                        tint = if (isDualAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.ui_text_simultaneous_dual_playback),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = stringResource(R.string.ui_sweep_play_in_phone_speaker_bluetooth_aux_at_the_same_time),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                )
                            }

                            Switch(
                                checked = isDualAudioEnabled,
                                onCheckedChange = { isChecked ->
                                    playerConnection?.service?.setDualAudioEnabled(isChecked)
                                },
                            )
                        }

                        AnimatedVisibility(visible = isDualAudioEnabled) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = stringResource(R.string.ui_text_secondary_mirror_output),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 6.dp),
                                )

                                val secondaryDevices = availableDevices.filter { it.id != activeDevice?.id }
                                val allSecondary = if (secondaryDevices.isNotEmpty()) secondaryDevices else availableDevices

                                allSecondary.forEach { secDevice ->
                                    val isSecSelected = (dualAudioSecondaryDeviceId == secDevice.id) ||
                                        (dualAudioSecondaryDeviceId == null && secDevice.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

                                    Surface(
                                        onClick = {
                                            playerConnection?.service?.setDualAudioSecondaryDevice(secDevice)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSecSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                        ) {
                                            Icon(
                                                imageVector = if (secDevice.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) Icons.Default.PhoneAndroid else Icons.Default.Headphones,
                                                contentDescription = null,
                                                tint = if (isSecSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                text = secDevice.outputName(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSecSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSecSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                            )
                                            if (isSecSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = stringResource(R.string.ui_a11y_active_secondary),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = stringResource(R.string.ui_text_speaker_volume),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        modifier = Modifier.width(100.dp),
                                    )
                                    Slider(
                                        value = dualAudioSecondaryVolume,
                                        onValueChange = {
                                            playerConnection?.service?.setDualAudioSecondaryVolume(it)
                                        },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "${(dualAudioSecondaryVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Done Button
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

internal fun getAvailableDevices(context: Context): List<AudioDeviceInfo> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { device ->
            device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                isBleHeadset(device) ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                device.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        .sortedBy { device ->
            when {
                device.isBluetoothOutput() -> 0
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET -> 1
                device.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET -> 2
                device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 3
                else -> 4
            }
        }
}

internal fun getActiveDevice(devices: List<AudioDeviceInfo>): AudioDeviceInfo? {
    return devices.firstOrNull { it.isBluetoothOutput() }
        ?: devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET
        }
        ?: devices.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }
        ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
}

internal fun AudioDeviceInfo.isBluetoothOutput(): Boolean {
    return type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        isBleHeadset(this)
}

internal fun isBleHeadset(device: AudioDeviceInfo): Boolean {
    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && device.type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER)
}

internal fun AudioDeviceInfo.outputName(): String {
    productName?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speaker"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired Headphones"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Device"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
        else -> if (isBleHeadset(this)) "BLE Headset" else "Audio Device"
    }
}



