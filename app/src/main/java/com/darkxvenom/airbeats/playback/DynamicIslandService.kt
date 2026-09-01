package com.darkxvenom.airbeats.playback

import android.animation.ValueAnimator
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.*
import com.darkxvenom.airbeats.extensions.currentMetadata
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.utils.dataStore
import kotlinx.coroutines.*
import kotlin.math.roundToInt

object AppForegroundTracker {
    var isForeground = false
        set(value) {
            field = value
            notifyListeners()
        }
    var isAdjustingIsland = false
        set(value) {
            field = value
            notifyListeners()
        }
    private val listeners = mutableListOf<(Boolean, Boolean) -> Unit>()
    fun addListener(listener: (Boolean, Boolean) -> Unit) {
        listeners.add(listener)
    }
    fun removeListener(listener: (Boolean, Boolean) -> Unit) {
        listeners.remove(listener)
    }
    private fun notifyListeners() {
        listeners.forEach { it(isForeground, isAdjustingIsland) }
    }
}

class DynamicIslandService : Service(), Player.Listener {
    private val scope = CoroutineScope(Dispatchers.Main) + Job()
    private lateinit var windowManager: WindowManager
    private lateinit var islandView: DynamicIslandView
    private var isAdded = false
    private var isAppInForeground = false

    private var portraitOffsetX = 0
    private var portraitOffsetY = 8
    private var portraitWidth = 160
    private var portraitHeight = 36

    private var landscapeOffsetX = 0
    private var landscapeOffsetY = 8
    private var landscapeWidth = 160
    private var landscapeHeight = 36

    private var islandBgColor = Color.BLACK
    private var islandAccentColor = Color.rgb(229, 19, 69)
    private var islandTextColor = Color.WHITE
    private var pauseDismissJob: Job? = null

    private val isLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private val currentOffsetX: Int
        get() = if (isLandscape) landscapeOffsetX else portraitOffsetX

    private val currentOffsetY: Int
        get() = if (isLandscape) landscapeOffsetY else portraitOffsetY

    private val currentIslandWidth: Int
        get() = if (isLandscape) landscapeWidth else portraitWidth

    private val currentIslandHeight: Int
        get() = if (isLandscape) landscapeHeight else portraitHeight

    private val foregroundListener: (Boolean, Boolean) -> Unit = { isForeground, isAdjusting ->
        isAppInForeground = isForeground
        if (isForeground && !isAdjusting) {
            hideIsland()
        } else {
            updateIsland()
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        islandView =
            DynamicIslandView(
                context = this,
                onExpandedChanged = { updateLayout() },
                onShuffle = {
                    MusicService.instance?.player?.let { player ->
                        player.shuffleModeEnabled = !player.shuffleModeEnabled
                        updateIsland()
                    }
                },
                onPrevious = {
                    MusicService.instance?.player?.let { player ->
                        player.seekToPrevious()
                        updateIsland()
                    }
                },
                onPlayPause = {
                    MusicService.instance?.player?.let { player ->
                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            player.play()
                        }
                        updateIsland()
                    }
                },
                onNext = {
                    MusicService.instance?.player?.let { player ->
                        player.seekToNext()
                        updateIsland()
                    }
                },
                onRepeat = {
                    MusicService.instance?.player?.let { player ->
                        player.repeatMode = when (player.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                        updateIsland()
                    }
                },
                onSeekTo = { posMs ->
                    MusicService.instance?.player?.seekTo(posMs)
                }
            )

        MusicService.instance?.player?.addListener(this)
        AppForegroundTracker.addListener(foregroundListener)
        isAppInForeground = AppForegroundTracker.isForeground

        scope.launch {
            dataStore.data.collect { prefs ->
                portraitOffsetX = prefs[DynamicIslandOffsetXKey] ?: 0
                portraitOffsetY = prefs[DynamicIslandOffsetYKey] ?: 8
                portraitWidth = prefs[DynamicIslandWidthKey] ?: 160
                portraitHeight = prefs[DynamicIslandHeightKey] ?: 36

                landscapeOffsetX = prefs[DynamicIslandLandscapeOffsetXKey] ?: 0
                landscapeOffsetY = prefs[DynamicIslandLandscapeOffsetYKey] ?: 8
                landscapeWidth = prefs[DynamicIslandLandscapeWidthKey] ?: 160
                landscapeHeight = prefs[DynamicIslandLandscapeHeightKey] ?: 36

                islandBgColor = prefs[DynamicIslandBgColorKey] ?: Color.BLACK
                islandAccentColor = prefs[DynamicIslandAccentColorKey] ?: Color.rgb(229, 19, 69)
                islandTextColor = prefs[DynamicIslandTextColorKey] ?: Color.WHITE

                islandView.applyDimensionsAndColors(
                    currentIslandWidth,
                    currentIslandHeight,
                    islandBgColor,
                    islandAccentColor,
                    islandTextColor,
                    currentOffsetY
                )
                updateLayout()
            }
        }

        // Periodic progress updater for timeline
        scope.launch {
            while (isActive) {
                if (isAdded && !isAppInForeground) {
                    val player = MusicService.instance?.player
                    if (player != null && player.isPlaying) {
                        islandView.updatePlaybackProgress(
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
                            isPlaying = player.isPlaying
                        )
                    }
                }
                delay(400L)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        islandView.applyDimensionsAndColors(
            currentIslandWidth,
            currentIslandHeight,
            islandBgColor,
            islandAccentColor,
            islandTextColor,
            currentOffsetY
        )
        updateLayout()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MusicService.instance?.player?.let { player ->
            player.removeListener(this)
            player.addListener(this)
        }
        updateIsland()
        return START_STICKY
    }

    override fun onEvents(player: Player, events: Player.Events) {
        updateIsland()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updateIsland()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateIsland()
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        updateIsland()
    }

    private fun updateIsland() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        if (AppForegroundTracker.isAdjustingIsland) {
            islandView.update(
                metadata = MediaMetadata(
                    id = "preview",
                    title = "Adjusting position & size...",
                    artists = emptyList(),
                    duration = 180,
                    thumbnailUrl = null,
                ),
                isPlaying = true,
                positionMs = 65000,
                durationMs = 180000,
                isShuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_OFF,
            )
            showIsland()
            loadArtwork(null)
            return
        }

        val player = MusicService.instance?.player
        if (player == null) {
            hideIsland()
            return
        }

        val metadata = player.currentMediaItem?.mediaMetadata
        val appMetadata = player.currentMetadata
        val hasSong =
            player.currentMediaItem != null &&
                player.playbackState != Player.STATE_IDLE &&
                player.playbackState != Player.STATE_ENDED

        if (!hasSong) {
            hideIsland()
            return
        }

        if (isAppInForeground) {
            hideIsland()
            return
        }

        val isPlaybackActive = player.playWhenReady && player.playbackState == Player.STATE_READY
        islandView.update(
            metadata =
                appMetadata ?: MediaMetadata(
                    id = player.currentMediaItem?.mediaId.orEmpty(),
                    title = metadata?.title?.toString().orEmpty(),
                    artists = metadata?.artist?.toString()?.let {
                        listOf(MediaMetadata.Artist(null, it))
                    } ?: emptyList(),
                    duration = (player.duration / 1000).toInt(),
                    thumbnailUrl = null,
                ),
            isPlaying = isPlaybackActive,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            isShuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )

        if (isPlaybackActive) {
            pauseDismissJob?.cancel()
            pauseDismissJob = null
            showIsland()
        } else {
            // Paused: Keep island visible but schedule automatic dismiss if paused for 3 minutes
            if (pauseDismissJob == null || pauseDismissJob?.isActive == false) {
                pauseDismissJob = scope.launch {
                    delay(3 * 60 * 1000L) // 3 minutes
                    hideIsland()
                }
            }
        }

        loadArtwork(appMetadata?.thumbnailUrl ?: metadata?.artworkUri?.toString())
    }

    private fun loadArtwork(url: String?) {
        if (url.isNullOrBlank()) {
            islandView.setArtwork(null)
            return
        }
        scope.launch {
            val bitmap =
                withContext(Dispatchers.IO) {
                    runCatching {
                        val result =
                            ImageLoader(this@DynamicIslandService).execute(
                                ImageRequest
                                    .Builder(this@DynamicIslandService)
                                    .data(url)
                                    .allowHardware(false)
                                    .build()
                            )
                        (result as? SuccessResult)?.drawable?.toBitmap()
                    }.getOrNull()
                }
            islandView.setArtwork(bitmap)
        }
    }

    private fun showIsland() {
        if (isAdded) {
            islandView.invalidate()
            updateLayout()
            return
        }
        windowManager.addView(islandView, layoutParams())
        isAdded = true
    }

    private fun hideIsland() {
        if (!isAdded) return
        windowManager.removeView(islandView)
        isAdded = false
    }

    private fun updateLayout() {
        if (isAdded) {
            windowManager.updateViewLayout(islandView, layoutParams())
        }
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val isExp = islandView.expanded

        val width = if (isExp) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            currentIslandWidth.coerceAtLeast(32).dp
        }

        val height = if (isExp) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            currentIslandHeight.coerceAtLeast(20).dp
        }

        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = if (isExp) 0 else currentOffsetX.dp
            y = if (isExp) 0 else currentOffsetY.dp

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    override fun onDestroy() {
        AppForegroundTracker.removeListener(foregroundListener)
        hideIsland()
        MusicService.instance?.player?.removeListener(this)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).roundToInt()
}

private class DynamicIslandView(
    context: Context,
    private val onExpandedChanged: () -> Unit,
    private val onShuffle: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onRepeat: () -> Unit,
    private val onSeekTo: (Long) -> Unit,
) : View(context) {
    var expanded = false
        private set

    var customWidth: Int = 160
        private set

    var customHeight: Int = 36
        private set

    private var currentOffsetY = 8
    private var isLiquidGlass = false
    private var rotationAngle = 0f

    private var downX = 0f
    private var downY = 0f
    private var isTap = false
    private var isDraggingSeekbar = false
    private var scrubPositionMs = 0L

    private val density = resources.displayMetrics.density
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val strokePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
        }
    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    private val subTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 255, 255, 255)
            textSize = 12f * density
        }
    private val progressPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(75, 255, 255, 255)
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            strokeWidth = 4f * density
        }
    private val progressFillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(229, 19, 69)
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
            strokeWidth = 4f * density
        }
    private val playButtonBgPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, 255, 255, 255)
            style = Paint.Style.FILL
        }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(229, 19, 69) }
    private val spotifyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 215, 96) }

    private var metadata: MediaMetadata? = null
    private var artwork: Bitmap? = null
    private var isPlaying = false
    private var positionMs = 0L
    private var durationMs = 0L
    private var isShuffleEnabled = false
    private var repeatMode = Player.REPEAT_MODE_OFF
    private val islandBounds = RectF()
    private val collapsedBounds = RectF()
    private val expandedBounds = RectF()
    private var morphProgress = 0f
    private var morphAnimator: ValueAnimator? = null
    private var morphAnimating = false
    private val dropPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(44, 255, 255, 255)
        }

    private val animationRunnable =
        object : Runnable {
            override fun run() {
                if (isPlaying) {
                    rotationAngle = (rotationAngle + 0.9f) % 360f
                    invalidate()
                    postDelayed(this, 16L) // ~60 FPS smooth rotation
                }
            }
        }

    fun applyDimensionsAndColors(
        width: Int,
        height: Int,
        bgColor: Int,
        accentColor: Int,
        textColor: Int,
        offsetY: Int
    ) {
        this.customWidth = width
        this.customHeight = height
        this.currentOffsetY = offsetY

        bgPaint.shader = null
        bgPaint.color = bgColor
        strokePaint.color = Color.argb(60, 255, 255, 255)
        strokePaint.strokeWidth = 1f * density

        accentPaint.color = accentColor
        spotifyPaint.color = accentColor
        progressFillPaint.color = accentColor
        textPaint.color = textColor
        subTextPaint.color = Color.argb(185, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
        invalidate()
    }

    fun update(metadata: MediaMetadata, isPlaying: Boolean, positionMs: Long, durationMs: Long, isShuffleEnabled: Boolean, repeatMode: Int) {
        this.metadata = metadata
        this.isPlaying = isPlaying
        if (!isDraggingSeekbar) {
            this.positionMs = positionMs
        }
        this.durationMs = durationMs
        this.isShuffleEnabled = isShuffleEnabled
        this.repeatMode = repeatMode
        removeCallbacks(animationRunnable)
        if (isPlaying) {
            post(animationRunnable)
        }
        invalidate()
    }

    fun updatePlaybackProgress(positionMs: Long, durationMs: Long, isPlaying: Boolean) {
        if (!isDraggingSeekbar) {
            this.positionMs = positionMs
        }
        this.durationMs = durationMs
        this.isPlaying = isPlaying
        invalidate()
    }

    fun setArtwork(bitmap: Bitmap?) {
        artwork = bitmap
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateIslandBounds()
        val corner = if (!expanded && morphProgress == 0f) {
            islandBounds.height() / 2f
        } else {
            lerp(islandBounds.height() / 2f, 24f.dp, morphProgress)
        }

        // Draw solid background
        canvas.drawRoundRect(islandBounds, corner, corner, bgPaint)

        drawDropEffect(canvas)
        canvas.drawRoundRect(islandBounds.insetBy(0.5f.dp), corner, corner, strokePaint)

        val checkpoint = canvas.save()
        canvas.translate(islandBounds.left, islandBounds.top)
        if (expanded && (!morphAnimating || morphProgress > 0.45f)) {
            drawExpanded(canvas)
        } else {
            drawCollapsed(canvas)
        }
        canvas.restoreToCount(checkpoint)
    }

    private fun drawCollapsed(canvas: Canvas) {
        val localWidth = islandBounds.width()
        val localHeight = islandBounds.height()

        if (localWidth <= 52f.dp) {
            // Mini Dot Mode (Clean rotating circular artwork without indicator dot)
            val padding = 3f.dp
            val art = RectF(padding, padding, localWidth - padding, localHeight - padding)
            drawArtwork(canvas, art, corner = (localHeight - padding * 2) / 2f, rotate = true)
        } else if (localWidth < 110f.dp) {
            // Compact Mini-Pill Mode (Rotating circular artwork on left, waveform on right)
            val artSize = (localHeight - 8f.dp).coerceAtLeast(14f.dp)
            val art = RectF(4f.dp, 4f.dp, 4f.dp + artSize, 4f.dp + artSize)
            drawArtwork(canvas, art, corner = artSize / 2f, rotate = true)
            drawSpotifyWaveform(canvas, localWidth - 22f.dp, localHeight / 2f, compact = true)
        } else {
            // Standard Full Pill Mode (Rotating circular artwork on left, live dot in center, waveform on right)
            val artSize = (localHeight - 8f.dp).coerceAtLeast(14f.dp)
            val art = RectF(5f.dp, 4f.dp, 5f.dp + artSize, 4f.dp + artSize)
            drawArtwork(canvas, art, corner = artSize / 2f, rotate = true)
            drawLiveDot(canvas, localWidth / 2f, localHeight / 2f)
            drawSpotifyWaveform(canvas, localWidth - 34f.dp, localHeight / 2f, compact = true)
        }
    }

    private fun drawExpanded(canvas: Canvas) {
        val localWidth = islandBounds.width()
        val title = metadata?.title.orEmpty().ifBlank { "AirBeats" }
        val artists = metadata?.artists?.joinToString { it.name }.orEmpty()
        val art = RectF(24f.dp, 28f.dp, 78f.dp, 82f.dp)
        drawArtwork(canvas, art, corner = 14f.dp, rotate = false)
        canvas.drawText(title.ellipsize(24), 92f.dp, 46f.dp, textPaint)
        canvas.drawText(artists.ellipsize(30), 92f.dp, 65f.dp, subTextPaint)
        drawSpotifyWaveform(canvas, localWidth - 44f.dp, 42f.dp, compact = false)

        // Progress Bar & Squiggly Wave Seekbar
        val progressStart = 72f.dp
        val progressEnd = localWidth - 72f.dp
        val progressY = 104f.dp
        val curMs = if (isDraggingSeekbar) scrubPositionMs else positionMs
        canvas.drawText(formatTime(curMs), 24f.dp, 108f.dp, subTextPaint)
        canvas.drawText(formatTime(durationMs), localWidth - 58f.dp, 108f.dp, subTextPaint)

        // Base Track Line
        canvas.drawLine(progressStart, progressY, progressEnd, progressY, progressPaint)

        val progress =
            if (durationMs > 0) {
                (curMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        val progressCurrentX = progressStart + (progressEnd - progressStart) * progress

        if (progressCurrentX > progressStart) {
            if (isPlaying && !isDraggingSeekbar) {
                // Animated Squiggly Wave Timeline matching classic player SquigglySlider
                val wavePath = Path()
                wavePath.moveTo(progressStart, progressY)
                val wavelength = 22f.dp
                val freq = (2f * Math.PI.toFloat()) / wavelength
                val phase = (SystemClock.uptimeMillis() % 1000L) / 1000f * (2f * Math.PI.toFloat())
                val amp = 2f.dp
                var stepX = progressStart
                while (stepX < progressCurrentX) {
                    val relX = stepX - progressStart
                    val waveY = progressY + kotlin.math.sin(relX * freq + phase) * amp
                    wavePath.lineTo(stepX, waveY)
                    stepX += 1.5f.dp
                }
                wavePath.lineTo(progressCurrentX, progressY)
                canvas.drawPath(wavePath, progressFillPaint)
            } else {
                // Straight Line while paused or scrubbing
                canvas.drawLine(progressStart, progressY, progressCurrentX, progressY, progressFillPaint)
            }
        }

        // Draw Drawable Icons for Playback Controls
        val controlTint = textPaint.color
        val activeAccentTint = accentPaint.color

        // Shuffle
        val shuffleIcon = if (isShuffleEnabled) R.drawable.shuffle_on else R.drawable.shuffle
        val shuffleTint = if (isShuffleEnabled) activeAccentTint else controlTint
        drawDrawable(canvas, shuffleIcon, 54f.dp, 148f.dp, 24f, shuffleTint)

        // Previous
        drawDrawable(canvas, R.drawable.skip_previous, localWidth * 0.32f, 148f.dp, 28f, controlTint)

        // Play / Pause with rounded glass badge
        val playCenter = localWidth * 0.5f
        val playBtnRect = RectF(playCenter - 22f.dp, 126f.dp, playCenter + 22f.dp, 170f.dp)
        canvas.drawRoundRect(playBtnRect, 22f.dp, 22f.dp, playButtonBgPaint)
        val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play
        drawDrawable(canvas, playPauseIcon, playCenter, 148f.dp, 28f, controlTint)

        // Next
        drawDrawable(canvas, R.drawable.skip_next, localWidth * 0.68f, 148f.dp, 28f, controlTint)

        // Repeat
        val repeatIcon = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
            Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
            else -> R.drawable.repeat
        }
        val repeatTint = if (repeatMode != Player.REPEAT_MODE_OFF) activeAccentTint else controlTint
        drawDrawable(canvas, repeatIcon, localWidth - 54f.dp, 148f.dp, 24f, repeatTint)
    }

    private fun drawDrawable(
        canvas: Canvas,
        @DrawableRes resId: Int,
        cx: Float,
        cy: Float,
        sizeDp: Float,
        tintColor: Int
    ) {
        val drawable = ContextCompat.getDrawable(context, resId)?.mutate() ?: return
        val sizePx = (sizeDp * density).roundToInt()
        val left = (cx - sizePx / 2f).roundToInt()
        val top = (cy - sizePx / 2f).roundToInt()
        drawable.setBounds(left, top, left + sizePx, top + sizePx)
        drawable.setTint(tintColor)
        drawable.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isTap = true

                if (expanded) {
                    val localWidth = islandBounds.width()
                    val progressStart = 72f.dp
                    val progressEnd = localWidth - 72f.dp
                    val y = event.y - islandBounds.top
                    val x = event.x - islandBounds.left

                    if (y in 84f.dp..124f.dp && x in (progressStart - 18f.dp)..(progressEnd + 18f.dp)) {
                        isDraggingSeekbar = true
                        val fraction = ((x - progressStart) / (progressEnd - progressStart)).coerceIn(0f, 1f)
                        scrubPositionMs = (fraction * durationMs).toLong()
                        invalidate()
                        return true
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingSeekbar && expanded) {
                    val localWidth = islandBounds.width()
                    val progressStart = 72f.dp
                    val progressEnd = localWidth - 72f.dp
                    val x = event.x - islandBounds.left
                    val fraction = ((x - progressStart) / (progressEnd - progressStart)).coerceIn(0f, 1f)
                    scrubPositionMs = (fraction * durationMs).toLong()
                    invalidate()
                    return true
                }

                val dx = kotlin.math.abs(event.x - downX)
                val dy = kotlin.math.abs(event.y - downY)
                if (dx > 20f.dp || dy > 20f.dp) {
                    isTap = false
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDraggingSeekbar && expanded) {
                    isDraggingSeekbar = false
                    positionMs = scrubPositionMs
                    onSeekTo(scrubPositionMs)
                    invalidate()
                    return true
                }

                if (!isTap && expanded) return true

                if (!expanded) {
                    // Tap on collapsed island reliably opens expanded player
                    expandWithDrop()
                    return true
                }
                if (!islandBounds.contains(event.x, event.y)) {
                    collapseWithDrop()
                    return true
                }
                val x = event.x - islandBounds.left
                val y = event.y - islandBounds.top
                if (y < 28f.dp || y > islandBounds.height() - 10f.dp) {
                    collapseWithDrop()
                    return true
                }
                if (y in 120f.dp..176f.dp) {
                    val localWidth = islandBounds.width()
                    val shuffleX = 54f.dp
                    val repeatX = localWidth - 54f.dp
                    when {
                        kotlin.math.abs(x - shuffleX) < 28f.dp -> onShuffle()
                        kotlin.math.abs(x - repeatX) < 28f.dp -> onRepeat()
                        x in (localWidth * 0.18f)..(localWidth * 0.40f) -> onPrevious()
                        x in (localWidth * 0.42f)..(localWidth * 0.58f) -> onPlayPause()
                        x in (localWidth * 0.60f)..(localWidth * 0.82f) -> onNext()
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isTap = false
                isDraggingSeekbar = false
                return true
            }
        }
        return true
    }

    private fun updateIslandBounds() {
        if (expanded || morphAnimating) {
            val collapsedW = customWidth.toFloat().dp
            val collapsedH = customHeight.toFloat().dp
            val topOffset = currentOffsetY.toFloat().dp

            collapsedBounds.set(
                (width - collapsedW) / 2f,
                topOffset,
                (width + collapsedW) / 2f,
                topOffset + collapsedH,
            )

            val expTop = currentOffsetY.coerceIn(0, 120).toFloat().dp
            expandedBounds.set(16f.dp, expTop, width - 16f.dp, expTop + 188f.dp)
            val p = morphProgress
            islandBounds.set(
                lerp(collapsedBounds.left, expandedBounds.left, p),
                lerp(collapsedBounds.top, expandedBounds.top, p),
                lerp(collapsedBounds.right, expandedBounds.right, p),
                lerp(collapsedBounds.bottom, expandedBounds.bottom, p),
            )
        } else {
            islandBounds.set(0f, 0f, width.toFloat(), height.toFloat())
        }
    }

    private fun expandWithDrop() {
        if (morphAnimating) return
        expanded = true
        morphProgress = 0f
        onExpandedChanged()
        startMorphAnimation(
            from = 0f,
            to = 1f,
            duration = 420L,
            interpolator = OvershootInterpolator(0.72f),
            onEnd = {
                morphProgress = 1f
                morphAnimating = false
                invalidate()
            },
        )
    }

    private fun collapseWithDrop() {
        if (morphAnimating) return
        startMorphAnimation(
            from = morphProgress.coerceAtLeast(0.001f),
            to = 0f,
            duration = 280L,
            interpolator = AccelerateDecelerateInterpolator(),
            onEnd = {
                morphProgress = 0f
                morphAnimating = false
                expanded = false
                onExpandedChanged()
                invalidate()
            },
        )
    }

    private fun startMorphAnimation(
        from: Float,
        to: Float,
        duration: Long,
        interpolator: android.animation.TimeInterpolator,
        onEnd: () -> Unit,
    ) {
        morphAnimator?.cancel()
        morphAnimating = true
        morphAnimator =
            ValueAnimator.ofFloat(from, to).apply {
                this.duration = duration
                this.interpolator = interpolator
                addUpdateListener {
                    morphProgress = it.animatedValue as Float
                    invalidate()
                }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            onEnd()
                        }
                    }
                )
                start()
            }
    }

    private fun drawDropEffect(canvas: Canvas) {
        if (!morphAnimating) return
        val p = morphProgress.coerceIn(0f, 1f)
        val intensity = 1f - kotlin.math.abs(p - 0.45f) / 0.45f
        val alpha = (42 * intensity.coerceIn(0f, 1f)).toInt()
        if (alpha <= 0) return
        dropPaint.color = Color.argb(alpha, 255, 255, 255)
        val radius = lerp(16f.dp, islandBounds.width() * 0.42f, p)
        canvas.drawCircle(islandBounds.centerX(), islandBounds.top + islandBounds.height() * 0.52f, radius, dropPaint)
    }

    private fun drawArtwork(canvas: Canvas, rect: RectF, corner: Float, rotate: Boolean = true) {
        val bitmap = artwork
        val checkpoint = canvas.save()
        if (rotate && isPlaying) {
            canvas.rotate(rotationAngle, rect.centerX(), rect.centerY())
        }
        if (bitmap == null) {
            canvas.drawRoundRect(rect, corner, corner, accentPaint)
            if (rect.width() > 24f.dp) {
                canvas.drawText("A", rect.left + rect.width() / 3f, rect.bottom - rect.height() / 3f, textPaint)
            }
        } else {
            val path =
                Path().apply {
                    addRoundRect(rect, corner, corner, Path.Direction.CW)
                }
            canvas.clipPath(path)
            canvas.drawBitmap(bitmap, null, rect, null)
        }
        canvas.restoreToCount(checkpoint)
    }

    private fun drawLiveDot(canvas: Canvas, cx: Float, cy: Float) {
        val dotRadius = (islandBounds.height() * 0.2f).coerceIn(3f.dp, 8f.dp)
        canvas.drawCircle(cx, cy, dotRadius, accentPaint)
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(cx, cy, dotRadius * 0.35f, whitePaint)
    }

    private fun drawSpotifyWaveform(canvas: Canvas, cx: Float, cy: Float, compact: Boolean) {
        val oldStroke = spotifyPaint.strokeWidth
        val availableH = if (compact) (islandBounds.height() - 8f.dp).coerceAtLeast(6f.dp) else 36f.dp
        val scale = if (compact) (availableH / 24f.dp).coerceIn(0.35f, 1.0f) else 1.0f

        spotifyPaint.strokeCap = Paint.Cap.ROUND
        spotifyPaint.strokeWidth = if (compact) (2.0f * scale).coerceAtLeast(1.4f).dp else 3.4f.dp
        val baseHeights =
            if (compact) {
                floatArrayOf(7f, 13f, 19f, 11f, 17f)
            } else {
                floatArrayOf(18f, 28f, 38f, 24f, 34f)
            }
        val gap = if (compact) (3.6f * scale).coerceAtLeast(2.4f).dp else 7f.dp
        val start = cx - gap * 2
        val phase = SystemClock.uptimeMillis() / 145f
        val maxBarH = availableH

        baseHeights.forEachIndexed { index, baseHeight ->
            val pulse =
                if (isPlaying) {
                    0.68f + 0.32f * kotlin.math.sin(phase + index * 1.15f).coerceAtLeast(0f)
                } else {
                    0.62f
                }
            val rawH = baseHeight * scale * pulse
            val height = if (compact) (rawH.dp).coerceAtMost(maxBarH) else (baseHeight * pulse).dp
            val x = start + gap * index
            canvas.drawLine(x, cy - height / 2f, x, cy + height / 2f, spotifyPaint)
        }
        spotifyPaint.strokeWidth = oldStroke
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animationRunnable)
        super.onDetachedFromWindow()
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun String.ellipsize(max: Int): String =
        if (length <= max) this else take(max - 1) + "..."

    private fun RectF.insetBy(value: Float): RectF =
        RectF(left + value, top + value, right - value, bottom - value)

    private fun lerp(start: Float, stop: Float, fraction: Float): Float =
        start + (stop - start) * fraction.coerceIn(0f, 1f)

    private val Float.dp: Float
        get() = this * density
}
