package com.darkxvenom.airbeats.ui.menu

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.darkxvenom.airbeats.LocalRingtoneViewModel
import com.darkxvenom.airbeats.ui.player.DeviceSelectionBottomSheet
import com.darkxvenom.airbeats.ui.player.getAvailableDevices
import com.darkxvenom.airbeats.ui.player.getActiveDevice
import com.darkxvenom.airbeats.ui.player.isBluetoothOutput
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlin.math.roundToInt
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.darkxvenom.airbeats.ui.component.createCastRouteButton
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.ui.component.LocalBackdrop
import com.darkxvenom.airbeats.ui.component.drawBackdropCustomShape
import com.darkxvenom.airbeats.constants.LiquidGlassKey
import com.darkxvenom.airbeats.constants.EqualizerPresetKey
import com.darkxvenom.airbeats.playback.DeviceCodecs
import com.darkxvenom.airbeats.utils.rememberPreference
import androidx.compose.animation.core.Animatable
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
import com.darkxvenom.airbeats.ui.component.DownloadQualityDialog
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
import kotlinx.coroutines.withContext
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
    val isExcluded by database.isRecommendationExcluded(mediaMetadata.id).collectAsState(initial = false)
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
    var showSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    var showSnippetStudioDialog by rememberSaveable { mutableStateOf(false) }

    if (showSnippetStudioDialog) {
        com.darkxvenom.airbeats.ui.component.SongSnippetStudioDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showSnippetStudioDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        com.darkxvenom.airbeats.ui.player.SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = { minutes ->
                playerConnection.service.sleepTimer.start(minutes)
                showSleepTimerDialog = false
                onDismiss()
            },
            onEndOfSong = {
                playerConnection.service.sleepTimer.start(-1)
                showSleepTimerDialog = false
                onDismiss()
            },
        )
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    var isMuted by remember { mutableStateOf(false) }
    var previousVolume by remember { mutableFloatStateOf(playerVolume.value) }
    var showEqualizerSheet by rememberSaveable { mutableStateOf(false) }
    var showAudioFxSheet by rememberSaveable { mutableStateOf(false) }
    var showDolbyAtmosSheet by rememberSaveable { mutableStateOf(false) }
    var showEightDAudioSheet by rememberSaveable { mutableStateOf(false) }
    var showListenTogetherSheet by rememberSaveable { mutableStateOf(false) }
    var showOutputDeviceSheet by rememberSaveable { mutableStateOf(false) }

    val availableAudioDevices by playerConnection.service.availableAudioDevices.collectAsState()
    val preferredAudioDevice by playerConnection.service.preferredAudioDevice.collectAsState()

    val fallbackDevices = remember { getAvailableDevices(context) }
    val displayDevices = if (availableAudioDevices.isNotEmpty()) availableAudioDevices else fallbackDevices
    val activeDevice = preferredAudioDevice ?: remember(displayDevices) { getActiveDevice(displayDevices) }
    val isBluetooth = activeDevice?.let { with(it) { isBluetoothOutput() } } ?: false
    val isCasting by playerConnection.service.isCasting.collectAsState()
    val castDeviceName by playerConnection.service.castDeviceName.collectAsState()

    val hasBluetoothPermission = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    var bluetoothPermissionGranted by remember { mutableStateOf(hasBluetoothPermission) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        bluetoothPermissionGranted = isGranted
        playerConnection.service.refreshAudioOutputDevices()
        showOutputDeviceSheet = true
    }

    val ringtoneViewModel = LocalRingtoneViewModel.current

    if (showOutputDeviceSheet) {
        DeviceSelectionBottomSheet(
            onDismiss = { showOutputDeviceSheet = false },
            availableDevices = displayDevices,
            activeDevice = activeDevice,
            preferredDevice = preferredAudioDevice,
            onSelectDevice = { device ->
                playerConnection.service.setPreferredOutputDevice(device)
            },
            hasBluetoothPermission = bluetoothPermissionGranted,
            onRequestBluetoothPermission = {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
    ) {
        item {
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

        }
        item {
                        var showQualityDialog by remember { mutableStateOf(false) }
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
                                    onDismiss()
                                } else {
                                    showQualityDialog = true
                                }
                            }
                        }
                        if (showQualityDialog) {
                            DownloadQualityDialog(
                                onDismiss = { showQualityDialog = false },
                                onQualitySelected = {
                                    showQualityDialog = false
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
                                    onDismiss()
                                },
                            )
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
                                com.darkxvenom.airbeats.utils.SaveToStorageUtil.saveToMusicFolderAsync(
                                    context = context,
                                    mediaMetadata = mediaMetadata,
                                )
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
                                    com.darkxvenom.airbeats.utils.SaveToStorageUtil.saveToMusicFolderAsync(
                                        context = context,
                                        mediaMetadata = mediaMetadata,
                                    )
                                    onDismiss()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            }
                        )
                    }

                    item {
                        val isCasting by (playerConnection.service.isCasting.collectAsState())
                        val castDeviceName by (playerConnection.service.castDeviceName.collectAsState())
                        val mediaRouteButtonRef = remember { mutableStateOf<MediaRouteButton?>(null) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            AndroidView(
                                factory = { ctx ->
                                    createCastRouteButton(ctx)?.also { mediaRouteButtonRef.value = it }
                                        ?: android.widget.FrameLayout(ctx)
                                },
                                modifier = Modifier.size(1.dp).alpha(0f)
                            )

                            androidx.compose.material3.ListItem(
                                headlineContent = {
                                    Text(if (isCasting && !castDeviceName.isNullOrBlank()) "Casting to $castDeviceName" else "Google Cast / Chromecast")
                                },
                                supportingContent = {
                                    Text(if (isCasting) "Tap to manage or disconnect" else "Stream audio to Chromecast or Smart TV")
                                },
                                leadingContent = {
                                    Icon(
                                        painter = painterResource(if (isCasting) R.drawable.ic_cast_connected else R.drawable.ic_cast),
                                        contentDescription = stringResource(R.string.ui_a11y_cast),
                                        tint = if (isCasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable {
                                    runCatching {
                                        mediaRouteButtonRef.value?.performClick()
                                    }.onFailure { e ->
                                        timber.log.Timber.w(e, "Failed to launch MediaRouteButton chooser")
                                    }
                                }
                            )
                        }
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.sleep_timer)) },
                            leadingContent = { Icon(painterResource(R.drawable.schedule), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showSleepTimerDialog = true },
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
                                            com.darkxvenom.airbeats.utils.RemoteConfigManager.getSongShareUrl(mediaMetadata.id)
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
                            headlineContent = {
                                Text(
                                    stringResource(
                                        if (isExcluded) R.string.allow_recommendations
                                        else R.string.dont_recommend_again
                                    )
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painterResource(R.drawable.block),
                                    contentDescription = null
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                val wasExcluded = isExcluded
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (wasExcluded) {
                                        database.removeRecommendationExclusion(mediaMetadata.id)
                                    } else {
                                        database.insert(
                                            com.darkxvenom.airbeats.db.entities.RecommendationExclusionEntity(
                                                songId = mediaMetadata.id,
                                                title = mediaMetadata.title,
                                                artist = mediaMetadata.artists.joinToString { it.name },
                                                thumbnailUrl = mediaMetadata.thumbnailUrl
                                            )
                                        )
                                        withContext(Dispatchers.Main) {
                                            playerConnection.removeSongFromQueue(mediaMetadata.id)
                                        }
                                    }
                                }
                                if (!wasExcluded) {
                                    onDismiss()
                                }
                                Toast.makeText(
                                    context,
                                    if (wasExcluded) R.string.recommendation_restored else R.string.dont_recommend_applied,
                                    Toast.LENGTH_SHORT
                                ).show()
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
                        val deviceName = when {
                            isCasting -> castDeviceName ?: "Cast device"
                            activeDevice != null -> activeDevice.productName.toString()
                            else -> "Phone Speaker"
                        }
                        val deviceIcon = when {
                            isCasting -> R.drawable.ic_cast_connected
                            isBluetooth -> R.drawable.ic_bluetooth
                            else -> R.drawable.airplay
                        }
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.output_audio_device)) },
                            supportingContent = { Text(deviceName) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(deviceIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showOutputDeviceSheet = true
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.set_as_ringtone)) },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.notification),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                if (ringtoneViewModel.hasSettingsPermission(context)) {
                                    ringtoneViewModel.showTrimmer(
                                        mediaMetadata.id,
                                        mediaMetadata.title,
                                        mediaMetadata.artists.joinToString { it.name },
                                        mediaMetadata.duration.toLong()
                                    )
                                } else {
                                    ringtoneViewModel.requestSettingsPermission(context)
                                }
                                onDismiss()
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.ringtone_studio)) },
                            supportingContent = { Text(stringResource(R.string.ringtone_studio_desc)) },
                            leadingContent = { Icon(painterResource(R.drawable.content_cut), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { showSnippetStudioDialog = true }
                        )
                    }

                    item {
                        val service = playerConnection.service
                        val audioBoostEnabled by service.audioBoostEnabled.collectAsState()
                        val audioBoostPercent by service.audioBoostPercent.collectAsState()

                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.ui_remaining_audio_fx_dj_studio)) },
                            supportingContent = {
                                Text(
                                    text = if (audioBoostEnabled) "${audioBoostPercent}% Boost Active • Tap for DJ Studio" else "Tap to open DJ Studio console",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (audioBoostEnabled) Color(0xFFFF2A6D) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_dj_console),
                                    contentDescription = null,
                                    tint = if (audioBoostEnabled) Color(0xFFFF2A6D) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = audioBoostEnabled,
                                    onCheckedChange = { enabled ->
                                        service.setAudioBoostEnabled(enabled)
                                        if (enabled && audioBoostPercent <= 100) {
                                            service.setAudioBoostPercent(150)
                                        }
                                    },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF2A6D)
                                    )
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showAudioFxSheet = true
                            }
                        )
                    }

                    item {
                        val dolbyAtmosEnabled by playerConnection?.service?.dolbyAtmosEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.dolby_atmos)) },
                            supportingContent = {
                                Text(
                                    text = if (dolbyAtmosEnabled) stringResource(R.string.enabled) else stringResource(R.string.disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (dolbyAtmosEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_dolby_atmos),
                                    contentDescription = null,
                                    tint = if (dolbyAtmosEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = dolbyAtmosEnabled,
                                    onCheckedChange = { playerConnection?.service?.setDolbyAtmosEnabled(it) }
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showDolbyAtmosSheet = true
                            }
                        )
                    }

                    item {
                        val eightDAudioEnabled by playerConnection.service.eightDAudioEnabled.collectAsState()
                        val eightDAudioLevel by playerConnection.service.eightDAudioLevel.collectAsState()
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.ui_remaining_8d_audio)) },
                            supportingContent = {
                                Text(
                                    text = if (eightDAudioEnabled) "${eightDAudioLevel}D Spatial Orbit Active" else stringResource(R.string.disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (eightDAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.graphic_eq),
                                    contentDescription = null,
                                    tint = if (eightDAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = eightDAudioEnabled,
                                    onCheckedChange = { playerConnection.service.setEightDAudioEnabled(it) }
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showEightDAudioSheet = true
                            }
                        )
                    }

                    item {
                        val spatialAudioEnabled by playerConnection?.service?.spatialAudioEnabled?.collectAsState() ?: remember { mutableStateOf(false) }
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.spatial_audio)) },
                            supportingContent = {
                                Text(
                                    text = if (spatialAudioEnabled) stringResource(R.string.enabled) else stringResource(R.string.disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (spatialAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.graphic_eq),
                                    contentDescription = null,
                                    tint = if (spatialAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = spatialAudioEnabled,
                                    onCheckedChange = { playerConnection?.service?.setSpatialAudioEnabled(it) }
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }

                    item {
                        val automixEnabled by playerConnection?.service?.automixEnabled?.collectAsState() ?: remember { mutableStateOf(false) }
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.automix)) },
                            supportingContent = {
                                Text(
                                    text = if (automixEnabled) stringResource(R.string.enabled) else stringResource(R.string.disabled),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (automixEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingContent = {
                                Icon(
                                    painter = painterResource(R.drawable.auto_awesome),
                                    contentDescription = null,
                                    tint = if (automixEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = automixEnabled,
                                    onCheckedChange = { playerConnection?.service?.setAutomixEnabled(it) }
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.listen_together)) },
                            leadingContent = { Icon(painterResource(R.drawable.group), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                navController.navigate("listen_together")
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
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

        if (showEqualizerSheet) {
            InAppEqualizerSheet(
                onDismiss = {
                    showEqualizerSheet = false
                }
            )
        }

        if (showAudioFxSheet) {
            InAppAudioFxSheet(
                onDismiss = {
                    showAudioFxSheet = false
                }
            )
        }

        if (showDolbyAtmosSheet) {
            InAppDolbyAtmosSheet(
                onDismiss = {
                    showDolbyAtmosSheet = false
                }
            )
        }

        if (showEightDAudioSheet) {
            InAppEightDAudioSheet(
                onDismiss = {
                    showEightDAudioSheet = false
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
    val bypassAllAudioEffects by playerConnection.service.bypassAllAudioEffects.collectAsState()
    val (equalizerPreset, onEqualizerPresetChange) = rememberPreference(EqualizerPresetKey, "Flat")
    val (enableLiquidGlass) = rememberPreference(LiquidGlassKey, false)
    val isFrosted = isFrostedGlassUiEnabled()
    val backdrop = LocalBackdrop.current
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    LaunchedEffect(Unit) {
        playerConnection.service.ensureEqualizer()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (enableLiquidGlass && !isFrosted && backdrop != null) {
            Color.Transparent
        } else if (isFrosted) {
            if (isDark) Color(0xFF141414).copy(alpha = 0.88f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = sheetShape,
        modifier = Modifier.then(
            if (enableLiquidGlass && !isFrosted && backdrop != null) {
                Modifier.drawBackdropCustomShape(backdrop = backdrop, layer = layer, luminanceAnimation = luminanceAnimation.value, shape = sheetShape)
            } else if (isFrosted) {
                Modifier.border(
                    BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                    sheetShape
                )
            } else {
                Modifier
            }
        ),
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
            if (bypassAllAudioEffects) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.auto_awesome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.ui_sweep_pure_audio_bypass_is_active_equalizer_is_locked_for_bit_exact_sou),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

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
                    checked = if (bypassAllAudioEffects) false else equalizerState.enabled,
                    enabled = equalizerState.isAvailable && !bypassAllAudioEffects,
                    onCheckedChange = playerConnection.service::setEqualizerEnabled,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (equalizerState.isAvailable && !bypassAllAudioEffects) {
                AudioEffectPresets(
                    selectedPreset = equalizerPreset,
                    onPresetSelected = { preset ->
                        playerConnection.service.setEqualizerEnabled(true)
                        onEqualizerPresetChange(preset.name)
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
                            onEqualizerPresetChange("Custom")
                            playerConnection.service.setEqualizerBandLevel(index, newLevel)
                        }
                    )
                }

                TextButton(
                    onClick = {
                        onEqualizerPresetChange("Flat")
                        playerConnection.service.resetEqualizer()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.reset))
                }
            }
        }
    }
}

@Composable
private fun AudioEffectPresets(
    selectedPreset: String?,
    onPresetSelected: (AudioEffectPreset) -> Unit,
) {
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
            val isSelected = preset.name.equals(selectedPreset, ignoreCase = true)
            Surface(
                onClick = { onPresetSelected(preset) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
                },
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    }
                ),
                modifier = Modifier.height(50.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

internal data class AudioEffectPreset(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InAppDolbyAtmosSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val dolbyAtmosEnabled by playerConnection.service.dolbyAtmosEnabled.collectAsState()
    val isTrackDolbyAtmos by playerConnection.service.isTrackDolbyAtmos.collectAsState()
    val spatialAudioEnabled by playerConnection.service.spatialAudioEnabled.collectAsState()
    val bypassAllAudioEffects by playerConnection.service.bypassAllAudioEffects.collectAsState()
    val dolbyAtmosSupported = remember { DeviceCodecs.playsDolbyAtmos }
    val (enableLiquidGlass) = rememberPreference(LiquidGlassKey, false)
    val isFrosted = isFrostedGlassUiEnabled()
    val backdrop = LocalBackdrop.current
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (enableLiquidGlass && !isFrosted && backdrop != null) {
            Color.Transparent
        } else if (isFrosted) {
            if (isDark) Color(0xFF141414).copy(alpha = 0.88f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = sheetShape,
        modifier = Modifier.then(
            if (enableLiquidGlass && !isFrosted && backdrop != null) {
                Modifier.drawBackdropCustomShape(backdrop = backdrop, layer = layer, luminanceAnimation = luminanceAnimation.value, shape = sheetShape)
            } else if (isFrosted) {
                Modifier.border(
                    BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                    sheetShape
                )
            } else {
                Modifier
            }
        ),
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
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
        ) {
            if (bypassAllAudioEffects) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.auto_awesome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.ui_sweep_pure_audio_bypass_is_active_dolby_atmos_spatial_virtualizer_are_l),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_dolby_atmos),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dolby_atmos),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (dolbyAtmosEnabled) {
                            if (isTrackDolbyAtmos) "Native Multichannel Dolby Stream Active" else "Spatial Virtual Surround Active"
                        } else {
                            stringResource(R.string.disabled)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = if (bypassAllAudioEffects) false else dolbyAtmosEnabled,
                    enabled = !bypassAllAudioEffects,
                    onCheckedChange = playerConnection.service::setDolbyAtmosEnabled,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.graphic_eq),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.ui_text_acoustic_spatial_processing),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.ui_sweep_expands_the_stereo_soundstage_into_a_multi_dimensional_listening_),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ui_sweep_hardware_e_ac_3_decoder),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (dolbyAtmosSupported) "Supported" else "Emulated",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dolbyAtmosSupported) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.ui_text_soundstage_enhancement),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (dolbyAtmosEnabled || spatialAudioEnabled) "2.5x Wide Stereo" else "Standard",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (dolbyAtmosEnabled || spatialAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.spatial_audio),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.spatial_audio_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = if (bypassAllAudioEffects) false else spatialAudioEnabled,
                            enabled = !bypassAllAudioEffects,
                            onCheckedChange = playerConnection.service::setSpatialAudioEnabled
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            androidx.compose.material3.Button(
                onClick = {
                    val opened = DeviceCodecs.openDolbyAtmosSettings(
                        context,
                        playerConnection.player.audioSessionId
                    )
                    if (!opened) {
                        android.widget.Toast.makeText(context, context.getString(R.string.no_dolby_atmos), android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.tune),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.dolby_atmos_system_panel))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InAppEightDAudioSheet(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val eightDAudioEnabled by playerConnection.service.eightDAudioEnabled.collectAsState()
    val eightDAudioLevel by playerConnection.service.eightDAudioLevel.collectAsState()
    val bypassAllAudioEffects by playerConnection.service.bypassAllAudioEffects.collectAsState()
    val (enableLiquidGlass) = rememberPreference(LiquidGlassKey, false)
    val isFrosted = isFrostedGlassUiEnabled()
    val backdrop = LocalBackdrop.current
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (enableLiquidGlass && !isFrosted && backdrop != null) {
            Color.Transparent
        } else if (isFrosted) {
            if (isDark) Color(0xFF141414).copy(alpha = 0.88f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = sheetShape,
        modifier = Modifier.then(
            if (enableLiquidGlass && !isFrosted && backdrop != null) {
                Modifier.drawBackdropCustomShape(backdrop = backdrop, layer = layer, luminanceAnimation = luminanceAnimation.value, shape = sheetShape)
            } else if (isFrosted) {
                Modifier.border(
                    BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                    sheetShape
                )
            } else {
                Modifier
            }
        ),
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
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
        ) {
            if (bypassAllAudioEffects) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.auto_awesome),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.ui_sweep_pure_audio_bypass_is_active_8d_audio_is_locked_for_bit_exact_soun),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Header Row with Icon, Title, and Switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.graphic_eq),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ui_remaining_8d_audio),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (eightDAudioEnabled) "${eightDAudioLevel}D Spatial Orbit Active" else stringResource(R.string.disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (eightDAudioEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = if (bypassAllAudioEffects) false else eightDAudioEnabled,
                    enabled = !bypassAllAudioEffects,
                    onCheckedChange = playerConnection.service::setEightDAudioEnabled,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Explanation & Info Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.discover_tune),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.ui_sweep_360_binaural_spatial_orbit),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.ui_sweep_music_and_vocals_orbit_360_through_your_head_using_acoustic_inter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Intensity row & badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.ui_text_effect_intensity),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        text = "${eightDAudioLevel}D",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = when {
                                    eightDAudioLevel <= 3 -> "Gentle & Slow Orbit (~25s per circle)"
                                    eightDAudioLevel <= 7 -> "Moderate Spatial Rotation (~14s per circle)"
                                    eightDAudioLevel == 8 -> "Classic 8D Orbit (~9s per circle)"
                                    eightDAudioLevel <= 12 -> "Fast Dynamic Orbit (~6s per circle)"
                                    else -> "Extreme Rapid 16D Orbit (~3.5s per circle)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1D to 16D Slider
                    Slider(
                        value = eightDAudioLevel.toFloat(),
                        onValueChange = { newLevel ->
                            val intLevel = newLevel.roundToInt().coerceIn(1, 16)
                            if (intLevel != eightDAudioLevel) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                playerConnection.service.setEightDAudioLevel(intLevel)
                            }
                        },
                        valueRange = 1f..16f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.ui_sweep_1d_subtle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Text(
                            text = stringResource(R.string.ui_sweep_8d_classic),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                        Text(
                            text = stringResource(R.string.ui_sweep_16d_extreme),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick preset chips (1D, 4D, 8D, 12D, 16D)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    1 to "1D",
                    4 to "4D",
                    8 to "8D",
                    12 to "12D",
                    16 to "16D"
                ).forEach { (presetLevel, presetLabel) ->
                    val isSelected = eightDAudioLevel == presetLevel
                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            playerConnection.service.setEightDAudioLevel(presetLevel)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = presetLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}


