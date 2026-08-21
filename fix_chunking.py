import re

file_path = 'app/src/main/java/com/darkxvenom/airbeats/playback/MusicService.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

old_cache_return = '''            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }'''

new_cache_return = '''            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
                    .subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
            }'''

content = content.replace(old_cache_return, new_cache_return)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
