package com.example.data.srs

import com.example.data.entity.Flashcard
import java.util.concurrent.TimeUnit

object SrsCalculator {
    private const val MIN_EASE = 1.3f
    private const val MAX_EASE = 3.0f
    private const val MAX_INTERVAL_DAYS = 3650

    private data class Schedule(
        val repetitionCount: Int,
        val intervalDays: Int,
        val easeFactor: Float
    )

    fun calculateNextReview(card: Flashcard, rating: Int): Flashcard {
        require(rating in 1..4) { "Review rating must be between 1 and 4." }
        val now = System.currentTimeMillis()
        val schedule = calculateSchedule(card, rating)
        val nextTimestamp = if (schedule.intervalDays == 0) {
            now + TimeUnit.MINUTES.toMillis(10)
        } else {
            now + TimeUnit.DAYS.toMillis(schedule.intervalDays.toLong())
        }

        return card.copy(
            intervalDays = schedule.intervalDays,
            easeFactor = schedule.easeFactor,
            repetitionCount = schedule.repetitionCount,
            nextReviewTimestamp = nextTimestamp,
            lastReviewedTimestamp = now,
            isMastered = schedule.intervalDays >= 21
        )
    }

    fun getIntervalText(card: Flashcard, rating: Int): String {
        require(rating in 1..4) { "Review rating must be between 1 and 4." }
        val days = calculateSchedule(card, rating).intervalDays
        return if (days == 0) "< 10 分鐘" else "$days 天後"
    }

    private fun calculateSchedule(card: Flashcard, rating: Int): Schedule {
        var repetitions = card.repetitionCount
        var interval = card.intervalDays
        var ease = card.easeFactor.coerceIn(MIN_EASE, MAX_EASE)

        when (rating) {
            1 -> {
                repetitions = 0
                interval = 0
                ease -= 0.2f
            }
            2 -> {
                repetitions += 1
                interval = if (interval == 0) 1 else (interval * 1.2f).toInt().coerceAtLeast(1)
                ease -= 0.15f
            }
            3 -> {
                repetitions += 1
                interval = when (repetitions) {
                    1 -> 1
                    2 -> 3
                    else -> (interval * ease).toInt().coerceAtLeast(interval + 1)
                }
            }
            4 -> {
                repetitions += 1
                interval = when (repetitions) {
                    1 -> 3
                    2 -> 6
                    else -> (interval * ease * 1.3f).toInt().coerceAtLeast(interval + 2)
                }
                ease += 0.15f
            }
        }

        return Schedule(
            repetitionCount = repetitions,
            intervalDays = interval.coerceIn(0, MAX_INTERVAL_DAYS),
            easeFactor = ease.coerceIn(MIN_EASE, MAX_EASE)
        )
    }
}
