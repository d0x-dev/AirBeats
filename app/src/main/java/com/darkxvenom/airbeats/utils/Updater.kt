package com.darkxvenom.airbeats.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONArray
import org.json.JSONObject

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String = "",
    val releaseUrl: String = "https://github.com/d0x-dev/AirBeats/releases/latest",
    val apkDownloadUrl: String = ""
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    private fun extractApkUrl(releaseObj: JSONObject, versionName: String): String {
        val assets = releaseObj.optJSONArray("assets")
        if (assets != null && assets.length() > 0) {
            var fallbackApkUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "")
                val url = asset.optString("browser_download_url", "")
                if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                    if (name.contains("universal", ignoreCase = true) ||
                        name.contains("signed", ignoreCase = true) ||
                        name.contains("release", ignoreCase = true) ||
                        name.contains("airbeats", ignoreCase = true)
                    ) {
                        return url
                    }
                    if (fallbackApkUrl.isBlank()) {
                        fallbackApkUrl = url
                    }
                }
            }
            if (fallbackApkUrl.isNotBlank()) {
                return fallbackApkUrl
            }
        }

        // Direct download fallback URL based on build type & tag
        return if (com.darkxvenom.airbeats.BuildConfig.IS_NIGHTLY) {
            "https://github.com/d0x-dev/AirBeats/releases/download/v${versionName}-nightly/Airbeats-v${versionName}-Nightly.apk"
        } else {
            "https://github.com/d0x-dev/AirBeats/releases/download/v$versionName/AirBeats_v${versionName}_signed.apk"
        }
    }

    suspend fun getLatestUpdateInfo(): Result<UpdateInfo> =
        runCatching {
            if (com.darkxvenom.airbeats.BuildConfig.IS_NIGHTLY) {
                val response = client.get("https://api.github.com/repos/d0x-dev/AirBeats/releases").bodyAsText()
                val jsonArray = JSONArray(response)
                var versionName = ""
                var releaseNotes = ""
                var releaseUrl = "https://github.com/d0x-dev/AirBeats/releases"
                var apkDownloadUrl = ""
                for (i in 0 until jsonArray.length()) {
                    val release = jsonArray.getJSONObject(i)
                    if (release.getBoolean("prerelease")) {
                        versionName = release.getString("tag_name").removePrefix("v").removeSuffix("-nightly").trim()
                        releaseNotes = release.optString("body", "").trim()
                        releaseUrl = release.optString("html_url", "https://github.com/d0x-dev/AirBeats/releases")
                        apkDownloadUrl = extractApkUrl(release, versionName)
                        break
                    }
                }
                lastCheckTime = System.currentTimeMillis()
                UpdateInfo(versionName, releaseNotes, releaseUrl, apkDownloadUrl)
            } else {
                val response = client.get("https://api.github.com/repos/d0x-dev/AirBeats/releases/latest").bodyAsText()
                val json = JSONObject(response)
                val versionName = json.getString("tag_name").removePrefix("v").trim()
                val releaseNotes = json.optString("body", "").trim()
                val releaseUrl = json.optString("html_url", "https://github.com/d0x-dev/AirBeats/releases/latest")
                val apkDownloadUrl = extractApkUrl(json, versionName)
                lastCheckTime = System.currentTimeMillis()
                UpdateInfo(versionName, releaseNotes, releaseUrl, apkDownloadUrl)
            }
        }

    suspend fun getLatestVersionName(): Result<String> =
        getLatestUpdateInfo().map { it.versionName }
}
