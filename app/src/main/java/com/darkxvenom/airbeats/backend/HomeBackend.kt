package com.darkxvenom.airbeats.backend

import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.PlaylistItem
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.pages.HomePage
import com.darkxvenom.airbeats.innertube.utils.completedLibraryPage
import com.darkxvenom.airbeats.jiosaavn.JioSaavnApi

/**
 * One home-feed source for every home layout (Classic, Better, Playful, Material...).
 * Layouts only render what HomeViewModel exposes from here.
 */
object HomeBackend {
    data class Remote(val page: HomePage?, val playlists: List<PlaylistItem>)

    fun isLoggedIn(): Boolean {
        val cookie = YouTube.cookie.orEmpty()
        val jar = cookie.split(';').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else part.substring(0, index).trim() to part.substring(index + 1).trim()
        }.toMap()
        val hasSapisid = listOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID").any { !jar[it].isNullOrBlank() }
        val hasSession = listOf("SID", "__Secure-1PSID", "__Secure-3PSID", "LOGIN_INFO").any { !jar[it].isNullOrBlank() }
        return hasSapisid && hasSession
    }

    suspend fun accountPlaylists(): List<PlaylistItem> =
        if (!isLoggedIn()) emptyList()
        else YouTube.library("FEmusic_liked_playlists").completedLibraryPage().getOrNull()
            ?.items?.filterIsInstance<PlaylistItem>()
            ?.filterNot { it.id in setOf("LM", "SE", "NE", "SS", "SR", "LL", "WL") }
            .orEmpty()

    suspend fun saavnSection(title: String, excluded: Set<String>): HomePage.Section? =
        JioSaavnApi.getTrendingSongs().getOrNull()?.filter { it.id !in excluded }
            ?.takeIf { it.isNotEmpty() }
            ?.let { HomePage.Section(title = title, label = "JioSaavn", thumbnail = null, endpoint = null, items = it) }

    /** YouTube Music home (personalised when signed in) + JioSaavn trending + playlists. */
    suspend fun load(excluded: Set<String>, saavnEnabled: Boolean, saavnTitle: String): Remote {
        val saavn = if (saavnEnabled) saavnSection(saavnTitle, excluded) else null
        val yt = YouTube.home().getOrNull()?.let { page ->
            page.copy(sections = page.sections.mapNotNull { s ->
                s.items.filter { it.id !in excluded }.takeIf { it.isNotEmpty() }?.let { s.copy(items = it) }
            })
        }
        val sections = listOfNotNull(saavn) + yt?.sections.orEmpty()
        val page = (yt ?: if (saavn != null) HomePage(chips = null, sections = emptyList()) else null)
            ?.copy(sections = sections)
        val playlists = accountPlaylists().ifEmpty {
            sections.flatMap { it.items }.filterIsInstance<PlaylistItem>().distinctBy { it.id }.take(15)
        }
        return Remote(page, playlists)
    }
}
