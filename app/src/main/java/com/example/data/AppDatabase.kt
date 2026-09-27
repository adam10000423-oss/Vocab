package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.DeckDao
import com.example.data.dao.FlashcardDao
import com.example.data.dao.StudyLogDao
import com.example.data.dao.GrammarNoteDao
import com.example.data.dao.GrammarQuestionDao
import com.example.data.dao.GrammarContentDao
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.entity.StudyLog
import com.example.data.entity.GrammarNote
import com.example.data.entity.GrammarQuestion
import com.example.data.entity.GrammarWeakness
import com.example.data.entity.GrammarPattern
import com.example.data.entity.GrammarExample

@Database(
    entities = [Deck::class, Flashcard::class, StudyLog::class, GrammarNote::class, GrammarQuestion::class, GrammarWeakness::class, GrammarPattern::class, GrammarExample::class],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun studyLogDao(): StudyLogDao
    abstract fun grammarNoteDao(): GrammarNoteDao
    abstract fun grammarQuestionDao(): GrammarQuestionDao
    abstract fun grammarContentDao(): GrammarContentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcards ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN isSuspended INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE flashcards ADD COLUMN mistakeCount INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcards ADD COLUMN normalizedWord TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE flashcards SET normalizedWord = lower(trim(word))")
                db.execSQL(
                    """
                    DELETE FROM flashcards
                    WHERE id NOT IN (
                        SELECT MIN(id)
                        FROM flashcards
                        GROUP BY deckId, normalizedWord
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_flashcards_deckId_normalizedWord " +
                        "ON flashcards(deckId, normalizedWord)"
                )
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE flashcards ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE flashcards SET sortOrder = createdAt")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE decks ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE decks SET sortOrder = createdAt")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grammar_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        course TEXT NOT NULL,
                        title TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        structure TEXT NOT NULL,
                        usage TEXT NOT NULL,
                        exampleSentence TEXT NOT NULL,
                        exampleTranslation TEXT NOT NULL,
                        commonMistakes TEXT NOT NULL,
                        comparison TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        sourceType TEXT NOT NULL,
                        sourceName TEXT NOT NULL,
                        questionTemplate TEXT NOT NULL,
                        answer TEXT NOT NULL,
                        acceptedAnswers TEXT NOT NULL,
                        options TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        favorite INTEGER NOT NULL,
                        masteryPercent INTEGER NOT NULL,
                        nextReviewAt INTEGER NOT NULL,
                        studyStep INTEGER NOT NULL,
                        attemptCount INTEGER NOT NULL,
                        correctCount INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grammar_questions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        grammarNoteId INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        prompt TEXT NOT NULL,
                        translation TEXT NOT NULL,
                        answer TEXT NOT NULL,
                        acceptedAnswers TEXT NOT NULL,
                        options TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        difficulty INTEGER NOT NULL,
                        sourceType TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_questions_grammarNoteId ON grammar_questions(grammarNoteId)")
                db.execSQL(
                    """
                    INSERT INTO grammar_questions (
                        grammarNoteId, type, prompt, translation, answer, acceptedAnswers,
                        options, explanation, difficulty, sourceType, sortOrder
                    )
                    SELECT id, 'CLOZE_CHOICE', questionTemplate, exampleTranslation, answer,
                        acceptedAnswers, options, explanation, 1, sourceType, createdAt
                    FROM grammar_notes WHERE questionTemplate != '' AND answer != ''
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grammar_weaknesses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ruleKey TEXT NOT NULL,
                        grammarNoteId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        originalText TEXT NOT NULL,
                        correctedText TEXT NOT NULL,
                        explanation TEXT NOT NULL,
                        occurrenceCount INTEGER NOT NULL,
                        lastOccurredAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_grammar_weaknesses_ruleKey ON grammar_weaknesses(ruleKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_weaknesses_grammarNoteId ON grammar_weaknesses(grammarNoteId)")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE grammar_notes ADD COLUMN category TEXT NOT NULL DEFAULT '其他'")
                db.execSQL("ALTER TABLE grammar_notes ADD COLUMN level TEXT NOT NULL DEFAULT '未分級'")
                db.execSQL("ALTER TABLE grammar_questions ADD COLUMN grammarPatternId INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grammar_patterns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        grammarNoteId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        formula TEXT NOT NULL,
                        meaning TEXT NOT NULL,
                        usage TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_patterns_grammarNoteId ON grammar_patterns(grammarNoteId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS grammar_examples (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        grammarNoteId INTEGER NOT NULL,
                        grammarPatternId INTEGER NOT NULL,
                        sentence TEXT NOT NULL,
                        translation TEXT NOT NULL,
                        highlightedText TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_examples_grammarNoteId ON grammar_examples(grammarNoteId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_examples_grammarPatternId ON grammar_examples(grammarPatternId)")
                db.execSQL(
                    """
                    INSERT INTO grammar_patterns (grammarNoteId, title, formula, meaning, usage, notes, sortOrder)
                    SELECT id, '主要句型', structure, summary, usage, '', 0
                    FROM grammar_notes WHERE trim(structure) != ''
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO grammar_examples (grammarNoteId, grammarPatternId, sentence, translation, highlightedText, sortOrder)
                    SELECT n.id, COALESCE((SELECT p.id FROM grammar_patterns p WHERE p.grammarNoteId = n.id ORDER BY p.sortOrder, p.id LIMIT 1), 0),
                        n.exampleSentence, n.exampleTranslation, '', 0
                    FROM grammar_notes n WHERE trim(n.exampleSentence) != ''
                    """.trimIndent()
                )
            }
        }
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE grammar_patterns ADD COLUMN masteryPercent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE grammar_patterns ADD COLUMN nextReviewAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE grammar_patterns ADD COLUMN attemptCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE grammar_patterns ADD COLUMN correctCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE grammar_patterns SET nextReviewAt = (SELECT nextReviewAt FROM grammar_notes WHERE grammar_notes.id = grammar_patterns.grammarNoteId)")
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vocab_pulse_db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
