package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.GrammarWritingRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarWritingDao {
    @Query("SELECT * FROM grammar_writing_records ORDER BY updatedAt DESC, id DESC")
    fun getAll(): Flow<List<GrammarWritingRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GrammarWritingRecord): Long

    @Delete
    suspend fun delete(record: GrammarWritingRecord)

    @Query("DELETE FROM grammar_writing_records")
    suspend fun deleteAll()
}
