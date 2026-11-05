package com.darkxvenom.airbeats.ui.player

import androidx.compose.ui.res.stringResource

import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.request.ImageRequest
import com.darkxvenom.airbeats.ui.theme.extractThemeColor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.ColourfullPlayerColorKey
import com.darkxvenom.airbeats.constants.SliderStyle
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyle
import com.darkxvenom.airbeats.ui.component.PlayerSliderTrack
import me.saket.squiggles.SquigglySlider
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColourfullPlayer(
    state: BottomSheetState,
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    sliderStyle: SliderStyle,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onOpenFullscreenLyrics: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCollapse: () -> Unit,
    onMenuClick: () -> Unit,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    val (colorInt, _) = rememberPreference(ColourfullPlayerColorKey, defaultValue = 0xFF4CAF50.toInt())
    
    var autoColor by remember { mutableStateOf<Color?>(null) }
    val context = LocalContext.current
    val thumbnailUrl = mediaMetadata?.thumbnailUrl
    
    LaunchedEffect(colorInt, thumbnailUrl) {
        if (colorInt == 0 && thumbnailUrl != null) {
            val result = ImageLoader(context).execute(
                ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .allowHardware(false)
                    .build()
            )
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            autoColor = bitmap?.extractThemeColor()
        }
    }

    val bgColor = if (colorInt == 0) (autoColor ?: Color(0xFF4CAF50)) else Color(colorInt)
    val textPrimary = Color.Black
    val textSecondary = Color.Black.copy(alpha = 0.6f)

    Box(modifier = Modifier.fillMaxSize().bottomSheetDraggable(state)
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Image(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = stringResource(R.string.collapse),
                        colorFilter = ColorFilter.tint(textPrimary),
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.ui_text_playing_now),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = textPrimary
                    )
                    mediaMetadata?.album?.title?.let { albumTitle ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = albumTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    .aspectRatio(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Thumbnail(
                    sliderPositionProvider = { position },
                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(32.dp),
                    showPlayingFrom = false,
                    artworkScale = 1.0f
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Metadata Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaMetadata?.title ?: "Unknown",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
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
                        colorFilter = ColorFilter.tint(textPrimary),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Slider Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                val valueRange = 0f..duration.coerceAtLeast(1L).toFloat()
                val sliderValue = if (duration > 0) position.coerceIn(0L, duration).toFloat() else 0f
                val sliderColors = SliderDefaults.colors(
                    thumbColor = textPrimary,
                    activeTrackColor = textPrimary,
                    inactiveTrackColor = textPrimary.copy(alpha = 0.2f)
                )

                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        Slider(
                            value = sliderValue,
                            valueRange = valueRange,
                            onValueChange = { onSeek(it.toLong()) },
                            onValueChangeFinished = onSeekFinished,
                            colors = sliderColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SliderStyle.SQUIGGLY -> {
                        SquigglySlider(
                            value = sliderValue,
                            valueRange = valueRange,
                            onValueChange = { onSeek(it.toLong()) },
                            onValueChangeFinished = onSeekFinished,
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
                            value = sliderValue,
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(position),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                    Text(
                        text = makeTimeString(duration),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Controls Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onQueueClick) {
                    Image(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = stringResource(R.string.queue),
                        colorFilter = ColorFilter.tint(textPrimary),
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = onPrevious) {
                    Image(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = stringResource(R.string.ui_a11y_previous),
                        colorFilter = ColorFilter.tint(if (canSkipPrevious) textPrimary else textPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(textPrimary)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        colorFilter = ColorFilter.tint(bgColor),
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onNext) {
                    Image(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.ui_a11y_next),
                        colorFilter = ColorFilter.tint(if (canSkipNext) textPrimary else textPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.size(36.dp)
                    )
                }

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
                        colorFilter = ColorFilter.tint(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) textPrimary.copy(alpha = 0.6f) else textPrimary),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}






