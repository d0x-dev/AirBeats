package com.darkxvenom.airbeats.utils

import com.darkxvenom.airbeats.BuildConfig

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
    private val baseUrl = BuildConfig.AIRBEATS_DATABASE_URL.trim().trimEnd('/')
    private val apiKey = BuildConfig.AIRBEATS_DATABASE_API_KEY.trim()
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    suspend fun readBoard(): Result<GlobalStatsBoard> =
        withContext(Dispatchers.IO) {
            runCatching {
                val configuredBaseUrl = requireBaseUrl()
                val request =
                    Request
                        .Builder()
                        .url("$configuredBaseUrl/read?file=$GLOBAL_STATS_FILE&_t=${System.currentTimeMillis()}")
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

    suspend fun uploadDaily(upload: LocalStatsUpload): Result<GlobalStatsBoard> =
        withContext(Dispatchers.IO) {
            runCatching {
                val configuredBaseUrl = requireBaseUrl()
                val configuredApiKey = requireApiKey()
                val current = readBoard().getOrThrow()
                val now = System.currentTimeMillis()
                val existingUser = current.users.find { it.id == upload.userId }
                val users =
                    (current.users.filterNot { it.id == upload.userId } +
                        GlobalStatsUser(
                            id = upload.userId,
                            name = upload.name.ifBlank { "AirBeats User" },
                            profileUrl = upload.profileUrl,
                            totalListenMs = upload.totalListenMs.coerceAtLeast(0L),
                            weeklyListenMs = upload.weeklyListenMs.coerceAtLeast(0L),
                            lastUpdatedAt = now,
                            fcmToken = upload.fcmToken ?: existingUser?.fcmToken,
                        ))
                        .sortedByDescending { it.totalListenMs }
                        .take(MAX_GLOBAL_USERS)
                        .mapIndexed { index, user -> user.copy(rank = index + 1) }

                val board = GlobalStatsBoard(users = users, updatedAt = now)
                val request =
                    Request
                        .Builder()
                        .url("$configuredBaseUrl/write?file=$GLOBAL_STATS_FILE")
                        .addHeader("X-API-Key", configuredApiKey)
                        .post(board.toJson().toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()
                client.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) error(parseError(text, response.code))
                }
                board
            }
        }

    private fun parseBoard(json: JSONObject): GlobalStatsBoard {
        val usersJson = json.optJSONArray("users") ?: JSONArray()
        val users =
            List(usersJson.length()) { index -> usersJson.optJSONObject(index) }
                .mapNotNull { user ->
                    user?.let {
                        GlobalStatsUser(
                            id = it.optString("id"),
                            name = it.optString("name", "AirBeats User"),
                            profileUrl = it.optString("profileUrl").takeIf(String::isNotBlank),
                            totalListenMs = it.optLong("totalListenMs"),
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

    private fun GlobalStatsBoard.toJson(): JSONObject =
        JSONObject()
            .put("service", "AirBeats Global Stats")
            .put("folder", "airbeats")
            .put("updatedAt", updatedAt)
            .put(
                "users",
                JSONArray(
                    users.map { user ->
                        JSONObject()
                            .put("id", user.id)
                            .put("name", user.name)
                            .put("profileUrl", user.profileUrl)
                            .put("totalListenMs", user.totalListenMs)
                            .put("weeklyListenMs", user.weeklyListenMs)
                            .put("lastUpdatedAt", user.lastUpdatedAt)
                            .put("rank", user.rank)
                            .put("fcmToken", user.fcmToken)
                    },
                ),
            )

    private fun parseError(text: String, code: Int): String =
        runCatching { JSONObject(text).optString("error").ifBlank { "HTTP $code" } }
            .getOrDefault("HTTP $code")

    private fun requireBaseUrl(): String =
        baseUrl.takeIf { it.isNotBlank() } ?: error("AirBeats database URL is not configured")

    private fun requireApiKey(): String =
        apiKey.takeIf { it.isNotBlank() } ?: error("AirBeats database API key is not configured")

    companion object {
        val isConfigured: Boolean
            get() =
                BuildConfig.AIRBEATS_DATABASE_URL.isNotBlank() &&
                    BuildConfig.AIRBEATS_DATABASE_API_KEY.isNotBlank()

        const val GLOBAL_STATS_FILE = "airbeats/global_stats.json"
        private const val MAX_GLOBAL_USERS = 1000
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
