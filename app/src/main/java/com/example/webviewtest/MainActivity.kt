package com.example.webviewtest

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var urlInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var backButton: Button
    private lateinit var forwardButton: Button

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        urlInput = findViewById(R.id.urlInput)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        backButton = findViewById(R.id.backButton)
        forwardButton = findViewById(R.id.forwardButton)

        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        WebView.setWebContentsDebuggingEnabled(debuggable)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = false
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = TestWebViewClient()
        webView.webChromeClient = TestWebChromeClient()

        findViewById<Button>(R.id.goButton).setOnClickListener { loadEnteredUrl() }
        findViewById<Button>(R.id.reloadButton).setOnClickListener { webView.reload() }
        backButton.setOnClickListener { webView.goBack() }
        forwardButton.setOnClickListener { webView.goForward() }

        urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadEnteredUrl()
                true
            } else {
                false
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack() else finish()
                }
            },
        )

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            val initialUrl = intent.dataString ?: DEFAULT_URL
            urlInput.setText(initialUrl)
            webView.loadUrl(initialUrl)
        }
    }

    private fun loadEnteredUrl() {
        val value = urlInput.text.toString().trim()
        if (value.isBlank()) return

        val url = if (SCHEME_REGEX.containsMatchIn(value)) value else "https://$value"
        urlInput.setText(url)
        webView.loadUrl(url)
    }

    private fun updateNavigationButtons() {
        backButton.isEnabled = webView.canGoBack()
        forwardButton.isEnabled = webView.canGoForward()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        webView.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    private inner class TestWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (uri.scheme == "http" || uri.scheme == "https") return false

            return try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this@MainActivity, "No app can open ${uri.scheme} links", Toast.LENGTH_SHORT).show()
                true
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            urlInput.setText(url)
            statusText.text = "Loading $url"
            updateNavigationButtons()
        }

        override fun onPageFinished(view: WebView, url: String) {
            urlInput.setText(url)
            statusText.text = view.title?.takeIf { it.isNotBlank() } ?: url
            updateNavigationButtons()
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            Log.e(TAG, "Web error ${error.errorCode}: ${error.description} at ${request.url}")
            if (request.isForMainFrame) {
                statusText.text = "Error ${error.errorCode}: ${error.description}"
            }
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            Log.e(TAG, "HTTP ${errorResponse.statusCode} at ${request.url}")
            if (request.isForMainFrame) {
                statusText.text = "HTTP ${errorResponse.statusCode}: ${request.url}"
            }
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            Log.e(TAG, "SSL error ${error.primaryError} at ${error.url}")
            statusText.text = "SSL error: connection blocked"
            handler.cancel()
        }
    }

    private inner class TestWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            progressBar.progress = newProgress
            progressBar.visibility = if (newProgress in 0..99) View.VISIBLE else View.GONE
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            if (!title.isNullOrBlank()) statusText.text = title
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            Log.d(
                CONSOLE_TAG,
                "${consoleMessage.messageLevel()}: ${consoleMessage.message()} " +
                    "(${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})",
            )
            return true
        }
    }

    companion object {
        private const val TAG = "WebViewTest"
        private const val CONSOLE_TAG = "WebViewConsole"
        private const val DEFAULT_URL = "https://example.com"
        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
    }
}
