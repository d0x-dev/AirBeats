import re
import glob

# Revert AccountViewModel, HomeViewModel, SyncUtils
files = [
    'app/src/main/java/com/darkxvenom/airbeats/viewmodels/AccountViewModel.kt',
    'app/src/main/java/com/darkxvenom/airbeats/viewmodels/HomeViewModel.kt',
    'app/src/main/java/com/darkxvenom/airbeats/utils/SyncUtils.kt'
]

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    content = content.replace('.completed()', '.completedLibraryPage()')
    content = content.replace('import com.darkxvenom.airbeats.innertube.utils.completed', 'import com.darkxvenom.airbeats.innertube.utils.completedLibraryPage')

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

# Fix Utils.kt in innertube
utils_path = 'innertube/src/main/java/com/darkxvenom/airbeats/innertube/utils/Utils.kt'
with open(utils_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('@JvmName("completedLibrary")\nsuspend fun Result<PlaylistPage>.completed(): Result<PlaylistPage>', 'suspend fun Result<PlaylistPage>.completedPlaylistPage(): Result<PlaylistPage>')
content = content.replace('@JvmName("completedPlaylist")\nsuspend fun Result<LibraryPage>.completed(): Result<LibraryPage>', 'suspend fun Result<LibraryPage>.completedLibraryPage(): Result<LibraryPage>')

with open(utils_path, 'w', encoding='utf-8') as f:
    f.write(content)

