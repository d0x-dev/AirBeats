package com.darkxvenom.airbeats.ui.player

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalPlayer(
    mediaMetadata: MediaMetadata?,
    nextMediaMetadata: MediaMetadata?,
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
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    onQueueClick: () -> Unit,
    state: BottomSheetState
) {
    val bgColor = Color(0xFFFCFCFE)
    val textPrimary = Color.Black
    val textSecondary = Color.Gray

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .bottomSheetDraggable(state)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.music_recognition_overlay_play),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .width(16.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.5f))
                    )
                }
                
                IconButton(onClick = onMenuClick) {
                    Image(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = stringResource(R.string.ui_a11y_menu),
                        colorFilter = ColorFilter.tint(textPrimary),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Artwork Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(start = 24.dp)
            ) {
                // Next Song Thumbnail Peek
                if (nextMediaMetadata != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 60.dp)
                            .size(240.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        AsyncImage(
                            model = nextMediaMetadata.thumbnailUrl?.highQualityThumbnail(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Vinyl Circle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 240.dp)
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E))
                )

                // Current Song Thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(300.dp)
                        .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.2f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.LightGray)
                ) {
                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl?.highQualityThumbnail(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Metadata Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title ?: "Unknown",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                        fontSize = 16.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = stringResource(R.string.action_like),
                        colorFilter = ColorFilter.tint(if (isLiked) Color(0xFFFF4B4B) else Color.LightGray),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Slider Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Static snake-wave slider drawn with Canvas
                StaticWaveSlider(
                    position = position,
                    duration = duration,
                    onSeek = onSeek,
                    onSeekFinished = onSeekFinished,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(position),
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                    Text(
                        text = makeTimeString(duration),
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onRepeatClick) {
                    Image(
                        painter = painterResource(
                            when (repeatMode) {
                                androidx.media3.common.Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                                androidx.media3.common.Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = stringResource(R.string.ui_a11y_repeat),
                        colorFilter = ColorFilter.tint(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) textPrimary else Color.Gray),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onPrevious) {
                    Image(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = stringResource(R.string.ui_a11y_previous),
                        colorFilter = ColorFilter.tint(if (canSkipPrevious) textPrimary else Color.LightGray),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.2f))
                        .clip(CircleShape)
                        .background(textPrimary)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Image(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.ui_a11y_next),
                        colorFilter = ColorFilter.tint(if (canSkipNext) textPrimary else Color.LightGray),
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(onClick = onQueueClick) {
                    Image(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = stringResource(R.string.queue),
                        colorFilter = ColorFilter.tint(textPrimary),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StaticWaveSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color.Black
    val inactiveColor = Color(0xFFCCCCCC)

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val fraction = if (isDragging && dragFraction != null) {
        dragFraction!!
    } else if (duration > 0) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val density = androidx.compose.ui.platform.LocalDensity.current

    Canvas(
        modifier = modifier
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    if (duration > 0) {
                        val f = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((f * duration).toLong())
                        onSeekFinished()
                    }
                }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        dragFraction?.let { f ->
                            if (duration > 0) {
                                onSeek((f * duration).toLong())
                                onSeekFinished()
                            }
                        }
                        dragFraction = null
                    },
                    onDragCancel = {
                        isDragging = false
                        dragFraction = null
                    }
                ) { change, _ ->
                    if (size.width > 0) {
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        if (duration > 0) {
                            onSeek((dragFraction!! * duration).toLong())
                        }
                    }
                }
            }
    ) {
        val w = size.width
        val cy = size.height / 2f

        // Use density-aware values (dp -> px)
        val strokePx    = with(density) { 1.5.dp.toPx() }
        val amplitudePx = with(density) { 4.dp.toPx() }   // small fixed wave height
        val thumbPx     = with(density) { 5.dp.toPx() }
        val wavelength  = w / 1.5f                         // ~1.5 gentle S-curves

        val thumbX = fraction * w

        val activePath   = androidx.compose.ui.graphics.Path()
        val inactivePath = androidx.compose.ui.graphics.Path()

        val steps = 400
        var firstActive   = true
        var firstInactive = true

        for (i in 0..steps) {
            val x = (i.toFloat() / steps) * w
            val y = cy - amplitudePx * sin(2.0 * PI * x / wavelength).toFloat()
            if (x <= thumbX) {
                if (firstActive)   { activePath.moveTo(x, y);   firstActive = false }
                else                 activePath.lineTo(x, y)
            } else {
                if (firstInactive) { inactivePath.moveTo(x, y); firstInactive = false }
                else                 inactivePath.lineTo(x, y)
            }
        }

        val stroke = Stroke(
            width = strokePx,
            cap   = StrokeCap.Round,
            join  = androidx.compose.ui.graphics.StrokeJoin.Round
        )

        drawPath(path = inactivePath, color = inactiveColor, style = stroke)
        drawPath(path = activePath,   color = activeColor,   style = stroke)

        // Thumb: solid black dot sitting on the wave
        val thumbY = cy - amplitudePx * sin(2.0 * PI * thumbX / wavelength).toFloat()
        drawCircle(color = Color.White, radius = thumbPx,         center = androidx.compose.ui.geometry.Offset(thumbX, thumbY))
        drawCircle(color = Color.Black, radius = thumbPx * 0.65f, center = androidx.compose.ui.geometry.Offset(thumbX, thumbY))
    }
}
