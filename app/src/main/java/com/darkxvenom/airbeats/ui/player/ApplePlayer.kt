package com.darkxvenom.airbeats.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.constants.SliderStyle
import com.darkxvenom.airbeats.constants.SliderStyleKey
import com.darkxvenom.airbeats.ui.component.PlayerSliderTrack
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import me.saket.squiggles.SquigglySlider

val ApplePlayerBackgroundGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFCF71),
        Color(0xFFF37550),
        Color(0xFFC73646)
    )
)

val AppleCardBackground = Color(0xFFF1F5F9)
val AppleRed = Color(0xFFFA233B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplePlayer(
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
) {
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)

    Box(modifier = Modifier.fillMaxSize().bottomSheetDraggable(state)
    ) {
        // Thumbnail Background with Liquid Glass effect
        val artworkUrl = mediaMetadata?.thumbnailUrl
        if (artworkUrl != null) {
            Box(Modifier.fillMaxSize()) {
                // Base artwork, scaled up slightly to avoid edge bleeding
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.2f)
                )

                // Heavy blur with offscreen compositing
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.2f)
                        .blur(80.dp, edgeTreatment = androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer {
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                        }
                )

                // Subtle dark scrim to ensure text/icons are readable
                Box(modifier = Modifier.fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()
                    .background(ApplePlayerBackgroundGradient)
            )
        }

        Column(modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onCollapse)
                        .padding(4.dp)
                )
                Text(
                    text = "Happy Mood",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                SortMenuIcon(
                    color = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onMenuClick)
                        .padding(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(AppleCardBackground)
            ) {
                Column(modifier = Modifier.fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album Art — tap to open lyrics
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(onClick = onOpenFullscreenLyrics)
                    ) {
                        AsyncImage(
                            model = mediaMetadata?.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gloss reflection overlay
                        Box(modifier = Modifier.fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.2f),
                                            Color.Transparent
                                        ),
                                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                        end = androidx.compose.ui.geometry.Offset(100f, 100f)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Title & Artist
                    Text(
                        text = mediaMetadata?.title ?: "",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name } ?: "",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(enabled = canSkipPrevious, onClick = onPrevious)
                        )
                        Spacer(modifier = Modifier.width(32.dp))
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CircleShape,
                                    ambientColor = AppleRed,
                                    spotColor = AppleRed
                                )
                                .clip(CircleShape)
                                .background(AppleRed)
                                .clickable(onClick = onPlayPause),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(if (isPlaying && !isLoading) R.drawable.pause else R.drawable.play),
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(enabled = canSkipNext, onClick = onNext)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Slider
                    val safeDuration = duration.takeIf { it > 0 } ?: 0L
                    val sliderValue = if (safeDuration > 0) position.coerceIn(0L, safeDuration).toFloat() else 0f
                    var isDragging by remember { mutableStateOf(false) }
                    var draggingValue by remember { mutableStateOf(0f) }
                    val displayValue = if (isDragging) draggingValue else sliderValue

                    val sliderColors = SliderDefaults.colors(
                        thumbColor = Color.Gray,
                        activeTrackColor = Color.Gray.copy(alpha = 0.5f),
                        inactiveTrackColor = Color.LightGray
                    )

                    when (sliderStyle) {
                        SliderStyle.DEFAULT -> {
                            Slider(
                                value = displayValue,
                                valueRange = 0f..(safeDuration.toFloat().coerceAtLeast(1f)),
                                onValueChange = {
                                    isDragging = true
                                    draggingValue = it
                                    onSeek(it.toLong())
                                },
                                onValueChangeFinished = {
                                    isDragging = false
                                    onSeekFinished()
                                },
                                colors = sliderColors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        SliderStyle.SQUIGGLY -> {
                            SquigglySlider(
                                value = displayValue,
                                valueRange = 0f..(safeDuration.toFloat().coerceAtLeast(1f)),
                                onValueChange = {
                                    isDragging = true
                                    draggingValue = it
                                    onSeek(it.toLong())
                                },
                                onValueChangeFinished = {
                                    isDragging = false
                                    onSeekFinished()
                                },
                                colors = sliderColors,
                                modifier = Modifier.fillMaxWidth(),
                                squigglesSpec = SquigglySlider.SquigglesSpec(
                                    amplitude = if (isPlaying) 2.dp else 0.dp,
                                    strokeWidth = 3.dp,
                                )
                            )
                        }
                        SliderStyle.SLIM -> {
                            Slider(
                                value = displayValue,
                                valueRange = 0f..(safeDuration.toFloat().coerceAtLeast(1f)),
                                onValueChange = {
                                    isDragging = true
                                    draggingValue = it
                                    onSeek(it.toLong())
                                },
                                onValueChangeFinished = {
                                    isDragging = false
                                    onSeekFinished()
                                },
                                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                track = { sliderState ->
                                    PlayerSliderTrack(
                                        sliderState = sliderState,
                                        colors = sliderColors,
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = makeTimeString(displayValue.toLong()), fontSize = 12.sp, color = Color.Gray)
                        Text(text = makeTimeString(safeDuration), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            
            // Bottom Bar Icons inside the gradient area
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left empty space for Queue
                Spacer(modifier = Modifier
                    .weight(0.5f)
                    .height(64.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onQueueClick
                    )
                )

                // Shuffle
                androidx.compose.material3.IconButton(onClick = onShuffleClick) {
                    Icon(
                        painter = painterResource(
                            if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle
                        ),
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) AppleRed else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Middle left empty space for Queue
                Spacer(modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onQueueClick
                    )
                )

                // Like
                androidx.compose.material3.IconButton(onClick = onLikeClick) {
                    Icon(
                        painter = painterResource(
                            if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = "Like",
                        tint = if (isLiked) AppleRed else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Middle right empty space for Queue
                Spacer(modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onQueueClick
                    )
                )

                // Repeat
                androidx.compose.material3.IconButton(onClick = onRepeatClick) {
                    Icon(
                        painter = painterResource(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) AppleRed else Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f)
                    )
                }

                // Right empty space for Queue
                Spacer(modifier = Modifier
                    .weight(0.5f)
                    .height(64.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onQueueClick
                    )
                )
            }
        }
    }
}

@Composable
fun SortMenuIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 2.dp.toPx()
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth / 2)

        // Top line
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(0f, height * 0.25f - strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(width, strokeWidth),
            cornerRadius = cornerRadius
        )
        // Middle line
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(0f, height * 0.5f - strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(width * 0.65f, strokeWidth),
            cornerRadius = cornerRadius
        )
        // Bottom line
        drawRoundRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(0f, height * 0.75f - strokeWidth / 2),
            size = androidx.compose.ui.geometry.Size(width * 0.35f, strokeWidth),
            cornerRadius = cornerRadius
        )
    }
}





