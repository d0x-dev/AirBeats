using System;
using System.IO;
using System.Text.RegularExpressions;

string content = File.ReadAllText("app/src/main/java/com/darkxvenom/airbeats/viewmodels/HomeViewModel.kt");

string newLoad = @"    private suspend fun load() {
        isLoading.value = true

        val musicProvider = context.dataStore.get(com.darkxvenom.airbeats.constants.MusicProviderKey, "YT")
        val isJioSaavn = musicProvider == "JIOSAAVN"

        if (isJioSaavn) {
            com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.getTrendingSongs().onSuccess { songs ->
                homePage.value = HomePage(chips = null, 
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
            }.onFailure {
                reportException(it)
            }
            explorePage.value = ExplorePage(emptyList(), emptyList())
        }

        // 1. Reactive Quick Picks based on latest played song
        viewModelScope.launch(Dispatchers.IO) {
            database.recentSongs(limit = 1).collectLatest { recentList ->
                if (isJioSaavn) {
                    quickPicks.value = emptyList()
                    return@collectLatest
                }
                val recentSong = recentList.firstOrNull()
                if (recentSong != null) {
                    val endpoint = YouTube.next(WatchEndpoint(videoId = recentSong.id)).getOrNull()?.relatedEndpoint
                    if (endpoint != null) {
                        val page = YouTube.related(endpoint).getOrNull()
                        quickPicks.value = page?.songs?.map { it.toMediaMetadata().toSongEntity() as Song }?.shuffled()?.take(20) ?: emptyList()
                    } else quickPicks.value = emptyList()
                } else quickPicks.value = emptyList()
            }
        }

        // 2. Reactive Keep Listening (Listen again)
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.flow.combine(
                database.recentSongs(limit = 50, offset = 0),
                database.recentAlbums(limit = 50, offset = 0),
                database.recentArtists(limit = 50, offset = 0)
            ) { songs, albums, artists ->
                val keepListeningSongs = songs.filter { if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:") }.shuffled().take(10)
                val keepListeningAlbums = albums.filter { it.album.thumbnailUrl != null }.filter { if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:") }.shuffled().take(5)
                val keepListeningArtists = artists.filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }.filter { if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:") }.shuffled().take(5)
                
                (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()
            }.collectLatest {
                keepListening.value = it
            }
        }
        
        // 3. Forgotten Favorites
        viewModelScope.launch(Dispatchers.IO) {
            database.forgottenFavorites().collectLatest { favs ->
                forgottenFavorites.value = favs.filter { if (isJioSaavn) it.id.startsWith("JS:") else !it.id.startsWith("JS:") }.shuffled().take(20)
            }
        }

        // 4. All Local Items
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.flow.combine(
                quickPicks,
                forgottenFavorites,
                keepListening
            ) { qp, ff, kl ->
                (qp.orEmpty() + ff.orEmpty() + kl.orEmpty()).filter { it is Song || it is Album }
            }.collectLatest {
                allLocalItems.value = it
            }
        }

        if (!isJioSaavn) {
            if (YouTube.cookie != null) {
                YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                    accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                        .filterNot { it.id == "SE" }
                }.onFailure {
                    reportException(it)
                }
            }

            // 5. Reactive Similar Recommendations
            viewModelScope.launch(Dispatchers.IO) {
                database.recentArtists(limit = 10).collectLatest { recentList ->
                    val artistRecommendations = recentList
                        .filter { it.artist.isYouTubeArtist }
                        .shuffled().take(3)
                        .mapNotNull {
                            val items = mutableListOf<YTItem>()
                            YouTube.artist(it.id).onSuccess { page ->
                                items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                                items += page.sections.lastOrNull()?.items.orEmpty()
                            }
                            SimilarRecommendation(
                                title = it.artist,
                                items = items.shuffled().take(8)
                            ).takeIf { it.items.isNotEmpty() }
                        }
                    
                    val songRecommendations = database.recentSongs(limit = 10).first()
                        .filter { !it.id.startsWith("JS:") }
                        .shuffled().take(2)
                        .mapNotNull { song ->
                            val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint ?: return@mapNotNull null
                            val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                            SimilarRecommendation(
                                title = song,
                                items = (page.songs.shuffled().take(8) + page.albums.shuffled().take(4) + page.artists.shuffled().take(4) + page.playlists.shuffled().take(4)).shuffled().take(10)
                            )
                        }
                    
                    similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()
                }
            }

            // 6. Generic Home Page
            YouTube.home().onSuccess {
                homePage.value = it
            }.onFailure {
                reportException(it)
            }
            YouTube.explore().onSuccess {
                explorePage.value = it
            }.onFailure {
                reportException(it)
            }
        }
        
        allYtItems.value = homePage.value.sections.flatMap { it.items } +
                explorePage.value.newReleaseAlbums + explorePage.value.moodAndGenres

        isLoading.value = false
    }";

string pattern = @"(?s)    private suspend fun load\(\) \{.*?(?=\n    fun refresh\(\))";
content = Regex.Replace(content, pattern, newLoad);
File.WriteAllText("app/src/main/java/com/darkxvenom/airbeats/viewmodels/HomeViewModel.kt", content);
