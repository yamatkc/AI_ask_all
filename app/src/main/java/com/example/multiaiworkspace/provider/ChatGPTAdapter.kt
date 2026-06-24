package com.example.multiaiworkspace.provider

import android.webkit.WebView
import com.example.multiaiworkspace.data.model.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ChatGPTAdapter : ProviderAdapter {
    override val provider = AIProvider.CHATGPT
    override val url = AIProvider.CHATGPT.url

    override fun supportsFileAttachment() = true

    override fun onWebViewCreated(webView: WebView) {
        // No additional setup needed beyond WebViewController defaults
    }

    override suspend fun sendPrompt(webView: WebView, prompt: String) {
        withContext(Dispatchers.Main) {
            val escaped = prompt.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            val js = """
                (function() {
                    var editor = document.querySelector('#prompt-textarea');
                    if (!editor) {
                        editor = document.querySelector('[contenteditable="true"]');
                    }
                    if (editor) {
                        editor.focus();
                        var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLElement.prototype, 'innerText') ||
                            Object.getOwnPropertyDescriptor(window.HTMLParagraphElement.prototype, 'innerText');
                        editor.innerText = `$escaped`;
                        editor.dispatchEvent(new InputEvent('input', { bubbles: true }));
                        setTimeout(function() {
                            var sendBtn = document.querySelector('[data-testid="send-button"]');
                            if (!sendBtn) sendBtn = document.querySelector('button[aria-label="Send prompt"]');
                            if (sendBtn) sendBtn.click();
                        }, 300);
                        return true;
                    }
                    return false;
                })();
            """.trimIndent()
            webView.evaluateJavascript(js, null)
        }
    }
}
