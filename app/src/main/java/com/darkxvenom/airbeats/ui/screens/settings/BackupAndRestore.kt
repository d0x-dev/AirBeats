package com.darkxvenom.airbeats.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.extensions.tryOrNull
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.component.PreferenceEntry
import com.darkxvenom.airbeats.ui.component.SettingsGeneralCategory
import com.darkxvenom.airbeats.ui.component.SettingsPage
import com.darkxvenom.airbeats.ui.component.SwitchPreference
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.ui.component.settingsCardContainerColor
import com.darkxvenom.airbeats.ui.component.settingsCardBorder
import com.darkxvenom.airbeats.ui.menu.OnlinePlaylistAdder
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.ui.utils.formatFileSize
import com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel
import com.darkxvenom.airbeats.utils.AutoBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("LogNotTimber")
@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerCache = LocalPlayerConnection.current?.service?.playerCache

    // Statuses
    var showVisitorDataDialog by remember { mutableStateOf(false) }
    var showVisitorDataResetDialog by remember { mutableStateOf(false) }
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by remember { mutableStateOf(false) }
    var isProgressStarted by remember { mutableStateOf(false) }
    var progressPercentage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadStorageBackupState(context)
    }

    val isAutoBackupToStorage by viewModel.isAutoBackupToStorage.collectAsState()
    var showStorageRestoreConfirmDialog by remember { mutableStateOf(false) }


    // Cache stats
    var playerCacheSize by remember { mutableLongStateOf(tryOrNull { playerCache?.cacheSpace } ?: 0L) }
    var isClearing by remember { mutableStateOf(false) }

    val animatedPlayerCacheSize by animateFloatAsState(
        targetValue = if (playerCacheSize > 0) 1f else 0f,
        label = "playerCacheProgress"
    )

    // Update cache size
    LaunchedEffect(playerCache) {
        while (true) {
            delay(1000)
            playerCacheSize = tryOrNull { playerCache?.cacheSpace } ?: 0L
        }
    }

    // Launchers
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(context, uri)
            }
        }

    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(context, uri)
            }
        }

    val backupCacheLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null) {
                viewModel.backupCache(context, uri)
            }
        }

    val restoreCacheLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restoreCache(context, uri)
            }
        }

    val importPlaylistFromCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val result = viewModel.importPlaylistFromCsv(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)
            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }

    val importM3uLauncherOnline =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val result = viewModel.loadM3UOnline(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)
            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }

    SettingsPage(
        title = stringResource(R.string.backup_restore),
        navController = navController,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.ui_heading_device_storage_backup_documents_airbeats),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.ui_remaining_auto_backup_to_storage)) },
                        description = stringResource(R.string.ui_extra_automatically_save_a_complete_backup_to_documents_airbeats_on_every_),
                        icon = { Icon(painterResource(R.drawable.save_to_storage), null) },
                        checked = isAutoBackupToStorage,
                        onCheckedChange = { enabled ->
                            viewModel.setAutoBackupToStorage(context, enabled)
                        }
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.ui_remaining_backup_to_documents_airbeats_now)) },
                        icon = { Icon(painterResource(R.drawable.backup), null) },
                        description = stringResource(R.string.ui_extra_save_an_immediate_backup_file_to_documents_airbeats_airbeats_backup_),
                        onClick = {
                            viewModel.backupToStorageNow(context) { success ->
                                Toast.makeText(
                                    context,
                                    if (success) "Backup saved to Documents/AirBeats" else "Could not save backup to storage",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.ui_remaining_restore_from_documents_airbeats)) },
                        icon = { Icon(painterResource(R.drawable.restore), null) },
                        description = stringResource(R.string.ui_extra_restore_full_profile_playlists_and_settings_from_documents_airbeats),
                        onClick = {
                            val backupFile = AutoBackupManager.findStorageBackupFile(context)
                            if (backupFile != null && backupFile.exists() && backupFile.length() > 0L) {
                                showStorageRestoreConfirmDialog = true
                            } else {
                                Toast.makeText(context, "No backup file found in Documents/AirBeats", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.backup_restore),
            items = listOf(
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.backup)) },
                    icon = { Icon(painterResource(R.drawable.backup), null) },
                    description = stringResource(R.string.backup_description),
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        backupLauncher.launch(
                            "${context.getString(R.string.app_name)}_${
                                LocalDateTime.now().format(formatter)
                            }.backup"
                        )
                    }
                )},
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.restore)) },
                    icon = { Icon(painterResource(R.drawable.restore), null) },
                    description = stringResource(R.string.restore_description),
                    onClick = {
                        restoreLauncher.launch(arrayOf("application/octet-stream"))
                    }
                )},
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.backup_cached_songs)) },
                    icon = { Icon(painterResource(R.drawable.cached), null) },
                    description = stringResource(R.string.backup_cached_songs_desc),
                    onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        backupCacheLauncher.launch(
                            "AirBeats_Cache_${LocalDateTime.now().format(formatter)}.airbeatscache"
                        )
                    }
                )},
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.restore_cached_songs)) },
                    icon = { Icon(painterResource(R.drawable.restore), null) },
                    description = stringResource(R.string.restore_cached_songs_desc),
                    onClick = {
                        restoreCacheLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    }
                )}
            )
        )

        // VISITOR_DATA Card
        MinimalVisitorDataCard(
            playerCacheSize = playerCacheSize,
            progress = animatedPlayerCacheSize,
            isClearing = isClearing,
            onResetClick = { showVisitorDataResetDialog = true },
            onInfoClick = { showVisitorDataDialog = true }
        )
    }


    // Dialogs
    if (showVisitorDataDialog) {
        MinimalInfoDialog(
            icon = painterResource(R.drawable.info),
            title = stringResource(R.string.visitor_data_info_title),
            message = stringResource(R.string.visitor_data_info_intro) + "\n\n" +
                    stringResource(R.string.visitor_data_info_problems) + "\n\n" +
                    stringResource(R.string.visitor_data_info_solution),
            onDismiss = { showVisitorDataDialog = false }
        )
    }

    if (showVisitorDataResetDialog) {
        MinimalConfirmDialog(
            icon = painterResource(R.drawable.replay),
            title = stringResource(R.string.visitor_data_reset_title),
            message = stringResource(R.string.visitor_data_reset_message),
            confirmText = stringResource(R.string.visitor_data_reset_confirm),
            onConfirm = {
                isClearing = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        // Clear song cache
                        playerCache?.keys?.toList()?.forEach { key ->
                            tryOrNull { playerCache.removeResource(key) }
                        }

                        // Reset VISITOR_DATA
                        viewModel.resetVisitorData(context)

                        delay(500) // Short delay to ensure completion

                        withContext(Dispatchers.Main) {
                            playerCacheSize = 0L
                            isClearing = false
                            showVisitorDataResetDialog = false
                        }
                    } catch (e: Exception) {
                        Log.e("BackupRestore", "Error when resetting VISITOR_DATA", e)
                        withContext(Dispatchers.Main) {
                            isClearing = false
                            showVisitorDataResetDialog = false
                        }
                    }
                }
            },
            onDismiss = { showVisitorDataResetDialog = false }
        )
    }

    OnlinePlaylistAdder(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        initialTextFieldValue = importedTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { newVal -> isProgressStarted = newVal },
        onPercentageChange = { newPercentage -> progressPercentage = newPercentage }
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    if (isProgressStarted) {
        MinimalLoadingOverlay(progress = progressPercentage)
    }

    if (showStorageRestoreConfirmDialog) {
        MinimalConfirmDialog(
            icon = painterResource(R.drawable.restore),
            title = stringResource(R.string.ui_heading_restore_from_documents_airbeats),
            message = "This will restore your complete database, accounts, playlists, and preferences from Documents/AirBeats and reboot the app.",
            confirmText = stringResource(R.string.backup_restore_action),
            onConfirm = {
                showStorageRestoreConfirmDialog = false
                viewModel.restoreFromStorageNow(context) { success ->
                    if (!success) {
                        Toast.makeText(context, "Failed to restore from storage", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showStorageRestoreConfirmDialog = false }
        )
    }
}


@Composable
private fun MinimalVisitorDataCard(
    playerCacheSize: Long,
    progress: Float,
    isClearing: Boolean,
    onResetClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFrosted) settingsCardContainerColor(true) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = settingsCardBorder(isFrosted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.replay),
                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.visitor_data_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = stringResource(R.string.visitor_data_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Cache indicator
            if (playerCacheSize > 0 || isClearing) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { if (isClearing) 0f else progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isClearing) {
                                stringResource(R.string.cache_clearing)
                            } else {
                                stringResource(R.string.song_cache)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatFileSize(playerCacheSize),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onInfoClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isClearing
                ) {
                    Icon(
                        painter = painterResource(R.drawable.help),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.info_button), style = MaterialTheme.typography.labelLarge)
                }

                FilledTonalButton(
                    onClick = onResetClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isClearing
                ) {
                    if (isClearing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.replay),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.reset_button), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}



@Composable
private fun MinimalLoadingOverlay(progress: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(64.dp),
                strokeWidth = 4.dp
            )
            Text(
                text = "$progress%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.processing_songs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MinimalInfoDialog(
    icon: Painter,
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.understood))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun MinimalConfirmDialog(
    icon: Painter,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}



