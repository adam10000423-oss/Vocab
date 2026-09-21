package com.example

import com.example.util.OcrWordParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrWordParserTest {

    @Test
    fun delimitedCardKeepsPartOfSpeechDefinitionAndSentenceInOrder() {
        val cards = OcrWordParser.parseTextToCards(
            "active | adj. | 活躍的；積極的 | The dog is very active.",
            deckId = 1L
        )

        assertEquals(1, cards.size)
        assertEquals("active", cards.single().word)
        assertEquals("adj.", cards.single().partOfSpeech)
        assertEquals("活躍的；積極的", cards.single().definition)
        assertEquals("The dog is very active.", cards.single().exampleSentence)
    }

    @Test
    fun sentenceWithoutDelimiterDoesNotBecomeAFakeWord() {
        val cards = OcrWordParser.parseTextToCards(
            "This is an example sentence without a vocabulary delimiter.",
            deckId = 1L
        )

        assertTrue(cards.isEmpty())
    }

    @Test
    fun duplicateWordsAreRemovedWithoutChangingFirstSeenOrder() {
        val cards = OcrWordParser.parseTextToCards(
            """
            apple | 蘋果
            banana | 香蕉
            Apple | 不應覆蓋第一筆
            """.trimIndent(),
            deckId = 1L
        )

        assertEquals(listOf("apple", "banana"), cards.map { it.word })
        assertEquals("蘋果", cards.first().definition)
    }

    @Test
    fun adjacentWordAndChineseDefinitionArePairedForOcrLayouts() {
        val cards = OcrWordParser.parseTextToCards(
            """
            resilient
            有韌性的；能迅速恢復的
            """.trimIndent(),
            deckId = 1L
        )

        assertEquals(1, cards.size)
        assertEquals("resilient", cards.single().word)
        assertEquals("有韌性的；能迅速恢復的", cards.single().definition)
    }
}
