package com.darkxvenom.airbeats.ui.player

import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import android.annotation.SuppressLint
import androidx.media3.common.Player
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import android.os.Build
import com.darkxvenom.airbeats.ui.menu.InAppEqualizerSheet
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.db.entities.LyricsEntity
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.ui.component.LocalBackdrop
import com.darkxvenom.airbeats.ui.component.drawBackdropCustomShape
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.utils.rememberPreference
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
//  FROST PLAYER SCREEN (Premium Neumorphic with Curved Upward Arc Slider)
// ─────────────────────────────────────────────────────────────────────────────

private val AccentBlue = Color(0xFF2F69FF)
private val HeartOrange = Color(0xFFFF7A00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostPlayer(
    state: com.darkxvenom.airbeats.ui.component.BottomSheetState,
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
    FrostPlayerScreen(
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

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostPlayerScreen(
    state: com.darkxvenom.airbeats.ui.component.BottomSheetState,
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
    dynamicColor: Color? = null,
    gradientColors: List<Color> = emptyList(),
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val artworkUrl = mediaMetadata?.thumbnailUrl?.highQualityThumbnail()
    val isLiked by playerConnection.currentSong
        .collectAsState(initial = null)
        .let { state -> derivedStateOf { state.value?.song?.liked == true } }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val enableLiquidGlass by rememberPreference(LiquidGlassKey, defaultValue = false)

    val outputDeviceName = rememberPlaybackOutputName()

    val iconColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF5D6B82)

    Box(modifier = Modifier.fillMaxSize().bottomSheetDraggable(state)
    ) {
        // ── BACKGROUND LAYER (DEFAULT / GRADIENT / BLUR) ──
        if (!isDark) {
            when (playerBackground) {
                PlayerBackgroundStyle.BLUR -> {
                    Box(modifier = Modifier.fillMaxSize()
                            .background(Color(0xFFE6EEF8))
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
                            .background(Color(0xFFE6EEF8))
                    ) {
                        if (gradientColors.size >= 2) {
                            val animatedGradientColors = gradientColors.map { color ->
                                animateColorAsState(color, label = "gradient_color").value
                            }
                            Box(modifier = Modifier.fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(colors = animatedGradientColors)
                                    )
                                    .alpha(0.8f)
                            )
                            Box(modifier = Modifier.fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize()
                                    .background(Color(0xFFE6EEF8))
                            )
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize()
                            .background(Color(0xFFE6EEF8))
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. HEADER ROW ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FrostNeumorphicButton(onClick = onCollapse, size = 44.dp, isDark = isDark) {
                    FrostChevronLeftIcon(tint = iconColor, modifier = Modifier.size(16.dp))
                }
                
                Text(
                    text = "Now Playing",
                    color = if (isDark) Color(0xFFDDDDDD) else Color(0xFF2E3D52),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpotifyFontFamily
                )
                
                FrostNeumorphicButton(
                    onClick = { playerConnection.toggleLike() },
                    size = 44.dp,
                    isDark = isDark
                ) {
                    Icon(
                        painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = "Like",
                        tint = if (isLiked) HeartOrange else iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 2. ARTWORK & VERTICAL COLUMN ROW ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular/Squircle card containing the album artwork with a soft frame
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .shadow(12.dp, RoundedCornerShape(24.dp), clip = false)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isDark) Color(0xFF1E1E22) else Color.White)
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
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Vertical column for options menu, repeat, queue music, lyrics
                Column(
                    modifier = Modifier.height(240.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = onOpenMenu) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = "Options Menu",
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    val repeatActive = repeatMode != Player.REPEAT_MODE_OFF
                    val repeatIcon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                        Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                        else -> R.drawable.repeat
                    }
                    IconButton(onClick = onRepeat) {
                        Icon(
                            painter = painterResource(repeatIcon),
                            contentDescription = "Repeat",
                            tint = if (repeatActive) AccentBlue else iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onOpenQueue) {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = "Queue Music",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onOpenLyrics) {
                        Icon(
                            painter = painterResource(R.drawable.lyrics),
                            contentDescription = "Lyrics Opener",
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 3. SONG INFO ──
            Text(
                text = mediaMetadata?.artists?.joinToString { it.name }?.uppercase() ?: "UNKNOWN ARTIST",
                color = if (isDark) Color(0xFFAAAAAA) else Color(0xFF7A8A9E),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SpotifyFontFamily
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = mediaMetadata?.title ?: "Unknown Title",
                color = if (isDark) Color.White else Color(0xFF2E3D52),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SpotifyFontFamily
            )

            Spacer(Modifier.height(48.dp))

            // ── 4. CURVED UPWARD PROGRESS SLIDER ──
            FrostCurvedSlider(
                position = position,
                duration = duration,
                onSeek = onSeek,
                onSeekFinished = onSeekFinished,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                isDark = isDark
            )

            Spacer(Modifier.height(8.dp))

            // ── 5. TIMESTAMPS & OUTPUT DEVICE ROW ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = makeTimeString(position),
                    color = if (isDark) Color(0xFF888888) else Color(0xFF7A8A9E),
                    fontSize = 11.sp,
                    fontFamily = SpotifyFontFamily
                )

                val isBluetooth = !outputDeviceName.equals("Speaker", ignoreCase = true)
                val deviceIcon = if (isBluetooth) R.drawable.ic_bluetooth else R.drawable.volume_up
                val deviceLabel = "PLAYING IN ${outputDeviceName.uppercase()}"

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(deviceIcon),
                        contentDescription = "Playback Device",
                        tint = if (isDark) Color(0xFF888888) else Color(0xFF7A8A9E),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = deviceLabel,
                        color = if (isDark) Color(0xFF888888) else Color(0xFF7A8A9E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontFamily = SpotifyFontFamily
                    )
                }

                Text(
                    text = makeTimeString(duration),
                    color = if (isDark) Color(0xFF888888) else Color(0xFF7A8A9E),
                    fontSize = 11.sp,
                    fontFamily = SpotifyFontFamily
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── 6. MAIN PLAYBACK CONTROLS (Prev, Play, Next) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                FrostNeumorphicButton(onClick = onPrevious, size = 56.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Play / Pause central button
                FrostPlayPauseButton(onClick = onPlayPause, isPlaying = isPlaying)

                Spacer(modifier = Modifier.width(28.dp))

                // Next button
                FrostNeumorphicButton(onClick = onNext, size = 56.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── 7. FOOTER NEXT SONG ROW ──
            val currentMediaItemIndex by playerConnection.currentMediaItemIndex.collectAsState()
            val nextSongTitle = remember(currentMediaItemIndex) {
                val nextIndex = currentMediaItemIndex + 1
                if (nextIndex in 0 until playerConnection.player.mediaItemCount) {
                    playerConnection.player.getMediaItemAt(nextIndex).mediaMetadata.title?.toString() ?: "Unknown Track"
                } else {
                    "None"
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NEXT",
                    color = if (isDark) Color(0xFF666666) else Color(0xFF9EABB8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = SpotifyFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = nextSongTitle,
                    color = if (isDark) Color(0xFFBBBBBB) else Color(0xFF5D6B82),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = SpotifyFontFamily
                )
            }
    }
}
}

// ─────────────────────────────────────────────────────────────────────────────
//  GEOMETRIC UPWARD-BENDING PROGRESS SLIDER
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FrostCurvedSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    
    Canvas(
        modifier = modifier
            .pointerInput(duration) {
                detectTapGestures(
                    onPress = { offset ->
                        val w = size.width
                        val padding = 24.dp.toPx()
                        val dy = 24.dp.toPx() // arch height
                        val dx = w / 2f - padding
                        val r = (dx * dx + dy * dy) / (2f * dy)
                        val cx = w / 2f
                        val cy = 4.dp.toPx() + r
                        
                        val angleRad = atan2(offset.y - cy, offset.x - cx)
                        var deg = Math.toDegrees(angleRad.toDouble()).toFloat()
                        if (deg < 0) deg += 360f
                        
                        val startAngleRad = atan2(dy - r, padding - cx)
                        val endAngleRad = atan2(dy - r, (w - padding) - cx)
                        val startDeg = Math.toDegrees(startAngleRad.toDouble()).toFloat() + 360f
                        val endDeg = Math.toDegrees(endAngleRad.toDouble()).toFloat() + 360f
                        
                        val coercedDeg = deg.coerceIn(startDeg, endDeg)
                        val p = (coercedDeg - startDeg) / (endDeg - startDeg)
                        onSeek((p * duration).toLong())
                        onSeekFinished()
                    }
                )
            }
            .pointerInput(duration) {
                detectDragGestures(
                    onDrag = { change, _ ->
                        val offset = change.position
                        val w = size.width
                        val padding = 24.dp.toPx()
                        val dy = 24.dp.toPx()
                        val dx = w / 2f - padding
                        val r = (dx * dx + dy * dy) / (2f * dy)
                        val cx = w / 2f
                        val cy = 4.dp.toPx() + r
                        
                        val angleRad = atan2(offset.y - cy, offset.x - cx)
                        var deg = Math.toDegrees(angleRad.toDouble()).toFloat()
                        if (deg < 0) deg += 360f
                        
                        val startAngleRad = atan2(dy - r, padding - cx)
                        val endAngleRad = atan2(dy - r, (w - padding) - cx)
                        val startDeg = Math.toDegrees(startAngleRad.toDouble()).toFloat() + 360f
                        val endDeg = Math.toDegrees(endAngleRad.toDouble()).toFloat() + 360f
                        
                        val coercedDeg = deg.coerceIn(startDeg, endDeg)
                        val p = (coercedDeg - startDeg) / (endDeg - startDeg)
                        onSeek((p * duration).toLong())
                    },
                    onDragEnd = {
                        onSeekFinished()
                    }
                )
            }
    ) {
        val w = size.width
        val padding = 24.dp.toPx()
        val dy = 24.dp.toPx() // arch height
        val dx = w / 2f - padding
        
        val r = (dx * dx + dy * dy) / (2f * dy)
        val cx = w / 2f
        val cy = 4.dp.toPx() + r
        
        val startAngleRad = atan2(dy - r, padding - cx)
        val endAngleRad = atan2(dy - r, (w - padding) - cx)
        
        val startDeg = Math.toDegrees(startAngleRad.toDouble()).toFloat() + 360f
        val endDeg = Math.toDegrees(endAngleRad.toDouble()).toFloat() + 360f
        val sweepAngle = endDeg - startDeg
        
        val strokeWidth = 3.5.dp.toPx()
        val rect = Rect(cx - r, cy - r, cx + r, cy + r)
        
        // Track Background (Off-white / dark gray)
        val trackBgColor = if (isDark) Color(0xFF2C2C32) else Color(0xFFD6DFEC)
        drawArc(
            color = trackBgColor,
            startAngle = startDeg,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Active Progress Blue Segment
        val activeSweep = progress * sweepAngle
        drawArc(
            color = AccentBlue,
            startAngle = startDeg,
            sweepAngle = activeSweep,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        
        // Thumb position
        val currentAngleRad = startAngleRad + progress * (endAngleRad - startAngleRad)
        val thumbX = cx + r * cos(currentAngleRad)
        val thumbY = cy + r * sin(currentAngleRad)
        
        // Circle thumb: white outer rim, blue center
        drawCircle(
            color = Color.White,
            radius = 9.dp.toPx(),
            center = Offset(thumbX, thumbY)
        )
        drawCircle(
            color = AccentBlue,
            radius = 5.dp.toPx(),
            center = Offset(thumbX, thumbY)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  FROST NEUMORPHIC SHADOW BUTTON
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FrostNeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CircleShape,
    isDark: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    val enableLiquidGlass by rememberPreference(LiquidGlassKey, defaultValue = false)
    val backdrop = LocalBackdrop.current
    val layer = rememberGraphicsLayer()

    val bgColor = if (isDark) Color(0xFF1E1E22) else Color(0xFFE6EEF8)
    val lightShadow = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.08f) else Color.White
    val darkShadow = if (isDark) Color(0xFF000000).copy(alpha = 0.5f) else Color(0xFFB8C6DB).copy(alpha = 0.75f)
    val borderTint = if (isDark) Color(0xFF2C2C30) else Color.White.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val r = this.size.minDimension / 2f
                val offsetPx = 3.dp.toPx()
                val blurPx = 4.dp.toPx()

                drawIntoCanvas { canvas ->
                    // Draw dark shadow (bottom right)
                    val darkPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = darkShadow.toArgb()
                        setShadowLayer(blurPx, offsetPx, offsetPx, darkShadow.toArgb())
                    }
                    canvas.nativeCanvas.drawCircle(center.x, center.y, r - 1.dp.toPx(), darkPaint)

                    // Draw light shadow (top left)
                    val lightPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = lightShadow.toArgb()
                        setShadowLayer(blurPx, -offsetPx, -offsetPx, lightShadow.toArgb())
                    }
                    canvas.nativeCanvas.drawCircle(center.x, center.y, r - 1.dp.toPx(), lightPaint)
                }
            }
            .then(
                if (enableLiquidGlass && backdrop != null) {
                    Modifier
                        .clip(shape)
                        .drawBackdropCustomShape(
                            backdrop = backdrop,
                            layer = layer,
                            luminanceAnimation = 0.3f,
                            shape = shape
                        )
                } else {
                    Modifier
                        .clip(shape)
                        .background(bgColor)
                        .border(1.dp, borderTint, shape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  GLOWING PLAY/PAUSE BUTTON (FROST ACCENT)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FrostPlayPauseButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "play_scale"
    )

    Box(
        modifier = modifier
            .size(68.dp)
            .scale(scale)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = AccentBlue,
                spotColor = AccentBlue
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF5D7BFF), AccentBlue)
                )
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CUSTOM CANVAS DRAWN CHEVRON
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FrostChevronLeftIcon(tint: Color, modifier: Modifier = Modifier) {
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








