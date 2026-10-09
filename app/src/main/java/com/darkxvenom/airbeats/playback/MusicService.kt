@file:Suppress("DEPRECATION")

package com.darkxvenom.airbeats.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.media.AudioManager
import android.media.AudioDeviceInfo
import android.media.AudioDeviceCallback
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Binder
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.darkxvenom.airbeats.playback.cast.CastPlayback
import com.darkxvenom.airbeats.playback.cast.CastPlaybackListener
import com.darkxvenom.airbeats.playback.cast.ResolvedCastStream
import com.google.android.gms.cast.framework.CastContext
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.models.SongItem
import com.darkxvenom.airbeats.innertube.models.WatchEndpoint
import com.darkxvenom.airbeats.airconnect.AirConnectClient
import com.darkxvenom.airbeats.MainActivity
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AudioNormalizationKey
import com.darkxvenom.airbeats.constants.AudioQualityKey
import com.darkxvenom.airbeats.constants.AutoLoadMoreKey
import com.darkxvenom.airbeats.constants.AutoSkipNextOnErrorKey
import com.darkxvenom.airbeats.constants.EnableJioSaavnKey
import com.darkxvenom.airbeats.constants.CrossfadeKey
import com.darkxvenom.airbeats.constants.CrossfadeEnabledKey
import com.darkxvenom.airbeats.constants.CrossfadeDurationKey
import com.darkxvenom.airbeats.constants.CrossfadeGaplessKey
import androidx.media3.exoplayer.DefaultLoadControl
import kotlin.math.roundToLong
import com.darkxvenom.airbeats.constants.DisableLoadMoreWhenRepeatAllKey
import com.darkxvenom.airbeats.constants.DiscordTokenKey
import com.darkxvenom.airbeats.constants.DiscordUseDetailsKey
import com.darkxvenom.airbeats.constants.DynamicIslandKey
import com.darkxvenom.airbeats.constants.EnableDiscordRPCKey
import com.darkxvenom.airbeats.constants.AudioBoostEnabledKey
import com.darkxvenom.airbeats.constants.AudioBoostPercentKey
import com.darkxvenom.airbeats.constants.EchoEnabledKey
import com.darkxvenom.airbeats.constants.EchoDelayMsKey
import com.darkxvenom.airbeats.constants.EchoFeedbackKey
import com.darkxvenom.airbeats.constants.EchoWetMixKey
import com.darkxvenom.airbeats.constants.EchoPingPongKey
import com.darkxvenom.airbeats.constants.DjFilterSweepKey
import com.darkxvenom.airbeats.constants.DjFlangerEnabledKey
import com.darkxvenom.airbeats.constants.DjFlangerRateKey
import com.darkxvenom.airbeats.constants.DjFlangerDepthKey
import com.darkxvenom.airbeats.constants.DjSaturationKey
import com.darkxvenom.airbeats.constants.DjTurntableLinkedKey
import com.darkxvenom.airbeats.constants.DjTempoSpeedKey
import com.darkxvenom.airbeats.constants.DjPitchKey
import com.darkxvenom.airbeats.constants.DolbyAtmosEnabledKey
import com.darkxvenom.airbeats.constants.SpatialAudioEnabledKey
import com.darkxvenom.airbeats.constants.EightDAudioEnabledKey
import com.darkxvenom.airbeats.constants.EightDAudioLevelKey
import com.darkxvenom.airbeats.constants.BypassAllAudioEffectsKey
import com.darkxvenom.airbeats.constants.AudioFxAndDjEnabledKey
import com.darkxvenom.airbeats.constants.BitPerfectEnabledKey
import com.darkxvenom.airbeats.constants.StreamingQualityPresetKey
import com.darkxvenom.airbeats.constants.QualityTiers
import com.darkxvenom.airbeats.constants.AutomixEnabledKey
import com.darkxvenom.airbeats.constants.AutomixPerformanceMode
import com.darkxvenom.airbeats.constants.AutomixPerformanceModeKey
import com.darkxvenom.airbeats.playback.automix.TrackAnalyzer
import com.darkxvenom.airbeats.constants.EqualizerEnabledKey
import com.darkxvenom.airbeats.constants.HideExplicitKey
import com.darkxvenom.airbeats.constants.HistoryDuration
import com.darkxvenom.airbeats.constants.MediaSessionConstants.CommandToggleLike
import com.darkxvenom.airbeats.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.darkxvenom.airbeats.constants.MediaSessionConstants.CommandToggleShuffle
import com.darkxvenom.airbeats.constants.PauseListenHistoryKey
import com.darkxvenom.airbeats.constants.PermanentShuffleKey
import com.darkxvenom.airbeats.constants.PersistentQueueKey
import com.darkxvenom.airbeats.constants.PlayerVolumeKey
import com.darkxvenom.airbeats.constants.RepeatModeKey
import com.darkxvenom.airbeats.constants.ShowLyricsKey
import com.darkxvenom.airbeats.constants.SimilarContent
import com.darkxvenom.airbeats.constants.SkipSilenceKey
import com.darkxvenom.airbeats.constants.SkipUncachedPartKey
import com.darkxvenom.airbeats.constants.StopMusicOnTaskClearKey
import com.darkxvenom.airbeats.db.MusicDatabase
import com.darkxvenom.airbeats.db.entities.Event
import com.darkxvenom.airbeats.db.entities.SongEntity
import com.darkxvenom.airbeats.db.entities.FormatEntity
import com.darkxvenom.airbeats.db.entities.LyricsEntity
import com.darkxvenom.airbeats.db.entities.RelatedSongMap
import com.darkxvenom.airbeats.di.DownloadCache
import com.darkxvenom.airbeats.di.PlayerCache
import com.darkxvenom.airbeats.extensions.tryOrNull
import com.darkxvenom.airbeats.extensions.SilentHandler
import com.darkxvenom.airbeats.extensions.collect
import com.darkxvenom.airbeats.extensions.collectLatest
import com.darkxvenom.airbeats.extensions.currentMetadata
import com.darkxvenom.airbeats.extensions.findNextMediaItemById
import com.darkxvenom.airbeats.extensions.mediaItems
import com.darkxvenom.airbeats.extensions.metadata
import com.darkxvenom.airbeats.extensions.toMediaItem
import com.darkxvenom.airbeats.extensions.toQueue
import com.darkxvenom.airbeats.lyrics.LyricsHelper
import com.darkxvenom.airbeats.models.PersistPlayerState
import com.darkxvenom.airbeats.models.PersistQueue
import com.darkxvenom.airbeats.models.toMediaMetadata
import com.darkxvenom.airbeats.playback.queues.EmptyQueue
import com.darkxvenom.airbeats.playback.queues.Queue
import com.darkxvenom.airbeats.playback.queues.YouTubeQueue
import com.darkxvenom.airbeats.playback.queues.filterExcluded
import com.darkxvenom.airbeats.playback.queues.filterExplicit
import com.darkxvenom.airbeats.utils.CoilBitmapLoader
import com.darkxvenom.airbeats.utils.DiscordRPC
import com.darkxvenom.airbeats.utils.NetworkConnectivityObserver
import com.darkxvenom.airbeats.utils.YTPlayerUtils
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.enumPreference
import com.darkxvenom.airbeats.utils.get
import com.darkxvenom.airbeats.utils.reportException
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class EqualizerUiState(
    val isAvailable: Boolean = false,
    val enabled: Boolean = false,
    val bandLevels: List<Short> = emptyList(),
    val centerFrequencies: List<Int> = emptyList(),
    val minBandLevel: Short = -1500,
    val maxBandLevel: Short = 1500,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    @Inject
    lateinit var scrobbleRepository: com.darkxvenom.airbeats.data.repository.ScrobbleRepository

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: Any? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        handleAudioFocusChange(focusChange)
    }
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false

    private var scope = CoroutineScope(Dispatchers.Main) + Job()

    private val mediaOkHttpClient: okhttp3.OkHttpClient by lazy {
        val baseBuilder = okhttp3.OkHttpClient
            .Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)

        val ytProxy = YouTube.proxy
        if (ytProxy != null) {
            baseBuilder.proxySelector(object : java.net.ProxySelector() {
                override fun select(uri: java.net.URI?): List<java.net.Proxy> {
                    val host = uri?.host.orEmpty()
                    val isYouTubeHost =
                        host.endsWith("googlevideo.com") ||
                            host.endsWith("googleusercontent.com") ||
                            host.endsWith("youtube.com") ||
                            host.endsWith("youtube-nocookie.com") ||
                            host.endsWith("ytimg.com")
                    return if (isYouTubeHost) listOf(ytProxy) else listOf(java.net.Proxy.NO_PROXY)
                }

                override fun connectFailed(uri: java.net.URI?, sa: java.net.SocketAddress?, ioe: java.io.IOException?) {
                    // ignore
                }
            })
        }

        baseBuilder.addInterceptor { chain ->
            val request = chain.request()
            val host = request.url.host
            val isYouTubeMediaHost =
                host.endsWith("googlevideo.com") ||
                    host.endsWith("googleusercontent.com") ||
                    host.endsWith("youtube.com") ||
                    host.endsWith("youtube-nocookie.com") ||
                    host.endsWith("ytimg.com")

            val finalRequest = if (isYouTubeMediaHost) {
                val clientParam = request.url.queryParameter("c")?.trim().orEmpty()
                val userAgent = com.darkxvenom.airbeats.utils.StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = com.darkxvenom.airbeats.utils.StreamClientUtils.resolveOriginReferer(clientParam)

                val builder = request.newBuilder().header("User-Agent", userAgent)
                originReferer.origin?.let { builder.header("Origin", it) }
                originReferer.referer?.let { builder.header("Referer", it) }
                builder.build()
            } else {
                request
            }

            val response = chain.proceed(finalRequest)

            if (response.code == 416) {
                val rangeHeader = request.header("Range")
                if (rangeHeader != null) {
                    Timber.tag("MusicService").w("Handling HTTP 416 for Range: $rangeHeader, returning empty EOF response")
                    response.close()
                    return@addInterceptor okhttp3.Response.Builder()
                        .request(finalRequest)
                        .protocol(response.protocol)
                        .code(206)
                        .message("Partial Content")
                        .header("Content-Length", "0")
                        .body(okhttp3.ResponseBody.create(response.body.contentType(), ByteArray(0)))
                        .build()
                }
            }

            if (response.isSuccessful) {
                val contentType = response.header("Content-Type")?.lowercase().orEmpty()
                if (contentType.contains("text/html") ||
                    contentType.contains("text/plain") ||
                    contentType.contains("application/json") ||
                    contentType.contains("application/xml")
                ) {
                    response.close()
                    throw java.io.IOException("Received invalid media Content-Type: $contentType from ${request.url}")
                }
            }

            response
        }.build()
    }

    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    val isNetworkConnected = MutableStateFlow(false)

    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = if (::connectivityManager.isInitialized) connectivityManager else getSystemService() as? ConnectivityManager
                ?: return isNetworkConnected.value
            val activeNet = cm.activeNetwork ?: return isNetworkConnected.value
            val caps = cm.getNetworkCapabilities(activeNet) ?: return isNetworkConnected.value
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            isNetworkConnected.value
        }
    }

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.darkxvenom.airbeats.constants.AudioQuality.AUTO
    )

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.darkxvenom.airbeats.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))
    private val audioFocusVolumeFactor = MutableStateFlow(1f)
    private val playbackFadeFactor = MutableStateFlow(1f)
    private val audioNormalizationEnabled = MutableStateFlow(true)
    private val maxSafeGainFactor = MAX_AUDIO_NORMALIZATION_FACTOR
    private var crossfadeEnabled = false
    private var crossfadeDurationMs = 0L
    private var crossfadeGapless = false
    private var crossfadeTriggerJob: Job? = null
    private var crossfadeJob: Job? = null
    private var secondaryCrossfadePlayer: ExoPlayer? = null
    private var secondaryCrossfadeTarget: CrossfadeTarget? = null
    private var isCrossfading = false
    private var crossfadeHandoffInProgress = false
    private var crossfadeBaseVolume = 1f
    private var crossfadeIncomingBaseVolume = 1f
    private var crossfadeProgress = 0f
    private var crossfadeHandoffProgress = 0f
    private var crossfadePlaybackRequested = false
    private var crossfadeSuppressedMediaId: String? = null
    private var audiblePlaybackRecoveryJob: Job? = null

    private val localPlayer: ExoPlayer
        get() = player

    private val secondaryCrossfadeListener =
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.tag(TAG).w(error, "Secondary crossfade player failed")
                scope.launch {
                    abortCrossfadeAndResumePrimary("secondary_player_error")
                }
            }
        }

    private data class CrossfadeConfig(
        val enabled: Boolean,
        val durationSeconds: Float,
        val gapless: Boolean,
    )

    private data class CrossfadeTarget(
        val index: Int,
        val mediaId: String,
    )

    private data class CachedSongUrl(
        val url: String,
        val expiresAt: Long,
        val contentLength: Long?,
    )
    private val songUrlCache = java.util.concurrent.ConcurrentHashMap<String, CachedSongUrl>()
    private val spotifyMatchCache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val jioSaavnAttemptedSongIds = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())
    private val jioSaavnFailedSongs = java.util.Collections.newSetFromMap(java.util.concurrent.ConcurrentHashMap<String, Boolean>())

    fun getCachedPlaybackUrl(mediaId: String): String? {
        return songUrlCache[mediaId]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.url
    }

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var equalizerSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    val equalizerState = MutableStateFlow(EqualizerUiState())
    val spatialAudioProcessor = SpatialAudioProcessor()
    val eightDAudioProcessor = EightDAudioProcessor()
    val djAudioProcessor = DjAudioProcessor()
    val echoEnabled = MutableStateFlow(false)
    val echoDelayMs = MutableStateFlow(280)
    val echoFeedback = MutableStateFlow(0.40f)
    val echoWetMix = MutableStateFlow(0.0f)
    val echoPingPong = MutableStateFlow(false)
    val djFilterSweep = MutableStateFlow(0.0f)
    val djFlangerEnabled = MutableStateFlow(false)
    val djFlangerRate = MutableStateFlow(0.5f)
    val djFlangerDepth = MutableStateFlow(0.0f)
    val djSaturation = MutableStateFlow(0.0f)
    val djTempoSpeed = MutableStateFlow(1.0f)
    val djPitch = MutableStateFlow(1.0f)
    val djTurntableLinked = MutableStateFlow(false)
    val audioFxAndDjEnabled = MutableStateFlow(false)
    val bypassAllAudioEffects = MutableStateFlow(false)
    val dolbyAtmosEnabled = MutableStateFlow(false)
    val spatialAudioEnabled = MutableStateFlow(false)
    val eightDAudioEnabled = MutableStateFlow(false)
    val eightDAudioLevel = MutableStateFlow(8)
    val audioBoostEnabled = MutableStateFlow(false)
    val audioBoostPercent = MutableStateFlow(100)
    val visualizerManager by lazy { AudioVisualizerManager(scope) }
    val isTrackDolbyAtmos = MutableStateFlow(false)
    val automixEnabled = MutableStateFlow(false)
    val automixPerformanceMode = MutableStateFlow(AutomixPerformanceMode.BALANCED)
    val trackAnalyzer by lazy { TrackAnalyzer(this) }
    val bitPerfectEnabled = MutableStateFlow(false)
    val isBitPerfectActive = MutableStateFlow(false)
    val preferredAudioDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val availableAudioDevices = MutableStateFlow<List<AudioDeviceInfo>>(emptyList())
    val simultaneousAudioProcessor by lazy { SimultaneousAudioProcessor(this) }
    val isDualAudioEnabled = MutableStateFlow(false)
    val dualAudioSecondaryDeviceId = MutableStateFlow<Int?>(null)
    val dualAudioSecondaryVolume = MutableStateFlow(1.0f)
    private var usbBitPerfectOutput: UsbBitPerfectOutput? = null
    private var audioDeviceCallback: AudioDeviceCallback? = null

    private var discordRpc: DiscordRPC? = null
    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: Job? = null
    private var mediaController: MediaController? = null

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())
    /** Keeps a short related-track tail available when the user enables Infinite queue. */
    private var infiniteQueueLoadJob: Job? = null

    private var consecutivePlaybackErr = 0
    private var offlineBufferingJob: Job? = null
    private var isAutoSkippingUncached = false

    // Google Cast / Chromecast audio streaming state
    private var castPlayback: CastPlayback? = null
    val isCasting = MutableStateFlow(false)
    val isCastPlaying = MutableStateFlow(false)
    val castDeviceName = MutableStateFlow<String?>(null)
    val castPositionMs = MutableStateFlow(0L)
    val castDurationMs = MutableStateFlow(0L)

    override fun onCreate() {
        super.onCreate()
        instance = this

        runCatching {
            val playbackCacheDir = java.io.File(cacheDir, "cached_playback")
            if (playbackCacheDir.exists()) {
                playbackCacheDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".tmp") || file.length() < 500_000L) {
                        file.delete()
                    }
                }
            }
        }

        setListener(object : MediaSessionService.Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                Timber.w("MediaSessionService listener: onForegroundServiceStartNotAllowedException caught and suppressed")
            }
        })
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.airbeats_monochrome)
                },
        )
        player =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false,
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build()
                .apply {
                    addListener(this@MusicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
                }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocus()
        val bitPerfect = UsbBitPerfectOutput(audioManager)
        usbBitPerfectOutput = bitPerfect
        setupUsbBitPerfectListener(bitPerfect)

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleLibrary = ::toggleLibrary
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        // Keep a connected controller so that notification works
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                runCatching { mediaController = controllerFuture.get() }
                    .onFailure(::reportException)
            },
            MoreExecutors.directExecutor()
        )

        connectivityManager = getSystemService()!!
        val activeCap = connectivityManager.activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        isNetworkConnected.value = activeCap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        connectivityObserver = NetworkConnectivityObserver(this)

        // Observar conectividad de red
        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    // Reintentar reproducción cuando vuelve la conexión
                    waitingForNetworkConnection.value = false
                    if (player.currentMediaItem != null && player.playWhenReady) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        combine(
            playerVolume,
            audioFocusVolumeFactor,
            playbackFadeFactor,
            bitPerfectEnabled,
            isBitPerfectActive,
        ) { vol, focus, fade, bpEnabled, bpActive ->
            if (bpEnabled && bpActive) {
                1.0f
            } else {
                vol * focus * fade
            }
        }.collectLatest(scope) { finalVolume ->
            applyEffectiveVolume(finalVolume)
        }

        dataStore.data
            .map { it[BitPerfectEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                bitPerfectEnabled.value = enabled
                usbBitPerfectOutput?.setEnabled(enabled)
                isBitPerfectActive.value = usbBitPerfectOutput?.isConfigured() == true
                if (enabled) {
                    equalizer?.enabled = false
                    loudnessEnhancer?.enabled = false
                    spatialAudioProcessor.enabled = false
                    eightDAudioProcessor.enabled = false
                } else {
                    setupEqualizer()
                    setupLoudnessEnhancer()
                    updateSpatialAudio()
                    updateEightDAudio()
                }
            }

        dataStore.data
            .map { it[StreamingQualityPresetKey] ?: QualityTiers.QUALITY_MAX_HI_RES }
            .distinctUntilChanged()
            .collectLatest(scope) { preset ->
                if (preset == QualityTiers.QUALITY_DOLBY_ATMOS) {
                    dolbyAtmosEnabled.value = true
                    updateSpatialAudio()
                }
            }

        dataStore.data
            .map { prefs ->
                val legacyDuration = prefs[CrossfadeKey] ?: 0
                val enabled = prefs[CrossfadeEnabledKey] ?: (legacyDuration > 0)
                val durationSeconds = prefs[CrossfadeDurationKey] ?: (if (legacyDuration > 0) legacyDuration.toFloat() else 5f)
                val gapless = prefs[CrossfadeGaplessKey] ?: true
                CrossfadeConfig(
                    enabled = enabled,
                    durationSeconds = durationSeconds,
                    gapless = gapless,
                )
            }.distinctUntilChanged()
            .collectLatest(scope) { config ->
                crossfadeEnabled = config.enabled
                if (!config.enabled) {
                    crossfadeSuppressedMediaId = null
                }
                crossfadeDurationMs =
                    (config.durationSeconds.coerceIn(0f, 10f) * 1000f)
                        .roundToLong()
                        .coerceAtLeast(0L)
                crossfadeGapless = config.gapless
                if (crossfadeEnabled && crossfadeDurationMs > 0L) {
                    scheduleCrossfade()
                } else {
                    cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
                }
            }

        dataStore.data
            .map { it[DolbyAtmosEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                dolbyAtmosEnabled.value = enabled
                updateSpatialAudio()
            }

        dataStore.data
            .map { it[SpatialAudioEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                spatialAudioEnabled.value = enabled
                updateSpatialAudio()
            }

        dataStore.data
            .map { it[EightDAudioEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                eightDAudioEnabled.value = enabled
                updateEightDAudio()
            }

        dataStore.data
            .map { it[EightDAudioLevelKey] ?: 8 }
            .distinctUntilChanged()
            .collectLatest(scope) { level ->
                eightDAudioLevel.value = level
                eightDAudioProcessor.level = level
            }

        dataStore.data
            .map { it[AudioBoostEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                audioBoostEnabled.value = enabled
                setupLoudnessEnhancer()
            }

        dataStore.data
            .map { it[AudioBoostPercentKey] ?: 100 }
            .distinctUntilChanged()
            .collectLatest(scope) { percent ->
                audioBoostPercent.value = percent
                setupLoudnessEnhancer()
            }

        dataStore.data
            .map { it[EchoEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                echoEnabled.value = enabled
                djAudioProcessor.echoEnabled = enabled
            }

        dataStore.data
            .map { it[EchoDelayMsKey] ?: 280 }
            .distinctUntilChanged()
            .collectLatest(scope) { delayMs ->
                echoDelayMs.value = delayMs
                djAudioProcessor.echoDelayMs = delayMs
            }

        dataStore.data
            .map { it[EchoFeedbackKey] ?: 0.40f }
            .distinctUntilChanged()
            .collectLatest(scope) { feedback ->
                echoFeedback.value = feedback
                djAudioProcessor.echoFeedback = feedback
            }

        dataStore.data
            .map { it[BypassAllAudioEffectsKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { bypass ->
                bypassAllAudioEffects.value = bypass
                applyBypassState(bypass)
            }

        dataStore.data
            .map { it[AudioFxAndDjEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                audioFxAndDjEnabled.value = enabled
                djAudioProcessor.enabled = enabled && !bypassAllAudioEffects.value
                if (!enabled) {
                    resetDjFx()
                }
            }

        dataStore.data
            .map { it[EchoWetMixKey] ?: 0.0f }
            .distinctUntilChanged()
            .collectLatest(scope) { wet ->
                echoWetMix.value = wet
                djAudioProcessor.echoWetMix = wet
            }

        dataStore.data
            .map { it[EchoPingPongKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { pingPong ->
                echoPingPong.value = pingPong
                djAudioProcessor.echoPingPong = pingPong
            }

        dataStore.data
            .map { it[DjFilterSweepKey] ?: 0.0f }
            .distinctUntilChanged()
            .collectLatest(scope) { sweep ->
                djFilterSweep.value = sweep
                djAudioProcessor.filterSweep = sweep
            }

        dataStore.data
            .map { it[DjFlangerEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                djFlangerEnabled.value = enabled
                djAudioProcessor.flangerEnabled = enabled
            }

        dataStore.data
            .map { it[DjFlangerRateKey] ?: 0.5f }
            .distinctUntilChanged()
            .collectLatest(scope) { rate ->
                djFlangerRate.value = rate
                djAudioProcessor.flangerRate = rate
            }

        dataStore.data
            .map { it[DjFlangerDepthKey] ?: 0.0f }
            .distinctUntilChanged()
            .collectLatest(scope) { depth ->
                djFlangerDepth.value = depth
                djAudioProcessor.flangerDepth = depth
            }

        dataStore.data
            .map { it[DjSaturationKey] ?: 0.0f }
            .distinctUntilChanged()
            .collectLatest(scope) { sat ->
                djSaturation.value = sat
                djAudioProcessor.saturation = sat
            }

        dataStore.data
            .map { it[DjTurntableLinkedKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { linked ->
                djTurntableLinked.value = linked
            }

        dataStore.data
            .map { it[AutomixEnabledKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                automixEnabled.value = enabled
            }

        dataStore.data
            .map {
                runCatching {
                    AutomixPerformanceMode.valueOf(it[AutomixPerformanceModeKey] ?: AutomixPerformanceMode.BALANCED.name)
                }.getOrDefault(AutomixPerformanceMode.BALANCED)
            }
            .distinctUntilChanged()
            .collectLatest(scope) { mode ->
                automixPerformanceMode.value = mode
                trackAnalyzer.setPerformanceMode(mode)
            }


        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }


        dataStore.data
            .map { it[DynamicIslandKey] ?: false }
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                if (enabled && Settings.canDrawOverlays(this)) {
                    try {
                        startService(Intent(this, DynamicIslandService::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        stopService(Intent(this, DynamicIslandService::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
            } else {
                discordRpc?.closeRPC()
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics,
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: false }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            audioNormalizationEnabled.value = normalizeAudio
            setupLoudnessEnhancer()
        }

        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { (key, enabled) ->
                if (discordRpc?.isRpcRunning() == true) {
                    discordRpc?.closeRPC()
                }
                discordRpc = null
                if (key != null && enabled) {
                    discordRpc = DiscordRPC(this, key)
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        currentSong.value?.let {
                            discordRpc?.updateSong(it, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                        }
                    }
                }
            }

        // Observar cambios en DiscordUseDetailsKey
        dataStore.data
            .map { it[DiscordUseDetailsKey] ?: false }
            .debounce(1000)
            .distinctUntilChanged()
            .collect(scope) { useDetails ->
                if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                    currentSong.value?.let { song ->
                        discordUpdateJob?.cancel()
                        discordUpdateJob = scope.launch {
                            delay(1000)
                            discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, useDetails)
                        }
                    }
                }
            }

        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch(SilentHandler) {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                // Convertir de vuelta al tipo de cola apropiado
                val restoredQueue = queue.toQueue()
                playQueue(
                    queue = restoredQueue,
                    playWhenReady = false,
                )
            }
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                automixItems.value = queue.items.map { it.toMediaItem() }
            }

            // Restaurar estado del reproductor
            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistPlayerState
                    }
                }
            }.onSuccess { playerState ->
                // Restaurar configuración del reproductor después de cargar la cola
                scope.launch {
                    delay(1000) // Esperar a que la cola se cargue
                    player.shuffleModeEnabled = playerState.shuffleModeEnabled
                    player.volume = playerState.volume

                    // Restaurar posición si sigue siendo válida
                    if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                        player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                    }
                }
            }
        }

        // Guardar cola periódicamente para prevenir pérdida por crash o force kill
        }

        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }

        // Guardar cola más frecuentemente cuando está reproduciendo
        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
            }
        }

        initializeCast()
    }


    private fun setupUsbBitPerfectListener(bitPerfect: UsbBitPerfectOutput) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    updateUsbDacRoute(bitPerfect)
                    refreshAudioOutputDevices()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    updateUsbDacRoute(bitPerfect)
                    refreshAudioOutputDevices()
                }
            }
            audioDeviceCallback = callback
            audioManager.registerAudioDeviceCallback(callback, null)
        }
        updateUsbDacRoute(bitPerfect)
        refreshAudioOutputDevices()
    }

    fun setPreferredOutputDevice(device: AudioDeviceInfo?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                player.setPreferredAudioDevice(device)
            } catch (e: Exception) {
                Timber.w(e, "Failed to set player preferred audio device")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    if (device?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                        audioManager.setCommunicationDevice(device)
                    } else {
                        audioManager.clearCommunicationDevice()
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to update communication device routing")
                }
            }
        }
        preferredAudioDevice.value = device
    }

    fun refreshAudioOutputDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
                availableAudioDevices.value = devices
                val currentPreferred = preferredAudioDevice.value
                if (currentPreferred != null && devices.none { it.id == currentPreferred.id }) {
                    setPreferredOutputDevice(null)
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to refresh audio output devices")
            }
        }
    }

    fun setDualAudioEnabled(enabled: Boolean) {
        isDualAudioEnabled.value = enabled
        simultaneousAudioProcessor.setEnabled(enabled)
    }

    fun setDualAudioSecondaryDevice(device: AudioDeviceInfo?) {
        dualAudioSecondaryDeviceId.value = device?.id
        simultaneousAudioProcessor.setTargetDevice(device)
    }

    fun setDualAudioSecondaryVolume(volume: Float) {
        dualAudioSecondaryVolume.value = volume
        simultaneousAudioProcessor.setVolume(volume)
    }

    private fun updateUsbDacRoute(bitPerfect: UsbBitPerfectOutput) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val usbDevice = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
            bitPerfect.setDevice(usbDevice)
            isBitPerfectActive.value = bitPerfect.isConfigured()
        }
    }

    fun setBitPerfectEnabled(enabled: Boolean) {
        bitPerfectEnabled.value = enabled
        usbBitPerfectOutput?.setEnabled(enabled)
        isBitPerfectActive.value = usbBitPerfectOutput?.isConfigured() == true
        scope.launch {
            dataStore.edit { settings ->
                settings[BitPerfectEnabledKey] = enabled
            }
        }
        if (enabled) {
            equalizer?.enabled = false
            loudnessEnhancer?.enabled = false
            spatialAudioProcessor.enabled = false
        } else {
            setupEqualizer()
            setupLoudnessEnhancer()
            updateSpatialAudio()
        }
    }

    private fun setupAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupAudioFocusRequestOreo()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupAudioFocusRequestOreo() {
        audioFocusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusChangeListener, android.os.Handler(android.os.Looper.getMainLooper()))
            .build()
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true

                if (wasPlayingBeforeAudioFocusLoss) {
                    wasPlayingBeforeAudioFocusLoss = false
                    player.play()
                }

                audioFocusVolumeFactor.value = 1f
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = false

                if (player.isPlaying) {
                    player.pause()
                }

                abandonAudioFocus()
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                val isVoiceAssistantRunning = com.darkxvenom.airbeats.voice.VoiceAssistantService.instance != null
                if (!isVoiceAssistantRunning) {
                    wasPlayingBeforeAudioFocusLoss = player.isPlaying
                    if (player.isPlaying) {
                        player.pause()
                    }
                } else {
                    // Voice assistant is listening in background; do NOT pause music
                    wasPlayingBeforeAudioFocusLoss = false
                }

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                val isVoiceAssistantRunning = com.darkxvenom.airbeats.voice.VoiceAssistantService.instance != null
                if (!isVoiceAssistantRunning) {
                    wasPlayingBeforeAudioFocusLoss = player.isPlaying
                    if (player.isPlaying) {
                        audioFocusVolumeFactor.value = 0.2f
                    }
                } else {
                    wasPlayingBeforeAudioFocusLoss = false
                }

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                hasAudioFocus = true

                if (wasPlayingBeforeAudioFocusLoss) {
                    wasPlayingBeforeAudioFocusLoss = false
                    player.play()
                }

                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true

                player.volume = playerVolume.value

                lastAudioFocusState = focusChange
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (audioFocusRequest as? android.media.AudioFocusRequest)?.let { request ->
                audioManager.requestAudioFocus(request)
            } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (audioFocusRequest as? android.media.AudioFocusRequest)?.let { request ->
                    audioManager.abandonAudioFocusRequest(request)
                }
            } else {
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
            hasAudioFocus = false
        }
    }

    fun hasAudioFocusForPlayback(): Boolean {
        return hasAudioFocus
    }

    private fun waitOnNetworkError() {
        waitingForNetworkConnection.value = true
    }

    private var queueLoadingJob: Job? = null
    private var songLoadingRetryJob: Job? = null
    private var songLoadingRetryCount = 0
    private var currentQueueLoadId = 0L

    fun stopPlayback() {
        currentQueueLoadId++
        queueLoadingJob?.cancel()
        queueLoadingJob = null
        songLoadingRetryJob?.cancel()
        offlineBufferingJob?.cancel()
        offlineBufferingJob = null
        waitingForNetworkConnection.value = false
        clearAutomix()
        player.stop()
        player.clearMediaItems()
    }

    fun getCachedBytesForSong(mediaId: String): Long {
        val dlBytes = runCatching { downloadCache.getCachedSpans(mediaId).sumOf { it.length } }.getOrDefault(0L)
        val plBytes = runCatching { playerCache.getCachedSpans(mediaId).sumOf { it.length } }.getOrDefault(0L)
        val total = dlBytes + plBytes
        if (total > 0L) return total
        val safeMediaId = mediaId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val offlineFile = java.io.File(cacheDir, "offline_cache").listFiles { _, name ->
            name.startsWith("cached_${safeMediaId}.")
        }?.firstOrNull()
        return offlineFile?.length() ?: 0L
    }

    fun isSongFullyCached(mediaId: String): Boolean {
        if (mediaId.startsWith("content://") || mediaId.startsWith("file://") || mediaId.startsWith("local:")) {
            return true
        }
        val safeMediaId = mediaId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val offlineFile = java.io.File(cacheDir, "offline_cache").listFiles { _, name ->
            name.startsWith("cached_${safeMediaId}.")
        }?.firstOrNull { it.length() > 50_000L }
        if (offlineFile != null) return true

        val cachedPlaybackFile = java.io.File(cacheDir, "cached_playback").listFiles { _, name ->
            name.startsWith("cached_${safeMediaId}.")
        }?.firstOrNull { it.length() > 50_000L }
        if (cachedPlaybackFile != null) return true

        val dlBytes = runCatching { downloadCache.getCachedBytes(mediaId, 0L, Long.MAX_VALUE) }.getOrDefault(0L)
        if (downloadCache.keys.contains(mediaId) && dlBytes > 100_000L) {
            return true
        }

        if (downloadCache.isFullyCached(mediaId)) {
            return true
        }

        val plBytes = runCatching { playerCache.getCachedBytes(mediaId, 0L, Long.MAX_VALUE) }.getOrDefault(0L)
        val totalCached = dlBytes + plBytes
        if (totalCached <= 50_000L) return false

        // Try getting content length from cache metadata
        val dlMeta = runCatching { downloadCache.getContentMetadata(mediaId) }.getOrNull()
        val plMeta = runCatching { playerCache.getContentMetadata(mediaId) }.getOrNull()
        val metaLen = listOfNotNull(dlMeta, plMeta)
            .map { runCatching { ContentMetadata.getContentLength(it) }.getOrDefault(-1L) }
            .firstOrNull { it > 0L } ?: -1L

        if (metaLen > 0L) {
            return totalCached >= (metaLen * 0.90).toLong()
        }

        val dbLen: Long = runCatching {
            runBlocking(Dispatchers.IO) {
                database.format(mediaId).firstOrNull()?.contentLength ?: -1L
            }
        }.getOrNull() ?: -1L

        if (dbLen > 0L) {
            return totalCached >= (dbLen * 0.90).toLong()
        }

        val durationSec: Int = runCatching {
            runBlocking(Dispatchers.IO) {
                database.song(mediaId).firstOrNull()?.duration ?: -1
            }
        }.getOrNull() ?: -1

        if (durationSec > 0) {
            val minExpectedBytes = (durationSec * 8_000L).coerceAtLeast(250_000L)
            return totalCached >= minExpectedBytes
        }

        return totalCached >= 800_000L
    }

    fun skipToNextPlayableTrack() {
        if (isNetworkAvailable()) {
            Log.d(TAG, "Device is online; skipToNextPlayableTrack ignored.")
            return
        }
        if (isAutoSkippingUncached) return
        isAutoSkippingUncached = true
        scope.launch(Dispatchers.Main) {
            try {
                val totalItems = player.mediaItemCount
                if (totalItems <= 1) {
                    Log.w(TAG, "No more items in queue to skip to")
                    offlineBufferingJob?.cancel()
                    PlayerConnection.instance?.clearError()
                    player.pause()
                    Toast.makeText(this@MusicService, "No cached songs available to play offline", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val currentIdx = player.currentMediaItemIndex
                val isOffline = !isNetworkAvailable()
                val isSkipUncached = dataStore.get(SkipUncachedPartKey, false)
                var nextPlayableIndex = -1

                // Search forward from currentIdx + 1 to end of queue
                for (i in (currentIdx + 1) until totalItems) {
                    val item = player.getMediaItemAt(i)
                    if (!isOffline || !isSkipUncached || isSongFullyCached(item.mediaId)) {
                        nextPlayableIndex = i
                        break
                    }
                }

                // If not found, wrap around from 0 to currentIdx if repeatMode is ALL or queue has multiple items
                if (nextPlayableIndex == -1 && (player.repeatMode == Player.REPEAT_MODE_ALL || totalItems > 1)) {
                    for (i in 0 until currentIdx) {
                        val item = player.getMediaItemAt(i)
                        if (!isOffline || !isSkipUncached || isSongFullyCached(item.mediaId)) {
                            nextPlayableIndex = i
                            break
                        }
                    }
                }

                if (nextPlayableIndex != -1 && nextPlayableIndex != currentIdx) {
                    Log.i(TAG, "Skipping to playable cached song at index $nextPlayableIndex (mediaId: ${player.getMediaItemAt(nextPlayableIndex).mediaId})")
                    offlineBufferingJob?.cancel()
                    songLoadingRetryJob?.cancel()
                    songLoadingRetryCount = 0
                    consecutivePlaybackErr = 0
                    PlayerConnection.instance?.clearError()
                    player.seekToDefaultPosition(nextPlayableIndex)
                    player.prepare()
                    player.play()
                } else {
                    Log.w(TAG, "No playable cached songs found anywhere in queue")
                    offlineBufferingJob?.cancel()
                    PlayerConnection.instance?.clearError()
                    player.pause()
                    Toast.makeText(this@MusicService, "No cached songs available to play offline", Toast.LENGTH_SHORT).show()
                }
            } finally {
                delay(300)
                isAutoSkippingUncached = false
            }
        }
    }

    private fun keepLoadingCurrentSongOnError() {
        val currentMediaId = player.currentMediaItem?.mediaId
        if (!currentMediaId.isNullOrBlank()) {
            songUrlCache.remove(currentMediaId)
            YTPlayerUtils.invalidateCachedStreamUrls(currentMediaId)
        }
        songLoadingRetryJob?.cancel()
        if (songLoadingRetryCount >= 3) {
            Log.w(TAG, "Max loading retry count reached for media item $currentMediaId, stopping retry loop")
            return
        }
        songLoadingRetryCount++
        songLoadingRetryJob = scope.launch(Dispatchers.Main) {
            // Under no circumstances should the player skip to the next song when loading takes time.
            // Keep loading the current track by waiting briefly and re-preparing.
            delay(1500)
            if (player.currentMediaItem != null && (player.playbackState == STATE_IDLE || !player.isPlaying)) {
                Log.d(TAG, "Keep loading: retrying current media item ${player.currentMediaItem?.mediaId} (attempt $songLoadingRetryCount)")
                player.prepare()
                player.play()
            }
        }
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked ==
                                true
                            ) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
            ),
        )
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                .getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        if (!database.hasRelatedSongs(mediaId) && !mediaId.startsWith("JS:")) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()
        songLoadingRetryJob?.cancel()
        songLoadingRetryCount = 0
        waitingForNetworkConnection.value = false
        PlayerConnection.instance?.clearError()
        cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
        currentQueue = queue
        queueTitle = null
        val isPermanentShuffle = dataStore.get(PermanentShuffleKey, false)
        val hadShuffle = player.shuffleModeEnabled || isPermanentShuffle
        // Temporarily disable shuffle so setMediaItems plays the clicked song, not a random shuffle item
        player.shuffleModeEnabled = false

        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        val loadJobId = ++currentQueueLoadId
        queueLoadingJob?.cancel()
        queueLoadingJob = scope.launch(SilentHandler) {
            val excludedSongIds = withContext(Dispatchers.IO) { database.getExcludedSongIds().toHashSet() }
            val initialStatus =
                withContext(Dispatchers.IO) {
                    val status = queue.getInitialStatus()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                    if (queue is com.darkxvenom.airbeats.playback.queues.ListQueue) {
                        status
                    } else {
                        status.filterExcluded(excludedSongIds)
                    }
                }
            if (loadJobId != currentQueueLoadId) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch

            val targetSongId = queue.preloadItem?.id
            val targetIndex = if (targetSongId != null) {
                val found = initialStatus.items.indexOfFirst { it.mediaId == targetSongId }
                if (found != -1) found else initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.lastIndex)
            } else {
                initialStatus.mediaItemIndex.coerceIn(0, initialStatus.items.lastIndex)
            }

            val currentMediaId = player.currentMediaItem?.mediaId
            val isCurrentItemTarget = currentMediaId != null &&
                (currentMediaId == initialStatus.items.getOrNull(targetIndex)?.mediaId || currentMediaId == targetSongId)
            val isAlreadyPlayingPreload = isCurrentItemTarget &&
                player.playbackState != STATE_IDLE &&
                player.mediaItemCount > 0

            if (isAlreadyPlayingPreload) {
                // Preload item is already seamlessly playing! Add remaining items around it without restarting playback
                if (targetIndex == 0) {
                    val itemsAfter = initialStatus.items.subList(1, initialStatus.items.size)
                    if (itemsAfter.isNotEmpty()) {
                        player.addMediaItems(itemsAfter)
                    }
                } else {
                    val itemsBefore = initialStatus.items.subList(0, targetIndex)
                    val itemsAfter = initialStatus.items.subList(targetIndex + 1, initialStatus.items.size)
                    if (itemsBefore.isNotEmpty()) {
                        player.addMediaItems(0, itemsBefore)
                    }
                    if (itemsAfter.isNotEmpty()) {
                        player.addMediaItems(itemsAfter)
                    }
                }
            } else {
                player.setMediaItems(
                    initialStatus.items,
                    targetIndex,
                    initialStatus.position,
                )
                player.prepare()
                player.playWhenReady = playWhenReady
            }

            if (hadShuffle && player.mediaItemCount > 1) {
                val shuffledIndices = IntArray(player.mediaItemCount) { it }
                shuffledIndices.shuffle()
                val currentIdx = targetIndex
                val currentPosInShuffled = shuffledIndices.indexOf(currentIdx)
                if (currentPosInShuffled != -1) {
                    shuffledIndices[currentPosInShuffled] = shuffledIndices[0]
                    shuffledIndices[0] = currentIdx
                }
                player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
                player.shuffleModeEnabled = true
            } else {
                player.shuffleModeEnabled = isPermanentShuffle
            }

            // Si la cola tiene 1 sola canción y Endless Queue está activo, precargar canciones relacionadas
            if (dataStore.get(AutoLoadMoreKey, true) && !currentQueue.hasNextPage() && player.mediaItemCount <= 1) {
                val seedId = player.currentMediaItem?.mediaId
                if (!seedId.isNullOrBlank()) {
                    extendInfiniteQueue(seedId)
                }
            }
        }
    }

    fun triggerEndlessQueueIfNeeded() {
        if (!dataStore.get(AutoLoadMoreKey, true)) return
        val remaining = player.mediaItemCount - player.currentMediaItemIndex
        if (remaining <= 2) {
            val seedId = player.currentMediaItem?.mediaId ?: player.mediaItems.lastOrNull()?.mediaId
            if (!seedId.isNullOrBlank()) {
                extendInfiniteQueue(seedId, autoPlayIfEnded = (player.playbackState == Player.STATE_ENDED || !player.isPlaying))
            }
        }
    }

    fun extendInfiniteQueue(seedId: String, autoPlayIfEnded: Boolean = false) {
        if (infiniteQueueLoadJob?.isActive == true) {
            if (autoPlayIfEnded) {
                infiniteQueueLoadJob?.cancel()
            } else {
                return
            }
        }
        infiniteQueueLoadJob = scope.launch(SilentHandler) {
            var newMediaItems: List<MediaItem> = emptyList()

            // 1. Resolve seed track metadata
            val currentItem = player.mediaItems.find { it.mediaId == seedId }
                ?: player.currentMediaItem
                ?: player.mediaItems.lastOrNull()
            val meta = currentItem?.metadata ?: currentMediaMetadata.value
            val title = meta?.title.orEmpty().ifBlank { currentItem?.mediaMetadata?.title?.toString().orEmpty() }
            val artist = meta?.artists?.firstOrNull()?.name.orEmpty().ifBlank { currentItem?.mediaMetadata?.artist?.toString().orEmpty() }

            // 2. Resolve effective YouTube Video ID
            var effectiveYtId: String? = if (!seedId.contains(":")) seedId else null
            if (effectiveYtId.isNullOrBlank() && (title.isNotBlank() || artist.isNotBlank())) {
                val query = "$title $artist".trim()
                effectiveYtId = withContext(Dispatchers.IO) {
                    runCatching {
                        val searchRes = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                        (searchRes?.items?.firstOrNull() as? SongItem)?.id
                    }.getOrNull()
                }
            }

            // 3. Query YouTube Radio (Automix Next / Up Next recommendations)
            if (isNetworkConnected.value && !effectiveYtId.isNullOrBlank()) {
                try {
                    val nextResult = withContext(Dispatchers.IO) {
                        YouTube.next(WatchEndpoint(videoId = effectiveYtId)).getOrNull()
                    }
                    val candidateSongs = nextResult?.items.orEmpty()
                    val existingIds = player.mediaItems.map { it.mediaId }.toHashSet()
                    newMediaItems = candidateSongs
                        .map { it.toMediaItem() }
                        .filter { it.mediaId.isNotBlank() && existingIds.add(it.mediaId) }
                        .take(15)

                    // Fallback to relatedEndpoint if candidateSongs was empty
                    val relEp = nextResult?.relatedEndpoint
                    if (newMediaItems.isEmpty() && relEp != null) {
                        val relatedSongs = withContext(Dispatchers.IO) {
                            YouTube.related(relEp).getOrNull()?.songs.orEmpty()
                        }
                        newMediaItems = relatedSongs
                            .map { it.toMediaItem() }
                            .filter { it.mediaId.isNotBlank() && existingIds.add(it.mediaId) }
                            .take(15)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load YouTube next radio for $effectiveYtId", e)
                }
            }

            // 4. Query JioSaavn search or trending if online and still empty
            if (newMediaItems.isEmpty() && isNetworkConnected.value) {
                try {
                    val query = if (artist.isNotBlank()) artist else title
                    if (query.isNotBlank()) {
                        val jsSongs = withContext(Dispatchers.IO) {
                            com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.searchSongs(query).getOrNull().orEmpty()
                        }
                        val existingIds = player.mediaItems.map { it.mediaId }.toHashSet()
                        newMediaItems = jsSongs
                            .map { it.toMediaItem() }
                            .filter { it.mediaId.isNotBlank() && existingIds.add(it.mediaId) }
                            .take(15)
                    }
                    if (newMediaItems.isEmpty()) {
                        val trendingSongs = withContext(Dispatchers.IO) {
                            com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.getTrendingSongs().getOrNull().orEmpty()
                        }
                        val existingIds = player.mediaItems.map { it.mediaId }.toHashSet()
                        newMediaItems = trendingSongs
                            .map { it.toMediaItem() }
                            .filter { it.mediaId.isNotBlank() && existingIds.add(it.mediaId) }
                            .take(15)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed JioSaavn fallback for endless queue", e)
                }
            }

            // 5. Offline database / cache fallback
            if (newMediaItems.isEmpty()) {
                try {
                    val existingIds = player.mediaItems.map { it.mediaId }.toHashSet()
                    val allDbSongs = withContext(Dispatchers.IO) {
                        database.allSongs().first()
                    }
                    val cachedKeys = (playerCache.keys.map { it.toString() } + downloadCache.keys.map { it.toString() }).toSet()
                    val availableSongs = if (cachedKeys.isNotEmpty()) {
                        val filtered = allDbSongs.filter { it.id in cachedKeys }
                        if (filtered.isNotEmpty()) filtered else allDbSongs
                    } else {
                        allDbSongs
                    }

                    val unusedSongs = availableSongs.filter { existingIds.add(it.id) }
                    if (unusedSongs.isNotEmpty()) {
                        newMediaItems = unusedSongs.shuffled().take(15).map { it.toMediaItem() }
                    } else if (availableSongs.isNotEmpty()) {
                        newMediaItems = availableSongs.filter { it.id != seedId }.shuffled().take(15).map { it.toMediaItem() }
                        if (newMediaItems.isEmpty()) {
                            newMediaItems = availableSongs.take(1).map { it.toMediaItem() }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load offline/cached songs for infinite queue", e)
                }
            }

            val excludedSongIds = withContext(Dispatchers.IO) { database.getExcludedSongIds().toHashSet() }
            newMediaItems = newMediaItems.filter { it.mediaId !in excludedSongIds }

            if (newMediaItems.isNotEmpty() && player.playbackState != STATE_IDLE) {
                val previousCount = player.mediaItemCount
                val wasEnded = player.playbackState == Player.STATE_ENDED || !player.isPlaying
                player.addMediaItems(newMediaItems)
                Log.d(TAG, "Endless Queue: Added ${newMediaItems.size} new songs to queue")
                if (autoPlayIfEnded || wasEnded) {
                    player.seekToDefaultPosition(previousCount)
                    player.prepare()
                    player.play()
                }
            } else if (player.playbackState == Player.STATE_ENDED) {
                // If absolutely no new items could be fetched, cycle queue from the start
                if (player.mediaItemCount > 0) {
                    player.seekToDefaultPosition(0)
                    player.prepare()
                    player.play()
                }
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return

        // Guardar canción actual
        val currentSong = player.currentMediaItem

        // Remover otras canciones de la cola
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }

        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(videoId = currentMediaMetadata.id)
            )
            val excludedSongIds = withContext(Dispatchers.IO) { database.getExcludedSongIds().toHashSet() }
            val initialStatus = radioQueue.getInitialStatus().filterExcluded(excludedSongIds)

            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }

            // Agregar canciones de radio después de la canción actual
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            YouTube
                .album(albumId)
                .onSuccess {
                    getAutomix(it.album.playlistId)
                }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore[SimilarContent] == true &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                YouTube
                    .next(WatchEndpoint(playlistId = playlistId))
                    .onSuccess {
                        YouTube
                            .next(WatchEndpoint(playlistId = it.endpoint.playlistId))
                            .onSuccess {
                                val excludedSongIds = withContext(Dispatchers.IO) { database.getExcludedSongIds().toHashSet() }
                                automixItems.value =
                                    it.items.map { song ->
                                        song.toMediaItem()
                                    }.filterExcluded(excludedSongIds)
                            }
                    }
            }
        }
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        // Si la cola está vacía o el reproductor está inactivo, reproducir inmediatamente
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            player.play()
            return
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        // Insertar items inmediatamente después del item actual en el espacio de ventana/índice
        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            // Reconstruir orden aleatorio para que los items recién insertados se reproduzcan a continuación
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex

                // Los índices recién insertados son un rango contiguo [insertIndex, insertIndex + items.size)
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                // Recopilar el orden de recorrido aleatorio existente excluyendo el índice actual
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse() // preservar el orden hacia adelante original

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }

                // Construir nuevo orden aleatorio: actual -> recién insertados (en orden de inserción) -> resto
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                // Llenar cualquier índice faltante (seguridad) para asegurar una permutación completa
                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }

                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }

    fun removeSongFromQueue(songId: String) {
        val count = player.mediaItemCount
        if (count == 0) return

        val currentIndex = player.currentMediaItemIndex
        val isCurrentPlaying = (currentIndex in 0 until count) && player.getMediaItemAt(currentIndex).mediaId == songId

        for (i in count - 1 downTo 0) {
            if (player.getMediaItemAt(i).mediaId == songId) {
                player.removeMediaItem(i)
            }
        }

        automixItems.value = automixItems.value.filter { it.mediaId != songId }

        if (isCurrentPlaying) {
            if (player.mediaItemCount > 0) {
                player.prepare()
                player.play()
            } else {
                player.stop()
                player.clearMediaItems()
            }
        }
    }

    private fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let {
                val updated = it.song.toggleLike()
                update(updated)
                val artistName = it.artists.joinToString { a -> a.name }.ifBlank { null }
                val trackTitle = it.song.title
                if (!artistName.isNullOrBlank() && trackTitle.isNotBlank()) {
                    scope.launch(Dispatchers.IO) {
                        if (updated.liked) {
                            scrobbleRepository.love(artistName, trackTitle)
                        } else {
                            scrobbleRepository.unlove(artistName, trackTitle)
                        }
                    }
                }
            }
        }
    }

    private fun setupLoudnessEnhancer() {
        if (bypassAllAudioEffects.value || (bitPerfectEnabled.value && isBitPerfectActive.value)) {
            loudnessEnhancer?.enabled = false
            return
        }
        val audioSessionId = player.audioSessionId

        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            Log.w(TAG, "setupLoudnessEnhancer: invalid audioSessionId ($audioSessionId), cannot create effect yet")
            return
        }

        // Crear o recrear enhancer si es necesario
        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Log.d(TAG, "LoudnessEnhancer created for sessionId=$audioSessionId")
            } catch (e: Exception) {
                reportException(e)
                loudnessEnhancer = null
                return
            }
        }

        scope.launch {
            try {
                val currentMediaId = withContext(Dispatchers.Main) {
                    player.currentMediaItem?.mediaId
                }

                val normalizeAudio = withContext(Dispatchers.IO) {
                    dataStore.data.map { it[AudioNormalizationKey] ?: false }.first()
                }

                var normGain = 0
                if (normalizeAudio && currentMediaId != null) {
                    val format = withContext(Dispatchers.IO) {
                        database.format(currentMediaId).first()
                    }
                    val loudnessDb = format?.loudnessDb
                    if (loudnessDb != null) {
                        normGain = (-loudnessDb * 100).toInt().coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)
                    }
                }

                val isBoost = audioBoostEnabled.value
                val boostFraction = ((audioBoostPercent.value - 100) / 100f).coerceIn(0f, 1f)
                val boostGainMb = if (isBoost) (boostFraction * 1800).toInt() else 0
                val totalGain = (normGain + boostGainMb).coerceIn(MIN_GAIN_MB, MAX_GAIN_MB + 1800)
                val shouldEnable = isBoost || (normalizeAudio && normGain != 0)

                withContext(Dispatchers.Main) {
                    if (shouldEnable) {
                        try {
                            loudnessEnhancer?.setTargetGain(totalGain)
                            loudnessEnhancer?.enabled = true
                            Log.d(TAG, "LoudnessEnhancer gain applied: $totalGain mB (norm=$normGain, boost=$boostGainMb)")
                        } catch (e: Exception) {
                            reportException(e)
                            releaseLoudnessEnhancer()
                        }
                    } else {
                        loudnessEnhancer?.enabled = false
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
            Log.d(TAG, "LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
            Log.e(TAG, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun equalizerBandKey(index: Int) = intPreferencesKey("equalizerBand$index")

    fun ensureEqualizer() {
        setupEqualizer()
    }

    private fun setupEqualizer() {
        if (bypassAllAudioEffects.value || (bitPerfectEnabled.value && isBitPerfectActive.value)) {
            equalizer?.enabled = false
            equalizerState.value = equalizerState.value.copy(enabled = false)
            return
        }
        val audioSessionId = player.audioSessionId
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            equalizerState.value = equalizerState.value.copy(isAvailable = false)
            return
        }

        if (equalizer != null && equalizerSessionId == audioSessionId) {
            return
        }

        releaseEqualizer()

        try {
            val newEqualizer = Equalizer(0, audioSessionId)
            val range = newEqualizer.bandLevelRange
            val enabled = dataStore.get(EqualizerEnabledKey, false)
            val bandLevels =
                (0 until newEqualizer.numberOfBands).map { index ->
                    dataStore
                        .get(equalizerBandKey(index), 0)
                        .coerceIn(range[0].toInt(), range[1].toInt())
                        .toShort()
                        .also { level ->
                            newEqualizer.setBandLevel(index.toShort(), level)
                        }
                }
            val frequencies =
                (0 until newEqualizer.numberOfBands).map { index ->
                    newEqualizer.getCenterFreq(index.toShort())
                }

            newEqualizer.enabled = enabled
            equalizer = newEqualizer
            equalizerSessionId = audioSessionId
            equalizerState.value =
                EqualizerUiState(
                    isAvailable = true,
                    enabled = enabled,
                    bandLevels = bandLevels,
                    centerFrequencies = frequencies,
                    minBandLevel = range[0],
                    maxBandLevel = range[1],
                )
        } catch (e: Exception) {
            reportException(e)
            equalizerState.value = EqualizerUiState(isAvailable = false)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        if (bypassAllAudioEffects.value && enabled) return
        setupEqualizer()
        try {
            equalizer?.enabled = enabled
            equalizerState.value = equalizerState.value.copy(enabled = enabled)
            scope.launch {
                dataStore.edit { settings ->
                    settings[EqualizerEnabledKey] = enabled
                }
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    fun setEqualizerBandLevel(
        index: Int,
        level: Short,
    ) {
        if (bypassAllAudioEffects.value) return
        setupEqualizer()
        val state = equalizerState.value
        if (index !in state.bandLevels.indices) return

        val clampedLevel =
            level
                .coerceIn(state.minBandLevel, state.maxBandLevel)
        try {
            equalizer?.setBandLevel(index.toShort(), clampedLevel)
            equalizerState.value =
                state.copy(
                    bandLevels =
                        state.bandLevels.toMutableList().also {
                            it[index] = clampedLevel
                        },
                )
            scope.launch {
                dataStore.edit { settings ->
                    settings[equalizerBandKey(index)] = clampedLevel.toInt()
                }
            }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    fun resetEqualizer() {
        setEqualizerEnabled(false)
        setupEqualizer()
        equalizerState.value.bandLevels.indices.forEach { index ->
            setEqualizerBandLevel(index, 0)
        }
    }

    private fun releaseEqualizer() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            reportException(e)
        } finally {
            equalizer = null
            equalizerSessionId = C.AUDIO_SESSION_ID_UNSET
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        setupEqualizer()
        ensureVisualizer()
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        releaseLoudnessEnhancer()
        visualizerManager.isPlaying = false
        runCatching { visualizerManager.stop() }
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            },
        )
    }

    fun setDolbyAtmosEnabled(enabled: Boolean) {
        if (bypassAllAudioEffects.value && enabled) return
        dolbyAtmosEnabled.value = enabled
        scope.launch {
            dataStore.edit { settings ->
                settings[DolbyAtmosEnabledKey] = enabled
            }
        }
        updateSpatialAudio()
    }

    fun setSpatialAudioEnabled(enabled: Boolean) {
        if (bypassAllAudioEffects.value && enabled) return
        spatialAudioEnabled.value = enabled
        scope.launch {
            dataStore.edit { settings ->
                settings[SpatialAudioEnabledKey] = enabled
            }
        }
        updateSpatialAudio()
    }

    fun setAutomixEnabled(enabled: Boolean) {
        automixEnabled.value = enabled
        scope.launch {
            dataStore.edit { settings ->
                settings[AutomixEnabledKey] = enabled
            }
        }
        if (enabled && automixItems.value.isEmpty()) {
            player.currentMediaItem?.mediaId?.let { fetchAutomixRecommendations(it) }
        }
    }

    fun setAutomixPerformanceMode(mode: AutomixPerformanceMode) {
        automixPerformanceMode.value = mode
        trackAnalyzer.setPerformanceMode(mode)
        scope.launch {
            dataStore.edit { settings ->
                settings[AutomixPerformanceModeKey] = mode.name
            }
        }
    }

    fun updateSpatialAudio() {
        if (bypassAllAudioEffects.value || (bitPerfectEnabled.value && isBitPerfectActive.value)) {
            spatialAudioProcessor.enabled = false
            return
        }
        val isNativeAtmos = isTrackDolbyAtmos.value
        spatialAudioProcessor.enabled = (spatialAudioEnabled.value || dolbyAtmosEnabled.value) && !isNativeAtmos
    }

    fun setEightDAudioEnabled(enabled: Boolean) {
        if (bypassAllAudioEffects.value && enabled) return
        eightDAudioEnabled.value = enabled
        updateEightDAudio()
        scope.launch {
            dataStore.edit { settings ->
                settings[EightDAudioEnabledKey] = enabled
            }
        }
    }

    fun setEightDAudioLevel(level: Int) {
        val clamped = level.coerceIn(1, 16)
        eightDAudioLevel.value = clamped
        eightDAudioProcessor.level = clamped
        scope.launch {
            dataStore.edit { settings ->
                settings[EightDAudioLevelKey] = clamped
            }
        }
    }

    fun updateEightDAudio() {
        if (bypassAllAudioEffects.value || (bitPerfectEnabled.value && isBitPerfectActive.value)) {
            eightDAudioProcessor.enabled = false
            return
        }
        eightDAudioProcessor.enabled = eightDAudioEnabled.value
    }

    fun setAudioBoostEnabled(enabled: Boolean) {
        if (bypassAllAudioEffects.value && enabled) return
        audioBoostEnabled.value = enabled
        setupLoudnessEnhancer()
        scope.launch {
            dataStore.edit { settings ->
                settings[AudioBoostEnabledKey] = enabled
            }
        }
    }

    fun setAudioBoostPercent(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        audioBoostPercent.value = clamped
        setupLoudnessEnhancer()
        scope.launch {
            dataStore.edit { settings ->
                settings[AudioBoostPercentKey] = clamped
            }
        }
    }

    fun setEchoEnabled(enabled: Boolean) {
        echoEnabled.value = enabled
        djAudioProcessor.echoEnabled = enabled
        scope.launch { dataStore.edit { it[EchoEnabledKey] = enabled } }
    }

    fun setEchoDelayMs(ms: Int) {
        val clamped = ms.coerceIn(20, 1000)
        echoDelayMs.value = clamped
        djAudioProcessor.echoDelayMs = clamped
        scope.launch { dataStore.edit { it[EchoDelayMsKey] = clamped } }
    }

    fun setEchoFeedback(feedback: Float) {
        val clamped = feedback.coerceIn(0.0f, 0.85f)
        echoFeedback.value = clamped
        djAudioProcessor.echoFeedback = clamped
        scope.launch { dataStore.edit { it[EchoFeedbackKey] = clamped } }
    }

    fun setEchoWetMix(mix: Float) {
        val clamped = mix.coerceIn(0.0f, 1.0f)
        echoWetMix.value = clamped
        djAudioProcessor.echoWetMix = clamped
        scope.launch { dataStore.edit { it[EchoWetMixKey] = clamped } }
    }

    fun setEchoPingPong(enabled: Boolean) {
        echoPingPong.value = enabled
        djAudioProcessor.echoPingPong = enabled
        scope.launch { dataStore.edit { it[EchoPingPongKey] = enabled } }
    }

    fun setDjFilterSweep(sweep: Float) {
        val clamped = sweep.coerceIn(-1.0f, 1.0f)
        djFilterSweep.value = clamped
        djAudioProcessor.filterSweep = clamped
        scope.launch { dataStore.edit { it[DjFilterSweepKey] = clamped } }
    }

    fun setDjFlangerEnabled(enabled: Boolean) {
        djFlangerEnabled.value = enabled
        djAudioProcessor.flangerEnabled = enabled
        scope.launch { dataStore.edit { it[DjFlangerEnabledKey] = enabled } }
    }

    fun setDjFlangerRate(rate: Float) {
        val clamped = rate.coerceIn(0.1f, 4.0f)
        djFlangerRate.value = clamped
        djAudioProcessor.flangerRate = clamped
        scope.launch { dataStore.edit { it[DjFlangerRateKey] = clamped } }
    }

    fun setDjFlangerDepth(depth: Float) {
        val clamped = depth.coerceIn(0.0f, 1.0f)
        djFlangerDepth.value = clamped
        djAudioProcessor.flangerDepth = clamped
        scope.launch { dataStore.edit { it[DjFlangerDepthKey] = clamped } }
    }

    fun setDjSaturation(sat: Float) {
        val clamped = sat.coerceIn(0.0f, 1.0f)
        djSaturation.value = clamped
        djAudioProcessor.saturation = clamped
        scope.launch { dataStore.edit { it[DjSaturationKey] = clamped } }
    }

    fun setDjTempoAndPitch(speed: Float, pitch: Float) {
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        djTempoSpeed.value = clampedSpeed
        djPitch.value = clampedPitch
        player.playbackParameters = androidx.media3.common.PlaybackParameters(clampedSpeed, clampedPitch)
        scope.launch {
            dataStore.edit {
                it[DjTempoSpeedKey] = clampedSpeed
                it[DjPitchKey] = clampedPitch
            }
        }
    }

    fun setDjTurntableLinked(linked: Boolean) {
        djTurntableLinked.value = linked
        if (linked) {
            setDjTempoAndPitch(djTempoSpeed.value, djTempoSpeed.value)
        }
        scope.launch { dataStore.edit { it[DjTurntableLinkedKey] = linked } }
    }

    fun resetDjFx() {
        setEchoEnabled(false)
        setEchoDelayMs(280)
        setEchoFeedback(0.40f)
        setEchoWetMix(0.0f)
        setEchoPingPong(false)
        setDjFilterSweep(0.0f)
        setDjFlangerEnabled(false)
        setDjFlangerRate(0.5f)
        setDjFlangerDepth(0.0f)
        setDjSaturation(0.0f)
        setDjTurntableLinked(false)
        setDjTempoAndPitch(1.0f, 1.0f)
        djAudioProcessor.resetFx()
    }

    fun applyDjPreset(preset: DjPreset) {
        if (bypassAllAudioEffects.value) return
        when (preset) {
            DjPreset.DEFAULT -> {
                resetDjFx()
            }
            DjPreset.CLUB_BOOTH -> {
                setEchoEnabled(true)
                setEchoDelayMs(180)
                setEchoFeedback(0.30f)
                setEchoWetMix(0.35f)
                setEchoPingPong(true)
                setDjFilterSweep(-0.15f)
                setDjSaturation(0.20f)
                setAudioBoostPercent(130)
                setAudioBoostEnabled(true)
                setDjTempoAndPitch(1.0f, 1.0f)
            }
            DjPreset.SLOWED_REVERB -> {
                setEchoEnabled(true)
                setEchoDelayMs(380)
                setEchoFeedback(0.55f)
                setEchoWetMix(0.50f)
                setEchoPingPong(true)
                setDjFilterSweep(-0.25f)
                setDjSaturation(0.15f)
                setDjTurntableLinked(true)
                setDjTempoAndPitch(0.85f, 0.85f)
            }
            DjPreset.NIGHTCORE -> {
                setEchoEnabled(false)
                setDjFilterSweep(0.20f)
                setDjSaturation(0.10f)
                setDjTurntableLinked(true)
                setDjTempoAndPitch(1.25f, 1.25f)
            }
            DjPreset.BASS_BOMB -> {
                setEchoEnabled(false)
                setDjFilterSweep(-0.35f)
                setDjSaturation(0.40f)
                setAudioBoostPercent(160)
                setAudioBoostEnabled(true)
                setDjTempoAndPitch(1.0f, 1.0f)
            }
            DjPreset.LOFI_VINYL -> {
                setEchoEnabled(true)
                setEchoDelayMs(120)
                setEchoFeedback(0.25f)
                setEchoWetMix(0.30f)
                setEchoPingPong(false)
                setDjFilterSweep(-0.50f)
                setDjSaturation(0.35f)
                setDjFlangerEnabled(true)
                setDjFlangerRate(0.2f)
                setDjFlangerDepth(0.25f)
                setDjTempoAndPitch(0.95f, 0.95f)
            }
            DjPreset.SPACE_ECHO -> {
                setEchoEnabled(true)
                setEchoDelayMs(450)
                setEchoFeedback(0.65f)
                setEchoWetMix(0.60f)
                setEchoPingPong(true)
                setDjFilterSweep(0.0f)
                setDjFlangerEnabled(true)
                setDjFlangerRate(0.4f)
                setDjFlangerDepth(0.35f)
                setDjSaturation(0.10f)
                setDjTempoAndPitch(1.0f, 1.0f)
            }
        }
    }

    fun resetAudioFx() {
        setAudioBoostPercent(100)
        setAudioBoostEnabled(false)
        resetEqualizer()
        resetDjFx()
        djAudioProcessor.enabled = false
    }

    fun setAudioFxAndDjEnabled(enabled: Boolean) {
        audioFxAndDjEnabled.value = enabled
        djAudioProcessor.enabled = enabled && !bypassAllAudioEffects.value
        if (!enabled) {
            resetAudioFx()
        }
        scope.launch {
            dataStore.edit { it[AudioFxAndDjEnabledKey] = enabled }
        }
    }

    fun setBypassAllAudioEffects(enabled: Boolean) {
        bypassAllAudioEffects.value = enabled
        applyBypassState(enabled)
        scope.launch {
            dataStore.edit { it[BypassAllAudioEffectsKey] = enabled }
        }
    }

    private fun applyBypassState(bypass: Boolean) {
        spatialAudioProcessor.bypass = bypass
        eightDAudioProcessor.bypass = bypass
        djAudioProcessor.bypass = bypass
        if (bypass) {
            spatialAudioProcessor.enabled = false
            spatialAudioProcessor.resetState()
            eightDAudioProcessor.enabled = false
            eightDAudioProcessor.resetState()
            djAudioProcessor.enabled = false
            djAudioProcessor.resetFx()
            simultaneousAudioProcessor.setEnabled(false)
            try {
                equalizer?.enabled = false
                releaseEqualizer()
            } catch (e: Exception) {
                reportException(e)
            }
            equalizerState.value = equalizerState.value.copy(enabled = false)
            try {
                loudnessEnhancer?.enabled = false
                releaseLoudnessEnhancer()
            } catch (e: Exception) {
                reportException(e)
            }
            audioBoostEnabled.value = false
            audioBoostPercent.value = 100
            player.playbackParameters = androidx.media3.common.PlaybackParameters.DEFAULT
            djTempoSpeed.value = 1.0f
            djPitch.value = 1.0f
            djTurntableLinked.value = false
        } else {
            djAudioProcessor.enabled = audioFxAndDjEnabled.value
            updateSpatialAudio()
            updateEightDAudio()
            setupEqualizer()
            setupLoudnessEnhancer()
        }
    }

    fun ensureVisualizer() {
        val sessionId = player.audioSessionId
        visualizerManager.isPlaying = player.isPlaying
        visualizerManager.start(sessionId)
    }

    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
        var isDolby = false
        for (group in tracks.groups) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                if (group.isTrackSelected(i) && DeviceCodecs.isDolbyAtmosFormat(format)) {
                    isDolby = true
                    break
                }
            }
        }
        isTrackDolbyAtmos.value = isDolby
        updateSpatialAudio()
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        offlineBufferingJob?.cancel()
        val isDeviceOffline = !isNetworkAvailable()
        val isSkipUncached = dataStore.get(SkipUncachedPartKey, false)
        if (isDeviceOffline && isSkipUncached && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            val mediaId = mediaItem?.mediaId
            if (mediaId != null && !isSongFullyCached(mediaId)) {
                Log.i(TAG, "Song $mediaId is uncached while offline with SkipUncachedPart enabled (auto-advance). Skipping.")
                skipToNextPlayableTrack()
                return
            }
        }

        if (isCasting.value) {
            player.pause()
            val metadata = mediaItem?.metadata ?: player.currentMetadata
            if (metadata != null) {
                castPlayback?.load(metadata, positionMs = 0L, autoplay = true)
            }
        }
        mediaItem?.mediaId?.let { id ->
            com.darkxvenom.airbeats.lyrics.LyricsTranslationHelper.onSongChanged(id)
        }
        if (!isCrossfading) {
            scheduleCrossfade()
        }
        lastPlaybackSpeed = -1.0f // forzar actualización de canción

        setupLoudnessEnhancer()
        setupEqualizer()
        updateSpatialAudio()
        ensureVisualizer()

        discordUpdateJob?.cancel()

        // Resetear errores consecutivos cuando hay transición exitosa
        consecutivePlaybackErr = 0
        songLoadingRetryJob?.cancel()
        songLoadingRetryCount = 0

        if (automixEnabled.value && automixItems.value.isEmpty()) {
            mediaItem?.mediaId?.let { fetchAutomixRecommendations(it) }
        }

        // Keep the source queue paged first. When it has no continuation, Infinite queue
        // extends it with a small, de-duplicated related-track tail instead.
        val shouldExtendQueue =
            (dataStore.get(AutoLoadMoreKey, true) || automixEnabled.value) &&
                reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
                player.mediaItemCount - player.currentMediaItemIndex <= 3 &&
                !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)

        if (shouldExtendQueue) {
            if (currentQueue.hasNextPage()) {
                scope.launch(SilentHandler) {
                    val excludedSongIds = withContext(Dispatchers.IO) { database.getExcludedSongIds().toHashSet() }
                    val mediaItems =
                        currentQueue.nextPage()
                            .filterExplicit(dataStore.get(HideExplicitKey, false))
                            .filterExcluded(excludedSongIds)
                    if (player.playbackState != STATE_IDLE && mediaItems.isNotEmpty()) {
                        player.addMediaItems(mediaItems)
                    } else if (mediaItems.isEmpty()) {
                        val seedId = mediaItem?.mediaId ?: player.currentMediaItem?.mediaId
                        if (!seedId.isNullOrBlank()) {
                            extendInfiniteQueue(seedId)
                        }
                    }
                }
            } else {
                val seedId = mediaItem?.mediaId ?: player.currentMediaItem?.mediaId
                if (!seedId.isNullOrBlank()) {
                    extendInfiniteQueue(seedId)
                }
            }
        }

        // Guardar estado cuando cambia el item de medios
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(500) // Pequeño delay para asegurar que el estado esté estable
                saveQueueToDisk()
            }
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        offlineBufferingJob?.cancel()
        if (playbackState == Player.STATE_BUFFERING) {
            val isDeviceOffline = !isNetworkAvailable()
            val isSkipUncached = dataStore.get(SkipUncachedPartKey, false)
            val autoSkipOnError = dataStore.get(AutoSkipNextOnErrorKey, false)
            if (isDeviceOffline && isSkipUncached && autoSkipOnError) {
                val currentMediaId = player.currentMediaItem?.mediaId
                offlineBufferingJob = scope.launch(Dispatchers.Main) {
                    delay(2500)
                    if (player.playbackState == Player.STATE_BUFFERING && !isNetworkAvailable()) {
                        if (currentMediaId != null && !isSongFullyCached(currentMediaId)) {
                            Log.i(TAG, "Playback stalled buffering offline for uncached track $currentMediaId. Skipping.")
                            skipToNextPlayableTrack()
                        }
                    }
                }
            }
        }

        // Guardar estado cuando cambia el estado de reproducción
        if (dataStore.get(PersistentQueueKey, true) && playbackState != Player.STATE_BUFFERING) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }

        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
            visualizerManager.isPlaying = false
            if (!isCrossfading || playbackState == Player.STATE_IDLE) {
                cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
            }
        } else if (playbackState == Player.STATE_READY) {
            scheduleCrossfade()
        }

        // Automatic advance / repeat handling to guarantee playback continuity
        if (playbackState == Player.STATE_ENDED) {
            if (player.mediaItemCount == 0 || player.playbackState == Player.STATE_IDLE) {
                return
            }
            // 1. Si el modo de repetición es REPEAT_MODE_ONE, reiniciar la misma canción
            if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                player.seekTo(0)
                player.prepare()
                player.play()
                return
            }

            // 2. Si el modo de repetición es REPEAT_MODE_ALL, volver al inicio de la cola
            if (player.repeatMode == Player.REPEAT_MODE_ALL) {
                if (player.mediaItemCount > 0) {
                    player.seekToDefaultPosition(0)
                    player.prepare()
                    player.play()
                    return
                }
            }

            // 3. Si el modo aleatorio está activado, mezclar y reiniciar desde el inicio
            if (player.shuffleModeEnabled && player.mediaItemCount > 0) {
                val shuffledIndices = IntArray(player.mediaItemCount) { it }
                shuffledIndices.shuffle()
                player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
                player.seekToDefaultPosition(0)
                player.prepare()
                player.play()
                return
            }

            // 4. Si Endless Queue (AutoLoadMore) está activado, cargar más canciones y continuar
            if (dataStore.get(AutoLoadMoreKey, true)) {
                val currentSeedId = player.currentMediaItem?.mediaId
                    ?: player.mediaItems.lastOrNull()?.mediaId
                if (!currentSeedId.isNullOrBlank()) {
                    extendInfiniteQueue(currentSeedId, autoPlayIfEnded = true)
                    return
                }
            }

            // 5. Ocultar notificación si la cola está vacía
            scope.launch {
                delay(1000)
                if (!player.isPlaying && player.mediaItemCount == 0) {
                    currentMediaMetadata.value = null
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        visualizerManager.isPlaying = isPlaying
        if (isPlaying) {
            ensureVisualizer()
            simultaneousAudioProcessor.onPlay()
        } else {
            simultaneousAudioProcessor.onPause()
        }
        secondaryCrossfadePlayer?.let { secondaryPlayer ->
            if (isCrossfading && !crossfadeHandoffInProgress) {
                val isEndOfOutgoingItemPause =
                    !isPlaying &&
                        !player.playWhenReady &&
                        crossfadePlaybackRequested &&
                        localPlayer.pauseAtEndOfMediaItems
                if (isPlaying || isEndOfOutgoingItemPause) {
                    secondaryPlayer.play()
                } else {
                    secondaryPlayer.pause()
                }
            }
        }
        if (isPlaying && !isCrossfading) {
            scheduleCrossfade()
        }
        updateAudiblePlaybackRecovery()
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        secondaryCrossfadePlayer?.let { secondaryPlayer ->
            if (isCrossfading) {
                val isEndOfOutgoingItemPause =
                    !playWhenReady &&
                        reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM &&
                        crossfadePlaybackRequested &&
                        (localPlayer.pauseAtEndOfMediaItems || crossfadeHandoffInProgress)
                if (!isEndOfOutgoingItemPause) {
                    crossfadePlaybackRequested = playWhenReady
                    secondaryPlayer.playWhenReady = crossfadePlaybackRequested
                    if (crossfadePlaybackRequested) {
                        secondaryPlayer.play()
                    } else {
                        secondaryPlayer.pause()
                    }
                } else if (!crossfadeHandoffInProgress) {
                    secondaryPlayer.playWhenReady = crossfadePlaybackRequested
                    if (crossfadePlaybackRequested) {
                        secondaryPlayer.play()
                    }
                }
            }
        }
        if (playWhenReady && !isCrossfading) {
            scheduleCrossfade()
        } else if (!playWhenReady && !isCrossfading) {
            crossfadeTriggerJob?.cancel()
            crossfadeTriggerJob = null
            localPlayer.pauseAtEndOfMediaItems = false
            releaseSecondaryCrossfadePlayer()
        }

        if (playWhenReady) {
            setupLoudnessEnhancer()
            setupEqualizer()
            ensureVisualizer()
            scope.launch {
                val enabled = dataStore.get(DynamicIslandKey, false)
                if (enabled && Settings.canDrawOverlays(this@MusicService)) {
                    try {
                        startService(Intent(this@MusicService, DynamicIslandService::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // Actualizar notificación cuando cambia el estado de reproducción
        scope.launch {
            delay(300)
            updateNotification()
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
        super.onPlaybackParametersChanged(playbackParameters)
        secondaryCrossfadePlayer?.playbackParameters = playbackParameters
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                if (isCasting.value) {
                    player.pause()
                } else {
                    val focusGranted = requestAudioFocus()
                    if (focusGranted) {
                        openAudioEffectSession()
                    }
                }
            } else {
                closeAudioEffectSession()
                // Abandonar foco de audio cuando no está reproduciendo
                if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    abandonAudioFocus()
                }
            }
        }

        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            if (!isCrossfading) {
                currentMediaMetadata.value = player.currentMetadata
            }
            // Forzar actualización de notificación para asegurar que la imagen se cargue
            scope.launch {
                delay(200)
                updateNotification()
            }
        }

        // Actualización de Discord RPC y Last.fm Now Playing
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (player.isPlaying) {
                currentSong.value?.let { song ->
                    scope.launch {
                        discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                    }
                    val artistName = song.artists.joinToString { a -> a.name }.ifBlank { "Unknown Artist" }
                    val trackTitle = song.song.title
                    val albumTitle = song.song.albumName
                    if (!artistName.isNullOrBlank() && trackTitle.isNotBlank()) {
                        scope.launch(Dispatchers.IO) {
                            scrobbleRepository.submitNowPlaying(artistName, trackTitle, albumTitle, packageName)
                        }
                    }
                }
            } else {
                // Send empty activity to the Discord RPC if the player is not playing
                if (!events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)){
                    scope.launch {
                        discordRpc?.stopActivity()
                    }
                    scrobbleRepository.clearNowPlaying()
                }
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
        val isSeekDiscontinuity =
            reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
        if (isSeekDiscontinuity) {
            if (!crossfadeHandoffInProgress) {
                cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
            }
        }
        if (!isCrossfading && !crossfadeHandoffInProgress) {
            scheduleCrossfade()
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            // Si la cola está vacía, no mezclar
            if (player.mediaItemCount == 0) return

            // Siempre poner el item que se está reproduciendo primero
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] =
                shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }

        // Guardar estado cuando cambia el modo aleatorio
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
        if (!isCrossfading) {
            scheduleCrossfade()
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        // Guardar estado cuando cambia el modo de repetición
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
        if (!isCrossfading) {
            scheduleCrossfade()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        Log.e(TAG, "Player error: ${error.errorCodeName}, message: ${error.message}", error)

        val currentMediaId = player.currentMediaItem?.mediaId
        val currentPos = player.currentPosition.coerceAtLeast(0L)
        val wasActivelyPlaying = currentPos > 3000L

        if (!currentMediaId.isNullOrBlank()) {
            if (jioSaavnAttemptedSongIds.remove(currentMediaId)) {
                Log.w(TAG, "JioSaavn stream failed for track $currentMediaId. Retrying immediately with native stream...")
                jioSaavnFailedSongs.add(currentMediaId)
                songUrlCache.remove(currentMediaId)
                database.query {
                    runCatching {
                        upsert(
                            FormatEntity(
                                id = currentMediaId,
                                itag = 0,
                                mimeType = "",
                                codecs = "",
                                bitrate = 0,
                                sampleRate = 0,
                                contentLength = 0L,
                                loudnessDb = null,
                                playbackUrl = ""
                            )
                        )
                    }
                }
                scope.launch(Dispatchers.Main) {
                    if (wasActivelyPlaying) {
                        player.seekTo(player.currentMediaItemIndex, currentPos)
                    }
                    player.prepare()
                    player.play()
                }
                return
            }
            songUrlCache.remove(currentMediaId)
            YTPlayerUtils.invalidateCachedStreamUrls(currentMediaId)
        }

        val isDeviceOffline = !isNetworkAvailable()
        val skipUncachedPart = dataStore.get(SkipUncachedPartKey, false)
        val autoSkipOnError = dataStore.get(AutoSkipNextOnErrorKey, false)

        // Mid-track playback recovery: if the song was already playing (>3s), NEVER auto-skip it!
        // Stalls or expired stream URLs should be refreshed and resumed from current position.
        if (wasActivelyPlaying && consecutivePlaybackErr < 3) {
            consecutivePlaybackErr++
            Log.w(TAG, "Song was actively playing at ${currentPos}ms when error occurred. Retrying from current position (attempt $consecutivePlaybackErr)...")
            scope.launch(Dispatchers.Main) {
                PlayerConnection.instance?.clearError()
                delay(800)
                if (player.currentMediaItem != null) {
                    player.seekTo(player.currentMediaItemIndex, currentPos)
                    player.prepare()
                    player.play()
                }
            }
            return
        }

        // CRITICAL: When "Auto-skip on error" is DISABLED by user preference,
        // NEVER skip to the next track under ANY circumstances (online or offline).
        if (!autoSkipOnError) {
            Log.w(TAG, "Auto-skip on error is disabled. Pausing player and preserving current track selection.")
            offlineBufferingJob?.cancel()
            songLoadingRetryJob?.cancel()
            songLoadingRetryCount = 0
            consecutivePlaybackErr = 0
            scope.launch(Dispatchers.Main) {
                val errorMsg = error.message?.takeIf { it.isNotBlank() } ?: getString(R.string.error_unknown)
                player.pause()
                Toast.makeText(this@MusicService, errorMsg, Toast.LENGTH_SHORT).show()
            }
            return
        }

        // AutoSkipOnError is TRUE from here onwards:
        // Only skip offline uncached tracks when genuinely offline AND autoSkipOnError is true
        if (isDeviceOffline && skipUncachedPart && (currentMediaId == null || !isSongFullyCached(currentMediaId))) {
            Log.i(TAG, "Uncached track $currentMediaId cannot play offline with skipUncached enabled. Skipping to next playable track.")
            offlineBufferingJob?.cancel()
            skipToNextPlayableTrack()
            return
        }

        if (autoSkipOnError && !wasActivelyPlaying) {
            PlayerConnection.instance?.clearError()
            if (isDeviceOffline && skipUncachedPart) {
                skipToNextPlayableTrack()
                return
            } else if (player.hasNextMediaItem()) {
                val nextIdx = player.currentMediaItemIndex + 1
                Log.i(TAG, "Auto-skipping failed song to next index $nextIdx")
                scope.launch(Dispatchers.Main) {
                    PlayerConnection.instance?.clearError()
                    player.seekToDefaultPosition(nextIdx)
                    player.prepare()
                    player.play()
                }
                return
            }
        }

        if (isDeviceOffline) {
            waitOnNetworkError()
            return
        }

        keepLoadingCurrentSongOnError()
    }

    private val cachedPlaybackLocks = java.util.concurrent.ConcurrentHashMap<String, Any>()

    fun getCachedPlaybackFile(mediaId: String): java.io.File? {
        if (mediaId.isBlank()) return null
        val lock = cachedPlaybackLocks.computeIfAbsent(mediaId) { Any() }
        synchronized(lock) {
            try {
                val playbackCacheDir = java.io.File(cacheDir, "cached_playback").apply { mkdirs() }
                val safeFileName = mediaId.replace(Regex("[^a-zA-Z0-9_-]"), "_")

                // Only allow serving local file if the song is 100% fully downloaded in downloadCache.
                // NEVER serve partial streaming cache from playerCache as a static file, because
                // static partial files hit EOF and force ExoPlayer to prematurely skip the whole song!
                if (!downloadCache.isFullyCached(mediaId)) {
                    // Clean up any stale partial files for this mediaId
                    playbackCacheDir.listFiles { _, name ->
                        name.startsWith("cached_${safeFileName}.")
                    }?.forEach { it.delete() }
                    return null
                }

                val totalCachedBytes = downloadCache.getCachedBytes(mediaId, 0L, Long.MAX_VALUE)
                if (totalCachedBytes < 300_000L) {
                    return null
                }

                // 1. Check if already assembled file exists and is valid
                val existingFiles = playbackCacheDir.listFiles { _, name ->
                    name.startsWith("cached_${safeFileName}.") && !name.endsWith(".tmp")
                }

                if (!existingFiles.isNullOrEmpty()) {
                    val existing = existingFiles.first()
                    if (existing.length() >= totalCachedBytes) {
                        return existing
                    } else {
                        existing.delete()
                    }
                }

                // 2. Locate spans in downloadCache only
                val spans = downloadCache.getCachedSpans(mediaId).sortedBy { it.position }
                if (spans.isEmpty() || spans.first().position != 0L) {
                    return null
                }

                // 3. Verify contiguous bytes from position 0
                var contiguousLength = 0L
                for (span in spans) {
                    if (span.position <= contiguousLength) {
                        contiguousLength = maxOf(contiguousLength, span.position + span.length)
                    } else {
                        break
                    }
                }

                if (contiguousLength < totalCachedBytes) {
                    return null
                }

                val firstSpanFile = spans.first().file ?: return null
                val headerBytes = ByteArray(32)
                val bytesRead = runCatching {
                    firstSpanFile.inputStream().use { it.read(headerBytes) }
                }.getOrDefault(0)
                if (bytesRead < 8) return null

                val ext = com.darkxvenom.airbeats.utils.SaveToStorageUtil.detectExtension(headerBytes)
                val targetFile = java.io.File(playbackCacheDir, "cached_${safeFileName}.$ext")
                val tempFile = java.io.File(playbackCacheDir, "cached_${safeFileName}.$ext.tmp")

                tempFile.outputStream().use { out ->
                    var written = 0L
                    for (span in spans) {
                        if (span.position <= written) {
                            val file = span.file ?: continue
                            val skip = written - span.position
                            file.inputStream().use { input ->
                                if (skip > 0) input.skip(skip)
                                val toCopy = (span.position + span.length) - written
                                if (toCopy > 0) {
                                    var remaining = toCopy
                                    val buffer = ByteArray(8192)
                                    while (remaining > 0) {
                                        val r = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                                        if (r == -1) break
                                        out.write(buffer, 0, r)
                                        remaining -= r
                                        written += r
                                    }
                                }
                            }
                        } else {
                            break // Stop at gap
                        }
                    }
                }

                if (tempFile.length() >= totalCachedBytes) {
                    if (targetFile.exists()) targetFile.delete()
                    if (tempFile.renameTo(targetFile)) {
                        return targetFile
                    } else {
                        return tempFile
                    }
                } else {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error assembling cached playback file for $mediaId")
            }
            return null
        }
    }

    private fun Cache.isFullyCached(mediaId: String): Boolean {
        val contentLength = runCatching { ContentMetadata.getContentLength(getContentMetadata(mediaId)) }.getOrDefault(-1L)
        if (contentLength > 0L) {
            return isCached(mediaId, 0L, contentLength)
        }
        val dbLen: Long = runCatching {
            runBlocking(Dispatchers.IO) {
                database.format(mediaId).firstOrNull()?.contentLength ?: -1L
            }
        }.getOrNull() ?: -1L
        if (dbLen > 0L) {
            return isCached(mediaId, 0L, dbLen)
        }
        return false
    }

    private fun Uri.shouldBypassPlayerCache(): Boolean {
        val normalizedScheme = scheme?.lowercase(java.util.Locale.US)
        return normalizedScheme == "content" ||
            normalizedScheme == "file" ||
            normalizedScheme == "android.resource"
    }

    private class SchemeRoutingDataSource(
        private val selectFactory: (DataSpec) -> DataSource.Factory,
    ) : DataSource {
        private val transferListeners = mutableListOf<TransferListener>()
        private var delegate: DataSource? = null

        override fun addTransferListener(transferListener: TransferListener) {
            transferListeners += transferListener
            delegate?.addTransferListener(transferListener)
        }

        override fun open(dataSpec: DataSpec): Long {
            val selectedFactory = selectFactory(dataSpec)
            val selectedDataSource = selectedFactory.createDataSource()
            transferListeners.forEach(selectedDataSource::addTransferListener)
            delegate = selectedDataSource
            return selectedDataSource.open(dataSpec)
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int = checkNotNull(delegate).read(buffer, offset, length)

        override fun getUri(): Uri? = delegate?.uri

        override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders ?: emptyMap()

        override fun close() {
            val selected = delegate
            delegate = null
            selected?.close()
        }
    }

    private fun createResolvedUpstreamDataSourceFactory(): DataSource.Factory {
        val httpFactory = OkHttpDataSource.Factory(mediaOkHttpClient)
        val mediaFactory = DefaultDataSource.Factory(this, httpFactory)
        return ResolvingDataSource.Factory(mediaFactory, ::resolvePlaybackDataSpec)
    }

    private fun createPlayerCacheDataSourceFactory(cacheWriteEnabled: Boolean = true): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(createResolvedUpstreamDataSourceFactory())
            .apply {
                if (!cacheWriteEnabled) {
                    setCacheWriteDataSinkFactory(null)
                }
            }.setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        val directFactory = createResolvedUpstreamDataSourceFactory()
        val streamingFactory = DataSource.Factory {
            createPlayerCacheDataSourceFactory(cacheWriteEnabled = true).createDataSource()
        }
        val downloadedFactory =
            CacheDataSource.Factory()
                .setCache(downloadCache)
                .setUpstreamDataSourceFactory(streamingFactory)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

        return DataSource.Factory {
            SchemeRoutingDataSource { dataSpec ->
                val mediaId = dataSpec.key
                when {
                    dataSpec.uri.shouldBypassPlayerCache() -> directFactory
                    else -> {
                        if (mediaId != null) {
                            scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                        }
                        if (mediaId != null && downloadCache.isFullyCached(mediaId)) {
                            downloadedFactory
                        } else {
                            streamingFactory
                        }
                    }
                }
            }
        }
    }

    private fun DataSpec.withStreamUrl(url: String, contentLength: Long?): DataSpec {
        val resolved = withUri(url.toUri())
        if (resolved.length != C.LENGTH_UNSET.toLong()) return resolved

        val remainingLength =
            if (contentLength != null && contentLength > 0L) {
                if (resolved.position >= contentLength) {
                    C.LENGTH_UNSET.toLong()
                } else {
                    contentLength - resolved.position
                }
            } else {
                C.LENGTH_UNSET.toLong()
            }

        return if (remainingLength != C.LENGTH_UNSET.toLong()) {
            resolved.buildUpon()
                .setLength(remainingLength)
                .build()
        } else {
            resolved
        }
    }

    private fun verifyJioSaavnUrl(rawUrl: String): String? {
        fun testUrl(targetUrl: String): Boolean {
            return runCatching {
                val req = okhttp3.Request.Builder()
                    .url(targetUrl)
                    .header("Range", "bytes=0-10")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                mediaOkHttpClient.newCall(req).execute().use { resp ->
                    resp.isSuccessful || resp.code in 200..206
                }
            }.getOrDefault(false)
        }

        if (testUrl(rawUrl)) return rawUrl
        val fallback160 = if (rawUrl.contains("_320.")) rawUrl.replace("_320.", "_160.") else null
        if (fallback160 != null && testUrl(fallback160)) return fallback160
        val fallback96 = if (rawUrl.contains("_320.")) rawUrl.replace("_320.", "_96.") else if (rawUrl.contains("_160.")) rawUrl.replace("_160.", "_96.") else null
        if (fallback96 != null && testUrl(fallback96)) return fallback96
        return null
    }

    private fun resolvePlaybackDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.shouldBypassPlayerCache()) {
            return dataSpec
        }

        val mediaId = dataSpec.key ?: dataSpec.uri.toString()

        if (dataSpec.uri.scheme == "file") {
            val fileExists = dataSpec.uri.path?.let { path -> java.io.File(path).isFile } == true
            val isOnlineSong = mediaId.matches(Regex("[A-Za-z0-9_-]{11}")) ||
                mediaId.startsWith("JS:") || mediaId.startsWith("sp:")
            if (fileExists || !isOnlineSong) {
                return dataSpec
            }
            Timber.w("Missing local source for online song $mediaId; resolving a fresh stream instead")
        }

        // 1. Instant Cached Playback: check if song audio is cached locally in downloadCache or playerCache
        val candidateIds = if (mediaId.startsWith("sp:")) {
            listOfNotNull(mediaId, spotifyMatchCache[mediaId])
        } else {
            listOf(mediaId)
        }

        for (candidateId in candidateIds) {
            val cachedFile = getCachedPlaybackFile(candidateId)
            if (cachedFile != null && cachedFile.length() > 0L) {
                Timber.tag("MusicService").d("Resolved instant cached playback for $mediaId via ${cachedFile.name} (${cachedFile.length()} bytes)")
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return dataSpec.buildUpon()
                    .setUri(android.net.Uri.fromFile(cachedFile))
                    .setKey(cachedFile.absolutePath)
                    .setLength(cachedFile.length())
                    .build()
            }
        }

        val checkLength = if (dataSpec.length > 0) dataSpec.length else 1L

        // If the song is downloaded in downloadCache, it is fully on disk and never needs network
        if (downloadCache.isCached(mediaId, dataSpec.position, checkLength)) {
            scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
            return dataSpec
        }

        // If cached in playerCache, play directly from cache
        if (playerCache.isCached(mediaId, dataSpec.position, checkLength)) {
            scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
            return dataSpec
        }

        // If offline, check if downloadCache or playerCache has any cached data for this song
        val hasCache = downloadCache.keys.contains(mediaId) || playerCache.keys.contains(mediaId)
        if (hasCache && !isNetworkAvailable()) {
            if (dataStore.get(SkipUncachedPartKey, false) && !isSongFullyCached(mediaId)) {
                throw PlaybackException(
                    "This song is only partially cached and skip uncached is enabled.",
                    null,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                )
            }
            val cachedBytes = maxOf(
                downloadCache.getCachedBytes(mediaId, 0L, Long.MAX_VALUE),
                playerCache.getCachedBytes(mediaId, 0L, Long.MAX_VALUE),
            )
            if (cachedBytes > 0L) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return dataSpec
            }
        }

        // Also check disk offline cache file
        val safeMediaId = mediaId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val offlineFile = java.io.File(cacheDir, "offline_cache").listFiles()?.firstOrNull {
            it.name.startsWith("cached_${safeMediaId}.") && it.length() > 50_000L
        }
        if (offlineFile != null) {
            scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
            return dataSpec.withStreamUrl(
                android.net.Uri.fromFile(offlineFile).toString(),
                offlineFile.length()
            )
        }

        // If offline and not in any cache, fail gracefully without attempting network
        if (!isNetworkAvailable()) {
            throw PlaybackException(
                "This song is not cached for offline playback. Connect to the internet to stream it.",
                null,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            )
        }

        songUrlCache[mediaId]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let {
            scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
            return dataSpec.withStreamUrl(it.url, it.contentLength)
        }

            var actualMediaId = mediaId
            if (mediaId.startsWith("JS:")) {
                var streamUrl: String? = null
                try {
                    streamUrl = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                        com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.getStreamUrl(mediaId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "JioSaavn stream fetching error for $mediaId")
                }

                val verifiedUrl = streamUrl?.let { verifyJioSaavnUrl(it) }

                if (verifiedUrl != null) {
                    database.query {
                        upsert(
                            FormatEntity(
                                id = mediaId,
                                itag = 141,
                                mimeType = "audio/mp4",
                                codecs = "mp4a.40.2",
                                bitrate = 320000,
                                sampleRate = 44100,
                                contentLength = 0L,
                                loudnessDb = null,
                                playbackUrl = verifiedUrl
                            )
                        )
                    }
                    songUrlCache[mediaId] = CachedSongUrl(
                        url = verifiedUrl,
                        expiresAt = System.currentTimeMillis() + 3600000L,
                        contentLength = null,
                    )
                    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                    return dataSpec.withStreamUrl(verifiedUrl, null)
                }

                // Seamless Fallback: JioSaavn stream failed or null -> find matching YouTube song
                Timber.w("JioSaavn stream unavailable for $mediaId, falling back to YouTube")
                val mediaMetadata = kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                    player.mediaItems.find { it.mediaId == mediaId }?.metadata
                        ?: (if (player.currentMediaItem?.mediaId == mediaId) player.currentMediaItem?.metadata else null)
                        ?: (if (currentMediaMetadata.value?.id == mediaId) currentMediaMetadata.value else null)
                } ?: runBlocking(Dispatchers.IO) {
                    database.song(mediaId).firstOrNull()?.toMediaMetadata()
                }

                val songTitle = mediaMetadata?.title?.takeIf { it.isNotBlank() }
                    ?: kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                        player.mediaItems.find { it.mediaId == mediaId }?.mediaMetadata?.title?.toString()
                            ?: player.currentMediaItem?.mediaMetadata?.title?.toString()
                    }
                val artistName = mediaMetadata?.artists?.firstOrNull()?.name
                    ?: kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                        player.mediaItems.find { it.mediaId == mediaId }?.mediaMetadata?.artist?.toString()
                            ?: player.currentMediaItem?.mediaMetadata?.artist?.toString()
                    }.orEmpty()

                if (!songTitle.isNullOrBlank()) {
                    val query = "$songTitle $artistName".trim()
                    val ytSong = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                        runCatching {
                            YouTube.search(query, com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.FILTER_SONG)
                                .getOrNull()?.items?.firstOrNull() as? com.darkxvenom.airbeats.innertube.models.SongItem
                        }.getOrNull()
                    }
                    if (ytSong != null) {
                        actualMediaId = ytSong.id
                    } else {
                        throw androidx.media3.common.PlaybackException(
                            "JioSaavn stream unavailable and no YouTube fallback match found",
                            null,
                            androidx.media3.common.PlaybackException.ERROR_CODE_REMOTE_ERROR
                        )
                    }
                } else {
                    throw androidx.media3.common.PlaybackException(
                        "JioSaavn stream unavailable",
                        null,
                        androidx.media3.common.PlaybackException.ERROR_CODE_REMOTE_ERROR
                    )
                }
            }
            if (mediaId.startsWith("sp:")) {
                val matchedId = spotifyMatchCache[mediaId]
                if (matchedId != null) {
                    actualMediaId = matchedId
                } else {
                    val mediaMetadata = kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                        player.mediaItems.find { it.mediaId == mediaId }?.metadata
                            ?: (if (player.currentMediaItem?.mediaId == mediaId) player.currentMediaItem?.metadata else null)
                            ?: (if (currentMediaMetadata.value?.id == mediaId) currentMediaMetadata.value else null)
                    } ?: runBlocking(Dispatchers.IO) {
                        database.song(mediaId).firstOrNull()?.toMediaMetadata()
                    }
                    if (mediaMetadata != null) {
                        val artistName = mediaMetadata.artists.firstOrNull()?.name ?: ""
                        val query = "${mediaMetadata.title} $artistName"
                        val searchResult = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                            YouTube.search(query, com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                        }
                        val songItem = searchResult?.items?.firstOrNull() as? com.darkxvenom.airbeats.innertube.models.SongItem
                        if (songItem != null) {
                            actualMediaId = songItem.id
                            spotifyMatchCache[mediaId] = actualMediaId
                        } else {
                            throw androidx.media3.common.PlaybackException(
                                "Spotify track match not found on YouTube",
                                null,
                                androidx.media3.common.PlaybackException.ERROR_CODE_REMOTE_ERROR
                            )
                        }
                    }
                }
            }

            val enableJioSaavn = runBlocking {
                dataStore.data.map { preferences ->
                    preferences[EnableJioSaavnKey] ?: true
                }.first()
            }

            // When JioSaavn integration is enabled, prioritize JioSaavn 320kbps streams first
            if (enableJioSaavn && !mediaId.startsWith("JS:") && !mediaId.startsWith("local:") && !jioSaavnFailedSongs.contains(mediaId)) {
                try {
                    val mediaMetadata = kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                        player.mediaItems.find { it.mediaId == mediaId }?.metadata
                    }
                    if (mediaMetadata != null && mediaMetadata.title.isNotBlank()) {
                        val artistName = mediaMetadata.artists.firstOrNull()?.name ?: ""
                        val jsStreamUrl: String? = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                            runCatching {
                                kotlinx.coroutines.withTimeoutOrNull(4000L) {
                                    com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.findMatchAndStreamUrl(
                                        mediaMetadata.title,
                                        artistName,
                                        mediaMetadata.duration
                                    )
                                }
                            }.getOrNull()
                        }
                        if (jsStreamUrl != null) {
                            val verifiedUrl = verifyJioSaavnUrl(jsStreamUrl)

                            if (verifiedUrl != null) {
                                jioSaavnAttemptedSongIds.add(mediaId)
                                Timber.tag("MusicService").d("JioSaavn Priority: Serving verified 320k for '${mediaMetadata.title}'")
                                database.query {
                                    upsert(
                                        FormatEntity(
                                            id = mediaId,
                                            itag = 141,
                                            mimeType = "audio/mp4",
                                            codecs = "mp4a.40.2",
                                            bitrate = 320000,
                                            sampleRate = 44100,
                                            contentLength = 0L,
                                            loudnessDb = null,
                                            playbackUrl = verifiedUrl
                                        )
                                    )
                                }
                                songUrlCache[mediaId] = CachedSongUrl(
                                    url = verifiedUrl,
                                    expiresAt = System.currentTimeMillis() + 3600000L,
                                    contentLength = null,
                                )
                                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                                return dataSpec.withStreamUrl(verifiedUrl, null)
                            } else {
                                Timber.tag("MusicService").w("JioSaavn stream URL validation failed for '$mediaId'. Falling back directly to YouTube.")
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e is InterruptedException || e is kotlinx.coroutines.CancellationException) {
                        Timber.tag("MusicService").d("JioSaavn priority resolution timed out or cancelled, falling through to YouTube")
                    } else {
                        Timber.tag("MusicService").w(e, "JioSaavn priority resolution error, falling through to YouTube")
                    }
                }
            }

            // YouTube backend (rock-solid primary / fallback)
            val ytLogTag = "YouTube"
            try {
                val playbackData = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                    YTPlayerUtils.playerResponseForPlayback(
                        actualMediaId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                    )
                }.getOrElse { throwable ->
                    when (throwable) {
                        is PlaybackException -> throw throwable

                        is ConnectException, is UnknownHostException -> {
                            throw PlaybackException(
                                getString(R.string.error_no_internet),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                            )
                        }

                        is SocketTimeoutException -> {
                            throw PlaybackException(
                                getString(R.string.error_timeout),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                            )
                        }

                        else -> throw PlaybackException(
                            getString(R.string.error_unknown),
                            throwable,
                            PlaybackException.ERROR_CODE_REMOTE_ERROR
                        )
                    }
                }

                val format = playbackData.format

                database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.split(";")[0],
                            codecs = format.mimeType.split("codecs=").getOrNull(1)?.removeSurrounding("\"").orEmpty(),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = format.contentLength ?: 0L,
                            loudnessDb = playbackData.audioConfig?.loudnessDb,
                            playbackUrl = playbackData.streamUrl
                        )
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, playbackData) }
                val streamUrl = playbackData.streamUrl

                songUrlCache[mediaId] = CachedSongUrl(
                    url = streamUrl,
                    expiresAt = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L),
                    contentLength = format.contentLength,
                )
                return dataSpec.withStreamUrl(streamUrl, format.contentLength)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException && player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                    throw e
                }
                Timber.tag(ytLogTag).e(e, "YouTube playback error, trying JioSaavn fallback")

                val enableJioSaavnFallback = runBlocking {
                    dataStore.data.map { preferences ->
                        preferences[EnableJioSaavnKey] ?: true
                    }.first()
                }

                if (enableJioSaavnFallback && !mediaId.startsWith("JS:")) {
                    try {
                        val mediaMetadata = kotlinx.coroutines.runBlocking(Dispatchers.Main) {
                            player.mediaItems.find { it.mediaId == mediaId }?.metadata
                        }
                        if (mediaMetadata != null) {
                            val artistName = mediaMetadata.artists.firstOrNull()?.name ?: ""
                            val jsStreamUrl = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                                com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.findMatchAndStreamUrl(
                                    mediaMetadata.title,
                                    artistName
                                )
                            }
                            if (jsStreamUrl != null) {
                                val verifiedUrl = verifyJioSaavnUrl(jsStreamUrl)
                                if (verifiedUrl != null) {
                                    jioSaavnAttemptedSongIds.add(mediaId)
                                    database.query {
                                        upsert(
                                            FormatEntity(
                                                id = mediaId,
                                                itag = 141,
                                                mimeType = "audio/mp4",
                                                codecs = "mp4a.40.2",
                                                bitrate = 320000,
                                                sampleRate = 44100,
                                                contentLength = 0L,
                                                loudnessDb = null,
                                                playbackUrl = verifiedUrl
                                            )
                                        )
                                    }
                                    songUrlCache[mediaId] = CachedSongUrl(
                                        url = verifiedUrl,
                                        expiresAt = System.currentTimeMillis() + 3600000L,
                                        contentLength = null,
                                    )
                                    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                                    return dataSpec.withStreamUrl(verifiedUrl, null)
                                }
                            }
                        }
                    } catch (jsEx: Exception) {
                        Timber.tag("JioSaavnFallback").e(jsEx, "JioSaavn fallback failed")
                    }
                }

                // Verificar si la fuente alternativa está habilitada
                val useAlternativeSource = runBlocking {
                    dataStore.data.map { preferences ->
                        val JossRedMultimedia = booleanPreferencesKey("JossRedMultimedia")
                        preferences[JossRedMultimedia] ?: false
                    }.first()
                }

                // Si la fuente alternativa está deshabilitada, relanzar la excepción
                if (!useAlternativeSource) {
                    throw e
                }

                // Fuente alternativa: JossRed (fallback)
                val JRlogTag = "JossRed"
                try {
                    val alternativeUrl = runCatching {
                        runBlocking(Dispatchers.IO) {
                            withTimeout(5000) {
                                AirConnectClient.getStreamingUrl(mediaId)
                            }
                        }
                    }.getOrNull()

                    if (alternativeUrl != null) {
                        // Verificar accesibilidad de URL con una solicitud HEAD
                        val client = OkHttpClient.Builder()
                            .connectTimeout(2, TimeUnit.SECONDS)
                            .readTimeout(2, TimeUnit.SECONDS)
                            .build()

                        val request = Request.Builder()
                            .url(alternativeUrl)
                            .head()
                            .build()

                        try {
                            val response = client.newCall(request).execute()
                            response.use {
                                if (it.isSuccessful) {
                                    Timber.tag(JRlogTag)
                                        .i("Using JossRed URL as fallback: $alternativeUrl")
                                    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                                    return dataSpec.withUri(alternativeUrl.toUri())
                                } else {
                                    Timber.tag(JRlogTag)
                                        .w("JossRed URL unreachable (HTTP ${it.code}), throwing original error")
                                    throw e
                                }
                            }
                        } catch (jrException: Exception) {
                            Timber.tag(JRlogTag).e(
                                jrException,
                                "Error verifying JossRed URL, throwing original error"
                            )
                            throw e
                        }
                    } else {
                        throw e
                    }
                } catch (jrException: Exception) {
                    when (jrException) {
                        is AirConnectClient.JossRedException -> {
                            Timber.tag(JRlogTag)
                                .w("JossRed error: ${jrException.message}, throwing original error")
                        }

                        is TimeoutCancellationException -> {
                            Timber.tag(JRlogTag).w("JossRed timeout, throwing original error")
                        }

                        else -> {
                            Timber.tag(JRlogTag)
                                .e(jrException, "JossRed error, throwing original error")
                        }
                    }
                    throw e
                }
            }
        }

    private fun createMediaSourceFactory(): DefaultMediaSourceFactory {
        val extractorsFactory = ExtractorsFactory {
            arrayOf(
                androidx.media3.extractor.mkv.MatroskaExtractor(),
                androidx.media3.extractor.mp4.Mp4Extractor(androidx.media3.extractor.mp4.Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS),
                androidx.media3.extractor.mp4.FragmentedMp4Extractor(androidx.media3.extractor.mp4.FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS),
                androidx.media3.extractor.mp3.Mp3Extractor(),
                androidx.media3.extractor.ts.AdtsExtractor(),
                androidx.media3.extractor.ogg.OggExtractor(),
                androidx.media3.extractor.flac.FlacExtractor(),
                androidx.media3.extractor.wav.WavExtractor(),
            )
        }
        return DefaultMediaSourceFactory(createDataSourceFactory(), extractorsFactory)
            .setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
                override fun getMinimumLoadableRetryCount(dataType: Int): Int = 10
                override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                    return 1500L
                }
            })
    }

    private fun createRenderersFactory(
        audioProcessors: Array<androidx.media3.common.audio.AudioProcessor> = arrayOf(
            djAudioProcessor,
            spatialAudioProcessor,
            eightDAudioProcessor,
            simultaneousAudioProcessor,
        )
    ) =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink
                .Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        *audioProcessors,
                    ),
                ).build()
        }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem = tryOrNull {
            if (!eventTime.timeline.isEmpty && eventTime.windowIndex in 0 until eventTime.timeline.windowCount) {
                eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
            } else null
        } ?: player.currentMediaItem ?: return

        val durationThresholdMs = dataStore[HistoryDuration]?.times(1000f) ?: 30000f
        val songDurationMs = (mediaItem.metadata?.duration?.takeIf { it > 0 } ?: (playbackStats.totalPlayTimeMs / 1000).toInt()) * 1000L
        val minPlayRequired = if (songDurationMs in 1 until durationThresholdMs.toLong()) {
            songDurationMs * 0.7f
        } else {
            durationThresholdMs
        }

        if (playbackStats.totalPlayTimeMs >= minPlayRequired &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (e: Exception) {
                    reportException(e)
                }
            }

            val song = currentSong.value
            val artistName = mediaItem.mediaMetadata.artist?.toString()?.ifBlank { null }
                ?: song?.artists?.joinToString { a -> a.name }?.ifBlank { null }
            val trackTitle = mediaItem.mediaMetadata.title?.toString()?.ifBlank { null }
                ?: song?.song?.title
            val albumTitle = mediaItem.mediaMetadata.albumTitle?.toString()?.ifBlank { null }
                ?: song?.song?.albumName
            if (!artistName.isNullOrBlank() && !trackTitle.isNullOrBlank()) {
                scope.launch(Dispatchers.IO) {
                    scrobbleRepository.scrobble(
                        artist = artistName,
                        track = trackTitle,
                        album = albumTitle,
                        timestampSec = System.currentTimeMillis() / 1000L,
                        durationMs = songDurationMs,
                        playTimeMs = playbackStats.totalPlayTimeMs
                    )
                }
            }
            val PauseRemoteListenHistoryKey = booleanPreferencesKey("pauseRemoteListenHistory")
            if (!dataStore.get(PauseRemoteListenHistoryKey, false)) {
                scope.launch(Dispatchers.IO) {
                    val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                        ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                            .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    playbackUrl?.let {
                        YouTube.registerPlayback(null, playbackUrl)
                            .onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE && player.mediaItemCount == 0) {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete()
            return
        }

        try {
            val persistQueue =
                PersistQueue(
                    title = queueTitle,
                    items = player.mediaItems.mapNotNull { it.metadata },
                    mediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                    position = if (player.currentPosition >= 0) player.currentPosition else 0,
                )
            val persistAutomix =
                PersistQueue(
                    title = "automix",
                    items = automixItems.value.mapNotNull { it.metadata },
                    mediaItemIndex = 0,
                    position = 0,
                )

            // Guardar estado del reproductor
            val playerState = PersistPlayerState(
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = player.volume,
                currentMediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                currentPosition = if (player.currentPosition >= 0) player.currentPosition else 0,
                playWhenReady = player.playWhenReady, // Estado de reproducción (si está listo para reproducir)
                playbackState = player.playbackState // Estado actual del reproductor
            )

            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistQueue)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving queue to disk", it)
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistAutomix)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving automix to disk", it)
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(playerState)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving player state to disk", it)
                reportException(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveQueueToDisk", e)
            reportException(e)
        }
    }

    override fun onDestroy() {
        offlineBufferingJob?.cancel()
        offlineBufferingJob = null
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        if (discordRpc?.isRpcRunning() == true) {
            discordRpc?.closeRPC()
        }
        discordRpc = null
        abandonAudioFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioDeviceCallback?.let {
                audioManager.unregisterAudioDeviceCallback(it)
            }
        }
        audioDeviceCallback = null
        usbBitPerfectOutput?.clear()
        usbBitPerfectOutput = null
        cancelCrossfade(resetVolume = false, resetPauseAtEnd = true)
        audiblePlaybackRecoveryJob?.cancel()
        audiblePlaybackRecoveryJob = null
        runCatching { trackAnalyzer.release() }
        runCatching { simultaneousAudioProcessor.setEnabled(false) }
        releaseLoudnessEnhancer()
        releaseEqualizer()
        runCatching { visualizerManager.stop() }
        mediaController?.release()
        mediaController = null
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        if (instance == this) {
            instance = null
        }
        runCatching {
            castPlayback?.release()
            castPlayback = null
        }
        super.onDestroy()
    }

    private fun initializeCast() {
        try {
            val castContext = CastContext.getSharedInstance(this)
            castPlayback = CastPlayback(
                context = this,
                listener = object : CastPlaybackListener {
                    override suspend fun resolveCastStream(mediaId: String): ResolvedCastStream? {
                        return this@MusicService.resolveAudioStreamForCast(mediaId)
                    }

                    override fun onCastSessionStarted(deviceName: String) {
                        isCasting.value = true
                        isCastPlaying.value = true
                        castDeviceName.value = deviceName
                        player.pause()
                        currentMediaMetadata.value?.let { track ->
                            val pos = player.currentPosition.coerceAtLeast(0L)
                            castPlayback?.load(track, pos, autoplay = true)
                        }
                    }

                    override fun onCastSessionEnded() {
                        val wasCasting = isCasting.value
                        val lastCastPosition = castPositionMs.value
                        isCasting.value = false
                        isCastPlaying.value = false
                        castDeviceName.value = null
                        castPositionMs.value = 0L
                        castDurationMs.value = 0L
                        if (wasCasting && player.currentMediaItem != null) {
                            player.seekTo(lastCastPosition)
                        }
                    }

                    override fun onCastTrackEnded() {
                        scope.launch(Dispatchers.Main) {
                            if (player.hasNextMediaItem()) {
                                player.seekToNextMediaItem()
                            }
                        }
                    }

                    override fun onCastStateUpdated(
                        isPlaying: Boolean,
                        isBuffering: Boolean,
                        positionMs: Long,
                        durationMs: Long,
                    ) {
                        isCastPlaying.value = isPlaying
                        castPositionMs.value = positionMs
                        if (durationMs > 0) {
                            castDurationMs.value = durationMs
                        }
                    }

                    override fun onCastError(message: String) {
                        Timber.e("Cast error: $message")
                        Toast.makeText(this@MusicService, message, Toast.LENGTH_SHORT).show()
                    }
                },
                scope = scope,
                castContext = castContext,
            ).apply {
                initialize()
            }
        } catch (e: Throwable) {
            Timber.w(e, "Chromecast CastContext initialization skipped or unavailable")
        }
    }

    suspend fun resolveAudioStreamForCast(mediaId: String): ResolvedCastStream? {
        val localUri = withContext(Dispatchers.Main) {
            player.mediaItems.find { it.mediaId == mediaId }?.localConfiguration?.uri
        }
        if (localUri != null && (localUri.scheme == "content" || localUri.scheme == "file")) {
            val detectedMime = runCatching {
                if (localUri.scheme == "content") contentResolver.getType(localUri)
                else null
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "audio/mpeg"
            return ResolvedCastStream(
                url = localUri.toString(),
                mimeType = detectedMime
            )
        }
        if (mediaId.startsWith("content://") || mediaId.startsWith("file://")) {
            val uri = android.net.Uri.parse(mediaId)
            val detectedMime = runCatching {
                if (uri.scheme == "content") contentResolver.getType(uri)
                else null
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "audio/mpeg"
            return ResolvedCastStream(
                url = mediaId,
                mimeType = detectedMime
            )
        }

        // Check if the song is available locally in downloadCache or playerCache (offline playback)
        val candidateIds = if (mediaId.startsWith("sp:")) {
            listOfNotNull(mediaId, spotifyMatchCache[mediaId])
        } else {
            listOf(mediaId)
        }
        for (candidateId in candidateIds) {
            val cachedFile = getCachedPlaybackFile(candidateId)
            if (cachedFile != null && cachedFile.length() > 0) {
                val mimeType = when (cachedFile.extension) {
                    "m4a", "mp4" -> "audio/mp4"
                    "opus", "ogg" -> "audio/ogg"
                    "flac" -> "audio/flac"
                    "wav" -> "audio/wav"
                    else -> "audio/mpeg"
                }
                return ResolvedCastStream(
                    url = android.net.Uri.fromFile(cachedFile).toString(),
                    mimeType = mimeType
                )
            }
        }

        songUrlCache[mediaId]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let { cached ->
            return ResolvedCastStream(
                url = cached.url,
                mimeType = "audio/mp4"
            )
        }

        var actualMediaId = mediaId

        if (mediaId.startsWith("JS:")) {
            try {
                val streamUrl = withContext(Dispatchers.IO) {
                    com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.getStreamUrl(mediaId)
                }
                if (streamUrl != null) {
                    return ResolvedCastStream(url = streamUrl, mimeType = "audio/mp4")
                }
            } catch (e: Exception) {
                Timber.e(e, "Cast: JioSaavn stream fetching error for $mediaId")
            }
        }

        if (mediaId.startsWith("sp:")) {
            val matchedId = spotifyMatchCache[mediaId]
            if (matchedId != null) {
                actualMediaId = matchedId
            } else {
                val mediaMetadata = withContext(Dispatchers.Main) {
                    player.mediaItems.find { it.mediaId == mediaId }?.metadata ?: currentMediaMetadata.value
                }
                if (mediaMetadata != null) {
                    val artistName = mediaMetadata.artists.firstOrNull()?.name ?: ""
                    val query = "${mediaMetadata.title} $artistName"
                    val searchResult = withContext(Dispatchers.IO) {
                        YouTube.search(query, com.darkxvenom.airbeats.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                    }
                    val songItem = searchResult?.items?.firstOrNull() as? SongItem
                    if (songItem != null) {
                        actualMediaId = songItem.id
                        spotifyMatchCache[mediaId] = actualMediaId
                    }
                }
            }
        }

        val enableJioSaavn = dataStore.data.map { preferences ->
            preferences[EnableJioSaavnKey] ?: true
        }.first()

        if (enableJioSaavn && !actualMediaId.startsWith("JS:") && !actualMediaId.startsWith("local:")) {
            try {
                val mediaMetadata = withContext(Dispatchers.Main) {
                    player.mediaItems.find { it.mediaId == mediaId }?.metadata ?: currentMediaMetadata.value
                }
                if (mediaMetadata != null && mediaMetadata.title.isNotBlank()) {
                    val artistName = mediaMetadata.artists.firstOrNull()?.name ?: ""
                    val jsStreamUrl = withContext(Dispatchers.IO) {
                        runCatching {
                            kotlinx.coroutines.withTimeoutOrNull(4000L) {
                                com.darkxvenom.airbeats.jiosaavn.JioSaavnApi.findMatchAndStreamUrl(
                                    mediaMetadata.title,
                                    artistName,
                                    mediaMetadata.duration
                                )
                            }
                        }.getOrNull()
                    }
                    if (jsStreamUrl != null) {
                        return ResolvedCastStream(url = jsStreamUrl, mimeType = "audio/mp4")
                    }
                }
            } catch (e: Exception) {
                if (e is InterruptedException || e is kotlinx.coroutines.CancellationException) {
                    Timber.d("Cast: JioSaavn priority resolution timed out or cancelled")
                } else {
                    Timber.w(e, "Cast: JioSaavn priority resolution failed")
                }
            }
        }

        try {
            val playbackData = withContext(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    actualMediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager
                )
            }.getOrNull()

            if (playbackData != null) {
                val clientParam = android.net.Uri.parse(playbackData.streamUrl).getQueryParameter("c")?.trim().orEmpty()
                val userAgent = com.darkxvenom.airbeats.utils.StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = com.darkxvenom.airbeats.utils.StreamClientUtils.resolveOriginReferer(clientParam)
                val headers = mutableMapOf<String, String>("User-Agent" to userAgent)
                originReferer.origin?.let { headers["Origin"] = it }
                originReferer.referer?.let { headers["Referer"] = it }

                return ResolvedCastStream(
                    url = playbackData.streamUrl,
                    mimeType = playbackData.format.mimeType.split(";")[0],
                    requestHeaders = headers
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Cast: YouTube playback resolution failed")
        }

        return null
    }

    fun toggleCastPlayPause() {
        castPlayback?.let {
            if (isCastPlaying.value) {
                it.pause()
                isCastPlaying.value = false
            } else {
                it.play()
                isCastPlaying.value = true
            }
        }
    }

    fun seekCastTo(positionMs: Long) {
        castPlayback?.seek(positionMs)
        castPositionMs.value = positionMs
    }

    fun disconnectCast() {
        castPlayback?.disconnect()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (dataStore.get(StopMusicOnTaskClearKey, false) || player.currentMediaItem == null) {
            stopSelf()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun startForegroundService(service: Intent?): ComponentName? {
        return try {
            super.startForegroundService(service)
        } catch (e: Exception) {
            // Android 12+ (API 31+) / Android 14+ / Android 15 (API 35) throws
            // ForegroundServiceStartNotAllowedException when startForegroundService() is called from background.
            // Catching here prevents uncaught fatal crash during async MediaNotificationManager transitions.
            Timber.e(e, "ForegroundServiceStartNotAllowedException caught and suppressed in startForegroundService")
            null
        }
    }

    override fun startService(service: Intent?): ComponentName? {
        return try {
            super.startService(service)
        } catch (e: Exception) {
            Timber.e(e, "Exception caught and suppressed in startService")
            null
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // Android 12+ rejects a foreground-service promotion which originates
        // from the background. Media3 can request one while creating an idle
        // session (for example after an async controller/Firebase callback),
        // even though there is no media to play. Do not promote an idle service;
        // active playback still receives its normal media notification.
        val hasPlaybackToPromote = player.currentMediaItem != null && player.playWhenReady
        val requiresForeground = !isCasting.value && startInForegroundRequired && hasPlaybackToPromote
        try {
            super.onUpdateNotification(session, requiresForeground)
        } catch (e: Exception) {
            Timber.e(e, "Suppressed onUpdateNotification error")
        }
    }

    fun fetchAutomixRecommendations(seedId: String) {
        if (!isNetworkAvailable() || seedId.isBlank()) return
        scope.launch(Dispatchers.IO) {
            try {
                val effectiveYtId = if (seedId.startsWith("JS:") || seedId.startsWith("local:")) {
                    val metadata = player.mediaItems.find { it.mediaId == seedId }?.metadata
                    val query = "${metadata?.title.orEmpty()} ${metadata?.artists?.firstOrNull()?.name.orEmpty()}".trim()
                    if (query.isNotBlank()) {
                        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()?.items?.firstOrNull()?.id
                    } else null
                } else seedId

                if (!effectiveYtId.isNullOrBlank()) {
                    val nextResult = YouTube.next(WatchEndpoint(videoId = effectiveYtId)).getOrNull()
                    val candidateSongs = nextResult?.items.orEmpty()
                    val existingIds = player.mediaItems.map { it.mediaId }.toHashSet()
                    val excludedSongIds = database.getExcludedSongIds().toHashSet()
                    val items = candidateSongs
                        .map { it.toMediaItem() }
                        .filter { it.mediaId.isNotBlank() && it.mediaId !in excludedSongIds && existingIds.add(it.mediaId) }
                        .take(15)

                    if (items.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            automixItems.value = items
                            // Crossfade only blends the queue the listener
                            // already chose. Automix additionally builds a
                            // related-song continuation, so it has a real
                            // purpose even after the source queue ends.
                            if (automixEnabled.value) {
                                player.addMediaItems(items)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to fetch automix recommendations for $seedId")
            }
        }
    }

    // ── ArchiveTune Exact Crossfade Engine ──────────────────────────────────────

    private fun currentEffectivePlayerVolume(): Float {
        val vol = playerVolume.value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f
        val focus = audioFocusVolumeFactor.value.takeIf { it.isFinite() }?.coerceIn(0.2f, 1f) ?: 1f
        val fade = playbackFadeFactor.value.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 1f
        return if (bitPerfectEnabled.value && isBitPerfectActive.value) {
            1.0f
        } else {
            (vol * focus * fade).coerceIn(0f, maxSafeGainFactor)
        }
    }

    private fun currentEffectivePlayerVolumeForMediaId(mediaId: String): Float =
        currentEffectivePlayerVolume()

    private fun applyEffectiveVolumeImmediately(finalVolume: Float = currentEffectivePlayerVolume()) {
        applyEffectiveVolume(finalVolume)
    }

    private fun applyEffectiveVolume(finalVolume: Float = currentEffectivePlayerVolume()) {
        crossfadeBaseVolume = finalVolume
        val incomingPlayer = secondaryCrossfadePlayer
        if (crossfadeHandoffInProgress && incomingPlayer != null) {
            val handoffBaseVolume =
                secondaryCrossfadeTarget?.let { currentEffectivePlayerVolumeForMediaId(it.mediaId) }
                    ?: finalVolume
            crossfadeIncomingBaseVolume = handoffBaseVolume
            applyCrossfadeVolumes(
                crossfadeHandoffProgress,
                handoffBaseVolume,
                handoffBaseVolume,
                incomingPlayer,
                localPlayer,
            )
            return
        }
        if (isCrossfading && incomingPlayer != null) {
            val incomingBaseVolume =
                secondaryCrossfadeTarget?.let { currentEffectivePlayerVolumeForMediaId(it.mediaId) }
                    ?: finalVolume
            crossfadeIncomingBaseVolume = incomingBaseVolume
            applyCrossfadeVolumes(crossfadeProgress, finalVolume, incomingBaseVolume, localPlayer, incomingPlayer)
            return
        }
        if (::player.isInitialized) {
            player.volume = finalVolume
        }
        incomingPlayer?.volume = 0f
    }

    private fun ensureAudiblePlaybackVolume(reason: String) {
        if (!::player.isInitialized) return
        if (isCrossfading || crossfadeHandoffInProgress) return
        if (!shouldKeepPlaybackAudible()) return
        if (playerVolume.value <= 0f) return

        val expectedVolume = currentEffectivePlayerVolume()
        if (expectedVolume <= MIN_AUDIBLE_EFFECTIVE_VOLUME) return
        if (player.volume > STUCK_MUTED_VOLUME_EPSILON) return

        Timber.tag(TAG).w(
            "Restoring muted primary player volume during active playback: reason=%s expected=%s actual=%s",
            reason,
            expectedVolume,
            player.volume,
        )
        applyEffectiveVolumeImmediately(expectedVolume)
    }

    private fun shouldKeepPlaybackAudible(): Boolean {
        if (!::player.isInitialized) return false
        if (player.currentMediaItem == null || !player.playWhenReady) return false
        return player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED
    }

    private fun updateAudiblePlaybackRecovery() {
        if (!::player.isInitialized || !shouldKeepPlaybackAudible()) {
            audiblePlaybackRecoveryJob?.cancel()
            audiblePlaybackRecoveryJob = null
            return
        }

        if (audiblePlaybackRecoveryJob?.isActive == true) return
        audiblePlaybackRecoveryJob =
            scope.launch {
                while (isActive && shouldKeepPlaybackAudible()) {
                    ensureAudiblePlaybackVolume("watchdog")
                    delay(AUDIBLE_PLAYBACK_VOLUME_CHECK_MS)
                }
                audiblePlaybackRecoveryJob = null
            }
    }

    private fun applyCrossfadeVolumes(
        progress: Float,
        outgoingBaseVolume: Float,
        incomingBaseVolume: Float,
        outgoingPlayer: ExoPlayer,
        incomingPlayer: ExoPlayer,
    ) {
        val gains = equalPowerGains(progress)
        outgoingPlayer.volume = (outgoingBaseVolume * gains.outgoing).coerceIn(0f, maxSafeGainFactor)
        incomingPlayer.volume = (incomingBaseVolume * gains.incoming).coerceIn(0f, maxSafeGainFactor)
    }

    private fun scheduleCrossfade() {
        if (!::player.isInitialized) return
        crossfadeTriggerJob?.cancel()
        crossfadeTriggerJob = null

        if (isCrossfading) return
        if (!player.playWhenReady || sleepTimer.pauseWhenSongEnd) {
            localPlayer.pauseAtEndOfMediaItems = false
            releaseSecondaryCrossfadePlayer()
            return
        }

        val currentMediaId = player.currentMediaItem?.mediaId ?: return
        if (crossfadeSuppressedMediaId == currentMediaId) {
            localPlayer.pauseAtEndOfMediaItems = false
            releaseSecondaryCrossfadePlayer()
            return
        }
        if (crossfadeSuppressedMediaId != null) {
            crossfadeSuppressedMediaId = null
        }

        val target = resolveCrossfadeTarget()
        val duration = player.duration
        val effectiveDuration = effectiveCrossfadeDuration(duration)
        if (target == null || effectiveDuration == null) {
            localPlayer.pauseAtEndOfMediaItems = false
            releaseSecondaryCrossfadePlayer()
            return
        }

        val currentIndex = player.currentMediaItemIndex
        val triggerAt = duration - effectiveDuration - CROSSFADE_END_GUARD_MS

        crossfadeTriggerJob =
            scope.launch {
                var hasPreparedSecondaryPlayer = false
                while (isActive) {
                    if (!crossfadeEnabled || isCrossfading) return@launch
                    if (player.currentMediaItem?.mediaId != currentMediaId || player.currentMediaItemIndex != currentIndex) {
                        return@launch
                    }
                    if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                        return@launch
                    }

                    val remainingToTrigger = triggerAt - player.currentPosition
                    if (!hasPreparedSecondaryPlayer && remainingToTrigger <= CROSSFADE_PREPARE_AHEAD_MS) {
                        prepareSecondaryCrossfadePlayer(target)
                        hasPreparedSecondaryPlayer = true
                    }
                    if (remainingToTrigger <= 0L) {
                        val adjustedDuration =
                            (duration - player.currentPosition - CROSSFADE_END_GUARD_MS)
                                .coerceAtMost(effectiveDuration)
                        if (adjustedDuration >= MIN_CROSSFADE_DURATION_MS) {
                            startCrossfade(target, adjustedDuration)
                        }
                        return@launch
                    }

                    val sleepMs =
                        when {
                            remainingToTrigger > 5_000L -> 1_000L
                            remainingToTrigger > 1_000L -> 250L
                            else -> 50L
                        }.coerceAtMost(remainingToTrigger).coerceAtLeast(1L)
                    delay(sleepMs)
                }
            }
    }

    private fun resolveCrossfadeTarget(): CrossfadeTarget? {
        if (!crossfadeEnabled || crossfadeDurationMs <= 0L) return null
        if (player.mediaItemCount == 0 || player.currentTimeline.isEmpty) return null
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) return null

        val currentIndex = player.currentMediaItemIndex
        if (currentIndex !in 0 until player.mediaItemCount) return null

        val repeatCurrent = player.repeatMode == Player.REPEAT_MODE_ONE
        val targetIndex = if (repeatCurrent) currentIndex else player.nextMediaItemIndex
        if (targetIndex == C.INDEX_UNSET || targetIndex !in 0 until player.mediaItemCount) return null
        if (!repeatCurrent && targetIndex == currentIndex) return null

        val currentItem = player.getMediaItemAt(currentIndex)
        val targetItem = player.getMediaItemAt(targetIndex)
        if (!repeatCurrent && crossfadeGapless && isGaplessAlbumTransition(currentItem, targetItem)) return null

        return CrossfadeTarget(
            index = targetIndex,
            mediaId = targetItem.mediaId,
        )
    }

    private fun effectiveCrossfadeDuration(duration: Long): Long? {
        if (duration == C.TIME_UNSET || duration <= 0L) return null
        val maxDuration = duration - CROSSFADE_END_GUARD_MS
        if (maxDuration < MIN_CROSSFADE_DURATION_MS) return null
        return crossfadeDurationMs
            .coerceAtLeast(MIN_CROSSFADE_DURATION_MS)
            .coerceAtMost(maxDuration)
    }

    private fun isGaplessAlbumTransition(
        currentItem: MediaItem,
        targetItem: MediaItem,
    ): Boolean {
        val currentAlbum =
            currentItem.metadata
                ?.album
                ?.id
                ?.takeIf { it.isNotBlank() }
                ?: currentItem.metadata
                    ?.album
                    ?.title
                    ?.takeIf { it.isNotBlank() }
                ?: currentItem.mediaMetadata.albumTitle
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
        val targetAlbum =
            targetItem.metadata
                ?.album
                ?.id
                ?.takeIf { it.isNotBlank() }
                ?: targetItem.metadata
                    ?.album
                    ?.title
                    ?.takeIf { it.isNotBlank() }
                ?: targetItem.mediaMetadata.albumTitle
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
        return currentAlbum != null && currentAlbum == targetAlbum
    }

    private fun prepareSecondaryCrossfadePlayer(target: CrossfadeTarget): ExoPlayer? {
        val existingPlayer = secondaryCrossfadePlayer
        if (existingPlayer != null && secondaryCrossfadeTarget == target) {
            return existingPlayer
        }

        releaseSecondaryCrossfadePlayer()

        val targetItem =
            runCatching { player.getMediaItemAt(target.index) }
                .getOrNull()
                ?.takeIf { it.mediaId == target.mediaId }
                ?: return null

        return runCatching {
            createSecondaryCrossfadePlayer().also { secondaryPlayer ->
                secondaryCrossfadePlayer = secondaryPlayer
                secondaryCrossfadeTarget = target
                secondaryPlayer.setMediaItem(targetItem)
                secondaryPlayer.playbackParameters = player.playbackParameters
                secondaryPlayer.volume = 0f
                secondaryPlayer.prepare()
            }
        }.onFailure { error ->
            Timber.tag(TAG).w(error, "Failed to prepare crossfade player")
            releaseSecondaryCrossfadePlayer()
        }.getOrNull()
    }

    private fun createCrossfadeLoadControl(): DefaultLoadControl =
        DefaultLoadControl
            .Builder()
            .setBufferDurationsMs(
                CROSSFADE_MIN_BUFFER_MS,
                CROSSFADE_MAX_BUFFER_MS,
                CROSSFADE_MIN_BUFFER_BEFORE_START_MS.toInt(),
                CROSSFADE_MIN_BUFFER_BEFORE_START_MS.toInt(),
            ).setPrioritizeTimeOverSizeThresholds(true)
            .build()

    private fun createSecondaryCrossfadePlayer(): ExoPlayer =
        ExoPlayer
            .Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setRenderersFactory(createRenderersFactory(audioProcessors = emptyArray()))
            .setLoadControl(createCrossfadeLoadControl())
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false,
            ).setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
            .apply {
                addListener(secondaryCrossfadeListener)
            }

    private fun startCrossfade(
        target: CrossfadeTarget,
        durationMs: Long,
    ) {
        if (isCrossfading || !crossfadeEnabled) return

        val incomingPlayer = prepareSecondaryCrossfadePlayer(target) ?: return
        val outgoingMediaId = player.currentMediaItem?.mediaId ?: return

        crossfadeTriggerJob?.cancel()
        crossfadeTriggerJob = null
        crossfadeJob?.cancel()
        crossfadeJob =
            scope.launch {
                isCrossfading = true
                crossfadeProgress = 0f
                crossfadeBaseVolume = currentEffectivePlayerVolume()
                crossfadeIncomingBaseVolume = currentEffectivePlayerVolumeForMediaId(target.mediaId)
                crossfadePlaybackRequested = player.playWhenReady

                try {
                    val requiredBufferedMs = requiredCrossfadeStartBufferMs(durationMs)
                    if (!awaitCrossfadePlayerReady(incomingPlayer, CROSSFADE_READY_TIMEOUT_MS, requiredBufferedMs)) {
                        abortCrossfadeAndResumePrimary("secondary_player_not_ready")
                        return@launch
                    }

                    val hasMovedToAnotherMediaItem = player.currentMediaItem?.mediaId != outgoingMediaId
                    if (!crossfadePlaybackRequested) {
                        cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
                        return@launch
                    }
                    if (hasMovedToAnotherMediaItem) {
                        cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
                        scheduleCrossfade()
                        return@launch
                    }
                    if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                        abortCrossfadeAndResumePrimary("primary_ended_before_crossfade_start")
                        return@launch
                    }

                    val currentDuration = player.duration
                    val remainingDurationMs =
                        (
                            if (currentDuration != C.TIME_UNSET) {
                                (currentDuration - player.currentPosition).coerceAtLeast(0L)
                            } else {
                                durationMs
                            }
                        ).coerceAtMost(durationMs)
                    if (remainingDurationMs < MIN_CROSSFADE_DURATION_MS) {
                        abortCrossfadeAndResumePrimary("secondary_player_ready_too_late")
                        return@launch
                    }

                    localPlayer.pauseAtEndOfMediaItems = true

                    incomingPlayer.playbackParameters = player.playbackParameters
                    incomingPlayer.playWhenReady = crossfadePlaybackRequested
                    if (crossfadePlaybackRequested) {
                        incomingPlayer.play()
                    }

                    var elapsedMs = 0L
                    var lastTickMs = android.os.SystemClock.elapsedRealtime()
                    while (isActive && elapsedMs < remainingDurationMs) {
                        if (player.currentMediaItem?.mediaId != outgoingMediaId) {
                            cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
                            scheduleCrossfade()
                            return@launch
                        }

                        val nowMs = android.os.SystemClock.elapsedRealtime()
                        if (crossfadePlaybackRequested) {
                            incomingPlayer.playWhenReady = true
                            elapsedMs = (elapsedMs + (nowMs - lastTickMs)).coerceAtMost(remainingDurationMs)
                            crossfadeProgress =
                                (elapsedMs.toFloat() / remainingDurationMs.toFloat()).coerceIn(0f, 1f)
                            applyCrossfadeVolumes(
                                crossfadeProgress,
                                crossfadeBaseVolume,
                                crossfadeIncomingBaseVolume,
                                localPlayer,
                                incomingPlayer,
                            )
                        } else {
                            incomingPlayer.pause()
                        }
                        lastTickMs = nowMs
                        delay(CROSSFADE_FRAME_MS)
                    }

                    finishCrossfade(target, incomingPlayer)
                } catch (error: kotlinx.coroutines.CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Timber.tag(TAG).w(error, "Crossfade failed")
                    abortCrossfadeAndResumePrimary("crossfade_exception")
                }
            }
    }

    private suspend fun awaitCrossfadePlayerReady(
        crossfadePlayer: ExoPlayer,
        timeoutMs: Long,
        minimumBufferedMs: Long,
    ): Boolean {
        val deadlineMs = android.os.SystemClock.elapsedRealtime() + timeoutMs
        while (kotlinx.coroutines.currentCoroutineContext().isActive && android.os.SystemClock.elapsedRealtime() < deadlineMs) {
            when (crossfadePlayer.playbackState) {
                Player.STATE_READY -> {
                    if (hasBufferedForSmoothStart(crossfadePlayer, minimumBufferedMs)) {
                        return true
                    }
                }

                Player.STATE_IDLE -> {
                    crossfadePlayer.prepare()
                }

                Player.STATE_ENDED -> {
                    return false
                }
            }
            delay(50L)
        }
        return crossfadePlayer.playbackState == Player.STATE_READY &&
            hasBufferedForSmoothStart(crossfadePlayer, minimumBufferedMs)
    }

    private suspend fun finishCrossfade(
        target: CrossfadeTarget,
        incomingPlayer: ExoPlayer,
    ) {
        val targetIndex = resolveCrossfadeTargetIndex(target)
        if (targetIndex == C.INDEX_UNSET) {
            cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
            return
        }

        val incomingPosition = incomingPlayer.currentPosition.coerceAtLeast(0L)
        val shouldContinuePlayback = crossfadePlaybackRequested

        var handoffCompleted = false
        try {
            crossfadeHandoffInProgress = true
            crossfadeHandoffProgress = 0f
            localPlayer.pauseAtEndOfMediaItems = false
            player.volume = 0f
            player.seekTo(targetIndex, incomingPosition)
            player.playWhenReady = shouldContinuePlayback
            if (shouldContinuePlayback) {
                if (!awaitPrimaryCrossfadeHandoffReady(incomingPlayer)) {
                    abortCrossfadeAndResumePrimary("primary_handoff_not_ready")
                    handoffCompleted = true
                    return
                }

                val primaryPosition = player.currentPosition.coerceAtLeast(0L)
                val secondaryPosition = incomingPlayer.currentPosition.coerceAtLeast(0L)
                if (needsCorrectiveCrossfadeSeek(
                        primaryPositionMs = primaryPosition,
                        secondaryPositionMs = secondaryPosition,
                        maximumDriftMs = CROSSFADE_HANDOFF_MAX_DRIFT_MS,
                    )
                ) {
                    player.seekTo(targetIndex, secondaryPosition)
                }

                if (!performCrossfadeHandoff(targetIndex, incomingPlayer)) {
                    if (crossfadePlaybackRequested && player.currentMediaItemIndex == targetIndex) {
                        abortCrossfadeAndResumePrimary("primary_handoff_failed")
                    } else {
                        cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)
                    }
                    handoffCompleted = true
                    return
                }
            } else {
                incomingPlayer.pause()
            }
            currentMediaMetadata.value = player.getMediaItemAt(targetIndex).metadata
            handoffCompleted = true
        } finally {
            if (!handoffCompleted) {
                crossfadeHandoffInProgress = false
                crossfadeHandoffProgress = 0f
                isCrossfading = false
                crossfadeProgress = 0f
                crossfadePlaybackRequested = false
                releaseSecondaryCrossfadePlayer()
                applyEffectiveVolumeImmediately()
            }
        }

        isCrossfading = false
        crossfadeHandoffInProgress = false
        crossfadeHandoffProgress = 0f
        crossfadeProgress = 0f
        crossfadeIncomingBaseVolume = 1f
        crossfadePlaybackRequested = false
        releaseSecondaryCrossfadePlayer()
        applyEffectiveVolumeImmediately()
        updateAudiblePlaybackRecovery()
        scheduleCrossfade()
    }

    private suspend fun awaitPrimaryCrossfadeHandoffReady(incomingPlayer: ExoPlayer): Boolean {
        val deadlineMs = android.os.SystemClock.elapsedRealtime() + CROSSFADE_HANDOFF_READY_TIMEOUT_MS
        while (kotlinx.coroutines.currentCoroutineContext().isActive && android.os.SystemClock.elapsedRealtime() < deadlineMs) {
            if (player.playbackState == Player.STATE_READY && canHandoffWithoutRebuffer(incomingPlayer)) {
                return true
            }
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                return false
            }
            delay(25L)
        }
        return player.playbackState == Player.STATE_READY && canHandoffWithoutRebuffer(incomingPlayer)
    }

    private suspend fun awaitPrimaryPositionAdvance(
        targetIndex: Int,
        positionAfterSeekMs: Long,
    ): Boolean {
        val deadlineMs = android.os.SystemClock.elapsedRealtime() + CROSSFADE_HANDOFF_READY_TIMEOUT_MS
        while (kotlinx.coroutines.currentCoroutineContext().isActive && android.os.SystemClock.elapsedRealtime() < deadlineMs) {
            if (!crossfadePlaybackRequested || !player.playWhenReady) return false
            if (player.currentMediaItemIndex != targetIndex) return false
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) return false
            if (player.playbackState == Player.STATE_READY &&
                player.isPlaying &&
                hasPlaybackPositionAdvanced(positionAfterSeekMs, player.currentPosition)
            ) {
                return true
            }
            delay(CROSSFADE_HANDOFF_POLL_MS)
        }
        return player.currentMediaItemIndex == targetIndex &&
            player.playbackState == Player.STATE_READY &&
            player.isPlaying &&
            hasPlaybackPositionAdvanced(positionAfterSeekMs, player.currentPosition)
    }

    private suspend fun performCrossfadeHandoff(
        targetIndex: Int,
        incomingPlayer: ExoPlayer,
    ): Boolean {
        var startedAtMs = android.os.SystemClock.elapsedRealtime()
        var lastConfirmedPrimaryPositionMs = player.currentPosition.coerceAtLeast(0L)
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            if (!crossfadePlaybackRequested || !player.playWhenReady) {
                incomingPlayer.pause()
                return false
            }
            if (player.currentMediaItemIndex != targetIndex) return false
            if (player.playbackState != Player.STATE_READY || !player.isPlaying) {
                crossfadeHandoffProgress = 0f
                applyEffectiveVolume()
                if (!awaitPrimaryPositionAdvance(targetIndex, lastConfirmedPrimaryPositionMs)) return false
                lastConfirmedPrimaryPositionMs = player.currentPosition.coerceAtLeast(0L)
                startedAtMs = android.os.SystemClock.elapsedRealtime()
                continue
            }

            val elapsedMs = android.os.SystemClock.elapsedRealtime() - startedAtMs
            crossfadeHandoffProgress =
                (elapsedMs.toFloat() / CROSSFADE_HANDOFF_DURATION_MS.toFloat()).coerceIn(0f, 1f)
            val handoffBaseVolume =
                secondaryCrossfadeTarget?.let { currentEffectivePlayerVolumeForMediaId(it.mediaId) }
                    ?: crossfadeIncomingBaseVolume
            applyCrossfadeVolumes(
                crossfadeHandoffProgress,
                handoffBaseVolume,
                handoffBaseVolume,
                incomingPlayer,
                localPlayer,
            )
            if (crossfadeHandoffProgress >= 1f) return true
            delay(CROSSFADE_HANDOFF_FRAME_MS)
        }
        return false
    }

    private fun canHandoffWithoutRebuffer(incomingPlayer: ExoPlayer): Boolean {
        if (player.currentMediaItem
                ?.localConfiguration
                ?.uri
                ?.shouldBypassPlayerCache() == true
        ) {
            return true
        }
        if (hasBufferedForSmoothStart(localPlayer, CROSSFADE_HANDOFF_BUFFER_MS)) {
            val bufferedPosition = localPlayer.bufferedPosition
            val incomingPosition = incomingPlayer.currentPosition.coerceAtLeast(0L)
            return bufferedPosition == C.TIME_UNSET ||
                incomingPosition + CROSSFADE_HANDOFF_SEEK_GUARD_MS <= bufferedPosition
        }
        return false
    }

    private fun requiredCrossfadeStartBufferMs(durationMs: Long): Long =
        (durationMs + CROSSFADE_HANDOFF_BUFFER_MS)
            .coerceAtLeast(CROSSFADE_MIN_BUFFER_BEFORE_START_MS)
            .coerceAtMost(CROSSFADE_MAX_BUFFER_BEFORE_START_MS)

    private fun hasBufferedForSmoothStart(
        targetPlayer: ExoPlayer,
        minimumBufferedMs: Long,
    ): Boolean {
        if (minimumBufferedMs <= 0L) return true
        if (targetPlayer.currentMediaItem
                ?.localConfiguration
                ?.uri
                ?.shouldBypassPlayerCache() == true
        ) {
            return true
        }

        val duration = targetPlayer.duration
        val currentPosition = targetPlayer.currentPosition.coerceAtLeast(0L)
        val remainingDuration =
            if (duration != C.TIME_UNSET && duration > currentPosition) {
                duration - currentPosition
            } else {
                Long.MAX_VALUE
            }
        val requiredBufferedMs = minimumBufferedMs.coerceAtMost(remainingDuration)
        if (requiredBufferedMs <= 0L) return true

        val bufferedDuration = targetPlayer.totalBufferedDuration.coerceAtLeast(0L)
        if (bufferedDuration >= requiredBufferedMs) return true

        return duration != C.TIME_UNSET &&
            targetPlayer.bufferedPosition >= duration - CROSSFADE_END_GUARD_MS
    }

    private fun resolveCrossfadeTargetIndex(target: CrossfadeTarget): Int {
        if (target.index in 0 until player.mediaItemCount &&
            player.getMediaItemAt(target.index).mediaId == target.mediaId
        ) {
            return target.index
        }

        for (index in 0 until player.mediaItemCount) {
            if (player.getMediaItemAt(index).mediaId == target.mediaId) {
                return index
            }
        }
        return C.INDEX_UNSET
    }

    private fun cancelCrossfade(
        resetVolume: Boolean,
        resetPauseAtEnd: Boolean,
    ) {
        crossfadeTriggerJob?.cancel()
        crossfadeTriggerJob = null
        crossfadeJob?.cancel()
        crossfadeJob = null
        isCrossfading = false
        crossfadeHandoffInProgress = false
        crossfadeHandoffProgress = 0f
        crossfadeProgress = 0f
        crossfadeIncomingBaseVolume = 1f
        crossfadePlaybackRequested = false
        if (::player.isInitialized && resetPauseAtEnd) {
            localPlayer.pauseAtEndOfMediaItems = false
        }
        releaseSecondaryCrossfadePlayer()
        if (resetVolume && ::player.isInitialized) {
            applyEffectiveVolumeImmediately()
        }
    }

    private fun abortCrossfadeAndResumePrimary(reason: String) {
        if (!::player.isInitialized) return

        val currentMediaId = player.currentMediaItem?.mediaId
        val targetIndex = secondaryCrossfadeTarget?.let(::resolveCrossfadeTargetIndex) ?: C.INDEX_UNSET
        val shouldResumePlayback = crossfadePlaybackRequested || player.playWhenReady
        val primaryAtEnd =
            player.playbackState == Player.STATE_ENDED ||
                (player.duration != C.TIME_UNSET && player.currentPosition >= player.duration)

        crossfadeSuppressedMediaId = currentMediaId

        Timber.tag(TAG).w("Falling back to primary playback after crossfade failure: reason=%s", reason)
        cancelCrossfade(resetVolume = true, resetPauseAtEnd = true)

        if (!shouldResumePlayback || player.currentMediaItem == null) return

        if (primaryAtEnd && targetIndex != C.INDEX_UNSET) {
            player.seekTo(targetIndex, 0L)
        }
        player.play()
    }

    private fun releaseSecondaryCrossfadePlayer() {
        val playerToRelease = secondaryCrossfadePlayer ?: return
        secondaryCrossfadePlayer = null
        secondaryCrossfadeTarget = null
        runCatching { playerToRelease.removeListener(secondaryCrossfadeListener) }
        runCatching { playerToRelease.stop() }
        runCatching { playerToRelease.clearMediaItems() }
        runCatching { playerToRelease.release() }
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        @Volatile
        var instance: MusicService? = null
            private set

        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"

        // Constants for audio normalization
        private const val MAX_GAIN_MB = 800 // Maximum gain in millibels (8 dB)
        private const val MIN_GAIN_MB = -800 // Minimum gain in millibels (-8 dB)

        const val MIN_CROSSFADE_DURATION_MS = 500L
        const val CROSSFADE_END_GUARD_MS = 150L
        const val CROSSFADE_PREPARE_AHEAD_MS = 30_000L
        const val CROSSFADE_READY_TIMEOUT_MS = 5_000L
        const val CROSSFADE_HANDOFF_READY_TIMEOUT_MS = 5_000L
        const val CROSSFADE_HANDOFF_BUFFER_MS = 5_000L
        const val CROSSFADE_HANDOFF_SEEK_GUARD_MS = 750L
        const val CROSSFADE_HANDOFF_MAX_DRIFT_MS = 75L
        const val CROSSFADE_HANDOFF_DURATION_MS = 96L
        const val CROSSFADE_HANDOFF_FRAME_MS = 8L
        const val CROSSFADE_HANDOFF_POLL_MS = 10L
        const val CROSSFADE_MIN_BUFFER_BEFORE_START_MS = 5_000L
        const val CROSSFADE_MAX_BUFFER_BEFORE_START_MS = 12_500L
        const val CROSSFADE_MIN_BUFFER_MS = 15_000
        const val CROSSFADE_MAX_BUFFER_MS = 45_000
        const val CROSSFADE_FRAME_MS = 32L
        const val AUDIBLE_PLAYBACK_VOLUME_CHECK_MS = 2_000L
        const val MIN_AUDIBLE_EFFECTIVE_VOLUME = 0.01f
        const val STUCK_MUTED_VOLUME_EPSILON = 0.001f
        const val MAX_AUDIO_NORMALIZATION_FACTOR = 1.414f

        private const val TAG = "MusicService"
    }
}


