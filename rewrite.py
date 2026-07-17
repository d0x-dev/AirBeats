import re

with open('app/src/main/java/com/darkxvenom/airbeats/ui/menu/PlayerMenu.kt', 'r', encoding='utf-8') as f:
    content = f.read()

start_pattern = r'                LazyVerticalGrid\(\s*columns = GridCells.Fixed\(3\),[\s\S]*?userScrollEnabled = false\s*\)\s*\{'
match = re.search(start_pattern, content)
if not match:
    print('Pattern not found')
    exit(1)

start_idx = match.start()
brace_count = 0
end_idx = -1
in_block = False
for i in range(start_idx, len(content)):
    if content[i] == '{':
        brace_count += 1
        in_block = True
    elif content[i] == '}':
        brace_count -= 1
    if in_block and brace_count == 0:
        end_idx = i
        break

old_block = content[start_idx:end_idx+1]

new_block = """                LazyColumn(
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
                            headlineContent = { Text(stringResource(R.string.sleep_timer)) },
                            leadingContent = { Icon(painterResource(R.drawable.sleep), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showSleepTimerDialog = true
                            }
                        )
                    }

                    item {
                        androidx.compose.material3.ListItem(
                            headlineContent = { Text(stringResource(R.string.equalizer)) },
                            leadingContent = { Icon(painterResource(R.drawable.equalizer), contentDescription = null) },
                            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                showEqualizerDialog = true
                            }
                        )
                    }
                }"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/darkxvenom/airbeats/ui/menu/PlayerMenu.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print('Successfully rewrote PlayerMenu.kt grid block')
