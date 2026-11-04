package com.darkxvenom.airbeats.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.data.local.LastFmSession
import com.darkxvenom.airbeats.data.local.LastFmSessionPreferences
import com.darkxvenom.airbeats.data.local.ScrobblerPreferences
import com.darkxvenom.airbeats.data.local.ScrobblerSettings
import com.darkxvenom.airbeats.data.repository.LastFmAuthCallbackCoordinator
import com.darkxvenom.airbeats.data.repository.LastFmAuthRepository
import com.darkxvenom.airbeats.data.repository.LastFmAuthState
import com.darkxvenom.airbeats.ui.component.PreferenceEntry
import com.darkxvenom.airbeats.ui.component.SettingsGeneralCategory
import com.darkxvenom.airbeats.ui.component.SettingsPage
import com.darkxvenom.airbeats.ui.component.SwitchPreference
import com.darkxvenom.airbeats.ui.component.isFrostedGlassUiEnabled
import com.darkxvenom.airbeats.ui.component.settingsCardBorder
import com.darkxvenom.airbeats.ui.component.settingsCardContainerColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ScrobblerSettingsViewModel @Inject constructor(
    private val scrobblerPreferences: ScrobblerPreferences,
    private val authRepository: LastFmAuthRepository,
    private val sessionPreferences: LastFmSessionPreferences,
    private val authCallbackCoordinator: LastFmAuthCallbackCoordinator,
) : ViewModel() {
    val settings: StateFlow<ScrobblerSettings> = scrobblerPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScrobblerSettings())

    val authState: StateFlow<LastFmAuthState> = authRepository.authState
    val session: StateFlow<LastFmSession> = sessionPreferences.session

    private val _showSessionKeyDialog = MutableStateFlow(false)
    val showSessionKeyDialog: StateFlow<Boolean> = _showSessionKeyDialog.asStateFlow()

    private val _sessionKeyLoading = MutableStateFlow(false)
    val sessionKeyLoading: StateFlow<Boolean> = _sessionKeyLoading.asStateFlow()

    private val _sessionKeyError = MutableStateFlow<String?>(null)
    val sessionKeyError: StateFlow<String?> = _sessionKeyError.asStateFlow()

    private val _showDirectSignInDialog = MutableStateFlow(false)
    val showDirectSignInDialog: StateFlow<Boolean> = _showDirectSignInDialog.asStateFlow()

    private val _directSignInLoading = MutableStateFlow(false)
    val directSignInLoading: StateFlow<Boolean> = _directSignInLoading.asStateFlow()

    private val _directSignInError = MutableStateFlow<String?>(null)
    val directSignInError: StateFlow<String?> = _directSignInError.asStateFlow()

    private val _showCredentialsDialog = MutableStateFlow(false)
    val showCredentialsDialog: StateFlow<Boolean> = _showCredentialsDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            authCallbackCoordinator.pendingToken.collect { token ->
                token ?: return@collect
                completeWebAuth(token)
            }
        }
    }

    fun completeWebAuth(token: String) {
        viewModelScope.launch {
            val res = authRepository.completeWebAuth(token)
            authCallbackCoordinator.consume(token)
            if (res.isSuccess) {
                _toastMessage.value = "Connected as ${res.getOrNull()}"
                scrobblerPreferences.setEnabled(true)
            } else {
                _toastMessage.value = res.exceptionOrNull()?.message ?: "Last.fm authentication failed"
            }
        }
    }

    private val _pendingToken = MutableStateFlow<String?>(null)
    val pendingToken: StateFlow<String?> = _pendingToken.asStateFlow()

    private val _showAuthConfirmDialog = MutableStateFlow(false)
    val showAuthConfirmDialog: StateFlow<Boolean> = _showAuthConfirmDialog.asStateFlow()

    private val _webAuthLoading = MutableStateFlow(false)
    val webAuthLoading: StateFlow<Boolean> = _webAuthLoading.asStateFlow()

    fun beginWebAuth(context: Context) {
        viewModelScope.launch {
            _webAuthLoading.value = true
            val tokenRes = authRepository.obtainRequestToken()
            _webAuthLoading.value = false
            if (tokenRes.isSuccess) {
                val token = tokenRes.getOrThrow()
                _pendingToken.value = token
                _showAuthConfirmDialog.value = true
                val url = authRepository.authUrl(token)
                if (url != null) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }.onFailure {
                        _toastMessage.value = "No browser found to open authentication link"
                    }
                }
            } else {
                _toastMessage.value = tokenRes.exceptionOrNull()?.message ?: "Failed to get Last.fm request token"
            }
        }
    }

    fun confirmWebAuth() {
        val token = _pendingToken.value
        if (token.isNullOrBlank()) {
            _toastMessage.value = "No pending authentication token"
            return
        }
        viewModelScope.launch {
            _webAuthLoading.value = true
            val res = authRepository.completeWebAuth(token)
            _webAuthLoading.value = false
            if (res.isSuccess) {
                _showAuthConfirmDialog.value = false
                _pendingToken.value = null
                _toastMessage.value = "Connected as ${res.getOrNull()}"
                scrobblerPreferences.setEnabled(true)
            } else {
                _toastMessage.value = res.exceptionOrNull()?.message ?: "Authorization not completed on Last.fm. Please tap Allow in your browser."
            }
        }
    }

    fun dismissAuthConfirmDialog() {
        _showAuthConfirmDialog.value = false
    }

    fun openSessionKeyDialog() {
        _sessionKeyError.value = null
        _showSessionKeyDialog.value = true
    }

    fun dismissSessionKeyDialog() {
        _showSessionKeyDialog.value = false
        _sessionKeyError.value = null
    }

    fun submitPasswordForSessionKey(password: String) {
        if (password.isBlank()) {
            _sessionKeyError.value = "Please enter your password"
            return
        }
        _sessionKeyLoading.value = true
        _sessionKeyError.value = null
        viewModelScope.launch {
            when (val res = authRepository.obtainSessionKey(password)) {
                is LastFmAuthRepository.SessionKeyResult.Success -> {
                    _sessionKeyLoading.value = false
                    _showSessionKeyDialog.value = false
                    _toastMessage.value = "Scrobbling authorized"
                    scrobblerPreferences.setEnabled(true)
                }
                is LastFmAuthRepository.SessionKeyResult.Failed -> {
                    _sessionKeyLoading.value = false
                    _sessionKeyError.value = res.message
                }
            }
        }
    }

    fun openDirectSignInDialog() {
        _directSignInError.value = null
        _showDirectSignInDialog.value = true
    }

    fun dismissDirectSignInDialog() {
        _showDirectSignInDialog.value = false
        _directSignInError.value = null
    }

    fun submitDirectSignIn(username: String, password: String) {
        if (username.isBlank()) {
            _directSignInError.value = "Please enter your Last.fm username"
            return
        }
        _directSignInLoading.value = true
        _directSignInError.value = null
        viewModelScope.launch {
            val directRes = authRepository.signInDirect(username)
            if (directRes.isSuccess) {
                if (password.isNotBlank()) {
                    when (val keyRes = authRepository.obtainSessionKey(password)) {
                        is LastFmAuthRepository.SessionKeyResult.Success -> {
                            _directSignInLoading.value = false
                            _showDirectSignInDialog.value = false
                            _toastMessage.value = "Connected as $username"
                            scrobblerPreferences.setEnabled(true)
                        }
                        is LastFmAuthRepository.SessionKeyResult.Failed -> {
                            _directSignInLoading.value = false
                            _directSignInError.value = keyRes.message
                        }
                    }
                } else {
                    _directSignInLoading.value = false
                    _showDirectSignInDialog.value = false
                    _toastMessage.value = "Connected as $username"
                }
            } else {
                _directSignInLoading.value = false
                _directSignInError.value = directRes.exceptionOrNull()?.message ?: "Sign-in failed"
            }
        }
    }

    fun openCredentialsDialog() {
        _showCredentialsDialog.value = true
    }

    fun dismissCredentialsDialog() {
        _showCredentialsDialog.value = false
    }

    fun saveApiCredentials(apiKey: String, apiSecret: String) {
        viewModelScope.launch {
            authRepository.saveApiCredentials(apiKey, apiSecret)
            _showCredentialsDialog.value = false
            _toastMessage.value = "API credentials updated"
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            scrobblerPreferences.setEnabled(false)
            _toastMessage.value = "Signed out of Last.fm"
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            val current = session.value
            if (!current.isAuthenticated) {
                _showDirectSignInDialog.value = true
                return
            }
            if (!current.hasSessionKey) {
                _showSessionKeyDialog.value = true
                return
            }
        }
        viewModelScope.launch { scrobblerPreferences.setEnabled(enabled) }
    }

    fun setSubmitNowPlaying(enabled: Boolean) {
        viewModelScope.launch { scrobblerPreferences.setSubmitNowPlaying(enabled) }
    }

    fun setScrobblePercent(percent: Int) {
        viewModelScope.launch { scrobblerPreferences.setScrobblePercent(percent) }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}

private fun isNotificationAccessGranted(context: Context): Boolean {
    val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
    return context.packageName in enabledListeners
}

private fun openNotificationAccessSettings(context: Context) {
    runCatching {
        context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrobblerSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ScrobblerSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val session by viewModel.session.collectAsState()
    val showSessionKeyDialog by viewModel.showSessionKeyDialog.collectAsState()
    val sessionKeyLoading by viewModel.sessionKeyLoading.collectAsState()
    val sessionKeyError by viewModel.sessionKeyError.collectAsState()
    val showDirectSignInDialog by viewModel.showDirectSignInDialog.collectAsState()
    val directSignInLoading by viewModel.directSignInLoading.collectAsState()
    val directSignInError by viewModel.directSignInError.collectAsState()
    val showCredentialsDialog by viewModel.showCredentialsDialog.collectAsState()
    val showAuthConfirmDialog by viewModel.showAuthConfirmDialog.collectAsState()
    val webAuthLoading by viewModel.webAuthLoading.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    var showDisconnectConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    SettingsPage(
        title = stringResource(R.string.ui_text_scrobbler),
        navController = navController,
        scrollBehavior = scrollBehavior,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Last.fm Account Card ──
        LastFmAccountCard(
            session = session,
            onConnectWeb = { viewModel.beginWebAuth(context) },
            onDirectSignIn = viewModel::openDirectSignInDialog,
            onAuthorizeScrobbling = viewModel::openSessionKeyDialog,
            onDisconnect = { showDisconnectConfirmDialog = true },
            onEditCredentials = viewModel::openCredentialsDialog,
        )

        // ── Scrobbler Configuration Category ──
        SettingsGeneralCategory(
            title = stringResource(R.string.ui_text_scrobbler),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.ui_text_scrobble)) },
                        description = if (settings.enabled) {
                            if (!isNotificationAccessGranted(context)) {
                                "Permission needed: tap to grant Notification Listener access"
                            } else {
                                "Watching ${settings.selectedPackages.size} app(s)"
                            }
                        } else {
                            "Detect and submit plays to Last.fm"
                        },
                        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                        checked = settings.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !isNotificationAccessGranted(context)) {
                                openNotificationAccessSettings(context)
                            }
                            viewModel.setEnabled(enabled)
                        },
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.ui_text_choose_apps)) },
                        description = if (settings.selectedPackages.isEmpty()) "None selected yet" else "${settings.selectedPackages.size} app(s) selected",
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        onClick = { navController.navigate("settings/scrobbler/apps") },
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.ui_dynamic_submit_now_playing)) },
                        description = stringResource(R.string.ui_extra_show_currently_playing_track_immediately_on_last_fm),
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        checked = settings.submitNowPlaying,
                        onCheckedChange = viewModel::setSubmitNowPlaying,
                    )
                },
                {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.ui_text_scrobble_delay),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${settings.scrobblePercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            text = "Plays must reach ${settings.scrobblePercent}% (max 4 mins) before registering",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                        )
                        Slider(
                            value = settings.scrobblePercent.toFloat(),
                            onValueChange = { viewModel.setScrobblePercent(it.roundToInt()) },
                            valueRange = 25f..90f,
                            steps = 12,
                        )
                    }
                }
            )
        )
    }

    // ── Dialog: Direct Sign-In ──
    if (showDirectSignInDialog) {
        var usernameInput by remember { mutableStateOf(session.username) }
        var passwordInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = viewModel::dismissDirectSignInDialog,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_lastfm),
                    contentDescription = null,
                    tint = Color(0xFFD51007),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(stringResource(R.string.ui_remaining_sign_in_to_last_fm)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter your Last.fm username and password to connect your profile and enable scrobbling.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text(stringResource(R.string.ui_text_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(stringResource(R.string.ui_text_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = directSignInError != null,
                        supportingText = directSignInError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitDirectSignIn(usernameInput, passwordInput) },
                    enabled = !directSignInLoading && usernameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD51007))
                ) {
                    if (directSignInLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.ui_text_connect), color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDirectSignInDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Dialog: Session Key Password ──
    if (showSessionKeyDialog) {
        var passwordInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = viewModel::dismissSessionKeyDialog,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_lastfm),
                    contentDescription = null,
                    tint = Color(0xFFD51007),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(stringResource(R.string.ui_text_enable_scrobbling_2)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Last.fm requires a signed mobile session to record scrobbles. Enter your password once to authorize scrobbling; it is never stored on the device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(stringResource(R.string.ui_remaining_last_fm_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = sessionKeyError != null,
                        supportingText = sessionKeyError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitPasswordForSessionKey(passwordInput) },
                    enabled = !sessionKeyLoading && passwordInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD51007))
                ) {
                    if (sessionKeyLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.ui_text_enable), color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissSessionKeyDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Dialog: Custom API Credentials ──
    if (showCredentialsDialog) {
        var apiKeyInput by remember { mutableStateOf(session.apiKey.ifBlank { LastFmSessionPreferences.DEFAULT_API_KEY }) }
        var apiSecretInput by remember { mutableStateOf(session.apiSecret.ifBlank { LastFmSessionPreferences.DEFAULT_API_SECRET }) }

        AlertDialog(
            onDismissRequest = viewModel::dismissCredentialsDialog,
            title = { Text(stringResource(R.string.ui_remaining_last_fm_api_credentials)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "AirBeats comes with built-in Last.fm API keys. If you prefer to use your own Last.fm API credentials, enter them below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text(stringResource(R.string.ai_api_key)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiSecretInput,
                        onValueChange = { apiSecretInput = it },
                        label = { Text(stringResource(R.string.ui_text_shared_secret)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.saveApiCredentials(apiKeyInput, apiSecretInput) }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCredentialsDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Dialog: Disconnect Confirmation ──
    if (showDisconnectConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmDialog = false },
            title = { Text(stringResource(R.string.ui_remaining_disconnect_last_fm)) },
            text = { Text(stringResource(R.string.ui_remaining_this_will_sign_out_your_account_and_disable_scrobbling_until_you_)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDisconnectConfirmDialog = false
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.ui_text_disconnect))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ── Dialog: Web Auth Confirmation ──
    if (showAuthConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAuthConfirmDialog,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_lastfm),
                    contentDescription = null,
                    tint = Color(0xFFD51007),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(stringResource(R.string.ui_remaining_complete_last_fm_sign_in)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "1. In your browser, tap \"Yes, allow access\" to authorize Airbeats on your Last.fm account.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "2. Return here and tap \"Complete Sign-In\" below.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmWebAuth,
                    enabled = !webAuthLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD51007))
                ) {
                    if (webAuthLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.ui_remaining_complete_sign_in), color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAuthConfirmDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun LastFmAccountCard(
    session: LastFmSession,
    onConnectWeb: () -> Unit,
    onDirectSignIn: () -> Unit,
    onAuthorizeScrobbling: () -> Unit,
    onDisconnect: () -> Unit,
    onEditCredentials: () -> Unit,
) {
    val isFrosted = isFrostedGlassUiEnabled()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFrosted) settingsCardContainerColor(true) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = settingsCardBorder(isFrosted),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFD51007).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lastfm),
                        contentDescription = stringResource(R.string.ui_a11y_last_fm),
                        tint = Color(0xFFD51007),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ui_sweep_last_fm_integration),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    if (session.isAuthenticated) {
                        Text(
                            text = session.username,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.ui_sweep_scrobble_track_your_listening_history),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (session.isAuthenticated) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (session.hasSessionKey) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (session.hasSessionKey) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Text(
                                text = if (session.hasSessionKey) "Active" else "Key Needed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (session.hasSessionKey) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }

            if (session.isAuthenticated) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!session.hasSessionKey) {
                        Button(
                            onClick = onAuthorizeScrobbling,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD51007))
                        ) {
                            Text(stringResource(R.string.ui_text_enable_scrobbling), color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = if (session.hasSessionKey) Modifier.fillMaxWidth() else Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Text(stringResource(R.string.ui_text_disconnect), color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConnectWeb,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD51007))
                    ) {
                        Text(stringResource(R.string.ui_text_log_in_via_web), color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onDirectSignIn,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    ) {
                        Text(stringResource(R.string.ui_remaining_direct_sign_in))
                    }
                }
            }

            // Small bottom row to configure developer credentials
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onEditCredentials),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.ui_text_api_credentials),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.ui_sweep_configure),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
