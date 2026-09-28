package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.GrammarLibrary
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarLibraryDao {
    @Query("SELECT * FROM grammar_libraries ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    fun getAll(): Flow<List<GrammarLibrary>>

    @Query("SELECT * FROM grammar_libraries ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    suspend fun getAllOnce(): List<GrammarLibrary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(library: GrammarLibrary): Long

    @Update
    suspend fun update(library: GrammarLibrary)

    @Update
    suspend fun updateAll(libraries: List<GrammarLibrary>)

    @Delete
    suspend fun delete(library: GrammarLibrary)

    @Query("DELETE FROM grammar_libraries")
    suspend fun deleteAll()
}
