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

import com.darkxvenom.itune.ui.artist.components.backdrop.Backdrop
import com.darkxvenom.itune.ui.artist.components.liquidGlass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.darkxvenom.itune.backend.MusicTrack
import com.darkxvenom.itune.backend.highQualityThumbnail
import com.darkxvenom.itune.data.model.Track
import com.darkxvenom.itune.ui.artist.components.ExplicitBadge
import com.darkxvenom.itune.ui.theme.AppleGlassBorder
import com.darkxvenom.itune.ui.theme.AppleGlassMiniPlayer
import com.darkxvenom.itune.ui.theme.AppleTextPrimary
import com.darkxvenom.itune.ui.theme.AppleTextSecondary

@Composable
fun FloatingMiniPlayer(
    currentTrack: Track?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onClick: () -> Unit,
    realTrack: MusicTrack? = null,
    isLoading: Boolean = false,
    showEmptyPlaceholder: Boolean = false,
    liquidGlassBackdrop: Backdrop? = null,
    modifier: Modifier = Modifier
) {
    val hasActiveTrack = realTrack != null || currentTrack != null
    // Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(54.dp)
            .then(
                if (liquidGlassBackdrop != null) {
                    // Liquid glass: same effect the artist screen's popup uses.
                    Modifier.liquidGlass(CircleShape, backdropOverride = liquidGlassBackdrop)
                } else {
                    Modifier
                        .shadow(
                            elevation = 14.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                        .clip(CircleShape)
                        .background(AppleGlassMiniPlayer)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(0.75.dp, AppleGlassBorder, CircleShape)
                }
            )
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(start = 10.dp, end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Music Note icon or Empty Placeholder Slot
            if (showEmptyPlaceholder) {
                // Empty space at avatar in miniplayer until the morphing avatar arrives
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                )
            } else if (realTrack != null && !realTrack.thumbnailUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2C2C2E))
                ) {
                    val context = LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(realTrack.thumbnailUrl.highQualityThumbnail())
                            .size(Size.ORIGINAL)
                            .crossfade(200)
                            .build(),
                        contentDescription = realTrack.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (currentTrack != null) {
                AlbumArtwork(
                    type = currentTrack.coverType,
                    size = 36.dp,
                    cornerRadius = 10.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No song playing",
                        tint = AppleTextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (!showEmptyPlaceholder) {
                // Track info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val isExplicit = realTrack?.isExplicit == true || currentTrack?.isExplicit == true
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isExplicit) {
                            ExplicitBadge(
                                color = AppleTextSecondary.copy(alpha = 0.90f),
                                modifier = Modifier.padding(end = 5.dp)
                            )
                        }
                        Text(
                            text = realTrack?.title ?: currentTrack?.title ?: "Not Playing",
                            color = AppleTextPrimary,
                            fontSize = if (hasActiveTrack) 14.sp else 15.sp,
                            fontWeight = if (hasActiveTrack) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (hasActiveTrack) {
                        Text(
                            text = realTrack?.artists ?: currentTrack?.artist.orEmpty(),
                            color = AppleTextSecondary,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = AppleTextPrimary,
                            strokeWidth = 2.2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "PlayPause"
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = AppleTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                IconButton(
                    onClick = onNextClick,
                    enabled = hasActiveTrack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Next Track",
                        tint = if (hasActiveTrack) AppleTextPrimary else Color.White.copy(alpha = 0.34f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(78.dp))
            }
        }
    }
}
