package com.darkxvenom.airbeats.ui.screens.settings

import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.EnableMiniPlayerSwipeKey
import com.darkxvenom.airbeats.constants.EnablePlayerDoubleTapSeekKey
import com.darkxvenom.airbeats.constants.EnableSwipeBackGestureKey
import com.darkxvenom.airbeats.constants.EnableTabSwipeGestureKey
import com.darkxvenom.airbeats.ui.component.SettingsGeneralCategory
import com.darkxvenom.airbeats.ui.component.SettingsPage
import com.darkxvenom.airbeats.ui.component.SwitchPreference
import com.darkxvenom.airbeats.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (enableSwipeBack, setEnableSwipeBack) = rememberPreference(
        EnableSwipeBackGestureKey,
        defaultValue = true
    )
    val (enableTabSwipe, setEnableTabSwipe) = rememberPreference(
        EnableTabSwipeGestureKey,
        defaultValue = true
    )
    val (enableMiniPlayerSwipe, setEnableMiniPlayerSwipe) = rememberPreference(
        EnableMiniPlayerSwipeKey,
        defaultValue = true
    )
    val (enableDoubleTapSeek, setEnableDoubleTapSeek) = rememberPreference(
        EnablePlayerDoubleTapSeekKey,
        defaultValue = true
    )

    SettingsPage(
        title = stringResource(R.string.ui_text_gestures),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.ui_heading_navigation_gestures),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.ui_text_swipe_to_go_back)) },
                        description = stringResource(R.string.ui_extra_slide_from_left_edge_towards_the_right_the_screen_follows_your_finge),
                        icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) },
                        checked = enableSwipeBack,
                        onCheckedChange = setEnableSwipeBack
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.ui_text_universal_tab_swipe)) },
                        description = stringResource(R.string.ui_extra_swipe_left_or_right_across_home_explore_and_library_to_switch_screen),
                        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                        checked = enableTabSwipe,
                        onCheckedChange = setEnableTabSwipe
                    )
                },
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.ui_heading_playback_gestures),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.ui_remaining_mini_player_swipe_to_skip)) },
                        description = stringResource(R.string.ui_extra_slide_left_or_right_on_the_bottom_mini_player_bar_to_skip_to_the_nex),
                        icon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                        checked = enableMiniPlayerSwipe,
                        onCheckedChange = setEnableMiniPlayerSwipe
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.ui_text_player_double_tap_seek)) },
                        description = stringResource(R.string.ui_extra_double_tap_the_left_or_right_side_of_the_now_playing_screen_artwork_),
                        icon = { Icon(Icons.Default.FastForward, contentDescription = null) },
                        checked = enableDoubleTapSeek,
                        onCheckedChange = setEnableDoubleTapSeek
                    )
                },
            )
        )
    }
}
