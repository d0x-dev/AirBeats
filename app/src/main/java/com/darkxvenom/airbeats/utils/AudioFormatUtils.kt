package com.darkxvenom.airbeats.utils

import java.util.Locale

/**
 * Defensive formatting helpers for audio stream metadata, file sizes, and playback durations.
 */
object AudioFormatUtils {

    /**
     * Formats a duration in milliseconds into `M:SS` or `H:MM:SS`.
     * Safely guards against negative or unset values (such as ExoPlayer's `TIME_UNSET`).
     */
    fun formatDurationMs(durationMs: Long): String {
        if (durationMs <= 0L || durationMs == Long.MIN_VALUE || durationMs == Long.MAX_VALUE) {
            return "0:00"
        }
        val totalSeconds = durationMs / 1000L
        val seconds = totalSeconds % 60L
        val minutes = (totalSeconds / 60L) % 60L
        val hours = totalSeconds / 3600L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats a bitrate in bits per second (bps) into a human-readable `kbps` label.
     */
    fun formatBitrateBps(bitrateBps: Int?): String? {
        if (bitrateBps == null || bitrateBps <= 0) return null
        val kbps = (bitrateBps + 500) / 1000
        return if (kbps > 0) "$kbps kbps" else "$bitrateBps bps"
    }

    /**
     * Formats a sample rate in Hz into a clean `kHz` string (e.g., `44.1 kHz` or `48 kHz`).
     */
    fun formatSampleRateHz(sampleRateHz: Int?): String? {
        if (sampleRateHz == null || sampleRateHz <= 0) return null
        return if (sampleRateHz % 1000 == 0) {
            "${sampleRateHz / 1000} kHz"
        } else {
            String.format(Locale.US, "%.1f kHz", sampleRateHz / 1000.0)
        }
    }

    /**
     * Formats a byte count into a human-readable `KB`, `MB`, or `GB` string.
     */
    fun formatFileSizeBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
