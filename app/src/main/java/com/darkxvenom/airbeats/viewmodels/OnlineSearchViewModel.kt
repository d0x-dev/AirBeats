/*
 * AirBeats Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.darkxvenom.airbeats.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.filterExplicit
import com.darkxvenom.airbeats.innertube.models.filterVideo
import com.darkxvenom.airbeats.innertube.pages.SearchSummaryPage
import com.darkxvenom.airbeats.constants.HideExplicitKey
import com.darkxvenom.airbeats.constants.HideVideoKey
import com.darkxvenom.airbeats.models.ItemsPage
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get
import com.darkxvenom.airbeats.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.darkxvenom.airbeats.constants.EnableJioSaavnKey
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.pages.SearchSummary
import com.darkxvenom.airbeats.jiosaavn.JioSaavnApi
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()
    var jioSaavnSongs by mutableStateOf<List<SongItem>>(emptyList())
    var isJioSaavnExpanded by mutableStateOf(false)

    init {
        viewModelScope.launch {
            val enableJioSaavn = context.dataStore.get(EnableJioSaavnKey, false)
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            if (enableJioSaavn) {
                launch(Dispatchers.IO) {
                    val jioResult = JioSaavnApi.searchSongs(query).getOrNull().orEmpty()
                    jioSaavnSongs = jioResult.filterExplicit(hideExplicit)
                }
            }
        }

        viewModelScope.launch {
            filter.collect { filter ->
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideo = context.dataStore.get(HideVideoKey, false)

                if (filter == null) {
                    if (summaryPage == null) {
                        YouTube
                            .searchSummary(query)
                            .onSuccess {
                                summaryPage = it.filterExplicit(hideExplicit).filterVideo(hideVideo)
                            }.onFailure {
                                val ytSongs = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                if (ytSongs != null && ytSongs.items.isNotEmpty()) {
                                    summaryPage = SearchSummaryPage(
                                        summaries = listOf(
                                            SearchSummary(
                                                title = "Songs",
                                                items = ytSongs.items
                                                    .distinctBy { it.id }
                                                    .filterExplicit(hideExplicit)
                                                    .filterVideo(hideVideo)
                                            )
                                        )
                                    )
                                } else {
                                    reportException(it)
                                }
                            }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                viewStateMap[filter.value] =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(hideExplicit)
                                            .filterVideo(hideVideo),
                                        result.continuation,
                                    )
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage(
                    (viewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
            }
        }
    }
}

