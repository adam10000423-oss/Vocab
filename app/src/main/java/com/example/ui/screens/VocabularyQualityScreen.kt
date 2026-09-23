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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard

private data class QualityFinding(
    val title: String,
    val detail: String,
    val card: Flashcard? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyQualityScreen(
    cards: List<Flashcard>,
    decks: List<Deck>,
    onEditCard: (Flashcard) -> Unit,
    onRunAiAudit: () -> Unit,
    onBack: () -> Unit
) {
    val deckNames = remember(decks) { decks.associate { it.id to it.name } }
    val findings = remember(cards, deckNames) { buildQualityFindings(cards, deckNames) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("單字資料品質", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("已檢查 ${cards.size} 張單字卡", fontWeight = FontWeight.Bold)
                        Text(
                            if (findings.isEmpty()) "沒有發現可由本機規則確認的問題。"
                            else "找到 ${findings.size} 個需要確認的項目。",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(onClick = onRunAiAudit, modifier = Modifier.fillMaxWidth(), enabled = cards.isNotEmpty()) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Text("用 AI 檢查解釋與疑似拼字")
                        }
                    }
                }
            }
            item {
                Text(
                    "本機會檢查重複、欄位缺漏與詞性格式；中文解釋正確性和拼字建議必須由 AI 分析並由你確認。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (findings.isEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("目前資料完整", modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                items(findings) { finding ->
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(finding.title, fontWeight = FontWeight.Bold)
                                Text(finding.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            finding.card?.let { card ->
                                IconButton(onClick = { onEditCard(card) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "編輯 ${card.word}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun buildQualityFindings(cards: List<Flashcard>, deckNames: Map<Long, String>): List<QualityFinding> {
    val findings = mutableListOf<QualityFinding>()
    cards.groupBy { it.normalizedWord }.filter { (word, group) -> word.isNotBlank() && group.size > 1 }
        .forEach { (_, group) ->
            findings += QualityFinding(
                title = "重複單字：${group.first().word}",
                detail = group.joinToString("、") { deckNames[it.deckId] ?: "未知資料夾" },
                card = group.first()
            )
        }
    val acceptedPos = Regex("(?i)^(n|v|vt|vi|adj|adv|prep|pron|conj|interj|det|aux|num|phr|abbr)\\.?([/、, ]+(n|v|vt|vi|adj|adv|prep|pron|conj|interj|det|aux|num|phr|abbr)\\.?)*(\\s*\\[[^]]+])?$")
    cards.forEach { card ->
        val missing = buildList {
            if (card.phonetic.isBlank()) add("音標")
            if (card.partOfSpeech.isBlank()) add("詞性")
            if (card.definition.isBlank()) add("中文解釋")
            if (card.exampleSentence.isBlank()) add("英文例句")
            if (card.exampleTranslation.isBlank()) add("例句翻譯")
        }
        if (missing.isNotEmpty()) {
            findings += QualityFinding("資料不完整：${card.word}", "缺少${missing.joinToString("、")}", card)
        } else if (!acceptedPos.matches(card.partOfSpeech.trim())) {
            findings += QualityFinding("詞性格式需確認：${card.word}", "目前內容：${card.partOfSpeech}", card)
        }
    }
    return findings
}
