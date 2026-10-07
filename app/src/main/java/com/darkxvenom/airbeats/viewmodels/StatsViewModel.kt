package com.darkxvenom.airbeats.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.constants.statToPeriod
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.ui.screens.OptionStats
import com.darkxvenom.airbeats.utils.AirBeatsStatsCloudClient
import com.darkxvenom.airbeats.utils.AirBeatsStatsCloudSync
import com.darkxvenom.airbeats.utils.GlobalStatsBoard
import com.darkxvenom.airbeats.utils.LocalStatsUpload
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.reportException
import timber.log.Timber
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale
import com.darkxvenom.airbeats.data.repository.NowPlayingTrack
import com.darkxvenom.airbeats.data.repository.ScrobbleRepository
import com.darkxvenom.airbeats.db.entities.EventWithSong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.darkxvenom.airbeats.ui.component.AirBeatsRank
import javax.inject.Inject

data class GlobalStatsUiState(
    val isLoading: Boolean = true,
    val board: GlobalStatsBoard = GlobalStatsBoard(),
    val error: String? = null,
    val currentUserId: String = "",
    val currentUserName: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel
@Inject
constructor(
    val database: MusicDatabase,
    @ApplicationContext private val context: Context,
    private val namePreferenceManager: NamePreferenceManager,
    private val scrobbleRepository: ScrobbleRepository,
) : ViewModel() {
    val selectedOption = MutableStateFlow(OptionStats.CONTINUOUS)
    val indexChips = MutableStateFlow(0)
    val globalStats = MutableStateFlow(GlobalStatsUiState())

    val nowPlayingTrack: StateFlow<NowPlayingTrack?> = scrobbleRepository.nowPlaying

    val recentEvents: StateFlow<List<EventWithSong>> = database.events()
        .map { it.take(25) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val totalListenHours: Flow<Double> = database.mostPlayedSongsStats(0L, limit = -1, toTimeStamp = Long.MAX_VALUE)
        .map { songs ->
            val totalMs = songs.sumOf { it.timeListened?.toLong() ?: 0L }
            totalMs.toDouble() / (3600.0 * 1000.0)
        }

    val currentRank: Flow<AirBeatsRank?> = totalListenHours.map { hours ->
        if (hours >= 1.0) AirBeatsRank.fromHours(hours.toInt()) else null
    }

    private val cloudClient = AirBeatsStatsCloudClient()
    private val statsPreferences =
        context.getSharedPreferences("airbeats_global_stats", Context.MODE_PRIVATE)

    val mostPlayedSongsStats =
        combine(
            selectedOption,
            indexChips,
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedSongsStats(
                        fromTimeStamp = statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp =
                            if (selection == OptionStats.CONTINUOUS || t == 0) {
                                Long.MAX_VALUE
                            } else {
                                statToPeriod(selection, t - 1)
                            },
                    )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedSongs =
        combine(
            selectedOption,
            indexChips,
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedSongs(
                        fromTimeStamp = statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp =
                            if (selection == OptionStats.CONTINUOUS || t == 0) {
                                Long.MAX_VALUE
                            } else {
                                statToPeriod(selection, t - 1)
                            },
                    )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedArtists =
        combine(
            selectedOption,
            indexChips,
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedArtists(
                        statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp =
                            if (selection == OptionStats.CONTINUOUS || t == 0) {
                                Long.MAX_VALUE
                            } else {
                                statToPeriod(selection, t - 1)
                            },
                    ).map { artists ->
                        artists.filter { it.artist.isYouTubeArtist || it.artist.isLocalArtist || it.artist.isScrobbleArtist }
                    }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedAlbums =
        combine(
            selectedOption,
            indexChips,
        ) { first, second -> Pair(first, second) }
            .flatMapLatest { (selection, t) ->
                database.mostPlayedAlbums(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp =
                        if (selection == OptionStats.CONTINUOUS || t == 0) {
                            Long.MAX_VALUE
                        } else {
                            statToPeriod(selection, t - 1)
                        },
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val firstEvent =
        database
            .firstEvent()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    init {
        viewModelScope.launch {
            syncAndLoadGlobalStats()
        }
        viewModelScope.launch {
            mostPlayedArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.isYouTubeArtist && (it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10))
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
        viewModelScope.launch {
            mostPlayedAlbums.collect { albums ->
                albums
                    .filter {
                        (it.album.id.startsWith("MPREb_") || it.album.id.startsWith("OLAK5uy_")) && it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }

    fun markWeeklyPopupSeen() {
        statsPreferences.edit().putString(KEY_LAST_WEEKLY_POPUP, currentWeekKey()).apply()
    }

    fun shouldShowWeeklyPopup(): Boolean =
        statsPreferences.getString(KEY_LAST_WEEKLY_POPUP, "") != currentWeekKey()

    fun refreshGlobalStats() {
        viewModelScope.launch {
            syncAndLoadGlobalStats(forceUpload = false)
        }
    }

    private suspend fun syncAndLoadGlobalStats(forceUpload: Boolean = false) {
        val currentName = runCatching { namePreferenceManager.userName.first().trim() }.getOrDefault("")
        globalStats.value = globalStats.value.copy(isLoading = true, error = null, currentUserName = currentName)
        val userId = AirBeatsStatsCloudSync.resolveStableUserId(context, namePreferenceManager, statsPreferences)

        var mustUpdateGlobalStats = forceUpload || shouldUploadToday()

        // Read leaderboard first to check if cloud has recorded higher or different listen time than local DB
        val boardResult = cloudClient.readBoard()
        val initialBoard = boardResult.getOrNull()
        if (initialBoard != null) {
            val userNumber = AirBeatsStatsCloudSync.getUserNumber(context)
            val matchedUser = initialBoard.users.firstOrNull { it.id == userId }
                ?: (if (currentName.isNotBlank() && !currentName.equals("AirBeats User", ignoreCase = true))
                    initialBoard.users.firstOrNull { it.name.trim().equals(currentName, ignoreCase = true) }
                else null)
                ?: (if (!userNumber.isNullOrBlank())
                    initialBoard.users.firstOrNull { it.user == userNumber }
                else null)

            // If global stats is greater than the app's local stats (e.g. user restored an older backup),
            // update global stats according to the app so the app is the authoritative source
            if (matchedUser != null) {
                val currentLocalTime = buildUpload(userId)?.totalListenMs ?: 0L
                if (matchedUser.totalListenMs > currentLocalTime) {
                    mustUpdateGlobalStats = true
                }
            }
        }

        if (mustUpdateGlobalStats) {
            buildUpload(userId)?.let { upload ->
                cloudClient
                    .uploadDaily(upload)
                    .onSuccess { board ->
                        statsPreferences.edit().putString(KEY_LAST_UPLOAD_DAY, LocalDate.now().toString()).apply()
                        val userNumber = AirBeatsStatsCloudSync.getUserNumber(context)
                        val updatedUserNumber = board.userNumber ?: board.users.firstOrNull { it.id == userId }?.user
                        if (!updatedUserNumber.isNullOrBlank()) {
                            AirBeatsStatsCloudSync.persistUserNumber(context, updatedUserNumber)
                        }
                        globalStats.value =
                            GlobalStatsUiState(
                                isLoading = false,
                                board = board,
                                currentUserId = userId,
                                currentUserName = currentName,
                                error = null,
                            )
                        return
                    }.onFailure { uploadError ->
                        Timber.d("StatsViewModel: Daily upload error/throttled: ${uploadError.message}")
                    }
            }
        }

        if (initialBoard != null) {
            globalStats.value =
                GlobalStatsUiState(
                    isLoading = false,
                    board = initialBoard,
                    currentUserId = userId,
                    currentUserName = currentName,
                )
        } else {
            val error = boardResult.exceptionOrNull()
            globalStats.value =
                globalStats.value.copy(
                    isLoading = false,
                    error = error?.message,
                    currentUserId = userId,
                    currentUserName = currentName,
                )
        }
    }

    private suspend fun buildUpload(userId: String): LocalStatsUpload? {
        val isNameSet = namePreferenceManager.isNameSet.first()
        val settings = context.dataStore.data.first()
        val settingsEmail = settings[com.darkxvenom.airbeats.constants.AccountEmailKey]?.trim()?.takeIf { it.isNotBlank() }
        val managerEmail = namePreferenceManager.accountEmail.first().trim().takeIf { it.isNotBlank() }
        val effectiveEmail = (managerEmail ?: settingsEmail)?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

        val settingsName = settings[com.darkxvenom.airbeats.constants.AccountNameKey]?.trim()?.takeIf { it.isNotBlank() }
        val managerName = namePreferenceManager.userName.first().trim().takeIf { it.isNotBlank() }
        val effectiveName = (managerName ?: settingsName ?: effectiveEmail?.substringBefore("@"))?.ifBlank { null }
            ?: (android.os.Build.MODEL ?: "AirBeats User")

        val isSignedIn = !effectiveEmail.isNullOrBlank() || settings[com.darkxvenom.airbeats.constants.InnerTubeCookieKey]?.isNotBlank() == true || isNameSet

        // Automatically sync email and username to namePreferenceManager if missing
        if (!effectiveEmail.isNullOrBlank() && managerEmail.isNullOrBlank()) {
            runCatching { namePreferenceManager.saveAccountEmail(effectiveEmail) }
        }
        if (managerName.isNullOrBlank() && !effectiveName.isBlank() && effectiveName != "AirBeats User") {
            runCatching { namePreferenceManager.saveUserName(effectiveName) }
        }

        if (!isSignedIn && !isNameSet) return null

        val now = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
        val weekStart =
            LocalDate
                .now()
                .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        val allSongs = database.mostPlayedSongsStats(0L, limit = -1, toTimeStamp = Long.MAX_VALUE).first()
        val weekSongs = database.mostPlayedSongsStats(weekStart, limit = -1, toTimeStamp = Long.MAX_VALUE).first()
        val totalListenMs = allSongs.sumOf { it.timeListened?.toLong() ?: 0L }
        val weeklyListenMs = weekSongs.sumOf { it.timeListened?.toLong() ?: 0L }
        val name = effectiveName
        val email = effectiveEmail
        val profileUrl =
            when (val avatar = AvatarPreferenceManager(context).getAvatarSelection.first()) {
                is AvatarSelection.DiceBear -> avatar.url
                is AvatarSelection.Custom -> avatar.cloudUrl ?: avatar.uri
                else -> null
            }
        var fcmToken: String? = null
        for (i in 1..3) {
            fcmToken = try {
                suspendCancellableCoroutine<String?> { continuation ->
                    com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(task.result)
                        } else {
                            continuation.resume(null)
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
            if (fcmToken != null) break
            kotlinx.coroutines.delay(1000L * i)
        }
        if (fcmToken == null) {
            fcmToken = "n/v"
        }

        return LocalStatsUpload(
            userId = userId,
            user = AirBeatsStatsCloudSync.getUserNumber(context),
            name = name,
            profileUrl = profileUrl,
            email = email,
            totalListenMs = totalListenMs,
            weeklyListenMs = weeklyListenMs,
            fcmToken = fcmToken,
        )
    }

    private fun shouldUploadToday(): Boolean = true

    private fun currentWeekKey(): String {
        val date = LocalDate.now()
        val fields = WeekFields.of(Locale.getDefault())
        return "${date.get(fields.weekBasedYear())}-${date.get(fields.weekOfWeekBasedYear())}"
    }

    private companion object {
        const val KEY_USER_ID = "global_stats_user_id"
        const val KEY_LAST_UPLOAD_DAY = "last_global_stats_upload_day"
        const val KEY_LAST_WEEKLY_POPUP = "last_weekly_global_popup"
    }
}
