package com.darkxvenom.airbeats.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.SpotifyCookieKey
import com.darkxvenom.airbeats.spotify.SpotifyAuth
import androidx.compose.material3.IconButton
import com.darkxvenom.airbeats.utils.CryptoManager
import com.darkxvenom.airbeats.utils.dataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SpotifyLoginScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spotify Login") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            factory = {
                WebView(it).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    
                    // Remove WebView identifier from User-Agent to prevent Google OAuth from blocking the login
                    val defaultUserAgent = settings.userAgentString
                    settings.userAgentString = defaultUserAgent.replace("; wv", "")
                    
                    // Clear cookies to ensure fresh login
                    CookieManager.getInstance().removeAllCookies(null)
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    CookieManager.getInstance().flush()

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            
                            // Check if we hit the redirect URL
                            if (url?.startsWith("https://open.spotify.com") == true) {
                                val cookies = CookieManager.getInstance().getCookie("https://open.spotify.com")
                                if (cookies != null && cookies.contains("sp_dc=")) {
                                    val spDc = cookies.split(";")
                                        .map { it.trim() }
                                        .firstOrNull { it.startsWith("sp_dc=") }
                                        ?.substringAfter("sp_dc=")
                                    
                                    if (spDc != null) {
                                        coroutineScope.launch {
                                            context.dataStore.edit { prefs ->
                                                prefs[SpotifyCookieKey] = CryptoManager.encrypt(spDc)
                                            }
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    loadUrl(SpotifyAuth.LOGIN_URL)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}
