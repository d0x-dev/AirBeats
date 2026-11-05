package com.darkxvenom.airbeats.ui.screens.settings

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.DiscordTokenKey
import com.darkxvenom.airbeats.ui.component.DevLogInBottomSheet
import com.darkxvenom.airbeats.ui.component.DevLogInType
import com.darkxvenom.airbeats.ui.utils.backToMain
import com.darkxvenom.airbeats.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val JS_SNIPPET =
    "javascript:(function()%7Bvar%20i%3Ddocument.createElement('iframe')%3Bdocument.body.appendChild(i)%3Balert(i.contentWindow.localStorage.token.slice(1,-1))%7D)()"

private const val SAMSUNG_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; SM-S921U; Build/UP1A.231005.007) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.363"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var discordToken by rememberPreference(DiscordTokenKey, "")
    var webView: WebView? = null
    var devLoginSheet by remember { mutableStateOf(false) }
    var isDone by remember { mutableStateOf(false) }

    fun finishLogin(token: String) {
        if (isDone) return
        val cleanToken = token.trim().trim('"', '\'')
        if (cleanToken.isNotBlank() && cleanToken != "null" && cleanToken != "error") {
            isDone = true
            discordToken = cleanToken
            scope.launch(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                webView?.loadUrl("about:blank")
                navController.navigateUp()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.action_login)) },
                navigationIcon = {
                    com.darkxvenom.airbeats.ui.component.IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { devLoginSheet = true }) {
                        Icon(
                            painterResource(R.drawable.codigo),
                            contentDescription = stringResource(R.string.ui_a11y_manual_token_login)
                        )
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
                        settings.userAgentString = SAMSUNG_USER_AGENT

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                if (url.endsWith("/app") || url.contains("/app") || url.contains("/channels/")) {
                                    view.stopLoading()
                                    view.loadUrl(JS_SNIPPET)
                                }
                                return false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val url = request.url.toString()
                                if (url.endsWith("/app") || url.contains("/app") || url.contains("/channels/")) {
                                    view.stopLoading()
                                    view.loadUrl(JS_SNIPPET)
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                if (url.endsWith("/app") || url.contains("/app") || url.contains("/channels/")) {
                                    view.loadUrl(JS_SNIPPET)
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onJsAlert(
                                view: WebView,
                                url: String,
                                message: String,
                                result: JsResult
                            ): Boolean {
                                finishLogin(message)
                                result.confirm()
                                return true
                            }
                        }

                        webView = this
                        loadUrl("https://discord.com/login")
                    }
                }
            )

            if (devLoginSheet) {
                DevLogInBottomSheet(
                    type = DevLogInType.Discord,
                    onDismiss = { devLoginSheet = false },
                    onDone = { pastedToken ->
                        devLoginSheet = false
                        finishLogin(pastedToken)
                    }
                )
            }
        }
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }
}
