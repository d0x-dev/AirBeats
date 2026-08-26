package com.darkxvenom.airbeats.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkxvenom.airbeats.constants.SpotifyCookieKey
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.AlbumItem
import com.darkxvenom.airbeats.innertube.models.ArtistItem
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.utils.completedLibraryPage
import com.darkxvenom.airbeats.spotify.Spotify
import com.darkxvenom.airbeats.spotify.SpotifyAuth
import com.darkxvenom.airbeats.spotify.models.SpotifyHomeFeed
import com.darkxvenom.airbeats.spotify.models.SpotifyPlaylist
import com.darkxvenom.airbeats.spotify.models.SpotifyUser
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.getSuspend
import com.darkxvenom.airbeats.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)

    val spotifyUser = MutableStateFlow<SpotifyUser?>(null)
    val spotifyPlaylists = MutableStateFlow<List<SpotifyPlaylist>?>(null)
    val spotifyFeed = MutableStateFlow<SpotifyHomeFeed?>(null)
    val isSpotifyLoading = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                playlists.value = it.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_liked_albums").completedLibraryPage().onSuccess {
                albums.value = it.items.filterIsInstance<AlbumItem>()
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_library_corpus_artists").completedLibraryPage().onSuccess {
                artists.value = it.items.filterIsInstance<ArtistItem>()
            }.onFailure {
                reportException(it)
            }
        }
    }

    fun loadSpotifyData() {
        if (isSpotifyLoading.value || spotifyUser.value != null) return
        isSpotifyLoading.value = true
        viewModelScope.launch {
            try {
                val spDc = context.dataStore.getSuspend(SpotifyCookieKey)
                if (spDc != null) {
                    val tokenResult = SpotifyAuth.fetchAccessToken(spDc)
                    tokenResult.onSuccess { token ->
                        Spotify.accessToken = token.accessToken
                        
                        Spotify.me().onSuccess { user ->
                            spotifyUser.value = user
                        }.onFailure { reportException(it) }

                        Spotify.myPlaylists(limit = 20).onSuccess { paging ->
                            spotifyPlaylists.value = paging.items
                        }.onFailure { reportException(it) }

                        Spotify.home().onSuccess { feed ->
                            spotifyFeed.value = feed
                        }.onFailure { reportException(it) }
                    }.onFailure {
                        reportException(it)
                    }
                }
            } catch (e: Exception) {
                reportException(e)
            } finally {
                isSpotifyLoading.value = false
            }
        }
    }
}
