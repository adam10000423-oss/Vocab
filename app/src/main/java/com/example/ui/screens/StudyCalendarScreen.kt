package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.StudyLog
import com.example.data.stats.LearningStats
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyCalendarScreen(
    logs: List<StudyLog>,
    dailyGoalCards: Int,
    makeUpDates: Set<LocalDate>,
    onToggleMakeUp: (LocalDate) -> Unit,
    onBack: () -> Unit
) {
    val today = LocalDate.now()
    val zoneId = ZoneId.systemDefault()
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }
    val countByDate = remember(logs, zoneId) {
        logs.groupingBy { Instant.ofEpochMilli(it.reviewedAt).atZone(zoneId).toLocalDate() }.eachCount()
    }
    val streak = remember(logs, makeUpDates, today) {
        LearningStats.calculateStreak(logs, today, zoneId, makeUpDates)
    }
    val leading = visibleMonth.atDay(1).dayOfWeek.value - 1
    val cells = remember(visibleMonth) {
        List(leading) { null } + (1..visibleMonth.lengthOfMonth()).map(visibleMonth::atDay)
    }
    val paddedCells = cells + List((7 - cells.size % 7) % 7) { null }
    val selectedCount = countByDate[selectedDate] ?: 0
    val selectedMakeUp = selectedDate in makeUpDates

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("學習日曆", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard("目前連續", "${streak.current} 天", Modifier.weight(1f))
                SummaryCard("最長連續", "${streak.longest} 天", Modifier.weight(1f))
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "上個月")
                        }
                        Text("${visibleMonth.year} 年 ${visibleMonth.monthValue} 月", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "下個月")
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    paddedCells.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                CalendarDay(
                                    date = date,
                                    count = date?.let { countByDate[it] } ?: 0,
                                    goal = dailyGoalCards,
                                    isMakeUp = date in makeUpDates,
                                    selected = date == selectedDate,
                                    today = date == today,
                                    onClick = { if (date != null) selectedDate = date },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(MaterialTheme.colorScheme.primary, "完成目標")
                        LegendDot(MaterialTheme.colorScheme.tertiary, "有學習")
                        LegendDot(MaterialTheme.colorScheme.secondary, "補打卡")
                    }
                }
            }

            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${selectedDate.monthValue} 月 ${selectedDate.dayOfMonth} 日", fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            selectedCount >= dailyGoalCards -> "已學習 $selectedCount 張，完成每日目標。"
                            selectedCount > 0 -> "已學習 $selectedCount 張，距離目標還差 ${dailyGoalCards - selectedCount} 張。"
                            selectedMakeUp -> "這一天沒有學習紀錄，已標記為補打卡。"
                            else -> "這一天沒有學習紀錄。"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedDate <= today && selectedCount == 0) {
                        Button(onClick = { onToggleMakeUp(selectedDate) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (selectedMakeUp) "取消補打卡" else "補打卡")
                        }
                    }
                }
            }
            Text(
                "補打卡只會補上連續紀錄，不會增加學習張數或偽造答題紀錄。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?, count: Int, goal: Int, isMakeUp: Boolean, selected: Boolean, today: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val color = when {
        count >= goal -> MaterialTheme.colorScheme.primary
        count > 0 -> MaterialTheme.colorScheme.tertiary
        isMakeUp -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }
    Box(modifier.aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
        if (date != null) {
            Surface(
                modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
                shape = CircleShape,
                color = color.copy(alpha = if (color == Color.Transparent) 0f else 0.2f),
                border = when {
                    selected -> androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    today -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    else -> null
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(date.dayOfMonth.toString(), fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.background(color, CircleShape).padding(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
