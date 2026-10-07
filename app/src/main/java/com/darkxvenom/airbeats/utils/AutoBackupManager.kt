package com.darkxvenom.airbeats.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.darkxvenom.airbeats.BuildConfig
import com.darkxvenom.airbeats.db.InternalDatabase
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.extensions.div
import com.darkxvenom.airbeats.extensions.tryOrNull
import com.darkxvenom.airbeats.extensions.zipInputStream
import com.darkxvenom.airbeats.extensions.zipOutputStream
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import kotlin.system.exitProcess

object AutoBackupManager {

    const val BACKUP_FILENAME = "airbeats_backup.backup"
    private const val PREFS_NAME = "auto_backup_state"
    const val KEY_LAST_BACKUP_TIME = "last_android_os_backup_time"
    private const val KEY_LAST_RESTORED_SIG = "last_restored_sig"
    private const val KEY_RESTART_ATTEMPTS = "restart_attempts"
    private const val KEY_JUST_RESTORED = "just_restored_from_setup"

    fun setJustRestored(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_JUST_RESTORED, value)
            .apply()
    }

    fun consumeJustRestoredFlag(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val justRestored = prefs.getBoolean(KEY_JUST_RESTORED, false)
        if (justRestored) {
            prefs.edit().putBoolean(KEY_JUST_RESTORED, false).apply()
        }
        return justRestored
    }

    const val SETTINGS_FILENAME = "settings.preferences_pb"
    const val USER_NAME_PREFS_FILENAME = "user_name_preferences.preferences_pb"
    const val GOOGLE_ACCOUNT_FILENAME = "google_account.json"
    const val GLOBAL_STATS_FILENAME = "airbeats_global_stats.xml"
    const val STATS_IDENTITY_FILENAME = "stats_identity.json"
    const val PLAYLIST_IMAGES_PREFS_FILENAME = "playlist_images.xml"
    const val PLAYLIST_IMAGES_DIR = "playlist_images"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun getDeviceId(context: Context): String {
        return runCatching {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown_device"
    }

    fun getDeviceCloudFilePath(context: Context): String {
        val deviceId = getDeviceId(context)
        return "airbeats/devices/$deviceId/$BACKUP_FILENAME"
    }

    fun getAutoBackupFile(context: Context): File = File(context.filesDir, BACKUP_FILENAME)

    fun getBackupSize(context: Context): Long {
        val file = getAutoBackupFile(context)
        if (file.exists() && file.length() > 0) {
            return file.length()
        }
        val legacyFile = File(context.filesDir, "os_backup/latest.backup")
        if (legacyFile.exists() && legacyFile.length() > 0) {
            return legacyFile.length()
        }
        var totalBytes = 0L
        val dbFile = context.getDatabasePath(InternalDatabase.DB_NAME)
        if (dbFile.exists()) totalBytes += dbFile.length()
        val datastoreDir = context.filesDir / "datastore"
        if (datastoreDir.exists()) {
            datastoreDir.listFiles()?.forEach { totalBytes += it.length() }
        }
        val playlistImagesDir = context.filesDir / PLAYLIST_IMAGES_DIR
        if (playlistImagesDir.exists()) {
            playlistImagesDir.listFiles()?.forEach { totalBytes += it.length() }
        }
        val parentFile = context.filesDir.parentFile
        if (parentFile != null) {
            val stats = parentFile / "shared_prefs" / GLOBAL_STATS_FILENAME
            if (stats.exists()) totalBytes += stats.length()
        }
        return totalBytes
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun setLastBackupTime(context: Context, time: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_BACKUP_TIME, time)
            .commit()
    }

    fun resetRestartAttempts(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_RESTART_ATTEMPTS, 0)
            .apply()
    }

    fun createBackupZip(context: Context, database: MusicDatabase?, rawStream: OutputStream) {
        // Ensure Room database WAL is fully flushed to song.db
        runCatching {
            val db = database ?: runCatching { com.darkxvenom.airbeats.App.instance.database }.getOrNull()
            db?.checkpoint()
        }.onFailure { e ->
            Timber.w(e, "Database checkpoint failed during backup creation")
        }

        // Synchronously commit relevant SharedPreferences to disk
        runCatching {
            context.getSharedPreferences("airbeats_global_stats", Context.MODE_PRIVATE).edit().commit()
            context.getSharedPreferences("playlist_images", Context.MODE_PRIVATE).edit().commit()
            context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE).edit().commit()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().commit()
        }

        rawStream.buffered().zipOutputStream().use { outputStream ->
            // 1. Comprehensive datastore backup: all preferences (name, settings, avatar, ranks, etc.)
            val datastoreDir = context.filesDir / "datastore"
            if (datastoreDir.exists() && datastoreDir.isDirectory) {
                datastoreDir.listFiles()?.forEach { dsFile ->
                    if (dsFile.isFile) {
                        dsFile.inputStream().buffered().use { inputStream ->
                            outputStream.putNextEntry(ZipEntry("datastore/${dsFile.name}"))
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }

            // Legacy standalone entries for maximum backwards compatibility
            val settingsFile = context.filesDir / "datastore" / SETTINGS_FILENAME
            if (settingsFile.exists()) {
                settingsFile.inputStream().buffered().use { inputStream ->
                    outputStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                    inputStream.copyTo(outputStream)
                }
            }

            val namePrefsFile = context.filesDir / "datastore" / USER_NAME_PREFS_FILENAME
            if (namePrefsFile.exists()) {
                namePrefsFile.inputStream().buffered().use { inputStream ->
                    outputStream.putNextEntry(ZipEntry(USER_NAME_PREFS_FILENAME))
                    inputStream.copyTo(outputStream)
                }
            }

            val accountEmail = runCatching {
                runBlocking { NamePreferenceManager(context).accountEmail.first() }
            }.getOrDefault("")
            if (accountEmail.isNotBlank()) {
                outputStream.putNextEntry(ZipEntry(GOOGLE_ACCOUNT_FILENAME))
                outputStream.write(
                    JSONObject()
                        .put("email", accountEmail)
                        .put("previouslyLoggedIn", true)
                        .toString()
                        .toByteArray()
                )
            }

            val currentUid = context.getSharedPreferences(AirBeatsStatsCloudSync.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(AirBeatsStatsCloudSync.KEY_USER_ID, null)
                ?: runCatching {
                    val f = File(context.filesDir, STATS_IDENTITY_FILENAME)
                    if (f.exists()) JSONObject(f.readText()).optString("userId").takeIf { it.isNotBlank() } else null
                }.getOrNull()
            val currentName = runCatching {
                runBlocking { NamePreferenceManager(context).userName.first() }
            }.getOrDefault("")

            val currentUserNum = AirBeatsStatsCloudSync.getUserNumber(context)

            if (!currentUid.isNullOrBlank() || !currentUserNum.isNullOrBlank()) {
                outputStream.putNextEntry(ZipEntry(STATS_IDENTITY_FILENAME))
                outputStream.write(
                    JSONObject()
                        .put("userId", currentUid ?: "")
                        .put("userNumber", currentUserNum ?: "")
                        .put("name", currentName)
                        .put("email", accountEmail)
                        .toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val parentFile = context.filesDir.parentFile
            if (parentFile != null) {
                val statsPrefsFile = parentFile / "shared_prefs" / GLOBAL_STATS_FILENAME
                if (statsPrefsFile.exists()) {
                    statsPrefsFile.inputStream().buffered().use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(GLOBAL_STATS_FILENAME))
                        inputStream.copyTo(outputStream)
                    }
                } else if (!currentUid.isNullOrBlank() || !currentUserNum.isNullOrBlank()) {
                    val xmlFallback = """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    ${if (!currentUid.isNullOrBlank()) "<string name=\"${AirBeatsStatsCloudSync.KEY_USER_ID}\">$currentUid</string>" else ""}
    ${if (!currentUserNum.isNullOrBlank()) "<string name=\"${AirBeatsStatsCloudSync.KEY_USER_NUMBER}\">$currentUserNum</string>" else ""}
</map>""".trimIndent()
                    outputStream.putNextEntry(ZipEntry(GLOBAL_STATS_FILENAME))
                    outputStream.write(xmlFallback.toByteArray(Charsets.UTF_8))
                }

                val playlistImagesPrefs = parentFile / "shared_prefs" / PLAYLIST_IMAGES_PREFS_FILENAME
                if (playlistImagesPrefs.exists()) {
                    playlistImagesPrefs.inputStream().buffered().use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(PLAYLIST_IMAGES_PREFS_FILENAME))
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            val playlistImagesDir = context.filesDir / PLAYLIST_IMAGES_DIR
            if (playlistImagesDir.exists() && playlistImagesDir.isDirectory) {
                playlistImagesDir.listFiles()?.forEach { imgFile ->
                    if (imgFile.isFile) {
                        imgFile.inputStream().buffered().use { inputStream ->
                            outputStream.putNextEntry(ZipEntry("$PLAYLIST_IMAGES_DIR/${imgFile.name}"))
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }

            val dbFile = context.getDatabasePath(InternalDatabase.DB_NAME)
            if (dbFile.exists()) {
                FileInputStream(dbFile).use { inputStream ->
                    outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                    inputStream.copyTo(outputStream)
                }
            }
            val walFile = context.getDatabasePath("${InternalDatabase.DB_NAME}-wal")
            if (walFile.exists() && walFile.length() > 0) {
                FileInputStream(walFile).use { inputStream ->
                    outputStream.putNextEntry(ZipEntry("${InternalDatabase.DB_NAME}-wal"))
                    inputStream.copyTo(outputStream)
                }
            }
            val shmFile = context.getDatabasePath("${InternalDatabase.DB_NAME}-shm")
            if (shmFile.exists() && shmFile.length() > 0) {
                FileInputStream(shmFile).use { inputStream ->
                    outputStream.putNextEntry(ZipEntry("${InternalDatabase.DB_NAME}-shm"))
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    fun hasBackableData(context: Context, database: MusicDatabase?): Boolean {
        // Room Database check
        val dbFile = context.getDatabasePath(InternalDatabase.DB_NAME)
        val walFile = context.getDatabasePath("${InternalDatabase.DB_NAME}-wal")
        if ((dbFile.exists() && dbFile.length() > 0) || (walFile.exists() && walFile.length() > 0)) return true

        // Datastore check (settings, profile)
        val datastoreDir = context.filesDir / "datastore"
        if (datastoreDir.exists() && datastoreDir.isDirectory) {
            val hasData = datastoreDir.listFiles()?.any { it.isFile && it.length() > 0 } == true
            if (hasData) return true
        }

        // Global stats check
        val parentFile = context.filesDir.parentFile
        if (parentFile != null) {
            val statsPrefs = parentFile / "shared_prefs" / GLOBAL_STATS_FILENAME
            if (statsPrefs.exists() && statsPrefs.length() > 0) return true
        }

        // Stats identity file check
        val idFile = File(context.filesDir, STATS_IDENTITY_FILENAME)
        if (idFile.exists() && idFile.length() > 0) return true

        return false
    }

    const val STORAGE_FOLDER_NAME = "AirBeats"
    const val STORAGE_BACKUP_FILENAME = "airbeats_backup.backup"
    private const val KEY_AUTO_BACKUP_STORAGE = "auto_backup_to_storage"
    private const val KEY_INITIAL_STORAGE_RESTORE_CHECKED = "initial_storage_restore_checked"

    fun getDocumentsBackupDir(): File {
        val docs = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS)
        return File(docs, STORAGE_FOLDER_NAME)
    }

    fun getDocumentsBackupFile(): File = File(getDocumentsBackupDir(), STORAGE_BACKUP_FILENAME)

    fun isAutoBackupToStorageEnabled(context: Context): Boolean {
        return context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
            // A new install must never overwrite a user's surviving Documents
            // backup before they have had a chance to restore it.
            .getBoolean(KEY_AUTO_BACKUP_STORAGE, false)
    }

    fun hasCompletedInitialStorageRestoreCheck(context: Context): Boolean =
        context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
            .getBoolean(KEY_INITIAL_STORAGE_RESTORE_CHECKED, false)

    fun markInitialStorageRestoreCheckComplete(context: Context) {
        context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INITIAL_STORAGE_RESTORE_CHECKED, true)
            .apply()
    }

    fun hasStoragePermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            true
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun setAutoBackupToStorageEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_BACKUP_STORAGE, enabled)
            .apply()
    }

    fun saveBackupToStorage(context: Context, database: MusicDatabase?): Boolean {
        return try {
            if (!hasBackableData(context, database)) {
                Timber.d("AutoBackupManager: No backable data to save to storage")
                return false
            }

            val docsDir = getDocumentsBackupDir()
            if (!docsDir.exists()) docsDir.mkdirs()
            val destFile = getDocumentsBackupFile()
            val tmpFile = File(docsDir, "$STORAGE_BACKUP_FILENAME.tmp")

            FileOutputStream(tmpFile).use { fos ->
                createBackupZip(context, database, fos)
            }

            if (tmpFile.exists() && tmpFile.length() > 0) {
                if (destFile.exists()) destFile.delete()
                tmpFile.renameTo(destFile)

                // Also copy to Downloads/AirBeats/airbeats_backup.backup for extra redundancy
                runCatching {
                    val dlDir = File(
                        android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                        STORAGE_FOLDER_NAME
                    )
                    if (!dlDir.exists()) dlDir.mkdirs()
                    destFile.copyTo(File(dlDir, STORAGE_BACKUP_FILENAME), overwrite = true)
                }

                // Also copy to app external files dir for guaranteed scoped-storage persistence without permissions
                runCatching {
                    context.getExternalFilesDir(null)?.let { extDir ->
                        destFile.copyTo(File(extDir, STORAGE_BACKUP_FILENAME), overwrite = true)
                    }
                }

                Timber.i("AutoBackupManager: Successfully saved backup to Documents/AirBeats (${destFile.length()} bytes)")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.w("AutoBackupManager: Could not save backup to Documents/AirBeats: ${e.message}")
            false
        }
    }

    fun performAutoBackupToStorageIfEnabled(context: Context, database: MusicDatabase?) {
        if (isAutoBackupToStorageEnabled(context)) {
            saveBackupToStorage(context, database)
        }
    }

    fun findStorageBackupFile(context: Context? = null): File? {
        val candidates = mutableListOf(
            getDocumentsBackupFile(),
            File(getDocumentsBackupDir(), "airbeats_auto_backup.backup"),
            File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "$STORAGE_FOLDER_NAME/$STORAGE_BACKUP_FILENAME"
            ),
            File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "$STORAGE_FOLDER_NAME/airbeats_auto_backup.backup"
            )
        )
        context?.getExternalFilesDir(null)?.let { extDir ->
            candidates.add(File(extDir, STORAGE_BACKUP_FILENAME))
            candidates.add(File(extDir, "airbeats_auto_backup.backup"))
        }
        for (f in candidates) {
            if (runCatching { f.exists() && f.length() > 0L }.getOrDefault(false)) return f
        }

        // Search Documents/AirBeats for any .backup file
        runCatching {
            val docsDir = getDocumentsBackupDir()
            if (docsDir.exists() && docsDir.isDirectory) {
                val file = docsDir.listFiles { f -> f.isFile && f.name.endsWith(".backup") && f.length() > 0L }
                    ?.maxByOrNull { it.lastModified() }
                if (file != null) return file
            }
        }

        // Also search Downloads/AirBeats for any .backup file
        runCatching {
            val dlDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                STORAGE_FOLDER_NAME
            )
            if (dlDir.exists() && dlDir.isDirectory) {
                val file = dlDir.listFiles { f -> f.isFile && f.name.endsWith(".backup") && f.length() > 0L }
                    ?.maxByOrNull { it.lastModified() }
                if (file != null) return file
            }
        }

        return null
    }

    fun restoreFromStorageBackup(context: Context, shouldRestart: Boolean = true): Boolean {
        val file = findStorageBackupFile(context) ?: return false
        Timber.i("AutoBackupManager: Restoring from storage backup file at ${file.absolutePath} (${file.length()} bytes)")
        val targetFile = getAutoBackupFile(context)
        runCatching { file.copyTo(targetFile, overwrite = true) }
        markInitialStorageRestoreCheckComplete(context)
        return runCatching {
            FileInputStream(file).use { stream ->
                restoreFromInputStream(context, stream, shouldRestart)
            }
        }.getOrDefault(false)
    }

    fun savePersistentExternalBackup(context: Context, sourceFile: File) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return

        // 1. Try public Downloads/AirBeats/airbeats_auto_backup.backup (survives app uninstall)
        runCatching {
            val downloadsDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                "AirBeats"
            )
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destInDownloads = File(downloadsDir, "airbeats_auto_backup.backup")
            sourceFile.copyTo(destInDownloads, overwrite = true)
            Timber.i("AutoBackupManager: Persistent external backup saved to Downloads (${destInDownloads.length()} bytes)")
        }.onFailure { e ->
            Timber.d("AutoBackupManager: Could not save persistent backup to Downloads: ${e.message}")
        }

        // 2. Try public Documents/AirBeats/airbeats_auto_backup.backup and airbeats_backup.backup (survives app uninstall)
        runCatching {
            val documentsDir = File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
                "AirBeats"
            )
            if (!documentsDir.exists()) documentsDir.mkdirs()
            val destInDocs = File(documentsDir, "airbeats_auto_backup.backup")
            sourceFile.copyTo(destInDocs, overwrite = true)
            sourceFile.copyTo(File(documentsDir, STORAGE_BACKUP_FILENAME), overwrite = true)
            Timber.i("AutoBackupManager: Persistent external backup saved to Documents (${destInDocs.length()} bytes)")
        }.onFailure { e ->
            Timber.d("AutoBackupManager: Could not save persistent backup to Documents: ${e.message}")
        }
    }

    fun findAvailableAutoBackup(context: Context): File? {
        // 1. Check internal filesDir
        val internalFile = getAutoBackupFile(context)
        if (internalFile.exists() && internalFile.length() > 0L) {
            return internalFile
        }

        // 2. Check legacy internal backup location
        val legacyFile = File(context.filesDir, "os_backup/latest.backup")
        if (legacyFile.exists() && legacyFile.length() > 0L) {
            return legacyFile
        }

        // 3. Check public Downloads/AirBeats/airbeats_auto_backup.backup (persistent after uninstall)
        val extDownloads = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            "AirBeats/airbeats_auto_backup.backup"
        )
        if (extDownloads.exists() && extDownloads.length() > 0L) {
            return extDownloads
        }

        // 4. Check public Documents/AirBeats/airbeats_auto_backup.backup (persistent after uninstall)
        val extDocs = File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS),
            "AirBeats/airbeats_auto_backup.backup"
        )
        if (extDocs.exists() && extDocs.length() > 0L) {
            return extDocs
        }

        // 5. Check for any user or auto backup files in Downloads/AirBeats/ or Documents/AirBeats/
        val candidateDirs = listOf(
            File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "AirBeats"),
            File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "AirBeats"),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        )
        for (dir in candidateDirs) {
            if (dir.exists() && dir.isDirectory) {
                val latestBackup = dir.listFiles { f -> f.isFile && f.name.endsWith(".backup") && f.length() > 0L }
                    ?.maxByOrNull { it.lastModified() }
                if (latestBackup != null) {
                    return latestBackup
                }
            }
        }

        return null
    }

    fun createAutoBackup(context: Context, database: MusicDatabase?): Boolean {
        return try {
            // Guard: Never overwrite existing backups with an empty, uninitialized state
            if (!hasBackableData(context, database)) {
                Timber.d("AutoBackupManager: Skipping auto_backup creation - database and profile have no content to back up")
                return false
            }

            val targetFile = getAutoBackupFile(context)
            val tmpFile = File(context.filesDir, "$BACKUP_FILENAME.tmp")

            FileOutputStream(tmpFile).use { fos ->
                createBackupZip(context, database, fos)
            }

            if (tmpFile.exists() && tmpFile.length() > 0) {
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                tmpFile.renameTo(targetFile)

                val now = System.currentTimeMillis()
                val currentSig = "${targetFile.length()}_${targetFile.lastModified()}"

                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_BACKUP_TIME, now)
                    .putString(KEY_LAST_RESTORED_SIG, currentSig)
                    .commit()

                if (isAutoBackupToStorageEnabled(context)) {
                    savePersistentExternalBackup(context, targetFile)
                }

                Timber.i("AutoBackupManager: auto_backup snapshot created successfully (${targetFile.length()} bytes)")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "AutoBackupManager: createAutoBackup failed")
            false
        }
    }

    fun restoreFromUri(context: Context, uri: Uri, shouldRestart: Boolean = true): Boolean {
        return try {
            Timber.d("AutoBackupManager: Starting restore from Uri: $uri")
            val targetFile = getAutoBackupFile(context)
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                FileOutputStream(targetFile).use { fos ->
                    stream.copyTo(fos)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                // Promote manually restored backup to cloud device backup immediately
                CoroutineScope(Dispatchers.IO).launch {
                    uploadToCloud(context, targetFile)
                }
                runCatching {
                    FileInputStream(targetFile).use { stream ->
                        restoreFromInputStream(context, stream, shouldRestart = shouldRestart)
                    }
                }.getOrDefault(false)
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "AutoBackupManager: restoreFromUri failed")
            reportException(e)
            false
        }
    }

    fun restoreFromInputStream(context: Context, rawStream: InputStream, shouldRestart: Boolean = true): Boolean {
        return try {
            val currentUserName = runBlocking {
                tryOrNull { NamePreferenceManager(context).userName.first() }
            }?.trim().orEmpty()
            val currentAvatar = runBlocking {
                tryOrNull { AvatarPreferenceManager(context).getAvatarSelection.first() }
            } ?: AvatarSelection.Default

            prepareDatabaseFilesForRestore(context)
            var databaseFilesPrepared = true
            rawStream.zipInputStream().use { inputStream ->
                var entry = tryOrNull { inputStream.nextEntry }
                while (entry != null) {
                    when {
                        entry.name.startsWith("datastore/") -> {
                            val relName = entry.name.removePrefix("datastore/")
                            val isUserNameEntry = relName.contains("user_name_preferences")
                            val isAvatarEntry = relName.contains("avatar_preferences")
                            if (relName.isNotBlank()) {
                                if (isUserNameEntry && currentUserName.isNotBlank()) {
                                    Timber.d("AutoBackupManager: Preserving universal username '$currentUserName', skipping backup datastore overwrite")
                                } else if (isAvatarEntry && currentAvatar !is AvatarSelection.Default) {
                                    Timber.d("AutoBackupManager: Preserving universal avatar, skipping backup datastore overwrite")
                                } else {
                                    val destFile = context.filesDir / "datastore" / relName
                                    destFile.parentFile?.mkdirs()
                                    destFile.outputStream().use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                            }
                        }

                        entry.name == SETTINGS_FILENAME -> {
                            val destFile = context.filesDir / "datastore" / SETTINGS_FILENAME
                            destFile.parentFile?.mkdirs()
                            destFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        entry.name == USER_NAME_PREFS_FILENAME -> {
                            if (currentUserName.isNotBlank()) {
                                Timber.d("AutoBackupManager: Preserving username '$currentUserName', skipping $USER_NAME_PREFS_FILENAME overwrite")
                            } else {
                                val destFile = context.filesDir / "datastore" / USER_NAME_PREFS_FILENAME
                                destFile.parentFile?.mkdirs()
                                destFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }

                        entry.name == STATS_IDENTITY_FILENAME -> {
                            val content = inputStream.readBytes().toString(Charsets.UTF_8)
                            runCatching {
                                val json = JSONObject(content)
                                val uid = json.optString("userId").trim()
                                if (uid.isNotBlank()) {
                                    AirBeatsStatsCloudSync.persistUserId(
                                        context,
                                        context.getSharedPreferences(AirBeatsStatsCloudSync.PREFERENCES_NAME, Context.MODE_PRIVATE),
                                        uid
                                    )
                                    Timber.i("AutoBackupManager: Restored stats userId from identity json: $uid")
                                }
                                val userNum = json.optString("userNumber").trim()
                                if (userNum.isNotBlank()) {
                                    AirBeatsStatsCloudSync.persistUserNumber(context, userNum)
                                    Timber.i("AutoBackupManager: Restored stats userNumber from identity json: $userNum")
                                }
                            }
                        }

                        entry.name == GLOBAL_STATS_FILENAME -> {
                            val bytes = inputStream.readBytes()
                            val xmlStr = bytes.toString(Charsets.UTF_8)
                            val uidRegex = """<string name="${AirBeatsStatsCloudSync.KEY_USER_ID}">([^<]+)</string>""".toRegex()
                            val match = uidRegex.find(xmlStr)
                            val extractedUid = match?.groupValues?.get(1)?.trim()
                            if (!extractedUid.isNullOrBlank()) {
                                AirBeatsStatsCloudSync.persistUserId(
                                    context,
                                    context.getSharedPreferences(AirBeatsStatsCloudSync.PREFERENCES_NAME, Context.MODE_PRIVATE),
                                    extractedUid
                                )
                                Timber.i("AutoBackupManager: Restored stats userId from XML: $extractedUid")
                            }
                            val numRegex = """<string name="${AirBeatsStatsCloudSync.KEY_USER_NUMBER}">([^<]+)</string>""".toRegex()
                            val numMatch = numRegex.find(xmlStr)
                            val extractedNum = numMatch?.groupValues?.get(1)?.trim()
                            if (!extractedNum.isNullOrBlank()) {
                                AirBeatsStatsCloudSync.persistUserNumber(context, extractedNum)
                                Timber.i("AutoBackupManager: Restored stats userNumber from XML: $extractedNum")
                            }
                            val parentFile = context.filesDir.parentFile
                            if (parentFile != null) {
                                val destFile = parentFile / "shared_prefs" / GLOBAL_STATS_FILENAME
                                destFile.parentFile?.mkdirs()
                                destFile.outputStream().use { outputStream ->
                                    outputStream.write(bytes)
                                }
                            }
                        }

                        entry.name == GOOGLE_ACCOUNT_FILENAME -> {
                            val email = inputStream.readBytes()
                                .toString(Charsets.UTF_8)
                                .let { JSONObject(it).optString("email") }
                                .trim()
                            if (email.isNotBlank()) {
                                runBlocking {
                                    NamePreferenceManager(context).rememberGoogleLoginEmail(email)
                                }
                            }
                        }

                        entry.name == InternalDatabase.DB_NAME -> {
                            if (!databaseFilesPrepared) {
                                prepareDatabaseFilesForRestore(context)
                                databaseFilesPrepared = true
                            }
                            val dbFile = context.getDatabasePath(InternalDatabase.DB_NAME)
                            dbFile.parentFile?.mkdirs()
                            FileOutputStream(dbFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        entry.name == "${InternalDatabase.DB_NAME}-wal" -> {
                            val walFile = context.getDatabasePath("${InternalDatabase.DB_NAME}-wal")
                            walFile.parentFile?.mkdirs()
                            FileOutputStream(walFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        entry.name == "${InternalDatabase.DB_NAME}-shm" -> {
                            val shmFile = context.getDatabasePath("${InternalDatabase.DB_NAME}-shm")
                            shmFile.parentFile?.mkdirs()
                            FileOutputStream(shmFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }

                        entry.name == PLAYLIST_IMAGES_PREFS_FILENAME -> {
                            val parentFile = context.filesDir.parentFile
                            if (parentFile != null) {
                                val destFile = parentFile / "shared_prefs" / PLAYLIST_IMAGES_PREFS_FILENAME
                                destFile.parentFile?.mkdirs()
                                destFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }

                        entry.name.startsWith("$PLAYLIST_IMAGES_DIR/") -> {
                            val relName = entry.name.removePrefix("$PLAYLIST_IMAGES_DIR/")
                            if (relName.isNotBlank()) {
                                val destFile = context.filesDir / PLAYLIST_IMAGES_DIR / relName
                                destFile.parentFile?.mkdirs()
                                destFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                    }
                    entry = tryOrNull { inputStream.nextEntry }
                }
            }
            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()

            runCatching {
                val db = com.darkxvenom.airbeats.db.InternalDatabase.newInstance(context)
                try {
                    com.darkxvenom.airbeats.db.DatabaseSanitizer.sanitizeDatabase(db)
                } finally {
                    db.close()
                }
            }

            markInitialStorageRestoreCheckComplete(context)
            resetRestartAttempts(context)
            val backupFile = getAutoBackupFile(context)
            val currentSig = if (backupFile.exists() && backupFile.length() > 0L) {
                "${backupFile.length()}_${backupFile.lastModified()}"
            } else {
                "restored_${System.currentTimeMillis()}"
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_RESTORED_SIG, currentSig)
                .putLong(KEY_LAST_BACKUP_TIME, backupFile.takeIf { it.exists() }?.lastModified() ?: System.currentTimeMillis())
                .commit()

            Timber.i("AutoBackupManager: Restore completed successfully with signature $currentSig")

            // Re-assert newly selected universal username & avatar so they are NEVER lost or overwritten
            if (currentUserName.isNotBlank()) {
                runBlocking {
                    tryOrNull { NamePreferenceManager(context).saveUserName(currentUserName) }
                }
            }
            if (currentAvatar !is AvatarSelection.Default) {
                runBlocking {
                    tryOrNull { AvatarPreferenceManager(context).saveAvatarSelection(currentAvatar) }
                }
            }
            setJustRestored(context, true)

            if (shouldRestart) {
                restartApp(context)
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "AutoBackupManager: restoreFromInputStream failed")
            reportException(e)
            false
        }
    }

    /** Close Room before replacing its files, then remove stale WAL/SHM files.
     * A stale WAL can make a restored database appear empty after app restart. */
    private fun prepareDatabaseFilesForRestore(context: Context) {
        runCatching { context.stopService(Intent(context, MusicService::class.java)) }
        runCatching { com.darkxvenom.airbeats.App.instance.database.close() }
            .onFailure { Timber.w(it, "Could not close active database before restore") }

        listOf(
            InternalDatabase.DB_NAME,
            "${InternalDatabase.DB_NAME}-wal",
            "${InternalDatabase.DB_NAME}-shm",
        ).forEach { name ->
            runCatching { context.getDatabasePath(name).delete() }
        }
    }

    fun restoreAutoBackup(context: Context, shouldRestart: Boolean = true): Boolean {
        val file = findAvailableAutoBackup(context)
        if (file == null || !file.exists() || file.length() == 0L) {
            Timber.w("AutoBackupManager: No backup file available to restore")
            return false
        }
        val targetFile = getAutoBackupFile(context)
        if (file.absolutePath != targetFile.absolutePath) {
            targetFile.parentFile?.mkdirs()
            runCatching { file.copyTo(targetFile, overwrite = true) }
        }
        val streamFile = if (targetFile.exists() && targetFile.length() > 0L) targetFile else file
        if (!streamFile.exists() || streamFile.length() == 0L) {
            return false
        }
        Timber.i("AutoBackupManager: Restoring auto backup from ${streamFile.absolutePath} (${streamFile.length()} bytes)")
        return runCatching {
            FileInputStream(streamFile).use { stream ->
                restoreFromInputStream(context, stream, shouldRestart)
            }
        }.getOrDefault(false)
    }

    suspend fun uploadToCloud(context: Context, backupFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!backupFile.exists() || backupFile.length() == 0L) {
            Timber.w("AutoBackupManager: Cannot upload non-existent or empty backup file")
            return@withContext false
        }
        return@withContext try {
            val cloudFile = getDeviceCloudFilePath(context)
            val url = "${RemoteConfigManager.statsBaseUrl}/upload?file=${URLEncoder.encode(cloudFile, "UTF-8")}"
            val mediaType = "application/octet-stream".toMediaTypeOrNull()
            val requestBody = backupFile.asRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val isSuccess = response.isSuccessful
                if (response.code == 404) {
                    Timber.d("AutoBackupManager: Custom cloud storage endpoint not active on worker (404). Local backup preserved.")
                } else {
                    Timber.i("AutoBackupManager: Cloud upload response code=${response.code}, success=$isSuccess")
                }
                isSuccess
            }
        } catch (e: Exception) {
            Timber.d("AutoBackupManager: Cloud upload unavailable: ${e.message}")
            false
        }
    }

    suspend fun downloadFromCloud(context: Context, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val cloudFile = getDeviceCloudFilePath(context)
            val url = "${RemoteConfigManager.statsBaseUrl}/download?file=${URLEncoder.encode(cloudFile, "UTF-8")}"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    Timber.d("AutoBackupManager: No remote cloud backup file on server (404)")
                    return@use false
                }
                if (!response.isSuccessful) {
                    Timber.w("AutoBackupManager: Cloud download failed with code=${response.code}")
                    return@use false
                }
                val body = response.body
                val tmpFile = File(destinationFile.parentFile ?: context.filesDir, "${destinationFile.name}.download")
                tmpFile.outputStream().use { fos ->
                    body.byteStream().copyTo(fos)
                }
                if (tmpFile.length() > 0L) {
                    if (destinationFile.exists()) destinationFile.delete()
                    tmpFile.renameTo(destinationFile)
                    Timber.i("AutoBackupManager: Cloud backup downloaded successfully (${destinationFile.length()} bytes)")
                    true
                } else {
                    tmpFile.delete()
                    false
                }
            }
        } catch (e: Exception) {
            Timber.d("AutoBackupManager: Cloud download unavailable: ${e.message}")
            false
        }
    }

    suspend fun deleteFromCloud(context: Context): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val cloudFile = getDeviceCloudFilePath(context)
            val url = "${RemoteConfigManager.statsBaseUrl}/delete?file=${URLEncoder.encode(cloudFile, "UTF-8")}"
            val request = Request.Builder()
                .url(url)
                .post("".toRequestBody(null))
                .build()

            httpClient.newCall(request).execute().use { response ->
                Timber.d("AutoBackupManager: Cloud delete response code=${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Timber.d("AutoBackupManager: Cloud delete unavailable: ${e.message}")
            false
        }
    }

    suspend fun checkAndRestoreDeviceCloudBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val restartAttempts = prefs.getInt(KEY_RESTART_ATTEMPTS, 0)
        if (restartAttempts >= 2) {
            Timber.w("AutoBackupManager: Aborting cloud restore to prevent restart loop ($restartAttempts attempts)")
            return@withContext false
        }

        // 1. Check if a local Android OS or persistent external auto-backup exists first
        val availableFile = findAvailableAutoBackup(context)
        if (availableFile != null && availableFile.exists() && availableFile.length() > 0L) {
            val restored = restoreAutoBackup(context, shouldRestart = false)
            if (restored) {
                Timber.i("AutoBackupManager: Restored state from discovered local backup file (${availableFile.length()} bytes)")
                prefs.edit()
                    .putInt(KEY_RESTART_ATTEMPTS, restartAttempts + 1)
                    .putLong(KEY_LAST_BACKUP_TIME, availableFile.lastModified())
                    .commit()
                withContext(Dispatchers.Main) {
                    restartApp(context)
                }
                return@withContext true
            }
        }

        // 2. Fall back to remote cloud download if available
        val targetFile = getAutoBackupFile(context)
        val downloadSuccess = downloadFromCloud(context, targetFile)
        if (!downloadSuccess || !targetFile.exists() || targetFile.length() == 0L) {
            return@withContext false
        }

        val currentSig = "${targetFile.length()}_${targetFile.lastModified()}"
        val lastRestoredSig = prefs.getString(KEY_LAST_RESTORED_SIG, null)
        if (lastRestoredSig == currentSig) {
            Timber.d("AutoBackupManager: Cloud backup already matches last restored signature, skipping duplicate unpack")
            return@withContext false
        }

        Timber.i("AutoBackupManager: Cloud backup downloaded for device. Restoring local state...")
        val success = runCatching {
            FileInputStream(targetFile).use { stream ->
                restoreFromInputStream(context, stream, shouldRestart = false)
            }
        }.getOrDefault(false)

        if (success) {
            prefs.edit()
                .putString(KEY_LAST_RESTORED_SIG, currentSig)
                .putInt(KEY_RESTART_ATTEMPTS, restartAttempts + 1)
                .putLong(KEY_LAST_BACKUP_TIME, targetFile.lastModified())
                .commit()

            Timber.i("AutoBackupManager: Cloud backup unpacked successfully! Restarting app with full restored profile...")
            withContext(Dispatchers.Main) {
                restartApp(context)
            }
            return@withContext true
        }
        return@withContext false
    }

    fun deleteBackup(context: Context): Boolean {
        return try {
            val file = getAutoBackupFile(context)
            if (file.exists()) {
                file.delete()
            }
            val legacyFile = File(context.filesDir, "os_backup/latest.backup")
            if (legacyFile.exists()) {
                legacyFile.delete()
            }

            runCatching {
                File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "AirBeats/airbeats_auto_backup.backup").delete()
                File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "AirBeats/airbeats_auto_backup.backup").delete()
            }

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
            true
        } catch (e: Exception) {
            Timber.e(e, "AutoBackupManager: deleteBackup failed")
            false
        }
    }

    fun restartApp(context: Context) {
        try {
            context.stopService(Intent(context, MusicService::class.java))
        } catch (_: Exception) {}

        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (intent != null) {
            try {
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    24601,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.set(
                    AlarmManager.RTC,
                    System.currentTimeMillis() + 400,
                    pendingIntent
                )
            } catch (_: Exception) {}
            try {
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        try {
            Thread.sleep(350)
        } catch (_: InterruptedException) {}

        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(0)
    }
}
