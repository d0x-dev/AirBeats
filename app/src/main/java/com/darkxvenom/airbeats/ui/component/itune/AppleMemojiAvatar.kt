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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.ui.component.AvatarDisplay































/**
 * Renders the user profile avatar with Apple iOS styling:
 * Automatically synchronizes with AvatarManager's persistent selection and falls back to
 * the signature Apple Memoji canvas if network is unavailable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleMemojiAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    avatarId: Int? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF141416))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x66FFFFFF), Color(0x22FFFFFF))
                ),
                shape = CircleShape
            )
            .then(
                when {
                    onLongClick != null && onClick != null -> Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                    onLongClick != null -> Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick,
                    )
                    onClick != null -> Modifier.clickable(onClick = onClick)
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        AvatarDisplay(
            size = size,
            showBorder = false,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "User Profile Avatar",
        )
    }
}
