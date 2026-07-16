package com.darkxvenom.airbeats.ui.screens.settings

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
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
        // 🎵 BLUR BACKGROUND
        val artworkUrl = mediaMetadata?.thumbnailUrl

        artworkUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(90.dp)
            )

            val isDarkTheme =
                MaterialTheme.colorScheme.background.luminance() < 0.5f

            val overlayBrush = if (isDarkTheme) {
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.5f),
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )
        }

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
                                title = { Text("Edit Display Name") },
                                description = if (currentDisplayName.isNotBlank())
                                    "Current: $currentDisplayName"
                                else
                                    "Not set",
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
                                                ?: stringResource(R.string.login)
                                        } else {
                                            stringResource(R.string.login)
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
                                        navController.navigate("login")
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
            title = { Text("Edit Display Name") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            if (it.text.length <= 9)
                                newName = it
                        },
                        label = { Text("Your name") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Changes will be applied after restarting the app",
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
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNameDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTokenEditor) {
        var cookieValue by remember { mutableStateOf(TextFieldValue(innerTubeCookie)) }
        var visitorDataValue by remember { mutableStateOf(TextFieldValue(visitorData)) }

        AlertDialog(
            onDismissRequest = { showTokenEditor = false },
            icon = { Icon(painterResource(R.drawable.token), null) },
            title = { Text(stringResource(R.string.advanced_login)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = cookieValue,
                        onValueChange = { cookieValue = it },
                        label = { Text("InnerTube Cookie") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = visitorDataValue,
                        onValueChange = { visitorDataValue = it },
                        label = { Text("VisitorData") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onInnerTubeCookieChange(cookieValue.text)
                        onVisitorDataChange(visitorDataValue.text)
                        showTokenEditor = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTokenEditor = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
