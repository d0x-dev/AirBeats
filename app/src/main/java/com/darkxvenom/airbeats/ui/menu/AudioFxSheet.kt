package com.darkxvenom.airbeats.ui.menu

import androidx.compose.ui.res.stringResource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.LiquidGlassKey
import com.darkxvenom.airbeats.playback.DjPreset
import com.darkxvenom.airbeats.ui.component.LocalBackdrop
import com.darkxvenom.airbeats.ui.component.drawBackdropCustomShape
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.utils.rememberPreference
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InAppAudioFxSheet(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val service = playerConnection.service

    val bypassAllAudioEffects by service.bypassAllAudioEffects.collectAsState()
    val audioFxAndDjEnabled by service.audioFxAndDjEnabled.collectAsState()

    val boostPercent by service.audioBoostPercent.collectAsState()
    val boostEnabled by service.audioBoostEnabled.collectAsState()
    val stats by service.visualizerManager.stats.collectAsState()

    val echoEnabled by service.echoEnabled.collectAsState()
    val echoDelayMs by service.echoDelayMs.collectAsState()
    val echoFeedback by service.echoFeedback.collectAsState()
    val echoWetMix by service.echoWetMix.collectAsState()
    val echoPingPong by service.echoPingPong.collectAsState()

    val djFilterSweep by service.djFilterSweep.collectAsState()
    val djFlangerEnabled by service.djFlangerEnabled.collectAsState()
    val djFlangerRate by service.djFlangerRate.collectAsState()
    val djFlangerDepth by service.djFlangerDepth.collectAsState()
    val djSaturation by service.djSaturation.collectAsState()

    val djTempoSpeed by service.djTempoSpeed.collectAsState()
    val djPitch by service.djPitch.collectAsState()
    val djTurntableLinked by service.djTurntableLinked.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Master, 1: Echo, 2: Mixer, 3: Turntable

    LaunchedEffect(Unit) {
        service.ensureVisualizer()
    }

    val (enableLiquidGlass) = rememberPreference(LiquidGlassKey, false)
    val isFrosted = isFrostedGlassUiEnabled()
    val backdrop = LocalBackdrop.current
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (enableLiquidGlass && !isFrosted && backdrop != null) {
            Color.Transparent
        } else if (isFrosted) {
            if (isDark) Color(0xFF111216).copy(alpha = 0.95f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        } else {
            Color(0xFF111216)
        },
        shape = sheetShape,
        modifier = Modifier.then(
            if (enableLiquidGlass && !isFrosted && backdrop != null) {
                Modifier.drawBackdropCustomShape(backdrop = backdrop, layer = layer, luminanceAnimation = luminanceAnimation.value, shape = sheetShape)
            } else if (isFrosted) {
                Modifier.border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    sheetShape
                )
            } else {
                Modifier
            }
        ),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(
                        color = Color(0xFF383A42),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Audio FX + STUDIO DJ SUITE and Reset Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF28131C))
                            .border(BorderStroke(1.dp, Color(0xFFFF2A6D).copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_dj_console),
                            contentDescription = null,
                            tint = Color(0xFFFF2A6D),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.ui_remaining_audio_fx_dj_studio),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.ui_text_professional_sound_sculpting),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 1.4.sp,
                                color = Color(0xFF6E7280)
                            )
                        )
                    }
                }

                // Reset Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF28131C))
                        .border(BorderStroke(1.dp, Color(0xFFFF2A6D).copy(alpha = 0.35f)), RoundedCornerShape(50))
                        .clickable {
                            service.resetAudioFx()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.refresh),
                            contentDescription = null,
                            tint = Color(0xFFFF2A6D),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.ui_text_reset_all),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFFF2A6D)
                            )
                        )
                    }
                }
            }

            if (bypassAllAudioEffects) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230)),
                    border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.auto_awesome),
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.ui_text_pure_audio_bypass_active),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.ui_sweep_all_dj_effects_audio_filters_and_equalizers_are_locked_the_audio_),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA0A5B5)
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { service.setBypassAllAudioEffects(false) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f))
                        ) {
                            Text(stringResource(R.string.ui_text_disable_pure_audio_bypass), color = Color(0xFF00E5FF), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (audioFxAndDjEnabled) Color(0xFF28131C) else Color(0xFF1E1F24)),
                    border = BorderStroke(1.dp, if (audioFxAndDjEnabled) Color(0xFFFF2A6D).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ui_text_studio_fx_engine),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (audioFxAndDjEnabled) "Active • Custom DSP processing enabled" else "Turned Off • All options reset to pure source playback",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (audioFxAndDjEnabled) Color(0xFFFF2A6D) else Color(0xFF888B96)
                            )
                        }
                        Switch(
                            checked = audioFxAndDjEnabled,
                            onCheckedChange = { enabled ->
                                service.setAudioFxAndDjEnabled(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF2A6D)
                            )
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !bypassAllAudioEffects && audioFxAndDjEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Studio Navigation Tabs
                    StudioTabs(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                echoActive = echoEnabled,
                filterActive = abs(djFilterSweep) > 0.05f || djFlangerEnabled,
                pitchActive = djTempoSpeed != 1.0f || djPitch != 1.0f
            )

            Spacer(modifier = Modifier.height(18.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 0: MASTER FX
                    // Three Reactive Circular Visualizer Dials
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AudioVisualizerDial(
                            title = "BASS",
                            dbValue = stats.bassDb,
                            energy = stats.bass,
                            isPeak = stats.isBassPeak,
                            accentDefaultColor = Color(0xFFFF2A6D),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        AudioVisualizerDial(
                            title = "MID",
                            dbValue = stats.midDb,
                            energy = stats.mid,
                            isPeak = false,
                            accentDefaultColor = Color(0xFFFFFFFF),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        AudioVisualizerDial(
                            title = "TREBLE",
                            dbValue = stats.trebleDb,
                            energy = stats.treble,
                            isPeak = false,
                            accentDefaultColor = Color(0xFF00E5FF),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Master Boost Card
                    MasterBoostCard(
                        boostPercent = boostPercent,
                        boostEnabled = boostEnabled,
                        onBoostPercentChange = { percent ->
                            service.setAudioBoostPercent(percent)
                            if (!boostEnabled && percent > 100) {
                                service.setAudioBoostEnabled(true)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // DJ Presets Section
                    DjPresetsRow(
                        onSelectPreset = { preset ->
                            service.applyDjPreset(preset)
                        }
                    )
                }

                1 -> {
                    // TAB 1: ECHO & DELAY
                    EchoDelaySection(
                        enabled = echoEnabled,
                        delayMs = echoDelayMs,
                        feedback = echoFeedback,
                        wetMix = echoWetMix,
                        pingPong = echoPingPong,
                        onToggle = { service.setEchoEnabled(it) },
                        onDelayMsChange = { service.setEchoDelayMs(it) },
                        onFeedbackChange = { service.setEchoFeedback(it) },
                        onWetMixChange = { service.setEchoWetMix(it) },
                        onPingPongChange = { service.setEchoPingPong(it) }
                    )
                }

                2 -> {
                    // TAB 2: DJ MIXER & FILTERS
                    DjMixerSection(
                        filterSweep = djFilterSweep,
                        flangerEnabled = djFlangerEnabled,
                        flangerRate = djFlangerRate,
                        flangerDepth = djFlangerDepth,
                        saturation = djSaturation,
                        onFilterSweepChange = { service.setDjFilterSweep(it) },
                        onFlangerToggle = { service.setDjFlangerEnabled(it) },
                        onFlangerRateChange = { service.setDjFlangerRate(it) },
                        onFlangerDepthChange = { service.setDjFlangerDepth(it) },
                        onSaturationChange = { service.setDjSaturation(it) }
                    )
                }

                3 -> {
                    // TAB 3: TURNTABLE & TEMPO
                    DjTurntableSection(
                        speed = djTempoSpeed,
                        pitch = djPitch,
                        linked = djTurntableLinked,
                        onSpeedChange = { s ->
                            if (djTurntableLinked) {
                                service.setDjTempoAndPitch(s, s)
                            } else {
                                service.setDjTempoAndPitch(s, djPitch)
                            }
                        },
                        onPitchChange = { p ->
                            if (djTurntableLinked) {
                                service.setDjTempoAndPitch(p, p)
                            } else {
                                service.setDjTempoAndPitch(djTempoSpeed, p)
                            }
                        },
                        onLinkedToggle = { service.setDjTurntableLinked(it) },
                        onReset = { service.setDjTempoAndPitch(1.0f, 1.0f) }
                    )
                }
            }
        }
    }
}
}
}

private data class StudioTabItem(
    val title: String,
    val iconRes: Int,
    val isActive: Boolean
)

/**
 * Studio Tabs bar with active glow indicators.
 */
@Composable
private fun StudioTabs(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    echoActive: Boolean,
    filterActive: Boolean,
    pitchActive: Boolean
) {
    val tabs = listOf(
        StudioTabItem("Master", R.drawable.graphic_eq, false),
        StudioTabItem("Echo", R.drawable.waves, echoActive),
        StudioTabItem("Mixer", R.drawable.tune, filterActive),
        StudioTabItem("Turntable", R.drawable.album, pitchActive)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF17191F))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedTab == index
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF28131C) else Color.Transparent,
                label = "tabBg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFFFF2A6D) else Color(0xFF8B8F9D),
                label = "tabText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable { onTabSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = textColor
                        )
                    )
                    if (tab.isActive && !isSelected) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E5FF))
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1-Tap DJ Presets list for instant track transformation.
 */
@Composable
private fun DjPresetsRow(onSelectPreset: (DjPreset) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF17191E))
            .border(BorderStroke(1.dp, Color(0xFF22252C)), RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ui_text_instant_dj_presets),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = Color(0xFF8B8F9D)
                )
            )
            Text(
                text = stringResource(R.string.ui_sweep_1_tap_transform),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color(0xFFFF2A6D)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DjPreset.entries.forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF21232B))
                        .border(BorderStroke(1.dp, Color(0xFF2E323D)), RoundedCornerShape(14.dp))
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF2B2E38)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(preset.iconRes),
                                    contentDescription = null,
                                    tint = Color(0xFFFF2A6D),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = Color(0xFF7A7E8D)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Echo & Delay Studio Panel.
 */
@Composable
private fun EchoDelaySection(
    enabled: Boolean,
    delayMs: Int,
    feedback: Float,
    wetMix: Float,
    pingPong: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelayMsChange: (Int) -> Unit,
    onFeedbackChange: (Float) -> Unit,
    onWetMixChange: (Float) -> Unit,
    onPingPongChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF17191E))
            .border(BorderStroke(1.dp, if (enabled) Color(0xFFFF2A6D).copy(alpha = 0.4f) else Color(0xFF22252C)), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // Echo Header with Master Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.ui_sweep_stereo_echo_delay),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = if (enabled) "Spatial repeating reflections active" else "Effect bypassed",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = if (enabled) Color(0xFFFF2A6D) else Color(0xFF6E7280)
                    )
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFFF2A6D),
                    uncheckedTrackColor = Color(0xFF282A32)
                )
            )
        }

        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                // Ping Pong Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1F222A))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.ui_sweep_3d_ping_pong_bounce),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = stringResource(R.string.ui_sweep_alternates_echoes_left_right),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Color(0xFF7A7E8D)
                            )
                        )
                    }

                    Switch(
                        checked = pingPong,
                        onCheckedChange = onPingPongChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E5FF),
                            uncheckedTrackColor = Color(0xFF2E313C)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Delay Time Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ui_text_delay_time),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD0D3DC)
                        )
                    )
                    Text(
                        text = "${delayMs}ms",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF2A6D)
                        )
                    )
                }
                Slider(
                    value = delayMs.toFloat(),
                    onValueChange = { onDelayMsChange(it.toInt()) },
                    valueRange = 40f..800f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF2A6D),
                        activeTrackColor = Color(0xFFFF2A6D),
                        inactiveTrackColor = Color(0xFF282A32)
                    )
                )

                // Feedback Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ui_sweep_feedback_repeats),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD0D3DC)
                        )
                    )
                    Text(
                        text = "${(feedback * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    )
                }
                Slider(
                    value = feedback,
                    onValueChange = onFeedbackChange,
                    valueRange = 0.05f..0.80f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF282A32)
                    )
                )

                // Wet Mix Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ui_sweep_wet_dry_mix),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD0D3DC)
                        )
                    )
                    Text(
                        text = "${(wetMix * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB703)
                        )
                    )
                }
                Slider(
                    value = wetMix,
                    onValueChange = onWetMixChange,
                    valueRange = 0.10f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFB703),
                        activeTrackColor = Color(0xFFFFB703),
                        inactiveTrackColor = Color(0xFF282A32)
                    )
                )

                // Quick Delay Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Slapback (90ms)" to (90 to 0.20f),
                        "Dub (280ms)" to (280 to 0.45f),
                        "Space (480ms)" to (480 to 0.65f)
                    ).forEach { (name, pair) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF21232B))
                                .clickable {
                                    onDelayMsChange(pair.first)
                                    onFeedbackChange(pair.second)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * DJ Mixer & Filters (Club Resonant Filter Sweep, Flanger, Saturation).
 */
@Composable
private fun DjMixerSection(
    filterSweep: Float,
    flangerEnabled: Boolean,
    flangerRate: Float,
    flangerDepth: Float,
    saturation: Float,
    onFilterSweepChange: (Float) -> Unit,
    onFlangerToggle: (Boolean) -> Unit,
    onFlangerRateChange: (Float) -> Unit,
    onFlangerDepthChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF17191E))
            .border(BorderStroke(1.dp, Color(0xFF22252C)), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // BIPOLAR DJ FILTER SWEEP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.ui_text_dj_filter_sweep),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = when {
                        filterSweep < -0.05f -> "Low-Pass Club Muffle (${(filterSweep * 100).toInt()}%)"
                        filterSweep > 0.05f -> "High-Pass Bass Cut Drop (+${(filterSweep * 100).toInt()}%)"
                        else -> "Flat / Center (Bypassed)"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = when {
                            filterSweep < -0.05f -> Color(0xFF00E5FF)
                            filterSweep > 0.05f -> Color(0xFFFF2A6D)
                            else -> Color(0xFF6E7280)
                        }
                    )
                )
            }

            // Center Snap Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF21242C))
                    .clickable { onFilterSweepChange(0.0f) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_text_snap_center),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0D3DC)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visual LPF <— FLAT —> HPF bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(R.string.ui_remaining_low_pass_muffle), fontSize = 10.sp, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.ui_text_flat), fontSize = 10.sp, color = Color(0xFF8B8F9D), fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.ui_remaining_high_pass_cut), fontSize = 10.sp, color = Color(0xFFFF2A6D), fontWeight = FontWeight.Bold)
        }

        Slider(
            value = filterSweep,
            onValueChange = onFilterSweepChange,
            valueRange = -1.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = if (filterSweep < 0f) Color(0xFF00E5FF) else Color(0xFFFF2A6D),
                activeTrackColor = Color(0xFFFF2A6D),
                inactiveTrackColor = Color(0xFF00E5FF)
            )
        )

        Spacer(modifier = Modifier.height(18.dp))

        // FLANGER SWEEP
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.ui_text_jet_flanger_swoosh),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = if (flangerEnabled) "Dynamic jet-plane phase sweep" else "Disabled",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = if (flangerEnabled) Color(0xFF9D4EDD) else Color(0xFF6E7280)
                    )
                )
            }

            Switch(
                checked = flangerEnabled,
                onCheckedChange = onFlangerToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF9D4EDD),
                    uncheckedTrackColor = Color(0xFF282A32)
                )
            )
        }

        AnimatedVisibility(visible = flangerEnabled) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.ui_dynamic_lfo_rate, flangerRate), fontSize = 11.sp, color = Color(0xFFD0D3DC))
                    Text(text = stringResource(R.string.ui_dynamic_flanger_depth, (flangerDepth * 100).toInt()), fontSize = 11.sp, color = Color(0xFF9D4EDD))
                }
                Slider(
                    value = flangerDepth,
                    onValueChange = onFlangerDepthChange,
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF9D4EDD),
                        activeTrackColor = Color(0xFF9D4EDD),
                        inactiveTrackColor = Color(0xFF282A32)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ANALOG SATURATION
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.ui_text_warm_tape_saturation),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = stringResource(R.string.ui_sweep_harmonic_punch_soft_limiting_drive),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color(0xFF6E7280)
                    )
                )
            }
            Text(
                text = "${(saturation * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB703)
                )
            )
        }

        Slider(
            value = saturation,
            onValueChange = onSaturationChange,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFB703),
                activeTrackColor = Color(0xFFFFB703),
                inactiveTrackColor = Color(0xFF282A32)
            )
        )
    }
}

/**
 * DJ Turntable & Pitch/Tempo controls.
 */
@Composable
private fun DjTurntableSection(
    speed: Float,
    pitch: Float,
    linked: Boolean,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onLinkedToggle: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF17191E))
            .border(BorderStroke(1.dp, Color(0xFF22252C)), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        // Turntable Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.ui_sweep_turntable_tempo),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = if (linked) "Vinyl Mode: Pitch linked with Speed" else "Independent Pitch & Tempo",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = if (linked) Color(0xFFFF2A6D) else Color(0xFF6E7280)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF21242C))
                    .clickable { onReset() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_sweep_1_0x_normal),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0D3DC)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vinyl Mode Switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1F222A))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.ui_text_vinyl_turntable_mode),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = stringResource(R.string.ui_sweep_speeding_up_increases_pitch_nightcore_slowed),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = Color(0xFF7A7E8D)
                    )
                )
            }

            Switch(
                checked = linked,
                onCheckedChange = onLinkedToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFFFF2A6D),
                    uncheckedTrackColor = Color(0xFF2E313C)
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Speed Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ui_sweep_playback_speed_bpm),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD0D3DC)
                )
            )
            Text(
                text = "${String.format("%.2f", speed)}x",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF2A6D)
                )
            )
        }
        Slider(
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = 0.50f..2.00f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFF2A6D),
                activeTrackColor = Color(0xFFFF2A6D),
                inactiveTrackColor = Color(0xFF282A32)
            )
        )

        // Pitch Slider (if not linked)
        AnimatedVisibility(visible = !linked) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ui_sweep_pitch_shift_key),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD0D3DC)
                        )
                    )
                    Text(
                        text = "${String.format("%.2f", pitch)}x",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    )
                }
                Slider(
                    value = pitch,
                    onValueChange = onPitchChange,
                    valueRange = 0.50f..2.00f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF282A32)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Tempo Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "0.85x Slowed" to 0.85f,
                "1.00x Pure" to 1.00f,
                "1.25x Nightcore" to 1.25f,
                "1.50x Fast" to 1.50f
            ).forEach { (label, spd) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (speed == spd) Color(0xFF28131C) else Color(0xFF21232B))
                        .border(
                            BorderStroke(
                                1.dp,
                                if (speed == spd) Color(0xFFFF2A6D) else Color.Transparent
                            ),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSpeedChange(spd) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (speed == spd) Color(0xFFFF2A6D) else Color.White
                        )
                    )
                }
            }
        }
    }
}

/**
 * Circular visualizer dial with dynamic White -> Green -> Glowing Red progression.
 */
@Composable
private fun AudioVisualizerDial(
    title: String,
    dbValue: Int,
    energy: Float,
    isPeak: Boolean,
    accentDefaultColor: Color,
    modifier: Modifier = Modifier
) {
    val targetDynamicColor = when {
        isPeak || energy > 0.70f -> Color(0xFFFF1744)
        energy > 0.35f -> {
            val t = ((energy - 0.35f) / 0.35f).coerceIn(0f, 1f)
            lerp(Color(0xFF00E676), Color(0xFFFF1744), t)
        }
        energy > 0.05f -> {
            val t = ((energy - 0.05f) / 0.30f).coerceIn(0f, 1f)
            lerp(Color.White, Color(0xFF00E676), t)
        }
        else -> accentDefaultColor
    }

    val animatedColor by animateColorAsState(
        targetValue = targetDynamicColor,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "dialColor"
    )

    val animatedSweep by animateFloatAsState(
        targetValue = (energy * 360f).coerceIn(0f, 360f),
        animationSpec = tween(80),
        label = "dialSweep"
    )

    Box(
        modifier = modifier
            .height(116.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF17191E))
            .border(
                BorderStroke(
                    1.dp,
                    if (isPeak) animatedColor.copy(alpha = 0.5f) else Color(0xFF22252C)
                ),
                RoundedCornerShape(22.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(86.dp)) {
            val strokeWidth = 5.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            drawCircle(
                color = Color(0xFF141519),
                radius = radius - strokeWidth / 2,
                center = center
            )

            drawCircle(
                color = Color(0xFF22252C),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            if (animatedSweep > 5f) {
                drawArc(
                    color = animatedColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth)
                )
            }

            val pillAngleRad = Math.toRadians(-90.0).toFloat()
            val pillX = center.x + radius * cos(pillAngleRad)
            val pillY = center.y + radius * sin(pillAngleRad)

            drawCircle(
                color = animatedColor,
                radius = 4.dp.toPx(),
                center = Offset(pillX, pillY)
            )

            if (isPeak) {
                drawCircle(
                    color = animatedColor.copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = Offset(pillX, pillY)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = Color(0xFF8B8F9D)
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (dbValue >= 0) "+$dbValue" else "$dbValue",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White
                )
            )
            Text(
                text = "dB",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = Color(0xFF5E6270)
                )
            )
        }
    }
}

/**
 * 200% Master Boost Card with quick preset pills and segmented bar slider.
 */
@Composable
private fun MasterBoostCard(
    boostPercent: Int,
    boostEnabled: Boolean,
    onBoostPercentChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF17191E))
            .border(
                BorderStroke(
                    1.dp,
                    if (boostEnabled) Color(0xFFFF2A6D).copy(alpha = 0.40f) else Color(0xFF22252C)
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.ui_text_master_boost),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.6.sp,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF2A6D))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "200% MAX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                color = Color.White
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.ui_sweep_high_fidelity_hardware_digital_amplifier),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = Color(0xFF6E7280)
                    )
                )
            }

            Text(
                text = "${boostPercent}%",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = if (boostEnabled) Color(0xFFFF2A6D) else Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SegmentedBoostSlider(
            currentPercent = boostPercent,
            onPercentChange = onBoostPercentChange
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(100, 125, 150, 175, 200).forEach { presetPercent ->
                val isSelected = boostPercent == presetPercent
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFF28131C) else Color(0xFF1E2027)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFFF2A6D) else Color.Transparent
                            ),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onBoostPercentChange(presetPercent) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${presetPercent}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) Color(0xFFFF2A6D) else Color(0xFF8B8F9D)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Segmented slider bar matching design system with smooth interactive drag/tap.
 */
@Composable
private fun SegmentedBoostSlider(
    currentPercent: Int,
    onPercentChange: (Int) -> Unit
) {
    var componentWidth by remember { mutableFloatStateOf(1f) }
    val progressFraction = ((currentPercent - 100) / 100f).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .onSizeChanged { componentWidth = it.width.toFloat().coerceAtLeast(1f) }
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF22242B))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / componentWidth).coerceIn(0f, 1f)
                    val newPercent = 100 + (frac * 100).toInt()
                    onPercentChange(newPercent)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val frac = (change.position.x / componentWidth).coerceIn(0f, 1f)
                    val newPercent = 100 + (frac * 100).toInt()
                    onPercentChange(newPercent)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val activeWidth = width * progressFraction

            if (activeWidth > 0) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF2A6D),
                            Color(0xFFFF3377)
                        ),
                        endX = activeWidth
                    ),
                    topLeft = Offset.Zero,
                    size = Size(activeWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(height / 2, height / 2)
                )
            }

            val segments = 10
            for (i in 1 until segments) {
                val segX = width * (i.toFloat() / segments)
                drawLine(
                    color = Color(0xFF17191E),
                    start = Offset(segX, 0f),
                    end = Offset(segX, height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
