package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "通用",
    val colorHex: String = "#426B63",
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Long = System.currentTimeMillis()
)
