package com.darkxvenom.airbeats.playback

import android.animation.ValueAnimator
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.darkxvenom.airbeats.constants.DynamicIslandAccentColorKey
import com.darkxvenom.airbeats.constants.DynamicIslandBgColorKey
import com.darkxvenom.airbeats.constants.DynamicIslandHeightKey
import com.darkxvenom.airbeats.constants.DynamicIslandLandscapeHeightKey
import com.darkxvenom.airbeats.constants.DynamicIslandLandscapeOffsetXKey
import com.darkxvenom.airbeats.constants.DynamicIslandLandscapeOffsetYKey
import com.darkxvenom.airbeats.constants.DynamicIslandLandscapeWidthKey
import com.darkxvenom.airbeats.constants.DynamicIslandOffsetXKey
import com.darkxvenom.airbeats.constants.DynamicIslandOffsetYKey
import com.darkxvenom.airbeats.constants.DynamicIslandTextColorKey
import com.darkxvenom.airbeats.constants.DynamicIslandWidthKey
import com.darkxvenom.airbeats.extensions.currentMetadata
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
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
    private var musicService: MusicService? = null
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

    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                musicService = (binder as? MusicService.MusicBinder)?.service
                musicService?.player?.addListener(this@DynamicIslandService)
                updateIsland()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                musicService?.player?.removeListener(this@DynamicIslandService)
                musicService = null
                hideIsland()
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
                    musicService?.player?.let { player ->
                        player.shuffleModeEnabled = !player.shuffleModeEnabled
                    }
                },
                onPrevious = { musicService?.player?.seekToPrevious() },
                onPlayPause = {
                    musicService?.player?.let { player ->
                        player.playWhenReady = !player.playWhenReady
                    }
                },
                onNext = { musicService?.player?.seekToNext() },
                onRepeat = {
                    musicService?.player?.let { player ->
                        player.repeatMode = if (player.repeatMode == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ALL else if (player.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    }
                },
            )
        bindService(Intent(this, MusicService::class.java), connection, Context.BIND_AUTO_CREATE)
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
        updateIsland()
        return START_STICKY
    }

    override fun onEvents(player: Player, events: Player.Events) {
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
                    duration = 100,
                    thumbnailUrl = null,
                ),
                isPlaying = true,
                positionMs = 50000,
                durationMs = 100000,
                isShuffleEnabled = false,
                repeatMode = Player.REPEAT_MODE_OFF,
            )
            showIsland()
            loadArtwork(null)
            return
        }

        val player = musicService?.player ?: return
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
            isPlaying = player.playWhenReady,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            isShuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
        showIsland()
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
            currentIslandHeight.coerceAtLeast(24).dp
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
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // When expanded, ALWAYS reset X to 0 so the expanded full form stays 100% inside screen bounds!
            x = if (isExp) 0 else currentOffsetX.dp
            y = if (isExp) 0 else currentOffsetY.dp
        }
    }

    override fun onDestroy() {
        AppForegroundTracker.removeListener(foregroundListener)
        hideIsland()
        musicService?.player?.removeListener(this)
        runCatching { unbindService(connection) }
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
) : View(context) {
    var expanded = false
        private set

    var customWidth: Int = 160
        private set

    var customHeight: Int = 36
        private set

    private var currentOffsetY = 8
    private var rotationAngle = 0f

    private var downX = 0f
    private var downY = 0f
    private var isTap = false

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
    private val controlPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 3f * density
        }
    private val progressPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 255, 255, 255)
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4f * density
        }
    private val progressFillPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4f * density
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
        bgPaint.color = bgColor
        accentPaint.color = accentColor
        spotifyPaint.color = accentColor
        progressFillPaint.color = accentColor
        textPaint.color = textColor
        subTextPaint.color = Color.argb(185, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
        controlPaint.color = textColor
        invalidate()
    }

    fun update(metadata: MediaMetadata, isPlaying: Boolean, positionMs: Long, durationMs: Long, isShuffleEnabled: Boolean, repeatMode: Int) {
        this.metadata = metadata
        this.isPlaying = isPlaying
        this.positionMs = positionMs
        this.durationMs = durationMs
        this.isShuffleEnabled = isShuffleEnabled
        this.repeatMode = repeatMode
        removeCallbacks(animationRunnable)
        if (isPlaying) {
            post(animationRunnable)
        }
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
            // Mini Dot Mode (Clean rotating circular artwork without top-right indicator dot)
            val padding = 3f.dp
            val art = RectF(padding, padding, localWidth - padding, localHeight - padding)
            drawArtwork(canvas, art, corner = (localHeight - padding * 2) / 2f, rotate = true)
        } else if (localWidth < 110f.dp) {
            // Compact Mini-Pill Mode (Rotating circular artwork on left, waveform on right)
            val artSize = localHeight - 8f.dp
            val art = RectF(4f.dp, 4f.dp, 4f.dp + artSize, 4f.dp + artSize)
            drawArtwork(canvas, art, corner = artSize / 2f, rotate = true)
            drawSpotifyWaveform(canvas, localWidth - 22f.dp, localHeight / 2f, compact = true)
        } else {
            // Standard Full Pill Mode (Rotating circular artwork on left, live dot in center, waveform on right)
            val artSize = localHeight - 8f.dp
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

        val progressStart = 72f.dp
        val progressEnd = localWidth - 72f.dp
        val progressY = 102f.dp
        canvas.drawText(formatTime(positionMs), 24f.dp, 106f.dp, subTextPaint)
        canvas.drawText(formatTime(durationMs), localWidth - 58f.dp, 106f.dp, subTextPaint)
        canvas.drawLine(progressStart, progressY, progressEnd, progressY, progressPaint)
        val progress =
            if (durationMs > 0) {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        canvas.drawLine(progressStart, progressY, progressStart + (progressEnd - progressStart) * progress, progressY, progressFillPaint)

        drawShuffle(canvas, 54f.dp, 148f.dp, isShuffleEnabled)
        drawPrevious(canvas, localWidth * 0.33f, 148f.dp)
        if (isPlaying) drawPause(canvas, localWidth * 0.5f, 148f.dp) else drawPlay(canvas, localWidth * 0.5f, 148f.dp)
        drawNext(canvas, localWidth * 0.67f, 148f.dp)
        drawRepeat(canvas, localWidth - 54f.dp, 148f.dp, repeatMode)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isTap = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = kotlin.math.abs(event.x - downX)
                val dy = kotlin.math.abs(event.y - downY)
                if (dx > 24f.dp || dy > 24f.dp) {
                    isTap = false
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isTap && expanded) return true

                if (!expanded) {
                    // Tap on collapsed island (any size/height) reliably opens expanded player
                    expandWithDrop()
                    return true
                }
                if (!islandBounds.contains(event.x, event.y)) {
                    collapseWithDrop()
                    return true
                }
                val x = event.x - islandBounds.left
                val y = event.y - islandBounds.top
                if (y < 34f.dp || y > islandBounds.height() - 12f.dp) {
                    collapseWithDrop()
                    return true
                }
                if (y in 126f.dp..172f.dp) {
                    val localWidth = islandBounds.width()
                    val shuffleX = 54f.dp
                    val repeatX = localWidth - 54f.dp
                    when {
                        kotlin.math.abs(x - shuffleX) < 28f.dp -> onShuffle()
                        kotlin.math.abs(x - repeatX) < 28f.dp -> onRepeat()
                        x in (localWidth * 0.22f)..(localWidth * 0.42f) -> onPrevious()
                        x in (localWidth * 0.42f)..(localWidth * 0.58f) -> onPlayPause()
                        x in (localWidth * 0.58f)..(localWidth * 0.78f) -> onNext()
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isTap = false
                return true
            }
        }
        return true
    }

    private fun updateIslandBounds() {
        if (expanded || morphAnimating) {
            val collapsedW = customWidth.toFloat().dp
            val collapsedH = customHeight.toFloat().dp
            val topOffset = currentOffsetY.coerceAtLeast(8).toFloat().dp

            collapsedBounds.set(
                (width - collapsedW) / 2f,
                topOffset,
                (width + collapsedW) / 2f,
                topOffset + collapsedH,
            )

            // Clamped expanded bounds so it stays centered and never clips out of screen
            val expTop = currentOffsetY.coerceIn(8, 60).toFloat().dp
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

    private fun drawShuffle(canvas: Canvas, cx: Float, cy: Float, enabled: Boolean) {
        val paint = Paint(controlPaint).apply {
            if (enabled) color = accentPaint.color
            strokeWidth = 2.5f.dp
        }
        // Arrow 1 (top-left to bottom-right)
        canvas.drawLine(cx - 8f.dp, cy - 5f.dp, cx + 8f.dp, cy + 5f.dp, paint)
        canvas.drawLine(cx + 4f.dp, cy + 5f.dp, cx + 8f.dp, cy + 5f.dp, paint)
        canvas.drawLine(cx + 8f.dp, cy + 1f.dp, cx + 8f.dp, cy + 5f.dp, paint)

        // Arrow 2 (bottom-left to top-right)
        canvas.drawLine(cx - 8f.dp, cy + 5f.dp, cx - 2f.dp, cy + 2f.dp, paint)
        canvas.drawLine(cx + 2f.dp, cy - 2f.dp, cx + 8f.dp, cy - 5f.dp, paint)
        canvas.drawLine(cx + 4f.dp, cy - 5f.dp, cx + 8f.dp, cy - 5f.dp, paint)
        canvas.drawLine(cx + 8f.dp, cy - 1f.dp, cx + 8f.dp, cy - 5f.dp, paint)
    }

    private fun drawPrevious(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawLine(cx - 9f.dp, cy - 10f.dp, cx - 9f.dp, cy + 10f.dp, controlPaint)
        val path = android.graphics.Path().apply {
            moveTo(cx + 8f.dp, cy - 12f.dp)
            lineTo(cx - 6f.dp, cy)
            lineTo(cx + 8f.dp, cy + 12f.dp)
            close()
        }
        canvas.drawPath(path, controlPaint)
    }

    private fun drawNext(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawLine(cx + 9f.dp, cy - 10f.dp, cx + 9f.dp, cy + 10f.dp, controlPaint)
        val path = android.graphics.Path().apply {
            moveTo(cx - 8f.dp, cy - 12f.dp)
            lineTo(cx + 6f.dp, cy)
            lineTo(cx - 8f.dp, cy + 12f.dp)
            close()
        }
        canvas.drawPath(path, controlPaint)
    }

    private fun drawPlay(canvas: Canvas, cx: Float, cy: Float) {
        val path = android.graphics.Path().apply {
            moveTo(cx - 6f.dp, cy - 12f.dp)
            lineTo(cx + 10f.dp, cy)
            lineTo(cx - 6f.dp, cy + 12f.dp)
            close()
        }
        canvas.drawPath(path, controlPaint)
    }

    private fun drawPause(canvas: Canvas, cx: Float, cy: Float) {
        canvas.drawLine(cx - 5f.dp, cy - 11f.dp, cx - 5f.dp, cy + 11f.dp, controlPaint)
        canvas.drawLine(cx + 5f.dp, cy - 11f.dp, cx + 5f.dp, cy + 11f.dp, controlPaint)
    }

    private fun drawRepeat(canvas: Canvas, cx: Float, cy: Float, repeatMode: Int) {
        val paint = Paint(controlPaint).apply {
            if (repeatMode != Player.REPEAT_MODE_OFF) color = accentPaint.color
            strokeWidth = 2.5f.dp
        }
        // Top right-pointing arrow
        canvas.drawLine(cx - 6f.dp, cy - 4f.dp, cx + 6f.dp, cy - 4f.dp, paint)
        canvas.drawLine(cx + 6f.dp, cy - 4f.dp, cx + 6f.dp, cy - 1f.dp, paint)
        canvas.drawLine(cx + 3f.dp, cy - 7f.dp, cx + 6f.dp, cy - 4f.dp, paint)
        canvas.drawLine(cx + 3f.dp, cy - 1f.dp, cx + 6f.dp, cy - 4f.dp, paint)

        // Bottom left-pointing arrow
        canvas.drawLine(cx + 6f.dp, cy + 4f.dp, cx - 6f.dp, cy + 4f.dp, paint)
        canvas.drawLine(cx - 6f.dp, cy + 4f.dp, cx - 6f.dp, cy + 1f.dp, paint)
        canvas.drawLine(cx - 3f.dp, cy + 7f.dp, cx - 6f.dp, cy + 4f.dp, paint)
        canvas.drawLine(cx - 3f.dp, cy + 1f.dp, cx - 6f.dp, cy + 4f.dp, paint)

        if (repeatMode == Player.REPEAT_MODE_ONE) {
            paint.style = Paint.Style.FILL
            paint.strokeWidth = 1f.dp
            canvas.drawText("1", cx - 3.5f.dp, cy + 3.5f.dp, paint.apply { textSize = 10f.dp })
        }
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
