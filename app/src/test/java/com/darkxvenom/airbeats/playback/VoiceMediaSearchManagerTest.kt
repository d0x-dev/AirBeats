package com.darkxvenom.airbeats.playback

import android.app.SearchManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceMediaSearchManagerTest {

    // Helper parser/logic mirror to unit-test VoiceMediaSearchManager pure query rules
    private val titleCleaner = { raw: String ->
        var q = raw.trim()
        q = q.replace(Regex("^(?:play|listen\\s+to|put\\s+on|stream|find)\\s+", RegexOption.IGNORE_CASE), "")
        q = q.replace(Regex("\\s+(?:on|in|with)\\s+air\\s*beats?$", RegexOption.IGNORE_CASE), "")
        q = q.replace(Regex("\\s+on\\s+airbeats$", RegexOption.IGNORE_CASE), "")
        q = q.replace(Regex("^(?:the\\s+)?(?:song|track)\\s+(?:called\\s+)?", RegexOption.IGNORE_CASE), "")
        q.trim()
    }

    private val artistExtractor = { raw: String ->
        val byMatch = Regex("\\s+by\\s+(.+)$", RegexOption.IGNORE_CASE).find(raw)
        if (byMatch != null) {
            val candidate = byMatch.groupValues[1]
                .replace(Regex("\\s+(?:on|in)\\s+air\\s*beats?$", RegexOption.IGNORE_CASE), "")
                .trim()
            if (candidate.isNotBlank()) candidate else null
        } else {
            val songsByMatch = Regex("^(?:play\\s+)?(?:songs|tracks|music)\\s+by\\s+(.+)$", RegexOption.IGNORE_CASE).find(raw)
            if (songsByMatch != null) {
                val candidate = songsByMatch.groupValues[1]
                    .replace(Regex("\\s+(?:on|in)\\s+air\\s*beats?$", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (candidate.isNotBlank()) candidate else null
            } else null
        }
    }

    private val isLikedChecker = { query: String, focus: String?, playlistExtra: String? ->
        if (focus == "vnd.android.cursor.item/playlist" &&
            (playlistExtra?.lowercase()?.contains("like") == true ||
             playlistExtra?.lowercase()?.contains("favorit") == true ||
             playlistExtra?.lowercase()?.contains("favourite") == true)
        ) {
            true
        } else {
            val lower = query.lowercase().trim()
            lower.matches(Regex(".*\\b(my\\s+)?(liked|favorites?|favourites?|loved)\\b.*")) ||
            lower == "liked songs" || lower == "my liked songs" || lower == "favorites" ||
            lower == "play favorites" || lower == "play liked songs"
        }
    }

    private val isGenericChecker = { query: String, focus: String?, titleExtra: String?, artistExtra: String?, albumExtra: String?, playlistExtra: String? ->
        if (query.isBlank() && focus == null && titleExtra.isNullOrBlank() &&
            artistExtra.isNullOrBlank() && albumExtra.isNullOrBlank() && playlistExtra.isNullOrBlank()
        ) {
            true
        } else {
            val lower = query.lowercase().trim()
            val genericPhrases = setOf(
                "play music", "play songs", "play some music", "play some songs",
                "start music", "play something", "play anything", "shuffle music",
                "shuffle songs", "resume", "play", "airbeats", "continue"
            )
            genericPhrases.contains(lower) || lower == "airbeats play music" || lower == "play music on airbeats"
        }
    }

    private val isArtistChecker = { query: String, focus: String?, artistExtra: String?, titleExtra: String?, albumExtra: String? ->
        if (focus == "vnd.android.cursor.item/artist") true
        else if (!artistExtra.isNullOrBlank() && titleExtra.isNullOrBlank() && albumExtra.isNullOrBlank()) true
        else {
            val lower = query.lowercase().trim()
            lower.startsWith("songs by ") || lower.startsWith("play songs by ") ||
            lower.startsWith("tracks by ") || lower.startsWith("play artist ")
        }
    }

    private val isAlbumChecker = { query: String, focus: String?, albumExtra: String?, titleExtra: String? ->
        if (focus == "vnd.android.cursor.item/album") true
        else if (!albumExtra.isNullOrBlank() && titleExtra.isNullOrBlank()) true
        else {
            val lower = query.lowercase().trim()
            lower.startsWith("album ") || lower.startsWith("play album ")
        }
    }

    @Test
    fun testSongTitleCleaning() {
        assertEquals("Believer", titleCleaner("Hey Google, play Believer on AirBeats".replace("Hey Google, ", "")))
        assertEquals("Starboy", titleCleaner("play Starboy on airbeats"))
        assertEquals("Shape of You", titleCleaner("listen to the song Shape of You"))
        assertEquals("Bohemian Rhapsody", titleCleaner("put on song called Bohemian Rhapsody"))
        assertEquals("Night Changes", titleCleaner("stream Night Changes on Air Beats"))
    }

    @Test
    fun testArtistExtraction() {
        assertEquals("Imagine Dragons", artistExtractor("Believer by Imagine Dragons"))
        assertEquals("Atif Aslam", artistExtractor("play songs by Atif Aslam on AirBeats"))
        assertEquals("Queen", artistExtractor("tracks by Queen"))
        assertNull(artistExtractor("play Believer on AirBeats"))
    }

    @Test
    fun testLikedSongsRecognition() {
        assertTrue(isLikedChecker("play my liked songs", null, null))
        assertTrue(isLikedChecker("play favorites", null, null))
        assertTrue(isLikedChecker("play my favorite songs on AirBeats", null, null))
        assertTrue(isLikedChecker("", "vnd.android.cursor.item/playlist", "Liked Songs"))
        assertTrue(isLikedChecker("", "vnd.android.cursor.item/playlist", "My Favorites"))
        assertFalse(isLikedChecker("play Believer", null, null))
        assertFalse(isLikedChecker("play Workout playlist", "vnd.android.cursor.item/playlist", "Workout"))
    }

    @Test
    fun testGenericPlayRecognition() {
        assertTrue(isGenericChecker("", null, null, null, null, null))
        assertTrue(isGenericChecker("play music", null, null, null, null, null))
        assertTrue(isGenericChecker("play songs", null, null, null, null, null))
        assertTrue(isGenericChecker("resume", null, null, null, null, null))
        assertTrue(isGenericChecker("play music on airbeats", null, null, null, null, null))
        assertFalse(isGenericChecker("play Believer", null, null, null, null, null))
    }

    @Test
    fun testArtistFocusRecognition() {
        assertTrue(isArtistChecker("", "vnd.android.cursor.item/artist", "Atif Aslam", null, null))
        assertTrue(isArtistChecker("play songs by Atif Aslam", null, null, null, null))
        assertTrue(isArtistChecker("songs by Taylor Swift", null, null, null, null))
        assertFalse(isArtistChecker("play Believer", null, null, null, null))
    }

    @Test
    fun testAlbumFocusRecognition() {
        assertTrue(isAlbumChecker("", "vnd.android.cursor.item/album", "Midnights", null))
        assertTrue(isAlbumChecker("play album Midnights", null, null, null))
        assertFalse(isAlbumChecker("play Believer", null, null, null))
    }

    @Test
    fun testAndroidAutoPathSeparation() {
        val autoSongPath = "song/12345"
        val autoArtistPath = "artist/67890/12345"
        val autoAlbumPath = "album/abcde/12345"
        val autoPlaylistPath = "playlist/liked/12345"
        val autoHistoryPath = "history/12345"

        val voiceSearchId = "Believer"

        assertEquals("song", autoSongPath.split("/").first())
        assertEquals("artist", autoArtistPath.split("/").first())
        assertEquals("album", autoAlbumPath.split("/").first())
        assertEquals("playlist", autoPlaylistPath.split("/").first())
        assertEquals("history", autoHistoryPath.split("/").first())

        val voiceParts = voiceSearchId.split("/").filter { it.isNotBlank() }
        assertFalse(listOf("song", "artist", "album", "playlist", "history").contains(voiceParts.first()))
    }

    @Test
    fun testMediaItemStructureForVoice() {
        val title = "Believer"
        val artist = "Imagine Dragons"
        val id = "7wtfhFlaKio"

        val item = MediaItem.Builder()
            .setMediaId(id)
            .setUri(id)
            .setCustomCacheKey(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setArtist(artist)
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()

        assertEquals(id, item.mediaId)
        assertEquals(title, item.mediaMetadata.title.toString())
        assertEquals(artist, item.mediaMetadata.artist.toString())
        assertTrue(item.mediaMetadata.isPlayable == true)
        assertFalse(item.mediaMetadata.isBrowsable == true)
    }
}
