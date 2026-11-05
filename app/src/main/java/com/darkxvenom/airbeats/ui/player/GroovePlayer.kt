package com.darkxvenom.airbeats.ui.player

import androidx.compose.ui.res.stringResource

import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.playback.PlayerConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroovePlayer(
    state: BottomSheetState,
    playerConnection: PlayerConnection,
    mediaMetadata: com.darkxvenom.airbeats.models.MediaMetadata?,
    playbackState: Int,
    duration: Long,
    position: Long,
    sliderPosition: Long?,
    onSliderPositionChange: (Long) -> Unit,
    onSliderPositionChangeFinished: () -> Unit,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCollapse: () -> Unit,
    shuffleModeEnabled: Boolean,
    onShuffleClick: () -> Unit,
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val accentColor = Color(0xFF00C2FF) // Vibrant, bright cyan-blue instead of pale system primary
    val backgroundColor = Color.White // Keeping it white to match screenshot
    val textPrimary = Color(0xFF2E3345)
    val textSecondary = Color(0xFF8A8F9E)

    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()

    val canSkipPrevious = playerConnection.player.hasPreviousMediaItem()
    val canSkipNext = playerConnection.player.hasNextMediaItem()
    
    var flyingCardIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize().bottomSheetDraggable(state)
            .background(backgroundColor)
            .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
    ) {
        Column(modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.collapse),
                        tint = textPrimary
                    )
                }
                Text(
                    text = stringResource(R.string.ui_text_music_world),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = textPrimary
                )
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.ui_a11y_menu),
                        tint = textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Timers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(position),
                    fontSize = 12.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatTime(duration),
                    fontSize = 12.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Halo Artwork
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background shadow glow
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(accentColor.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )

                // Artwork with 3D pulse on change
                var artworkScale by remember { mutableStateOf(1f) }
                LaunchedEffect(mediaMetadata) {
                    artworkScale = 1.1f
                    kotlinx.coroutines.delay(150)
                    artworkScale = 1f
                }
                val animatedArtworkScale by animateFloatAsState(
                    targetValue = artworkScale,
                    animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )

                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = stringResource(R.string.ui_a11y_artwork),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            scaleX = animatedArtworkScale
                            scaleY = animatedArtworkScale
                        }
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onLyricsClick
                        )
                )

                // Interactive Progress Ring (Circular Slider)
                var dragProgress by remember { mutableStateOf<Float?>(null) }
                val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                val activeProgress = dragProgress ?: progress
                val currentAngle = activeProgress * 360f

                Canvas(
                    modifier = Modifier
                        .size(260.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    var angle = (atan2(dy, dx) * (180f / PI)).toFloat() + 90f
                                    if (angle < 0) angle += 360f
                                    dragProgress = angle / 360f
                                },
                                onDrag = { change, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = change.position.x - center.x
                                    val dy = change.position.y - center.y
                                    var angle = (atan2(dy, dx) * (180f / PI)).toFloat() + 90f
                                    if (angle < 0) angle += 360f
                                    val newProgress = angle / 360f
                                    dragProgress = newProgress
                                    onSliderPositionChange((newProgress * duration).toLong())
                                },
                                onDragEnd = {
                                    dragProgress?.let { finalProgress ->
                                        onSliderPositionChange((finalProgress * duration).toLong())
                                        onSliderPositionChangeFinished()
                                    }
                                    dragProgress = null
                                },
                                onDragCancel = { dragProgress = null }
                            )
                        }
                ) {
                    // Draw track
                    drawArc(
                        color = Color(0xFFF0F0F0),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    // Draw active progress arc
                    drawArc(
                        color = accentColor,
                        startAngle = -90f,
                        sweepAngle = currentAngle,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Draw draggable thumb
                    val angleRad = (currentAngle - 90f) * (PI / 180f)
                    val radius = size.width / 2f
                    val thumbX = center.x + radius * cos(angleRad).toFloat()
                    val thumbY = center.y + radius * sin(angleRad).toFloat()
                    
                    // Thumb shadow
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.2f),
                        radius = 12.dp.toPx(),
                        center = Offset(thumbX, thumbY + 4.dp.toPx())
                    )
                    // Thumb ball
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                    // Inner accent dot
                    drawCircle(
                        color = accentColor,
                        radius = 4.dp.toPx(),
                        center = Offset(thumbX, thumbY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Title and Artist
            Text(
                text = mediaMetadata?.title ?: "Unknown Title",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                fontSize = 14.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.weight(0.75f)) // Less weight above to pull controls up

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffleClick) {
                    Icon(
                        painter = painterResource(if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                        tint = if (shuffleModeEnabled) accentColor else Color(0xFF8A8F9E),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp)) // Bring side control closer

                // Central Pill + Play Button
                Box(contentAlignment = Alignment.Center) {
                    // Pill shape background for Previous and Next
                    Row(
                        modifier = Modifier
                            .width(230.dp) // Increased width
                            .height(56.dp)
                            .border(1.dp, Color(0xFFF0F0F0), CircleShape)
                            .background(Color.White, CircleShape),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(enabled = canSkipPrevious, onClick = onPrevious),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = stringResource(R.string.ui_a11y_previous),
                                tint = if (canSkipPrevious) Color(0xFF8A8F9E) else Color(0xFFE0E0E0),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(80.dp)) // Increased space for central play button

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(enabled = canSkipNext, onClick = onNext),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = stringResource(R.string.ui_a11y_next),
                                tint = if (canSkipNext) Color(0xFF8A8F9E) else Color(0xFFE0E0E0),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Play/Pause Button overlapping the pill
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        // Glow effect for button
                        Box(modifier = Modifier.fillMaxSize()
                                .blur(16.dp)
                                .background(accentColor, CircleShape)
                        )
                        
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.ui_a11y_play_pause),
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp)) // Bring side control closer

                IconButton(onClick = onRepeatClick) {
                    val repeatIcon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                        Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                        else -> R.drawable.repeat
                    }
                    val repeatTint = if (repeatMode != Player.REPEAT_MODE_OFF) accentColor else Color(0xFF8A8F9E)
                    Icon(
                        painter = painterResource(repeatIcon),
                        contentDescription = stringResource(R.string.ui_a11y_repeat),
                        tint = repeatTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1.25f)) // More weight below to push controls up
            Spacer(modifier = Modifier.height(80.dp)) // Padding to account for the queue cards at the bottom
        }

        // Floating Vertical Stack (Custom Animated Deck)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = 75.dp) // Adjusted for taller cards
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                    )
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            // Render up to 5 cards to allow smooth transition of leaving and entering cards
            val visibleIndices = (currentWindowIndex - 1 .. currentWindowIndex + 4).filter { it in queueWindows.indices }
            
            // Render from back to front (highest index first)
            val sortedIndices = visibleIndices.sortedByDescending { it }
            
            sortedIndices.forEach { index ->
                key(index) {
                    val window = queueWindows[index]
                    
                    val isFlying = flyingCardIndex == index
                    val flyProgress by animateFloatAsState(
                        targetValue = if (isFlying) 1f else 0f,
                        animationSpec = tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                        finishedListener = {
                            if (it == 1f && isFlying) {
                                flyingCardIndex = null
                                playerConnection.player.seekToDefaultPosition(index)
                                playerConnection.player.play()
                            }
                        }
                    )
                    
                    // Smoothly animate the card's offset relative to the current playing index
                    // By subtracting 1, the currently playing song is pushed to offset -1 (hidden),
                    // and the NEXT song sits at offset 0 (front of stack).
                    val targetOffset = (index - currentWindowIndex - 1).toFloat()
                    val offsetInfo by animateFloatAsState(
                        targetValue = targetOffset,
                        animationSpec = tween(durationMillis = 500)
                    )
                    
                    val absoluteOffset = kotlin.math.abs(offsetInfo)
                    
                    // Math for the 3-card stack
                    val stackScale = if (offsetInfo > 0) {
                        1f - (offsetInfo * 0.12f).coerceIn(0f, 1f) // Increased shrink rate so back cards are smaller
                    } else {
                        1f + (absoluteOffset * 0.1f)
                    }

                    // Translation Y: future items move UP (stacking vertically). Past items move DOWN and fall away.
                    val stackTransY = if (offsetInfo > 0) {
                        -offsetInfo * 42f * density // Decreased push up so back cards stick out less
                    } else {
                        absoluteOffset * 80f * density // drop down fast
                    }

                    // Alpha: Front card fully lit, behind cards lower lit ("low lighten")
                    val stackAlpha = if (offsetInfo > 2.5f) {
                        0f // Hide cards beyond the 3rd
                    } else if (offsetInfo > 0) {
                        1f - (offsetInfo * 0.35f).coerceIn(0f, 1f)
                    } else {
                        1f - (absoluteOffset * 1.5f).coerceIn(0f, 1f)
                    }

                    // Interpolate towards flying state!
                    val finalScale = stackScale + (1f - stackScale) * flyProgress
                    val finalTransY = stackTransY + (-480f * density - stackTransY) * flyProgress
                    val finalRotationX = 360f * flyProgress
                    val finalAlpha = stackAlpha + (1f - stackAlpha) * flyProgress
                    val finalZIndex = -offsetInfo + (100f - (-offsetInfo)) * flyProgress

                    // Morph dimensions
                    val cardWidth = 300.dp + (220.dp - 300.dp) * flyProgress
                    val cardHeight = 140.dp + (220.dp - 140.dp) * flyProgress
                    val cornerRadius = 22.dp + (110.dp - 22.dp) * flyProgress

                    Box(
                        modifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                            .zIndex(finalZIndex)
                            .graphicsLayer {
                                scaleX = finalScale
                                scaleY = finalScale
                                rotationX = finalRotationX
                                translationY = finalTransY
                                alpha = finalAlpha
                                cameraDistance = 12f * density
                                shape = RoundedCornerShape(cornerRadius)
                                clip = true
                            }
                            .clickable {
                                if (flyingCardIndex == null) {
                                    flyingCardIndex = index
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Soft blur behind carousel element
                        AsyncImage(
                            model = window.mediaItem.mediaMetadata.artworkUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(16.dp)
                        )
                        
                        // Actual thumbnail
                        AsyncImage(
                            model = window.mediaItem.mediaMetadata.artworkUri,
                            contentDescription = stringResource(R.string.ui_a11y_queue_thumbnail),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                        )
                        
                        // Dimming overlay for cards behind to enhance the "low lighten" effect
                        val overlayAlpha = if (offsetInfo > 0) (offsetInfo * 0.25f).coerceIn(0f, 0.6f) else 0f
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = overlayAlpha)))
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms < 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}






