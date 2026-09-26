package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.data.dictionary.DictionaryEntry
import com.example.data.entity.Deck
import com.example.ui.components.AddFolderDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordLookupScreen(
    decks: List<Deck>,
    onBack: () -> Unit,
    onLookup: suspend (String) -> Result<DictionaryEntry>,
    onAddToDeck: (DictionaryEntry, Long, (Boolean) -> Unit) -> Unit,
    onCreateFolderAndAdd: (String, String, String, String, DictionaryEntry, (Boolean) -> Unit) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<DictionaryEntry?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showDeckPicker by remember { mutableStateOf(false) }
    var showCreateFolder by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun search() {
        if (query.isBlank() || loading) return
        keyboard?.hide()
        loading = true
        error = null
        result = null
        scope.launch {
            onLookup(query).onSuccess { result = it }
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
                            onClick = {
                                result?.let { entry ->
                                    onAddToDeck(entry, deck.id) { if (it) showDeckPicker = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null)
                            Text("  ${deck.category} · ${deck.name}", modifier = Modifier.weight(1f))
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { showDeckPicker = false; showCreateFolder = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("新增資料夾")
                        }
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
                    onCreateFolderAndAdd(course, name, description, color, entry) {
                        if (it) showCreateFolder = false
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜尋單字", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("英文單字或短語") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = ::search, enabled = query.isNotBlank() && !loading) {
                            if (loading) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            else Icon(Icons.Default.Search, contentDescription = "搜尋")
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            result?.let { entry ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(entry.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                if (entry.partOfSpeech.isNotBlank()) Text(entry.partOfSpeech, color = MaterialTheme.colorScheme.primary)
                            }
                            if (entry.phonetic.isNotBlank()) Text(entry.phonetic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entry.definition, style = MaterialTheme.typography.titleMedium)
                            if (entry.exampleSentence.isNotBlank()) Text(entry.exampleSentence)
                            if (entry.exampleTranslation.isNotBlank()) Text(entry.exampleTranslation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("資料來源：Google 翻譯網頁", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { showDeckPicker = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("加入資料夾")
                            }
                        }
                    }
                }
            }
        }
    }
}
