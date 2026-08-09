package com.darkxvenom.airbeats.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.IntentCompat
import androidx.media3.common.MediaItem
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.playback.PlayerConnection
import com.darkxvenom.airbeats.playback.queues.ListQueue
import java.io.File

object ExternalPlayerUtil {
    private const val TAG = "ExternalPlayerUtil"

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "opus", "flac", "wav", "ogg", "oga", "aac", "webm", "wma", "mid", "amr"
    )

    /**
     * Determines whether the given intent represents an external audio file to be played.
     */
    fun isAudioIntent(intent: Intent, context: Context): Boolean {
        val action = intent.action ?: return false
        if (action != Intent.ACTION_VIEW && action != Intent.ACTION_SEND) {
            return false
        }

        val type = intent.type?.lowercase()
        if (type != null && (
                type.startsWith("audio/") ||
                type == "application/ogg" ||
                type == "application/x-ogg" ||
                type == "application/opus" ||
                type == "application/x-flac" ||
                type == "application/flac" ||
                type == "application/itunes"
            )
        ) {
            return true
        }

        val uri = getAudioUri(intent) ?: return false
        val scheme = uri.scheme?.lowercase()

        if (scheme == "file" || scheme == "content") {
            // Check file extension in URI path
            val path = uri.path?.lowercase().orEmpty()
            val ext = path.substringAfterLast('.', "")
            if (ext in AUDIO_EXTENSIONS) {
                return true
            }

            // Check ContentResolver type
            if (scheme == "content") {
                try {
                    val crType = context.contentResolver.getType(uri)?.lowercase()
                    if (crType != null && (
                            crType.startsWith("audio/") ||
                            crType == "application/ogg" ||
                            crType == "application/x-ogg" ||
                            crType == "application/opus" ||
                            crType == "application/x-flac" ||
                            crType == "application/flac"
                        )
                    ) {
                        return true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to inspect content resolver type: ${e.message}")
                }
            }
        }

        return false
    }

    /**
     * Extracts the target audio Uri from Intent (ACTION_VIEW data, ACTION_SEND EXTRA_STREAM, or clipData).
     */
    fun getAudioUri(intent: Intent): Uri? {
        if (intent.action == Intent.ACTION_SEND) {
            val streamUri = try {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            } catch (_: Exception) {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
            }
            if (streamUri != null) return streamUri
        }

        return intent.data ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    /**
     * Extracts metadata from audio Uri and constructs an ExoPlayer MediaItem.
     */
    fun extractMediaItem(context: Context, uri: Uri): MediaItem {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var durationMs: Long = 0L
        var artUri: Uri? = null

        // 1. Try to read filename from ContentResolver
        if (uri.scheme.equals(ContentResolver.SCHEME_CONTENT, ignoreCase = true)) {
            try {
                context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            title = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "ContentResolver query failed: ${e.message}")
            }
        }

        // 2. Use MediaMetadataRetriever to extract tags and embedded album art
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!metaTitle.isNullOrBlank()) {
                title = metaTitle.trim()
            }

            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)

            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (dur != null) {
                durationMs = dur.toLongOrNull() ?: 0L
            }

            // Extract embedded album art
            val pictureBytes = retriever.embeddedPicture
            if (pictureBytes != null && pictureBytes.isNotEmpty()) {
                try {
                    val artCacheDir = File(context.cacheDir, "external_album_art").apply { mkdirs() }
                    val artFile = File(artCacheDir, "art_${uri.hashCode().toUInt()}.jpg")
                    if (!artFile.exists() || artFile.length() == 0L) {
                        artFile.writeBytes(pictureBytes)
                    }
                    artUri = Uri.fromFile(artFile)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to cache embedded album art: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever failed for $uri: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        // Fallback for title
        if (title.isNullOrBlank()) {
            title = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio Track"
        }

        // Strip file extension from title if displayed directly
        val dotIndex = title.lastIndexOf('.')
        if (dotIndex > 0) {
            val possibleExt = title.substring(dotIndex + 1).lowercase()
            if (possibleExt in AUDIO_EXTENSIONS) {
                title = title.substring(0, dotIndex)
            }
        }

        if (artist.isNullOrBlank()) {
            artist = "Unknown Artist"
        }

        val mediaMetadata = MediaMetadata(
            id = uri.toString(),
            title = title,
            artists = listOf(MediaMetadata.Artist(id = null, name = artist)),
            album = MediaMetadata.Album(id = "", title = album.orEmpty()),
            duration = (durationMs / 1000).toInt(),
            thumbnailUrl = artUri?.toString(),
        )

        return MediaItem.Builder()
            .setMediaId(uri.toString())
            .setUri(uri)
            .setTag(mediaMetadata)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artUri)
                    .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()
    }

    /**
     * Plays the external audio URI immediately.
     */
    fun playExternalAudio(playerConnection: PlayerConnection, context: Context, uri: Uri) {
        try {
            val mediaItem = extractMediaItem(context, uri)
            val queue = ListQueue(
                title = mediaItem.mediaMetadata.title?.toString(),
                items = listOf(mediaItem)
            )
            playerConnection.playQueue(queue)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing external audio URI: $uri", e)
        }
    }
}
