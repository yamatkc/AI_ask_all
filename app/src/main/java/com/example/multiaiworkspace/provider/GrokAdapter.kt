package com.example.multiaiworkspace.provider

import android.util.Log
import android.webkit.WebView
import com.example.multiaiworkspace.data.model.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GrokAdapter : ProviderAdapter {
    override val provider = AIProvider.GROK
    override val url = AIProvider.GROK.url

    override fun supportsFileAttachment() = false

    override fun onWebViewCreated(webView: WebView) {
        webView.settings.apply {
            userAgentString =
                "Mozilla/5.0 (Linux; Android 14; SM-F946B) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"
            // モバイルUAなのでワイドビューポートを無効化（多パネル時のはみ出し防止）
            useWideViewPort = false
            loadWithOverviewMode = false
        }
    }

    override suspend fun sendPrompt(webView: WebView, prompt: String) {
        withContext(Dispatchers.Main) {
            val escaped = prompt.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            val js = """
                (function() {
                    var el = document.querySelector('textarea.w-full');
                    if (!el) return 'NO_INPUT';

                    // Reactのtextareaにテキストをセット
                    var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
                    if (nativeSetter && nativeSetter.set) {
                        nativeSetter.set.call(el, `$escaped`);
                    } else {
                        el.value = `$escaped`;
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    el.focus();

                    setTimeout(function() {
                        // 1. formがあればsubmit
                        var form = el.closest('form');
                        if (form) {
                            var submitBtn = form.querySelector('button[type="submit"]');
                            if (submitBtn && !submitBtn.disabled) {
                                submitBtn.click();
                                return;
                            }
                            // formのsubmitボタンを探す（type属性なし含む）
                            var allBtns = Array.from(form.querySelectorAll('button'));
                            if (allBtns.length > 0) {
                                allBtns[allBtns.length - 1].click();
                                return;
                            }
                        }
                        // 2. textareaの親要素内のボタンを探す
                        var parent = el.parentElement;
                        for (var i = 0; i < 5; i++) {
                            if (!parent) break;
                            var btns = Array.from(parent.querySelectorAll('button:not([disabled])'));
                            if (btns.length > 0) {
                                // 最後のボタンが送信ボタンの可能性が高い
                                btns[btns.length - 1].click();
                                return;
                            }
                            parent = parent.parentElement;
                        }
                        // 3. フォールバック: Enterキー
                        el.dispatchEvent(new KeyboardEvent('keydown', {
                            bubbles: true, cancelable: true,
                            key: 'Enter', code: 'Enter', keyCode: 13, which: 13, shiftKey: false
                        }));
                    }, 400);
                    return 'ok';
                })()
            """.trimIndent()
            webView.evaluateJavascript(js) { result ->
                Log.d("WebViewDebug", "[GROK] sendPrompt result: $result")
            }
        }
    }
}
