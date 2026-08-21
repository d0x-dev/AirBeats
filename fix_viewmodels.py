import re

# HomeViewModel
hv_path = 'app/src/main/java/com/darkxvenom/airbeats/viewmodels/HomeViewModel.kt'
with open(hv_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('homePage.value = HomePage(', 'homePage.value = HomePage(chips = null, ')

with open(hv_path, 'w', encoding='utf-8') as f:
    f.write(content)

# NewReleaseViewModel
nv_path = 'app/src/main/java/com/darkxvenom/airbeats/viewmodels/NewReleaseViewModel.kt'
with open(nv_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Just remove the local enum if it exists, or update the usage
content = content.replace('com.darkxvenom.airbeats.viewmodels.AlbumReleaseType', 'com.darkxvenom.airbeats.innertube.models.AlbumReleaseType')

with open(nv_path, 'w', encoding='utf-8') as f:
    f.write(content)
