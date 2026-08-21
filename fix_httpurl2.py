import re

file_path = 'app/src/main/java/com/darkxvenom/airbeats/utils/YTPlayerUtils.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('import okhttp3.OkHttpClient', 'import okhttp3.OkHttpClient\nimport okhttp3.HttpUrl.Companion.toHttpUrlOrNull')
content = content.replace('@Suppress("DEPRECATION")\n            val httpUrl = okhttp3.HttpUrl.parse(url)', 'val httpUrl = url.toHttpUrlOrNull()')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
