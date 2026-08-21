file_path = 'app/src/main/java/com/darkxvenom/airbeats/utils/YTPlayerUtils.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('okhttp3.HttpUrl.Companion.toHttpUrlOrNull(url)', 'okhttp3.HttpUrl.parse(url)')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
