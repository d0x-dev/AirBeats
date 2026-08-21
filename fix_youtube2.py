import re

file_path = 'innertube/src/main/java/com/darkxvenom/airbeats/innertube/YouTube.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('suspend fun libraryContinuation(continuation: String): Result<com.darkxvenom.airbeats.innertube.pages.LibraryPage> = runCatching {', 'suspend fun libraryContinuation(continuation: String): Result<com.darkxvenom.airbeats.innertube.pages.LibraryContinuationPage> = runCatching {')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
