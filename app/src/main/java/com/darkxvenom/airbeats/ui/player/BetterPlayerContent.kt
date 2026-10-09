package com.darkxvenom.airbeats.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.entities.FormatEntity
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.playback.PlayerConnection
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.makeTimeString
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import kotlin.math.abs

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BetterPlayerContent(
    mediaMetadata: MediaMetadata,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    textBackgroundColor: Color, // accent (text/icon color, soft pastel)
    textButtonColor: Color,     // field (background fill color, deep tone)
    onCollapseClick: () -> Unit,
    onQueueClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    onSleepTimerClick: () -> Unit,
    sleepTimerEnabled: Boolean,
    sleepTimerTimeLeft: Long,
    onMenuClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    currentFormat: FormatEntity? = null,
    modifier: Modifier = Modifier,
) {
    val artworkUrl = mediaMetadata.thumbnailUrl?.highQualityThumbnail()
    val onPlayPauseClick = {
        if (playbackState == STATE_ENDED) {
            playerConnection.player.seekTo(0, 0)
            playerConnection.player.playWhenReady = true
        } else {
            playerConnection.player.togglePlayPause()
        }
    }

    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val liked = currentSong?.song?.liked == true
    val onToggleLike = playerConnection::toggleLike

    // The two-tone contract: field + accent
    val accent = textBackgroundColor
    val field = textButtonColor

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(field)
    ) {
        // ========== TOP BAR ==========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BetterCircleButton(
                onClick = onCollapseClick,
                accent = accent,
                field = field,
                size = 44.dp
            ) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = "Collapse",
                    modifier = Modifier.size(26.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sleepTimerEnabled) {
                    val countdownText = makeTimeString(sleepTimerTimeLeft.coerceAtLeast(0L))
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .clickable(onClick = onSleepTimerClick),
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(LocalContentColor provides field) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = countdownText,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(R.drawable.bedtime),
                                    contentDescription = "Sleep timer",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    BetterCircleButton(
                        onClick = onSleepTimerClick,
                        accent = field,
                        field = accent,
                        size = 44.dp
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.bedtime),
                            contentDescription = "Sleep timer",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                BetterCircleButton(
                    onClick = onLyricsClick,
                    accent = accent,
                    field = field,
                    size = 44.dp
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics),
                        contentDescription = "Lyrics",
                        modifier = Modifier.size(22.dp)
                    )
                }

                BetterCircleButton(
                    onClick = onMenuClick,
                    accent = accent,
                    field = field,
                    size = 44.dp
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "More",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ========== DIE-CUT ART ==========
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f),
            contentAlignment = Alignment.Center
        ) {
            BetterDieCutArt(
                artworkUrl = artworkUrl,
                mediaMetadataId = mediaMetadata.id,
                isPlaying = isPlaying,
                accent = accent,
                field = field
            )
        }

        // ========== HEADLINE ==========
        val title = mediaMetadata.title
        val headlineBase = when {
            title.length <= 12 -> MaterialTheme.typography.displayLarge
            title.length <= 24 -> MaterialTheme.typography.displayMedium
            else -> MaterialTheme.typography.displaySmall
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = headlineBase.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold
                ),
                color = accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {
                            mediaMetadata.album?.let { album ->
                                navController.navigate("album/${album.id}")
                                state.collapseSoft()
                            }
                        }
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (mediaMetadata.explicit) {
                    Icon(
                        painter = painterResource(R.drawable.explicit),
                        contentDescription = "Explicit",
                        tint = accent.copy(alpha = 0.8f),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(16.dp)
                    )
                }
                BetterClickableArtists(
                    artists = mediaMetadata.artists,
                    onArtistClick = { artistId ->
                        navController.navigate("artist/$artistId")
                        state.collapseSoft()
                    },
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = accent.copy(alpha = 0.8f),
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 2000
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ========== CONTROL CLUSTER (Asymmetric Bento) ==========
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Row 1: Word pill + Next circle
            val haptic = LocalHapticFeedback.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .clickable(onClick = onPlayPauseClick),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(LocalContentColor provides field) {
                        if (isLoading && !isPlaying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = field,
                                strokeWidth = 3.dp
                            )
                        } else {
                            AnimatedContent(
                                targetState = isPlaying,
                                label = "BetterPlayWord"
                            ) { playing ->
                                Text(
                                    text = if (playing) "PAUSE" else "PLAY",
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 3.sp
                                    )
                                )
                            }
                        }
                    }
                }

                BetterCircleButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playerConnection.seekToNext()
                    },
                    accent = accent,
                    field = field,
                    size = 80.dp
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Previous circle + Progress line with times
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                BetterCircleButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playerConnection.seekToPrevious()
                    },
                    accent = accent,
                    field = field,
                    size = 80.dp
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    var scrubPosition by remember { mutableStateOf<Float?>(null) }
                    val displayedProgress = scrubPosition?.toLong() ?: (sliderPosition ?: position)
                    val progressFraction =
                        if (duration > 0) displayedProgress.toFloat() / duration.toFloat() else 0f
                    val animatedProgress by animateFloatAsState(
                        targetValue = progressFraction,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "BetterProgress"
                    )
                    val lineStroke = Stroke(
                        width = with(LocalDensity.current) { 4.dp.toPx() },
                        cap = StrokeCap.Round
                    )

                    Box(contentAlignment = Alignment.Center) {
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp),
                            color = accent,
                            trackColor = accent.copy(alpha = 0.35f),
                            stroke = lineStroke,
                            trackStroke = lineStroke,
                            amplitude = { if (isPlaying) 1f else 0f }
                        )
                        Slider(
                            value = scrubPosition ?: (sliderPosition ?: position).toFloat(),
                            onValueChange = { scrubPosition = it },
                            onValueChangeFinished = {
                                scrubPosition?.let { onSliderValueChange(it.toLong()) }
                                onSliderValueChangeFinished()
                                scrubPosition = null
                            },
                            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Transparent,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatBetterTime(displayedProgress.coerceAtLeast(0L)),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent.copy(alpha = 0.8f)
                        )
                        Text(
                            text = formatBetterTime(duration.coerceAtLeast(0L)),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Audio specs tag: e.g. WEBM • 145 kbps • 2.7 MB
            val audioFormatSpec = remember(currentFormat) {
                if (currentFormat == null) ""
                else {
                    val codec = currentFormat.mimeType.substringAfter("/").uppercase()
                    val bitrate = if (currentFormat.bitrate > 0) "${currentFormat.bitrate / 1000} kbps" else null
                    val fileSize = if (currentFormat.contentLength > 0) {
                        String.format(java.util.Locale.US, "%.1f MB", currentFormat.contentLength / 1024.0 / 1024.0)
                    } else null
                    listOfNotNull(codec.takeIf { it.isNotBlank() }, bitrate, fileSize).joinToString(" • ")
                }
            }

            if (audioFormatSpec.isNotBlank()) {
                Text(
                    text = audioFormatSpec,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = accent.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }

            // ========== BOTTOM CONTROL ROW (5 SQUIRCLE BUTTONS) ==========
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount < -15) {
                                change.consume()
                                onQueueClick()
                            }
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Queue
                BetterSquircleButton(
                    checked = false,
                    onClick = onQueueClick,
                    accent = accent,
                    field = field,
                    iconResId = R.drawable.list,
                    contentDescription = "Queue",
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                // Button 2: Shuffle
                BetterSquircleButton(
                    checked = shuffleModeEnabled,
                    onClick = { playerConnection.toggleShuffle() },
                    accent = accent,
                    field = field,
                    iconResId = R.drawable.shuffle,
                    contentDescription = "Shuffle",
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                // Button 3: Repeat
                BetterSquircleButton(
                    checked = repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = { playerConnection.toggleRepeatMode() },
                    accent = accent,
                    field = field,
                    iconResId = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat,
                    contentDescription = "Repeat",
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                // Button 4: Like (Heart)
                BetterSquircleButton(
                    checked = liked,
                    onClick = onToggleLike,
                    accent = accent,
                    field = field,
                    iconResId = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = "Like",
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                // Button 5: Add to Playlist
                BetterSquircleButton(
                    checked = false,
                    onClick = onAddToPlaylistClick,
                    accent = accent,
                    field = field,
                    iconResId = R.drawable.library_add,
                    contentDescription = "Add to playlist",
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier
                .navigationBarsPadding()
                .height(6.dp)
        )
    }
}

// ========== HELPERS ==========

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BetterDieCutArt(
    artworkUrl: String?,
    mediaMetadataId: String,
    isPlaying: Boolean,
    accent: Color,
    field: Color
) {
    val dieCuts = remember {
        listOf(
            MaterialShapes.Flower,
            MaterialShapes.Clover4Leaf,
            MaterialShapes.Puffy,
            MaterialShapes.Cookie12Sided,
            MaterialShapes.SoftBurst
        )
    }
    val targetPolygon = remember(mediaMetadataId) {
        dieCuts[abs(mediaMetadataId.hashCode()) % dieCuts.size]
    }

    var morphFrom by remember { mutableStateOf(targetPolygon) }
    var morphTo by remember { mutableStateOf(targetPolygon) }
    val morphProgress = remember { Animatable(1f) }
    LaunchedEffect(targetPolygon) {
        if (targetPolygon !== morphTo) {
            morphFrom = morphTo
            morphTo = targetPolygon
            morphProgress.snapTo(0f)
            morphProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }
    val morph = remember(morphFrom, morphTo) { Morph(morphFrom, morphTo) }
    val dieCutShape = remember(morph, morphProgress.value) {
        BetterMorphShape(morph, morphProgress.value)
    }

    val artScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "BetterArtScale"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (maxWidth < 50.dp || maxHeight < 50.dp) return@BoxWithConstraints
        val artSize = minOf(maxWidth, maxHeight) * 0.95f

        Box(
            modifier = Modifier
                .size(artSize)
                .graphicsLayer {
                    scaleX = artScale
                    scaleY = artScale
                }
                .clip(dieCutShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = field,
                    modifier = Modifier.size(artSize * 0.3f)
                )
            }
        }
    }
}

@Composable
internal fun BetterCircleButton(
    onClick: () -> Unit,
    accent: Color,
    field: Color,
    size: Dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CircleButtonScale"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(accent)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = field.copy(alpha = 0.2f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides field) {
            content()
        }
    }
}

@Composable
private fun BetterSquircleButton(
    checked: Boolean,
    onClick: () -> Unit,
    accent: Color,
    field: Color,
    iconResId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (checked) accent else accent.copy(alpha = 0.08f),
        label = "BetterSquircleButtonBg"
    )
    val contentColor = if (checked) field else accent.copy(alpha = 0.85f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
    }
}

internal class BetterMorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = morph.toPath(progress).asComposePath()
        val matrix = Matrix()
        val bounds = morph.calculateBounds()
        val boundsWidth = bounds[2] - bounds[0]
        val boundsHeight = bounds[3] - bounds[1]

        matrix.scale(size.width / boundsWidth, size.height / boundsHeight)
        matrix.translate(-bounds[0], -bounds[1])
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

internal fun formatBetterTime(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
fun BetterClickableArtists(
    artists: List<MediaMetadata.Artist>,
    onArtistClick: (artistId: String) -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val annotatedString = remember(artists) {
        buildAnnotatedString {
            artists.forEachIndexed { index, artist ->
                pushStringAnnotation(tag = "artist_${artist.id.orEmpty()}", annotation = artist.id.orEmpty())
                append(artist.name)
                pop()
                if (index != artists.lastIndex) append(", ")
            }
        }
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedString,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layoutResult = it },
        modifier = modifier.pointerInput(annotatedString) {
            detectTapGestures(
                onTap = { offset ->
                    val layout = layoutResult ?: return@detectTapGestures
                    val pos = layout.getOffsetForPosition(offset)
                    annotatedString.getStringAnnotations(pos, pos)
                        .firstOrNull()
                        ?.let { if (it.item.isNotBlank()) onArtistClick(it.item) }
                }
            )
        },
    )
}
