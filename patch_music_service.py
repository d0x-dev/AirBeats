import re

# Patch MusicService.kt
file_path = 'app/src/main/java/com/darkxvenom/airbeats/playback/MusicService.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add mediaOkHttpClient property
media_http_client = '''
    private val mediaOkHttpClient: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .proxy(YouTube.proxy)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val isYouTubeMediaHost =
                    host.endsWith("googlevideo.com") ||
                        host.endsWith("googleusercontent.com") ||
                        host.endsWith("youtube.com") ||
                        host.endsWith("youtube-nocookie.com") ||
                        host.endsWith("ytimg.com")

                if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)

                val clientParam = request.url.queryParameter("c")?.trim().orEmpty()

                val userAgent = com.darkxvenom.airbeats.utils.StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = com.darkxvenom.airbeats.utils.StreamClientUtils.resolveOriginReferer(clientParam)

                val builder = request.newBuilder().header("User-Agent", userAgent)
                originReferer.origin?.let { builder.header("Origin", it) }
                originReferer.referer?.let { builder.header("Referer", it) }

                chain.proceed(builder.build())
            }.build()
    }
'''

if "private val mediaOkHttpClient" not in content:
    content = re.sub(r'(class MusicService : MediaLibraryService\(\) \{)', r'\1\n' + media_http_client, content)

# Replace the OkHttpClient builder inside OkHttpDataSource.Factory
old_datasource = '''OkHttpDataSource.Factory(
                                OkHttpClient
                                    .Builder()
                                    .proxy(YouTube.proxy)
                                    .build(),
                            )'''

new_datasource = '''OkHttpDataSource.Factory(
                                mediaOkHttpClient,
                            )'''

content = content.replace(old_datasource, new_datasource)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

# Patch DownloadUtil.kt
file_path_dl = 'app/src/main/java/com/darkxvenom/airbeats/playback/DownloadUtil.kt'
with open(file_path_dl, 'r', encoding='utf-8') as f:
    content_dl = f.read()

if "private val mediaOkHttpClient" not in content_dl:
    content_dl = re.sub(r'(object DownloadUtil \{)', r'\1\n' + media_http_client, content_dl)

content_dl = content_dl.replace(old_datasource, new_datasource)

with open(file_path_dl, 'w', encoding='utf-8') as f:
    f.write(content_dl)
