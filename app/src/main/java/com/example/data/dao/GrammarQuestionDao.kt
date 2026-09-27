package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.GrammarQuestion
import com.example.data.entity.GrammarWeakness
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarQuestionDao {
    @Query("SELECT * FROM grammar_questions ORDER BY grammarNoteId, sortOrder, id")
    fun getAllQuestions(): Flow<List<GrammarQuestion>>

    @Query("SELECT * FROM grammar_questions WHERE grammarNoteId = :noteId ORDER BY sortOrder, id")
    suspend fun getForNote(noteId: Long): List<GrammarQuestion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: GrammarQuestion): Long

    @Update
    suspend fun updateQuestion(question: GrammarQuestion)

    @Query("DELETE FROM grammar_questions WHERE grammarNoteId = :noteId")
    suspend fun deleteForNote(noteId: Long)

    @Query("DELETE FROM grammar_weaknesses WHERE grammarNoteId = :noteId")
    suspend fun deleteWeaknessesForNote(noteId: Long)

    @Query("SELECT * FROM grammar_weaknesses ORDER BY lastOccurredAt DESC")
    fun getAllWeaknesses(): Flow<List<GrammarWeakness>>

    @Query("SELECT * FROM grammar_weaknesses WHERE ruleKey = :ruleKey LIMIT 1")
    suspend fun getWeakness(ruleKey: String): GrammarWeakness?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeakness(weakness: GrammarWeakness): Long

    @Update
    suspend fun updateWeakness(weakness: GrammarWeakness)

    @Query("DELETE FROM grammar_questions")
    suspend fun deleteAllQuestions()

    @Query("DELETE FROM grammar_weaknesses")
    suspend fun deleteAllWeaknesses()
}
