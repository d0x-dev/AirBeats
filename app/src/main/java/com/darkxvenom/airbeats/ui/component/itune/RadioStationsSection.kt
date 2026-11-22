/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of iTune and is licensed under the GNU General Public License v3.0 (GPL-3.0).
 * Any copying, modification, or redistribution of this code must strictly comply with the
 * terms of the project LICENSE file and retain full author credit.
 */

package com.darkxvenom.airbeats.ui.component.itune

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.darkxvenom.itune.radio.OnlineRadioStation
import com.darkxvenom.itune.ui.artist.components.ShimmerBox
import com.darkxvenom.itune.ui.theme.AppleMusicRed
import com.darkxvenom.itune.ui.theme.AppleTextPrimary
import com.darkxvenom.itune.ui.theme.AppleTextSecondary

private val HOME_RADIO_GRADIENTS: List<List<Color>> = listOf(
    listOf(Color(0xFFFA2D48), Color(0xFF990D24)), // Apple Crimson
    listOf(Color(0xFFFF9F0A), Color(0xFFA35F00)), // Warm Amber
    listOf(Color(0xFF30D158), Color(0xFF15752E)), // Vibrant Emerald
    listOf(Color(0xFF0A84FF), Color(0xFF0040A3)), // Deep Cobalt
    listOf(Color(0xFFBF5AF2), Color(0xFF5E1B8E)), // Royal Violet
    listOf(Color(0xFF32ADE6), Color(0xFF0C5F87)), // Sky Teal
    listOf(Color(0xFFFF375F), Color(0xFF8E1130)), // Coral Rose
    listOf(Color(0xFF5E5CE6), Color(0xFF2C2A8F)), // Rich Indigo
    listOf(Color(0xFFFF6B00), Color(0xFF943400)), // Sunset Copper
    listOf(Color(0xFF00C7BE), Color(0xFF0A635F)), // Aqua Mint
)

@Composable
fun RadioStationsSection(
    stations: List<OnlineRadioStation>,
    favoriteIds: Set<String>,
    currentStationUuid: String?,
    liveMetadataTitle: String?,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onHeaderClick: () -> Unit = {},
    onStationClick: (OnlineRadioStation) -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Live Radio Stations",
                color = AppleTextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppleTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        when {
            isLoading && stations.isEmpty() -> {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(3) {
                        Column(modifier = Modifier.width(220.dp)) {
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ShimmerBox(
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                            )
                        }
                    }
                }
            }

            stations.isEmpty() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1C1C1E))
                        .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onHeaderClick)
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Browse Live Online Radio",
                            color = AppleTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to select Country & City FM broadcasts",
                            color = AppleTextSecondary,
                            fontSize = 12.5.sp,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AppleTextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            else -> {
                val rowListState = rememberLazyListState()
                val firstUuid = stations.firstOrNull()?.uuid
                LaunchedEffect(firstUuid) {
                    if (rowListState.firstVisibleItemIndex > 0 || rowListState.firstVisibleItemScrollOffset > 0) {
                        rowListState.scrollToItem(0)
                    }
                }
                LazyRow(
                    state = rowListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(
                        items = stations,
                        key = { _, st -> "home_radio_${st.uuid}" },
                        contentType = { _, _ -> "home_radio_card" },
                    ) { index, station ->
                        val gradient = HOME_RADIO_GRADIENTS[index % HOME_RADIO_GRADIENTS.size]
                        val isCurrent = station.uuid == currentStationUuid
                        val showSubtitle = station.liveShowOrTagsLabel(
                            liveMetadataTitle = if (isCurrent) liveMetadataTitle else null,
                        )
                        OnlineHomeRadioStationCard(
                            station = station,
                            gradient = gradient,
                            showSubtitle = showSubtitle,
                            isFavorite = favoriteIds.contains(station.uuid),
                            onClick = { onStationClick(station) },
                        )
                    }
                }
            }
        }
    }
}
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

@Composable
private fun OnlineHomeRadioStationCard(
    station: OnlineRadioStation,
    gradient: List<Color>,
    showSubtitle: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.horizontalGradient(colors = gradient))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                .padding(14.dp),
        ) {
            if (isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Favorite Radio",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            // LIVE badge pill
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Live",
                    tint = AppleMusicRed,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(end = 6.dp),
            ) {
                Text(
                    text = station.name,
                    color = Color.White,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = showSubtitle,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Hosted by ${station.realHostedByLabel()}",
            color = AppleTextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
