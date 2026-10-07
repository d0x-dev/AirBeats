package com.darkxvenom.airbeats

import android.Manifest
import com.darkxvenom.airbeats.ui.component.LocalUserName
import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.core.app.ActivityCompat
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import com.darkxvenom.airbeats.utils.AutoBackupManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import android.content.ActivityNotFoundException
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.animation.core.animateFloatAsState
import android.content.ComponentName
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.statusBarsPadding
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.darkxvenom.airbeats.ui.component.CircleIconButton
import com.darkxvenom.airbeats.ui.component.SwipeBackContainer
import com.darkxvenom.airbeats.ui.component.tabSwipeGesture
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darkxvenom.airbeats.viewmodels.RingtoneViewModel
import com.darkxvenom.airbeats.ui.component.RingtoneTrimmerDialog
import com.darkxvenom.airbeats.ui.component.RingtoneProgressDialog
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import com.darkxvenom.airbeats.utils.ExternalPlayerUtil
import com.darkxvenom.airbeats.utils.ListenTogetherSync
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.constants.AppBarHeight
import com.darkxvenom.airbeats.constants.DarkModeKey
import com.darkxvenom.airbeats.constants.DefaultOpenTabKey
import com.darkxvenom.airbeats.constants.DisableScreenshotKey
import com.darkxvenom.airbeats.constants.DynamicThemeKey
import com.darkxvenom.airbeats.constants.MiniPlayerHeight
import com.darkxvenom.airbeats.constants.NavigationBarAnimationSpec
import com.darkxvenom.airbeats.constants.NavigationBarHeight
import com.darkxvenom.airbeats.constants.PauseSearchHistoryKey
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyle
import androidx.compose.foundation.Image
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyleKey
import com.darkxvenom.airbeats.constants.PureBlackKey
import com.darkxvenom.airbeats.constants.SearchSource
import com.darkxvenom.airbeats.constants.SearchSourceKey
import com.darkxvenom.airbeats.constants.SlimNavBarKey
import com.darkxvenom.airbeats.constants.StopMusicOnTaskClearKey
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.db.entities.SearchHistory
import com.darkxvenom.airbeats.extensions.toEnum
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.DownloadUtil
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.MusicService.MusicBinder
import com.darkxvenom.airbeats.playback.PlayerConnection
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.BottomSheet
import com.darkxvenom.airbeats.ui.component.BottomSheetMenu
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.constants.LiquidGlassKey
import com.darkxvenom.airbeats.constants.FrostedGlassCardsButtonsKey
import com.darkxvenom.airbeats.constants.UseSystemFontKey
import com.darkxvenom.airbeats.constants.AppFont
import com.darkxvenom.airbeats.constants.AppFontKey
import com.darkxvenom.airbeats.ui.component.CurvedBottomNavigationItem
import com.darkxvenom.airbeats.ui.component.LocalMenuState
import com.darkxvenom.airbeats.ui.component.rememberBackdrop
import com.darkxvenom.airbeats.ui.component.layerBackdrop
import com.darkxvenom.airbeats.ui.component.LocalBackdrop
import com.darkxvenom.airbeats.ui.component.LiquidGlassBottomNavigationBar
import com.darkxvenom.airbeats.ui.component.LocaleManager
import com.darkxvenom.airbeats.ui.component.Lyrics
import com.darkxvenom.airbeats.ui.component.SpotifyLyrics
import com.darkxvenom.airbeats.constants.PlayerScreenStyleKey
import com.darkxvenom.airbeats.constants.PlayerScreenStyle
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.constants.HomeScreenStyle
import com.darkxvenom.airbeats.constants.HomeScreenStyleKey
import com.darkxvenom.airbeats.constants.NavBarStyle
import com.darkxvenom.airbeats.constants.NavBarStyleKey
import com.darkxvenom.airbeats.ui.component.NameProvider
import com.darkxvenom.airbeats.ui.component.SwitchPreference
import com.darkxvenom.airbeats.ui.component.TopSearch
import com.darkxvenom.airbeats.ui.component.rememberBottomSheetState
import com.darkxvenom.airbeats.ui.component.shimmer.ShimmerTheme
import com.darkxvenom.airbeats.ui.menu.YouTubeSongMenu
import com.darkxvenom.airbeats.ui.player.BottomSheetPlayer
import com.darkxvenom.airbeats.ui.screens.HomeScreen
import com.darkxvenom.airbeats.ui.screens.Screens
import com.darkxvenom.airbeats.ui.screens.navigationBuilder
import com.darkxvenom.airbeats.ui.screens.search.LocalSearchScreen
import com.darkxvenom.airbeats.ui.screens.search.OnlineSearchScreen
import com.darkxvenom.airbeats.ui.screens.settings.DarkMode
import com.darkxvenom.airbeats.ui.screens.settings.NavigationTab
import com.darkxvenom.airbeats.ui.theme.ColorSaver
import com.darkxvenom.airbeats.ui.theme.DefaultThemeColor
import com.darkxvenom.airbeats.ui.theme.AirBeatsTheme
import com.darkxvenom.airbeats.ui.theme.extractThemeColor
import com.darkxvenom.airbeats.ui.utils.appBarScrollBehavior
import com.darkxvenom.airbeats.constants.ReduceAnimationsKey
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.ui.utils.resetHeightOffset
import com.darkxvenom.airbeats.ui.component.UpdateAvailableDialog
import com.darkxvenom.airbeats.utils.UpdateInfo
import com.darkxvenom.airbeats.utils.SyncUtils
import com.darkxvenom.airbeats.utils.Updater
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get
import com.darkxvenom.airbeats.utils.rememberEnumPreference
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.utils.reportException
import com.darkxvenom.airbeats.viewmodels.NewReleaseViewModel
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.days
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.darkxvenom.airbeats.ui.component.RankPreferenceManager
import com.darkxvenom.airbeats.ui.component.RankUpPopup
import com.darkxvenom.airbeats.ui.component.AirBeatsRank
import com.darkxvenom.airbeats.ui.component.RankBadge
import androidx.hilt.navigation.compose.hiltViewModel
import com.darkxvenom.airbeats.viewmodels.StatsViewModel
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.pointerInput
import com.darkxvenom.airbeats.constants.AodAutoActivationKey
import kotlinx.coroutines.isActive

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var namePreferenceManager: NamePreferenceManager

    @Inject
    lateinit var lastFmAuthCallbackCoordinator: com.darkxvenom.airbeats.data.repository.LastFmAuthCallbackCoordinator

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isServiceBound = false
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private var latestVersionName by mutableStateOf<String>(BuildConfig.VERSION_NAME)

    override fun onStart() {
        super.onStart()
        com.darkxvenom.airbeats.playback.AppForegroundTracker.isForeground = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            runCatching {
                startService(Intent(this, MusicService::class.java))
            }.onFailure { Timber.e(it, "Failed to start MusicService from MainActivity") }
        }
        if (!isServiceBound) {
            runCatching {
                bindService(
                    Intent(this, MusicService::class.java),
                    serviceConnection,
                    Context.BIND_AUTO_CREATE
                )
            }.onSuccess { isServiceBound = it }
             .onFailure { Timber.e(it, "Failed to bind MusicService from MainActivity") }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                database.checkpoint()
                android.app.backup.BackupManager(this@MainActivity).dataChanged()
            }
        }
    }

    override fun onStop() {
        com.darkxvenom.airbeats.playback.AppForegroundTracker.isForeground = false
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                database.checkpoint()
                android.app.backup.BackupManager(this@MainActivity).dataChanged()
            }
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val enabled = dataStore.get(com.darkxvenom.airbeats.constants.DynamicIslandKey, false)
            if (enabled && android.provider.Settings.canDrawOverlays(this@MainActivity)) {
                try {
                    startService(android.content.Intent(this@MainActivity, com.darkxvenom.airbeats.playback.DynamicIslandService::class.java))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(
                StopMusicOnTaskClearKey,
                false
            ) && playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
        }
        playerConnection = null
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleManager.getInstance(newBase).applyLocaleToContext(newBase)
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lastFmAuthCallbackCoordinator.capture(intent)
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        lastFmAuthCallbackCoordinator.capture(intent)

        // 🔔 Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )
        }

        // 🔥 Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_TOKEN", "Token obtained")
            } else {
                Log.e("FCM_TOKEN", "Token failed")
            }
        }

        // 🔥 Subscribe all users
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("FCM", "Subscribed to all_users")
                } else {
                    Log.e("FCM", "Subscription failed")
                }
            }

        // 🔥 Version-specific FCM topic synchronization & auto-cleanup of outdated versions
        val versionName = BuildConfig.VERSION_NAME
        val fcmPrefs = getSharedPreferences("airbeats_fcm_prefs", Context.MODE_PRIVATE)
        val lastSubscribedVersion = fcmPrefs.getString("last_subscribed_version", null)

        if (!lastSubscribedVersion.isNullOrBlank() && lastSubscribedVersion != versionName) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(lastSubscribedVersion)
                .addOnSuccessListener {
                    Log.d("FCM", "Unsubscribed from outdated version topic: $lastSubscribedVersion")
                }
            FirebaseMessaging.getInstance().unsubscribeFromTopic("v$lastSubscribedVersion")
            FirebaseMessaging.getInstance().unsubscribeFromTopic("${lastSubscribedVersion}-nightly")
            FirebaseMessaging.getInstance().unsubscribeFromTopic("v${lastSubscribedVersion}-nightly")
        }

        // Proactively unsubscribe from legacy version tags if updating from older releases
        val legacyVersions = listOf(
            "6.0.0", "6.0.1", "6.0.2", "6.0.3", "6.0.4", "6.1.0", "6.1.1", "6.1.2"
        )
        for (legacy in legacyVersions) {
            if (legacy != versionName) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(legacy)
                FirebaseMessaging.getInstance().unsubscribeFromTopic("v$legacy")
                FirebaseMessaging.getInstance().unsubscribeFromTopic("$legacy-nightly")
                FirebaseMessaging.getInstance().unsubscribeFromTopic("v$legacy-nightly")
            }
        }

        FirebaseMessaging.getInstance().subscribeToTopic(versionName)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d("FCM", "Subscribed to $versionName")
                    fcmPrefs.edit().putString("last_subscribed_version", versionName).apply()
                }
            }

        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        lifecycleScope.launch {
            dataStore.data
                .map { (try { it[com.darkxvenom.airbeats.constants.AiRecommendationsKey] } catch (e: Exception) { null }) ?: false }
                .distinctUntilChanged()
                .collectLatest { enabled ->
                    val workManager = androidx.work.WorkManager.getInstance(this@MainActivity)
                    if (enabled) {
                        val request = androidx.work.PeriodicWorkRequestBuilder<com.darkxvenom.airbeats.ai.AiRecommendationWorker>(1, java.util.concurrent.TimeUnit.DAYS)
                            .setConstraints(
                                androidx.work.Constraints.Builder()
                                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                    .build()
                            )
                            .build()
                        workManager.enqueueUniquePeriodicWork(
                            "AiRecommendationWorker",
                            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                            request
                        )
                    } else {
                        workManager.cancelUniqueWork("AiRecommendationWorker")
                    }
                }
        }

        setContent {
            var updateInfoState by remember { mutableStateOf<UpdateInfo?>(null) }
            
            LaunchedEffect(Unit) {
                Updater.getLatestUpdateInfo().onSuccess { info ->
                    latestVersionName = info.versionName
                    if (info.versionName.isNotBlank() && com.darkxvenom.airbeats.utils.VersionUtils.isVersionGreater(info.versionName, BuildConfig.VERSION_NAME)) {
                        updateInfoState = info
                    }
                }
            }

            val isNameSet by namePreferenceManager.isNameSet.collectAsState(initial = null)
            var showSplash by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                AutoBackupManager.resetRestartAttempts(this@MainActivity)
                delay(1000)
                showSplash = false
            }

            // Fail-safe watchdog: ensure splash screen never hangs permanently
            LaunchedEffect(Unit) {
                delay(3000)
                if (showSplash) {
                    timber.log.Timber.w("Splash screen safety watchdog triggered: auto-dismissing splash")
                    showSplash = false
                }
            }

            val settingsEmail by rememberPreference(com.darkxvenom.airbeats.constants.AccountEmailKey, "")
            val managerEmail by namePreferenceManager.accountEmail.collectAsState(initial = "")
            val effectiveEmail = settingsEmail.ifBlank { managerEmail }
            val (_, setLastBackupTimestamp) = rememberPreference(com.darkxvenom.airbeats.constants.LastBackupTimestampKey, 0L)
            val backupViewModel = com.darkxvenom.airbeats.ui.utils.safeHiltViewModel<com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel>()
            val context = androidx.compose.ui.platform.LocalContext.current
            val userName by namePreferenceManager.userName.collectAsState(initial = "AirBeats User")
            var showFullscreenLyrics by remember { mutableStateOf(false) }

            val playerScreenStyle by rememberEnumPreference<PlayerScreenStyle>(PlayerScreenStyleKey, defaultValue = PlayerScreenStyle.IOS_STYLED)
            val homeScreenStyle by rememberEnumPreference(HomeScreenStyleKey, defaultValue = HomeScreenStyle.CLASSIC)
            val navBarStyle by rememberEnumPreference(NavBarStyleKey, defaultValue = NavBarStyle.APPLE)
            val enableNewLyricsScreen by rememberPreference(com.darkxvenom.airbeats.constants.EnableNewLyricsScreenKey, defaultValue = true)
            val lyricsScreenStyle by rememberEnumPreference(com.darkxvenom.airbeats.constants.LyricsScreenStyleKey, defaultValue = com.darkxvenom.airbeats.constants.LyricsScreenStyle.LYRICS_2)

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val themeAccentColor by rememberPreference(com.darkxvenom.airbeats.constants.ThemeAccentColorKey, defaultValue = 0xFF4285F4.toInt())
            val themeColorEffectKey by rememberPreference(com.darkxvenom.airbeats.constants.ThemeColorEffectKey, defaultValue = com.darkxvenom.airbeats.constants.ThemeColorEffect.NONE.name)
            val themeColorEffect = remember(themeColorEffectKey) {
                try {
                    com.darkxvenom.airbeats.constants.ThemeColorEffect.valueOf(themeColorEffectKey)
                } catch (e: Exception) {
                    com.darkxvenom.airbeats.constants.ThemeColorEffect.NONE
                }
            }
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val enableLiquidGlass by rememberPreference(LiquidGlassKey, defaultValue = false)
            val frostedGlassCardsButtons by rememberPreference(FrostedGlassCardsButtonsKey, defaultValue = true)

            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val appFontKey by rememberPreference(AppFontKey, defaultValue = AppFont.LINOTTE.key)
            val appFont = remember(appFontKey) { AppFont.fromKey(appFontKey) }
            val isPlayful = homeScreenStyle == HomeScreenStyle.PLAYFUL
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme, enableLiquidGlass, frostedGlassCardsButtons, isPlayful) {
                    if (isPlayful) {
                        false
                    } else if (enableLiquidGlass || frostedGlassCardsButtons) {
                        true
                    } else {
                        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                    }
                }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            val (isVoiceAssistantEnabled) = rememberPreference(com.darkxvenom.airbeats.constants.EnableVoiceAssistantKey, defaultValue = false)
            LaunchedEffect(isVoiceAssistantEnabled) {
                if (isVoiceAssistantEnabled) {
                    runCatching { com.darkxvenom.airbeats.voice.VoiceAssistantService.start(this@MainActivity) }
                } else {
                    runCatching { com.darkxvenom.airbeats.voice.VoiceAssistantService.stop(this@MainActivity) }
                }
            }
            var dynamicColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    dynamicColor = DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    dynamicColor =
                        if (song != null) {
                            withContext(Dispatchers.IO) {
                                val result =
                                    imageLoader.execute(
                                        ImageRequest
                                            .Builder(this@MainActivity)
                                            .data(song.thumbnailUrl)
                                            .allowHardware(false)
                                            .build(),
                                    )
                                (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor()
                                    ?: DefaultThemeColor
                            }
                        } else {
                            DefaultThemeColor
                        }
                }
            }

            val effectiveThemeColor = if (enableDynamicTheme) dynamicColor else Color(themeAccentColor)

            AirBeatsTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack && !enableLiquidGlass && !frostedGlassCardsButtons && !isPlayful,
                appFont = appFont,
                themeColor = effectiveThemeColor,
                colorEffect = themeColorEffect,
                enableDynamicTheme = enableDynamicTheme,
            ) {
                val rankPrefMgr = remember { RankPreferenceManager(this@MainActivity) }
                val lastSeenRank by rankPrefMgr.lastSeenRank.collectAsState(initial = null)
                val statsViewModel = com.darkxvenom.airbeats.ui.utils.safeHiltViewModel<StatsViewModel>()
                val totalHours by (statsViewModel?.totalListenHours ?: kotlinx.coroutines.flow.flowOf(0.0)).collectAsState(initial = 0.0)
                val currentRank = remember(totalHours) {
                    if (totalHours >= 1.0) AirBeatsRank.fromHours(totalHours.toInt()) else null
                }
                var activeRankUpPopup by remember { mutableStateOf<AirBeatsRank?>(null) }

                LaunchedEffect(currentRank, lastSeenRank) {
                    if (currentRank != null && lastSeenRank != currentRank) {
                        activeRankUpPopup = currentRank
                    }
                }

                activeRankUpPopup?.let { rank ->
                    val popupScope = rememberCoroutineScope()
                    RankUpPopup(
                        newRank = rank,
                        onDismiss = {
                            popupScope.launch {
                                rankPrefMgr.saveLastSeenRank(rank)
                            }
                            activeRankUpPopup = null
                        }
                    )
                }

                val backdrop = rememberBackdrop()

                if (showSplash) {
                    HeadphoneSplashScreen(statusText = null)
                } else {

                    NameProvider(
                        namePreferenceManager = namePreferenceManager
                    ) {
                        BoxWithConstraints(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent),
                        )
                        {
                            val focusManager = LocalFocusManager.current
                            val density = LocalDensity.current
                            val windowsInsets = WindowInsets.systemBars
                            val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                            val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                            val navController = rememberNavController()
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val (previousTab) = rememberSaveable { mutableStateOf("home") }

                            val navigationItems = remember(homeScreenStyle, navBarStyle, enableLiquidGlass) { 
                                if (navBarStyle == NavBarStyle.LIQUID_GLASS || enableLiquidGlass) {
                                    listOf(Screens.Home, Screens.Search, Screens.Explore, Screens.Library)
                                } else {
                                    when (navBarStyle) {
                                        NavBarStyle.LIQUID_GLASS -> listOf(Screens.Home, Screens.Search, Screens.Explore, Screens.Library)
                                        NavBarStyle.SPOTIFY -> listOf(Screens.Home, Screens.Search, Screens.Explore, Screens.Library)
                                        NavBarStyle.APPLE -> listOf(Screens.Home, Screens.Stats, Screens.Explore, Screens.Library, Screens.Search)
                                        NavBarStyle.NEW_CLASSIC -> listOf(Screens.Home, Screens.Search, Screens.Explore, Screens.Library)
                                        NavBarStyle.MATERIAL -> listOf(Screens.Home, Screens.Explore, Screens.Library, Screens.Search)
                                    }
                                }
                            }
                            val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                            val (reduceAnimations) = rememberPreference(ReduceAnimationsKey, defaultValue = false)
                            val defaultOpenTab by rememberEnumPreference(
                                DefaultOpenTabKey,
                                defaultValue = NavigationTab.HOME,
                            )
                            val tabOpenedFromShortcut =
                                remember {
                                    when (intent?.action) {
                                        ACTION_LIBRARY -> NavigationTab.LIBRARY
                                        ACTION_EXPLORE -> NavigationTab.EXPLORE
                                        else -> null
                                    }
                                }

                            val topLevelScreens =
                                listOf(
                                    Screens.Home.route,
                                    Screens.Explore.route,
                                    Screens.Library.route,
                                    Screens.Search.route,
                                    Screens.Stats.route,
                                    "settings",
                                )

                            val (query, onQueryChange) =
                                rememberSaveable(stateSaver = TextFieldValue.Saver) {
                                    mutableStateOf(TextFieldValue())
                                }

                            var active by rememberSaveable {
                                mutableStateOf(false)
                            }

                            val onActiveChange: (Boolean) -> Unit = { newActive ->
                                active = newActive
                                if (!newActive) {
                                    focusManager.clearFocus()
                                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                        onQueryChange(TextFieldValue())
                                    }
                                }
                            }

                            var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                            val searchBarFocusRequester = remember { FocusRequester() }

                            val onSearch: (String) -> Unit = {
                                if (it.isNotEmpty()) {
                                    onActiveChange(false)
                                    navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                                    if (dataStore[PauseSearchHistoryKey] != true) {
                                        database.query {
                                            insert(SearchHistory(query = it))
                                        }
                                    }
                                }
                            }

                            var openSearchImmediately: Boolean by remember {
                                mutableStateOf(intent?.action == ACTION_SEARCH)
                            }

                            val shouldShowSearchBar =
                                remember(active, navBackStackEntry) {
                                    active ||
                                            navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                            navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                }

                            val shouldShowNavigationBar =
                                remember(navBackStackEntry, active) {
                                    navBackStackEntry?.destination?.route == null ||
                                            navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                                            !active
                                }

                            val navigationBarHeight by animateDpAsState(
                                targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                                animationSpec = if (reduceAnimations) snap() else NavigationBarAnimationSpec,
                                label = "",
                            )

                            val playerBottomSheetState =
                                rememberBottomSheetState(
                                    dismissedBound = 0.dp,
                                    collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight - 16.dp else 0.dp) + MiniPlayerHeight,
                                    expandedBound = maxHeight,
                                )

                            val playerAwareWindowInsets =
                                remember(
                                    bottomInset,
                                    shouldShowNavigationBar,
                                    playerBottomSheetState.isDismissed
                                ) {
                                    var bottom = bottomInset
                                    if (shouldShowNavigationBar) bottom += NavigationBarHeight - 16.dp
                                    if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                                    windowsInsets
                                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                        .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                                }

                            appBarScrollBehavior(
                                canScroll = {
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                            (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                                }
                            )

                            val searchBarScrollBehavior =
                                appBarScrollBehavior(
                                    canScroll = {
                                        navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                                    },
                                )
                            val topAppBarScrollBehavior =
                                appBarScrollBehavior(
                                    canScroll = {
                                        navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                                (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                                    },
                                )

                            LaunchedEffect(navBackStackEntry) {
                                if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                                    val query = navBackStackEntry?.arguments?.getString("query") ?: return@LaunchedEffect
                                    val searchQuery =
                                        withContext(Dispatchers.IO) {
                                            if (query.contains("%")) {
                                                query
                                            } else {
                                                URLDecoder.decode(query, "UTF-8")
                                            }
                                        }
                                    onQueryChange(
                                        TextFieldValue(
                                            searchQuery,
                                            TextRange(searchQuery.length)
                                        )
                                    )
                                    if (searchQuery.isEmpty()) {
                                        onActiveChange(true)
                                    }
                                } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                    onQueryChange(TextFieldValue())
                                }
                                searchBarScrollBehavior.state.resetHeightOffset()
                                topAppBarScrollBehavior.state.resetHeightOffset()
                            }
                            LaunchedEffect(active) {
                                if (active) {
                                    searchBarScrollBehavior.state.resetHeightOffset()
                                    topAppBarScrollBehavior.state.resetHeightOffset()
                                    searchBarFocusRequester.requestFocus()
                                }
                            }

                            LaunchedEffect(playerConnection) {
                                val player = playerConnection?.player ?: return@LaunchedEffect
                                if (player.currentMediaItem == null) {
                                    if (!playerBottomSheetState.isDismissed) {
                                        playerBottomSheetState.dismiss()
                                    }
                                } else {
                                    if (playerBottomSheetState.isDismissed) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }

                            DisposableEffect(playerConnection, playerBottomSheetState) {
                                val player =
                                    playerConnection?.player ?: return@DisposableEffect onDispose { }
                                val listener =
                                    object : Player.Listener {
                                        override fun onMediaItemTransition(
                                            mediaItem: MediaItem?,
                                            reason: Int,
                                        ) {
                                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                                mediaItem != null &&
                                                playerBottomSheetState.isDismissed
                                            ) {
                                                playerBottomSheetState.collapseSoft()
                                            }
                                        }
                                    }
                                player.addListener(listener)
                                onDispose {
                                    player.removeListener(listener)
                                }
                            }

                            var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                            LaunchedEffect(navBackStackEntry) {
                                shouldShowTopBar =
                                    !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
                            }

                            val coroutineScope = rememberCoroutineScope()
                            var sharedSong: SongItem? by remember {
                                mutableStateOf(null)
                            }
                            DisposableEffect(Unit) {
                                val listener =
                                    Consumer<Intent> { intent ->
                                        if (ExternalPlayerUtil.isAudioIntent(intent, this@MainActivity)) {
                                            val audioUri = ExternalPlayerUtil.getAudioUri(intent)
                                            if (audioUri != null) {
                                                coroutineScope.launch {
                                                    while (playerConnection == null) {
                                                        delay(50)
                                                    }
                                                    playerConnection?.let { pc ->
                                                        ExternalPlayerUtil.playExternalAudio(pc, this@MainActivity, audioUri)
                                                        if (playerBottomSheetState.isDismissed) {
                                                            playerBottomSheetState.collapseSoft()
                                                        }
                                                    }
                                                }
                                                return@Consumer
                                            }
                                        }

                                        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)
                                            ?.toUri() ?: return@Consumer
                                        
                                        if (com.darkxvenom.airbeats.utils.RemoteConfigManager.isMatchingListenTogetherDomain(uri.host)) {
                                            val code = uri.getQueryParameter("code")
                                            if (code != null) {
                                                ListenTogetherSync.joinSession(code)
                                                return@Consumer
                                            }
                                        }
                                        when (val path = uri.pathSegments.firstOrNull()) {
                                            "playlist" ->
                                                (uri.getQueryParameter("id") ?: uri.getQueryParameter("list"))?.let { playlistId ->
                                                    if (playlistId.startsWith("OLAK5uy_")) {
                                                        coroutineScope.launch {
                                                            YouTube
                                                                .albumSongs(playlistId)
                                                                .onSuccess { songs ->
                                                                    songs.firstOrNull()?.album?.id?.let { browseId ->
                                                                        navController.navigate("album/$browseId")
                                                                    }
                                                                }.onFailure {
                                                                    reportException(it)
                                                                }
                                                        }
                                                    } else {
                                                        navController.navigate("online_playlist/$playlistId")
                                                    }
                                                }

                                            "browse" ->
                                                uri.lastPathSegment?.let { browseId ->
                                                    navController.navigate("album/$browseId")
                                                }

                                            "channel", "c" ->
                                                uri.lastPathSegment?.let { artistId ->
                                                    navController.navigate("artist/$artistId")
                                                }

                                            "artist" ->
                                                uri.getQueryParameter("id")?.let { artistId ->
                                                    navController.navigate("artist/$artistId")
                                                }

                                            else ->
                                                when {
                                                    path == "watch" -> uri.getQueryParameter("v")
                                                    uri.host == "youtu.be" -> path
                                                    com.darkxvenom.airbeats.utils.RemoteConfigManager.isMatchingPlayDomain(uri.host) && path == "song" -> uri.getQueryParameter("id")
                                                    com.darkxvenom.airbeats.utils.RemoteConfigManager.isMatchingPlayDomain(uri.host) -> path
                                                    else -> null
                                                }?.let { videoId ->
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            YouTube.queue(listOf(videoId))
                                                        }.onSuccess {
                                                            playerConnection?.playQueue(
                                                                YouTubeQueue(
                                                                    WatchEndpoint(videoId = it.firstOrNull()?.id),
                                                                    it.firstOrNull()?.toMediaMetadata()
                                                                )
                                                            )
                                                        }.onFailure {
                                                            reportException(it)
                                                        }
                                                    }
                                                }
                                        }
                                    }

                                addOnNewIntentListener(listener)
                                intent?.let { initialIntent ->
                                    coroutineScope.launch {
                                        while (navController.currentDestination == null) {
                                            delay(50)
                                        }
                                        listener.accept(initialIntent)
                                    }
                                }
                                onDispose { removeOnNewIntentListener(listener) }
                            }

                            val currentTitle = remember(navBackStackEntry) {
                                when (navBackStackEntry?.destination?.route) {
                                    Screens.Home.route -> R.string.home
                                    Screens.Explore.route -> R.string.explore
                                    Screens.Library.route -> R.string.filter_library
                                    else -> null
                                }
                            }
                            val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                            val insetBg = if (playerBottomSheetState.progress > 0f) Color.Transparent else baseBg
                            val ringtoneViewModel: RingtoneViewModel = viewModel()

                            CompositionLocalProvider(
                                LocalDatabase provides database,
                                LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                                LocalPlayerConnection provides playerConnection,
                                LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                                LocalDownloadUtil provides downloadUtil,
                                LocalShimmerTheme provides ShimmerTheme,
                                LocalSyncUtils provides syncUtils,
                                LocalBackdrop provides backdrop,
                                LocalRingtoneViewModel provides ringtoneViewModel,
                            ) {
                                var showRealNavBar by remember { mutableStateOf(false) }
                                var playIntroAnimation by remember { mutableStateOf(true) }

                                val aodAutoTimeoutSeconds by rememberPreference(AodAutoActivationKey, 0)
                                var isAodActive by remember { mutableStateOf(false) }
                                var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

                                val resetAodTimer = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    if (isAodActive) {
                                        isAodActive = false
                                    }
                                }

                                LaunchedEffect(aodAutoTimeoutSeconds, playerBottomSheetState.isExpanded, lastInteractionTime, isAodActive) {
                                    if (aodAutoTimeoutSeconds > 0 && playerBottomSheetState.isExpanded && !isAodActive) {
                                        kotlinx.coroutines.delay(100L)
                                        while (isActive) {
                                            kotlinx.coroutines.delay(100L)
                                            val elapsedSeconds = (System.currentTimeMillis() - lastInteractionTime) / 1000f
                                            if (elapsedSeconds >= aodAutoTimeoutSeconds) {
                                                isAodActive = true
                                                navController.navigate("always_on_display") {
                                                    launchSingleTop = true
                                                }
                                                break
                                            }
                                        }
                                    }
                                }

                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(200)
                                    showRealNavBar = true
                                    kotlinx.coroutines.delay(600)
                                    playIntroAnimation = false
                                }

                                Scaffold(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                awaitPointerEvent()
                                                resetAodTimer()
                                            }
                                        },
                                    containerColor = Color.Transparent,
                                    topBar = {
                                        val isSearchRoute =
                                            navBackStackEntry?.destination?.route?.startsWith("search/") == true

                                        if (active || isSearchRoute) {
                                            val topSearchContent: @Composable () -> Unit = {
                                                TopSearch(
                                                query = query,
                                                onQueryChange = onQueryChange,
                                                onSearch = onSearch,
                                                active = active,
                                                onActiveChange = onActiveChange,
                                                placeholder = {
                                                    Text(
                                                        text = stringResource(
                                                            when (searchSource) {
                                                                SearchSource.LOCAL -> R.string.search_library
                                                                SearchSource.ONLINE -> R.string.search_yt_music
                                                            }
                                                        ),
                                                    )
                                                },
                                                leadingIcon = {
                                                    IconButton(
                                                        onClick = {
                                                            when {
                                                                active -> onActiveChange(false)
                                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                    navController.navigateUp()
                                                                }
                                                                else -> onActiveChange(true)
                                                            }
                                                        },
                                                        onLongClick = {
                                                            when {
                                                                active -> {}
                                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                    navController.backToMain()
                                                                }
                                                                else -> {}
                                                            }
                                                        },
                                                    ) {
                                                        Icon(
                                                            painterResource(
                                                                if (active ||
                                                                    !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                                ) {
                                                                    R.drawable.arrow_back
                                                                } else {
                                                                    R.drawable.search
                                                                }
                                                            ),
                                                            contentDescription = null,
                                                        )
                                                    }
                                                },
                                                trailingIcon = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (active) {
                                                            if (query.text.isNotEmpty()) {
                                                                IconButton(
                                                                    onClick = {
                                                                        onQueryChange(TextFieldValue(""))
                                                                    },
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(R.drawable.close),
                                                                        contentDescription = null,
                                                                    )
                                                                }
                                                            }
                                                            IconButton(
                                                                onClick = {
                                                                    searchSource =
                                                                        if (searchSource == SearchSource.ONLINE) {
                                                                            SearchSource.LOCAL
                                                                        } else {
                                                                            SearchSource.ONLINE
                                                                        }
                                                                },
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(
                                                                        when (searchSource) {
                                                                            SearchSource.LOCAL -> R.drawable.library_music
                                                                            SearchSource.ONLINE -> R.drawable.language
                                                                        }
                                                                    ),
                                                                    contentDescription = stringResource(
                                                                        when (searchSource) {
                                                                            SearchSource.LOCAL -> R.string.search_online
                                                                            SearchSource.ONLINE -> R.string.search_library
                                                                        }
                                                                    ),
                                                                )
                                                            }
                                                        }
                                                        IconButton(onClick = { navController.navigate(com.darkxvenom.airbeats.ui.screens.musicrecognition.MusicRecognitionRoute) }) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.mic),
                                                                contentDescription = "Music Recognition"
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .focusRequester(searchBarFocusRequester)
                                                    .align(Alignment.TopCenter)
                                                    .fillMaxWidth(),
                                                focusRequester = searchBarFocusRequester
                                            ) {
                                                Crossfade(
                                                    targetState = searchSource,
                                                    label = "search_content_transition",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(
                                                            bottom = if (!playerBottomSheetState.isDismissed) {
                                                                MiniPlayerHeight
                                                            } else {
                                                                0.dp
                                                            }
                                                        )
                                                        .navigationBarsPadding(),
                                                ) { currentSearchSource ->
                                                    when (currentSearchSource) {
                                                        SearchSource.LOCAL -> LocalSearchScreen(
                                                            query = query.text,
                                                            navController = navController,
                                                            onDismiss = { onActiveChange(false) },
                                                            pureBlack = pureBlack,
                                                        )
                                                        SearchSource.ONLINE -> OnlineSearchScreen(
                                                            query = query.text,
                                                            onQueryChange = onQueryChange,
                                                            navController = navController,
                                                            onSearch = { searchQuery ->
                                                                try {
                                                                    val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
                                                                    navController.navigate("search/$encodedQuery")
                                                                    if (dataStore[PauseSearchHistoryKey] != true) {
                                                                        database.query {
                                                                            insert(SearchHistory(query = searchQuery))
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e("SearchNavigation", "Error navigating to search: ${e.message}", e)
                                                                }
                                                            },
                                                            onDismiss = { onActiveChange(false) },
                                                            pureBlack = pureBlack,
                                                        )
                                                    }
                                                }
                                            }
                                            }
                                            val isPlayful = homeScreenStyle == HomeScreenStyle.PLAYFUL
                                            if (isPlayful) {
                                                MaterialTheme(
                                                    colorScheme = MaterialTheme.colorScheme.copy(
                                                        background = Color(0xFFFFD54F),
                                                        surface = Color(0xFFFFD54F),
                                                        surfaceContainerLow = Color.White,
                                                        onBackground = Color.Black,
                                                        onSurface = Color.Black,
                                                        onSurfaceVariant = Color.DarkGray
                                                    )
                                                ) {
                                                    CompositionLocalProvider(LocalContentColor provides Color.Black) {
                                                        topSearchContent()
                                                    }
                                                }
                                            } else {
                                                topSearchContent()
                                            }
                                        }
                                    },
                                    bottomBar = {
                                        Box {

                                            val currentRoute = navBackStackEntry?.destination?.route

                                            val isTopLevel =
                                                currentRoute == Screens.Home.route ||
                                                        currentRoute == Screens.Explore.route ||
                                                        currentRoute == Screens.Library.route ||
                                                        (currentRoute == "stats" && homeScreenStyle == HomeScreenStyle.APPLE)

                                            val isPlayfulHome = (currentRoute == Screens.Home.route || currentRoute == Screens.Library.route || currentRoute == Screens.Explore.route) && homeScreenStyle == HomeScreenStyle.PLAYFUL

                                            val isAuthScreen = currentRoute == "onboarding" || currentRoute == "guest_profile_setup" || currentRoute == "discord_login"

                                            if (!isAuthScreen && (!isPlayfulHome || !playerBottomSheetState.isCollapsed)) {
                                                BottomSheetPlayer(
                                                    state = playerBottomSheetState,
                                                    navController = navController,
                                                    onOpenFullscreenLyrics = {
                                                        showFullscreenLyrics = true
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .offset(y = 0.dp)
                                                )
                                            }
                                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                                val lyricsBottomSheetState = rememberBottomSheetState(
                                                    dismissedBound = 0.dp,
                                                    expandedBound = with(androidx.compose.ui.platform.LocalDensity.current) { constraints.maxHeight.toDp() },
                                                )

                                            LaunchedEffect(showFullscreenLyrics) {
                                                if (showFullscreenLyrics) {
                                                    lyricsBottomSheetState.expandSoft()
                                                } else {
                                                    lyricsBottomSheetState.collapseSoft()
                                                }
                                            }

                                            LaunchedEffect(lyricsBottomSheetState.isCollapsed) {
                                                if (lyricsBottomSheetState.isCollapsed && showFullscreenLyrics) {
                                                    showFullscreenLyrics = false
                                                }
                                            }

                                            BottomSheet(
                                                state = lyricsBottomSheetState,
                                                modifier = Modifier.fillMaxSize(),
                                                background = {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(MaterialTheme.colorScheme.background.copy(alpha = lyricsBottomSheetState.progress.coerceIn(0f, 1f)))
                                                    )
                                                },
                                                collapsedContent = {},
                                            ) {
                                                val playerConnection = LocalPlayerConnection.current
                                                val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
                                                    ?: return@BottomSheet

                                                if (mediaMetadata != null) {
                                                    if (playerScreenStyle == PlayerScreenStyle.SPOTIFY) {
                                                        SpotifyLyrics(
                                                            onNavigateBack = {
                                                                lyricsBottomSheetState.collapseSoft()
                                                            },
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else if (lyricsScreenStyle == com.darkxvenom.airbeats.constants.LyricsScreenStyle.LYRICS_2 && playerScreenStyle != PlayerScreenStyle.GALAXY) {
                                                        com.darkxvenom.airbeats.ui.player.AirBeatsLyricsScreen(
                                                            mediaMetadata = mediaMetadata!!,
                                                            navController = navController,
                                                            onBackClick = {
                                                                lyricsBottomSheetState.collapseSoft()
                                                            },
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Lyrics(
                                                            sliderPositionProvider = { null },
                                                            onNavigateBack = {
                                                                lyricsBottomSheetState.collapseSoft()
                                                            },
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(MaterialTheme.colorScheme.background),
                                                        contentAlignment = Alignment.Center
                                                    ) {}
                                                }
                                                }
                                            }

                                            val configuration = LocalConfiguration.current
                                            val isTabletLandscape = configuration.screenWidthDp >= 600 &&
                                                    configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

                                            val shouldShowBottomNav =
                                                shouldShowNavigationBar &&
                                                        playerBottomSheetState.progress < 0.95f &&
                                                        !isPlayfulHome

                                            if (shouldShowBottomNav) {

                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .then(if (navBarStyle != NavBarStyle.SPOTIFY) Modifier.navigationBarsPadding() else Modifier)
                                                        .then(
                                                            if (navBarStyle == NavBarStyle.SPOTIFY) {
                                                                Modifier.fillMaxWidth()
                                                                    .height(NavigationBarHeight - 16.dp + bottomInset)
                                                            } else {
                                                                Modifier
                                                                    .padding(bottom = 6.dp)
                                                                    .fillMaxWidth(if (navBarStyle == NavBarStyle.MATERIAL) 0.94f else 0.88f)
                                                                    .height(NavigationBarHeight - 16.dp)
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {

                                                    val offsetY by animateDpAsState(
                                                        targetValue = if (playIntroAnimation) 120.dp else 0.dp,
                                                        animationSpec = tween(600),
                                                        label = "nav_offset"
                                                    )

                                                    val scale by animateFloatAsState(
                                                        targetValue = if (playIntroAnimation) 0.3f else 1f,
                                                        animationSpec = tween(600),
                                                        label = "nav_scale"
                                                    )

                                                    val alpha by animateFloatAsState(
                                                        targetValue = if (playIntroAnimation) 0.6f else 1f,
                                                        animationSpec = tween(600),
                                                        label = "nav_alpha"
                                                    )

                                                    // ═══════════════════════════════════════════════════════
                                                    // LIQUID GLASS NAV BAR — works on both dark & light themes
                                                    // Strategy:
                                                    //   • BLUR layer  — Android 12+ real blur, older = tinted fallback
                                                    //   • TINT layer  — semi-transparent surface tint (adapts to theme)
                                                    //   • HIGHLIGHT   — white-to-transparent vertical gradient (top sheen)
                                                    //   • BORDER      — dual-stroke: outer white shimmer + inner bright line
                                                    //   • SHADOW      — outer drop shadow for lift / separation
                                                    // The tint uses colorScheme.surface so it is warm-white in light
                                                    // mode and dark-grey in dark mode, making the bar always visible.
                                                    // ═══════════════════════════════════════════════════════

                                                    val surfaceColor = MaterialTheme.colorScheme.surface
                                                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

                                                    val curvedItems = navigationItems.map { screen ->
                                                        CurvedBottomNavigationItem(
                                                            iconInactive = screen.iconIdInactive,
                                                            iconActive = screen.iconIdActive,
                                                            titleId = screen.titleId
                                                        )
                                                    }

                                                    val selectedIndex = navigationItems.indexOfFirst { screen ->
                                                        navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
                                                    }.takeIf { it >= 0 } ?: 0

                                                    var lastTapTime by remember { mutableLongStateOf(0L) }
                                                    var lastTappedIcon by remember { mutableStateOf<Int?>(null) }
                                                    var navigateToExplore by remember { mutableStateOf(false) }

                                                    val onItemSelectedAction: (Int) -> Unit = { index ->
                                                         if (index in navigationItems.indices) {
                                                             val screen = navigationItems[index]
                                                             val isSelected = screen.route == navBackStackEntry?.destination?.route

                                                             val currentTapTime = System.currentTimeMillis()
                                                             val timeSinceLastTap = currentTapTime - lastTapTime
                                                             val isDoubleTap =
                                                                 screen.titleId == R.string.explore &&
                                                                         lastTappedIcon == R.string.explore &&
                                                                         timeSinceLastTap < 300L

                                                             lastTapTime = currentTapTime
                                                             lastTappedIcon = screen.titleId

                                                             if (screen.titleId == R.string.explore && navBarStyle != NavBarStyle.MATERIAL) {
                                                                 if (isDoubleTap) {
                                                                     onActiveChange(true)
                                                                     navigateToExplore = false
                                                                 } else {
                                                                     navigateToExplore = true
                                                                     coroutineScope.launch {
                                                                         delay(300L)
                                                                         if (navigateToExplore) {
                                                                             navigateToScreen(navController, screen)
                                                                         }
                                                                     }
                                                                 }
                                                             } else {
                                                                 if (isSelected) {
                                                                     navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                                                     coroutineScope.launch {
                                                                         searchBarScrollBehavior.state.resetHeightOffset()
                                                                     }
                                                                 } else {
                                                                     navigateToScreen(navController, screen)
                                                                 }
                                                             }
                                                         }
                                                     }

                                                     if (navBarStyle == NavBarStyle.NEW_CLASSIC) {
                                                         com.darkxvenom.airbeats.ui.component.NewClassicBottomNavigationBar(
                                                             items = curvedItems,
                                                             selectedIndex = selectedIndex,
                                                             onItemSelected = onItemSelectedAction,
                                                             onNavigateRoute = { route -> navController.navigate(route) },
                                                             modifier = Modifier
                                                                 .fillMaxSize()
                                                                 .offset(y = offsetY)
                                                                 .scale(scale)
                                                                 .alpha(alpha)
                                                         )
                                                     } else if (navBarStyle == NavBarStyle.SPOTIFY) {
                                                         com.darkxvenom.airbeats.ui.component.SpotifyBottomNavigationBar(
                                                             items = curvedItems,
                                                             selectedIndex = selectedIndex,
                                                             onItemSelected = onItemSelectedAction,
                                                             modifier = Modifier
                                                                 .fillMaxSize()
                                                                 .offset(y = offsetY)
                                                                 .scale(scale)
                                                                 .alpha(alpha)
                                                         )
                                                     } else if (navBarStyle == NavBarStyle.APPLE) {
                                                         com.darkxvenom.airbeats.ui.component.AppleNavigationBar(
                                                             items = curvedItems,
                                                             selectedIndex = selectedIndex,
                                                             onItemSelected = onItemSelectedAction,
                                                             backdrop = backdrop,
                                                             modifier = Modifier
                                                                 .fillMaxSize()
                                                                 .offset(y = offsetY)
                                                                 .scale(scale)
                                                                 .alpha(alpha)
                                                         )
                                                     } else if (navBarStyle == NavBarStyle.MATERIAL) {
                                                         com.darkxvenom.airbeats.ui.component.MaterialBottomNavigationBar(
                                                             items = curvedItems,
                                                             selectedIndex = selectedIndex,
                                                             onItemSelected = onItemSelectedAction,
                                                             modifier = Modifier
                                                                 .fillMaxSize()
                                                                 .offset(y = offsetY)
                                                                 .scale(scale)
                                                                 .alpha(alpha)
                                                         )
                                                     } else if (navBarStyle == NavBarStyle.LIQUID_GLASS || enableLiquidGlass) {
                                                         LiquidGlassBottomNavigationBar(
                                                             items = curvedItems,
                                                             selectedIndex = selectedIndex,
                                                             onItemSelected = onItemSelectedAction,
                                                             backdrop = backdrop,
                                                             modifier = Modifier
                                                                 .offset(y = offsetY)
                                                                 .scale(scale)
                                                                 .alpha(alpha)
                                                         )
                                                     } else {
                                                          com.darkxvenom.airbeats.ui.component.NewClassicBottomNavigationBar(
                                                              onNavigateRoute = { route -> navController.navigate(route) },
                                                             items = curvedItems,
                                                             selectedIndex = selectedIndex,
                                                             onItemSelected = onItemSelectedAction,
                                                             modifier = Modifier
                                                                 .fillMaxSize()
                                                                 .offset(y = offsetY)
                                                                 .scale(scale)
                                                                 .alpha(alpha)
                                                         )
                                                     }
                                                }

                                            } else {
                                                // Removed redundant bottomInsetDp box
                                            }
                                        }
                                    },
                                ) { paddingValues ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .layerBackdrop(backdrop)
                                    ) {
                                        var transitionDirection =
                                        AnimatedContentTransitionScope.SlideDirection.Left

                                    if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                        if (navigationItems.fastAny { it.route == previousTab }) {
                                            val curIndex = navigationItems.indexOf(
                                                navigationItems.fastFirstOrNull {
                                                    it.route == navBackStackEntry?.destination?.route
                                                }
                                            )
                                            val prevIndex = navigationItems.indexOf(
                                                navigationItems.fastFirstOrNull {
                                                    it.route == previousTab
                                                }
                                            )
                                            if (prevIndex > curIndex)
                                                AnimatedContentTransitionScope.SlideDirection.Right.also {
                                                    transitionDirection = it
                                                }
                                        }
                                    }

                                    val enableSwipeBackGesture by rememberPreference(com.darkxvenom.airbeats.constants.EnableSwipeBackGestureKey, defaultValue = true)
                                    val enableTabSwipeGesture by rememberPreference(com.darkxvenom.airbeats.constants.EnableTabSwipeGestureKey, defaultValue = true)
                                    val currentDestRoute = navBackStackEntry?.destination?.route
                                    val rootTabRoutes = remember(navigationItems) { navigationItems.map { it.route }.toSet() }
                                    val isRootScreen = currentDestRoute in rootTabRoutes
                                    val canSwipeBack = !isRootScreen && navController.previousBackStackEntry != null

                                    SwipeBackContainer(
                                        enabled = enableSwipeBackGesture,
                                        canSwipeBack = canSwipeBack,
                                        onBack = { navController.popBackStack() },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .tabSwipeGesture(
                                                enabled = enableTabSwipeGesture && isRootScreen,
                                                currentRoute = currentDestRoute,
                                                navigationItems = navigationItems,
                                                onNavigateToRoute = { targetRoute: String ->
                                                    val screen = navigationItems.firstOrNull { it.route == targetRoute }
                                                    if (screen != null) {
                                                        navigateToScreen(navController, screen)
                                                    } else {
                                                        navController.navigate(targetRoute)
                                                    }
                                                }
                                            )
                                    ) {
                                    val justRestored = remember { AutoBackupManager.consumeJustRestoredFlag(this@MainActivity) }
                                    NavHost(
                                        navController = navController,
                                        startDestination = if (justRestored) {
                                            when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                                NavigationTab.HOME -> Screens.Home
                                                NavigationTab.EXPLORE -> Screens.Explore
                                                NavigationTab.LIBRARY -> Screens.Library
                                            }.route
                                        } else {
                                            "onboarding"
                                        },

                                        enterTransition = {
                                            if (reduceAnimations) {
                                                fadeIn(tween(0))
                                            } else {
                                                val fromIdx = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                                val toIdx = navigationItems.indexOfFirst { it.route == targetState.destination.route }
                                                if (fromIdx != -1 && toIdx != -1) {
                                                    if (toIdx > fromIdx) {
                                                        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(150))
                                                    } else {
                                                        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(150))
                                                    }
                                                } else {
                                                    fadeIn(tween(250)) + slideInHorizontally(
                                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                                        initialOffsetX = { it / 2 }
                                                    )
                                                }
                                            }
                                        },

                                        exitTransition = {
                                            if (reduceAnimations) {
                                                fadeOut(tween(0))
                                            } else {
                                                val fromIdx = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                                val toIdx = navigationItems.indexOfFirst { it.route == targetState.destination.route }
                                                if (fromIdx != -1 && toIdx != -1) {
                                                    if (toIdx > fromIdx) {
                                                        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(150))
                                                    } else {
                                                        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(150))
                                                    }
                                                } else {
                                                    fadeOut(tween(200)) + slideOutHorizontally(
                                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                                        targetOffsetX = { -it / 2 }
                                                    )
                                                }
                                            }
                                        },

                                        popEnterTransition = {
                                            if (reduceAnimations) {
                                                fadeIn(tween(0))
                                            } else {
                                                val fromIdx = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                                val toIdx = navigationItems.indexOfFirst { it.route == targetState.destination.route }
                                                if (fromIdx != -1 && toIdx != -1) {
                                                    if (toIdx > fromIdx) {
                                                        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(150))
                                                    } else {
                                                        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it } + fadeIn(tween(150))
                                                    }
                                                } else {
                                                    fadeIn(tween(250)) + slideInHorizontally(
                                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                                        initialOffsetX = { -it / 3 }
                                                    )
                                                }
                                            }
                                        },

                                        popExitTransition = {
                                            if (reduceAnimations) {
                                                fadeOut(tween(0))
                                            } else {
                                                val fromIdx = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                                val toIdx = navigationItems.indexOfFirst { it.route == targetState.destination.route }
                                                if (fromIdx != -1 && toIdx != -1) {
                                                    if (toIdx > fromIdx) {
                                                        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(150))
                                                    } else {
                                                        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(150))
                                                    }
                                                } else {
                                                    fadeOut(tween(200)) + slideOutHorizontally(
                                                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                                                        targetOffsetX = { it }
                                                    )
                                                }
                                            }
                                        },

                                        modifier = Modifier.nestedScroll(
                                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                                navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                            ) {
                                                searchBarScrollBehavior.nestedScrollConnection
                                            } else {
                                                topAppBarScrollBehavior.nestedScrollConnection
                                            }
                                        )
                                    ) {
                                        navigationBuilder(
                                            navController = navController,
                                            scrollBehavior = if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                                navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                            ) searchBarScrollBehavior else topAppBarScrollBehavior,
                                            latestVersionName = latestVersionName,
                                            playerBottomSheetState = playerBottomSheetState,
                                            onSearchClick = { onActiveChange(true) }
                                        )
                                    }
                                    }
                                    }
                                }

                                BottomSheetMenu(
                                    state = LocalMenuState.current,
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )

                                sharedSong?.let { song ->
                                    playerConnection?.let {
                                        Dialog(
                                            onDismissRequest = { sharedSong = null },
                                            properties = DialogProperties(usePlatformDefaultWidth = false),
                                        ) {
                                            Surface(
                                                modifier = Modifier.padding(24.dp),
                                                shape = RoundedCornerShape(16.dp),
                                                color = AlertDialogDefaults.containerColor,
                                                tonalElevation = AlertDialogDefaults.TonalElevation,
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                ) {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = { sharedSong = null },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                val currentRoute = navBackStackEntry?.destination?.route
                                val isOnboardingOrAuth = isNameSet != true ||
                                    currentRoute == null ||
                                    currentRoute == "onboarding" ||
                                    currentRoute == "guest_profile_setup" ||
                                    currentRoute == "discord_login"

                                var hasSettledOnMainScreen by rememberSaveable { mutableStateOf(false) }
                                LaunchedEffect(isOnboardingOrAuth) {
                                    if (!isOnboardingOrAuth) {
                                        delay(600)
                                        hasSettledOnMainScreen = true
                                    } else {
                                        hasSettledOnMainScreen = false
                                    }
                                }

                                var showNoInternetDialog by rememberSaveable { mutableStateOf(false) }
                                var hasCheckedInternetOnOpen by rememberSaveable { mutableStateOf(false) }

                                LaunchedEffect(hasSettledOnMainScreen, isOnboardingOrAuth) {
                                    if (hasSettledOnMainScreen && !isOnboardingOrAuth && !hasCheckedInternetOnOpen) {
                                        hasCheckedInternetOnOpen = true
                                        delay(400)
                                        if (!com.darkxvenom.airbeats.utils.isInternetAvailable(this@MainActivity)) {
                                            showNoInternetDialog = true
                                        }
                                    }
                                }

                                if (hasSettledOnMainScreen && !isOnboardingOrAuth) {
                                    if (showNoInternetDialog) {
                                        com.darkxvenom.airbeats.ui.component.NoInternetDialog(
                                            onDismiss = { showNoInternetDialog = false },
                                            onListenCached = {
                                                showNoInternetDialog = false
                                                navController.navigate("cache_playlist/cached")
                                            },
                                            onGoToLibrary = {
                                                showNoInternetDialog = false
                                                navController.navigate(Screens.Library.route)
                                            }
                                        )
                                    }

                                    updateInfoState?.let { info ->
                                        UpdateAvailableDialog(
                                            updateInfo = info,
                                            onDismiss = { updateInfoState = null }
                                        )
                                    }

                                    com.darkxvenom.airbeats.ui.component.DeveloperNewsPopupDialog(
                                        onNavigateToNews = {
                                            navController.navigate("settings/developer_news")
                                        }
                                    )
                                }

                                val ringtoneUiState by ringtoneViewModel.uiState.collectAsState()

                                RingtoneTrimmerDialog(
                                    isVisible = ringtoneUiState.showTrimmer,
                                    songId = ringtoneUiState.targetSongId,
                                    songTitle = ringtoneUiState.targetSongTitle,
                                    duration = ringtoneUiState.targetSongDuration,
                                    onDismiss = { ringtoneViewModel.hideTrimmer() },
                                    onResolveStreamUrl = { ringtoneViewModel.getStreamUrl(this@MainActivity, it) },
                                    onConfirm = { start, end -> ringtoneViewModel.setAsRingtone(this@MainActivity, start, end) }
                                )

                                if (ringtoneUiState.showProgress) {
                                    RingtoneProgressDialog(
                                        isVisible = ringtoneUiState.showProgress,
                                        progress = ringtoneUiState.progress,
                                        statusMessage = ringtoneUiState.statusMessage,
                                        isComplete = ringtoneUiState.isComplete,
                                        isSuccess = ringtoneUiState.isSuccess,
                                        onDismiss = { ringtoneViewModel.dismissProgress() },
                                        onOpenSettings = { ringtoneViewModel.openRingtoneSettings(this@MainActivity) }
                                    )
                                }
                            }

                            LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                                if (shouldShowSearchBar && openSearchImmediately) {
                                    onActiveChange(true)
                                    try {
                                        delay(100)
                                        searchBarFocusRequester.requestFocus()
                                    } catch (_: Exception) {
                                    }
                                    openSearchImmediately = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToScreen(
        navController: NavHostController,
        screen: Screens
    ) {
        val startDestId = try {
            navController.graph.findStartDestination().id
        } catch (_: Exception) {
            navController.graph.startDestinationId
        }
        navController.navigate(screen.route) {
            popUpTo(startDestId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    private fun handlevideoIdIntent(intent: Intent) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        when {
            uri.pathSegments.firstOrNull() == "watch" -> uri.getQueryParameter("v")
            uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
            com.darkxvenom.airbeats.utils.RemoteConfigManager.isMatchingPlayDomain(uri.host) -> {
                if (uri.pathSegments.firstOrNull() == "song") {
                    uri.getQueryParameter("id")
                } else if (uri.pathSegments.firstOrNull() == "artist") {
                    null
                } else {
                    uri.pathSegments.firstOrNull()
                }
            }
            else -> null
        }?.let { videoId ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    YouTube.queue(listOf(videoId))
                }.onSuccess {
                    playerConnection?.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                            it.firstOrNull()?.toMediaMetadata()
                        )
                    )
                }.onFailure {
                    reportException(it)
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.darkxvenom.airbeats.action.SEARCH"
        const val ACTION_EXPLORE = "com.darkxvenom.airbeats.action.EXPLORE"
        const val ACTION_LIBRARY = "com.darkxvenom.airbeats.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    compositionLocalOf<PlayerConnection?> { null }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalRingtoneViewModel = staticCompositionLocalOf<RingtoneViewModel> { error("No RingtoneViewModel provided") }


@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val checkNotificationPermission = remember {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (!isGranted) {
            Log.d("NotificationPermission", "Permiso de notificaciones denegado")
        }
    }

    LaunchedEffect(Unit) {
        permissionGranted = checkNotificationPermission()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SwitchPreference(
        title = { Text(stringResource(R.string.notification)) },
        icon = {
            Icon(
                painter = painterResource(
                    id = if (permissionGranted) R.drawable.notification_on
                    else R.drawable.notification_off
                ),
                contentDescription = stringResource(
                    if (permissionGranted) R.string.notifications_enabled
                    else R.string.notifications_disabled
                )
            )
        },
        checked = permissionGranted,
        onCheckedChange = { checked ->
            when {
                checked && !permissionGranted -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings(context)
                    }
                }
                !checked && permissionGranted -> {
                    openNotificationSettings(context)
                }
            }
        }
    )
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("NotificationSettings", "No se pudo abrir configuración de notificaciones", e)
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL(com.darkxvenom.airbeats.utils.RemoteConfigManager.getLatestReleaseApiUrl(isNightly = false))
        val connection = url.openConnection()
        connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        return@withContext jsonObject.getString("tag_name")
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}

@Composable
fun ProfileIconWithUpdateBadge(
    currentVersion: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
    var showUpdateBadge by remember { mutableStateOf(false) }
    val updatedOnClick = rememberUpdatedState(onProfileClick)

    val infiniteTransition = rememberInfiniteTransition(label = "badge_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    LaunchedEffect(currentVersion) {
        try {
            val latestVersion = withContext(Dispatchers.IO) { checkForUpdates() }
            showUpdateBadge = latestVersion?.let { isNewerVersion(it, currentVersion) } ?: false
        } catch (e: Exception) {
            Timber.tag("ProfileIcon").e("Error checking for updates: ${e.message}")
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                try {
                    updatedOnClick.value()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (currentSelection) {
                is AvatarSelection.Custom -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                            .crossfade(true)
                            .error(R.drawable.person)
                            .placeholder(R.drawable.person)
                            .build(),
                        contentDescription = "Avatar personalizado",
                        modifier = modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                is AvatarSelection.DiceBear -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data((currentSelection as AvatarSelection.DiceBear).url)
                            .crossfade(true)
                            .error(R.drawable.person)
                            .placeholder(R.drawable.person)
                            .build(),
                        contentDescription = "Avatar DiceBear",
                        modifier = modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = "Avatar predeterminado",
                        modifier = modifier
                    )
                }
            }
        }

        if (showUpdateBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * alpha),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            shape = CircleShape
                        )
                )
                Icon(
                    painter = painterResource(R.drawable.update),
                    contentDescription = "Actualización disponible",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.Center)
                        .scale(scale)
                        .alpha(alpha)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernHomeTopBar(
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager
        .getAvatarSelection
        .collectAsState(initial = AvatarSelection.Default)

    val userName = LocalUserName.current
    val displayName = if (userName.isNotEmpty()) userName else "Friend"

    val collapsedFraction by remember {
        derivedStateOf { scrollBehavior.state.collapsedFraction }
    }

    val contentAlpha = 1f - collapsedFraction
    val yOffset = (-collapsedFraction * 120).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 12.dp)
            .offset {
                IntOffset(x = 0, y = yOffset)
            }
    ) {
        Column(modifier = Modifier.alpha(contentAlpha)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        when (currentSelection) {
                            is AvatarSelection.Custom -> {
                                AsyncImage(
                                    model = (currentSelection as AvatarSelection.Custom).uri.toUri(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            is AvatarSelection.DiceBear -> {
                                AsyncImage(
                                    model = (currentSelection as AvatarSelection.DiceBear).url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.person),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hi, $displayName",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val statsViewModel = com.darkxvenom.airbeats.ui.utils.safeHiltViewModel<StatsViewModel>()
                        val totalHours by (statsViewModel?.totalListenHours ?: kotlinx.coroutines.flow.flowOf(0.0)).collectAsState(initial = 0.0)
                        val currentRank = remember(totalHours) {
                            if (totalHours >= 1.0) AirBeatsRank.fromHours(totalHours.toInt()) else null
                        }
                        val rankPrefMgr = remember { RankPreferenceManager(context) }
                        val displayedRank by rankPrefMgr.displayedRank.collectAsState(initial = null)

                        currentRank?.let { rank ->
                            Spacer(modifier = Modifier.width(8.dp))
                            RankBadge(rank = rank, displayedRank = displayedRank, size = 26.dp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CircleIconButton(icon = R.drawable.search, onClick = onSearchClick)
                    CircleIconButton(icon = R.drawable.favorite, onClick = { })
                }
            }
        }
    }
}

@Composable
fun HeadphoneSplashScreen(statusText: String? = null) {

    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    val shift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "shift"
    )

    val colors = MaterialTheme.colorScheme

    val animatedBackground = Brush.radialGradient(
        colors = listOf(
            Color(0x228E2DE2),
            Color(0x224A00E0),
            Color(0x22FF00C8),
            colors.background
        ),
        center = Offset(shift % 600f, shift % 900f),
        radius = 1200f
    )

    var startAnimation by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.9f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(900),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
        ) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(145.dp)
                    .clipToBounds()
            ) {
                Image(
                    painter = painterResource(R.drawable.airbeats_logo),
                    contentDescription = null,
                    modifier = Modifier.size(220.dp)
                )
            }

            Text(
                text = "AirBeats",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE91E63),
                            Color(0xFFFFC107),
                            Color(0xFF2196F3)
                        )
                    )
                )
            )

            if (!statusText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedBar(
    heightMultiplier: Float,
    brush: Brush
) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height((120 * heightMultiplier).dp)
            .clip(RoundedCornerShape(50))
            .background(brush)
            .shadow(
                elevation = 25.dp,
                shape = RoundedCornerShape(50)
            )
    )
}






