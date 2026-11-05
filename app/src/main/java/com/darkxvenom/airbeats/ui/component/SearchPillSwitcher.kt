package com.darkxvenom.airbeats.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.darkxvenom.airbeats.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SearchTab(val titleRes: Int) {
    RECENT_SEARCHES(R.string.recent_searches),
    AIRBEATS_CHARTS(R.string.airbeats_charts),
    BROWSE_ALL(R.string.browse_all)
}

@Composable
fun SearchPillSwitcher(
    selectedTab: SearchTab,
    onTabSelected: (SearchTab) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    selectedTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val tabs = remember { SearchTab.values() }

    LaunchedEffect(selectedTab) {
        val index = tabs.indexOf(selectedTab)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        items(tabs) { tab ->
            val isSelected = tab == selectedTab

            val itemBgColor by animateColorAsState(
                targetValue = if (isSelected) selectedColor else containerColor,
                animationSpec = tween(durationMillis = 200),
                label = "tabBg_${tab.name}"
            )

            val itemTextColor by animateColorAsState(
                targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
                animationSpec = tween(durationMillis = 200),
                label = "tabText_${tab.name}"
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(itemBgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTabSelected(tab)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(tab.titleRes),
                    color = itemTextColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
