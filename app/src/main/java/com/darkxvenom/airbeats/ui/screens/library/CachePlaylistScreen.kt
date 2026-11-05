package com.darkxvenom.airbeats.ui.screens.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.darkxvenom.airbeats.constants.AppBarHeight
import com.darkxvenom.airbeats.utils.makeTimeString
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import com.darkxvenom.airbeats.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AlbumThumbnailSize
import com.darkxvenom.airbeats.constants.ThumbnailCornerRadius
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.extensions.tryOrNull
import com.darkxvenom.airbeats.playback.queues.ListQueue
import com.darkxvenom.airbeats.ui.component.EmptyPlaceholder
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.SongListItem
import com.darkxvenom.airbeats.ui.menu.SelectionSongMenu
import com.darkxvenom.airbeats.ui.menu.SongMenu
import com.darkxvenom.airbeats.ui.utils.ItemWrapper
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel
import com.darkxvenom.airbeats.viewmodels.HistoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HistoryViewModel = hiltViewModel(),
    backupViewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    val cacheSongsQueueTitle = stringResource(R.string.ui_heading_cache_songs)

    val context = LocalContext.current
    val backupCacheLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null) {
                backupViewModel.backupCache(context, uri)
            }
        }
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val events by viewModel.events.collectAsState()
    val dbSongs by viewModel.database.allSongs().collectAsState(initial = emptyList())

    val service = LocalPlayerConnection.current?.service
    val playerCache = service?.playerCache
    val downloadCache = service?.downloadCache

    val restoredCacheIds = remember {
        runCatching {
            val file = context.filesDir.resolve("restored_cache_ids.json")
            if (file.exists()) {
                val json = org.json.JSONArray(file.readText())
                val set = mutableSetOf<String>()
                for (i in 0 until json.length()) {
                    set.add(json.getString(i))
                }
                set
            } else emptySet()
        }.getOrDefault(emptySet())
    }

    var cachedSongIds by remember {
        mutableStateOf(
            buildSet {
                addAll(restoredCacheIds)
                tryOrNull { playerCache?.keys }?.mapNotNullTo(this) { it.toString() }
                tryOrNull { downloadCache?.keys }?.mapNotNullTo(this) { it.toString() }
            }
        )
    }

    LaunchedEffect(playerCache, downloadCache) {
        while (isActive) {
            val keys = mutableSetOf<String>()
            keys.addAll(restoredCacheIds)
            tryOrNull { playerCache?.keys }?.mapNotNullTo(keys) { it.toString() }
            tryOrNull { downloadCache?.keys }?.mapNotNullTo(keys) { it.toString() }
            if (keys != cachedSongIds) {
                cachedSongIds = keys.toSet()
            }
            delay(1000)
        }
    }

    val allSongs = remember(events, dbSongs, cachedSongIds) {
        val historySongs = events.values.flatten().map { it.song }
        (historySongs + dbSongs)
            .distinctBy { it.id }
            .filter { it.id in cachedSongIds }
    }

    val wrappedSongs = remember(allSongs) {
        mutableStateListOf<ItemWrapper<Song>>().apply {
            addAll(allSongs.map { ItemWrapper(it) })
        }
    }

    var selection by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val filteredSongs = remember(wrappedSongs, query) {
        if (query.text.isEmpty()) wrappedSongs
        else wrappedSongs.filter { wrapper ->
            val song = wrapper.item
            song.title.contains(query.text, true) ||
                    song.artists.any { it.name.contains(query.text, true) }
        }
    }

    val state = rememberLazyListState()

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(filteredSongs.firstOrNull()?.item?.thumbnailUrl) {
        val thumbnailUrl = filteredSongs.firstOrNull()?.item?.thumbnailUrl
        if (thumbnailUrl != null) {
            withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE)
                    .allowHardware(false)
                    .build()

                val result = runCatching {
                    context.imageLoader.execute(request)
                }.getOrNull()

                if (result != null && result.drawable != null) {
                    val rawBitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        ?: result.drawable?.toBitmap()
                    val bitmap = rawBitmap?.let {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && it.config == android.graphics.Bitmap.Config.HARDWARE) {
                            it.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                        } else it
                    }
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()

                        val extractedColors = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = fallbackColor
                        )
                        withContext(Dispatchers.Main) {
                            gradientColors = extractedColors
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val gradientAlpha by remember {
        derivedStateOf {
            if (state.firstVisibleItemIndex == 0) {
                val offset = state.firstVisibleItemScrollOffset
                (1f - (offset / 800f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.55f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {
                        val width = size.width
                        val height = size.height

                        if (gradientColors.size >= 3) {
                            val c0 = gradientColors[0]
                            val c1 = gradientColors[1]
                            val c2 = gradientColors[2]
                            val c3 = gradientColors.getOrElse(3) { c0 }
                            val c4 = gradientColors.getOrElse(4) { c1 }
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c0.copy(alpha = gradientAlpha * 0.75f),
                                        c0.copy(alpha = gradientAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.15f),
                                    radius = width * 0.8f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c1.copy(alpha = gradientAlpha * 0.55f),
                                        c1.copy(alpha = gradientAlpha * 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.1f, height * 0.4f),
                                    radius = width * 0.6f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c2.copy(alpha = gradientAlpha * 0.5f),
                                        c2.copy(alpha = gradientAlpha * 0.25f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.9f, height * 0.35f),
                                    radius = width * 0.55f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c3.copy(alpha = gradientAlpha * 0.35f),
                                        c3.copy(alpha = gradientAlpha * 0.18f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.25f, height * 0.65f),
                                    radius = width * 0.75f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c4.copy(alpha = gradientAlpha * 0.3f),
                                        c4.copy(alpha = gradientAlpha * 0.15f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.55f, height * 0.85f),
                                    radius = width * 0.9f
                                )
                            )
                        } else if (gradientColors.isNotEmpty()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.7f),
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.35f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.25f),
                                    radius = width * 0.85f
                                )
                            )
                        }
                    }
            )
        }
        LazyColumn(
            state = state,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (filteredSongs.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.music_note,
                        text = stringResource(R.string.playlist_is_empty)
                    )
                }
            } else {
                if (!isSearching) {
                    item {
                        val thumbnails = remember(filteredSongs) {
                            filteredSongs.mapNotNull { it.item.thumbnailUrl }.distinct().take(4)
                        }
                        val cacheLength = remember(filteredSongs) {
                            filteredSongs.sumOf { it.item.duration }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding() + AppBarHeight),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp, bottom = 20.dp)
                            ) {
                                if (thumbnails.size == 1) {
                                    Surface(
                                        modifier = Modifier
                                            .size(240.dp)
                                            .shadow(
                                                elevation = 24.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                spotColor = gradientColors.getOrNull(0)?.copy(alpha = 0.5f)
                                                    ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        AsyncImage(
                                            model = thumbnails[0],
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else if (thumbnails.size > 1) {
                                    Surface(
                                        modifier = Modifier
                                            .size(240.dp)
                                            .shadow(
                                                elevation = 24.dp,
                                                shape = RoundedCornerShape(16.dp),
                                                spotColor = gradientColors.getOrNull(0)?.copy(alpha = 0.5f)
                                                    ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            ),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            listOf(
                                                Alignment.TopStart,
                                                Alignment.TopEnd,
                                                Alignment.BottomStart,
                                                Alignment.BottomEnd,
                                            ).forEachIndexed { index, alignment ->
                                                AsyncImage(
                                                    model = thumbnails.getOrNull(index) ?: thumbnails[0],
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .align(alignment)
                                                        .size(120.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .size(240.dp)
                                            .shadow(
                                                elevation = 16.dp,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.cached),
                                                contentDescription = null,
                                                modifier = Modifier.size(80.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = stringResource(R.string.cached_playlist),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetadataChip(
                                    icon = R.drawable.music_note,
                                    text = pluralStringResource(
                                        id = R.plurals.n_song,
                                        count = filteredSongs.size,
                                        filteredSongs.size
                                    )
                                )

                                if (cacheLength > 0) {
                                    MetadataChip(
                                        icon = R.drawable.schedule,
                                        text = makeTimeString(cacheLength * 1000L)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = cacheSongsQueueTitle,
                                                items = filteredSongs.map { it.item.toMediaItem() },
                                            )
                                        )
                                    },
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.play))
                                }

                                OutlinedButton(
                                    onClick = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = cacheSongsQueueTitle,
                                                items = filteredSongs.shuffled()
                                                    .map { it.item.toMediaItem() },
                                            )
                                        )
                                    },
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )
                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.shuffle))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                itemsIndexed(
                    filteredSongs,
                    key = { _, song -> song.item.id }) { index, songWrapper ->
                    SongListItem(
                        song = songWrapper.item,
                        isActive = songWrapper.item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        isSelected = songWrapper.isSelected && selection,
                        showInLibraryIcon = true,
                        trailingContent = {
                            IconButton(onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = songWrapper.item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!selection) {
                                        if (songWrapper.item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = cacheSongsQueueTitle,
                                                    items = filteredSongs.map { it.item.toMediaItem() },
                                                    startIndex = index
                                                )
                                            )
                                        }
                                    } else {
                                        songWrapper.isSelected = !songWrapper.isSelected
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!selection) {
                                        selection = true
                                        wrappedSongs.forEach { it.isSelected = false }
                                        songWrapper.isSelected = true
                                    }
                                }
                            )
                            .animateItem()
                    )
                }
            }
        }

        TopAppBar(
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    isSearching -> {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }

                    else -> {
                        Text(
                            stringResource(R.string.cached_playlist),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    when {
                        isSearching -> {
                            isSearching = false
                            query = TextFieldValue()
                            focusManager.clearFocus()
                        }

                        selection -> {
                            selection = false
                        }

                        else -> {
                            navController.navigateUp()
                        }
                    }
                }, onLongClick = {
                    if (!isSearching && !selection) {
                        navController.backToMain()
                    }
                }) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    IconButton(onClick = {
                        if (count == wrappedSongs.size) {
                            wrappedSongs.forEach { it.isSelected = false }
                        } else {
                            wrappedSongs.forEach { it.isSelected = true }
                        }
                    }) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }

                    IconButton(onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = wrappedSongs.filter { it.isSelected }
                                    .map { it.item },
                                onDismiss = menuState::dismiss,
                                clearAction = { selection = false }
                            )
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    IconButton(onClick = {
                        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        backupCacheLauncher.launch(
                            "AirBeats_Cache_${LocalDateTime.now().format(formatter)}.airbeatscache"
                        )
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.backup),
                            contentDescription = stringResource(R.string.backup_cached_songs)
                        )
                    }
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun MetadataChip(
    icon: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
