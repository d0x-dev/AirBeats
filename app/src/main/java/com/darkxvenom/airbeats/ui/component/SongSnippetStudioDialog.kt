package com.darkxvenom.airbeats.ui.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.utils.AudioTrimmerUtil
import com.darkxvenom.airbeats.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

@Composable
fun SongSnippetStudioDialog(
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current

    var sourceFile by remember { mutableStateOf<File?>(null) }
    var totalDurationMs by remember {
        mutableLongStateOf((mediaMetadata.duration * 1000L).coerceAtLeast(30_000L))
    }
    var isLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isProcessing by remember { mutableStateOf(false) }
    var processingStatus by remember { mutableStateOf("") }

    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember {
        val initialEnd = 30_000L.coerceAtMost(if (mediaMetadata.duration > 0) mediaMetadata.duration * 1000L else 30_000L)
        mutableLongStateOf(initialEnd.coerceAtLeast(5_000L))
    }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }

    // ExoPlayer for loop preview playback
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    exoPlayer.seekTo(startMs)
                    exoPlayer.play()
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Timber.tag("SnippetStudio").e(error, "ExoPlayer preview error")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Loop monitoring: when current position reaches endMs, smoothly seek back to startMs
    LaunchedEffect(isPlaying, startMs, endMs) {
        if (isPlaying) {
            val cur = exoPlayer.currentPosition
            if (cur < startMs || cur >= endMs) {
                exoPlayer.seekTo(startMs)
            }
            while (isPlaying && isActive) {
                delay(40)
                val pos = exoPlayer.currentPosition
                currentPositionMs = pos
                if (pos >= endMs) {
                    exoPlayer.seekTo(startMs)
                }
            }
        }
    }

    fun loadAudio() {
        isLoading = true
        errorMessage = null
        loadingProgress = 0
        coroutineScope.launch {
            val result = AudioTrimmerUtil.prepareAudioSource(
                context = context,
                mediaMetadata = mediaMetadata,
                playerConnection = playerConnection
            ) { progress ->
                loadingProgress = progress
            }
            when (result) {
                is AudioTrimmerUtil.AudioSourceResult.Success -> {
                    sourceFile = result.file
                    totalDurationMs = result.durationMs.coerceAtLeast(30_000L)
                    endMs = (startMs + 30_000L).coerceAtMost(totalDurationMs)
                    withContext(Dispatchers.Main) {
                        try {
                            exoPlayer.setMediaItem(MediaItem.fromUri(android.net.Uri.fromFile(result.file)))
                            exoPlayer.prepare()
                            exoPlayer.seekTo(startMs)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to prepare preview player")
                        }
                    }
                    isLoading = false
                }
                is AudioTrimmerUtil.AudioSourceResult.Error -> {
                    errorMessage = result.message
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(mediaMetadata.id) {
        loadAudio()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, R.string.storage_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    fun ensurePermission(onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onGranted()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onGranted()
            } else {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    fun shareClip() {
        val src = sourceFile ?: return
        isProcessing = true
        processingStatus = context.getString(R.string.processing_snippet)
        coroutineScope.launch {
            val outputFile = File(context.cacheDir, "snippet_share_${System.currentTimeMillis()}.m4a")
            val trimmed = AudioTrimmerUtil.trimAudio(src, outputFile, startMs, endMs)
            withContext(Dispatchers.Main) {
                isProcessing = false
                if (trimmed && outputFile.exists()) {
                    AudioTrimmerUtil.shareSnippet(context, outputFile, mediaMetadata.title)
                } else {
                    Toast.makeText(context, "Failed to create shareable clip", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun saveClipToMusic() {
        val src = sourceFile ?: return
        ensurePermission {
            isProcessing = true
            processingStatus = context.getString(R.string.processing_snippet)
            coroutineScope.launch {
                val outputFile = File(context.cacheDir, "snippet_download_${System.currentTimeMillis()}.m4a")
                val trimmed = AudioTrimmerUtil.trimAudio(src, outputFile, startMs, endMs)
                if (trimmed && outputFile.exists()) {
                    val artistName = mediaMetadata.artists.joinToString(", ") { it.name }
                    val uri = AudioTrimmerUtil.saveSnippetToMusic(context, outputFile, mediaMetadata.title, artistName)
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        if (uri != null) {
                            Toast.makeText(context, R.string.clip_saved_to_storage, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Failed to save snippet", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, "Trimming failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            exoPlayer.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.content_cut),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ringtone_studio),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${mediaMetadata.title} • ${mediaMetadata.artists.joinToString { it.name }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                    }
                    IconButton(
                        onClick = {
                            exoPlayer.stop()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading_snippet_audio),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (loadingProgress > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.ui_dynamic_progress_percent_final, loadingProgress),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else if (errorMessage != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Failed to load audio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { loadAudio() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                } else {
                    // Studio Controls
                    // 1. Time / Duration Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${makeTimeString(startMs)}  ➔  ${makeTimeString(endMs)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val snippetDurationSec = ((endMs - startMs) / 1000).coerceAtLeast(1)
                            Text(
                                text = stringResource(R.string.snippet_duration, "${snippetDurationSec}s"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Circular Loop Play / Pause Button
                        Surface(
                            shape = CircleShape,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(48.dp)
                                .clickable {
                                    if (isPlaying) {
                                        exoPlayer.pause()
                                    } else {
                                        playerConnection?.player?.pause()
                                        val cur = exoPlayer.currentPosition
                                        if (cur < startMs || cur >= endMs) {
                                            exoPlayer.seekTo(startMs)
                                        }
                                        exoPlayer.play()
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                    contentDescription = null,
                                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2. Dual Range Slider
                    RangeSlider(
                        value = startMs.toFloat()..endMs.toFloat(),
                        onValueChange = { range ->
                            val s = range.start.toLong().coerceAtLeast(0L)
                            val e = range.endInclusive.toLong().coerceAtMost(totalDurationMs)
                            if (e - s >= 3000L) { // Min 3s duration
                                startMs = s
                                endMs = e
                            }
                        },
                        valueRange = 0f..totalDurationMs.coerceAtLeast(3000L).toFloat(),
                        onValueChangeFinished = {
                            exoPlayer.seekTo(startMs)
                            if (isPlaying) {
                                exoPlayer.play()
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0:00",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = makeTimeString(totalDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // 3. Quick Presets (15s Story, 30s Clip, 45s, 60s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val presets = listOf(
                            15_000L to stringResource(R.string.preset_15s),
                            30_000L to stringResource(R.string.preset_30s),
                            45_000L to stringResource(R.string.preset_45s),
                            60_000L to stringResource(R.string.preset_60s)
                        )
                        presets.forEach { (duration, label) ->
                            val isSelected = kotlin.math.abs((endMs - startMs) - duration) <= 500L
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newEnd = (startMs + duration).coerceAtMost(totalDurationMs)
                                    val newStart = if (newEnd - startMs < duration) {
                                        (newEnd - duration).coerceAtLeast(0L)
                                    } else {
                                        startMs
                                    }
                                    startMs = newStart
                                    endMs = newEnd
                                    exoPlayer.seekTo(startMs)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))

                    // 4. Action Row: Download and Share buttons only
                    if (isProcessing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = processingStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Download Button
                            Button(
                                onClick = { saveClipToMusic() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.save_to_storage),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.save_snippet),
                                    maxLines = 1,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Share Clip Button
                            OutlinedButton(
                                onClick = { shareClip() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.share),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.share_clip),
                                    maxLines = 1,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
