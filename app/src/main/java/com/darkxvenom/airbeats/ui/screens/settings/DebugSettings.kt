package com.darkxvenom.airbeats.ui.screens.settings

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.utils.GlobalLog
import com.darkxvenom.airbeats.utils.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettings(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Experimental Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LogViewerPanel()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogViewerPanel() {
    val allLogs by GlobalLog.logs.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var filterMode by remember { mutableStateOf(1) }
    var selectedLevels by remember {
        mutableStateOf(setOf(Log.INFO, Log.WARN, Log.ERROR))
    }
    var levelsMenuExpanded by remember { mutableStateOf(false) }

    val filtered = remember(allLogs, filterMode, selectedLevels) {
        allLogs.filter { entry ->
            val tagMatch = when (filterMode) {
                0 -> entry.tag?.contains("Discord", true) == true ||
                        entry.message.contains("Discord", true) == true
                else -> true
            }
            val levelMatch = selectedLevels.contains(entry.level)
            tagMatch && levelMatch
        }
    }

    val listState = rememberLazyListState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.manage_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Debug logs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${filtered.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Box {
                    FilledTonalIconButton(
                        onClick = { levelsMenuExpanded = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Filter Levels"
                        )
                    }

                    DropdownMenu(
                        expanded = levelsMenuExpanded,
                        onDismissRequest = { levelsMenuExpanded = false }
                    ) {
                        LogLevelMenuItem(
                            label = "Verbose",
                            level = Log.VERBOSE,
                            selectedLevels = selectedLevels,
                            onToggle = { level ->
                                selectedLevels = if (selectedLevels.contains(level))
                                    selectedLevels - level
                                else
                                    selectedLevels + level
                            }
                        )
                        LogLevelMenuItem(
                            label = "Debug",
                            level = Log.DEBUG,
                            selectedLevels = selectedLevels,
                            onToggle = { level ->
                                selectedLevels = if (selectedLevels.contains(level))
                                    selectedLevels - level
                                else
                                    selectedLevels + level
                            }
                        )
                        LogLevelMenuItem(
                            label = "Info",
                            level = Log.INFO,
                            selectedLevels = selectedLevels,
                            onToggle = { level ->
                                selectedLevels = if (selectedLevels.contains(level))
                                    selectedLevels - level
                                else
                                    selectedLevels + level
                            }
                        )
                        LogLevelMenuItem(
                            label = "Warning",
                            level = Log.WARN,
                            selectedLevels = selectedLevels,
                            onToggle = { level ->
                                selectedLevels = if (selectedLevels.contains(level))
                                    selectedLevels - level
                                else
                                    selectedLevels + level
                            }
                        )
                        LogLevelMenuItem(
                            label = "Error",
                            level = Log.ERROR,
                            selectedLevels = selectedLevels,
                            onToggle = { level ->
                                selectedLevels = if (selectedLevels.contains(level))
                                    selectedLevels - level
                                else
                                    selectedLevels + level
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            onClick = {
                                selectedLevels = setOf(Log.INFO, Log.WARN, Log.ERROR)
                                levelsMenuExpanded = false
                            },
                            text = {
                                Text(
                                    text = "Reset to Default",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.restore),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = filterMode == 0,
                    onClick = { filterMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { }
                ) {
                    Text("Discord only")
                }
                SegmentedButton(
                    selected = filterMode == 1,
                    onClick = { filterMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { }
                ) {
                    Text("All Logs")
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 500.dp)
            ) {
                if (filtered.isEmpty()) {
                    EmptyLogPlaceholder()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .nestedScroll(rememberNestedScrollInteropConnection())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(filtered) { _, entry ->
                            LogEntryItem(
                                entry = entry,
                                clipboard = clipboard,
                                coroutineScope = coroutineScope
                            )
                        }
                    }
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { GlobalLog.clear() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text("Clear")
                }
                androidx.compose.material3.Button(
                    onClick = {
                        val sb = StringBuilder()
                        filtered.forEach { sb.appendLine(GlobalLog.format(it)) }
                        val copiedMessage = "Copied ${filtered.size} logs to clipboard"
                        clipboard.setText(AnnotatedString(sb.toString()))
                        GlobalLog.append(Log.INFO, "DebugSettings", copiedMessage)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
                    Text("Copy")
                }
            }
        }
    }
}

@Composable
private fun EmptyLogPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.info),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "No logs match the current filters.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Adjust the level or tag filter to see more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LogEntryItem(
    entry: LogEntry,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    coroutineScope: CoroutineScope
) {
    val levelColor = when (entry.level) {
        Log.ERROR -> MaterialTheme.colorScheme.error
        Log.WARN -> androidx.compose.ui.graphics.Color(0xFFFFA000)
        Log.INFO -> MaterialTheme.colorScheme.primary
        Log.DEBUG -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    var showCopiedFeedback by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (showCopiedFeedback) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else 
            MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(top = 6.dp)
                    .background(levelColor, androidx.compose.foundation.shape.CircleShape)
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val timeString = runCatching {
                        java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(entry.time))
                    }.getOrDefault("")
                    
                    Text(
                        text = entry.tag ?: "Unknown",
                        style = MaterialTheme.typography.labelMedium,
                        color = levelColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = entry.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun LogLevelMenuItem(
    label: String,
    level: Int,
    selectedLevels: Set<Int>,
    onToggle: (Int) -> Unit
) {
    val isSelected = selectedLevels.contains(level)
    DropdownMenuItem(
        onClick = { onToggle(level) },
        text = { Text(label) },
        trailingIcon = {
            androidx.compose.material3.Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle(level) }
            )
        }
    )
}
