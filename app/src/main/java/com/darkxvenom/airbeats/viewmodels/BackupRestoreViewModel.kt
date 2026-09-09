package com.darkxvenom.airbeats.viewmodels

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkxvenom.airbeats.MainActivity
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.db.InternalDatabase
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.db.entities.ArtistEntity
import com.darkxvenom.airbeats.db.entities.Event
import com.darkxvenom.airbeats.db.entities.FormatEntity
import com.darkxvenom.airbeats.db.entities.Song
import com.darkxvenom.airbeats.db.entities.SongEntity
import com.darkxvenom.airbeats.models.MediaMetadata
import java.time.LocalDateTime
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
            context.filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            restartApp(context)
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

    fun restartApp(context: Context) {
        try {
            context.stopService(Intent(context, MusicService::class.java))
        } catch (_: Exception) {}

        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
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

    fun backupCache(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                tryOrNull { database.checkpoint() }

                context.applicationContext.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.buffered().zipOutputStream().use { zipOut ->
                        // 1. Backup exoplayer cache chunks
                        val exoDir = context.filesDir.resolve("exoplayer")
                        if (exoDir.exists() && exoDir.isDirectory) {
                            exoDir.walkTopDown().forEach { file ->
                                if (file.isFile) {
                                    tryOrNull {
                                        val relPath = "exoplayer/" + file.relativeTo(exoDir).path.replace('\\', '/')
                                        zipOut.putNextEntry(ZipEntry(relPath))
                                        file.inputStream().buffered().use { it.copyTo(zipOut) }
                                    }
                                }
                            }
                        }

                        // 2. Backup download cache if present
                        val dlDir = context.filesDir.resolve("download")
                        if (dlDir.exists() && dlDir.isDirectory) {
                            dlDir.walkTopDown().forEach { file ->
                                if (file.isFile) {
                                    tryOrNull {
                                        val relPath = "download/" + file.relativeTo(dlDir).path.replace('\\', '/')
                                        zipOut.putNextEntry(ZipEntry(relPath))
                                        file.inputStream().buffered().use { it.copyTo(zipOut) }
                                    }
                                }
                            }
                        }

                        // 3. Backup exoplayer internal database
                        val exoDb = context.getDatabasePath("exoplayer_internal.db")
                        if (exoDb.exists() && exoDb.isFile) {
                            tryOrNull {
                                SQLiteDatabase.openDatabase(exoDb.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                                }
                            }
                            tryOrNull {
                                zipOut.putNextEntry(ZipEntry("exoplayer_internal.db"))
                                exoDb.inputStream().buffered().use { it.copyTo(zipOut) }
                            }
                        }
                        val exoDbWal = context.getDatabasePath("exoplayer_internal.db-wal")
                        if (exoDbWal.exists() && exoDbWal.isFile) {
                            tryOrNull {
                                zipOut.putNextEntry(ZipEntry("exoplayer_internal.db-wal"))
                                exoDbWal.inputStream().buffered().use { it.copyTo(zipOut) }
                            }
                        }

                        // Backup restored_cache_ids.json if present
                        val restoredFile = context.filesDir.resolve("restored_cache_ids.json")
                        if (restoredFile.exists() && restoredFile.isFile) {
                            tryOrNull {
                                zipOut.putNextEntry(ZipEntry("restored_cache_ids.json"))
                                restoredFile.inputStream().buffered().use { it.copyTo(zipOut) }
                            }
                        }

                        // 4. Backup cached song metadata and formats
                        tryOrNull {
                            val formatsMap = mutableMapOf<String, FormatEntity>()
                            tryOrNull {
                                database.openHelper.readableDatabase.query(
                                    "SELECT id, itag, mimeType, codecs, bitrate, sampleRate, contentLength, loudnessDb, playbackUrl FROM format"
                                ).use { cursor ->
                                    val idCol = cursor.getColumnIndex("id")
                                    val itagCol = cursor.getColumnIndex("itag")
                                    val mimeCol = cursor.getColumnIndex("mimeType")
                                    val codecsCol = cursor.getColumnIndex("codecs")
                                    val bitrateCol = cursor.getColumnIndex("bitrate")
                                    val sampleCol = cursor.getColumnIndex("sampleRate")
                                    val lenCol = cursor.getColumnIndex("contentLength")
                                    val loudCol = cursor.getColumnIndex("loudnessDb")
                                    val urlCol = cursor.getColumnIndex("playbackUrl")
                                    while (cursor.moveToNext()) {
                                        val sId = cursor.getString(idCol)
                                        formatsMap[sId] = FormatEntity(
                                            id = sId,
                                            itag = cursor.getInt(itagCol),
                                            mimeType = cursor.getString(mimeCol),
                                            codecs = cursor.getString(codecsCol),
                                            bitrate = cursor.getInt(bitrateCol),
                                            sampleRate = if (!cursor.isNull(sampleCol)) cursor.getInt(sampleCol) else null,
                                            contentLength = cursor.getLong(lenCol),
                                            loudnessDb = if (!cursor.isNull(loudCol)) cursor.getDouble(loudCol) else null,
                                            playbackUrl = if (!cursor.isNull(urlCol)) cursor.getString(urlCol) else null
                                        )
                                    }
                                }
                            }

                            val songsList = mutableListOf<SongEntity>()
                            tryOrNull {
                                database.openHelper.readableDatabase.query(
                                    "SELECT id, title, duration, thumbnailUrl FROM song"
                                ).use { cursor ->
                                    val idCol = cursor.getColumnIndex("id")
                                    val titleCol = cursor.getColumnIndex("title")
                                    val durCol = cursor.getColumnIndex("duration")
                                    val thumbCol = cursor.getColumnIndex("thumbnailUrl")
                                    while (cursor.moveToNext()) {
                                        songsList.add(
                                            SongEntity(
                                                id = cursor.getString(idCol),
                                                title = cursor.getString(titleCol),
                                                duration = cursor.getInt(durCol),
                                                thumbnailUrl = if (!cursor.isNull(thumbCol)) cursor.getString(thumbCol) else null
                                            )
                                        )
                                    }
                                }
                            }

                            val artistMap = mutableMapOf<String, MutableList<String>>()
                            tryOrNull {
                                database.openHelper.readableDatabase.query(
                                    "SELECT song_artist_map.songId, artist.name FROM song_artist_map JOIN artist ON song_artist_map.artistId = artist.id"
                                ).use { cursor ->
                                    val songIdCol = cursor.getColumnIndex("songId")
                                    val nameCol = cursor.getColumnIndex("name")
                                    while (cursor.moveToNext()) {
                                        val sId = cursor.getString(songIdCol)
                                        val name = cursor.getString(nameCol)
                                        artistMap.getOrPut(sId) { mutableListOf() }.add(name)
                                    }
                                }
                            }

                            val songJsonArray = JSONArray()
                            for (song in songsList) {
                                val format = formatsMap[song.id]
                                val artists = artistMap[song.id] ?: emptyList()
                                val obj = JSONObject().apply {
                                    put("id", song.id)
                                    put("title", song.title)
                                    put("duration", song.duration)
                                    put("thumbnailUrl", song.thumbnailUrl)
                                    put("artists", JSONArray(artists))
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
                // Stop playback service first so files and databases are released
                withContext(Dispatchers.Main) {
                    try {
                        context.stopService(Intent(context, MusicService::class.java))
                    } catch (_: Exception) {}
                }

                val tempDir = java.io.File(context.cacheDir, "cache_restore_${System.currentTimeMillis()}").apply { mkdirs() }

                try {
                    context.applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                        java.util.zip.ZipInputStream(inputStream.buffered()).use { zipIn ->
                            var entry = tryOrNull { zipIn.nextEntry }
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    val normName = entry.name.replace('\\', '/').trimStart('/')
                                    val destFile = java.io.File(tempDir, normName)
                                    if (destFile.canonicalPath.startsWith(tempDir.canonicalPath)) {
                                        destFile.parentFile?.mkdirs()
                                        destFile.outputStream().buffered().use { zipIn.copyTo(it) }
                                    }
                                }
                                zipIn.closeEntry()
                                entry = tryOrNull { zipIn.nextEntry }
                            }
                        }
                    }

                    // 1. Process cached songs metadata and formats into Room DB
                    val metadataFile = tempDir.walkTopDown().firstOrNull {
                        it.isFile && (it.name == "cached_songs_metadata.json" || it.name == "metadata.json")
                    }
                    val restoredSongIds = mutableListOf<String>()
                    val songOrderFromMetadata = mutableListOf<String>()

                    if (metadataFile != null && metadataFile.exists()) {
                        tryOrNull {
                            val jsonStr = metadataFile.readText(Charsets.UTF_8)
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
                                restoredSongIds.add(id)
                                songOrderFromMetadata.add(id)

                                val mediaMetadata = MediaMetadata(
                                    id = id,
                                    title = title,
                                    artists = artists.map { MediaMetadata.Artist(id = null, name = it) },
                                    duration = duration,
                                    thumbnailUrl = thumbnailUrl
                                )
                                database.query {
                                    insert(mediaMetadata)
                                    val existing = getSongById(id)
                                    if (existing != null) {
                                        update(existing.song.copy(
                                            title = title,
                                            duration = if (duration != -1) duration else existing.song.duration,
                                            thumbnailUrl = thumbnailUrl ?: existing.song.thumbnailUrl
                                        ))
                                    }
                                }

                                if (obj.has("itag")) {
                                    database.query {
                                        upsert(
                                            FormatEntity(
                                                id = id,
                                                itag = obj.getInt("itag"),
                                                mimeType = obj.getString("mimeType"),
                                                codecs = obj.getString("codecs"),
                                                bitrate = obj.getInt("bitrate"),
                                                sampleRate = if (obj.has("sampleRate") && !obj.isNull("sampleRate")) obj.getInt("sampleRate") else null,
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

                    // 2. Target directories on device
                    val targetExoDir = context.filesDir.resolve("exoplayer").apply { mkdirs() }
                    val targetDlDir = context.filesDir.resolve("download").apply { mkdirs() }
                    val targetDbFile = context.getDatabasePath("exoplayer_internal.db").apply { parentFile?.mkdirs() }

                    // 3. Preserve device's active UIDs (NEVER delete or change existing .uid files!)
                    val existingExoUidFiles = targetExoDir.listFiles { _, name -> name.endsWith(".uid") } ?: emptyArray()
                    val backupExoUidFile = tempDir.walkTopDown().firstOrNull {
                        it.isFile && it.name.endsWith(".uid") && (it.parentFile?.name == "exoplayer" || it.parentFile == tempDir)
                    }
                    val activeExoUid = if (existingExoUidFiles.isNotEmpty()) {
                        existingExoUidFiles.first().name.removeSuffix(".uid")
                    } else if (backupExoUidFile != null) {
                        val bUid = backupExoUidFile.name.removeSuffix(".uid")
                        targetExoDir.resolve("$bUid.uid").createNewFile()
                        bUid
                    } else {
                        val newUid = java.lang.Long.toHexString(java.security.SecureRandom().nextLong())
                        targetExoDir.resolve("$newUid.uid").createNewFile()
                        newUid
                    }

                    targetExoDir.listFiles { _, name -> name.endsWith(".uid") }?.forEach { f ->
                        if (!f.name.equals("$activeExoUid.uid", ignoreCase = true)) {
                            f.delete()
                        }
                    }

                    val existingDlUidFiles = targetDlDir.listFiles { _, name -> name.endsWith(".uid") } ?: emptyArray()
                    val backupDlUidFile = tempDir.walkTopDown().firstOrNull {
                        it.isFile && it.name.endsWith(".uid") && it.parentFile?.name == "download"
                    }
                    val activeDlUid = if (existingDlUidFiles.isNotEmpty()) {
                        existingDlUidFiles.first().name.removeSuffix(".uid")
                    } else if (backupDlUidFile != null) {
                        val bUid = backupDlUidFile.name.removeSuffix(".uid")
                        targetDlDir.resolve("$bUid.uid").createNewFile()
                        bUid
                    } else {
                        val newUid = java.lang.Long.toHexString(java.security.SecureRandom().nextLong())
                        targetDlDir.resolve("$newUid.uid").createNewFile()
                        newUid
                    }

                    targetDlDir.listFiles { _, name -> name.endsWith(".uid") }?.forEach { f ->
                        if (!f.name.equals("$activeDlUid.uid", ignoreCase = true)) {
                            f.delete()
                        }
                    }

                    // 4. Open device's exoplayer_internal.db
                    SQLiteDatabase.openOrCreateDatabase(targetDbFile.path, null).use { db ->
                        tryOrNull {
                            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                        }

                        // Register active UIDs in ExoPlayerVersions
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS ExoPlayerVersions (" +
                            "feature INTEGER NOT NULL, " +
                            "instance_uid TEXT NOT NULL, " +
                            "version INTEGER NOT NULL, " +
                            "PRIMARY KEY (feature, instance_uid))"
                        )
                        db.execSQL("INSERT OR REPLACE INTO ExoPlayerVersions (feature, instance_uid, version) VALUES (1, '$activeExoUid', 1)")
                        db.execSQL("INSERT OR REPLACE INTO ExoPlayerVersions (feature, instance_uid, version) VALUES (1, '$activeDlUid', 1)")

                        // Inspect backup database in tempDir
                        val backupDbFile = tempDir.walkTopDown().firstOrNull { it.isFile && it.name == "exoplayer_internal.db" }
                        val backupExoEntries = mutableMapOf<Int, Pair<String, ByteArray>>() // backupChunkId -> (key, metadata)
                        val backupDlEntries = mutableMapOf<Int, Pair<String, ByteArray>>()

                        if (backupDbFile != null && backupDbFile.exists()) {
                            tryOrNull {
                                SQLiteDatabase.openDatabase(backupDbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { bDb ->
                                    val tables = mutableListOf<String>()
                                    bDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'ExoPlayerCacheIndex%'", null).use { c ->
                                        while (c.moveToNext()) tables.add(c.getString(0))
                                    }
                                    for (table in tables) {
                                        val isDlTable = backupDlUidFile != null && table.contains(backupDlUidFile.name.removeSuffix(".uid"))
                                        bDb.rawQuery("SELECT id, key, metadata FROM $table", null).use { c ->
                                            while (c.moveToNext()) {
                                                val id = c.getInt(0)
                                                val key = c.getString(1)
                                                val metadata = c.getBlob(2)
                                                if (isDlTable) {
                                                    backupDlEntries[id] = Pair(key, metadata)
                                                } else {
                                                    backupExoEntries[id] = Pair(key, metadata)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val emptyMetadata = byteArrayOf(0, 0, 0, 0)

                        // 5. Merge exoplayer cache entries
                        val targetExoTable = "ExoPlayerCacheIndex$activeExoUid"
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS $targetExoTable (" +
                            "id INTEGER PRIMARY KEY NOT NULL, " +
                            "key TEXT NOT NULL, " +
                            "metadata BLOB NOT NULL)"
                        )

                        val existingExoKeyToId = mutableMapOf<String, Int>()
                        val existingExoIds = mutableSetOf<Int>()
                        db.rawQuery("SELECT id, key FROM $targetExoTable", null).use { cursor ->
                            while (cursor.moveToNext()) {
                                val id = cursor.getInt(0)
                                val key = cursor.getString(1)
                                existingExoKeyToId[key] = id
                                existingExoIds.add(id)
                            }
                        }
                        targetExoDir.walkTopDown().forEach { file ->
                            if (file.isFile && file.name.endsWith(".exo")) {
                                file.name.substringBefore('.').toIntOrNull()?.let { existingExoIds.add(it) }
                            }
                        }
                        var maxExoId = existingExoIds.maxOrNull() ?: -1

                        val backupExoFiles = tempDir.walkTopDown().filter { file ->
                            file.isFile && file.name.endsWith(".exo") && !file.path.replace('\\', '/').contains("/download/")
                        }.toList()

                        val exoFilesByBackupId = backupExoFiles.groupBy { it.name.substringBefore('.').toIntOrNull() }
                        for ((backupId, files) in exoFilesByBackupId) {
                            if (backupId == null) continue
                            val entry = backupExoEntries[backupId]
                            val songKey = entry?.first
                                ?: songOrderFromMetadata.getOrNull(backupId)
                                ?: restoredSongIds.getOrNull(backupId)
                                ?: "restored_$backupId"
                            val metadata = entry?.second ?: emptyMetadata

                            val targetId = existingExoKeyToId.getOrPut(songKey) {
                                val newId = ++maxExoId
                                tryOrNull {
                                    val stmt = db.compileStatement(
                                        "INSERT OR REPLACE INTO $targetExoTable (id, key, metadata) VALUES (?, ?, ?)"
                                    )
                                    stmt.bindLong(1, newId.toLong())
                                    stmt.bindString(2, songKey)
                                    stmt.bindBlob(3, metadata)
                                    stmt.executeInsert()
                                }
                                newId
                            }

                            for (chunkFile in files) {
                                val newFileName = "$targetId." + chunkFile.name.substringAfter('.')
                                val targetFile = targetExoDir.resolve(newFileName)
                                if (!targetFile.exists()) {
                                    chunkFile.copyTo(targetFile, overwrite = false)
                                }
                            }
                        }

                        // 6. Merge download cache entries
                        val targetDlTable = "ExoPlayerCacheIndex$activeDlUid"
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS $targetDlTable (" +
                            "id INTEGER PRIMARY KEY NOT NULL, " +
                            "key TEXT NOT NULL, " +
                            "metadata BLOB NOT NULL)"
                        )

                        val existingDlKeyToId = mutableMapOf<String, Int>()
                        val existingDlIds = mutableSetOf<Int>()
                        db.rawQuery("SELECT id, key FROM $targetDlTable", null).use { cursor ->
                            while (cursor.moveToNext()) {
                                val id = cursor.getInt(0)
                                val key = cursor.getString(1)
                                existingDlKeyToId[key] = id
                                existingDlIds.add(id)
                            }
                        }
                        targetDlDir.walkTopDown().forEach { file ->
                            if (file.isFile && file.name.endsWith(".exo")) {
                                file.name.substringBefore('.').toIntOrNull()?.let { existingDlIds.add(it) }
                            }
                        }
                        var maxDlId = existingDlIds.maxOrNull() ?: -1

                        val backupDlFiles = tempDir.walkTopDown().filter { file ->
                            file.isFile && file.name.endsWith(".exo") && file.path.replace('\\', '/').contains("/download/")
                        }.toList()

                        val dlFilesByBackupId = backupDlFiles.groupBy { it.name.substringBefore('.').toIntOrNull() }
                        for ((backupId, files) in dlFilesByBackupId) {
                            if (backupId == null) continue
                            val entry = backupDlEntries[backupId] ?: backupExoEntries[backupId]
                            val songKey = entry?.first
                                ?: songOrderFromMetadata.getOrNull(backupId)
                                ?: restoredSongIds.getOrNull(backupId)
                                ?: "restored_$backupId"
                            val metadata = entry?.second ?: emptyMetadata

                            val targetId = existingDlKeyToId.getOrPut(songKey) {
                                val newId = ++maxDlId
                                tryOrNull {
                                    val stmt = db.compileStatement(
                                        "INSERT OR REPLACE INTO $targetDlTable (id, key, metadata) VALUES (?, ?, ?)"
                                    )
                                    stmt.bindLong(1, newId.toLong())
                                    stmt.bindString(2, songKey)
                                    stmt.bindBlob(3, metadata)
                                    stmt.executeInsert()
                                }
                                newId
                            }

                            for (chunkFile in files) {
                                val newFileName = "$targetId." + chunkFile.name.substringAfter('.')
                                val targetFile = targetDlDir.resolve(newFileName)
                                if (!targetFile.exists()) {
                                    chunkFile.copyTo(targetFile, overwrite = false)
                                }
                            }
                        }

                        tryOrNull {
                            db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                        }

                        // 7. Merge restoredSongIds and existing cached song IDs into restored_cache_ids.json
                        val restoredFile = context.filesDir.resolve("restored_cache_ids.json")
                        val mergedIds = mutableSetOf<String>()
                        if (restoredFile.exists()) {
                            tryOrNull {
                                val arr = JSONArray(restoredFile.readText())
                                for (i in 0 until arr.length()) mergedIds.add(arr.getString(i))
                            }
                        }
                        mergedIds.addAll(restoredSongIds)
                        mergedIds.addAll(existingExoKeyToId.keys)
                        mergedIds.addAll(existingDlKeyToId.keys)
                        tryOrNull {
                            val backupRestoredFile = tempDir.walkTopDown().firstOrNull { it.isFile && it.name == "restored_cache_ids.json" }
                            if (backupRestoredFile != null && backupRestoredFile.exists()) {
                                val arr = JSONArray(backupRestoredFile.readText())
                                for (i in 0 until arr.length()) mergedIds.add(arr.getString(i))
                            }
                        }
                        restoredFile.writeText(JSONArray(mergedIds.toList()).toString())
                    }
                } finally {
                    tempDir.deleteRecursively()
                }

                // Checkpoint database to flush restored Room entries
                tryOrNull { database.checkpoint() }
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.restore_cache_success, Toast.LENGTH_SHORT).show()
                    restartApp(context)
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

