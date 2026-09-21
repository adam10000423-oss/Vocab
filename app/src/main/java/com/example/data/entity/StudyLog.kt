package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val rating: Int, // 1: Again, 2: Hard, 3: Good, 4: Easy
    val reviewedAt: Long = System.currentTimeMillis()
)
