package com.darkxvenom.airbeats

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import androidx.media3.session.MediaButtonReceiver
import coil.ImageLoader
import coil.request.ImageRequest
import com.darkxvenom.airbeats.models.MediaMetadata
import com.darkxvenom.airbeats.models.PersistPlayerState
import com.darkxvenom.airbeats.models.PersistQueue
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ObjectInputStream
import java.util.concurrent.TimeUnit

class MusicWidget : AppWidgetProvider() {

    private val handler = Handler(Looper.getMainLooper())

    // ✅ FIXED: nullable instead of lateinit
    private var runnable: Runnable? = null
    private var isUpdating = false

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
        startProgressUpdater(context)
    }

    override fun onEnabled(context: Context) {
        startProgressUpdater(context)
    }

    override fun onDisabled(context: Context) {
        stopProgressUpdater()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                PlayerConnection.instance?.togglePlayPause()
                    ?: sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                updateAllWidgets(context)
            }

            ACTION_PREV -> {
                PlayerConnection.instance?.seekToPrevious()
                    ?: sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                updateAllWidgets(context)
            }

            ACTION_NEXT -> {
                PlayerConnection.instance?.seekToNext()
                    ?: sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                updateAllWidgets(context)
            }

            ACTION_SHUFFLE -> {
                PlayerConnection.instance?.toggleShuffle()
                updateAllWidgets(context)
            }

            ACTION_LIKE -> {
                PlayerConnection.instance?.toggleLike()
                updateAllWidgets(context)
            }

            ACTION_REPLAY -> {
                PlayerConnection.instance?.toggleReplayMode()
                updateAllWidgets(context)
            }

            ACTION_OPEN_APP -> {
                openApp(context)
            }

            ACTION_STATE_CHANGED, ACTION_UPDATE_PROGRESS -> {
                updateAllWidgets(context)
            }
        }
    }

    private fun openApp(context: Context) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                context.startActivity(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startProgressUpdater(context: Context) {
        if (isUpdating) return

        isUpdating = true

        runnable = Runnable {
            val playerConnection = PlayerConnection.instance
            val player = playerConnection?.player

            if (player != null && (player.isPlaying || player.playbackState == Player.STATE_READY)) {
                updateAllWidgets(context)
                runnable?.let { handler.postDelayed(it, 1000) }
            } else {
                updateAllWidgets(context)
                runnable?.let { handler.postDelayed(it, 5000) }
            }
        }

        runnable?.let { handler.post(it) }
    }

    private fun stopProgressUpdater() {
        isUpdating = false
        runnable?.let {
            handler.removeCallbacks(it)
        }
        runnable = null
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.darkxvenom.airbeats.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.darkxvenom.airbeats.ACTION_PREV"
        const val ACTION_NEXT = "com.darkxvenom.airbeats.ACTION_NEXT"
        const val ACTION_SHUFFLE = "com.darkxvenom.airbeats.ACTION_SHUFFLE"
        const val ACTION_LIKE = "com.darkxvenom.airbeats.ACTION_LIKE"
        const val ACTION_REPLAY = "com.darkxvenom.airbeats.ACTION_REPLAY"
        const val ACTION_OPEN_APP = "com.darkxvenom.airbeats.ACTION_OPEN_APP"
        const val ACTION_STATE_CHANGED = "com.darkxvenom.airbeats.ACTION_STATE_CHANGED"
        const val ACTION_UPDATE_PROGRESS = "com.darkxvenom.airbeats.ACTION_UPDATE_PROGRESS"

        private data class PersistedWidgetPlayback(
            val song: MediaMetadata,
            val positionMs: Long,
            val durationMs: Long,
            val isPlaying: Boolean,
            val shuffleEnabled: Boolean,
            val repeatMode: Int,
        )

        @Volatile
        private var cachedPersistedPlayback: PersistedWidgetPlayback? = null

        @Volatile
        private var cachedQueueModifiedAt = Long.MIN_VALUE

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MusicWidget::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                widgetIds.forEach { updateWidget(context, appWidgetManager, it) }
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music)
            val playerConnection = PlayerConnection.instance
            val player = playerConnection?.player ?: MusicService.instance?.getPlayerIfInitialized()
            val livePlayer = player?.takeIf { it.currentMediaItem != null && it.mediaItemCount > 0 }
            val persistedPlayback = if (livePlayer == null) readPersistedPlayback(context) else null

            setPendingIntents(context, views)

            if (livePlayer != null || persistedPlayback != null) {
                val metadata = livePlayer?.mediaMetadata
                val songTitle = metadata?.title?.toString()
                    ?: persistedPlayback?.song?.title
                    ?: context.getString(R.string.song_title)
                val artist = metadata?.artist?.toString()
                    ?: persistedPlayback?.song?.artists?.joinToString { it.name }
                    ?: context.getString(R.string.artist_name)
                val isPlaying = livePlayer?.isPlaying ?: persistedPlayback!!.isPlaying
                val shuffleEnabled = livePlayer?.shuffleModeEnabled ?: persistedPlayback!!.shuffleEnabled
                val repeatMode = livePlayer?.repeatMode ?: persistedPlayback!!.repeatMode
                val currentPos = livePlayer?.currentPosition ?: persistedPlayback!!.positionMs
                val duration = livePlayer?.duration ?: persistedPlayback!!.durationMs

                views.setTextViewText(R.id.widget_song_title, songTitle)
                views.setTextViewText(R.id.widget_artist, artist)
                val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play
                views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)

                val shuffleIcon = if (shuffleEnabled) R.drawable.shuffle_on else R.drawable.shuffle
                views.setImageViewResource(R.id.widget_shuffle, shuffleIcon)

                val likeIcon =
                    if (playerConnection?.isCurrentSongLiked() ?: (persistedPlayback?.song?.liked == true))
                        R.drawable.favorite else R.drawable.favorite_border
                views.setImageViewResource(R.id.widget_like, likeIcon)

                val currentTimeText = formatTime(currentPos)
                val durationText = formatTime(duration)

                views.setTextViewText(R.id.widget_current_time, currentTimeText)
                views.setTextViewText(R.id.widget_duration, durationText)

                val progress =
                    if (duration > 0 && duration != Long.MAX_VALUE) {
                        (currentPos * 100 / duration).toInt()
                    } else 0

                if (duration > 0 && duration != Long.MAX_VALUE) {
                    views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_current_time, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.widget_duration, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_progress_bar, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_current_time, android.view.View.GONE)
                    views.setViewVisibility(R.id.widget_duration, android.view.View.GONE)
                }

                val playbackStateText = when {
                    repeatMode == Player.REPEAT_MODE_ONE -> context.getString(R.string.repeat_mode_one)
                    repeatMode == Player.REPEAT_MODE_ALL -> context.getString(R.string.repeat_mode_all)
                    else -> ""
                }

                if (playbackStateText.isNotEmpty()) {
                    views.setTextViewText(R.id.widget_playback_state, playbackStateText)
                    views.setViewVisibility(R.id.widget_playback_state, android.view.View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_playback_state, android.view.View.GONE)
                }

                val thumbnailUrl = metadata?.artworkUri?.toString() ?: persistedPlayback?.song?.thumbnailUrl
                if (!thumbnailUrl.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val request = ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .size(160, 160)
                                .build()
                            val drawable = ImageLoader(context).execute(request).drawable
                            drawable?.let {
                                val bitmap = it.toBitmap()
                                views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                            }
                        } catch (e: Exception) {
                            views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views)
                        }
                    }
                } else {
                    views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
                }
            } else {
                views.setTextViewText(R.id.widget_song_title, context.getString(R.string.app_name))
                views.setTextViewText(R.id.widget_artist, context.getString(R.string.tap_to_open))
                views.setImageViewResource(R.id.widget_album_art, R.drawable.music_note)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setPendingIntents(context: Context, views: RemoteViews) {
            val playPausePendingIntent = getMediaButtonPendingIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            val prevPendingIntent = getMediaButtonPendingIntent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            val nextPendingIntent = getMediaButtonPendingIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT)
            val shufflePendingIntent = getBroadcastPendingIntent(context, ACTION_SHUFFLE)
            val likePendingIntent = getBroadcastPendingIntent(context, ACTION_LIKE)
            val openAppPendingIntent = getBroadcastPendingIntent(context, ACTION_OPEN_APP)

            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_prev, prevPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_shuffle, shufflePendingIntent)
            views.setOnClickPendingIntent(R.id.widget_like, likePendingIntent)

            views.setOnClickPendingIntent(R.id.widget_album_art, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_song_title, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_artist, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_progress_bar, openAppPendingIntent)
        }

        private fun getMediaButtonPendingIntent(context: Context, keyCode: Int): PendingIntent {
            val intent = createMediaButtonIntent(context, keyCode)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            return PendingIntent.getBroadcast(context, keyCode, intent, flags)
        }

        private fun sendMediaButton(context: Context, keyCode: Int) {
            context.sendBroadcast(createMediaButtonIntent(context, keyCode))
        }

        private fun createMediaButtonIntent(context: Context, keyCode: Int) =
            Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = ComponentName(context, MediaButtonReceiver::class.java)
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }

        private fun readPersistedPlayback(context: Context): PersistedWidgetPlayback? {
            val queueFile = context.filesDir.resolve(MusicService.PERSISTENT_QUEUE_FILE)
            if (!queueFile.exists()) {
                cachedPersistedPlayback = null
                cachedQueueModifiedAt = 0L
                return null
            }

            val modifiedAt = queueFile.lastModified()
            if (modifiedAt == cachedQueueModifiedAt) return cachedPersistedPlayback

            val playback = runCatching {
                val queue = queueFile.inputStream().use { input ->
                    ObjectInputStream(input).use { it.readObject() as PersistQueue }
                }
                if (queue.items.isEmpty()) return@runCatching null
                val song = queue.items[queue.mediaItemIndex.coerceIn(0, queue.items.lastIndex)]
                val playerState = runCatching {
                    context.filesDir.resolve(MusicService.PERSISTENT_PLAYER_STATE_FILE).inputStream().use { input ->
                        ObjectInputStream(input).use { it.readObject() as PersistPlayerState }
                    }
                }.getOrNull()
                val durationMs = song.duration.takeIf { it > 0 }?.times(1000L) ?: 0L
                PersistedWidgetPlayback(
                    song = song,
                    positionMs = queue.position.coerceAtLeast(0L),
                    durationMs = durationMs,
                    isPlaying = playerState?.playWhenReady == true,
                    shuffleEnabled = playerState?.shuffleModeEnabled == true,
                    repeatMode = playerState?.repeatMode ?: Player.REPEAT_MODE_OFF,
                )
            }.getOrNull()

            if (playback != null) {
                cachedPersistedPlayback = playback
                cachedQueueModifiedAt = modifiedAt
            }
            return playback
        }

        private fun getBroadcastPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicWidget::class.java).apply {
                this.action = action
            }

            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

            return PendingIntent.getBroadcast(context, action.hashCode(), intent, flags)
        }

        @SuppressLint("DefaultLocale")
        private fun formatTime(millis: Long): String {
            return if (millis < 0 || millis == Long.MAX_VALUE) "0:00" else String.format(
                "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            )
        }
    }
}

