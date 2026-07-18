/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.darkxvenom.airbeats.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.entities.FormatEntity
import com.darkxvenom.airbeats.db.entities.Song
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun ShowMediaInfo(videoId: String) {

    if (videoId.isBlank()) return

    val windowInsets = WindowInsets.systemBars
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current

    var song by remember { mutableStateOf<Song?>(null) }
    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    LaunchedEffect(videoId) {
        song = database.song(videoId).firstOrNull()
        currentFormat = database.format(videoId).firstOrNull()
    }

    fun copy(text: String) {
        val cm =
            context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("text", text))
        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(windowInsets.asPaddingValues())
            .padding(horizontal = 16.dp)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        /* ============================================================
         * DETAILS SECTION
         * ============================================================ */

        if (song != null) {

            item {
                SectionTitle(
                    icon = R.drawable.info,
                    title = stringResource(R.string.details)
                )
            }

            item {

                ElevatedCard(
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(Modifier.padding(20.dp)) {

                        val baseList = listOf<Triple<Int, String, String?>>(
                            Triple(R.drawable.music_note, stringResource(R.string.song_title), song?.title),
                            Triple(R.drawable.person, stringResource(R.string.song_artists),
                                song?.artists?.joinToString { it.name }),
                            Triple(R.drawable.info, stringResource(R.string.media_id), song?.id)
                        )

                        val extendedList = baseList + if (currentFormat != null) {
                            listOf<Triple<Int, String, String?>>(
                                Triple(R.drawable.info, "Itag", currentFormat?.itag?.toString()),
                                Triple(R.drawable.info, stringResource(R.string.mime_type), currentFormat?.mimeType),
                                Triple(R.drawable.tune, stringResource(R.string.codecs), currentFormat?.codecs),
                                Triple(R.drawable.graphic_eq, stringResource(R.string.bitrate),
                                    currentFormat?.bitrate?.let { "${it / 1000} Kbps" }),
                                Triple(R.drawable.info, stringResource(R.string.sample_rate),
                                    currentFormat?.sampleRate?.let { "$it Hz" }),
                                Triple(R.drawable.volume_up, stringResource(R.string.volume),
                                    "${(playerConnection?.player?.volume?.times(100))?.toInt()}%"),
                                Triple(
                                    R.drawable.folder,
                                    stringResource(R.string.file_size),
                                    currentFormat?.contentLength?.let {
                                        Formatter.formatShortFileSize(context, it)
                                    }
                                )
                            )
                        } else emptyList()

                        extendedList.forEach { (icon, label, value) ->

                            val text = value ?: stringResource(R.string.unknown)

                            MediaRow(
                                icon = icon,
                                label = label,
                                value = text,
                                onClick = { copy(text) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ============================================================
 * COMPONENTS
 * ============================================================ */

@Composable
private fun SectionTitle(icon: Int, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun MediaRow(
    icon: Int,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(Modifier.padding(vertical = 6.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(start = 24.dp, top = 4.dp)
        )
    }
}
