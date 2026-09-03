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
    val email: String? = null,
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
    val email: String? = null,
    val totalListenMs: Long,
    val weeklyListenMs: Long,
    val fcmToken: String? = null,
)

class AirBeatsStatsCloudClient {
    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                        .build()
                )
            }
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
                        .header("X-API-Key", API_KEY)
                        .get()
                        .build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 404) return@use GlobalStatsBoard()
                    val text = response.body?.bytes()?.let { String(it, Charsets.UTF_8) }.orEmpty()
                    if (!response.isSuccessful) error(parseError(text, response.code))
                    val wrapper = try {
                        JSONObject(text)
                    } catch (e: Exception) {
                        try {
                            val safeText = text.substringBeforeLast(",{") + "]}}"
                            JSONObject(safeText)
                        } catch (e2: Exception) {
                            JSONObject()
                        }
                    }
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
            val text = response.body?.bytes()?.let { String(it, Charsets.UTF_8) }.orEmpty()
            if (!response.isSuccessful) error(parseError(text, response.code))
        }
    }

    suspend fun uploadDaily(upload: LocalStatsUpload): Result<GlobalStatsBoard> =
        withContext(Dispatchers.IO) {
            runCatching {
                val currentGlobal = readBoard(GLOBAL_STATS_FILE).getOrThrow()
                val currentFcm = readBoard(FCM_STATS_FILE).getOrElse { GlobalStatsBoard() }
                val now = System.currentTimeMillis()
                val normalizedEmail = upload.email.normalizedEmail()
                val existingGlobalUser =
                    currentGlobal.users.firstOrNull { it.id == upload.userId }
                        ?: normalizedEmail?.let { email ->
                            currentGlobal.users.firstOrNull { it.email.normalizedEmail() == email }
                        }
                val resolvedUserId = existingGlobalUser?.id ?: upload.userId

                val globalUsers =
                    (currentGlobal.users.filterNot {
                        it.id == resolvedUserId ||
                            (normalizedEmail != null && it.email.normalizedEmail() == normalizedEmail)
                    } +
                        GlobalStatsUser(
                            id = resolvedUserId,
                            name = upload.name.ifBlank { "AirBeats User" },
                            profileUrl = upload.profileUrl,
                            email = normalizedEmail ?: existingGlobalUser?.email,
                            totalListenMs = maxOf(upload.totalListenMs.coerceAtLeast(0L), existingGlobalUser?.totalListenMs ?: 0L),
                            weeklyListenMs = upload.weeklyListenMs.coerceAtLeast(0L),
                            lastUpdatedAt = now,
                        ))
                        .sortedByDescending { it.totalListenMs }
                        .take(MAX_GLOBAL_USERS)
                        .mapIndexed { index, user -> user.copy(rank = index + 1) }

                val globalBoard = GlobalStatsBoard(users = globalUsers, updatedAt = now)
                
                val existingFcmUser =
                    currentFcm.users.firstOrNull { it.id == resolvedUserId }
                        ?: normalizedEmail?.let { email ->
                            currentFcm.users.firstOrNull { it.email.normalizedEmail() == email }
                        }
                val validNewToken = upload.fcmToken.takeIf { it != null && it != "n/v" }
                val resolvedToken = validNewToken ?: existingFcmUser?.fcmToken ?: "n/v"

                val fcmUsers = 
                    (currentFcm.users.filterNot {
                        it.id == resolvedUserId ||
                            (normalizedEmail != null && it.email.normalizedEmail() == normalizedEmail)
                    } +
                        GlobalStatsUser(
                            id = resolvedUserId,
                            name = upload.name.ifBlank { "AirBeats User" },
                            totalListenMs = maxOf(upload.totalListenMs.coerceAtLeast(0L), existingFcmUser?.totalListenMs ?: 0L),
                            rank = globalUsers.find { it.id == resolvedUserId }?.rank ?: 0,
                            fcmToken = resolvedToken,
                            lastUpdatedAt = now,
                            weeklyListenMs = 0L,
                            profileUrl = null,
                            email = normalizedEmail ?: existingFcmUser?.email,
                        ))
                        .sortedByDescending { it.totalListenMs }
                        .take(MAX_GLOBAL_USERS)

                val fcmBoard = GlobalStatsBoard(users = fcmUsers, updatedAt = now)

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
                        val email =
                            it.optString("email")
                                .trim()
                                .takeIf { value -> value.isNotBlank() && !value.equals("null", ignoreCase = true) }
                        
                        GlobalStatsUser(
                            id = parsedId,
                            name = it.optString("name", "AirBeats User"),
                            profileUrl = profileUrl,
                            email = email,
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
                                .put("email", user.email ?: JSONObject.NULL)
                                .put("fcmToken", user.fcmToken ?: JSONObject.NULL)
                                .put("listenTime", user.totalListenMs)
                                .put("rank", user.rank)
                        } else {
                            JSONObject()
                                .put("id", user.id)
                                .put("name", user.name)
                                .put("email", user.email ?: JSONObject.NULL)
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

    private fun String?.normalizedEmail(): String? =
        this
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() && it != "null" }

    private companion object {
        val BASE_URL = com.darkxvenom.airbeats.BuildConfig.STATS_BASE_URL
        val API_KEY = com.darkxvenom.airbeats.BuildConfig.STATS_API_KEY
        const val GLOBAL_STATS_FILE = "airbeats/global_stats.json"
        const val FCM_STATS_FILE = "airbeats/fcm.json"
        const val MAX_GLOBAL_USERS = 10000000
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
