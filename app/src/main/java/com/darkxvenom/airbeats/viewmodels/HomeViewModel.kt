package com.darkxvenom.airbeats.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkxvenom.airbeats.data.artist.ArtistInnertubeClient
import com.darkxvenom.airbeats.data.library.HomeFeedRepository
import com.darkxvenom.airbeats.data.model.HomeFeedItem
import com.darkxvenom.airbeats.data.model.HomeFeedShelf
import com.darkxvenom.airbeats.data.model.HomeHeroCard
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.innertube.models.YTItem
import com.darkxvenom.airbeats.innertube.pages.ExplorePage
import com.darkxvenom.airbeats.innertube.pages.HomePage
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val isRefreshing = MutableStateFlow(false)
    val isLoading = HomeFeedRepository.isLoading

    val remoteShelves: StateFlow<List<HomeFeedShelf>> = HomeFeedRepository.remoteShelves
    val accountPlaylistsFeed: StateFlow<List<HomeFeedItem>> = HomeFeedRepository.accountPlaylists
    val subscribedArtistsFeed: StateFlow<List<HomeFeedItem>> = HomeFeedRepository.subscribedArtists
    val likedSongsFeed: StateFlow<List<HomeFeedItem>> = HomeFeedRepository.likedSongs
    val historyItemsFeed: StateFlow<List<HomeFeedItem>> = HomeFeedRepository.historyItems
    val heroCardsFeed: StateFlow<List<HomeHeroCard>> = HomeFeedRepository.heroCards

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

    private suspend fun syncFeedBridge() {
        val savedCookie = context.dataStore.get(
            com.darkxvenom.airbeats.constants.InnerTubeCookieKey, ""
        )
        ArtistInnertubeClient.activeCookie = savedCookie
        YouTube.cookie = savedCookie
        YouTube.useLoginForBrowse = true
        HomeFeedRepository.ensureLoaded(savedCookie)
    }

    fun refresh() {
        if (isRefreshing.value) return
        loadJob?.cancel()
        isRefreshing.value = true
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedCookie = context.dataStore.get(
                    com.darkxvenom.airbeats.constants.InnerTubeCookieKey, ""
                )
                ArtistInnertubeClient.activeCookie = savedCookie
                YouTube.cookie = savedCookie
                HomeFeedRepository.refresh(savedCookie)
            } finally {
                isRefreshing.value = false
            }
        }
    }

    /** Refresh the personalised YouTube Music home feed on login/logout. */
    fun onAccountChanged(cookie: String) {
        val fingerprint = cookie.hashCode()
        if (accountFingerprint == fingerprint) return
        val isFirst = (accountFingerprint == null)
        accountFingerprint = fingerprint
        ArtistInnertubeClient.activeCookie = cookie
        YouTube.cookie = cookie
        if (!isFirst) {
            loadJob?.cancel()
            loadJob = viewModelScope.launch(Dispatchers.IO) {
                HomeFeedRepository.refresh(cookie)
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            syncFeedBridge()
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                HomeFeedRepository.remoteShelves,
                HomeFeedRepository.accountPlaylists,
                HomeFeedRepository.likedSongs,
                HomeFeedRepository.historyItems,
            ) { shelves, playlists, liked, history ->
                val sections = mutableListOf<HomePage.Section>()

                // If account playlists exist, add them as a top section if not already present
                if (playlists.isNotEmpty()) {
                    sections.add(
                        HomePage.Section(
                            title = "Your Playlists",
                            label = "YouTube Music",
                            thumbnail = null,
                            endpoint = null,
                            items = playlists.map { it.toPlaylistItem() },
                        )
                    )
                }

                // If liked songs exist, add them as a section
                if (liked.isNotEmpty()) {
                    sections.add(
                        HomePage.Section(
                            title = "Liked Songs",
                            label = "YouTube Music",
                            thumbnail = null,
                            endpoint = null,
                            items = liked.take(25).map { it.toSongItem() },
                        )
                    )
                }

                // Add all remote shelves from YouTube Music
                shelves.forEach { shelf ->
                    sections.add(
                        HomePage.Section(
                            title = shelf.title,
                            label = shelf.strapline.ifBlank { null },
                            thumbnail = null,
                            endpoint = shelf.moreBrowseId?.let {
                                com.darkxvenom.airbeats.innertube.models.BrowseEndpoint(browseId = it)
                            },
                            items = shelf.items.map { it.toYTItem() },
                        )
                    )
                }

                if (sections.isNotEmpty()) {
                    homePage.value = HomePage(chips = emptyList(), sections = sections)
                }

                // Quick picks: from song shelf or liked songs
                val songShelf = shelves.firstOrNull { it.isSongShelf }
                val songItems = songShelf?.items ?: liked
                if (songItems.isNotEmpty()) {
                    quickPicks.value = songItems.map { it.toSongItem() }.map(::mapToSong).take(20)
                }

                // Account playlists
                accountPlaylists.value = playlists.map { it.toPlaylistItem() }

                // Keep listening & history
                if (history.isNotEmpty()) {
                    keepListening.value = history.take(20).map { it.toSongItem() }.map(::mapToSong)
                    forgottenFavorites.value = history.drop(5).take(15).map { it.toSongItem() }.map(::mapToSong)
                }

                // All YT items for local search & suggestions
                allYtItems.value = sections.flatMap { it.items }
            }.collect()
        }
    }
}
