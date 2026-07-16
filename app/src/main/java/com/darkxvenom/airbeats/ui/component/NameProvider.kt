package com.darkxvenom.airbeats.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.darkxvenom.airbeats.App
import com.darkxvenom.airbeats.utils.AirBeatsStatsCloudSync
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

val LocalUserName = staticCompositionLocalOf { "" }

@Composable
fun NameProvider(
    namePreferenceManager: NamePreferenceManager,
    content: @Composable () -> Unit
) {
    val userName by namePreferenceManager.userName.collectAsState(initial = "")
    var showNameDialog by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Check if name is set when component first loads
    LaunchedEffect(Unit) {
        val isNameSet = namePreferenceManager.isNameSet.first()
        if (!isNameSet) {
            showNameDialog = true
        }
        isInitialized = true
    }

    // Handle name confirmation
    val onNameConfirmed: (String) -> Unit = { name ->
        if (name.isNotBlank()) {
            showNameDialog = false
            // Save to preferences and sync immediately
            scope.launch {
                namePreferenceManager.saveUserName(name)
                try {
                    AirBeatsStatsCloudSync.syncDaily(
                        context = App.instance,
                        database = App.instance.database,
                        namePreferenceManager = namePreferenceManager,
                    )?.onFailure {
                        Timber.e(it, "Failed to sync stats after name confirmation")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception syncing stats after name confirmation")
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalUserName provides userName
    ) {
        content()
    }
}
