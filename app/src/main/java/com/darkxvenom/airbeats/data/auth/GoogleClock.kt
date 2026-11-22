/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of AirBeats and is licensed under GPL-3.0.
 */

package com.darkxvenom.airbeats.data.auth

import com.darkxvenom.airbeats.backend.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object GoogleClock {
    @Volatile private var offsetMs: Long = 0L
    @Volatile private var synced = false

    val offsetSeconds: Long get() = offsetMs / 1000L
    val isDeviceClockOff: Boolean get() = synced && abs(offsetMs) > 120_000L

    fun nowSeconds(): Long = (System.currentTimeMillis() + offsetMs) / 1000L

    fun syncFromDateHeader(date: String?) {
        if (date.isNullOrBlank()) return
        val server = runCatching {
            ZonedDateTime.parse(date.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        }.getOrNull() ?: return
        offsetMs = server - System.currentTimeMillis()
        synced = true
    }

    suspend fun ensureSynced() {
        if (synced) return
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url("https://www.google.com/generate_204").head().build()
                Net.client.newCall(req).execute().use { syncFromDateHeader(it.header("Date")) }
            }
        }
    }
}
