package com.darkxvenom.airbeats.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.darkxvenom.airbeats.R
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Downloads a release APK with visible progress, verifies its signer, then opens Android's installer. */
class AppUpdateService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val downloadUrl = intent?.getStringExtra(EXTRA_DOWNLOAD_URL).orEmpty()
        if (intent?.action != ACTION_DOWNLOAD || downloadUrl.isBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (!packageManager.canRequestPackageInstalls()) {
            showPermissionNotification()
            startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    .setData(Uri.parse("package:$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, notification("Downloading update", 0, true))
        thread(name = "app-update-download") {
            runCatching { downloadAndInstall(downloadUrl) }
                .onFailure { showFinishedNotification("Update download failed") }
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun downloadAndInstall(downloadUrl: String) {
        val outputDir = File(cacheDir, "updates").apply { mkdirs() }
        val apk = File(outputDir, "AirBeats-update.apk")
        val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream")
        }
        connection.connect()
        require(connection.responseCode in 200..299) { "Release download failed (${connection.responseCode})" }
        val total = connection.contentLengthLong
        connection.inputStream.use { input ->
            apk.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var downloaded = 0L
                var lastProgress = -1
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    downloaded += count
                    if (total > 0) {
                        val progress = ((downloaded * 100) / total).toInt()
                        if (progress != lastProgress) {
                            lastProgress = progress
                            notificationManager.notify(NOTIFICATION_ID, notification("Downloading update", progress, true))
                        }
                    }
                }
            }
        }
        connection.disconnect()
        require(isSignedLikeInstalledApp(apk)) { "Downloaded APK is not signed by this app's signer" }
        notificationManager.notify(NOTIFICATION_ID, notification("Update ready to install", 100, false))
        openInstaller(apk)
    }

    @Suppress("DEPRECATION")
    private fun isSignedLikeInstalledApp(apk: File): Boolean {
        val installed: Array<android.content.pm.Signature>
        val downloaded: Array<android.content.pm.Signature>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            installed = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners ?: return false
            downloaded = packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
                ?.signingInfo?.apkContentsSigners ?: return false
        } else {
            installed = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
                ?: return false
            downloaded = packageManager.getPackageArchiveInfo(apk.absolutePath, PackageManager.GET_SIGNATURES)
                ?.signatures ?: return false
        }
        return installed.any { current -> downloaded.any { candidate -> current.toByteArray().contentEquals(candidate.toByteArray()) } }
    }

    private fun openInstaller(apk: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", apk)
        startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    private fun notification(title: String, progress: Int, ongoing: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.airbeats_monochrome)
            .setContentTitle(title)
            .setContentText(if (ongoing && progress > 0) "$progress%" else null)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setProgress(100, progress, ongoing)
            .build()

    private fun showPermissionNotification() {
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.airbeats_monochrome)
                .setContentTitle("Allow updates from AirBeats")
                .setContentText("Enable Install unknown apps, then start the update again.")
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun showFinishedNotification(title: String) {
        notificationManager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.airbeats_monochrome)
                .setContentTitle(title)
                .setAutoCancel(true)
                .build(),
        )
    }

    private val notificationManager get() = getSystemService(NotificationManager::class.java)

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 6_204
        private const val ACTION_DOWNLOAD = "com.darkxvenom.airbeats.action.DOWNLOAD_UPDATE"
        private const val EXTRA_DOWNLOAD_URL = "download_url"

        fun start(context: Context, downloadUrl: String) {
            val intent = Intent(context, AppUpdateService::class.java)
                .setAction(ACTION_DOWNLOAD)
                .putExtra(EXTRA_DOWNLOAD_URL, downloadUrl)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
