/*
 * AirBeats Project (2026)
 * Native Android Media & Google Assistant / Gemini Voice Integration
 */

package com.darkxvenom.airbeats.playback

import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.darkxvenom.airbeats.constants.EnableJioSaavnKey
import com.darkxvenom.airbeats.constants.HideExplicitKey
import com.darkxvenom.airbeats.constants.MusicProviderKey
import com.darkxvenom.airbeats.constants.SongSortType
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.extensions.mediaItems
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.filterExplicit
import com.darkxvenom.airbeats.jiosaavn.JioSaavnApi
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.ListQueue
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.providers.ProviderSong
import com.darkxvenom.airbeats.songs.IdentifiedSong
import com.darkxvenom.airbeats.songs.SongMatchEngine
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.getSuspend
import com.darkxvenom.airbeats.voice.VoiceCommandParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles structured and unstructured voice media search requests from Google Assistant,
 * Gemini, or Android system media-session controllers, mapping them to existing providers,
 * the local database, and playback queues without breaking Android Auto or manual playback.
 */
@Singleton
class VoiceMediaSearchManager
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) {

    /**
     * Resolves an incoming [MediaItem] containing search metadata (query or MediaStore extras)
     * into a populated [MediaSession.MediaItemsWithStartPosition] for playback.
     */
    suspend fun resolveSearchToMediaItems(
        item: MediaItem,
        player: Player?,
        defaultResult: MediaSession.MediaItemsWithStartPosition,
    ): MediaSession.MediaItemsWithStartPosition = withContext(Dispatchers.IO) {
        try {
            val requestMetadata = item.requestMetadata
            val extras = requestMetadata.extras ?: Bundle.EMPTY
            val rawQuery = requestMetadata.searchQuery ?: extras.getString(SearchManager.QUERY) ?: ""
            val focus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS)
            val titleExtra = extras.getString(MediaStore.EXTRA_MEDIA_TITLE)?.trim()
            val artistExtra = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)?.trim()
            val albumExtra = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)?.trim()
            val playlistExtra = extras.getString(MediaStore.EXTRA_MEDIA_PLAYLIST)?.trim()
            val genreExtra = extras.getString(MediaStore.EXTRA_MEDIA_GENRE)?.trim()

            Timber.d(
                "VoiceMediaSearchManager resolving query='%s', focus='%s', title='%s', artist='%s', album='%s', playlist='%s'",
                rawQuery,
                focus,
                titleExtra,
                artistExtra,
                albumExtra,
                playlistExtra
            )

            // 1. Check for Liked Songs / Playlist requests
            if (isLikedRequest(rawQuery, focus, playlistExtra)) {
                val likedSongs = database.likedSongs(SongSortType.CREATE_DATE, descending = true).first()
                if (likedSongs.isNotEmpty()) {
                    Timber.d("VoiceMediaSearchManager resolved liked songs request (%d items)", likedSongs.size)
                    syncServiceQueue(ListQueue("Liked Songs", likedSongs.map { it.toMediaItem() }))
                    return@withContext MediaSession.MediaItemsWithStartPosition(
                        likedSongs.map { it.toMediaItem() },
                        0,
                        0L
                    )
                }
            } else if (focus == MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE || !playlistExtra.isNullOrBlank()) {
                val targetPlaylistName = playlistExtra ?: titleExtra ?: rawQuery
                val playlistResult = resolvePlaylistRequest(targetPlaylistName)
                if (playlistResult != null) return@withContext playlistResult
            }

            // 2. Check for Generic Music / Playback Resumption requests
            if (isGenericPlayRequest(rawQuery, focus, titleExtra, artistExtra, albumExtra, playlistExtra)) {
                return@withContext resolvePlaybackResumption(player)
            }

            // 3. Check for Artist requests
            if (isArtistRequest(rawQuery, focus, artistExtra, titleExtra, albumExtra)) {
                val artistName = (artistExtra ?: extractArtistFromQuery(rawQuery) ?: rawQuery).trim()
                if (artistName.isNotBlank()) {
                    val artistResult = resolveArtistRequest(artistName)
                    if (artistResult != null) return@withContext artistResult
                }
            }

            // 4. Check for Album requests
            if (isAlbumRequest(rawQuery, focus, albumExtra, titleExtra)) {
                val albumName = (albumExtra ?: titleExtra ?: rawQuery).trim()
                if (albumName.isNotBlank()) {
                    val albumResult = resolveAlbumRequest(albumName, artistExtra)
                    if (albumResult != null) return@withContext albumResult
                }
            }

            // 5. Specific Song Search (Title + optional Artist, with fallback)
            val cleanTitle = titleExtra ?: cleanTitleFromQuery(rawQuery)
            val cleanArtist = artistExtra ?: extractArtistFromQuery(rawQuery)

            val songResult = resolveSongRequest(cleanTitle, cleanArtist, rawQuery)
            if (songResult != null) {
                return@withContext songResult
            }

            Timber.w("VoiceMediaSearchManager found no matching results for query: '%s'", rawQuery)
            defaultResult
        } catch (e: Exception) {
            Timber.e(e, "VoiceMediaSearchManager error during voice search resolution")
            defaultResult
        }
    }

    /**
     * Resolves generic play or playback resumption requests ("Hey Google, resume AirBeats",
     * "Hey Google, play music on AirBeats").
     */
    suspend fun resolvePlaybackResumption(
        player: Player?,
    ): MediaSession.MediaItemsWithStartPosition = withContext(Dispatchers.IO) {
        // A. If the authoritative player already has media items loaded, resume them
        if (player != null && player.mediaItemCount > 0) {
            val safeIndex = player.currentMediaItemIndex.coerceIn(0, player.mediaItemCount - 1)
            val safePosition = player.currentPosition.coerceAtLeast(0L)
            Timber.d("VoiceMediaSearchManager resuming existing player queue at index %d, pos %d", safeIndex, safePosition)
            return@withContext MediaSession.MediaItemsWithStartPosition(
                player.mediaItems,
                safeIndex,
                safePosition
            )
        }

        // B. If player has no queue, check user's listening history
        try {
            val recentEvents = database.events().first()
            val recentSongs = recentEvents.map { it.song }.distinctBy { it.id }
            if (recentSongs.isNotEmpty()) {
                Timber.d("VoiceMediaSearchManager resuming playback from user history (%d items)", recentSongs.size)
                val mediaItems = recentSongs.map { it.toMediaItem() }
                syncServiceQueue(ListQueue("History", mediaItems))
                return@withContext MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load listening history for resumption")
        }

        // C. Check user's liked songs
        try {
            val liked = database.likedSongs(SongSortType.PLAY_TIME, descending = true).first()
            if (liked.isNotEmpty()) {
                Timber.d("VoiceMediaSearchManager resuming playback from liked songs (%d items)", liked.size)
                val mediaItems = liked.map { it.toMediaItem() }
                syncServiceQueue(ListQueue("Liked Songs", mediaItems))
                return@withContext MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load liked songs for resumption")
        }

        // D. Check user's saved/cached library songs
        try {
            val librarySongs = database.songs(SongSortType.PLAY_TIME, descending = true).first()
            if (librarySongs.isNotEmpty()) {
                Timber.d("VoiceMediaSearchManager resuming playback from library songs (%d items)", librarySongs.size)
                val mediaItems = librarySongs.map { it.toMediaItem() }
                syncServiceQueue(ListQueue("Library", mediaItems))
                return@withContext MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load library songs for resumption")
        }

        // E. Online trending recommendation fallback
        try {
            val hideExplicit = context.dataStore.getSuspend(HideExplicitKey, false)
            val searchResult = YouTube.search("Trending Songs", YouTube.SearchFilter.FILTER_SONG).getOrNull()
            val trendingSongs = searchResult?.items
                ?.filterIsInstance<SongItem>()
                ?.filterExplicit(hideExplicit)

            if (!trendingSongs.isNullOrEmpty()) {
                val seed = trendingSongs.first()
                val metadata = seed.toMediaMetadata()
                val queue = YouTubeQueue(WatchEndpoint(seed.id), metadata)
                syncServiceQueue(queue)
                val mediaItems = trendingSongs.map { it.toMediaItem() }
                return@withContext MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load online trending songs for resumption")
        }

        MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L)
    }

    /**
     * Resolves a specific song search request across local database and active online providers.
     */
    private suspend fun resolveSongRequest(
        title: String,
        artist: String?,
        rawQuery: String,
    ): MediaSession.MediaItemsWithStartPosition? {
        val normalizedTitle = VoiceCommandParser.normalizeSongQuery(title)
        val cleanArtist = artist?.let { VoiceCommandParser.normalizeSongQuery(it) }

        // 1. Check local cache / downloaded library first for an exact or high confidence match
        val localSong = searchLocalCachedSong(normalizedTitle, cleanArtist)
        if (localSong != null) {
            Timber.d("VoiceMediaSearchManager found local library match: %s", localSong.song.title)
            val mediaItem = localSong.toMediaItem()
            syncServiceQueue(ListQueue(localSong.song.title, listOf(mediaItem)))
            return MediaSession.MediaItemsWithStartPosition(listOf(mediaItem), 0, 0L)
        }

        // 2. Query online providers (YouTube Music, JioSaavn)
        val candidates = searchOnlineCandidates(normalizedTitle, cleanArtist, rawQuery)
        if (candidates.isNotEmpty()) {
            val targetSong = IdentifiedSong(
                title = normalizedTitle,
                artist = cleanArtist,
                album = null,
                albumArtUrl = null,
                durationMs = null,
            )
            val ranked = SongMatchEngine.rankCandidates(targetSong, candidates)
            val topMatch = ranked.firstOrNull() ?: candidates.first()

            Timber.d("VoiceMediaSearchManager selected top match: %s by %s", topMatch.title, topMatch.artists.joinToString())
            val metadata = topMatch.mediaMetadata
            val queue = YouTubeQueue(WatchEndpoint(topMatch.id), metadata)
            syncServiceQueue(queue)

            val mediaItem = metadata.toMediaItem()
            return MediaSession.MediaItemsWithStartPosition(listOf(mediaItem), 0, 0L)
        }

        // 3. Secondary fallback: fuzzy search across local database songs
        val fallbackLocal = searchLocalCachedSongFuzzy(normalizedTitle)
        if (fallbackLocal != null) {
            Timber.d("VoiceMediaSearchManager found fuzzy local fallback: %s", fallbackLocal.song.title)
            val mediaItem = fallbackLocal.toMediaItem()
            syncServiceQueue(ListQueue(fallbackLocal.song.title, listOf(mediaItem)))
            return MediaSession.MediaItemsWithStartPosition(listOf(mediaItem), 0, 0L)
        }

        return null
    }

    /**
     * Resolves an artist-focused search request, returning tracks prioritizing the artist.
     */
    private suspend fun resolveArtistRequest(
        artistName: String,
    ): MediaSession.MediaItemsWithStartPosition? {
        val cleanArtist = VoiceCommandParser.normalizeSongQuery(artistName).trim()

        // 1. Check local database
        try {
            val localArtists = database.searchArtists(cleanArtist, previewSize = 5).first()
            val matchedArtist = localArtists.firstOrNull {
                it.title.contains(cleanArtist, ignoreCase = true) || cleanArtist.contains(it.title, ignoreCase = true)
            } ?: localArtists.firstOrNull()

            if (matchedArtist != null) {
                val songs = database.artistSongsByCreateDateAsc(matchedArtist.id).first()
                if (songs.isNotEmpty()) {
                    Timber.d("VoiceMediaSearchManager resolved artist locally: %s (%d tracks)", matchedArtist.title, songs.size)
                    val mediaItems = songs.map { it.toMediaItem() }
                    syncServiceQueue(ListQueue(matchedArtist.title, mediaItems))
                    return MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error searching local artists for '%s'", cleanArtist)
        }

        // 2. Query online providers
        try {
            val hideExplicit = context.dataStore.getSuspend(HideExplicitKey, false)
            val ytResult = YouTube.search(cleanArtist, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            val songs = ytResult?.items
                ?.filterIsInstance<SongItem>()
                ?.filterExplicit(hideExplicit)

            if (!songs.isNullOrEmpty()) {
                // Prioritize songs where artist matches
                val sorted = songs.sortedByDescending { item ->
                    if (item.artists.any { it.name.contains(cleanArtist, ignoreCase = true) }) 1 else 0
                }
                val firstTrack = sorted.first()
                val queue = YouTubeQueue(WatchEndpoint(firstTrack.id), firstTrack.toMediaMetadata())
                syncServiceQueue(queue)

                val mediaItems = sorted.map { it.toMediaItem() }
                return MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error searching online artist songs for '%s'", cleanArtist)
        }

        return null
    }

    /**
     * Resolves an album-focused search request, preserving the album's track ordering.
     */
    private suspend fun resolveAlbumRequest(
        albumName: String,
        artistName: String?,
    ): MediaSession.MediaItemsWithStartPosition? {
        val cleanAlbum = VoiceCommandParser.normalizeSongQuery(albumName).trim()

        // 1. Local database search
        try {
            val localAlbums = database.searchAlbums(cleanAlbum, previewSize = 5).first()
            val matchedAlbum = localAlbums.firstOrNull {
                it.title.contains(cleanAlbum, ignoreCase = true) || cleanAlbum.contains(it.title, ignoreCase = true)
            } ?: localAlbums.firstOrNull()

            if (matchedAlbum != null) {
                val albumWithSongs = database.albumWithSongs(matchedAlbum.id).first()
                if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
                    Timber.d("VoiceMediaSearchManager resolved album locally: %s (%d tracks)", matchedAlbum.title, albumWithSongs.songs.size)
                    val mediaItems = albumWithSongs.songs.map { it.toMediaItem() }
                    syncServiceQueue(ListQueue(matchedAlbum.title, mediaItems))
                    return MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error searching local albums for '%s'", cleanAlbum)
        }

        // 2. Online search
        try {
            val hideExplicit = context.dataStore.getSuspend(HideExplicitKey, false)
            val ytAlbums = YouTube.search(cleanAlbum, YouTube.SearchFilter.FILTER_ALBUM).getOrNull()
            val firstAlbum = ytAlbums?.items?.firstOrNull()
            if (firstAlbum != null) {
                val albumPage = YouTube.album(firstAlbum.id).getOrNull()
                val songs = albumPage?.songs?.filterExplicit(hideExplicit)
                if (!songs.isNullOrEmpty()) {
                    val mediaItems = songs.map { it.toMediaItem() }
                    syncServiceQueue(ListQueue(firstAlbum.title, mediaItems))
                    return MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error searching online albums for '%s'", cleanAlbum)
        }

        return null
    }

    /**
     * Resolves a named playlist request from the user's stored playlists.
     */
    private suspend fun resolvePlaylistRequest(
        playlistName: String,
    ): MediaSession.MediaItemsWithStartPosition? {
        val cleanName = VoiceCommandParser.normalizeSongQuery(playlistName).trim()
        try {
            val matchedPlaylists = database.searchPlaylists(cleanName, previewSize = 5).first()
            val playlist = matchedPlaylists.firstOrNull {
                it.title.contains(cleanName, ignoreCase = true) || cleanName.contains(it.title, ignoreCase = true)
            } ?: matchedPlaylists.firstOrNull()

            if (playlist != null) {
                val songs = database.playlistSongs(playlist.id).first().map { it.song }
                if (songs.isNotEmpty()) {
                    Timber.d("VoiceMediaSearchManager resolved playlist: %s (%d tracks)", playlist.title, songs.size)
                    val mediaItems = songs.map { it.toMediaItem() }
                    syncServiceQueue(ListQueue(playlist.title, mediaItems))
                    return MediaSession.MediaItemsWithStartPosition(mediaItems, 0, 0L)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Error searching playlists for '%s'", cleanName)
        }
        return null
    }

    /**
     * Queries online providers (YouTube Music, JioSaavn) using progressive queries and filters.
     */
    private suspend fun searchOnlineCandidates(
        title: String,
        artist: String?,
        rawQuery: String,
    ): List<ProviderSong> {
        val candidates = mutableListOf<ProviderSong>()
        val hideExplicit = context.dataStore.getSuspend(HideExplicitKey, false)
        val enableJioSaavn = context.dataStore.getSuspend(EnableJioSaavnKey, false)
        val musicProvider = context.dataStore.getSuspend(MusicProviderKey, "YT")

        val cleanTitle = title.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
        val queries = linkedSetOf(
            if (!artist.isNullOrBlank()) "$cleanTitle $artist".trim() else "",
            cleanTitle,
            rawQuery.trim()
        ).filter { it.isNotBlank() }

        // A. JioSaavn if enabled as primary or enabled in settings
        if (musicProvider == "JIOSAAVN" || enableJioSaavn) {
            for (q in queries.take(2)) {
                try {
                    val jioResult = JioSaavnApi.searchSongs(q).getOrNull()
                    jioResult?.forEach { item ->
                        if (candidates.none { it.id == item.id }) {
                            candidates.add(
                                ProviderSong(
                                    id = item.id,
                                    title = item.title,
                                    artists = item.artists.map { it.name },
                                    duration = item.duration ?: -1,
                                    thumbnailUrl = item.thumbnail,
                                    mediaMetadata = item.toMediaMetadata(),
                                    provider = "JIOSAAVN"
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "JioSaavn online search failed for query: %s", q)
                }
            }
        }

        // B. YouTube Music search
        for (q in queries) {
            if (candidates.size >= 6) break
            try {
                val ytResult = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                ytResult?.items
                    ?.filterIsInstance<SongItem>()
                    ?.filterExplicit(hideExplicit)
                    ?.forEach { item ->
                        if (candidates.none { it.id == item.id }) {
                            candidates.add(
                                ProviderSong(
                                    id = item.id,
                                    title = item.title,
                                    artists = item.artists.map { it.name },
                                    duration = item.duration ?: -1,
                                    thumbnailUrl = item.thumbnail,
                                    mediaMetadata = item.toMediaMetadata(),
                                    provider = "YT"
                                )
                            )
                        }
                    }
            } catch (e: Exception) {
                Timber.w(e, "YouTube online search failed for query: %s", q)
            }
        }

        // C. Fallback: YouTube summary search if song search returned no results
        if (candidates.isEmpty()) {
            for (q in queries.take(2)) {
                try {
                    val summaryResult = YouTube.searchSummary(q).getOrNull()
                    summaryResult?.summaries
                        ?.flatMap { it.items }
                        ?.filterIsInstance<SongItem>()
                        ?.filterExplicit(hideExplicit)
                        ?.forEach { item ->
                            if (candidates.none { it.id == item.id }) {
                                candidates.add(
                                    ProviderSong(
                                        id = item.id,
                                        title = item.title,
                                        artists = item.artists.map { it.name },
                                        duration = item.duration ?: -1,
                                        thumbnailUrl = item.thumbnail,
                                        mediaMetadata = item.toMediaMetadata(),
                                        provider = "YT"
                                    )
                                )
                            }
                        }
                } catch (e: Exception) {
                    Timber.w(e, "YouTube summary search failed for query: %s", q)
                }
            }
        }

        return candidates
    }

    /**
     * Searches local cached and downloaded songs for an exact or high-confidence title + artist match.
     */
    private suspend fun searchLocalCachedSong(title: String, artist: String?): Song? {
        val cleanTitle = title.lowercase(Locale.ROOT).trim()
        val cleanArtist = artist?.lowercase(Locale.ROOT)?.trim()

        try {
            val matched = database.searchSongs(cleanTitle, previewSize = 30).first()
            if (matched.isNotEmpty()) {
                if (!cleanArtist.isNullOrBlank()) {
                    val artistMatch = matched.firstOrNull { song ->
                        song.artists.any { it.name.lowercase(Locale.ROOT).contains(cleanArtist) }
                    }
                    if (artistMatch != null) return artistMatch
                }
                val exactTitleMatch = matched.firstOrNull {
                    it.song.title.lowercase(Locale.ROOT).trim() == cleanTitle
                }
                if (exactTitleMatch != null) return exactTitleMatch
            }
        } catch (e: Exception) {
            Timber.w(e, "Error querying local songs for '%s'", title)
        }
        return null
    }

    /**
     * Fuzzy search for local cached songs when offline or online providers return empty.
     */
    private suspend fun searchLocalCachedSongFuzzy(query: String): Song? {
        val cleanQuery = query.lowercase(Locale.ROOT).trim()
            .replace(Regex("[.,!?;:'\"()\\[\\]\\-_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        try {
            val songs = database.searchSongs(cleanQuery, previewSize = 30).first()
            if (songs.isNotEmpty()) return songs.first()

            val all = database.allSongs().first()
            return all.firstOrNull { song ->
                val title = song.song.title.lowercase(Locale.ROOT)
                title.contains(cleanQuery) || cleanQuery.contains(title)
            }
        } catch (e: Exception) {
            Timber.w(e, "Error fuzzy searching local songs for '%s'", query)
        }
        return null
    }

    /**
     * Helper to synchronize queue state with active MusicService where available,
     * ensuring infinite radio or auto-loading operates seamlessly.
     */
    private fun syncServiceQueue(queue: com.darkxvenom.airbeats.playback.queues.Queue) {
        try {
            val service = MusicService.instance ?: PlayerConnection.instance?.service
            service?.let {
                it.playQueue(queue, playWhenReady = false)
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not synchronize service queue directly")
        }
    }

    /**
     * Helper to check if a query/focus indicates a liked songs or favorites request.
     */
    fun isLikedRequest(query: String, focus: String?, playlistExtra: String?): Boolean {
        if (focus == MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE &&
            (playlistExtra?.lowercase(Locale.ROOT)?.contains("like") == true ||
             playlistExtra?.lowercase(Locale.ROOT)?.contains("favorit") == true ||
             playlistExtra?.lowercase(Locale.ROOT)?.contains("favourite") == true)
        ) {
            return true
        }
        val lower = query.lowercase(Locale.ROOT).trim()
        return lower.matches(Regex(".*\\b(my\\s+)?(liked|favorites?|favourites?|loved)\\b.*")) ||
               lower == "liked songs" || lower == "my liked songs" || lower == "favorites" ||
               lower == "play favorites" || lower == "play liked songs"
    }

    /**
     * Helper to check if a query represents a generic music playback request.
     */
    fun isGenericPlayRequest(
        query: String,
        focus: String?,
        titleExtra: String?,
        artistExtra: String?,
        albumExtra: String?,
        playlistExtra: String?,
    ): Boolean {
        if (query.isBlank() && focus == null && titleExtra.isNullOrBlank() &&
            artistExtra.isNullOrBlank() && albumExtra.isNullOrBlank() && playlistExtra.isNullOrBlank()
        ) {
            return true
        }

        val lower = query.lowercase(Locale.ROOT).trim()
        val genericPhrases = setOf(
            "play music", "play songs", "play some music", "play some songs",
            "start music", "play something", "play anything", "shuffle music",
            "shuffle songs", "resume", "play", "airbeats", "continue"
        )
        return genericPhrases.contains(lower) || lower == "airbeats play music" || lower == "play music on airbeats"
    }

    /**
     * Helper to check if request is artist-focused.
     */
    fun isArtistRequest(
        query: String,
        focus: String?,
        artistExtra: String?,
        titleExtra: String?,
        albumExtra: String?,
    ): Boolean {
        if (focus == MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE) return true
        if (!artistExtra.isNullOrBlank() && titleExtra.isNullOrBlank() && albumExtra.isNullOrBlank()) return true
        val lower = query.lowercase(Locale.ROOT).trim()
        return lower.startsWith("songs by ") ||
               lower.startsWith("play songs by ") ||
               lower.startsWith("tracks by ") ||
               lower.startsWith("play artist ")
    }

    /**
     * Helper to check if request is album-focused.
     */
    fun isAlbumRequest(
        query: String,
        focus: String?,
        albumExtra: String?,
        titleExtra: String?,
    ): Boolean {
        if (focus == MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE) return true
        if (!albumExtra.isNullOrBlank() && titleExtra.isNullOrBlank()) return true
        val lower = query.lowercase(Locale.ROOT).trim()
        return lower.startsWith("album ") || lower.startsWith("play album ")
    }

    /**
     * Strips voice assistant conversational prefixes/suffixes from song query.
     */
    fun cleanTitleFromQuery(rawQuery: String): String {
        var q = rawQuery.trim()
        // Strip common command verbs
        q = q.replace(Regex("^(?:play|listen\\s+to|put\\s+on|stream|find)\\s+", RegexOption.IGNORE_CASE), "")
        // Strip app name references
        q = q.replace(Regex("\\s+(?:on|in|with)\\s+air\\s*beats?$", RegexOption.IGNORE_CASE), "")
        q = q.replace(Regex("\\s+on\\s+airbeats$", RegexOption.IGNORE_CASE), "")
        // Strip "the song", "song", "track"
        q = q.replace(Regex("^(?:the\\s+)?(?:song|track)\\s+(?:called\\s+)?", RegexOption.IGNORE_CASE), "")
        return q.trim()
    }

    /**
     * Extracts artist from patterns like "... by [Artist]" or "songs by [Artist]".
     */
    fun extractArtistFromQuery(rawQuery: String): String? {
        val byMatch = Regex("\\s+by\\s+(.+)$", RegexOption.IGNORE_CASE).find(rawQuery)
        if (byMatch != null) {
            val candidate = byMatch.groupValues[1]
                .replace(Regex("\\s+(?:on|in)\\s+air\\s*beats?$", RegexOption.IGNORE_CASE), "")
                .trim()
            if (candidate.isNotBlank()) return candidate
        }
        val songsByMatch = Regex("^(?:play\\s+)?(?:songs|tracks|music)\\s+by\\s+(.+)$", RegexOption.IGNORE_CASE).find(rawQuery)
        if (songsByMatch != null) {
            val candidate = songsByMatch.groupValues[1]
                .replace(Regex("\\s+(?:on|in)\\s+air\\s*beats?$", RegexOption.IGNORE_CASE), "")
                .trim()
            if (candidate.isNotBlank()) return candidate
        }
        return null
    }

    /**
     * Combines local and online searches to power MediaLibrarySession search results.
     */
    suspend fun searchSongsOnlineAndLocal(query: String, requestedCount: Int): List<MediaItem> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank() || requestedCount <= 0) return@withContext emptyList()

        val results = mutableListOf<MediaItem>()

        // Local songs
        try {
            val localSongs = database.searchSongs(q, previewSize = requestedCount).first()
            results.addAll(localSongs.map { it.toMediaItem() })
        } catch (e: Exception) {
            Timber.w(e, "Error searching local database songs in searchSongsOnlineAndLocal")
        }

        // Online songs if local count is below requested
        if (results.size < requestedCount) {
            try {
                val hideExplicit = context.dataStore.getSuspend(HideExplicitKey, false)
                val ytResult = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                ytResult?.items
                    ?.filterIsInstance<SongItem>()
                    ?.filterExplicit(hideExplicit)
                    ?.forEach { item ->
                        if (results.size < requestedCount && results.none { it.mediaId == "${MusicService.SONG}/${item.id}" || it.mediaId == item.id }) {
                            results.add(item.toMediaItem())
                        }
                    }
            } catch (e: Exception) {
                Timber.w(e, "Error searching YouTube songs in searchSongsOnlineAndLocal")
            }
        }

        results
    }
}
