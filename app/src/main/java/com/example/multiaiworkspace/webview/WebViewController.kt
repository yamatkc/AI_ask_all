package com.example.multiaiworkspace.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.util.Log
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

import com.example.multiaiworkspace.data.model.AIProvider
import com.example.multiaiworkspace.provider.ChatGPTAdapter
import com.example.multiaiworkspace.provider.ClaudeAdapter
import com.example.multiaiworkspace.provider.GeminiAdapter
import com.example.multiaiworkspace.provider.GrokAdapter
import com.example.multiaiworkspace.provider.ProviderAdapter

private const val DESKTOP_CHROME_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

const val FILE_CHOOSER_REQUEST_CODE = 1001

class WebViewController(private val appContext: Context) {

    var activity: Activity? = null
    var startupMode: String = "new"

    private val webViews = mutableMapOf<AIProvider, WebView>()
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var onRecreated: ((AIProvider) -> Unit)? = null

    var onChildWebViewChanged: ((AIProvider, WebView?) -> Unit)? = null

    val adapters: Map<AIProvider, ProviderAdapter> = mapOf(
        AIProvider.CHATGPT to ChatGPTAdapter(),
        AIProvider.CLAUDE to ClaudeAdapter(),
        AIProvider.GEMINI to GeminiAdapter(),
        AIProvider.GROK to GrokAdapter(),
    )

    fun setOnRecreatedListener(listener: (AIProvider) -> Unit) {
        onRecreated = listener
    }

    fun getWebView(provider: AIProvider): WebView {
        return webViews.getOrPut(provider) { createWebView(provider) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(provider: AIProvider): WebView {
        val t0 = System.currentTimeMillis()
        Log.d("LoadTime", "[${provider.name}] WebView生成開始: 0ms")

        val webView = WebView(appContext).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                userAgentString = DESKTOP_CHROME_UA
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                mediaPlaybackRequiresUserGesture = false
                allowContentAccess = true
                allowFileAccess = true
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                @Suppress("DEPRECATION")
                setRenderPriority(WebSettings.RenderPriority.HIGH)
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    Log.d("WebViewAuth", "[${provider.name}] URL: ${request.url}")
                    return false
                }

                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    Log.d("LoadTime", "[${provider.name}] onPageStarted: ${System.currentTimeMillis() - t0}ms")
                    Log.d("StartupMode", "[${provider.name}] currentUrl=${view.url} (onPageStarted url=$url)")
                    Log.d("WebViewAuth", "[${provider.name}] onPageStarted: $url")
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    Log.d("LoadTime", "[${provider.name}] onPageFinished: ${System.currentTimeMillis() - t0}ms")
                    Log.d("WebViewAuth", "[${provider.name}] onPageFinished: $url w=${view.width} h=${view.height} visible=${view.visibility}")
                    if (view.visibility != android.view.View.VISIBLE) {
                        Log.w("WebViewAuth", "[${provider.name}] visibility was not VISIBLE — forcing VISIBLE")
                        view.visibility = android.view.View.VISIBLE
                        view.invalidate()
                        view.requestLayout()
                    }
                    if (provider == AIProvider.CLAUDE) {
                        Log.d("WebViewDebug", "[CLAUDE] contentHeight: ${view.contentHeight}")
                        view.evaluateJavascript(
                            "document.body ? document.body.innerHTML.substring(0, 200) : 'NO BODY'"
                        ) { result ->
                            Log.d("WebViewDebug", "[CLAUDE] body: $result")
                        }
                    }
                    // ファイル添付セレクタ調査（チャット画面のみ）
                    val isMainPage = when (provider) {
                        AIProvider.CHATGPT -> url.contains("chatgpt.com")
                        AIProvider.CLAUDE -> url.contains("claude.ai")
                        AIProvider.GEMINI -> url.contains("gemini.google.com")
                        AIProvider.GROK -> url.contains("grok.com")
                    }
                    if (isMainPage) {
                        val js = when (provider) {
                            AIProvider.CHATGPT -> """
                                (function() {
                                    var fileInput = document.querySelector('input[type="file"]');
                                    var attachBtn = document.querySelector('[data-testid*="attach"], button[aria-label*="attach"], button[aria-label*="ファイル"]');
                                    return JSON.stringify({
                                        fileInput: fileInput ? fileInput.outerHTML.substring(0, 150) : null,
                                        attachBtn: attachBtn ? attachBtn.getAttribute('aria-label') : null
                                    });
                                })()
                            """.trimIndent()
                            AIProvider.CLAUDE -> """
                                (function() {
                                    var fileInput = document.querySelector('input[type="file"]');
                                    var attachBtn = document.querySelector('button[aria-label*="ファイル"], button[aria-label*="追加"], button[aria-label*="attach"]');
                                    return JSON.stringify({
                                        fileInput: fileInput ? fileInput.outerHTML.substring(0, 150) : null,
                                        attachBtn: attachBtn ? attachBtn.getAttribute('aria-label') : null
                                    });
                                })()
                            """.trimIndent()
                            AIProvider.GEMINI -> """
                                (function() {
                                    var fileInput = document.querySelector('input[type="file"]');
                                    var attachBtn = document.querySelector('button[aria-label*="ファイル"], button[aria-label*="追加"], button[aria-label*="attach"], button[aria-label*="Upload"]');
                                    return JSON.stringify({
                                        fileInput: fileInput ? fileInput.outerHTML.substring(0, 150) : null,
                                        attachBtn: attachBtn ? attachBtn.getAttribute('aria-label') : null
                                    });
                                })()
                            """.trimIndent()
                            AIProvider.GROK -> """
                                (function() {
                                    var inputs = Array.from(document.querySelectorAll('textarea, [contenteditable="true"], [role="textbox"]'));
                                    var btns = Array.from(document.querySelectorAll('button[type="submit"], button[aria-label]'))
                                        .slice(0, 5)
                                        .map(function(b) { return b.getAttribute('aria-label') || b.textContent.trim().substring(0, 30); });
                                    return JSON.stringify({ inputs: inputs.map(function(el) { return el.tagName + '.' + el.className.substring(0, 50); }), buttons: btns });
                                })()
                            """.trimIndent()
                        }
                        view.evaluateJavascript(js) { result ->
                            Log.d("WebViewDebug", "[${provider.name}] file_attach: $result")
                        }
                    }
                    // Google accounts ページで window.chrome を注入してブラウザ検出を回避
                    if (url.contains("accounts.google.com")) {
                        view.evaluateJavascript(
                            "if(!window.chrome){window.chrome={runtime:{},loadTimes:function(){},csi:function(){},app:{}};}",
                            null
                        )
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) {
                        Log.e(
                            "WebViewError",
                            "[${provider.name}] onReceivedError: ${error.errorCode} / ${error.description} / ${request.url}"
                        )
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: android.webkit.WebResourceResponse
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request.isForMainFrame) {
                        Log.e(
                            "WebViewError",
                            "[${provider.name}] onReceivedHttpError: ${errorResponse.statusCode} / ${request.url}"
                        )
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message
                ): Boolean {
                    Log.d("WebViewAuth", "[${provider.name}] onCreateWindow isDialog=$isDialog isUserGesture=$isUserGesture")
                    val child = createChildWebView(provider)
                    val transport = resultMsg.obj as WebView.WebViewTransport
                    transport.webView = child
                    resultMsg.sendToTarget()
                    onChildWebViewChanged?.invoke(provider, child)
                    return true
                }

                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    val level = message.messageLevel()
                    val text = "[${provider.name}] JS(${message.sourceId()}:${message.lineNumber()}) ${message.message()}"
                    when (level) {
                        ConsoleMessage.MessageLevel.ERROR -> Log.e("WebViewConsole", text)
                        ConsoleMessage.MessageLevel.WARNING -> Log.w("WebViewConsole", text)
                        else -> Log.d("WebViewConsole", text)
                    }
                    return true
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    this@WebViewController.filePathCallback?.onReceiveValue(null)
                    this@WebViewController.filePathCallback = filePathCallback
                    launchFilePicker()
                    return true
                }
            }
        }

        adapters[provider]?.onWebViewCreated(webView)
        val startUrl = provider.startupUrl(startupMode)
        Log.d("StartupMode", "[${provider.name}] generatedUrl=$startUrl (startupMode=$startupMode)")
        Log.d("StartupMode", "[${provider.name}] loadUrl=$startUrl")
        Log.d("LoadTime", "[${provider.name}] loadUrl開始: ${System.currentTimeMillis() - t0}ms")
        webView.loadUrl(startUrl)
        return webView
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createChildWebView(parentProvider: AIProvider): WebView {
        return WebView(appContext).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                userAgentString = DESKTOP_CHROME_UA
                loadWithOverviewMode = true
                useWideViewPort = true
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                allowContentAccess = true
                allowFileAccess = true
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    Log.d("WebViewAuth", "[${parentProvider.name}:child] URL: ${request.url}")
                    return false
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    Log.d("WebViewAuth", "[${parentProvider.name}:child] onPageFinished: $url")
                    // window.chrome注入
                    if (url.contains("accounts.google.com")) {
                        view.evaluateJavascript(
                            "if(!window.chrome){window.chrome={runtime:{},loadTimes:function(){},csi:function(){},app:{}};}",
                            null
                        )
                    }
                    // 親ドメインに戻ったらオーバーレイを閉じて親を更新
                    val parentHost = parentProvider.url.removePrefix("https://").removeSuffix("/")
                    if (url.startsWith("https://$parentHost")) {
                        Log.d("WebViewAuth", "[${parentProvider.name}:child] returned to parent, closing")
                        onChildWebViewChanged?.invoke(parentProvider, null)
                        webViews[parentProvider]?.loadUrl(url)
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCloseWindow(window: WebView) {
                    Log.d("WebViewAuth", "[${parentProvider.name}:child] onCloseWindow")
                    onChildWebViewChanged?.invoke(parentProvider, null)
                }
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    val text = "[${parentProvider.name}:child] JS ${message.message()}"
                    when (message.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> Log.e("WebViewConsole", text)
                        else -> Log.d("WebViewConsole", text)
                    }
                    return true
                }
            }
        }
    }

    private fun launchFilePicker() {
        val act = activity ?: return
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        @Suppress("DEPRECATION")
        act.startActivityForResult(
            Intent.createChooser(intent, "ファイルを選択"),
            FILE_CHOOSER_REQUEST_CODE
        )
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FILE_CHOOSER_REQUEST_CODE) return
        val callback = filePathCallback ?: return
        filePathCallback = null
        if (resultCode == Activity.RESULT_OK && data != null) {
            val uris = mutableListOf<Uri>()
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    uris.add(clip.getItemAt(i).uri)
                }
            } ?: data.data?.let { uris.add(it) }
            callback.onReceiveValue(uris.toTypedArray())
        } else {
            callback.onReceiveValue(null)
        }
    }

    fun notifyWebViewRecreated(provider: AIProvider) {
        webViews.remove(provider)
        onRecreated?.invoke(provider)
    }

    fun flushCookies() {
        CookieManager.getInstance().flush()
    }
}
