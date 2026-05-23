package com.darkxvenom.airbeats.models

import android.net.Uri

data class LocalSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // milliseconds
    val uri: Uri,
    val albumArtUri: Uri?,
    val size: Long,
    val dateAdded: Long,
) {
    val durationText: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}