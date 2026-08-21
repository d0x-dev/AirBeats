import re

file_path = 'innertube/src/main/java/com/darkxvenom/airbeats/innertube/pages/NewPipe.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the trailing syntax errors
bad_tail = '''        } ?: throw ParsingException("Could not find format url")

            return@runCatching YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url
            )
        }'''

if bad_tail in content:
    content = content.replace(bad_tail, '        }')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
