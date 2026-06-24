package com.example.multiaiworkspace.ui.main

import android.app.Application
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.multiaiworkspace.data.model.AIProvider
import com.example.multiaiworkspace.data.model.SendTarget
import com.example.multiaiworkspace.data.repository.WorkspaceRepository
import com.example.multiaiworkspace.webview.WebViewController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import android.util.Log
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedProvider: AIProvider = AIProvider.CHATGPT,
    val aiOrder: List<AIProvider> = AIProvider.entries.toList(),
    val showInputPanel: Boolean = false,
    val sendTarget: SendTarget = SendTarget(),
    val recreatedProviders: Set<AIProvider> = emptySet(),
    val childWebView: WebView? = null,
    val tabOffset: Int = 0,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val webViewController = WebViewController(application)
    private val repository = WorkspaceRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // DataStore から startupMode を読み込み完了したら true になるフラグ
    // これが true になるまで WebView は生成しない（loadUrl の URL を確定させるため）
    private val _startupReady = MutableStateFlow(false)
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()

    val aiOrder: StateFlow<List<AIProvider>> = repository.aiOrderFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AIProvider.entries.toList())

    val initialAI: StateFlow<AIProvider> = repository.initialAIFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AIProvider.CHATGPT)

    val startupMode: StateFlow<String> = repository.startupModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "new")

    val enabledProviders: StateFlow<Set<AIProvider>> = repository.enabledProvidersFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            setOf(AIProvider.CHATGPT, AIProvider.CLAUDE, AIProvider.GEMINI, AIProvider.GROK),
        )

    val coverDisplayCount: StateFlow<Int> = repository.coverDisplayCountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    val expandedDisplayCount: StateFlow<Int> = repository.expandedDisplayCountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 3)

    init {
        webViewController.onChildWebViewChanged = { _, childWebView ->
            _uiState.value = _uiState.value.copy(childWebView = childWebView)
        }

        webViewController.setOnRecreatedListener { provider ->
            _uiState.value = _uiState.value.copy(
                recreatedProviders = _uiState.value.recreatedProviders + provider
            )
        }

        viewModelScope.launch {
            repository.aiOrderFlow.collect { order ->
                _uiState.value = _uiState.value.copy(aiOrder = order)
            }
        }
        viewModelScope.launch {
            repository.initialAIFlow.collect { provider ->
                _uiState.value = _uiState.value.copy(selectedProvider = provider)
            }
        }

        // startupMode を DataStore から読み込んでから WebView 生成を解禁する
        // first() で最初の値が来るまで待ち、WebViewController に設定してから startupReady = true
        viewModelScope.launch {
            val initialMode = repository.startupModeFlow.first()
            Log.d("StartupMode", "startup_mode読み込み値: $initialMode")
            webViewController.startupMode = initialMode
            _startupReady.value = true
            // 以降の変更も反映（設定変更は次回起動時の WebView 生成時に有効）
            repository.startupModeFlow.collect { mode ->
                webViewController.startupMode = mode
            }
        }
    }

    fun selectProvider(provider: AIProvider) {
        _uiState.value = _uiState.value.copy(selectedProvider = provider)
    }

    fun setShowInputPanel(show: Boolean) {
        _uiState.value = _uiState.value.copy(showInputPanel = show)
    }

    fun updateSendTarget(target: SendTarget) {
        _uiState.value = _uiState.value.copy(sendTarget = target)
    }

    fun dismissRecreatedNotice(provider: AIProvider) {
        _uiState.value = _uiState.value.copy(
            recreatedProviders = _uiState.value.recreatedProviders - provider
        )
    }

    fun sendPrompt(prompt: String, target: SendTarget) {
        val order = _uiState.value.aiOrder
        viewModelScope.launch {
            for (provider in order) {
                if (target.isSelected(provider)) {
                    val webView = webViewController.getWebView(provider)
                    webViewController.adapters[provider]?.sendPrompt(webView, prompt)
                }
            }
        }
        setShowInputPanel(false)
    }

    fun reorderAI(newOrder: List<AIProvider>) {
        viewModelScope.launch { repository.saveAIOrder(newOrder) }
    }

    fun saveDisplayCount(count: Int) {
        viewModelScope.launch { repository.saveDisplayCount(count) }
    }

    fun saveInitialAI(provider: AIProvider) {
        viewModelScope.launch { repository.saveInitialAI(provider) }
    }

    fun saveEnabledProviders(enabled: Set<AIProvider>) {
        viewModelScope.launch { repository.saveEnabledProviders(enabled) }
    }

    fun saveStartupMode(mode: String) {
        viewModelScope.launch { repository.saveStartupMode(mode) }
    }

    fun saveCoverDisplayCount(count: Int) {
        viewModelScope.launch { repository.saveCoverDisplayCount(count) }
    }

    fun saveExpandedDisplayCount(count: Int) {
        viewModelScope.launch { repository.saveExpandedDisplayCount(count) }
    }

    fun shiftTab(delta: Int, visibleCount: Int) {
        val activeSize = enabledProviders.value.size
        val maxOffset = (activeSize - visibleCount).coerceAtLeast(0)
        val newOffset = (_uiState.value.tabOffset + delta).coerceIn(0, maxOffset)
        _uiState.value = _uiState.value.copy(tabOffset = newOffset)
    }

    fun closeChildWebView() {
        _uiState.value = _uiState.value.copy(childWebView = null)
    }

    fun getWebView(provider: AIProvider): WebView = webViewController.getWebView(provider)

    override fun onCleared() {
        super.onCleared()
        webViewController.flushCookies()
    }
}
