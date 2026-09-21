package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Deck
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    suspend fun getAllDecksOnce(): List<Deck>

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: Long): Deck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: Deck): Long

    @Update
    suspend fun updateDeck(deck: Deck)

    @Update
    suspend fun updateDecks(decks: List<Deck>)

    @Query("UPDATE decks SET category = :newName WHERE category = :oldName")
    suspend fun renameCourse(oldName: String, newName: String): Int

    @Delete
    suspend fun deleteDeck(deck: Deck)

    @Query("DELETE FROM decks")
    suspend fun deleteAllDecks()
}
