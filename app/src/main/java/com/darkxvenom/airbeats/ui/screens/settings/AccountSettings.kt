package com.darkxvenom.airbeats.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.App.Companion.forgetAccount
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.*
import com.darkxvenom.airbeats.ui.component.*
import com.darkxvenom.airbeats.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.widget.Toast
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.datastore.preferences.core.edit
import com.darkxvenom.airbeats.utils.dataStore
import kotlinx.coroutines.flow.map
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val nameManager = remember { NamePreferenceManager(context) }
    val currentDisplayName by nameManager.userName.collectAsState(initial = "")
    val avatarManager = remember { AvatarPreferenceManager(context) }
    var showEditNameDialog by remember { mutableStateOf(false) }

    val (accountName, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) =
        rememberPreference(AccountChannelHandleKey, "")
    val (innerTubeCookie, onInnerTubeCookieChange) =
        rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) =
        rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) =
        rememberPreference(DataSyncIdKey, "")

    val isLoggedIn = remember(innerTubeCookie) {
        innerTubeCookie.isNotEmpty() &&
                "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val getAccountDisplayName =
        remember(accountName, accountEmail, accountChannelHandle, isLoggedIn) {
            when {
                !isLoggedIn -> ""
                accountName.isNotBlank() -> accountName
                accountEmail.isNotBlank() -> accountEmail.substringBefore("@")
                accountChannelHandle.isNotBlank() -> accountChannelHandle
                else -> "No username"
            }
        }

    val getAccountDescription =
        remember(accountEmail, accountChannelHandle, isLoggedIn) {
            when {
                !isLoggedIn -> null
                accountEmail.isNotBlank() -> accountEmail
                accountChannelHandle.isNotBlank() -> accountChannelHandle
                else -> null
            }
        }

    val (useLoginForBrowse, onUseLoginForBrowseChange) =
        rememberPreference(UseLoginForBrowse, true)
    val (ytmSync, onYtmSyncChange) =
        rememberPreference(YtmSyncKey, true)

    var showToken by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }

    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf(null) }



    Box(modifier = Modifier.fillMaxSize()) {
        // Adaptive background: blurred song thumbnail when playing, Library mesh when no song playing
        val artworkUrl = mediaMetadata?.thumbnailUrl
        com.darkxvenom.airbeats.ui.component.ScreenAdaptiveBackground(
            artworkUrl = artworkUrl
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(end = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.account),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                bottomStart = 30.dp,
                                bottomEnd = 30.dp
                            )
                        )
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                                )
                            )
                        )
                        .border(
                            width = 0.6.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(
                                bottomStart = 30.dp,
                                bottomEnd = 30.dp
                            )
                        ),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
            ) {
            // Main content with horizontal padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                SettingsGeneralCategory(
                    title = stringResource(R.string.google),
                    items = listOf(

                        // 🔹 EDIT DISPLAY NAME
                        {
                            PreferenceEntry(
                                title = { Text(stringResource(R.string.edit_display_name)) },
                                description = if (currentDisplayName.isNotBlank())
                                    stringResource(R.string.current_value, currentDisplayName)
                                else
                                    stringResource(R.string.not_set),
                                icon = { Icon(painterResource(R.drawable.person), null) },
                                onClick = { showEditNameDialog = true }
                            )
                        },

                        // 🔹 LOGIN / LOGOUT
                        {
                            PreferenceEntry(
                                title = {
                                    Text(
                                        if (isLoggedIn) {
                                            getAccountDisplayName.takeIf { it.isNotBlank() }
                                                ?: "Login to YouTube"
                                        } else {
                                            "Login to YouTube"
                                        }
                                    )
                                },
                                description = if (isLoggedIn) getAccountDescription else null,
                                icon = { Icon(painterResource(R.drawable.login), null) },
                                trailingContent = {
                                    if (isLoggedIn) {
                                        OutlinedButton(onClick = {
                                            onInnerTubeCookieChange("")
                                            onAccountNameChange("")
                                            onAccountEmailChange("")
                                            onAccountChannelHandleChange("")
                                            onVisitorDataChange("")
                                            onDataSyncIdChange("")
                                            forgetAccount(context)
                                        }) {
                                            Text(stringResource(R.string.logout))
                                        }
                                    }
                                },
                                onClick = {
                                    if (!isLoggedIn)
                                        navController.navigate("youtube_login")
                                }
                            )
                        },



                        // 🔹 ADVANCED LOGIN
                        {
                            PreferenceEntry(
                                title = {
                                    Text(
                                        if (!isLoggedIn)
                                            stringResource(R.string.advanced_login)
                                        else if (showToken)
                                            stringResource(R.string.token_shown)
                                        else
                                            stringResource(R.string.token_hidden)
                                    )
                                },
                                icon = { Icon(painterResource(R.drawable.token), null) },
                                onClick = {
                                    if (!isLoggedIn) {
                                        showTokenEditor = true
                                    } else {
                                        if (!showToken)
                                            showToken = true
                                        else
                                            showTokenEditor = true
                                    }
                                }
                            )
                        },

                        // 🔹 USE LOGIN FOR BROWSE
                        {
                            if (isLoggedIn) {
                                SwitchPreference(
                                    title = {
                                        Text(stringResource(R.string.use_login_for_browse))
                                    },
                                    description = stringResource(R.string.use_login_for_browse_desc),
                                    icon = {
                                        Icon(painterResource(R.drawable.person), null)
                                    },
                                    checked = useLoginForBrowse,
                                    onCheckedChange = {
                                        YouTube.useLoginForBrowse = it
                                        onUseLoginForBrowseChange(it)
                                    }
                                )
                            }
                        },

                        // 🔹 YTM SYNC
                        {
                            if (isLoggedIn) {
                                SwitchPreference(
                                    title = { Text(stringResource(R.string.ytm_sync)) },
                                    icon = {
                                        Icon(painterResource(R.drawable.cached), null)
                                    },
                                    checked = ytmSync,
                                    onCheckedChange = onYtmSyncChange
                                )
                            }
                        },
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // Spotify Group
                val hasSpotifyCookie by context.dataStore.data.map { it.contains(com.darkxvenom.airbeats.constants.SpotifyCookieKey) }.collectAsState(initial = false)
                SettingsGeneralCategory(
                    title = "Spotify",
                    items = listOf(
                        {
                            PreferenceEntry(
                                title = { Text(if (hasSpotifyCookie) "Connected" else "Login to Spotify") },
                                description = if (hasSpotifyCookie) "Connected to Spotify account" else "Sign in to see your feed and playlists",
                                icon = { Icon(painterResource(R.drawable.music_note), null) },
                                trailingContent = {
                                    if (hasSpotifyCookie) {
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                context.dataStore.edit { it.remove(com.darkxvenom.airbeats.constants.SpotifyCookieKey) }
                                            }
                                        }) {
                                            Text(stringResource(R.string.logout))
                                        }
                                    }
                                },
                                onClick = {
                                    if (!hasSpotifyCookie) {
                                        navController.navigate("spotify_login")
                                    } else {
                                        navController.navigate("spotify_account")
                                    }
                                }
                            )
                        }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Discord Group
                val discordToken by context.dataStore.data.map { it[DiscordTokenKey] ?: "" }.collectAsState(initial = "")
                val hasDiscordToken = discordToken.isNotEmpty()
                val (discordRPC, onDiscordRPCChange) = rememberPreference(
                    key = EnableDiscordRPCKey,
                    defaultValue = true
                )

                SettingsGeneralCategory(
                    title = stringResource(R.string.discord_integration),
                    items = listOf(
                        {
                            PreferenceEntry(
                                title = { Text(if (hasDiscordToken) "Connected" else "Login to Discord") },
                                description = if (hasDiscordToken) "Connected to Discord account" else "Sign in to enable Rich Presence",
                                icon = { Icon(painterResource(R.drawable.discord), null) },
                                trailingContent = {
                                    if (hasDiscordToken) {
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                context.dataStore.edit { 
                                                    it.remove(DiscordTokenKey) 
                                                    it[EnableDiscordRPCKey] = false
                                                }
                                            }
                                        }) {
                                            Text(stringResource(R.string.logout))
                                        }
                                    }
                                },
                                onClick = {
                                    if (!hasDiscordToken) {
                                        navController.navigate("settings/discord/login")
                                    } else {
                                        navController.navigate("settings/discord")
                                    }
                                }
                            )
                        },
                        {
                            if (hasDiscordToken) {
                                SwitchPreference(
                                    title = { Text(stringResource(R.string.enable_discord_rpc)) },
                                    icon = {
                                        Icon(painterResource(R.drawable.discord), null)
                                    },
                                    checked = discordRPC,
                                    onCheckedChange = onDiscordRPCChange
                                )
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 🔥 AVATAR SELECTOR
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    AvatarSelector(modifier = Modifier.padding(vertical = 8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 🔥 RANK BADGE SELECTOR
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    RankBadgeSelector(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    }

    // 🔥 EDIT NAME DIALOG
    if (showEditNameDialog) {
        var newName by remember {
            mutableStateOf(TextFieldValue(currentDisplayName))
        }

        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(stringResource(R.string.edit_display_name)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            if (it.text.length <= 9)
                                newName = it
                        },
                        label = { Text(stringResource(R.string.your_name)) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.ui_text_changes_will_be_applied_after_restarting_the_app),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            nameManager.saveUserName(newName.text)
                        }
                        showEditNameDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showTokenEditor) {
        AdvancedTokenLoginDialog(
            initialCookie = innerTubeCookie,
            initialVisitorData = visitorData,
            onDismiss = { showTokenEditor = false },
            onSave = { newCookie, newVisitorData ->
                onInnerTubeCookieChange(newCookie)
                onVisitorDataChange(newVisitorData)
                showTokenEditor = false
            }
        )
    }
}

@Composable
fun AdvancedTokenLoginDialog(
    initialCookie: String,
    initialVisitorData: String,
    onDismiss: () -> Unit,
    onSave: (cookie: String, visitorData: String) -> Unit,
) {
    val existingCookies = remember(initialCookie) { parseCookieString(initialCookie) }
    var useRawMode by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    var sapisid by remember { mutableStateOf(existingCookies["SAPISID"].orEmpty()) }
    var hsid by remember { mutableStateOf(existingCookies["HSID"].orEmpty()) }
    var ssid by remember { mutableStateOf(existingCookies["SSID"].orEmpty()) }
    var sid by remember { mutableStateOf(existingCookies["SID"].orEmpty()) }
    var loginInfo by remember { mutableStateOf(existingCookies["LOGIN_INFO"].orEmpty()) }
    var rawCookie by remember { mutableStateOf(initialCookie) }
    var visitorData by remember { mutableStateOf(initialVisitorData) }

    val formattedCookie = remember(sapisid, hsid, ssid, sid, loginInfo, useRawMode, rawCookie) {
        if (useRawMode) {
            rawCookie.trim()
        } else {
            buildList {
                if (sapisid.isNotBlank()) add("SAPISID=${sapisid.trim()}")
                if (hsid.isNotBlank()) add("HSID=${hsid.trim()}")
                if (ssid.isNotBlank()) add("SSID=${ssid.trim()}")
                if (sid.isNotBlank()) add("SID=${sid.trim()}")
                if (loginInfo.isNotBlank()) add("LOGIN_INFO=${loginInfo.trim()}")
            }.joinToString("; ")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.token), contentDescription = null) },
        title = { Text(stringResource(R.string.advanced_login)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // How-to guide accordion
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHelp = !showHelp },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.ui_sweep_how_to_get_your_cookies),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (showHelp) "▲" else "▼",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (showHelp) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.ui_cookie_instruction_step_one) +
                                        "2. Press F12 -> Application tab -> Cookies -> https://music.youtube.com.\n" +
                                        "3. Copy the values of SAPISID, HSID, SSID, SID, and LOGIN_INFO into the fields below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Mode switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { useRawMode = !useRawMode }) {
                        Text(
                            text = if (useRawMode) "Switch to Individual Fields" else "Switch to Raw Cookie",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (!useRawMode) {
                    OutlinedTextField(
                        value = sapisid,
                        onValueChange = { sapisid = it },
                        label = { Text(stringResource(R.string.ui_dynamic_cookie_required, "SAPISID")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hsid,
                        onValueChange = { hsid = it },
                        label = { Text(stringResource(R.string.ui_dynamic_cookie_required, "HSID")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        label = { Text(stringResource(R.string.ui_dynamic_cookie_required, "SSID")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sid,
                        onValueChange = { sid = it },
                        label = { Text(stringResource(R.string.ui_dynamic_cookie_required, "SID")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = loginInfo,
                        onValueChange = { loginInfo = it },
                        label = { Text(stringResource(R.string.ui_dynamic_cookie_required, "LOGIN_INFO")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = rawCookie,
                        onValueChange = { rawCookie = it },
                        label = { Text(stringResource(R.string.inner_tube_cookie)) },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = visitorData,
                    onValueChange = { visitorData = it },
                    label = { Text(stringResource(R.string.visitor_data) + " (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(formattedCookie, visitorData.trim())
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}




