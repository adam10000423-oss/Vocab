package com.example

import com.example.data.entity.StudyLog
import com.example.data.stats.LearningStats
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningStatsTest {
    private val zone = ZoneId.of("Asia/Taipei")
    private val today = LocalDate.of(2026, 7, 29)

    @Test
    fun streakUsesDistinctCalendarDays() {
        val logs = listOf(
            log(today.minusDays(2)),
            log(today.minusDays(1)),
            log(today.minusDays(1)),
            log(today)
        )
        val result = LearningStats.calculateStreak(logs, today, zone)
        assertEquals(3, result.current)
        assertEquals(3, result.longest)
    }

    @Test
    fun expiredStreakKeepsLongest() {
        val logs = listOf(log(today.minusDays(5)), log(today.minusDays(4)))
        val result = LearningStats.calculateStreak(logs, today, zone)
        assertEquals(0, result.current)
        assertEquals(2, result.longest)
    }

    @Test
    fun makeUpDayBridgesRealStudyLogsWithoutCreatingLogs() {
        val logs = listOf(log(today.minusDays(2)), log(today))
        val result = LearningStats.calculateStreak(
            logs = logs,
            today = today,
            zoneId = zone,
            extraActiveDays = setOf(today.minusDays(1))
        )
        assertEquals(3, result.current)
        assertEquals(3, result.longest)
    }

    private fun log(day: LocalDate) = StudyLog(
        cardId = 1,
        rating = 3,
        reviewedAt = day.atStartOfDay(zone).toInstant().toEpochMilli()
    )
}
