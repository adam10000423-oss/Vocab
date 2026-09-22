package com.example.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationScoreTest {
    @Test
    fun exactRecognitionScoresHighly() {
        assertTrue(pronunciationScore("active", "active", 0.95f) >= 90)
    }

    @Test
    fun unrelatedRecognitionScoresLow() {
        assertTrue(pronunciationScore("active", "banana", 0.9f) < 50)
    }

    @Test
    fun matchingWordInsidePhraseCanStillScore() {
        assertTrue(pronunciationScore("effect", "the word effect", 0.8f) >= 80)
    }
}
