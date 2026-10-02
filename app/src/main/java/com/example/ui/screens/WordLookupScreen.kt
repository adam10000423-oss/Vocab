package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import com.example.ui.components.ModernAlertDialog as AlertDialog
import com.example.ui.components.ModernButton as Button
import com.example.ui.components.ModernCard as Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.ui.components.ModernOutlinedButton as OutlinedButton
import com.example.ui.components.ModernTextField as OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.dictionary.DictionaryEntry
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.components.AddFolderDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLookupScreen(
    decks: List<Deck>,
    cards: List<Flashcard>,
    onBack: () -> Unit,
    onLookup: suspend (String) -> Result<DictionaryEntry>,
    onAiComplete: suspend (String) -> DictionaryEntry?,
    onOpenLocalCard: (Flashcard) -> Unit,
    onAddToDeck: (DictionaryEntry, Long, (Boolean) -> Unit) -> Unit,
    onCreateFolderAndAdd: (String, String, String, String, DictionaryEntry, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("word_lookup", Context.MODE_PRIVATE) }
    var history by remember { mutableStateOf(loadLookupHistory(preferences.getString("history", "[]").orEmpty())) }
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<DictionaryEntry?>(null) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var aiLoading by remember { mutableStateOf(false) }
    var showDeckPicker by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(180)
        searchFocusRequester.requestFocus()
        keyboard?.show()
    }
    val localMatches = remember(query, cards, searched) {
        if (!searched || query.isBlank()) emptyList() else cards.filter {
            it.word.contains(query.trim(), true) || it.definition.contains(query.trim(), true)
        }.distinctBy { it.normalizedWord }.take(20)
    }

    fun saveHistory(value: String) {
        history = (listOf(value.trim()) + history.filterNot { it.equals(value.trim(), true) }).take(20)
        preferences.edit().putString("history", JSONArray(history).toString()).apply()
    }
    fun search(value: String = query) {
        val normalized = value.trim()
        if (normalized.isBlank() || loading) return
        query = normalized
        keyboard?.hide()
        loading = true
        searched = true
        error = null
        result = null
        saveHistory(normalized)
        scope.launch {
            onLookup(normalized).onSuccess { result = it }
                .onFailure { error = it.message ?: "查詢失敗，請稍後再試" }
            loading = false
        }
    }

    if (showDeckPicker) {
        AlertDialog(
            onDismissRequest = { showDeckPicker = false },
            title = { Text("加入資料夾") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(decks, key = { it.id }) { deck ->
                        TextButton(
                            onClick = { result?.let { onAddToDeck(it, deck.id) { ok -> if (ok) showDeckPicker = false } } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, null)
                            Text("  ${deck.category} · ${deck.name}", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { showDeckPicker = false; showCreateFolder = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Icon(Icons.Default.Add, null); Text("新增資料夾") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDeckPicker = false }) { Text("取消") } }
        )
    }
    if (showCreateFolder) {
        AddFolderDialog(
            existingCourses = decks.map { it.category },
            onDismiss = { showCreateFolder = false },
            onConfirm = { course, name, description, color ->
                result?.let { entry ->
                    onCreateFolderAndAdd(course, name, description, color, entry) { if (it) showCreateFolder = false }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it; searched = false },
                        placeholder = { Text("搜尋英文單字或中文解釋") },
                        trailingIcon = {
                            IconButton(onClick = { if (query.isNotBlank()) search() }) {
                                if (loading) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                                else Icon(Icons.Default.Search, "搜尋")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester)
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(top = 6.dp).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!searched) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("搜尋紀錄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (history.isNotEmpty()) TextButton(onClick = {
                            history = emptyList(); preferences.edit().remove("history").apply()
                        }) { Text("全部清除") }
                    }
                }
                items(history) { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { search(item) }) { Icon(Icons.Default.History, null) }
                        TextButton(onClick = { search(item) }, modifier = Modifier.weight(1f)) {
                            Text(item, modifier = Modifier.weight(1f), maxLines = 1)
                            Icon(Icons.Default.NorthWest, null)
                        }
                        IconButton(onClick = {
                            history = history - item
                            preferences.edit().putString("history", JSONArray(history).toString()).apply()
                        }) { Icon(Icons.Default.Close, "刪除") }
                    }
                }
            } else {
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (localMatches.isNotEmpty()) {
                    item { Text("我的單字庫", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(localMatches, key = { it.id }) { card ->
                        LocalDictionaryResult(card = card, onClick = { onOpenLocalCard(card) })
                    }
                }
                result?.let { entry ->
                    item { Text(result?.category ?: "線上字典", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    item {
                        DictionaryResultCard(
                            entry = entry,
                            aiLoading = aiLoading,
                            onAiComplete = {
                                aiLoading = true
                                scope.launch {
                                    runCatching { onAiComplete(entry.word) }.onSuccess { enriched ->
                                        if (enriched != null) result = enriched
                                    }.onFailure { error = "AI 補齊失敗：${it.message ?: "請檢查 API 設定"}" }
                                    aiLoading = false
                                }
                            },
                            onAdd = { showDeckPicker = true }
                        )
                    }
                }
                if (!loading && result == null && localMatches.isEmpty() && error == null) {
                    item { Text("找不到符合的結果", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun LocalDictionaryResult(card: Flashcard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)),
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(card.word, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(listOf(card.partOfSpeech, card.phonetic).filter(String::isNotBlank).joinToString("  "), color = MaterialTheme.colorScheme.primary)
            Text(card.definition)
            if (card.exampleSentence.isNotBlank()) Text(card.exampleSentence, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DictionaryResultCard(entry: DictionaryEntry, aiLoading: Boolean, onAiComplete: () -> Unit, onAdd: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(entry.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (entry.partOfSpeech.isNotBlank()) Text(entry.partOfSpeech, color = MaterialTheme.colorScheme.primary)
            }
            if (entry.phonetic.isNotBlank()) Text(entry.phonetic, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(entry.definition, style = MaterialTheme.typography.titleMedium)
            if (entry.exampleSentence.isNotBlank()) Text(entry.exampleSentence)
            if (entry.exampleTranslation.isNotBlank()) Text(entry.exampleTranslation, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("資料來源：${entry.category}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAiComplete, enabled = !aiLoading, modifier = Modifier.weight(1f)) {
                    if (aiLoading) CircularProgressIndicator(modifier = Modifier.padding(4.dp)) else Icon(Icons.Default.AutoAwesome, null)
                    Text(if (aiLoading) "補齊中" else "AI 補齊")
                }
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text("加入資料夾") }
            }
        }
    }
}

private fun loadLookupHistory(raw: String): List<String> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
}.getOrDefault(emptyList())
