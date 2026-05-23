// AirBeats - About and Contributors Screen
package com.darkxvenom.airbeats.ui.screens.settings

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.darkxvenom.airbeats.BuildConfig
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.ui.component.IconButton

// ==================== SHIMMER EFFECT ====================

@Composable
fun shimmerEffect(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmerEffect")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmerEffect"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )
}

// ==================== USER CARD ====================

@Composable
fun SocialIconBadge(
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun UserCard(
    imageUrl: String,
    name: String,
    role: String,
    githubUrl: String? = null,
    telegramUrl: String? = null,
    instagramUrl: String? = null,
    websiteUrl: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
        )
    )

    Card(
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .height(240.dp)
            .scale(if (isPressed) 0.98f else 1f)
            .shadow(12.dp, RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isPressed = true
                onClick()
                isPressed = false
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f),
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar - Centered at top
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .border(1.5.dp, borderBrush, CircleShape)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Text - Centered
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Social Badges Row at the bottom of the card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val contextUriHandler = LocalUriHandler.current
                if (githubUrl != null) {
                    SocialIconBadge(
                        iconRes = R.drawable.github,
                        onClick = { contextUriHandler.openUri(githubUrl) }
                    )
                }
                if (telegramUrl != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    SocialIconBadge(
                        iconRes = R.drawable.telegram,
                        onClick = { contextUriHandler.openUri(telegramUrl) }
                    )
                }
                if (instagramUrl != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    SocialIconBadge(
                        iconRes = R.drawable.instagram,
                        onClick = { contextUriHandler.openUri(instagramUrl) }
                    )
                }
                if (websiteUrl != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    SocialIconBadge(
                        iconRes = R.drawable.resource_public,
                        onClick = { contextUriHandler.openUri(websiteUrl) }
                    )
                }
            }
        }
    }
}

// ==================== SOCIAL ICON ROW ====================

@Composable
fun SocialIconRow(uriHandler: UriHandler) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(4.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { uriHandler.openUri("https://www.facebook.com/venom.digital.creator") }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.facebook),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = { uriHandler.openUri("https://www.instagram.com/Dark__336/") }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.instagram),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = { uriHandler.openUri("https://github.com/d0x-dev") }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.github),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = { uriHandler.openUri("https://g.dev/Darkboy336") }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.google),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = { uriHandler.openUri("https://AirBeats.stormx.pw/") }
            ) {
                Icon(
                    modifier = Modifier.size(22.dp),
                    painter = painterResource(R.drawable.resource_public),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ==================== MAIN ABOUT SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current
    val shimmerBrush = shimmerEffect()

    // Get player connection for album artwork
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🎵 BLUR BACKGROUND
        val artworkUrl = mediaMetadata?.thumbnailUrl

        artworkUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(90.dp)
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

        // Main Scaffold with new TopAppBar
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // U-Shaped TopAppBar - NO BACK BUTTON
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.about),
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
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
                    .padding(innerPadding) // This pushes content below the top bar
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp), // Add small top padding for spacing
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo with shimmer
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.surfaceColorAtElevation(
                                    NavigationBarDefaults.Elevation
                                )
                            )
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.airbeats_monochrome),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.onBackground,
                                BlendMode.SrcIn
                            ),
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { }
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(shimmerBrush)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // App Name
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(color = MaterialTheme.colorScheme.primary)
                            ) {
                                append("Air")
                            }
                            withStyle(
                                SpanStyle(color = MaterialTheme.colorScheme.secondary)
                            ) {
                                append("Beats")
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Version badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = BuildConfig.VERSION_NAME.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )

                        if (BuildConfig.DEBUG) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "DEBUG",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Dev By DarkXVenom 亗",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Social Icons
                    SocialIconRow(uriHandler)

                    Spacer(Modifier.height(16.dp))

                    // Contributors Title - LEFT ALIGNED
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.group),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.contributors),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Contributors Cards - SIDE-BY-SIDE VERTICAL CARDS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        UserCard(
                            imageUrl = "https://avatars.githubusercontent.com/u/218248866",
                            name = "Darkboy",
                            role = "Lead Developer",
                            githubUrl = "https://github.com/d0x-dev",
                            telegramUrl = "https://t.me/songpy",
                            instagramUrl = "https://instagram.com/dark__336",
                            websiteUrl = "https://darkboy.pro",
                            modifier = Modifier.weight(1f),
                            onClick = { uriHandler.openUri("https://darkboy.pro") }
                        )

                        UserCard(
                            imageUrl = "https://avatars.githubusercontent.com/u/241423835",
                            name = "Venom",
                            role = "UI/UX Specialist",
                            githubUrl = "https://github.com/drkvenom786",
                            websiteUrl = "https://venomx.pro",
                            modifier = Modifier.weight(1f),
                            onClick = { uriHandler.openUri("https://drkvenom786.github.io/webpage/") }
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}