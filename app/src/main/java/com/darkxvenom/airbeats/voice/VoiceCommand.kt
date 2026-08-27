package com.darkxvenom.airbeats.voice

sealed interface VoiceCommand {
    data class PlaySong(val query: String) : VoiceCommand
    data object PlayGenericMusic : VoiceCommand
    data object PlayCachedSongs : VoiceCommand
    data object PlayLikedSongs : VoiceCommand
    data object Pause : VoiceCommand
    data object Resume : VoiceCommand
    data object NextTrack : VoiceCommand
    data object PreviousTrack : VoiceCommand
    data object ToggleLike : VoiceCommand
    data object StartRadio : VoiceCommand
    data object VolumeUp : VoiceCommand
    data object VolumeDown : VoiceCommand
    data class SetVolume(val levelPercent: Int) : VoiceCommand
    data object Mute : VoiceCommand
    data object Unmute : VoiceCommand
    data class Unknown(val rawText: String) : VoiceCommand
}
