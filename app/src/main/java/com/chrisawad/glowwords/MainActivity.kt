package com.chrisawad.glowwords

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var pendingAudioRequest: PermissionRequest? = null
    private var textToSpeech: TextToSpeech? = null
    private var textToSpeechReady = false
    private val pendingSpeech = mutableListOf<SpeechRequest>()
    private var utteranceNumber = 0

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingAudioRequest?.let { request ->
                if (granted && request.isTrustedAudioRequest()) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                } else {
                    request.deny()
                }
            }
            pendingAudioRequest = null
        }

    @SuppressLint("AddJavascriptInterface", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            useWideViewPort = true
            loadWithOverviewMode = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = false
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, false)
        }

        initializeTextToSpeech()
        webView.addJavascriptInterface(NativeSpeechBridge(), NATIVE_SPEECH_BRIDGE)
        webView.webViewClient = GlowWordsWebViewClient()
        webView.webChromeClient = GlowWordsWebChromeClient()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack() else finish()
                }
            },
        )

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(GLOW_WORDS_URL)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        pendingAudioRequest?.deny()
        pendingAudioRequest = null
        pendingSpeech.clear()
        textToSpeechReady = false
        textToSpeech?.apply {
            stop()
            shutdown()
        }
        textToSpeech = null
        webView.apply {
            stopLoading()
            removeJavascriptInterface(NATIVE_SPEECH_BRIDGE)
            webChromeClient = null
            webViewClient = WebViewClient()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(applicationContext) { status ->
            runOnUiThread {
                val engine = textToSpeech
                val languageStatus =
                    if (status == TextToSpeech.SUCCESS && engine != null) {
                        engine.setLanguage(Locale.US)
                    } else {
                        TextToSpeech.LANG_NOT_SUPPORTED
                    }
                textToSpeechReady = languageStatus != TextToSpeech.LANG_MISSING_DATA &&
                    languageStatus != TextToSpeech.LANG_NOT_SUPPORTED

                if (!textToSpeechReady || engine == null) {
                    pendingSpeech.clear()
                    Log.e(TAG, "Android text-to-speech is unavailable")
                    return@runOnUiThread
                }

                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                pendingSpeech.toList().also { pendingSpeech.clear() }.forEach(::speak)
            }
        }
    }

    private fun speak(request: SpeechRequest) {
        val engine = textToSpeech ?: return
        if (!textToSpeechReady) {
            if (pendingSpeech.size < MAX_PENDING_SPEECH) pendingSpeech += request
            return
        }

        engine.setSpeechRate(request.rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE))
        engine.setPitch(request.pitch.coerceIn(MIN_SPEECH_PITCH, MAX_SPEECH_PITCH))
        utteranceNumber += 1
        engine.speak(
            request.text,
            TextToSpeech.QUEUE_ADD,
            null,
            "glowwords-$utteranceNumber",
        )
    }

    private fun stopSpeaking() {
        pendingSpeech.clear()
        textToSpeech?.stop()
    }

    private inner class NativeSpeechBridge {
        @JavascriptInterface
        fun speak(text: String, rate: Double, pitch: Double) {
            val request = SpeechRequest(
                text = text.take(MAX_SPEECH_TEXT_LENGTH),
                rate = rate.toFloat(),
                pitch = pitch.toFloat(),
            )
            if (request.text.isBlank()) return

            webView.post {
                if (webView.isTrustedGlowWordsPage()) this@MainActivity.speak(request)
            }
        }

        @JavascriptInterface
        fun cancel() {
            webView.post {
                if (webView.isTrustedGlowWordsPage()) stopSpeaking()
            }
        }
    }

    private inner class GlowWordsWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (uri.isTrustedGlowWordsUrl()) return false

            return try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                true
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this@MainActivity, "No app can open ${uri.scheme} links", Toast.LENGTH_SHORT).show()
                true
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            if (Uri.parse(url).isTrustedGlowWordsUrl()) {
                view.evaluateJavascript(NATIVE_SPEECH_POLYFILL, null)
            }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            Log.e(TAG, "Web error ${error.errorCode}: ${error.description} at ${request.url}")
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            Log.e(TAG, "HTTP ${errorResponse.statusCode} at ${request.url}")
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            Log.e(TAG, "SSL error ${error.primaryError} at ${error.url}")
            handler.cancel()
        }
    }

    private inner class GlowWordsWebChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest) {
            runOnUiThread {
                if (!request.isTrustedAudioRequest()) {
                    request.deny()
                    return@runOnUiThread
                }

                if (
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                } else {
                    pendingAudioRequest?.deny()
                    pendingAudioRequest = request
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest) {
            runOnUiThread {
                if (pendingAudioRequest === request) {
                    pendingAudioRequest = null
                }
            }
        }
    }

    private fun PermissionRequest.isTrustedAudioRequest(): Boolean =
        origin.scheme == "https" &&
            origin.host?.lowercase(Locale.US)?.let(TRUSTED_GLOW_WORDS_HOSTS::contains) == true &&
            webView.isTrustedGlowWordsPage() &&
            resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

    private fun WebView.isTrustedGlowWordsPage(): Boolean =
        url?.let(Uri::parse)?.isTrustedGlowWordsUrl() == true

    private fun Uri.isTrustedGlowWordsUrl(): Boolean {
        if (scheme != "https") return false
        return when (host?.lowercase(Locale.US)) {
            GLOW_WORDS_HOST -> true
            GITHUB_PAGES_HOST -> path == GITHUB_PAGES_PATH.removeSuffix("/") ||
                path?.startsWith(GITHUB_PAGES_PATH) == true
            else -> false
        }
    }

    private data class SpeechRequest(
        val text: String,
        val rate: Float,
        val pitch: Float,
    )

    companion object {
        private const val TAG = "GlowWords"
        private const val GLOW_WORDS_HOST = "glow.chrisawad.com"
        private const val GITHUB_PAGES_HOST = "chrisawad.github.io"
        private const val GITHUB_PAGES_PATH = "/GlowWords/"
        private const val GLOW_WORDS_URL = "https://$GLOW_WORDS_HOST"
        private val TRUSTED_GLOW_WORDS_HOSTS = setOf(GLOW_WORDS_HOST, GITHUB_PAGES_HOST)
        private const val NATIVE_SPEECH_BRIDGE = "GlowWordsNativeSpeech"
        private const val MAX_PENDING_SPEECH = 32
        private const val MAX_SPEECH_TEXT_LENGTH = 80
        private const val MIN_SPEECH_RATE = 0.1f
        private const val MAX_SPEECH_RATE = 2f
        private const val MIN_SPEECH_PITCH = 0.5f
        private const val MAX_SPEECH_PITCH = 2f
        private val NATIVE_SPEECH_POLYFILL = """
            (() => {
              const bridge = window.GlowWordsNativeSpeech;
              if (!bridge) return;

              if (!window.SpeechSynthesisUtterance) {
                window.SpeechSynthesisUtterance = class {
                  constructor(text = '') {
                    this.text = String(text);
                    this.lang = '';
                    this.voice = null;
                    this.volume = 1;
                    this.rate = 1;
                    this.pitch = 1;
                    this.onstart = null;
                    this.onend = null;
                    this.onerror = null;
                  }
                };
              }

              if (!window.speechSynthesis) {
                window.speechSynthesis = {
                  speaking: false,
                  pending: false,
                  paused: false,
                  speak(utterance) {
                    if (!utterance || !utterance.text) return;
                    bridge.speak(
                      String(utterance.text),
                      Number(utterance.rate) || 1,
                      Number(utterance.pitch) || 1,
                    );
                  },
                  cancel() {
                    bridge.cancel();
                  },
                  pause() {
                    bridge.cancel();
                    this.paused = true;
                  },
                  resume() {
                    this.paused = false;
                  },
                  getVoices() {
                    return [];
                  },
                };
              }
            })();
        """.trimIndent()
    }
}
