package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["word"]),
        Index(value = ["deckId", "normalizedWord"], unique = true)
    ]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val word: String,
    val normalizedWord: String = word.trim().lowercase(),
    val phonetic: String = "",
    val partOfSpeech: String = "",
    val definition: String,
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val imageUrl: String? = null,
    val notes: String = "",
    val isMastered: Boolean = false,
    val isFavorite: Boolean = false,
    val isSuspended: Boolean = false,
    val mistakeCount: Int = 0,
    
    // Spaced Repetition Parameters (SuperMemo SM-2)
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val repetitionCount: Int = 0,
    val nextReviewTimestamp: Long = System.currentTimeMillis(),
    val lastReviewedTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Long = System.currentTimeMillis()
)
