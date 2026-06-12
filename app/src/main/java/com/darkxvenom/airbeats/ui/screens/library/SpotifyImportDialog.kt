package com.darkxvenom.airbeats.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.utils.SpotifyImporter
import kotlinx.coroutines.launch

@Composable
fun SpotifyImportDialog(
    onDismiss: () -> Unit
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!isImporting) onDismiss()
        },
        title = { Text(if (isImporting) "Importing Playlist..." else "Import from Spotify") },
        text = {
            Column {
                if (resultMessage != null) {
                    Text(resultMessage!!)
                } else if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                } else if (isImporting) {
                    LinearProgressIndicator(
                        progress = { if (total > 0) progress.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Text("Processing song $progress of $total...")
                } else {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Spotify Playlist URL") },
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
                    Text("OK")
                }
            } else if (!isImporting) {
                TextButton(
                    onClick = {
                        if (url.isNotBlank()) {
                            isImporting = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = SpotifyImporter.importPlaylist(
                                    url = url,
                                    dao = database,
                                    onProgress = { current, max ->
                                        progress = current
                                        total = max
                                    }
                                )
                                result.onSuccess { playlistName ->
                                    resultMessage = "Successfully imported '$playlistName'!"
                                }.onFailure { error ->
                                    errorMessage = "Failed to import: ${error.message}"
                                }
                                isImporting = false
                            }
                        }
                    },
                    enabled = url.isNotBlank()
                ) {
                    Text("Import")
                }
            }
        },
        dismissButton = {
            if (!isImporting && resultMessage == null && errorMessage == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
