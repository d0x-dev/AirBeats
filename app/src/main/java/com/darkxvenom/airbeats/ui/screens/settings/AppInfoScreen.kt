package com.darkxvenom.airbeats.ui.screens.settings

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.darkxvenom.airbeats.BuildConfig
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.isNewerVersion
import com.darkxvenom.airbeats.ui.component.ScreenAdaptiveBackground
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.ui.component.settingsCardBorder
import com.darkxvenom.airbeats.ui.component.settingsCardContainerColor
import com.darkxvenom.airbeats.utils.AppUpdateService
import com.darkxvenom.airbeats.utils.GitHubApiClient
import com.darkxvenom.airbeats.utils.GitHubCommitSummary
import com.darkxvenom.airbeats.utils.GitHubReleaseItem
import com.darkxvenom.airbeats.utils.RemoteConfigManager
import com.darkxvenom.airbeats.utils.UpdateDownloadState
import com.darkxvenom.airbeats.utils.UpdateInfo
import com.darkxvenom.airbeats.utils.Updater
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==================== UI STATE & VIEWMODEL ====================

sealed interface UpdateCheckUiState {
    data object Idle : UpdateCheckUiState
    data object Checking : UpdateCheckUiState
    data class UpToDate(val versionName: String) : UpdateCheckUiState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateCheckUiState
    data class Error(val message: String) : UpdateCheckUiState
}

data class AppInfoUiState(
    val isRefreshing: Boolean = false,
    val updateCheckState: UpdateCheckUiState = UpdateCheckUiState.Idle,
    val releases: List<GitHubReleaseItem> = emptyList(),
    val isLoadingReleases: Boolean = true,
    val releasesError: String? = null,
    val commits: List<GitHubCommitSummary> = emptyList(),
    val isLoadingCommits: Boolean = true,
    val commitsError: String? = null,
    val isPreparingNightlyDownload: Boolean = false,
    val nightlyDownloadError: String? = null
)

class AppInfoViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppInfoUiState())
    val uiState: StateFlow<AppInfoUiState> = _uiState.asStateFlow()

    private val gitHubClient = GitHubApiClient()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            launch { checkRealtimeUpdates() }
            launch { fetchReleases() }
            launch { fetchCommits() }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun checkRealtimeUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateCheckState = UpdateCheckUiState.Checking) }
            try {
                val result = Updater.getLatestUpdateInfo()
                result.fold(
                    onSuccess = { info ->
                        if (info.versionName.isNotBlank() && isNewerVersion(info.versionName, BuildConfig.VERSION_NAME)) {
                            _uiState.update { it.copy(updateCheckState = UpdateCheckUiState.UpdateAvailable(info)) }
                        } else {
                            _uiState.update { it.copy(updateCheckState = UpdateCheckUiState.UpToDate(BuildConfig.VERSION_NAME)) }
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(updateCheckState = UpdateCheckUiState.Error(err.localizedMessage ?: "Failed to check for updates"))
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error checking real-time updates")
                _uiState.update {
                    it.copy(updateCheckState = UpdateCheckUiState.Error(e.localizedMessage ?: "Network error while checking updates"))
                }
            }
        }
    }

    fun startNightlySwitchDownload(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingNightlyDownload = true, nightlyDownloadError = null) }
            try {
                val nightlyResult = Updater.getLatestUpdateInfo(isNightly = true)
                nightlyResult.fold(
                    onSuccess = { info ->
                        val downloadUrl = info.apkDownloadUrl.ifBlank {
                            RemoteConfigManager.getApkDownloadUrl(info.versionName, isNightly = true)
                        }
                        if (downloadUrl.isNotBlank()) {
                            AppUpdateService.start(context, downloadUrl)
                            _uiState.update { it.copy(isPreparingNightlyDownload = false) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isPreparingNightlyDownload = false,
                                    nightlyDownloadError = "Nightly APK download link not found"
                                )
                            }
                        }
                    },
                    onFailure = { err ->
                        _uiState.update {
                            it.copy(
                                isPreparingNightlyDownload = false,
                                nightlyDownloadError = err.localizedMessage ?: "Could not fetch latest nightly build"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to start nightly download")
                _uiState.update {
                    it.copy(
                        isPreparingNightlyDownload = false,
                        nightlyDownloadError = e.localizedMessage ?: "Download error"
                    )
                }
            }
        }
    }

    private suspend fun fetchReleases() {
        _uiState.update { it.copy(isLoadingReleases = true, releasesError = null) }
        try {
            val list = gitHubClient.getRecentReleases("d0x-dev", "AirBeats", perPage = 8)
            _uiState.update { it.copy(releases = list, isLoadingReleases = false) }
        } catch (e: Exception) {
            Timber.e(e, "Error loading releases")
            _uiState.update { it.copy(isLoadingReleases = false, releasesError = e.localizedMessage) }
        }
    }

    private suspend fun fetchCommits() {
        _uiState.update { it.copy(isLoadingCommits = true, commitsError = null) }
        try {
            val list = gitHubClient.getRecentCommits("d0x-dev", "AirBeats", perPage = 8)
            _uiState.update { it.copy(commits = list, isLoadingCommits = false) }
        } catch (e: Exception) {
            Timber.e(e, "Error loading commits")
            _uiState.update { it.copy(isLoadingCommits = false, commitsError = e.localizedMessage) }
        }
    }
}

// ==================== MAIN APP INFO SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppInfoScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    viewModel: AppInfoViewModel = viewModel()
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    val downloadState by AppUpdateService.downloadState.collectAsState()

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val refreshRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "refreshRotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenAdaptiveBackground(artworkUrl = mediaMetadata?.thumbnailUrl)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_info),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refreshAll() },
                            enabled = !uiState.isRefreshing
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = stringResource(R.string.ui_text_refresh),
                                modifier = if (uiState.isRefreshing) Modifier.rotate(refreshRotation) else Modifier
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Hero App Header
                item {
                    AppInfoHeaderCard(
                        onCopyPackageName = {
                            clipboardManager.setText(AnnotatedString(context.packageName))
                            Toast.makeText(context, "Package ID copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 2. Real-Time Update Checker
                item {
                    RealtimeUpdateCard(
                        updateCheckState = uiState.updateCheckState,
                        onCheckUpdates = { viewModel.checkRealtimeUpdates() },
                        onStartDownload = { apkUrl ->
                            AppUpdateService.start(context, apkUrl)
                        }
                    )
                }

                // 3. In-App Download Progress Banner (Active when downloading or completed)
                if (downloadState !is UpdateDownloadState.Idle) {
                    item {
                        ActiveDownloadCard(
                            downloadState = downloadState,
                            onInstall = { apkFile ->
                                AppUpdateService.openInstaller(context, apkFile)
                            },
                            onDismiss = {
                                AppUpdateService.resetState()
                            }
                        )
                    }
                }

                // 4. Switch to Nightly Channel (or Switch to Stable)
                item {
                    ChannelSwitcherCard(
                        isNightly = BuildConfig.IS_NIGHTLY,
                        isPreparing = uiState.isPreparingNightlyDownload,
                        error = uiState.nightlyDownloadError,
                        onSwitchToNightly = {
                            viewModel.startNightlySwitchDownload(context)
                        },
                        onSwitchToStable = {
                            uriHandler.openUri(RemoteConfigManager.getLatestReleasePageUrl())
                        }
                    )
                }

                // 5. Desktop Platforms & Repository Section
                item {
                    DesktopEcosystemSection(
                        onOpenUrl = { url -> uriHandler.openUri(url) }
                    )
                }

                // 6. Recent Releases Section
                item {
                    SectionHeaderRow(
                        title = stringResource(R.string.ui_heading_recent_releases),
                        icon = painterResource(R.drawable.deployed_code_update),
                        actionText = stringResource(R.string.ui_extra_all_releases),
                        onActionClick = {
                            uriHandler.openUri(RemoteConfigManager.getReleasesPageUrl().ifBlank { "https://github.com/d0x-dev/AirBeats/releases" })
                        }
                    )
                }

                if (uiState.isLoadingReleases) {
                    item {
                        SectionLoadingCard(message = "Fetching latest GitHub releases…")
                    }
                } else if (uiState.releasesError != null) {
                    item {
                        SectionErrorCard(
                            message = uiState.releasesError.orEmpty(),
                            onRetry = { viewModel.refreshAll() }
                        )
                    }
                } else {
                    items(uiState.releases.take(4)) { release ->
                        AppReleaseCard(
                            release = release,
                            onDownloadApk = { url ->
                                if (url.endsWith(".apk", ignoreCase = true)) {
                                    AppUpdateService.start(context, url)
                                } else {
                                    uriHandler.openUri(url)
                                }
                            },
                            onOpenGitHub = { url -> uriHandler.openUri(url) }
                        )
                    }
                }

                // 7. Recent Commits Section
                item {
                    SectionHeaderRow(
                        title = stringResource(R.string.ui_heading_recent_commits),
                        icon = painterResource(R.drawable.github),
                        actionText = stringResource(R.string.ui_extra_view_commits),
                        onActionClick = {
                            uriHandler.openUri("https://github.com/d0x-dev/AirBeats/commits")
                        }
                    )
                }

                if (uiState.isLoadingCommits) {
                    item {
                        SectionLoadingCard(message = "Fetching repository commit activity…")
                    }
                } else if (uiState.commitsError != null) {
                    item {
                        SectionErrorCard(
                            message = uiState.commitsError.orEmpty(),
                            onRetry = { viewModel.refreshAll() }
                        )
                    }
                } else {
                    items(uiState.commits.take(5)) { commit ->
                        AppCommitCard(
                            commit = commit,
                            onClick = {
                                val url = commit.html_url.ifBlank { "https://github.com/d0x-dev/AirBeats/commit/${commit.sha}" }
                                uriHandler.openUri(url)
                            }
                        )
                    }
                }

                // 8. Community & Contribution Hub
                item {
                    SectionHeaderRow(
                        title = stringResource(R.string.ui_heading_community_support),
                        icon = painterResource(R.drawable.resource_public),
                        actionText = null,
                        onActionClick = null
                    )
                }

                item {
                    CommunityHubCard(
                        onOpenUrl = { url -> uriHandler.openUri(url) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ==================== UI COMPONENTS ====================

@Composable
private fun AppInfoHeaderCard(
    onCopyPackageName: () -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val isNightly = BuildConfig.IS_NIGHTLY

    val channelColor = if (isNightly) Color(0xFFFFB74D) else MaterialTheme.colorScheme.primary
    val channelBg = if (isNightly) Color(0xFFFFB74D).copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val channelText = if (isNightly) "NIGHTLY CHANNEL" else "STABLE RELEASE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = settingsCardContainerColor(isFrosted)),
        border = settingsCardBorder(isFrosted) ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon with glow aura
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.airbeats_monochrome),
                        contentDescription = stringResource(R.string.ui_a11y_airbeats_logo),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Name
            Text(
                text = stringResource(R.string.ui_text_airbeats),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Version & Channel Tags Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = channelBg,
                    border = BorderStroke(1.dp, channelColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = channelText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp,
                            fontSize = 10.sp
                        ),
                        color = channelColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            // System info chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    modifier = Modifier.weight(1f),
                    label = "Package",
                    value = "com.darkxvenom.airbeats",
                    isClickable = true,
                    onClick = onCopyPackageName
                )
                InfoChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.ui_extra_build_flavor),
                    value = BuildConfig.BUILD_TYPE.uppercase(),
                    isClickable = false
                )
                InfoChip(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.ui_extra_android_target),
                    value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    isClickable = false
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    isClickable: Boolean,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (isClickable && onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RealtimeUpdateCard(
    updateCheckState: UpdateCheckUiState,
    onCheckUpdates: () -> Unit,
    onStartDownload: (String) -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = settingsCardContainerColor(isFrosted)),
        border = settingsCardBorder(isFrosted) ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.update),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.ui_text_check_for_updates),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.ui_sweep_real_time_github_release_check),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onCheckUpdates,
                    enabled = updateCheckState !is UpdateCheckUiState.Checking,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 32.dp, max = 36.dp)
                ) {
                    if (updateCheckState is UpdateCheckUiState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.ui_text_check_now),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (updateCheckState) {
                UpdateCheckUiState.Idle -> {
                    Text(
                        text = stringResource(R.string.ui_sweep_tap_check_now_to_verify_if_your_airbeats_is_running_the_latest_bu),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                UpdateCheckUiState.Checking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.ui_sweep_querying_latest_github_release_information),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is UpdateCheckUiState.UpToDate -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1B5E20).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check_circle),
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "You are on the latest version! (v${updateCheckState.versionName})",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                is UpdateCheckUiState.UpdateAvailable -> {
                    val info = updateCheckState.info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = "Update Available: v${info.versionName}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Current: v${BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = {
                                        val apkUrl = info.apkDownloadUrl.ifBlank { info.releaseUrl }
                                        onStartDownload(apkUrl)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 32.dp, max = 36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.save_snippet),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
                is UpdateCheckUiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = updateCheckState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveDownloadCard(
    downloadState: UpdateDownloadState,
    onInstall: (java.io.File) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            when (downloadState) {
                is UpdateDownloadState.Downloading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = stringResource(R.string.ui_sweep_downloading_update),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (downloadState.progress >= 0f) {
                            Text(
                                text = "${(downloadState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { if (downloadState.progress >= 0f) downloadState.progress else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val downloadedMb = downloadState.downloadedBytes / (1024f * 1024f)
                    val totalMb = downloadState.totalBytes / (1024f * 1024f)
                    val statusText = if (downloadState.totalBytes > 0) {
                        String.format(Locale.getDefault(), "%.1f MB / %.1f MB", downloadedMb, totalMb)
                    } else if (downloadState.downloadedBytes > 0) {
                        String.format(Locale.getDefault(), "%.1f MB downloaded", downloadedMb)
                    } else {
                        "Establishing secure connection…"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is UpdateDownloadState.Completed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check_circle),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.ui_text_update_ready_to_install),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.ui_sweep_apk_verified_downloaded),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = { onInstall(downloadState.apkFile) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 34.dp, max = 36.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ui_text_install_now),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                is UpdateDownloadState.Failed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.ui_text_download_failed),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = downloadState.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_forward),
                                contentDescription = stringResource(R.string.dismiss),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                UpdateDownloadState.Idle -> Unit
            }
        }
    }
}


@Composable
private fun ChannelSwitcherCard(
    isNightly: Boolean,
    isPreparing: Boolean,
    error: String?,
    onSwitchToNightly: () -> Unit,
    onSwitchToStable: () -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = settingsCardContainerColor(isFrosted)),
        border = settingsCardBorder(isFrosted) ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isNightly) Color(0xFFFFB74D).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.secondaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(if (isNightly) R.drawable.deployed_code_update else R.drawable.cloud_download),
                        contentDescription = null,
                        tint = if (isNightly) Color(0xFFFFB74D) else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isNightly) "Nightly Channel Active" else "Switch to Nightly Builds",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isNightly)
                            "You are currently receiving bleeding-edge updates."
                        else
                            "Experience latest features and fixes as soon as committed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!error.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isNightly) {
                    OutlinedButton(
                        onClick = onSwitchToStable,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp, max = 36.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ui_text_get_stable),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSwitchToNightly,
                        enabled = !isPreparing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB74D),
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp, max = 36.dp)
                    ) {
                        if (isPreparing) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.Black)
                        } else {
                            Text(
                                text = stringResource(R.string.ui_sweep_re_download_nightly),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onSwitchToNightly,
                        enabled = !isPreparing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 32.dp, max = 36.dp)
                    ) {
                        if (isPreparing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.ui_sweep_fetching),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.ui_text_switch_to_nightly),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopEcosystemSection(
    onOpenUrl: (String) -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeaderRow(
            title = stringResource(R.string.ui_heading_airbeats_for_desktop),
            icon = painterResource(R.drawable.desktop_windows),
            actionText = stringResource(R.string.ui_extra_desktop_repo),
            onActionClick = { onOpenUrl("https://github.com/d0x-dev/airbeats-desktop") }
        )

        // Desktop Repository Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = settingsCardContainerColor(isFrosted)),
            border = settingsCardBorder(isFrosted) ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.ui_text_airbeats_desktop_repository),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.ui_sweep_official_cross_platform_desktop_application_with_synchronized_pla),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Three Platform Cards
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Windows Card
                    PlatformCard(
                        icon = painterResource(R.drawable.ic_platform_windows),
                        title = stringResource(R.string.ui_heading_airbeats_for_windows),
                        subtitle = stringResource(R.string.ui_heading_windows_10_11_64_bit_exe_portable),
                        actionText = stringResource(R.string.ui_extra_get_windows),
                        onClick = { onOpenUrl("https://github.com/d0x-dev/airbeats-desktop/releases") }
                    )

                    // Mac Card
                    PlatformCard(
                        icon = painterResource(R.drawable.ic_platform_mac),
                        title = stringResource(R.string.ui_heading_airbeats_for_mac),
                        subtitle = stringResource(R.string.ui_heading_macos_apple_silicon_intel_dmg),
                        actionText = stringResource(R.string.ui_extra_get_macos),
                        onClick = { onOpenUrl("https://github.com/d0x-dev/airbeats-desktop/releases") }
                    )

                    // Linux Card
                    PlatformCard(
                        icon = painterResource(R.drawable.ic_platform_linux),
                        title = stringResource(R.string.ui_heading_airbeats_for_linux),
                        subtitle = stringResource(R.string.ui_heading_snap_store_appimage_debian_ubuntu_deb),
                        actionText = stringResource(R.string.ui_extra_snap_store),
                        onClick = { onOpenUrl("https://snapcraft.io/airbeats") }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformCard(
    icon: Painter,
    title: String,
    subtitle: String,
    actionText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = title,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun AppReleaseCard(
    release: GitHubReleaseItem,
    onDownloadApk: (String) -> Unit,
    onOpenGitHub: (String) -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()
    var isExpanded by remember { mutableStateOf(false) }

    val apkAsset = remember(release.assets) {
        release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = settingsCardContainerColor(isFrosted)),
        border = settingsCardBorder(isFrosted) ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = release.tag_name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (release.prerelease) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFB74D).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = stringResource(R.string.ui_text_nightly_2),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = Color(0xFFFFB74D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = formatIsoDate(release.published_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!release.name.isNullOrBlank() && release.name != release.tag_name) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = release.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Expandable Markdown changelog
            if (!release.body.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        MarkdownText(
                            markdown = release.body,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Text(
                    text = if (isExpanded) "Hide Changelog ▲" else "View Changelog ▼",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { onOpenGitHub(release.html_url) },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 30.dp, max = 34.dp)
                ) {
                    Text(stringResource(R.string.ui_text_github), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                }

                if (apkAsset != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onDownloadApk(apkAsset.browser_download_url) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 30.dp, max = 34.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.ui_text_download_apk),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCommitCard(
    commit: GitHubCommitSummary,
    onClick: () -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val shortSha = commit.sha.take(7)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = settingsCardContainerColor(isFrosted)),
        border = settingsCardBorder(isFrosted) ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Text(
                    text = shortSha,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commit.commit.message.lines().firstOrNull() ?: "Commit",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = commit.author?.login ?: commit.commit.author.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatIsoDate(commit.commit.author.date),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.open_in_new),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun CommunityHubCard(
    onOpenUrl: (String) -> Unit
) {
    val isFrosted = isFrostedGlassUiEnabled()
    val telegramUrl = RemoteConfigManager.telegramUrl.ifBlank { RemoteConfigManager.DEFAULT_TELEGRAM_URL }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = settingsCardContainerColor(isFrosted)),
        border = settingsCardBorder(isFrosted) ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CommunityLinkItem(
                icon = painterResource(R.drawable.github),
                title = stringResource(R.string.ui_heading_github_repository),
                subtitle = stringResource(R.string.ui_heading_browse_source_code_branches_and_pull_requests),
                onClick = { onOpenUrl("https://github.com/d0x-dev/AirBeats") }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            CommunityLinkItem(
                icon = painterResource(R.drawable.star),
                title = stringResource(R.string.ui_heading_give_a_star_on_github),
                subtitle = stringResource(R.string.ui_heading_show_your_support_and_help_airbeats_grow),
                onClick = { onOpenUrl("https://github.com/d0x-dev/AirBeats") }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            CommunityLinkItem(
                icon = painterResource(R.drawable.bug_report),
                title = stringResource(R.string.ui_heading_raise_an_issue),
                subtitle = stringResource(R.string.ui_heading_report_unexpected_bugs_or_request_new_feature),
                onClick = { onOpenUrl("https://github.com/d0x-dev/AirBeats/issues/new") }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            CommunityLinkItem(
                icon = painterResource(R.drawable.telegram),
                title = stringResource(R.string.ui_heading_telegram_community),
                subtitle = stringResource(R.string.ui_heading_join_real_time_discussion_channels_and_tester),
                onClick = { onOpenUrl(telegramUrl) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            CommunityLinkItem(
                icon = painterResource(R.drawable.globe_search),
                title = stringResource(R.string.ui_heading_community_airbeats_org),
                subtitle = stringResource(R.string.ui_heading_official_web_community_forum_documentation),
                onClick = { onOpenUrl("https://community.airbeats.org") }
            )
        }
    }
}

@Composable
private fun CommunityLinkItem(
    icon: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    icon: Painter,
    actionText: String?,
    onActionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!actionText.isNullOrBlank() && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun SectionLoadingCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = stringResource(R.string.ui_text_unable_to_load_data),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 30.dp, max = 34.dp)
            ) {
                Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }
        }
    }
}

private fun formatIsoDate(isoDate: String?): String {
    if (isoDate.isNullOrBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val date = parser.parse(isoDate) ?: return isoDate.take(10)
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        try {
            val parser2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val date = parser2.parse(isoDate) ?: return isoDate.take(10)
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formatter.format(date)
        } catch (e2: Exception) {
            isoDate.take(10)
        }
    }
}
