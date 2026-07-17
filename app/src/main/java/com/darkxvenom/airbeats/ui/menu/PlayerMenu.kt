package com.darkxvenom.airbeats.ui.menu

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalDownloadUtil
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.ListItemHeight
import com.darkxvenom.airbeats.constants.ListThumbnailSize
import com.darkxvenom.airbeats.constants.ThumbnailCornerRadius
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.playback.ExoDownloadService
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.ListDialog
import com.darkxvenom.airbeats.ui.component.ListItem
import com.darkxvenom.airbeats.utils.ListenTogetherClient
import com.darkxvenom.airbeats.utils.ListenTogetherPlaybackState
import com.darkxvenom.airbeats.utils.ListenTogetherSession
import com.darkxvenom.airbeats.utils.ListenTogetherStore
import com.darkxvenom.airbeats.utils.ListenTogetherSync
import com.darkxvenom.airbeats.utils.joinByBullet
import com.darkxvenom.airbeats.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()



    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            item {
                ListItem(
                    title = mediaMetadata.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = mediaMetadata.thumbnailUrl,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle =
                        joinByBullet(
                            mediaMetadata.artists.joinToString { it.name },
                            makeTimeString(mediaMetadata.duration * 1000L),
                        ),
                )
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    var isMuted by remember { mutableStateOf(false) }
    var previousVolume by remember { mutableFloatStateOf(playerVolume.value) }
    var showEqualizerSheet by rememberSaveable { mutableStateOf(false) }
    var showListenTogetherSheet by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(scrollState)
            ) {
                PlayerMenuHeader(mediaMetadata = mediaMetadata)

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.volume),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isMuted) "0%" else "${(playerVolume.value * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                ),
                                contentDescription = stringResource(
                                    if (isMuted) R.string.unmute else R.string.mute
                                ),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        isMuted = !isMuted
                                        if (isMuted) {
                                            previousVolume = playerVolume.value
                                            playerConnection.setVolume(0f)
                                            playerConnection.service.playerVolume.value = 0f
                                        } else {
                                            playerConnection.setVolume(previousVolume)
                                            playerConnection.service.playerVolume.value = previousVolume
                                        }
                                    }
                                    .padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Slider(
                                value = if (isMuted) 0f else playerVolume.value,
                                onValueChange = { newVolume ->
                                    if (!isMuted) {
                                        playerConnection.setVolume(newVolume)
                                        playerConnection.service.playerVolume.value = newVolume
                                        previousVolume = newVolume
                                    }
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                    ),
                ) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            PlayerMenuActionTile(
                                icon = R.drawable.radio,
                                title = R.string.start_radio,
                                modifier = Modifier.weight(1f).height(76.dp),
                            ) {
                                playerConnection.playQueue(
                                    YouTubeQueue(
                                        WatchEndpoint(videoId = mediaMetadata.id),
                                        mediaMetadata
                                    )
                                )
                                onDismiss()
                            }
                            
                            PlayerMenuActionTile(
                                icon = R.drawable.playlist_add,
                                title = R.string.add_to_playlist,
                                modifier = Modifier.weight(1f).height(76.dp),
                            ) {
                                showChoosePlaylistDialog = true
                            }
                            
                            PlayerMenuActionTile(
                                icon = if (download?.state == Download.STATE_COMPLETED) R.drawable.offline else R.drawable.download,
                                title = if (download?.state == Download.STATE_COMPLETED) R.string.remove_download else R.string.download,
                                modifier = Modifier.weight(1f).height(76.dp),
                            ) {
                                if (download?.state == Download.STATE_COMPLETED) {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                } else {
                                    database.transaction {
                                        insert(mediaMetadata)
                                    }
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                            .setCustomCacheKey(mediaMetadata.id)
                                            .setData(mediaMetadata.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                                onDismiss()
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        val savingToastMsg = stringResource(R.string.saving_song)
                        val savedToastMsg = stringResource(R.string.song_saved_successfully)
                        val failedToastMsg = stringResource(R.string.song_save_failed)
                        val permReqMsg = stringResource(R.string.storage_permission_required)

                        val permissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            if (isGranted) {
                                Toast.makeText(context, savingToastMsg, Toast.LENGTH_SHORT).show()
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO).launch {
                                    com.darkxvenom.airbeats.utils.SaveToStorageUtil
                                        .saveToMusicFolder(context, mediaMetadata)
                                        .onSuccess {
                                            launch(Dispatchers.Main) {
                                                Toast.makeText(context, savedToastMsg, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                        .onFailure { e ->
                                            launch(Dispatchers.Main) {
                                                Toast.makeText(context, "$failedToastMsg: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                }
                                onDismiss()
                            } else {
                                Toast.makeText(context, permReqMsg, Toast.LENGTH_LONG).show()
                            }
                        }

                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.save_to_local)) },
                            leadingContent = { Icon(painterResource(R.drawable.save_to_storage), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    true
                                } else {
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) == PackageManager.PERMISSION_GRANTED
                                }

                                if (hasPermission) {
                                    Toast.makeText(context, savingToastMsg, Toast.LENGTH_SHORT).show()
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO).launch {
                                        com.darkxvenom.airbeats.utils.SaveToStorageUtil
                                            .saveToMusicFolder(context, mediaMetadata)
                                            .onSuccess {
                                                launch(Dispatchers.Main) {
                                                    Toast.makeText(context, savedToastMsg, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                            .onFailure { e ->
                                                launch(Dispatchers.Main) {
                                                    Toast.makeText(context, "$failedToastMsg: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                    }
                                    onDismiss()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(
                                        if (librarySong?.song?.inLibrary != null) R.string.remove_from_library else R.string.add_to_library
                                    )
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painterResource(
                                        if (librarySong?.song?.inLibrary != null) R.drawable.library_add_check else R.drawable.library_add
                                    ),
                                    contentDescription = null
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                if (librarySong?.song?.inLibrary != null) {
                                    database.query {
                                        inLibrary(mediaMetadata.id, null)
                                    }
                                } else {
                                    database.transaction {
                                        insert(mediaMetadata)
                                        inLibrary(mediaMetadata.id, LocalDateTime.now())
                                    }
                                }
                                onDismiss()
                            }
                        )
                    }

                    if (artists.isNotEmpty()) {
                        item {
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(stringResource(R.string.view_artist)) },
                                leadingContent = { Icon(painterResource(R.drawable.artist), contentDescription = null) },
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    if (mediaMetadata.artists.size == 1) {
                                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    } else {
                                        showSelectArtistDialog = true
                                    }
                                }
                            )
                        }
                    }

                    if (mediaMetadata.album != null) {
                        item {
                            androidx.compose.material3.ListItem(
                                headlineContent = { Text(stringResource(R.string.view_album)) },
                                leadingContent = { Icon(painterResource(R.drawable.album), contentDescription = null) },
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    navController.navigate("album/${mediaMetadata.album.id}")
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                }
                            )
                        }
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.share)) },
                            leadingContent = { Icon(painterResource(R.drawable.share), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val intent =
                                    Intent().apply {
                                        action = Intent.ACTION_SEND
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "https://play.airbeats.app/${mediaMetadata.id}"
                                        )
                                    }
                                context.startActivity(Intent.createChooser(intent, null))
                                onDismiss()
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.details)) },
                            leadingContent = { Icon(painterResource(R.drawable.info), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                onShowDetailsDialog()
                                onDismiss()
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.always_on_display)) },
                            leadingContent = { Icon(painterResource(R.drawable.dark_mode), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                navController.navigate("always_on_display")
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.equalizer)) },
                            leadingContent = { Icon(painterResource(R.drawable.equalizer), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showEqualizerSheet = true
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.listen_together)) },
                            leadingContent = { Icon(painterResource(R.drawable.group), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showListenTogetherSheet = true
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.advanced)) },
                            leadingContent = { Icon(painterResource(R.drawable.tune), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showPitchTempoDialog = true
                            }
                        )
                    }
                }
            }

        if (showEqualizerSheet) {
            InAppEqualizerSheet(
                onDismiss = {
                    showEqualizerSheet = false
                }
            )
        }

        if (showListenTogetherSheet) {
            InPlayerListenTogetherSheet(
                onDismiss = {
                    showListenTogetherSheet = false
                }
            )
        }
}

@Composable
private fun PlayerMenuHeader(mediaMetadata: MediaMetadata) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Text(
                    text = mediaMetadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = joinByBullet(
                        mediaMetadata.artists.joinToString { it.name },
                        mediaMetadata.album?.title.orEmpty(),
                    ).ifBlank { makeTimeString(mediaMetadata.duration * 1000L) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlayerMenuActionTile(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    modifier: Modifier = Modifier.fillMaxWidth().height(76.dp),
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier

    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InPlayerListenTogetherSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState(initial = null)
    val session by ListenTogetherSync.session.collectAsState()
    val isHost by ListenTogetherSync.isHost.collectAsState()
    val syncMessage by ListenTogetherSync.message.collectAsState()
    val syncedDisplayName by ListenTogetherSync.displayName.collectAsState()
    val codeCopiedMessage = stringResource(R.string.session_code_copied)
    val listenTogetherCodeLabel = stringResource(R.string.listen_together_code)

    var displayName by rememberSaveable { mutableStateOf(syncedDisplayName) }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(syncedDisplayName) {
        displayName = syncedDisplayName
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {},
        title = {
            Text(
                text = stringResource(R.string.listen_together),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            ListenTogetherStatusCard(
                session = session,
                isHost = isHost,
                mediaMetadata = mediaMetadata,
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = {
                    displayName = it
                    ListenTogetherSync.setDisplayName(it)
                },
                label = { Text(stringResource(R.string.display_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    enabled = mediaMetadata != null,
                    onClick = {
                        ListenTogetherSync.setDisplayName(displayName)
                        ListenTogetherSync.createSession()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.create_session))
                }

                OutlinedButton(
                    enabled = session?.joinUrl?.isNotBlank() == true,
                    onClick = {
                        val activeSession = session ?: return@OutlinedButton
                        val shareText = "${activeSession.joinUrl}\n$listenTogetherCodeLabel: ${activeSession.code}"
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                },
                                null
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.share))
                }
            }

            OutlinedTextField(
                value = joinCode,
                onValueChange = { joinCode = it.uppercase() },
                label = { Text(stringResource(R.string.session_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = joinCode.isNotBlank(),
                onClick = {
                    ListenTogetherSync.setDisplayName(displayName)
                    ListenTogetherSync.joinSession(joinCode)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.join_session))
            }

            session?.let { activeSession ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(activeSession.code))
                            message = codeCopiedMessage
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.copy_session_code))
                    }
                    OutlinedButton(
                        onClick = {
                            ListenTogetherSync.leaveSession()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.leave_session))
                    }
                }

                PopupParticipantsSection(activeSession)
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            syncMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        }
    )
}

@Composable
private fun PopupParticipantsSection(session: ListenTogetherSession) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.session_users),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        session.participantList.forEach { participant ->
            Text(
                text = if (participant.isHost) {
                    stringResource(R.string.created_by_name, participant.name)
                } else {
                    participant.name
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ListenTogetherStatusCard(
    session: ListenTogetherSession?,
    isHost: Boolean,
    mediaMetadata: MediaMetadata?,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = when {
                    session == null -> stringResource(R.string.no_active_session)
                    isHost -> stringResource(R.string.hosting_session, session.code)
                    else -> stringResource(R.string.joined_session_with_code, session.code)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = mediaMetadata?.title ?: session?.state?.title ?: stringResource(R.string.play_song_first),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            session?.let {
                Text(
                    text = "${it.participants} listeners",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InAppEqualizerSheet(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val equalizerState by playerConnection.service.equalizerState.collectAsState()

    LaunchedEffect(Unit) {
        playerConnection.service.ensureEqualizer()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(34.dp)
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f),
                        shape = RoundedCornerShape(50)
                    )
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.equalizer),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (equalizerState.isAvailable) {
                            stringResource(R.string.equalizer_in_app_description)
                        } else {
                            stringResource(R.string.equalizer_unavailable)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = equalizerState.enabled,
                    enabled = equalizerState.isAvailable,
                    onCheckedChange = playerConnection.service::setEqualizerEnabled,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (equalizerState.isAvailable) {
                AudioEffectPresets(
                    onPresetSelected = { preset ->
                        playerConnection.service.setEqualizerEnabled(true)
                        preset.levels.forEachIndexed { index, level ->
                            if (index in equalizerState.bandLevels.indices) {
                                playerConnection.service.setEqualizerBandLevel(index, level.toShort())
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                equalizerState.bandLevels.forEachIndexed { index, level ->
                    EqualizerBandSlider(
                        label = formatEqualizerFrequency(equalizerState.centerFrequencies.getOrNull(index)),
                        level = level,
                        minLevel = equalizerState.minBandLevel,
                        maxLevel = equalizerState.maxBandLevel,
                        enabled = equalizerState.enabled,
                        onLevelChange = { newLevel ->
                            playerConnection.service.setEqualizerBandLevel(index, newLevel)
                        }
                    )
                }

                TextButton(
                    onClick = playerConnection.service::resetEqualizer,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.reset))
                }
            }
        }
    }
}

@Composable
private fun AudioEffectPresets(onPresetSelected: (AudioEffectPreset) -> Unit) {
    Text(
        text = stringResource(R.string.audio_effects),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(176.dp)
    ) {
        items(AudioEffectPreset.presets.size) { index ->
            val preset = AudioEffectPreset.presets[index]
            Surface(
                onClick = { onPresetSelected(preset) },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private data class AudioEffectPreset(
    val name: String,
    val levels: List<Int>,
) {
    companion object {
        val presets =
            listOf(
                AudioEffectPreset("Flat", listOf(0, 0, 0, 0, 0)),
                AudioEffectPreset("Bass+", listOf(850, 650, 150, -100, -200)),
                AudioEffectPreset("Treble+", listOf(-200, -100, 150, 650, 850)),
                AudioEffectPreset("8D Space", listOf(450, -250, 350, -150, 700)),
                AudioEffectPreset("Vocal", listOf(-300, 100, 850, 350, -150)),
                AudioEffectPreset("Rock", listOf(650, 250, -250, 350, 700)),
                AudioEffectPreset("Pop", listOf(-100, 350, 650, 300, -100)),
                AudioEffectPreset("Dance", listOf(750, 500, 0, 350, 600)),
                AudioEffectPreset("Electronic", listOf(700, 250, -150, 500, 850)),
                AudioEffectPreset("Jazz", listOf(350, 150, 250, 450, 300)),
                AudioEffectPreset("Classical", listOf(300, 200, 150, 350, 550)),
                AudioEffectPreset("Night", listOf(-350, -150, 150, 250, 100)),
            )
    }
}

@Composable
private fun EqualizerBandSlider(
    label: String,
    level: Short,
    minLevel: Short,
    maxLevel: Short,
    enabled: Boolean,
    onLevelChange: (Short) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${level / 100} dB",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = level.toFloat(),
            onValueChange = { onLevelChange(it.toInt().toShort()) },
            valueRange = minLevel.toFloat()..maxLevel.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

private fun formatEqualizerFrequency(frequencyMilliHz: Int?): String {
    val hz = (frequencyMilliHz ?: 0) / 1000
    return if (hz >= 1000) {
        "${hz / 1000}kHz"
    } else {
        "${hz}Hz"
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = R.drawable.speed,
                    currentValue = tempo,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                    onValueUpdate = {
                        tempo = it
                        updatePlaybackParameters()
                    },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
            )
        }
    }
}
