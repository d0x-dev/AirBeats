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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
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

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

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

    private suspend fun load() {
        isLoading.value = true

        val musicProvider = context.dataStore.get(com.darkxvenom.airbeats.constants.MusicProviderKey, "YT")
        val isJioSaavn = musicProvider == "JIOSAAVN"

        if (isJioSaavn) {
            com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.getTrendingSongs().onSuccess { songs ->
                homePage.value = HomePage(
                    chips = null,
                    sections = listOf(
                        HomePage.Section(
                            title = "Trending Songs",
                            label = "JioSaavn",
                            thumbnail = null,
                            endpoint = null,
                            items = songs
                        )
                    )
                )
                if (quickPicks.value.isNullOrEmpty()) {
                    quickPicks.value = songs.filterIsInstance<SongItem>().map(::mapToSong).take(20)
                }
            }.onFailure {
                reportException(it)
            }
            explorePage.value = ExplorePage(emptyList(), emptyList())
        }

        viewModelScope.launch(Dispatchers.IO) {
            database.quickPicks().collectLatest { qpList ->
                if (isJioSaavn) return@collectLatest
                val filtered = qpList.filter { !it.id.startsWith("JS:") }
                if (filtered.isNotEmpty()) {
                    quickPicks.value = filtered.shuffled().take(20)
                } else {
                    val recentSong = database.recentSongs(limit = 1).first().firstOrNull()
                    if (recentSong != null) {
                        val endpoint = YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint
                        if (endpoint != null) {
                            val page = YouTube.related(endpoint).getOrNull()
                            val songs = page?.songs?.map(::mapToSong)?.shuffled()?.take(20).orEmpty()
                            if (songs.isNotEmpty()) {
                                quickPicks.value = songs
                                return@collectLatest
                            }
                        }
                    }
                    val dbSongs = database.songsByPlayTimeAsc().first().filter { !it.id.startsWith("JS:") }.shuffled().take(20)
                    if (dbSongs.isNotEmpty()) {
                        quickPicks.value = dbSongs
                    } else if (quickPicks.value.isNullOrEmpty()) {
                        val homeResult = homePage.value?.sections?.asSequence()
                            ?.flatMap { it.items.asSequence() }
                            ?.filterIsInstance<SongItem>()
                            ?.map(::mapToSong)
                            ?.take(20)
                            ?.toList()
                            .orEmpty()
                        if (homeResult.isNotEmpty()) {
                            quickPicks.value = homeResult
                        }
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                database.recentSongs(limit = 50, offset = 0),
                database.recentAlbums(limit = 50, offset = 0),
                database.recentArtists(limit = 50, offset = 0)
            ) { songs, albums, artists ->
                val klSongs = songs.filter { if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:") }.shuffled().take(10)
                val klAlbums = albums.filter { it.album.thumbnailUrl != null }.filter { if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:") }.shuffled().take(5)
                val klArtists = artists.filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.filter { if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:") }.shuffled().take(5)
                (klSongs + klAlbums + klArtists).shuffled()
            }.collectLatest { keepListening.value = it }
        }

        viewModelScope.launch(Dispatchers.IO) {
            database.forgottenFavorites().collectLatest { favs ->
                forgottenFavorites.value = favs.filter { if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:") }.shuffled().take(20)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(quickPicks, forgottenFavorites, keepListening) { qp, ff, kl ->
                (qp.orEmpty() + ff.orEmpty() + kl.orEmpty()).filter { it is Song || it is Album }
            }.collectLatest { allLocalItems.value = it }
        }

        if (!isJioSaavn) {
            if (YouTube.cookie != null) {
                YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                    accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
                }.onFailure { reportException(it) }
            }

            viewModelScope.launch(Dispatchers.IO) {
                database.recentArtists(limit = 10).collectLatest { recentList ->
                    val artistRecs = recentList.filter { it.artist.isYouTubeArtist }.shuffled().take(3).mapNotNull {
                        val items = mutableListOf<YTItem>()
                        YouTube.artist(it.id).onSuccess { page ->
                            items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                            items += page.sections.lastOrNull()?.items.orEmpty()
                        }
                        SimilarRecommendation(title = it, items = items.shuffled().take(8)).takeIf { it.items.isNotEmpty() }
                    }
                    val songRecs = database.recentSongs(limit = 10).first().filter { !it.id.startsWith("JS:") }.shuffled().take(2).mapNotNull { song ->
                        val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
                        val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                        SimilarRecommendation(title = song, items = (page.songs.shuffled().take(8) + page.albums.shuffled().take(4) + page.artists.shuffled().take(4) + page.playlists.shuffled().take(4)).shuffled().take(10))
                    }
                    similarRecommendations.value = (artistRecs + songRecs).shuffled()
                }
            }

            YouTube.home().onSuccess { page ->
                homePage.value = page
                if (quickPicks.value.isNullOrEmpty()) {
                    val fallbackPicks = page.sections.asSequence()
                        .flatMap { it.items.asSequence() }
                        .filterIsInstance<SongItem>()
                        .map(::mapToSong)
                        .take(20)
                        .toList()
                    if (fallbackPicks.isNotEmpty()) {
                        quickPicks.value = fallbackPicks
                    }
                }
            }.onFailure { reportException(it) }

            if (quickPicks.value.isNullOrEmpty()) {
                YouTube.search("Trending Songs", YouTube.SearchFilter.FILTER_SONG).onSuccess { res ->
                    val songs = res.items.filterIsInstance<SongItem>().map(::mapToSong).take(20)
                    if (songs.isNotEmpty() && quickPicks.value.isNullOrEmpty()) {
                        quickPicks.value = songs
                    }
                }
            }

            YouTube.explore().onSuccess { explorePage.value = it }.onFailure { reportException(it) }
        }

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() + homePage.value?.sections?.flatMap { it.items }.orEmpty() + explorePage.value?.newReleaseAlbums.orEmpty()
        isLoading.value = false
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }
}
