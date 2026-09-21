package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.StudyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyLogDao {
    @Query("UPDATE study_logs SET cardId = :targetCardId WHERE cardId = :sourceCardId")
    suspend fun reassignCardLogs(sourceCardId: Long, targetCardId: Long)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StudyLog)

    @Query("SELECT * FROM study_logs WHERE reviewedAt >= :sinceTimestamp ORDER BY reviewedAt ASC")
    fun getLogsSince(sinceTimestamp: Long): Flow<List<StudyLog>>

    @Query("SELECT * FROM study_logs ORDER BY reviewedAt ASC")
    fun getAllLogs(): Flow<List<StudyLog>>

    @Query("SELECT COUNT(*) FROM study_logs")
    fun getTotalReviewCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM study_logs WHERE reviewedAt >= :sinceTimestamp")
    suspend fun countLogsSince(sinceTimestamp: Long): Int

    @Query("DELETE FROM study_logs")
    suspend fun deleteAllLogs()

    @Query("""
        DELETE FROM study_logs
        WHERE id = (
            SELECT id FROM study_logs
            WHERE cardId = :cardId
            ORDER BY reviewedAt DESC, id DESC
            LIMIT 1
        )
    """)
    suspend fun deleteLatestForCard(cardId: Long)
}
