package com.darkxvenom.airbeats.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AndroidAutoConstants
import com.darkxvenom.airbeats.ui.component.PreferenceGroupTitle
import androidx.compose.material3.IconButton
import com.darkxvenom.airbeats.ui.component.SwitchPreference
import com.darkxvenom.airbeats.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidAutoSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (autoEnabled, onAutoEnabledChange) = rememberPreference(AndroidAutoConstants.AndroidAutoEnabledKey, true)
    val (simplifiedMode, onSimplifiedModeChange) = rememberPreference(AndroidAutoConstants.AndroidAutoSimplifiedModeKey, false)
    val (showLiked, onShowLikedChange) = rememberPreference(AndroidAutoConstants.AndroidAutoShowLikedSongsKey, true)
    val (showDownloaded, onShowDownloadedChange) = rememberPreference(AndroidAutoConstants.AndroidAutoShowDownloadedKey, true)
    val (showPlaylists, onShowPlaylistsChange) = rememberPreference(AndroidAutoConstants.AndroidAutoShowYouTubePlaylistsKey, true)
    val (showHistory, onShowHistoryChange) = rememberPreference(AndroidAutoConstants.AndroidAutoShowHistoryKey, true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.android_auto)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
        ) {
            PreferenceGroupTitle(title = stringResource(R.string.android_auto_general))
            
            SwitchPreference(
                title = { Text(stringResource(R.string.android_auto_enable)) },
                description = stringResource(R.string.android_auto_enable_desc),
                icon = { Icon(painterResource(R.drawable.play), null) },
                checked = autoEnabled,
                onCheckedChange = onAutoEnabledChange
            )
            
            SwitchPreference(
                title = { Text(stringResource(R.string.android_auto_simplified_mode)) },
                description = stringResource(R.string.android_auto_simplified_mode_desc),
                icon = { Icon(painterResource(R.drawable.play), null) },
                checked = simplifiedMode,
                onCheckedChange = onSimplifiedModeChange
            )

            PreferenceGroupTitle(title = stringResource(R.string.android_auto_library))

            SwitchPreference(
                title = { Text(stringResource(R.string.android_auto_show_liked_songs)) },
                icon = { Icon(painterResource(R.drawable.play), null) },
                checked = showLiked,
                onCheckedChange = onShowLikedChange
            )

            SwitchPreference(
                title = { Text(stringResource(R.string.android_auto_show_downloaded)) },
                icon = { Icon(painterResource(R.drawable.play), null) },
                checked = showDownloaded,
                onCheckedChange = onShowDownloadedChange
            )

            SwitchPreference(
                title = { Text(stringResource(R.string.android_auto_show_youtube_playlists)) },
                description = stringResource(R.string.android_auto_show_youtube_playlists_desc),
                icon = { Icon(painterResource(R.drawable.play), null) },
                checked = showPlaylists,
                onCheckedChange = onShowPlaylistsChange
            )

            SwitchPreference(
                title = { Text(stringResource(R.string.android_auto_show_history)) },
                icon = { Icon(painterResource(R.drawable.play), null) },
                checked = showHistory,
                onCheckedChange = onShowHistoryChange
            )
        }
    }
}
