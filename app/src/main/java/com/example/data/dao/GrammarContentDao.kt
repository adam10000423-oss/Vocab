package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.GrammarExample
import com.example.data.entity.GrammarPattern
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarContentDao {
    @Query("SELECT * FROM grammar_patterns ORDER BY grammarNoteId, sortOrder, id")
    fun getAllPatterns(): Flow<List<GrammarPattern>>

    @Query("SELECT * FROM grammar_examples ORDER BY grammarNoteId, sortOrder, id")
    fun getAllExamples(): Flow<List<GrammarExample>>

    @Query("SELECT * FROM grammar_patterns WHERE grammarNoteId = :noteId ORDER BY sortOrder, id")
    suspend fun getPatternsForNote(noteId: Long): List<GrammarPattern>

    @Query("SELECT * FROM grammar_patterns WHERE id = :id LIMIT 1")
    suspend fun getPatternById(id: Long): GrammarPattern?

    @Query("SELECT * FROM grammar_examples WHERE grammarNoteId = :noteId ORDER BY sortOrder, id")
    suspend fun getExamplesForNote(noteId: Long): List<GrammarExample>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: GrammarPattern): Long

    @Update
    suspend fun updatePattern(pattern: GrammarPattern)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExample(example: GrammarExample): Long

    @Query("DELETE FROM grammar_examples WHERE grammarNoteId = :noteId")
    suspend fun deleteExamplesForNote(noteId: Long)

    @Query("DELETE FROM grammar_patterns WHERE grammarNoteId = :noteId")
    suspend fun deletePatternsForNote(noteId: Long)

    @Query("DELETE FROM grammar_examples")
    suspend fun deleteAllExamples()

    @Query("DELETE FROM grammar_patterns")
    suspend fun deleteAllPatterns()
}
