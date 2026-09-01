package com.darkxvenom.airbeats.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import com.darkxvenom.airbeats.voice.VoiceAssistantActionExecutor
import com.darkxvenom.airbeats.voice.VoiceAssistantManager
import com.darkxvenom.airbeats.voice.VoiceCommand
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.EnableVoiceAssistantKey
import com.darkxvenom.airbeats.constants.VoiceAssistantAutoStartOnBootKey
import com.darkxvenom.airbeats.constants.VoiceAssistantDirectCommandsKey
import com.darkxvenom.airbeats.constants.VoiceAssistantTtsFeedbackKey
import com.darkxvenom.airbeats.ui.component.PreferenceGroupTitle
import com.darkxvenom.airbeats.ui.component.SettingsGeneralCategory
import com.darkxvenom.airbeats.ui.component.SettingsPage
import com.darkxvenom.airbeats.ui.component.SwitchPreference
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.voice.VoiceAssistantService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (enableVoiceAssistant, onEnableVoiceAssistantChange) = rememberPreference(
        EnableVoiceAssistantKey,
        defaultValue = false
    )
    val (directCommands, onDirectCommandsChange) = rememberPreference(
        VoiceAssistantDirectCommandsKey,
        defaultValue = false
    )
    val (ttsFeedback, onTtsFeedbackChange) = rememberPreference(
        VoiceAssistantTtsFeedbackKey,
        defaultValue = true
    )
    val (autoStartOnBoot, onAutoStartOnBootChange) = rememberPreference(
        VoiceAssistantAutoStartOnBootKey,
        defaultValue = false
    )

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val isIgnoringBatteryOptimizations by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            onEnableVoiceAssistantChange(true)
            VoiceAssistantService.start(context)
        } else {
            Toast.makeText(context, R.string.voice_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    val isServiceRunning by VoiceAssistantService.isServiceRunning.collectAsState()

    SettingsPage(
        title = stringResource(R.string.voice_assistant),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        // Master switch category
        SettingsGeneralCategory(
            title = stringResource(R.string.voice_assistant),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_voice_assistant)) },
                        description = stringResource(R.string.enable_voice_assistant_desc),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.mic),
                                contentDescription = null
                            )
                        },
                        checked = enableVoiceAssistant,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (!hasMicPermission) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    onEnableVoiceAssistantChange(true)
                                    VoiceAssistantService.start(context)
                                }
                            } else {
                                onEnableVoiceAssistantChange(false)
                                VoiceAssistantService.stop(context)
                            }
                        }
                    )
                }
            )
        )

        // Status & Permissions Banner
        if (!hasMicPermission || !isIgnoringBatteryOptimizations || !hasOverlayPermission) {
            PreferenceGroupTitle(title = "Permissions & Enhancements")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!hasMicPermission) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.voice_grant_mic_permission),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.voice_mic_permission_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    }
                }

                if (!hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✨ AirBeats Voice Aura HUD",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Displays an animated glow & live speech sheet at the bottom of your phone when AirBeats listens in the background.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Open Settings -> Apps -> AirBeats -> Display over other apps", Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }

                if (!isIgnoringBatteryOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.voice_disable_battery_opt),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.voice_disable_battery_opt_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Open Settings -> Apps -> AirBeats -> Battery -> Unrestricted", Toast.LENGTH_LONG).show()
                                }
                            }
                        ) {
                            Text("Disable", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Assistant Configuration
        AnimatedVisibility(visible = enableVoiceAssistant) {
            Column {
                SettingsGeneralCategory(
                    title = "Configuration",
                    items = listOf(
                        {
                            SwitchPreference(
                                title = { Text(stringResource(R.string.voice_direct_commands)) },
                                description = stringResource(R.string.voice_direct_commands_desc),
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.mic),
                                        contentDescription = null
                                    )
                                },
                                checked = directCommands,
                                onCheckedChange = onDirectCommandsChange
                            )
                        },
                        {
                            SwitchPreference(
                                title = { Text(stringResource(R.string.voice_tts_feedback)) },
                                description = stringResource(R.string.voice_tts_feedback_desc),
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.volume_up),
                                        contentDescription = null
                                    )
                                },
                                checked = ttsFeedback,
                                onCheckedChange = onTtsFeedbackChange
                            )
                        },
                        {
                            SwitchPreference(
                                title = { Text(stringResource(R.string.voice_auto_start_boot)) },
                                description = stringResource(R.string.voice_auto_start_boot_desc),
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.sync),
                                        contentDescription = null
                                    )
                                },
                                checked = autoStartOnBoot,
                                onCheckedChange = onAutoStartOnBootChange
                            )
                        }
                    )
                )


                // Interactive Live Test Card
                PreferenceGroupTitle(title = stringResource(R.string.voice_test_assistant))
                VoiceTestCard()
            }
        }

        // Commands Guide
        PreferenceGroupTitle(title = stringResource(R.string.voice_commands_guide))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CommandGuideRow(
                phrase = "\"Hi AirBeats, play Starboy\"",
                action = "Plays the requested song or artist"
            )
            CommandGuideRow(
                phrase = "\"Hi AirBeats, pause\" / \"Resume\"",
                action = "Pauses or resumes music playback"
            )
            CommandGuideRow(
                phrase = "\"Hi AirBeats, next song\" / \"Skip\"",
                action = "Skips to the next track in queue"
            )
            CommandGuideRow(
                phrase = "\"Hi AirBeats, previous track\"",
                action = "Plays the previous track"
            )
            CommandGuideRow(
                phrase = "\"Hi AirBeats, like this song\"",
                action = "Toggles favorite / like status"
            )
            CommandGuideRow(
                phrase = "\"Hi AirBeats, volume up\" / \"Volume 80%\"",
                action = "Controls player volume level"
            )
            CommandGuideRow(
                phrase = "\"Hi AirBeats, start radio\"",
                action = "Starts radio based on current song"
            )
        }
    }
}

@Composable
private fun CommandGuideRow(phrase: String, action: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = phrase,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = action,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VoiceTestCard() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = com.darkxvenom.airbeats.playback.PlayerConnection.instance
    var isTesting by remember { mutableStateOf(false) }
    var actionStatus by remember { mutableStateOf("") }

    val actionExecutor = remember {
        VoiceAssistantActionExecutor(
            context = context,
            scope = coroutineScope,
            getMusicService = { playerConnection?.service }
        )
    }

    val testManager = remember {
        VoiceAssistantManager(
            context = context,
            onWakeWordHeard = { /* live text automatically streamed */ },
            onCommandRecognized = { command, text ->
                when (command) {
                    is VoiceCommand.PlayGenericMusic -> {
                        actionStatus = "Loading recommended music..."
                    }
                    is VoiceCommand.PlayCachedSongs -> {
                        actionStatus = "Loading and playing cached library songs..."
                    }
                    is VoiceCommand.PlayLikedSongs -> {
                        actionStatus = "Loading and playing liked songs..."
                    }
                    is VoiceCommand.PlaySong -> {
                        actionStatus = "Searching and playing: \"${command.query}\"..."
                    }
                    is VoiceCommand.Pause -> {
                        actionStatus = "Music paused"
                    }
                    is VoiceCommand.Resume -> {
                        actionStatus = "Music resumed"
                    }
                    is VoiceCommand.NextTrack -> {
                        actionStatus = "Skipping to next song"
                    }
                    is VoiceCommand.PreviousTrack -> {
                        actionStatus = "Playing previous song"
                    }
                    is VoiceCommand.ToggleLike -> {
                        actionStatus = "Toggled favorite"
                    }
                    is VoiceCommand.StartRadio -> {
                        actionStatus = "Starting radio station"
                    }
                    is VoiceCommand.VolumeUp -> {
                        actionStatus = "Volume increased"
                    }
                    is VoiceCommand.VolumeDown -> {
                        actionStatus = "Volume decreased"
                    }
                    is VoiceCommand.SetVolume -> {
                        actionStatus = "Volume set to ${command.levelPercent}%"
                    }
                    is VoiceCommand.Mute -> {
                        actionStatus = "Muted"
                    }
                    is VoiceCommand.Unmute -> {
                        actionStatus = "Unmuted"
                    }
                    is VoiceCommand.Unknown -> {
                        actionStatus = "Heard: \"$text\""
                    }
                }
                actionExecutor.execute(command)
                isTesting = false
            }
        )
    }

    val liveSpokenText by testManager.lastRecognizedText.collectAsState()
    val isMicListening by testManager.isListening.collectAsState()
    val audioRms by testManager.audioRms.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            testManager.destroy()
            actionExecutor.release()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isTesting && isMicListening) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        if (isTesting) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .clickable {
                        if (!isTesting) {
                            isTesting = true
                            actionStatus = "Listening... say \"Play Starboy\" or any command"
                            testManager.triggerListeningSession()
                        } else {
                            isTesting = false
                            testManager.stop()
                            actionStatus = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = null,
                    tint = if (isTesting) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                text = if (isTesting) "Tap mic to stop test" else "Tap mic to test live voice & playback",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            // Live speech display (shows words live as you speak)
            if (isTesting && !liveSpokenText.isNullOrBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🗣️ Spoken words:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "\"$liveSpokenText\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Action status & feedback
            if (actionStatus.isNotBlank()) {
                Text(
                    text = actionStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
