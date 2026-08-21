import re

file_path = 'app/src/main/java/com/darkxvenom/airbeats/viewmodels/NewReleaseViewModel.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove enum and extension
content = re.sub(r'enum class AlbumReleaseType \{.*?\}', '', content, flags=re.DOTALL)
content = re.sub(r'val AlbumItem\.releaseType: AlbumReleaseType.*?else -> AlbumReleaseType\.ALBUM\n        \}', '', content, flags=re.DOTALL)

# Add import if missing
if 'import com.darkxvenom.airbeats.innertube.models.AlbumReleaseType' not in content:
    content = content.replace('import com.darkxvenom.airbeats.innertube.models.AlbumItem', 'import com.darkxvenom.airbeats.innertube.models.AlbumItem\nimport com.darkxvenom.airbeats.innertube.models.AlbumReleaseType')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
