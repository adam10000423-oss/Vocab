package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.ui.components.CalmEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyHubScreen(
    decks: List<Deck>,
    allCards: List<Flashcard>,
    onStartReviewForDeck: (Long) -> Unit,
    onStartReviewForCourse: (String) -> Unit,
    onStartReviewForDecks: (Set<Long>) -> Unit = {},
    onManageDeck: (Long) -> Unit = {},
    onQuizDeck: (Long) -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val courses = remember(decks) { decks.map { it.category }.distinct() }
    var selectedCourse by remember(courses) { mutableStateOf<String?>(null) }
    val filteredDecks = remember(decks, selectedCourse) {
        selectedCourse?.let { course -> decks.filter { it.category == course } } ?: decks
    }
    val courseCardCount = remember(allCards, filteredDecks) {
        val ids = filteredDecks.map { it.id }.toSet()
        allCards.count { it.deckId in ids }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("學習", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAssistant) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Vocab AI")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (decks.isEmpty()) {
                item {
                    CalmEmptyState(
                        icon = Icons.Default.Folder,
                        title = "還沒有資料夾",
                        message = "先建立資料夾，再開始學習。"
                    )
                }
            } else {
                item {
                    Text(
                        text = "選擇課程",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            val isSelected = selectedCourse == null
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCourse = null }
                            ) {
                                Text(
                                    text = "全部資料夾 (${decks.size})",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                        items(courses) { course ->
                            val isSelected = selectedCourse == course
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedCourse = course }
                            ) {
                                Text(
                                    text = "$course (${decks.count { it.category == course }})",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            selectedCourse?.let(onStartReviewForCourse)
                                ?: onStartReviewForDecks(filteredDecks.map { it.id }.toSet())
                        },
                        enabled = courseCardCount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Style, contentDescription = null)
                        Text("複習全部 · $courseCardCount")
                    }
                }
                items(filteredDecks) { deck ->
                    val deckCards = allCards.filter { it.deckId == deck.id }
                    val totalCards = deckCards.size
                    val masteredCount = deckCards.count { it.isMastered }
                    val dueCount = deckCards.count { it.nextReviewTimestamp <= System.currentTimeMillis() && !it.isMastered && !it.isSuspended }
                    val progressRatio = if (totalCards > 0) masteredCount.toFloat() / totalCards.toFloat() else 0f

                    val deckColor = try {
                        Color(android.graphics.Color.parseColor(deck.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(deckColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = deck.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (dueCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = when {
                                            totalCards == 0 -> "沒有單字"
                                            dueCount > 0 -> "待複習 $dueCount"
                                            masteredCount == totalCards -> "已完成"
                                            else -> "無到期"
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (dueCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Progress indicator bar
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("精通進度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("${(progressRatio * 100).toInt()}% ($masteredCount/$totalCards)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = deckColor,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onManageDeck(deck.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("管理", fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { onStartReviewForDeck(deck.id) },
                                    enabled = totalCards > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("start_review_deck_${deck.id}")
                                ) { Text("學習", fontWeight = FontWeight.Bold) }
                                OutlinedButton(
                                    onClick = { onQuizDeck(deck.id) },
                                    enabled = totalCards > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) { Text("測驗", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
