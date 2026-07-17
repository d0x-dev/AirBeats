import re

# Fix IosStyledPlayer.kt
with open("app/src/main/java/com/darkxvenom/airbeats/ui/player/IosStyledPlayer.kt", "r", encoding="utf-8") as f:
    ios_player = f.read()

# Remove bottomSheetPageState
ios_player = ios_player.replace("bottomSheetPageState: BottomSheetPageState,", "")
# Remove imports of BottomSheetPageState and SongDetailsDialogState if they exist
ios_player = ios_player.replace("import com.darkxvenom.airbeats.ui.component.BottomSheetPageState\n", "")
ios_player = ios_player.replace("import com.darkxvenom.airbeats.ui.component.SongDetailsDialogState\n", "")

var_definition = """    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {"""
ios_player = ios_player.replace("    Box(modifier = Modifier.fillMaxSize()) {", var_definition)

imports_for_ios = """import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.darkxvenom.airbeats.ui.component.SongDetailsDialog
"""
if "import com.darkxvenom.airbeats.ui.component.SongDetailsDialog\n" not in ios_player:
    ios_player = ios_player.replace("import androidx.compose.runtime.Composable", imports_for_ios + "import androidx.compose.runtime.Composable")

with open("app/src/main/java/com/darkxvenom/airbeats/ui/player/IosStyledPlayer.kt", "w", encoding="utf-8") as f:
    f.write(ios_player)


# Fix AlternateQueue.kt
with open("app/src/main/java/com/darkxvenom/airbeats/ui/player/AlternateQueue.kt", "r", encoding="utf-8") as f:
    alt_queue = f.read()

alt_queue_var = """    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }"""
alt_queue = alt_queue.replace("    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()", alt_queue_var)

if "import com.darkxvenom.airbeats.ui.component.SongDetailsDialog\n" not in alt_queue:
    alt_queue = alt_queue.replace("import androidx.compose.runtime.Composable", imports_for_ios + "import androidx.compose.runtime.Composable")

with open("app/src/main/java/com/darkxvenom/airbeats/ui/player/AlternateQueue.kt", "w", encoding="utf-8") as f:
    f.write(alt_queue)

print("Fixed variables!")
