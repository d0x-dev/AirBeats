package com.darkxvenom.airbeats.ui.screens

import androidx.compose.ui.res.stringResource

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.SpotifyCookieKey
import com.darkxvenom.airbeats.spotify.SpotifyAuth
import com.darkxvenom.airbeats.ui.component.DevLogInBottomSheet
import com.darkxvenom.airbeats.ui.component.DevLogInType
import com.darkxvenom.airbeats.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpotifyLoginScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var devLoginSheet by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    fun saveSpDcAndFinish(rawCookie: String) {
        if (isFinished) return
        val spDc = if (rawCookie.contains("sp_dc=")) {
            rawCookie.substringAfter("sp_dc=").substringBefore(";").trim()
        } else {
            rawCookie.trim()
        }

        if (spDc.isNotBlank()) {
            isFinished = true
            coroutineScope.launch(Dispatchers.Main) {
                context.dataStore.edit { prefs ->
                    prefs[SpotifyCookieKey] = spDc
                }
                Toast.makeText(context, context.getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
    }

    val statusUrlRegex = remember {
        Regex("^https://accounts\\.spotify\\.com/(?:[^/]+/)?status(?:\\?.*)?$")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_text_spotify_login)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { devLoginSheet = true }) {
                        Icon(painterResource(R.drawable.codigo), contentDescription = stringResource(R.string.ui_a11y_manual_cookie_login))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true

                        // Clean user-agent so Google OAuth / WebPlayer login is not blocked
                        val defaultUserAgent = settings.userAgentString
                        settings.userAgentString = defaultUserAgent.replace("; wv", "")

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        fun inspectCookies(currentUrl: String?) {
                            if (currentUrl == null || isFinished) return
                            val cookies = cookieManager.getCookie(currentUrl)
                                ?: cookieManager.getCookie("https://open.spotify.com")
                                ?: cookieManager.getCookie("https://accounts.spotify.com")
                                ?: ""

                            if (statusUrlRegex.matches(currentUrl) || currentUrl.startsWith("https://open.spotify.com") || cookies.contains("sp_dc=")) {
                                if (cookies.contains("sp_dc=")) {
                                    saveSpDcAndFinish(cookies)
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                inspectCookies(url)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                inspectCookies(url)
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                inspectCookies(url)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString()
                                inspectCookies(url)
                                return false
                            }
                        }

                        loadUrl(SpotifyAuth.LOGIN_URL)
                    }
                }
            )

            if (devLoginSheet) {
                DevLogInBottomSheet(
                    type = DevLogInType.Spotify,
                    onDismiss = { devLoginSheet = false },
                    onDone = { pastedValue ->
                        devLoginSheet = false
                        saveSpDcAndFinish(pastedValue)
                    }
                )
            }
        }
    }
}
