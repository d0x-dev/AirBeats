package com.darkxvenom.airbeats.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.DarkModeKey
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.ui.screens.settings.DarkMode
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import com.darkxvenom.airbeats.ui.component.BottomSheetState

@Composable
fun PaperPlayer(
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
    volume: Float,
    onVolumeChange: (Float) -> Unit,
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
) {
    val systemDark = isSystemInDarkTheme()
    val darkMode by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val dark = if (darkMode == DarkMode.AUTO) systemDark else darkMode == DarkMode.ON
    
    // Neumorphic colors for full screen
    val paper = if (dark) Color(0xFF20201D) else Color(0xFFFFFFFF)
    val paperHigh = if (dark) Color(0xFF2A2A26) else Color(0xFFFFFFFF)
    val paperLow = if (dark) Color(0xFF151513) else Color(0xFFD1D1D8)
    val ink = if (dark) Color(0xFFF3F2EC) else Color(0xFF111111)
    val groove = if (dark) Color(0xFF4F4D47) else Color(0xFFB0B0B0)

    // Generate a subtle noise texture for paper effect
    val noiseBitmap = remember(dark) {
        val width = 200
        val height = 200
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val random = java.util.Random()
        val alpha = if (dark) 4 else 8
        for (i in pixels.indices) {
            val intensity = random.nextInt(256)
            pixels[i] = android.graphics.Color.argb(alpha, intensity, intensity, intensity)
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.asImageBitmap()
    }

    val safeDuration = duration.takeIf { it > 0 } ?: 0L
    val shownPosition = position.coerceIn(0L, safeDuration.coerceAtLeast(0L))
    val progress = if (safeDuration > 0) shownPosition.toFloat() / safeDuration else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paper)
            .bottomSheetDraggable(state)
            .drawWithContent {
                drawContent()
                for (x in 0 until size.width.toInt() step noiseBitmap.width) {
                    for (y in 0 until size.height.toInt() step noiseBitmap.height) {
                        drawImage(noiseBitmap, topLeft = Offset(x.toFloat(), y.toFloat()))
                    }
                }
            }
            .padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onCollapse),
                ) {
                    Icon(painterResource(R.drawable.arrow_back), null, tint = ink, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Back", color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onOpenMenu) {
                    Icon(painterResource(R.drawable.menu), null, tint = ink, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.height(48.dp))

            // Text
            Text(
                text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty().ifBlank { "AirBeats" },
                color = ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = mediaMetadata?.title.orEmpty().ifBlank { "Nothing playing" },
                color = ink,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 36.sp,
            )

            Spacer(Modifier.weight(1f))

            // Center Disc & Arc
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center,
            ) {
                var dragAngle by remember { mutableStateOf<Float?>(null) }
                val currentVolume = dragAngle?.let { angle ->
                    val normalized = (90f - angle) / 180f
                    normalized.coerceIn(0f, 1f)
                } ?: volume

                // Arc (Volume Control)
                Canvas(modifier = Modifier
                    .size(280.dp)
                    .pointerInput(Unit) {
                        coroutineScope {
                            launch {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val angle = Math.toDegrees(kotlin.math.atan2(
                                        (offset.y - center.y).toDouble(),
                                        (offset.x - center.x).toDouble()
                                    )).toFloat()
                                    if (angle in -90f..90f) {
                                        val normalized = (90f - angle) / 180f
                                        onVolumeChange(normalized.coerceIn(0f, 1f))
                                    }
                                }
                            }
                            launch {
                                detectDragGestures(
                                    onDragEnd = { dragAngle = null },
                                    onDragCancel = { dragAngle = null },
                                ) { change, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val angle = Math.toDegrees(kotlin.math.atan2(
                                        (change.position.y - center.y).toDouble(),
                                        (change.position.x - center.x).toDouble()
                                    )).toFloat()
                                    if (angle in -90f..90f) {
                                        dragAngle = angle
                                        val normalized = (90f - angle) / 180f
                                        onVolumeChange(normalized.coerceIn(0f, 1f))
                                    }
                                }
                            }
                        }
                    }
                ) {
                    val stroke = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
                    
                    // Arc traces right half of the circle from bottom (90) to top (-90)
                    drawArc(
                        color = groove,
                        startAngle = 90f,
                        sweepAngle = -180f,
                        useCenter = false,
                        topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                        size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
                        style = stroke,
                    )
                    drawArc(
                        color = ink.copy(alpha = 0.72f),
                        startAngle = 90f,
                        sweepAngle = -180f * currentVolume,
                        useCenter = false,
                        topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                        size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
                        style = stroke,
                    )
                    
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 16.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    if (dark) textPaint.color = android.graphics.Color.WHITE
                    
                    drawContext.canvas.nativeCanvas.drawText("+", center.x, 10.dp.toPx(), textPaint)
                    drawContext.canvas.nativeCanvas.drawText("-", center.x, size.height - 2.dp.toPx(), textPaint)

                    // Knob
                    val knobAngle = Math.toRadians((90f - 180f * currentVolume).toDouble())
                    val radius = size.width / 2f - 12.dp.toPx()
                    val knob = Offset(
                        x = center.x + kotlin.math.cos(knobAngle).toFloat() * radius,
                        y = center.y + kotlin.math.sin(knobAngle).toFloat() * radius,
                    )
                    drawCircle(Color.Black.copy(alpha = 0.2f), 8.dp.toPx(), knob + Offset(2.dp.toPx(), 4.dp.toPx()))
                    drawCircle(if (dark) Color(0xFFD8D5C9) else Color(0xFF4D4B45), 7.dp.toPx(), knob)
                }

                // Layer 1: Raised outer circle
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .shadow(24.dp, CircleShape, ambientColor = paperLow.copy(alpha = 0.7f), spotColor = paperLow.copy(alpha = 0.9f))
                        .clip(CircleShape)
                        .background(paper),
                    contentAlignment = Alignment.Center,
                ) {
                    // Layer 2: Middle circle
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .shadow(2.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.2f), spotColor = Color.Black.copy(alpha = 0.3f))
                            .clip(CircleShape)
                            .background(if (dark) Color(0xFF2C2B27) else Color(0xFFE5E5E8)),
                        contentAlignment = Alignment.Center,
                    ) {
                        // Layer 3: Black core
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = mediaMetadata?.thumbnailUrl?.highQualityThumbnail(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Time and Linear Progress
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(makeTimeString(shownPosition), color = ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(makeTimeString(safeDuration), color = ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            var dragProgress by remember { mutableStateOf<Float?>(null) }
            val currentProgress = dragProgress ?: progress

            Canvas(modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    coroutineScope {
                        launch {
                            detectTapGestures { offset ->
                                val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek((newProgress * safeDuration).toLong())
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
                                        onSeek((it * safeDuration).toLong())
                                        onSeekFinished()
                                    }
                                    dragProgress = null
                                },
                                onDragCancel = { dragProgress = null }
                            ) { change, _ ->
                                dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeek((dragProgress!! * safeDuration).toLong())
                            }
                        }
                    }
                }
            ) {
                val cy = size.height / 2f
                
                // Top shadow line (darker) to create inset
                drawLine(
                    color = paperLow.copy(alpha = if (dark) 0.8f else 0.4f),
                    start = Offset(0f, cy - 0.5f.dp.toPx()),
                    end = Offset(size.width, cy - 0.5f.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Bottom highlight line (lighter) to finish inset
                drawLine(
                    color = paperHigh,
                    start = Offset(0f, cy + 0.5f.dp.toPx()),
                    end = Offset(size.width, cy + 0.5f.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Active track fill (optional thin line inside groove)
                if (currentProgress > 0) {
                    drawLine(
                        color = ink.copy(alpha = 0.5f),
                        start = Offset(0f, cy),
                        end = Offset(size.width * currentProgress, cy),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                
                // Thumb
                val thumbX = size.width * currentProgress
                // Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, cy + 4.dp.toPx())
                )
                // Thumb circle
                drawCircle(
                    color = if (dark) Color(0xFFE5E5E5) else Color(0xFF383838),
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, cy)
                )
            }

            Spacer(Modifier.height(24.dp))
            
            // Small extra controls (Shuffle, Lyrics, Queue, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeumorphicSmallButton(R.drawable.shuffle, paper, paperLow, ink, shuffleModeEnabled, onShuffle)
                NeumorphicSmallButton(R.drawable.lyrics, paper, paperLow, ink, false, onOpenLyrics)
                NeumorphicSmallButton(R.drawable.queue_music, paper, paperLow, ink, false, onOpenQueue)
                NeumorphicSmallButton(
                    when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                        Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                        else -> R.drawable.repeat
                    },
                    paper, paperLow, ink, repeatMode != Player.REPEAT_MODE_OFF, onRepeat
                )
            }

            Spacer(Modifier.height(24.dp))

            // Playback controls (Previous, Play/Pause, Next)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NeumorphicButton(R.drawable.skip_previous, paper, paperLow, ink, false, onPrevious, canSkipPrevious)
                NeumorphicPlayButton(isPlaying, isLoading, paper, paperLow, onPlayPause)
                NeumorphicButton(R.drawable.skip_next, paper, paperLow, ink, false, onNext, canSkipNext)
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NeumorphicButton(
    icon: Int,
    paper: Color,
    paperLow: Color,
    ink: Color,
    active: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .shadow(12.dp, CircleShape, ambientColor = paperLow.copy(alpha = 0.5f), spotColor = paperLow.copy(alpha = 0.8f))
            .clip(CircleShape)
            .background(if (active) ink else paper)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            null,
            tint = if (active) paper else ink.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun NeumorphicPlayButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    paper: Color,
    paperLow: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .shadow(16.dp, CircleShape, ambientColor = paperLow.copy(alpha = 0.6f), spotColor = paperLow.copy(alpha = 0.9f))
            .clip(CircleShape)
            .background(Color.Black)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(if (isPlaying && !isLoading) R.drawable.pause else R.drawable.play),
            null,
            tint = Color.White,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun NeumorphicSmallButton(
    icon: Int,
    paper: Color,
    paperLow: Color,
    ink: Color,
    active: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .shadow(6.dp, CircleShape, ambientColor = paperLow.copy(alpha = 0.4f), spotColor = paperLow.copy(alpha = 0.7f))
            .clip(CircleShape)
            .background(if (active) ink else paper)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            null,
            tint = if (active) paper else ink.copy(alpha = if (enabled) 0.8f else 0.35f),
            modifier = Modifier.size(18.dp),
        )
    }
}
