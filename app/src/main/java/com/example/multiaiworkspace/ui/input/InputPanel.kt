package com.example.multiaiworkspace.ui.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.multiaiworkspace.data.model.AIProvider
import com.example.multiaiworkspace.data.model.SendTarget

@Composable
fun InputPanel(
    visible: Boolean,
    sendTarget: SendTarget,
    onSendTargetChange: (SendTarget) -> Unit,
    onSend: (String, SendTarget) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        val screenWidthDp = LocalConfiguration.current.screenWidthDp
        val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
        val isCompact = screenWidthDp < 400

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeightDp * 0.6f)
                .imePadding(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            var text by remember { mutableStateOf("") }

            Column(modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("プロンプトを入力...") },
                    minLines = 2,
                    maxLines = 4,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // チェックボックス群: 狭い画面(< 400dp)は2列、広い画面は横並び
                    if (isCompact) {
                        val entries = AIProvider.entries
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                entries.take(2).forEach { provider ->
                                    CheckboxItem(
                                        provider = provider,
                                        sendTarget = sendTarget,
                                        onSendTargetChange = onSendTargetChange,
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                entries.drop(2).forEach { provider ->
                                    CheckboxItem(
                                        provider = provider,
                                        sendTarget = sendTarget,
                                        onSendTargetChange = onSendTargetChange,
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            AIProvider.entries.forEach { provider ->
                                CheckboxItem(
                                    provider = provider,
                                    sendTarget = sendTarget,
                                    onSendTargetChange = onSendTargetChange,
                                )
                            }
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("閉じる")
                    }
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSend(text.trim(), sendTarget)
                                text = ""
                            }
                        },
                        enabled = text.isNotBlank() &&
                            (sendTarget.chatGPT || sendTarget.claude || sendTarget.gemini || sendTarget.grok),
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "送信")
                        Text("送信", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckboxItem(
    provider: AIProvider,
    sendTarget: SendTarget,
    onSendTargetChange: (SendTarget) -> Unit,
) {
    val checked = sendTarget.isSelected(provider)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = { isChecked ->
                val updated = when (provider) {
                    AIProvider.CHATGPT -> sendTarget.copy(chatGPT = isChecked)
                    AIProvider.CLAUDE -> sendTarget.copy(claude = isChecked)
                    AIProvider.GEMINI -> sendTarget.copy(gemini = isChecked)
                    AIProvider.GROK -> sendTarget.copy(grok = isChecked)
                }
                onSendTargetChange(updated)
            }
        )
        Text(
            text = provider.shortName,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
