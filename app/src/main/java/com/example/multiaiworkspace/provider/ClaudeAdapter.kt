package com.example.multiaiworkspace.provider

import android.util.Log
import android.webkit.WebView
import com.example.multiaiworkspace.data.model.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClaudeAdapter : ProviderAdapter {
    override val provider = AIProvider.CLAUDE
    override val url = AIProvider.CLAUDE.url

    override fun supportsFileAttachment() = true

    override fun onWebViewCreated(webView: WebView) {
        // No additional setup needed beyond WebViewController defaults
    }

    override suspend fun sendPrompt(webView: WebView, prompt: String) {
        withContext(Dispatchers.Main) {
            val escaped = prompt.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            val js = """
                (function() {
                    var el = document.querySelector('.ProseMirror');
                    if (!el) return 'NO_INPUT';
                    el.focus();
                    document.execCommand('insertText', false, `$escaped`);
                    setTimeout(function() {
                        var btn = document.querySelector('button[aria-label="メッセージを送信"]');
                        if (btn && !btn.disabled) {
                            btn.click();
                            return;
                        }
                        el.dispatchEvent(new KeyboardEvent('keydown', {
                            bubbles: true, cancelable: true,
                            key: 'Enter', code: 'Enter',
                            keyCode: 13, which: 13,
                            shiftKey: false
                        }));
                    }, 500);
                    return 'ok';
                })();
            """.trimIndent()
            webView.evaluateJavascript(js) { result ->
                Log.d("WebViewDebug", "[CLAUDE] sendPrompt result: $result")
            }
        }
    }
}
