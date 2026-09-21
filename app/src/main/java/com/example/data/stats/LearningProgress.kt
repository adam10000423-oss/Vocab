package com.example.data.stats

import com.example.data.entity.StudyLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyLearningProgress(
    val completedCards: Int,
    val goalCards: Int,
    val dailyXp: Int,
    val totalXp: Int,
    val level: Int,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int = 500
) {
    val isGoalReached: Boolean get() = completedCards >= goalCards
    val remainingCards: Int get() = (goalCards - completedCards).coerceAtLeast(0)
    val goalFraction: Float get() = (completedCards.toFloat() / goalCards.coerceAtLeast(1)).coerceIn(0f, 1f)
    val levelFraction: Float get() = (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}

object LearningProgress {
    fun xpForRating(rating: Int): Int = when (rating.coerceIn(1, 4)) {
        1 -> 5
        2 -> 8
        3 -> 10
        else -> 12
    }

    fun calculate(
        logs: List<StudyLog>,
        goalCards: Int,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DailyLearningProgress {
        val todayLogs = logs.filter {
            Instant.ofEpochMilli(it.reviewedAt).atZone(zoneId).toLocalDate() == today
        }
        val totalXp = logs.sumOf { xpForRating(it.rating) }
        val dailyXp = todayLogs.sumOf { xpForRating(it.rating) }
        val levelSize = 500
        return DailyLearningProgress(
            completedCards = todayLogs.size,
            goalCards = goalCards.coerceAtLeast(1),
            dailyXp = dailyXp,
            totalXp = totalXp,
            level = totalXp / levelSize + 1,
            xpIntoLevel = totalXp % levelSize,
            xpForNextLevel = levelSize
        )
    }
}
