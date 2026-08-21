import re

# DatabaseDao.kt
db_path = 'app/src/main/java/com/darkxvenom/airbeats/db/DatabaseDao.kt'
with open(db_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('artistPage.artist.thumbnail.resize(544, 544)', 'artistPage.artist.thumbnail?.resize(544, 544)')

with open(db_path, 'w', encoding='utf-8') as f:
    f.write(content)

# NewReleaseScreen.kt
nr_path = 'app/src/main/java/com/darkxvenom/airbeats/ui/screens/NewReleaseScreen.kt'
with open(nr_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import com.darkxvenom.airbeats.viewmodels.AlbumReleaseType', 'import com.darkxvenom.airbeats.innertube.models.AlbumReleaseType')

with open(nr_path, 'w', encoding='utf-8') as f:
    f.write(content)

# SyncUtils.kt (LM playlist)
sync_path = 'app/src/main/java/com/darkxvenom/airbeats/utils/SyncUtils.kt'
with open(sync_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('YouTube.playlist("LM").completedLibraryPage()', 'YouTube.playlist("LM").completedPlaylistPage()')

with open(sync_path, 'w', encoding='utf-8') as f:
    f.write(content)
