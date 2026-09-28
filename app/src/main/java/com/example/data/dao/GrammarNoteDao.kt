package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.GrammarNote
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarNoteDao {
    @Query("SELECT * FROM grammar_notes ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    fun getAll(): Flow<List<GrammarNote>>

    @Query("SELECT * FROM grammar_notes ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    suspend fun getAllOnce(): List<GrammarNote>

    @Query("SELECT * FROM grammar_notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GrammarNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: GrammarNote): Long

    @Update
    suspend fun update(note: GrammarNote)

    @Update
    suspend fun updateAll(notes: List<GrammarNote>)

    @Delete
    suspend fun delete(note: GrammarNote)

    @Query("DELETE FROM grammar_notes")
    suspend fun deleteAll()

    @Query("UPDATE grammar_notes SET course = :newName, updatedAt = :updatedAt WHERE course = :oldName COLLATE NOCASE")
    suspend fun renameCourse(oldName: String, newName: String, updatedAt: Long): Int

    @Query("UPDATE grammar_notes SET course = :course, folder = :newName, updatedAt = :updatedAt WHERE course = :course AND folder = :oldName")
    suspend fun renameLibrary(course: String, oldName: String, newName: String, updatedAt: Long): Int

    @Query("DELETE FROM grammar_notes WHERE course = :course AND folder = :folder")
    suspend fun deleteLibraryNotes(course: String, folder: String): Int
}
