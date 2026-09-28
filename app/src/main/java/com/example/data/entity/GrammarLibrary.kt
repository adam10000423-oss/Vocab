package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grammar_libraries")
data class GrammarLibrary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val course: String = "通用",
    val name: String,
    val description: String = "",
    val colorHex: String = "#426B63",
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Long = System.currentTimeMillis()
)
