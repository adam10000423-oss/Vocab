package com.example

import com.example.data.importer.ExternalDeckImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalDeckImporterTest {
    @Test
    fun parsesQuizletStyleTabSeparatedText() {
        val parsed = ExternalDeckImporter.parseText(
            "apple\t蘋果\napplication\t申請；應用程式\napply\t申請；應用"
        )
        assertEquals(3, parsed.cards.size)
        assertEquals("apple", parsed.cards.first().word)
        assertEquals("蘋果", parsed.cards.first().definition)
    }

    @Test
    fun parsesJsonCardsAndDetectsSource() {
        val parsed = ExternalDeckImporter.parseText(
            """{"cards":[{"word":"apple","definition":"蘋果","example":"I ate an apple."}]}"""
        )
        assertEquals("I ate an apple.", parsed.cards.single().exampleSentence)
        assertEquals("Quizlet", ExternalDeckImporter.detectSource("https://quizlet.com/123/test")?.name)
    }

    @Test
    fun recognizesOnlyDirectDataFiles() {
        assertTrue(ExternalDeckImporter.isDirectTextUrl("https://example.com/deck.csv?download=1"))
    }
}
