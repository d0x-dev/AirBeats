package com.darkxvenom.airbeats.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import com.darkxvenom.airbeats.ui.player.components.ThinSlider
import com.darkxvenom.airbeats.utils.makeTimeString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import coil.compose.AsyncImage
import android.media.AudioManager
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernPlayer(
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
    onOpenFullscreenLyrics: () -> Unit,
    navController: NavController
) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val context = LocalContext.current
    val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume
    var volume by remember { mutableFloatStateOf(currentVolume) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .bottomSheetDraggable(state)
    ) {
        ImmersivePlayerBackdrop(
            thumbnailUrl = mediaMetadata?.thumbnailUrl,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // MAIN PLAYER SCREEN
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight)
                    .padding(horizontal = 24.dp)
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown, 
                            contentDescription = "Collapse",
                            tint = Color.White
                        )
                    }
                }
                
                // Artwork
                val artworkSize = configuration.screenWidthDp.dp - 48.dp
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(artworkSize)
                        .clip(RoundedCornerShape(12.dp))
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Title and Artist Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaMetadata?.title ?: "Unknown Title",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    IconButton(onClick = onLikeClick) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color.Red else Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ThinSlider Progress Bar
                val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
                ThinSlider(
                    value = progress,
                    onValueChange = { onSeek((it * duration).toLong()) },
                    onValueChangeFinished = onSeekFinished,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(position),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = makeTimeString(duration),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(36.dp))
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Volume Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeDown, null, tint = Color.White.copy(alpha = 0.5f))
                    Spacer(Modifier.width(10.dp))
                    ThinSlider(
                        value = volume,
                        onValueChange = { 
                            volume = it
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxVolume).toInt(), 0)
                        },
                        modifier = Modifier.weight(1f),
                        activeHeight = 10.dp,
                        idleHeight = 6.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White.copy(alpha = 0.5f))
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Queue & Lyrics Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Queue Button / Section
                Button(
                    onClick = onQueueClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("Up Next", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lyrics Button / Section
                Button(
                    onClick = onOpenFullscreenLyrics,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("Lyrics", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
