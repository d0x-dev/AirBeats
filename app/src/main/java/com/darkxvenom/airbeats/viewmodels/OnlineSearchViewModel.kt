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

    init {
        viewModelScope.launch {
            val musicProvider = context.dataStore.get(com.darkxvenom.airbeats.constants.MusicProviderKey, "YT")
            filter.collect { filter ->
                if (filter == null) {
                    if (summaryPage == null) {
                        if (musicProvider == "JIOSAAVN") {
                            com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.searchSongs(query)
                                .onSuccess { songs ->
                                    summaryPage = SearchSummaryPage(
                                        summaries = listOf(
                                            com.darkxvenom.airbeats.innertube.pages.SearchSummary(
                                                title = "Songs",
                                                items = songs
                                            )
                                        )
                                    )
                                }.onFailure {
                                    reportException(it)
                                }
                        } else {
                            YouTube
                                .searchSummary(query)
                                .onSuccess {
                                    summaryPage = it.filterExplicit(context.dataStore.get(HideExplicitKey, false)).filterVideo(context.dataStore.get(HideVideoKey, false))
                                }.onFailure {
                                    reportException(it)
                                }
                        }
                    }
                } else {
                    if (viewStateMap[filter.value] == null) {
                        if (musicProvider == "JIOSAAVN") {
                            if (filter == YouTube.SearchFilter.FILTER_SONG) {
                                com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.searchSongs(query)
                                    .onSuccess { songs ->
                                        viewStateMap[filter.value] = ItemsPage(songs, null)
                                    }.onFailure {
                                        reportException(it)
                                    }
                            } else {
                                viewStateMap[filter.value] = ItemsPage(emptyList(), null)
                            }
                        } else {
                            YouTube
                                .search(query, filter)
                                .onSuccess { result ->
                                    viewStateMap[filter.value] =
                                        ItemsPage(
                                            result.items
                                                .distinctBy { it.id }
                                                .filterExplicit(
                                                    context.dataStore.get(
                                                        HideExplicitKey,
                                                        false
                                                    )
                                                ).filterVideo(context.dataStore.get(HideVideoKey, false)),
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

