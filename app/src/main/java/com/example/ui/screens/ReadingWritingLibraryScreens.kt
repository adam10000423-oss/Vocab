package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.assistant.AssistantMessage
import com.example.data.entity.Deck
import com.example.data.entity.GrammarWritingRecord
import com.example.ui.components.CalmEmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReadingLibraryScreen(
    decks: List<Deck>,
    articleMessages: List<AssistantMessage>,
    onGenerate: (Deck) -> Unit,
    onImport: (String) -> Unit,
    onOpenArticle: (AssistantMessage) -> Unit
) {
    var chooseDeck by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    if (chooseDeck) AlertDialog(
        onDismissRequest = { chooseDeck = false },
        title = { Text("用哪個資料夾產生文章？") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(decks) { deck ->
                    Surface(
                        onClick = { chooseDeck = false; onGenerate(deck) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(deck.name, fontWeight = FontWeight.Bold)
                            Text(deck.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = { chooseDeck = false }) { Text("取消") } }
    )
    if (showImport) AlertDialog(
        onDismissRequest = { showImport = false },
        title = { Text("匯入文章") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("貼上英文文章或公開網址。AI 只整理段落與學習資訊，不會改寫原文。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(importText, { importText = it }, minLines = 6, label = { Text("文章或網址") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(enabled = importText.isNotBlank(), onClick = { val value = importText.trim(); showImport = false; importText = ""; onImport(value) }) { Text("預覽文章") } },
        dismissButton = { TextButton(onClick = { showImport = false }) { Text("取消") } }
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("文章閱讀", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("把正在讀的文章放在最前面，點文字即可查詢與建立單字卡。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        articleMessages.firstOrNull()?.let { recent ->
            item {
                Card(
                    onClick = { onOpenArticle(recent) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("繼續閱讀", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(articleTitle(recent), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("開啟文章並繼續學習目標單字", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { chooseDeck = true }, enabled = decks.isNotEmpty(), modifier = Modifier.weight(1f)) { Icon(Icons.Default.AutoAwesome, null); Text("AI 產生") }
                OutlinedButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.FileOpen, null); Text("匯入文章") }
            }
        }
        item { Text("我的文章", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (articleMessages.isEmpty()) item { CalmEmptyState(Icons.Default.Article, "還沒有文章", "可由單字資料夾產生文章，或匯入自己的內容。") }
        items(articleMessages, key = { it.id }) { message ->
            Card(
                onClick = { onOpenArticle(message) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(articleTitle(message), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(message.content.replace('\n', ' '), maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateText(message.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun WritingLibraryScreen(
    records: List<GrammarWritingRecord>,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onOpen: (GrammarWritingRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("作文練習", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("保留原稿、修改版本與逐項說明，讓每次修改都能變成弱點練習。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        records.firstOrNull()?.let { recent ->
            item {
                Card(
                    onClick = { onOpen(recent) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("繼續修改", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(recent.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${recent.originalText.split(Regex("\\s+")).count { it.isNotBlank() }} 字 · ${issueCount(recent)} 項批改", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onNew, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Text("自由寫作") }
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Icon(Icons.Default.CameraAlt, null); Text("匯入作文") }
            }
        }
        item { Text("作文紀錄", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (records.isEmpty()) item { CalmEmptyState(Icons.Default.EditNote, "還沒有作文", "新增作文或拍照匯入，AI 批改前會先讓你確認辨識文字。") }
        items(records, key = { it.id }) { record ->
            Card(
                onClick = { onOpen(record) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(record.originalText.replace('\n', ' '), maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(record.course, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text("${issueCount(record)} 項批改", style = MaterialTheme.typography.labelMedium)
                        Text(dateText(record.updatedAt), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

private fun articleTitle(message: AssistantMessage): String = message.content.lineSequence()
    .firstOrNull { it.isNotBlank() }
    ?.removePrefix("#")?.trim()?.take(70)
    ?.ifBlank { null }
    ?: "互動閱讀文章"

private fun issueCount(record: GrammarWritingRecord): Int = runCatching {
    org.json.JSONArray(record.issuesJson).length()
}.getOrDefault(0)

private fun dateText(time: Long): String = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date(time))
