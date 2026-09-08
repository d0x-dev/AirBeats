package com.darkxvenom.airbeats.di

import android.content.Context
import androidx.annotation.Keep
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.darkxvenom.airbeats.constants.MaxSongCacheSizeKey
import com.darkxvenom.airbeats.db.InternalDatabase
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache

@Module
@InstallIn(SingletonComponent::class)
@Keep
object AppModule {
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MusicDatabase = InternalDatabase.newInstance(context)

    @Singleton
    @Provides
    fun provideDatabaseProvider(
        @ApplicationContext context: Context,
    ): DatabaseProvider = StandaloneDatabaseProvider(context)

    @Singleton
    @Provides
    @PlayerCache
    fun providePlayerCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        ensureCacheUidAligned(context, "exoplayer")
        val constructor = {
            SimpleCache(
                context.filesDir.resolve("exoplayer"),
                when (val cacheSize = context.dataStore[MaxSongCacheSizeKey] ?: 1024) {
                    -1 -> NoOpCacheEvictor()
                    else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
                },
                databaseProvider,
            )
        }
        constructor().release()
        return constructor()
    }

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        ensureCacheUidAligned(context, "download")
        val constructor = {
            SimpleCache(context.filesDir.resolve("download"), NoOpCacheEvictor(), databaseProvider)
        }
        constructor().release()
        return constructor()
    }

    fun ensureCacheUidAligned(context: Context, dirName: String) {
        try {
            val dir = context.filesDir.resolve(dirName)
            if (!dir.exists()) return

            // 1. Collect all chunk IDs from .exo files recursively
            val chunkIds = mutableSetOf<Int>()
            try {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && file.name.endsWith(".exo")) {
                        val idPart = file.name.substringBefore('.')
                        idPart.toIntOrNull()?.let { chunkIds.add(it) }
                    }
                }
            } catch (_: Exception) {}

            val dbFile = context.getDatabasePath("exoplayer_internal.db")
            dbFile.parentFile?.mkdirs()

            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
                dbFile.path,
                null
            ).use { db ->
                try {
                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                } catch (_: Exception) {}

                // Ensure ExoPlayerVersions table exists
                try {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS ExoPlayerVersions (" +
                        "feature INTEGER NOT NULL, " +
                        "instance_uid TEXT NOT NULL, " +
                        "version INTEGER NOT NULL, " +
                        "PRIMARY KEY (feature, instance_uid))"
                    )
                } catch (_: Exception) {}

                val tables = mutableListOf<String>()
                try {
                    db.rawQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'ExoPlayerCacheIndex%'",
                        null
                    ).use { cursor ->
                        while (cursor.moveToNext()) {
                            tables.add(cursor.getString(0))
                        }
                    }
                } catch (_: Exception) {}

                var bestHexUid: String? = null
                var maxMatchingRows = -1

                for (table in tables) {
                    val hex = table.removePrefix("ExoPlayerCacheIndex")
                    var matchingScore = 0

                    if (chunkIds.isNotEmpty()) {
                        for (chunkId in chunkIds) {
                            val count = try {
                                db.rawQuery("SELECT count(*) FROM $table WHERE id = $chunkId", null).use { c ->
                                    if (c.moveToFirst()) c.getInt(0) else 0
                                }
                            } catch (_: Exception) { 0 }
                            if (count > 0) matchingScore++
                        }
                    }

                    val totalRows = try {
                        db.rawQuery("SELECT count(*) FROM $table", null).use { c ->
                            if (c.moveToFirst()) c.getInt(0) else 0
                        }
                    } catch (_: Exception) {}

                    val finalScore = if (matchingScore > 0) matchingScore * 1000 + totalRows else totalRows

                    if (finalScore > maxMatchingRows) {
                        maxMatchingRows = finalScore
                        bestHexUid = hex
                    }
                }

                // If no bestHexUid found from existing tables, check if there is an existing .uid file in dir
                if (bestHexUid.isNullOrEmpty()) {
                    val existingUidFile = dir.listFiles { _, name -> name.endsWith(".uid") }?.firstOrNull()
                    if (existingUidFile != null) {
                        bestHexUid = existingUidFile.name.removeSuffix(".uid")
                    } else {
                        // Generate a valid 16-hex-digit UID
                        bestHexUid = java.lang.Long.toHexString(java.security.SecureRandom().nextLong())
                    }
                }

                val targetTable = "ExoPlayerCacheIndex$bestHexUid"
                try {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS $targetTable (" +
                        "id INTEGER PRIMARY KEY NOT NULL, " +
                        "key TEXT NOT NULL, " +
                        "metadata BLOB NOT NULL)"
                    )
                } catch (_: Exception) {}

                // Register version = 1 for this instance_uid so ExoPlayer won't drop the table on startup!
                try {
                    db.execSQL(
                        "INSERT OR REPLACE INTO ExoPlayerVersions (feature, instance_uid, version) VALUES (1, '$bestHexUid', 1)"
                    )
                } catch (_: Exception) {}

                // If table is missing entries for any of our chunkIds, populate them so ExoPlayer never deletes .exo files!
                if (chunkIds.isNotEmpty()) {
                    val existingIds = mutableSetOf<Int>()
                    try {
                        db.rawQuery("SELECT id FROM $targetTable", null).use { c ->
                            while (c.moveToNext()) {
                                existingIds.add(c.getInt(0))
                            }
                        }
                    } catch (_: Exception) {}

                    val missingIds = chunkIds.filter { !existingIds.contains(it) }.sorted()
                    if (missingIds.isNotEmpty()) {
                        val restoredSongs = mutableListOf<String>()
                        try {
                            val restoredFile = context.filesDir.resolve("restored_cache_ids.json")
                            if (restoredFile.exists()) {
                                val jsonArr = org.json.JSONArray(restoredFile.readText())
                                for (i in 0 until jsonArr.length()) {
                                    restoredSongs.add(jsonArr.getString(i))
                                }
                            }
                        } catch (_: Exception) {}

                        if (restoredSongs.isEmpty()) {
                            val airbeatsDb = context.getDatabasePath("airbeats.db")
                            if (airbeatsDb.exists()) {
                                try {
                                    android.database.sqlite.SQLiteDatabase.openDatabase(
                                        airbeatsDb.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                                    ).use { rdb ->
                                        rdb.rawQuery("SELECT id FROM song", null).use { sc ->
                                            while (sc.moveToNext()) {
                                                restoredSongs.add(sc.getString(0))
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }

                        val emptyMetadata = byteArrayOf(0, 0, 0, 0)
                        for (idx in missingIds.indices) {
                            val chunkId = missingIds[idx]
                            val songKey = restoredSongs.getOrNull(idx) ?: restoredSongs.getOrNull(chunkId) ?: "restored_$chunkId"
                            try {
                                val statement = db.compileStatement(
                                    "INSERT OR IGNORE INTO $targetTable (id, key, metadata) VALUES (?, ?, ?)"
                                )
                                statement.bindLong(1, chunkId.toLong())
                                statement.bindString(2, songKey)
                                statement.bindBlob(3, emptyMetadata)
                                statement.executeInsert()
                            } catch (_: Exception) {}
                        }
                    }
                }

                // Ensure the dir has matching <bestHexUid>.uid and delete other .uid files
                val uidFiles = dir.listFiles { _, name -> name.endsWith(".uid") } ?: emptyArray()
                val targetName = "$bestHexUid.uid"
                var targetExists = false
                for (f in uidFiles) {
                    if (f.name.equals(targetName, ignoreCase = true)) {
                        targetExists = true
                    } else {
                        f.delete()
                    }
                }
                if (!targetExists) {
                    dir.resolve(targetName).createNewFile()
                }

                try {
                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
