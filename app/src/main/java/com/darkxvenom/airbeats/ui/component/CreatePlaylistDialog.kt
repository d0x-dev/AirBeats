package com.darkxvenom.airbeats.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.entities.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val cookie by rememberPreference(InnerTubeCookieKey, "")
    val loggedIn = "SAPISID" in parseCookieString(cookie)
    // Signed in: default to Google (YouTube) so the playlist shows up everywhere; user can pick Local.
    var syncedPlaylist by remember(loggedIn) { mutableStateOf(loggedIn) }
    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(R.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        onDone = { playlistName ->
            coroutineScope.launch(Dispatchers.IO) {
                val browseId = if (syncedPlaylist) {
                    YouTube.createPlaylist(playlistName).getOrNull()
                } else null
                database.query {
                    insert(
                        PlaylistEntity(
                            name = playlistName,
                            browseId = browseId,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true,
                        )
                    )
                }
            }
        },
        extraContent = {
            if (allowSyncing && loggedIn) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 40.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.sync_playlist),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.allows_for_sync_witch_youtube),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        FilterChip(selected = !syncedPlaylist, onClick = { syncedPlaylist = false }, label = { Text("Local") })
                        FilterChip(selected = syncedPlaylist, onClick = { syncedPlaylist = true }, label = { Text("Google") },
                            modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    )
}
