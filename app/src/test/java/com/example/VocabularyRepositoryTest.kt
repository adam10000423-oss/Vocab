package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.VocabularyRepository
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VocabularyRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: VocabularyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VocabularyRepository(
            database,
            database.deckDao(),
            database.flashcardDao(),
            database.studyLogDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun masteredCardsAreNotReturnedAsDue() = runTest {
        val deckId = database.deckDao().insertDeck(Deck(name = "test"))
        database.flashcardDao().insertCards(listOf(
            Flashcard(deckId = deckId, word = "due", definition = "到期", nextReviewTimestamp = 0),
            Flashcard(deckId = deckId, word = "mastered", definition = "精通", isMastered = true, nextReviewTimestamp = 0)
        ))
        val due = repository.getDueCards(Long.MAX_VALUE).first()
        assertEquals(listOf("due"), due.map { it.word })
        assertEquals(1, repository.getDueCardCount(Long.MAX_VALUE).first())
    }

    @Test
    fun sameDeckRejectsCaseInsensitiveDuplicateWords() = runTest {
        val deckId = repository.insertDeck(Deck(name = "test"))
        repository.insertCard(Flashcard(deckId = deckId, word = "Apple", definition = "蘋果"))

        val duplicateResult = runCatching {
            repository.insertCard(Flashcard(deckId = deckId, word = " apple ", definition = "水果"))
        }

        assertTrue(duplicateResult.isFailure)
        assertEquals(listOf("Apple"), repository.getCardsByDeck(deckId).first().map { it.word })
    }

    @Test
    fun repeatedBatchSaveUpdatesTheInsertedCardInsteadOfCreatingAnother() = runTest {
        val deckId = repository.insertDeck(Deck(name = "test"))
        val draft = Flashcard(deckId = deckId, word = "stable", definition = "穩定的")

        val insertedId = repository.saveBatch(listOf(draft), emptyList()).single()
        repository.saveBatch(
            listOf(draft.copy(id = insertedId, definition = "穩定；可靠")),
            emptyList()
        )

        val savedCards = repository.getCardsByDeck(deckId).first()
        assertEquals(1, savedCards.size)
        assertEquals("穩定；可靠", savedCards.single().definition)
    }

    @Test
    fun dragOrderReordersOnlyTheSelectedCourseDeckSlots() = runTest {
        val first = repository.insertDeck(Deck(name = "A1", category = "A", sortOrder = 0))
        repository.insertDeck(Deck(name = "B1", category = "B", sortOrder = 1))
        val third = repository.insertDeck(Deck(name = "A2", category = "A", sortOrder = 2))

        repository.reorderDecks(listOf(third, first))

        assertEquals(
            listOf("A2", "B1", "A1"),
            database.deckDao().getAllDecksOnce().map { it.name }
        )
    }

    @Test
    fun dragOrderPersistsTheExactCardOrder() = runTest {
        val deckId = repository.insertDeck(Deck(name = "test"))
        val first = repository.insertCard(
            Flashcard(deckId = deckId, word = "first", definition = "一", sortOrder = 3)
        )
        val second = repository.insertCard(
            Flashcard(deckId = deckId, word = "second", definition = "二", sortOrder = 2)
        )
        val third = repository.insertCard(
            Flashcard(deckId = deckId, word = "third", definition = "三", sortOrder = 1)
        )

        repository.reorderCards(deckId, listOf(third, first, second))

        assertEquals(
            listOf("third", "first", "second"),
            repository.getCardsByDeck(deckId).first().map { it.word }
        )
    }
}
