import re
import glob

files = [
    'app/src/main/java/com/darkxvenom/airbeats/viewmodels/AccountViewModel.kt',
    'app/src/main/java/com/darkxvenom/airbeats/viewmodels/HomeViewModel.kt',
    'app/src/main/java/com/darkxvenom/airbeats/utils/SyncUtils.kt'
]

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace the import
    content = content.replace('com.darkxvenom.airbeats.innertube.utils.completedLibraryPage', 'com.darkxvenom.airbeats.innertube.utils.completed')

    # Since I might have replaced completedLibraryPage with completed, let's make sure the import is correct.
    if 'com.darkxvenom.airbeats.innertube.utils.completed' not in content:
        content = content.replace('import com.darkxvenom.airbeats.innertube.YouTube', 'import com.darkxvenom.airbeats.innertube.YouTube\nimport com.darkxvenom.airbeats.innertube.utils.completed')

    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
