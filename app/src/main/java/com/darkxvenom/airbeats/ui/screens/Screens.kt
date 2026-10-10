package com.darkxvenom.airbeats.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.darkxvenom.airbeats.R

@Immutable
sealed class Screens(
    @StringRes val titleId: Int,
    @DrawableRes val iconIdInactive: Int,
    @DrawableRes val iconIdActive: Int,
    val route: String,
) {
    data object Home : Screens(
        titleId = R.string.home,
        iconIdInactive = R.drawable.home_outlined,
        iconIdActive = R.drawable.home_filled,
        route = "home"
    )

    data object Explore : Screens(
        titleId = R.string.explore,
        iconIdInactive = R.drawable.explore_outlined,
        iconIdActive = R.drawable.explore_filled,
        route = "explore"
    )

    data object Library : Screens(
        titleId = R.string.filter_library,
        iconIdInactive = R.drawable.library_music_outlined,
        iconIdActive = R.drawable.library_music_filled,
        route = "library"
    )

    data object Search : Screens(
        titleId = R.string.search,
        iconIdInactive = R.drawable.search, // Replace with outlined if available
        iconIdActive = R.drawable.search, // Replace with filled if available
        route = "search_home"
    )

    data object Stats : Screens(
        titleId = R.string.stats,
        iconIdInactive = R.drawable.trending_up,
        iconIdActive = R.drawable.trending_up,
        route = "stats"
    )

    data object Generator : Screens(
        titleId = R.string.generator,
        iconIdInactive = R.drawable.auto_awesome,
        iconIdActive = R.drawable.auto_awesome,
        route = "generator"
    )

    data object Charts : Screens(
        titleId = R.string.charts,
        iconIdInactive = R.drawable.trending_up,
        iconIdActive = R.drawable.trending_up,
        route = "charts"
    )

    companion object {
        val MainScreens: List<Screens>
            get() = listOf(Home, Search, Explore, Library, Stats)
    }
}
