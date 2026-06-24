package com.example.multiaiworkspace.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.multiaiworkspace.data.model.AIProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "workspace_prefs")

class WorkspaceRepository(private val context: Context) {

    private val AI_ORDER_KEY = stringPreferencesKey("ai_order")
    private val INITIAL_AI_KEY = stringPreferencesKey("initial_ai")
    private val DISPLAY_COUNT_KEY = intPreferencesKey("display_count")
    private val COVER_DISPLAY_COUNT_KEY = intPreferencesKey("cover_display_count")
    private val EXPANDED_DISPLAY_COUNT_KEY = intPreferencesKey("expanded_display_count")
    private val ENABLED_PROVIDERS_KEY = stringPreferencesKey("enabled_providers")
    private val STARTUP_MODE_KEY = stringPreferencesKey("startup_mode")

    // デフォルト有効: 全AI（ChatGPT / Claude / Gemini / Grok）
    private val defaultEnabled = setOf(
        AIProvider.CHATGPT.name,
        AIProvider.CLAUDE.name,
        AIProvider.GEMINI.name,
        AIProvider.GROK.name,
    )

    val aiOrderFlow: Flow<List<AIProvider>> = context.dataStore.data.map { prefs ->
        val json = prefs[AI_ORDER_KEY]
        if (json != null) {
            try {
                Json.decodeFromString<List<String>>(json).mapNotNull { name ->
                    AIProvider.entries.find { it.name == name }
                }.takeIf { it.size == AIProvider.entries.size } ?: AIProvider.entries.toList()
            } catch (e: Exception) {
                AIProvider.entries.toList()
            }
        } else {
            AIProvider.entries.toList()
        }
    }

    val initialAIFlow: Flow<AIProvider> = context.dataStore.data.map { prefs ->
        val name = prefs[INITIAL_AI_KEY]
        AIProvider.entries.find { it.name == name } ?: AIProvider.CHATGPT
    }

    val displayCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[DISPLAY_COUNT_KEY] ?: 1
    }

    // カバー画面の表示数（1〜2）
    val coverDisplayCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COVER_DISPLAY_COUNT_KEY] ?: 1
    }

    // 展開時の表示数（1〜4）
    val expandedDisplayCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[EXPANDED_DISPLAY_COUNT_KEY] ?: 3
    }

    val enabledProvidersFlow: Flow<Set<AIProvider>> = context.dataStore.data.map { prefs ->
        val json = prefs[ENABLED_PROVIDERS_KEY]
        if (json != null) {
            try {
                Json.decodeFromString<List<String>>(json).mapNotNull { name ->
                    AIProvider.entries.find { it.name == name }
                }.toSet().ifEmpty { defaultEnabled.mapNotNull { n -> AIProvider.entries.find { it.name == n } }.toSet() }
            } catch (e: Exception) {
                defaultEnabled.mapNotNull { n -> AIProvider.entries.find { it.name == n } }.toSet()
            }
        } else {
            defaultEnabled.mapNotNull { n -> AIProvider.entries.find { it.name == n } }.toSet()
        }
    }

    suspend fun saveAIOrder(order: List<AIProvider>) {
        context.dataStore.edit { prefs ->
            prefs[AI_ORDER_KEY] = Json.encodeToString(order.map { it.name })
        }
    }

    suspend fun saveInitialAI(provider: AIProvider) {
        context.dataStore.edit { prefs ->
            prefs[INITIAL_AI_KEY] = provider.name
        }
    }

    suspend fun saveDisplayCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[DISPLAY_COUNT_KEY] = count.coerceIn(1, 3)
        }
    }

    suspend fun saveCoverDisplayCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[COVER_DISPLAY_COUNT_KEY] = count.coerceIn(1, 2)
        }
    }

    suspend fun saveExpandedDisplayCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[EXPANDED_DISPLAY_COUNT_KEY] = count.coerceIn(1, 4)
        }
    }

    suspend fun saveEnabledProviders(enabled: Set<AIProvider>) {
        context.dataStore.edit { prefs ->
            prefs[ENABLED_PROVIDERS_KEY] = Json.encodeToString(enabled.map { it.name })
        }
    }

    val startupModeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[STARTUP_MODE_KEY] ?: "new"
    }

    suspend fun saveStartupMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[STARTUP_MODE_KEY] = mode
        }
    }
}
