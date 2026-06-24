package com.example.multiaiworkspace.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.multiaiworkspace.data.model.AIProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    aiOrder: List<AIProvider>,
    initialAI: AIProvider,
    enabledProviders: Set<AIProvider>,
    startupMode: String,
    coverDisplayCount: Int,
    expandedDisplayCount: Int,
    onReorder: (List<AIProvider>) -> Unit,
    onInitialAIChange: (AIProvider) -> Unit,
    onEnabledProvidersChange: (Set<AIProvider>) -> Unit,
    onStartupModeChange: (String) -> Unit,
    onCoverDisplayCountChange: (Int) -> Unit,
    onExpandedDisplayCountChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                SectionTitle("起動時のチャット")
                listOf(
                    "new" to "新しいチャットを開く",
                    "continue" to "前回のチャットを継続する",
                ).forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStartupModeChange(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = startupMode == mode,
                            onClick = { onStartupModeChange(mode) }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            item {
                SectionTitle("カバー画面の表示数")
                (1..2).forEach { count ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCoverDisplayCountChange(count) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = coverDisplayCount == count,
                            onClick = { onCoverDisplayCountChange(count) }
                        )
                        Text("${count}画面", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            item {
                SectionTitle("展開時の表示数")
                (1..4).forEach { count ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExpandedDisplayCountChange(count) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = expandedDisplayCount == count,
                            onClick = { onExpandedDisplayCountChange(count) }
                        )
                        Text("${count}画面", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            item {
                SectionTitle("使用するAI")
                AIProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(provider.displayName, modifier = Modifier.padding(start = 8.dp))
                        Switch(
                            checked = provider in enabledProviders,
                            onCheckedChange = { isEnabled ->
                                val updated = if (isEnabled) {
                                    enabledProviders + provider
                                } else {
                                    // 最低1つはONを維持
                                    if (enabledProviders.size > 1) enabledProviders - provider
                                    else enabledProviders
                                }
                                onEnabledProvidersChange(updated)
                            }
                        )
                    }
                }
            }

            item {
                SectionTitle("AI表示順")
                AIOrderEditor(
                    order = aiOrder,
                    onReorder = onReorder,
                )
            }

            item {
                SectionTitle("初期表示AI")
                AIProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onInitialAIChange(provider) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = initialAI == provider,
                            onClick = { onInitialAIChange(provider) }
                        )
                        Text(provider.displayName, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun AIOrderEditor(
    order: List<AIProvider>,
    onReorder: (List<AIProvider>) -> Unit,
) {
    // Simple up/down button reorder (drag-and-drop requires complex gesture handling)
    var localOrder by remember(order) { mutableStateOf(order) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        localOrder.forEachIndexed { index, provider ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.DragHandle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = provider.displayName,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                    )
                    Column {
                        if (index > 0) {
                            Text(
                                text = "↑",
                                modifier = Modifier
                                    .clickable {
                                        val newOrder = localOrder.toMutableList()
                                        val tmp = newOrder[index]
                                        newOrder[index] = newOrder[index - 1]
                                        newOrder[index - 1] = tmp
                                        localOrder = newOrder
                                        onReorder(newOrder)
                                    }
                                    .padding(4.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        if (index < localOrder.lastIndex) {
                            Text(
                                text = "↓",
                                modifier = Modifier
                                    .clickable {
                                        val newOrder = localOrder.toMutableList()
                                        val tmp = newOrder[index]
                                        newOrder[index] = newOrder[index + 1]
                                        newOrder[index + 1] = tmp
                                        localOrder = newOrder
                                        onReorder(newOrder)
                                    }
                                    .padding(4.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
