package com.darkxvenom.airbeats.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkxvenom.airbeats.MainActivity
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.InternalDatabase
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.db.entities.ArtistEntity
import com.darkxvenom.airbeats.db.entities.FormatEntity
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.db.entities.SongEntity
import com.darkxvenom.airbeats.models.MediaMetadata
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import com.darkxvenom.airbeats.extensions.div
import com.darkxvenom.airbeats.extensions.tryOrNull
import com.darkxvenom.airbeats.extensions.zipInputStream
import com.darkxvenom.airbeats.extensions.zipOutputStream
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    fun backup(context: Context, uri: Uri) {
        runCatching {
            context.applicationContext.contentResolver.openOutputStream(uri)?.use {
                it.buffered().zipOutputStream().use { outputStream ->
                    (context.filesDir / "datastore" / SETTINGS_FILENAME).inputStream().buffered()
                        .use { inputStream ->
                            outputStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                            inputStream.copyTo(outputStream)
                        }

                    val namePrefsFile = context.filesDir / "datastore" / "user_name_preferences.preferences_pb"
                    if (namePrefsFile.exists()) {
                        namePrefsFile.inputStream().buffered().use { inputStream ->
                            outputStream.putNextEntry(ZipEntry("user_name_preferences.preferences_pb"))
                            inputStream.copyTo(outputStream)
                        }
                    }

                    val accountEmail = runBlocking { NamePreferenceManager(context).accountEmail.first() }
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

                    val parentFile = context.filesDir.parentFile
                    if (parentFile != null) {
                        val statsPrefsFile = parentFile / "shared_prefs" / "airbeats_global_stats.xml"
                        if (statsPrefsFile.exists()) {
                            statsPrefsFile.inputStream().buffered().use { inputStream ->
                                outputStream.putNextEntry(ZipEntry("airbeats_global_stats.xml"))
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }

                    runBlocking(Dispatchers.IO) {
                        database.checkpoint()
                    }
                    FileInputStream(database.openHelper.writableDatabase.path).use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                }
            }
        }.onSuccess {
            Toast.makeText(context, R.string.backup_create_success, Toast.LENGTH_SHORT).show()
        }.onFailure {
            reportException(it)
            Toast.makeText(context, R.string.backup_create_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun restore(context: Context, uri: Uri) {
        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use {
                it.zipInputStream().use { inputStream ->
                    var entry = tryOrNull { inputStream.nextEntry } // prevent ZipException
                    while (entry != null) {
                        when (entry.name) {
                            SETTINGS_FILENAME -> {
                                (context.filesDir / "datastore" / SETTINGS_FILENAME).outputStream()
                                    .use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                            }

                            "user_name_preferences.preferences_pb" -> {
                                val destFile = context.filesDir / "datastore" / "user_name_preferences.preferences_pb"
                                destFile.parentFile?.mkdirs()
                                destFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }

                            "airbeats_global_stats.xml" -> {
                                val parentFile = context.filesDir.parentFile
                                if (parentFile != null) {
                                    val destFile = parentFile / "shared_prefs" / "airbeats_global_stats.xml"
                                    destFile.parentFile?.mkdirs()
                                    destFile.outputStream().use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                            }

                            GOOGLE_ACCOUNT_FILENAME -> {
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

                            InternalDatabase.DB_NAME -> {
                                runBlocking(Dispatchers.IO) {
                                    database.checkpoint()
                                }
                                database.close()
                                FileOutputStream(database.openHelper.writableDatabase.path).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        entry = tryOrNull { inputStream.nextEntry } // prevent ZipException
                    }
                }
            }
            context.stopService(Intent(context, MusicService::class.java))
            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            context.startActivity(Intent(context, MainActivity::class.java))
            exitProcess(0)
        }.onFailure {
            reportException(it)
            Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun backupToDrive(context: Context, email: String, name: String = "AirBeats User"): com.darkxvenom.airbeats.utils.DriveResult<Boolean> {
        return try {
            val tempFile = java.io.File(context.cacheDir, "temp_backup.zip")
            tempFile.outputStream().use { fileOut ->
                fileOut.buffered().zipOutputStream().use { outputStream ->
                    (context.filesDir / "datastore" / SETTINGS_FILENAME).takeIf { it.exists() }?.inputStream()?.buffered()?.use { inputStream ->
                        outputStream.putNextEntry(java.util.zip.ZipEntry(SETTINGS_FILENAME))
                        inputStream.copyTo(outputStream)
                    }

                    val namePrefsFile = context.filesDir / "datastore" / "user_name_preferences.preferences_pb"
                    if (namePrefsFile.exists()) {
                        namePrefsFile.inputStream().buffered().use { inputStream ->
                            outputStream.putNextEntry(java.util.zip.ZipEntry("user_name_preferences.preferences_pb"))
                            inputStream.copyTo(outputStream)
                        }
                    }

                    outputStream.putNextEntry(java.util.zip.ZipEntry(GOOGLE_ACCOUNT_FILENAME))
                    outputStream.write(
                        org.json.JSONObject()
                            .put("email", email)
                            .put("previouslyLoggedIn", true)
                            .toString()
                            .toByteArray()
                    )

                    val parentFile = context.filesDir.parentFile
                    if (parentFile != null) {
                        val statsPrefsFile = parentFile / "shared_prefs" / "airbeats_global_stats.xml"
                        if (statsPrefsFile.exists()) {
                            statsPrefsFile.inputStream().buffered().use { inputStream ->
                                outputStream.putNextEntry(java.util.zip.ZipEntry("airbeats_global_stats.xml"))
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }

                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        database.checkpoint()
                    }
                    java.io.FileInputStream(database.openHelper.writableDatabase.path).use { inputStream ->
                        outputStream.putNextEntry(java.util.zip.ZipEntry(com.darkxvenom.airbeats.db.InternalDatabase.DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                }
            }

            val backupClient = com.darkxvenom.airbeats.utils.CloudBackupClient()
            
            // Note: CloudBackupClient handles the details.json internally as part of uploadBackup
            val success = backupClient.uploadBackup(
                email = email,
                name = name,
                backupFile = tempFile
            )

            if (success) {
                com.darkxvenom.airbeats.utils.DriveResult.Success(true)
            } else {
                com.darkxvenom.airbeats.utils.DriveResult.Error(Exception("Cloud backup upload failed"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            com.darkxvenom.airbeats.utils.DriveResult.Error(e)
        }
    }

    suspend fun restoreFromDrive(context: Context, email: String): com.darkxvenom.airbeats.utils.DriveResult<Boolean> {
        return try {
            val tempFile = java.io.File(context.cacheDir, "temp_restore.zip")
            val backupClient = com.darkxvenom.airbeats.utils.CloudBackupClient()
            
            val success = backupClient.downloadBackup(email, tempFile)
            if (!success) {
                return com.darkxvenom.airbeats.utils.DriveResult.Error(Exception("Backup not found in cloud"))
            }

            tempFile.inputStream().use { fileIn ->
                fileIn.zipInputStream().use { inputStream ->
                    var entry = runCatching { inputStream.nextEntry }.getOrNull()
                    while (entry != null) {
                        when (entry?.name) {
                            SETTINGS_FILENAME -> {
                                (context.filesDir / "datastore" / SETTINGS_FILENAME).outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            "user_name_preferences.preferences_pb" -> {
                                val destFile = context.filesDir / "datastore" / "user_name_preferences.preferences_pb"
                                destFile.parentFile?.mkdirs()
                                destFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            "airbeats_global_stats.xml" -> {
                                val parentFile = context.filesDir.parentFile
                                if (parentFile != null) {
                                    val destFile = parentFile / "shared_prefs" / "airbeats_global_stats.xml"
                                    destFile.parentFile?.mkdirs()
                                    destFile.outputStream().use { outputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                            }
                            GOOGLE_ACCOUNT_FILENAME -> {
                                val restoredEmail = inputStream.readBytes()
                                    .toString(Charsets.UTF_8)
                                    .let { org.json.JSONObject(it).optString("email") }
                                    .trim()
                                if (restoredEmail.isNotBlank()) {
                                    kotlinx.coroutines.runBlocking {
                                        NamePreferenceManager(context).rememberGoogleLoginEmail(restoredEmail)
                                    }
                                }
                            }
                            com.darkxvenom.airbeats.db.InternalDatabase.DB_NAME -> {
                                kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                                    database.checkpoint()
                                }
                                database.close()
                                java.io.FileOutputStream(database.openHelper.writableDatabase.path).use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        entry = runCatching { inputStream.nextEntry }.getOrNull()
                    }
                }
            }
            com.darkxvenom.airbeats.utils.DriveResult.Success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            com.darkxvenom.airbeats.utils.DriveResult.Error(e)
        }
    }

    fun importPlaylistFromCsv(context: Context, uri: Uri): ArrayList<Song> {
        val songs = arrayListOf<Song>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                lines.forEachIndexed { _, line ->
                    val parts = line.split(",").map { it.trim() }
                    val title = parts[0]
                    val artistStr = parts[1]

                    val artists = artistStr.split(";").map { it.trim() }.map {
                        ArtistEntity(
                            id = "",
                            name = it,
                        )
                    }
                    val mockSong = Song(
                        song = SongEntity(
                            id = "",
                            title = title,
                        ),
                        artists = artists,
                    )
                    songs.add(mockSong)
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    fun loadM3UOnline(
        context: Context,
        uri: Uri,
    ): ArrayList<Song> {
        val songs = ArrayList<Song>()

        runCatching {
            context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                val lines = stream.bufferedReader().readLines()
                if (lines.firstOrNull()?.startsWith("#EXTM3U") == true) {
                    lines.forEachIndexed { _, rawLine ->
                        if (rawLine.startsWith("#EXTINF:")) {
                            // maybe later write this to be more efficient
                            val artists =
                                rawLine.substringAfter("#EXTINF:").substringAfter(',').substringBefore(" - ").split(';')
                            val title = rawLine.substringAfter("#EXTINF:").substringAfter(',').substringAfter(" - ")

                            val mockSong = Song(
                                song = SongEntity(
                                    id = "",
                                    title = title,
                                ),
                                artists = artists.map { ArtistEntity("", it) },
                            )
                            songs.add(mockSong)

                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            Toast.makeText(
                context,
                "No songs found. Invalid file, or perhaps no song matches were found.",
                Toast.LENGTH_SHORT
            ).show()
        }
        return songs
    }

    fun resetVisitorData(context: Context) {
        runCatching {
            // Implementa aquí cómo borras VISITOR_DATA, por ejemplo, desde DataStore
            val visitorDataFile = context.filesDir / "datastore" / SETTINGS_FILENAME
            if (visitorDataFile.exists()) {
                // Borra solo la parte de VISITOR_DATA si es posible, o reinicia el archivo
                visitorDataFile.delete()
            }

            Toast.makeText(
                context,
                "VISITOR_DATA reseteado. La aplicación se reiniciará.",
                Toast.LENGTH_SHORT
            ).show()

            context.stopService(Intent(context, MusicService::class.java))
            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            context.startActivity(
                Intent(
                    context,
                    MainActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            exitProcess(0)
        }.onFailure {
            reportException(it)
            Toast.makeText(context, "Error al resetear VISITOR_DATA", Toast.LENGTH_SHORT).show()
        }
    }

    fun backupCache(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                database.checkpoint()
                context.applicationContext.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.buffered().zipOutputStream().use { zipOut ->
                        // 1. Backup exoplayer cache chunks
                        val exoDir = context.filesDir.resolve("exoplayer")
                        if (exoDir.exists() && exoDir.isDirectory) {
                            exoDir.walkTopDown().forEach { file ->
                                if (file.isFile) {
                                    val relPath = "exoplayer/" + file.relativeTo(exoDir).path.replace('\\', '/')
                                    zipOut.putNextEntry(ZipEntry(relPath))
                                    file.inputStream().buffered().use { it.copyTo(zipOut) }
                                }
                            }
                        }

                        // 2. Backup download cache if present
                        val dlDir = context.filesDir.resolve("download")
                        if (dlDir.exists() && dlDir.isDirectory) {
                            dlDir.walkTopDown().forEach { file ->
                                if (file.isFile) {
                                    val relPath = "download/" + file.relativeTo(dlDir).path.replace('\\', '/')
                                    zipOut.putNextEntry(ZipEntry(relPath))
                                    file.inputStream().buffered().use { it.copyTo(zipOut) }
                                }
                            }
                        }

                        // 3. Backup exoplayer internal database
                        val exoDb = context.getDatabasePath("exoplayer_internal.db")
                        if (exoDb.exists() && exoDb.isFile) {
                            zipOut.putNextEntry(ZipEntry("exoplayer_internal.db"))
                            exoDb.inputStream().buffered().use { it.copyTo(zipOut) }
                        }
                        val exoDbWal = context.getDatabasePath("exoplayer_internal.db-wal")
                        if (exoDbWal.exists() && exoDbWal.isFile) {
                            zipOut.putNextEntry(ZipEntry("exoplayer_internal.db-wal"))
                            exoDbWal.inputStream().buffered().use { it.copyTo(zipOut) }
                        }

                        // 4. Backup cached song metadata and formats
                        val allSongs = runCatching { database.songsByNameAsc().first() }.getOrDefault(emptyList())
                        val songJsonArray = JSONArray()
                        for (song in allSongs) {
                            val format = runCatching { database.format(song.id).first() }.getOrNull()
                            val obj = JSONObject().apply {
                                put("id", song.id)
                                put("title", song.title)
                                put("duration", song.duration)
                                put("thumbnailUrl", song.thumbnailUrl)
                                put("artists", JSONArray(song.artists.map { it.name }))
                                if (format != null) {
                                    put("itag", format.itag)
                                    put("mimeType", format.mimeType)
                                    put("codecs", format.codecs)
                                    put("bitrate", format.bitrate)
                                    put("sampleRate", format.sampleRate)
                                    put("contentLength", format.contentLength)
                                    if (format.loudnessDb != null) put("loudnessDb", format.loudnessDb)
                                    if (format.playbackUrl != null) put("playbackUrl", format.playbackUrl)
                                }
                            }
                            songJsonArray.put(obj)
                        }
                        zipOut.putNextEntry(ZipEntry("cached_songs_metadata.json"))
                        zipOut.write(songJsonArray.toString().toByteArray(Charsets.UTF_8))
                    }
                }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.backup_cache_success, Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                reportException(it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.backup_cache_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun restoreCache(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val exoDir = context.filesDir.resolve("exoplayer")
                val dlDir = context.filesDir.resolve("download")
                exoDir.mkdirs()
                dlDir.mkdirs()

                context.applicationContext.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.zipInputStream().use { zipIn ->
                        var entry = tryOrNull { zipIn.nextEntry }
                        while (entry != null) {
                            val name = entry.name
                            when {
                                name.startsWith("exoplayer/") -> {
                                    val rel = name.removePrefix("exoplayer/")
                                    if (rel.isNotEmpty()) {
                                        val target = exoDir.resolve(rel)
                                        target.parentFile?.mkdirs()
                                        target.outputStream().buffered().use { zipIn.copyTo(it) }
                                    }
                                }
                                name.startsWith("download/") -> {
                                    val rel = name.removePrefix("download/")
                                    if (rel.isNotEmpty()) {
                                        val target = dlDir.resolve(rel)
                                        target.parentFile?.mkdirs()
                                        target.outputStream().buffered().use { zipIn.copyTo(it) }
                                    }
                                }
                                name == "exoplayer_internal.db" || name == "db/exoplayer_internal.db" -> {
                                    val target = context.getDatabasePath("exoplayer_internal.db")
                                    target.parentFile?.mkdirs()
                                    target.outputStream().buffered().use { zipIn.copyTo(it) }
                                }
                                name == "exoplayer_internal.db-wal" || name == "db/exoplayer_internal.db-wal" -> {
                                    val target = context.getDatabasePath("exoplayer_internal.db-wal")
                                    target.parentFile?.mkdirs()
                                    target.outputStream().buffered().use { zipIn.copyTo(it) }
                                }
                                name == "cached_songs_metadata.json" || name == "metadata.json" -> {
                                    val jsonStr = zipIn.readBytes().toString(Charsets.UTF_8)
                                    val array = JSONArray(jsonStr)
                                    for (i in 0 until array.length()) {
                                        val obj = array.getJSONObject(i)
                                        val id = obj.getString("id")
                                        val title = obj.getString("title")
                                        val duration = obj.optInt("duration", -1)
                                        val thumbnailUrl = if (obj.has("thumbnailUrl") && !obj.isNull("thumbnailUrl")) obj.getString("thumbnailUrl") else null
                                        val artistsArray = obj.optJSONArray("artists")
                                        val artists = mutableListOf<String>()
                                        if (artistsArray != null) {
                                            for (j in 0 until artistsArray.length()) {
                                                artists.add(artistsArray.getString(j))
                                            }
                                        }
                                        val mediaMetadata = MediaMetadata(
                                            id = id,
                                            title = title,
                                            artists = artists.map { MediaMetadata.Artist(id = null, name = it) },
                                            duration = duration,
                                            thumbnailUrl = thumbnailUrl
                                        )
                                        database.insert(mediaMetadata)

                                        if (obj.has("itag")) {
                                            database.query {
                                                upsert(
                                                    FormatEntity(
                                                        id = id,
                                                        itag = obj.getInt("itag"),
                                                        mimeType = obj.getString("mimeType"),
                                                        codecs = obj.getString("codecs"),
                                                        bitrate = obj.getInt("bitrate"),
                                                        sampleRate = obj.getInt("sampleRate"),
                                                        contentLength = obj.getLong("contentLength"),
                                                        loudnessDb = if (obj.has("loudnessDb") && !obj.isNull("loudnessDb")) obj.getDouble("loudnessDb") else null,
                                                        playbackUrl = if (obj.has("playbackUrl") && !obj.isNull("playbackUrl")) obj.getString("playbackUrl") else null
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            entry = tryOrNull { zipIn.nextEntry }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.restore_cache_success, Toast.LENGTH_SHORT).show()
                    context.stopService(Intent(context, MusicService::class.java))
                    context.startActivity(
                        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    exitProcess(0)
                }
            }.onFailure {
                reportException(it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.restore_cache_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        const val SETTINGS_FILENAME = "settings.preferences_pb"
        const val GOOGLE_ACCOUNT_FILENAME = "google_account.json"
    }
}

