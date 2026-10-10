package com.darkxvenom.airbeats.ui.player

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import com.darkxvenom.airbeats.lyrics.WordTimestamp
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AutoTranslateKey
import com.darkxvenom.airbeats.constants.DefaultPlayPauseButtonShape
import com.darkxvenom.airbeats.constants.EqualizerPresetKey
import com.darkxvenom.airbeats.constants.PlayPauseButtonShapeKey
import com.darkxvenom.airbeats.constants.TranslateLanguageKey
import com.darkxvenom.airbeats.db.entities.LyricsEntity
import com.darkxvenom.airbeats.lyrics.LyricsEntry
import com.darkxvenom.airbeats.lyrics.LyricsTranslationHelper
import com.darkxvenom.airbeats.lyrics.LyricsUtils
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.playback.PlayerConnection
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.BlurredBackground
import com.darkxvenom.airbeats.ui.component.MenuState
import com.darkxvenom.airbeats.ui.component.SongDetailsDialog
import com.darkxvenom.airbeats.ui.component.AudioPipelineDialog
import com.darkxvenom.airbeats.ui.component.AudioQualityTag
import com.darkxvenom.airbeats.ui.menu.AudioEffectPreset
import com.darkxvenom.airbeats.ui.menu.InAppEqualizerSheet
import com.darkxvenom.airbeats.ui.menu.PlayerMenu
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import com.darkxvenom.airbeats.utils.getPlayPauseShape
import com.darkxvenom.airbeats.utils.makeTimeString
import com.darkxvenom.airbeats.utils.rememberPreference
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow





/**
 * Material Expressive Player matching reference design:
 * - Dynamic Light/Dark artwork backdrop (Light: top blur fading to light surface; Dark: full-screen dark blur)
 * - Rounded album art
 * - Track metadata with Like, Share, More circular quick actions
 * - Tappable lyrics line leading to fullscreen lyrics
 * - Expressive polygon Play/Pause button governed by [PlayPauseButtonShapeKey]
 * - Volume slider
 * - 3 Bottom pill cards: "This phone >" (Audio device sheet), "Custom >" (In-App Equalizer), "Queue >" (Queue sheet)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MaterialPlayer(
    state: BottomSheetState,
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCollapse: () -> Unit,
    onMenuClick: () -> Unit,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    onQueueClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenFullscreenLyrics: () -> Unit = {},
    playerConnection: PlayerConnection,
    navController: NavController,
    menuState: MenuState,
    currentLyrics: LyricsEntity?,
    playerVolume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Clean, high-contrast colors compatible with both Dark and Light themes:
    // In Dark theme: White controls, icons, and text on subtle frosted glass surfaces.
    // In Light theme: Deep dark charcoal controls, icons, and text on soft light surfaces.
    val onBackgroundColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val buttonIconColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val primaryControlColor = if (isDark) Color.White else Color(0xFF1E1E1E)
    val playPauseButtonBg = if (isDark) Color.White else Color(0xFF1E1E1E)
    val playPauseIconColor = if (isDark) Color(0xFF121212) else Color.White

    val surfaceContainer = if (isDark) Color(0x2EFFFFFF) else Color(0x12000000)
    val circularButtonBg = if (isDark) Color(0x2EFFFFFF) else Color(0x12000000)
    val playControlsContainer = if (isDark) Color(0x24FFFFFF) else Color(0x12000000)
    val inactiveTrackColor = if (isDark) Color(0x38FFFFFF) else Color(0x20000000)

    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }
    var showDeviceSheet by rememberSaveable { mutableStateOf(false) }
    var showEqualizerSheet by rememberSaveable { mutableStateOf(false) }
    var showAudioPipelineDialog by rememberSaveable { mutableStateOf(false) }

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    val preferredAudioDevice by playerConnection.service.preferredAudioDevice.collectAsState()
    val availableAudioDevices by playerConnection.service.availableAudioDevices.collectAsState()

    val fallbackDevices = remember { getAvailableDevices(context) }
    val displayDevices = if (availableAudioDevices.isNotEmpty()) availableAudioDevices else fallbackDevices
    val activeDevice = preferredAudioDevice ?: remember(displayDevices) { getActiveDevice(displayDevices) }
    val isBluetooth = activeDevice?.isBluetoothOutput() == true

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
        playerConnection.service.refreshAudioOutputDevices()
        showDeviceSheet = true
    }

    val equalizerState by playerConnection.service.equalizerState.collectAsState()
    val equalizerPreset by rememberPreference(EqualizerPresetKey, "Flat")

    LaunchedEffect(Unit) {
        playerConnection.service.ensureEqualizer()
    }

    val equalizerButtonLabel = remember(equalizerState.enabled, equalizerState.bandLevels, equalizerPreset) {
        if (!equalizerState.enabled) {
            "Equalizer"
        } else {
            val currentLevels = equalizerState.bandLevels.map { it.toInt() }
            val matchedPreset = AudioEffectPreset.presets.firstOrNull { preset ->
                preset.levels.size == currentLevels.size && preset.levels.indices.all { i ->
                    preset.levels[i] == currentLevels[i]
                }
            }
            when {
                matchedPreset != null -> matchedPreset.name
                equalizerPreset.isNotBlank() && equalizerPreset != "Custom" -> {
                    val namedPreset = AudioEffectPreset.presets.firstOrNull { it.name.equals(equalizerPreset, ignoreCase = true) }
                    if (namedPreset != null && namedPreset.levels.size == currentLevels.size &&
                        namedPreset.levels.indices.all { namedPreset.levels[it] == currentLevels[it] }
                    ) {
                        namedPreset.name
                    } else {
                        "Custom"
                    }
                }
                else -> "Custom"
            }
        }
    }


    // Translated synchronized lyrics
    val autoTranslate by rememberPreference(AutoTranslateKey, false)
    val targetLanguage by rememberPreference(TranslateLanguageKey, "hi-Latn")
    val currentTranslationLang by LyricsTranslationHelper.currentLanguageCode.collectAsState()
    val translationVersion by LyricsTranslationHelper.translationVersion.collectAsState()

    LaunchedEffect(mediaMetadata?.id) {
        mediaMetadata?.id?.let { LyricsTranslationHelper.onSongChanged(it) }
    }

    val parsedLyrics = remember(currentLyrics?.lyrics) {
        val raw = LyricsTranslationHelper.parseLyricsToEntries(currentLyrics?.lyrics)
        raw.mapIndexed { index, entry ->
            if (entry.words != null || entry.text.isBlank() || entry.time < 0) {
                entry
            } else {
                val nextEntry = raw.getOrNull(index + 1)
                val lineDurationMs = if (nextEntry != null && nextEntry.time > entry.time) {
                    (nextEntry.time - entry.time).coerceIn(800L, 10000L)
                } else {
                    4000L
                }
                val lineStartSec = entry.time / 1000.0
                val tokens = entry.text.split(Regex("\\s+")).filter { it.isNotBlank() }
                if (tokens.isEmpty()) return@mapIndexed entry

                val totalChars = tokens.sumOf { it.length }.coerceAtLeast(1)
                val words = mutableListOf<WordTimestamp>()
                var currentOffsetMs = 0.0

                tokens.forEachIndexed { wordIdx, token ->
                    val weight = token.length.toDouble() / totalChars
                    val wordDurMs = lineDurationMs * weight
                    val wordStartSec = lineStartSec + (currentOffsetMs / 1000.0)
                    val wordEndSec = wordStartSec + (wordDurMs / 1000.0)
                    val wordText = if (wordIdx < tokens.lastIndex) "$token " else token
                    words.add(
                        WordTimestamp(
                            text = wordText,
                            startTime = wordStartSec,
                            endTime = wordEndSec,
                        )
                    )
                    currentOffsetMs += wordDurMs
                }
                entry.copy(words = words)
            }
        }
    }

    DisposableEffect(parsedLyrics) {
        LyricsTranslationHelper.registerLyrics(parsedLyrics)
        onDispose {
            LyricsTranslationHelper.unregisterLyrics(parsedLyrics)
        }
    }

    LaunchedEffect(parsedLyrics, mediaMetadata?.id, targetLanguage, currentTranslationLang, translationVersion, autoTranslate) {
        val songId = mediaMetadata?.id ?: return@LaunchedEffect
        if (parsedLyrics.isEmpty()) return@LaunchedEffect

        if (autoTranslate) {
            val activeLang = currentTranslationLang.ifBlank { targetLanguage }
            var loaded = LyricsTranslationHelper.loadTranslationsFromCache(
                lyrics = parsedLyrics,
                context = context,
                songId = songId,
                targetLanguageCode = activeLang
            )
            if (!loaded && activeLang != targetLanguage) {
                loaded = LyricsTranslationHelper.loadTranslationsFromCache(
                    lyrics = parsedLyrics,
                    context = context,
                    songId = songId,
                    targetLanguageCode = targetLanguage
                )
            }
            if (!loaded) {
                val dir = File(context.filesDir, "lyrics_translations")
                val safeSongId = songId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val cachedFile = dir.listFiles { _, name -> name.startsWith("${safeSongId}_") && name.endsWith(".json") }?.firstOrNull()
                if (cachedFile != null) {
                    val foundLang = cachedFile.name.removePrefix("${safeSongId}_").removeSuffix(".json")
                    if (foundLang.isNotBlank()) {
                        LyricsTranslationHelper.loadTranslationsFromCache(
                            lyrics = parsedLyrics,
                            context = context,
                            songId = songId,
                            targetLanguageCode = foundLang
                        )
                    }
                }
            }
        }
    }

    val activeEntry = remember(parsedLyrics, position) {
        if (parsedLyrics.isEmpty()) null
        else {
            parsedLyrics.findLast { it.time <= position } ?: parsedLyrics.firstOrNull()
        }
    }

    val fallbackFlow = remember { MutableStateFlow<String?>(null) }
    val activeTranslatedText by (activeEntry?.translatedTextFlow ?: fallbackFlow).collectAsState()

    val currentLyricText = remember(activeEntry, activeTranslatedText) {
        activeTranslatedText?.takeIf { it.isNotBlank() } ?: activeEntry?.text
    }

    val activeWords = remember(activeEntry, activeTranslatedText) {
        if (!activeTranslatedText.isNullOrBlank() && activeEntry != null && activeEntry.time >= 0) {
            val nextEntry = parsedLyrics.getOrNull(parsedLyrics.indexOf(activeEntry) + 1)
            val lineDurationMs = if (nextEntry != null && nextEntry.time > activeEntry.time) {
                (nextEntry.time - activeEntry.time).coerceIn(800L, 10000L)
            } else {
                4000L
            }
            val lineStartSec = activeEntry.time / 1000.0
            val tokens = activeTranslatedText!!.split(Regex("\\s+")).filter { it.isNotBlank() }
            val totalChars = tokens.sumOf { it.length }.coerceAtLeast(1)
            val words = mutableListOf<WordTimestamp>()
            var currentOffsetMs = 0.0
            tokens.forEachIndexed { wordIdx, token ->
                val weight = token.length.toDouble() / totalChars
                val wordDurMs = lineDurationMs * weight
                val wordStartSec = lineStartSec + (currentOffsetMs / 1000.0)
                val wordEndSec = wordStartSec + (wordDurMs / 1000.0)
                val wordText = if (wordIdx < tokens.lastIndex) "$token " else token
                words.add(
                    WordTimestamp(
                        text = wordText,
                        startTime = wordStartSec,
                        endTime = wordEndSec,
                    )
                )
                currentOffsetMs += wordDurMs
            }
            words
        } else {
            activeEntry?.words
        }
    }

    val lyricLineData = remember(activeEntry, currentLyricText, activeWords) {
        LyricsLineData(
            time = activeEntry?.time ?: -1L,
            text = currentLyricText,
            words = activeWords,
        )
    }

    // Play/Pause button shape controlled by user preference (Flower, Cookie, etc.)
    val playPauseShapeState = rememberPreference(
        key = PlayPauseButtonShapeKey,
        defaultValue = DefaultPlayPauseButtonShape
    )
    val playPauseShape = remember(playPauseShapeState.value) {
        getPlayPauseShape(playPauseShapeState.value)
    }
    val currentPlayPauseShape = remember(isPlaying, playPauseShape) {
        if (isPlaying) playPauseShape else MaterialShapes.Square
    }
    val infiniteTransition = rememberInfiniteTransition(label = "material_play_pause_rotation")
    val playPauseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "material_rotation"
    )

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }

    if (showDeviceSheet) {
        DeviceSelectionBottomSheet(
            onDismiss = { showDeviceSheet = false },
            availableDevices = displayDevices,
            activeDevice = activeDevice,
            preferredDevice = preferredAudioDevice,
            onSelectDevice = { device ->
                playerConnection.service.setPreferredOutputDevice(device)
            },
            textBackgroundColor = onBackgroundColor,
            hasBluetoothPermission = bluetoothPermissionGranted,
            onRequestBluetoothPermission = {
                bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        )
    }

    if (showEqualizerSheet) {
        InAppEqualizerSheet(
            onDismiss = { showEqualizerSheet = false }
        )
    }

    if (showAudioPipelineDialog) {
        AudioPipelineDialog(
            currentFormat = currentFormat,
            mediaMetadata = mediaMetadata,
            onDismiss = { showAudioPipelineDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .bottomSheetDraggable(state)
    ) {
        // 1. DYNAMIC BACKGROUND
        MaterialPlayerBackdrop(
            thumbnailUrl = mediaMetadata?.thumbnailUrl,
            isDark = isDark,
            modifier = Modifier.fillMaxSize()
        )

        // 2. PLAYER CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom + WindowInsetsSides.Top
                    )
                )
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Subtle drag handle at top
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(onBackgroundColor.copy(alpha = 0.22f))
                    .clickable(onClick = onCollapse)
            )

            // Album Artwork Card
            Card(
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f)
                    .padding(vertical = 4.dp),
            ) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl?.highQualityThumbnail(),
                    contentDescription = mediaMetadata?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Song Info & Actions Row (Heart, Share, More)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaMetadata?.title ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp,
                            ),
                            color = onBackgroundColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = mediaMetadata?.artists?.joinToString(", ") { it.name } ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = onBackgroundColor.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // 3 Circular Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Button
                        Surface(
                            onClick = onLikeClick,
                            shape = CircleShape,
                            color = circularButtonBg,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) Color(0xFFFF3B30) else buttonIconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Share Button
                        Surface(
                            onClick = onShareClick,
                            shape = CircleShape,
                            color = circularButtonBg,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Share",
                                    tint = buttonIconColor,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // More Button
                        Surface(
                            onClick = onMenuClick,
                            shape = CircleShape,
                            color = circularButtonBg,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More",
                                    tint = buttonIconColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Tappable Synchronized Lyrics Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenFullscreenLyrics)
                        .padding(vertical = 4.dp)
                ) {
                    AnimatedContent(
                        targetState = lyricLineData,
                        transitionSpec = {
                            (slideInVertically(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)) { (it * 0.75f).toInt() } + fadeIn(tween(350)))
                                .togetherWith(slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { -(it * 0.75f).toInt() } + fadeOut(tween(200)))
                        },
                        contentAlignment = Alignment.CenterStart,
                        label = "lyrics_line_transition",
                        modifier = Modifier.weight(1f, fill = false)
                    ) { state ->
                        if (state.text.isNullOrBlank()) {
                            Text(
                                text = "Tap to view lyrics",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = onBackgroundColor.copy(alpha = 0.55f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else {
                            val words = state.words
                            if (!words.isNullOrEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                ) {
                                    words.forEach { word ->
                                        AnimatedWordPreview(
                                            word = word,
                                            currentPositionMs = position,
                                            activeColor = onBackgroundColor,
                                            dimColor = onBackgroundColor.copy(alpha = 0.40f),
                                            fontSize = 14.sp,
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = state.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    ),
                                    color = onBackgroundColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Open lyrics",
                        tint = onBackgroundColor.copy(alpha = 0.55f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Progress Slider & Timers
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Slider(
                    value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1L).toFloat()),
                    valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                    onValueChange = { onSeek(it.toLong()) },
                    onValueChangeFinished = onSeekFinished,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryControlColor,
                        activeTrackColor = primaryControlColor,
                        inactiveTrackColor = inactiveTrackColor,
                    ),
                    track = { sliderState ->
                        val fraction = ((sliderState.value - sliderState.valueRange.start) /
                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                            .coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(inactiveTrackColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(primaryControlColor)
                            )
                        }
                    },
                    thumb = {
                        Spacer(modifier = Modifier.size(0.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = makeTimeString(position),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = onBackgroundColor.copy(alpha = 0.85f)
                    )
                    AudioQualityTag(
                        currentFormat = currentFormat,
                        mediaMetadata = mediaMetadata,
                        tint = onBackgroundColor,
                        onClick = { showAudioPipelineDialog = true }
                    )
                    Text(
                        text = if (duration > 0L) makeTimeString(duration) else "0:00",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = onBackgroundColor.copy(alpha = 0.85f)
                    )
                }
            }

            // Playback Controls (Previous, Play/Pause, Next)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Button
                Surface(
                    onClick = onPrevious,
                    enabled = canSkipPrevious,
                    shape = CircleShape,
                    color = playControlsContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = buttonIconColor.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(Modifier.width(28.dp))

                // Play/Pause Button with Expressive Shape (Flower / Cookie)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .rotate(if (isPlaying) playPauseRotation else 0f)
                        .clip(currentPlayPauseShape.toShape())
                        .background(playPauseButtonBg)
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = playPauseIconColor,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = playPauseIconColor,
                            modifier = Modifier
                                .size(36.dp)
                                .rotate(if (isPlaying) -playPauseRotation else 0f)
                        )
                    }
                }

                Spacer(Modifier.width(28.dp))

                // Next Button
                Surface(
                    onClick = onNext,
                    enabled = canSkipNext,
                    shape = CircleShape,
                    color = playControlsContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = buttonIconColor.copy(alpha = if (canSkipNext) 1f else 0.4f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            // Volume Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.volume_off),
                    contentDescription = "Volume Down",
                    tint = buttonIconColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(19.dp)
                )

                Slider(
                    value = playerVolume.coerceIn(0f, 1f),
                    valueRange = 0f..1f,
                    onValueChange = onVolumeChange,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryControlColor,
                        activeTrackColor = primaryControlColor,
                        inactiveTrackColor = inactiveTrackColor,
                    ),
                    track = { sliderState ->
                        val fraction = ((sliderState.value - sliderState.valueRange.start) /
                            (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                            .coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(inactiveTrackColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(primaryControlColor)
                            )
                        }
                    },
                    thumb = {
                        Spacer(modifier = Modifier.size(0.dp))
                    },
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(R.drawable.volume_up),
                    contentDescription = "Volume Up",
                    tint = buttonIconColor.copy(alpha = 0.65f),
                    modifier = Modifier.size(21.dp)
                )
            }

            // Bottom 3 Pill Action Cards ("This phone >", "Custom >", "Queue >")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Device Selector ("This phone >")
                MaterialPillButton(
                    icon = {
                        if (isBluetooth) {
                            Icon(
                                painter = painterResource(R.drawable.ic_bluetooth),
                                contentDescription = null,
                                tint = buttonIconColor,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = buttonIconColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    label = activeDevice?.outputName() ?: "This phone",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !bluetoothPermissionGranted) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            showDeviceSheet = true
                        }
                    },
                    containerColor = surfaceContainer,
                    contentColor = onBackgroundColor,
                    modifier = Modifier.weight(1f)
                )

                // 2. Equalizer ("Custom >")
                MaterialPillButton(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.graphic_eq),
                            contentDescription = null,
                            tint = buttonIconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = equalizerButtonLabel,
                    onClick = { showEqualizerSheet = true },
                    containerColor = surfaceContainer,
                    contentColor = onBackgroundColor,
                    modifier = Modifier.weight(1f)
                )

                // 3. Queue ("Queue >")
                MaterialPillButton(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                            tint = buttonIconColor,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = "Queue",
                    onClick = onQueueClick,
                    containerColor = surfaceContainer,
                    contentColor = onBackgroundColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Backdrop for Material Player:
 * - Light Mode: Artwork blurred on top ~52%, smoothly fading into light surface below.
 * - Dark Mode: Full-screen artwork blur with dark gradient scrim.
 */
@Composable
private fun MaterialPlayerBackdrop(
    thumbnailUrl: String?,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val lightSurface = MaterialTheme.colorScheme.surface
    val baseBg = if (isDark) Color(0xFF100F12) else lightSurface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            if (isDark) {
                // Full screen dark blur
                BlurredBackground(
                    model = thumbnailUrl.highQualityThumbnail(),
                    modifier = Modifier.fillMaxSize(),
                    blurRadius = 90.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Black.copy(alpha = 0.35f),
                                    0.45f to Color.Black.copy(alpha = 0.65f),
                                    1f to Color.Black.copy(alpha = 0.88f),
                                )
                            )
                        )
                )
            } else {
                // Light mode: only top side blurred thumbnail, fading into light surface below
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                ) {
                    BlurredBackground(
                        model = thumbnailUrl.highQualityThumbnail(),
                        modifier = Modifier.fillMaxSize(),
                        blurRadius = 75.dp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to Color.White.copy(alpha = 0.15f),
                                        0.35f to Color.White.copy(alpha = 0.35f),
                                        0.70f to lightSurface.copy(alpha = 0.85f),
                                        1f to lightSurface,
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun MaterialPillButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(3.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.50f),
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

internal data class LyricsLineData(
    val time: Long,
    val text: String?,
    val words: List<WordTimestamp>?,
)

@Composable
internal fun AnimatedWordPreview(
    word: WordTimestamp,
    currentPositionMs: Long,
    activeColor: Color,
    dimColor: Color,
    fontSize: TextUnit = 14.sp,
) {
    val wordStartMs = (word.startTime * 1000).toLong()
    val wordEndMs = (word.endTime * 1000).toLong()
    val wordDuration = (wordEndMs - wordStartMs).coerceAtLeast(1L)
    val isWordComplete = currentPositionMs >= wordEndMs
    val isWordActive = currentPositionMs in wordStartMs until wordEndMs

    val progress = when {
        isWordComplete -> 1f
        currentPositionMs <= wordStartMs -> 0f
        else -> ((currentPositionMs - wordStartMs).toFloat() / wordDuration).coerceIn(0f, 1f)
    }

    val sinProgress = kotlin.math.sin(progress * kotlin.math.PI).toFloat()
    val wordScale = 1f + (0.015f * sinProgress)
    val targetFloat = if (isWordActive) -3.5f * sinProgress else 0f
    val floatOffset by animateFloatAsState(
        targetValue = targetFloat,
        animationSpec = tween(
            durationMillis = if (isWordActive) 50 else 350,
            easing = FastOutSlowInEasing
        ),
        label = "word_float"
    )
    val glowProgress = (progress * 2f).coerceAtMost(1f)
    val glowAlpha = if (isWordActive) glowProgress * 0.45f else 0f
    val glowRadius = if (isWordActive) glowProgress * 10f else 0f

    Box(
        modifier = Modifier.graphicsLayer {
            translationY = floatOffset * density
            scaleX = wordScale
            scaleY = wordScale
        }
    ) {
        // Dimmed base text
        Text(
            text = word.text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
            ),
            color = dimColor,
            maxLines = 1,
            softWrap = false,
        )
        // Liquid sweep overlay text: pops up, adds soft glow, and sweeps to pure active color
        if (isWordComplete || isWordActive) {
            Text(
                text = word.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    shadow = if (glowAlpha > 0f) {
                        Shadow(
                            color = activeColor.copy(alpha = glowAlpha),
                            offset = Offset.Zero,
                            blurRadius = glowRadius.coerceAtLeast(1f)
                        )
                    } else null,
                ),
                color = activeColor,
                maxLines = 1,
                softWrap = false,
                modifier = if (isWordActive && !isWordComplete) {
                    Modifier
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val edgeWidth = 6.dp.toPx()
                            val center = (size.width + edgeWidth * 2) * progress - edgeWidth
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = center - edgeWidth,
                                    endX = center + edgeWidth,
                                ),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                } else Modifier
            )
        }
    }
}
