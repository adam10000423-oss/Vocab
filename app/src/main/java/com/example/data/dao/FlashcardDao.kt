package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY sortOrder DESC, createdAt DESC, id DESC")
    fun getAllCards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY sortOrder DESC, createdAt DESC, id DESC")
    fun getCardsByDeck(deckId: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY sortOrder DESC, createdAt DESC, id DESC")
    suspend fun getCardsByDeckOnce(deckId: Long): List<Flashcard>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM flashcards WHERE deckId = :deckId")
    suspend fun getMaxSortOrder(deckId: Long): Long

    @Query("SELECT COALESCE(MIN(sortOrder), 0) FROM flashcards WHERE deckId = :deckId")
    suspend fun getMinSortOrder(deckId: Long): Long

    @Query("SELECT * FROM flashcards WHERE isMastered = 0 AND isSuspended = 0 AND nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    fun getDueCards(currentTime: Long = System.currentTimeMillis()): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND isMastered = 0 AND isSuspended = 0 AND nextReviewTimestamp <= :currentTime ORDER BY nextReviewTimestamp ASC")
    fun getDueCardsForDeck(deckId: Long, currentTime: Long = System.currentTimeMillis()): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE isMastered = 1")
    fun getMasteredCards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE id = :cardId LIMIT 1")
    suspend fun getCardById(cardId: Long): Flashcard?

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId AND normalizedWord = :normalizedWord LIMIT 1")
    suspend fun getCardByWord(deckId: Long, normalizedWord: String): Flashcard?

    @Query("SELECT COUNT(*) FROM flashcards")
    fun getTotalCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE isMastered = 1")
    fun getMasteredCardCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcards WHERE isMastered = 0 AND isSuspended = 0 AND nextReviewTimestamp <= :currentTime")
    fun getDueCardCount(currentTime: Long = System.currentTimeMillis()): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCard(card: Flashcard): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCards(cards: List<Flashcard>)

    @Update
    suspend fun updateCard(card: Flashcard)

    @Update
    suspend fun updateCards(cards: List<Flashcard>)

    @Delete
    suspend fun deleteCard(card: Flashcard)

    @Delete
    suspend fun deleteCards(cards: List<Flashcard>)

    @Query("DELETE FROM flashcards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)

    @Query("DELETE FROM flashcards")
    suspend fun deleteAllCards()
}
