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
import com.darkxvenom.airbeats.utils.GlobalStatsBoard
import com.darkxvenom.airbeats.utils.LocalStatsUpload
import com.darkxvenom.airbeats.utils.reportException
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
import com.google.firebase.messaging.FirebaseMessaging
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import com.darkxvenom.airbeats.ui.component.AirBeatsRank

data class GlobalStatsUiState(
    val isLoading: Boolean = true,
    val board: GlobalStatsBoard = GlobalStatsBoard(),
    val error: String? = null,
    val currentUserId: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel
@Inject
constructor(
    val database: MusicDatabase,
    @ApplicationContext private val context: Context,
    private val namePreferenceManager: NamePreferenceManager,
) : ViewModel() {
    val selectedOption = MutableStateFlow(OptionStats.CONTINUOUS)
    val indexChips = MutableStateFlow(0)
    val globalStats = MutableStateFlow(GlobalStatsUiState())

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
                                LocalDateTime
                                    .now()
                                    .toInstant(
                                        ZoneOffset.UTC,
                                    ).toEpochMilli()
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
                                LocalDateTime
                                    .now()
                                    .toInstant(
                                        ZoneOffset.UTC,
                                    ).toEpochMilli()
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
                                LocalDateTime
                                    .now()
                                    .toInstant(
                                        ZoneOffset.UTC,
                                    ).toEpochMilli()
                            } else {
                                statToPeriod(selection, t - 1)
                            },
                    ).map { artists ->
                        artists.filter { it.artist.isYouTubeArtist }
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
                            LocalDateTime
                                .now()
                                .toInstant(
                                    ZoneOffset.UTC,
                                ).toEpochMilli()
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
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
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
                        it.album.songCount == 0
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
        globalStats.value = globalStats.value.copy(isLoading = true, error = null)
        val userId = stableUserId()
        if (forceUpload || shouldUploadToday()) {
            buildUpload(userId)?.let { upload ->
                cloudClient
                    .uploadDaily(upload)
                    .onSuccess { board ->
                        statsPreferences.edit().putString(KEY_LAST_UPLOAD_DAY, LocalDate.now().toString()).apply()
                        globalStats.value =
                            GlobalStatsUiState(
                                isLoading = false,
                                board = board,
                                currentUserId = userId,
                            )
                    }.onFailure { error ->
                        globalStats.value =
                            globalStats.value.copy(
                                isLoading = false,
                                error = error.message,
                                currentUserId = userId,
                            )
                    }
                return
            }
        }

        cloudClient
            .readBoard()
            .onSuccess { board ->
                globalStats.value =
                    GlobalStatsUiState(
                        isLoading = false,
                        board = board,
                        currentUserId = userId,
                    )
            }.onFailure { error ->
                globalStats.value =
                    globalStats.value.copy(
                        isLoading = false,
                        error = error.message,
                        currentUserId = userId,
                    )
            }
    }

    private suspend fun buildUpload(userId: String): LocalStatsUpload? {
        val isNameSet = namePreferenceManager.isNameSet.first()
        if (!isNameSet) return null

        val now = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
        val weekStart =
            LocalDate
                .now()
                .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        val allSongs = database.mostPlayedSongsStats(0L, limit = -1, toTimeStamp = now).first()
        val weekSongs = database.mostPlayedSongsStats(weekStart, limit = -1, toTimeStamp = now).first()
        val totalListenMs = allSongs.sumOf { it.timeListened?.toLong() ?: 0L }
        val weeklyListenMs = weekSongs.sumOf { it.timeListened?.toLong() ?: 0L }
        val name = namePreferenceManager.userName.first().ifBlank { android.os.Build.MODEL ?: "AirBeats User" }
        val profileUrl =
            when (val avatar = AvatarPreferenceManager(context).getAvatarSelection.first()) {
                is AvatarSelection.DiceBear -> avatar.url
                else -> null
            }

        val fcmToken = try {
            suspendCancellableCoroutine<String?> { continuation ->
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
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

        return LocalStatsUpload(
            userId = userId,
            name = name,
            profileUrl = profileUrl,
            totalListenMs = totalListenMs,
            weeklyListenMs = weeklyListenMs,
            fcmToken = fcmToken,
        )
    }

    private fun shouldUploadToday(): Boolean = true

    private fun stableUserId(): String {
        val existing = statsPreferences.getString(KEY_USER_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        statsPreferences.edit().putString(KEY_USER_ID, generated).apply()
        return generated
    }

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
