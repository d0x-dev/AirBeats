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
            val dbFile = context.getDatabasePath("exoplayer_internal.db")
            if (!dbFile.exists()) return

            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                try {
                    db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
                } catch (_: Exception) {}

                val tables = mutableListOf<String>()
                db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE 'ExoPlayerCacheIndex%'",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        tables.add(cursor.getString(0))
                    }
                }

                if (tables.isEmpty()) return

                // Scan files in dir to find any cached chunk IDs (e.g. "0.0.1234.v3.exo" -> id = 0)
                val chunkIds = mutableSetOf<Int>()
                dir.listFiles()?.forEach { file ->
                    val name = file.name
                    if (name.endsWith(".exo")) {
                        val idPart = name.substringBefore('.')
                        idPart.toIntOrNull()?.let { chunkIds.add(it) }
                    }
                }

                var bestHexUid: String? = null
                var maxMatchingRows = -1

                for (table in tables) {
                    val hex = table.removePrefix("ExoPlayerCacheIndex")
                    var matchingScore = 0

                    if (chunkIds.isNotEmpty()) {
                        for (chunkId in chunkIds.take(10)) {
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
                    } catch (_: Exception) { 0 }

                    val finalScore = if (matchingScore > 0) matchingScore * 1000 + totalRows else totalRows

                    if (finalScore > maxMatchingRows) {
                        maxMatchingRows = finalScore
                        bestHexUid = hex
                    }
                }

                if (!bestHexUid.isNullOrEmpty()) {
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
                }
            }
        } catch (_: Exception) {}
    }
}
