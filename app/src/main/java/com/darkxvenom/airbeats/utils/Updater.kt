package com.darkxvenom.airbeats.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String = "",
    val releaseUrl: String = "https://github.com/d0x-dev/AirBeats/releases/latest"
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    suspend fun getLatestUpdateInfo(): Result<UpdateInfo> =
        runCatching {
            if (com.darkxvenom.airbeats.BuildConfig.IS_NIGHTLY) {
                val response = client.get("https://api.github.com/repos/d0x-dev/AirBeats/releases").bodyAsText()
                val jsonArray = org.json.JSONArray(response)
                var versionName = ""
                var releaseNotes = ""
                var releaseUrl = "https://github.com/d0x-dev/AirBeats/releases"
                for (i in 0 until jsonArray.length()) {
                    val release = jsonArray.getJSONObject(i)
                    if (release.getBoolean("prerelease")) {
                        versionName = release.getString("tag_name").removePrefix("v").removeSuffix("-nightly").trim()
                        releaseNotes = release.optString("body", "").trim()
                        releaseUrl = release.optString("html_url", "https://github.com/d0x-dev/AirBeats/releases")
                        break
                    }
                }
                lastCheckTime = System.currentTimeMillis()
                UpdateInfo(versionName, releaseNotes, releaseUrl)
            } else {
                val response = client.get("https://api.github.com/repos/d0x-dev/AirBeats/releases/latest").bodyAsText()
                val json = JSONObject(response)
                val versionName = json.getString("tag_name").removePrefix("v").trim()
                val releaseNotes = json.optString("body", "").trim()
                val releaseUrl = json.optString("html_url", "https://github.com/d0x-dev/AirBeats/releases/latest")
                lastCheckTime = System.currentTimeMillis()
                UpdateInfo(versionName, releaseNotes, releaseUrl)
            }
        }

    suspend fun getLatestVersionName(): Result<String> =
        getLatestUpdateInfo().map { it.versionName }
}
