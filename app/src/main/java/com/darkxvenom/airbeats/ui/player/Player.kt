package com.darkxvenom.airbeats.ui.player

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter
import android.os.Build
import android.text.format.Formatter

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.IconButton
import com.darkxvenom.airbeats.db.MusicDatabase
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalDownloadUtil
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.DarkModeKey
import com.darkxvenom.airbeats.constants.DefaultPlayPauseButtonShape
import com.darkxvenom.airbeats.constants.DefaultSmallButtonsShape
import com.darkxvenom.airbeats.constants.PlayPauseButtonShapeKey
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyle
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyleKey
import com.darkxvenom.airbeats.constants.PlayerButtonsStyle
import com.darkxvenom.airbeats.constants.PlayerButtonsStyleKey
import com.darkxvenom.airbeats.constants.PlayerHorizontalPadding
import com.darkxvenom.airbeats.constants.PlayerScreenStyle
import com.darkxvenom.airbeats.constants.PlayerScreenStyleKey
import com.darkxvenom.airbeats.constants.PlayerTextAlignmentKey
import com.darkxvenom.airbeats.constants.PureBlackKey
import com.darkxvenom.airbeats.constants.QueuePeekHeight
import com.darkxvenom.airbeats.constants.ShowLyricsKey
import com.darkxvenom.airbeats.constants.EnableNewQueueScreenKey
import com.darkxvenom.airbeats.constants.SliderStyle
import com.darkxvenom.airbeats.constants.SliderStyleKey
import com.darkxvenom.airbeats.constants.SmallButtonsShapeKey
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.extensions.toggleRepeatMode
import com.darkxvenom.airbeats.db.entities.LyricsEntity
import com.darkxvenom.airbeats.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.darkxvenom.airbeats.db.entities.ArtistEntity
import com.darkxvenom.airbeats.lyrics.LyricsEntry
import com.darkxvenom.airbeats.lyrics.LyricsUtils.parseLyrics
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.playback.ExoDownloadService
import com.darkxvenom.airbeats.ui.component.BottomSheet
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.LocalBottomSheetPageState
import com.darkxvenom.airbeats.ui.component.PlayerSliderTrack
import com.darkxvenom.airbeats.ui.component.ResizableIconButton
import com.darkxvenom.airbeats.ui.component.rememberBottomSheetState
import com.darkxvenom.airbeats.ui.menu.PlayerMenu
import com.darkxvenom.airbeats.ui.menu.AddToPlaylistDialog
import com.darkxvenom.airbeats.innertube.YouTube
import androidx.compose.runtime.rememberCoroutineScope
import com.darkxvenom.airbeats.ui.screens.settings.DarkMode
import com.darkxvenom.airbeats.ui.screens.settings.PlayerTextAlignment
import com.darkxvenom.airbeats.ui.theme.PlayerColorExtractor
import com.darkxvenom.airbeats.ui.theme.PlayerSliderColors
import com.darkxvenom.airbeats.ui.theme.extractGradientColors
import com.darkxvenom.airbeats.utils.getPlayPauseShape
import com.darkxvenom.airbeats.utils.getSmallButtonShape
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt

internal val SpotifyFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val coroutineScope = rememberCoroutineScope()

    val clipboardManager = LocalClipboardManager.current

    var showFullscreenLyrics by remember { mutableStateOf(false) }
    val playerConnection = LocalPlayerConnection.current ?: return

    val playerTextAlignment by rememberEnumPreference(
        PlayerTextAlignmentKey,
        PlayerTextAlignment.CENTER
    )

    val playerScreenStyle by rememberEnumPreference<PlayerScreenStyle>(
        PlayerScreenStyleKey,
        PlayerScreenStyle.IOS_STYLED
    )

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val enableNewQueueScreen by rememberPreference(EnableNewQueueScreenKey, defaultValue = true)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val primaryArtistId = mediaMetadata?.artists?.firstOrNull { it.id != null }?.id
    val primaryArtist by remember(primaryArtistId) {
        primaryArtistId?.let(database::artist) ?: flowOf(null)
    }.collectAsState(initial = null)
    val spotifyArtistThumbnailUrl =
        primaryArtist?.thumbnailUrl ?: currentSong?.artists?.firstOrNull()?.thumbnailUrl
    var spotifyLyricsEntity by remember { mutableStateOf<LyricsEntity?>(null) }
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val playerVolume = playerConnection.service.playerVolume.collectAsState()

    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex = playerConnection.player.currentMediaItemIndex
    val nextMediaMetadata = remember(queueWindows, currentWindowIndex) {
        if (currentWindowIndex + 1 < queueWindows.size) {
            val nextItem = queueWindows[currentWindowIndex + 1].mediaItem
            // Try to extract our domain MediaMetadata. If not possible, we could at least map the artwork.
            // AirBeats maps LocalMediaItem data to its MediaMetadata using extensions or we can just fetch it.
            // We just need the thumbnail URL. For simplicity, we can get it from the DB or assume it's attached.
            com.darkxvenom.airbeats.models.MediaMetadata(
                id = nextItem.mediaId,
                title = nextItem.mediaMetadata.title?.toString() ?: "",
                artists = emptyList(),
                duration = 0,
                thumbnailUrl = nextItem.mediaMetadata.artworkUri?.toString()
            )
        } else null
    }

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    var changeColor by remember {
        mutableStateOf(false)
    }

    // Animations for background effects
    var backgroundImageUrl by remember { mutableStateOf<String?>(null) }
    val blurRadius by animateDpAsState(
        targetValue = if (state.isExpanded && playerBackground == PlayerBackgroundStyle.BLUR) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "blurRadius"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (state.isExpanded && playerBackground != PlayerBackgroundStyle.DEFAULT) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = when {
            !state.isExpanded -> 0f
            playerBackground == PlayerBackgroundStyle.BLUR -> 0.3f
            playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2 -> 0.2f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )

    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    LaunchedEffect(playerScreenStyle, mediaMetadata?.id, currentLyrics) {
        val metadata = mediaMetadata
        if ((playerScreenStyle != PlayerScreenStyle.SPOTIFY && playerScreenStyle != PlayerScreenStyle.GALAXY) || metadata == null) {
            spotifyLyricsEntity = null
            return@LaunchedEffect
        }
        if (!currentLyrics?.lyrics.isNullOrBlank() && currentLyrics?.lyrics != LYRICS_NOT_FOUND) {
            spotifyLyricsEntity = currentLyrics
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
                spotifyLyricsEntity = entity
            }
        }
    }

    // Obtener el color del tema antes de LaunchedEffect
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColorArgb = surfaceColor.toArgb()

    LaunchedEffect(mediaMetadata, playerBackground, fallbackColorArgb) {
        // Update image URL for smooth transitions
        backgroundImageUrl = mediaMetadata?.thumbnailUrl?.highQualityThumbnail()

        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.BLUR) {
            gradientColors = listOf(Color.Black, Color.Black)
        }
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.GRADIENT) {
            gradientColors = listOf(Color.Black, Color.Black)
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            withContext(Dispatchers.IO) {
                val result = runCatching {
                    ImageLoader(context)
                        .execute(
                            ImageRequest
                                .Builder(context)
                                .data(mediaMetadata?.thumbnailUrl?.highQualityThumbnail())
                                .allowHardware(false)
                                .build(),
                        ).drawable as? BitmapDrawable
                }.getOrNull()

                result?.bitmap?.let { rawBitmap ->
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && rawBitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
                        rawBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: rawBitmap
                    } else rawBitmap
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()

                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColorArgb
                    )

                    withContext(Dispatchers.Main) {
                        gradientColors = extractedColors
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    var spotifyColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    LaunchedEffect(mediaMetadata, fallbackColorArgb) {
        val metadata = mediaMetadata ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val result = runCatching {
                ImageLoader(context)
                    .execute(
                        ImageRequest
                            .Builder(context)
                            .data(metadata.thumbnailUrl?.highQualityThumbnail())
                            .allowHardware(false)
                            .build(),
                    ).drawable as? BitmapDrawable
            }.getOrNull()

            result?.bitmap?.let { rawBitmap ->
                val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && rawBitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
                    rawBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: rawBitmap
                } else rawBitmap
                val palette = Palette.from(bitmap)
                    .maximumColorCount(8)
                    .resizeBitmapArea(100 * 100)
                    .generate()

                val extractedColors = PlayerColorExtractor.extractGradientColors(
                    palette = palette,
                    fallbackColor = fallbackColorArgb
                )

                withContext(Dispatchers.Main) {
                    spotifyColors = extractedColors
                }
            }
        }
    }

    val spotifySurfaceColor = spotifyColors.firstOrNull() ?: Color(0xFF311000)
    val spotifyHeaderColor = spotifyColors.getOrNull(1) ?: Color(0xFF260300)

    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.Black
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.White
                } else {
                    changeColor = false
                    Color.White
                }
            }
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.White
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.Black
                } else {
                    changeColor = false
                    Color.Black
                }
            }
        }

    // Sistema de color scheme con PRIMARY y TERTIARY colors
    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT ->
            if (useDarkTheme) Pair(Color.White, Color.Black)
            else Pair(Color.Black, Color.White)
        PlayerButtonsStyle.PRIMARY -> Pair(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        PlayerButtonsStyle.TERTIARY -> Pair(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )
    }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val smallButtonsShapeState = rememberPreference(
        key = SmallButtonsShapeKey,
        defaultValue = DefaultSmallButtonsShape
    )

    val smallButtonShape = remember(smallButtonsShapeState.value) {
        getSmallButtonShape(smallButtonsShapeState.value)
    }

    val playPauseShapeState = rememberPreference(
        key = PlayPauseButtonShapeKey,
        defaultValue = DefaultPlayPauseButtonShape
    )

    val playPauseShape = remember(playPauseShapeState.value) {
        getPlayPauseShape(playPauseShapeState.value)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "play_pause_rotation")
    val playPauseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000, // 9 seconds for a full rotation
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Forma dinámica: cuando está reproduciendo usa la forma seleccionada
    // Cuando está en pausa usa Square
    val currentPlayPauseShape = remember(isPlaying, playPauseShape) {
        if (isPlaying) {
            playPauseShape
        } else {
            MaterialShapes.Square
        }
    }

    // Function to create the modifier for small buttons
    val smallButtonModifier = @Composable {
        Modifier
            .size(42.dp)
            .clip(smallButtonShape.toShape())
            .background(textButtonColor)
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDetailsDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            containerColor = if (useBlackBackground) Color.Black else AlertDialogDefaults.containerColor,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        "Itag" to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size) to
                                currentFormat?.contentLength?.let {
                                    Formatter.formatShortFileSize(
                                        context,
                                        it
                                    )
                                },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(displayText))
                                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            mediaMetadata?.let { metadata ->
                database.transaction {
                    insert(metadata)
                }
                coroutineScope.launch(Dispatchers.IO) {
                    playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, metadata.id) }
                }
                listOf(metadata.id)
            } ?: emptyList()
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues()
                .calculateBottomPadding(),
            expandedBound = state.expandedBound,
        )

    val bottomSheetBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT ->
            MaterialTheme.colorScheme.surfaceContainer
        else ->
            if (useBlackBackground) Color.Black
            else MaterialTheme.colorScheme.surfaceContainer
    }

    val navBarStyle by rememberEnumPreference(
        com.darkxvenom.airbeats.constants.NavBarStyleKey,
        defaultValue = com.darkxvenom.airbeats.constants.NavBarStyle.CLASSIC
    )
    val isNeon = navBarStyle == com.darkxvenom.airbeats.constants.NavBarStyle.NEON

    BottomSheet(
        state = state,
        modifier = modifier,
        shape = if (isNeon) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(
            topStart = if (!state.isExpanded) 16.dp else 0.dp,
            topEnd = if (!state.isExpanded) 16.dp else 0.dp
        ),
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        AnimatedContent(
                            targetState = mediaMetadata?.thumbnailUrl,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "blurBackground"
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl.highQualityThumbnail())
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(if (useDarkTheme) 150.dp else 100.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "gradientBackground"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val gradientColorStops = if (colors.size >= 3) {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.5f to colors[1],
                                        1.0f to colors[2]
                                    )
                                } else {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.6f to colors[0].copy(alpha = 0.7f),
                                        1.0f to Color.Black
                                    )
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                )
                            }
                        }
                    }
                    else -> {
                        PlayerBackgroundStyle.DEFAULT
                    }
                }
            }
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            val (navBarStyle, _) = com.darkxvenom.airbeats.utils.rememberEnumPreference<com.darkxvenom.airbeats.constants.NavBarStyle>(
                com.darkxvenom.airbeats.constants.NavBarStyleKey,
                defaultValue = com.darkxvenom.airbeats.constants.NavBarStyle.CLASSIC
            )
            if (navBarStyle == com.darkxvenom.airbeats.constants.NavBarStyle.NEW_CLASSIC) {
                NewClassicMiniPlayer(
                    position = position,
                    duration = duration,
                    state = state,
                    navController = navController,
                )
            } else if (navBarStyle == com.darkxvenom.airbeats.constants.NavBarStyle.NEON) {
                NeonMiniPlayer(state = state)
            } else if (navBarStyle == com.darkxvenom.airbeats.constants.NavBarStyle.APPLE) {
                AppleMiniPlayer(
                    position = position,
                    duration = duration,
                    state = state,
                )
            } else {
                MiniPlayer(
                    position = position,
                    duration = duration,
                    state = state,
                )
            }
        },
    ) {
        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            var prevBounce by remember { mutableIntStateOf(0) }
            var nextBounce by remember { mutableIntStateOf(0) }
            var playBounce by remember { mutableIntStateOf(0) }
            var shuffleBounce by remember { mutableIntStateOf(0) }
            var likeBounce by remember { mutableIntStateOf(0) }
            var repeatBounce by remember { mutableIntStateOf(0) }

            val prevScale by animateFloatAsState(if (prevBounce % 2 == 1) 1.22f else 1f, spring(), label = "prevBounce")
            val nextScale by animateFloatAsState(if (nextBounce % 2 == 1) 1.22f else 1f, spring(), label = "nextBounce")
            val playScale by animateFloatAsState(if (playBounce % 2 == 1) 1.15f else 1f, spring(), label = "playBounce")
            val shuffleScale by animateFloatAsState(if (shuffleBounce % 2 == 1) 1.20f else 1f, spring(), label = "shuffleBounce")
            val likeScale by animateFloatAsState(if (likeBounce % 2 == 1) 1.20f else 1f, spring(), label = "likeBounce")
            val repeatScale by animateFloatAsState(if (repeatBounce % 2 == 1) 1.20f else 1f, spring(), label = "repeatBounce")

            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                label = "playPauseRoundness",
            )

            Row(
                horizontalArrangement =
                    when (playerTextAlignment) {
                        PlayerTextAlignment.SIDED -> Arrangement.Start
                        PlayerTextAlignment.CENTER -> Arrangement.Center
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                AnimatedContent(
                    targetState = mediaMetadata.title,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "",
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = onBackgroundColor,
                        modifier =
                            Modifier
                                .basicMarquee()
                                .clickable(enabled = mediaMetadata.album != null) {
                                    navController.navigate("album/${mediaMetadata.album!!.id}")
                                    state.collapseSoft()
                                },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Row(
                horizontalArrangement =
                    when (playerTextAlignment) {
                        PlayerTextAlignment.SIDED -> Arrangement.Start
                        PlayerTextAlignment.CENTER -> Arrangement.Center
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                mediaMetadata.artists.fastForEachIndexed { index, artist ->
                    AnimatedContent(
                        targetState = artist.name,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "",
                    ) { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = onBackgroundColor,
                            maxLines = 1,
                            modifier =
                                Modifier.clickable(enabled = artist.id != null) {
                                    navController.navigate("artist/${artist.id}")
                                    state.collapseSoft()
                                },
                        )
                    }

                    if (index != mediaMetadata.artists.lastIndex) {
                        AnimatedContent(
                            targetState = ", ",
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "",
                        ) { comma ->
                            Text(
                                text = comma,
                                style = MaterialTheme.typography.titleMedium,
                                color = onBackgroundColor,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Box(
                    modifier = smallButtonModifier()
                        .clickable {
                            playerConnection.service.startRadioSeamlessly()
                        },
                ) {
                    Image(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconButtonColor),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Box(
                    modifier = smallButtonModifier()
                        .clickable {
                            if (download?.state == Download.STATE_COMPLETED) {
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    mediaMetadata.id,
                                    false,
                                )
                            } else {
                                database.transaction {
                                    insert(mediaMetadata)
                                }
                                val downloadRequest =
                                    DownloadRequest
                                        .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                        .setCustomCacheKey(mediaMetadata.id)
                                        .setData(mediaMetadata.title.toByteArray())
                                        .build()
                                DownloadService.sendAddDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    downloadRequest,
                                    false,
                                )
                            }
                        },
                ) {
                    if (download?.state == Download.STATE_DOWNLOADING) {
                        val progress = download!!.percentDownloaded / 100f
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            ),
                            label = "downloadProgress"
                        )

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                        ) {
                            val strokeWidth = 3.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val center = Offset(size.width / 2, size.height / 2)

                            // Background circle (gray)
                            drawCircle(
                                color = iconButtonColor.copy(alpha = 0.3f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )

                            // Progress circle
                            if (animatedProgress > 0f) {
                                drawArc(
                                    color = iconButtonColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f * animatedProgress,
                                    useCenter = false,
                                    topLeft = Offset(
                                        center.x - radius,
                                        center.y - radius
                                    ),
                                    size = Size(radius * 2, radius * 2),
                                    style = Stroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }
                    }

                    val iconResource = when (download?.state) {
                        Download.STATE_COMPLETED -> R.drawable.offline
                        Download.STATE_DOWNLOADING -> R.drawable.pause
                        Download.STATE_FAILED -> R.drawable.error
                        else -> R.drawable.download
                    }

                    val iconAlpha by animateFloatAsState(
                        targetValue = if (download?.state == Download.STATE_DOWNLOADING) 0.8f else 1f,
                        animationSpec = tween(durationMillis = 200),
                        label = "iconAlpha"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (download?.state == Download.STATE_DOWNLOADING) 0.9f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "iconScale"
                    )

                    Image(
                        painter = painterResource(iconResource),
                        contentDescription = when (download?.state) {
                            Download.STATE_COMPLETED -> stringResource(R.string.download)
                            Download.STATE_DOWNLOADING -> stringResource(R.string.downloading)
                            Download.STATE_FAILED -> stringResource(R.string.download_errorup)
                            else -> stringResource(R.string.download)
                        },
                        colorFilter = ColorFilter.tint(
                            when (download?.state) {
                                Download.STATE_FAILED -> MaterialTheme.colorScheme.error
                                else -> iconButtonColor
                            }
                        ),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                            .scale(iconScale)
                            .alpha(iconAlpha),
                    )

                    // Small progress text
                    if (download?.state == Download.STATE_DOWNLOADING) {
                        val progress = (download!!.percentDownloaded).toInt()
                        Text(
                            text = "$progress%",
                            style = MaterialTheme.typography.labelSmall,
                            color = iconButtonColor,
                            fontSize = 8.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-2).dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(smallButtonShape.toShape())
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    AnimatedContent(
                        label = "sleepTimer",
                        targetState = sleepTimerEnabled,
                    ) { sleepTimerEnabled ->
                        if (sleepTimerEnabled) {
                            Text(
                                text = makeTimeString(sleepTimerTimeLeft),
                                style = MaterialTheme.typography.labelLarge,
                                color = onBackgroundColor,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClick = playerConnection.service.sleepTimer::clear)
                                    .basicMarquee(),
                            )
                        } else {
                            Box(
                                modifier = smallButtonModifier()
                                    .clickable {
                                        showSleepTimerDialog = true
                                    },
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.bedtime),
                                    colorFilter = ColorFilter.tint(iconButtonColor),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = smallButtonModifier()
                        .clickable {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = state,
                                    onShowDetailsDialog = { showDetailsDialog = true },
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                ) {
                    Image(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconButtonColor),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            when (sliderStyle) {
                SliderStyle.DEFAULT -> {
                    Slider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        colors = PlayerSliderColors.getSliderColors(
                            textButtonColor = TextBackgroundColor,
                            playerBackground = playerBackground,
                            useDarkTheme = useDarkTheme
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                    )
                }

                SliderStyle.SQUIGGLY -> {
                    SquigglySlider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        colors = PlayerSliderColors.getSliderColors(
                            textButtonColor = TextBackgroundColor,
                            playerBackground = playerBackground,
                            useDarkTheme = useDarkTheme
                        ),
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                        squigglesSpec =
                            SquigglySlider.SquigglesSpec(
                                amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                                strokeWidth = 3.dp,
                            ),
                    )
                }

                SliderStyle.SLIM -> {
                    Slider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = {
                            sliderPosition = it.toLong()
                        },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                playerConnection.player.seekTo(it)
                                position = it
                            }
                            sliderPosition = null
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = PlayerSliderColors.getSliderColors(
                                    textButtonColor = TextBackgroundColor,
                                    playerBackground = playerBackground,
                                    useDarkTheme = useDarkTheme
                                )
                            )
                        },
                        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding + 4.dp),
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_previous,
                        enabled = canSkipPrevious,
                        color = TextBackgroundColor,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .scale(prevScale)
                                .align(Alignment.Center),
                        onClick = { prevBounce++; playerConnection.seekToPrevious() },
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier =
                        Modifier
                            .size(85.dp)
                            .scale(playScale)
                            .rotate(if (isPlaying) playPauseRotation else 0f)
                            .clip(currentPlayPauseShape.toShape())
                            .background(textButtonColor)
                            .clickable {
                                playBounce++
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                ) {
                    Image(
                        painter =
                            painterResource(
                                if (playbackState ==
                                    STATE_ENDED
                                ) {
                                    R.drawable.replay
                                } else if (isPlaying) {
                                    R.drawable.pause
                                } else {
                                    R.drawable.play
                                }
                            ),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(iconButtonColor),
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                                .rotate(if (isPlaying) -playPauseRotation else 0f),
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = R.drawable.skip_next,
                        enabled = canSkipNext,
                        color = TextBackgroundColor,
                        modifier =
                            Modifier
                                .size(32.dp)
                                .scale(nextScale)
                                .align(Alignment.Center),
                        onClick = { nextBounce++; playerConnection.seekToNext() },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
            ) {
                ResizableIconButton(
                    icon = if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle,
                    color = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else TextBackgroundColor,
                    modifier = Modifier.size(32.dp).padding(4.dp).scale(shuffleScale),
                    onClick = {
                        shuffleBounce++
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    }
                )

                ResizableIconButton(
                    icon = if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border,
                    color = if (currentSong?.song?.liked == true) MaterialTheme.colorScheme.error else TextBackgroundColor,
                    modifier = Modifier.size(32.dp).padding(4.dp).scale(likeScale),
                    onClick = { likeBounce++; playerConnection.toggleLike() },
                )

                ResizableIconButton(
                    icon = when (repeatMode) {
                        Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                        else -> throw IllegalStateException()
                    },
                    color = TextBackgroundColor,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .scale(repeatScale)
                        .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                    onClick = {
                        repeatBounce++
                        playerConnection.player.toggleRepeatMode()
                    },
                )
            }
        }
        val immersiveControlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED
            val codecLabel = remember(currentFormat) {
                currentFormat?.mimeType
                    ?.substringAfter("/", missingDelimiterValue = "")
                    ?.uppercase()
                    ?.let { codec ->
                        when {
                            codec.contains("MP4A") -> "MP4"
                            codec.contains("AAC") -> "AAC"
                            codec.contains("OPUS") -> "OPUS"
                            codec.contains("VORBIS") -> "VORBIS"
                            codec.contains("FLAC") -> "LOSSLESS"
                            codec.isBlank() -> null
                            else -> codec
                        }
                    }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        AnimatedContent(
                            targetState = mediaMetadata.title,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "modernTitle",
                        ) { title ->
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .basicMarquee()
                                    .clickable(enabled = mediaMetadata.album != null) {
                                        navController.navigate("album/${mediaMetadata.album!!.id}")
                                        state.collapseSoft()
                                    },
                            )
                        }

                        AnimatedContent(
                            targetState = mediaMetadata.artists.joinToString { it.name },
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "modernArtists",
                        ) { artists ->
                            Text(
                                text = artists,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.86f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentSong?.song?.liked == true) Color.White.copy(alpha = 0.16f)
                                else Color.Transparent
                            )
                            .clickable(onClick = playerConnection::toggleLike)
                    ) {
                        Image(
                            painter = painterResource(
                                if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                if (currentSong?.song?.liked == true) Color.White else Color.White.copy(alpha = 0.75f)
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = mediaMetadata,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = { showDetailsDialog = true },
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.75f)),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = PlayerSliderColors.getSliderColors(
                                textButtonColor = Color.White,
                                playerBackground = playerBackground,
                                useDarkTheme = true
                            )
                        )
                    }

                    SliderStyle.SQUIGGLY -> {
                        SquigglySlider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = PlayerSliderColors.getSliderColors(
                                textButtonColor = Color.White,
                                playerBackground = playerBackground,
                                useDarkTheme = true
                            ),
                            squigglesSpec = SquigglySlider.SquigglesSpec(
                                amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                                strokeWidth = 3.dp,
                            ),
                        )
                    }

                    SliderStyle.SLIM -> {
                        Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = PlayerSliderColors.getSliderColors(
                                        textButtonColor = Color.White,
                                        playerBackground = playerBackground,
                                        useDarkTheme = true
                                    )
                                )
                            }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = makeTimeString(sliderPosition ?: position),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                    codecLabel?.let { label ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.14f))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(R.drawable.graphic_eq),
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(Color.White.copy(alpha = 0.78f)),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f),
                                )
                            }
                        }
                    }
                    Text(
                        text = if (duration != C.TIME_UNSET) "-${makeTimeString((duration - (sliderPosition ?: position)).coerceAtLeast(0L))}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ResizableIconButton(
                        icon = R.drawable.skip_previous,
                        enabled = canSkipPrevious,
                        color = Color.White.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                        modifier = Modifier.size(52.dp),
                        onClick = playerConnection::seekToPrevious,
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable {
                                if (playbackState == STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.player.togglePlayPause()
                                }
                            },
                    ) {
                        if (isLoading) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(44.dp),
                            )
                        } else {
                            Image(
                                painter = painterResource(
                                    if (playbackState == STATE_ENDED) {
                                        R.drawable.replay
                                    } else if (isPlaying) {
                                        R.drawable.pause
                                    } else {
                                        R.drawable.play
                                    },
                                ),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(52.dp),
                            )
                        }
                    }

                    ResizableIconButton(
                        icon = R.drawable.skip_next,
                        enabled = canSkipNext,
                        color = Color.White.copy(alpha = if (canSkipNext) 1f else 0.4f),
                        modifier = Modifier.size(52.dp),
                        onClick = playerConnection::seekToNext,
                    )
                }
            }
        }

        val selectedControlsContent =
            if (playerScreenStyle == PlayerScreenStyle.MODERN) immersiveControlsContent else controlsContent

        // Animated background effects
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background with blurred image
            AnimatedVisibility(
                visible = playerBackground == PlayerBackgroundStyle.BLUR && backgroundImageUrl != null,
                enter = fadeIn(tween(600)),
                exit = fadeOut(tween(400))
            ) {
                AsyncImage(
                    model = backgroundImageUrl?.highQualityThumbnail(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius)
                        .alpha(backgroundAlpha)
                )
            }

            // Animated gradient background
            AnimatedVisibility(
                visible = playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2,
                enter = fadeIn(tween(800)),
                exit = fadeOut(tween(600))
            ) {
                val animatedGradientColors = gradientColors.map { color ->
                    androidx.compose.animation.animateColorAsState(
                        targetValue = color,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "gradientColor"
                    ).value
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(backgroundAlpha)
                        .background(
                            Brush.verticalGradient(
                                colors = if (animatedGradientColors.isNotEmpty()) animatedGradientColors else gradientColors
                            )
                        )
                )
            }

            // Animated fluid background
            AnimatedVisibility(
                visible = playerBackground == PlayerBackgroundStyle.FLUID,
                enter = fadeIn(tween(800)),
                exit = fadeOut(tween(600))
            ) {
                FluidBackground(
                    modifier = Modifier.alpha(backgroundAlpha)
                )
            }

            // Animated dark overlay
            AnimatedVisibility(
                visible = overlayAlpha > 0f,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = overlayAlpha))
                )
            }

            // Additional overlay for lyrics
            if (playerBackground != PlayerBackgroundStyle.DEFAULT && showLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = animateFloatAsState(
                                    targetValue = if (state.isExpanded) 0.4f else 0f,
                                    animationSpec = tween(durationMillis = 500),
                                    label = "lyricsOverlay"
                                ).value
                            )
                        )
                )
            }
        }

        if (playerScreenStyle == PlayerScreenStyle.PAPER) {
            var volume by remember { mutableFloatStateOf(playerConnection.player.volume) }

            PaperPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                repeatMode = repeatMode,
                shuffleModeEnabled = shuffleModeEnabled,
                volume = volume,
                onVolumeChange = {
                    volume = it
                    playerConnection.player.volume = it
                },
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = playerConnection::seekToNext,
                onShuffle = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                onRepeat = { playerConnection.player.toggleRepeatMode() },
                onOpenLyrics = onOpenFullscreenLyrics,
                onOpenQueue = queueSheetState::expandSoft,
                onCollapse = state::collapseSoft,
                onOpenMenu = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
            )
        } else if (playerScreenStyle == PlayerScreenStyle.LIQUID) {
            LiquidPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                repeatMode = repeatMode,
                shuffleModeEnabled = shuffleModeEnabled,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = playerConnection::seekToNext,
                onShuffle = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                onRepeat = { playerConnection.player.toggleRepeatMode() },
                onOpenLyrics = onOpenFullscreenLyrics,
                onOpenQueue = queueSheetState::expandSoft,
                onCollapse = state::collapseSoft,
                onOpenArtist = { artistId ->
                    navController.navigate("artist/$artistId")
                    state.collapseSoft()
                },
                onOpenMenu = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                currentLyrics = currentLyrics,
                database = database,
                gradientColors = gradientColors,
            )
        } else if (playerScreenStyle == PlayerScreenStyle.CLOUDGLOW) {
            CloudGlowPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                repeatMode = repeatMode,
                shuffleModeEnabled = shuffleModeEnabled,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = playerConnection::seekToNext,
                onShuffle = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                onRepeat = { playerConnection.player.toggleRepeatMode() },
                onOpenLyrics = onOpenFullscreenLyrics,
                onOpenQueue = queueSheetState::expandSoft,
                onCollapse = state::collapseSoft,
                onOpenArtist = { artistId ->
                    navController.navigate("artist/$artistId")
                    state.collapseSoft()
                },
                onOpenMenu = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                currentLyrics = spotifyLyricsEntity,
                database = database,
                gradientColors = gradientColors,
            )
        } else if (playerScreenStyle == PlayerScreenStyle.FROST) {
            FrostPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                repeatMode = repeatMode,
                shuffleModeEnabled = shuffleModeEnabled,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = playerConnection::seekToNext,
                onShuffle = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                onRepeat = { playerConnection.player.toggleRepeatMode() },
                onOpenLyrics = onOpenFullscreenLyrics,
                onOpenQueue = queueSheetState::expandSoft,
                onCollapse = state::collapseSoft,
                onOpenArtist = { artistId ->
                    navController.navigate("artist/$artistId")
                    state.collapseSoft()
                },
                onOpenMenu = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                currentLyrics = spotifyLyricsEntity,
                database = database,
                gradientColors = gradientColors,
            )
        } else if (playerScreenStyle == PlayerScreenStyle.FOLD) {
            FoldPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                onPlayPause = {
                    if (playbackState == androidx.media3.common.Player.STATE_IDLE || playbackState == androidx.media3.common.Player.STATE_ENDED) {
                        playerConnection.player.seekToDefaultPosition()
                        playerConnection.player.playWhenReady = true
                    } else if (isPlaying) {
                        playerConnection.player.pause()
                    } else {
                        playerConnection.player.play()
                    }
                },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = { playerConnection.player.seekToNext() },
                onCollapse = state::collapseSoft,
                shuffleModeEnabled = shuffleModeEnabled,
                onShuffleClick = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                repeatMode = repeatMode,
                onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                onLyricsClick = onOpenFullscreenLyrics,
                onQueueClick = { queueSheetState.expandSoft() },
                onMenuClick = {
                    mediaMetadata?.let { metadata ->
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = metadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = { showDetailsDialog = true },
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                }
            )
        } else if (playerScreenStyle == PlayerScreenStyle.SPOTIFY) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                val spotifyScrollState = rememberScrollState()
                SpotifyPlayerBackdrop(
                    thumbnailUrl = mediaMetadata?.thumbnailUrl,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(state.preUpPostDownNestedScrollConnection)
                        .verticalScroll(spotifyScrollState)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 24.dp)
                        .padding(bottom = queueSheetState.collapsedBound + 28.dp),
                ) {
                    Spacer(Modifier.height(22.dp))

                    mediaMetadata?.let { metadata ->
                        SpotifyPlayerContent(
                            mediaMetadata = metadata,
                            position = sliderPosition ?: position,
                            duration = duration,
                            isPlaying = isPlaying,
                            isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                            canSkipPrevious = canSkipPrevious,
                            canSkipNext = canSkipNext,
                            repeatMode = repeatMode,
                            sliderStyle = sliderStyle,
                            lyricsPreview = (spotifyLyricsEntity ?: currentLyrics)?.lyrics,
                            onSeek = { sliderPosition = it },
                            onSeekFinished = {
                                sliderPosition?.let { playerConnection.player.seekTo(it) }
                                sliderPosition = null
                            },
                            onPlayPause = { playerConnection.player.togglePlayPause() },
                            onPrevious = { playerConnection.player.seekToPrevious() },
                            onNext = playerConnection::seekToNext,
                            onShuffle = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                            onRepeat = { playerConnection.player.toggleRepeatMode() },
                            onOpenLyrics = onOpenFullscreenLyrics,
                            onOpenQueue = queueSheetState::expandSoft,
                            onStartRadio = playerConnection.service::startRadioSeamlessly,
                            onCollapse = state::collapseSoft,
                            onOpenArtist = { artistId ->
                                navController.navigate("artist/$artistId")
                                state.collapseSoft()
                            },
                            onOpenMenu = {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = metadata,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = { showDetailsDialog = true },
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                            isLiked = currentSong?.song?.liked == true,
                            onLike = playerConnection::toggleLike,
                            onAddToPlaylist = { showChoosePlaylistDialog = true },
                            onShowDetails = { showDetailsDialog = true },
                            shuffleModeEnabled = shuffleModeEnabled,
                            currentSongArtists = currentSong?.artists ?: emptyList(),
                            surfaceColor = spotifySurfaceColor,
                        )
                    }
                }

                mediaMetadata?.let { metadata ->
                    AnimatedVisibility(
                        visible = spotifyScrollState.value > 980,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(180)),
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        SpotifyCollapsedHeader(
                            mediaMetadata = metadata,
                            surfaceColor = spotifyHeaderColor,
                            isLiked = currentSong?.song?.liked == true,
                            onLike = playerConnection::toggleLike,
                            onOpenMenu = {
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = metadata,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = { showDetailsDialog = true },
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        } else if (playerScreenStyle == PlayerScreenStyle.GROOVE) {
            GroovePlayer(
                state = state,
                playerConnection = playerConnection,
                mediaMetadata = mediaMetadata,
                playbackState = playbackState,
                duration = duration,
                position = position,
                sliderPosition = sliderPosition,
                onSliderPositionChange = { sliderPosition = it },
                onSliderPositionChangeFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                    }
                    sliderPosition = null
                },
                isPlaying = isPlaying,
                isLoading = playbackState != Player.STATE_READY && playbackState != Player.STATE_ENDED,
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = { playerConnection.player.seekToNext() },
                onCollapse = state::collapseSoft,
                shuffleModeEnabled = shuffleModeEnabled,
                onShuffleClick = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                repeatMode = repeatMode,
                onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                onLyricsClick = onOpenFullscreenLyrics,
                onMenuClick = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                }
            )
        } else if (playerScreenStyle == PlayerScreenStyle.POPSY) {
            PopsyPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                    }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = { playerConnection.player.seekToNext() },
                onCollapse = state::collapseSoft,
                onMenuClick = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                isLiked = currentSong?.song?.liked == true,
                onLikeClick = playerConnection::toggleLike,
                onAddToPlaylistClick = { showChoosePlaylistDialog = true },
                shuffleModeEnabled = shuffleModeEnabled,
                onShuffleClick = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                repeatMode = repeatMode,
                onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                onLyricsClick = onOpenFullscreenLyrics,
                onQueueClick = { queueSheetState.expandSoft() }
            )
        } else if (playerScreenStyle == PlayerScreenStyle.MINIMAL) {
            MinimalPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                nextMediaMetadata = nextMediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                    }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = { playerConnection.player.seekToNext() },
                onCollapse = state::collapseSoft,
                onMenuClick = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                isLiked = currentSong?.song?.liked == true,
                onLikeClick = playerConnection::toggleLike,
                repeatMode = repeatMode,
                onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                onQueueClick = { queueSheetState.expandSoft() }
            )
        } else if (playerScreenStyle == PlayerScreenStyle.COLOURFULL) {
            ColourfullPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                sliderStyle = sliderStyle,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                    }
                    sliderPosition = null
                },
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = { playerConnection.player.seekToNext() },
                onCollapse = state::collapseSoft,
                onMenuClick = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                isLiked = currentSong?.song?.liked == true,
                onLikeClick = playerConnection::toggleLike,
                repeatMode = repeatMode,
                onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                onQueueClick = { queueSheetState.expandSoft() }
            )
        } else if (playerScreenStyle == PlayerScreenStyle.APPLE) {
            ApplePlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = { playerConnection.player.seekToNext() },
                onCollapse = state::collapseSoft,
                onMenuClick = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                isLiked = currentSong?.song?.liked == true,
                onLikeClick = playerConnection::toggleLike,
                onQueueClick = { queueSheetState.expandSoft() },
                onShareClick = {
                    mediaMetadata?.let { metadata ->
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://play.airbeats.app/song?id=${metadata.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
                shuffleModeEnabled = shuffleModeEnabled,
                onShuffleClick = {
                    playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                },
                repeatMode = repeatMode,
                onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
            )
        } else if (playerScreenStyle == PlayerScreenStyle.IOS_STYLED) {
            IosStyledPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = { playerConnection.player.seekToNext() },
                onCollapse = state::collapseSoft,
                onMenuClick = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                isLiked = currentSong?.song?.liked == true,
                onLikeClick = playerConnection::toggleLike,
                onQueueClick = { queueSheetState.expandSoft() },
                onShareClick = {
                    mediaMetadata?.let { metadata ->
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://play.airbeats.app/song?id=${metadata.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
                shuffleModeEnabled = shuffleModeEnabled,
                onShuffleClick = {
                    playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                },
                repeatMode = repeatMode,
                onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                playerConnection = playerConnection,
                navController = navController,
                menuState = menuState,
                nextUpMetadata = nextMediaMetadata,
                currentFormat = currentFormat,
                playerVolume = playerVolume.value,
                onVolumeChange = { playerConnection.service.playerVolume.value = it },
            )
        } else if (playerScreenStyle == PlayerScreenStyle.GALAXY) {
            GalaxyPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
                position = sliderPosition ?: position,
                duration = duration,
                isPlaying = isPlaying,
                isLoading = playbackState != STATE_READY && playbackState != STATE_ENDED,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onSeek = { sliderPosition = it },
                onSeekFinished = {
                    sliderPosition?.let { playerConnection.player.seekTo(it) }
                    sliderPosition = null
                },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                onPrevious = { playerConnection.player.seekToPrevious() },
                onNext = { playerConnection.player.seekToNext() },
                onCollapse = state::collapseSoft,
                onMenuClick = {
                    menuState.show {
                        PlayerMenu(
                            mediaMetadata = mediaMetadata ?: return@show,
                            navController = navController,
                            playerBottomSheetState = state,
                            onShowDetailsDialog = { showDetailsDialog = true },
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
                isLiked = currentSong?.song?.liked == true,
                onLikeClick = playerConnection::toggleLike,
                onQueueClick = { queueSheetState.expandSoft() },
                onPlayQueueIndex = { index ->
                    playerConnection.player.seekTo(index, 0)
                },
                onShareClick = {
                    mediaMetadata?.let { metadata ->
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://play.airbeats.app/song?id=${metadata.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                },
                shuffleModeEnabled = shuffleModeEnabled,
                onShuffleClick = {
                    playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                },
                repeatMode = repeatMode,
                onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                queueWindows = queueWindows.map { it.mediaItem },
                currentWindowIndex = currentWindowIndex,
                lyrics = (spotifyLyricsEntity ?: currentLyrics)?.lyrics
            )
        } else if (playerScreenStyle == PlayerScreenStyle.MODERN) {
            val playbackOutputName = rememberPlaybackOutputName()

            Box(modifier = Modifier.fillMaxSize()) {
                ImmersivePlayerBackdrop(
                    thumbnailUrl = mediaMetadata?.thumbnailUrl,
                    modifier = Modifier.fillMaxSize()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 58.dp)
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
                ) {
                    mediaMetadata?.let {
                        selectedControlsContent(it)
                    }

                    Spacer(Modifier.height(34.dp))

                    ImmersiveBottomActions(
                        textColor = Color.White,
                        onOpenQueue = queueSheetState::expandSoft,
                        onOpenLyrics = onOpenFullscreenLyrics,
                        onDeviceClick = {
                            Toast.makeText(context, playbackOutputName, Toast.LENGTH_SHORT).show()
                        },
                        deviceName = playbackOutputName,
                    )
                }
            }
        } else when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(top = queueSheetState.collapsedBound)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        val screenWidth = LocalConfiguration.current.screenWidthDp
                        val thumbnailSize = (screenWidth * 0.4).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                modifier = Modifier.size(thumbnailSize)
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(1f)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            selectedControlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = queueSheetState.collapsedBound)
                            .bottomSheetDraggable(state),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection)
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                            )
                        }
                    }

                    mediaMetadata?.let {
                        selectedControlsContent(it)
                    }

                    Spacer(Modifier.height(30.dp))
                }
            }
        }

        if (playerScreenStyle == PlayerScreenStyle.APPLE) {
            AppleQueue(
                state = queueSheetState
            )
        } else if (playerScreenStyle == PlayerScreenStyle.CLASSIC || !queueSheetState.isCollapsed) {
            val bgCol = if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.surfaceContainer
            
            if (enableNewQueueScreen) {
                AlternateQueue(
                    state = queueSheetState,
                    playerBottomSheetState = state,
                    navController = navController,
                    backgroundColor = bgCol,
                    onBackgroundColor = onBackgroundColor,
                    TextBackgroundColor = TextBackgroundColor,
                    textButtonColor = textButtonColor,
                    iconButtonColor = iconButtonColor,
                    pureBlack = pureBlack,
                )
            } else {
                Queue(
                    state = queueSheetState,
                    playerBottomSheetState = state,
                    navController = navController,
                    backgroundColor = bgCol,
                    onBackgroundColor = onBackgroundColor,
                    textBackgroundColor = TextBackgroundColor,
                )
            }
        }
    }
}

@Composable
private fun SpotifyPlayerBackdrop(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.Black)) {
        AsyncImage(
            model = thumbnailUrl?.highQualityThumbnail(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(110.dp)
                .alpha(0.42f)
                .scale(1.14f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.1f),
                            0.42f to Color.Black.copy(alpha = 0.5f),
                            1f to Color.Black,
                        )
                    )
                )
        )
    }
}

@Composable
private fun SpotifyPlayerContent(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    repeatMode: Int,
    sliderStyle: SliderStyle,
    lyricsPreview: String?,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onStartRadio: () -> Unit,
    onCollapse: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenMenu: () -> Unit,
    isLiked: Boolean,
    onLike: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowDetails: () -> Unit,
    shuffleModeEnabled: Boolean,
    currentSongArtists: List<ArtistEntity>,
    surfaceColor: Color,
) {
    val safeDuration = duration.takeIf { it > 0 } ?: 0L
    val sliderValue = if (safeDuration > 0) position.coerceIn(0L, safeDuration).toFloat() else 0f
    val artists = mediaMetadata.artists.joinToString { it.name }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SpotifyRoundIconButton(
                icon = R.drawable.expand_more,
                transparent = true,
                onClick = onCollapse,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.now_playing).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = SpotifyFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = "\"${mediaMetadata.title}\" in Search",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SpotifyFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                )
            }
            SpotifyRoundIconButton(
                icon = R.drawable.more_vert,
                transparent = true,
                onClick = onOpenMenu,
            )
        }
    }

    Spacer(Modifier.height(78.dp))

    AsyncImage(
        model = mediaMetadata.thumbnailUrl?.highQualityThumbnail(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    )

    Spacer(Modifier.height(54.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = SpotifyFontFamily, fontSize = 25.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = artists,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SpotifyFontFamily, fontSize = 18.sp, fontWeight = FontWeight.Normal),
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
        SpotifyRoundIconButton(
            icon = if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
            transparent = true,
            tint = if (isLiked) Color(0xFF1DB954) else Color.White,
            onClick = onLike
        )
    }

    Spacer(Modifier.height(18.dp))

    SpotifyStyledSlider(
        value = sliderValue,
        duration = safeDuration,
        sliderStyle = sliderStyle,
        isPlaying = isPlaying,
        onSeek = onSeek,
        onSeekFinished = onSeekFinished,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = makeTimeString(position.coerceAtLeast(0L)),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.68f),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = makeTimeString(safeDuration),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.68f),
        )
    }

    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpotifyPlainIconButton(
            if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle,
            onShuffle,
            if (shuffleModeEnabled) Color(0xFF1DB954) else Color.White,
        )
        SpotifyPlainIconButton(
            R.drawable.skip_previous,
            onPrevious,
            if (canSkipPrevious) Color.White else Color.White.copy(alpha = 0.32f),
        )
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(if (isPlaying && !isLoading) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.Black),
                modifier = Modifier.size(38.dp)
            )
        }
        SpotifyPlainIconButton(
            R.drawable.skip_next,
            onNext,
            if (canSkipNext) Color.White else Color.White.copy(alpha = 0.32f),
        )
        SpotifyPlainIconButton(
            if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
            onRepeat,
            if (repeatMode == Player.REPEAT_MODE_OFF) Color.White else Color(0xFF1DB954),
        )
    }

    Spacer(Modifier.height(22.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpotifyRoundIconButton(icon = R.drawable.info, transparent = true, onClick = onShowDetails)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            SpotifyRoundIconButton(icon = R.drawable.playlist_add, transparent = true, onClick = onAddToPlaylist)
            SpotifyRoundIconButton(icon = R.drawable.queue_music, transparent = true, onClick = onOpenQueue)
        }
    }

    Spacer(Modifier.height(26.dp))

    SpotifyLyricsPreviewCard(
        lyrics = lyricsPreview,
        position = position,
        surfaceColor = surfaceColor,
        onClick = onOpenLyrics,
    )

    Spacer(Modifier.height(12.dp))

    mediaMetadata.artists.forEach { artist ->
        val artistEntity = currentSongArtists.find { it.id == artist.id }
        val thumbnailUrl = artistEntity?.thumbnailUrl?.takeIf { it.isNotBlank() } ?: mediaMetadata.thumbnailUrl
        SpotifyArtistCard(
            artist = artist.name,
            thumbnailUrl = thumbnailUrl,
            onClick = {
                artist.id?.let { artistId ->
                    onOpenArtist(artistId)
                }
            },
        )
        Spacer(Modifier.height(12.dp))
    }

    Spacer(Modifier.height(12.dp))

    SpotifyDetailsCard(
        mediaMetadata = mediaMetadata,
        artists = artists,
        surfaceColor = surfaceColor,
        onClick = onOpenMenu,
    )
}

@Composable
private fun SpotifyActionTile(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SpotifyCollapsedHeader(
    mediaMetadata: MediaMetadata,
    surfaceColor: Color,
    isLiked: Boolean,
    onLike: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor.copy(alpha = 0.96f))
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
            .padding(horizontal = 36.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = mediaMetadata.thumbnailUrl?.highQualityThumbnail(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(45.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaMetadata.title,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = SpotifyFontFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = mediaMetadata.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpotifyFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal),
                color = Color.White.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
        }
        Spacer(Modifier.width(22.dp))
        SpotifyRoundIconButton(
            icon = if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
            transparent = true,
            tint = if (isLiked) Color(0xFF1DB954) else Color.White,
            onClick = onLike
        )
        Spacer(Modifier.width(18.dp))
        SpotifyRoundIconButton(icon = R.drawable.more_vert, transparent = true, onClick = onOpenMenu)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotifyStyledSlider(
    value: Float,
    duration: Long,
    sliderStyle: SliderStyle,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val valueRange = 0f..duration.coerceAtLeast(1L).toFloat()
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.26f),
    )

    when (sliderStyle) {
        SliderStyle.DEFAULT -> {
            Slider(
                value = value,
                valueRange = valueRange,
                onValueChange = { onSeek(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                colors = sliderColors,
                modifier = modifier,
            )
        }

        SliderStyle.SQUIGGLY -> {
            SquigglySlider(
                value = value,
                valueRange = valueRange,
                onValueChange = { onSeek(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                colors = PlayerSliderColors.getSliderColors(
                    textButtonColor = Color.White,
                    playerBackground = PlayerBackgroundStyle.DEFAULT,
                    useDarkTheme = true,
                ),
                modifier = modifier,
                squigglesSpec = SquigglySlider.SquigglesSpec(
                    amplitude = if (isPlaying) 2.dp else 0.dp,
                    strokeWidth = 3.dp,
                ),
            )
        }

        SliderStyle.SLIM -> {
            Slider(
                value = value,
                valueRange = valueRange,
                onValueChange = { onSeek(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = sliderColors,
                    )
                },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SpotifyLyricsPreviewCard(
    lyrics: String?,
    position: Long,
    surfaceColor: Color,
    onClick: () -> Unit,
) {
    val lyricEntries = remember(lyrics) {
        when {
            lyrics.isNullOrBlank() || lyrics == LYRICS_NOT_FOUND -> emptyList()
            lyrics.startsWith("[") -> parseLyrics(lyrics)
            else -> lyrics.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapIndexed { index, line -> LyricsEntry(index * 2500L, line) }
        }
    }
    val activeLineIndex = remember(lyricEntries, position) {
        lyricEntries.indexOfLast { it.time <= position }.coerceAtLeast(0)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeLineIndex, lyricEntries.size) {
        if (lyricEntries.isNotEmpty()) {
            listState.animateScrollToItem(activeLineIndex.coerceAtMost(lyricEntries.lastIndex))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColor.copy(alpha = 0.9f))
            .clickable(onClick = onClick)
            .padding(15.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.lyrics),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = SpotifyFontFamily, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "Show",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }

            Spacer(Modifier.height(26.dp))

            if (lyricEntries.isEmpty()) {
                Text(
                    text = stringResource(R.string.lyrics_not_found),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White.copy(alpha = 0.45f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        userScrollEnabled = false,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(lyricEntries.size) { index ->
                            val isCurrent = index == activeLineIndex
                            val isPast = index < activeLineIndex
                            Text(
                                text = lyricEntries[index].text,
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SpotifyFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                color = when {
                                    isCurrent -> Color.White
                                    isPast -> Color.White.copy(alpha = 0.5f)
                                    else -> Color.White.copy(alpha = 0.25f)
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        item {
                            Spacer(Modifier.height(150.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = if (lyricEntries.isEmpty()) "" else "Line Synced\nLyrics provided by LRCLIB",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpotifyFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Normal),
                color = Color.White.copy(alpha = 0.68f),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun SpotifyArtistCard(
    artist: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = thumbnailUrl?.highQualityThumbnail(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.82f),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.66f))
                    )
                )
        )
        Text(
            text = stringResource(R.string.artists),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(28.dp),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(28.dp),
        ) {
            Text(
                text = artist,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Text(
                text = "Artist",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SpotifyDetailsCard(
    mediaMetadata: MediaMetadata,
    artists: String,
    surfaceColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceColor.copy(alpha = 0.9f))
            .clickable(onClick = onClick)
            .padding(28.dp),
    ) {
        Column {
            Text(
                text = mediaMetadata.album?.title?.takeIf { it.isNotBlank() } ?: mediaMetadata.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = makeTimeString(mediaMetadata.duration * 1000L),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = artists,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.62f),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Description",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SpotifyFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = listOf(mediaMetadata.title, artists).joinToString(" • "),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.62f),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "More",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = SpotifyFontFamily, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    color = Color.White,
            )
        }
    }
}

@Composable
private fun SpotifyRoundIconButton(
    icon: Int,
    transparent: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (transparent) Color.Transparent else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun SpotifyPlainIconButton(
    icon: Int,
    onClick: () -> Unit,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
private fun ImmersivePlayerBackdrop(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.Black)) {
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(900)) },
            label = "immersiveBackdrop",
        ) { artworkUrl ->
            if (artworkUrl != null) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = artworkUrl.highQualityThumbnail(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.06f
                                scaleY = 1.06f
                                alpha = 0.82f
                            }
                    )

                    AsyncImage(
                        model = artworkUrl.highQualityThumbnail(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(90.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithCache {
                                val blurMask = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        0.46f to Color.Transparent,
                                        0.58f to Color.Black.copy(alpha = 0.6f),
                                        0.72f to Color.Black,
                                        1f to Color.Black,
                                    )
                                )

                                onDrawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = blurMask,
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                            }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.08f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.34f to Color.Transparent,
                            0.64f to Color.Black.copy(alpha = 0.22f),
                            1f to Color.Black.copy(alpha = 0.82f),
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.56f to Color.Transparent,
                            0.8f to MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                            1f to MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                        )
                    )
                )
        )
    }
}

@Composable
private fun ImmersiveBottomActions(
    textColor: Color,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onDeviceClick: () -> Unit,
    deviceName: String,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImmersiveCircleButton(
                icon = R.drawable.queue_music,
                textColor = textColor,
                onClick = onOpenQueue,
            )
            ImmersiveCircleButton(
                icon = R.drawable.lyrics,
                textColor = textColor,
                onClick = onOpenLyrics,
            )
        }

        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(textColor.copy(alpha = 0.10f))
                .clickable(onClick = onDeviceClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.volume_up),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun rememberPlaybackOutputName(): String {
    val context = LocalContext.current
    var outputName by remember { mutableStateOf(resolvePlaybackOutputName(context)) }

    LaunchedEffect(context) {
        while (isActive) {
            outputName = resolvePlaybackOutputName(context)
            delay(2000)
        }
    }

    return outputName
}

@Suppress("DEPRECATION")
private fun resolvePlaybackOutputName(context: Context): String {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val devices =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
            } catch (_: SecurityException) {
                emptyArray()
            }
        } else {
            emptyArray()
        }

    devices.firstOrNull { it.isBluetoothOutput() }?.productName?.toString()?.takeIf {
        it.isNotBlank()
    }?.let {
        return it
    }

    val selectedRouteName =
        try {
            val mediaRouter = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as? MediaRouter
            mediaRouter
                ?.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
                ?.name
                ?.toString()
        } catch (_: Exception) {
            null
        }

    selectedRouteName
        ?.takeIf { it.isNotBlank() && !it.equals("Phone", ignoreCase = true) }
        ?.let { return it }

    devices.firstOrNull { it.isWiredOutput() }?.productName?.toString()?.takeIf {
        it.isNotBlank()
    }?.let {
        return it
    }

    devices.firstOrNull { it.isUsbOutput() }?.productName?.toString()?.takeIf {
        it.isNotBlank()
    }?.let {
        return it
    }

    return "Speaker"
}

private fun AudioDeviceInfo.isBluetoothOutput(): Boolean =
    type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                type == AudioDeviceInfo.TYPE_BLE_BROADCAST))

private fun AudioDeviceInfo.isWiredOutput(): Boolean =
    type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
        type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
        type == AudioDeviceInfo.TYPE_AUX_LINE

private fun AudioDeviceInfo.isUsbOutput(): Boolean =
    type == AudioDeviceInfo.TYPE_USB_DEVICE ||
        type == AudioDeviceInfo.TYPE_USB_HEADSET ||
        type == AudioDeviceInfo.TYPE_DOCK

@Composable
private fun ImmersiveCircleButton(
    icon: Int,
    textColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(textColor.copy(alpha = 0.10f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(textColor),
            modifier = Modifier.size(25.dp)
        )
    }
}

@Composable
fun ConcentricWaveEffect(
    isPlaying: Boolean,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (colors.isEmpty()) return

    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    // Slower animation (8 seconds instead of 5)
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,   // ← slow speed
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveProgress"
    )

    Canvas(modifier = modifier) {

        val circleCount = 24   // ← increased from 14 to 24
        val maxRadius = size.minDimension / 2f

        repeat(circleCount) { index ->

            // 🚫 If paused → freeze animation
            val progress = if (isPlaying) {
                (animatedProgress + index / circleCount.toFloat()) % 1f
            } else {
                0f
            }

            val radius = maxRadius * progress

            drawCircle(
                color = colors[index % colors.size]
                    .copy(alpha = if (isPlaying) 1f - progress else 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
fun LiquidPlayer(
    state: BottomSheetState,
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onCollapse: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenMenu: () -> Unit,
    currentLyrics: LyricsEntity?,
    database: MusicDatabase,
    dynamicColor: Color? = null,
    gradientColors: List<Color> = emptyList(),
) {
    FuturisticPlayer(
                state = state,
                mediaMetadata = mediaMetadata,
        position = position,
        duration = duration,
        isPlaying = isPlaying,
        isLoading = isLoading,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        repeatMode = repeatMode,
        shuffleModeEnabled = shuffleModeEnabled,
        onSeek = onSeek,
        onSeekFinished = onSeekFinished,
        onPlayPause = onPlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onShuffle = onShuffle,
        onRepeat = onRepeat,
        onOpenLyrics = onOpenLyrics,
        onOpenQueue = onOpenQueue,
        onCollapse = onCollapse,
        onOpenMenu = onOpenMenu,
        dynamicColor = dynamicColor,
        gradientColors = gradientColors
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudGlowPlayer(
    state: BottomSheetState,
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onCollapse: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenMenu: () -> Unit,
    currentLyrics: LyricsEntity?,
    database: MusicDatabase,
    dynamicColor: Color? = null,
    gradientColors: List<Color> = emptyList(),
) {
    CloudGlowPlayerScreen(
                state = state,
        mediaMetadata = mediaMetadata,
        position = position,
        duration = duration,
        isPlaying = isPlaying,
        isLoading = isLoading,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        repeatMode = repeatMode,
        shuffleModeEnabled = shuffleModeEnabled,
        onSeek = onSeek,
        onSeekFinished = onSeekFinished,
        onPlayPause = onPlayPause,
        onPrevious = onPrevious,
        onNext = onNext,
        onShuffle = onShuffle,
        onRepeat = onRepeat,
        onOpenLyrics = onOpenLyrics,
        onOpenQueue = onOpenQueue,
        onCollapse = onCollapse,
        onOpenMenu = onOpenMenu,
        dynamicColor = dynamicColor,
        gradientColors = gradientColors
    )
}

@Composable
fun CurvedProgressSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val safeDuration = duration.takeIf { it > 0 } ?: 1000L
    val progress = (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    // Arc geometry constants
    val startAngle = 160f
    val sweepAngle = 220f
    val radiusDp = 110.dp
    val strokeWidthDp = 4.dp

    Box(
        modifier = modifier
            .size(260.dp)
            .pointerInput(safeDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                        val angle = calculateAngle(offset, center)
                        val newProgress = calculateProgressFromAngle(angle, startAngle, sweepAngle)
                        onSeek((newProgress * safeDuration).toLong())
                    },
                    onDrag = { change, _ ->
                        val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                        val angle = calculateAngle(change.position, center)
                        val newProgress = calculateProgressFromAngle(angle, startAngle, sweepAngle)
                        onSeek((newProgress * safeDuration).toLong())
                    },
                    onDragEnd = {
                        onSeekFinished()
                    }
                )
            }
            .pointerInput(safeDuration) {
                detectTapGestures { offset ->
                    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                    val angle = calculateAngle(offset, center)
                    val newProgress = calculateProgressFromAngle(angle, startAngle, sweepAngle)
                    onSeek((newProgress * safeDuration).toLong())
                    onSeekFinished()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size
            val radius = radiusDp.toPx()
            val strokeWidth = strokeWidthDp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)
            val rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)

            // 1. Draw background arc track (thick glowing blue)
            drawArc(
                color = Color(0xFF0F1B5F).copy(alpha = 0.55f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Draw active progress track (white)
            drawArc(
                color = Color.White,
                startAngle = startAngle,
                sweepAngle = sweepAngle * progress,
                useCenter = false,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Draw white thumb dot at the end of progress
            val thumbAngleRad = Math.toRadians((startAngle + sweepAngle * progress).toDouble())
            val thumbX = center.x + radius * Math.cos(thumbAngleRad).toFloat()
            val thumbY = center.y + radius * Math.sin(thumbAngleRad).toFloat()
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
            drawCircle(
                color = Color(0xFF0F1B5F),
                radius = 3.dp.toPx(),
                center = Offset(thumbX, thumbY)
            )
        }
    }
}

// Helpers for angle mapping
private fun calculateAngle(point: Offset, center: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0) {
        angle += 360f
    }
    return angle
}

private fun calculateProgressFromAngle(angle: Float, startAngle: Float, sweepAngle: Float): Float {
    var normalizedAngle = angle
    if (normalizedAngle < startAngle - 40f) {
        normalizedAngle += 360f
    }
    return ((normalizedAngle - startAngle) / sweepAngle).coerceIn(0f, 1f)
}

@Composable
fun ConcentricGlowRings(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "rings")
    val pulseScale1 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Scale"
    )
    val pulseAlpha1 by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1Alpha"
    )

    val pulseScale2 by transition.animateFloat(
        initialValue = 1f,
        targetValue = 2.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearOutSlowInEasing),
            initialStartOffset = StartOffset(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Scale"
    )
    val pulseAlpha2 by transition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearOutSlowInEasing),
            initialStartOffset = StartOffset(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2Alpha"
    )

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying) {
            // Outer pulsing ring 1
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        scaleX = pulseScale1
                        scaleY = pulseScale1
                        alpha = pulseAlpha1
                    }
                    .border(
                        width = 1.5.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFF0055FF).copy(alpha = 0.1f), Color.Transparent),
                        ),
                        shape = CircleShape
                    )
            )

            // Outer pulsing ring 2
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        scaleX = pulseScale2
                        scaleY = pulseScale2
                        alpha = pulseAlpha2
                    }
                    .border(
                        width = 1.5.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFF0055FF).copy(alpha = 0.1f), Color.Transparent),
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Static glowing base ring
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0055FF).copy(alpha = 0.3f),
                            Color(0xFF0F1B5F).copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun LiquidControlBarBackground(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.height(64.dp).fillMaxWidth()) {
        val h = size.height
        val w = size.width
        val r = h / 2f
        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(w / 2f - 60.dp.toPx(), 0f)
            // Dip down in the center
            cubicTo(
                w / 2f - 30.dp.toPx(), 0f,
                w / 2f - 35.dp.toPx(), h * 0.45f,
                w / 2f, h * 0.45f
            )
            cubicTo(
                w / 2f + 35.dp.toPx(), h * 0.45f,
                w / 2f + 30.dp.toPx(), 0f,
                w / 2f + 60.dp.toPx(), 0f
            )
            lineTo(w - r, 0f)
            // Right cap
            arcTo(
                rect = Rect(w - 2 * r, 0f, w, h),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(w / 2f + 60.dp.toPx(), h)
            // Dip up in the center
            cubicTo(
                w / 2f + 30.dp.toPx(), h,
                w / 2f + 35.dp.toPx(), h * 1.45f,
                w / 2f, h * 1.45f
            )
            cubicTo(
                w / 2f - 35.dp.toPx(), h * 1.45f,
                w / 2f - 30.dp.toPx(), h,
                w / 2f - 60.dp.toPx(), h
            )
            lineTo(r, h)
            // Left cap
            arcTo(
                rect = Rect(0f, 0f, 2 * r, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }

        // Draw background glass
        drawPath(
            path = path,
            color = Color.White.copy(alpha = 0.05f)
        )
        // Draw soft glowing cyan/blue outer border (wide)
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.25f),
                    Color(0xFF0055FF).copy(alpha = 0.25f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 4.dp.toPx())
        )
        // Draw sharp glowing cyan/blue inner border (thin)
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.65f),
                    Color(0xFF0055FF).copy(alpha = 0.65f)
                )
            ),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

