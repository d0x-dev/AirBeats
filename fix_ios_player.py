import re

with open("app/src/main/java/com/darkxvenom/airbeats/ui/player/IosStyledPlayer.kt", "r", encoding="utf-8") as f:
    content = f.read()

# The block to remove:
bad_block = """                var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {"""

if bad_block in content:
    content = content.replace(bad_block, "                Box(modifier = Modifier.fillMaxSize()) {")

# The block to inject at the top of IosStyledPlayer:
good_block = """) {
    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {"""

if good_block not in content:
    content = content.replace(") {\n    Box(modifier = Modifier.fillMaxSize()) {", good_block, 1)

with open("app/src/main/java/com/darkxvenom/airbeats/ui/player/IosStyledPlayer.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("Fixed IosStyledPlayer variables again!")
