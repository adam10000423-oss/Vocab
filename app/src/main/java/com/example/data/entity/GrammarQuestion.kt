package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(
    tableName = "grammar_questions",
    indices = [Index("grammarNoteId")]
)
data class GrammarQuestion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val grammarNoteId: Long,
    @ColumnInfo(defaultValue = "0") val grammarPatternId: Long = 0,
    val type: String = "CLOZE_CHOICE",
    val prompt: String,
    val translation: String = "",
    val answer: String,
    val acceptedAnswers: String = "",
    val options: String = "",
    val explanation: String = "",
    val difficulty: Int = 1,
    val sourceType: String = "MANUAL",
    val sortOrder: Long = System.currentTimeMillis()
)

data class GrammarQuestionDraft(
    val id: Long = 0,
    val grammarPatternIndex: Int = -1,
    val type: String = "CLOZE_CHOICE",
    val prompt: String = "",
    val translation: String = "",
    val answer: String = "",
    val acceptedAnswers: String = "",
    val options: String = "",
    val explanation: String = "",
    val difficulty: Int = 1,
    val sourceType: String = "MANUAL"
)

@Entity(
    tableName = "grammar_weaknesses",
    indices = [Index(value = ["ruleKey"], unique = true), Index("grammarNoteId")]
)
data class GrammarWeakness(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ruleKey: String,
    val grammarNoteId: Long,
    val title: String,
    val originalText: String,
    val correctedText: String,
    val explanation: String = "",
    val occurrenceCount: Int = 1,
    val lastOccurredAt: Long = System.currentTimeMillis()
)

data class GrammarWritingIssue(
    val ruleKey: String,
    val title: String,
    val originalSentence: String,
    val correctedSentence: String,
    val originalText: String,
    val correctedText: String,
    val explanation: String
)

data class GrammarWritingScanResult(
    val recognizedText: String,
    val uncertainParts: List<String> = emptyList()
)

data class GrammarQuizResult(
    val grammarNoteId: Long,
    val totalQuestions: Int,
    val firstTryCorrect: Int,
    val totalAttempts: Int,
    val wrongQuestionIds: List<Long>,
    val wrongCounts: Map<Long, Int>,
    val masteryBefore: Int,
    val masteryAfter: Int
) {
    val accuracyPercent: Int
        get() = if (totalQuestions == 0) 0 else (firstTryCorrect * 100 / totalQuestions)
}
