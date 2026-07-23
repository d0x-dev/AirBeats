package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AccountChannelHandleKey
import com.darkxvenom.airbeats.constants.AccountEmailKey
import com.darkxvenom.airbeats.constants.AccountNameKey
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.constants.VisitorDataKey
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.utils.reportException
import com.darkxvenom.airbeats.utils.AuthApiClient
import com.darkxvenom.airbeats.utils.AuthResult
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

private const val YOUTUBE_MUSIC_URL = "https://music.youtube.com"
private const val MAX_RETRY_ATTEMPTS = 3
private const val RETRY_DELAY_MS = 1000L

enum class LoginUiState {
    EMAIL_LOGIN, EMAIL_SIGNUP, GOOGLE_WEBVIEW
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun LoginScreen(navController: NavController) {
    var visitorData by rememberPreference(VisitorDataKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    val context = LocalContext.current
    val backupViewModel: com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val authClient = remember { AuthApiClient() }

    var uiState by remember { mutableStateOf(LoginUiState.EMAIL_LOGIN) }
    var isLoadingAccountInfo by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (innerTubeCookie.isNotBlank()) {
            Toast.makeText(context, "You are already logged in with Google. Please clear data or reinstall the app to use Email login.", Toast.LENGTH_LONG).show()
            navController.backToMain()
        }
    }

    suspend fun fetchAccountInfoWithRetry(retryCount: Int = 0) {
        try {
            YouTube.accountInfo().onSuccess { accountInfo ->
                val name = accountInfo.name.takeIf { it.isNotBlank() } ?: ""
                val email = accountInfo.email?.takeIf { it.isNotBlank() } ?: ""
                val handle = accountInfo.channelHandle?.takeIf { it.isNotBlank() } ?: ""

                if (name.isNotEmpty()) {
                    accountName = name
                    accountEmail = email
                    accountChannelHandle = handle
                    Timber.tag("WebView").d("Account info retrieved successfully: $name, $email, $handle")
                    isLoadingAccountInfo = false

                    if (email.isNotBlank()) {
                        withContext(Dispatchers.IO) {
                            val backupClient = com.darkxvenom.airbeats.utils.CloudBackupClient()
                            if (backupClient.checkBackupExists(email)) {
                                val result = backupViewModel.restoreFromDrive(context, email)
                                if (result is com.darkxvenom.airbeats.utils.DriveResult.Success) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Cloud backup restored successfully", Toast.LENGTH_SHORT).show()
                                        context.stopService(android.content.Intent(context, com.darkxvenom.airbeats.playback.MusicService::class.java))
                                        context.filesDir.resolve(com.darkxvenom.airbeats.playback.MusicService.PERSISTENT_QUEUE_FILE).delete()
                                        context.startActivity(android.content.Intent(context, com.darkxvenom.airbeats.MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
                                        kotlin.system.exitProcess(0)
                                    }
                                }
                            } else {
                                backupViewModel.backupToDrive(context, email)
                                withContext(Dispatchers.Main) {
                                    navController.backToMain()
                                }
                            }
                        }
                    } else {
                        navController.backToMain()
                    }
                } else {
                    if (retryCount < MAX_RETRY_ATTEMPTS) {
                        Timber.tag("WebView").w("Account name is empty, retrying... Attempt ${retryCount + 1}")
                        delay(RETRY_DELAY_MS)
                        fetchAccountInfoWithRetry(retryCount + 1)
                    } else {
                        isLoadingAccountInfo = false
                        navController.backToMain()
                    }
                }
            }.onFailure { exception ->
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    delay(RETRY_DELAY_MS)
                    fetchAccountInfoWithRetry(retryCount + 1)
                } else {
                    reportException(exception)
                    isLoadingAccountInfo = false
                }
            }
        } catch (e: Exception) {
            reportException(e)
            isLoadingAccountInfo = false
        }
    }

    if (uiState == LoginUiState.GOOGLE_WEBVIEW) {
        var webView: WebView? = null
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(if (isLoadingAccountInfo) stringResource(R.string.login) + " - Loading..." else stringResource(R.string.login)) },
                navigationIcon = {
                    IconButton(onClick = { uiState = LoginUiState.EMAIL_LOGIN }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
            AndroidView(
                modifier = Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                    .fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                if (url != null && url.startsWith(YOUTUBE_MUSIC_URL)) {
                                    val youTubeCookieString = CookieManager.getInstance().getCookie(url)
                                    val parsedCookies = parseCookieString(youTubeCookieString)
                                    if ("SAPISID" in parsedCookies) {
                                        innerTubeCookie = youTubeCookieString
                                        isLoadingAccountInfo = true
                                        GlobalScope.launch {
                                            delay(500)
                                            fetchAccountInfoWithRetry()
                                        }
                                        loadUrl("javascript:Android.onRetrieveVisitorData(window.yt.config_.VISITOR_DATA)")
                                    } else {
                                        innerTubeCookie = ""
                                    }
                                }
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                        }
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onRetrieveVisitorData(newVisitorData: String?) {
                                    if (innerTubeCookie.isNotEmpty() && !newVisitorData.isNullOrBlank()) {
                                        visitorData = newVisitorData
                                    }
                                }
                            },
                            "Android"
                        )
                        webView = this
                        loadUrl("https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=$YOUTUBE_MUSIC_URL")
                    }
                }
            )
        }
        BackHandler(enabled = webView?.canGoBack() == true) {
            webView?.goBack()
        }
        return
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    fun processEmailAuth(isSignup: Boolean) {
        if (email.isBlank() || password.isBlank() || (isSignup && name.isBlank())) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }
        isProcessing = true
        scope.launch {
            val result = if (isSignup) {
                authClient.signup(name, email, password)
            } else {
                authClient.login(email, password)
            }
            when (result) {
                is AuthResult.Success -> {
                    accountEmail = result.user.email
                    accountName = result.user.name
                    withContext(Dispatchers.IO) {
                        val backupClient = com.darkxvenom.airbeats.utils.CloudBackupClient()
                        if (isSignup || !backupClient.checkBackupExists(result.user.email)) {
                            backupViewModel.backupToDrive(context, result.user.email)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Account created and backup saved!", Toast.LENGTH_SHORT).show()
                                navController.backToMain()
                            }
                        } else {
                            val driveResult = backupViewModel.restoreFromDrive(context, result.user.email)
                            if (driveResult is com.darkxvenom.airbeats.utils.DriveResult.Success) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Cloud backup restored successfully", Toast.LENGTH_SHORT).show()
                                    context.stopService(android.content.Intent(context, com.darkxvenom.airbeats.playback.MusicService::class.java))
                                    context.filesDir.resolve(com.darkxvenom.airbeats.playback.MusicService.PERSISTENT_QUEUE_FILE).delete()
                                    context.startActivity(android.content.Intent(context, com.darkxvenom.airbeats.MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
                                    kotlin.system.exitProcess(0)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Failed to restore backup", Toast.LENGTH_SHORT).show()
                                    navController.backToMain()
                                }
                            }
                        }
                    }
                }
                is AuthResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    isProcessing = false
                }
            }
        }
    }

    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            val mediaItem = androidx.media3.common.MediaItem.fromUri("android.resource://${context.packageName}/${R.raw.login_bg_video}")
            setMediaItem(mediaItem)
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }
    val hazeState = remember { dev.chrisbanes.haze.HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video Background
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            }
        )

        // Back Button
        IconButton(
            onClick = {
                if (uiState == LoginUiState.EMAIL_SIGNUP) {
                    uiState = LoginUiState.EMAIL_LOGIN
                } else {
                    navController.navigateUp()
                }
            },
            modifier = Modifier.statusBarsPadding().padding(16.dp).align(Alignment.TopStart)
        ) {
            Icon(painterResource(R.drawable.arrow_back), contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
        }

        // Glassmorphism Card
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        tint = HazeTint(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
                        blurRadius = 30.dp
                    )
                )
                .background(
                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f), 
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
                )
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (uiState == LoginUiState.EMAIL_SIGNUP) "Get Started Free" else "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (uiState == LoginUiState.EMAIL_SIGNUP) "Free Forever. No Credit Card Needed" else "Welcome back we missed you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Input Fields
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                    focusedContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f),
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White,
                    cursorColor = androidx.compose.ui.graphics.Color.White
                )

                if (uiState == LoginUiState.EMAIL_SIGNUP) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text("Your name", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("@yourname", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(painterResource(R.drawable.person), contentDescription = null, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text(if (uiState == LoginUiState.EMAIL_SIGNUP) "Email address" else "Username / Email", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("yourname@gmail.com", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(painterResource(if (uiState == LoginUiState.EMAIL_SIGNUP) R.drawable.email else R.drawable.person), contentDescription = null, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Password", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(painterResource(R.drawable.lock), contentDescription = null, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp)) },
                        trailingIcon = { 
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(painterResource(if (passwordVisible) R.drawable.visibility else R.drawable.visibility_off), contentDescription = null, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                            }
                        },
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }

                Text(
                    text = "Forgot Password?",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End).padding(bottom = 24.dp).clickable { }
                )

                // Gradient Button
                val gradientBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(androidx.compose.ui.graphics.Color(0xFFC084FC), androidx.compose.ui.graphics.Color(0xFFF472B6), androidx.compose.ui.graphics.Color(0xFFFB923C))
                )
                Button(
                    onClick = { processEmailAuth(uiState == LoginUiState.EMAIL_SIGNUP) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(brush = gradientBrush, shape = androidx.compose.foundation.shape.RoundedCornerShape(25.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    contentPadding = PaddingValues(),
                    enabled = !isProcessing
                ) {
                    Text(if (uiState == LoginUiState.EMAIL_SIGNUP) "Sign up" else "Login", color = androidx.compose.ui.graphics.Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Or continue with
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Divider(modifier = Modifier.weight(1f), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                    Text(
                        text = if (uiState == LoginUiState.EMAIL_SIGNUP) " Or sign up with " else " Or continue with ",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Divider(modifier = Modifier.weight(1f), color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social Icons Row
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val socialModifier = Modifier
                        .size(48.dp)
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(12.dp)
                    
                    Box(modifier = socialModifier.clickable { uiState = LoginUiState.GOOGLE_WEBVIEW }, contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.google), contentDescription = "Google", tint = androidx.compose.ui.graphics.Color.Unspecified)
                    }
                    Box(modifier = socialModifier.clickable { }, contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.facebook), contentDescription = "Facebook", tint = androidx.compose.ui.graphics.Color(0xFF1877F2))
                    }
                    Box(modifier = socialModifier.clickable { }, contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.github), contentDescription = "Github", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { navController.backToMain() }) {
                    Text("Continue as Guest", color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                }
                TextButton(onClick = { uiState = if (uiState == LoginUiState.EMAIL_LOGIN) LoginUiState.EMAIL_SIGNUP else LoginUiState.EMAIL_LOGIN }) {
                    Text(if (uiState == LoginUiState.EMAIL_LOGIN) "Create an account" else "Already have an account? Login", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}
