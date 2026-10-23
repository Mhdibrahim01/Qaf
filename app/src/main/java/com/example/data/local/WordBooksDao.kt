package com.example.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBooksDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntriesBatchIgnore(entries: List<EntryEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEntryIgnore(entry: EntryEntity): Long

    /**
     * Upserts content fields by `id` without touching `word_progress` (favorites, mastered, SRS)
     * or `sentences`, and NEVER overwrites `arabic` if `arabic_source = 'user'`.
     */
    @Query(
        """
        UPDATE entries
        SET type = CASE WHEN :type != '' THEN :type ELSE type END,
            deck = CASE WHEN :deck != '' THEN :deck ELSE deck END,
            text = CASE WHEN :text != '' THEN :text ELSE text END,
            pos = :pos,
            level = :level,
            effective_level = :effectiveLevel,
            definition_en = CASE WHEN :definitionEn != '' THEN :definitionEn ELSE definition_en END,
            note_en = CASE WHEN :noteEn != '' THEN :noteEn ELSE note_en END,
            examples_en = CASE WHEN :examplesEn != '' THEN :examplesEn ELSE examples_en END,
            examples_ar = CASE WHEN :examplesAr != '' THEN :examplesAr ELSE examples_ar END,
            definition_url = CASE WHEN :definitionUrl != '' THEN :definitionUrl ELSE definition_url END,
            audio_url = CASE WHEN :audioUrl != '' THEN :audioUrl ELSE audio_url END,
            ipa = CASE WHEN :ipa != '' THEN :ipa ELSE ipa END,
            arabic = CASE
                WHEN arabic_source = 'user' THEN arabic
                WHEN TRIM(:arabic) != '' THEN :arabic
                ELSE arabic
            END,
            arabic_source = CASE
                WHEN arabic_source = 'user' THEN 'user'
                WHEN TRIM(:arabic) != '' THEN :arabicSource
                ELSE arabic_source
            END,
            data_version = :dataVersion
        WHERE id = :id
        """
    )
    suspend fun updateEntryContentByIdPreservingUserArabic(
        id: String,
        type: String,
        deck: String,
        text: String,
        pos: String,
        level: String,
        effectiveLevel: String?,
        definitionEn: String,
        noteEn: String,
        examplesEn: String,
        examplesAr: String,
        definitionUrl: String,
        audioUrl: String,
        ipa: String,
        arabic: String,
        arabicSource: String,
        dataVersion: Int
    )

    @Query(
        """
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
        """
    )
    suspend fun backfillEffectiveLevels()

    @Query("SELECT * FROM entries WHERE deck = 'oxford5000' AND UPPER(TRIM(level)) IN ('A1', 'A2', 'B1', 'B2', 'C1')")
    suspend fun getOxfordGradedEntries(): List<EntryEntity>

    @Transaction
    suspend fun upsertEntryContentById(entry: EntryEntity) {
        val rowId = insertEntryIgnore(entry)
        if (rowId == -1L) {
            val resolvedSource = if (entry.arabic.isNotBlank()) {
                entry.arabicSource.ifBlank { "file" }
            } else {
                ""
            }
            updateEntryContentByIdPreservingUserArabic(
                id = entry.id,
                type = entry.type,
                deck = entry.deck,
                text = entry.text,
                pos = entry.pos,
                level = entry.level,
                effectiveLevel = entry.effectiveLevel,
                definitionEn = entry.definitionEn,
                noteEn = entry.noteEn,
                examplesEn = entry.examplesEn,
                examplesAr = entry.examplesAr,
                definitionUrl = entry.definitionUrl,
                audioUrl = entry.audioUrl,
                ipa = entry.ipa,
                arabic = entry.arabic,
                arabicSource = resolvedSource,
                dataVersion = entry.dataVersion
            )
        }
    }

    @Transaction
    suspend fun upsertEntriesBatchPreservingUserEdits(entries: List<EntryEntity>) {
        val results = insertEntriesBatchIgnore(entries)
        for (i in entries.indices) {
            if (results[i] == -1L) {
                val entry = entries[i]
                val resolvedSource = if (entry.arabic.isNotBlank()) {
                    entry.arabicSource.ifBlank { "file" }
                } else {
                    ""
                }
                updateEntryContentByIdPreservingUserArabic(
                    id = entry.id,
                    type = entry.type,
                    deck = entry.deck,
                    text = entry.text,
                    pos = entry.pos,
                    level = entry.level,
                    effectiveLevel = entry.effectiveLevel,
                    definitionEn = entry.definitionEn,
                    noteEn = entry.noteEn,
                    examplesEn = entry.examplesEn,
                    examplesAr = entry.examplesAr,
                    definitionUrl = entry.definitionUrl,
                    audioUrl = entry.audioUrl,
                    ipa = entry.ipa,
                    arabic = entry.arabic,
                    arabicSource = resolvedSource,
                    dataVersion = entry.dataVersion
                )
            }
        }
        if (entries.size < 100 && getTotalEntryCount() < 1000) {
            backfillEffectiveLevels()
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDecks(decks: List<DeckEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWordProgress(progress: WordProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFeatureFlags(flags: List<FeatureFlagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAiCache(cache: AiCacheEntity)

    @Query("SELECT * FROM ai_cache WHERE cache_key = :cacheKey LIMIT 1")
    suspend fun getAiCache(cacheKey: String): AiCacheEntity?

    @Query("DELETE FROM ai_cache WHERE cache_key NOT LIKE :prefixPattern")
    suspend fun deleteStaleAiCacheNotStartingWith(prefixPattern: String)

    @Query("DELETE FROM entries")
    suspend fun deleteAllEntries()

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun getTotalEntryCount(): Int

    @Query("SELECT COUNT(*) FROM entries")
    fun observeTotalEntryCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT deck || ':' || LOWER(TRIM(text))) FROM entries")
    suspend fun getTotalUniqueWordCount(): Int

    @Query("SELECT COUNT(DISTINCT deck || ':' || LOWER(TRIM(text))) FROM entries")
    fun observeTotalUniqueWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM entries WHERE TRIM(arabic) = ''")
    fun observeEmptyArabicCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM entries WHERE TRIM(arabic) = ''")
    suspend fun getEmptyArabicCount(): Int

    @Query("SELECT * FROM entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: String): EntryEntity?

    /**
     * Per-deck breakdown of Arabic provenance: from file, from AI, from user, and still empty.
     */
    @Query(
        """
        SELECT deck AS deck,
               COUNT(*) AS totalRows,
               SUM(CASE WHEN TRIM(arabic) != '' AND (arabic_source = 'file' OR arabic_source = '') THEN 1 ELSE 0 END) AS fromFileCount,
               SUM(CASE WHEN TRIM(arabic) != '' AND arabic_source = 'ai' THEN 1 ELSE 0 END) AS fromAiCount,
               SUM(CASE WHEN TRIM(arabic) != '' AND arabic_source = 'user' THEN 1 ELSE 0 END) AS fromUserCount,
               SUM(CASE WHEN TRIM(arabic) = '' THEN 1 ELSE 0 END) AS emptyCount
        FROM entries
        GROUP BY deck
        """
    )
    fun observeDeckArabicStats(): Flow<List<DeckArabicStats>>

    @Query(
        """
        SELECT deck AS deck,
               COUNT(*) AS totalRows,
               SUM(CASE WHEN TRIM(arabic) != '' AND (arabic_source = 'file' OR arabic_source = '') THEN 1 ELSE 0 END) AS fromFileCount,
               SUM(CASE WHEN TRIM(arabic) != '' AND arabic_source = 'ai' THEN 1 ELSE 0 END) AS fromAiCount,
               SUM(CASE WHEN TRIM(arabic) != '' AND arabic_source = 'user' THEN 1 ELSE 0 END) AS fromUserCount,
               SUM(CASE WHEN TRIM(arabic) = '' THEN 1 ELSE 0 END) AS emptyCount
        FROM entries
        GROUP BY deck
        """
    )
    suspend fun getDeckArabicStats(): List<DeckArabicStats>

    @Query("SELECT MAX(data_version) FROM entries")
    suspend fun getMaxDataVersion(): Int?

    @Query("SELECT * FROM entries WHERE TRIM(arabic) = '' AND arabic_source != 'user' ORDER BY CAST(id AS INTEGER) ASC LIMIT :limit")
    suspend fun getUntranslatedEntriesBatch(limit: Int): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE deck = :deck AND TRIM(arabic) = '' AND arabic_source != 'user' ORDER BY CAST(id AS INTEGER) ASC LIMIT :limit")
    suspend fun getUntranslatedEntriesForDeckBatch(deck: String, limit: Int): List<EntryEntity>

    /**
     * Returns untranslated entries ordered by:
     * 1. Favorites first (`word_progress.is_favorite = 1`)
     * 2. Lower CEFR levels first (A1 -> A2 -> B1 -> B2 -> C1 -> C2 -> other)
     * 3. Stable numeric ID ascending
     * Skips entries that already have non-empty `arabic` or where `arabic_source` is `'file'` or `'user'`.
     */
    @Query(
        """
        SELECT e.* FROM entries e
        LEFT JOIN word_progress wp
            ON wp.deck = e.deck AND wp.text = LOWER(e.text)
        WHERE (:deckFilter = '' OR e.deck = :deckFilter)
          AND TRIM(e.arabic) = ''
          AND e.arabic_source NOT IN ('file', 'user')
        ORDER BY
          CASE WHEN wp.is_favorite = 1 THEN 0 ELSE 1 END ASC,
          CASE UPPER(TRIM(e.level))
              WHEN 'A1' THEN 1
              WHEN 'A2' THEN 2
              WHEN 'B1' THEN 3
              WHEN 'B2' THEN 4
              WHEN 'C1' THEN 5
              WHEN 'C2' THEN 6
              ELSE 7
          END ASC,
          CAST(e.id AS INTEGER) ASC,
          e.id ASC
        LIMIT :limit
        """
    )
    suspend fun getUntranslatedEntriesPrioritized(deckFilter: String, limit: Int): List<EntryEntity>

    @Query(
        """
        SELECT e.* FROM entries e
        WHERE e.id IN (:ids)
          AND TRIM(e.arabic) = ''
          AND e.arabic_source NOT IN ('file', 'user')
        """
    )
    suspend fun getUntranslatedEntriesByIds(ids: List<String>): List<EntryEntity>

    @Query(
        """
        SELECT * FROM entries
        WHERE LOWER(TRIM(text)) = LOWER(TRIM(:text))
          AND LOWER(TRIM(pos)) = LOWER(TRIM(:pos))
          AND TRIM(arabic) != ''
        LIMIT 1
        """
    )
    suspend fun findTranslatedEntryByTextAndPos(text: String, pos: String): EntryEntity?

    @Query("SELECT * FROM entries WHERE TRIM(arabic) != ''")
    suspend fun getAllTranslatedEntries(): List<EntryEntity>

    @Query(
        """
        UPDATE entries
        SET arabic = :arabic,
            arabic_source = 'ai',
            ipa = CASE WHEN :ipa != '' THEN :ipa ELSE ipa END,
            examples_ar = CASE WHEN :examplesAr != '' THEN :examplesAr ELSE examples_ar END
        WHERE id = :primaryId
          AND TRIM(arabic) = ''
          AND arabic_source NOT IN ('file', 'user')
        """
    )
    suspend fun updateAiTranslationForPrimaryId(
        primaryId: String,
        arabic: String,
        ipa: String,
        examplesAr: String
    ): Int

    @Query(
        """
        UPDATE entries
        SET arabic = :arabic,
            arabic_source = 'ai',
            ipa = CASE WHEN :ipa != '' THEN :ipa ELSE ipa END,
            examples_ar = CASE
                WHEN TRIM(examples_en) = TRIM(:primaryExamplesEn) AND :examplesAr != '' THEN :examplesAr
                ELSE examples_ar
            END
        WHERE text = :text
          AND pos = :pos
          AND TRIM(arabic) = ''
          AND arabic_source NOT IN ('file', 'user')
        """
    )
    suspend fun updateAiTranslationForMatchingTextAndPos(
        text: String,
        pos: String,
        arabic: String,
        ipa: String,
        primaryExamplesEn: String,
        examplesAr: String
    ): Int

    suspend fun updateAiTranslationForSharedTextAndPos(
        primaryId: String,
        text: String,
        pos: String,
        arabic: String,
        ipa: String,
        primaryExamplesEn: String,
        examplesAr: String
    ): Int {
        var count = updateAiTranslationForPrimaryId(primaryId, arabic, ipa, examplesAr)
        if (text.isNotBlank()) {
            count += updateAiTranslationForMatchingTextAndPos(text, pos, arabic, ipa, primaryExamplesEn, examplesAr)
        }
        return count
    }

    /**
     * Permanently saves a full batch of validated AI translations in a single Room database transaction
     * so stopping at any moment loses nothing.
     */
    @Transaction
    suspend fun saveAiTranslatedBatchTransaction(items: List<AiBatchSavePayload>): Int {
        var totalUpdatedRows = 0
        for (item in items) {
            totalUpdatedRows += updateAiTranslationForSharedTextAndPos(
                primaryId = item.id,
                text = item.text,
                pos = item.pos,
                arabic = item.arabic,
                ipa = item.ipa,
                primaryExamplesEn = item.examplesEn,
                examplesAr = item.examplesAr
            )
        }
        return totalUpdatedRows
    }

    /**
     * Updates AI-enriched fields for an entry, NEVER overwriting `arabic` if `arabic_source` is `'file'` or `'user'`.
     */
    @Query(
        """
        UPDATE entries
        SET arabic = CASE
                WHEN arabic_source IN ('user', 'file') THEN arabic
                WHEN TRIM(:arabic) != '' THEN :arabic
                ELSE arabic
            END,
            arabic_source = CASE
                WHEN arabic_source IN ('user', 'file') THEN arabic_source
                WHEN TRIM(:arabic) != '' THEN 'ai'
                ELSE arabic_source
            END,
            ipa = CASE WHEN :ipa != '' THEN :ipa ELSE ipa END,
            definition_en = CASE WHEN definition_en = '' AND :definitionEn != '' THEN :definitionEn ELSE definition_en END,
            examples_en = CASE WHEN examples_en = '' AND :examplesEn != '' THEN :examplesEn ELSE examples_en END,
            examples_ar = CASE WHEN :examplesAr != '' THEN :examplesAr ELSE examples_ar END
        WHERE id = :id
        """
    )
    suspend fun updateEntryAiEnrichment(
        id: String,
        arabic: String,
        ipa: String,
        definitionEn: String,
        examplesEn: String,
        examplesAr: String
    )

    /**
     * Saves a user-authored Arabic meaning and marks `arabic_source = 'user'` so imports and AI
     * never overwrite it.
     */
    @Query(
        """
        UPDATE entries
        SET arabic = :userArabic,
            arabic_source = CASE WHEN TRIM(:userArabic) = '' THEN '' ELSE 'user' END
        WHERE id = :id
        """
    )
    suspend fun updateEntryUserArabicById(id: String, userArabic: String)

    @Query(
        """
        UPDATE entries
        SET arabic = :userArabic,
            arabic_source = CASE WHEN TRIM(:userArabic) = '' THEN '' ELSE 'user' END
        WHERE deck = :deck AND LOWER(text) = LOWER(:text)
        """
    )
    suspend fun updateEntryUserArabicForWord(deck: String, text: String, userArabic: String)

    @Query("SELECT LOWER(text) FROM entries WHERE deck = :deck")
    suspend fun getExistingWordsInDeck(deck: String): List<String>

    @Query(
        """
        SELECT COALESCE(MAX(CAST(id AS INTEGER)), :minIdMinusOne)
        FROM entries
        WHERE deck = :deck AND CAST(id AS INTEGER) >= :minId
        """
    )
    suspend fun getMaxNumericIdForDeckInRange(deck: String, minId: Int, minIdMinusOne: Int): Int

    @Query("SELECT COUNT(*) FROM entries WHERE INSTR(id, '_') > 0 OR definition_en LIKE 'Core Oxford 5000 vocabulary definition%'")
    suspend fun getFakeLegacyEntryCount(): Int

    @Query("SELECT COUNT(*) FROM entries WHERE deck = :deckId")
    suspend fun getEntryCountForDeck(deckId: String): Int

    @Query("SELECT COUNT(DISTINCT LOWER(text)) FROM entries WHERE deck = :deckId")
    suspend fun getUniqueWordCountForDeck(deckId: String): Int

    @Query("SELECT COUNT(DISTINCT LOWER(TRIM(text))) FROM entries WHERE deck = :deckId AND (level = :level OR effective_level = :level)")
    suspend fun getUniqueWordCountForDeckByLevel(deckId: String, level: String): Int

    @Query("SELECT * FROM decks ORDER BY display_order ASC")
    fun observeDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks ORDER BY display_order ASC")
    suspend fun getDecksList(): List<DeckEntity>

    @Query(
        """
        SELECT
            e.id AS id,
            e.type AS type,
            e.deck AS deck,
            e.text AS text,
            GROUP_CONCAT(DISTINCT e.pos) AS pos,
            MIN(e.level) AS level,
            e.definition_en AS definition_en,
            e.note_en AS note_en,
            e.examples_en AS examples_en,
            e.examples_ar AS examples_ar,
            e.definition_url AS definition_url,
            e.audio_url AS audio_url,
            e.arabic AS arabic,
            e.arabic_source AS arabic_source,
            e.ipa AS ipa,
            e.syllables AS syllables,
            e.extra_examples_json AS extra_examples_json,
            e.data_version AS data_version,
            MIN(COALESCE(e.effective_level, e.level)) AS effective_level
        FROM entries e
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter OR e.effective_level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
        GROUP BY e.deck, LOWER(TRIM(e.text))
        ORDER BY
          CASE WHEN :sortOrder = 'AZ' THEN LOWER(TRIM(e.text)) END ASC,
          CASE WHEN :sortOrder = 'ZA' THEN LOWER(TRIM(e.text)) END DESC,
          CASE WHEN :sortOrder = 'LEVEL' THEN
              CASE MIN(CASE UPPER(COALESCE(e.effective_level, e.level))
                  WHEN 'A1' THEN 1
                  WHEN 'A2' THEN 2
                  WHEN 'B1' THEN 3
                  WHEN 'B2' THEN 4
                  WHEN 'C1' THEN 5
                  ELSE 6
              END)
                  WHEN 1 THEN 1
                  WHEN 2 THEN 2
                  WHEN 3 THEN 3
                  WHEN 4 THEN 4
                  WHEN 5 THEN 5
                  ELSE 6
              END
          END ASC,
          LOWER(TRIM(e.text)) ASC
        """
    )
    fun getPagedEntries(
        deckId: String,
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String,
        sortOrder: String
    ): PagingSource<Int, EntryEntity>

    @Query(
        """
        SELECT
            e.id AS id,
            e.type AS type,
            e.deck AS deck,
            e.text AS text,
            GROUP_CONCAT(DISTINCT e.pos) AS pos,
            MIN(e.level) AS level,
            e.definition_en AS definition_en,
            e.note_en AS note_en,
            e.examples_en AS examples_en,
            e.examples_ar AS examples_ar,
            e.definition_url AS definition_url,
            e.audio_url AS audio_url,
            e.arabic AS arabic,
            e.arabic_source AS arabic_source,
            e.ipa AS ipa,
            e.syllables AS syllables,
            e.extra_examples_json AS extra_examples_json,
            e.data_version AS data_version,
            MIN(COALESCE(e.effective_level, e.level)) AS effective_level
        FROM entries e
        INNER JOIN word_progress wp
            ON wp.deck = e.deck AND wp.text = LOWER(TRIM(e.text)) AND wp.is_favorite = 1
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter OR e.effective_level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
        GROUP BY e.deck, LOWER(TRIM(e.text))
        ORDER BY
          CASE WHEN :sortOrder = 'AZ' THEN LOWER(TRIM(e.text)) END ASC,
          CASE WHEN :sortOrder = 'ZA' THEN LOWER(TRIM(e.text)) END DESC,
          CASE WHEN :sortOrder = 'LEVEL' THEN
              CASE MIN(CASE UPPER(COALESCE(e.effective_level, e.level))
                  WHEN 'A1' THEN 1
                  WHEN 'A2' THEN 2
                  WHEN 'B1' THEN 3
                  WHEN 'B2' THEN 4
                  WHEN 'C1' THEN 5
                  ELSE 6
              END)
                  WHEN 1 THEN 1
                  WHEN 2 THEN 2
                  WHEN 3 THEN 3
                  WHEN 4 THEN 4
                  WHEN 5 THEN 5
                  ELSE 6
              END
          END ASC,
          LOWER(TRIM(e.text)) ASC
        """
    )
    fun getPagedFavoriteEntries(
        deckId: String = "",
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String,
        sortOrder: String
    ): PagingSource<Int, EntryEntity>

    @Query(
        """
        SELECT
            e.id AS id,
            e.type AS type,
            e.deck AS deck,
            e.text AS text,
            GROUP_CONCAT(DISTINCT e.pos) AS pos,
            MIN(e.level) AS level,
            e.definition_en AS definition_en,
            e.note_en AS note_en,
            e.examples_en AS examples_en,
            e.examples_ar AS examples_ar,
            e.definition_url AS definition_url,
            e.audio_url AS audio_url,
            e.arabic AS arabic,
            e.arabic_source AS arabic_source,
            e.ipa AS ipa,
            e.syllables AS syllables,
            e.extra_examples_json AS extra_examples_json,
            e.data_version AS data_version,
            MIN(COALESCE(e.effective_level, e.level)) AS effective_level
        FROM entries e
        LEFT JOIN word_progress wp
            ON wp.deck = e.deck AND wp.text = LOWER(TRIM(e.text))
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND COALESCE(wp.is_mastered, 0) = 0
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter OR e.effective_level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
        GROUP BY e.deck, LOWER(TRIM(e.text))
        ORDER BY
          CASE WHEN :sortOrder = 'AZ' THEN LOWER(TRIM(e.text)) END ASC,
          CASE WHEN :sortOrder = 'ZA' THEN LOWER(TRIM(e.text)) END DESC,
          CASE WHEN :sortOrder = 'LEVEL' THEN
              CASE MIN(CASE UPPER(COALESCE(e.effective_level, e.level))
                  WHEN 'A1' THEN 1
                  WHEN 'A2' THEN 2
                  WHEN 'B1' THEN 3
                  WHEN 'B2' THEN 4
                  WHEN 'C1' THEN 5
                  ELSE 6
              END)
                  WHEN 1 THEN 1
                  WHEN 2 THEN 2
                  WHEN 3 THEN 3
                  WHEN 4 THEN 4
                  WHEN 5 THEN 5
                  ELSE 6
              END
          END ASC,
          LOWER(TRIM(e.text)) ASC
        """
    )
    fun getPagedLearningEntries(
        deckId: String,
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String,
        sortOrder: String
    ): PagingSource<Int, EntryEntity>

    @Query(
        """
        SELECT
            e.id AS id,
            e.type AS type,
            e.deck AS deck,
            e.text AS text,
            GROUP_CONCAT(DISTINCT e.pos) AS pos,
            MIN(e.level) AS level,
            e.definition_en AS definition_en,
            e.note_en AS note_en,
            e.examples_en AS examples_en,
            e.examples_ar AS examples_ar,
            e.definition_url AS definition_url,
            e.audio_url AS audio_url,
            e.arabic AS arabic,
            e.arabic_source AS arabic_source,
            e.ipa AS ipa,
            e.syllables AS syllables,
            e.extra_examples_json AS extra_examples_json,
            e.data_version AS data_version,
            MIN(COALESCE(e.effective_level, e.level)) AS effective_level
        FROM entries e
        INNER JOIN word_progress wp
            ON wp.deck = e.deck AND wp.text = LOWER(TRIM(e.text)) AND wp.is_mastered = 1
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter OR e.effective_level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
        GROUP BY e.deck, LOWER(TRIM(e.text))
        ORDER BY
          CASE WHEN :sortOrder = 'AZ' THEN LOWER(TRIM(e.text)) END ASC,
          CASE WHEN :sortOrder = 'ZA' THEN LOWER(TRIM(e.text)) END DESC,
          CASE WHEN :sortOrder = 'LEVEL' THEN
              CASE MIN(CASE UPPER(COALESCE(e.effective_level, e.level))
                  WHEN 'A1' THEN 1
                  WHEN 'A2' THEN 2
                  WHEN 'B1' THEN 3
                  WHEN 'B2' THEN 4
                  WHEN 'C1' THEN 5
                  ELSE 6
              END)
                  WHEN 1 THEN 1
                  WHEN 2 THEN 2
                  WHEN 3 THEN 3
                  WHEN 4 THEN 4
                  WHEN 5 THEN 5
                  ELSE 6
              END
          END ASC,
          LOWER(TRIM(e.text)) ASC
        """
    )
    fun getPagedMasteredEntries(
        deckId: String,
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String,
        sortOrder: String
    ): PagingSource<Int, EntryEntity>

    @Query(
        """
        SELECT COUNT(DISTINCT deck || ':' || LOWER(TRIM(text))) FROM entries
        WHERE (:deckId IS NULL OR :deckId = '' OR deck = :deckId)
          AND LOWER(TRIM(text)) < :letterPrefix
        """
    )
    suspend fun getEntryIndexBeforeLetter(deckId: String?, letterPrefix: String): Int

    @Query(
        """
        SELECT COUNT(DISTINCT e.deck || ':' || LOWER(TRIM(e.text))) FROM entries e
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter OR e.effective_level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
          AND LOWER(TRIM(e.text)) < :letterPrefix
        """
    )
    suspend fun getFilteredEntryIndexBeforeLetter(
        deckId: String,
        letterPrefix: String,
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT e.deck || ':' || LOWER(TRIM(e.text))) FROM entries e
        INNER JOIN word_progress wp
            ON wp.deck = e.deck AND wp.text = LOWER(TRIM(e.text)) AND wp.is_favorite = 1
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter OR e.effective_level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
          AND LOWER(TRIM(e.text)) < :letterPrefix
        """
    )
    suspend fun getFavoriteFilteredEntryIndexBeforeLetter(
        deckId: String,
        letterPrefix: String,
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT e.deck || ':' || LOWER(TRIM(e.text))) FROM entries e
        INNER JOIN word_progress wp
            ON wp.deck = e.deck AND wp.text = LOWER(TRIM(e.text)) AND wp.is_mastered = 1
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter OR e.effective_level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
          AND LOWER(TRIM(e.text)) < :letterPrefix
        """
    )
    suspend fun getMasteredFilteredEntryIndexBeforeLetter(
        deckId: String,
        letterPrefix: String,
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String
    ): Int

    @Query(
        """
        SELECT COUNT(DISTINCT e.deck || ':' || LOWER(TRIM(e.text))) FROM entries e
        LEFT JOIN word_progress wp
            ON wp.deck = e.deck AND wp.text = LOWER(TRIM(e.text))
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND COALESCE(wp.is_mastered, 0) = 0
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter OR e.effective_level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
          AND LOWER(TRIM(e.text)) < :letterPrefix
        """
    )
    suspend fun getLearningFilteredEntryIndexBeforeLetter(
        deckId: String,
        letterPrefix: String,
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String
    ): Int

    @Query(
        """
        SELECT UPPER(SUBSTR(e.text, 1, 1)) AS initial, COUNT(*) AS count
        FROM entries e
        LEFT JOIN word_progress wp
            ON wp.deck = e.deck AND wp.text = LOWER(e.text)
        WHERE (:deckId = '' OR e.deck = :deckId)
          AND (:typeFilter = '' OR e.type = :typeFilter)
          AND (:levelFilter = '' OR e.level = :levelFilter)
          AND (
              :posFilter = ''
              OR LOWER(e.pos) = LOWER(:posFilter)
              OR (:posFilter = 'noun' AND LOWER(e.pos) IN ('noun', 'n.', 'n'))
              OR (:posFilter = 'verb' AND LOWER(e.pos) IN ('verb', 'v.', 'v', 'Phrasal Verb'))
              OR (:posFilter = 'adjective' AND LOWER(e.pos) IN ('adjective', 'adj.', 'adj'))
              OR (:posFilter = 'adverb' AND LOWER(e.pos) IN ('adverb', 'adv.', 'adv'))
              OR (:posFilter = 'preposition' AND LOWER(e.pos) IN ('preposition', 'prep.', 'prep'))
          )
          AND (
              :searchQuery = ''
              OR LOWER(e.text) LIKE '%' || LOWER(:searchQuery) || '%'
              OR e.arabic LIKE '%' || :searchQuery || '%'
              OR LOWER(e.definition_en) LIKE '%' || LOWER(:searchQuery) || '%'
          )
          AND (:onlyFavorites = 0 OR wp.is_favorite = 1)
          AND (:onlyMastered = 0 OR wp.is_mastered = 1)
          AND (:onlyLearning = 0 OR (COALESCE(wp.is_mastered, 0) = 0 AND (COALESCE(wp.repetitions, 0) > 0 OR COALESCE(wp.last_reviewed_timestamp, 0) > 0)))
        GROUP BY UPPER(SUBSTR(e.text, 1, 1))
        ORDER BY initial ASC
        """
    )
    suspend fun getGroupedInitialLetterCounts(
        deckId: String,
        searchQuery: String,
        typeFilter: String,
        levelFilter: String,
        posFilter: String,
        onlyFavorites: Int = 0,
        onlyMastered: Int = 0,
        onlyLearning: Int = 0
    ): List<LetterCountResult>

    // --- WORD DETAILS TABLE QUERIES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordDetail(detail: WordDetailEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWordDetails(details: List<WordDetailEntity>)

    @Query("SELECT * FROM word_details WHERE `key` = :key LIMIT 1")
    suspend fun getWordDetail(key: String): WordDetailEntity?

    @Query("SELECT * FROM word_details")
    suspend fun getAllWordDetails(): List<WordDetailEntity>

    @Query("SELECT COUNT(*) FROM word_details")
    suspend fun getWordDetailsCount(): Int

    @Query("""
        UPDATE entries
        SET arabic = CASE
                WHEN arabic_source IN ('user', 'file') THEN arabic
                WHEN TRIM(:arabic) != '' THEN :arabic
                ELSE arabic
            END,
            arabic_source = CASE
                WHEN arabic_source IN ('user', 'file') THEN arabic_source
                WHEN TRIM(:arabic) != '' THEN 'ai'
                ELSE arabic_source
            END,
            ipa = CASE WHEN (ipa IS NULL OR ipa = '') AND :ipa != '' THEN :ipa ELSE ipa END
        WHERE LOWER(text) = LOWER(:wordText) AND (arabic IS NULL OR TRIM(arabic) = '')
    """)
    suspend fun updateEntryArabicIfEmpty(wordText: String, arabic: String, ipa: String = "")


    @Query("SELECT * FROM entries WHERE deck = :deckId AND LOWER(text) = LOWER(:wordText) ORDER BY pos ASC")
    suspend fun getWordVariants(deckId: String, wordText: String): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE LOWER(text) = LOWER(:wordText) ORDER BY deck ASC, pos ASC")
    suspend fun getWordVariantsAcrossDecks(wordText: String): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE deck = :deckId ORDER BY LOWER(text) ASC, CAST(id AS INTEGER) ASC")
    suspend fun getEntriesForDeckOrdered(deckId: String): List<EntryEntity>

    @Query(
        """
        SELECT e.* FROM entries e
        INNER JOIN word_progress wp
            ON (wp.entry_id = e.id OR (wp.entry_id IS NULL AND wp.deck = e.deck AND wp.text = LOWER(e.text)))
        WHERE wp.is_mastered = 0 AND wp.due_timestamp > 0 AND wp.due_timestamp <= :endOfTodayMillis
        ORDER BY wp.due_timestamp ASC, LOWER(e.text) ASC, CAST(e.id AS INTEGER) ASC
        """
    )
    suspend fun getDueTodayEntriesOrdered(endOfTodayMillis: Long): List<EntryEntity>

    @Query(
        """
        SELECT e.* FROM entries e
        INNER JOIN word_progress wp
            ON (wp.entry_id = e.id OR (wp.entry_id IS NULL AND wp.deck = e.deck AND wp.text = LOWER(e.text)))
        WHERE wp.is_mastered = 0 AND wp.due_timestamp > 0 AND wp.due_timestamp <= :endOfTodayMillis
          AND wp.deck IN (:decks)
        ORDER BY wp.due_timestamp ASC, LOWER(e.text) ASC, CAST(e.id AS INTEGER) ASC
        """
    )
    suspend fun getDueTodayEntriesForDecksOrdered(endOfTodayMillis: Long, decks: List<String>): List<EntryEntity>

    @Query(
        """
        SELECT e.* FROM entries e
        INNER JOIN word_progress wp
            ON (wp.entry_id = e.id OR (wp.entry_id IS NULL AND wp.deck = e.deck AND wp.text = LOWER(e.text)))
        WHERE wp.is_mastered = 0 AND wp.due_timestamp > 0 AND wp.due_timestamp <= :nowMillis
        ORDER BY wp.due_timestamp ASC, LOWER(e.text) ASC, CAST(e.id AS INTEGER) ASC
        """
    )
    suspend fun getDueNowEntriesOrdered(nowMillis: Long): List<EntryEntity>

    @Query(
        """
        SELECT e.* FROM entries e
        INNER JOIN word_progress wp
            ON (wp.entry_id = e.id OR (wp.entry_id IS NULL AND wp.deck = e.deck AND wp.text = LOWER(e.text)))
        WHERE wp.is_mastered = 0 AND wp.due_timestamp > 0 AND wp.due_timestamp <= :nowMillis
          AND wp.deck IN (:decks)
        ORDER BY wp.due_timestamp ASC, LOWER(e.text) ASC, CAST(e.id AS INTEGER) ASC
        """
    )
    suspend fun getDueNowEntriesForDecksOrdered(nowMillis: Long, decks: List<String>): List<EntryEntity>

    @Query("SELECT * FROM word_progress")
    fun observeAllWordProgress(): Flow<List<WordProgressEntity>>

    @Query("SELECT * FROM word_progress")
    suspend fun getAllWordProgressList(): List<WordProgressEntity>

    @Query("UPDATE word_progress SET conquered_timestamp = 0 WHERE again_count < 2 AND conquered_timestamp > 0")
    suspend fun clearInvalidConqueredTimestamps()

    @Query("SELECT * FROM word_progress WHERE deck = :deckId")
    suspend fun getWordProgressForDeck(deckId: String): List<WordProgressEntity>

    @Query("SELECT * FROM word_progress WHERE card_key = :cardKey LIMIT 1")
    suspend fun getWordProgress(cardKey: String): WordProgressEntity?

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_favorite = 1")
    fun observeFavoriteWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 1")
    fun observeMasteredWordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :endOfTodayMillis")
    fun observeReviewDueTodayCount(endOfTodayMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :endOfTodayMillis AND deck IN (:decks)")
    fun observeReviewDueTodayCountForDecks(endOfTodayMillis: Long, decks: List<String>): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :endOfTodayMillis")
    suspend fun getReviewDueTodayCount(endOfTodayMillis: Long): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :endOfTodayMillis AND deck IN (:decks)")
    suspend fun getReviewDueTodayCountForDecks(endOfTodayMillis: Long, decks: List<String>): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :nowMillis")
    fun observeDueNowCount(nowMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :nowMillis AND deck IN (:decks)")
    fun observeDueNowCountForDecks(nowMillis: Long, decks: List<String>): Flow<Int>

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :nowMillis")
    suspend fun getDueNowCardsCount(nowMillis: Long): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > 0 AND due_timestamp <= :nowMillis AND deck IN (:decks)")
    suspend fun getDueNowCardsCountForDecks(nowMillis: Long, decks: List<String>): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > :nowMillis AND due_timestamp <= :endOfTodayMillis")
    suspend fun getLaterTodayCardsCount(nowMillis: Long, endOfTodayMillis: Long): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > :nowMillis AND due_timestamp <= :endOfTodayMillis AND deck IN (:decks)")
    suspend fun getLaterTodayCardsCountForDecks(nowMillis: Long, endOfTodayMillis: Long, decks: List<String>): Int

    @Query("SELECT MIN(due_timestamp) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > :nowMillis AND due_timestamp <= :endOfTodayMillis")
    suspend fun getNextLaterTodayDueTimestamp(nowMillis: Long, endOfTodayMillis: Long): Long?

    @Query("SELECT MIN(due_timestamp) FROM word_progress WHERE is_mastered = 0 AND due_timestamp > :nowMillis AND due_timestamp <= :endOfTodayMillis AND deck IN (:decks)")
    suspend fun getNextLaterTodayDueTimestampForDecks(nowMillis: Long, endOfTodayMillis: Long, decks: List<String>): Long?

    @Query(
        """
        SELECT COUNT(*) FROM word_progress
        WHERE first_learned_timestamp >= :startOfTodayMillis
           OR (first_learned_timestamp = 0 AND last_reviewed_timestamp >= :startOfTodayMillis AND (repetitions > 0 OR again_count > 0))
        """
    )
    fun observeStudiedTodayCount(startOfTodayMillis: Long): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM word_progress
        WHERE first_learned_timestamp >= :startOfTodayMillis
           OR (first_learned_timestamp = 0 AND last_reviewed_timestamp >= :startOfTodayMillis AND (repetitions > 0 OR again_count > 0))
        """
    )
    suspend fun getStudiedTodayCount(startOfTodayMillis: Long): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE deck = :deckId AND is_mastered = 1")
    suspend fun getMasteredCountForDeck(deckId: String): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE deck = :deckId AND is_favorite = 1")
    suspend fun getFavoriteCountForDeck(deckId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeckStudyState(state: DeckStudyStateEntity)

    @Query("SELECT * FROM deck_study_state WHERE deck_id = :deckId LIMIT 1")
    suspend fun getDeckStudyState(deckId: String): DeckStudyStateEntity?

    @Query("SELECT * FROM deck_study_state ORDER BY updated_at DESC")
    fun observeAllDeckStudyStates(): Flow<List<DeckStudyStateEntity>>

    // --- SENTENCES TABLE ---
    @Query("SELECT * FROM sentences ORDER BY created_at DESC, id DESC")
    fun observeSentencesNewestFirst(): Flow<List<SentenceEntity>>

    @Query("SELECT * FROM sentences ORDER BY created_at ASC, id ASC")
    fun observeSentencesOldestFirst(): Flow<List<SentenceEntity>>

    @Query("SELECT * FROM sentences ORDER BY created_at DESC, id DESC")
    suspend fun getAllSentencesNewestFirst(): List<SentenceEntity>

    @Query("SELECT * FROM sentences WHERE id = :id LIMIT 1")
    suspend fun getSentenceById(id: Long): SentenceEntity?

    @Query("SELECT * FROM sentences WHERE LOWER(TRIM(english_text)) = LOWER(TRIM(:englishText)) LIMIT 1")
    suspend fun getSentenceByEnglishText(englishText: String): SentenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentence(sentence: SentenceEntity): Long

    @Query("DELETE FROM sentences WHERE id = :id")
    suspend fun deleteSentenceById(id: Long)

    @Query("DELETE FROM sentences WHERE LOWER(TRIM(english_text)) = LOWER(TRIM(:englishText))")
    suspend fun deleteSentenceByEnglishText(englishText: String)

    @Query("UPDATE sentences SET ai_explanation = :explanation WHERE id = :id")
    suspend fun updateSentenceAiExplanation(id: Long, explanation: String)

    @Query("UPDATE sentences SET arabic_text = :arabicText WHERE id = :id")
    suspend fun updateSentenceArabicText(id: Long, arabicText: String)

    @Query("SELECT COUNT(*) FROM sentences")
    fun observeSavedSentencesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sentences")
    suspend fun getSavedSentencesCount(): Int

    @Query("SELECT * FROM entries WHERE TRIM(examples_en) != '' ORDER BY CAST(id AS INTEGER) ASC LIMIT :limit")
    suspend fun getEntriesWithExamples(limit: Int = 60): List<EntryEntity>

    @Query("SELECT * FROM feature_flags ORDER BY id ASC")
    fun observeFeatureFlags(): Flow<List<FeatureFlagEntity>>

    // --- GRAMMAR LESSONS & PROGRESS ---
    @Query(
        """
        SELECT * FROM grammar_lessons
        ORDER BY
          CASE UPPER(TRIM(level))
              WHEN 'A1' THEN 1
              WHEN 'A2' THEN 2
              WHEN 'B1' THEN 3
              WHEN 'B2' THEN 4
              WHEN 'C1' THEN 5
              WHEN 'C2' THEN 6
              ELSE 7
          END ASC,
          topic ASC,
          id ASC
        """
    )
    fun observeAllGrammarLessons(): Flow<List<GrammarLessonEntity>>

    @Query("SELECT * FROM grammar_lessons ORDER BY id ASC")
    suspend fun getAllGrammarLessons(): List<GrammarLessonEntity>

    @Query("SELECT * FROM grammar_lessons WHERE UPPER(TRIM(level)) = UPPER(TRIM(:level)) ORDER BY id ASC")
    suspend fun getGrammarLessonsByLevel(level: String): List<GrammarLessonEntity>

    @Query("SELECT * FROM grammar_lessons WHERE id = :id LIMIT 1")
    suspend fun getGrammarLessonById(id: String): GrammarLessonEntity?

    @Query("""
        SELECT gl.* FROM grammar_lessons gl
        LEFT JOIN grammar_progress gp ON gl.id = gp.lesson_id
        ORDER BY COALESCE(gp.last_score, 0) ASC, gl.id ASC
        LIMIT :limit
    """)
    suspend fun getLowestScoringGrammarLessons(limit: Int = 10): List<GrammarLessonEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGrammarLesson(lesson: GrammarLessonEntity)

    @Query(
        """
        UPDATE grammar_lessons
        SET level = :level,
            topic = :topic,
            title_en = :titleEn,
            title_ar = :titleAr,
            explanation_ar = :explanationAr,
            examples_json = :examplesJson,
            exercises_json = :exercisesJson,
            data_version = :dataVersion
        WHERE id = :id
        """
    )
    suspend fun updateGrammarLessonContentById(
        id: String,
        level: String,
        topic: String,
        titleEn: String,
        titleAr: String,
        explanationAr: String,
        examplesJson: String,
        exercisesJson: String,
        dataVersion: Int
    )

    @Query("UPDATE grammar_lessons SET exercises_json = :exercisesJson WHERE id = :id")
    suspend fun updateGrammarLessonExercisesJson(id: String, exercisesJson: String)

    @Query("SELECT COUNT(*) FROM grammar_lessons")
    fun observeGrammarLessonCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM grammar_lessons")
    suspend fun getGrammarLessonCount(): Int

    // --- GRAMMAR PROGRESS ---
    @Query("SELECT * FROM grammar_progress")
    fun observeAllGrammarProgress(): Flow<List<GrammarProgressEntity>>

    @Query("SELECT * FROM grammar_progress WHERE lesson_id = :lessonId LIMIT 1")
    suspend fun getGrammarProgress(lessonId: String): GrammarProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGrammarProgress(progress: GrammarProgressEntity)

    // --- AI HISTORY ---
    @Query("SELECT * FROM ai_history WHERE context_type = :contextType AND context_id = :contextId ORDER BY timestamp ASC, id ASC")
    fun observeAiHistory(contextType: String, contextId: String): Flow<List<AiHistoryEntity>>

    @Query("SELECT * FROM ai_history WHERE context_type = :contextType AND context_id = :contextId ORDER BY timestamp ASC, id ASC")
    suspend fun getAiHistory(contextType: String, contextId: String): List<AiHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiHistory(item: AiHistoryEntity): Long

    @Query("SELECT * FROM ai_history ORDER BY id ASC")
    suspend fun getAllAiHistoryItems(): List<AiHistoryEntity>

    @Query("UPDATE ai_history SET text = :cleanedText WHERE id = :id")
    suspend fun updateAiHistoryText(id: Long, cleanedText: String)

    @Query("DELETE FROM ai_history WHERE context_type = :contextType AND context_id = :contextId")
    suspend fun clearAiHistory(contextType: String, contextId: String)

    // --- READING TEXTS ---
    @Query("SELECT * FROM reading_texts ORDER BY created_at DESC")
    fun observeAllReadingTexts(): Flow<List<ReadingTextEntity>>

    @Query("SELECT * FROM reading_texts ORDER BY created_at DESC")
    suspend fun getAllReadingTexts(): List<ReadingTextEntity>

    @Query("SELECT * FROM reading_texts WHERE id = :id LIMIT 1")
    suspend fun getReadingTextById(id: String): ReadingTextEntity?

    @Query("""
        SELECT rt.* FROM reading_texts rt
        LEFT JOIN reading_progress rp ON rt.id = rp.text_id
        ORDER BY COALESCE(rp.last_score, 0) ASC, rt.id ASC
        LIMIT :limit
    """)
    suspend fun getLowestScoringReadingTexts(limit: Int = 10): List<ReadingTextEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingText(text: ReadingTextEntity)

    @Query(
        """
        UPDATE reading_texts 
        SET level = :level, topic = :topic, title_en = :titleEn, title_ar = :titleAr,
            sentences_json = :sentencesJson, questions_json = :questionsJson,
            grammar_ids_json = :grammarIdsJson, target_words_json = :targetWordsJson, data_version = :dataVersion
        WHERE id = :id
        """
    )
    suspend fun updateReadingTextContentById(
        id: String,
        level: String,
        topic: String,
        titleEn: String,
        titleAr: String,
        sentencesJson: String,
        questionsJson: String,
        grammarIdsJson: String,
        dataVersion: Int,
        targetWordsJson: String = "[]"
    )

    @Query("UPDATE reading_texts SET sentences_json = :sentencesJson, questions_json = :questionsJson WHERE id = :id")
    suspend fun updateReadingTextSentencesAndQuestions(id: String, sentencesJson: String, questionsJson: String)

    @Query("DELETE FROM reading_texts WHERE id = :id")
    suspend fun deleteReadingTextById(id: String)

    @Query("SELECT COUNT(*) FROM reading_texts")
    fun observeReadingTextsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reading_texts")
    suspend fun getReadingTextsCount(): Int

    // --- READING PROGRESS ---
    @Query("SELECT * FROM reading_progress")
    fun observeAllReadingProgress(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress")
    suspend fun getAllReadingProgress(): List<ReadingProgressEntity>

    @Query("SELECT * FROM reading_progress WHERE text_id = :textId LIMIT 1")
    suspend fun getReadingProgress(textId: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgress(progress: ReadingProgressEntity)

    // --- CORRECTION NOTEBOOK ---
    @Query("SELECT * FROM correction_notebook ORDER BY updated_at DESC")
    fun observeAllCorrectionEntries(): Flow<List<CorrectionNotebookEntity>>

    @Query("SELECT * FROM correction_notebook WHERE grammar_lesson_id = :lessonId ORDER BY updated_at DESC")
    fun observeCorrectionEntriesForLesson(lessonId: String): Flow<List<CorrectionNotebookEntity>>

    @Query("SELECT * FROM correction_notebook WHERE id = :id LIMIT 1")
    suspend fun getCorrectionEntryById(id: Long): CorrectionNotebookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCorrectionEntry(entry: CorrectionNotebookEntity): Long

    @Query("DELETE FROM correction_notebook WHERE id = :id")
    suspend fun deleteCorrectionEntryById(id: Long)

    @Query("SELECT COUNT(*) FROM correction_notebook WHERE status = 'not_checked'")
    fun observeUncheckedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM correction_notebook WHERE next_review_timestamp > 0 AND next_review_timestamp <= :currentTime AND status != 'mastered'")
    fun observeDueReviewCount(currentTime: Long): Flow<Int>

    // --- STUDY PLAN PROFILE ---
    @Query("SELECT * FROM study_plan_profile WHERE id = 1 LIMIT 1")
    suspend fun getStudyPlanProfile(): StudyPlanProfileEntity?

    @Query("SELECT * FROM study_plan_profile WHERE id = 1 LIMIT 1")
    fun observeStudyPlanProfile(): Flow<StudyPlanProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyPlanProfile(profile: StudyPlanProfileEntity)

    // --- STUDY PLANS ---
    @Query("SELECT * FROM study_plans WHERE month = :month LIMIT 1")
    suspend fun getStudyPlanForMonth(month: String): StudyPlanEntity?

    @Query("SELECT * FROM study_plans WHERE month = :month LIMIT 1")
    fun observeStudyPlanForMonth(month: String): Flow<StudyPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyPlan(plan: StudyPlanEntity)

    // --- DAILY TASK CHECKLIST ---
    @Query("SELECT * FROM daily_task_checklist WHERE date = :date LIMIT 1")
    suspend fun getDailyChecklist(date: String): DailyChecklistEntity?

    @Query("SELECT * FROM daily_task_checklist WHERE date = :date LIMIT 1")
    fun observeDailyChecklist(date: String): Flow<DailyChecklistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyChecklist(checklist: DailyChecklistEntity)

    @Query("SELECT * FROM daily_task_checklist ORDER BY date DESC")
    suspend fun getAllDailyChecklists(): List<DailyChecklistEntity>

    @Query("SELECT * FROM daily_task_checklist WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    fun observeChecklistsForMonth(monthPrefix: String): Flow<List<DailyChecklistEntity>>

    @Query("SELECT * FROM daily_task_checklist WHERE date LIKE :monthPrefix || '%' ORDER BY date ASC")
    suspend fun getChecklistsForMonth(monthPrefix: String): List<DailyChecklistEntity>

    // --- QUIZ RESULTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity): Long

    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    fun observeAllQuizResults(): Flow<List<QuizResultEntity>>

    @Query("SELECT * FROM quiz_results WHERE date = :date")
    suspend fun getQuizResultsForDate(date: String): List<QuizResultEntity>

    @Query("SELECT * FROM quiz_results WHERE skill = :skill ORDER BY timestamp DESC")
    suspend fun getQuizResultsForSkill(skill: String): List<QuizResultEntity>

    // --- PLAN & TELEMETRY QUERIES ---
    @Query("""
        SELECT e.* FROM entries e
        JOIN word_progress wp ON e.deck = wp.deck AND LOWER(e.text) = LOWER(wp.text)
        WHERE wp.again_count > 0 AND TRIM(e.arabic) != ''
        GROUP BY LOWER(e.text)
        ORDER BY wp.again_count DESC, wp.last_reviewed_timestamp DESC
        LIMIT :limit
    """)
    suspend fun getWeakestWords(limit: Int): List<EntryEntity>

    @Query("""
        SELECT COUNT(*) FROM word_progress
        WHERE due_timestamp > 0 AND due_timestamp <= :currentTime AND is_mastered = 0
    """)
    suspend fun getDueCardsCount(currentTime: Long): Int

    @Query("""
        SELECT COUNT(*) FROM word_progress
        WHERE due_timestamp > 0 AND due_timestamp <= :currentTime AND is_mastered = 0
          AND deck IN (:decks)
    """)
    suspend fun getDueCardsCountForDecks(currentTime: Long, decks: List<String>): Int

    @Query("""
        SELECT * FROM word_progress
        WHERE due_timestamp > 0 AND due_timestamp <= :currentTime AND is_mastered = 0
        ORDER BY due_timestamp ASC
        LIMIT :limit
    """)
    suspend fun getDueCards(currentTime: Long, limit: Int): List<WordProgressEntity>

    @Query("""
        SELECT COUNT(*) FROM word_progress
        WHERE last_reviewed_timestamp >= :startOfDayMillis
          AND (first_learned_timestamp > 0 OR repetitions > 0 OR again_count > 0)
    """)
    suspend fun getCardsRatedTodayCount(startOfDayMillis: Long): Int

    /**
     * Counts new words introduced (given their first real SM-2 rating) on or after [startOfDayMillis].
     * Uses `first_learned_timestamp` (with a fallback for legacy/test rows that set repetitions=1 without first_learned_timestamp).
     * Never counts favorite-only or manual-mastery-only rows (`last_reviewed_timestamp = 0` and `first_learned_timestamp = 0`).
     */
    @Query("""
        SELECT COUNT(*) FROM word_progress
        WHERE (first_learned_timestamp >= :startOfDayMillis)
           OR (first_learned_timestamp = 0 AND last_reviewed_timestamp >= :startOfDayMillis AND repetitions = 1 AND again_count = 0)
    """)
    suspend fun getNewWordsRatedTodayCount(startOfDayMillis: Long): Int

    @Query("""
        SELECT COUNT(*) FROM word_progress
        WHERE (first_learned_timestamp >= :startMillis AND first_learned_timestamp <= :endMillis)
           OR (first_learned_timestamp = 0 AND last_reviewed_timestamp >= :startMillis AND last_reviewed_timestamp <= :endMillis AND repetitions = 1 AND again_count = 0)
    """)
    suspend fun getNewWordsLearnedInRangeCount(startMillis: Long, endMillis: Long): Int

    @Query("""
        SELECT COUNT(*) FROM word_progress
        WHERE (first_learned_timestamp >= :startOfDayMillis)
           OR (first_learned_timestamp = 0 AND last_reviewed_timestamp >= :startOfDayMillis AND repetitions = 1 AND again_count = 0)
    """)
    fun observeNewWordsLearnedTodayCount(startOfDayMillis: Long): Flow<Int>

    /**
     * Counts review ratings completed on or after [startOfDayMillis], strictly separated from new words.
     * Sums `flashcard_review` attempts from `attempts` plus any legacy/test `word_progress` review row
     * without a corresponding attempt row.
     */
    @Query("""
        SELECT (
            (SELECT COUNT(*) FROM attempts WHERE activity_type = 'flashcard_review' AND timestamp >= :startOfDayMillis)
            +
            (SELECT COUNT(*) FROM word_progress wp
             WHERE wp.last_reviewed_timestamp >= :startOfDayMillis
               AND (
                   (wp.first_learned_timestamp > 0 AND wp.first_learned_timestamp < :startOfDayMillis)
                   OR (wp.first_learned_timestamp = 0 AND wp.repetitions > 1)
               )
               AND NOT EXISTS (
                   SELECT 1 FROM attempts a
                   WHERE a.activity_type = 'flashcard_review'
                     AND a.item_id = wp.card_key
                     AND a.timestamp >= :startOfDayMillis
               )
            )
        )
    """)
    suspend fun getReviewCardsRatedTodayCount(startOfDayMillis: Long): Int

    @Query("""
        SELECT (
            (SELECT COUNT(*) FROM attempts WHERE activity_type = 'flashcard_review' AND timestamp >= :startMillis AND timestamp <= :endMillis)
            +
            (SELECT COUNT(*) FROM word_progress wp
             WHERE wp.last_reviewed_timestamp >= :startMillis AND wp.last_reviewed_timestamp <= :endMillis
               AND (
                   (wp.first_learned_timestamp > 0 AND wp.first_learned_timestamp < :startMillis)
                   OR (wp.first_learned_timestamp = 0 AND wp.repetitions > 1)
               )
               AND NOT EXISTS (
                   SELECT 1 FROM attempts a
                   WHERE a.activity_type = 'flashcard_review'
                     AND a.item_id = wp.card_key
                     AND a.timestamp >= :startMillis AND a.timestamp <= :endMillis
               )
            )
        )
    """)
    suspend fun getReviewCardsRatedInRangeCount(startMillis: Long, endMillis: Long): Int

    @Query("""
        SELECT COUNT(*) FROM attempts
        WHERE activity_type = 'flashcard_review' AND timestamp >= :startOfDayMillis
    """)
    fun observeReviewAttemptsTodayCount(startOfDayMillis: Long): Flow<Int>

    // --- ATTEMPTS TABLE & TRANSACTIONAL PRACTICE LOGGING ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempts(attempts: List<AttemptEntity>)

    @Query("SELECT * FROM attempts ORDER BY timestamp ASC, id ASC")
    suspend fun getAllAttempts(): List<AttemptEntity>

    @Query("SELECT * FROM attempts ORDER BY timestamp DESC, id DESC")
    fun observeAllAttempts(): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE timestamp >= :startMillis AND timestamp <= :endMillis ORDER BY timestamp ASC, id ASC")
    suspend fun getAttemptsInRange(startMillis: Long, endMillis: Long): List<AttemptEntity>

    @Query("SELECT * FROM attempts WHERE activity_type = :activityType AND item_id = :itemId ORDER BY timestamp ASC, id ASC")
    suspend fun getAttemptsForItem(activityType: String, itemId: String): List<AttemptEntity>

    @Query("SELECT COUNT(*) FROM attempts")
    suspend fun getTotalAttemptsCount(): Int

    @Transaction
    suspend fun upsertWordProgressWithAttempt(progress: WordProgressEntity, attempt: AttemptEntity) {
        upsertWordProgress(progress)
        insertAttempt(attempt)
    }

    @Transaction
    suspend fun upsertGrammarProgressWithAttempt(progress: GrammarProgressEntity, attempt: AttemptEntity) {
        upsertGrammarProgress(progress)
        insertAttempt(attempt)
    }

    @Transaction
    suspend fun upsertReadingProgressWithAttempt(progress: ReadingProgressEntity, attempt: AttemptEntity) {
        upsertReadingProgress(progress)
        insertAttempt(attempt)
    }

    @Transaction
    suspend fun upsertCorrectionWithAttempt(entry: CorrectionNotebookEntity, attempt: AttemptEntity): Long {
        val id = upsertCorrectionEntry(entry)
        val resolvedAttempt = if (attempt.itemId.isBlank() || attempt.itemId == "0") {
            attempt.copy(itemId = id.toString())
        } else {
            attempt
        }
        insertAttempt(resolvedAttempt)
        return id
    }

    @Transaction
    suspend fun insertQuizResultsWithAttempts(results: List<QuizResultEntity>, attempts: List<AttemptEntity>) {
        for (result in results) {
            insertQuizResult(result)
        }
        if (attempts.isNotEmpty()) {
            insertAttempts(attempts)
        }
    }

    // --- BATCH BACKUP / RESTORE HELPERS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWordProgressBatch(items: List<WordProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSentencesBatch(items: List<SentenceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGrammarProgressBatch(items: List<GrammarProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadingProgressBatch(items: List<ReadingProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCorrectionEntriesBatch(items: List<CorrectionNotebookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyChecklistsBatch(items: List<DailyChecklistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuizResultsBatch(items: List<QuizResultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyPlansBatch(items: List<StudyPlanEntity>)

    @Query("SELECT * FROM study_plans ORDER BY month ASC")
    suspend fun getAllStudyPlansList(): List<StudyPlanEntity>

    @Query("SELECT * FROM grammar_progress")
    suspend fun getAllGrammarProgressList(): List<GrammarProgressEntity>

    @Query("SELECT * FROM correction_notebook ORDER BY id ASC")
    suspend fun getAllCorrectionEntriesList(): List<CorrectionNotebookEntity>

    @Query("SELECT * FROM quiz_results ORDER BY id ASC")
    suspend fun getAllQuizResultsList(): List<QuizResultEntity>

    @Transaction
    suspend fun restoreUserBackupPayload(payload: UserBackupPayload) {
        if (payload.wordProgress.isNotEmpty()) {
            upsertWordProgressBatch(payload.wordProgress)
        }
        if (payload.attempts.isNotEmpty()) {
            insertAttempts(payload.attempts)
        }
        if (payload.sentences.isNotEmpty()) {
            upsertSentencesBatch(payload.sentences)
        }
        if (payload.grammarProgress.isNotEmpty()) {
            upsertGrammarProgressBatch(payload.grammarProgress)
        }
        if (payload.readingProgress.isNotEmpty()) {
            upsertReadingProgressBatch(payload.readingProgress)
        }
        if (payload.correctionNotebook.isNotEmpty()) {
            upsertCorrectionEntriesBatch(payload.correctionNotebook)
        }
        if (payload.studyPlanProfile != null) {
            upsertStudyPlanProfile(payload.studyPlanProfile)
        }
        if (payload.studyPlans.isNotEmpty()) {
            upsertStudyPlansBatch(payload.studyPlans)
        }
        if (payload.dailyChecklists.isNotEmpty()) {
            upsertDailyChecklistsBatch(payload.dailyChecklists)
        }
        if (payload.quizResults.isNotEmpty()) {
            upsertQuizResultsBatch(payload.quizResults)
        }
    }

    @Query("""
        SELECT e.* FROM entries e
        WHERE (
            e.effective_level IN (:levels)
            OR (e.deck = 'oxford_opal' AND e.effective_level IS NULL)
          )
          AND e.deck IN (:decks)
          AND TRIM(e.arabic) != ''
          AND NOT EXISTS (
              SELECT 1 FROM word_progress wp
              WHERE wp.entry_id = e.id AND (wp.first_learned_timestamp > 0 OR wp.last_reviewed_timestamp > 0)
          )
        ORDER BY
          CASE UPPER(TRIM(COALESCE(e.effective_level, '')))
            WHEN 'A1' THEN 1
            WHEN 'A2' THEN 2
            WHEN 'B1' THEN 3
            WHEN 'B2' THEN 4
            WHEN 'C1' THEN 5
            WHEN 'C2' THEN 6
            ELSE 7
          END ASC,
          CAST(e.id AS INTEGER) ASC,
          e.id ASC
        LIMIT :limit
    """)
    suspend fun getNewWordsForLevelsAndDecks(levels: List<String>, decks: List<String>, limit: Int): List<EntryEntity>

    @Query("""
        SELECT e.* FROM entries e
        WHERE e.deck = :deck
          AND (
            (:includeUnlevelled = 1 AND e.effective_level IS NULL)
            OR (:level != '' AND UPPER(TRIM(e.effective_level)) = UPPER(TRIM(:level)))
            OR (:level = '' AND :includeUnlevelled = 0)
          )
          AND TRIM(e.arabic) != ''
          AND NOT EXISTS (
              SELECT 1 FROM word_progress wp
              LEFT JOIN entries e_studied ON wp.entry_id = e_studied.id
              WHERE (wp.first_learned_timestamp > 0 OR wp.last_reviewed_timestamp > 0)
                AND (
                    wp.entry_id = e.id
                    OR (e_studied.id IS NOT NULL AND LOWER(TRIM(e_studied.text)) = LOWER(TRIM(e.text)))
                )
          )
        ORDER BY ((CAST(e.id AS INTEGER) * 2654435761) % 4294967296) ASC, e.id ASC
        LIMIT :limit
    """)
    suspend fun getUnstudiedEntriesForDeckAndLevel(
        deck: String,
        level: String,
        includeUnlevelled: Int,
        limit: Int
    ): List<EntryEntity>

    @Query("""
        SELECT e.* FROM entries e
        WHERE e.deck = :deck
          AND (
            e.effective_level IN (:levels)
            OR (:includeNullLevel = 1 AND e.effective_level IS NULL)
          )
          AND TRIM(e.arabic) != ''
          AND NOT EXISTS (
              SELECT 1 FROM word_progress wp
              LEFT JOIN entries e_studied ON wp.entry_id = e_studied.id
              WHERE (wp.first_learned_timestamp > 0 OR wp.last_reviewed_timestamp > 0)
                AND (
                    wp.entry_id = e.id
                    OR (e_studied.id IS NOT NULL AND LOWER(TRIM(e_studied.text)) = LOWER(TRIM(e.text)))
                )
          )
        ORDER BY ((CAST(e.id AS INTEGER) * 2654435761) % 4294967296) ASC, e.id ASC
    """)
    suspend fun getUnstudiedEntriesForDeckAndEffectiveLevels(
        deck: String,
        levels: List<String>,
        includeNullLevel: Int
    ): List<EntryEntity>

    @Query("""
        SELECT gl.* FROM grammar_lessons gl
        LEFT JOIN grammar_progress gp ON gl.id = gp.lesson_id
        WHERE gl.level = :level AND (gp.is_completed = 0 OR gp.lesson_id IS NULL)
        ORDER BY gl.id ASC
    """)
    suspend fun getUncompletedGrammarLessonsForLevel(level: String): List<GrammarLessonEntity>

    @Query("""
        SELECT rt.* FROM reading_texts rt
        LEFT JOIN reading_progress rp ON rt.id = rp.text_id
        WHERE rt.level = :level AND (rp.is_completed = 0 OR rp.text_id IS NULL)
        ORDER BY rt.id ASC
    """)
    suspend fun getUncompletedReadingTextsForLevel(level: String): List<ReadingTextEntity>

    @Query("""
        SELECT COUNT(*) FROM correction_notebook
        WHERE next_review_timestamp > 0 AND next_review_timestamp <= :currentTime AND status != 'mastered'
    """)
    suspend fun getDueCorrectionReviewsCount(currentTime: Long): Int

    @Query("""
        SELECT COUNT(DISTINCT wp.card_key) FROM word_progress wp
        JOIN entries e ON wp.deck = e.deck AND LOWER(wp.text) = LOWER(e.text)
        WHERE wp.last_reviewed_timestamp > 0 AND e.level = :level
    """)
    suspend fun getLearnedWordsCountByLevel(level: String): Int

    @Query("""
        SELECT COUNT(DISTINCT wp.card_key) FROM word_progress wp
        WHERE wp.last_reviewed_timestamp > 0 AND wp.deck = :deckId
    """)
    suspend fun getLearnedWordsCountByDeck(deckId: String): Int

    // --- ONBOARDING QUERIES ---
    @Query(
        """
        SELECT * FROM entries
        WHERE UPPER(TRIM(level)) = UPPER(TRIM(:level))
          AND LOWER(TRIM(pos)) IN ('noun', 'verb', 'adjective', 'adverb')
          AND LENGTH(TRIM(text)) >= 4
          AND deck IN ('oxford5000', 'oxford_opal')
          AND TRIM(arabic) != ''
          AND NOT EXISTS (
              SELECT 1 FROM word_progress r
              WHERE r.entry_id = entries.id
          )
        ORDER BY CASE WHEN TRIM(audio_url) != '' THEN 0 ELSE 1 END ASC, CAST(id AS INTEGER) ASC, id ASC
        LIMIT 3
        """
    )
    suspend fun getOnboardingFirstWinEntries(level: String): List<EntryEntity>

    @Query(
        """
        SELECT COUNT(*) FROM entries
        WHERE UPPER(TRIM(level)) = UPPER(TRIM(:level))
          AND TRIM(arabic) != ''
        """
    )
    suspend fun getEntryCountForLevelWithArabic(level: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM entries
        WHERE UPPER(TRIM(level)) = UPPER(TRIM(:level))
          AND TRIM(arabic) != ''
        """
    )
    fun observeEntryCountForLevelWithArabic(level: String): Flow<Int>

    @Query(
        """
        SELECT * FROM entries
        WHERE UPPER(TRIM(level)) = UPPER(TRIM(:level))
          AND TRIM(arabic) != ''
          AND LENGTH(TRIM(text)) >= 3
        ORDER BY
          CASE WHEN deck IN ('oxford5000', 'oxford_opal') THEN 0 ELSE 1 END ASC,
          CASE WHEN LOWER(TRIM(pos)) IN ('noun', 'verb', 'adjective', 'adverb') THEN 0 ELSE 1 END ASC,
          ((CAST(id AS INTEGER) * 2654435761) % 4294967296) ASC,
          id ASC
        LIMIT :limit
        """
    )
    suspend fun getPlacementVocabularyCandidatesForLevel(level: String, limit: Int = 80): List<EntryEntity>

    @Query(
        """
        SELECT * FROM grammar_lessons
        WHERE UPPER(TRIM(level)) = UPPER(TRIM(:level))
          AND TRIM(exercises_json) != ''
          AND TRIM(exercises_json) != '[]'
        ORDER BY id ASC
        """
    )
    suspend fun getGrammarLessonsForLevelQuiz(level: String): List<GrammarLessonEntity>

    @Query(
        """
        SELECT id FROM entries
        WHERE deck = :deck AND LOWER(TRIM(text)) = LOWER(TRIM(:wordText))
        ORDER BY CAST(id AS INTEGER) ASC, id ASC
        LIMIT 1
        """
    )
    suspend fun getPrimaryEntryIdForCard(deck: String, wordText: String): String?

    @Query(
        """
        SELECT * FROM entries
        WHERE (UPPER(TRIM(level)) = UPPER(TRIM(:level)) OR :level = '')
          AND (deck IN (:decks) OR :hasDecks = 0)
          AND TRIM(arabic) != ''
          AND TRIM(text) != ''
        ORDER BY CAST(id AS INTEGER) ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getKickoffStarterWords(level: String, decks: List<String>, hasDecks: Int, limit: Int = 5): List<EntryEntity>

    @Query(
        """
        SELECT * FROM entries
        WHERE (UPPER(TRIM(COALESCE(NULLIF(TRIM(effective_level), ''), level))) = UPPER(TRIM(:level)) OR :level = '')
          AND TRIM(text) != ''
        ORDER BY LOWER(text) ASC, pos ASC
        LIMIT :limit
        """
    )
    suspend fun getRecommendedWordsForLevel(level: String, limit: Int = 5000): List<EntryEntity>

    @Query(
        """
        SELECT * FROM entries
        WHERE (:deckId = '' OR deck = :deckId)
          AND (UPPER(TRIM(COALESCE(NULLIF(TRIM(effective_level), ''), level))) = UPPER(TRIM(:level)) OR :level = '')
          AND TRIM(text) != ''
        ORDER BY LOWER(text) ASC, pos ASC
        """
    )
    fun observeWordsForDeckAndLevel(deckId: String, level: String): Flow<List<EntryEntity>>

    // --- ADAPTIVE V2 3-TIER OVERDUENESS & STORY VOCABULARY QUERIES ---
    @Query(
        """
        SELECT e.* FROM entries e
        INNER JOIN word_progress wp
            ON (wp.entry_id = e.id OR (wp.entry_id IS NULL AND wp.deck = e.deck AND wp.text = LOWER(e.text)))
        WHERE wp.is_mastered = 0 AND wp.due_timestamp > 0 AND wp.due_timestamp <= :endOfTodayMillis
        ORDER BY
            CASE WHEN wp.interval_days = 0 THEN 0 ELSE 1 END ASC,
            CASE WHEN (wp.again_count >= 3 OR wp.last_looked_up_timestamp >= :lookupBoostCutoffMillis) THEN 0 ELSE 1 END ASC,
            (CAST(MAX(0, :nowMillis - wp.due_timestamp) AS REAL) / (CAST(MAX(1, wp.interval_days) AS REAL) * 86400000.0)) DESC,
            wp.ease_factor ASC,
            CAST(e.id AS INTEGER) ASC
        LIMIT :limit
        """
    )
    suspend fun getDueTodayEntriesAdaptiveOrdered(
        endOfTodayMillis: Long,
        nowMillis: Long,
        lookupBoostCutoffMillis: Long,
        limit: Int = 30
    ): List<EntryEntity>

    @Query(
        """
        SELECT e.* FROM entries e
        INNER JOIN word_progress wp
            ON (wp.entry_id = e.id OR (wp.entry_id IS NULL AND wp.deck = e.deck AND wp.text = LOWER(e.text)))
        WHERE wp.is_mastered = 0 AND wp.due_timestamp > 0 AND wp.due_timestamp <= :endOfTodayMillis
          AND wp.deck IN (:decks)
        ORDER BY
            CASE WHEN wp.interval_days = 0 THEN 0 ELSE 1 END ASC,
            CASE WHEN (wp.again_count >= 3 OR wp.last_looked_up_timestamp >= :lookupBoostCutoffMillis) THEN 0 ELSE 1 END ASC,
            (CAST(MAX(0, :nowMillis - wp.due_timestamp) AS REAL) / (CAST(MAX(1, wp.interval_days) AS REAL) * 86400000.0)) DESC,
            wp.ease_factor ASC,
            CAST(e.id AS INTEGER) ASC
        LIMIT :limit
        """
    )
    suspend fun getDueTodayEntriesForDecksAdaptiveOrdered(
        endOfTodayMillis: Long,
        nowMillis: Long,
        lookupBoostCutoffMillis: Long,
        decks: List<String>,
        limit: Int = 30
    ): List<EntryEntity>

    @Query(
        """
        SELECT e.* FROM entries e
        WHERE LOWER(TRIM(e.text)) IN (:headwords)
          AND e.deck IN (:decks)
          AND TRIM(e.arabic) != ''
        ORDER BY CAST(e.id AS INTEGER) ASC, e.id ASC
        """
    )
    suspend fun getEntriesByHeadwordsInDecks(
        headwords: List<String>,
        decks: List<String>
    ): List<EntryEntity>

    @Query(
        """
        SELECT e.* FROM entries e
        WHERE LOWER(TRIM(e.text)) IN (:headwords)
          AND TRIM(e.arabic) != ''
        ORDER BY
          CASE WHEN e.deck = 'oxford5000' THEN 0 WHEN e.deck = 'oxford_opal' THEN 1 ELSE 2 END ASC,
          CAST(e.id AS INTEGER) ASC,
          e.id ASC
        """
    )
    suspend fun getEntriesByHeadwords(headwords: List<String>): List<EntryEntity>
}
