package com.example.multiaiworkspace.ui.workspace

import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.multiaiworkspace.data.model.AIProvider

private const val GOOGLE_LOGIN_URL = "https://accounts.google.com"

@Composable
fun AIWebViewPanel(
    provider: AIProvider,
    getWebView: (AIProvider) -> WebView,
    modifier: Modifier = Modifier,
    isInputPanelVisible: Boolean = false,
) {
    val context = LocalContext.current
    val webView = remember(provider) { getWebView(provider) }

    // Gemini 専用: Google ログイン誘導バナーの表示制御
    // ユーザーが一度 CustomTabs でログインしたら非表示にできる
    var showGeminiLoginHint by remember { mutableStateOf(false) }

    Box(modifier = modifier.clipToBounds()) {
        AndroidView(
            factory = {
                Log.d("WebViewCompose", "[${provider.name}] AndroidView factory")
                webView
            },
            modifier = Modifier.fillMaxSize().clipToBounds(),
            update = { view ->
                Log.d("WebViewCompose", "[${provider.name}] AndroidView update: visibility=${view.visibility} w=${view.width} h=${view.height}")
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                if (view.visibility != View.VISIBLE) {
                    view.visibility = View.VISIBLE
                    view.invalidate()
                    view.requestLayout()
                }
                Log.d("WebViewVisibility", "[${provider.name}] visibility=${view.visibility} width=${view.width} height=${view.height} contentHeight=${view.contentHeight}")
            }
        )

        // Gemini のみ: Google アカウントログイン誘導ボタン（InputPanel表示中は隠す）
        if (provider == AIProvider.GEMINI && showGeminiLoginHint && !isInputPanelVisible) {
            Button(
                onClick = {
                    CustomTabsIntent.Builder()
                        .setShowTitle(true)
                        .build()
                        .launchUrl(context, Uri.parse(GOOGLE_LOGIN_URL))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp, start = 16.dp, end = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text("Googleアカウントでログイン（Chrome）")
            }

            // ログイン完了後に非表示にするボタン
            Button(
                onClick = { showGeminiLoginHint = false },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 72.dp, end = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.outline,
                ),
                elevation = null,
            ) {
                Text("ログイン済み ✕", style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    DisposableEffect(provider) {
        onDispose {
            Log.d("WebViewCompose", "[${provider.name}] onDispose → GONE")
            webView.visibility = View.GONE
        }
    }
}
