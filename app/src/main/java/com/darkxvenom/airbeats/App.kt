package com.darkxvenom.airbeats

import android.app.Application
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.darkxvenom.airbeats.ui.component.LocaleAwareApplication
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.initializeCache
import android.widget.Toast.LENGTH_SHORT
import androidx.datastore.preferences.core.edit
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.YouTubeLocale
import com.darkxvenom.airbeats.kugou.KuGou
import com.darkxvenom.airbeats.constants.AccountChannelHandleKey
import com.darkxvenom.airbeats.constants.AccountEmailKey
import com.darkxvenom.airbeats.constants.AccountNameKey
import com.darkxvenom.airbeats.constants.ContentCountryKey
import com.darkxvenom.airbeats.constants.ContentLanguageKey
import com.darkxvenom.airbeats.constants.CountryCodeToName
import com.darkxvenom.airbeats.constants.DataSyncIdKey
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.constants.LanguageCodeToName
import com.darkxvenom.airbeats.constants.MaxImageCacheSizeKey
import com.darkxvenom.airbeats.constants.ProxyEnabledKey
import com.darkxvenom.airbeats.constants.ProxyTypeKey
import com.darkxvenom.airbeats.constants.ProxyUrlKey
import com.darkxvenom.airbeats.constants.SYSTEM_DEFAULT
import com.darkxvenom.airbeats.constants.UseLoginForBrowse
import com.darkxvenom.airbeats.constants.VisitorDataKey
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.extensions.toEnum
import com.darkxvenom.airbeats.extensions.toInetSocketAddress
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.utils.AirBeatsStatsCloudSync
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get
import com.darkxvenom.airbeats.utils.reportException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.Proxy
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class App : LocaleAwareApplication(), ImageLoaderFactory {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var namePreferenceManager: NamePreferenceManager

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this
        kotlinx.coroutines.runBlocking {
            runCatching { dataStore.initializeCache() }
        }
        Timber.plant(com.darkxvenom.airbeats.utils.GlobalLogTree())

        try {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    val sw = java.io.StringWriter()
                    val pw = java.io.PrintWriter(sw)
                    throwable.printStackTrace(pw)
                    val stack = sw.toString()

                    val intent = android.content.Intent(this@App, com.darkxvenom.airbeats.ui.activities.DebugActivity::class.java).apply {
                        putExtra(com.darkxvenom.airbeats.ui.activities.DebugActivity.EXTRA_STACK_TRACE, stack)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
                    try { Thread.sleep(100) } catch (_: InterruptedException) {}
                } catch (e: Exception) {
                    reportException(e)
                } finally {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(2)
                }
            }
        } catch (e: Exception) {
            reportException(e)
        }

        val locale = Locale.getDefault()
        val languageTag = locale.toLanguageTag().replace("-Hant", "") // replace zh-Hant-* to zh-*
        YouTube.locale = YouTubeLocale(
            gl = dataStore[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.country.takeIf { it in CountryCodeToName }
                ?: "US",
            hl = dataStore[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT }
                ?: locale.language.takeIf { it in LanguageCodeToName }
                ?: languageTag.takeIf { it in LanguageCodeToName }
                ?: "en"
        )
        if (languageTag == "zh-TW") {
            KuGou.useTraditionalChinese = true
        }

        if (dataStore[ProxyEnabledKey] == true) {
            try {
                YouTube.proxy = Proxy(
                    dataStore[ProxyTypeKey].toEnum(defaultValue = Proxy.Type.HTTP),
                    dataStore[ProxyUrlKey]!!.toInetSocketAddress()
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to parse proxy url.", LENGTH_SHORT).show()
                reportException(e)
            }
        }

        if (dataStore[UseLoginForBrowse] != false) {
            YouTube.useLoginForBrowse = true
        }

        GlobalScope.launch {
            AirBeatsStatsCloudSync.syncDaily(
                context = this@App,
                database = database,
                namePreferenceManager = namePreferenceManager,
            )?.onFailure(::reportException)
        }

        GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                val email = namePreferenceManager.accountEmail.first().ifBlank {
                    dataStore[AccountEmailKey] ?: ""
                }
                val name = namePreferenceManager.userName.first().ifBlank { "AirBeats User" }

                val automaticCloudBackupEnabled = getSharedPreferences("backup_settings", Context.MODE_PRIVATE)
                    .getBoolean("enable_cloud_upload", true)

                if (automaticCloudBackupEnabled && email.isNotBlank()) {
                    Timber.i("App launch: Starting automatic cloud backup upload for $email")
                    val backupViewModel = com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel(com.darkxvenom.airbeats.db.InternalDatabase.newInstance(this@App))
                    val result = backupViewModel.backupToDrive(this@App, email, name)
                    if (result is com.darkxvenom.airbeats.utils.DriveResult.Success) {
                        dataStore.edit { preferences ->
                            preferences[com.darkxvenom.airbeats.constants.LastBackupTimestampKey] = System.currentTimeMillis()
                        }
                        Timber.i("App launch: Cloud backup upload completed successfully for $email")
                    } else {
                        Timber.e("App launch: Cloud backup upload failed for $email")
                    }

                    // Schedule periodic 24-hour backup worker
                    val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.darkxvenom.airbeats.worker.DailyBackupWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                        .setConstraints(
                            androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .build()
                        )
                        .build()

                    androidx.work.WorkManager.getInstance(this@App).enqueueUniquePeriodicWork(
                        "DailyBackupWorker",
                        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "App launch: Error during automatic cloud backup")
            }
        }

        GlobalScope.launch {
            dataStore.data
                .map { it[VisitorDataKey] }
                .distinctUntilChanged()
                .collect { visitorData ->
                    YouTube.visitorData = visitorData
                        ?.takeIf { it != "null" } // Previously visitorData was sometimes saved as "null" due to a bug
                        ?: YouTube.visitorData().onFailure {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@App, "Failed to get visitorData.", LENGTH_SHORT)
                                    .show()
                            }
                            reportException(it)
                        }.getOrNull()?.also { newVisitorData ->
                            dataStore.edit { settings ->
                                settings[VisitorDataKey] = newVisitorData
                            }
                        }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[DataSyncIdKey] }
                .distinctUntilChanged()
                .collect { dataSyncId ->
                    YouTube.dataSyncId = dataSyncId?.let {
                        /*
                         * Workaround to avoid breaking older installations that have a dataSyncId
                         * that contains "||" in it.
                         * If the dataSyncId ends with "||" and contains only one id, then keep the
                         * id before the "||".
                         * If the dataSyncId contains "||" and is not at the end, then keep the
                         * second id.
                         * This is needed to keep using the same account as before.
                         */
                        it.takeIf { !it.contains("||") }
                            ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                            ?: it.substringAfter("||")
                    }
                }
        }
        GlobalScope.launch {
            dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    try {
                        YouTube.cookie = cookie
                    } catch (e: Exception) {
                        // we now allow user input now, here be the demons. This serves as a last ditch effort to avoid a crash loop
                        Timber.e("Could not parse cookie. Clearing existing cookie. %s", e.message)
                        forgetAccount(this@App)
                    }
                }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val cacheSize = dataStore[MaxImageCacheSizeKey]

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .dispatcher(okhttp3.Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 16
            })
            .connectionPool(okhttp3.ConnectionPool(8, 5, java.util.concurrent.TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .build()

        val builder = ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(150)
            .respectCacheHeaders(false)
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)

        if (cacheSize == 0) {
            return builder
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        }

        return builder
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil"))
                    .maxSizeBytes((cacheSize ?: 512) * 1024 * 1024L)
                    .build()
            }
            .build()
    }

    companion object {
        lateinit var instance: App
            private set

        fun forgetAccount(context: Context) {
            runBlocking {
                context.dataStore.edit { settings ->
                    settings.remove(InnerTubeCookieKey)
                    settings.remove(VisitorDataKey)
                    settings.remove(DataSyncIdKey)
                    settings.remove(AccountNameKey)
                    settings.remove(AccountEmailKey)
                    settings.remove(AccountChannelHandleKey)
                }
            }
        }
    }
}
