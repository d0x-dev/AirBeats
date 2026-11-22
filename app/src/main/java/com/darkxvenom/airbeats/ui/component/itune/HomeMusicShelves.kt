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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.darkxvenom.itune.backend.MusicArtist
import com.darkxvenom.itune.backend.MusicPlaylistRef
import com.darkxvenom.itune.backend.MusicTrack
import com.darkxvenom.itune.backend.NewReleaseAlbum
import com.darkxvenom.itune.backend.highQualityThumbnail
import com.darkxvenom.itune.ui.theme.AppleTextPrimary
import com.darkxvenom.itune.ui.theme.AppleTextSecondary

@Composable
fun HomeTrackShelf(
    title: String,
    tracks: List<MusicTrack>,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onTrackClick: (MusicTrack) -> Unit,
) {
    if (tracks.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(title, color = AppleTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = AppleTextSecondary, fontSize = 12.sp, maxLines = 1)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            items(tracks, key = { "home_track_${it.id}" }) { track ->
                Column(Modifier.width(146.dp).clickable { onTrackClick(track) }) {
                    AsyncImage(
                        model = track.thumbnailUrl?.highQualityThumbnail() ?: track.thumbnailUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(146.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202024)),
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(track.title, color = AppleTextPrimary, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artists, color = AppleTextSecondary, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
// Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

@Composable
fun HomeCollectionShelf(
    title: String,
    items: List<MusicPlaylistRef>,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    onItemClick: (MusicPlaylistRef) -> Unit,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(title, color = AppleTextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = AppleTextSecondary, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            items(items, key = { "home_collection_${it.id}" }) { item ->
                Column(Modifier.width(150.dp).clickable { onItemClick(item) }) {
                    AsyncImage(
                        model = item.thumbnailUrl?.highQualityThumbnail() ?: item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202024)),
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(item.title, color = AppleTextPrimary, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.author.ifBlank { if (item.isAlbum) "Album" else "YouTube Music" },
                        color = AppleTextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun HomeArtistShelf(
    title: String,
    artists: List<MusicArtist>,
    modifier: Modifier = Modifier,
    onArtistClick: (MusicArtist) -> Unit,
) {
    if (artists.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, modifier = Modifier.padding(horizontal = 20.dp), color = AppleTextPrimary,
            fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(artists, key = { "home_artist_${it.id}" }) { artist ->
                Column(Modifier.width(116.dp).clickable { onArtistClick(artist) }) {
                    AsyncImage(
                        model = artist.thumbnailUrl?.highQualityThumbnail() ?: artist.thumbnailUrl,
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(116.dp).clip(RoundedCornerShape(58.dp))
                            .background(Color(0xFF202024)),
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(artist.name, color = AppleTextPrimary, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Artist", color = AppleTextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HomeNewReleaseShelf(
    title: String,
    albums: List<NewReleaseAlbum>,
    modifier: Modifier = Modifier,
    onAlbumClick: (NewReleaseAlbum) -> Unit,
) {
    if (albums.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, modifier = Modifier.padding(horizontal = 20.dp), color = AppleTextPrimary,
            fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            items(albums, key = { "home_release_${it.browseId}" }) { album ->
                Column(Modifier.width(150.dp).clickable { onAlbumClick(album) }) {
                    AsyncImage(
                        model = album.thumbnailUrl?.highQualityThumbnail() ?: album.thumbnailUrl,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202024)),
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(album.title, color = AppleTextPrimary, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(album.subtitleLine, color = AppleTextSecondary, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
