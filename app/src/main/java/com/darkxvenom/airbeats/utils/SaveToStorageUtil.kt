package com.darkxvenom.airbeats.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AudioQuality
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object SaveToStorageUtil {
    private const val TAG = "SaveToStorageUtil"
    private const val CHANNEL_ID = "airbeats_storage_downloads"
    private const val CHANNEL_NAME = "Storage Downloads"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows download progress when saving songs to device storage"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }

    private fun showProgressNotification(
        context: Context,
        notificationId: Int,
        title: String,
        progress: Int,
        subText: String? = null,
    ) {
        try {
            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.save_to_storage)
                .setContentTitle(title)
                .setContentText(if (progress in 0..100) "$progress%" else "Downloading…")
                .setProgress(100, progress.coerceIn(0, 100), progress < 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            if (!subText.isNullOrEmpty()) {
                builder.setSubText(subText)
            }

            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update download progress notification")
        }
    }

    private fun showCompleteNotification(
        context: Context,
        notificationId: Int,
        title: String,
        success: Boolean,
        message: String,
    ) {
        try {
            createNotificationChannel(context)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(if (success) R.drawable.save_to_storage else R.drawable.close)
                .setContentTitle(if (success) "Downloaded" else "Download failed")
                .setContentText(message)
                .setProgress(0, 0, false)
                .setOngoing(false)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)

            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to show complete notification")
        }
    }

    suspend fun savePlaylistToMusicFolder(
        context: Context,
        playlistName: String,
        mediaList: List<MediaMetadata>,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val subFolder = playlistName.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(50)
            val relativeSubPath = if (subFolder.isNotEmpty()) "AirBeats/$subFolder" else "AirBeats"
            var savedCount = 0
            val total = mediaList.size

            mediaList.forEachIndexed { index, mediaMetadata ->
                try {
                    val subText = "${index + 1}/$total"
                    val notificationId = 20000 + (mediaMetadata.id.hashCode() and 0x7FFF)
                    saveToFolder(
                        context = context,
                        mediaMetadata = mediaMetadata,
                        relativeFolder = relativeSubPath,
                        notificationId = notificationId,
                        subText = subText,
                    ).onSuccess {
                        savedCount++
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error saving song ${mediaMetadata.title} in playlist")
                }
            }
            savedCount
        }
    }

    suspend fun saveToMusicFolder(
        context: Context,
        mediaMetadata: MediaMetadata,
    ): Result<String> = saveToFolder(
        context = context,
        mediaMetadata = mediaMetadata,
        relativeFolder = "AirBeats",
        notificationId = 20000 + (mediaMetadata.id.hashCode() and 0x7FFF),
        subText = null,
    )

    suspend fun saveToFolder(
        context: Context,
        mediaMetadata: MediaMetadata,
        relativeFolder: String,
        notificationId: Int = 20000 + (mediaMetadata.id.hashCode() and 0x7FFF),
        subText: String? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Starting save for: ${mediaMetadata.title} into $relativeFolder")
            showProgressNotification(context, notificationId, mediaMetadata.title, 0, subText)

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

            // 4. Download the stream with live progress updates
            val request = Request.Builder().url(streamUrl).build()
            val response = httpClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    throw Exception("Download failed: HTTP ${resp.code}")
                }
                val body = resp.body ?: throw Exception("Response body is null")
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputBuffer = ByteArrayOutputStream(
                    if (contentLength > 0 && contentLength < Int.MAX_VALUE) contentLength.toInt() else 1024 * 1024
                )

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastUpdateMs = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputBuffer.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    val now = System.currentTimeMillis()
                    if (contentLength > 0 && (now - lastUpdateMs >= 200 || totalBytesRead == contentLength)) {
                        lastUpdateMs = now
                        val percent = ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
                        showProgressNotification(
                            context = context,
                            notificationId = notificationId,
                            title = mediaMetadata.title,
                            progress = percent,
                            subText = subText,
                        )
                    }
                }

                val audioBytes = outputBuffer.toByteArray()
                Timber.tag(TAG).d("Downloaded ${audioBytes.size} bytes for $fileName")

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

            showCompleteNotification(
                context = context,
                notificationId = notificationId,
                title = mediaMetadata.title,
                success = true,
                message = "${mediaMetadata.title} saved to Music/$relativeFolder",
            )
            fileName
        }.onFailure { e ->
            Timber.tag(TAG).e(e, "Failed to save song to local storage")
            showCompleteNotification(
                context = context,
                notificationId = notificationId,
                title = mediaMetadata.title,
                success = false,
                message = "Failed to download: ${e.message}",
            )
        }
    }
}
