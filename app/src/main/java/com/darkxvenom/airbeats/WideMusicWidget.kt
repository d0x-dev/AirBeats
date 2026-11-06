package com.darkxvenom.airbeats

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.session.MediaButtonReceiver
import coil.imageLoader
import coil.request.ImageRequest
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WideMusicWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
        MusicWidgetRefreshScheduler.start(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        MusicWidgetRefreshScheduler.start(context)
    }

    override fun onDisabled(context: Context) {
        MusicWidgetRefreshScheduler.stopIfNoWidgets(context)
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val widgetIds = manager.getAppWidgetIds(ComponentName(context, WideMusicWidget::class.java))
            widgetIds.forEach { updateWidget(context, manager, it) }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music_wide)
            val playerConnection = PlayerConnection.instance
            val player = playerConnection?.player ?: MusicService.instance?.getPlayerIfInitialized()
            val livePlayer = player?.takeIf { it.currentMediaItem != null && it.mediaItemCount > 0 }
            val savedPlayback = if (livePlayer == null) MusicWidget.readPersistedPlayback(context) else null

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260)
                .takeIf { it > 0 } ?: 260
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)
                .takeIf { it > 0 } ?: 80

            val showTitle = minHeight >= 64
            val showSubtitle = minHeight >= 80 && minWidth >= 180
            views.setViewVisibility(R.id.wide_song_title, if (showTitle) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.wide_subtitle, if (showSubtitle) View.VISIBLE else View.GONE)
            views.setViewVisibility(
                R.id.wide_shuffle,
                if (minWidth >= 220) View.VISIBLE else View.GONE,
            )
            views.setViewVisibility(
                R.id.wide_like,
                if (minWidth >= 220) View.VISIBLE else View.GONE,
            )
            views.setViewVisibility(
                R.id.wide_prev,
                if (minWidth >= 160) View.VISIBLE else View.GONE,
            )
            views.setViewVisibility(
                R.id.wide_next,
                if (minWidth >= 160) View.VISIBLE else View.GONE,
            )
            setPendingIntents(context, views)

            if (livePlayer != null || savedPlayback != null) {
                val metadata = livePlayer?.mediaMetadata
                val title = metadata?.title?.toString()
                    ?: savedPlayback?.song?.title
                    ?: context.getString(R.string.song_title)
                val artist = metadata?.artist?.toString()
                    ?: savedPlayback?.song?.artists?.joinToString { it.name }
                    ?: context.getString(R.string.artist_name)
                val isPlaying = livePlayer?.let { MusicWidget.isPlaybackActiveForWidget(it) }
                    ?: savedPlayback!!.isPlaying
                val shuffleEnabled = livePlayer?.shuffleModeEnabled ?: savedPlayback!!.shuffleEnabled
                val currentPosition = livePlayer?.currentPosition ?: savedPlayback!!.positionMs
                val duration = livePlayer?.duration ?: savedPlayback!!.durationMs
                val isLiked = playerConnection?.isCurrentSongLiked()
                    ?: MusicService.instance?.currentSongLikedState()
                    ?: (savedPlayback?.song?.liked == true)

                views.setTextViewText(R.id.wide_song_title, title)
                val subtitle = if (duration > 0 && duration != Long.MAX_VALUE) {
                    "${formatTime(currentPosition)} / ${formatTime(duration)} · $artist"
                } else {
                    artist
                }
                views.setTextViewText(R.id.wide_subtitle, subtitle)
                views.setImageViewResource(
                    R.id.wide_play_pause,
                    if (isPlaying) R.drawable.pause else R.drawable.play,
                )
                views.setImageViewResource(
                    R.id.wide_shuffle,
                    if (shuffleEnabled) R.drawable.shuffle_on else R.drawable.shuffle,
                )
                views.setImageViewResource(
                    R.id.wide_like,
                    if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
                )

                val artworkUrl = metadata?.artworkUri?.toString() ?: savedPlayback?.song?.thumbnailUrl
                val songId = livePlayer?.currentMediaItem?.mediaId ?: savedPlayback?.song?.id
                if (!artworkUrl.isNullOrBlank()) {
                    val cachedArtwork = WidgetArtworkCache.get(artworkUrl, 640, 360)
                    if (cachedArtwork != null) {
                        views.setImageViewBitmap(R.id.wide_background_art, cachedArtwork)
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            runCatching {
                                val request = ImageRequest.Builder(context)
                                    .data(artworkUrl)
                                    .size(640, 360)
                                    .build()
                                context.imageLoader.execute(request).drawable?.toBitmap()
                            }.getOrNull()?.let { bitmap ->
                                WidgetArtworkCache.put(artworkUrl, 640, 360, bitmap)
                                withContext(Dispatchers.Main) {
                                    if (songId != null && MusicWidget.isCurrentSongForWidget(context, songId)) {
                                        val artworkViews = RemoteViews(context.packageName, R.layout.widget_music_wide).apply {
                                            setImageViewBitmap(R.id.wide_background_art, bitmap)
                                        }
                                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, artworkViews)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                views.setTextViewText(R.id.wide_song_title, context.getString(R.string.app_name))
                views.setTextViewText(R.id.wide_subtitle, context.getString(R.string.tap_to_open))
                views.setImageViewResource(R.id.wide_play_pause, R.drawable.play)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setPendingIntents(context: Context, views: RemoteViews) {
            views.setOnClickPendingIntent(
                R.id.wide_shuffle,
                getWidgetActionPendingIntent(context, MusicWidget.ACTION_SHUFFLE),
            )
            views.setOnClickPendingIntent(
                R.id.wide_prev,
                getWidgetActionPendingIntent(context, MusicWidget.ACTION_PREV),
            )
            views.setOnClickPendingIntent(
                R.id.wide_play_pause,
                getMediaButtonPendingIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE),
            )
            views.setOnClickPendingIntent(
                R.id.wide_next,
                getWidgetActionPendingIntent(context, MusicWidget.ACTION_NEXT),
            )
            views.setOnClickPendingIntent(
                R.id.wide_like,
                getWidgetActionPendingIntent(context, MusicWidget.ACTION_LIKE),
            )

            val openAppPendingIntent = getWidgetActionPendingIntent(context, MusicWidget.ACTION_OPEN_APP)
            views.setOnClickPendingIntent(R.id.wide_widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.wide_background_art, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.wide_background_scrim, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.wide_content, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.wide_controls, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.wide_song_title, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.wide_subtitle, openAppPendingIntent)
        }

        private fun getWidgetActionPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MusicWidget::class.java).setAction(action)
            return PendingIntent.getBroadcast(context, action.hashCode(), intent, pendingIntentFlags())
        }

        private fun getMediaButtonPendingIntent(context: Context, keyCode: Int): PendingIntent {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                component = ComponentName(context, MediaButtonReceiver::class.java)
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }
            return PendingIntent.getBroadcast(context, keyCode, intent, pendingIntentFlags())
        }

        private fun pendingIntentFlags(): Int = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

        @SuppressLint("DefaultLocale")
        private fun formatTime(millis: Long): String {
            if (millis < 0L || millis == Long.MAX_VALUE) return "0:00"
            return String.format(
                "%d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) % 60,
            )
        }
    }
}
