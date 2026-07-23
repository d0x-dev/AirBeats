package com.darkxvenom.airbeats.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.playback.AppForegroundTracker
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.*
import com.darkxvenom.airbeats.constants.HomeScreenStyle
import com.darkxvenom.airbeats.constants.HomeScreenStyleKey
import com.darkxvenom.airbeats.constants.NavBarStyle
import com.darkxvenom.airbeats.constants.NavBarStyleKey
import com.darkxvenom.airbeats.ui.component.*
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.utils.rememberPreference
import me.saket.squiggles.SquigglySlider
import timber.log.Timber

// ==================== MAIN APPEARANCE SETTINGS SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (playerTextAlignment, onPlayerTextAlignmentChange) =
        rememberEnumPreference(
            PlayerTextAlignmentKey,
            defaultValue = PlayerTextAlignment.CENTER,
        )

    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )
    val (playerScreenStyle, onPlayerScreenStyleChange) =
        rememberEnumPreference<PlayerScreenStyle>(
            PlayerScreenStyleKey,
            defaultValue = PlayerScreenStyle.IOS_STYLED,
        )
    val (homeScreenStyle, onHomeScreenStyleChange) =
        rememberEnumPreference(
            HomeScreenStyleKey,
            defaultValue = HomeScreenStyle.CLASSIC,
        )
    val (navBarStyle, onNavBarStyleChange) =
        rememberEnumPreference(
            NavBarStyleKey,
            defaultValue = NavBarStyle.APPLE,
        )
    val isPlayful = homeScreenStyle == HomeScreenStyle.PLAYFUL

    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)
    val (colourfullPlayerColor, onColourfullPlayerColorChange) = rememberPreference(
        ColourfullPlayerColorKey,
        defaultValue = 0xFF4CAF50.toInt()
    )
    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (enableNewLyricsScreen, onEnableNewLyricsScreenChange) = rememberPreference(EnableNewLyricsScreenKey, defaultValue = true)
    val (enableNewQueueScreen, onEnableNewQueueScreenChange) = rememberPreference(EnableNewQueueScreenKey, defaultValue = true)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.SQUIGGLY
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.BIG
    )
    val (animateLyrics, onAnimateLyricsChange) = rememberPreference(
        AnimateLyricsKey,
        defaultValue = true
    )


    val (rotateBackground, onRotateBackgroundChange) = rememberPreference(
        key = RotateBackgroundKey,
        defaultValue = false
    )

    // Estados de formas
    val smallButtonsShapeState = rememberPreference(
        key = SmallButtonsShapeKey,
        defaultValue = DefaultSmallButtonsShape
    )

    val playPauseShapeState = rememberPreference(
        key = PlayPauseButtonShapeKey,
        defaultValue = DefaultPlayPauseButtonShape
    )

    val miniPlayerThumbnailShapeState = rememberPreference(
        key = MiniPlayerThumbnailShapeKey,
        defaultValue = DefaultMiniPlayerThumbnailShape
    )

    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = false)
    val (enableLiquidGlass, onEnableLiquidGlassChange) = rememberPreference(
        LiquidGlassKey,
        defaultValue = false
    )
    val (enableDynamicIsland, onEnableDynamicIslandChange) = rememberPreference(
        DynamicIslandKey,
        defaultValue = false
    )
    val (appFontKey, onAppFontKeyChange) = rememberPreference(
        AppFontKey,
        defaultValue = AppFont.LINOTTE.key
    )
    val selectedFont = remember(appFontKey) { AppFont.fromKey(appFontKey) }
    var showFontDialog by remember { mutableStateOf(false) }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme, enableLiquidGlass, isPlayful) {
            if (isPlayful) {
                false
            } else if (enableLiquidGlass) {
                true
            } else {
                if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
            }
        }

    // Automatically disable pureBlack when switching to light mode
    LaunchedEffect(useDarkTheme) {
        if (!useDarkTheme && pureBlack) {
            onPureBlackChange(false)
        }
    }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showIslandAdjustmentDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showColorPickerOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showColorPickerOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showColorPickerOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showColorPickerOptionDialog = false
            }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onColourfullPlayerColorChange(0)
                            showColorPickerOptionDialog = false
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta)
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(R.string.auto_from_song), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                val colors = listOf(
                    0xFF4CAF50.toInt(), // Green
                    0xFFF44336.toInt(), // Red
                    0xFF2196F3.toInt(), // Blue
                    0xFFFF9800.toInt(), // Orange
                    0xFF9C27B0.toInt(), // Purple
                    0xFF00BCD4.toInt(), // Cyan
                    0xFFE91E63.toInt(), // Pink
                    0xFFFFEB3B.toInt(), // Yellow
                    0xFF8BC34A.toInt(), // Light Green
                    0xFF3F51B5.toInt(), // Indigo
                    0xFF009688.toInt(), // Teal
                    0xFFFF5722.toInt(), // Deep Orange
                    0xFF795548.toInt(), // Brown
                    0xFF607D8B.toInt(), // Blue Grey
                    0xFF673AB7.toInt()  // Deep Purple
                )
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(5),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colors.size) { index ->
                        val colorInt = colors[index]
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color(colorInt))
                                .clickable {
                                    onColourfullPlayerColorChange(colorInt)
                                    showColorPickerOptionDialog = false
                                }
                        )
                    }
                }
            }
        }
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.default_),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    SquigglySlider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.squiggly),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SLIM)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showIslandAdjustmentDialog) {
        val context = LocalContext.current
        val (islandOffsetX, onIslandOffsetXChange) = rememberPreference(DynamicIslandOffsetXKey, defaultValue = 0)
        val (islandOffsetY, onIslandOffsetYChange) = rememberPreference(DynamicIslandOffsetYKey, defaultValue = 8)
        
        DisposableEffect(Unit) {
            AppForegroundTracker.isAdjustingIsland = true
            onDispose {
                AppForegroundTracker.isAdjustingIsland = false
            }
        }
        
        DefaultDialog(
            buttons = {
                TextButton(onClick = { 
                    onIslandOffsetXChange(0)
                    onIslandOffsetYChange(8)
                }) {
                    Text(stringResource(R.string.reset))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showIslandAdjustmentDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            },
            onDismiss = { showIslandAdjustmentDialog = false }
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.adjust_position), style = MaterialTheme.typography.titleLarge)
                
                Text(
                    "Use the arrows to adjust the actual Dynamic Island position on your screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { onIslandOffsetYChange(islandOffsetY - 4) }) {
                        Icon(painterResource(R.drawable.arrow_upward), "Up")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = { onIslandOffsetXChange(islandOffsetX - 4) }) {
                            Icon(painterResource(R.drawable.arrow_back), "Left")
                        }
                        IconButton(onClick = { onIslandOffsetXChange(islandOffsetX + 4) }) {
                            Icon(painterResource(R.drawable.arrow_forward), "Right")
                        }
                    }
                    IconButton(onClick = { onIslandOffsetYChange(islandOffsetY + 4) }) {
                        Icon(painterResource(R.drawable.arrow_downward), "Down")
                    }
                }
            }
        }
    }

    // Get player connection for album artwork
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🎵 BLUR BACKGROUND
        val artworkUrl = mediaMetadata?.thumbnailUrl

        artworkUrl?.let { imageUrl ->
            com.darkxvenom.airbeats.ui.component.BlurredBackground(
                model = imageUrl
            )

            val isDarkTheme =
                MaterialTheme.colorScheme.background.luminance() < 0.5f

            val overlayBrush = if (isDarkTheme) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )
        }

        // Main Scaffold with U-Shaped TopAppBar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // U-Shaped TopAppBar
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.appearance),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        Spacer(modifier = Modifier.width(48.dp))
                    },
                    actions = {
                        Spacer(modifier = Modifier.width(48.dp))
                    },
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                bottomStart = 30.dp,
                                bottomEnd = 30.dp
                            )
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(
                            width = 0.6.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(
                                bottomStart = 30.dp,
                                bottomEnd = 30.dp
                            )
                        ),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            // Content with proper scrolling
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
            ) {
                // Theme Category
                SettingsGeneralCategory(
                    title = stringResource(R.string.theme),
                    items = listOf(
                        {EnumListPreference(
                            title = { Text(stringResource(R.string.home_screen_style)) },
                            icon = { Icon(painterResource(R.drawable.home), null) },
                            selectedValue = homeScreenStyle,
                            onValueSelected = onHomeScreenStyleChange,
                            valueText = {
                                when (it) {
                                    HomeScreenStyle.CLASSIC -> "Classic"
                                    HomeScreenStyle.PLAYFUL -> "Playful"
                                    HomeScreenStyle.NEON -> "Neon"
                                    HomeScreenStyle.SPOTIFY -> "Spotify"
                                    HomeScreenStyle.APPLE -> "Apple"
                                }
                            },
                        )},
                        {EnumListPreference(
                            title = { Text(stringResource(R.string.navigation_bar_style)) },
                            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                            selectedValue = navBarStyle,
                            onValueSelected = onNavBarStyleChange,
                            valueText = {
                                when (it) {
                                    NavBarStyle.CLASSIC -> "Classic"
                                    NavBarStyle.LIQUID_GLASS -> "Liquid Glass"
                                    NavBarStyle.SPOTIFY -> "Spotify"
                                    NavBarStyle.APPLE -> "Apple"
                                    NavBarStyle.NEON -> "Neon"
                                    NavBarStyle.NEW_CLASSIC -> "New Classic"
                                }
                            },
                        )},
                        {SwitchPreference(
                            title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                            icon = { Icon(painterResource(R.drawable.palette), null) },
                            checked = dynamicTheme,
                            onCheckedChange = onDynamicThemeChange,
                        )},
                        {EnumListPreference(
                            title = { Text(stringResource(R.string.dark_theme)) },
                            icon = { Icon(painterResource(R.drawable.dark_mode), null) },
                            selectedValue = if (enableLiquidGlass) DarkMode.ON else if (isPlayful) DarkMode.OFF else darkMode,
                            onValueSelected = onDarkModeChange,
                            valueText = {
                                if (enableLiquidGlass) {
                                    stringResource(R.string.dark_theme_on)
                                } else if (isPlayful) {
                                    stringResource(R.string.dark_theme_off)
                                } else {
                                    when (it) {
                                        DarkMode.ON -> stringResource(R.string.dark_theme_on)
                                        DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                                        DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                                    }
                                }
                            },
                            isEnabled = !enableLiquidGlass && !isPlayful
                        )},
                        {
                            val context = LocalContext.current
                            SwitchPreference(
                                title = { Text(stringResource(R.string.enable_dynamic_island)) },
                                description = stringResource(R.string.enable_dynamic_island_desc),
                                icon = { Icon(painterResource(R.drawable.music_note), null) },
                                checked = enableDynamicIsland,
                                onCheckedChange = { newValue ->
                                    if (newValue && !Settings.canDrawOverlays(context)) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        onEnableDynamicIslandChange(newValue)
                                        val serviceIntent = Intent(context, com.darkxvenom.airbeats.playback.DynamicIslandService::class.java)
                                        if (newValue) {
                                            context.startService(serviceIntent)
                                        } else {
                                            context.stopService(serviceIntent)
                                        }
                                    }
                                }
                            )
                        },
                        *(if (enableDynamicIsland) arrayOf(
                            { PreferenceEntry(
                                title = { Text(stringResource(R.string.adjust_dynamic_island)) },
                                description = "Change the position of the dynamic island on screen",
                                icon = { Icon(painterResource(R.drawable.add), null) },
                                onClick = {
                                    showIslandAdjustmentDialog = true
                                }
                            ) }
                        ) else emptyArray()),
                        {SwitchPreference(
                            title = { Text(stringResource(R.string.enable_liquid_glass)) },
                            description = stringResource(R.string.enable_liquid_glass_desc),
                            icon = { Icon(painterResource(R.drawable.palette), null) },
                            checked = enableLiquidGlass && !isPlayful,
                            onCheckedChange = { newValue ->
                                onEnableLiquidGlassChange(newValue)
                                if (newValue) {
                                    onDarkModeChange(DarkMode.ON)
                                }
                            },
                            isEnabled = !isPlayful
                        )},
                        {AnimatedVisibility(useDarkTheme) {
                            SwitchPreference(
                                title = { Text(stringResource(R.string.pure_black)) },
                                icon = { Icon(painterResource(R.drawable.contrast), null) },
                                checked = pureBlack && useDarkTheme && !enableLiquidGlass,
                                onCheckedChange = { newValue ->
                                    if (useDarkTheme && !enableLiquidGlass) {
                                        onPureBlackChange(newValue)
                                    }
                                },
                                isEnabled = useDarkTheme && !enableLiquidGlass
                            )
                        }},
                        { PreferenceEntry(
                            title = { Text("Fonts") },
                            description = selectedFont.title,
                            icon = { Icon(painterResource(R.drawable.tune), null) },
                            onClick = { showFontDialog = true }
                        ) }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (showFontDialog) {
                    AlertDialog(
                        onDismissRequest = { showFontDialog = false },
                        title = { Text("Fonts", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AppFont.entries.forEach { font ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onAppFontKeyChange(font.key)
                                                showFontDialog = false
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = (font == selectedFont),
                                            onClick = {
                                                onAppFontKeyChange(font.key)
                                                showFontDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = font.title,
                                                fontFamily = font.getFontFamily(),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "The quick brown fox jumps over the lazy dog",
                                                fontFamily = font.getFontFamily(),
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFontDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }

                // Language preferences
                SettingsGeneralCategory(
                    title = stringResource(R.string.app_language),
                    items = listOf(
                        { LanguagePreference() }
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Determine the options available based on the Android version
                val availableBackgroundStyles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    enumValues<PlayerBackgroundStyle>().toList()
                } else {
                    enumValues<PlayerBackgroundStyle>().filter {
                        it != PlayerBackgroundStyle.BLUR
                    }
                }

                // Also ensure that the selected value is compatible.
                val safeSelectedValue = if (playerBackground == PlayerBackgroundStyle.BLUR &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                ) {
                    PlayerBackgroundStyle.DEFAULT
                } else {
                    playerBackground
                }

                // Player Category
                SettingsGeneralCategory(
                    title = stringResource(R.string.player),
                    items = listOf(
                        {EnumListPreference(
                            title = { Text(stringResource(R.string.player_screen_style)) },
                            icon = { Icon(painterResource(R.drawable.palette), null) },
                            selectedValue = playerScreenStyle,
                            onValueSelected = onPlayerScreenStyleChange,
                            valueText = {
                                when (it) {
                                    PlayerScreenStyle.PAPER -> stringResource(R.string.paper_player)
                                    PlayerScreenStyle.CLASSIC -> stringResource(R.string.classic_player)
                                    PlayerScreenStyle.MODERN -> stringResource(R.string.modern_player)
                                    PlayerScreenStyle.SPOTIFY -> stringResource(R.string.spotify_player)
                                    PlayerScreenStyle.LIQUID -> stringResource(R.string.liquid_player)
                                    PlayerScreenStyle.CLOUDGLOW -> "CloudGlow"
                                    PlayerScreenStyle.FROST -> "Frost"
                                    PlayerScreenStyle.FOLD -> "Fold"
                                    PlayerScreenStyle.GROOVE -> "Groove"
                                    PlayerScreenStyle.POPSY -> "Popsy"
                                    PlayerScreenStyle.MINIMAL -> "Minimal"
                                    PlayerScreenStyle.COLOURFULL -> "Colourfull"
                                    PlayerScreenStyle.APPLE -> "Apple"
                                    PlayerScreenStyle.GALAXY -> "Galaxy"
                                    PlayerScreenStyle.IOS_STYLED -> "IOS Styled"
                                }
                            },
                        )},

                        *(if (playerScreenStyle == PlayerScreenStyle.COLOURFULL || playerScreenStyle == PlayerScreenStyle.APPLE || playerScreenStyle == PlayerScreenStyle.GALAXY) arrayOf(
                            { PreferenceEntry(
                                title = { Text(stringResource(R.string.player_colour)) },
                                description = "Choose a custom background color",
                                icon = { Icon(painterResource(R.drawable.palette), null) },
                                onClick = {
                                    showColorPickerOptionDialog = true
                                }
                            ) }
                        ) else emptyArray()),

                        {EnumListPreference(
                            title = { Text(stringResource(R.string.player_background_style)) },
                            icon = { Icon(painterResource(R.drawable.gradient), null) },
                            selectedValue = safeSelectedValue,
                            onValueSelected = onPlayerBackgroundChange,
                            valueText = {
                                when (it) {
                                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                                    PlayerBackgroundStyle.FLUID -> stringResource(R.string.player_background_fluid)
                                }
                            },
                            values = availableBackgroundStyles
                        )},

                        {ThumbnailCornerRadiusSelectorButton(
                            onRadiusSelected = { selectedRadius ->
                                Timber.tag("Thumbnail").d("Selected radio: $selectedRadius")
                            }
                        )},

                        {
                            UnifiedShapeSelectorButton(
                                smallButtonsShape = smallButtonsShapeState.value,
                                playPauseShape = playPauseShapeState.value,
                                miniPlayerShape = miniPlayerThumbnailShapeState.value,
                                onSmallButtonsShapeSelected = { newShape ->
                                    smallButtonsShapeState.value = newShape
                                },
                                onPlayPauseShapeSelected = { newShape ->
                                    playPauseShapeState.value = newShape
                                },
                                onMiniPlayerShapeSelected = { newShape ->
                                    miniPlayerThumbnailShapeState.value = newShape
                                }
                            )
                        },

                        {EnumListPreference(
                            title = { Text(stringResource(R.string.player_buttons_style)) },
                            icon = { Icon(painterResource(R.drawable.palette), null) },
                            selectedValue = playerButtonsStyle,
                            onValueSelected = onPlayerButtonsStyleChange,
                            valueText = {
                                when (it) {
                                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.secondary_color_style)
                                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                                }
                            },
                        )},

                        {PreferenceEntry(
                            title = { Text(stringResource(R.string.player_slider_style)) },
                            description =
                                when (sliderStyle) {
                                    SliderStyle.DEFAULT -> stringResource(R.string.default_)
                                    SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                                    SliderStyle.SLIM -> stringResource(R.string.slim)
                                },
                            icon = { Icon(painterResource(R.drawable.sliders), null) },
                            onClick = {
                                showSliderOptionDialog = true
                            },
                        )},

                        *(if (playerScreenStyle == PlayerScreenStyle.GALAXY) arrayOf(
                            {
                                val (showGalaxySlider, onShowGalaxySliderChange) = rememberPreference(
                                    ShowGalaxySliderKey,
                                    defaultValue = true
                                )
                                SwitchPreference(
                                    title = { Text(stringResource(R.string.show_galaxy_slider)) },
                                    description = stringResource(R.string.show_galaxy_slider_desc),
                                    icon = { Icon(painterResource(R.drawable.sliders), null) },
                                    checked = showGalaxySlider,
                                    onCheckedChange = onShowGalaxySliderChange
                                )
                            }
                        ) else emptyArray()),

                        {SwitchPreference(
                            title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                            icon = { Icon(painterResource(R.drawable.swipe), null) },
                            checked = swipeThumbnail,
                            onCheckedChange = onSwipeThumbnailChange,
                        )},

                        {SwitchPreference(
                            title = { Text(stringResource(R.string.Rotatelyricsbackground)) },
                            description = null,
                            icon = { Icon(painterResource(R.drawable.album), null) },
                            checked = rotateBackground,
                            onCheckedChange = onRotateBackgroundChange
                        )},

                        {EnumListPreference(
                            title = { Text(stringResource(R.string.player_text_alignment)) },
                            icon = {
                                Icon(
                                    painter =
                                        painterResource(
                                            when (playerTextAlignment) {
                                                PlayerTextAlignment.CENTER -> R.drawable.format_align_center
                                                PlayerTextAlignment.SIDED -> R.drawable.format_align_left
                                            },
                                        ),
                                    contentDescription = null,
                                )
                            },
                            selectedValue = playerTextAlignment,
                            onValueSelected = onPlayerTextAlignmentChange,
                            valueText = {
                                when (it) {
                                    PlayerTextAlignment.SIDED -> stringResource(R.string.sided)
                                    PlayerTextAlignment.CENTER -> stringResource(R.string.center)
                                }
                            },
                        )},

                        {EnumListPreference(
                            title = { Text(stringResource(R.string.lyrics_text_position)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            selectedValue = lyricsPosition,
                            onValueSelected = onLyricsPositionChange,
                            valueText = {
                                when (it) {
                                    LyricsPosition.LEFT -> stringResource(R.string.left)
                                    LyricsPosition.CENTER -> stringResource(R.string.center)
                                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                                }
                            },
                        )},

                        {SwitchPreference(
                            title = { Text(stringResource(R.string.lyrics_click_change)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            checked = lyricsClick,
                            onCheckedChange = onLyricsClickChange,
                        )},

                        {SwitchPreference(
                            title = { Text(stringResource(R.string.animate_lyrics)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            description = stringResource(R.string.animate_lyrics_desc),
                            checked = animateLyrics,
                            onCheckedChange = onAnimateLyricsChange
                        )},

                        {SwitchPreference(
                            title = { Text(stringResource(R.string.enable_new_lyrics_screen)) },
                            icon = { Icon(painterResource(R.drawable.lyrics), null) },
                            description = stringResource(R.string.enable_new_lyrics_screen_desc),
                            checked = enableNewLyricsScreen,
                            onCheckedChange = onEnableNewLyricsScreenChange
                        )},

                        {SwitchPreference(
                            title = { Text(stringResource(R.string.new_queue_screen)) },
                            icon = { Icon(painterResource(R.drawable.music_note), null) },
                            description = "Use AirBeats's queue screen",
                            checked = enableNewQueueScreen,
                            onCheckedChange = onEnableNewQueueScreenChange
                        )}
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Misc Category
                SettingsGeneralCategory(
                    title = stringResource(R.string.misc),
                    items = listOf(
                        {EnumListPreference(
                            title = { Text(stringResource(R.string.default_open_tab)) },
                            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                            selectedValue = defaultOpenTab,
                            onValueSelected = onDefaultOpenTabChange,
                            valueText = {
                                when (it) {
                                    NavigationTab.HOME -> stringResource(R.string.home)
                                    NavigationTab.EXPLORE -> stringResource(R.string.explore)
                                    NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                                }
                            },
                        )},

                        {ListPreference(
                            title = { Text(stringResource(R.string.default_lib_chips)) },
                            icon = { Icon(painterResource(R.drawable.tab), null) },
                            selectedValue = defaultChip,
                            values = listOf(
                                LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS,
                                LibraryFilter.ALBUMS, LibraryFilter.ARTISTS
                            ),
                            valueText = {
                                when (it) {
                                    LibraryFilter.SONGS -> stringResource(R.string.songs)
                                    LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                                    LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                                    LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                                    LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                                    LibraryFilter.LOCAL -> stringResource(R.string.filter_local)
                                }
                            },
                            onValueSelected = onDefaultChipChange,
                        )},

                        {SwitchPreference(
                            title = { Text(stringResource(R.string.slim_navbar)) },
                            icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                            checked = slimNav,
                            onCheckedChange = onSlimNavChange
                        )},

                        {EnumListPreference(
                            title = { Text(stringResource(R.string.grid_cell_size)) },
                            icon = { Icon(painterResource(R.drawable.grid_view), null) },
                            selectedValue = gridItemSize,
                            onValueSelected = onGridItemSizeChange,
                            valueText = {
                                when (it) {
                                    GridItemSize.SMALL -> stringResource(R.string.small)
                                    GridItemSize.BIG -> stringResource(R.string.big)
                                }
                            },
                        )},
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Avatar section completely removed

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    EXPLORE,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

