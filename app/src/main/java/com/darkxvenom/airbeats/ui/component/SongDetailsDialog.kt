package com.darkxvenom.airbeats.ui.component

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.MediaMetadata

@Composable
fun SongDetailsDialog(
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    
    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        containerColor = AlertDialogDefaults.containerColor,
        icon = {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                listOf(
                    stringResource(R.string.song_title) to mediaMetadata?.title,
                    stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                    stringResource(R.string.media_id) to mediaMetadata?.id,
                    "Itag" to currentFormat?.itag?.toString(),
                    stringResource(R.string.mime_type) to currentFormat?.mimeType,
                    stringResource(R.string.codecs) to currentFormat?.codecs,
                    stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                    stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                    stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                    stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                    stringResource(R.string.file_size) to
                            currentFormat?.contentLength?.let {
                                if (it > 0) Formatter.formatShortFileSize(context, it) else null
                            },
                ).forEach { (label, text) ->
                    val displayText = text ?: stringResource(R.string.unknown)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
