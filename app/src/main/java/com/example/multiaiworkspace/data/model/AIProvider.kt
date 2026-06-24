package com.example.multiaiworkspace.data.model

enum class AIProvider(val displayName: String, val shortName: String, val url: String) {
    CHATGPT("ChatGPT", "GPT", "https://chatgpt.com"),
    CLAUDE("Claude", "Cla", "https://claude.ai"),
    GEMINI("Gemini", "Gem", "https://gemini.google.com"),
    GROK("Grok", "Grk", "https://grok.com");

    fun startupUrl(mode: String): String = when (this) {
        CHATGPT -> "https://chatgpt.com/"
        // "new": 新規チャット / "continue": 前回チャットの続き（トップページ）
        CLAUDE -> if (mode == "new") "https://claude.ai/new" else "https://claude.ai/"
        GEMINI -> "https://gemini.google.com/app"
        GROK -> "https://grok.com/"
    }
}
