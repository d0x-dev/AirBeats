import os

# AccountViewModel
av_path = 'app/src/main/java/com/darkxvenom/airbeats/viewmodels/AccountViewModel.kt'
with open(av_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('.completedLibraryPage()', '.completed()')

with open(av_path, 'w', encoding='utf-8') as f:
    f.write(content)

# HomeViewModel
hv_path = 'app/src/main/java/com/darkxvenom/airbeats/viewmodels/HomeViewModel.kt'
with open(hv_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('.completedLibraryPage()', '.completed()')

with open(hv_path, 'w', encoding='utf-8') as f:
    f.write(content)
