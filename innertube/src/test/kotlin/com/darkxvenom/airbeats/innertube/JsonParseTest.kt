package com.darkxvenom.airbeats.innertube

import com.darkxvenom.airbeats.innertube.models.response.SearchResponse
import com.darkxvenom.airbeats.innertube.models.response.GetSearchSuggestionsResponse
import com.darkxvenom.airbeats.innertube.models.getItems
import com.darkxvenom.airbeats.innertube.pages.SearchSuggestionPage
import com.darkxvenom.airbeats.innertube.pages.SearchSummaryPage
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class JsonParseTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun testOnlineSuggestions() = runBlocking {
        println("Calling YouTube.searchSuggestions online...")
        val result = YouTube.searchSuggestions("taylor swift")
        println("Online suggestions result: $result")
        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
        }
    }

    @Test
    fun testParseSuggestions() {
        val file = File("C:/Users/Dark/.gemini/antigravity/scratch/suggestions_response.json")
        assumeTrue("Missing local suggestions_response.json fixture", file.exists())
        val content = file.readText()
        val parsed = json.decodeFromString<GetSearchSuggestionsResponse>(content)
        println("Suggestions contents count: ${parsed.contents?.size}")
        parsed.contents?.forEachIndexed { index, content ->
            val section = content.searchSuggestionsSectionRenderer
            println("Section $index content items count: ${section?.contents?.size}")
            section?.contents?.forEachIndexed { contentIndex, item ->
                val suggestion = item.searchSuggestionRenderer
                val recommended = item.musicResponsiveListItemRenderer
                if (suggestion != null) {
                    println("  Suggestion $contentIndex: ${suggestion.suggestion?.runs?.joinToString("") { it.text }}")
                }
                if (recommended != null) {
                    val result = SearchSuggestionPage.fromMusicResponsiveListItemRenderer(recommended)
                    println("  Recommended $contentIndex parsed to: $result")
                }
            }
        }
    }

    @Test
    fun testParseSearch() {
        val file = File("C:/Users/Dark/.gemini/antigravity/scratch/search_response.json")
        assumeTrue("Missing local search_response.json fixture", file.exists())
        val content = file.readText()
        val parsed = json.decodeFromString<SearchResponse>(content)
        val contents = parsed.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
        if (contents == null) {
            println("Search response contents is null!")
            return
        }
        println("Search contents count: ${contents.size}")
        contents.forEachIndexed { index, it ->
            if (it.musicCardShelfRenderer != null) {
                val card = it.musicCardShelfRenderer
                val parsedCard = SearchSummaryPage.fromMusicCardShelfRenderer(card)
                println("Shelf $index (Card): title='${card.header?.musicCardShelfHeaderBasicRenderer?.title?.runs?.firstOrNull()?.text}', parsed=$parsedCard")
                card.contents?.mapNotNull { it.musicResponsiveListItemRenderer }?.forEachIndexed { itemIndex, item ->
                    val parsedItem = SearchSummaryPage.fromMusicResponsiveListItemRenderer(item)
                    println("  Card Item $itemIndex: parsed=$parsedItem")
                }
            } else if (it.musicShelfRenderer != null) {
                val shelf = it.musicShelfRenderer
                val title = shelf.title?.runs?.firstOrNull()?.text ?: "Other"
                println("Shelf $index (MusicShelf): title='$title'")
                shelf.contents?.getItems()?.forEachIndexed { itemIndex, renderer ->
                    val parsedItem = SearchSummaryPage.fromMusicResponsiveListItemRenderer(renderer)
                    if (parsedItem == null) {
                        println("  Item $itemIndex FAILED parsing! isSong=${renderer.isSong}, isArtist=${renderer.isArtist}, isAlbum=${renderer.isAlbum}, isPlaylist=${renderer.isPlaylist}")
                        // Diagnose why it failed
                        val flexColumns = renderer.flexColumns
                        println("    flexColumns count: ${flexColumns.size}")
                        val secondaryLine = flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                        println("    secondaryLine runs null: ${secondaryLine == null}")
                        if (renderer.isSong) {
                            println("    song videoId: ${renderer.playlistItemData?.videoId}")
                            println("    song nav watchEndpoint videoId: ${renderer.navigationEndpoint?.watchEndpoint?.videoId}")
                        } else if (renderer.isArtist) {
                            println("    artist browseId: ${renderer.navigationEndpoint?.browseEndpoint?.browseId}")
                        } else if (renderer.isAlbum) {
                            println("    album browseId: ${renderer.navigationEndpoint?.browseEndpoint?.browseId}")
                            println("    album play overlay watchPlaylistEndpoint playlistId: ${renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint?.playlistId}")
                        } else if (renderer.isPlaylist) {
                            println("    playlist browseId: ${renderer.navigationEndpoint?.browseEndpoint?.browseId}")
                            println("    playlist play overlay watchPlaylistEndpoint: ${renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint}")
                            println("    playlist menu shuffle watchPlaylistEndpoint: ${renderer.menu?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint}")
                        }
                    } else {
                        println("  Item $itemIndex: title='${parsedItem}', type=${parsedItem::class.simpleName}")
                    }
                }
            } else if (it.itemSectionRenderer != null) {
                val isr = it.itemSectionRenderer
                println("Shelf $index (ItemSectionRenderer): items count=${isr.contents?.size}")
                isr.contents?.getItems()?.forEachIndexed { itemIndex, renderer ->
                    val parsedItem = SearchSummaryPage.fromMusicResponsiveListItemRenderer(renderer)
                    if (parsedItem == null) {
                        println("  ItemSection Item $itemIndex FAILED parsing! isSong=${renderer.isSong}, isArtist=${renderer.isArtist}, isAlbum=${renderer.isAlbum}, isPlaylist=${renderer.isPlaylist}")
                    } else {
                        println("  ItemSection Item $itemIndex: title='${parsedItem}', type=${parsedItem::class.simpleName}")
                    }
                }
            } else {
                println("Shelf $index: unknown renderer")
            }
        }
    }
}
