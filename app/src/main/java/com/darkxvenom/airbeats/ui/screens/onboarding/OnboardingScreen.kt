package com.darkxvenom.airbeats.ui.screens.onboarding

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.ui.component.AvatarPreferenceManager
import com.darkxvenom.airbeats.ui.component.AvatarSelection
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.utils.AuthApiClient
import com.darkxvenom.airbeats.utils.AuthResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val PRIMARY_VIDEO_URL = "https://database.airbeats.net/login_bg_video.mp4"
private const val FALLBACK_VIDEO_URL = "https://raw.githubusercontent.com/d0x-dev/AirBeats/main/assets/login_bg_video.mp4"

enum class SyncState {
    IDLE,
    CHECKING,
    RESTORING,
    RESTORED,
    CREATING_BACKUP,
    NEW_USER
}

enum class AuthMode {
    LOGIN, SIGNUP
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val namePrefManager = remember { NamePreferenceManager(context) }
    val avatarPrefManager = remember { AvatarPreferenceManager(context) }
    val backupViewModel: com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val authClient = remember { AuthApiClient() }
    
    var syncState by remember { mutableStateOf(SyncState.IDLE) }
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    var currentUserName by remember { mutableStateOf("") }
    var currentUserEmail by remember { mutableStateOf("") }
    var featureStep by remember { mutableStateOf(0) }
    var isGoogleSignInOpen by remember { mutableStateOf(false) }

    // Input States
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isEmailProcessing by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = authMode == AuthMode.SIGNUP) {
        authMode = AuthMode.LOGIN
    }

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

    fun saveProfileAndSync(name: String, email: String, photoUrl: String? = null, isNewSignup: Boolean = false) {
        coroutineScope.launch {
            if (!namePrefManager.canUseGoogleEmail(email)) {
                val lockedEmail = namePrefManager.previousGoogleEmail.first().ifBlank { "your previous email" }
                isGoogleSignInOpen = false
                syncState = SyncState.IDLE
                isEmailProcessing = false
                Toast.makeText(context, namePrefManager.lockedEmailMessage(lockedEmail), Toast.LENGTH_LONG).show()
                return@launch
            }

            currentUserName = name
            currentUserEmail = email
            namePrefManager.saveUserName(name)
            namePrefManager.rememberGoogleLoginEmail(email)
            if (!photoUrl.isNullOrBlank()) {
                avatarPrefManager.saveAvatarSelection(
                    AvatarSelection.Custom(uri = photoUrl, cloudUrl = photoUrl)
                )
            } else {
                avatarPrefManager.saveAvatarSelection(
                    AvatarSelection.DiceBear(generatedAvatarUrl(name, email))
                )
            }

            if (isNewSignup) {
                // New user sign up -> Skip checking backup, immediately create initial backup!
                syncState = SyncState.CREATING_BACKUP
                backupViewModel.backupToDrive(context, email, name)
                syncState = SyncState.NEW_USER
            } else {
                // Existing user login -> Check if backup exists first
                syncState = SyncState.CHECKING
                val backupClient = com.darkxvenom.airbeats.utils.CloudBackupClient()
                if (backupClient.checkBackupExists(email)) {
                    syncState = SyncState.RESTORING
                    when (backupViewModel.restoreFromDrive(context, email)) {
                        is com.darkxvenom.airbeats.utils.DriveResult.Success -> {
                            syncState = SyncState.RESTORED
                            delay(1500)
                            context.stopService(android.content.Intent(context, com.darkxvenom.airbeats.playback.MusicService::class.java))
                            context.startActivity(
                                android.content.Intent(context, com.darkxvenom.airbeats.MainActivity::class.java).apply {
                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                            )
                            Runtime.getRuntime().exit(0)
                        }
                        else -> {
                            Toast.makeText(context, "Cloud restore failed. Creating a fresh backup.", Toast.LENGTH_SHORT).show()
                            syncState = SyncState.CREATING_BACKUP
                            backupViewModel.backupToDrive(context, email, name)
                            syncState = SyncState.NEW_USER
                        }
                    }
                } else {
                    syncState = SyncState.CREATING_BACKUP
                    backupViewModel.backupToDrive(context, email, name)
                    syncState = SyncState.NEW_USER
                }
            }
        }
    }

    fun handleEmailAuth() {
        if (emailInput.isBlank() || passwordInput.isBlank() || (authMode == AuthMode.SIGNUP && nameInput.isBlank())) {
            Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        isEmailProcessing = true
        coroutineScope.launch {
            val result = if (authMode == AuthMode.SIGNUP) {
                authClient.signup(nameInput, emailInput, passwordInput)
            } else {
                authClient.login(emailInput, passwordInput)
            }
            when (result) {
                is AuthResult.Success -> {
                    val displayName = result.user.name.ifBlank { displayNameFromEmail(result.user.email) }
                    saveProfileAndSync(displayName, result.user.email, photoUrl = null, isNewSignup = (authMode == AuthMode.SIGNUP))
                }
                is AuthResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    isEmailProcessing = false
                }
            }
        }
    }

    fun continueToHome() {
        coroutineScope.launch {
            syncState = SyncState.IDLE
            navController.navigate("home") {
                popUpTo("onboarding") {
                    inclusive = true
                }
            }
        }
    }

    fun showSignInError(message: String) {
        isGoogleSignInOpen = false
        syncState = SyncState.IDLE
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun googleSignInErrorMessage(error: ApiException): String {
        return when (error.statusCode) {
            GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> "Google sign in was cancelled."
            GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Google sign in is already open."
            GoogleSignInStatusCodes.SIGN_IN_FAILED -> "Google sign in failed. Check the Android OAuth client package and SHA-1."
            else -> "Google sign in failed (${error.statusCode}): ${error.message.orEmpty()}"
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
                showSignInError("Google did not return an email for this account.")
                return@rememberLauncherForActivityResult
            }
            val name = account.displayName
                ?.takeIf { it.isNotBlank() }
                ?: account.givenName
                ?: displayNameFromEmail(email)

            isGoogleSignInOpen = false
            saveProfileAndSync(name, email, account.photoUrl?.toString(), isNewSignup = false)
        } catch (e: ApiException) {
            e.printStackTrace()
            showSignInError(googleSignInErrorMessage(e))
        } catch (e: Exception) {
            e.printStackTrace()
            showSignInError("Google sign in failed: ${e.message}")
        }
    }

    val onGoogleSignInClick: () -> Unit = {
        if (!isGoogleSignInOpen) {
            isGoogleSignInOpen = true
            googleSignInClient.revokeAccess().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    // Video Background Setup — Cached or Remote Stream
    val cachedVideoFile = remember { File(context.cacheDir, "login_bg_video.mp4") }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = if (cachedVideoFile.exists() && cachedVideoFile.length() > 0) {
                Uri.fromFile(cachedVideoFile)
            } else {
                Uri.parse("android.resource://${context.packageName}/${R.raw.login_bg_video}")
            }
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(Unit) {
        if (!cachedVideoFile.exists() || cachedVideoFile.length() == 0L) {
            withContext(Dispatchers.IO) {
                try {
                    val url = java.net.URL(PRIMARY_VIDEO_URL)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 15000
                    if (connection.responseCode == 200) {
                        connection.inputStream.use { input ->
                            cachedVideoFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(cachedVideoFile)))
                            exoPlayer.prepare()
                        }
                    } else {
                        val fallbackUrl = java.net.URL(FALLBACK_VIDEO_URL)
                        val fallbackConn = fallbackUrl.openConnection() as java.net.HttpURLConnection
                        fallbackConn.connectTimeout = 8000
                        fallbackConn.readTimeout = 15000
                        if (fallbackConn.responseCode == 200) {
                            fallbackConn.inputStream.use { input ->
                                cachedVideoFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(cachedVideoFile)))
                                exoPlayer.prepare()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error caching background video")
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // True Fullscreen Video Background using TextureView for Haze capture
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState),
            factory = { ctx ->
                android.view.TextureView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    exoPlayer.setVideoTextureView(this)
                }
            }
        )

        // Top Back Button when in Signup mode or when opened from navigation
        IconButton(
            onClick = {
                if (authMode == AuthMode.SIGNUP) {
                    authMode = AuthMode.LOGIN
                } else {
                    if (!navController.popBackStack()) {
                        navController.navigate("home")
                    }
                }
            },
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Liquid Glassmorphism Frosted Card — Lowered for top character spacing
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(if (authMode == AuthMode.SIGNUP) 0.74f else 0.67f)
                .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeMaterials.ultraThin()
                )
                .background(
                    Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                )
        ) {
            val scrollState = rememberScrollState()
            Crossfade(targetState = syncState, label = "OnboardingState") { state ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (state) {
                        SyncState.IDLE -> {
                            Text(
                                text = if (authMode == AuthMode.SIGNUP) "Get Started Free" else "Welcome Back",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (authMode == AuthMode.SIGNUP) "Free Forever. No Credit Card Needed" else "Welcome back we missed you",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val textFieldColors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White.copy(alpha = 0.12f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            )

                            if (authMode == AuthMode.SIGNUP) {
                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                                    Text("Your name", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        placeholder = { Text("@yourname", color = Color.White.copy(alpha = 0.5f)) },
                                        leadingIcon = { Icon(painterResource(R.drawable.person), contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = textFieldColors
                                    )
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                                Text(if (authMode == AuthMode.SIGNUP) "Email address" else "Username / Email", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    placeholder = { Text("yourname@gmail.com", color = Color.White.copy(alpha = 0.5f)) },
                                    leadingIcon = { Icon(painterResource(if (authMode == AuthMode.SIGNUP) R.drawable.email else R.drawable.person), contentDescription = null, tint = Color.White.copy(alpha = 0.5f)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                                Text("Password", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    placeholder = { Text("••••••••", color = Color.White.copy(alpha = 0.5f)) },
                                    leadingIcon = { Icon(painterResource(R.drawable.lock), contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(painterResource(if (passwordVisible) R.drawable.visibility else R.drawable.visibility_off), contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = textFieldColors
                                )
                            }

                            if (authMode == AuthMode.LOGIN) {
                                Text(
                                    text = "Forgot Password?",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.align(Alignment.End).padding(bottom = 14.dp).clickable { }
                                )
                            } else {
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Gradient Action Button
                            val gradientBrush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFC084FC), Color(0xFFF472B6), Color(0xFFFB923C))
                            )
                            Button(
                                onClick = { handleEmailAuth() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(brush = gradientBrush, shape = RoundedCornerShape(24.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(),
                                enabled = !isEmailProcessing
                            ) {
                                if (isEmailProcessing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(if (authMode == AuthMode.SIGNUP) "Sign up" else "Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Or continue with divider
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                                Text(
                                    text = if (authMode == AuthMode.SIGNUP) " Or sign up with " else " Or continue with ",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Social Icons & Google Sign-In Row
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                val socialModifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                                
                                Box(modifier = socialModifier.clickable { onGoogleSignInClick() }, contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.google), contentDescription = "Google", tint = Color.Unspecified)
                                }
                                Box(modifier = socialModifier.clickable { }, contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.facebook), contentDescription = "Facebook", tint = Color(0xFF1877F2))
                                }
                                Box(modifier = socialModifier.clickable { }, contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.github), contentDescription = "Github", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { navController.navigate("guest_profile_setup") }) {
                                    Text("Continue as Guest", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                }
                                TextButton(onClick = { authMode = if (authMode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN }) {
                                    Text(
                                        text = if (authMode == AuthMode.LOGIN) "Sign up" else "Login",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        SyncState.CHECKING -> {
                            CircularProgressIndicator(color = Color(0xFFA259FF), modifier = Modifier.padding(bottom = 24.dp))
                            Text(
                                text = "Looking for your cloud backup...",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(stringResource(R.string.please_wait), color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                        }

                        SyncState.RESTORING -> {
                            CircularProgressIndicator(color = Color(0xFFA259FF), modifier = Modifier.padding(bottom = 24.dp))
                            Text(
                                text = "Restoring your cloud backup...",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(stringResource(R.string.please_wait), color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                        }

                        SyncState.RESTORED -> {
                            Icon(painter = painterResource(R.drawable.check_circle), contentDescription = "Success", tint = Color(0xFFA259FF), modifier = Modifier.size(64.dp).padding(bottom = 16.dp))
                            Text(
                                text = "Hi $currentUserName,",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Welcome again back to Airbeats",
                                color = Color.LightGray,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        SyncState.CREATING_BACKUP -> {
                            CircularProgressIndicator(color = Color(0xFFA259FF), modifier = Modifier.padding(bottom = 24.dp))
                            Text(
                                text = "Creating your cloud backup...",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text(stringResource(R.string.uploading_first_backup), color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                        }

                        SyncState.NEW_USER -> {
                            val featureTitles = listOf("Discover Music", "Create Playlists", "Offline Mode")
                            val featureDesc = listOf("Find millions of songs matching your mood.", "Curate your perfect listening experience.", "Download your favorites and listen anywhere.")
                            
                            Text(
                                text = if (featureStep == 0) "Hi $currentUserName, welcome!" else featureTitles[featureStep - 1],
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = if (featureStep == 0) "Looking like you are new to Airbeats" else featureDesc[featureStep - 1],
                                color = Color.LightGray,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 32.dp)
                            )
                            Button(
                                onClick = {
                                    if (featureStep < featureTitles.size) featureStep++ else continueToHome()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA259FF)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text(if (featureStep == featureTitles.size) "Get Started" else "Next", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
