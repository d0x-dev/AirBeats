package com.darkxvenom.airbeats.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.*
import com.darkxvenom.airbeats.playback.AppForegroundTracker
import com.darkxvenom.airbeats.playback.DynamicIslandService
import com.darkxvenom.airbeats.ui.component.SwitchPreference
import com.darkxvenom.airbeats.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicIslandSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val (enableDynamicIsland, onEnableDynamicIslandChange) = rememberPreference(
        DynamicIslandKey,
        defaultValue = false
    )

    var selectedOrientationTab by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(0) } // 0: Portrait, 1: Landscape

    // Portrait values
    val (islandOffsetX, onIslandOffsetXChange) = rememberPreference(DynamicIslandOffsetXKey, defaultValue = 0)
    val (islandOffsetY, onIslandOffsetYChange) = rememberPreference(DynamicIslandOffsetYKey, defaultValue = 8)
    val (islandWidth, onIslandWidthChange) = rememberPreference(DynamicIslandWidthKey, defaultValue = 160)
    val (islandHeight, onIslandHeightChange) = rememberPreference(DynamicIslandHeightKey, defaultValue = 36)

    // Landscape values
    val (islandLandscapeOffsetX, onIslandLandscapeOffsetXChange) = rememberPreference(DynamicIslandLandscapeOffsetXKey, defaultValue = 0)
    val (islandLandscapeOffsetY, onIslandLandscapeOffsetYChange) = rememberPreference(DynamicIslandLandscapeOffsetYKey, defaultValue = 8)
    val (islandLandscapeWidth, onIslandLandscapeWidthChange) = rememberPreference(DynamicIslandLandscapeWidthKey, defaultValue = 160)
    val (islandLandscapeHeight, onIslandLandscapeHeightChange) = rememberPreference(DynamicIslandLandscapeHeightKey, defaultValue = 36)

    // Colors & Liquid Glass
    val (islandBgColor, onIslandBgColorChange) = rememberPreference(
        DynamicIslandBgColorKey,
        defaultValue = android.graphics.Color.BLACK
    )
    val (islandAccentColor, onIslandAccentColorChange) = rememberPreference(
        DynamicIslandAccentColorKey,
        defaultValue = android.graphics.Color.rgb(229, 19, 69)
    )
    val (islandTextColor, onIslandTextColorChange) = rememberPreference(
        DynamicIslandTextColorKey,
        defaultValue = android.graphics.Color.WHITE
    )
    val (enableLiquidGlass, onEnableLiquidGlassChange) = rememberPreference(
        DynamicIslandLiquidGlassKey,
        defaultValue = false
    )

    DisposableEffect(Unit) {
        AppForegroundTracker.isAdjustingIsland = true
        onDispose {
            AppForegroundTracker.isAdjustingIsland = false
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val activeX = if (selectedOrientationTab == 0) islandOffsetX else islandLandscapeOffsetX
    val onActiveXChange: (Int) -> Unit = if (selectedOrientationTab == 0) onIslandOffsetXChange else onIslandLandscapeOffsetXChange

    val activeY = if (selectedOrientationTab == 0) islandOffsetY else islandLandscapeOffsetY
    val onActiveYChange: (Int) -> Unit = if (selectedOrientationTab == 0) onIslandOffsetYChange else onIslandLandscapeOffsetYChange

    val activeW = if (selectedOrientationTab == 0) islandWidth else islandLandscapeWidth
    val onActiveWChange: (Int) -> Unit = if (selectedOrientationTab == 0) onIslandWidthChange else onIslandLandscapeWidthChange

    val activeH = if (selectedOrientationTab == 0) islandHeight else islandLandscapeHeight
    val onActiveHChange: (Int) -> Unit = if (selectedOrientationTab == 0) onIslandHeightChange else onIslandLandscapeHeightChange

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dynamic Island",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enable Switch Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SwitchPreference(
                        title = { Text("Enable Dynamic Island", fontWeight = FontWeight.Bold) },
                        description = "Displays live rotating artwork, squiggly seekbar, and player controls overlay",
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
                                val serviceIntent = Intent(context, DynamicIslandService::class.java)
                                try {
                                    if (newValue) {
                                        context.startService(serviceIntent)
                                    } else {
                                        context.stopService(serviceIntent)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = enableDynamicIsland) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Position & Size Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Position & Size Adjustment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Orientation Segmented Switcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf("Portrait", "Landscape").forEachIndexed { index, label ->
                                    val isSelected = selectedOrientationTab == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                            )
                                            .clickable {
                                                selectedOrientationTab = index
                                                val activity = context as? Activity
                                                if (index == 1) {
                                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                                } else {
                                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                                }
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Horizontal Position (X) Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Horizontal Position (X)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(if (activeX == 0) "Center (0 dp)" else if (activeX > 0) "+$activeX dp" else "$activeX dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = activeX.toFloat(),
                                valueRange = -500f..500f,
                                onValueChange = { onActiveXChange(it.toInt()) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Vertical Position (Y) Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vertical Position (Y)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("$activeY dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = activeY.toFloat(),
                                valueRange = -150f..400f,
                                onValueChange = { onActiveYChange(it.toInt()) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // Width Slider
                            val widthLabel = if (activeW <= 46) "Mini Dot ($activeW dp)" else if (activeW < 120) "Compact ($activeW dp)" else "Pill ($activeW dp)"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Island Width", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(widthLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = activeW.toFloat(),
                                valueRange = 32f..300f,
                                onValueChange = { onActiveWChange(it.toInt()) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Height Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Island Height", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("$activeH dp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(
                                value = activeH.toFloat(),
                                valueRange = 20f..54f,
                                onValueChange = { onActiveHChange(it.toInt()) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Reset Button
                            TextButton(
                                onClick = {
                                    onActiveXChange(0)
                                    onActiveYChange(8)
                                    onActiveWChange(160)
                                    onActiveHChange(36)
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Reset to Defaults")
                            }
                        }
                    }

                    // Styling & Liquid Glass Card
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Styling & Custom Colors",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            SwitchPreference(
                                title = { Text("Liquid Glass Effect", fontWeight = FontWeight.SemiBold) },
                                description = "Translucent frosted acrylic glass with specular highlights and glowing border",
                                icon = { Icon(painterResource(R.drawable.palette), null) },
                                checked = enableLiquidGlass,
                                onCheckedChange = onEnableLiquidGlassChange
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // Background Color
                            val bgPresets = listOf(
                                "Pure Black" to android.graphics.Color.BLACK,
                                "Deep Night" to android.graphics.Color.parseColor("#121218"),
                                "Dark Glass" to android.graphics.Color.parseColor("#1C1C28"),
                                "Navy Blue" to android.graphics.Color.parseColor("#0A192F"),
                                "AMOLED Dark" to android.graphics.Color.parseColor("#212121"),
                                "Crimson Dark" to android.graphics.Color.parseColor("#2A0808"),
                            )
                            Text("Background Color", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                bgPresets.forEach { (_, colorInt) ->
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                            .border(
                                                width = if (islandBgColor == colorInt) 2.5.dp else 1.dp,
                                                color = if (islandBgColor == colorInt) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { onIslandBgColorChange(colorInt) }
                                    )
                                }
                            }

                            // Accent Color
                            val accentPresets = listOf(
                                "Apple Red" to android.graphics.Color.parseColor("#E51345"),
                                "Spotify Green" to android.graphics.Color.parseColor("#1ED760"),
                                "Cyan Blue" to android.graphics.Color.parseColor("#00E5FF"),
                                "Neon Purple" to android.graphics.Color.parseColor("#BB86FC"),
                                "Electric Amber" to android.graphics.Color.parseColor("#FF6D00"),
                                "Bubble Pink" to android.graphics.Color.parseColor("#FF4081"),
                            )
                            Text("Accent & Progress Color", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                accentPresets.forEach { (_, colorInt) ->
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                            .border(
                                                width = if (islandAccentColor == colorInt) 2.5.dp else 1.dp,
                                                color = if (islandAccentColor == colorInt) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { onIslandAccentColorChange(colorInt) }
                                    )
                                }
                            }

                            // Text Color
                            val textPresets = listOf(
                                "Pure White" to android.graphics.Color.WHITE,
                                "Soft Silver" to android.graphics.Color.parseColor("#E0E0E0"),
                                "Warm Cream" to android.graphics.Color.parseColor("#FFFDD0"),
                                "Neon Yellow" to android.graphics.Color.parseColor("#FFFF00"),
                            )
                            Text("Text & Icon Color", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                textPresets.forEach { (_, colorInt) ->
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                            .border(
                                                width = if (islandTextColor == colorInt) 2.5.dp else 1.dp,
                                                color = if (islandTextColor == colorInt) MaterialTheme.colorScheme.primary else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { onIslandTextColorChange(colorInt) }
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
