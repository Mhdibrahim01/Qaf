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
        AttemptEntity::class,
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
    version = 15,
    exportSchema = true
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

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE word_progress ADD COLUMN entry_id TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_word_progress_entry_id ON word_progress(entry_id)")
                db.execSQL("""
                    UPDATE word_progress
                    SET entry_id = (
                        SELECT e.id FROM entries e
                        WHERE e.deck = word_progress.deck
                          AND LOWER(TRIM(e.text)) = LOWER(TRIM(word_progress.text))
                        ORDER BY CAST(e.id AS INTEGER) ASC, e.id ASC
                        LIMIT 1
                    )
                    WHERE entry_id IS NULL
                """.trimIndent())
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN effective_level TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_entries_effective_level ON entries(effective_level)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_entries_deck_effective_level ON entries(deck, effective_level)")

                db.execSQL("CREATE INDEX IF NOT EXISTS temp_idx_ox_lower ON entries(LOWER(TRIM(text)), deck)")
                db.execSQL("""
                    UPDATE entries
                    SET effective_level = CASE
                        WHEN deck = 'oxford5000' AND UPPER(TRIM(level)) IN ('A1', 'A2', 'B1', 'B2', 'C1')
                            THEN UPPER(TRIM(level))
                        WHEN EXISTS (
                            SELECT 1 FROM entries ox
                            WHERE ox.deck = 'oxford5000'
                              AND LOWER(TRIM(ox.text)) = LOWER(TRIM(entries.text))
                              AND UPPER(TRIM(ox.level)) IN ('A1', 'A2', 'B1', 'B2', 'C1')
                        ) THEN (
                            SELECT UPPER(TRIM(ox.level)) FROM entries ox
                            WHERE ox.deck = 'oxford5000'
                              AND LOWER(TRIM(ox.text)) = LOWER(TRIM(entries.text))
                              AND UPPER(TRIM(ox.level)) IN ('A1', 'A2', 'B1', 'B2', 'C1')
                            ORDER BY
                                CASE UPPER(TRIM(ox.level))
                                    WHEN 'A1' THEN 1
                                    WHEN 'A2' THEN 2
                                    WHEN 'B1' THEN 3
                                    WHEN 'B2' THEN 4
                                    WHEN 'C1' THEN 5
                                    ELSE 6
                                END ASC,
                                CAST(ox.id AS INTEGER) ASC,
                                ox.id ASC
                            LIMIT 1
                        )
                        WHEN deck IN ('oxford5000', 'oxford_phrases', 'cefr_c1')
                             AND UPPER(TRIM(level)) IN ('A1', 'A2', 'B1', 'B2', 'C1')
                            THEN UPPER(TRIM(level))
                        ELSE NULL
                    END
                """.trimIndent())
                db.execSQL("DROP INDEX IF EXISTS temp_idx_ox_lower")

                db.execSQL("""
                    UPDATE word_progress
                    SET entry_id = (
                        SELECT e.id FROM entries e
                        WHERE e.deck = word_progress.deck
                          AND LOWER(TRIM(e.text)) = LOWER(TRIM(word_progress.text))
                        ORDER BY CAST(e.id AS INTEGER) ASC, e.id ASC
                        LIMIT 1
                    )
                    WHERE entry_id IS NULL
                """.trimIndent())

                db.execSQL("ALTER TABLE study_plan_profile ADD COLUMN start_at_user_level INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add first_learned_timestamp to word_progress
                db.execSQL("ALTER TABLE word_progress ADD COLUMN first_learned_timestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_word_progress_first_learned_timestamp ON word_progress(first_learned_timestamp)")

                // Backfill first_learned_timestamp ONLY for cards that actually had a real SM-2 rating
                // (repetitions > 0 OR again_count > 0), never for favorite-only or manual-mastery-only rows.
                db.execSQL("""
                    UPDATE word_progress
                    SET first_learned_timestamp = last_reviewed_timestamp
                    WHERE (repetitions > 0 OR again_count > 0) AND last_reviewed_timestamp > 0
                """.trimIndent())

                // 2. Create append-only attempts table (no synthetic history is invented during migration)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `attempts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `activity_type` TEXT NOT NULL,
                        `item_id` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `sm2_rating` INTEGER,
                        `is_correct` INTEGER,
                        `score` REAL,
                        `deck` TEXT
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attempts_timestamp` ON `attempts` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attempts_activity_type_item_id` ON `attempts` (`activity_type`, `item_id`)")
            }
        }

        fun getInstance(context: Context): WordBooksDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WordBooksDatabase::class.java,
                    "wordbooks.db"
                )
                    .addMigrations(
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

