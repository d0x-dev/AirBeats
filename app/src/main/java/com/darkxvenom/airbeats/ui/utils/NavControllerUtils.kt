package com.darkxvenom.airbeats.ui.utils

import androidx.compose.ui.util.fastAny
import androidx.navigation.NavController
import com.darkxvenom.airbeats.ui.screens.Screens

val NavController.canNavigateUp: Boolean
    get() = currentBackStackEntry?.destination?.parent?.route != null

fun NavController.backToMain() {
    while (canNavigateUp && !Screens.MainScreens.fastAny { it.route == currentBackStackEntry?.destination?.route }) {
        navigateUp()
    }
}
