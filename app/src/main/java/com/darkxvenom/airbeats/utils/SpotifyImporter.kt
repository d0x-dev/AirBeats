package com.darkxvenom.airbeats.utils

import com.darkxvenom.airbeats.db.DatabaseDao
import com.darkxvenom.airbeats.db.entities.PlaylistEntity
import com.darkxvenom.airbeats.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.util.UUID
import com.darkxvenom.airbeats.models.toMediaMetadata

object SpotifyImporter {

    suspend fun importPlaylist(
        url: String,
        dao: DatabaseDao,
        onProgress: (Int, Int) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Convert URL to embed URL
            val playlistId = url.substringAfter("playlist/").substringBefore("?")
            if (playlistId.isBlank()) throw IllegalArgumentException("Invalid Spotify Playlist URL")
            val embedUrl = "https://open.spotify.com/embed/playlist/$playlistId"

            // 2. Fetch HTML using Jsoup
            val doc = Jsoup.connect(embedUrl).get()

            // 3. Extract JSON state
            val nextDataElement = doc.select("script#__NEXT_DATA__").first()
                ?: throw IllegalStateException("Could not find Spotify playlist data")
            val jsonString = nextDataElement.html()

            val json = JSONObject(jsonString)
            val entity = json.getJSONObject("props")
                .getJSONObject("pageProps")
                .getJSONObject("state")
                .getJSONObject("data")
                .getJSONObject("entity")

            val playlistName = entity.optString("name", "Imported Playlist")

            // Tracks are in trackList
            val trackListArray = entity.optJSONArray("trackList")
                ?: throw IllegalStateException("Could not find track list")

            val totalTracks = trackListArray.length()
            val songIds = mutableListOf<String>()

            // 4. Create Playlist in DB
            val newPlaylistId = UUID.randomUUID().toString()
            val playlistEntity = PlaylistEntity(
                id = newPlaylistId,
                name = playlistName,
                bookmarkedAt = java.time.LocalDateTime.now()
            )
            dao.insert(playlistEntity)

            // 5. Match songs and add to playlist
            for (i in 0 until totalTracks) {
                onProgress(i + 1, totalTracks)
                val trackObj = trackListArray.optJSONObject(i) ?: continue
                val title = trackObj.optString("title")
                val artist = trackObj.optString("subtitle")
                
                if (title.isBlank()) continue
                val query = "$title $artist"

                try {
                    // Search YouTube
                    val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                    val firstSong = searchResult?.items?.firstOrNull() as? com.darkxvenom.airbeats.innertube.models.SongItem
                    if (firstSong != null) {
                        songIds.add(firstSong.id)
                        dao.insert(firstSong.toMediaMetadata())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 6. Save songs to playlist
            // Since we only have a PlaylistEntity, we need to convert to Playlist model, 
            // but addSongToPlaylist takes a Playlist object which is complex.
            // Let's just insert PlaylistSongMap directly.
            songIds.forEachIndexed { index, songId ->
                dao.insert(
                    com.darkxvenom.airbeats.db.entities.PlaylistSongMap(
                        songId = songId,
                        playlistId = newPlaylistId,
                        position = index
                    )
                )
            }

            playlistName
        }
    }
}
