import re
import glob

files = [
    'app/src/main/java/com/darkxvenom/airbeats/ui/menu/YouTubePlaylistMenu.kt',
    'app/src/main/java/com/darkxvenom/airbeats/ui/screens/library/YouTubeImportDialog.kt',
    'app/src/main/java/com/darkxvenom/airbeats/ui/screens/playlist/LocalPlaylistScreen.kt',
    'app/src/main/java/com/darkxvenom/airbeats/viewmodels/OnlinePlaylistViewModel.kt'
]

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    content = content.replace('.completed()', '.completedPlaylistPage()')
    content = content.replace('import com.darkxvenom.airbeats.innertube.utils.completed', 'import com.darkxvenom.airbeats.innertube.utils.completedPlaylistPage')

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
