package com.darkxvenom.airbeats.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.appicon.AppIcon
import com.darkxvenom.airbeats.appicon.AppIconRepository
import com.darkxvenom.airbeats.ui.component.DefaultDialog
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.component.ScreenAdaptiveBackground
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppIconScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }

    var activeIconId by remember { mutableStateOf(AppIconRepository.getActiveIconId(context)) }
    val builtInIcons = remember { AppIconRepository.getAvailableIcons() }

    var isRefreshingCommunity by remember { mutableStateOf(false) }
    var communityIcons by remember { mutableStateOf<List<AppIcon>>(emptyList()) }

    fun refreshCommunityIcons() {
        coroutineScope.launch {
            isRefreshingCommunity = true
            communityIcons = AppIconRepository.fetchCommunityIcons(context)
            isRefreshingCommunity = false
        }
    }

    LaunchedEffect(Unit) {
        refreshCommunityIcons()
    }

    // Top section: inApp == true (Default Brand, Airbeats Winters, etc.)
    val inAppIcons = remember(builtInIcons, communityIcons) {
        val remoteInApp = communityIcons.filter { it.inApp }
        (builtInIcons + remoteInApp).distinctBy { it.id }
    }

    // Bottom section: inApp == false (pure community shortcut icons)
    val shortcutIcons = remember(communityIcons) {
        communityIcons.filter { !it.inApp }
    }

    val selectedIcon = remember(activeIconId, inAppIcons, shortcutIcons) {
        (inAppIcons + shortcutIcons).find { it.id == activeIconId } ?: AppIconRepository.DEFAULT_ICON
    }

    var showSubmitDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenAdaptiveBackground(
            artworkUrl = mediaMetadata?.thumbnailUrl
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "App Icon",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior,
                    modifier = Modifier
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(
                            width = 0.6.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.White.copy(alpha = 0.25f)
                                )
                            ),
                            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                        )
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    ),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Hero Preview Card
                item {
                    HomeScreenMockupCard(
                        icon = selectedIcon,
                        onPinToHomeScreen = { iconToPin ->
                            coroutineScope.launch {
                                val success = AppIconRepository.applyCommunityIcon(context, iconToPin)
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        "Adding \"${iconToPin.title}\" to Home screen...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Could not add shortcut on this launcher",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }

                // 2. In-App Launcher Icons Section Header
                item {
                    Column {
                        Text(
                            text = "App Icons",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Official app launcher icons. Tap to switch directly.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 3. Official App Icons (Default Brand, Airbeats Winters, etc.)
                items(inAppIcons.size) { index ->
                    val icon = inAppIcons[index]
                    val isSelected = activeIconId == icon.id
                    InAppIconCard(
                        icon = icon,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                val success = AppIconRepository.setActiveIcon(context, icon.id)
                                if (success) {
                                    activeIconId = icon.id
                                    Toast.makeText(
                                        context,
                                        "App icon changed to \"${icon.title}\"",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to change app icon",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    )
                }

                // 4. Community Icons Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Community Icons",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                            Text(
                                text = "Submitted by community. Pin as shortcut until in-app update.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (shortcutIcons.isNotEmpty()) {
                                Text(
                                    text = "${shortcutIcons.size} available",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Checking GitHub for new icons...", Toast.LENGTH_SHORT).show()
                                    refreshCommunityIcons()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                if (isRefreshingCommunity) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.sync),
                                        contentDescription = "Refresh from GitHub",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Community Icons Grid (where inApp == false)
                if (shortcutIcons.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isRefreshingCommunity) "Checking GitHub for icons..." else "No community shortcut icons right now. All verified icons are in App Icons above!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    val shortcutIconRows = shortcutIcons.chunked(2)
                    items(shortcutIconRows.size) { rowIndex ->
                        val rowIcons = shortcutIconRows[rowIndex]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (icon in rowIcons) {
                                Box(modifier = Modifier.weight(1f)) {
                                    CommunityIconCard(
                                        icon = icon,
                                        isSelected = icon.id == activeIconId,
                                        onClick = {
                                            activeIconId = icon.id
                                        },
                                        onPin = {
                                            activeIconId = icon.id
                                            coroutineScope.launch {
                                                val success = AppIconRepository.applyCommunityIcon(context, icon)
                                                if (success) {
                                                    Toast.makeText(
                                                        context,
                                                        "Adding \"${icon.title}\" to Home screen...",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Could not add shortcut on this launcher",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            if (rowIcons.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 6. Community Submission Banner
                item {
                    CommunitySubmissionBanner(
                        onOpenSubmitDialog = { showSubmitDialog = true }
                    )
                }

                // 7. System Launcher Notice
                item {
                    SystemNoticeCard()
                }
            }
        }

        if (showSubmitDialog) {
            CommunityIconSubmitDialog(
                onDismiss = { showSubmitDialog = false }
            )
        }
    }
}

/**
 * Realistic Home Screen Mockup previewing the selected icon.
 */
@Composable
private fun HomeScreenMockupCard(
    icon: AppIcon,
    onPinToHomeScreen: ((AppIcon) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Active badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                        )
                        Text(
                            text = "Live Launcher Preview",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Large App Icon Preview
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = MaterialTheme.colorScheme.primary)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(icon.bgColors))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon.id == "default") {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = icon.title,
                            modifier = Modifier.fillMaxSize(0.72f)
                        )
                    } else if (icon.id == "airbeats_winters") {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_winter_foreground),
                            contentDescription = icon.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (icon.id == "melody_hearts") {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_melody_hearts_foreground),
                            contentDescription = icon.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (icon.id == "gta_6") {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_gta6_foreground),
                            contentDescription = icon.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!icon.svgUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(icon.svgUrl)
                                .apply {
                                    if (icon.svgUrl.endsWith(".svg", ignoreCase = true)) {
                                        decoderFactory(SvgDecoder.Factory())
                                    }
                                }
                                .crossfade(true)
                                .build(),
                            contentDescription = icon.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (icon.fgTint != null) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = icon.title,
                            colorFilter = ColorFilter.tint(icon.fgTint),
                            modifier = Modifier.fillMaxSize(0.72f)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = icon.title,
                            modifier = Modifier.fillMaxSize(0.72f)
                        )
                    }
                }

                // Label under icon
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${icon.title} (${icon.subtitle})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (!icon.inApp && onPinToHomeScreen != null) {
                    Button(
                        onClick = { onPinToHomeScreen(icon) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to Home Screen",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dedicated In-App Official Launcher Icon Card.
 */
@Composable
private fun InAppIconCard(
    icon: AppIcon,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(icon.bgColors))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (icon.id == "default") {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = icon.title,
                        modifier = Modifier.fillMaxSize(0.72f)
                    )
                } else if (icon.id == "airbeats_winters") {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_winter_foreground),
                        contentDescription = icon.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (icon.id == "melody_hearts") {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_melody_hearts_foreground),
                        contentDescription = icon.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (icon.id == "gta_6") {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_gta6_foreground),
                        contentDescription = icon.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!icon.svgUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(icon.svgUrl)
                            .apply {
                                if (icon.svgUrl.endsWith(".svg", ignoreCase = true)) {
                                    decoderFactory(SvgDecoder.Factory())
                                }
                            }
                            .crossfade(true)
                            .build(),
                        contentDescription = icon.title,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = icon.title,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = icon.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = icon.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active Icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = "Apply", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/**
 * Community Icon Card loaded dynamically from GitHub.
 */
@Composable
private fun CommunityIconCard(
    icon: AppIcon,
    isSelected: Boolean,
    onClick: () -> Unit,
    onPin: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.TopEnd
            ) {
                // Adaptive Squircle Badge
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(10.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.linearGradient(icon.bgColors))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!icon.svgUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(icon.svgUrl)
                                .apply {
                                    if (icon.svgUrl.endsWith(".svg", ignoreCase = true)) {
                                        decoderFactory(SvgDecoder.Factory())
                                    }
                                }
                                .crossfade(true)
                                .build(),
                            contentDescription = icon.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Checkmark badge if selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = icon.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "by ${icon.author}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onPin,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add to Home",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * Community submission banner card.
 */
@Composable
private fun CommunitySubmissionBanner(
    onOpenSubmitDialog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Design an AirBeats Icon?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Submit an SVG link to get your theme included!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Anyone in the community can contribute. Share your vector design with a raw GitHub SVG URL and it will be added to the live catalog for everyone to pin to their home screen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onOpenSubmitDialog,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Submit Custom Icon",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

/**
 * Explanatory card for system launcher refresh behavior.
 */
@Composable
private fun SystemNoticeCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "The Default Icon changes the primary app launcher icon. Community icons from GitHub are pinned directly to your Home screen with their unique vector artwork via the system Add to Home screen feature.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Community icon submission dialog.
 */
@Composable
private fun CommunityIconSubmitDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var iconName by rememberSaveable { mutableStateOf("") }
    var designerName by rememberSaveable { mutableStateOf("") }
    var svgLink by rememberSaveable { mutableStateOf("") }

    DefaultDialog(
        onDismiss = onDismiss,
        title = {
            Text(
                text = "Submit Icon Design",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
            Button(
                onClick = {
                    if (svgLink.isBlank()) {
                        Toast.makeText(context, "Please enter an SVG link", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val url = AppIconRepository.buildCommunitySubmissionUrl(
                        iconTitle = iconName.ifBlank { "Custom Community Icon" },
                        authorName = designerName.ifBlank { "Community Contributor" },
                        svgUrl = svgLink.trim()
                    )
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = svgLink.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Open GitHub")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Provide a direct public link to your SVG (e.g. GitHub raw file or Gist). We will review it and add it to the AirBeats icon catalog!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = iconName,
                onValueChange = { iconName = it },
                label = { Text("Icon Title") },
                placeholder = { Text("e.g. Midnight Cyber") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = designerName,
                onValueChange = { designerName = it },
                label = { Text("Your Name / GitHub Handle") },
                placeholder = { Text("e.g. @yourhandle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = svgLink,
                onValueChange = { svgLink = it },
                label = { Text("Direct SVG URL *") },
                placeholder = { Text("https://raw.githubusercontent.com/.../icon.svg") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Live preview if URL starts with http
            if (svgLink.startsWith("http")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = svgLink.trim(),
                            contentDescription = "SVG Preview",
                            modifier = Modifier.fillMaxSize(0.8f)
                        )
                    }
                    Column {
                        Text(
                            text = "Live SVG Preview",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Will be loaded via URL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val template = """
                        [AirBeats Icon Submission]
                        Title: ${iconName.ifBlank { "Custom Icon" }}
                        Author: ${designerName.ifBlank { "Anonymous" }}
                        SVG Link: ${svgLink.trim()}
                    """.trimIndent()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Icon Submission", template))
                    Toast.makeText(context, "Copied submission template to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Copy Submission Template")
            }
        }
    }
}
