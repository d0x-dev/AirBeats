package com.darkxvenom.airbeats.ui.player

import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.LiquidGlassKey
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyle
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyleKey
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.ui.component.LocalBackdrop
import com.darkxvenom.airbeats.ui.component.drawBackdropCustomShape
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.utils.rememberPreference
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
//  WHITE PAPER PLAYER (CloudGlow, LiquidGlass & Configurable Backgrounds)
// ─────────────────────────────────────────────────────────────────────────────

private val ActiveRed   = Color(0xFFE53935)
private val ActiveBlue  = Color(0xFF007AFF)

@SuppressLint("UnrememberedMutableState")
@Composable
fun FuturisticPlayer(
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
    onOpenMenu: () -> Unit,
    dynamicColor: Color? = null,        // kept for API compatibility
    gradientColors: List<Color> = emptyList(),
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val artworkUrl = mediaMetadata?.thumbnailUrl?.highQualityThumbnail()
    val isLiked by playerConnection.currentSong
        .collectAsState(initial = null)
        .let { state -> derivedStateOf { state.value?.song?.liked == true } }

    // Detect dark theme using background luminance
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // User preference settings
    val enableLiquidGlass by rememberPreference(LiquidGlassKey, defaultValue = false)
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val backdrop = LocalBackdrop.current
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }

    // Theme-dependent colors
    val pillBgColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
    val pillBorderColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val playCircleBgColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111111)
    val playCircleIconColor = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val iconColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111111)
    val iconMutedColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF777777)
    val iconFaintColor = if (isDark) Color(0xFF333333) else Color(0xFFE5E5EA)
    val progressTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val progressActiveColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF111111)

    Box(modifier = Modifier.fillMaxSize().bottomSheetDraggable(state)
    ) {
        // ── BACKGROUND LAYER (DEFAULT / GRADIENT / BLUR) ─────────────────────
        if (!isDark) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    Box(modifier = Modifier.fillMaxSize()
                            .background(Color(0xFFF5F5F7))
                    ) {
                        if (artworkUrl != null) {
                            AsyncImage(
                                model = artworkUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(100.dp)
                                    .alpha(0.6f)
                            )
                        }
                        Box(modifier = Modifier.fillMaxSize()
                                .background(Color.White.copy(alpha = 0.55f))
                        )
                    }
                }
                PlayerBackgroundStyle.GRADIENT -> {
                    Box(modifier = Modifier.fillMaxSize()
                            .background(Color(0xFFF5F5F7))
                    ) {
                        if (gradientColors.size >= 2) {
                            val animatedGradientColors = gradientColors.map { color ->
                                animateColorAsState(color, label = "gradient_color").value
                            }
                            Box(modifier = Modifier.fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = animatedGradientColors
                                        )
                                    )
                                    .alpha(0.8f)
                            )
                            Box(modifier = Modifier.fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        } else {
                            DefaultBackground(isDark = false)
                        }
                    }
                }
                else -> {
                    DefaultBackground(isDark = false)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. HEADER ROW ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    ChevronLeftIcon(tint = iconColor)
                }
                IconButton(onClick = onOpenMenu) {
                    MenuSortIcon(tint = iconColor)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 2. U-SHAPED ARTWORK & PROGRESS ARC CONTAINER ─────────────────
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(380.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Concentric Progress Arc
                ConcentricArcProgressBar(
                    position = position,
                    duration = duration,
                    onSeek = onSeek,
                    onSeekFinished = onSeekFinished,
                    trackColor = progressTrackColor,
                    activeColor = progressActiveColor,
                    dotColor = Color.Black, // scrubber border is black for visual clarity
                    modifier = Modifier.fillMaxSize()
                )

                // Floating U-shaped Artwork Card
                UShapedArtworkCard(
                    artworkUrl = artworkUrl,
                    title = mediaMetadata?.title ?: "Unknown",
                    artist = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                    modifier = Modifier
                        .width(240.dp)
                        .height(360.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── 3. CENTERED TIMESTAMP ────────────────────────────────────────
            Text(
                text = makeTimeString(position),
                color = iconColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = SpotifyFontFamily
            )

            Spacer(Modifier.weight(1f))

            // ── 4. MAIN CONTROLS ROW (Shuffle, Pill, Queue) ──────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(onClick = onShuffle) {
                    Icon(
                        painter = painterResource(if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) ActiveBlue else iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Prev / Play-Pause / Next Pill
                val pillModifier = Modifier
                    .width(200.dp)
                    .height(56.dp)
                    .then(
                        if (enableLiquidGlass && backdrop != null) {
                            Modifier
                                .clip(CircleShape)
                                .drawBackdropCustomShape(
                                    backdrop = backdrop,
                                    layer = layer,
                                    luminanceAnimation = luminanceAnimation.value,
                                    shape = CircleShape
                                )
                        } else {
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(pillBgColor)
                                .border(1.dp, pillBorderColor, RoundedCornerShape(50))
                        }
                    )

                Row(
                    modifier = pillModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onPrevious, enabled = canSkipPrevious) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = if (canSkipPrevious) iconColor else iconFaintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play/Pause button (circle)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(playCircleBgColor)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = playCircleIconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onNext, enabled = canSkipNext) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = if (canSkipNext) iconColor else iconFaintColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Queue Button
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Queue",
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── 5. BOTTOM ACTIONS ROW (Lyrics, Like, Repeat) ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lyrics
                IconButton(onClick = onOpenLyrics) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics),
                        contentDescription = "Lyrics",
                        tint = iconMutedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Heart (Like)
                IconButton(onClick = { playerConnection.toggleLike() }) {
                    Icon(
                        painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = "Like",
                        tint = if (isLiked) ActiveRed else iconMutedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Repeat
                val repeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                val repeatIcon = when (repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                    else -> R.drawable.repeat
                }
                IconButton(onClick = onRepeat) {
                    Icon(
                        painter = painterResource(repeatIcon),
                        contentDescription = "Repeat",
                        tint = if (repeatActive) ActiveBlue else iconMutedColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  DEFAULT BACKGROUND COMPOSABLE (CloudGlow / Pure Black)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DefaultBackground(isDark: Boolean) {
    if (isDark) {
        Box(modifier = Modifier.fillMaxSize()
                .background(Color.Black)
        )
    } else {
        // CloudGlow purple effect background for light theme
        Box(modifier = Modifier.fillMaxSize()
                .background(Color(0xFFF5F5F7))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Cloud 1: Soft Purple Glow (top right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE8D5FF).copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.25f),
                        radius = size.width * 0.9f
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.25f),
                    radius = size.width * 0.9f
                )
                
                // Cloud 2: Soft Pink/Lavender Glow (bottom left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFCE4EC).copy(alpha = 0.6f), Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.75f),
                        radius = size.width * 0.9f
                    ),
                    center = Offset(size.width * 0.2f, size.height * 0.75f),
                    radius = size.width * 0.9f
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  U-SHAPED ARTWORK CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun UShapedArtworkCard(
    artworkUrl: String?,
    title: String,
    artist: String,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(
        topStart = 28.dp,
        topEnd = 28.dp,
        bottomStart = 120.dp,
        bottomEnd = 120.dp
    )
    Box(
        modifier = modifier
            .clip(cardShape)
            .background(Color(0xFF111111))
    ) {
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1A1A2E), Color(0xFF0F3460))
                        )
                    )
            )
        }

        // Bottom gradient overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
        )

        // Title + Artist text centered horizontally in the bottom semicircle area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SpotifyFontFamily
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SpotifyFontFamily
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CONCENTRIC PROGRESS ARC
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ConcentricArcProgressBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color,
    activeColor: Color,
    dotColor: Color,
) {
    val progress = remember(position, duration) {
        if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val startAngle = 160f
    val sweepAngle = -140f

    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    val activeProgress = if (isDragging) dragProgress else progress

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(onPress = { offset ->
                    isDragging = true
                    val center = Offset(size.width / 2f, 240.dp.toPx())
                    val frac = arcFraction(offset, center, startAngle, sweepAngle)
                    dragProgress = frac
                    onSeek((frac * duration).toLong())
                    tryAwaitRelease()
                    isDragging = false
                    onSeekFinished()
                })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val center = Offset(size.width / 2f, 240.dp.toPx())
                        dragProgress = arcFraction(offset, center, startAngle, sweepAngle)
                        onSeek((dragProgress * duration).toLong())
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, 240.dp.toPx())
                        dragProgress = arcFraction(change.position, center, startAngle, sweepAngle)
                        onSeek((dragProgress * duration).toLong())
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekFinished()
                    },
                    onDragCancel = { isDragging = false }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val centerX = w / 2f
            val centerY = 240.dp.toPx()
            val center = Offset(centerX, centerY)

            val radius = 136.dp.toPx()
            val sw = 2.dp.toPx()

            val topLeft = Offset(centerX - radius, centerY - radius)
            val arcSize = Size(radius * 2f, radius * 2f)

            // Inactive track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )

            // Active progress
            val activeSweep = sweepAngle * activeProgress
            if (activeProgress > 0.001f) {
                drawArc(
                    color = activeColor,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw * 2.5f, cap = StrokeCap.Round)
                )
            }

            // Scrubber dot
            val dotDeg = startAngle + activeSweep
            val dotRad = Math.toRadians(dotDeg.toDouble())
            val dotX = center.x + radius * cos(dotRad).toFloat()
            val dotY = center.y + radius * sin(dotRad).toFloat()
            val dot = Offset(dotX, dotY)

            // White circle background/fill
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = dot
            )
            // Black border
            drawCircle(
                color = dotColor,
                radius = 8.dp.toPx(),
                center = dot,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

private fun arcFraction(
    offset: Offset,
    center: Offset,
    startAngle: Float,
    sweepAngle: Float
): Float {
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (deg < 0f) deg += 360f
    val absSweep = abs(sweepAngle)
    var relative = startAngle - deg
    if (relative < 0f) relative += 360f
    return if (relative > absSweep) {
        if (relative - absSweep > (360f - absSweep) / 2f) 0f else 1f
    } else {
        (relative / absSweep).coerceIn(0f, 1f)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CUSTOM CANVAS DRAWN CHEVRON AND MENU ICONS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChevronLeftIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val sw = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.65f, h * 0.25f)
            lineTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.65f, h * 0.75f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = sw,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun MenuSortIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val sw = 2.dp.toPx()
        // Top line (long)
        drawLine(
            color = tint,
            start = Offset(w * 0.25f, h * 0.3f),
            end = Offset(w * 0.75f, h * 0.3f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        // Middle line (medium)
        drawLine(
            color = tint,
            start = Offset(w * 0.35f, h * 0.5f),
            end = Offset(w * 0.75f, h * 0.5f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
        // Bottom line (short)
        drawLine(
            color = tint,
            start = Offset(w * 0.45f, h * 0.7f),
            end = Offset(w * 0.75f, h * 0.7f),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}





