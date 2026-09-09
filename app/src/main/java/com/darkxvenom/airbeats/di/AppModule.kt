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

            // 1. Get or establish the active UID from the existing .uid file in dir (NEVER delete the active UID!)
            val existingUidFiles = dir.listFiles { _, name -> name.endsWith(".uid") } ?: emptyArray()
            val activeHexUid = if (existingUidFiles.isNotEmpty()) {
                existingUidFiles.first().name.removeSuffix(".uid")
            } else {
                val newUid = java.lang.Long.toHexString(java.security.SecureRandom().nextLong())
                dir.resolve("$newUid.uid").createNewFile()
                newUid
            }

            // Remove any extra conflicting .uid files so ExoPlayer is never confused
            val activeUidFileName = "$activeHexUid.uid"
            for (f in existingUidFiles) {
                if (!f.name.equals(activeUidFileName, ignoreCase = true)) {
                    f.delete()
                }
            }

            val dbFile = context.getDatabasePath("exoplayer_internal.db")
            dbFile.parentFile?.mkdirs()

            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(
                dbFile.path,
                null
            ).use { db ->
                try {
                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                } catch (_: Exception) {}

                // Ensure ExoPlayerVersions table exists and has activeHexUid
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS ExoPlayerVersions (" +
                    "feature INTEGER NOT NULL, " +
                    "instance_uid TEXT NOT NULL, " +
                    "version INTEGER NOT NULL, " +
                    "PRIMARY KEY (feature, instance_uid))"
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO ExoPlayerVersions (feature, instance_uid, version) VALUES (1, '$activeHexUid', 1)"
                )

                val targetTable = "ExoPlayerCacheIndex$activeHexUid"
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS $targetTable (" +
                    "id INTEGER PRIMARY KEY NOT NULL, " +
                    "key TEXT NOT NULL, " +
                    "metadata BLOB NOT NULL)"
                )

                // Collect all chunk IDs from .exo files in dir and ensure targetTable has entries for all of them
                val chunkIds = mutableSetOf<Int>()
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && file.name.endsWith(".exo")) {
                        file.name.substringBefore('.').toIntOrNull()?.let { chunkIds.add(it) }
                    }
                }

                if (chunkIds.isNotEmpty()) {
                    val existingIds = mutableSetOf<Int>()
                    val existingKeys = mutableSetOf<String>()
                    try {
                        db.rawQuery("SELECT id, key FROM $targetTable", null).use { c ->
                            while (c.moveToNext()) {
                                existingIds.add(c.getInt(0))
                                existingKeys.add(c.getString(1))
                            }
                        }
                    } catch (_: Exception) {}

                    val missingIds = chunkIds.filter { !existingIds.contains(it) }.sorted()
                    if (missingIds.isNotEmpty()) {
                        val otherTables = mutableListOf<String>()
                        try {
                            db.rawQuery(
                                "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'ExoPlayerCacheIndex%' AND name != '$targetTable'",
                                null
                            ).use { c ->
                                while (c.moveToNext()) otherTables.add(c.getString(0))
                            }
                        } catch (_: Exception) {}

                        val recoveredFromOtherTables = mutableSetOf<Int>()
                        for (otherTbl in otherTables) {
                            for (mId in missingIds) {
                                if (recoveredFromOtherTables.contains(mId)) continue
                                try {
                                    db.rawQuery("SELECT key, metadata FROM $otherTbl WHERE id = $mId", null).use { c ->
                                        if (c.moveToNext()) {
                                            val k = c.getString(0)
                                            val m = c.getBlob(1)
                                            if (!existingKeys.contains(k)) {
                                                val stmt = db.compileStatement(
                                                    "INSERT OR REPLACE INTO $targetTable (id, key, metadata) VALUES (?, ?, ?)"
                                                )
                                                stmt.bindLong(1, mId.toLong())
                                                stmt.bindString(2, k)
                                                stmt.bindBlob(3, m)
                                                stmt.executeInsert()
                                                existingKeys.add(k)
                                                existingIds.add(mId)
                                                recoveredFromOtherTables.add(mId)
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }

                        val stillMissingIds = missingIds.filter { !recoveredFromOtherTables.contains(it) }
                        if (stillMissingIds.isNotEmpty()) {
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
                                val songDb = context.getDatabasePath(com.darkxvenom.airbeats.db.InternalDatabase.DB_NAME)
                                if (songDb.exists()) {
                                    try {
                                        android.database.sqlite.SQLiteDatabase.openDatabase(
                                            songDb.path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
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

                            val availableSongs = restoredSongs.filter { !existingKeys.contains(it) }.toMutableList()
                            val emptyMetadata = byteArrayOf(0, 0, 0, 0)
                            for (chunkId in stillMissingIds) {
                                val songKey = if (availableSongs.isNotEmpty()) availableSongs.removeAt(0) else "restored_$chunkId"
                                existingKeys.add(songKey)
                                try {
                                    val statement = db.compileStatement(
                                        "INSERT OR REPLACE INTO $targetTable (id, key, metadata) VALUES (?, ?, ?)"
                                    )
                                    statement.bindLong(1, chunkId.toLong())
                                    statement.bindString(2, songKey)
                                    statement.bindBlob(3, emptyMetadata)
                                    statement.executeInsert()
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }

                try {
                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
