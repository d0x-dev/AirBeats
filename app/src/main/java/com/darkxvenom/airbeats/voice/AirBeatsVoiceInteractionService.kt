package com.darkxvenom.airbeats.voice

import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import timber.log.Timber

/**
 * Android VoiceInteractionService implementation for AirBeats.
 * Enables AirBeats to be chosen as the device's Default Digital Assistant.
 * Handles system assistant activation gestures (home button long press, power button, assistant shortcut).
 */
class AirBeatsVoiceInteractionService : VoiceInteractionService() {

    override fun onReady() {
        super.onReady()
        Timber.i("AirBeatsVoiceInteractionService is ready as selected Voice Assistant")
    }

    override fun onShutdown() {
        super.onShutdown()
        Timber.i("AirBeatsVoiceInteractionService shutdown")
    }

    companion object {
        const val EXTRA_HOTWORD_TRIGGERED = "extra_hotword_triggered"
    }
}
