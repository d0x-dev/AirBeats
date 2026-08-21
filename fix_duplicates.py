import re

file_path = 'app/src/main/java/com/darkxvenom/airbeats/utils/YTPlayerUtils.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

bot_detection = '''
    private fun isBotDetectionError(reason: String): Boolean {
        val lower = reason.lowercase(java.util.Locale.US)
        return "bot" in lower ||
            "unusual traffic" in lower ||
            "automated" in lower ||
            "confirm" in lower && "not a" in lower ||
            "not a robot" in lower ||
            "verify" in lower && "human" in lower
    }

    fun isBotDetectionException(error: androidx.media3.common.PlaybackException): Boolean {
        val message = error.message.orEmpty()
        if (isBotDetectionError(message)) return true
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (isBotDetectionError(cause.message.orEmpty())) return true
            cause = cause.cause
        }
        return false
    }'''

# Replace all occurrences of bot_detection with nothing
content = content.replace(bot_detection, '')

# Append it once
content = re.sub(r'\}\s*$', bot_detection + '\n}\n', content.strip())

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
