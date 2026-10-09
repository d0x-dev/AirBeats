package com.darkxvenom.airbeats.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import com.darkxvenom.airbeats.ui.player.DeviceSelectionBottomSheet
import com.darkxvenom.airbeats.ui.player.getAvailableDevices
import com.darkxvenom.airbeats.ui.player.getActiveDevice
import com.darkxvenom.airbeats.ui.player.isBluetoothOutput
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlin.math.abs
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AudioNormalizationKey
import com.darkxvenom.airbeats.constants.AudioQuality
import com.darkxvenom.airbeats.constants.AudioQualityKey
import com.darkxvenom.airbeats.constants.AutoLoadMoreKey
import com.darkxvenom.airbeats.constants.AutomixEnabledKey
import com.darkxvenom.airbeats.constants.AutomixPerformanceMode
import com.darkxvenom.airbeats.constants.AutomixPerformanceModeKey
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.SliderDefaults
import kotlin.math.roundToInt
import com.darkxvenom.airbeats.constants.BypassAllAudioEffectsKey
import com.darkxvenom.airbeats.constants.AudioFxAndDjEnabledKey
import com.darkxvenom.airbeats.constants.DolbyAtmosEnabledKey
import com.darkxvenom.airbeats.constants.SpatialAudioEnabledKey
import com.darkxvenom.airbeats.constants.EightDAudioEnabledKey
import com.darkxvenom.airbeats.constants.EightDAudioLevelKey
import com.darkxvenom.airbeats.constants.BitPerfectEnabledKey
import com.darkxvenom.airbeats.constants.StreamingQualityPresetKey
import com.darkxvenom.airbeats.constants.QualityTiers
import com.darkxvenom.airbeats.playback.DeviceCodecs
import com.darkxvenom.airbeats.ui.component.PreferenceEntry
import com.darkxvenom.airbeats.constants.DownloadQualityKey
import com.darkxvenom.airbeats.constants.CrossfadeKey
import com.darkxvenom.airbeats.constants.CrossfadeEnabledKey
import com.darkxvenom.airbeats.constants.CrossfadeDurationKey
import com.darkxvenom.airbeats.constants.CrossfadeGaplessKey
import com.darkxvenom.airbeats.constants.PermanentShuffleKey
import com.darkxvenom.airbeats.constants.PersistentQueueKey
import com.darkxvenom.airbeats.constants.SimilarContent
import com.darkxvenom.airbeats.constants.SkipSilenceKey
import com.darkxvenom.airbeats.constants.SkipUncachedPartKey
import com.darkxvenom.airbeats.constants.StopMusicOnTaskClearKey
import com.darkxvenom.airbeats.ui.component.CrossfadeSliderPreference
import com.darkxvenom.airbeats.ui.component.EnumListPreference
import com.darkxvenom.airbeats.ui.component.ListPreference
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.component.PreferenceGroupTitle
import com.darkxvenom.airbeats.ui.component.SettingsGeneralCategory
import com.darkxvenom.airbeats.ui.component.SettingsPage
import com.darkxvenom.airbeats.ui.component.SwitchPreference
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val (bypassAllAudioEffects, onBypassAllAudioEffectsChange) = rememberPreference(
        BypassAllAudioEffectsKey,
        defaultValue = false
    )
    val (audioFxAndDjEnabled, onAudioFxAndDjEnabledChange) = rememberPreference(
        AudioFxAndDjEnabledKey,
        defaultValue = false
    )
    val (dolbyAtmos, onDolbyAtmosChange) = rememberPreference(
        DolbyAtmosEnabledKey,
        defaultValue = false
    )
    val dolbyAtmosSupported = remember { DeviceCodecs.playsDolbyAtmos }

    val (spatialAudio, onSpatialAudioChange) = rememberPreference(
        SpatialAudioEnabledKey,
        defaultValue = false
    )
    val (eightDAudio, onEightDAudioChange) = rememberPreference(
        EightDAudioEnabledKey,
        defaultValue = false
    )
    val (eightDAudioLevel, onEightDAudioLevelChange) = rememberPreference(
        EightDAudioLevelKey,
        defaultValue = 8
    )
    val (automix, onAutomixChange) = rememberPreference(
        AutomixEnabledKey,
        defaultValue = false
    )
    val (automixPerformance, onAutomixPerformanceChange) = rememberEnumPreference(
        AutomixPerformanceModeKey,
        defaultValue = AutomixPerformanceMode.BALANCED
    )

    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (downloadQuality, onDownloadQualityChange) = rememberEnumPreference(
        DownloadQualityKey,
        defaultValue = AudioQuality.HIGH
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (permanentShuffle, onPermanentShuffleChange) = rememberPreference(
        PermanentShuffleKey,
        defaultValue = false
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = false
    )
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )

    val (skipUncachedPart, onSkipUncachedPartChange) = rememberPreference(
        SkipUncachedPartKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )
    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(
        CrossfadeEnabledKey,
        defaultValue = false
    )
    val (crossfadeDurationSeconds, onCrossfadeDurationSecondsChange) = rememberPreference(
        CrossfadeDurationKey,
        defaultValue = 5f
    )
    val (crossfadeGapless, onCrossfadeGaplessChange) = rememberPreference(
        CrossfadeGaplessKey,
        defaultValue = true
    )
    val (streamingQualityPreset, onStreamingQualityPresetChange) = rememberPreference(
        StreamingQualityPresetKey,
        defaultValue = QualityTiers.QUALITY_MAX_HI_RES
    )
    val (bitPerfect, onBitPerfectChange) = rememberPreference(
        BitPerfectEnabledKey,
        defaultValue = false
    )
    val (enableJioSaavn, onEnableJioSaavnChange) = rememberPreference(
        com.darkxvenom.airbeats.constants.EnableJioSaavnKey,
        defaultValue = true
    )
    var showQualityDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val service = playerConnection?.service
    val serviceBypassAllAudioEffects by service?.bypassAllAudioEffects?.collectAsState() ?: remember { mutableStateOf(false) }
    val isBypassActive = bypassAllAudioEffects || serviceBypassAllAudioEffects
    val serviceAudioFxAndDjEnabled by service?.audioFxAndDjEnabled?.collectAsState() ?: remember { mutableStateOf(false) }
    val isAudioFxAndDjOn = (!isBypassActive) && (audioFxAndDjEnabled || serviceAudioFxAndDjEnabled)
    val audioBoostEnabled by service?.audioBoostEnabled?.collectAsState() ?: remember { mutableStateOf(false) }
    val audioBoostPercent by service?.audioBoostPercent?.collectAsState() ?: remember { mutableIntStateOf(100) }
    val echoEnabled by service?.echoEnabled?.collectAsState() ?: remember { mutableStateOf(false) }
    val echoDelayMs by service?.echoDelayMs?.collectAsState() ?: remember { mutableIntStateOf(280) }
    val djFilterSweep by service?.djFilterSweep?.collectAsState() ?: remember { mutableFloatStateOf(0.0f) }
    var showAudioFxModal by remember { mutableStateOf(false) }

    var showDeviceSheet by rememberSaveable { mutableStateOf(false) }

    val availableAudioDevices by (playerConnection?.service?.availableAudioDevices?.collectAsState() ?: remember { mutableStateOf(emptyList()) })
    val preferredAudioDevice by (playerConnection?.service?.preferredAudioDevice?.collectAsState() ?: remember { mutableStateOf(null) })

    val fallbackDevices = remember { getAvailableDevices(context) }
    val displayDevices = if (availableAudioDevices.isNotEmpty()) availableAudioDevices else fallbackDevices
    val activeDevice = preferredAudioDevice ?: remember(displayDevices) { getActiveDevice(displayDevices) }
    val isBluetooth = activeDevice?.let { with(it) { isBluetoothOutput() } } ?: false
    val isCasting by (playerConnection?.service?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) })
    val castDeviceName by (playerConnection?.service?.castDeviceName?.collectAsState() ?: remember { mutableStateOf<String?>(null) })

    val hasBluetoothPermission = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    var bluetoothPermissionGranted by remember { mutableStateOf(hasBluetoothPermission) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        bluetoothPermissionGranted = isGranted
        playerConnection?.service?.refreshAudioOutputDevices()
        showDeviceSheet = true
    }

    SettingsPage(
        title = stringResource(R.string.player_and_audio),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {
                    val deviceName = when {
                        isCasting -> castDeviceName ?: "Cast device"
                        activeDevice != null -> activeDevice.productName.toString()
                        else -> "Phone Speaker"
                    }
                    val deviceIcon = when {
                        isCasting -> R.drawable.ic_cast_connected
                        isBluetooth -> R.drawable.ic_bluetooth
                        else -> R.drawable.airplay
                    }
                    PreferenceEntry(
                        title = { Text("Output audio device") },
                        description = deviceName,
                        icon = {
                            Icon(
                                painter = painterResource(deviceIcon),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { showDeviceSheet = true }
                    )
                },

                {
                    val streamingQualitySubtitle = when (streamingQualityPreset) {
                        QualityTiers.QUALITY_DOLBY_ATMOS -> "Dolby Atmos • Spatial Audio"
                        QualityTiers.QUALITY_MAX_HI_RES -> "Max Quality • Up to 24-bit / 192 kHz"
                        QualityTiers.QUALITY_HI_RES_96 -> "Hi-Res Audio • 24-bit / 96 kHz"
                        QualityTiers.QUALITY_CD_LOSSLESS -> "CD Lossless • 16-bit / 44.1 kHz"
                        QualityTiers.QUALITY_MP3_320 -> "Standard Quality • 320 kbps"
                        QualityTiers.QUALITY_DATA_SAVER -> "Data Saver • 96 kbps"
                        else -> "YouTube Music • Native stream"
                    }
                    PreferenceEntry(
                        title = { Text("Streaming Quality") },
                        description = streamingQualitySubtitle,
                        icon = { Icon(Icons.Filled.HighQuality, null) },
                        onClick = { showQualityDialog = true }
                    )
                },

                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_jiosaavn)) },
                        description = stringResource(R.string.enable_jiosaavn_desc),
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        checked = enableJioSaavn,
                        onCheckedChange = onEnableJioSaavnChange,
                    )
                },

                {
                    SwitchPreference(
                        title = { Text("Pure Audio (Direct Source Bypass)") },
                        description = if (isBypassActive) {
                            "ACTIVE • All audio effects, filters, and processors are locked. Bit-exact source audio stream."
                        } else {
                            "Disable and lock every audio effect (Dolby, Spatial, DJ FX, 8D, EQ, Normalization) to play pure untouched audio from source."
                        },
                        icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
                        checked = isBypassActive,
                        onCheckedChange = { enabled ->
                            onBypassAllAudioEffectsChange(enabled)
                            playerConnection?.service?.setBypassAllAudioEffects(enabled)
                        }
                    )
                },

                {
                    SwitchPreference(
                        title = { Text("Bit-Perfect Output") },
                        description = "Bypasses Android audio resampling, software volume scaling, and equalizer when connected to a compatible USB DAC (Android 14+).",
                        icon = { Icon(painterResource(R.drawable.tune), null) },
                        checked = bitPerfect,
                        isEnabled = !isBypassActive,
                        onCheckedChange = { enabled ->
                            onBitPerfectChange(enabled)
                            playerConnection?.service?.setBitPerfectEnabled(enabled)
                        }
                    )
                },

                {EnumListPreference(
                    title = { Text(stringResource(R.string.audio_quality)) },
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    selectedValue = audioQuality,
                    onValueSelected = onAudioQualityChange,
                    valueText = {
                        when (it) {
                            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                            AudioQuality.MEDIUM -> "Medium"
                            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                        }
                    }
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.dolby_atmos)) },
                    description = if (isBypassActive) {
                        "Locked by Pure Audio Bypass"
                    } else {
                        stringResource(
                            if (dolbyAtmosSupported) {
                                R.string.dolby_atmos_subtitle
                            } else {
                                R.string.dolby_atmos_unavailable
                            }
                        )
                    },
                    icon = { Icon(painterResource(R.drawable.ic_dolby_atmos), null) },
                    checked = if (isBypassActive) false else dolbyAtmos,
                    isEnabled = !isBypassActive,
                    onCheckedChange = { enabled ->
                        onDolbyAtmosChange(enabled)
                        playerConnection?.service?.setDolbyAtmosEnabled(enabled)
                    }
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.spatial_audio)) },
                    description = if (isBypassActive) "Locked by Pure Audio Bypass" else stringResource(R.string.spatial_audio_subtitle),
                    icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                    checked = if (isBypassActive) false else spatialAudio,
                    isEnabled = !isBypassActive,
                    onCheckedChange = { enabled ->
                        onSpatialAudioChange(enabled)
                        playerConnection?.service?.setSpatialAudioEnabled(enabled)
                    }
                )},

                {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SwitchPreference(
                            title = { Text("Audio FX & DJ Studio") },
                            description = when {
                                isBypassActive -> "Locked by Pure Audio Bypass"
                                isAudioFxAndDjOn -> if (audioBoostEnabled) "${audioBoostPercent}% Boost Active • Tap to expand controls" else "Studio DJ effects active • Tap to expand controls"
                                else -> "Studio DJ effects: 200% Volume Boost, Echo & Delay, Club Filters, and Slowed/Nightcore"
                            },
                            icon = { Icon(painterResource(R.drawable.ic_dj_console), null) },
                            checked = isAudioFxAndDjOn,
                            isEnabled = !isBypassActive,
                            onCheckedChange = { enabled ->
                                onAudioFxAndDjEnabledChange(enabled)
                                playerConnection?.service?.setAudioFxAndDjEnabled(enabled)
                            }
                        )

                        AnimatedVisibility(
                            visible = isAudioFxAndDjOn,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp, top = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Master Volume Boost",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (audioBoostEnabled) "${audioBoostPercent}%" else "OFF",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (audioBoostEnabled) Color(0xFFFF2A6D) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Switch(
                                            checked = audioBoostEnabled,
                                            onCheckedChange = { boostOn ->
                                                service?.setAudioBoostEnabled(boostOn)
                                                if (boostOn && audioBoostPercent <= 100) {
                                                    service?.setAudioBoostPercent(150)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = Color(0xFFFF2A6D)
                                            )
                                        )
                                    }
                                }
                                if (audioBoostEnabled) {
                                    Slider(
                                        value = audioBoostPercent.toFloat(),
                                        onValueChange = { service?.setAudioBoostPercent(it.toInt()) },
                                        valueRange = 100f..200f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFFFF2A6D),
                                            activeTrackColor = Color(0xFFFF2A6D)
                                        )
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Echo & Delay Effect",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (echoEnabled) {
                                            Text(
                                                text = "${echoDelayMs}ms delay active",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF00E5FF)
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = echoEnabled,
                                        onCheckedChange = { service?.setEchoEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF00E5FF)
                                        )
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DJ Filter Sweep",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when {
                                            djFilterSweep < -0.05f -> "Low-Pass ${(djFilterSweep * 100).toInt()}%"
                                            djFilterSweep > 0.05f -> "High-Pass +${(djFilterSweep * 100).toInt()}%"
                                            else -> "FLAT"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (abs(djFilterSweep) > 0.05f) Color(0xFFFF2A6D) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Slider(
                                    value = djFilterSweep,
                                    onValueChange = { service?.setDjFilterSweep(it) },
                                    valueRange = -1.0f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = if (djFilterSweep < 0f) Color(0xFF00E5FF) else Color(0xFFFF2A6D),
                                        activeTrackColor = Color(0xFFFF2A6D),
                                        inactiveTrackColor = Color(0xFF00E5FF)
                                    )
                                )

                                Spacer(Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF28131C),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAudioFxModal = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_dj_console),
                                            contentDescription = null,
                                            tint = Color(0xFFFF2A6D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Open Full DJ Studio Console",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF2A6D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },

                {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SwitchPreference(
                            title = { Text("8D Audio") },
                            description = when {
                                isBypassActive -> "Locked by Pure Audio Bypass"
                                eightDAudio -> "${eightDAudioLevel}D Spatial Orbit • Best with headphones or dual speakers"
                                else -> "Rotates music in a 360° circle around your head (requires headphones or dual speakers)"
                            },
                            icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                            checked = if (isBypassActive) false else eightDAudio,
                            isEnabled = !isBypassActive,
                            onCheckedChange = { enabled ->
                                onEightDAudioChange(enabled)
                                playerConnection?.service?.setEightDAudioEnabled(enabled)
                            }
                        )

                        AnimatedVisibility(
                            visible = (!isBypassActive) && eightDAudio,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp, top = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "8D Effect Intensity",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                            ) {
                                                Text(
                                                    text = "${eightDAudioLevel}D",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = when {
                                                eightDAudioLevel <= 3 -> "Gentle & Slow Orbit (~25s per circle)"
                                                eightDAudioLevel <= 7 -> "Moderate Spatial Rotation (~14s per circle)"
                                                eightDAudioLevel == 8 -> "Classic 8D Orbit (~9s per circle)"
                                                eightDAudioLevel <= 12 -> "Fast Dynamic Orbit (~6s per circle)"
                                                else -> "Extreme Rapid 16D Orbit (~3.5s per circle)"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                Slider(
                                    value = eightDAudioLevel.toFloat(),
                                    onValueChange = { newLevel ->
                                        val intLevel = newLevel.roundToInt().coerceIn(1, 16)
                                        if (intLevel != eightDAudioLevel) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onEightDAudioLevelChange(intLevel)
                                            playerConnection?.service?.setEightDAudioLevel(intLevel)
                                        }
                                    },
                                    valueRange = 1f..16f,
                                    steps = 14,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "1D (Subtle)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                    Text(
                                        text = "8D (Classic)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                    Text(
                                        text = "16D (Extreme)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                    }
                },

                {PreferenceEntry(
                    title = { Text(stringResource(R.string.dolby_atmos_system_panel)) },
                    description = stringResource(R.string.dolby_atmos_system_panel_desc),
                    icon = { Icon(painterResource(R.drawable.tune), null) },
                    onClick = {
                        DeviceCodecs.openDolbyAtmosSettings(
                            context,
                            playerConnection?.player?.audioSessionId ?: 0
                        )
                    }
                )},

                {EnumListPreference(
                    title = { Text("Download quality") },
                    icon = { Icon(painterResource(R.drawable.download), null) },
                    selectedValue = downloadQuality,
                    values = listOf(AudioQuality.LOW, AudioQuality.MEDIUM, AudioQuality.HIGH),
                    onValueSelected = onDownloadQualityChange,
                    valueText = {
                        when (it) {
                            AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                            AudioQuality.MEDIUM -> "Medium"
                            AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                            AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                        }
                    }
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.permanent_shuffle)) },
                    description = stringResource(R.string.permanent_shuffle_desc),
                    icon = { Icon(painterResource(R.drawable.shuffle), null) },
                    checked = permanentShuffle,
                    onCheckedChange = onPermanentShuffleChange
                )},



                {SwitchPreference(
                    title = { Text(stringResource(R.string.skip_silence)) },
                    icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                    checked = skipSilence,
                    onCheckedChange = onSkipSilenceChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.audio_normalization)) },
                    description = if (isBypassActive) "Locked by Pure Audio Bypass" else null,
                    icon = { Icon(painterResource(R.drawable.volume_up), null) },
                    checked = if (isBypassActive) false else audioNormalization,
                    isEnabled = !isBypassActive,
                    onCheckedChange = onAudioNormalizationChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.automix)) },
                    description = stringResource(
                        if (automix) R.string.automix_enabled_subtitle else R.string.automix_disabled_subtitle
                    ),
                    icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
                    checked = automix,
                    onCheckedChange = { enabled ->
                        onAutomixChange(enabled)
                        playerConnection?.service?.setAutomixEnabled(enabled)
                    }
                )},

                if (automix) {
                    {EnumListPreference(
                        title = { Text(stringResource(R.string.automix_performance)) },
                        icon = { Icon(painterResource(R.drawable.speed), null) },
                        selectedValue = automixPerformance,
                        onValueSelected = { mode ->
                            onAutomixPerformanceChange(mode)
                            playerConnection?.service?.setAutomixPerformanceMode(mode)
                        },
                        valueText = { mode ->
                            when (mode) {
                                AutomixPerformanceMode.EFFICIENT -> stringResource(R.string.automix_mode_efficient)
                                AutomixPerformanceMode.BALANCED -> stringResource(R.string.automix_mode_balanced)
                                AutomixPerformanceMode.PERFORMANCE -> stringResource(R.string.automix_mode_performance)
                            }
                        }
                    )}
                } else {
                    {
                        Column {
                            SwitchPreference(
                                title = { Text(stringResource(R.string.audio_crossfade_title)) },
                                description = stringResource(R.string.audio_crossfade_description),
                                icon = { Icon(painterResource(R.drawable.sync), null) },
                                checked = crossfadeEnabled,
                                onCheckedChange = { enabled ->
                                    onCrossfadeEnabledChange(enabled)
                                },
                            )
                            CrossfadeSliderPreference(
                                valueSeconds = crossfadeDurationSeconds,
                                onValueChange = onCrossfadeDurationSecondsChange,
                                isEnabled = crossfadeEnabled,
                            )
                            SwitchPreference(
                                title = { Text(stringResource(R.string.crossfade_gapless_title)) },
                                description = stringResource(R.string.crossfade_gapless_description),
                                icon = { Icon(painterResource(R.drawable.album), null) },
                                checked = crossfadeGapless,
                                onCheckedChange = onCrossfadeGaplessChange,
                                isEnabled = crossfadeEnabled,
                            )
                        }
                    }
                },
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.queue),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.persistent_queue)) },
                    description = stringResource(R.string.persistent_queue_desc),
                    icon = { Icon(painterResource(R.drawable.queue_music), null) },
                    checked = persistentQueue,
                    onCheckedChange = onPersistentQueueChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.auto_load_more)) },
                    description = stringResource(R.string.auto_load_more_desc),
                    icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                    checked = autoLoadMore,
                    onCheckedChange = onAutoLoadMoreChange
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_similar_content)) },
                    description = stringResource(R.string.similar_content_desc),
                    icon = { Icon(painterResource(R.drawable.similar), null) },
                    checked = similarContentEnabled,
                    onCheckedChange = similarContentEnabledChange,
                )},



                {SwitchPreference(
                    title = { Text(stringResource(R.string.skip_uncached_part)) },
                    description = stringResource(R.string.skip_uncached_part_desc),
                    icon = { Icon(painterResource(R.drawable.cached), null) },
                    checked = skipUncachedPart,
                    onCheckedChange = onSkipUncachedPartChange
                )},
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                    icon = { Icon(painterResource(R.drawable.clear_all), null) },
                    checked = stopMusicOnTaskClear,
                    onCheckedChange = onStopMusicOnTaskClearChange
                )},
            )
        )
    }

    if (showQualityDialog) {
        val tiers = listOf(
            Triple(QualityTiers.QUALITY_DOLBY_ATMOS, "Dolby Atmos", "Spatial Immersive Audio • Tidal Master" to "ATMOS"),
            Triple(QualityTiers.QUALITY_MAX_HI_RES, "Max Quality", "Up to 24-bit / 192 kHz • Lossless Studio FLAC" to "24-BIT / 192k"),
            Triple(QualityTiers.QUALITY_HI_RES_96, "Hi-Res Audio", "24-bit / 96 kHz • Lossless Studio FLAC" to "24-BIT / 96k"),
            Triple(QualityTiers.QUALITY_CD_LOSSLESS, "CD Lossless", "16-bit / 44.1 kHz • Lossless CD FLAC" to "16-BIT / 44.1k"),
            Triple(QualityTiers.QUALITY_MP3_320, "Standard Quality", "320 kbps • MP3 / AAC" to "320 kbps"),
            Triple(QualityTiers.QUALITY_DATA_SAVER, "Data Saver", "96 kbps • High Efficiency AAC" to "96 kbps"),
            Triple(QualityTiers.QUALITY_YOUTUBE, "YouTube Music", "128-256 kbps • YouTube Music AAC / Opus stream" to "YOUTUBE"),
        )
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showQualityDialog = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            dragHandle = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp),
                ) {}
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.HighQuality,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Streaming Quality",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Select preferred audio resolution & bit depth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    "Lossless streams provide bit-exact studio quality. If your chosen quality is unavailable, the player automatically streams the best available tier or falls back to YouTube Music native streams.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tiers.forEach { (qualityId, title, meta) ->
                        val (subtitle, badge) = meta
                        val isSelected = streamingQualityPreset == qualityId
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStreamingQualityPresetChange(qualityId)
                                if (qualityId == QualityTiers.QUALITY_DOLBY_ATMOS) {
                                    playerConnection?.service?.setDolbyAtmosEnabled(true)
                                }
                                showQualityDialog = false
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            shadowElevation = if (isSelected) 3.dp else 0.dp,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ) {
                                            Text(
                                                badge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAudioFxModal) {
        com.darkxvenom.airbeats.ui.menu.InAppAudioFxSheet(
            onDismiss = { showAudioFxModal = false }
        )
    }

    if (showDeviceSheet) {
        DeviceSelectionBottomSheet(
            onDismiss = { showDeviceSheet = false },
            availableDevices = displayDevices,
            activeDevice = activeDevice,
            preferredDevice = preferredAudioDevice,
            onSelectDevice = { device ->
                playerConnection?.service?.setPreferredOutputDevice(device)
            },
            hasBluetoothPermission = bluetoothPermissionGranted,
            onRequestBluetoothPermission = {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        )
    }
}
