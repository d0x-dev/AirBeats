package com.darkxvenom.airbeats.utils

import android.content.Context
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.db.entities.Event
import com.darkxvenom.airbeats.db.entities.SongEntity
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
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
        val userId = resolveStableUserId(context, namePreferenceManager, preferences)
        val cloudClient = AirBeatsStatsCloudClient()

        val upload = buildUpload(context, database, namePreferenceManager, userId) ?: return null
        return cloudClient
            .uploadDaily(upload)
            .onSuccess { board ->
                preferences.edit().putString(KEY_LAST_UPLOAD_DAY, LocalDate.now().toString()).apply()
                val updatedUserNumber = board.userNumber ?: board.users.firstOrNull { it.id == userId }?.user
                if (!updatedUserNumber.isNullOrBlank()) {
                    persistUserNumber(context, updatedUserNumber)
                }
            }
    }

    suspend fun buildUpload(
        context: Context,
        database: MusicDatabase,
        namePreferenceManager: NamePreferenceManager,
        userId: String,
    ): LocalStatsUpload? {
        val isNameSet = namePreferenceManager.isNameSet.first()
        val settings = context.dataStore.data.first()
        val settingsEmail = settings[com.darkxvenom.airbeats.constants.AccountEmailKey]?.trim()?.takeIf { it.isNotBlank() }
        val managerEmail = namePreferenceManager.accountEmail.first().trim().takeIf { it.isNotBlank() }
        val effectiveEmail = (managerEmail ?: settingsEmail).normalizedEmail()

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
        val profileUrl =
            when (val avatar = AvatarPreferenceManager(context).getAvatarSelection.first()) {
                is AvatarSelection.DiceBear -> avatar.url
                is AvatarSelection.Custom -> avatar.cloudUrl ?: avatar.uri
                else -> null
            }
        return LocalStatsUpload(
            userId = userId,
            user = getUserNumber(context),
            name = effectiveName,
            profileUrl = profileUrl,
            email = effectiveEmail,
            totalListenMs = totalListenMs,
            weeklyListenMs = weeklyListenMs,
        )
    }

    const val PREFERENCES_NAME = "airbeats_global_stats"
    const val KEY_USER_ID = "global_stats_user_id"
    const val KEY_USER_NUMBER = "global_stats_user_number"
    const val KEY_LAST_UPLOAD_DAY = "last_global_stats_upload_day"
    const val KEY_LAST_WEEKLY_POPUP = "last_weekly_global_popup"
    const val STATS_IDENTITY_FILENAME = "stats_identity.json"

    fun getUserNumber(context: Context): String? {
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return preferences.getString(KEY_USER_NUMBER, null)?.trim()?.takeIf { it.isNotBlank() }
            ?: runCatching {
                val f = java.io.File(context.filesDir, STATS_IDENTITY_FILENAME)
                if (f.exists()) {
                    org.json.JSONObject(f.readText()).optString("userNumber").trim().takeIf { it.isNotBlank() }
                } else null
            }.getOrNull()
            ?: runCatching {
                val parent = context.filesDir.parentFile
                val xmlFile = java.io.File(parent, "shared_prefs/$PREFERENCES_NAME.xml")
                if (xmlFile.exists()) {
                    val content = xmlFile.readText()
                    val match = """<string name="$KEY_USER_NUMBER">([^<]+)</string>""".toRegex().find(content)
                    match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                } else null
            }.getOrNull()
    }

    fun persistUserNumber(context: Context, userNumber: String) {
        if (userNumber.isBlank()) return
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val oldNumber = preferences.getString(KEY_USER_NUMBER, null)?.trim()
        preferences.edit().putString(KEY_USER_NUMBER, userNumber).commit()
        runCatching {
            val f = java.io.File(context.filesDir, STATS_IDENTITY_FILENAME)
            val json = if (f.exists()) runCatching { org.json.JSONObject(f.readText()) }.getOrDefault(org.json.JSONObject()) else org.json.JSONObject()
            json.put("userNumber", userNumber)
            f.writeText(json.toString())
        }
        if (!oldNumber.isNullOrBlank() && oldNumber != userNumber) {
            runCatching {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic(oldNumber)
                    .addOnSuccessListener {
                        timber.log.Timber.i("Unsubscribed from old FCM topic: $oldNumber")
                    }
            }
        }
        // Subscribe to direct numeric topic (e.g. "1", "2", "10")
        runCatching {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(userNumber)
                .addOnSuccessListener {
                    timber.log.Timber.i("Subscribed to FCM personal topic: $userNumber")
                }
                .addOnFailureListener { e ->
                    timber.log.Timber.w(e, "Failed to subscribe to FCM personal topic: $userNumber")
                }
        }
    }

    fun stableUserId(context: Context): String =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).getString(KEY_USER_ID, null)
            ?: runCatching {
                val f = java.io.File(context.filesDir, STATS_IDENTITY_FILENAME)
                if (f.exists()) org.json.JSONObject(f.readText()).optString("userId").takeIf(String::isNotBlank) else null
            }.getOrNull()
            ?: UUID.randomUUID().toString().also { newId ->
                persistUserId(context, context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE), newId)
            }

    suspend fun resolveStableUserId(
        context: Context,
        namePreferenceManager: NamePreferenceManager,
        preferences: android.content.SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
    ): String {
        // 1. Check if we have an existing candidate UID locally from preferences, stats_identity.json, or XML
        var candidateUid = preferences.getString(KEY_USER_ID, null)?.trim()?.takeIf { it.isNotBlank() }

        if (candidateUid == null) {
            candidateUid = runCatching {
                val identityFile = java.io.File(context.filesDir, STATS_IDENTITY_FILENAME)
                if (identityFile.exists()) {
                    org.json.JSONObject(identityFile.readText()).optString("userId").trim().takeIf { it.isNotBlank() }
                } else null
            }.getOrNull()
        }

        if (candidateUid == null) {
            candidateUid = runCatching {
                val parent = context.filesDir.parentFile
                val xmlFile = java.io.File(parent, "shared_prefs/$PREFERENCES_NAME.xml")
                if (xmlFile.exists()) {
                    val content = xmlFile.readText()
                    val match = """<string name="$KEY_USER_ID">([^<]+)</string>""".toRegex().find(content)
                    match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                } else null
            }.getOrNull()
        }

        val settings = runCatching { context.dataStore.data.first() }.getOrNull()
        val settingsEmail = settings?.get(com.darkxvenom.airbeats.constants.AccountEmailKey)?.trim()?.takeIf { it.isNotBlank() }
        val settingsName = settings?.get(com.darkxvenom.airbeats.constants.AccountNameKey)?.trim()?.takeIf { it.isNotBlank() }

        val currentName = runCatching { namePreferenceManager.userName.first().trim() }.getOrDefault("").ifBlank { settingsName.orEmpty() }
        val currentEmail = (runCatching { namePreferenceManager.accountEmail.first().normalizedEmail() }.getOrNull() ?: settingsEmail).normalizedEmail()

        // 2. Fetch remote leaderboard to match with existing stats
        val board = runCatching { AirBeatsStatsCloudClient().readBoard().getOrNull() }.getOrNull()
        val boardUsers = board?.users.orEmpty()

        if (boardUsers.isNotEmpty()) {
            // Check A: If candidateUid exists, does it match an existing leaderboard slot?
            if (!candidateUid.isNullOrBlank()) {
                val userByUid = boardUsers.firstOrNull { it.id == candidateUid }
                if (userByUid != null) {
                    persistUserId(context, preferences, candidateUid)
                    userByUid.user?.let { persistUserNumber(context, it) }
                    return candidateUid
                }
            }

            // Check B: Match by email if present
            if (!currentEmail.isNullOrBlank()) {
                val userByEmail = boardUsers.firstOrNull { it.email.normalizedEmail() == currentEmail }
                if (userByEmail != null) {
                    persistUserId(context, preferences, userByEmail.id)
                    userByEmail.user?.let { persistUserNumber(context, it) }
                    return userByEmail.id
                }
            }

            // Check C: Match by display name if not generic
            if (currentName.isNotBlank() && !currentName.equals("AirBeats User", ignoreCase = true)) {
                val userByName = boardUsers.firstOrNull { it.name.trim().equals(currentName, ignoreCase = true) }
                if (userByName != null) {
                    timber.log.Timber.i("AirBeatsStatsCloudSync: Matched existing stats slot for user '$currentName' -> ${userByName.id}")
                    persistUserId(context, preferences, userByName.id)
                    userByName.user?.let { persistUserNumber(context, it) }
                    return userByName.id
                }
            }
        }

        // 3. If candidateUid was already set locally (even if not yet on board or offline), keep it!
        if (!candidateUid.isNullOrBlank()) {
            persistUserId(context, preferences, candidateUid)
            return candidateUid
        }

        // 4. Fallback for Google account deterministic ID if available
        if (!currentEmail.isNullOrBlank()) {
            val resolved = "google-${sha256(currentEmail)}"
            persistUserId(context, preferences, resolved)
            return resolved
        }

        // 5. If truly new user and no match found, create new UID
        val generated = UUID.randomUUID().toString()
        timber.log.Timber.i("AirBeatsStatsCloudSync: No existing stats identity found. Generated new UID: $generated")
        persistUserId(context, preferences, generated)
        return generated
    }

    fun persistUserId(context: Context, preferences: android.content.SharedPreferences, userId: String) {
        preferences.edit().putString(KEY_USER_ID, userId).commit()
        runCatching {
            val f = java.io.File(context.filesDir, STATS_IDENTITY_FILENAME)
            val json = if (f.exists()) runCatching { org.json.JSONObject(f.readText()) }.getOrDefault(org.json.JSONObject()) else org.json.JSONObject()
            json.put("userId", userId)
            f.writeText(json.toString())
        }
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }

    private fun String?.normalizedEmail(): String? =
        this
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() && it != "null" }
}

