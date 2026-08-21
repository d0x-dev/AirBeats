import re

file_path = 'app/src/main/java/com/darkxvenom/airbeats/ui/screens/apple/AppleScreens.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('item.thumbnail.highQualityThumbnail()', 'item.thumbnail?.highQualityThumbnail() ?: ""')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
