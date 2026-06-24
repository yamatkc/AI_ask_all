package com.example.multiaiworkspace.ui.workspace

import android.webkit.WebView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import com.example.multiaiworkspace.data.model.AIProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkspaceScreen(
    aiOrder: List<AIProvider>,
    coverDisplayCount: Int,
    expandedDisplayCount: Int,
    tabOffset: Int,
    selectedProvider: AIProvider,
    recreatedProviders: Set<AIProvider>,
    getWebView: (AIProvider) -> WebView,
    onSelectProvider: (AIProvider) -> Unit,
    onShiftTab: (delta: Int, visibleCount: Int) -> Unit,
    onDismissRecreated: (AIProvider) -> Unit,
    snackbarHostState: SnackbarHostState,
    onToggleInputPanel: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isInputPanelVisible: Boolean = false,
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isExpanded = screenWidthDp > 600

    val rawSetting = if (isExpanded) expandedDisplayCount else coverDisplayCount
    val displayCount = if (isExpanded) {
        expandedDisplayCount.coerceIn(1, aiOrder.size.coerceAtLeast(1))
    } else {
        coverDisplayCount.coerceIn(1, aiOrder.size.coerceAtLeast(1))
    }
    Log.d("DisplayCount", "displayCount=$displayCount, settingValue(raw)=$rawSetting, aiOrder.size=${aiOrder.size}, isExpanded=$isExpanded")

    // tabOffsetを安全範囲にクランプ（enabledProviders変化時の保護）
    val safeOffset = tabOffset.coerceIn(0, (aiOrder.size - displayCount).coerceAtLeast(0))
    val visibleProviders = aiOrder.drop(safeOffset).take(displayCount)
    val canShiftLeft = safeOffset > 0
    val canShiftRight = safeOffset + displayCount < aiOrder.size

    Log.d("TabSwitch", "tabOffset=$safeOffset visibleProviders=${visibleProviders.joinToString { it.shortName }}")
    Log.d("ArrowBtn", "enabledProviders.size: ${aiOrder.size}")
    Log.d("ArrowBtn", "visibleCount: $displayCount")
    Log.d("ArrowBtn", "tabOffset: $safeOffset")
    Log.d("ArrowBtn", "isExpanded: $isExpanded")
    Log.d("ArrowBtn", "visibleProviders: ${visibleProviders.joinToString { it.shortName }}")
    val showLeft = safeOffset > 0
    val showRight = safeOffset + displayCount < aiOrder.size
    Log.d("ArrowBtn", "showLeft=$showLeft showRight=$showRight")
    Log.d("ArrowBtn", "showArrow条件: enabledProviders.size(${aiOrder.size}) > visibleCount($displayCount) = ${aiOrder.size > displayCount}")

    LaunchedEffect(recreatedProviders) {
        for (provider in recreatedProviders) {
            snackbarHostState.showSnackbar("${provider.displayName} を再読み込みしました")
            onDismissRecreated(provider)
        }
    }

    when {
        displayCount >= 4 -> MultiPanelLayout(
            allProviders = aiOrder,
            visibleProviders = visibleProviders,
            displayCount = displayCount,
            canShiftLeft = canShiftLeft,
            canShiftRight = canShiftRight,
            getWebView = getWebView,
            onToggleInputPanel = onToggleInputPanel,
            onNavigateToSettings = onNavigateToSettings,
            onShiftLeft = { onShiftTab(-1, displayCount) },
            onShiftRight = { onShiftTab(1, displayCount) },
            modifier = modifier,
            isInputPanelVisible = isInputPanelVisible,
        )
        displayCount == 3 -> MultiPanelLayout(
            allProviders = aiOrder,
            visibleProviders = visibleProviders,
            displayCount = displayCount,
            canShiftLeft = canShiftLeft,
            canShiftRight = canShiftRight,
            getWebView = getWebView,
            onToggleInputPanel = onToggleInputPanel,
            onNavigateToSettings = onNavigateToSettings,
            onShiftLeft = { onShiftTab(-1, displayCount) },
            onShiftRight = { onShiftTab(1, displayCount) },
            modifier = modifier,
            isInputPanelVisible = isInputPanelVisible,
        )
        displayCount == 2 -> MultiPanelLayout(
            allProviders = aiOrder,
            visibleProviders = visibleProviders,
            displayCount = displayCount,
            canShiftLeft = canShiftLeft,
            canShiftRight = canShiftRight,
            getWebView = getWebView,
            onToggleInputPanel = onToggleInputPanel,
            onNavigateToSettings = onNavigateToSettings,
            onShiftLeft = { onShiftTab(-1, displayCount) },
            onShiftRight = { onShiftTab(1, displayCount) },
            modifier = modifier,
            isInputPanelVisible = isInputPanelVisible,
        )
        else -> SinglePanelLayout(
            aiOrder = aiOrder,
            selectedProvider = selectedProvider,
            getWebView = getWebView,
            onSelectProvider = onSelectProvider,
            onToggleInputPanel = onToggleInputPanel,
            onNavigateToSettings = onNavigateToSettings,
            modifier = modifier,
            isInputPanelVisible = isInputPanelVisible,
        )
    }
}

// 2/3/4パネル共通レイアウト（tabOffsetによるスライド対応）
@Composable
private fun MultiPanelLayout(
    allProviders: List<AIProvider>,
    visibleProviders: List<AIProvider>,
    displayCount: Int,
    canShiftLeft: Boolean,
    canShiftRight: Boolean,
    getWebView: (AIProvider) -> WebView,
    onToggleInputPanel: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onShiftLeft: () -> Unit,
    onShiftRight: () -> Unit,
    modifier: Modifier = Modifier,
    isInputPanelVisible: Boolean = false,
) {
    // ラベルはdisplayCountに応じてshortName/displayNameを使い分け
    val useShortName = displayCount >= 3

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // 一斉送信ボタン
            Box(
                modifier = Modifier.size(48.dp).clickable { onToggleInputPanel() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Edit, contentDescription = "一斉送信", modifier = Modifier.size(24.dp))
            }

            // 左矢印（非表示のAIがある場合）
            if (canShiftLeft) {
                IconButton(onClick = onShiftLeft, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "前のAI",
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Box(modifier = Modifier.size(32.dp))
            }

            // 表示中のAIラベル
            visibleProviders.forEach { provider ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (useShortName) provider.shortName else provider.displayName,
                        style = if (useShortName) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }

            // 右矢印（非表示のAIがある場合）
            if (canShiftRight) {
                IconButton(onClick = onShiftRight, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "次のAI",
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Box(modifier = Modifier.size(32.dp))
            }

            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "設定")
            }
        }

        // WebViewエリア: visibleProvidersのみVISIBLE、他はGONE
        // key(provider)でリスト位置ではなくprovider自体でComposableを同定し、
        // tabOffset変更時の不要なonDispose（→GONE）を防ぐ
        Row(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            visibleProviders.forEach { provider ->
                key(provider) {
                    AIWebViewPanel(
                        provider = provider,
                        getWebView = getWebView,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isInputPanelVisible = isInputPanelVisible,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SinglePanelLayout(
    aiOrder: List<AIProvider>,
    selectedProvider: AIProvider,
    getWebView: (AIProvider) -> WebView,
    onSelectProvider: (AIProvider) -> Unit,
    onToggleInputPanel: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isInputPanelVisible: Boolean = false,
) {
    val selectedIndex = aiOrder.indexOf(selectedProvider).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { aiOrder.size.coerceAtLeast(1) },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedProvider) {
        val idx = aiOrder.indexOf(selectedProvider)
        if (idx >= 0 && pagerState.currentPage != idx) {
            pagerState.animateScrollToPage(idx)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (aiOrder.isNotEmpty()) onSelectProvider(aiOrder[pagerState.currentPage])
    }

    val isCompact = LocalConfiguration.current.screenWidthDp < 400

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clickable { onToggleInputPanel() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Edit, contentDescription = "一斉送信", modifier = Modifier.size(24.dp))
            }
            TabRow(
                selectedTabIndex = pagerState.currentPage.coerceAtMost(aiOrder.lastIndex.coerceAtLeast(0)),
                modifier = Modifier.weight(1f),
            ) {
                aiOrder.forEachIndexed { index, provider ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                            onSelectProvider(provider)
                        },
                        text = {
                            Text(
                                text = if (isCompact) provider.shortName else provider.displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                            )
                        },
                    )
                }
            }
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "設定")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { aiOrder[it] },
                userScrollEnabled = false,
            ) { page ->
                AIWebViewPanel(
                    provider = aiOrder[page],
                    getWebView = getWebView,
                    modifier = Modifier.fillMaxSize(),
                    isInputPanelVisible = isInputPanelVisible,
                )
            }

            if (aiOrder.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 200.dp, start = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val prevIndex = (pagerState.currentPage - 1 + aiOrder.size) % aiOrder.size
                    val nextIndex = (pagerState.currentPage + 1) % aiOrder.size
                    IconButton(
                        onClick = { onSelectProvider(aiOrder[prevIndex]) },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.25f), MaterialTheme.shapes.small),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前のAI", tint = Color.White)
                    }
                    IconButton(
                        onClick = { onSelectProvider(aiOrder[nextIndex]) },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.25f), MaterialTheme.shapes.small),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "次のAI", tint = Color.White)
                    }
                }
            }
        }
    }
}
