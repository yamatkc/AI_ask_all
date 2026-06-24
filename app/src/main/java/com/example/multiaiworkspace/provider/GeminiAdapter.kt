package com.example.multiaiworkspace.provider

import android.util.Log
import android.webkit.WebView
import com.example.multiaiworkspace.data.model.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAdapter : ProviderAdapter {
    override val provider = AIProvider.GEMINI
    override val url = AIProvider.GEMINI.url

    override fun supportsFileAttachment() = true

    override fun onWebViewCreated(webView: WebView) {
        // No additional setup needed beyond WebViewController defaults
    }

    override suspend fun sendPrompt(webView: WebView, prompt: String) {
        withContext(Dispatchers.Main) {
            // セレクタデバッグ: 実際のinput要素を出力
            webView.evaluateJavascript("""
                (function() {
                    var els = document.querySelectorAll('textarea, [contenteditable]');
                    return JSON.stringify(Array.from(els).map(function(el) {
                        return el.tagName + ':' + el.className.substring(0, 60);
                    }));
                })()
            """.trimIndent()) { result ->
                Log.d("WebViewDebug", "[GEMINI] inputs found: $result")
            }

            val escaped = prompt.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            val js = """
                (function() {
                    try {
                        var editor = document.querySelector('.ql-editor');
                        if (!editor) editor = document.querySelector('[contenteditable="true"]');
                        if (!editor) return 'NO_INPUT';
                        editor.focus();
                        document.execCommand('selectAll', false, null);
                        document.execCommand('insertText', false, `$escaped`);
                        editor.dispatchEvent(new Event('input', { bubbles: true }));
                        setTimeout(function() {
                            try {
                                var allBtns = Array.from(document.querySelectorAll('button'));
                                var btn = allBtns.find(function(b) {
                                    var label = b.getAttribute('aria-label') || '';
                                    return label.includes('Send') || label.includes('送信');
                                });
                                if (btn && !btn.disabled) { btn.click(); return; }
                                editor.dispatchEvent(new KeyboardEvent('keydown', {
                                    key: 'Enter', keyCode: 13, which: 13,
                                    bubbles: true, shiftKey: false
                                }));
                            } catch(e2) {}
                        }, 600);
                        return 'ok';
                    } catch(e) {
                        return 'ERR:' + e.message;
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(js) { result ->
                Log.d("WebViewDebug", "[GEMINI] sendPrompt result: $result")
            }
        }
    }
}
