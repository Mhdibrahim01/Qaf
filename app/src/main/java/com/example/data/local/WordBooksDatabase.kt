package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EntryEntity::class,
        DeckEntity::class,
        WordProgressEntity::class,
        SentenceEntity::class,
        GrammarLessonEntity::class,
        GrammarProgressEntity::class,
        AiHistoryEntity::class,
        ReadingTextEntity::class,
        ReadingProgressEntity::class,
        AiCacheEntity::class,
        FeatureFlagEntity::class,
        FeatureSuggestionEntity::class,
        DeckStudyStateEntity::class,
        CorrectionNotebookEntity::class,
        StudyPlanProfileEntity::class,
        StudyPlanEntity::class,
        DailyChecklistEntity::class,
        QuizResultEntity::class,
        WordDetailEntity::class
    ],
    version = 12,
    exportSchema = false
)
abstract class WordBooksDatabase : RoomDatabase() {
    abstract fun wordBooksDao(): WordBooksDao

    companion object {
        @Volatile
        private var INSTANCE: WordBooksDatabase? = null

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add again_count to word_progress
                db.execSQL("ALTER TABLE word_progress ADD COLUMN again_count INTEGER NOT NULL DEFAULT 0")

                // 2. Create study_plan_profile
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS study_plan_profile (
                        id INTEGER PRIMARY KEY NOT NULL,
                        level TEXT NOT NULL,
                        goal TEXT NOT NULL,
                        minutes_per_day INTEGER NOT NULL,
                        study_days_per_week INTEGER NOT NULL,
                        start_date TEXT NOT NULL,
                        selected_decks_json TEXT NOT NULL,
                        rest_days_json TEXT NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())

                // 3. Create study_plans
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS study_plans (
                        month TEXT PRIMARY KEY NOT NULL,
                        level_start TEXT NOT NULL,
                        level_target TEXT NOT NULL,
                        weeks_json TEXT NOT NULL,
                        daily_template_json TEXT NOT NULL,
                        tips_ar TEXT NOT NULL,
                        source TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """.trimIndent())

                // 4. Create daily_task_checklist
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_task_checklist (
                        date TEXT PRIMARY KEY NOT NULL,
                        tasks_json TEXT NOT NULL,
                        completed_count INTEGER NOT NULL,
                        total_count INTEGER NOT NULL,
                        is_completed INTEGER NOT NULL,
                        completed_at INTEGER,
                        is_adjusted INTEGER NOT NULL
                    )
                """.trimIndent())

                // 5. Create quiz_results
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS quiz_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        quiz_type TEXT NOT NULL,
                        skill TEXT NOT NULL,
                        total_questions INTEGER NOT NULL,
                        correct_answers INTEGER NOT NULL,
                        accuracy_percent REAL NOT NULL,
                        details_json TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_quiz_results_date ON quiz_results (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_quiz_results_skill ON quiz_results (skill)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_quiz_results_quiz_type ON quiz_results (quiz_type)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `word_details` (
                        `key` TEXT NOT NULL,
                        `json` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentences_created_at ON sentences(created_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sentences_english_text ON sentences(english_text)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_word_progress_is_mastered_due_timestamp ON word_progress(is_mastered, due_timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_grammar_lessons_level ON grammar_lessons(level)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_texts_level ON reading_texts(level)")
            }
        }

        fun getInstance(context: Context): WordBooksDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WordBooksDatabase::class.java,
                    "wordbooks.db"
                )
                    .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

