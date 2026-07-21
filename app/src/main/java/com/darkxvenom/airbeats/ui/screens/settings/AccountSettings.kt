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
import kotlinx.coroutines.delay
import android.widget.Toast
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.first
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
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
    val currentGoogleEmail by nameManager.accountEmail.collectAsState(initial = "")
    
    val backupViewModel: BackupRestoreViewModel = hiltViewModel()
    val avatarManager = remember { AvatarPreferenceManager(context) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var isGoogleSignInOpen by remember { mutableStateOf(false) }

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

    fun generatedAvatarUrl(name: String, email: String): String {
        val seed = name.takeIf { it.isNotBlank() } ?: email
        val encodedSeed = URLEncoder.encode(seed, StandardCharsets.UTF_8.toString())
        return "https://api.dicebear.com/9.x/initials/svg?seed=$encodedSeed&backgroundType=gradientLinear"
    }

    fun displayNameFromEmail(email: String): String {
        return email
            .substringBefore("@")
            .replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase() else char.toString()
                }
            }
            .ifBlank { "Friend" }
    }

    fun linkGoogleAccount(name: String, email: String, photoUrl: String?) {
        scope.launch {
            try {
                if (!nameManager.canUseGoogleEmail(email)) {
                    val lockedEmail = nameManager.previousGoogleEmail.first().ifBlank { "your previous email" }
                    Toast.makeText(context, nameManager.lockedEmailMessage(lockedEmail), Toast.LENGTH_LONG).show()
                    return@launch
                }

                nameManager.saveUserName(name)
                nameManager.rememberGoogleLoginEmail(email)
                if (!photoUrl.isNullOrBlank()) {
                    avatarManager.saveAvatarSelection(
                        AvatarSelection.Custom(uri = photoUrl, cloudUrl = photoUrl)
                    )
                } else {
                    avatarManager.saveAvatarSelection(
                        AvatarSelection.DiceBear(generatedAvatarUrl(name, email))
                    )
                }
                
                val backupClient = com.darkxvenom.airbeats.utils.CloudBackupClient()
                val backupExists = backupClient.checkBackupExists(email)
                
                if (backupExists) {
                    Toast.makeText(context, "Restoring cloud backup...", Toast.LENGTH_SHORT).show()
                    val result = backupViewModel.restoreFromDrive(context, email)
                    if (result is com.darkxvenom.airbeats.utils.DriveResult.Success) {
                        Toast.makeText(context, "Cloud backup restored!", Toast.LENGTH_SHORT).show()
                        
                        delay(1500)
                        context.stopService(android.content.Intent(context, com.darkxvenom.airbeats.playback.MusicService::class.java))
                        context.startActivity(android.content.Intent(context, com.darkxvenom.airbeats.MainActivity::class.java).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        })
                        Runtime.getRuntime().exit(0)
                        return@launch
                    } else {
                        Toast.makeText(context, context.getString(R.string.restore_failed), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.creating_initial_cloud_backup), Toast.LENGTH_SHORT).show()
                    val result = backupViewModel.backupToDrive(context, email, name)
                    if (result is com.darkxvenom.airbeats.utils.DriveResult.Success) {
                        Toast.makeText(context, context.getString(R.string.google_account_linked_backup_created), Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.backup_create_failed_account_linked), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.google_account_linked_cloud_sync_failed,
                        e.message.orEmpty()
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val googleSignInClient = remember {
        com.darkxvenom.airbeats.utils.GoogleAuthManager(context).getSignInClient()
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val email = account.email.orEmpty()
            if (email.isBlank()) {
                Toast.makeText(context, context.getString(R.string.google_email_missing), Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val name = account.displayName
                ?.takeIf { it.isNotBlank() }
                ?: account.givenName
                ?: displayNameFromEmail(email)

            isGoogleSignInOpen = false
            linkGoogleAccount(name, email, account.photoUrl?.toString())
        } catch (e: ApiException) {
            e.printStackTrace()
            val message = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                    context.getString(R.string.google_sign_in_cancelled)
                GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
                    context.getString(R.string.google_sign_in_in_progress)
                GoogleSignInStatusCodes.SIGN_IN_FAILED ->
                    context.getString(R.string.google_sign_in_failed_oauth)
                else -> context.getString(
                    R.string.google_sign_in_failed_with_status,
                    e.statusCode,
                    e.message.orEmpty()
                )
            }
            isGoogleSignInOpen = false
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            isGoogleSignInOpen = false
            Toast.makeText(
                context,
                context.getString(R.string.google_sign_in_failed_message, e.message.orEmpty()),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestGoogleSignIn() {
        if (isGoogleSignInOpen) return
        isGoogleSignInOpen = true
        googleSignInClient.revokeAccess().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🎵 BLUR BACKGROUND
        val artworkUrl = mediaMetadata?.thumbnailUrl

        artworkUrl?.let { imageUrl ->
            com.darkxvenom.airbeats.ui.component.BlurredBackground(
                model = imageUrl
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

                        // 🔹 GOOGLE CLOUD ACCOUNT
                        {
                            PreferenceEntry(
                                title = {
                                    Text(
                                        if (currentGoogleEmail.isNotBlank()) currentGoogleEmail 
                                        else stringResource(R.string.login_with_google)
                                    )
                                },
                                description = if (currentGoogleEmail.isNotBlank()) {
                                    stringResource(R.string.cloud_backup_stats_linked)
                                } else {
                                    stringResource(R.string.link_account_for_cloud_backups)
                                },
                                icon = { Icon(painterResource(R.drawable.google), null, tint = Color.Unspecified) },
                                trailingContent = {
                                    if (currentGoogleEmail.isNotBlank()) {
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                nameManager.saveAccountEmail("")
                                                Toast.makeText(context, context.getString(R.string.google_account_unlinked), Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Text(stringResource(R.string.logout))
                                        }
                                    }
                                },
                                onClick = {
                                    if (currentGoogleEmail.isBlank()) {
                                        requestGoogleSignIn()
                                    }
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
                        label = { Text(stringResource(R.string.inner_tube_cookie)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = visitorDataValue,
                        onValueChange = { visitorDataValue = it },
                        label = { Text(stringResource(R.string.visitor_data)) },
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
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTokenEditor = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
