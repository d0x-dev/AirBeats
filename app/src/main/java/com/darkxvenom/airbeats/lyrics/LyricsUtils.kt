package com.darkxvenom.airbeats.lyrics

import android.text.format.DateUtils
import com.darkxvenom.airbeats.ui.component.ANIMATE_SCROLL_DURATION

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

    fun parseLyrics(lyrics: String): List<LyricsEntry> =
        lyrics
            .lines()
            .flatMap { line ->
                parseLine(line).orEmpty()
            }.sorted()

    private fun parseLine(line: String): List<LyricsEntry>? {
        if (line.isEmpty()) {
            return null
        }
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        val text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                val mil = when (milString.length) {
                    1 -> milString.toLong() * 100
                    2 -> milString.toLong() * 10
                    else -> milString.take(3).toLong()
                }
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                LyricsEntry(time, text)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.lastIndex
        while (low <= high) {
            val mid = (low + high).ushr(1)
            if (lines[mid].time <= position) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return high.coerceIn(0, lines.lastIndex)
    }
}
