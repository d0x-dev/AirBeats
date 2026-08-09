package com.darkxvenom.airbeats.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.darkxvenom.airbeats.innertube.NewPipeUtils
import android.net.ConnectivityManager
import com.darkxvenom.airbeats.constants.AudioQuality
import com.darkxvenom.airbeats.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object SaveToStorageUtil {
    private const val TAG = "SaveToStorageUtil"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    suspend fun savePlaylistToMusicFolder(
        context: Context,
        playlistName: String,
        mediaList: List<MediaMetadata>,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val subFolder = playlistName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(50)
            val relativeSubPath = if (subFolder.isNotEmpty()) "AirBeats/$subFolder" else "AirBeats"
            var savedCount = 0

            mediaList.forEach { mediaMetadata ->
                try {
                    saveToFolder(context, mediaMetadata, relativeSubPath).onSuccess {
                        savedCount++
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error saving song ${mediaMetadata.title} in playlist")
                }
            }
            savedCount
        }
    }

    /**
     * Downloads the audio stream for the given media and saves it to the
     * device's Music folder. Uses MediaStore on Android 10+ and direct file
     * write on older versions.
     */
    suspend fun saveToMusicFolder(
        context: Context,
        mediaMetadata: MediaMetadata,
    ): Result<String> = saveToFolder(context, mediaMetadata, "AirBeats")

    suspend fun saveToFolder(
        context: Context,
        mediaMetadata: MediaMetadata,
        relativeFolder: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Starting save for: ${mediaMetadata.title} into $relativeFolder")

            // 1. Resolve stream URL and format using robust playerResponseForPlayback with fallbacks
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val playbackData = YTPlayerUtils.playerResponseForPlayback(
                videoId = mediaMetadata.id,
                playlistId = null,
                audioQuality = AudioQuality.HIGH,
                connectivityManager = connectivityManager
            ).getOrThrow()

            val format = playbackData.format
            val streamUrl = playbackData.streamUrl

            Timber.tag(TAG).d("Stream URL resolved, format: ${format.mimeType}, bitrate: ${format.bitrate}")

            // 2. Determine file extension from mime type
            val extension = when {
                format.mimeType.contains("opus") || format.mimeType.contains("webm") -> "opus"
                format.mimeType.contains("mp4") || format.mimeType.contains("m4a") -> "m4a"
                else -> "m4a"
            }

            // 3. Sanitise file name
            val sanitisedTitle = mediaMetadata.title
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(200)
            val artistName = mediaMetadata.artists.joinToString(", ") { it.name }
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(100)
            val fileName = "${sanitisedTitle} - ${artistName}.$extension"

            // 4. Download the stream
            val request = Request.Builder().url(streamUrl).build()
            val response = httpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw Exception("Download failed: HTTP ${resp.code}")
                }
                val audioBytes = resp.body?.bytes()
                    ?: throw Exception("Response body is null")
                Timber.tag(TAG).d("Downloaded ${audioBytes.size} bytes")

                // 5. Write to Music folder
                val mimeType = when (extension) {
                    "opus" -> "audio/ogg"
                    "m4a" -> "audio/mp4"
                    else -> "audio/mpeg"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ use MediaStore (scoped storage)
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$relativeFolder")
                        put(MediaStore.Audio.Media.TITLE, mediaMetadata.title)
                        put(MediaStore.Audio.Media.ARTIST, artistName)
                        mediaMetadata.album?.title?.let {
                            put(MediaStore.Audio.Media.ALBUM, it)
                        }
                        put(MediaStore.Audio.Media.DURATION, mediaMetadata.duration * 1000L)
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    }

                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: throw Exception("Failed to create MediaStore entry")

                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(audioBytes)
                    } ?: throw Exception("Failed to open output stream")

                    // Mark as complete
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)

                    Timber.tag(TAG).d("Saved via MediaStore: $fileName")
                } else {
                    // Android 9 and below - direct file write
                    val musicDir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                        relativeFolder
                    )
                    if (!musicDir.exists()) musicDir.mkdirs()

                    val outputFile = File(musicDir, fileName)
                    FileOutputStream(outputFile).use { fos ->
                        fos.write(audioBytes)
                    }

                    // Notify media scanner
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(outputFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )

                    Timber.tag(TAG).d("Saved via direct file write: ${outputFile.absolutePath}")
                }
            }

            fileName
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Failed to save song to local storage")
        }
    }
}
