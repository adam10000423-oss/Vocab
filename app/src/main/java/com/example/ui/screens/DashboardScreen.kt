package com.example.ui.screens
import androidx.compose.foundation.layout.PaddingValues
import com.example.ui.components.ModernTonalButton as FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CollectionsBookmark
import com.example.ui.components.ModernListRow
import com.example.ui.components.CalmSectionHeader

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
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import com.example.ui.components.ModernAlertDialog as AlertDialog
import com.example.ui.components.ModernButton as Button
import androidx.compose.material3.ButtonDefaults
import com.example.ui.components.ModernCard as Card
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
import java.time.LocalDate

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
    makeUpDates: Set<LocalDate>,
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
    updateAvailable: Boolean = false,
    showTopBar: Boolean = true,
    onOpenExternalImport: () -> Unit,
    onStartInteractiveReading: (Long) -> Unit,
    onOpenStudyCalendar: () -> Unit,
    onOpenQualityCheck: () -> Unit,
    onOpenAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var pendingFolderAction by remember { mutableStateOf<String?>(null) } // "REVIEW" or "GAME"
    val courses = remember(decks) { decks.map { it.category }.filter(String::isNotBlank).distinct() }
    var selectedCourseFilter by remember { mutableStateOf<String?>(null) }
    val streak = remember(logs, makeUpDates) { LearningStats.calculateStreak(logs, extraActiveDays = makeUpDates) }
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
                    text = when (actionType) {
                        "REVIEW" -> "選擇要複習的資料夾"
                        "READING" -> "選擇文章使用的資料夾"
                        else -> "選擇要測驗的資料夾"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
                ) {
                    // Option 1: All Folders
                    if (actionType != "READING") item {
                        Card(
                            onClick = {
                                onSelectDeck(null)
                                pendingFolderAction = null
                                if (actionType == "REVIEW") onStartReview() else onOpenQuizGames(null)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    when (actionType) {
                                        "REVIEW" -> onStartReview()
                                        "READING" -> onStartInteractiveReading(deck.id)
                                        else -> onOpenQuizGames(deck.id)
                                    }
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
            if (showTopBar) TopAppBar(
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
                        Box {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "設定",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (updateAvailable) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape)
                                )
                            }
                        }
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
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("繼續今天的學習", style = MaterialTheme.typography.headlineSmall)
                Text("每天一點，讓英文成為習慣。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("今日進度", style = MaterialTheme.typography.titleMedium)
                            Text("連續 ${streak.current} 天", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${progress.completedCards} / ${progress.goalCards}", style = MaterialTheme.typography.headlineLarge)
                        LinearProgressIndicator(progress = { progress.goalFraction }, modifier = Modifier.fillMaxWidth().height(5.dp))
                        Text("待複習 $dueCount 張 · ${progress.dailyXp} XP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { pendingFolderAction = "REVIEW" }, enabled = totalCount > 0, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(50.dp).testTag("start_review_button")) { Text("開始複習") }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = onOpenAddCard, modifier = Modifier.weight(1f).height(50.dp), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("新增單字") }
                    FilledTonalButton(onClick = onOpenPhotoOcr, modifier = Modifier.weight(1f).height(50.dp).testTag("tool_photo_ocr"), shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.DocumentScanner, null); Spacer(Modifier.width(6.dp)); Text("掃描匯入") }
                }
            }
            item { ModernListRow("匯入單字集", "連結、檔案或貼上文字", Icons.Default.Download, onOpenExternalImport, modifier = Modifier.testTag("tool_external_import")) }
            item { ModernListRow("測驗", "選擇資料夾，練習記憶與應用", Icons.Default.Extension, { pendingFolderAction = "GAME" }, modifier = Modifier.testTag("tool_quiz_games")) }
            item { CalmSectionHeader("我的單字庫", action = { IconButton(onClick = { showAddFolderDialog = true }, modifier = Modifier.testTag("dashboard_add_folder_button")) { Icon(Icons.Default.Add, "新增資料夾") } }) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = selectedCourseFilter == null, onClick = { selectedCourseFilter = null }, label = { Text("全部") }) }
                    items(courses) { course -> FilterChip(selected = selectedCourseFilter == course, onClick = { selectedCourseFilter = course }, label = { Text(course) }) }
                }
            }
            item { ModernListRow("所有單字", "$totalCount 張 · 已精通 $masteredCount 張", Icons.Default.CollectionsBookmark, { onSelectDeck(null); onOpenCardList() }) }
            items(decks.filter { selectedCourseFilter == null || it.category == selectedCourseFilter }) { deck ->
                ModernListRow(deck.name, "${deck.category} · ${allCards.count { it.deckId == deck.id }} 張", Icons.Default.Folder, { onSelectDeck(deck.id); onOpenCardList() })
            }
            item { CalmSectionHeader("學習工具") }
            item { ModernListRow("文章閱讀", "用熟悉的單字練習閱讀", Icons.AutoMirrored.Filled.MenuBook, { pendingFolderAction = "READING" }) }
            item { ModernListRow("學習日曆", "檢視每日學習紀錄", Icons.Default.CalendarMonth, onOpenStudyCalendar) }
            item { ModernListRow("資料檢查", "補齊缺漏與修正資料", Icons.AutoMirrored.Filled.FactCheck, onOpenQualityCheck) }
            item { StatisticsChart(totalCards = totalCount, masteredCards = masteredCount, dueCards = dueCount, logs = logs) }
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
private fun CompactLearningTool(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.7f else 0.35f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            Text(title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
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
