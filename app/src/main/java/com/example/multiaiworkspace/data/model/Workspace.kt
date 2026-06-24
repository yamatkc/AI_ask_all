package com.example.multiaiworkspace.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AIOrder(val order: List<AIProvider> = AIProvider.entries.toList())

data class SendTarget(
    val chatGPT: Boolean = true,
    val claude: Boolean = true,
    val gemini: Boolean = true,
    val grok: Boolean = true,
) {
    fun isSelected(provider: AIProvider): Boolean = when (provider) {
        AIProvider.CHATGPT -> chatGPT
        AIProvider.CLAUDE -> claude
        AIProvider.GEMINI -> gemini
        AIProvider.GROK -> grok
    }
}
