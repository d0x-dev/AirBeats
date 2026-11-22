package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.AccountChannelHandleKey
import com.darkxvenom.airbeats.constants.AccountEmailKey
import com.darkxvenom.airbeats.constants.AccountNameKey
import com.darkxvenom.airbeats.constants.InnerTubeCookieKey
import com.darkxvenom.airbeats.constants.VisitorDataKey
import com.darkxvenom.airbeats.innertube.YouTube
import com.darkxvenom.airbeats.innertube.utils.parseCookieString
import com.darkxvenom.airbeats.ui.component.IconButton
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.utils.rememberPreference
import com.darkxvenom.airbeats.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val YOUTUBE_MUSIC_URL = "https://music.youtube.com/"
private const val MAX_RETRY_ATTEMPTS = 3
private const val RETRY_DELAY_MS = 1000L

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeLoginScreen(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    var visitorData by rememberPreference(VisitorDataKey, "")
    var innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    var accountName by rememberPreference(AccountNameKey, "")
    var accountEmail by rememberPreference(AccountEmailKey, "")
    var accountChannelHandle by rememberPreference(AccountChannelHandleKey, "")

    val context = androidx.compose.ui.platform.LocalContext.current

    var webView: WebView? = null
    var isLoadingAccountInfo by remember { mutableStateOf(false) }
    var isAccountHandled by remember { mutableStateOf(false) }

    suspend fun fetchAccountInfoWithRetry(retryCount: Int = 0) {
        if (isAccountHandled) return
        try {
            val result = withContext(Dispatchers.IO) {
                YouTube.accountInfo()
            }
            result.onSuccess { accountInfo ->
                val name = accountInfo.name.takeIf { it.isNotBlank() } ?: ""
                val email = accountInfo.email?.takeIf { it.isNotBlank() } ?: ""
                val handle = accountInfo.channelHandle?.takeIf { it.isNotBlank() } ?: ""

                withContext(Dispatchers.Main) {
                    if (isAccountHandled) return@withContext
                    if (name.isNotEmpty()) {
                        isAccountHandled = true
                        accountName = name
                        accountEmail = email
                        accountChannelHandle = handle
                        isLoadingAccountInfo = false

                        // Automatically sync profile and trigger global stats upload
                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                            val namePrefManager = com.darkxvenom.airbeats.ui.component.NamePreferenceManager(context)
                            if (email.isNotBlank()) {
                                namePrefManager.saveAccountEmail(email)
                                namePrefManager.rememberGoogleLoginEmail(email)
                            }
                            if (name.isNotBlank()) {
                                namePrefManager.saveUserName(name)
                            }
                            com.darkxvenom.airbeats.utils.AirBeatsStatsCloudSync.syncDaily(
                                context,
                                com.darkxvenom.airbeats.db.InternalDatabase.newInstance(context),
                                namePrefManager
                            )
                        }

                        navController.backToMain()
                    } else {
                        if (retryCount < MAX_RETRY_ATTEMPTS) {
                            delay(RETRY_DELAY_MS)
                            fetchAccountInfoWithRetry(retryCount + 1)
                        } else {
                            isAccountHandled = true
                            isLoadingAccountInfo = false
                            navController.backToMain()
                        }
                    }
                }
            }.onFailure { exception ->
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    delay(RETRY_DELAY_MS)
                    fetchAccountInfoWithRetry(retryCount + 1)
                } else {
                    reportException(exception)
                    withContext(Dispatchers.Main) {
                        if (!isAccountHandled) {
                            isAccountHandled = true
                            isLoadingAccountInfo = false
                            navController.backToMain()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            reportException(e)
            withContext(Dispatchers.Main) {
                if (!isAccountHandled) {
                    isAccountHandled = true
                    isLoadingAccountInfo = false
                    navController.backToMain()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isLoadingAccountInfo) {
                            stringResource(R.string.login) + " - Loading..."
                        } else {
                            stringResource(R.string.login)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            if (url != null && url.startsWith(YOUTUBE_MUSIC_URL)) {
                                runCatching { CookieManager.getInstance().flush() }
                                val youTubeCookieString = CookieManager.getInstance().getCookie(url)
                                val parsedCookies = parseCookieString(youTubeCookieString)

                                if ("SAPISID" in parsedCookies) {
                                    innerTubeCookie = youTubeCookieString
                                    isLoadingAccountInfo = true

                                    coroutineScope.launch(Dispatchers.Main) {
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
                        domStorageEnabled = true
                        // Google rejects embedded WebViews ("; wv" marker) -> strip it like iTune does.
                        userAgentString = userAgentString.replace("; wv", "")
                        setSupportZoom(true)
                        builtInZoomControls = true
                    }
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onRetrieveVisitorData(newVisitorData: String?) {
                                if (innerTubeCookie.isEmpty()) {
                                    visitorData = ""
                                    return
                                }
                                if (!newVisitorData.isNullOrBlank()) {
                                    visitorData = newVisitorData
                                }
                            }
                        },
                        "Android",
                    )
                    webView = this
                    loadUrl(
                        "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&prompt=select_account&continue=$YOUTUBE_MUSIC_URL",
                    )
                }
            },
        )
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
