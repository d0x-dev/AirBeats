import re

# Fix PlayerMenu.kt
with open("app/src/main/java/com/darkxvenom/airbeats/ui/menu/PlayerMenu.kt", "r", encoding="utf-8") as f:
    player_menu = f.read()

imports_to_add = """
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.Color
"""
player_menu = player_menu.replace("import androidx.compose.foundation.lazy.grid.LazyVerticalGrid", imports_to_add + "import androidx.compose.foundation.lazy.grid.LazyVerticalGrid")

with open("app/src/main/java/com/darkxvenom/airbeats/ui/menu/PlayerMenu.kt", "w", encoding="utf-8") as f:
    f.write(player_menu)

# Fix IosStyledPlayer.kt
with open("app/src/main/java/com/darkxvenom/airbeats/ui/player/IosStyledPlayer.kt", "r", encoding="utf-8") as f:
    ios_player = f.read()

ios_player = ios_player.replace("import com.darkxvenom.airbeats.ui.component.SongDetailsDialogState\n", "")
ios_player = ios_player.replace("import com.darkxvenom.airbeats.ui.component.BottomSheetPageState\n", "")
ios_player = ios_player.replace("bottomSheetPageState: BottomSheetPageState,", "")
# Find where variables are defined
var_definition = """    val clipboardManager = LocalClipboard.current

    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }
"""
ios_player = ios_player.replace("    val clipboardManager = LocalClipboard.current", var_definition)

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

alt_queue_var = """    val clipboardManager = LocalClipboard.current

    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }

    if (showDetailsDialog) {
        SongDetailsDialog(
            mediaMetadata = mediaMetadata,
            onDismiss = { showDetailsDialog = false }
        )
    }
"""
alt_queue = alt_queue.replace("    val clipboardManager = LocalClipboard.current", alt_queue_var)
if "import com.darkxvenom.airbeats.ui.component.SongDetailsDialog\n" not in alt_queue:
    alt_queue = alt_queue.replace("import androidx.compose.runtime.Composable", imports_for_ios + "import androidx.compose.runtime.Composable")

with open("app/src/main/java/com/darkxvenom/airbeats/ui/player/AlternateQueue.kt", "w", encoding="utf-8") as f:
    f.write(alt_queue)

print("Fixed imports and variables!")
