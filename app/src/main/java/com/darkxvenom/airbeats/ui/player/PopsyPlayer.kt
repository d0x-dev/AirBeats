package com.darkxvenom.airbeats.ui.player

import androidx.compose.ui.res.stringResource

import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.utils.makeTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopsyPlayer(
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
    onAddToPlaylistClick: () -> Unit,
    shuffleModeEnabled: Boolean,
    onShuffleClick: () -> Unit,
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit
) {
    val popsyPurple = Color(0xFF8B5CF6)
    val bottomBg = Color(0xFFF3F4F6)
    
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val topSectionHeight = screenHeight * 0.65f

    Box(modifier = Modifier.fillMaxSize().bottomSheetDraggable(state).background(bottomBg)) {
        // Purple Top Background
        Surface(
            color = popsyPurple,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(topSectionHeight)
        ) {
            Column(modifier = Modifier.fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    .padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onCollapse),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = stringResource(R.string.collapse),
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.ui_text_n_o_w_p_l_a_y_i_n_g),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onMenuClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.menu),
                            contentDescription = stringResource(R.string.ui_a11y_menu),
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Artwork and Side Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Like button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onLikeClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = stringResource(R.string.action_like),
                            colorFilter = ColorFilter.tint(popsyPurple),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Artwork
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(Color.White, CircleShape)
                            .padding(12.dp)
                    ) {
                        AsyncImage(
                            model = mediaMetadata?.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        // Vinyl hole
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(40.dp)
                                .background(Color(0xFF2C2C35), CircleShape)
                        )
                    }

                    // Add to Playlist Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = onAddToPlaylistClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.playlist_add),
                            contentDescription = stringResource(R.string.add_to_playlist),
                            colorFilter = ColorFilter.tint(popsyPurple),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Text details
                Text(
                    text = mediaMetadata?.title ?: "Title",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name } ?: "Artist",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(54.dp))
            }
        }

        // Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .offset(y = topSectionHeight - 36.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(16.dp, CircleShape, spotColor = popsyPurple.copy(alpha = 0.2f))
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = stringResource(R.string.ui_a11y_previous),
                    colorFilter = ColorFilter.tint(popsyPurple.copy(alpha = if (canSkipPrevious) 1f else 0.4f)),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            // Play/Pause
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(16.dp, CircleShape, spotColor = popsyPurple.copy(alpha = 0.3f))
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = popsyPurple,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Image(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = stringResource(R.string.ui_a11y_play_pause),
                        colorFilter = ColorFilter.tint(popsyPurple),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Next
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(16.dp, CircleShape, spotColor = popsyPurple.copy(alpha = 0.2f))
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = stringResource(R.string.ui_a11y_next),
                    colorFilter = ColorFilter.tint(popsyPurple.copy(alpha = if (canSkipNext) 1f else 0.4f)),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Secondary Controls (Shuffle, Lyrics, Queue, Repeat)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 185.dp, start = 32.dp, end = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffleClick) {
                Image(
                    painter = painterResource(if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                    contentDescription = stringResource(R.string.shuffle),
                    colorFilter = ColorFilter.tint(if (shuffleModeEnabled) popsyPurple else Color.Gray),
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onLyricsClick) {
                Image(
                    painter = painterResource(R.drawable.lyrics),
                    contentDescription = stringResource(R.string.lyrics),
                    colorFilter = ColorFilter.tint(Color.Gray),
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onQueueClick) {
                Image(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = stringResource(R.string.queue),
                    colorFilter = ColorFilter.tint(Color.Gray),
                    modifier = Modifier.size(24.dp)
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
                    colorFilter = ColorFilter.tint(if (repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) Color.Gray else popsyPurple),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Bottom section: Slider
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 60.dp, start = 32.dp, end = 32.dp)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = makeTimeString(position),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (duration != androidx.media3.common.C.TIME_UNSET) makeTimeString(duration) else "",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Slider(
                value = position.toFloat(),
                valueRange = 0f..(if (duration == androidx.media3.common.C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { onSeek(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = popsyPurple,
                    inactiveTrackColor = Color(0xFFD1D5DB)
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.White, CircleShape)
                            .border(6.dp, popsyPurple, CircleShape)
                            .shadow(2.dp, CircleShape)
                    )
                }
            )
        }
    }
}






