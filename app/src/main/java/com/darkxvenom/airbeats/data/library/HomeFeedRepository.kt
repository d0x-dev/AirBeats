/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of AirBeats and is licensed under GPL-3.0.
 */

package com.darkxvenom.airbeats.data.library

import android.util.Log
import com.darkxvenom.airbeats.data.artist.ArtistInnertubeClient
import com.darkxvenom.airbeats.data.auth.YouTubeAccountParsers
import com.darkxvenom.airbeats.data.auth.hasApiSid
import com.darkxvenom.airbeats.data.model.HomeFeedItem
import com.darkxvenom.airbeats.data.model.HomeFeedShelf
import com.darkxvenom.airbeats.data.model.HomeHeroCard
import com.darkxvenom.airbeats.data.model.HomeItemKind
import com.darkxvenom.airbeats.data.model.HomeShelfGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Real YouTube Music data repository for AirBeats Home screen.
 *
 * - Signed in: account's personalized FEmusic_home feed + account playlists (FEmusic_liked_playlists)
 *   + subscribed artists (FEmusic_library_corpus_artists) + liked songs (FEmusic_liked_videos)
 *   + watch history (FEmusic_history) + podcasts.
 * - Signed out: default YouTube Music home feed + Explore (trending songs, albums, videos)
 *   + Charts (top artists, top songs) + podcasts.
 */
object HomeFeedRepository {
    private const val TAG = "HomeFeedRepository"
    private const val MAX_CONTINUATIONS = 3

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loadJob: Job? = null

    private data class Snapshot(
        val remote: List<HomeFeedShelf>,
        val playlists: List<HomeFeedItem>,
        val artists: List<HomeFeedItem>,
        val liked: List<HomeFeedItem>,
        val history: List<HomeFeedItem>,
    )

    private val cache = ConcurrentHashMap<String, Snapshot>()
    @Volatile private var activeKey: String? = null

    private val _remoteShelves = MutableStateFlow<List<HomeFeedShelf>>(emptyList())
    val remoteShelves: StateFlow<List<HomeFeedShelf>> = _remoteShelves.asStateFlow()

    private val _accountPlaylists = MutableStateFlow<List<HomeFeedItem>>(emptyList())
    val accountPlaylists: StateFlow<List<HomeFeedItem>> = _accountPlaylists.asStateFlow()

    private val _subscribedArtists = MutableStateFlow<List<HomeFeedItem>>(emptyList())
    val subscribedArtists: StateFlow<List<HomeFeedItem>> = _subscribedArtists.asStateFlow()

    private val _likedSongs = MutableStateFlow<List<HomeFeedItem>>(emptyList())
    val likedSongs: StateFlow<List<HomeFeedItem>> = _likedSongs.asStateFlow()

    private val _historyItems = MutableStateFlow<List<HomeFeedItem>>(emptyList())
    val historyItems: StateFlow<List<HomeFeedItem>> = _historyItems.asStateFlow()

    private val _heroCards = MutableStateFlow<List<HomeHeroCard>>(emptyList())
    val heroCards: StateFlow<List<HomeHeroCard>> = _heroCards.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    private fun sessionKey(cookie: String?): String {
        val signed = hasApiSid(cookie)
        return if (signed) "user_${cookie.hashCode()}" else "anon"
    }

    fun ensureLoaded(cookie: String?) {
        val key = sessionKey(cookie)
        if (key != activeKey) {
            activeKey = key
            val cached = cache[key]
            if (cached != null) {
                _remoteShelves.value = cached.remote
                _accountPlaylists.value = cached.playlists
                _subscribedArtists.value = cached.artists
                _likedSongs.value = cached.liked
                _historyItems.value = cached.history
                _heroCards.value = buildHeroCards(cached.remote, cached.playlists, hasApiSid(cookie))
            }
            load(cookie, key)
        } else if (_remoteShelves.value.isEmpty() && loadJob?.isActive != true) {
            load(cookie, key)
        }
    }

    fun refresh(cookie: String?) {
        val key = sessionKey(cookie)
        activeKey = key
        load(cookie, key)
    }

    private fun load(cookie: String?, key: String) {
        loadJob?.cancel()
        loadJob = scope.launch {
            _isLoading.value = true
            _failed.value = false
            val signed = hasApiSid(cookie)
            try {
                if (signed) {
                    val homeTask = async { runCatching { loadHome(signed = true) }.getOrDefault(emptyList()) }
                    val playlistsTask = async { runCatching { loadLibraryPlaylists() }.getOrDefault(emptyList()) }
                    val artistsTask = async { runCatching { loadSubscribedArtists() }.getOrDefault(emptyList()) }
                    val likesTask = async { runCatching { loadLikedSongs() }.getOrDefault(emptyList()) }
                    val historyTask = async { runCatching { loadHistory() }.getOrDefault(emptyList()) }
                    val podcastsTask = async {
                        runCatching { loadSingle("FEmusic_non_music_audio", HomeShelfGroup.PODCASTS, "podcasts", signed = true) }
                            .getOrDefault(emptyList())
                    }

                    val homeShelves = homeTask.await()
                    val userPlaylists = playlistsTask.await()
                    val userArtists = artistsTask.await()
                    val userLikes = likesTask.await()
                    val userHistory = historyTask.await()
                    val podcasts = podcastsTask.await()

                    val mergedShelves = (homeShelves + podcasts).dedupeByTitle()

                    if (sessionKey(cookie) != key) return@launch

                    _remoteShelves.value = mergedShelves
                    _accountPlaylists.value = userPlaylists
                    _subscribedArtists.value = userArtists
                    _likedSongs.value = userLikes
                    _historyItems.value = userHistory
                    _heroCards.value = buildHeroCards(mergedShelves, userPlaylists, signedIn = true)

                    cache[key] = Snapshot(
                        remote = mergedShelves,
                        playlists = userPlaylists,
                        artists = userArtists,
                        liked = userLikes,
                        history = userHistory,
                    )
                } else {
                    val parts = listOf(
                        async { runCatching { loadHome(signed = false) }.getOrDefault(emptyList()) },
                        async {
                            runCatching { loadSingle("FEmusic_explore", HomeShelfGroup.TRENDING, "explore", signed = false) }
                                .getOrDefault(emptyList())
                        },
                        async {
                            runCatching { loadSingle("FEmusic_charts", HomeShelfGroup.CHARTS, "charts", signed = false) }
                                .getOrDefault(emptyList())
                        },
                        async {
                            runCatching { loadSingle("FEmusic_non_music_audio", HomeShelfGroup.PODCASTS, "podcasts", signed = false) }
                                .getOrDefault(emptyList())
                        },
                    ).awaitAll()

                    val merged = parts.flatten().dedupeByTitle()

                    if (sessionKey(cookie) != key) return@launch

                    if (merged.isNotEmpty()) {
                        _remoteShelves.value = merged
                        _accountPlaylists.value = emptyList()
                        _subscribedArtists.value = emptyList()
                        _likedSongs.value = emptyList()
                        _historyItems.value = emptyList()
                        _heroCards.value = buildHeroCards(merged, emptyList(), signedIn = false)

                        cache[key] = Snapshot(
                            remote = merged,
                            playlists = emptyList(),
                            artists = emptyList(),
                            liked = emptyList(),
                            history = emptyList(),
                        )
                    } else if (_remoteShelves.value.isEmpty()) {
                        _failed.value = true
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.w(TAG, "Error loading home feed: ${e.message}", e)
                if (_remoteShelves.value.isEmpty()) {
                    _failed.value = true
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadHome(signed: Boolean): List<HomeFeedShelf> {
        val out = mutableListOf<HomeFeedShelf>()
        var root = ArtistInnertubeClient.browse("FEmusic_home", anonymous = !signed)
        var page = HomeFeedParser.parse(root, HomeShelfGroup.FEED, "home0")
        out += page.shelves
        var n = 0
        while (page.continuation != null && n < MAX_CONTINUATIONS) {
            n++
            root = runCatching {
                ArtistInnertubeClient.browseContinuation(page.continuation!!, anonymous = !signed)
            }.getOrNull() ?: break
            page = HomeFeedParser.parse(root, HomeShelfGroup.FEED, "home$n")
            out += page.shelves
        }
        return out
    }

    private suspend fun loadSingle(
        browseId: String,
        group: HomeShelfGroup,
        prefix: String,
        signed: Boolean
    ): List<HomeFeedShelf> {
        val root = ArtistInnertubeClient.browse(browseId, anonymous = !signed)
        return HomeFeedParser.parse(root, group, prefix).shelves
    }

    private suspend fun loadLibraryPlaylists(): List<HomeFeedItem> {
        val root = ArtistInnertubeClient.browse("FEmusic_liked_playlists", anonymous = false)
        return YouTubeAccountParsers.parseLibraryPlaylists(root).first
    }

    private suspend fun loadSubscribedArtists(): List<HomeFeedItem> {
        val root = ArtistInnertubeClient.browse("FEmusic_library_corpus_artists", anonymous = false)
        val first = YouTubeAccountParsers.parseLibraryArtists(root).first
        if (first.isNotEmpty()) return first
        // Fallback to corpus track artists
        val fallbackRoot = runCatching {
            ArtistInnertubeClient.browse("FEmusic_library_corpus_track_artists", anonymous = false)
        }.getOrNull() ?: return emptyList()
        return YouTubeAccountParsers.parseLibraryArtists(fallbackRoot).first
    }

    private suspend fun loadLikedSongs(): List<HomeFeedItem> {
        val root = runCatching {
            ArtistInnertubeClient.browse("FEmusic_liked_videos", anonymous = false)
        }.getOrNull() ?: runCatching {
            ArtistInnertubeClient.browse("VLLM", anonymous = false)
        }.getOrNull() ?: return emptyList()
        return YouTubeAccountParsers.parseTracks(root).first
    }

    private suspend fun loadHistory(): List<HomeFeedItem> {
        val root = runCatching {
            ArtistInnertubeClient.browse("FEmusic_history", anonymous = false)
        }.getOrNull() ?: return emptyList()
        return YouTubeAccountParsers.parseTracks(root).first
    }

    private fun List<HomeFeedShelf>.dedupeByTitle(): List<HomeFeedShelf> {
        val seen = HashSet<String>()
        return filter { seen.add(it.title.trim().lowercase() + "|" + it.strapline.trim().lowercase()) }
    }

    private fun buildHeroCards(
        remote: List<HomeFeedShelf>,
        playlists: List<HomeFeedItem>,
        signedIn: Boolean
    ): List<HomeHeroCard> {
        val cards = mutableListOf<HomeHeroCard>()
        val used = HashSet<String>()

        fun add(card: HomeHeroCard?) {
            if (card != null && !card.imageUrl.isNullOrBlank() && used.add(card.item.stableKey)) {
                cards += card
            }
        }

        // 1. First playlist or album from feed / user playlists
        val feedPick = remote.firstOrNull {
            it.group == HomeShelfGroup.FEED && !it.isSongShelf && !it.isArtistShelf &&
                it.items.any { i -> i.kind == HomeItemKind.PLAYLIST || i.kind == HomeItemKind.ALBUM }
        }
        feedPick?.items?.filter { (it.kind == HomeItemKind.PLAYLIST || it.kind == HomeItemKind.ALBUM) && !it.thumbnailUrl.isNullOrBlank() }
            ?.take(2)
            ?.forEachIndexed { idx, item ->
                add(
                    HomeHeroCard(
                        id = if (idx == 0) "hero_feed" else "hero_feed_$idx",
                        category = if (signedIn) "Made for you" else "Featured",
                        title = item.title,
                        subtitle = feedPick.title,
                        caption = item.subtitle.ifBlank { feedPick.strapline.ifBlank { feedPick.title } },
                        imageUrl = item.thumbnailUrl,
                        item = item,
                    ),
                )
            }

        // 2. User account playlist if available
        playlists.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.let { pl ->
            add(
                HomeHeroCard(
                    id = "hero_playlist",
                    category = "Your playlist",
                    title = pl.title,
                    subtitle = pl.subtitle,
                    caption = "From your YouTube Music library",
                    imageUrl = pl.thumbnailUrl,
                    item = pl,
                )
            )
        }

        // 3. Trending / Quick picks song
        val trending = remote.firstOrNull { it.group == HomeShelfGroup.TRENDING && it.isSongShelf }
            ?: remote.firstOrNull { it.title.contains("quick picks", true) && it.isSongShelf }
            ?: remote.firstOrNull { it.isSongShelf }
        trending?.items?.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.let { item ->
            add(
                HomeHeroCard(
                    id = "hero_trending",
                    category = if (trending.group == HomeShelfGroup.TRENDING) "Trending now" else trending.title,
                    title = item.title,
                    subtitle = item.artistName.ifBlank { item.subtitle },
                    caption = "Play ${trending.title} starting with ${item.title}",
                    imageUrl = item.thumbnailUrl,
                    item = item,
                    queue = trending.items,
                ),
            )
        }

        return cards
    }
}
