package com.darkxvenom.airbeats

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.playback.PlayerConnection

internal object MusicWidgetRefreshScheduler {
    private val handler = Handler(Looper.getMainLooper())
    private var refreshTask: Runnable? = null

    fun start(context: Context) {
        if (refreshTask != null) return

        val appContext = context.applicationContext
        if (!hasWidgets(appContext)) return

        val task = object : Runnable {
            override fun run() {
                if (!hasWidgets(appContext)) {
                    refreshTask = null
                    return
                }

                val player = PlayerConnection.instance?.player
                    ?: MusicService.instance?.getPlayerIfInitialized()
                MusicWidget.updateAllWidgets(appContext)

                val interval = if (player != null && MusicWidget.isPlaybackActiveForWidget(player)) {
                    1_000L
                } else {
                    5_000L
                }
                handler.postDelayed(this, interval)
            }
        }
        refreshTask = task
        handler.post(task)
    }

    fun refreshNow(context: Context) {
        val task = refreshTask
        if (task == null) {
            start(context)
            return
        }

        handler.removeCallbacks(task)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run()
        } else {
            handler.post(task)
        }
    }

    fun stopIfNoWidgets(context: Context) {
        if (!hasWidgets(context)) {
            refreshTask?.let(handler::removeCallbacks)
            refreshTask = null
        }
    }

    private fun hasWidgets(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        return appWidgetManager.getAppWidgetIds(
            ComponentName(context, MusicWidget::class.java)
        ).isNotEmpty() || appWidgetManager.getAppWidgetIds(
            ComponentName(context, WideMusicWidget::class.java)
        ).isNotEmpty()
    }
}
