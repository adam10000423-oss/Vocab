package com.example.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grammar_writing_records",
    indices = [Index("course"), Index("updatedAt")]
)
data class GrammarWritingRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val course: String = "通用",
    val title: String,
    val originalText: String,
    val revisedText: String,
    /** JSON array of the full issue snapshots shown when the record was saved. */
    @ColumnInfo(defaultValue = "'[]'") val issuesJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
