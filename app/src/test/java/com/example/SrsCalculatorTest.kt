package com.example

import com.example.data.entity.Flashcard
import com.example.data.srs.SrsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SrsCalculatorTest {
    private val card = Flashcard(
        id = 1,
        deckId = 1,
        word = "resilient",
        definition = "有韌性的"
    )

    @Test
    fun againResetsProgressAndSchedulesTenMinutes() {
        val before = System.currentTimeMillis()
        val result = SrsCalculator.calculateNextReview(
            card.copy(intervalDays = 12, repetitionCount = 4, isMastered = true),
            rating = 1
        )
        assertEquals(0, result.intervalDays)
        assertEquals(0, result.repetitionCount)
        assertFalse(result.isMastered)
        assertTrue(result.nextReviewTimestamp >= before + 9 * 60_000)
        assertEquals("< 10 分鐘", SrsCalculator.getIntervalText(card, 1))
    }

    @Test
    fun goodAndEasyPreviewMatchStoredInterval() {
        for (rating in 2..4) {
            val result = SrsCalculator.calculateNextReview(card, rating)
            assertEquals("${result.intervalDays} 天後", SrsCalculator.getIntervalText(card, rating))
        }
    }

    @Test
    fun invalidRatingIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SrsCalculator.calculateNextReview(card, 0)
        }
    }

    @Test
    fun intervalAndEaseAreBounded() {
        val result = SrsCalculator.calculateNextReview(
            card.copy(intervalDays = 5000, repetitionCount = 20, easeFactor = 9f),
            rating = 4
        )
        assertEquals(3650, result.intervalDays)
        assertEquals(3f, result.easeFactor)
        assertTrue(result.isMastered)
    }
}
