package com.example.multiaiworkspace.ui.main

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.multiaiworkspace.ui.input.InputPanel
import com.example.multiaiworkspace.ui.workspace.WorkspaceScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiOrder by viewModel.aiOrder.collectAsState()
    val enabledProviders by viewModel.enabledProviders.collectAsState()
    val activeAiOrder = aiOrder.filter { it in enabledProviders }
    val startupReady by viewModel.startupReady.collectAsState()
    val coverDisplayCount by viewModel.coverDisplayCount.collectAsState()
    val expandedDisplayCount by viewModel.expandedDisplayCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).systemBarsPadding().imePadding()) {
            // startupMode が DataStore から読み込まれるまで待機（WebView のロード URL 確定のため）
            if (startupReady) {
                WorkspaceScreen(
                    aiOrder = activeAiOrder,
                    coverDisplayCount = coverDisplayCount,
                    expandedDisplayCount = expandedDisplayCount,
                    tabOffset = uiState.tabOffset,
                    selectedProvider = uiState.selectedProvider,
                    recreatedProviders = uiState.recreatedProviders,
                    getWebView = viewModel::getWebView,
                    onSelectProvider = viewModel::selectProvider,
                    onShiftTab = viewModel::shiftTab,
                    onDismissRecreated = viewModel::dismissRecreatedNotice,
                    snackbarHostState = snackbarHostState,
                    onToggleInputPanel = { viewModel.setShowInputPanel(!uiState.showInputPanel) },
                    onNavigateToSettings = onNavigateToSettings,
                    modifier = Modifier.fillMaxSize(),
                    isInputPanelVisible = uiState.showInputPanel,
                )
            }

            // Input panel slides up from bottom
            InputPanel(
                visible = uiState.showInputPanel,
                sendTarget = uiState.sendTarget,
                onSendTargetChange = viewModel::updateSendTarget,
                onSend = viewModel::sendPrompt,
                onDismiss = { viewModel.setShowInputPanel(false) },
                modifier = Modifier.align(Alignment.BottomCenter).imePadding(),
            )

            // OAuth popup overlay (onCreateWindow child WebView)
            uiState.childWebView?.let { childWebView ->
                ChildWebViewOverlay(
                    childWebView = childWebView,
                    onClose = { viewModel.closeChildWebView() },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ChildWebViewOverlay(
    childWebView: WebView,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.White)) {
        AndroidView(
            factory = { childWebView },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 8.dp),
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "閉じる",
                tint = Color.Black,
            )
        }
    }
}
