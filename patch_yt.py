import os

with open('app/src/main/java/com/darkxvenom/airbeats/utils/YTPlayerUtils.kt', 'r', encoding='utf-8') as f:
    airbeats_content = f.read()

with open('C:/projects/OpenTune_Temp/app/src/main/kotlin/com/arturo254/opentune/utils/YTPlayerUtils.kt', 'r', encoding='utf-8') as f:
    opentune_content = f.read()

opentune_content = opentune_content.replace('com.arturo254.opentune', 'com.darkxvenom.airbeats')

with open('app/src/main/java/com/darkxvenom/airbeats/utils/YTPlayerUtils.kt', 'w', encoding='utf-8') as f:
    f.write(opentune_content)
