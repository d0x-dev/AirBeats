package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.darkxvenom.airbeats.BuildConfig
import com.darkxvenom.airbeats.constants.HomeScreenStyle
import com.darkxvenom.airbeats.constants.HomeScreenStyleKey
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.ui.screens.artist.ArtistItemsScreen
import com.darkxvenom.airbeats.ui.screens.artist.ArtistScreen
import com.darkxvenom.airbeats.ui.screens.artist.ArtistSongsScreen
import com.darkxvenom.airbeats.ui.screens.library.CachePlaylistScreen
import com.darkxvenom.airbeats.ui.screens.library.LibraryScreen
import com.darkxvenom.airbeats.ui.screens.library.PlayfulLibraryScreen
import com.darkxvenom.airbeats.ui.screens.playlist.AutoPlaylistScreen
import com.darkxvenom.airbeats.ui.screens.playlist.LocalPlaylistScreen
import com.darkxvenom.airbeats.ui.screens.playlist.OnlinePlaylistScreen
import com.darkxvenom.airbeats.ui.screens.playlist.TopPlaylistScreen
import com.darkxvenom.airbeats.ui.screens.search.OnlineSearchResult
import com.darkxvenom.airbeats.ui.screens.settings.AboutScreen
import com.darkxvenom.airbeats.ui.screens.settings.AccountSettings
import com.darkxvenom.airbeats.ui.screens.settings.AppearanceSettings
import com.darkxvenom.airbeats.ui.screens.settings.BackupAndRestore
import com.darkxvenom.airbeats.ui.screens.settings.ContentSettings
import com.darkxvenom.airbeats.ui.screens.settings.DiscordLoginScreen
import com.darkxvenom.airbeats.ui.screens.settings.DiscordSettings
import com.darkxvenom.airbeats.ui.screens.settings.PlayerSettings
import com.darkxvenom.airbeats.ui.screens.settings.PrivacySettings
import com.darkxvenom.airbeats.ui.screens.settings.SettingsScreen
import com.darkxvenom.airbeats.ui.screens.settings.StorageSettings

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    playerBottomSheetState: BottomSheetState,
    onSearchClick: () -> Unit,
) {
    composable(Screens.Home.route) {
        val (homeScreenStyle, _) = rememberEnumPreference(
            HomeScreenStyleKey,
            defaultValue = HomeScreenStyle.CLASSIC
        )

        if (homeScreenStyle == HomeScreenStyle.PLAYFUL) {
            PlayfulHomeScreen(navController = navController, playerBottomSheetState = playerBottomSheetState, onSearchClick = onSearchClick)
        } else if (homeScreenStyle == HomeScreenStyle.NEON) {
            NeonHomeScreen(navController = navController)
        } else if (homeScreenStyle == HomeScreenStyle.SPOTIFY) {
            SpotifyHomeScreen(navController = navController)
        } else if (homeScreenStyle == HomeScreenStyle.APPLE) {
            com.darkxvenom.airbeats.ui.screens.apple.AppleHomeScreen(navController = navController)
        } else {
            HomeScreen(navController = navController)
        }
    }

    composable(
        Screens.Library.route,
    ) {
        val (homeScreenStyle, _) = rememberEnumPreference(
            HomeScreenStyleKey,
            defaultValue = HomeScreenStyle.CLASSIC
        )

        if (homeScreenStyle == HomeScreenStyle.PLAYFUL) {
            PlayfulLibraryScreen(
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
                onSearchClick = onSearchClick
            )
        } else if (homeScreenStyle == HomeScreenStyle.NEON) {
            com.darkxvenom.airbeats.ui.screens.library.NeonLibraryScreen(navController = navController)
        } else if (homeScreenStyle == HomeScreenStyle.SPOTIFY) {
            SpotifyLibraryScreen(navController)
        } else if (homeScreenStyle == HomeScreenStyle.APPLE) {
            com.darkxvenom.airbeats.ui.screens.apple.AppleLibraryScreen(navController = navController)
        } else {
            LibraryScreen(navController)
        }
    }
    composable(Screens.Explore.route) {
        val (homeScreenStyle, _) = rememberEnumPreference(
            HomeScreenStyleKey,
            defaultValue = HomeScreenStyle.CLASSIC
        )

        if (homeScreenStyle == HomeScreenStyle.PLAYFUL) {
            PlayfulExploreScreen(
                navController = navController,
                playerBottomSheetState = playerBottomSheetState,
                onSearchClick = onSearchClick
            )
        } else if (homeScreenStyle == HomeScreenStyle.NEON) {
            NeonExploreScreen(navController = navController)
        } else if (homeScreenStyle == HomeScreenStyle.SPOTIFY) {
            SpotifyExploreScreen(navController = navController)
        } else if (homeScreenStyle == HomeScreenStyle.APPLE) {
            com.darkxvenom.airbeats.ui.screens.apple.AppleExploreScreen(navController = navController)
        } else {
            ExploreScreen(navController,scrollBehavior)
        }
    }
    composable(Screens.Search.route) {
        val (homeScreenStyle, _) = rememberEnumPreference(
            HomeScreenStyleKey,
            defaultValue = HomeScreenStyle.CLASSIC
        )
        if (homeScreenStyle == HomeScreenStyle.NEON) {
            com.darkxvenom.airbeats.ui.screens.search.NeonSearchScreen(navController = navController)
        } else if (homeScreenStyle == HomeScreenStyle.APPLE) {
            com.darkxvenom.airbeats.ui.screens.apple.AppleSearchScreen(navController = navController)
        } else {
            SpotifySearchScreen(navController = navController)
        }
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("neon_search") {
        com.darkxvenom.airbeats.ui.screens.search.NeonSearchScreen(navController = navController)
    }
    composable("stats") {
        val (homeScreenStyle, _) = rememberEnumPreference(
            HomeScreenStyleKey,
            defaultValue = HomeScreenStyle.CLASSIC
        )
        if (homeScreenStyle == HomeScreenStyle.APPLE) {
            com.darkxvenom.airbeats.ui.screens.apple.AppleStatsScreen(navController = navController)
        } else {
            StatsScreen(navController)
        }
    }
    composable("account") {
        AccountScreen(navController, scrollBehavior)
    }
    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("insight") {
        InsightScreen(navController)
    }
    composable("listen_together") {
        ListenTogetherScreen(navController, scrollBehavior)
    }
    composable(com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionRoute) {
        com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionScreen(navController)
    }






    composable(
        route = "search/{query}",
        arguments =
            listOf(
                navArgument("query") {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) {
        OnlineSearchResult(navController)
    }
    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) { backStackEntry ->
        val artistId = backStackEntry.arguments?.getString("artistId")!!
        if (artistId.startsWith("LA")) {
            ArtistSongsScreen(navController, scrollBehavior)
        } else {
            ArtistScreen(navController, scrollBehavior)
        }
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }



    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }


    composable("settings") {
        val latestVersion by mutableLongStateOf(BuildConfig.VERSION_CODE.toLong())
        SettingsScreen(latestVersion, navController, scrollBehavior)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior)
    }
    composable("settings/account") {
        AccountSettings(navController, scrollBehavior)
    }
    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }
    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }
    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }
    composable("login") {
        LoginScreen(navController)
    }
}

