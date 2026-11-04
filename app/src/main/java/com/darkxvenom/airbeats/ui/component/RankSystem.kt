package com.darkxvenom.airbeats.ui.component

import androidx.compose.ui.res.stringResource

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.viewmodels.StatsViewModel
import kotlinx.coroutines.launch

/**
 * Exquisite custom‑drawn Compose RankBadge.
 * Each badge is unique, styled with a harmonious gradient, custom Canvas-drawn emblem, and glowing borders.
 * Requires NO local drawable resource files, preventing compilation errors!
 */
@Composable
fun RankBadge(
    rank: AirBeatsRank,
    displayedRank: AirBeatsRank?,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier,
) {
    val badge = displayedRank ?: rank
    val colors = when (badge) {
        AirBeatsRank.Echo -> listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))
        AirBeatsRank.Pulse -> listOf(Color(0xFF00FF87), Color(0xFF60EFFF))
        AirBeatsRank.Bronze -> listOf(Color(0xFFCA7345), Color(0xFFEAA17C))
        AirBeatsRank.Silver -> listOf(Color(0xFFBDC3C7), Color(0xFFE5E9F0))
        AirBeatsRank.Gold -> listOf(Color(0xFFFFD700), Color(0xFFFFA500))
        AirBeatsRank.Platinum -> listOf(Color(0xFFE5E9F0), Color(0xFFB0C4DE))
        AirBeatsRank.Diamond -> listOf(Color(0xFF00F2FE), Color(0xFF9B51E0))
        AirBeatsRank.Elite -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
        AirBeatsRank.Master -> listOf(Color(0xFFFF007F), Color(0xFFFF5E62))
        AirBeatsRank.Legend -> listOf(Color(0xFFF12711), Color(0xFFF5AF19))
        AirBeatsRank.Mythic -> listOf(Color(0xFF0575E6), Color(0xFF00F260))
        AirBeatsRank.Immortal -> listOf(Color(0xFF1F1C2C), Color(0xFF928DAB))
        AirBeatsRank.Cosmic -> listOf(Color(0xFF1A1A2E), Color(0xFFE94560))
        AirBeatsRank.Nova -> listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
        AirBeatsRank.Celestial -> listOf(Color(0xFF7F00FF), Color(0xFFE100FF))
        AirBeatsRank.Godlike -> listOf(Color(0xFFFF007F), Color(0xFFFFD700), Color(0xFF00F2FE))
        AirBeatsRank.Universal -> listOf(Color(0xFF00C6FF), Color(0xFF0072FF))
        AirBeatsRank.Eternal -> listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))
    }

    val gradientBrush = Brush.linearGradient(colors)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.25f))
            .border(
                width = if (size > 30.dp) 2.5.dp else 1.5.dp,
                brush = gradientBrush,
                shape = CircleShape
            )
            .padding(if (size > 30.dp) 6.dp else 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = if (size > 30.dp) 4f else 2f
            when (badge) {
                AirBeatsRank.Echo -> {
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 4)
                }
                AirBeatsRank.Pulse -> {
                    val path = Path().apply {
                        moveTo(0f, this@Canvas.size.height / 2)
                        lineTo(this@Canvas.size.width * 0.3f, this@Canvas.size.height / 2)
                        lineTo(this@Canvas.size.width * 0.45f, this@Canvas.size.height * 0.15f)
                        lineTo(this@Canvas.size.width * 0.55f, this@Canvas.size.height * 0.85f)
                        lineTo(this@Canvas.size.width * 0.7f, this@Canvas.size.height / 2)
                        lineTo(this@Canvas.size.width, this@Canvas.size.height / 2)
                    }
                    drawPath(path, brush = gradientBrush, style = Stroke(strokeWidth))
                }
                AirBeatsRank.Bronze -> {
                    val path = Path().apply {
                        val cx = this@Canvas.size.width / 2
                        val cy = this@Canvas.size.height / 2
                        val r = this@Canvas.size.minDimension / 2.5f
                        for (i in 0 until 5) {
                            val angle = (i * 2 * Math.PI / 5) - Math.PI / 2
                            val x = (cx + r * Math.cos(angle)).toFloat()
                            val y = (cy + r * Math.sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                }
                AirBeatsRank.Silver -> {
                    val path = Path().apply {
                        val w1 = this@Canvas.size.width
                        val h1 = this@Canvas.size.height
                        moveTo(w1 * 0.2f, h1 * 0.2f)
                        lineTo(w1 * 0.8f, h1 * 0.2f)
                        lineTo(w1 * 0.8f, h1 * 0.6f)
                        quadraticTo(w1 * 0.8f, h1 * 0.9f, w1 * 0.5f, h1 * 0.98f)
                        quadraticTo(w1 * 0.2f, h1 * 0.9f, w1 * 0.2f, h1 * 0.6f)
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                }
                AirBeatsRank.Gold -> {
                    val path = Path().apply {
                        val cx = this@Canvas.size.width / 2
                        val cy = this@Canvas.size.height / 2
                        val rOuter = this@Canvas.size.minDimension / 2.2f
                        val rInner = rOuter / 2.2f
                        for (i in 0 until 10) {
                            val r = if (i % 2 == 0) rOuter else rInner
                            val angle = (i * Math.PI / 5) - Math.PI / 2
                            val x = (cx + r * Math.cos(angle)).toFloat()
                            val y = (cy + r * Math.sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                }
                AirBeatsRank.Platinum -> {
                    val path = Path().apply {
                        val cx = this@Canvas.size.width / 2
                        val cy = this@Canvas.size.height / 2
                        val r = this@Canvas.size.minDimension / 2.4f
                        for (i in 0 until 6) {
                            val angle = (i * Math.PI / 3) - Math.PI / 2
                            val x = (cx + r * Math.cos(angle)).toFloat()
                            val y = (cy + r * Math.sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                }
                AirBeatsRank.Diamond -> {
                    val path = Path().apply {
                        val cx = this@Canvas.size.width / 2
                        val cy = this@Canvas.size.height / 2
                        val rx = this@Canvas.size.width / 2.2f
                        val ry = this@Canvas.size.height / 2.2f
                        moveTo(cx, cy - ry)
                        lineTo(cx + rx, cy)
                        lineTo(cx, cy + ry)
                        lineTo(cx - rx, cy)
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                }
                AirBeatsRank.Elite -> {
                    val path = Path().apply {
                        val w1 = this@Canvas.size.width
                        val h1 = this@Canvas.size.height
                        moveTo(w1 * 0.15f, h1 * 0.8f)
                        lineTo(w1 * 0.15f, h1 * 0.35f)
                        lineTo(w1 * 0.38f, h1 * 0.55f)
                        lineTo(w1 * 0.5f, h1 * 0.22f)
                        lineTo(w1 * 0.62f, h1 * 0.55f)
                        lineTo(w1 * 0.85f, h1 * 0.35f)
                        lineTo(w1 * 0.85f, h1 * 0.8f)
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                }
                AirBeatsRank.Master -> {
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 2.5f, style = Stroke(strokeWidth))
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 4.8f)
                }
                AirBeatsRank.Legend -> {
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 3f)
                    val starPath = Path().apply {
                        val cx = this@Canvas.size.width / 2
                        val cy = this@Canvas.size.height / 2
                        val rOuter = this@Canvas.size.minDimension / 4.5f
                        val rInner = rOuter / 2f
                        for (i in 0 until 10) {
                            val r = if (i % 2 == 0) rOuter else rInner
                            val angle = (i * Math.PI / 5) - Math.PI / 2
                            val x = (cx + r * Math.cos(angle)).toFloat()
                            val y = (cy + r * Math.sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(starPath, brush = Brush.linearGradient(listOf(Color.White, Color.Transparent)))
                }
                AirBeatsRank.Mythic -> {
                    val path = Path().apply {
                        val w1 = this@Canvas.size.width
                        val h1 = this@Canvas.size.height
                        moveTo(w1 * 0.25f, h1 * 0.5f)
                        cubicTo(w1 * 0.25f, h1 * 0.25f, w1 * 0.48f, h1 * 0.25f, w1 * 0.5f, h1 * 0.5f)
                        cubicTo(w1 * 0.52f, h1 * 0.75f, w1 * 0.75f, h1 * 0.75f, w1 * 0.75f, h1 * 0.5f)
                        cubicTo(w1 * 0.75f, h1 * 0.25f, w1 * 0.52f, h1 * 0.25f, w1 * 0.5f, h1 * 0.5f)
                        cubicTo(w1 * 0.48f, h1 * 0.75f, w1 * 0.25f, h1 * 0.75f, w1 * 0.25f, h1 * 0.5f)
                    }
                    drawPath(path, brush = gradientBrush, style = Stroke(strokeWidth * 1.5f))
                }
                AirBeatsRank.Immortal -> {
                    val path = Path().apply {
                        val cx = this@Canvas.size.width / 2
                        val cy = this@Canvas.size.height / 2
                        val r = this@Canvas.size.minDimension / 2.5f
                        moveTo(cx - r, cy - r)
                        lineTo(cx + r, cy - r)
                        lineTo(cx + r, cy + r)
                        lineTo(cx - r, cy + r)
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                    drawLine(color = Color.White.copy(alpha = 0.6f), start = Offset(this.size.width / 2, this.size.height * 0.2f), end = Offset(this.size.width / 2, this.size.height * 0.8f), strokeWidth = strokeWidth)
                }
                AirBeatsRank.Cosmic -> {
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 3f)
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 1.8f, style = Stroke(strokeWidth / 1.5f))
                }
                AirBeatsRank.Nova -> {
                    for (i in 0 until 8) {
                        val angle = (i * Math.PI / 4).toFloat()
                        val dx = (Math.cos(angle.toDouble()) * this@Canvas.size.width / 2.2).toFloat()
                        val dy = (Math.sin(angle.toDouble()) * this@Canvas.size.height / 2.2).toFloat()
                        drawLine(brush = gradientBrush, start = center, end = center + Offset(dx, dy), strokeWidth = strokeWidth * 1.2f)
                    }
                    drawCircle(color = Color.White, radius = this.size.minDimension / 5f)
                }
                AirBeatsRank.Celestial -> {
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 2.6f, style = Stroke(strokeWidth))
                    val path = Path().apply {
                        val cx = this@Canvas.size.width / 2
                        val cy = this@Canvas.size.height / 2
                        val rx = this@Canvas.size.width / 3.5f
                        val ry = this@Canvas.size.height / 3.5f
                        moveTo(cx, cy - ry)
                        lineTo(cx + rx, cy)
                        lineTo(cx, cy + ry)
                        lineTo(cx - rx, cy)
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                }
                AirBeatsRank.Godlike -> {
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 2.4f, style = Stroke(strokeWidth * 1.5f))
                    val path = Path().apply {
                        val cx = this@Canvas.size.width / 2
                        val cy = this@Canvas.size.height / 2
                        val r = this@Canvas.size.minDimension / 3.5f
                        for (i in 0 until 3) {
                            val angle = (i * 2 * Math.PI / 3) - Math.PI / 2
                            val x = (cx + r * Math.cos(angle)).toFloat()
                            val y = (cy + r * Math.sin(angle)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(path, brush = gradientBrush)
                }
                AirBeatsRank.Universal -> {
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 2.2f, style = Stroke(strokeWidth))
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 3.2f, style = Stroke(strokeWidth * 0.8f))
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 5.2f)
                }
                AirBeatsRank.Eternal -> {
                    drawCircle(brush = gradientBrush, radius = this.size.minDimension / 2.1f, style = Stroke(strokeWidth * 1.6f))
                    val path = Path().apply {
                        val w1 = this@Canvas.size.width
                        val h1 = this@Canvas.size.height
                        moveTo(w1 * 0.3f, h1 * 0.5f)
                        cubicTo(w1 * 0.3f, h1 * 0.3f, w1 * 0.48f, h1 * 0.3f, w1 * 0.5f, h1 * 0.5f)
                        cubicTo(w1 * 0.52f, h1 * 0.7f, w1 * 0.7f, h1 * 0.7f, w1 * 0.7f, h1 * 0.5f)
                        cubicTo(w1 * 0.7f, h1 * 0.3f, w1 * 0.52f, h1 * 0.3f, w1 * 0.5f, h1 * 0.5f)
                        cubicTo(w1 * 0.48f, h1 * 0.7f, w1 * 0.3f, h1 * 0.7f, w1 * 0.3f, h1 * 0.5f)
                    }
                    drawPath(path, brush = Brush.linearGradient(listOf(Color.White, Color(0xFFFFD700))), style = Stroke(strokeWidth * 1.2f))
                }
            }
        }
    }
}

/**
 * Animated popup that celebrates a rank‑up with state‑of‑the‑art entrance springs, rotating celestial borders, and glowing backgrounds.
 */
@Composable
fun RankUpPopup(
    newRank: AirBeatsRank,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }

    val scaleAnim = remember { Animatable(0.2f) }
    val rotateAnim = remember { Animatable(0f) }
    val glowAnim = remember { Animatable(0.6f) }

    LaunchedEffect(visible) {
        if (visible) {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
            )
        }
    }

    LaunchedEffect(Unit) {
        rotateAnim.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(9000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    LaunchedEffect(Unit) {
        glowAnim.animateTo(
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    if (visible) {
        Dialog(
            onDismissRequest = { visible = false; onDismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .clickable { visible = false; onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                // Soft background radial glow
                Box(
                    modifier = Modifier
                        .size(310.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value * glowAnim.value
                            scaleY = scaleAnim.value * glowAnim.value
                            rotationZ = rotateAnim.value
                            alpha = scaleAnim.value * 0.28f
                        }
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF8E2DE2),
                                    Color(0xFF00F2FE),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Popup card container
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                            shadowElevation = 24f
                        }
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF131324).copy(alpha = 0.94f))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF00F2FE), Color(0xFFFF007F), Color(0xFFFFD700))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.ui_sweep_new_tier_unlocked),
                            color = Color(0xFFFFD700),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.5.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(130.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(122.dp)
                                    .graphicsLayer { rotationZ = rotateAnim.value }
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.sweepGradient(
                                            listOf(Color(0xFFFF007F), Color(0xFF00F2FE), Color(0xFFFFD700), Color(0xFFFF007F))
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            RankBadge(rank = newRank, displayedRank = null, size = 80.dp)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = newRank.name,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Tier reached at ${newRank.thresholdHours} hours listened",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { visible = false; onDismiss() },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A00E0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ui_sweep_awesome),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Premium custom badge customization interface that renders inside the settings/avatar customizer screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RankBadgeSelector(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val statsViewModel = com.darkxvenom.airbeats.ui.utils.safeHiltViewModel<StatsViewModel>()
    val totalHours by (statsViewModel?.totalListenHours ?: kotlinx.coroutines.flow.flowOf(0.0)).collectAsState(initial = 0.0)
    val currentRank by (statsViewModel?.currentRank ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)

    val rankPrefMgr = remember { RankPreferenceManager(context) }
    val displayedRank by rankPrefMgr.displayedRank.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val unlockedRanks = remember(totalHours) {
        unlockedRanksFromHours(totalHours)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.ui_text_rank_badge_customisation),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.ui_sweep_showcase_any_of_your_unlocked_badges_next_to_your_name_changing_t),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    coroutineScope.launch {
                        rankPrefMgr.saveDisplayedRank(null)
                    }
                }
                .background(
                    if (displayedRank == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else Color.Transparent
                )
                .border(
                    width = 1.dp,
                    color = if (displayedRank == null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.cached),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ui_sweep_auto_show_highest_rank),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Current: ${currentRank?.name ?: "No rank unlocked (needs 1h)"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (displayedRank == null) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.ui_text_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.ui_text_all_ranks),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {
            AirBeatsRank.values().forEach { rank ->
                val isUnlocked = unlockedRanks.contains(rank)
                val isSelected = displayedRank == rank

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else if (!isUnlocked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else if (!isUnlocked) Color.Transparent
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = isUnlocked) {
                            coroutineScope.launch {
                                rankPrefMgr.saveDisplayedRank(rank)
                            }
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    alpha = if (isUnlocked) 1f else 0.35f
                                }
                        ) {
                            RankBadge(rank = rank, displayedRank = rank, size = 32.dp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rank.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${rank.thresholdHours}h",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = if (isUnlocked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    if (!isUnlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = stringResource(R.string.ui_a11y_locked),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Basic dialog selector fallback.
 */
@Composable
fun BadgeSelector(
    unlockedRanks: List<AirBeatsRank>,
    currentDisplayed: AirBeatsRank?,
    onSelect: (AirBeatsRank?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_text_select_badge)) },
        text = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(null) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.auto_real_rank), fontWeight = if (currentDisplayed == null) FontWeight.Bold else FontWeight.Normal)
                }
                unlockedRanks.forEach { rank ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(rank) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RankBadge(rank = rank, displayedRank = rank, size = 24.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(rank.name, fontWeight = if (currentDisplayed == rank) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

fun unlockedRanksFromHours(hours: Double): List<AirBeatsRank> {
    return AirBeatsRank.values().filter { hours >= it.thresholdHours }
}
