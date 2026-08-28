package com.darkxvenom.airbeats.voice

import java.util.Locale

object VoiceCommandParser {

    private val WAKE_WORD_PATTERNS = listOf(
        "(?:hi|hey|ok|hello|hai|ay|yo)?\\s*air\\s*beats?",
        "(?:hi|hey|ok|hello|hai|ay|yo)?\\s*aerobeats?",
        "(?:hi|hey|ok|hello|hai|ay|yo)?\\s*air\\s*beat",
        "(?:hi|hey|ok|hello|hai|ay|yo)?\\s*air\\s*bits?",
        "(?:hi|hey|ok|hello|hai|ay|yo)?\\s*air\\s*base"
    )

    private val WAKE_WORD_REGEX = Regex(
        "^\\s*(?:${WAKE_WORD_PATTERNS.joinToString("|")})[,\\s]*",
        RegexOption.IGNORE_CASE
    )

    private val VOLUME_PERCENT_REGEX = Regex(
        "(?:set\\s+|change\\s+|put\\s+|make\\s+|turn\\s+)?volume(?:\\s+to|\\s+at)?\\s+(\\d{1,3})(?:\\s*%)?|(\\d{1,3})(?:\\s*%|\\s+percent)(?:\\s+volume)?",
        RegexOption.IGNORE_CASE
    )

    private val PLAY_CACHED_REGEX = Regex(
        "^(?:play\\s+|listen\\s+to\\s+|stream\\s+)?(?:my\\s+)?(?:cached|cache|downloaded|download|downloads|offline|saved|local|library|stored)\\s*(?:songs?|tracks?|music)?$" +
                "|^(?:play\\s+|listen\\s+to\\s+)?(?:songs?|music|tracks?)\\s+from\\s+(?:my\\s+)?(?:library|cache|downloads?|storage)$",
        RegexOption.IGNORE_CASE
    )

    private val PLAY_LIKED_REGEX = Regex(
        "^(?:play\\s+|listen\\s+to\\s+|stream\\s+)?(?:my\\s+)?(?:liked|favorites?|favourites?|loved)\\s*(?:songs?|tracks?|music)?$" +
                "|^(?:play\\s+|listen\\s+to\\s+)?(?:songs?|music|tracks?)\\s+from\\s+(?:my\\s+)?(?:favorites?|favourites?|liked)$",
        RegexOption.IGNORE_CASE
    )

    /**
     * Checks whether the given spoken text contains an AirBeats wake word.
     */
    fun containsWakeWord(text: String): Boolean {
        val trimmed = text.trim()
        return WAKE_WORD_REGEX.containsMatchIn(trimmed) ||
                trimmed.lowercase(Locale.ROOT).contains("airbeat") ||
                trimmed.lowercase(Locale.ROOT).contains("air beats") ||
                trimmed.lowercase(Locale.ROOT).contains("aerobeat")
    }

    /**
     * Parses spoken text into a [VoiceCommand].
     */
    fun parse(rawText: String, requireWakeWord: Boolean = true): VoiceCommand {
        val trimmed = rawText.trim().lowercase(Locale.ROOT)
            .replace(Regex("[.,!?;:]"), "")
            .trim()

        if (trimmed.isBlank()) {
            return VoiceCommand.Unknown(rawText)
        }

        val hasWakeWord = containsWakeWord(trimmed)
        if (requireWakeWord && !hasWakeWord) {
            return VoiceCommand.Unknown(rawText)
        }

        // Strip wake word prefix if present
        val commandBody = WAKE_WORD_REGEX.replace(trimmed, "").trim()

        if (commandBody.isEmpty()) {
            return VoiceCommand.Unknown(rawText)
        }

        return parseCommandBody(commandBody, rawText, hasWakeWord)
    }

    private fun parseCommandBody(body: String, rawOriginal: String, hasWakeWord: Boolean): VoiceCommand {
        val normalized = body.trim()

        // 1. Play Cached / Downloaded / Library Songs
        if (PLAY_CACHED_REGEX.matches(normalized)) {
            return VoiceCommand.PlayCachedSongs
        }

        // 2. Play Liked / Favorite Songs
        if (PLAY_LIKED_REGEX.matches(normalized)) {
            return VoiceCommand.PlayLikedSongs
        }

        // 3. Generic Play Songs / Play Music
        if (normalized == "play songs" || normalized == "play song" || normalized == "play some songs" ||
            normalized == "play music" || normalized == "play some music" || normalized == "start music" ||
            normalized == "play something" || normalized == "play anything" || normalized == "shuffle music" ||
            normalized == "shuffle songs"
        ) {
            return VoiceCommand.PlayGenericMusic
        }

        // 4. Play / Search Song with explicit prefix
        val playPrefixMatch = Regex(
            "^(?:play|listen\\s+to|put\\s+on|stream|find)(?:\\s+the\\s+song|\\s+the\\s+track|\\s+song|\\s+track)?(?:\\s+called)?(?:\\s+)(.+)$",
            RegexOption.IGNORE_CASE
        ).find(normalized)

        if (playPrefixMatch != null) {
            val query = playPrefixMatch.groupValues[1]
                .replace(Regex("^(?:the\\s+)?music\\s+", RegexOption.IGNORE_CASE), "")
                .trim()
            if (query.isNotBlank() && query != "music" && query != "song" && query != "songs" && query != "something" && query != "anything") {
                return VoiceCommand.PlaySong(query)
            }
        }

        // 5. Simple Resume / Play without query
        if (normalized == "play" || normalized == "resume" || normalized == "continue" ||
            normalized == "unpause"
        ) {
            return VoiceCommand.Resume
        }

        // 5. Pause / Stop
        if (normalized == "pause" || normalized == "stop" || normalized == "pause music" ||
            normalized == "stop music" || normalized == "halt" || normalized == "freeze" ||
            normalized == "shut up" || normalized == "hold on"
        ) {
            return VoiceCommand.Pause
        }

        // 6. Next Track / Skip
        if (normalized == "next" || normalized == "skip" || normalized == "next song" ||
            normalized == "next track" || normalized == "skip song" || normalized == "skip track" ||
            normalized == "play next" || normalized == "skip this"
        ) {
            return VoiceCommand.NextTrack
        }

        // 7. Previous Track / Back
        if (normalized == "previous" || normalized == "previous song" || normalized == "previous track" ||
            normalized == "go back" || normalized == "back" || normalized == "last song" ||
            normalized == "play previous" || normalized == "replay song"
        ) {
            return VoiceCommand.PreviousTrack
        }

        // 8. Like / Favorite
        if (normalized == "like" || normalized == "like this song" || normalized == "like the song" ||
            normalized == "favorite" || normalized == "add to favorites" || normalized == "love this song" ||
            normalized == "thumbs up" || normalized == "thumb up"
        ) {
            return VoiceCommand.ToggleLike
        }

        // 9. Radio
        if (normalized == "start radio" || normalized == "play radio" || normalized == "radio" ||
            normalized == "start station" || normalized == "similar songs" || normalized == "more like this"
        ) {
            return VoiceCommand.StartRadio
        }

        // 10. Volume Controls
        val volumeMatch = VOLUME_PERCENT_REGEX.find(normalized)
        if (volumeMatch != null) {
            val levelStr = volumeMatch.groupValues[1].ifBlank { volumeMatch.groupValues[2] }
            val level = levelStr.toIntOrNull()
            if (level != null) {
                return VoiceCommand.SetVolume(level.coerceIn(0, 100))
            }
        }

        if (normalized == "volume up" || normalized == "increase volume" || normalized == "louder" ||
            normalized == "turn it up" || normalized == "turn up" || normalized == "raise volume"
        ) {
            return VoiceCommand.VolumeUp
        }

        if (normalized == "volume down" || normalized == "decrease volume" || normalized == "softer" ||
            normalized == "turn it down" || normalized == "turn down" || normalized == "lower volume"
        ) {
            return VoiceCommand.VolumeDown
        }

        if (normalized == "mute" || normalized == "silence" || normalized == "be quiet") {
            return VoiceCommand.Mute
        }

        if (normalized == "unmute") {
            return VoiceCommand.Unmute
        }

        // 11. If wake word was spoken followed directly by a song title (e.g. "Hi AirBeats Starboy")
        if (hasWakeWord && normalized.length >= 2 && !isControlKeyword(normalized)) {
            val cleanSong = normalized
                .replace(Regex("^(?:song|track|music)\\s+", RegexOption.IGNORE_CASE), "")
                .trim()
            if (cleanSong.isNotBlank()) {
                return VoiceCommand.PlaySong(cleanSong)
            }
        }

        return VoiceCommand.Unknown(rawOriginal)
    }

    private fun isControlKeyword(text: String): Boolean {
        val controls = listOf(
            "play", "pause", "stop", "resume", "next", "skip", "previous", "back",
            "like", "favorite", "radio", "volume", "mute", "unmute"
        )
        return controls.contains(text.lowercase(Locale.ROOT))
    }
}
