package com.darkxvenom.airbeats.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.spotify.models.SpotifyHomeFeedItem

@Composable
fun SpotifyGridItem(
    item: SpotifyHomeFeedItem,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = false
) {
    val title = when (item) {
        is SpotifyHomeFeedItem.Playlist -> item.name
        is SpotifyHomeFeedItem.Album -> item.name
        is SpotifyHomeFeedItem.Artist -> item.name
    }
    val subtitle = when (item) {
        is SpotifyHomeFeedItem.Playlist -> item.description ?: "Playlist"
        is SpotifyHomeFeedItem.Album -> item.artists.joinToString { it.name }
        is SpotifyHomeFeedItem.Artist -> "Artist"
    }
    val imageUrl = when (item) {
        is SpotifyHomeFeedItem.Playlist -> item.imageUrl
        is SpotifyHomeFeedItem.Album -> item.imageUrl
        is SpotifyHomeFeedItem.Artist -> item.imageUrl
    }
    
    val isArtist = item is SpotifyHomeFeedItem.Artist

    Column(
        modifier = modifier
            .padding(8.dp)
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.width(120.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(if (isArtist) RoundedCornerShape(50) else RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
