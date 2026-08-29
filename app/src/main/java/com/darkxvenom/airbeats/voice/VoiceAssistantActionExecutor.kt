package com.darkxvenom.airbeats.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Toast
import com.darkxvenom.airbeats.App
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.HideExplicitKey
import com.darkxvenom.airbeats.constants.MusicProviderKey
import com.darkxvenom.airbeats.constants.SongSortType
import com.darkxvenom.airbeats.constants.VoiceAssistantTtsFeedbackKey
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.filterExplicit
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.PlayerConnection
import com.darkxvenom.airbeats.playback.queues.ListQueue
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get
import com.darkxvenom.airbeats.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

class VoiceAssistantActionExecutor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val getMusicService: () -> MusicService?,
    private val overlayManager: VoiceAssistantOverlayManager? = null
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize TextToSpeech")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            isTtsReady = true
        } else {
            Timber.w("TextToSpeech initialization failed with status $status")
        }
    }

    fun showToast(message: String, iconResId: Int = R.drawable.music_note) {
        overlayManager?.showActionResult(message, iconResId)
        mainHandler.post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    fun speak(text: String) {
        scope.launch {
            val ttsEnabled = context.dataStore.get(VoiceAssistantTtsFeedbackKey, true)
            if (ttsEnabled && isTtsReady && tts != null) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "airbeats_voice_feedback")
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun applyVolume(levelPercent: Int) {
        val targetRatio = (levelPercent / 100f).coerceIn(0f, 1f)
        val service = ensureMusicService()
        if (service != null) {
            service.playerVolume.value = targetRatio
            service.player.volume = targetRatio
        } else {
            PlayerConnection.instance?.let {
                it.service.playerVolume.value = targetRatio
                it.service.player.volume = targetRatio
            }
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
            val targetSystemVol = (targetRatio * maxVol).toInt().coerceIn(0, maxVol)
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetSystemVol, 0)
        } catch (_: Exception) {}

        showToast("Volume: $levelPercent%", if (levelPercent == 0) R.drawable.volume_off else R.drawable.volume_up)
    }

    fun execute(command: VoiceCommand) {
        Timber.d("Executing voice command: %s", command)

        when (command) {
            is VoiceCommand.PlayGenericMusic -> {
                showToast("Playing songs", R.drawable.music_note)
                handlePlayGenericMusic()
            }
            is VoiceCommand.PlayCachedSongs -> {
                showToast("Loading library songs...", R.drawable.library_music)
                handlePlayCachedSongs()
            }
            is VoiceCommand.PlayLikedSongs -> {
                showToast("Loading liked songs...", R.drawable.favorite)
                handlePlayLikedSongs()
            }
            is VoiceCommand.PlaySong -> {
                showToast("Searching: \"${command.query}\"", R.drawable.search)
                handlePlaySong(command.query)
            }
            is VoiceCommand.Pause -> {
                scope.launch(Dispatchers.Main) {
                    showToast("Paused", R.drawable.pause)
                    val service = ensureMusicService()
                    service?.player?.pause()
                }
            }
            is VoiceCommand.Resume -> {
                scope.launch(Dispatchers.Main) {
                    showToast("Resumed", R.drawable.play)
                    val service = ensureMusicService()
                    service?.player?.play()
                }
            }
            is VoiceCommand.NextTrack -> {
                scope.launch(Dispatchers.Main) {
                    showToast("Next track", R.drawable.skip_next)
                    val service = ensureMusicService()
                    service?.player?.let { player ->
                        if (player.hasNextMediaItem()) {
                            player.seekToNext()
                            player.prepare()
                            player.playWhenReady = true
                            player.play()
                        }
                    }
                }
            }
            is VoiceCommand.PreviousTrack -> {
                scope.launch(Dispatchers.Main) {
                    showToast("Previous track", R.drawable.skip_previous)
                    val service = ensureMusicService()
                    service?.player?.let { player ->
                        if (player.hasPreviousMediaItem()) {
                            player.seekToPreviousMediaItem()
                            player.prepare()
                            player.playWhenReady = true
                            player.play()
                        } else {
                            player.seekTo(0)
                            player.prepare()
                            player.playWhenReady = true
                            player.play()
                        }
                    }
                }
            }
            is VoiceCommand.ToggleLike -> {
                scope.launch(Dispatchers.Main) {
                    showToast("Added to favorites", R.drawable.favorite)
                    val service = ensureMusicService()
                    service?.toggleLike()
                }
            }
            is VoiceCommand.StartRadio -> {
                scope.launch(Dispatchers.Main) {
                    showToast("Starting radio...", R.drawable.radio)
                    val service = ensureMusicService()
                    service?.startRadioSeamlessly()
                }
            }
            is VoiceCommand.VolumeUp -> {
                scope.launch(Dispatchers.Main) {
                    val service = ensureMusicService()
                    val currentRatio = service?.playerVolume?.value ?: service?.player?.volume ?: 0.5f
                    val newPercent = ((currentRatio + 0.15f) * 100).toInt().coerceIn(0, 100)
                    applyVolume(newPercent)
                }
            }
            is VoiceCommand.VolumeDown -> {
                scope.launch(Dispatchers.Main) {
                    val service = ensureMusicService()
                    val currentRatio = service?.playerVolume?.value ?: service?.player?.volume ?: 0.5f
                    val newPercent = ((currentRatio - 0.15f) * 100).toInt().coerceIn(0, 100)
                    applyVolume(newPercent)
                }
            }
            is VoiceCommand.SetVolume -> {
                scope.launch(Dispatchers.Main) {
                    applyVolume(command.levelPercent)
                }
            }
            is VoiceCommand.Mute -> {
                scope.launch(Dispatchers.Main) {
                    applyVolume(0)
                }
            }
            is VoiceCommand.Unmute -> {
                scope.launch(Dispatchers.Main) {
                    applyVolume(100)
                }
            }
            is VoiceCommand.Unknown -> {
                Timber.d("Unknown voice command: %s", command.rawText)
            }
        }
    }

    private suspend fun getAllLocalCachedSongs(): List<Song> = withContext(Dispatchers.IO) {
        try {
            val database = App.instance.database
            val librarySongs: List<Song> = try { database.getAllLibrarySongsSync() } catch (_: Exception) { emptyList() }
            val library: List<Song> = try { database.songs(SongSortType.CREATE_DATE, true).firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            val liked: List<Song> = try { database.likedSongs(SongSortType.CREATE_DATE, true).firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            val all: List<Song> = try { database.allSongs().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }

            (librarySongs + library + liked + all).distinctBy { it.id }.filter { it.id.isNotBlank() }
        } catch (e: Exception) {
            Timber.e(e, "Error querying local cached songs")
            emptyList()
        }
    }

    private fun handlePlayGenericMusic() {
        scope.launch {
            try {
                val isOnline = isNetworkAvailable()
                if (!isOnline) {
                    // When offline, immediately play and shuffle songs from the local library
                    handlePlayCachedSongs()
                    return@launch
                }

                // 1. Check user's listening history, liked songs, and library to determine personal taste
                val database = App.instance.database
                val userTasteSeeds = withContext(Dispatchers.IO) {
                    try {
                        val liked = database.likedSongs(SongSortType.PLAY_TIME, true).firstOrNull() ?: emptyList()
                        val played = database.songs(SongSortType.PLAY_TIME, true).firstOrNull() ?: emptyList()
                        val all = database.allSongs().firstOrNull() ?: emptyList()
                        (liked + played + all).distinctBy { it.id }.filter { it.id.isNotBlank() }
                    } catch (e: Exception) {
                        Timber.w(e, "Could not fetch user taste seeds")
                        emptyList()
                    }
                }

                if (userTasteSeeds.isNotEmpty()) {
                    // Pick a random seed song from user favorites to generate personalized endless radio tailored to their taste
                    val rng = java.util.Random(System.nanoTime())
                    val seedSong = userTasteSeeds.shuffled(rng).first()
                    val queue = YouTubeQueue.radio(seedSong.toMediaMetadata())

                    withContext(Dispatchers.Main) {
                        val service = ensureMusicService()
                        if (service != null) {
                            service.playQueue(queue, playWhenReady = true)
                            service.player.shuffleModeEnabled = true
                            service.player.prepare()
                            service.player.playWhenReady = true
                            service.player.play()
                        } else {
                            PlayerConnection.instance?.let {
                                it.playQueue(queue)
                                it.service.player.shuffleModeEnabled = true
                                it.service.player.prepare()
                                it.service.player.playWhenReady = true
                                it.service.player.play()
                            }
                        }
                        showToast("Playing songs")
                    }
                    speak("Playing songs")
                    return@launch
                }

                // 2. If user is brand new with no local history, fetch popular / trending tracks
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val searchResult = withContext(Dispatchers.IO) {
                    YouTube.search("Trending Songs", YouTube.SearchFilter.FILTER_SONG)
                }

                val songs = searchResult.getOrNull()?.items
                    ?.filterIsInstance<SongItem>()
                    ?.filterExplicit(hideExplicit)

                if (!songs.isNullOrEmpty()) {
                    val rng = java.util.Random(System.nanoTime())
                    val shuffled = songs.shuffled(rng)
                    val first = shuffled.first()
                    val queue = YouTubeQueue.radio(first.toMediaMetadata())

                    withContext(Dispatchers.Main) {
                        val service = ensureMusicService()
                        if (service != null) {
                            service.playQueue(queue, playWhenReady = true)
                            service.player.shuffleModeEnabled = true
                            service.player.prepare()
                            service.player.playWhenReady = true
                            service.player.play()
                        } else {
                            PlayerConnection.instance?.let {
                                it.playQueue(queue)
                                it.service.player.shuffleModeEnabled = true
                                it.service.player.prepare()
                                it.service.player.playWhenReady = true
                                it.service.player.play()
                            }
                        }
                        showToast("Playing songs")
                    }
                    speak("Playing songs")
                } else {
                    handlePlayCachedSongs()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error playing recommended music, falling back to library")
                handlePlayCachedSongs()
            }
        }
    }

    private fun handlePlayCachedSongs() {
        scope.launch {
            try {
                val songs = getAllLocalCachedSongs()

                if (songs.isNotEmpty()) {
                    val rng = java.util.Random(System.nanoTime())
                    val shuffledSongs = songs.shuffled(rng)
                    val mediaItems = shuffledSongs.map { it.toMediaItem() }
                    val randomStartIndex = if (mediaItems.size > 1) rng.nextInt(mediaItems.size) else 0

                    val queue = ListQueue(
                        title = "Library Songs",
                        items = mediaItems,
                        startIndex = randomStartIndex,
                        position = 0L
                    )

                    withContext(Dispatchers.Main) {
                        val service = ensureMusicService()
                        if (service != null) {
                            service.playQueue(queue, playWhenReady = true)
                            service.player.shuffleModeEnabled = true
                            service.player.seekTo(randomStartIndex, 0L)
                            service.player.prepare()
                            service.player.playWhenReady = true
                            service.player.play()
                        } else {
                            PlayerConnection.instance?.let {
                                it.playQueue(queue)
                                it.service.player.shuffleModeEnabled = true
                                it.service.player.seekTo(randomStartIndex, 0L)
                                it.service.player.prepare()
                                it.service.player.playWhenReady = true
                                it.service.player.play()
                            }
                        }
                        showToast("Playing songs from your library", R.drawable.library_music)
                    }
                    speak("Playing songs from your library")
                } else {
                    showToast("No songs found in library", R.drawable.library_music)
                    speak("No songs found in your library")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading cached songs")
                showToast("Failed to play library songs", R.drawable.error)
                speak("Error loading library songs")
            }
        }
    }

    private fun handlePlayLikedSongs() {
        scope.launch {
            try {
                val database = App.instance.database
                val liked = withContext(Dispatchers.IO) {
                    try {
                        database.likedSongs(SongSortType.CREATE_DATE, true).firstOrNull()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load liked songs")
                        null
                    }
                }

                if (!liked.isNullOrEmpty()) {
                    val rng = java.util.Random(System.nanoTime())
                    val shuffledLiked = liked.shuffled(rng)
                    val mediaItems = shuffledLiked.map { it.toMediaItem() }
                    val randomStartIndex = if (mediaItems.size > 1) rng.nextInt(mediaItems.size) else 0

                    val queue = ListQueue(
                        title = "Liked Songs",
                        items = mediaItems,
                        startIndex = randomStartIndex,
                        position = 0L
                    )

                    withContext(Dispatchers.Main) {
                        val service = ensureMusicService()
                        if (service != null) {
                            service.playQueue(queue, playWhenReady = true)
                            service.player.shuffleModeEnabled = true
                            service.player.seekTo(randomStartIndex, 0L)
                            service.player.prepare()
                            service.player.playWhenReady = true
                            service.player.play()
                        } else {
                            PlayerConnection.instance?.let {
                                it.playQueue(queue)
                                it.service.player.shuffleModeEnabled = true
                                it.service.player.seekTo(randomStartIndex, 0L)
                                it.service.player.prepare()
                                it.service.player.playWhenReady = true
                                it.service.player.play()
                            }
                        }
                        showToast("Playing your liked songs", R.drawable.favorite)
                    }
                    speak("Playing your liked songs")
                } else {
                    showToast("No liked songs found", R.drawable.favorite_border)
                    speak("No liked songs found")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading liked songs")
                showToast("Failed to play liked songs", R.drawable.error)
                speak("Error loading liked songs")
            }
        }
    }

    private suspend fun searchCachedLibrarySong(query: String): Song? = withContext(Dispatchers.IO) {
        try {
            val allSongs = getAllLocalCachedSongs()
            if (allSongs.isEmpty()) return@withContext null

            val cleanQuery = query.lowercase(Locale.ROOT).trim()
                .replace(Regex("[.,!?;:'\"()\\[\\]\\-_]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            // 1. Exact clean match
            allSongs.firstOrNull { song ->
                val cleanTitle = song.song.title.lowercase(Locale.ROOT)
                    .replace(Regex("[.,!?;:'\"()\\[\\]\\-_]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                cleanTitle == cleanQuery
            }
            // 2. Title contains full query
            ?: allSongs.firstOrNull { song ->
                song.song.title.lowercase(Locale.ROOT).contains(cleanQuery)
            }
            // 3. Title starts with query
            ?: allSongs.firstOrNull { song ->
                song.song.title.lowercase(Locale.ROOT).startsWith(cleanQuery)
            }
            // 4. Keyword fuzzy match (every word in query matches song title or artist)
            ?: allSongs.firstOrNull { song ->
                val words = cleanQuery.split(" ").filter { it.length >= 2 }
                val title = song.song.title.lowercase(Locale.ROOT)
                val artists = song.artists.joinToString(" ") { it.name.lowercase(Locale.ROOT) }
                words.isNotEmpty() && words.all { title.contains(it) || artists.contains(it) }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error searching cached songs")
            null
        }
    }

    private suspend fun playLocalCachedSong(song: Song) {
        val mediaItem = song.toMediaItem()
        val queue = ListQueue(
            title = "Cached Song",
            items = listOf(mediaItem)
        )

        withContext(Dispatchers.Main) {
            val service = ensureMusicService()
            if (service != null) {
                service.playQueue(queue, playWhenReady = true)
                service.player.play()
            } else {
                PlayerConnection.instance?.let {
                    it.playQueue(queue)
                    it.service.player.play()
                }
            }
            showToast("Playing: ${song.song.title}", R.drawable.play)
        }

        val artistName = song.artists.joinToString { it.name }
        if (artistName.isNotBlank()) {
            speak("Playing ${song.song.title} by $artistName")
        } else {
            speak("Playing ${song.song.title}")
        }
    }

    private fun handlePlaySong(query: String) {
        scope.launch {
            try {
                val isOnline = isNetworkAvailable()
                var songToPlayOnline: SongItem? = null

                // If offline, immediately search local cached songs
                if (!isOnline) {
                    val localSong = searchCachedLibrarySong(query)
                    if (localSong != null) {
                        playLocalCachedSong(localSong)
                        return@launch
                    } else {
                        showToast("Offline: \"$query\" not found in cache", R.drawable.search_off)
                        speak("Song $query was not found")
                        return@launch
                    }
                }

                // If online, search YouTube Music / JioSaavn
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val musicProvider = context.dataStore.get(MusicProviderKey, "YT")

                if (musicProvider == "JIOSAAVN") {
                    try {
                        val jioResult = com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.searchSongs(query)
                        songToPlayOnline = jioResult.getOrNull()?.firstOrNull()
                    } catch (e: Exception) {
                        Timber.w(e, "JioSaavn search error")
                    }
                }

                if (songToPlayOnline == null) {
                    try {
                        val searchResult = withContext(Dispatchers.IO) {
                            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        }

                        songToPlayOnline = searchResult.getOrNull()?.items
                            ?.filterIsInstance<SongItem>()
                            ?.filterExplicit(hideExplicit)
                            ?.firstOrNull()

                        if (songToPlayOnline == null) {
                            val summaryResult = withContext(Dispatchers.IO) {
                                YouTube.searchSummary(query)
                            }
                            songToPlayOnline = summaryResult.getOrNull()?.summaries
                                ?.flatMap { it.items }
                                ?.filterIsInstance<SongItem>()
                                ?.filterExplicit(hideExplicit)
                                ?.firstOrNull()
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Online search failed")
                    }
                }

                if (songToPlayOnline != null) {
                    val metadata = songToPlayOnline.toMediaMetadata()
                    val queue = YouTubeQueue(WatchEndpoint(songToPlayOnline.id), metadata)

                    withContext(Dispatchers.Main) {
                        val service = ensureMusicService()
                        if (service != null) {
                            service.playQueue(queue, playWhenReady = true)
                            service.player.play()
                        } else {
                            PlayerConnection.instance?.let {
                                it.playQueue(queue)
                                it.service.player.play()
                            }
                        }
                        showToast("Playing: ${songToPlayOnline.title}", R.drawable.play)
                    }

                    val artistName = songToPlayOnline.artists.joinToString { it.name }
                    if (artistName.isNotBlank()) {
                        speak(context.getString(R.string.voice_playing_feedback, songToPlayOnline.title, artistName))
                    } else {
                        speak(context.getString(R.string.voice_playing_simple, songToPlayOnline.title))
                    }
                    return@launch
                }

                // Fallback to local cached songs if online returned no results
                val localFallback = searchCachedLibrarySong(query)
                if (localFallback != null) {
                    playLocalCachedSong(localFallback)
                } else {
                    showToast("Song not found: \"$query\"", R.drawable.search_off)
                    speak(context.getString(R.string.voice_song_not_found))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching and playing song for query '%s'", query)
                val localFallback = searchCachedLibrarySong(query)
                if (localFallback != null) {
                    playLocalCachedSong(localFallback)
                } else {
                    reportException(e)
                    showToast("Error playing song", R.drawable.error)
                    speak(context.getString(R.string.voice_song_not_found))
                }
            }
        }
    }

    private fun ensureMusicService(): MusicService? {
        val service = MusicService.instance ?: getMusicService() ?: PlayerConnection.instance?.service
        if (service == null) {
            try {
                val intent = Intent(context, MusicService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start MusicService")
            }
        }
        return service ?: MusicService.instance ?: PlayerConnection.instance?.service
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsReady = false
        } catch (e: Exception) {
            Timber.e(e, "Error releasing TextToSpeech")
        }
    }
}
