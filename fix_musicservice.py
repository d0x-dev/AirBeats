import re

with open('app/src/main/java/com/darkxvenom/airbeats/playback/MusicService.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = '''                if (enabled && Settings.canDrawOverlays(this)) {
                    startService(Intent(this, DynamicIslandService::class.java))
                } else {
                    stopService(Intent(this, DynamicIslandService::class.java))
                }'''

replacement = '''                if (enabled && Settings.canDrawOverlays(this)) {
                    try {
                        startService(Intent(this, DynamicIslandService::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    try {
                        stopService(Intent(this, DynamicIslandService::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }'''

if target in content:
    content = content.replace(target, replacement)
else:
    print("Could not find the target string in MusicService.kt")

with open('app/src/main/java/com/darkxvenom/airbeats/playback/MusicService.kt', 'w', encoding='utf-8') as f:
    f.write(content)
