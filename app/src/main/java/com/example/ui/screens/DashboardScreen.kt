package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.entity.StudyLog
import com.example.data.stats.LearningStats
import com.example.data.stats.LearningProgress
import com.example.ui.components.StatisticsChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    decks: List<Deck>,
    allCards: List<Flashcard>,
    dueCards: List<Flashcard>,
    totalCount: Int,
    masteredCount: Int,
    dueCount: Int,
    logs: List<StudyLog>,
    dailyGoalCards: Int,
    selectedDeckId: Long?,
    onSelectDeck: (Long?) -> Unit,
    onAddFolder: (courseName: String, folderName: String, description: String, colorHex: String) -> Unit,
    onStartReview: () -> Unit,
    onOpenAddCard: () -> Unit,
    onOpenPhotoOcr: () -> Unit,
    onOpenQuizGames: (Long?) -> Unit,
    onOpenCardList: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExternalImport: () -> Unit,
    onOpenAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var pendingFolderAction by remember { mutableStateOf<String?>(null) } // "REVIEW" or "GAME"
    val courses = remember(decks) { decks.map { it.category }.distinct().ifEmpty { listOf("通用課程") } }
    var selectedCourseFilter by remember { mutableStateOf<String?>(null) }
    val streak = remember(logs) { LearningStats.calculateStreak(logs) }
    val progress = remember(logs, dailyGoalCards) {
        LearningProgress.calculate(logs, dailyGoalCards)
    }

    // Folder Selection Dialog for Review or Games
    if (pendingFolderAction != null) {
        val actionType = pendingFolderAction!!
        AlertDialog(
            onDismissRequest = { pendingFolderAction = null },
            title = {
                Text(
                    text = if (actionType == "REVIEW") "選擇要複習的資料夾" else "選擇要測驗的資料夾",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                ) {
                    // Option 1: All Folders
                    item {
                        Card(
                            onClick = {
                                onSelectDeck(null)
                                pendingFolderAction = null
                                if (actionType == "REVIEW") onStartReview() else onOpenQuizGames(null)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "全站所有單字卡",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "包含所有資料夾",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Text(
                                    text = "$totalCount 張",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                    }

                    // Option 2..N: Specific Decks
                    items(decks) { deck ->
                        val deckCardCount = allCards.count { it.deckId == deck.id }
                        val deckColor = try {
                            Color(android.graphics.Color.parseColor(deck.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        Card(
                            onClick = {
                                if (deckCardCount > 0) {
                                    onSelectDeck(deck.id)
                                    pendingFolderAction = null
                                    if (actionType == "REVIEW") onStartReview() else onOpenQuizGames(deck.id)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = deckColor.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(deck.name, fontWeight = FontWeight.Bold)
                                    Text(deck.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = deckColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (deckCardCount == 0) "空" else "$deckCardCount 張",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingFolderAction = null }) {
                    Text("取消", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddFolderDialog) {
        com.example.ui.components.AddFolderDialog(
            existingCourses = courses,
            defaultCourse = selectedCourseFilter,
            onDismiss = { showAddFolderDialog = false },
            onConfirm = { course, folder, desc, color ->
                onAddFolder(course, folder, desc, color)
                selectedCourseFilter = course
                showAddFolderDialog = false
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                expandedHeight = 48.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(
                                text = "Vocab",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAssistant) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Vocab AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "設定",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Keep today's status, goal and streak in one calm summary card.
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("今日學習", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("連續 ${streak.current} 天", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            if (progress.isGoalReached) "今日目標已完成"
                            else if (dueCount > 0) "待複習 $dueCount 張"
                            else "還差 ${progress.remainingCards} 張完成目標",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { progress.goalFraction },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "${progress.completedCards} / ${progress.goalCards} 張 · ${progress.dailyXp} XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Primary SRS Review Action Button
            item {
                Button(
                    onClick = { pendingFolderAction = "REVIEW" },
                    enabled = totalCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_review_button")
                ) {
                    Text(
                        text = if (dueCount > 0) "開始複習（$dueCount）" else "開始學習",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickToolCard(
                        title = "掃描文件",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onOpenPhotoOcr,
                        modifier = Modifier.weight(1f).testTag("tool_photo_ocr")
                    )

                    QuickToolCard(
                        title = "測驗",
                        color = MaterialTheme.colorScheme.tertiary,
                        onClick = { pendingFolderAction = "GAME" },
                        enabled = totalCount > 0,
                        modifier = Modifier.weight(1f).testTag("tool_quiz_games")
                    )
                }
            }
            item {
                QuickToolCard(
                    title = "匯入單字集",
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = onOpenExternalImport,
                    modifier = Modifier.fillMaxWidth().testTag("tool_external_import")
                )
            }

            // Course & Folder Management Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "我的單字庫",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        androidx.compose.material3.TextButton(
                            onClick = { showAddFolderDialog = true },
                            modifier = Modifier.testTag("dashboard_add_folder_button")
                        ) {
                            Text("新增", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Course Chips Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Surface(
                                onClick = { selectedCourseFilter = null },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedCourseFilter == null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedCourseFilter == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                ),
                                contentColor = if (selectedCourseFilter == null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ) {
                                Text(
                                    text = "全部",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }

                        items(courses) { course ->
                            val isSelected = selectedCourseFilter == course
                            Surface(
                                onClick = { selectedCourseFilter = course },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                                ),
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            ) {
                                Text(
                                    text = course,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Folder Cards Display
                    val filteredDecks = if (selectedCourseFilter == null) decks else decks.filter { it.category == selectedCourseFilter }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            DeckChip(
                                title = "所有單字",
                                count = totalCount,
                                isSelected = selectedDeckId == null,
                                onClick = {
                                    onSelectDeck(null)
                                    onOpenCardList()
                                }
                            )
                        }

                        items(filteredDecks) { deck ->
                            val isSelected = selectedDeckId == deck.id
                            val deckColor = try {
                                Color(android.graphics.Color.parseColor(deck.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Card(
                                onClick = {
                                    onSelectDeck(deck.id)
                                    onOpenCardList()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        deckColor.copy(alpha = 0.12f)
                                    }
                                ),
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(95.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = deck.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else deckColor,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = deck.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Statistics Chart & Progress Section
            item {
                StatisticsChart(
                    totalCards = totalCount,
                    masteredCards = masteredCount,
                    dueCards = dueCount,
                    logs = logs
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun QuickToolCard(
    title: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = if (enabled) 0.12f else 0.05f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (enabled) color else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun DeckChip(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            if (count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "($count)",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
