package com.darkxvenom.airbeats.voice

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Service that creates and manages AirBeats VoiceInteractionSession instances.
 */
class AirBeatsVoiceInteractionSessionService : VoiceInteractionSessionService() {

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return AirBeatsVoiceInteractionSession(this)
    }
}
