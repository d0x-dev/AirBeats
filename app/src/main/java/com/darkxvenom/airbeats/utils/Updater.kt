package com.darkxvenom.airbeats.utils

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import org.json.JSONObject

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set

    suspend fun getLatestVersionName(): Result<String> =
        runCatching {
            if (com.darkxvenom.airbeats.BuildConfig.IS_NIGHTLY) {
                val response = client.get("https://api.github.com/repos/d0x-dev/AirBeats/releases").bodyAsText()
                val jsonArray = org.json.JSONArray(response)
                var versionName = ""
                for (i in 0 until jsonArray.length()) {
                    val release = jsonArray.getJSONObject(i)
                    if (release.getBoolean("prerelease")) {
                        versionName = release.getString("tag_name").removePrefix("v").removeSuffix("-nightly").trim()
                        break
                    }
                }
                lastCheckTime = System.currentTimeMillis()
                versionName
            } else {
                val response = client.get("https://api.github.com/repos/d0x-dev/AirBeats/releases/latest").bodyAsText()
                val json = JSONObject(response)
                val versionName = json.getString("tag_name").removePrefix("v").trim()
                lastCheckTime = System.currentTimeMillis()
                versionName
            }
        }
}
