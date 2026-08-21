import re

file_path = 'app/src/main/java/com/darkxvenom/airbeats/utils/YTPlayerUtils.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('validateStatus(streamUrl)', 'validateStatus(streamUrl, client.userAgent)')
content = content.replace('findUrlOrNull(format, videoId)', 'findUrlOrNull(format, videoId, client)')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
