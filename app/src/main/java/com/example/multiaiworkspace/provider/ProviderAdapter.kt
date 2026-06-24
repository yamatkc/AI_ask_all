package com.example.multiaiworkspace.provider

import android.webkit.WebView
import com.example.multiaiworkspace.data.model.AIProvider

interface ProviderAdapter {
    val provider: AIProvider
    val url: String

    suspend fun sendPrompt(webView: WebView, prompt: String)

    fun supportsFileAttachment(): Boolean

    fun onWebViewCreated(webView: WebView)
}
