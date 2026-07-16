package com.darkxvenom.airbeats.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Timeline
import com.darkxvenom.airbeats.extensions.metadata
import kotlin.math.abs

/**
 * RadialSongCarousel — file name kept, visual updated to match the white
 * paper theme.  The dark rotating 3-D effect is replaced with a clean
 * flat horizontal scroll where the centred item is highlighted in ink and
 * neighbours are muted.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RadialSongCarousel(
    queueWindows: List<Timeline.Window>,
    currentWindowIndex: Int,
    onSongSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState    = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density      = LocalDensity.current

    val itemWidthDp  = 160.dp
    val itemWidthPx  = with(density) { itemWidthDp.toPx() }
    val spacingDp    = 12.dp

    var parentWidthPx by remember { mutableStateOf(0f) }
    var hasInitialScrolled by remember { mutableStateOf(false) }
    var lastScrolledIndex  by remember { mutableStateOf(-1) }

    LaunchedEffect(currentWindowIndex, parentWidthPx) {
        if (parentWidthPx <= 0f) return@LaunchedEffect
        if (!hasInitialScrolled || lastScrolledIndex != currentWindowIndex) {
            val offset = (parentWidthPx / 2f - itemWidthPx / 2f).toInt()
            listState.scrollToItem(currentWindowIndex, -offset)
            hasInitialScrolled  = true
            lastScrolledIndex   = currentWindowIndex
        }
    }

    val centeredIndex by remember {
        derivedStateOf {
            val info  = listState.layoutInfo
            val items = info.visibleItemsInfo
            if (items.isEmpty()) return@derivedStateOf currentWindowIndex
            val center = info.viewportStartOffset +
                    (info.viewportEndOffset - info.viewportStartOffset) / 2f
            var closest = currentWindowIndex
            var minDist = Float.MAX_VALUE
            for (item in items) {
                val d = abs(item.offset + item.size / 2f - center)
                if (d < minDist) { minDist = d; closest = item.index }
            }
            closest
        }
    }

    var userScrolling by remember { mutableStateOf(false) }
    val isDragging by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragging) { if (isDragging) userScrolling = true }
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && userScrolling) {
            userScrolling = false
            val snapped = centeredIndex
            if (snapped != currentWindowIndex && snapped in queueWindows.indices) {
                lastScrolledIndex = snapped
                onSongSelected(snapped)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .onGloballyPositioned { parentWidthPx = it.size.width.toFloat() },
        contentAlignment = Alignment.Center
    ) {
        if (parentWidthPx > 0f) {
            val padH = with(density) { (parentWidthPx / 2f - itemWidthPx / 2f).toDp() }

            LazyRow(
                state              = listState,
                flingBehavior      = flingBehavior,
                contentPadding     = PaddingValues(horizontal = padH),
                horizontalArrangement = Arrangement.spacedBy(spacingDp),
                verticalAlignment  = Alignment.CenterVertically,
                modifier           = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = queueWindows,
                    key   = { _, item -> item.uid.hashCode() }
                ) { index, window ->
                    val metadata   = window.mediaItem.metadata!!
                    val isSelected = index == centeredIndex

                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .width(itemWidthDp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFFFFFFFF)
                                else Color.Transparent
                            )
                            .clickable { onSongSelected(index) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text       = String.format("%02d", index + 1),
                                color      = if (isSelected) Color(0xFF999999)
                                else Color(0xFFCCCAC5),
                                fontSize   = if (isSelected) 10.sp else 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontFamily = SpotifyFontFamily
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text       = metadata.title,
                                color      = if (isSelected) Color(0xFF111111)
                                else Color(0xFFAAABA8),
                                fontSize   = if (isSelected) 13.sp else 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                else FontWeight.Normal,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis,
                                fontFamily = SpotifyFontFamily
                            )
                        }
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text     = metadata.artists.joinToString { it.name }.uppercase(),
                            color    = if (isSelected) Color(0xFF888888) else Color(0xFFCCCAC5),
                            fontSize = if (isSelected) 9.sp else 8.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = SpotifyFontFamily,
                            modifier = Modifier.padding(start = 22.dp)
                        )
                    }
                }
            }
        }
    }
}
