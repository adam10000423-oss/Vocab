package com.example.data.stats

import com.example.data.entity.StudyLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class StreakStats(val current: Int, val longest: Int)

object LearningStats {
    fun calculateStreak(
        logs: List<StudyLog>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        extraActiveDays: Set<LocalDate> = emptySet()
    ): StreakStats {
        val days = (logs.asSequence()
            .map { Instant.ofEpochMilli(it.reviewedAt).atZone(zoneId).toLocalDate() }
            .plus(extraActiveDays.asSequence()))
            .distinct()
            .sorted()
            .toList()
        if (days.isEmpty()) return StreakStats(0, 0)

        var longest = 1
        var run = 1
        for (index in 1 until days.size) {
            if (days[index - 1].plusDays(1) == days[index]) {
                run++
                longest = maxOf(longest, run)
            } else {
                run = 1
            }
        }

        val last = days.last()
        if (last != today && last != today.minusDays(1)) return StreakStats(0, longest)
        var current = 1
        var cursor = last
        for (index in days.lastIndex - 1 downTo 0) {
            if (days[index] == cursor.minusDays(1)) {
                current++
                cursor = days[index]
            } else break
        }
        return StreakStats(current, longest)
    }
}
