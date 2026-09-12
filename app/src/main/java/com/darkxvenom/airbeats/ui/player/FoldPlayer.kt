package com.darkxvenom.airbeats.ui.player

import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.media3.common.Player
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.utils.makeTimeString
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun FoldPlayer(
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
    shuffleModeEnabled: Boolean,
    onShuffleClick: () -> Unit,
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val safeDuration = duration.takeIf { it > 0 } ?: 0L
    val shownPosition = position.coerceIn(0L, safeDuration.coerceAtLeast(0L))
    val progress = if (safeDuration > 0) shownPosition.toFloat() / safeDuration else 0f

    val bg = Color(0xFFF0F0F0)
    val textPrimary = Color(0xFF333333)
    val textSecondary = Color(0xFFA0A0A0)

    Column(modifier = Modifier.fillMaxSize().bottomSheetDraggable(state)
            .background(bg)
            .padding(horizontal = 24.dp)
            .padding(top = 80.dp), // Push things down much more
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RectangleShape,
                    ambientColor = Color.Black.copy(alpha = 0.2f),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(RectangleShape)
                .background(bg)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lyrics),
                    contentDescription = "Lyrics",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp).clickable(onClick = onLyricsClick)
                )
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = "Queue",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp).clickable(onClick = onQueueClick)
                )
                Icon(
                    painter = painterResource(if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = if (shuffleModeEnabled) Color(0xFFE91E63) else textSecondary,
                    modifier = Modifier.size(20.dp).clickable(onClick = onShuffleClick)
                )
                Icon(
                    painter = painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = "Repeat",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color(0xFFE91E63) else textSecondary,
                    modifier = Modifier.size(20.dp).clickable(onClick = onRepeatClick)
                )
            }
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFD0D0D0)))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Music",
                    color = textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onCollapse)
                )
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Menu",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp).clickable(onClick = onMenuClick)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.7f))

        // Album Art (CD Cutout Style)
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(24.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Center cutout
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(bg)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title and Artist
        Text(
            text = mediaMetadata?.title ?: "Unknown",
            color = textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
            color = textSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Waveform Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = makeTimeString(shownPosition),
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(16.dp))
            
            WaveformSlider(
                progress = progress,
                duration = safeDuration,
                onSeek = onSeek,
                onSeekFinished = onSeekFinished,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = makeTimeString(safeDuration),
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = if (canSkipPrevious) Color(0xFF404040) else Color(0xFFC0C0C0),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(enabled = canSkipPrevious, onClick = onPrevious)
            )

            Spacer(modifier = Modifier.width(40.dp))

            // Play/Pause
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFFE91E63),
                    modifier = Modifier.size(40.dp).padding(4.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color(0xFF2E3345),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onPlayPause)
                )
            }

            Spacer(modifier = Modifier.width(40.dp))

            // Next
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = if (canSkipNext) Color(0xFF404040) else Color(0xFFC0C0C0),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(enabled = canSkipNext, onClick = onNext)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun WaveformSlider(
    progress: Float,
    duration: Long,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragProgress by remember { mutableStateOf<Float?>(null) }
    val currentProgress = dragProgress ?: progress
    
    // Generate static random waveform bars based on a fixed seed so it doesn't flicker
    val bars = remember {
        val random = Random(42)
        FloatArray(60) { random.nextFloat() * 0.8f + 0.2f }
    }

    val gradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF33C7D4), // Cyan
            Color(0xFFEA4857), // Red
            Color(0xFFDE3783)  // Magenta
        )
    )
    val inactiveColor = Color(0xFFD6D6D6)

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            coroutineScope {
                launch {
                    detectTapGestures { offset ->
                        val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((newProgress * duration).toLong())
                        onSeekFinished()
                    }
                }
                launch {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragProgress?.let {
                                onSeek((it * duration).toLong())
                                onSeekFinished()
                            }
                            dragProgress = null
                        },
                        onDragCancel = { dragProgress = null }
                    ) { change, _ ->
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((dragProgress!! * duration).toLong())
                    }
                }
            }
        }
    ) {
        val barCount = bars.size
        val barWidth = size.width / (barCount * 1.5f)
        val spacing = (size.width - (barCount * barWidth)) / (barCount - 1)
        
        for (i in 0 until barCount) {
            val x = i * (barWidth + spacing)
            val barProgress = x / size.width
            val barHeight = size.height * bars[i]
            val y = (size.height - barHeight) / 2f
            
            val color = if (barProgress <= currentProgress) Color.Transparent else inactiveColor
            val brush = if (barProgress <= currentProgress) gradient else null
            
            if (brush != null) {
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2)
                )
            } else {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2)
                )
            }
        }
    }
}






