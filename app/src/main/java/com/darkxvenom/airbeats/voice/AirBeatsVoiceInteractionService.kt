package com.darkxvenom.airbeats.voice

import android.os.Bundle
import android.service.voice.AlwaysOnHotwordDetector
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import timber.log.Timber
import java.util.Locale

/**
 * Android VoiceInteractionService implementation for AirBeats.
 * Enables AirBeats to be chosen as the device's Default Digital Assistant.
 * Coordinates system hotword detection and assist sessions.
 */
class AirBeatsVoiceInteractionService : VoiceInteractionService() {

    private var hotwordDetector: AlwaysOnHotwordDetector? = null

    override fun onReady() {
        super.onReady()
        Timber.i("AirBeatsVoiceInteractionService is ready as selected Voice Assistant")

        try {
            // Attempt to create AlwaysOnHotwordDetector for "AirBeats" / "Hey AirBeats"
            hotwordDetector = createAlwaysOnHotwordDetector(
                "AirBeats",
                Locale.getDefault(),
                object : AlwaysOnHotwordDetector.Callback() {
                    override fun onAvailabilityChanged(status: Int) {
                        Timber.d("AlwaysOnHotwordDetector availability changed: $status")
                        if (status == AlwaysOnHotwordDetector.STATE_KEYPHRASE_ENROLLED) {
                            try {
                                hotwordDetector?.startRecognition(
                                    AlwaysOnHotwordDetector.RECOGNITION_FLAG_ALLOW_MULTIPLE_TRIGGERS or
                                            AlwaysOnHotwordDetector.RECOGNITION_FLAG_CAPTURE_TRIGGER_AUDIO
                                )
                            } catch (e: Exception) {
                                Timber.w(e, "Could not start AlwaysOnHotwordDetector recognition")
                            }
                        }
                    }

                    override fun onDetected(eventPayload: AlwaysOnHotwordDetector.EventPayload) {
                        Timber.i("Hotword detected by DSP hardware! Launching AirBeats session...")
                        showSession(
                            Bundle().apply {
                                putBoolean(EXTRA_HOTWORD_TRIGGERED, true)
                            },
                            VoiceInteractionSession.SHOW_SOURCE_APPLICATION
                        )
                    }

                    override fun onError() {
                        Timber.e("AlwaysOnHotwordDetector error encountered")
                    }

                    override fun onRecognitionPaused() {
                        Timber.d("AlwaysOnHotwordDetector recognition paused")
                    }

                    override fun onRecognitionResumed() {
                        Timber.d("AlwaysOnHotwordDetector recognition resumed")
                    }
                }
            )
        } catch (e: Exception) {
            Timber.w(e, "AlwaysOnHotwordDetector not supported on this device/hardware model")
        }
    }

    override fun onShutdown() {
        try {
            hotwordDetector?.stopRecognition()
        } catch (_: Exception) {}
        hotwordDetector = null
        super.onShutdown()
    }

    companion object {
        const val EXTRA_HOTWORD_TRIGGERED = "extra_hotword_triggered"
    }
}
