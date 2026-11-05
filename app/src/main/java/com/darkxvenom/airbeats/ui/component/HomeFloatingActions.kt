package com.darkxvenom.airbeats.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalDatabase
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Universal 3-dot Floating Action Button for Home screens.
 * Provides quick access to Smart Shuffle, Music Recognition, and AirBeats Charts.
 */
@Composable
fun BoxScope.HomeFloatingActions(
    navController: NavController,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current

    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
            .then(modifier)
    ) {
        val fabRotation by animateFloatAsState(
            targetValue = if (expanded) 90f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 500f,
            ),
            label = "homeFabRotation",
        )
        val fabScale by animateFloatAsState(
            targetValue = if (expanded) 0.94f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = 500f,
            ),
            label = "homeFabScale",
        )

        FloatingActionButton(
            modifier = Modifier
                .padding(16.dp)
                .scale(fabScale),
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                expanded = true
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = stringResource(R.string.options),
                modifier = Modifier.rotate(fabRotation),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(16.dp)
            )
        ) {
            // Shuffle
            DropdownMenuItem(
                text = { Text(stringResource(R.string.shuffle)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    coroutineScope.launch {
                        val fallbackSong = withContext(Dispatchers.IO) {
                            runCatching { database.recentSongs(15).first().shuffled().firstOrNull() }.getOrNull()
                        }
                        if (fallbackSong != null) {
                            playerConnection?.playQueue(YouTubeQueue.radio(fallbackSong.toMediaMetadata()))
                        }
                    }
                }
            )

            // Music Recognition
            DropdownMenuItem(
                text = { Text(stringResource(R.string.music_recognition)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.navigate(MusicRecognitionRoute)
                }
            )

            // AirBeats Charts
            DropdownMenuItem(
                text = { Text(stringResource(R.string.airbeats_charts)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.trending_up),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                onClick = {
                    expanded = false
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    navController.navigate("charts")
                }
            )
        }
    }
}
