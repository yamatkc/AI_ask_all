# Multi AI Workspace — 完全引き継ぎドキュメント

最終更新: 2026-06-15

---

## 1. アプリ概要

ChatGPT・Claude・Gemini・Grok の4つのAIをWebViewで同時表示し、同じプロンプトを一斉送信できるAndroidアプリ。

- **言語・フレームワーク**: Kotlin + Jetpack Compose
- **パッケージ名**: `com.example.multiaiworkspace`
- **プロジェクトパス**: `C:\Users\takas\AI_ask_all\MultiAIWorkspace`
- **対象端末**: Galaxy Z Fold（カバー画面＋展開画面両対応）

---

## 2. アーキテクチャ

```
MainActivity
└── MainScreen (Compose UI)
    ├── WorkspaceScreen
    │   ├── SinglePanelLayout  ← 表示数1: タブ切り替え（HorizontalPager）
    │   ├── TwoPanelLayout     ← 表示数2: 横2分割
    │   ├── ThreePanelLayout   ← 表示数3: 横3分割
    │   └── FourPanelLayout    ← 表示数4: 横4分割
    ├── InputPanel             ← 一斉送信パネル（下からスライドイン）
    └── ChildWebViewOverlay    ← OAuthポップアップ用オーバーレイ

MainViewModel
└── WebViewController
    ├── WebView × 4（ChatGPT / Claude / Gemini / Grok）シングルトン管理
    ├── ChatGPTAdapter / ClaudeAdapter / GeminiAdapter / GrokAdapter
    └── OAuthポップアップ管理（childWebView）

WorkspaceRepository（DataStore）
└── AI表示順・カバー表示数・展開表示数・初期AI・起動モード・有効AI設定を永続化
```

---

## 3. 主要ファイル一覧

| ファイル | 役割 |
|---------|------|
| `MainActivity.kt` | `enableEdgeToEdge()`・WebViewController初期化・ナビゲーション |
| `ui/main/MainScreen.kt` | Scaffold + Box。WorkspaceScreen・InputPanel・ChildWebViewOverlayを配置 |
| `ui/main/MainViewModel.kt` | WebViewController管理・一斉送信ロジック・startupReady制御 |
| `ui/workspace/WorkspaceScreen.kt` | 表示数に応じてSingle/Two/Three/FourPanelLayoutを切り替え |
| `ui/workspace/AIWebViewPanel.kt` | AndroidViewでWebViewを表示（clipToBounds付き） |
| `ui/input/InputPanel.kt` | 一斉送信パネル（テキスト・チェックボックス2列・送信ボタン） |
| `ui/settings/SettingsScreen.kt` | 起動モード・カバー表示数・展開表示数・使用AI・AI順序・初期AI設定 |
| `webview/WebViewController.kt` | WebView生成・ライフサイクル・OAuth処理・ファイルピッカー |
| `provider/ChatGPTAdapter.kt` | ChatGPTへのプロンプト送信JS |
| `provider/ClaudeAdapter.kt` | Claudeへのプロンプト送信JS |
| `provider/GeminiAdapter.kt` | Geminiへのプロンプト送信JS |
| `provider/GrokAdapter.kt` | Grokへのプロンプト送信JS（モバイルUA・useWideViewPort=false） |
| `data/model/AIProvider.kt` | enum: CHATGPT / CLAUDE / GEMINI / GROK |
| `data/model/Workspace.kt` | SendTarget（送信先チェックボックス状態） |
| `data/repository/WorkspaceRepository.kt` | DataStore永続化 |
| `AndroidManifest.xml` | `windowSoftInputMode="adjustResize"` 設定 |

---

## 4. 絶対に守るべき設計ルール

### WebView
- **WebViewは絶対にdestroyしない**・`onPause()`も`pauseTimers()`も呼ばない
- 1AI = 1WebViewシングルトン。非表示切り替えは `visibility = GONE/VISIBLE` のみ
- `AndroidView`の`update`ブロックで必ず `layoutParams = MATCH_PARENT` を設定すること
- ユーザーエージェントはデスクトップChromeを偽装（Grok以外）:
  `"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"`
- **Grokのみ** モバイルChromeを使用:
  `"Mozilla/5.0 (Linux; Android 14; SM-F946B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"`

### システムバー・キーボード
- `MainActivity`で`enableEdgeToEdge()`のみ使用
- **`WindowCompat.setDecorFitsSystemWindows(false)`は追加禁止**（`adjustResize`と競合してキーボード動作が壊れる）
- `AndroidManifest.xml`の`windowSoftInputMode="adjustResize"`は変更禁止
- `MainScreen`の`Box`に`systemBarsPadding().imePadding()`を付与済み

### タブスワイプ
- `HorizontalPager`は`userScrollEnabled = false`（タップのみ切り替え）
- WebViewのスクロールジェスチャと競合するため、スワイプ切り替えは無効化が必須

### 多パネルオーバーフロー対策
- `AIWebViewPanel`の`Box`と`AndroidView`両方に`Modifier.clipToBounds()`を付与
- Two/Three/FourPanelLayoutのコンテンツ`Row`にも`Modifier.clipToBounds()`を付与
- GrokAdapterで`useWideViewPort = false` + `loadWithOverviewMode = false`を設定（モバイルUAのためデスクトップ幅を要求しないように）

### startupMode レースコンディション対策
- `MainViewModel`の`_startupReady`フラグが`true`になるまでWebViewを生成しない
- `startupModeFlow.first()`で DataStore の最初の値が来るまで待機してから `startupReady = true`
- `MainScreen`で`if (startupReady)` の条件ガードで`WorkspaceScreen`を囲む

---

## 5. AIProvider enum（現在の定義）

```kotlin
enum class AIProvider(val displayName: String, val shortName: String, val url: String) {
    CHATGPT("ChatGPT", "GPT", "https://chatgpt.com"),
    CLAUDE("Claude", "Cla", "https://claude.ai"),
    GEMINI("Gemini", "Gem", "https://gemini.google.com"),
    GROK("Grok", "Grk", "https://grok.com");

    fun startupUrl(mode: String): String = when (this) {
        CHATGPT -> "https://chatgpt.com/"
        CLAUDE -> if (mode == "new") "https://claude.ai/new" else "https://claude.ai/"
        GEMINI -> "https://gemini.google.com/app"
        GROK -> "https://grok.com/"
    }
}
```

---

## 6. DataStore 永続化キーと仕様

| キー | 型 | デフォルト | 説明 |
|------|-----|-----------|------|
| `ai_order` | String（JSON） | 全AI順 | AI表示順リスト |
| `initial_ai` | String | CHATGPT | 初期表示AI |
| `display_count` | Int | 1 | （旧・未使用） |
| `cover_display_count` | Int | 1 | カバー画面の表示数（1〜2） |
| `expanded_display_count` | Int | 3 | 展開時の表示数（1〜4） |
| `enabled_providers` | String（JSON） | 全AI | 有効なAIのセット |
| `startup_mode` | String | "new" | "new" or "continue" |

---

## 7. 画面レイアウト詳細

### 表示数の決定ロジック
```kotlin
val isExpanded = screenWidthDp > 600  // Foldの展開判定
val displayCount = if (isExpanded) {
    expandedDisplayCount.coerceIn(1, aiOrder.size.coerceAtLeast(1))
} else {
    coverDisplayCount.coerceIn(1, aiOrder.size.coerceAtLeast(1))
}
```

### SinglePanelLayout（表示数=1）
タブバー行:
```
[✏️編集(48dp)] [GPT / Cla / Gem / Grk タブ(weight=1f)] [⚙️設定(48dp)]
```
- 画面幅 < 400dp: `shortName` 表示（GPT / Cla / Gem / Grk）
- 画面幅 ≥ 400dp: `displayName` 表示（ChatGPT / Claude / Gemini / Grok）

WebViewエリア下部の左右矢印（`Box`オーバーレイ）:
- `padding(bottom = 200.dp)` の位置
- 半透明黒背景（`alpha = 0.25f`）・白アイコン

### TwoPanelLayout（表示数=2）
```
[✏️] [ChatGPT] [Gemini] [⚙️]    ← displayName
[WebView 1/2]  [WebView 2/2]     ← Row + clipToBounds
```

### ThreePanelLayout（表示数=3）
```
[✏️] [GPT] [Cla] [Gem] [⚙️]    ← shortName
[WebView][WebView][WebView]       ← Row + clipToBounds
```

### FourPanelLayout（表示数=4）
```
[✏️] [GPT] [Cla] [Gem] [Grk] [⚙️]    ← shortName
[WebView][WebView][WebView][WebView]    ← Row + clipToBounds
```

### InputPanel（一斉送信パネル）
- `✏️`ボタンタップでBottomCenterからスライドイン
- テキスト入力欄（minLines=2, maxLines=4）
- チェックボックス: 画面幅 < 400dp の場合は2列表示（GPT/Grk ｜ Cla/Gem）
- 「閉じる」TextButton + 「送信」Button
- チェックボックスのラベルは `provider.shortName` を使用

### SettingsScreen（設定画面）
セクション順:
1. 起動時のチャット（新規 / 継続のラジオボタン）
2. カバー画面の表示数（1〜2のラジオボタン）
3. 展開時の表示数（1〜4のラジオボタン）
4. 使用するAI（4つのスイッチ、最低1つはON維持）
5. AI表示順（↑↓ボタンで並び替え）
6. 初期表示AI（ラジオボタン）

---

## 8. 各AIのプロンプト送信実装

### ChatGPT（`ChatGPTAdapter.kt`）
```
入力欄: #prompt-textarea（なければcontenteditable）
テキスト: innerText セッター + InputEvent('input')
送信: [data-testid="send-button"] → フォールバック button[aria-label="Send prompt"]
遅延: setTimeout 300ms
```

### Claude（`ClaudeAdapter.kt`）
```
入力欄: .ProseMirror（TipTap製）
テキスト: execCommand('insertText')
送信: button[aria-label="メッセージを送信"]（日本語、完全一致）
フォールバック: Enterキーイベント（keyCode:13）
遅延: setTimeout 500ms
```
> **重要**: `aria-label*="Send"` の部分一致は別ボタンに当たる。必ず完全一致の `"メッセージを送信"` を使うこと。

### Gemini（`GeminiAdapter.kt`）
```
入力欄: .ql-editor（Quill製）→ フォールバック [contenteditable="true"]
テキスト: execCommand('selectAll') → execCommand('insertText') + Event('input')
送信: aria-labelに"Send"か"送信"を含むボタンを走査してクリック
フォールバック: Enterキーイベント
遅延: setTimeout 600ms
```

### Grok（`GrokAdapter.kt`）
```
UA: モバイルChrome（SM-F946B）
useWideViewPort: false（多パネル時のはみ出し防止）
loadWithOverviewMode: false

入力欄: textarea.w-full
テキスト: HTMLTextAreaElement.prototype の nativeSetter 経由でセット + input/change イベント
送信:
  1. form内の button[type="submit"] をクリック
  2. formの最後のボタンをクリック
  3. 親要素を5階層上まで辿りbutton:not([disabled])の最後をクリック
  4. フォールバック: Enterキーイベント
遅延: setTimeout 400ms
```
> Grokへのテキスト入力は確認済み（result="ok"）。自動送信は動作するが不安定な場合あり。

---

## 9. OAuthポップアップ処理

`WebViewController.createChildWebView()` で処理:
1. `onCreateWindow` で子WebViewを作成
2. `onChildWebViewChanged` コールバックで `MainScreen` の `childWebView` stateを更新
3. `ChildWebViewOverlay` が全画面表示
4. 子WebViewの `onPageFinished` で親ドメインに戻ったことを検知 → オーバーレイを閉じる
5. `onCloseWindow` でもオーバーレイを閉じる

**Google認証回避**: `accounts.google.com` ページで以下JSを注入:
```js
if(!window.chrome){window.chrome={runtime:{},loadTimes:function(){},csi:function(){},app:{}};}
```

---

## 10. ファイルピッカー（WebView内個別操作用）

`WebViewController`:
- `filePathCallback` を保持
- `onShowFileChooser`: `launchFilePicker()` → `Intent.ACTION_GET_CONTENT`
- `onActivityResult`: 選択されたURIを `filePathCallback.onReceiveValue()` で返す
- `MainActivity.onActivityResult()` → `webViewController.onActivityResult()` に委譲

---

## 11. 断念した機能と理由

### 一斉送信時のファイル添付

| 方法 | 結果 | 失敗理由 |
|------|------|---------|
| 既存の添付ボタン（aria-label）をJS click() | メニューが開く | サイト側のUIが起動する |
| 非表示 input[type="file"] をJS click() | FileChooser未発火 | WebViewセキュリティ：ユーザー操作なしのinput.click()はブロック |
| ダミーinputを動的生成してclick() | BRIDGE_CLICKED返るが FileChooser未発火 | 同上 |
| Base64 → DataTransfer → input.files セット | FILE_SET:1返るが反映されない | ReactはDOMの直接書き換えを検知しない |
| ClipboardEvent('paste') でファイル渡し | PASTE_SENT:1返るが反映されない | ChatGPTがClipboardEventを無視 |

**結論**: WebViewのセキュリティ制限とReactの合成イベントシステムにより、JS経由での一斉ファイル添付は現時点で不可能。現運用: 各AIのWebView画面でユーザーが個別に手動操作する。

---

## 12. 過去に発生した問題と解決策

| 問題 | 原因 | 解決策 |
|------|------|--------|
| Claude白画面 | React SPAの初期化に約1秒かかる | `onPageFinished`でlayoutParams=MATCH_PARENT再設定 |
| キーボードでUIが隠れる | EdgeToEdge + setDecorFitsSystemWindowsの競合 | `setDecorFitsSystemWindows`を削除、`adjustResize`+`imePadding()`で対応 |
| タブスワイプとWebViewスクロール競合 | HorizontalPagerのデフォルト動作 | `userScrollEnabled = false` |
| Claude送信でメニューが開く | `aria-label*="Send"`で別ボタンに当たっていた | `aria-label="メッセージを送信"`（完全一致）に修正 |
| カバー画面でタブとボタンが重なる | 画面幅が狭い | shortName切り替え + 編集・設定ボタンをタブ行に統合 |
| 3分割表示で編集・設定ボタンが消える | TwoPanel/ThreePanelにボタンがなかった | 各レイアウトのヘッダー行にボタンを追加 |
| Gemini sendPrompt result=null | innerHTML方式でJS例外 | execCommand('insertText') + try-catch に変更 |
| `when`式が網羅的でないビルドエラー | GROK追加後に`isMainPage`と`val js`のwhen式にGROKブランチが不足 | 両方にGROKブランチを追加 |
| Grok多パネル時にはみ出す | `useWideViewPort=true`でモバイルUAなのに幅広レンダリング | GrokAdapterで`useWideViewPort=false`+`loadWithOverviewMode=false`を設定 |
| 起動モード設定が反映されない | DataStore読み込み前にWebViewが生成されていた | `startupModeFlow.first()`で待機→`_startupReady=true`のゲート実装 |

---

## 13. 残っているデバッグログ（不要なら削除可）

`WebViewController.kt` の `onPageFinished` 内:
- `[CLAUDE] contentHeight` / `[CLAUDE] body` ログ（Claudeデバッグ用）
- `[{provider}] file_attach` ログ（ファイル添付DOM調査用）
- `WebViewAuth` タグ全般（URL遷移・visibility確認用）

これらは機能に影響しないが、リリース前に削除を推奨。

---

## 14. 現在の完全なソースコード

### `data/model/AIProvider.kt`
```kotlin
package com.example.multiaiworkspace.data.model

enum class AIProvider(val displayName: String, val shortName: String, val url: String) {
    CHATGPT("ChatGPT", "GPT", "https://chatgpt.com"),
    CLAUDE("Claude", "Cla", "https://claude.ai"),
    GEMINI("Gemini", "Gem", "https://gemini.google.com"),
    GROK("Grok", "Grk", "https://grok.com");

    fun startupUrl(mode: String): String = when (this) {
        CHATGPT -> "https://chatgpt.com/"
        CLAUDE -> if (mode == "new") "https://claude.ai/new" else "https://claude.ai/"
        GEMINI -> "https://gemini.google.com/app"
        GROK -> "https://grok.com/"
    }
}
```

### `data/model/Workspace.kt`
```kotlin
package com.example.multiaiworkspace.data.model

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
```

### `data/repository/WorkspaceRepository.kt`
```kotlin
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

    private val defaultEnabled = setOf(
        AIProvider.CHATGPT.name, AIProvider.CLAUDE.name,
        AIProvider.GEMINI.name, AIProvider.GROK.name,
    )

    val aiOrderFlow: Flow<List<AIProvider>> = context.dataStore.data.map { prefs ->
        val json = prefs[AI_ORDER_KEY]
        if (json != null) {
            try {
                Json.decodeFromString<List<String>>(json).mapNotNull { name ->
                    AIProvider.entries.find { it.name == name }
                }.takeIf { it.size == AIProvider.entries.size } ?: AIProvider.entries.toList()
            } catch (e: Exception) { AIProvider.entries.toList() }
        } else AIProvider.entries.toList()
    }

    val initialAIFlow: Flow<AIProvider> = context.dataStore.data.map { prefs ->
        AIProvider.entries.find { it.name == prefs[INITIAL_AI_KEY] } ?: AIProvider.CHATGPT
    }

    val displayCountFlow: Flow<Int> = context.dataStore.data.map { prefs -> prefs[DISPLAY_COUNT_KEY] ?: 1 }
    val coverDisplayCountFlow: Flow<Int> = context.dataStore.data.map { prefs -> prefs[COVER_DISPLAY_COUNT_KEY] ?: 1 }
    val expandedDisplayCountFlow: Flow<Int> = context.dataStore.data.map { prefs -> prefs[EXPANDED_DISPLAY_COUNT_KEY] ?: 3 }

    val enabledProvidersFlow: Flow<Set<AIProvider>> = context.dataStore.data.map { prefs ->
        val json = prefs[ENABLED_PROVIDERS_KEY]
        val fallback = defaultEnabled.mapNotNull { n -> AIProvider.entries.find { it.name == n } }.toSet()
        if (json != null) {
            try {
                Json.decodeFromString<List<String>>(json)
                    .mapNotNull { n -> AIProvider.entries.find { it.name == n } }
                    .toSet().ifEmpty { fallback }
            } catch (e: Exception) { fallback }
        } else fallback
    }

    val startupModeFlow: Flow<String> = context.dataStore.data.map { prefs -> prefs[STARTUP_MODE_KEY] ?: "new" }

    suspend fun saveAIOrder(order: List<AIProvider>) {
        context.dataStore.edit { it[AI_ORDER_KEY] = Json.encodeToString(order.map { p -> p.name }) }
    }
    suspend fun saveInitialAI(provider: AIProvider) {
        context.dataStore.edit { it[INITIAL_AI_KEY] = provider.name }
    }
    suspend fun saveDisplayCount(count: Int) {
        context.dataStore.edit { it[DISPLAY_COUNT_KEY] = count.coerceIn(1, 3) }
    }
    suspend fun saveCoverDisplayCount(count: Int) {
        context.dataStore.edit { it[COVER_DISPLAY_COUNT_KEY] = count.coerceIn(1, 2) }
    }
    suspend fun saveExpandedDisplayCount(count: Int) {
        context.dataStore.edit { it[EXPANDED_DISPLAY_COUNT_KEY] = count.coerceIn(1, 4) }
    }
    suspend fun saveEnabledProviders(enabled: Set<AIProvider>) {
        context.dataStore.edit { it[ENABLED_PROVIDERS_KEY] = Json.encodeToString(enabled.map { p -> p.name }) }
    }
    suspend fun saveStartupMode(mode: String) {
        context.dataStore.edit { it[STARTUP_MODE_KEY] = mode }
    }
}
```

### `ui/main/MainViewModel.kt`
```kotlin
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
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedProvider: AIProvider = AIProvider.CHATGPT,
    val aiOrder: List<AIProvider> = AIProvider.entries.toList(),
    val showInputPanel: Boolean = false,
    val sendTarget: SendTarget = SendTarget(),
    val recreatedProviders: Set<AIProvider> = emptySet(),
    val childWebView: WebView? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val webViewController = WebViewController(application)
    private val repository = WorkspaceRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // DataStore から startupMode を読み込み完了したら true になるフラグ
    // これが true になるまで WorkspaceScreen（WebView生成）をガード
    private val _startupReady = MutableStateFlow(false)
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()

    val aiOrder: StateFlow<List<AIProvider>> = repository.aiOrderFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AIProvider.entries.toList())
    val initialAI: StateFlow<AIProvider> = repository.initialAIFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AIProvider.CHATGPT)
    val startupMode: StateFlow<String> = repository.startupModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "new")
    val enabledProviders: StateFlow<Set<AIProvider>> = repository.enabledProvidersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly,
            setOf(AIProvider.CHATGPT, AIProvider.CLAUDE, AIProvider.GEMINI, AIProvider.GROK))
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
        // startupModeをDataStoreから読み込んでからWebView生成を解禁
        viewModelScope.launch {
            val initialMode = repository.startupModeFlow.first()
            webViewController.startupMode = initialMode
            _startupReady.value = true
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
    fun closeChildWebView() {
        _uiState.value = _uiState.value.copy(childWebView = null)
    }
    fun getWebView(provider: AIProvider): WebView = webViewController.getWebView(provider)

    override fun onCleared() {
        super.onCleared()
        webViewController.flushCookies()
    }
}
```

### `ui/main/MainScreen.kt`
```kotlin
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
            if (startupReady) {
                WorkspaceScreen(
                    aiOrder = activeAiOrder,
                    coverDisplayCount = coverDisplayCount,
                    expandedDisplayCount = expandedDisplayCount,
                    selectedProvider = uiState.selectedProvider,
                    recreatedProviders = uiState.recreatedProviders,
                    getWebView = viewModel::getWebView,
                    onSelectProvider = viewModel::selectProvider,
                    onDismissRecreated = viewModel::dismissRecreatedNotice,
                    snackbarHostState = snackbarHostState,
                    onToggleInputPanel = { viewModel.setShowInputPanel(!uiState.showInputPanel) },
                    onNavigateToSettings = onNavigateToSettings,
                    modifier = Modifier.fillMaxSize(),
                    isInputPanelVisible = uiState.showInputPanel,
                )
            }
            InputPanel(
                visible = uiState.showInputPanel,
                sendTarget = uiState.sendTarget,
                onSendTargetChange = viewModel::updateSendTarget,
                onSend = viewModel::sendPrompt,
                onDismiss = { viewModel.setShowInputPanel(false) },
                modifier = Modifier.align(Alignment.BottomCenter).imePadding(),
            )
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
        AndroidView(factory = { childWebView }, modifier = Modifier.fillMaxSize())
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 8.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = "閉じる", tint = Color.Black)
        }
    }
}
```

### `MainActivity.kt`
```kotlin
package com.example.multiaiworkspace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.multiaiworkspace.ui.main.MainScreen
import com.example.multiaiworkspace.ui.main.MainViewModel
import com.example.multiaiworkspace.ui.settings.SettingsScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.webViewController.activity = this

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToSettings = { navController.navigate("settings") },
                            )
                        }
                        composable("settings") {
                            val aiOrder by viewModel.aiOrder.collectAsState()
                            val initialAI by viewModel.initialAI.collectAsState()
                            val enabledProviders by viewModel.enabledProviders.collectAsState()
                            val startupMode by viewModel.startupMode.collectAsState()
                            val coverDisplayCount by viewModel.coverDisplayCount.collectAsState()
                            val expandedDisplayCount by viewModel.expandedDisplayCount.collectAsState()
                            SettingsScreen(
                                aiOrder = aiOrder,
                                initialAI = initialAI,
                                enabledProviders = enabledProviders,
                                startupMode = startupMode,
                                coverDisplayCount = coverDisplayCount,
                                expandedDisplayCount = expandedDisplayCount,
                                onReorder = viewModel::reorderAI,
                                onInitialAIChange = viewModel::saveInitialAI,
                                onEnabledProvidersChange = viewModel::saveEnabledProviders,
                                onStartupModeChange = viewModel::saveStartupMode,
                                onCoverDisplayCountChange = viewModel::saveCoverDisplayCount,
                                onExpandedDisplayCountChange = viewModel::saveExpandedDisplayCount,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.webViewController.activity = this
    }

    override fun onStop() {
        super.onStop()
        viewModel.webViewController.activity = null
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.webViewController.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.webViewController.flushCookies()
    }
}
```

### `provider/GrokAdapter.kt`
```kotlin
package com.example.multiaiworkspace.provider

import android.util.Log
import android.webkit.WebView
import com.example.multiaiworkspace.data.model.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GrokAdapter : ProviderAdapter {
    override val provider = AIProvider.GROK
    override val url = AIProvider.GROK.url
    override fun supportsFileAttachment() = false

    override fun onWebViewCreated(webView: WebView) {
        webView.settings.apply {
            userAgentString =
                "Mozilla/5.0 (Linux; Android 14; SM-F946B) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"
            useWideViewPort = false      // 多パネル時のはみ出し防止
            loadWithOverviewMode = false
        }
    }

    override suspend fun sendPrompt(webView: WebView, prompt: String) {
        withContext(Dispatchers.Main) {
            val escaped = prompt.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            val js = """
                (function() {
                    var el = document.querySelector('textarea.w-full');
                    if (!el) return 'NO_INPUT';
                    var nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
                    if (nativeSetter && nativeSetter.set) { nativeSetter.set.call(el, `${'$'}escaped`); }
                    else { el.value = `${'$'}escaped`; }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    el.focus();
                    setTimeout(function() {
                        var form = el.closest('form');
                        if (form) {
                            var submitBtn = form.querySelector('button[type="submit"]');
                            if (submitBtn && !submitBtn.disabled) { submitBtn.click(); return; }
                            var allBtns = Array.from(form.querySelectorAll('button'));
                            if (allBtns.length > 0) { allBtns[allBtns.length - 1].click(); return; }
                        }
                        var parent = el.parentElement;
                        for (var i = 0; i < 5; i++) {
                            if (!parent) break;
                            var btns = Array.from(parent.querySelectorAll('button:not([disabled])'));
                            if (btns.length > 0) { btns[btns.length - 1].click(); return; }
                            parent = parent.parentElement;
                        }
                        el.dispatchEvent(new KeyboardEvent('keydown', {
                            bubbles: true, cancelable: true,
                            key: 'Enter', code: 'Enter', keyCode: 13, which: 13, shiftKey: false
                        }));
                    }, 400);
                    return 'ok';
                })()
            """.trimIndent()
            webView.evaluateJavascript(js) { result ->
                Log.d("WebViewDebug", "[GROK] sendPrompt result: $result")
            }
        }
    }
}
```

### `AndroidManifest.xml`（activity属性）
```xml
android:windowSoftInputMode="adjustResize"
```

---

## 開発運用ルール（2026年6月更新）

- メインの開発はGitHub上で行う方針に変更した。
- Claude Codeでの編集はブランチ（claude/〜）で行い、
  master へのマージ/PRを経てから反映する。
- Android Studio側は app/build.gradle.kts に追加した
  自動git pull設定（preBuild時にgitPullタスクが実行される）により、
  ビルドのたびに自動で最新コードを取得する。
  これにより、pull忘れによる古いコードでのビルド事故を防止する。
- 上記の自動pull設定があるため、ローカルで手動の git pull は基本的に不要。
  ただしブランチを切り替える場合は明示的に pull/checkout を行うこと。
