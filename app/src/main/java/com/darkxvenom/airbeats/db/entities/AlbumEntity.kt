package com.darkxvenom.airbeats.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.darkxvenom.airbeats.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "album")
data class AlbumEntity(
    @PrimaryKey val id: String,
    val playlistId: String? = null,
    val title: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val themeColor: Int? = null,
    val songCount: Int,
    val duration: Int,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val bookmarkedAt: LocalDateTime? = null,
    val likedDate: LocalDateTime? = null,
    val inLibrary: LocalDateTime? = null,
) {
    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now()
    )

    fun toggleLike() = localToggleLike().also { updated ->
        val isSaved = updated.bookmarkedAt != null
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val pid = playlistId
                if (!pid.isNullOrBlank()) {
                    com.darkxvenom.airbeats.data.artist.ArtistInnertubeClient.ratePlaylist(pid.removePrefix("VL"), isSaved)
                }
            }.onFailure {
                if (playlistId != null) {
                    runCatching { YouTube.likePlaylist(playlistId, isSaved) }
                }
            }
        }
    }
}
