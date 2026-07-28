/*
 * AirBeats Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.darkxvenom.airbeats.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import android.content.res.Configuration
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_READY
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import androidx.core.graphics.drawable.toBitmap
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyle
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyleKey
import com.darkxvenom.airbeats.constants.SliderStyle
import com.darkxvenom.airbeats.constants.SliderStyleKey
import com.darkxvenom.airbeats.db.entities.LyricsEntity
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.extensions.toggleRepeatMode
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.ui.component.Lyrics
import com.darkxvenom.airbeats.ui.component.LyricsV2
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.BigSeekBar
import androidx.navigation.NavController
import com.darkxvenom.airbeats.ui.menu.LyricsMenu
import com.darkxvenom.airbeats.ui.theme.PlayerColorExtractor
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.di.LyricsHelperEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.runCatching
import com.darkxvenom.airbeats.utils.makeTimeString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirBeatsLyricsScreen(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val useLyricsV2 = true

    // Auto-fetch lyrics when no lyrics found (same logic as refetch)
    LaunchedEffect(mediaMetadata.id, currentLyrics) {
        if (currentLyrics == null) {
            // Small delay to ensure database state is stable
            delay(500)
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Get LyricsHelper from Hilt
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    
                    // Fetch lyrics automatically
                    val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                    
                    // Save to database
                    database.query {
                        upsert(LyricsEntity(mediaMetadata.id, lyrics))
                    }
                } catch (e: Exception) {
                    // Handle error silently - user can manually refetch if needed
                }
            }
        }
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    
    // Track loading state: when buffering or when user is seeking
    val isLoading = playbackState == STATE_BUFFERING || sliderPosition != null

    val textBackgroundColor = Color.White
    val icBackgroundColor = Color.Black

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = player.currentPosition
                duration = player.duration
            }
        }
    }

    BackHandler(onBack = onBackClick)

    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            coil.compose.AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
            )
        }

        // Check orientation and layout accordingly
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // Landscape layout - split screen horizontally
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    // Unified header across full width
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .zIndex(1f),  // Ensure header is above content
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Down arrow button (left)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 16.dp
                                    )
                                ) { onBackClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = stringResource(R.string.close),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Now Playing info in center
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.titleMedium,
                                color = textBackgroundColor
                            )
                            Text(
                                text = mediaMetadata.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textBackgroundColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // More button (right)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 16.dp
                                    )
                                ) {
                                    menuState.show {
                                        LyricsMenu(
                                            lyricsProvider = { currentLyrics },
                                            mediaMetadataProvider = { mediaMetadata },
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_horiz),
                                contentDescription = stringResource(R.string.more_options),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Main content row
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Right side - Lyrics only
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            // Lyrics content - centered in landscape
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center  // Center lyrics in landscape
                            ) {
                                if (useLyricsV2) {
                                    LyricsV2(
                                        sliderPositionProvider = { sliderPosition }
                                    )
                                } else {
                                    Lyrics(
                                        sliderPositionProvider = { sliderPosition },

                                        modifier = modifier,
                                    )
                                }
                            }
                        }
                        
                        // Left side - Controls only (from slider to volume)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(horizontal = 48.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Slider
                            androidx.compose.material3.Slider(
                                value = (sliderPosition ?: position).toFloat(),
                                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                onValueChange = {
                                    sliderPosition = it.toLong()
                                },
                                onValueChangeFinished = {
                                    sliderPosition?.let {
                                        player.seekTo(it)
                                        position = it
                                    }
                                    sliderPosition = null
                                },
                                colors = androidx.compose.material3.SliderDefaults.colors(
                                    activeTrackColor = textBackgroundColor,
                                    thumbColor = textBackgroundColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Time display below slider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = makeTimeString(sliderPosition ?: position),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textBackgroundColor.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textBackgroundColor.copy(alpha = 0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Control buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Repeat button
                                IconButton(
                                    onClick = { playerConnection.player.toggleRepeatMode() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            when (repeatMode) {
                                                Player.REPEAT_MODE_OFF, 
                                                Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                                else -> R.drawable.repeat
                                            }
                                        ),
                                        contentDescription = when (repeatMode) {
                                            Player.REPEAT_MODE_OFF -> "Repeat Off"
                                            Player.REPEAT_MODE_ALL -> "Repeat All"
                                            Player.REPEAT_MODE_ONE -> "Repeat One"
                                            else -> "Repeat"
                                        },
                                        tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                                            textBackgroundColor.copy(alpha = 0.4f)
                                        } else {
                                            textBackgroundColor
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Previous button
                                IconButton(
                                    onClick = { player.seekToPrevious() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_previous),
                                        contentDescription = null,
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Play/Pause button
                                IconButton(
                                    onClick = { player.togglePlayPause() },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = textBackgroundColor,
                                            strokeWidth = 3.dp
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(
                                                if (isPlaying) R.drawable.pause else R.drawable.play
                                            ),
                                            contentDescription = if (isPlaying) "Pause" else stringResource(R.string.play),
                                            tint = textBackgroundColor,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                // Next button
                                IconButton(
                                    onClick = { player.seekToNext() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_next),
                                        contentDescription = null,
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Shuffle button
                                IconButton(
                                    onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = if (shuffleModeEnabled) stringResource(R.string.shuffle) else stringResource(R.string.shuffle),
                                        tint = if (shuffleModeEnabled) {
                                            textBackgroundColor
                                        } else {
                                            textBackgroundColor.copy(alpha = 0.4f)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Volume Control
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.volume_off),
                                    contentDescription = "Minimum Volume",
                                    modifier = Modifier.size(20.dp),
                                    tint = textBackgroundColor
                                )

                                BigSeekBar(
                                    progressProvider = playerVolume::value,
                                    onProgressChange = { playerConnection.service.playerVolume.value = it },
                                    color = textBackgroundColor,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(24.dp)
                                        .padding(horizontal = 16.dp)
                                )

                                Icon(
                                    painter = painterResource(R.drawable.volume_up),
                                    contentDescription = "Maximum Volume",
                                    modifier = Modifier.size(20.dp),
                                    tint = textBackgroundColor
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                // Portrait layout - original layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    // Header with More button and Down arrow on opposite sides
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Down arrow button (left)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 16.dp
                                    )
                                ) { onBackClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = stringResource(R.string.close),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Centered content
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.now_playing),
                                style = MaterialTheme.typography.titleMedium,
                                color = textBackgroundColor
                            )
                            Text(
                                text = mediaMetadata.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textBackgroundColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // More button (right)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(
                                        bounded = true,
                                        radius = 16.dp
                                    )
                                ) {
                                    menuState.show {
                                        LyricsMenu(
                                            lyricsProvider = { currentLyrics },
                                            mediaMetadataProvider = { mediaMetadata },
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_horiz),
                                contentDescription = stringResource(R.string.more_options),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (useLyricsV2) {
                            LyricsV2(
                                sliderPositionProvider = { sliderPosition }
                            )
                        } else {
                            Lyrics(
                                sliderPositionProvider = { sliderPosition },

                                modifier = modifier,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 16.dp)
                    ) {
                        androidx.compose.material3.Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = textBackgroundColor,
                                thumbColor = textBackgroundColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Time display below slider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = makeTimeString(sliderPosition ?: position),
                                style = MaterialTheme.typography.bodySmall,
                                color = textBackgroundColor.copy(alpha = 0.7f)
                            )
                            Text(
                                text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = textBackgroundColor.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Optimized control buttons for better fit
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp), // Reduced padding
                            horizontalArrangement = Arrangement.SpaceEvenly, // Even distribution
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Repeat button with clear state indication
                            IconButton(
                                onClick = { playerConnection.player.toggleRepeatMode() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (repeatMode) {
                                            Player.REPEAT_MODE_OFF, 
                                            Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                            else -> R.drawable.repeat
                                        }
                                    ),
                                    contentDescription = when (repeatMode) {
                                        Player.REPEAT_MODE_OFF -> "Repeat Off"
                                        Player.REPEAT_MODE_ALL -> "Repeat All"
                                        Player.REPEAT_MODE_ONE -> "Repeat One"
                                        else -> "Repeat"
                                    },
                                    tint = if (repeatMode == Player.REPEAT_MODE_OFF) {
                                        // Inactive state - low opacity
                                        textBackgroundColor.copy(alpha = 0.4f)
                                    } else {
                                        // Active state - full brightness
                                        textBackgroundColor
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Previous button
                            IconButton(
                                onClick = { player.seekToPrevious() },
                                modifier = Modifier.size(40.dp) // Slightly smaller
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_previous),
                                    contentDescription = null,
                                    tint = textBackgroundColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Play/Pause button (largest)
                            IconButton(
                                onClick = { player.togglePlayPause() },
                                modifier = Modifier.size(56.dp) // Slightly smaller but still prominent
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = textBackgroundColor,
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(
                                            if (isPlaying) R.drawable.pause else R.drawable.play
                                        ),
                                        contentDescription = if (isPlaying) "Pause" else stringResource(R.string.play),
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // Next button
                            IconButton(
                                onClick = { player.seekToNext() },
                                modifier = Modifier.size(40.dp) // Slightly smaller
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.skip_next),
                                    contentDescription = null,
                                    tint = textBackgroundColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Shuffle button with clear state indication
                            IconButton(
                                onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = if (shuffleModeEnabled) stringResource(R.string.shuffle) else stringResource(R.string.shuffle),
                                    tint = if (shuffleModeEnabled) {
                                        // Active state - full brightness
                                        textBackgroundColor
                                    } else {
                                        // Inactive state - low opacity
                                        textBackgroundColor.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp)) // Proper spacing

                        // Volume Control
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 48.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.volume_off),
                                contentDescription = "Minimum Volume",
                                modifier = Modifier.size(20.dp),
                                tint = textBackgroundColor
                            )

                            BigSeekBar(
                                progressProvider = playerVolume::value,
                                onProgressChange = { playerConnection.service.playerVolume.value = it },
                                color = textBackgroundColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .padding(horizontal = 16.dp)
                            )

                            Icon(
                                painter = painterResource(R.drawable.volume_up),
                                contentDescription = "Maximum Volume",
                                modifier = Modifier.size(20.dp),
                                tint = textBackgroundColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
