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

        val excludedSongIds = runCatching { database.getExcludedSongIds().toHashSet() }.getOrDefault(emptySet())
        val musicProvider = context.dataStore.get(com.darkxvenom.airbeats.constants.MusicProviderKey, "YT")
        val isJioSaavn = musicProvider == "JIOSAAVN"

        if (isJioSaavn) {
            com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.getTrendingSongs().onSuccess { songs ->
                val filteredSongs = songs.filter { it.id !in excludedSongIds }
                homePage.value = HomePage(
                    chips = null,
                    sections = listOf(
                        HomePage.Section(
                            title = context.getString(com.darkxvenom.airbeats.R.string.ui_dynamic_trending_songs),
                            label = "JioSaavn",
                            thumbnail = null,
                            endpoint = null,
                            items = filteredSongs
                        )
                    )
                )
                if (quickPicks.value.isNullOrEmpty()) {
                    quickPicks.value = filteredSongs.filterIsInstance<SongItem>().map(::mapToSong).take(20)
                }
            }.onFailure {
                reportException(it)
            }
            explorePage.value = ExplorePage(emptyList(), emptyList())
        }

        supervisorScope {
            // 1. Quick Picks snapshot
            launch(Dispatchers.IO) {
                if (isJioSaavn) return@launch
                val qpList = runCatching { database.quickPicks().first() }.getOrDefault(emptyList()).filter { !it.id.startsWith("JS:") && it.id !in excludedSongIds }
                val rawPicks = if (qpList.isNotEmpty()) {
                    qpList
                } else {
                    runCatching { database.recentSongs(limit = 60).first() }.getOrDefault(emptyList()).filter { !it.id.startsWith("JS:") && it.id !in excludedSongIds }
                }
                quickPicks.value = rawPicks.distinctBy { it.id }.shuffled(homeRandom).take(20).takeIf { it.isNotEmpty() }
            }

            // 2. Keep Listening snapshot
            launch(Dispatchers.IO) {
                val songs = runCatching { database.recentSongs(limit = 50, offset = 0).first() }.getOrDefault(emptyList())
                    .filter { (if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:")) && it.id !in excludedSongIds }
                    .distinctBy { it.id }.shuffled(homeRandom).take(10)
                val albums = runCatching { database.recentAlbums(limit = 50, offset = 0).first() }.getOrDefault(emptyList())
                    .filter { it.album.thumbnailUrl != null && (if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:")) }
                    .distinctBy { it.id }.shuffled(homeRandom).take(5)
                val artists = runCatching { database.recentArtists(limit = 50, offset = 0).first() }.getOrDefault(emptyList())
                    .filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null && (if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:")) }
                    .distinctBy { it.id }.shuffled(homeRandom).take(5)
                keepListening.value = (songs + albums + artists).shuffled(homeRandom).takeIf { it.isNotEmpty() }
            }

            // 3. Forgotten Favorites snapshot
            launch(Dispatchers.IO) {
                val favs = runCatching { database.forgottenFavorites().first() }.getOrDefault(emptyList())
                    .filter { (if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:")) && it.id !in excludedSongIds }
                    .distinctBy { it.id }.shuffled(homeRandom).take(20)
                forgottenFavorites.value = favs.takeIf { it.isNotEmpty() }
            }

            // 4. Remote items snapshot (YouTube)
            if (!isJioSaavn) {
                launch(Dispatchers.IO) {
                    if (YouTube.cookie?.contains("SAPISID=") == true) {
                        YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                            accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
                        }.onFailure { reportException(it) }
                    }
                }

                launch(Dispatchers.IO) {
                    val recentArtistsList = runCatching { database.recentArtists(limit = 10).first() }.getOrDefault(emptyList())
                    val artistRecs = recentArtistsList.filter { it.artist.isYouTubeArtist }.shuffled(homeRandom).take(3).mapNotNull {
                        val items = mutableListOf<YTItem>()
                        YouTube.artist(it.id).onSuccess { page ->
                            items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                            items += page.sections.lastOrNull()?.items.orEmpty()
                        }
                        SimilarRecommendation(title = it, items = items.filter { item -> item.id !in excludedSongIds }.distinctBy { it.id }.shuffled(homeRandom).take(8)).takeIf { it.items.isNotEmpty() }
                    }
                    val songRecs = runCatching { database.recentSongs(limit = 10).first() }.getOrDefault(emptyList()).filter { !it.id.startsWith("JS:") && it.id !in excludedSongIds }.shuffled(homeRandom).take(2).mapNotNull { song ->
                        val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
                        val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                        val rawItems = (page.songs.shuffled(homeRandom).take(8) + page.albums.shuffled(homeRandom).take(4) + page.artists.shuffled(homeRandom).take(4) + page.playlists.shuffled(homeRandom).take(4))
                        val filteredItems = rawItems.filter { it.id !in excludedSongIds }.distinctBy { it.id }.shuffled(homeRandom).take(10)
                        SimilarRecommendation(title = song, items = filteredItems).takeIf { it.items.isNotEmpty() }
                    }
                    similarRecommendations.value = (artistRecs + songRecs).shuffled(homeRandom).takeIf { it.isNotEmpty() }
                }

                launch(Dispatchers.IO) {
                    val enableJioSaavn = context.dataStore.get(com.darkxvenom.airbeats.constants.EnableJioSaavnKey, true)
                    val jioSection = if (enableJioSaavn) {
                        com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.getTrendingSongs().getOrNull()?.let { songs ->
                            val filteredSongs = songs.filter { it.id !in excludedSongIds }
                            if (filteredSongs.isNotEmpty()) {
                                HomePage.Section(
                                    title = context.getString(com.darkxvenom.airbeats.R.string.ui_dynamic_trending_jiosaavn),
                                    label = "JioSaavn",
                                    thumbnail = null,
                                    endpoint = null,
                                    items = filteredSongs
                                )
                            } else null
                        }
                    } else null

                    YouTube.home().onSuccess { page ->
                        val filteredSections = page.sections.mapNotNull { sec ->
                            val filteredItems = sec.items.filter { it.id !in excludedSongIds }
                            if (filteredItems.isNotEmpty()) sec.copy(items = filteredItems) else null
                        }
                        val allSections = listOfNotNull(jioSection) + filteredSections
                        homePage.value = page.copy(sections = allSections)

                        // Dynamic fallback for fresh installs or empty local history
                        val onlineSongs = allSections.flatMap { it.items }.filterIsInstance<SongItem>()
                        if (quickPicks.value.isNullOrEmpty() && onlineSongs.isNotEmpty()) {
                            quickPicks.value = onlineSongs.map(::mapToSong).distinctBy { it.id }.shuffled(homeRandom).take(20)
                        }
                        if (forgottenFavorites.value.isNullOrEmpty() && onlineSongs.size > 10) {
                            forgottenFavorites.value = onlineSongs.drop(10).map(::mapToSong).distinctBy { it.id }.take(20)
                        }
                        if (accountPlaylists.value.isNullOrEmpty()) {
                            val onlinePlaylists = allSections.flatMap { it.items }.filterIsInstance<PlaylistItem>()
                            if (onlinePlaylists.isNotEmpty()) {
                                accountPlaylists.value = onlinePlaylists.distinctBy { it.id }.take(15)
                            }
                        }
                    }.onFailure {
                        if (jioSection != null) {
                            homePage.value = HomePage(chips = null, sections = listOf(jioSection))
                            val onlineSongs = jioSection.items.filterIsInstance<SongItem>()
                            if (quickPicks.value.isNullOrEmpty() && onlineSongs.isNotEmpty()) {
                                quickPicks.value = onlineSongs.map(::mapToSong).distinctBy { it.id }.shuffled(homeRandom).take(20)
                            }
                        }
                        reportException(it)
                    }
                }

                launch(Dispatchers.IO) {
                    YouTube.explore().onSuccess { page ->
                        explorePage.value = page.copy(newReleaseAlbums = page.newReleaseAlbums.filter { item -> item.id !in excludedSongIds })
                    }.onFailure { reportException(it) }
                }
            }
        }

        allLocalItems.value = (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty()).filter { it is Song || it is Album }
        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() + homePage.value?.sections?.flatMap { it.items }.orEmpty() + explorePage.value?.newReleaseAlbums.orEmpty()
        isLoading.value = false
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
