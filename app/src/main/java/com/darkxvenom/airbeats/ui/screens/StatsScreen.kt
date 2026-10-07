package com.darkxvenom.airbeats.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.ui.component.settingsCardContainerColor
import com.darkxvenom.airbeats.ui.component.settingsCardBorder
import com.darkxvenom.airbeats.ui.component.AvatarDisplay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.GenericShape
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.darkxvenom.airbeats.data.repository.NowPlayingTrack
import com.darkxvenom.airbeats.db.entities.Artist
import com.darkxvenom.airbeats.db.entities.EventWithSong
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.db.entities.SongWithStats
import coil.compose.AsyncImage
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.StatPeriod
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.ListQueue
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.ChoiceChipsRow
import com.darkxvenom.airbeats.ui.component.HideOnScrollFAB
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.component.LocalAlbumsGrid
import com.darkxvenom.airbeats.ui.component.LocalArtistsGrid
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.LocalSongsGrid
import com.darkxvenom.airbeats.ui.component.NavigationTitle
import com.darkxvenom.airbeats.ui.menu.AlbumMenu
import com.darkxvenom.airbeats.ui.menu.ArtistMenu
import com.darkxvenom.airbeats.ui.menu.SongMenu
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.utils.joinByBullet
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.utils.GlobalStatsUser
import com.darkxvenom.airbeats.viewmodels.GlobalStatsUiState
import com.darkxvenom.airbeats.viewmodels.StatsViewModel
import com.darkxvenom.airbeats.ui.component.RankBadge
import com.darkxvenom.airbeats.ui.component.AirBeatsRank
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val context = LocalContext.current

    val indexChips by viewModel.indexChips.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val firstEvent by viewModel.firstEvent.collectAsState()
    val currentDate = LocalDateTime.now()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsState()
    val globalStats by viewModel.globalStats.collectAsState()
    val nowPlayingTrack by viewModel.nowPlayingTrack.collectAsState()
    val recentEvents by viewModel.recentEvents.collectAsState()

    // BottomSheet para Insight
    var showInsightBottomSheet by remember { mutableStateOf(false) }
    var showWeeklyGlobalStats by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(globalStats.board.updatedAt, globalStats.board.users.size) {
        if (globalStats.board.users.isNotEmpty() && viewModel.shouldShowWeeklyPopup()) {
            showWeeklyGlobalStats = true
        }
    }

    val weeklyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")

                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)

                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year

                    val text =
                        when {
                            startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                            startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                            else -> "${date.dayOfMonth} - $endDateFormatted"
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val monthlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1)
            ) { it.minusMonths(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp
                            ?.withDayOfMonth(1),
                    )
                }.mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text =
                        if (date.year != currentDate.year) {
                            "$formattedDate ${date.year}"
                        } else {
                            formattedDate
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val yearlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate
                    .plusYears(1)
                    .withDayOfYear(1)
                    .minusDays(1),
            ) { it.minusYears(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp,
                    )
                }.mapIndexed { index, date ->
                    Pair(index, "${date.year}")
                }.toList()
        } else {
            emptyList()
        }

    Box(modifier = Modifier.fillMaxSize()) {
        // Adaptive background: blurred song thumbnail when playing, Library mesh when no song playing
        val artworkUrl = mediaMetadata?.thumbnailUrl
        com.darkxvenom.airbeats.ui.component.ScreenAdaptiveBackground(
            artworkUrl = artworkUrl
        )

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)
            )
        ) {
            item {
                ChoiceChipsRow(
                    chips =
                        when (selectedOption) {
                            OptionStats.WEEKS -> weeklyDates
                            OptionStats.MONTHS -> monthlyDates
                            OptionStats.YEARS -> yearlyDates
                            OptionStats.CONTINUOUS -> {
                                listOf(
                                    StatPeriod.WEEK_1.ordinal to pluralStringResource(
                                        R.plurals.n_week,
                                        1,
                                        1
                                    ),
                                    StatPeriod.MONTH_1.ordinal to pluralStringResource(
                                        R.plurals.n_month,
                                        1,
                                        1
                                    ),
                                    StatPeriod.MONTH_3.ordinal to pluralStringResource(
                                        R.plurals.n_month,
                                        3,
                                        3
                                    ),
                                    StatPeriod.MONTH_6.ordinal to pluralStringResource(
                                        R.plurals.n_month,
                                        6,
                                        6
                                    ),
                                    StatPeriod.YEAR_1.ordinal to pluralStringResource(
                                        R.plurals.n_year,
                                        1,
                                        1
                                    ),
                                    StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
                                )
                            }
                        },
                    options =
                        listOf(
                            OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
                            OptionStats.WEEKS to stringResource(R.string.weeks),
                            OptionStats.MONTHS to stringResource(R.string.months),
                            OptionStats.YEARS to stringResource(R.string.years),
                        ),
                    selectedOption = selectedOption,
                    onSelectionChange = {
                        viewModel.selectedOption.value = it
                        viewModel.indexChips.value = 0
                    },
                    currentValue = indexChips,
                    onValueUpdate = { viewModel.indexChips.value = it },
                )
            }

            item {
                val totalTime = mostPlayedSongsStats.sumOf { it.timeListened?.toLong() ?: 0L }
                val totalSongs = mostPlayedSongsStats.size
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(if (isFrosted) 20.dp else 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFrosted) settingsCardContainerColor(true) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    border = settingsCardBorder(isFrosted)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Time", style = MaterialTheme.typography.labelMedium)
                            Text(text = makeTimeString(totalTime), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Unique Songs", style = MaterialTheme.typography.labelMedium)
                            Text(text = "$totalSongs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                GlobalStatsBoardCard(
                    state = globalStats,
                    onRefresh = viewModel::refreshGlobalStats,
                )
            }

            item {
                ScrobblerStatsHubCard(
                    nowPlaying = nowPlayingTrack,
                    recentEvents = recentEvents,
                    isFrosted = isFrosted,
                    navController = navController,
                    onSongClick = { songId ->
                        playerConnection.playQueue(
                            YouTubeQueue(
                                endpoint = WatchEndpoint(songId),
                            ),
                        )
                    },
                )
            }

            item {
                StatsHighlightsSection(
                    topArtist = mostPlayedArtists.firstOrNull(),
                    topSong = mostPlayedSongsStats.firstOrNull(),
                    topSongEntity = mostPlayedSongs.firstOrNull(),
                    navController = navController,
                )
            }

            item(key = "artistPieChart") {
                if (mostPlayedArtists.isNotEmpty()) {
                    val overallTotalTime = mostPlayedSongsStats.sumOf { it.timeListened?.toLong() ?: 0L }
                    Spacer(modifier = Modifier.size(16.dp))
                    ArtistPieChart(
                        artists = mostPlayedArtists.take(5),
                        totalTime = overallTotalTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .animateItem()
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }

            item(key = "mostPlayedSongs") {
                NavigationTitle(
                    title = "${mostPlayedSongsStats.size} ${stringResource(id = R.string.songs)}",
                    modifier = Modifier.animateItem(),
                )

                LazyRow(
                    modifier = Modifier.animateItem(),
                ) {
                    itemsIndexed(
                        items = mostPlayedSongsStats,
                        key = { _, song -> song.id },
                    ) { index, song ->
                        LocalSongsGrid(
                            title = "${index + 1}. ${song.title}",
                            subtitle =
                                joinByBullet(
                                    pluralStringResource(
                                        R.plurals.n_time,
                                        song.songCountListened,
                                        song.songCountListened,
                                    ),
                                    makeTimeString(song.timeListened),
                                ),
                            thumbnailUrl = song.thumbnailUrl,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        endpoint = WatchEndpoint(song.id),
                                                        preloadItem = mostPlayedSongs[index].toMediaMetadata(),
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = mostPlayedSongs[index],
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                        )
                    }
                }
            }

            item(key = "mostPlayedArtists") {
                NavigationTitle(
                    title = "${mostPlayedArtists.size} ${stringResource(id = R.string.artists)}",
                    modifier = Modifier.animateItem(),
                )

                LazyRow(
                    modifier = Modifier.animateItem(),
                ) {
                    itemsIndexed(
                        items = mostPlayedArtists,
                        key = { _, artist -> artist.id },
                    ) { index, artist ->
                        LocalArtistsGrid(
                            title = "${index + 1}. ${artist.artist.name}",
                            subtitle =
                                joinByBullet(
                                    pluralStringResource(
                                        R.plurals.n_time,
                                        artist.songCount,
                                        artist.songCount
                                    ),
                                    makeTimeString(artist.timeListened?.toLong()),
                                ),
                            thumbnailUrl = artist.artist.thumbnailUrl,
                            modifier =
                                Modifier
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("artist/${artist.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                ArtistMenu(
                                                    originalArtist = artist,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                        )
                    }
                }
            }

            item(key = "mostPlayedAlbums") {
                NavigationTitle(
                    title = "${mostPlayedAlbums.size} ${stringResource(id = R.string.albums)}",
                    modifier = Modifier.animateItem(),
                )

                if (mostPlayedAlbums.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.animateItem(),
                    ) {
                        itemsIndexed(
                            items = mostPlayedAlbums,
                            key = { _, album -> album.id },
                        ) { index, album ->
                            LocalAlbumsGrid(
                                title = "${index + 1}. ${album.album.title}",
                                subtitle =
                                    joinByBullet(
                                        pluralStringResource(
                                            R.plurals.n_time,
                                            album.songCountListened!!,
                                            album.songCountListened
                                        ),
                                        makeTimeString(album.timeListened?.toLong()),
                                    ),
                                thumbnailUrl = album.album.thumbnailUrl,
                                isActive = album.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("album/${album.id}")
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = album,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                            )
                        }
                    }
                }
            }
        }

        // FAB to shuffle most played songs
        if (mostPlayedSongs.isNotEmpty()) {
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.most_played_songs),
                            items = mostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }
                                .shuffled()
                        )
                    )
                }
            )
        }

        TopAppBar(
            title = { Text(stringResource(R.string.stats)) },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            actions = {
                IconButton(
                    onClick = { navController.navigate("year_in_music") },
                    modifier = Modifier.size(48.dp),
                    enabled = true,
                    onLongClick = {}
                ) {
                    Icon(
                        painter = painterResource(R.drawable.stats),
                        contentDescription = "AirBeats Insights",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    // BottomSheet de Insight
    if (showInsightBottomSheet) {
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ModalBottomSheet(
            onDismissRequest = { showInsightBottomSheet = false },
            sheetState = sheetState,
            containerColor = if (isFrosted) (if (isDark) Color(0xFF141414).copy(alpha = 0.88f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) else MaterialTheme.colorScheme.surface,
            shape = sheetShape,
            modifier = Modifier.then(
                if (isFrosted) Modifier.border(BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)), sheetShape) else Modifier
            )
        ) {
            InsightBottomSheetContent(
                onNavigateToFullInsight = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showInsightBottomSheet = false
                    }
                    navController.navigate("insight")
                },
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        showInsightBottomSheet = false
                    }
                }
            )
        }
    }

    if (showWeeklyGlobalStats) {
        val weeklyUsers = remember(globalStats.board.users) {
            globalStats.board.users
                .filter { it.weeklyListenMs > 0 }
                .sortedByDescending { it.weeklyListenMs }
                .mapIndexed { index, user -> user.copy(rank = index + 1) }
        }
        WeeklyGlobalStatsSheet(
            users = weeklyUsers,
            currentUserId = globalStats.currentUserId,
            currentUserName = globalStats.currentUserName,
            onDismiss = {
                viewModel.markWeeklyPopupSeen()
                showWeeklyGlobalStats = false
            },
        )
    }
}

@Composable
private fun GlobalStatsBoardCard(
    state: GlobalStatsUiState,
    onRefresh: () -> Unit,
) {
    val users = state.board.users
    val topUser = users.firstOrNull()
    val currentUser = users.firstOrNull { it.id == state.currentUserId }
        ?: if (state.currentUserName.isNotBlank() && !state.currentUserName.equals("AirBeats User", ignoreCase = true)) {
            users.firstOrNull { it.name.trim().equals(state.currentUserName.trim(), ignoreCase = true) }
        } else null

    val isFrosted = isFrostedGlassUiEnabled()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(if (isFrosted) 20.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFrosted) settingsCardContainerColor(true) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = settingsCardBorder(isFrosted)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Global Stats",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = topUser?.let { "Most listened: ${it.name} • Top ${users.size}" } ?: "Waiting for daily cloud stats",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(onClick = onRefresh, enabled = !state.isLoading) {
                    Text(if (state.isLoading) "Syncing" else "Refresh")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlobalStatPill(
                    label = "Top listener",
                    value = topUser?.let { formatListenHours(it.totalListenMs) } ?: "--",
                    modifier = Modifier.weight(1f),
                )
                GlobalStatPill(
                    label = "Your rank",
                    value = currentUser?.rank?.let { "#$it" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    val isCurrent = user.id == state.currentUserId || (currentUser != null && user.id == currentUser.id)
                    GlobalUserRankRow(
                        user = user,
                        isCurrentUser = isCurrent,
                    )
                }
            }

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun GlobalStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val pillBg = if (isFrosted) (if (isDark) Color.White.copy(alpha = 0.06f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val pillBorder = if (isDark) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(
        modifier =
            modifier
                .background(pillBg, RoundedCornerShape(12.dp))
                .then(
                    if (isFrosted) Modifier.border(BorderStroke(1.dp, pillBorder), RoundedCornerShape(12.dp)) else Modifier
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlobalUserRankRow(
    user: GlobalStatsUser,
    isCurrentUser: Boolean,
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val rowBg = if (isCurrentUser) {
        if (isFrosted) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        if (isFrosted) (if (isDark) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val rowBorder = if (isCurrentUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(rowBg, RoundedCornerShape(12.dp))
                .then(
                    if (isFrosted) Modifier.border(BorderStroke(1.dp, rowBorder), RoundedCornerShape(12.dp)) else Modifier
                )
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "#${user.rank}",
            modifier = Modifier.width(42.dp),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (isCurrentUser) {
            AvatarDisplay(size = 34.dp)
        } else {
            ProfileBubble(user.profileUrl, user.name)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = user.name,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isCurrentUser) FontWeight.Black else FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            val userHours = user.totalListenMs.toDouble() / (3600.0 * 1000.0)
            val userRank = if (userHours >= 1.0) AirBeatsRank.fromHours(userHours.toInt()) else null
            userRank?.let { rank ->
                Spacer(modifier = Modifier.width(6.dp))
                RankBadge(rank = rank, displayedRank = null, size = 18.dp)
            }
        }
        Text(
            text = formatListenHours(user.totalListenMs),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeeklyGlobalStatsSheet(
    users: List<GlobalStatsUser>,
    currentUserId: String,
    currentUserName: String = "",
    onDismiss: () -> Unit,
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isFrosted) (if (isDark) Color(0xFF141414).copy(alpha = 0.88f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)) else MaterialTheme.colorScheme.surface,
        shape = sheetShape,
        modifier = Modifier.then(
            if (isFrosted) Modifier.border(BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)), sheetShape) else Modifier
        ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1DB954).copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface,
                            ),
                        ),
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            Column {
                Text(
                    text = "Weekly Global Stats",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "Total Users: ${users.size} • Only names and listened hours are shown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
                Spacer(modifier = Modifier.height(18.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                ) {
                    items(users, key = { it.id }) { user ->
                        val isCurrent = user.id == currentUserId || (currentUserName.isNotBlank() && !currentUserName.equals("AirBeats User", ignoreCase = true) && user.name.trim().equals(currentUserName.trim(), ignoreCase = true))
                        val rowBg = if (isCurrent) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else if (isFrosted) {
                            if (isDark) Color.White.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                        }
                        val rowBorder = if (isCurrent) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                        }

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .background(
                                        rowBg,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .then(
                                        if (isFrosted) Modifier.border(BorderStroke(1.dp, rowBorder), RoundedCornerShape(14.dp)) else Modifier
                                    )
                                    .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "#${user.rank}",
                                modifier = Modifier.width(46.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                            )
                            if (isCurrent) {
                                AvatarDisplay(size = 34.dp)
                            } else {
                                ProfileBubble(user.profileUrl, user.name)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = user.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold,
                                )
                                val userHours = user.totalListenMs.toDouble() / (3600.0 * 1000.0)
                                val userRank = if (userHours >= 1.0) AirBeatsRank.fromHours(userHours.toInt()) else null
                                userRank?.let { rank ->
                                    Spacer(modifier = Modifier.width(6.dp))
                                    RankBadge(rank = rank, displayedRank = null, size = 18.dp)
                                }
                            }
                            Text(
                                text = formatListenHours(user.weeklyListenMs),
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.done))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileBubble(
    profileUrl: String?,
    name: String,
) {
    val validProfileUrl = profileUrl?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    var imageLoadFailed by remember(validProfileUrl) { mutableStateOf(false) }

    if (validProfileUrl != null && !imageLoadFailed) {
        AsyncImage(
            model = validProfileUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onError = { imageLoadFailed = true },
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape),
        )
    } else {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun formatListenHours(milliseconds: Long): String {
    val hours = milliseconds / 3_600_000.0
    return if (hours >= 10) {
        "${hours.toInt()}h"
    } else {
        String.format(Locale.US, "%.1fh", hours)
    }
}

@Composable
fun InsightBottomSheetContent(
    onNavigateToFullInsight: () -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = LocalDateTime.now().year
    val gradientColors = listOf(
        Color(0xFF1DB954),
        Color(0xFF1ED760),
        Color(0xFF191414)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.auto_awesome), // o bar_chart
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF1DB954)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AirBeats Insight",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Discover your musical year",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Card principal
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(gradientColors)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Your Musical Year",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Column {
                        Text(
                            text = "$currentYear",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Tap to view your full statistics",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Características
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InsightFeatureItem(
                iconRes = R.drawable.music_note,
                label = "Top\nSongs"
            )
            InsightFeatureItem(
                iconRes = R.drawable.person,
                label = "Top\nArtists"
            )
            InsightFeatureItem(
                iconRes = R.drawable.equalizer,
                label = "Full\nStatistics"
            )
            InsightFeatureItem(
                iconRes = R.drawable.download,
                label = "Download\nReport"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para ver completo
        Button(
            onClick = onNavigateToFullInsight,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "View Full AirBeats Insight",
                modifier = Modifier.padding(8.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun InsightFeatureItem(
    iconRes: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = Color(0xFF1DB954)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }

@Composable
fun ArtistPieChart(
    artists: List<Artist>,
    totalTime: Long = artists.sumOf { it.timeListened?.toLong() ?: 0L },
    modifier: Modifier = Modifier
) {
    val effectiveTotalTime = if (totalTime > 0L) totalTime else artists.sumOf { it.timeListened?.toLong() ?: 0L }
    if (effectiveTotalTime == 0L) return

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            var startAngle = -90f

            artists.forEach { artist ->
                val time = artist.timeListened?.toLong() ?: 0L
                val sweepAngle = (time.toFloat() / effectiveTotalTime) * 360f
                
                if (sweepAngle > 1f) {
                    AsyncImage(
                        model = artist.artist.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(PieSliceShape(startAngle, sweepAngle))
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Column {
            Text(
                text = "Total Time Listened",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = makeTimeString(effectiveTotalTime),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

fun PieSliceShape(startAngle: Float, sweepAngle: Float): GenericShape {
    return GenericShape { size, _ ->
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        
        moveTo(center.x, center.y)
        
        val startRad = Math.toRadians(startAngle.toDouble())
        
        lineTo(
            (center.x + radius * cos(startRad)).toFloat(),
            (center.y + radius * sin(startRad)).toFloat()
        )
        
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(
                center = center,
                radius = radius
            ),
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngle,
            forceMoveTo = false
        )
        
        lineTo(center.x, center.y)
        close()
    }
}

@Composable
fun StatsHighlightsSection(
    topArtist: Artist?,
    topSong: SongWithStats?,
    topSongEntity: com.darkxvenom.airbeats.db.entities.Song?,
    navController: NavController
) {
    if (topArtist == null && topSong == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (topArtist != null) {
            StatsHighlightCard(
                title = "Your Favourite Artist",
                mainText = topArtist.artist.name,
                subText = "${topArtist.songCount} songs played • ${makeTimeString(topArtist.timeListened?.toLong())}",
                imageUrl = topArtist.artist.thumbnailUrl,
                onClick = { navController.navigate("artist/${topArtist.id}") }
            )
        }

        if (topSong != null && topSongEntity != null) {
            StatsHighlightCard(
                title = "Your Favourite Song",
                mainText = topSong.title,
                subText = "${topSong.songCountListened} plays • ${makeTimeString(topSong.timeListened)}",
                imageUrl = topSong.thumbnailUrl,
                onClick = { }
            )
        }
    }
}

@Composable
fun StatsHighlightCard(
    title: String,
    mainText: String,
    subText: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (isFrosted) 20.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFrosted) settingsCardContainerColor(true) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = settingsCardBorder(isFrosted)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = mainText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun cleanPackageName(pkg: String?): String = when {
    pkg == null -> "External App"
    pkg.contains("spotify", ignoreCase = true) -> "Spotify"
    pkg.contains("youtube", ignoreCase = true) -> "YT Music"
    pkg.contains("apple", ignoreCase = true) -> "Apple Music"
    pkg.contains("amazon", ignoreCase = true) -> "Amazon Music"
    pkg.contains("deezer", ignoreCase = true) -> "Deezer"
    pkg.contains("tidal", ignoreCase = true) -> "Tidal"
    pkg.contains("jiosaavn", ignoreCase = true) -> "JioSaavn"
    pkg.contains("wynk", ignoreCase = true) -> "Wynk"
    pkg.contains("gaana", ignoreCase = true) -> "Gaana"
    pkg.contains("soundcloud", ignoreCase = true) -> "SoundCloud"
    pkg.contains("bandcamp", ignoreCase = true) -> "Bandcamp"
    else -> pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}

private fun formatRelativeTime(dateTime: LocalDateTime): String {
    val duration = Duration.between(dateTime, LocalDateTime.now())
    return when {
        duration.toMinutes() < 1 -> "Just now"
        duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
        duration.toHours() < 24 -> "${duration.toHours()}h ago"
        duration.toDays() == 1L -> "Yesterday"
        duration.toDays() < 7 -> "${duration.toDays()}d ago"
        else -> dateTime.format(DateTimeFormatter.ofPattern("dd MMM"))
    }
}

@Composable
fun LiveEqualizerBars(
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "live_eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 3f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq_bar1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 13f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 310, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq_bar2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 5f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 440, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq_bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar1.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(2.5.dp)
                .height(bar3.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(barColor)
        )
    }
}

@Composable
fun ScrobblerStatsHubCard(
    nowPlaying: NowPlayingTrack?,
    recentEvents: List<EventWithSong>,
    isFrosted: Boolean,
    navController: NavController,
    onSongClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scrobblerPrefs = remember { com.darkxvenom.airbeats.data.local.ScrobblerPreferences(context) }
    val settings by scrobblerPrefs.settings.collectAsState(initial = com.darkxvenom.airbeats.data.local.ScrobblerSettings())
    val selectedCount = settings.selectedPackages.size
    val isEnabled = settings.enabled

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(if (isFrosted) 20.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFrosted) settingsCardContainerColor(true) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        border = settingsCardBorder(isFrosted)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isEnabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.graphic_eq),
                        contentDescription = null,
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Music Scrobbler",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isEnabled) Color(0xFF2E7D32).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isEnabled) "Active" else "Inactive",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnabled) {
                            if (nowPlaying != null) "Listening on ${cleanPackageName(nowPlaying.packageName)}"
                            else if (selectedCount > 0) "$selectedCount app(s) monitored (Spotify, YT Music, etc.)"
                            else "No apps selected — tap Select Apps"
                        } else {
                            "Track plays from Spotify, YT Music & other apps"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (nowPlaying != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // LIVE NOW PLAYING CARD
            if (nowPlaying != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            nowPlaying.songId?.let { onSongClick(it) }
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album Art / Fallback with Equalizer animation overlay
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (!nowPlaying.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = nowPlaying.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.music_note),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            // Equalizer badge on bottom right of cover art
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                            ) {
                                LiveEqualizerBars(
                                    modifier = Modifier.height(11.dp),
                                    barColor = Color(0xFFFF4081)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nowPlaying.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = nowPlaying.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!nowPlaying.packageName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "via ${cleanPackageName(nowPlaying.packageName)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Pulse Dot + Now Playing Badge (as in screenshot 1)
                        val pulseTransition = rememberInfiniteTransition(label = "pulse_badge")
                        val dotAlpha by pulseTransition.animateFloat(
                            initialValue = 0.35f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 650, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "dot_pulse"
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE91E63).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFFE91E63).copy(alpha = 0.35f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE91E63).copy(alpha = dotAlpha))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Now Playing",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF4081)
                                )
                            }
                        }
                    }
                }
            }

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("settings/scrobbler/apps") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select Apps")
                }

                androidx.compose.material3.OutlinedButton(
                    onClick = { navController.navigate("settings/scrobbler") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.tune),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Settings")
                }
            }

            // RECENT SCROBBLES / PLAYS PREVIEW (Matching Screenshot 1)
            val scrobbleHistory = remember(recentEvents) {
                recentEvents.filter { it.song.song.id.startsWith("scrobble_") || it.event.playTime > 0 }.take(5)
            }
            if (scrobbleHistory.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Plays",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${recentEvents.size} total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    scrobbleHistory.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSongClick(item.song.id) }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (!item.song.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = item.song.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.music_note),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.song.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.song.artists.joinToString { it.name }.ifEmpty { "Unknown Artist" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = formatRelativeTime(item.event.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

