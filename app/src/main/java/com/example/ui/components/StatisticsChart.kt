package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StudyLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsChart(
    totalCards: Int,
    masteredCards: Int,
    dueCards: Int,
    logs: List<StudyLog>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statistics_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "學習進度",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge(
                    count = totalCards,
                    label = "總數",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatBadge(
                    count = masteredCards,
                    label = "精通",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatBadge(
                    count = dueCards,
                    label = "到期",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mastered Percentage Bar
            val masteredRatio = if (totalCards > 0) {
                (masteredCards.toFloat() / totalCards.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            Text(
                text = "精通 ${(masteredRatio * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Custom Mastered Ratio Canvas Bar
            val masteredColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surface
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
            ) {
                val cornerRadius = 7.dp.toPx()
                val width = size.width
                val masteredWidth = width * masteredRatio

                // Background
                drawRoundRect(
                    color = trackColor,
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )

                // Mastered Bar (Green)
                if (masteredWidth > 0) {
                    drawRoundRect(
                        color = masteredColor,
                        size = Size(masteredWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                    )
                }

            }

            Spacer(modifier = Modifier.height(20.dp))

            // 7-day Review Activity Bar Chart
            Text(
                text = "近 7 天",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            WeeklyBarChart(logs = logs)
        }
    }
}

@Composable
private fun StatBadge(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
private fun WeeklyBarChart(logs: List<StudyLog>) {
    val dayLabels = mutableListOf<Pair<String, Int>>()
    val sdf = SimpleDateFormat("E", Locale.TAIWAN)
    for (i in 6 downTo 0) {
        val dateCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -i)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = dateCal.timeInMillis
        val endMs = startMs + 24 * 60 * 60 * 1000L

        val dayCount = logs.count { it.reviewedAt in startMs until endMs }
        dayLabels.add(Pair(sdf.format(Date(startMs)), dayCount))
    }

    val maxCount = (dayLabels.maxOfOrNull { it.second } ?: 1).coerceAtLeast(5)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        dayLabels.forEach { (day, count) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                Text(
                    text = if (count > 0) count.toString() else "",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                val barHeightFraction = (count.toFloat() / maxCount).coerceIn(0.1f, 1f)
                val barColor = if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((barHeightFraction * 58f).coerceAtLeast(5f).dp)
                        .padding(horizontal = 2.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        color = barColor,
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
