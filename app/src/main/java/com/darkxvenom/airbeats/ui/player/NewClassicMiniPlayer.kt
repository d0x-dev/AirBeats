package com.darkxvenom.airbeats.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.extensions.togglePlayPause
import com.darkxvenom.airbeats.ui.component.BottomSheetState
import com.darkxvenom.airbeats.ui.component.bottomSheetDraggable
import com.darkxvenom.airbeats.ui.utils.highQualityThumbnail
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NewClassicMiniPlayer(
    position: Long,
    duration: Long,
    state: BottomSheetState,
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    if (mediaMetadata == null) return

    val progress = if (duration > 0) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(0.73f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp)
            .bottomSheetDraggable(state)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragStartTime = System.currentTimeMillis()
                        totalDragDistance = 0f
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = animationSpec
                            )
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        val adjustedDragAmount =
                            if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                        totalDragDistance += abs(adjustedDragAmount)
                        coroutineScope.launch {
                            offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                        }
                    },
                    onDragEnd = {
                        val dragDuration = System.currentTimeMillis() - dragStartTime
                        val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                        val currentOffset = offsetXAnimatable.value

                        val minDistanceThreshold = 50f
                        val velocityThreshold = (0.73f * -8.25f) + 8.5f

                        val shouldChangeSong = (
                                abs(currentOffset) > minDistanceThreshold &&
                                        velocity > velocityThreshold
                                ) || (abs(currentOffset) > autoSwipeThreshold)

                        if (shouldChangeSong) {
                            val isRightSwipe = currentOffset > 0
                            if (isRightSwipe) {
                                playerConnection.player.seekToPreviousMediaItem()
                            } else {
                                playerConnection.player.seekToNext()
                            }
                        }

                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(
                                targetValue = 0f,
                                animationSpec = animationSpec
                            )
                        }
                    }
                )
            }
    ) {
        // Inner sliding container that responds to drag gestures
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { state.expandSoft() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                // 1. Artwork with progress ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(46.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                        strokeWidth = 2.5.dp
                    )

                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl?.highQualityThumbnail(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 2. Info (Title & Artist with basicMarquee)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = mediaMetadata?.title.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                // 3. Artist Avatar Button
                IconButton(
                    onClick = {
                        val firstArtist = mediaMetadata?.artists?.firstOrNull()
                        if (firstArtist?.id != null && navController != null) {
                            navController.navigate("artist/${firstArtist.id}")
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = "Artist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 4. Like Heart Button with Spring Animation
                val isLiked = currentSong?.song?.liked == true
                IconButton(
                    onClick = { playerConnection.toggleLike() },
                    modifier = Modifier.size(36.dp)
                ) {
                    val heartScale by animateFloatAsState(
                        targetValue = if (isLiked) 1.25f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "heartScale"
                    )
                    Icon(
                        painter = painterResource(
                            if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                scaleX = heartScale
                                scaleY = heartScale
                            }
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 5. Airbeats Play/Pause Button (Cookie9Sided shape)
                FilledIconButton(
                    onClick = { playerConnection.player.togglePlayPause() },
                    modifier = Modifier.size(44.dp),
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(150)) +
                                    scaleIn(
                                        initialScale = 0.4f, animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    ).togetherWith(
                                    fadeOut(animationSpec = tween(100)) +
                                            scaleOut(targetScale = 1.6f, animationSpec = tween(100))
                                )
                        },
                        label = "playPauseIcon",
                    ) { playing ->
                        Icon(
                            painter = painterResource(
                                if (playing) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Real-time swipe indicator arrow
        if (abs(offsetXAnimatable.value) > 30f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (abs(offsetXAnimatable.value) / autoSwipeThreshold.toFloat()).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
