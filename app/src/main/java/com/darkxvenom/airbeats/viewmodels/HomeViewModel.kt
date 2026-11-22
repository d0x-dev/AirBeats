package com.darkxvenom.airbeats.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.innertube.pages.ExplorePage
import com.darkxvenom.airbeats.innertube.pages.HomePage
import com.darkxvenom.airbeats.innertube.utils.completedLibraryPage
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.db.entities.Album
import com.darkxvenom.airbeats.db.entities.Artist
import com.darkxvenom.airbeats.db.entities.LocalItem
import com.darkxvenom.airbeats.db.entities.Playlist
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.models.SimilarRecommendation
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get
import com.darkxvenom.airbeats.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

data class HeroPlaylistData(
    val title: String,
    val subtitle: String,
    val tag: String,
    val thumbnailUrl: String?,
    val songs: List<Song>,
    val playlistId: String? = null,
)


@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    private val homeRandom = kotlin.random.Random(System.currentTimeMillis())
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

    val aiRecommendedPlaylist = combine(
        database.playlistsByNameAsc()
            .map { playlists -> playlists.find { it.playlist.name == "Recommended by AI" } }
            .flatMapLatest { playlist ->
                if (playlist != null && playlist.songCount > 0) {
                    database.playlistSongs(playlist.playlist.id).map { playlistSongs ->
                        playlist to playlistSongs.map { it.song }
                    }
                } else {
                    flowOf<Pair<Playlist, List<Song>>?>(null)
                }
            },
        database.observeExcludedSongIds()
    ) { aiPlaylistData, excludedIds ->
        val excludedSet = excludedIds.toHashSet()
        if (aiPlaylistData == null) null
        else {
            val (playlist, songs) = aiPlaylistData
            val filteredSongs = songs.filter { it.id !in excludedSet }
            playlist to filteredSongs
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val heroPlaylist = combine(
        database.playlists(com.darkxvenom.airbeats.constants.PlaylistSortType.LAST_UPDATED, descending = true)
            .flatMapLatest { playlists ->
                val topPlaylist = playlists.firstOrNull { it.songCount > 0 }
                if (topPlaylist != null) {
                    database.playlistSongs(topPlaylist.playlist.id).map { playlistSongs ->
                        val songs = playlistSongs.map { it.song }
                        HeroPlaylistData(
                            title = topPlaylist.playlist.name,
                            subtitle = context.getString(com.darkxvenom.airbeats.R.string.ui_heading_based_on_your_last_listening_habits_and_artis),
                            tag = "TOP PLAYLIST",
                            thumbnailUrl = topPlaylist.thumbnails.firstOrNull() ?: songs.firstOrNull()?.thumbnailUrl,
                            songs = songs,
                            playlistId = topPlaylist.playlist.id,
                        )
                    }
                } else {
                    flowOf<HeroPlaylistData?>(null)
                }
            },
        database.observeExcludedSongIds()
    ) { heroData, excludedIds ->
        val excludedSet = excludedIds.toHashSet()
        if (heroData == null) null
        else {
            val filteredSongs = heroData.songs.filter { it.id !in excludedSet }
            heroData.copy(songs = filteredSongs)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)


    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    private var loadJob: Job? = null
    private var accountFingerprint: Int? = null

    private fun mapToSong(item: SongItem): Song {
        return Song(
            song = item.toMediaMetadata().toSongEntity(),
            artists = item.artists.map { a ->
                com.darkxvenom.airbeats.db.entities.ArtistEntity(id = a.id ?: "", name = a.name)
            },
            album = item.album?.let { a ->
                com.darkxvenom.airbeats.db.entities.AlbumEntity(id = a.id, title = a.name, songCount = 0, duration = 0)
            }
        )
    }

    private fun filterHomeContent(excludedSet: Set<String>) {
        if (excludedSet.isEmpty()) return

        quickPicks.value = quickPicks.value?.filter { it.id !in excludedSet }?.takeIf { it.isNotEmpty() }
        forgottenFavorites.value = forgottenFavorites.value?.filter { it.id !in excludedSet }?.takeIf { it.isNotEmpty() }
        keepListening.value = keepListening.value?.filter { item ->
            when (item) {
                is Song -> item.id !in excludedSet
                else -> true
            }
        }?.takeIf { it.isNotEmpty() }

        similarRecommendations.value = similarRecommendations.value?.mapNotNull { rec ->
            val filteredItems = rec.items.filter { it.id !in excludedSet }
            if (filteredItems.isNotEmpty() && (rec.title !is Song || rec.title.id !in excludedSet)) {
                rec.copy(items = filteredItems)
            } else {
                null
            }
        }?.takeIf { it.isNotEmpty() }

        homePage.value = homePage.value?.let { page ->
            val filteredSections = page.sections.mapNotNull { section ->
                val filteredItems = section.items.filter { it.id !in excludedSet }
                if (filteredItems.isNotEmpty()) {
                    section.copy(items = filteredItems)
                } else {
                    null
                }
            }
            page.copy(sections = filteredSections)
        }

        explorePage.value = explorePage.value?.let { page ->
            page.copy(newReleaseAlbums = page.newReleaseAlbums.filter { it.id !in excludedSet })
        }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty()).filter { it is Song || it is Album }
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() + homePage.value?.sections?.flatMap { it.items }.orEmpty() + explorePage.value?.newReleaseAlbums.orEmpty()
    }

    private suspend fun load() {
        isLoading.value = true
        try {
            val excludedSongIds = runCatching {
                database.getExcludedSongIds().toHashSet()
            }.getOrDefault(emptySet())

            // Every presentation consumes the same YouTube Music FEmusic_home feed.
            // Playback-provider settings do not alter discovery/home content.
            val savedCookie = context.dataStore.get(
                com.darkxvenom.airbeats.constants.InnerTubeCookieKey, ""
            )
            YouTube.cookie = savedCookie
            YouTube.useLoginForBrowse = true

            val remote = com.darkxvenom.airbeats.backend.HomeBackend.load(excludedSongIds)
            homePage.value = remote.page
            accountPlaylists.value = remote.playlists

            val feedSongs = remote.page?.sections.orEmpty()
                .flatMap { it.items }
                .filterIsInstance<SongItem>()
                .filterNot { it.id in excludedSongIds }
                .distinctBy { it.id }

            quickPicks.value = feedSongs.take(20).map(::mapToSong).takeIf { it.isNotEmpty() }
            forgottenFavorites.value = null
            keepListening.value = null
            similarRecommendations.value = null
            explorePage.value = null
            recentActivity.value = null
            recentPlaylistsDb.value = null
            allLocalItems.value = emptyList()
            allYtItems.value = remote.page?.sections.orEmpty()
                .flatMap { it.items }
                .filterNot { it.id in excludedSongIds }
                .distinctBy { it.id }
        } catch (error: Throwable) {
            reportException(error)
            homePage.value = null
            accountPlaylists.value = emptyList()
            allLocalItems.value = emptyList()
            allYtItems.value = emptyList()
        } finally {
            isLoading.value = false
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        loadJob?.cancel()
        // Set this before launching. Setting it inside the coroutine leaves a
        // race where repeated UI events can each cancel and restart Home.
        isRefreshing.value = true
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                load()
            } finally {
                isRefreshing.value = false
            }
        }
    }

    /** Refresh the personalised YouTube Music home feed exactly once per login/logout. */
    fun onAccountChanged(cookie: String) {
        val fingerprint = cookie.hashCode()
        if (accountFingerprint == fingerprint) return
        val isFirst = (accountFingerprint == null)
        accountFingerprint = fingerprint
        // The application collector is asynchronous; set the client now so the
        // refresh cannot accidentally request the anonymous home feed.
        YouTube.cookie = cookie
        if (!isFirst) {
            loadJob?.cancel()
            loadJob = viewModelScope.launch(Dispatchers.IO) {
                load()
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            database.observeExcludedSongIds().collect { excludedIds ->
                val excludedSet = excludedIds.toHashSet()
                filterHomeContent(excludedSet)
            }
        }
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }
}
