package com.darkxvenom.airbeats.utils

import android.content.Context
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID

object AirBeatsStatsCloudSync {
    suspend fun syncDaily(
        context: Context,
        database: MusicDatabase,
        namePreferenceManager: NamePreferenceManager,
    ): Result<GlobalStatsBoard>? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val userId = stableUserId(preferences)
        val upload = buildUpload(context, database, namePreferenceManager, userId) ?: return null
        return AirBeatsStatsCloudClient()
            .uploadDaily(upload)
            .onSuccess {
                preferences.edit().putString(KEY_LAST_UPLOAD_DAY, LocalDate.now().toString()).apply()
            }
    }

    suspend fun buildUpload(
        context: Context,
        database: MusicDatabase,
        namePreferenceManager: NamePreferenceManager,
        userId: String,
    ): LocalStatsUpload? {
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
                is AvatarSelection.Custom -> avatar.cloudUrl
                else -> null
            }
        return LocalStatsUpload(
            userId = userId,
            name = name,
            profileUrl = profileUrl,
            totalListenMs = totalListenMs,
            weeklyListenMs = weeklyListenMs,
        )
    }

    fun stableUserId(context: Context): String =
        stableUserId(context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE))

    private fun stableUserId(preferences: android.content.SharedPreferences): String {
        val existing = preferences.getString(KEY_USER_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_USER_ID, generated).apply()
        return generated
    }

    const val PREFERENCES_NAME = "airbeats_global_stats"
    const val KEY_USER_ID = "global_stats_user_id"
    const val KEY_LAST_UPLOAD_DAY = "last_global_stats_upload_day"
    const val KEY_LAST_WEEKLY_POPUP = "last_weekly_global_popup"
}
