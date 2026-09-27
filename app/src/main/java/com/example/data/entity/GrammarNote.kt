package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "grammar_notes")
data class GrammarNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val course: String = "通用",
    @ColumnInfo(defaultValue = "'未分類'") val folder: String = "未分類",
    val title: String,
    @ColumnInfo(defaultValue = "'其他'") val category: String = "其他",
    @ColumnInfo(defaultValue = "'未分級'") val level: String = "未分級",
    val summary: String = "",
    val structure: String = "",
    val usage: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val commonMistakes: String = "",
    val comparison: String = "",
    val tags: String = "",
    val sourceType: String = "MANUAL",
    val sourceName: String = "",
    val questionTemplate: String = "",
    val answer: String = "",
    val acceptedAnswers: String = "",
    val options: String = "",
    val explanation: String = "",
    val favorite: Boolean = false,
    val masteryPercent: Int = 0,
    val nextReviewAt: Long = System.currentTimeMillis(),
    val studyStep: Int = 0,
    val attemptCount: Int = 0,
    val correctCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sortOrder: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "grammar_patterns",
    indices = [Index("grammarNoteId")]
)
data class GrammarPattern(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grammarNoteId: Long,
    val title: String = "句型",
    val formula: String,
    val meaning: String = "",
    val usage: String = "",
    val notes: String = "",
    @ColumnInfo(defaultValue = "0") val masteryPercent: Int = 0,
    @ColumnInfo(defaultValue = "0") val nextReviewAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val attemptCount: Int = 0,
    @ColumnInfo(defaultValue = "0") val correctCount: Int = 0,
    val sortOrder: Long = 0
)

@Entity(
    tableName = "grammar_examples",
    indices = [Index("grammarNoteId"), Index("grammarPatternId")]
)
data class GrammarExample(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grammarNoteId: Long,
    val grammarPatternId: Long = 0,
    val sentence: String,
    val translation: String = "",
    val highlightedText: String = "",
    val sortOrder: Long = 0
)

data class GrammarPatternDraft(
    val title: String = "句型",
    val formula: String = "",
    val meaning: String = "",
    val usage: String = "",
    val notes: String = "",
    val examples: List<GrammarExampleDraft> = emptyList()
)

data class GrammarExampleDraft(
    val sentence: String = "",
    val translation: String = "",
    val highlightedText: String = ""
)

data class GrammarDraft(
    val title: String = "",
    val category: String = "其他",
    val level: String = "未分級",
    val summary: String = "",
    val structure: String = "",
    val usage: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val commonMistakes: String = "",
    val comparison: String = "",
    val tags: String = "",
    val questionTemplate: String = "",
    val answer: String = "",
    val acceptedAnswers: String = "",
    val options: String = "",
    val explanation: String = "",
    val patterns: List<GrammarPatternDraft> = emptyList(),
    val questions: List<GrammarQuestionDraft> = emptyList()
)
