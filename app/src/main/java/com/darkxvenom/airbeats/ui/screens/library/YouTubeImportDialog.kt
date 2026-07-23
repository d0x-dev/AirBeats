package com.darkxvenom.airbeats.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.entities.PlaylistEntity
import com.darkxvenom.airbeats.db.entities.PlaylistSongMap
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.utils.completed
import com.darkxvenom.airbeats.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun YouTubeImportDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("Fetching playlist details...") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!isImporting) onDismiss()
        },
        icon = {
            Icon(
                painter = painterResource(R.drawable.youtube),
                contentDescription = "YouTube",
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            )
        },
        title = {
            Text(
                text = if (isImporting) "Importing YouTube Playlist" else "Import YouTube Playlist",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (resultMessage != null) {
                    Text(resultMessage!!, style = MaterialTheme.typography.bodyMedium)
                } else if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else if (isImporting) {
                    LinearProgressIndicator(
                        progress = { if (total > 0) progress.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = if (total > 0) "Importing song $progress of $total..." else statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Paste a YouTube or YouTube Music playlist link below to import it to your library:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("YouTube Playlist Link") },
                        placeholder = { Text("https://www.youtube.com/playlist?list=...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            if (resultMessage != null || errorMessage != null) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            } else if (!isImporting) {
                TextButton(
                    onClick = {
                        val playlistId = extractYouTubePlaylistId(url)
                        if (playlistId.isNullOrBlank()) {
                            errorMessage = "Invalid YouTube playlist link or ID."
                            return@TextButton
                        }

                        isImporting = true
                        errorMessage = null
                        statusText = "Connecting to YouTube..."

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val cleanBrowseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
                                val rawId = if (playlistId.startsWith("VL")) playlistId.removePrefix("VL") else playlistId

                                val playlistPage = YouTube.playlist(rawId).completed().getOrNull()
                                    ?: YouTube.playlist(cleanBrowseId).completed().getOrNull()

                                if (playlistPage == null) {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "Unable to fetch playlist. Ensure the link is public or unlisted."
                                        isImporting = false
                                    }
                                    return@launch
                                }

                                val songs = playlistPage.songs
                                val playlistTitle = playlistPage.playlist.title

                                withContext(Dispatchers.Main) {
                                    total = songs.size
                                    statusText = "Saving $total songs to library..."
                                }

                                val playlistEntity = PlaylistEntity(
                                    name = playlistTitle,
                                    browseId = cleanBrowseId,
                                    isEditable = false,
                                    remoteSongCount = songs.size
                                ).toggleLike()

                                database.transaction {
                                    insert(playlistEntity)
                                    songs.map { it.toMediaMetadata() }
                                        .onEach { song ->
                                            insert(song)
                                        }
                                        .mapIndexed { index, song ->
                                            PlaylistSongMap(
                                                songId = song.id,
                                                playlistId = playlistEntity.id,
                                                position = index
                                            )
                                        }
                                        .forEach { songMap ->
                                            insert(songMap)
                                        }
                                }

                                withContext(Dispatchers.Main) {
                                    resultMessage = "Successfully imported '$playlistTitle' (${songs.size} songs)!"
                                    isImporting = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Error importing playlist: ${e.message ?: "Unknown error"}"
                                    isImporting = false
                                }
                            }
                        }
                    },
                    enabled = url.isNotBlank()
                ) {
                    Text("Import Playlist")
                }
            }
        },
        dismissButton = {
            if (!isImporting && resultMessage == null && errorMessage == null) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}

private fun extractYouTubePlaylistId(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("PL") || trimmed.startsWith("VL") || trimmed.startsWith("RD") || trimmed.startsWith("FL")) {
        return trimmed
    }
    return try {
        val uri = android.net.Uri.parse(trimmed)
        val listParam = uri.getQueryParameter("list")
        if (!listParam.isNullOrEmpty()) {
            listParam
        } else {
            val regex = Regex("""[?&]list=([a-zA-Z0-9_-]+)""")
            regex.find(trimmed)?.groupValues?.get(1) ?: trimmed
        }
    } catch (e: Exception) {
        trimmed
    }
}
