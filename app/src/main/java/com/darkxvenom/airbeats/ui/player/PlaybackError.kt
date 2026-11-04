package com.darkxvenom.airbeats.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.PlaybackException
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.MediaMetadata
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PlaybackError(
    error: PlaybackException,
    retry: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onTap = { retry() },
                )
            },
    ) {
        Icon(
            painter = painterResource(R.drawable.info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )

        Text(
            text = error.cause?.cause?.message ?: stringResource(R.string.error_unknown),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Full-featured popup dialog shown across all player screens whenever a playback/stream error occurs.
 * Displays human-readable diagnosis, song information, error details, and includes Copy and Share buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackErrorDialog(
    error: PlaybackException,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current

    val songTitle = mediaMetadata?.title?.takeIf { it.isNotBlank() } ?: "Unknown Song"
    val songArtist = mediaMetadata?.artists?.joinToString(", ") { it.name }?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
    val songId = mediaMetadata?.id ?: ""

    val errorCodeName = error.errorCodeName
    val errorCode = error.errorCode
    val rawMessage = error.message ?: ""
    val rootCause = generateSequence<Throwable>(error) { it.cause }.lastOrNull()
    val causeMessage = rootCause?.message ?: rootCause?.localizedMessage ?: error.cause?.message

    // Human-friendly error diagnosis
    val friendlyExplanation = when {
        rawMessage.contains("403") || (causeMessage?.contains("403") == true) ->
            "Stream link was rejected with HTTP 403 Forbidden. The streaming token or URL expired or is restricted."
        rawMessage.contains("JioSaavn") ->
            "Unable to retrieve JioSaavn audio stream: ${causeMessage ?: rawMessage}"
        rawMessage.contains("Spotify track match") ->
            "Could not match this Spotify track to a playable YouTube stream."
        rawMessage.contains("behind the live window") ->
            "Playback fell behind the live audio buffer."
        causeMessage?.contains("Unable to connect", ignoreCase = true) == true ||
        rawMessage.contains("Unable to connect", ignoreCase = true) ||
        errorCodeName.contains("NETWORK", ignoreCase = true) ->
            "Network connection error. Please check your internet connectivity."
        !causeMessage.isNullOrBlank() -> causeMessage
        rawMessage.isNotBlank() -> rawMessage
        else -> stringResource(R.string.error_unknown)
    }

    val fullErrorReport = buildString {
        appendLine("=== AirBeats Playback Error Report ===")
        appendLine("Song: $songTitle")
        appendLine("Artist: $songArtist")
        if (songId.isNotBlank()) appendLine("Media ID: $songId")
        appendLine("Error Code: $errorCodeName ($errorCode)")
        if (rawMessage.isNotBlank()) appendLine("Message: $rawMessage")
        if (!causeMessage.isNullOrBlank() && causeMessage != rawMessage) appendLine("Root Cause: $causeMessage")
        if (error.cause != null) {
            appendLine("Exception: ${error.cause!!::class.java.name}")
        }
        appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Error Icon Header
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.ui_text_playback_error),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$songTitle • $songArtist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Error Details Container
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = friendlyExplanation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val detailsText = buildString {
                                append("Code: $errorCodeName ($errorCode)")
                                if (rawMessage.isNotBlank() && rawMessage != friendlyExplanation) {
                                    append("\n$rawMessage")
                                }
                                if (!causeMessage.isNullOrBlank() && causeMessage != friendlyExplanation && causeMessage != rawMessage) {
                                    append("\nCause: $causeMessage")
                                }
                            }
                            Text(
                                text = detailsText.trim(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Copy & Share Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AirBeats Error Report", fullErrorReport)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Error details copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.content_copy),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.ui_text_copy), maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "AirBeats Error: $songTitle")
                                putExtra(Intent.EXTRA_TEXT, fullErrorReport)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Playback Error").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.share_clip), maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons: Dismiss & Retry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(text = stringResource(R.string.dismiss))
                    }

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.refresh),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.retry), maxLines = 1)
                    }
                }
            }
        }
    }
}
