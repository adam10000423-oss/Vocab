package com.example.data

import androidx.room.withTransaction
import com.example.data.dao.DeckDao
import com.example.data.dao.FlashcardDao
import com.example.data.dao.StudyLogDao
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.entity.StudyLog
import com.example.data.srs.SrsCalculator
import kotlinx.coroutines.flow.Flow

class VocabularyRepository(
    private val database: AppDatabase,
    private val deckDao: DeckDao,
    private val cardDao: FlashcardDao,
    private val logDao: StudyLogDao
) {
    data class MoveCardsResult(
        val movedCount: Int,
        val mergedDuplicateCount: Int
    )
    val allDecks: Flow<List<Deck>> = deckDao.getAllDecks()
    val allCards: Flow<List<Flashcard>> = cardDao.getAllCards()
    val totalCardCount: Flow<Int> = cardDao.getTotalCardCount()
    val masteredCardCount: Flow<Int> = cardDao.getMasteredCardCount()

    fun getDueCards(currentTime: Long = System.currentTimeMillis()): Flow<List<Flashcard>> =
        cardDao.getDueCards(currentTime)

    fun getDueCardCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> =
        cardDao.getDueCardCount(currentTime)

    fun getCardsByDeck(deckId: Long): Flow<List<Flashcard>> =
        cardDao.getCardsByDeck(deckId)

    fun getDueCardsForDeck(deckId: Long, currentTime: Long = System.currentTimeMillis()): Flow<List<Flashcard>> =
        cardDao.getDueCardsForDeck(deckId, currentTime)

    fun getLogsSince(sinceTimestamp: Long): Flow<List<StudyLog>> =
        logDao.getLogsSince(sinceTimestamp)

    fun getAllLogs(): Flow<List<StudyLog>> = logDao.getAllLogs()

    suspend fun getCardById(cardId: Long): Flashcard? =
        cardDao.getCardById(cardId)

    suspend fun insertDeck(deck: Deck): Long = deckDao.insertDeck(deck)

    suspend fun updateDeck(deck: Deck) = deckDao.updateDeck(deck)

    suspend fun moveDeck(deckId: Long, direction: Int) = database.withTransaction {
        val all = deckDao.getAllDecksOnce()
        val moving = all.firstOrNull { it.id == deckId } ?: return@withTransaction
        val sameCourse = all.filter { it.category == moving.category }
        val currentIndex = sameCourse.indexOfFirst { it.id == deckId }
        val targetIndex = (currentIndex + direction).coerceIn(sameCourse.indices)
        if (currentIndex < 0 || targetIndex == currentIndex) return@withTransaction
        val target = sameCourse[targetIndex]
        val reordered = all.toMutableList()
        val globalCurrentIndex = reordered.indexOfFirst { it.id == moving.id }
        val globalTargetIndex = reordered.indexOfFirst { it.id == target.id }
        reordered[globalCurrentIndex] = target
        reordered[globalTargetIndex] = moving
        deckDao.updateDecks(
            reordered.mapIndexed { index, deck -> deck.copy(sortOrder = index.toLong()) }
        )
    }

    suspend fun moveDeckGlobally(deckId: Long, direction: Int) = database.withTransaction {
        val all = deckDao.getAllDecksOnce()
        val currentIndex = all.indexOfFirst { it.id == deckId }
        if (currentIndex < 0) return@withTransaction
        val targetIndex = (currentIndex + direction).coerceIn(all.indices)
        if (targetIndex == currentIndex) return@withTransaction
        val reordered = all.toMutableList().apply {
            val moving = removeAt(currentIndex)
            add(targetIndex, moving)
        }
        deckDao.updateDecks(
            reordered.mapIndexed { index, deck -> deck.copy(sortOrder = index.toLong()) }
        )
    }

    suspend fun reorderDecks(orderedDeckIds: List<Long>) = database.withTransaction {
        if (orderedDeckIds.isEmpty()) return@withTransaction
        val all = deckDao.getAllDecksOnce()
        val requestedIds = orderedDeckIds.distinct()
        require(requestedIds.size == orderedDeckIds.size) { "資料夾排序包含重複項目" }
        val requestedSet = requestedIds.toSet()
        require(requestedSet.all { id -> all.any { it.id == id } }) { "找不到要排序的資料夾" }
        val orderedDecks = requestedIds.map { id ->
            requireNotNull(all.firstOrNull { it.id == id })
        }
        var orderedIndex = 0
        val merged = all.map { deck ->
            if (deck.id in requestedSet) orderedDecks[orderedIndex++] else deck
        }
        deckDao.updateDecks(
            merged.mapIndexed { index, deck -> deck.copy(sortOrder = index.toLong()) }
        )
    }

    suspend fun renameCourse(oldName: String, newName: String): Int {
        val oldValue = oldName.trim()
        val newValue = newName.trim()
        require(oldValue.isNotBlank() && newValue.isNotBlank()) { "課程名稱不能空白" }
        if (oldValue == newValue) return 0
        return deckDao.renameCourse(oldValue, newValue)
    }

    suspend fun deleteDeck(deck: Deck) = deckDao.deleteDeck(deck)

    suspend fun insertCard(card: Flashcard): Long = cardDao.insertCard(card.normalizedCopy())

    suspend fun insertCards(cards: List<Flashcard>) =
        cardDao.insertCards(cards.map { it.normalizedCopy() }.distinctBy { it.deckId to it.normalizedWord })

    suspend fun insertCardsDeduplicating(cards: List<Flashcard>): Int = database.withTransaction {
        var insertedCount = 0
        cards.forEach { incoming ->
            val normalized = incoming.normalizedCopy()
            if (normalized.normalizedWord.isBlank()) return@forEach
            val existing = cardDao.getCardByWord(normalized.deckId, normalized.normalizedWord)
            if (existing == null) {
                cardDao.insertCard(normalized)
                insertedCount += 1
            } else {
                cardDao.updateCard(
                    existing.copy(
                        phonetic = existing.phonetic.ifBlank { normalized.phonetic },
                        partOfSpeech = existing.partOfSpeech.ifBlank { normalized.partOfSpeech },
                        definition = existing.definition.ifBlank { normalized.definition },
                        exampleSentence = existing.exampleSentence.ifBlank { normalized.exampleSentence },
                        exampleTranslation = existing.exampleTranslation.ifBlank { normalized.exampleTranslation },
                        imageUrl = existing.imageUrl ?: normalized.imageUrl,
                        notes = existing.notes.ifBlank { normalized.notes }
                    )
                )
            }
        }
        insertedCount
    }

    suspend fun nextSortOrder(deckId: Long, count: Int): Long =
        cardDao.getMaxSortOrder(deckId) + count.coerceAtLeast(1) + 1L

    suspend fun nextBottomSortOrder(deckId: Long): Long =
        cardDao.getMinSortOrder(deckId) - 1L

    suspend fun moveCard(cardId: Long, deckId: Long, direction: Int) = database.withTransaction {
        val cards = cardDao.getCardsByDeckOnce(deckId)
        val currentIndex = cards.indexOfFirst { it.id == cardId }
        if (currentIndex < 0) return@withTransaction
        val targetIndex = (currentIndex + direction).coerceIn(cards.indices)
        if (targetIndex == currentIndex) return@withTransaction

        val reordered = cards.toMutableList().apply {
            val moving = removeAt(currentIndex)
            add(targetIndex, moving)
        }
        val topOrder = maxOf(
            System.currentTimeMillis() + reordered.size,
            (reordered.maxOfOrNull { it.sortOrder } ?: 0L) + reordered.size
        )
        cardDao.updateCards(
            reordered.mapIndexed { index, card ->
                card.copy(sortOrder = topOrder - index)
            }
        )
    }

    suspend fun reorderCards(deckId: Long, orderedCardIds: List<Long>) = database.withTransaction {
        val current = cardDao.getCardsByDeckOnce(deckId)
        require(current.map { it.id }.toSet() == orderedCardIds.toSet()) {
            "排序內容與資料夾目前的單字不一致，請重新執行"
        }
        val byId = current.associateBy { it.id }
        val topOrder = maxOf(
            System.currentTimeMillis() + orderedCardIds.size,
            (current.maxOfOrNull { it.sortOrder } ?: 0L) + orderedCardIds.size
        )
        cardDao.updateCards(
            orderedCardIds.mapIndexed { index, id ->
                requireNotNull(byId[id]) { "找不到要排序的單字卡" }
                    .copy(sortOrder = topOrder - index)
            }
        )
    }

    suspend fun moveCards(cardIds: List<Long>, targetDeckId: Long): MoveCardsResult =
        database.withTransaction {
            require(deckDao.getDeckById(targetDeckId) != null) { "找不到目標資料夾" }
            val orderedIds = cardIds.distinct()
            val cards = orderedIds.mapNotNull { cardDao.getCardById(it) }
            var movedCount = 0
            var mergedCount = 0
            var nextOrder = cardDao.getMaxSortOrder(targetDeckId) + cards.size + 1L

            cards.forEach { source ->
                if (source.deckId == targetDeckId) return@forEach
                val duplicate = cardDao.getCardByWord(targetDeckId, source.normalizedWord)
                if (duplicate != null && duplicate.id != source.id) {
                    val moreAdvanced = listOf(duplicate, source).maxWithOrNull(
                        compareBy<Flashcard> { it.repetitionCount }
                            .thenBy { it.intervalDays }
                            .thenBy { it.lastReviewedTimestamp ?: 0L }
                    ) ?: duplicate
                    cardDao.updateCard(
                        duplicate.copy(
                            phonetic = duplicate.phonetic.ifBlank { source.phonetic },
                            partOfSpeech = duplicate.partOfSpeech.ifBlank { source.partOfSpeech },
                            definition = duplicate.definition.ifBlank { source.definition },
                            exampleSentence = duplicate.exampleSentence.ifBlank { source.exampleSentence },
                            exampleTranslation = duplicate.exampleTranslation.ifBlank { source.exampleTranslation },
                            imageUrl = duplicate.imageUrl ?: source.imageUrl,
                            notes = duplicate.notes.ifBlank { source.notes },
                            isMastered = duplicate.isMastered || source.isMastered,
                            isFavorite = duplicate.isFavorite || source.isFavorite,
                            mistakeCount = maxOf(duplicate.mistakeCount, source.mistakeCount),
                            intervalDays = moreAdvanced.intervalDays,
                            easeFactor = moreAdvanced.easeFactor,
                            repetitionCount = moreAdvanced.repetitionCount,
                            nextReviewTimestamp = moreAdvanced.nextReviewTimestamp,
                            lastReviewedTimestamp = maxOf(
                                duplicate.lastReviewedTimestamp ?: 0L,
                                source.lastReviewedTimestamp ?: 0L
                            ).takeIf { it > 0L }
                        )
                    )
                    logDao.reassignCardLogs(source.id, duplicate.id)
                    cardDao.deleteCard(source)
                    mergedCount += 1
                } else {
                    cardDao.updateCard(
                        source.copy(
                            deckId = targetDeckId,
                            sortOrder = nextOrder--
                        ).normalizedCopy()
                    )
                    movedCount += 1
                }
            }
            MoveCardsResult(movedCount, mergedCount)
        }

    suspend fun saveBatch(cardsToSave: List<Flashcard>, cardsToDelete: List<Flashcard>): List<Long> =
        database.withTransaction {
            if (cardsToDelete.isNotEmpty()) cardDao.deleteCards(cardsToDelete)
            val normalizedCards = cardsToSave.map { it.normalizedCopy() }
            require(
                normalizedCards.map { it.deckId to it.normalizedWord }.distinct().size == normalizedCards.size
            ) { "同一個資料夾不能有重複單字" }
            normalizedCards.map { card ->
                val duplicate = cardDao.getCardByWord(card.deckId, card.normalizedWord)
                when {
                    card.id != 0L -> {
                        require(duplicate == null || duplicate.id == card.id) {
                            "「${card.word}」已存在於此資料夾"
                        }
                        cardDao.updateCard(card)
                        card.id
                    }
                    duplicate != null -> error("「${card.word}」已存在於這個資料夾")
                    else -> cardDao.insertCard(card)
                }
            }
        }

    suspend fun updateCard(card: Flashcard) = cardDao.updateCard(card.normalizedCopy())

    suspend fun updateCards(cards: List<Flashcard>) = database.withTransaction {
        cardDao.updateCards(cards.map { it.normalizedCopy() })
    }

    suspend fun deleteCard(card: Flashcard) = cardDao.deleteCard(card)

    suspend fun deleteCards(cards: List<Flashcard>) = cardDao.deleteCards(cards)

    suspend fun deleteCardById(cardId: Long) = cardDao.deleteCardById(cardId)

    /**
     * Submit an SRS review answer (1: Again, 2: Hard, 3: Good, 4: Easy).
     */
    suspend fun recordReview(card: Flashcard, rating: Int) {
        require(rating in 1..4) { "Review rating must be between 1 and 4." }
        val updatedCard = SrsCalculator.calculateNextReview(card, rating)
        cardDao.updateCard(updatedCard)
        logDao.insertLog(StudyLog(cardId = card.id, rating = rating))
    }

    suspend fun undoLatestReview(originalCard: Flashcard) = database.withTransaction {
        cardDao.updateCard(originalCard)
        logDao.deleteLatestForCard(originalCard.id)
    }

    /**
     * Toggle card mastered state directly.
     */
    suspend fun toggleMastered(card: Flashcard) {
        val updatedCard = card.copy(
            isMastered = !card.isMastered,
            intervalDays = if (!card.isMastered) 30 else 0,
            nextReviewTimestamp = if (!card.isMastered) {
                System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(30)
            } else {
                System.currentTimeMillis()
            }
        )
        cardDao.updateCard(updatedCard)
    }

    suspend fun toggleFavorite(card: Flashcard) {
        cardDao.updateCard(card.copy(isFavorite = !card.isFavorite))
    }

    suspend fun recordGameMistake(card: Flashcard) {
        val current = cardDao.getCardById(card.id) ?: card
        val updated = SrsCalculator.calculateNextReview(
            current.copy(mistakeCount = current.mistakeCount + 1),
            1
        )
        // Quiz mistakes affect the card's real SRS state and mistake counter, but
        // do not masquerade as a completed flashcard-learning review.
        cardDao.updateCard(updated)
    }

    suspend fun clearAllData() = database.withTransaction {
        logDao.deleteAllLogs()
        cardDao.deleteAllCards()
        deckDao.deleteAllDecks()
    }

    private fun Flashcard.normalizedCopy(
        id: Long = this.id,
        deckId: Long = this.deckId
    ): Flashcard {
        val trimmedWord = word.trim()
        return copy(
            id = id,
            deckId = deckId,
            word = trimmedWord,
            normalizedWord = trimmedWord.lowercase()
        )
    }
}
