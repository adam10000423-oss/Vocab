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
import com.example.data.entity.Deck
import com.example.data.entity.Flashcard
import com.example.data.entity.StudyLog

@Database(
    entities = [Deck::class, Flashcard::class, StudyLog::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun studyLogDao(): StudyLogDao

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
                    MIGRATION_4_5
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
