package ru.moneyapp.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ru.moneyapp.R
import ru.moneyapp.data.remote.OAuth2Service

@AndroidEntryPoint
class OAuthWebViewActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var authorizationCode: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get OAuth parameters from intent
        val clientId = intent.getStringExtra("client_id") ?: return finish()
        val redirectUri = intent.getStringExtra("redirect_uri")
            ?: "moneyapp://oauth2-callback"
        val scopes = intent.getStringArrayListExtra("scopes")
            ?: listOf(
                OAuth2Service.SPREADSHEETS_SCOPE,
                OAuth2Service.DRIVE_SCOPE
            )

        // PKCE challenge (required by Google for all clients, incl. installed)
        val codeChallenge = intent.getStringExtra("code_challenge")

        // Build OAuth URL
        val authUrl = buildAuthUrl(clientId, redirectUri, scopes, codeChallenge)

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = OAuthWebViewClient(redirectUri)
            loadUrl(authUrl)
        }

        setContentView(webView)
    }

    private fun buildAuthUrl(
        clientId: String,
        redirectUri: String,
        scopes: List<String>,
        codeChallenge: String?
    ): String {
        val pkce = if (!codeChallenge.isNullOrBlank()) {
            "&code_challenge=${Uri.encode(codeChallenge)}&code_challenge_method=S256"
        } else {
            ""
        }
        return "${OAuth2Service.AUTH_URL}?" +
                "response_type=code&" +
                "client_id=${Uri.encode(clientId)}&" +
                "redirect_uri=${Uri.encode(redirectUri)}&" +
                "scope=${Uri.encode(scopes.joinToString(" "))}&" +
                "access_type=offline&" +
                "prompt=consent" +
                pkce
    }

    private inner class OAuthWebViewClient(private val redirectUri: String) : WebViewClient() {

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: android.webkit.WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString()
            return if (url.startsWith(redirectUri)) {
                // Extract authorization code from redirect URI
                val uri = Uri.parse(url)
                authorizationCode = uri.getQueryParameter("code")
                handleAuthCode()
                true
            } else {
                false
            }
        }

        private fun handleAuthCode() {
            val code = authorizationCode
            if (code != null) {
                // Send auth code back to main activity
                val resultIntent = Intent("moneyapp.OAUTH_RESULT").apply {
                    putExtra("auth_code", code)
                }
                setResult(RESULT_OK, resultIntent)
            } else {
                val error = Uri.parse(webView.url).getQueryParameter("error")
                val resultIntent = Intent("moneyapp.OAUTH_RESULT").apply {
                    putExtra("error", error ?: "Unknown error")
                }
                setResult(RESULT_CANCELED, resultIntent)
            }
            finish()
        }
    }

    // Predictive back (manifest enableOnBackInvokedCallback): the framework
    // bridges back to onBackPressed() when the app registers no
    // OnBackInvokedCallback, so this override stays the single back path.
    @Deprecated("Back is handled through the framework default callback")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
            setResult(RESULT_CANCELED)
        }
    }
}
