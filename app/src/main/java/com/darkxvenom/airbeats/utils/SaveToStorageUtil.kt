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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AudioQuality
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.PlayerConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    // Global application coroutine scope that survives Compose lifecycle / menu dismissals
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private fun detectExtension(bytes: ByteArray): String {
        if (bytes.size >= 8) {
            // Check for ftyp (mp4/m4a)
            if (bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() && bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()) {
                return "m4a"
            }
            // Check for OggS (opus/ogg)
            if (bytes[0] == 'O'.code.toByte() && bytes[1] == 'g'.code.toByte() && bytes[2] == 'g'.code.toByte() && bytes[3] == 'S'.code.toByte()) {
                return "opus"
            }
            // Check for Matroska / WebM
            if (bytes[0] == 0x1A.toByte() && bytes[1] == 0x45.toByte() && bytes[2] == 0xDF.toByte() && bytes[3] == 0xA3.toByte()) {
                return "opus"
            }
            // Check for ID3 / MP3
            if (bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
                return "mp3"
            }
            if ((bytes[0].toInt() and 0xFF) == 0xFF && ((bytes[1].toInt() and 0xE0) == 0xE0)) {
                return "mp3"
            }
        }
        return "m4a"
    }

    private fun getCachedAudioBytes(context: Context, mediaId: String): Pair<ByteArray, String>? {
        try {
            val downloadCache = PlayerConnection.instance?.service?.downloadCache
                ?: MusicService.instance?.downloadCache
            val playerCache = PlayerConnection.instance?.service?.playerCache
                ?: MusicService.instance?.playerCache

            // 1. Try downloadCache first (fully downloaded songs)
            if (downloadCache != null) {
                val spans = downloadCache.getCachedSpans(mediaId).sortedBy { it.position }
                if (spans.isNotEmpty()) {
                    val out = ByteArrayOutputStream()
                    for (span in spans) {
                        span.file?.inputStream()?.use { it.copyTo(out) }
                    }
                    val bytes = out.toByteArray()
                    if (bytes.isNotEmpty()) {
                        val ext = detectExtension(bytes)
                        Timber.tag(TAG).d("Found ${bytes.size} cached bytes in downloadCache for $mediaId ($ext)")
                        return bytes to ext
                    }
                }
            }

            // 2. Try playerCache (stream cache)
            if (playerCache != null) {
                val spans = playerCache.getCachedSpans(mediaId).sortedBy { it.position }
                if (spans.isNotEmpty()) {
                    val out = ByteArrayOutputStream()
                    for (span in spans) {
                        span.file?.inputStream()?.use { it.copyTo(out) }
                    }
                    val bytes = out.toByteArray()
                    if (bytes.isNotEmpty()) {
                        val ext = detectExtension(bytes)
                        Timber.tag(TAG).d("Found ${bytes.size} cached bytes in playerCache for $mediaId ($ext)")
                        return bytes to ext
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reading cached audio bytes for $mediaId")
        }
        return null
    }

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

    fun saveToMusicFolderAsync(
        context: Context,
        mediaMetadata: MediaMetadata,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Throwable) -> Unit)? = null,
    ) {
        val appContext = context.applicationContext
        applicationScope.launch {
            saveToFolder(
                context = appContext,
                mediaMetadata = mediaMetadata,
                relativeFolder = "AirBeats",
                notificationId = 20000 + (mediaMetadata.id.hashCode() and 0x7FFF),
            ).onSuccess {
                withContext(Dispatchers.Main) {
                    onSuccess?.invoke() ?: Toast.makeText(
                        appContext,
                        appContext.getString(R.string.song_saved_successfully),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.onFailure { e ->
                if (e !is CancellationException) {
                    withContext(Dispatchers.Main) {
                        onFailure?.invoke(e) ?: Toast.makeText(
                            appContext,
                            "${appContext.getString(R.string.song_save_failed)}: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun savePlaylistToMusicFolderAsync(
        context: Context,
        playlistName: String,
        mediaList: List<MediaMetadata>,
        onSuccess: ((Int) -> Unit)? = null,
        onFailure: ((Throwable) -> Unit)? = null,
    ) {
        val appContext = context.applicationContext
        applicationScope.launch {
            savePlaylistToMusicFolder(
                context = appContext,
                playlistName = playlistName,
                mediaList = mediaList,
            ).onSuccess { count ->
                withContext(Dispatchers.Main) {
                    onSuccess?.invoke(count) ?: Toast.makeText(
                        appContext,
                        "Saved $count songs to Music/AirBeats/$playlistName",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }.onFailure { e ->
                if (e !is CancellationException) {
                    withContext(Dispatchers.Main) {
                        onFailure?.invoke(e) ?: Toast.makeText(
                            appContext,
                            "Save failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    suspend fun savePlaylistToMusicFolder(
        context: Context,
        playlistName: String,
        mediaList: List<MediaMetadata>,
    ): Result<Int> = withContext(Dispatchers.IO + NonCancellable) {
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
                        context = context.applicationContext,
                        mediaMetadata = mediaMetadata,
                        relativeFolder = relativeSubPath,
                        notificationId = notificationId,
                        subText = subText,
                    ).onSuccess {
                        savedCount++
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Timber.tag(TAG).e(e, "Error saving song ${mediaMetadata.title} in playlist")
                    }
                }
            }
            savedCount
        }
    }

    suspend fun saveToMusicFolder(
        context: Context,
        mediaMetadata: MediaMetadata,
    ): Result<String> = saveToFolder(
        context = context.applicationContext,
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
    ): Result<String> = withContext(Dispatchers.IO + NonCancellable) {
        runCatching {
            val appContext = context.applicationContext
            Timber.tag(TAG).d("Starting save for: ${mediaMetadata.title} into $relativeFolder")
            showProgressNotification(appContext, notificationId, mediaMetadata.title, 0, subText)

            var audioBytes: ByteArray? = null
            var extension: String = "m4a"

            // 1. Check if song is already cached locally (for 100% offline export)
            val cachedData = getCachedAudioBytes(appContext, mediaMetadata.id)
            if (cachedData != null) {
                Timber.tag(TAG).d("Extracting song from local cache (offline mode) for: ${mediaMetadata.title}")
                audioBytes = cachedData.first
                extension = cachedData.second
                showProgressNotification(appContext, notificationId, mediaMetadata.title, 50, subText)
            } else {
                // 2. Resolve stream URL and download if online
                val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val playbackData = YTPlayerUtils.playerResponseForPlayback(
                    videoId = mediaMetadata.id,
                    playlistId = null,
                    audioQuality = AudioQuality.HIGH,
                    connectivityManager = connectivityManager
                ).getOrThrow()

                val format = playbackData.format
                val streamUrl = playbackData.streamUrl

                Timber.tag(TAG).d("Stream URL resolved, format: ${format.mimeType}, bitrate: ${format.bitrate}")

                extension = when {
                    format.mimeType.contains("opus") || format.mimeType.contains("webm") -> "opus"
                    format.mimeType.contains("mp4") || format.mimeType.contains("m4a") -> "m4a"
                    else -> "m4a"
                }

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
                                context = appContext,
                                notificationId = notificationId,
                                title = mediaMetadata.title,
                                progress = percent,
                                subText = subText,
                            )
                        }
                    }
                    audioBytes = outputBuffer.toByteArray()
                }
            }

            val finalAudioBytes = audioBytes ?: throw Exception("No audio data available")

            // 3. Sanitise file name
            val sanitisedTitle = mediaMetadata.title
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(200)
            val artistName = mediaMetadata.artists.joinToString(", ") { it.name }
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .take(100)
            val fileName = "${sanitisedTitle} - ${artistName}.$extension"
            Timber.tag(TAG).d("Writing ${finalAudioBytes.size} bytes for $fileName")

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

                    val resolver = appContext.contentResolver
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
                        appContext,
                        arrayOf(outputFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )

                    Timber.tag(TAG).d("Saved via direct file write: ${outputFile.absolutePath}")
                }
            }

            showCompleteNotification(
                context = appContext,
                notificationId = notificationId,
                title = mediaMetadata.title,
                success = true,
                message = "${mediaMetadata.title} saved to Music/$relativeFolder",
            )
            fileName
        }.onFailure { e ->
            if (e !is CancellationException) {
                Timber.tag(TAG).e(e, "Failed to save song to local storage")
                showCompleteNotification(
                    context = context.applicationContext,
                    notificationId = notificationId,
                    title = mediaMetadata.title,
                    success = false,
                    message = "Failed to download: ${e.message}",
                )
            }
        }
    }
}
