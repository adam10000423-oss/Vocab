package com.example

import com.example.data.entity.StudyLog
import com.example.data.stats.LearningProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LearningProgressTest {
    private val zone = ZoneId.of("Asia/Taipei")
    private val today = LocalDate.of(2026, 7, 29)

    @Test
    fun calculatesTodayGoalXpAndLevel() {
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val yesterday = today.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val logs = listOf(
            StudyLog(cardId = 1, rating = 4, reviewedAt = todayStart + 1_000),
            StudyLog(cardId = 2, rating = 3, reviewedAt = todayStart + 2_000),
            StudyLog(cardId = 3, rating = 1, reviewedAt = yesterday)
        )

        val progress = LearningProgress.calculate(logs, goalCards = 2, today = today, zoneId = zone)

        assertEquals(2, progress.completedCards)
        assertEquals(22, progress.dailyXp)
        assertEquals(27, progress.totalXp)
        assertTrue(progress.isGoalReached)
        assertEquals(0, progress.remainingCards)
        assertEquals(1, progress.level)
    }

    @Test
    fun reportsRemainingCards() {
        val progress = LearningProgress.calculate(emptyList(), goalCards = 20, today = today, zoneId = zone)
        assertFalse(progress.isGoalReached)
        assertEquals(20, progress.remainingCards)
    }
}
