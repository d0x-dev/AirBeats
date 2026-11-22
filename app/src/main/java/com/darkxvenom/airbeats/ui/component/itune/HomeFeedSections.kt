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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.darkxvenom.itune.ui.artist.components.ExplicitBadge
import com.darkxvenom.itune.ui.theme.AppleExplicitBadge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.itune.ui.theme.LocalAppFontFamily
import com.darkxvenom.airbeats.data.model.itune.HomeFeedItem
import com.darkxvenom.airbeats.data.model.itune.HomeFeedShelf
import com.darkxvenom.airbeats.data.model.itune.HomeHeroCard
import com.darkxvenom.airbeats.data.model.itune.HomeItemKind
import com.darkxvenom.itune.ui.theme.AppleSurface
import com.darkxvenom.itune.ui.theme.AppleSurfaceElevated
import com.darkxvenom.itune.ui.theme.AppleTextPrimary
import com.darkxvenom.itune.ui.theme.AppleTextSecondary

/** Apple Music style section title: optional small-caps strapline, bold title, chevron. */
@Composable
fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    strapline: String = "",
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        if (strapline.isNotBlank()) {
            Text(
                text = strapline.uppercase(),
                color = AppleTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = AppleTextPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppleTextSecondary,
                    modifier = Modifier.padding(start = 4.dp).size(20.dp),
                )
            }
        }
    }
}

/** Square artwork cards: albums, playlists, mixes, podcasts. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SquareCardShelf(
    shelf: HomeFeedShelf,
    modifier: Modifier = Modifier,
    cardSize: Dp = 150.dp,
    onHeaderClick: (() -> Unit)? = null,
    onItemClick: (HomeFeedItem) -> Unit,
    onItemMenuClick: (HomeFeedItem, Offset) -> Unit = { _, _ -> },
) {
    Column(modifier.fillMaxWidth()) {
        HomeSectionHeader(shelf.title, strapline = shelf.strapline, onClick = onHeaderClick)
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shelf.items, key = { it.stableKey }) { item ->
                var cardCenter by remember { mutableStateOf(Offset.Zero) }
                Column(
                    Modifier
                        .width(cardSize)
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                val pos = coords.positionInWindow()
                                cardCenter = Offset(
                                    x = pos.x + coords.size.width / 2f,
                                    y = pos.y + coords.size.height / 2f,
                                )
                            }
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemMenuClick(item, cardCenter) },
                        ),
                ) {
                    Box(Modifier.size(cardSize)) {
                        Artwork(item.thumbnailUrl, Modifier.fillMaxSize(), RoundedCornerShape(10.dp))
                        // Subtle 3-dots button overlay at top right of artwork
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .clickable { onItemMenuClick(item, cardCenter) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isExplicit) {
                            ExplicitBadge(color = AppleExplicitBadge)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            item.title,
                            color = AppleTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            item.subtitle,
                            color = AppleTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

/** Circular artist portraits with centered names. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistCircleShelf(
    shelf: HomeFeedShelf,
    modifier: Modifier = Modifier,
    diameter: Dp = 124.dp,
    onHeaderClick: (() -> Unit)? = null,
    onItemClick: (HomeFeedItem) -> Unit,
    onItemMenuClick: (HomeFeedItem, Offset) -> Unit = { _, _ -> },
) {
    Column(modifier.fillMaxWidth()) {
        HomeSectionHeader(shelf.title, strapline = shelf.strapline, onClick = onHeaderClick)
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(shelf.items, key = { it.stableKey }) { item ->
                var itemCenter by remember { mutableStateOf(Offset.Zero) }
                Column(
                    Modifier
                        .width(diameter)
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                val pos = coords.positionInWindow()
                                itemCenter = Offset(
                                    x = pos.x + coords.size.width / 2f,
                                    y = pos.y + coords.size.height / 2f,
                                )
                            }
                        }
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemMenuClick(item, itemCenter) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Artwork(item.thumbnailUrl, Modifier.size(diameter), CircleShape)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        item.title,
                        color = AppleTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            item.subtitle,
                            color = AppleTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** 16:9 cards: music videos and podcast episodes. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WideCardShelf(
    shelf: HomeFeedShelf,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 250.dp,
    onHeaderClick: (() -> Unit)? = null,
    onItemClick: (HomeFeedItem) -> Unit,
    onItemMenuClick: (HomeFeedItem, Offset) -> Unit = { _, _ -> },
) {
    Column(modifier.fillMaxWidth()) {
        HomeSectionHeader(shelf.title, strapline = shelf.strapline, onClick = onHeaderClick)
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shelf.items, key = { it.stableKey }) { item ->
                var itemCenter by remember { mutableStateOf(Offset.Zero) }
                Column(
                    Modifier
                        .width(cardWidth)
                        .onGloballyPositioned { coords ->
                            if (coords.isAttached) {
                                val pos = coords.positionInWindow()
                                itemCenter = Offset(
                                    x = pos.x + coords.size.width / 2f,
                                    y = pos.y + coords.size.height / 2f,
                                )
                            }
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemMenuClick(item, itemCenter) },
                        ),
                ) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        Artwork(item.thumbnailUrl, Modifier.fillMaxSize(), RoundedCornerShape(10.dp))
                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        // 3-dots button overlay at top right of video card
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                                .clickable { onItemMenuClick(item, itemCenter) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.isExplicit) {
                            ExplicitBadge(color = AppleExplicitBadge)
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            item.title,
                            color = AppleTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            item.subtitle,
                            color = AppleTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Grey placeholder shelf shown while the first feed request is in flight. */
@Composable
fun HomeShelfSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier.padding(horizontal = 20.dp, vertical = 8.dp).width(150.dp).height(20.dp)
                .clip(RoundedCornerShape(6.dp)).background(AppleSurfaceElevated),
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            userScrollEnabled = false,
        ) {
            items(4) {
                Column(Modifier.width(150.dp)) {
                    Box(Modifier.size(150.dp).clip(RoundedCornerShape(10.dp)).background(AppleSurface))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(110.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(AppleSurface))
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.width(70.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(AppleSurface))
                }
            }
        }
    }
}

/**
 * Top carousel — same layout as [FeaturedHeroCarousel] (category / title / subtitle above a
 * 320×210 banner with a bottom caption) but fed with real YouTube Music data and fully tappable,
 * supporting 3-dot options and long press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LiveFeaturedHeroCarousel(
    cards: List<HomeHeroCard>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isPortrait: Boolean = false,
    isApple: Boolean = false,
    sectionTitle: String = "Top Picks for You",
    onCardClick: (HomeHeroCard) -> Unit,
    onCardMenuClick: (HomeHeroCard, Offset) -> Unit = { _, _ -> },
) {
    if (isPortrait || isApple) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = sectionTitle,
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                ),
                color = AppleTextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            Spacer(Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = cards.isNotEmpty(),
            ) {
                if (cards.isEmpty() && isLoading) {
                    items(2) { HeroPortraitSkeletonCard() }
                } else {
                    items(cards, key = { it.id }) { card ->
                        if (isApple) {
                            AppleHeroCardItem(
                                card = card,
                                onClick = { onCardClick(card) },
                                onMenuClick = { offset -> onCardMenuClick(card, offset) },
                            )
                        } else {
                            PortraitHeroCardItem(
                                card = card,
                                onClick = { onCardClick(card) },
                                onMenuClick = { offset -> onCardMenuClick(card, offset) },
                            )
                        }
                    }
                }
            }
        }
    } else {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = cards.isNotEmpty(),
        ) {
            if (cards.isEmpty() && isLoading) {
                items(2) { HeroSkeletonCard() }
            } else {
                items(cards, key = { it.id }) { card ->
                    var cardCenter by remember { mutableStateOf(Offset.Zero) }
                    Column(
                        Modifier
                            .width(320.dp)
                            .onGloballyPositioned { coords ->
                                if (coords.isAttached) {
                                    val pos = coords.positionInWindow()
                                    cardCenter = Offset(
                                        x = pos.x + coords.size.width / 2f,
                                        y = pos.y + coords.size.height / 2f,
                                    )
                                }
                            }
                            .clip(RoundedCornerShape(18.dp))
                            .combinedClickable(
                                onClick = { onCardClick(card) },
                                onLongClick = { onCardMenuClick(card, cardCenter) },
                            ),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                card.category.uppercase(),
                                color = AppleTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                maxLines = 1,
                            )
                            IconButton(
                                onClick = { onCardMenuClick(card, cardCenter) },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Options",
                                    tint = AppleTextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            card.title,
                            color = AppleTextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text(
                            card.subtitle,
                            color = AppleTextSecondary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier
                                .width(320.dp)
                                .height(210.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp)),
                        ) {
                            if (!card.imageUrl.isNullOrBlank()) {
                                Artwork(card.imageUrl, Modifier.fillMaxSize(), RoundedCornerShape(18.dp))
                            } else if (card.heroType != null) {
                                AlbumArtwork(type = card.heroType, modifier = Modifier.fillMaxSize(), cornerRadius = 18.dp)
                            } else {
                                AppleMusicAmbientArtwork(title = card.title, category = card.category)
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomStart)
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000))))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Text(
                                    card.caption,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 17.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PortraitHeroCardItem(
    card: HomeHeroCard,
    onClick: () -> Unit,
    onMenuClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cardCenter by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .width(235.dp)
            .height(325.dp)
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    val pos = coords.positionInWindow()
                    cardCenter = Offset(
                        x = pos.x + coords.size.width / 2f,
                        y = pos.y + coords.size.height / 2f,
                    )
                }
            }
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onMenuClick(cardCenter) },
            ),
    ) {
        // Base Artwork Layer
        if (!card.imageUrl.isNullOrBlank()) {
            Artwork(card.imageUrl, Modifier.fillMaxSize(), RoundedCornerShape(26.dp))
        } else if (card.heroType != null) {
            AlbumArtwork(type = card.heroType, modifier = Modifier.fillMaxSize(), cornerRadius = 26.dp)
        } else {
            AppleMusicAmbientArtwork(title = card.title, category = card.category)
        }

        // Top Subtle Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )

        // Top-right iTune badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_itune_waves),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "iTune",
                style = TextStyle(
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = (-0.3).sp
                ),
                color = Color.White.copy(alpha = 0.95f)
            )
        }

        // Bottom Morph Blur Container with smooth liquid glass gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.68f),
                            Color.Black.copy(alpha = 0.92f),
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val catText = card.category.ifBlank { "Mood for You" }
                Text(
                    text = catText,
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = card.title,
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        lineHeight = 23.sp,
                        letterSpacing = (-0.3).sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                val desc = card.caption.ifBlank { card.subtitle }
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.5.sp,
                            lineHeight = 16.5.sp
                        ),
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Apple style top card: full artwork, tag outside above card, logo + title on top, details on frosted bottom.
private val AppleHeroCornerRadius = 8.dp
private val AppleHeroShape = RoundedCornerShape(AppleHeroCornerRadius)
private val AppleHeroWidth = 250.dp
private val AppleHeroHeight = 320.dp

@Composable
private fun HeroCardArtwork(card: HomeHeroCard, modifier: Modifier, cornerRadius: androidx.compose.ui.unit.Dp) {
    if (!card.imageUrl.isNullOrBlank()) {
        Artwork(card.imageUrl, modifier, RoundedCornerShape(cornerRadius))
    } else if (card.heroType != null) {
        AlbumArtwork(type = card.heroType, modifier = modifier, cornerRadius = cornerRadius)
    } else {
        AppleMusicAmbientArtwork(title = card.title, category = card.category, modifier = modifier)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppleHeroCardItem(
    card: HomeHeroCard,
    onClick: () -> Unit,
    onMenuClick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cardCenter by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier.width(AppleHeroWidth),
    ) {
        // Tag outside the card above (e.g. "Made for You", "Listen Again", "Trending now")
        val categoryTag = card.category.ifBlank { "Made for You" }
        Text(
            text = categoryTag,
            style = TextStyle(
                fontFamily = LocalAppFontFamily.current,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                letterSpacing = (-0.1).sp,
            ),
            color = AppleTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        )

        Box(
            modifier = Modifier
                .width(AppleHeroWidth)
                .height(AppleHeroHeight)
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        val pos = coords.positionInWindow()
                        cardCenter = Offset(
                            x = pos.x + coords.size.width / 2f,
                            y = pos.y + coords.size.height / 2f,
                        )
                    }
                }
                .clip(AppleHeroShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { onMenuClick(cardCenter) },
                ),
        ) {
            HeroCardArtwork(card, Modifier.fillMaxSize(), AppleHeroCornerRadius)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.50f), Color.Transparent))),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_itune_waves),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "iTune",
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = card.title,
                    style = TextStyle(
                        fontFamily = LocalAppFontFamily.current,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.4).sp,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppleHeroHeight * 0.22f)
                    .align(Alignment.BottomCenter)
                    .clipToBounds(),
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(AppleHeroWidth, AppleHeroHeight)
                        .align(Alignment.BottomCenter)
                        .blur(28.dp),
                ) {
                    HeroCardArtwork(card, Modifier.fillMaxSize(), 0.dp)
                }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))
                val details = card.caption.ifBlank { card.subtitle }
                if (details.isNotBlank()) {
                    Text(
                        text = details,
                        style = TextStyle(
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleMusicAmbientArtwork(
    title: String,
    category: String,
    modifier: Modifier = Modifier,
) {
    val bgGradient = remember(title, category) {
        when {
            title.contains("Focus", ignoreCase = true) || category.contains("Focus", ignoreCase = true) ->
                listOf(Color(0xFFF07E26), Color(0xFFDD5F1B))
            title.contains("Heavy", ignoreCase = true) || title.contains("Rotation", ignoreCase = true) ->
                listOf(Color(0xFFF9A825), Color(0xFFE65100))
            title.contains("Chill", ignoreCase = true) || title.contains("Relax", ignoreCase = true) ->
                listOf(Color(0xFF1E88E5), Color(0xFF004D40))
            else ->
                listOf(Color(0xFFE53935), Color(0xFF8E24AA))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.radialGradient(bgGradient))
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.48f, size.height * 0.44f)
            val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            val ringColor = Color.White.copy(alpha = 0.28f)
            val radii = listOf(size.width * 0.12f, size.width * 0.24f, size.width * 0.36f, size.width * 0.48f)
            radii.forEach { r ->
                drawCircle(color = ringColor, radius = r, center = center, style = stroke)
            }
            drawCircle(color = Color.White.copy(alpha = 0.4f), radius = 6.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun HeroPortraitSkeletonCard() {
    Column(Modifier.width(AppleHeroWidth)) {
        Box(Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(AppleSurfaceElevated))
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(AppleHeroWidth)
                .height(AppleHeroHeight)
                .clip(AppleHeroShape)
                .background(AppleSurface)
        )
    }
}

@Composable
private fun HeroSkeletonCard() {
    Column(Modifier.width(320.dp)) {
        Box(Modifier.width(80.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(AppleSurfaceElevated))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(190.dp).height(22.dp).clip(RoundedCornerShape(5.dp)).background(AppleSurfaceElevated))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(120.dp).height(15.dp).clip(RoundedCornerShape(4.dp)).background(AppleSurface))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(320.dp).height(210.dp).clip(RoundedCornerShape(18.dp)).background(AppleSurface))
    }
}

@Composable
private fun Artwork(url: String?, modifier: Modifier, shape: androidx.compose.ui.graphics.Shape) {
    Box(modifier.clip(shape).background(AppleSurface)) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Maps a feed item kind to a short label used for accessibility / fallbacks. */
fun HomeItemKind.label(): String = when (this) {
    HomeItemKind.SONG -> "Song"
    HomeItemKind.VIDEO -> "Video"
    HomeItemKind.ALBUM -> "Album"
    HomeItemKind.ARTIST -> "Artist"
    HomeItemKind.PLAYLIST -> "Playlist"
    HomeItemKind.PODCAST -> "Podcast"
    HomeItemKind.EPISODE -> "Episode"
}
