package com.darkxvenom.airbeats.backend

import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.pages.HomePage
import com.darkxvenom.airbeats.innertube.utils.completedLibraryPage

/**
 * Shared YouTube Music home source for every AirBeats home presentation.
 *
 * This repository deliberately does not consult the selected playback provider:
 * JioSaavn may still be used by the player, but it must never replace YouTube
 * Music's home shelves or account playlists.
 */
object HomeBackend {
    data class Remote(
        val page: HomePage?,
        val playlists: List<PlaylistItem>,
    )

    fun isLoggedIn(): Boolean {
        val cookie = YouTube.cookie.orEmpty()
        val jar = cookie.split(';').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null
            else part.substring(0, index).trim() to part.substring(index + 1).trim()
        }.toMap()

        val hasApiSid = listOf(
            "SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID"
        ).any { !jar[it].isNullOrBlank() }
        val hasSession = listOf(
            "SID", "__Secure-1PSID", "__Secure-3PSID", "LOGIN_INFO"
        ).any { !jar[it].isNullOrBlank() }
        return hasApiSid && hasSession
    }

    suspend fun accountPlaylists(): List<PlaylistItem> {
        if (!isLoggedIn()) return emptyList()
        return YouTube.library("FEmusic_liked_playlists")
            .completedLibraryPage()
            .getOrThrow()
            .items
            .filterIsInstance<PlaylistItem>()
            .filterNot { it.id in SYSTEM_PLAYLIST_IDS }
            .distinctBy { it.id }
    }

    /**
     * Fetches YouTube Music's own FEmusic_home feed, follows its continuations,
     * filters excluded tracks, and attaches the signed-in account's playlists.
     */
    suspend fun load(excluded: Set<String>): Remote {
        val pageResult = YouTube.home()
        val page = pageResult.getOrThrow()
        val sections = page.sections.mapNotNull { section ->
            val items = section.items.filterNot { it.id in excluded }
            items.takeIf { it.isNotEmpty() }?.let { section.copy(items = it) }
        }

        // The feed may already contain mixes/playlists. Also expose the actual
        // account library playlists so all screen variants can render them.
        val playlists = accountPlaylists()
        return Remote(page.copy(sections = sections), playlists)
    }

    private val SYSTEM_PLAYLIST_IDS = setOf(
        "LM", "SE", "NE", "SS", "SR", "LL", "WL"
    )
}
