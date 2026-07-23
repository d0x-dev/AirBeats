package com.darkxvenom.airbeats.ui.screens.settings

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.BuildConfig
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AccountNameKey
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.ChangelogScreen
import com.darkxvenom.airbeats.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// ==================== DIVIDER COMPONENT ====================

@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.1f),
    thickness: Dp = 0.5.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

// ==================== SETTINGS CATEGORY COMPONENTS ====================

data class SettingsCategoryItem(
    val icon: androidx.compose.ui.graphics.painter.Painter,
    val title: @Composable () -> Unit,
    val trailingContent: @Composable (() -> Unit)? = null,
    val onClick: () -> Unit
)

@Composable
fun SettingsCategory(
    title: String,
    items: List<SettingsCategoryItem>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items.forEachIndexed { index, item ->
                    SettingsCategoryItemContent(
                        item = item,
                        isLast = index == items.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCategoryItemContent(
    item: SettingsCategoryItem,
    isLast: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isPressed) 0.98f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isPressed) {
                    Modifier.background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    )
                } else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                item.onClick()
                isPressed = false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Title
            Box(modifier = Modifier.weight(1f)) {
                item.title()
            }

            // Trailing content
            if (item.trailingContent != null) {
                item.trailingContent()
            } else {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (!isLast) {
        SettingsDivider(
            modifier = Modifier.padding(start = 72.dp, end = 16.dp)
        )
    }
}

// ==================== GLASS CARD COMPONENT ====================

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

// ==================== WATER DROP BUTTON COMPONENTS ====================

@Composable
fun WaterDropButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: String? = null,
    colors: List<Color> = listOf(
        Color(0xFF6C5CE7),
        Color(0xFFA463F5),
        Color(0xFFC45AF0)
    )
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .scale(if (isPressed) 0.95f else 1f)
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = RoundedCornerShape(30.dp),
                clip = false
            )
            .background(
                brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset(0f, 0f),
                    end = Offset(100f, 100f)
                ),
                shape = RoundedCornerShape(30.dp)
            )
            .drawBehind {
                // Water drop effect - bottom highlight
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(size.width * 0.2f, size.height * 0.7f),
                    size = Size(size.width * 0.6f, size.height * 0.3f),
                    blendMode = BlendMode.Screen
                )
                // Top highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = size.minDimension * 0.15f,
                    center = Offset(size.width * 0.3f, size.height * 0.3f),
                    blendMode = BlendMode.Screen
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
                isPressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                icon()
                if (text != null) Spacer(modifier = Modifier.width(8.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun WaterDropIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .scale(if (isPressed) 0.9f else 1f)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                clip = false
            )
            .background(
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
                CircleShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
                isPressed = false
            },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

// ==================== ORIGINAL FUNCTIONS ====================

@SuppressLint("ObsoleteSdkInt")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun getAppVersion(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        }
        packageInfo.versionName ?: "Unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "Unknown"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun VersionCard(uriHandler: UriHandler) {
    val context = LocalContext.current
    val appVersion = remember { getAppVersion(context) }

    Spacer(Modifier.height(16.dp))

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.app_info),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // Version item
                SettingsCategoryItemContent(
                    item = SettingsCategoryItem(
                        icon = painterResource(R.drawable.info),
                        title = {
                            Column {
                                Text(
                                    text = stringResource(R.string.Version),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = appVersion,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = { uriHandler.openUri("https://github.com/d0x-dev/AirBeats/releases/latest") }
                    ),
                    isLast = false
                )

                SettingsDivider(modifier = Modifier.padding(start = 72.dp, end = 16.dp))

                // Website item
                SettingsCategoryItemContent(
                    item = SettingsCategoryItem(
                        icon = painterResource(R.drawable.resource_public),
                        title = {
                            Text(
                                text = "Official Website",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingContent = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_forward),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        onClick = { uriHandler.openUri("https://airbeats.app") }
                    ),
                    isLast = true
                )
            }
        }
    }
}

@Composable
fun UpdateCard(latestVersion: String = "") {
    val context = LocalContext.current
    var showUpdateCard by remember { mutableStateOf(false) }
    var currentLatestVersion by remember { mutableStateOf(latestVersion) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val newVersion = checkForUpdates()
        if (newVersion != null && isNewerVersion(newVersion, BuildConfig.VERSION_NAME)) {
            showUpdateCard = true
            currentLatestVersion = newVersion
        }
    }

    if (showDownloadDialog) {
        UpdateDownloadDialog(
            latestVersion = currentLatestVersion,
            onDismiss = { showDownloadDialog = false }
        )
    }

    if (showUpdateCard) {
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDownloadDialog = true }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Update icon with animation
                    val infiniteTransition = rememberInfiniteTransition(label = "update")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(scale)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.update),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.NewVersion) + ": $currentLatestVersion",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.tap_to_update),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateDownloadDialog(
    latestVersion: String,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var downloadStatus by remember { mutableStateOf(DownloadStatus.NOT_STARTED) }

    Dialog(onDismissRequest = {
        if (downloadStatus != DownloadStatus.REDIRECTING) {
            onDismiss()
        }
    }) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.update_version, latestVersion),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (downloadStatus) {
                    DownloadStatus.NOT_STARTED -> {
                        Text(
                            stringResource(R.string.download_question),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WaterDropButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.cancel),
                                colors = listOf(
                                    Color(0xFF2A2A2A),
                                    Color(0xFF3A3A3A),
                                    Color(0xFF2A2A2A)
                                )
                            )

                            WaterDropButton(
                                onClick = {
                                    downloadStatus = DownloadStatus.REDIRECTING
                                    val downloadUrl = "https://github.com/d0x-dev/AirBeats/releases/download/$latestVersion/AirBeats_v${latestVersion}_signed.apk"
                                    uriHandler.openUri(downloadUrl)
                                    downloadStatus = DownloadStatus.COMPLETED
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.download),
                                colors = listOf(
                                    Color(0xFF6C5CE7),
                                    Color(0xFFA463F5),
                                    Color(0xFFC45AF0)
                                )
                            )
                        }
                    }

                    DownloadStatus.REDIRECTING -> {
                        Text(
                            stringResource(R.string.opening_browser),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    DownloadStatus.COMPLETED -> {
                        Text(
                            stringResource(R.string.download_started),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        WaterDropButton(
                            onClick = onDismiss,
                            text = stringResource(R.string.close),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    DownloadStatus.ERROR -> {
                        Text(
                            stringResource(R.string.download_errorup),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        WaterDropButton(
                            onClick = onDismiss,
                            text = stringResource(R.string.close),
                            modifier = Modifier.fillMaxWidth(),
                            colors = listOf(
                                Color(0xFFCF6679),
                                Color(0xFFB0003A),
                                Color(0xFFCF6679)
                            )
                        )
                    }
                }
            }
        }
    }
}

enum class DownloadStatus {
    NOT_STARTED,
    REDIRECTING,
    COMPLETED,
    ERROR
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/d0x-dev/AirBeats/releases/latest")
        val connection = url.openConnection()
        connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        return@withContext jsonObject.getString("tag_name")
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    fun normalize(version: String): List<Int> {
        return version
            .replace(Regex("[^0-9.]"), "")
            .split(".")
            .map { it.toIntOrNull() ?: 0 }
    }

    val remote = normalize(remoteVersion)
    val current = normalize(currentVersion)

    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }

    return false
}

// ==================== MAIN SETTINGS SCREEN ====================
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    latestVersion: Long,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    var showTranslateDialog by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }

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
                    listOf(
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
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

        // Main Scaffold with TopAppBar that scrolls
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // U-Shaped TopAppBar that scrolls with content
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.settings),
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
                                listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(
                            width = 0.6.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
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
            // Content with proper padding from Scaffold
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding) // VERY IMPORTANT
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
            )

            {
                // Add a small top padding to separate from header

                val context = LocalContext.current
                val avatarManager = remember { AvatarPreferenceManager(context) }
                val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
                val accountName by rememberPreference(AccountNameKey, "")
                val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
                val isLoggedIn = remember(innerTubeCookie) {
                    "SAPISID" in parseCookieString(innerTubeCookie)
                }

                // Profile Section
                ProfileSection(
                    isLoggedIn = isLoggedIn,
                    accountName = accountName,
                    currentSelection = currentSelection
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Settings Categories
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // General Settings
                    SettingsCategory(
                        title = stringResource(R.string.general_settings),
                        items = listOf(
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.palette),
                                title = {
                                    Text(
                                        stringResource(R.string.appearance),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/appearance") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.schedule),
                                title = {
                                    Text(
                                        stringResource(R.string.always_on_display),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/always_on_display") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.person),
                                title = {
                                    Text(
                                        stringResource(R.string.account),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/account") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.language),
                                title = {
                                    Text(
                                        stringResource(R.string.content),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/content") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.play),
                                title = {
                                    Text(
                                        stringResource(R.string.player_and_audio),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/player") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.group),
                                title = {
                                    Text(
                                        stringResource(R.string.listen_together),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("listen_together") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.storage),
                                title = {
                                    Text(
                                        stringResource(R.string.storage),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/storage") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.security),
                                title = {
                                    Text(
                                        stringResource(R.string.privacy),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/privacy") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.restore),
                                title = {
                                    Text(
                                        stringResource(R.string.backup_restore),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/backup_restore") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.bug_report),
                                title = {
                                    Text(
                                        "Experimental Settings",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/experimental") }
                            )
                        )
                    )

                    // About & Community
                    SettingsCategory(
                        title = stringResource(R.string.community),
                        items = listOf(
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.info),
                                title = {
                                    Text(
                                        stringResource(R.string.about),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { navController.navigate("settings/about") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.translate), // Assuming R.drawable.translate exists. If not, I can use R.drawable.language
                                title = {
                                    Text(
                                        "Help Translate",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { uriHandler.openUri("https://crowdin.com/project/airbeats") }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.schedule),
                                title = {
                                    Text(
                                        stringResource(R.string.Changelog),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { showChangelogSheet = true }
                            ),
                            SettingsCategoryItem(
                                icon = painterResource(R.drawable.telegram),
                                title = {
                                    Text(
                                        "Telegram Bot",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { uriHandler.openUri("https://t.me/Stormxmusicrobot") }
                            )
                        )
                    )
                }

                // Update Card
                UpdateCard()

                // Version Card
                VersionCard(uriHandler)

                Spacer(Modifier.height(32.dp))
            }
        }

        // Dialogs and Bottom Sheets
        if (showTranslateDialog) {
            Dialog(onDismissRequest = { showTranslateDialog = false }) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.Redirección),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.poeditor_redirect),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WaterDropButton(
                                onClick = { showTranslateDialog = false },
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.cancel),
                                colors = listOf(
                                    Color(0xFF2A2A2A),
                                    Color(0xFF3A3A3A),
                                    Color(0xFF2A2A2A)
                                )
                            )

                            WaterDropButton(
                                onClick = {
                                    showTranslateDialog = false
                                    uriHandler.openUri("https://poeditor.com/join/project/208BwCVazA")
                                },
                                modifier = Modifier.weight(1f),
                                text = "OK",
                                colors = listOf(
                                    Color(0xFF6C5CE7),
                                    Color(0xFFA463F5),
                                    Color(0xFFC45AF0)
                                )
                            )
                        }
                    }
                }
            }
        }

        if (showChangelogSheet) {
            ModalBottomSheet(
                onDismissRequest = { showChangelogSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.Changelog),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        ChangelogScreen()

                        Spacer(Modifier.height(24.dp))

                        WaterDropButton(
                            onClick = { showChangelogSheet = false },
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.close),
                            colors = listOf(
                                Color(0xFF6C5CE7),
                                Color(0xFFA463F5),
                                Color(0xFFC45AF0)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    isLoggedIn: Boolean,
    accountName: String,
    currentSelection: AvatarSelection
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoggedIn) {
            var imageLoadError by remember { mutableStateOf(false) }
            var isImageLoading by remember { mutableStateOf(false) }

            // Avatar with glow effect
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .blur(20.dp)
                )

                // Avatar container
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        currentSelection is AvatarSelection.Custom && !imageLoadError -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                                    .crossfade(true)
                                    .listener(
                                        onStart = { isImageLoading = true },
                                        onSuccess = { _, _ ->
                                            isImageLoading = false
                                            imageLoadError = false
                                        },
                                        onError = { _, _ ->
                                            isImageLoading = false
                                            imageLoadError = true
                                        }
                                    )
                                    .build(),
                                contentDescription = "Avatar de $accountName",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            if (isImageLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        currentSelection is AvatarSelection.DiceBear && !imageLoadError -> {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data((currentSelection as AvatarSelection.DiceBear).url)
                                    .crossfade(true)
                                    .listener(
                                        onStart = { isImageLoading = true },
                                        onSuccess = { _, _ ->
                                            isImageLoading = false
                                            imageLoadError = false
                                        },
                                        onError = { _, _ ->
                                            isImageLoading = false
                                            imageLoadError = true
                                        }
                                    )
                                    .build(),
                                contentDescription = "Avatar DiceBear de $accountName",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            if (isImageLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        else -> {
                            val initials = remember(accountName) {
                                val cleanName = accountName.replace("@", "").trim()
                                when {
                                    cleanName.isEmpty() -> "?"
                                    cleanName.contains(" ") -> {
                                        val parts = cleanName.split(" ")
                                        "${parts.first().firstOrNull()?.uppercase() ?: ""}${
                                            parts.last().firstOrNull()?.uppercase() ?: ""
                                        }"
                                    }
                                    else -> cleanName.take(2).uppercase()
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            ),
                                            start = Offset(0f, 0f),
                                            end = Offset(100f, 100f)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Online indicator with pulse animation
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(x = 32.dp, y = 32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00FF00).copy(alpha = pulseAlpha),
                                    Color(0xFF00FF00).copy(alpha = 0.3f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username with animation
            AnimatedContent(
                targetState = accountName.replace("@", "").takeIf { it.isNotBlank() } ?: "",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "username"
            ) { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

        } else {
            // Not logged in state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo with glow effect
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .blur(20.dp)
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.airbeats_monochrome),
                        contentDescription = "Logo de AirBeats",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append("Air")
                            }

                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                append("Beats")
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Dev By DxV STUDIO 亗",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
