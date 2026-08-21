file_path = 'app/src/main/java/com/darkxvenom/airbeats/utils/SyncUtils.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('YouTube.playlist(browseId).completedLibraryPage()', 'YouTube.playlist(browseId).completedPlaylistPage()')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
