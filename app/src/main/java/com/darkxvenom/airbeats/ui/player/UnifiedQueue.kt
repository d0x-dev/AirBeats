package com.darkxvenom.airbeats.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AutoLoadMoreKey
import com.darkxvenom.airbeats.extensions.metadata
import com.darkxvenom.airbeats.extensions.toggleRepeatMode
import com.darkxvenom.airbeats.ui.component.BottomSheet
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.utils.rememberPreference

/** The single player queue: current track, playback controls, Endless queue, and upcoming tracks. */
@Composable
fun UnifiedQueue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentIndex by playerConnection.currentWindowIndex.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    var endlessQueue by rememberPreference(AutoLoadMoreKey, defaultValue = true)
    val listState = rememberLazyListState()
    val currentItem = queueWindows.getOrNull(currentIndex)?.mediaItem
    var actionMenuIndex by remember { mutableIntStateOf(-1) }
    val upcoming = remember(queueWindows, currentIndex) {
        queueWindows.withIndex().drop((currentIndex + 1).coerceAtLeast(0))
    }

    LaunchedEffect(currentIndex, state.isCollapsed) {
        if (!state.isCollapsed && currentIndex >= 0) listState.scrollToItem(0)
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        background = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) },
        collapsedContent = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IconButton(onClick = state::expandSoft) {
                    Icon(painterResource(R.drawable.expand_less), contentDescription = "Open queue")
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = state::collapseSoft) {
                    Icon(painterResource(R.drawable.expand_more), contentDescription = "Close queue")
                }
                Text(
                    text = "Queue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled }) {
                    Icon(
                        painter = painterResource(if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
                IconButton(onClick = playerConnection.player::toggleRepeatMode) {
                    Icon(
                        painter = painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one else R.drawable.repeat),
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            currentItem?.metadata?.let { metadata ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                ) {
                    AsyncImage(
                        model = metadata.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Now playing", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(metadata.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(metadata.artists.joinToString { it.name }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Continue playing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (!queueTitle.isNullOrBlank()) Text(queueTitle!!, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("Endless queue", style = MaterialTheme.typography.labelLarge)
                Switch(checked = endlessQueue, onCheckedChange = { endlessQueue = it }, modifier = Modifier.padding(start = 10.dp))
            }

            Text("Up next", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 36.dp),
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(upcoming, key = { _, item -> "${item.index}-${item.value.uid}" }) { _, item ->
                    val metadata = item.value.mediaItem.metadata ?: return@itemsIndexed
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { playerConnection.player.seekToDefaultPosition(item.index) }
                            .padding(vertical = 4.dp),
                    ) {
                        AsyncImage(model = metadata.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(metadata.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                            Text(metadata.artists.joinToString { it.name }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        }
                        Box {
                            IconButton(onClick = { actionMenuIndex = item.index }) {
                                Icon(painterResource(R.drawable.more_vert), contentDescription = "Queue item options")
                            }
                            DropdownMenu(expanded = actionMenuIndex == item.index, onDismissRequest = { actionMenuIndex = -1 }) {
                                DropdownMenuItem(text = { Text("Move up") }, enabled = item.index > currentIndex + 1, onClick = { playerConnection.player.moveMediaItem(item.index, item.index - 1); actionMenuIndex = -1 })
                                DropdownMenuItem(text = { Text("Move down") }, enabled = item.index < queueWindows.lastIndex, onClick = { playerConnection.player.moveMediaItem(item.index, item.index + 1); actionMenuIndex = -1 })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { playerConnection.player.removeMediaItem(item.index); actionMenuIndex = -1 })
                            }
                        }
                    }
                }
            }
        }
    }
}
