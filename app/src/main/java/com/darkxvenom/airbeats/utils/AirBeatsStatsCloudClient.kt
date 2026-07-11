package com.darkxvenom.airbeats.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GlobalStatsUser(
    val id: String,
    val name: String,
    val profileUrl: String?,
    val totalListenMs: Long,
    val weeklyListenMs: Long,
    val lastUpdatedAt: Long,
    val rank: Int = 0,
    val fcmToken: String? = null,
)

data class GlobalStatsBoard(
    val users: List<GlobalStatsUser> = emptyList(),
    val updatedAt: Long = 0L,
)

data class LocalStatsUpload(
    val userId: String,
    val name: String,
    val profileUrl: String?,
    val totalListenMs: Long,
    val weeklyListenMs: Long,
    val fcmToken: String? = null,
)

class AirBeatsStatsCloudClient {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    suspend fun readBoard(fileName: String = GLOBAL_STATS_FILE): Result<GlobalStatsBoard> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request =
                    Request
                        .Builder()
                        .url("$BASE_URL/read?file=$fileName&_t=${System.currentTimeMillis()}")
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .get()
                        .build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 404) return@use GlobalStatsBoard()
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error(parseError(text, response.code))
                    val wrapper = JSONObject(text)
                    parseBoard(wrapper.optJSONObject("data") ?: wrapper)
                }
            }
        }

    private fun writeBoard(fileName: String, json: JSONObject) {
        val request =
            Request
                .Builder()
                .url("$BASE_URL/write?file=$fileName")
                .addHeader("X-API-Key", API_KEY)
                .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) error(parseError(text, response.code))
        }
    }

    suspend fun uploadDaily(upload: LocalStatsUpload): Result<GlobalStatsBoard> =
        withContext(Dispatchers.IO) {
            runCatching {
                val currentGlobal = readBoard(GLOBAL_STATS_FILE).getOrThrow()
                val currentFcm = readBoard(FCM_STATS_FILE).getOrElse { GlobalStatsBoard() }
                val now = System.currentTimeMillis()

                // 1. Process Global Stats
                val globalUsers =
                    (currentGlobal.users.filterNot { it.id == upload.userId } +
                        GlobalStatsUser(
                            id = upload.userId,
                            name = upload.name.ifBlank { "AirBeats User" },
                            profileUrl = upload.profileUrl,
                            totalListenMs = upload.totalListenMs.coerceAtLeast(0L),
                            weeklyListenMs = upload.weeklyListenMs.coerceAtLeast(0L),
                            lastUpdatedAt = now,
                        ))
                        .sortedByDescending { it.totalListenMs }
                        .take(MAX_GLOBAL_USERS)
                        .mapIndexed { index, user -> user.copy(rank = index + 1) }

                val globalBoard = GlobalStatsBoard(users = globalUsers, updatedAt = now)
                
                // 2. Process FCM Stats
                val existingFcmUser = currentFcm.users.find { it.id == upload.userId }
                val validNewToken = upload.fcmToken.takeIf { it != null && it != "n/v" }
                val resolvedToken = validNewToken ?: existingFcmUser?.fcmToken ?: "n/v"

                val fcmUsers = 
                    (currentFcm.users.filterNot { it.id == upload.userId } +
                        GlobalStatsUser(
                            id = upload.userId,
                            name = upload.name.ifBlank { "AirBeats User" },
                            totalListenMs = upload.totalListenMs.coerceAtLeast(0L),
                            rank = globalUsers.find { it.id == upload.userId }?.rank ?: 0,
                            fcmToken = resolvedToken,
                            lastUpdatedAt = now,
                            weeklyListenMs = 0L,
                            profileUrl = null
                        ))
                        .sortedByDescending { it.totalListenMs }
                        .take(MAX_GLOBAL_USERS)

                val fcmBoard = GlobalStatsBoard(users = fcmUsers, updatedAt = now)

                // 3. Upload Both
                writeBoard(GLOBAL_STATS_FILE, globalBoard.toJson(isFcmFile = false))
                writeBoard(FCM_STATS_FILE, fcmBoard.toJson(isFcmFile = true))

                globalBoard
            }
        }

    private fun parseBoard(json: JSONObject): GlobalStatsBoard {
        val usersJson = json.optJSONArray("users") ?: JSONArray()
        val users =
            List(usersJson.length()) { index -> usersJson.optJSONObject(index) }
                .mapNotNull { user ->
                    user?.let {
                        val parsedId = it.optString("id").ifBlank { it.optString("uuid") }
                        if (parsedId.isBlank()) return@let null
                        val profileUrl =
                            it.optString("profileUrl")
                                .trim()
                                .takeIf { value -> value.isNotBlank() && !value.equals("null", ignoreCase = true) }
                        
                        GlobalStatsUser(
                            id = parsedId,
                            name = it.optString("name", "AirBeats User"),
                            profileUrl = profileUrl,
                            totalListenMs = it.optLong("totalListenMs").takeIf { v -> v > 0 } ?: it.optLong("listenTime"),
                            weeklyListenMs = it.optLong("weeklyListenMs"),
                            lastUpdatedAt = it.optLong("lastUpdatedAt"),
                            rank = it.optInt("rank"),
                            fcmToken = it.optString("fcmToken").takeIf(String::isNotBlank),
                        )
                    }
                }
                .sortedByDescending { it.totalListenMs }
                .take(MAX_GLOBAL_USERS)
                .mapIndexed { index, user -> user.copy(rank = index + 1) }
        return GlobalStatsBoard(users = users, updatedAt = json.optLong("updatedAt"))
    }

    private fun GlobalStatsBoard.toJson(isFcmFile: Boolean): JSONObject =
        JSONObject()
            .put("service", if (isFcmFile) "AirBeats FCM Stats" else "AirBeats Global Stats")
            .put("folder", "airbeats")
            .put("updatedAt", updatedAt)
            .put(
                "users",
                JSONArray(
                    users.map { user ->
                        if (isFcmFile) {
                            JSONObject()
                                .put("uuid", user.id)
                                .put("name", user.name)
                                .put("fcmToken", user.fcmToken ?: JSONObject.NULL)
                                .put("listenTime", user.totalListenMs)
                                .put("rank", user.rank)
                        } else {
                            JSONObject()
                                .put("id", user.id)
                                .put("name", user.name)
                                .put("profileUrl", user.profileUrl ?: JSONObject.NULL)
                                .put("totalListenMs", user.totalListenMs)
                                .put("weeklyListenMs", user.weeklyListenMs)
                                .put("lastUpdatedAt", user.lastUpdatedAt)
                                .put("rank", user.rank)
                        }
                    },
                ),
            )

    private fun parseError(text: String, code: Int): String =
        runCatching { JSONObject(text).optString("error").ifBlank { "HTTP $code" } }
            .getOrDefault("HTTP $code")

    private companion object {
        const val BASE_URL = "https://database.ispro.in"
        const val API_KEY = "WASDARK-DEEPX-STORMX"
        const val GLOBAL_STATS_FILE = "airbeats/global_stats.json"
        const val FCM_STATS_FILE = "airbeats/fcm.json"
        const val MAX_GLOBAL_USERS = 10000000
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
