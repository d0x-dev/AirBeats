package com.darkxvenom.airbeats.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

val LocalUserName = staticCompositionLocalOf { "" }

@Composable
fun NameProvider(
    namePreferenceManager: NamePreferenceManager,
    content: @Composable () -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var showNameDialog by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    // Check if name is set when component first loads
    LaunchedEffect(Unit) {
        val isNameSet = namePreferenceManager.isNameSet.first()
        if (!isNameSet) {
            showNameDialog = true
        } else {
            userName = namePreferenceManager.userName.first()
        }
        isInitialized = true
    }

    // Handle name confirmation
    val onNameConfirmed: (String) -> Unit = { name ->
        if (name.isNotBlank()) {
            userName = name
            showNameDialog = false
            // Save to preferences
            runBlocking {
                namePreferenceManager.saveUserName(name)
            }
        }
    }

    CompositionLocalProvider(
        LocalUserName provides userName
    ) {
        content()
    }

    // Show dialog if needed
    if (showNameDialog && isInitialized) {
        NameSetupDialog(
            onNameConfirmed = onNameConfirmed,
            onDismiss = {
                // If user skips, set a default name
                onNameConfirmed("Friend")
            }
        )
    }
}