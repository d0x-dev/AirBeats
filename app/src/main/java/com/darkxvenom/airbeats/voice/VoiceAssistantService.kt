package com.darkxvenom.airbeats.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.edit
import com.darkxvenom.airbeats.MainActivity
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.EnableVoiceAssistantKey
import com.darkxvenom.airbeats.constants.VoiceAssistantDirectCommandsKey
import com.darkxvenom.airbeats.playback.MusicService
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

class VoiceAssistantService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var musicService: MusicService? = null
    private var isMusicServiceBound = false

    private lateinit var voiceAssistantManager: VoiceAssistantManager
    private lateinit var actionExecutor: VoiceAssistantActionExecutor

    companion object {
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "voice_assistant_channel"
        const val ACTION_STOP_VOICE_ASSISTANT = "com.darkxvenom.airbeats.voice.STOP"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        @Volatile
        var instance: VoiceAssistantService? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoiceAssistantService::class.java)
            context.stopService(intent)
        }
    }

    private val musicServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder is MusicService.MusicBinder) {
                musicService = binder.service
                Timber.d("VoiceAssistantService connected to MusicService")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            Timber.d("VoiceAssistantService disconnected from MusicService")
        }
    }

    private var overlayManager: VoiceAssistantOverlayManager? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isServiceRunning.value = true

        acquireWakeLock()
        createNotificationChannel()
        startAsForeground()
        bindMusicService()

        overlayManager = VoiceAssistantOverlayManager(this)

        actionExecutor = VoiceAssistantActionExecutor(
            context = this,
            scope = serviceScope,
            getMusicService = { musicService },
            overlayManager = overlayManager
        )

        voiceAssistantManager = VoiceAssistantManager(
            context = this,
            onWakeWordHeard = { text ->
                overlayManager?.showListening()
                overlayManager?.updateSpokenText(text)
            },
            onCommandRecognized = { command, text ->
                Timber.i("VoiceAssistantService received command: %s (from text: '%s')", command, text)
                overlayManager?.updateSpokenText(text)
                updateNotificationText("Executing: \"$text\"")
                actionExecutor.execute(command)
            }
        )

        serviceScope.launch {
            val directCommands = dataStore.get(VoiceAssistantDirectCommandsKey, false)
            val requireWakeWord = !directCommands
            voiceAssistantManager.start(requireWakeWord = requireWakeWord)

            // Observe direct command setting changes
            dataStore.data.collectLatest {
                val direct = it[VoiceAssistantDirectCommandsKey] ?: false
                voiceAssistantManager.updateSettings(requireWakeWord = !direct)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_VOICE_ASSISTANT) {
            Timber.i("Stopping VoiceAssistantService via notification action")
            serviceScope.launch {
                dataStore.edit { preferences ->
                    preferences[EnableVoiceAssistantKey] = false
                }
            }
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val notification = buildForegroundNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting VoiceAssistantService as foreground")
        }
    }

    private fun buildForegroundNotification(statusText: String? = null): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceAssistantService::class.java).apply {
                action = ACTION_STOP_VOICE_ASSISTANT
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = statusText ?: getString(R.string.voice_notification_text)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.airbeats_monochrome)
            .setContentTitle(getString(R.string.voice_notification_title))
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.airbeats_monochrome,
                getString(R.string.voice_notification_stop),
                stopIntent
            )
            .build()
    }

    fun updateNotificationText(statusText: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildForegroundNotification(statusText))

            // Revert back to default status after 4 seconds
            serviceScope.launch {
                delay(4000)
                notificationManager.notify(NOTIFICATION_ID, buildForegroundNotification(null))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating notification text")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.voice_assistant_desc)
                setShowBadge(false)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AirBeats:VoiceAssistantWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max safety limit
            }
        } catch (e: Exception) {
            Timber.e(e, "Error acquiring wake lock")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Timber.e(e, "Error releasing wake lock")
        }
    }

    private fun bindMusicService() {
        try {
            val intent = Intent(this, MusicService::class.java)
            startService(intent)
            isMusicServiceBound = bindService(intent, musicServiceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Timber.e(e, "Error binding to MusicService")
        }
    }

    private fun unbindMusicService() {
        if (isMusicServiceBound) {
            try {
                unbindService(musicServiceConnection)
                isMusicServiceBound = false
            } catch (e: Exception) {
                Timber.e(e, "Error unbinding from MusicService")
            }
        }
    }

    override fun onDestroy() {
        _isServiceRunning.value = false
        instance = null

        overlayManager?.hide()
        overlayManager = null

        voiceAssistantManager.destroy()
        actionExecutor.release()
        releaseWakeLock()
        unbindMusicService()
        serviceScope.cancel()

        super.onDestroy()
        Timber.d("VoiceAssistantService destroyed")
    }
}
