package com.darkxvenom.airbeats.ui.player

import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
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
import com.darkxvenom.airbeats.constants.SliderStyle
import com.darkxvenom.airbeats.constants.SliderStyleKey
import com.darkxvenom.airbeats.ui.component.PlayerSliderTrack
import me.saket.squiggles.SquigglySlider
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
//  CLOUDGLOW PLAYER (Neumorphic Aesthetic)
// ─────────────────────────────────────────────────────────────────────────────

private val ActiveRed   = Color(0xFFE53935)
private val ActiveBlue  = Color(0xFF007AFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudGlowPlayerScreen(
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
    val sliderStyle by rememberEnumPreference(SliderStyleKey, defaultValue = SliderStyle.SQUIGGLY)

    val enableLiquidGlass by rememberPreference(LiquidGlassKey, defaultValue = false)
    val backdrop = LocalBackdrop.current
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }

    val iconColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF5D6B82)

    Box(modifier = Modifier.fillMaxSize().bottomSheetDraggable(state)
    ) {
        // ── BACKGROUND LAYER (DEFAULT / GRADIENT / BLUR) ─────────────────────
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
                            DefaultBackground(isDark = false, artworkUrl = artworkUrl)
                        }
                    }
                }
                else -> {
                    DefaultBackground(isDark = false, artworkUrl = artworkUrl)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. HEADER ROW ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeumorphicButton(onClick = onCollapse, size = 44.dp, isDark = isDark) {
                    ChevronLeftIcon(tint = iconColor, modifier = Modifier.size(16.dp))
                }
                
                Text(
                    text = "PLAYING NOW",
                    color = if (isDark) Color(0xFFAAAAAA) else Color(0xFF7A8A9E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = SpotifyFontFamily
                )
                
                NeumorphicButton(onClick = onOpenMenu, size = 44.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "Menu",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 2. NEUMORPHIC CIRCULAR ARTWORK ──────────────────────────────
            NeumorphicArtworkFrame(isDark = isDark) {
                if (artworkUrl != null) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1A1A2E), Color(0xFF0F3460))
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── 3. SONG INFO ────────────────────────────────────────────────
            Text(
                text = mediaMetadata?.title ?: "Unknown",
                color = if (isDark) Color.White else Color(0xFF2E3D52),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SpotifyFontFamily
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                color = if (isDark) Color(0xFFAAAAAA) else Color(0xFF7A8A9E),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = SpotifyFontFamily
            )

            Spacer(Modifier.height(36.dp))

            // ── 4. SLIDER PROGRESS & TIMESTAMPS ──────────────────────────────
            val sliderProgress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            
            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = makeTimeString(position),
                    color = if (isDark) Color(0xFF888888) else Color(0xFF7A8A9E),
                    fontSize = 11.sp,
                    fontFamily = SpotifyFontFamily
                )
                Text(
                    text = makeTimeString(duration),
                    color = if (isDark) Color(0xFF888888) else Color(0xFF7A8A9E),
                    fontSize = 11.sp,
                    fontFamily = SpotifyFontFamily
                )
            }
            
            Spacer(Modifier.height(4.dp))
            
            when (sliderStyle) {
                SliderStyle.DEFAULT -> {
                    Slider(
                        value = sliderProgress,
                        onValueChange = { onSeek((it * duration).toLong()) },
                        onValueChangeFinished = onSeekFinished,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF755DFF),
                            activeTrackColor = Color(0xFF755DFF),
                            inactiveTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5EBF5)
                        )
                    )
                }
                SliderStyle.SQUIGGLY -> {
                    SquigglySlider(
                        value = sliderProgress,
                        onValueChange = { onSeek((it * duration).toLong()) },
                        onValueChangeFinished = onSeekFinished,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF755DFF),
                            activeTrackColor = Color(0xFF755DFF),
                            inactiveTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5EBF5)
                        ),
                        squigglesSpec = SquigglySlider.SquigglesSpec(
                            amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                            strokeWidth = 3.dp,
                        )
                    )
                }
                SliderStyle.SLIM -> {
                    Slider(
                        value = sliderProgress,
                        onValueChange = { onSeek((it * duration).toLong()) },
                        onValueChangeFinished = onSeekFinished,
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color(0xFF755DFF),
                                    inactiveTrackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5EBF5)
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── 5. MAIN CONTROLS ROW (Prev, Play, Next) ─────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeumorphicButton(onClick = onPrevious, size = 56.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                PlayPauseButton(onClick = onPlayPause, isPlaying = isPlaying)

                NeumorphicButton(onClick = onNext, size = 56.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── 6. BOTTOM ROW (Shuffle, Lyrics, Like, Repeat, Queue) ───────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                NeumorphicButton(onClick = onShuffle, size = 44.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) ActiveBlue else iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Lyrics
                NeumorphicButton(onClick = onOpenLyrics, size = 44.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics),
                        contentDescription = "Lyrics",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Like
                NeumorphicButton(onClick = { playerConnection.toggleLike() }, size = 44.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = "Like",
                        tint = if (isLiked) ActiveRed else iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Repeat
                val repeatActive = repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF
                val repeatIcon = when (repeatMode) {
                    androidx.media3.common.Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                    androidx.media3.common.Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                    else -> R.drawable.repeat
                }
                NeumorphicButton(onClick = onRepeat, size = 44.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(repeatIcon),
                        contentDescription = "Repeat",
                        tint = if (repeatActive) ActiveBlue else iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Queue
                NeumorphicButton(onClick = onOpenQueue, size = 44.dp, isDark = isDark) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Queue",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
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
private fun DefaultBackground(isDark: Boolean, artworkUrl: String?) {
    if (isDark) {
        Box(modifier = Modifier.fillMaxSize()
                .background(Color.Black)
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(100.dp)
                        .alpha(0.4f)
                )
            }
            Box(modifier = Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()
                .background(Color(0xFFE6EEF8))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Cloud 1: Soft Purple Glow (top right)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFDCD6FF).copy(alpha = 0.7f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.25f),
                        radius = size.width * 0.9f
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.25f),
                    radius = size.width * 0.9f
                )
                
                // Cloud 2: Soft Pink/Lavender Glow (bottom left)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFBE4EC).copy(alpha = 0.6f), Color.Transparent),
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
//  NEUMORPHIC SHADOW BUTTON
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NeumorphicButton(
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
//  NEUMORPHIC ARTWORK FRAME
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NeumorphicArtworkFrame(
    modifier: Modifier = Modifier,
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
            .size(260.dp)
            .drawBehind {
                val r = this.size.minDimension / 2f
                val offsetPx = 6.dp.toPx()
                val blurPx = 10.dp.toPx()

                drawIntoCanvas { canvas ->
                    // Draw dark shadow (bottom right)
                    val darkPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = darkShadow.toArgb()
                        setShadowLayer(blurPx, offsetPx, offsetPx, darkShadow.toArgb())
                    }
                    canvas.nativeCanvas.drawCircle(center.x, center.y, r - 2.dp.toPx(), darkPaint)

                    // Draw light shadow (top left)
                    val lightPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = lightShadow.toArgb()
                        setShadowLayer(blurPx, -offsetPx, -offsetPx, lightShadow.toArgb())
                    }
                    canvas.nativeCanvas.drawCircle(center.x, center.y, r - 2.dp.toPx(), lightPaint)
                }
            }
            .then(
                if (enableLiquidGlass && backdrop != null) {
                    Modifier
                        .clip(CircleShape)
                        .drawBackdropCustomShape(
                            backdrop = backdrop,
                            layer = layer,
                            luminanceAnimation = 0.3f,
                            shape = CircleShape
                        )
                } else {
                    Modifier
                        .clip(CircleShape)
                        .background(bgColor)
                        .border(1.dp, borderTint, CircleShape)
                }
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  GLOWING PLAY/PAUSE BUTTON
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlayPauseButton(
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
                ambientColor = Color(0xFF6B57FF),
                spotColor = Color(0xFF6B57FF)
            )
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF5D7BFF), Color(0xFF755DFF))
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







