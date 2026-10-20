package com.example.data.ai

import android.content.Context
import com.example.data.local.WordBooksDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single Source of Truth (SSOT) for AI prompt formatting rules, output sanitization,
 * and schema-versioned local caching across Reading, Grammar, and Dictionary services.
 *
 * Incrementing [PROMPT_SCHEMA_VERSION] automatically invalidates stale `ai_cache` keys
 * and triggers a one-time startup sanitization of stored AI history on physical devices.
 */
object AiPromptContract {

    const val PROMPT_SCHEMA_VERSION = 3
    private const val PREFS_NAME = "ssot_ai_contract_prefs"
    private const val KEY_APPLIED_SCHEMA_VERSION = "applied_prompt_schema_version"

    val CACHE_KEY_PREFIX: String
        get() = "v${PROMPT_SCHEMA_VERSION}_"

    const val STRICT_PLAIN_TEXT_RULES = """
STRICT FORMATTING:
1. Do NOT use markdown bold asterisks (**) or markdown headers (###).
2. Output clean, readable plain text using bullet points (•) and natural punctuation.
3. Do NOT include conversational greetings or closing remarks.
"""

    /**
     * Builds the canonical in-context word explanation prompt for Reading passages.
     */
    fun buildWordInContextPrompt(word: String, sentenceEn: String): String {
        return """
You are an in-app linguistic dictionary. Explain the word "$word" strictly in the context of this sentence:
"$sentenceEn"

STRICT RULES:
1. Absolutely NO conversational greetings or pleasantries (Do NOT say "أهلاً بك" or "يسعدني مساعدتك").
2. Absolutely NO closing remarks or questions (Do NOT say "أتمنى أن يكون الشرح مفيداً" or "هل لديك استفسار").
3. Do NOT use markdown bold asterisks (**) or markdown headers (###).
4. Output clean, direct bullet points:
   • المعنى في هذا السياق: (Direct concise Arabic meaning)
   • نوع الكلمة: (Part of speech)
   • مثال إضافي: (One clean sentence in English followed by its Arabic translation)
""".trimIndent()
    }

    /**
     * Generates a schema-versioned cache key so that bumping [PROMPT_SCHEMA_VERSION]
     * guarantees fresh AI responses aligned with the latest formatting contract.
     */
    fun versionedCacheKey(namespace: String, rawKey: String): String {
        return "${CACHE_KEY_PREFIX}${namespace}:${rawKey.trim().lowercase()}"
    }

    /**
     * Single Source of Truth plain-text sanitizer for all AI responses and BidiText rendering.
     * Strips markdown asterisks (**), headers (###), conversational fluff lines, and normalizes bullets (•).
     */
    fun sanitizeAiPlainText(raw: String): String {
        if (raw.isEmpty()) return raw

        val withoutInlineMarkdown = raw
            .replace("**", "")
            .replace("__", "")
            .replace("### ", "")
            .replace("## ", "")
            .replace("###", "")
            .replace("`", "")

        val cleanedLines = withoutInlineMarkdown.lines().mapNotNull { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                ""
            } else if (isConversationalFluffLine(trimmed)) {
                null
            } else {
                // Convert leading markdown bullets (* or -) into clean Unicode bullets (•)
                val normalizedBullet = when {
                    trimmed.startsWith("* ") -> "• " + trimmed.substring(2).trimStart()
                    trimmed.startsWith("- ") -> "• " + trimmed.substring(2).trimStart()
                    else -> rawLine.replace("*", "•")
                }
                normalizedBullet
            }
        }

        return cleanedLines
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun isConversationalFluffLine(trimmedLine: String): Boolean {
        val lower = trimmedLine.lowercase()
        val greetingPrefixes = listOf(
            "أهلاً بك",
            "أهلا بك",
            "مرحباً بك",
            "مرحبا بك",
            "يسعدني مساعدتك",
            "بالتأكيد! إليك",
            "بالتأكيد، إليك",
            "أتمنى أن يكون هذا الشرح",
            "أتمنى أن يكون الشرح مفيداً",
            "أتمنى أن يكون الشرح مفيدا",
            "هل لديك أي استفسار",
            "هل لديك سؤال آخر",
            "لا تتردد في السؤال"
        )
        return greetingPrefixes.any { prefix ->
            lower.startsWith(prefix) && trimmedLine.length < 110
        }
    }

    /**
     * Automatically executes on startup to ensure physical phones upgrading from an older APK
     * purge unversioned `ai_cache` rows and sanitize existing `ai_history` & `sentences` explanations
     * without losing any user study progress.
     */
    suspend fun syncAndSanitizeCacheIfNeeded(
        context: Context,
        dao: WordBooksDao
    ) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val appliedVersion = prefs.getInt(KEY_APPLIED_SCHEMA_VERSION, 0)
        if (appliedVersion >= PROMPT_SCHEMA_VERSION) {
            return@withContext
        }

        runCatching {
            // 1. Invalidate stale unversioned or older-versioned AI cache entries
            dao.deleteStaleAiCacheNotStartingWith("${CACHE_KEY_PREFIX}%")

            // 2. Sanitize existing AI chat/open-question history rows in-place
            val historyItems = dao.getAllAiHistoryItems()
            for (item in historyItems) {
                if (item.role == "assistant") {
                    val sanitized = sanitizeAiPlainText(item.text)
                    if (sanitized != item.text && sanitized.isNotBlank()) {
                        dao.updateAiHistoryText(item.id, sanitized)
                    }
                }
            }

            // 3. Sanitize any saved sentence AI explanations in-place
            val sentences = dao.getAllSentencesNewestFirst()
            for (sentence in sentences) {
                if (sentence.aiExplanation.isNotBlank()) {
                    val sanitized = sanitizeAiPlainText(sentence.aiExplanation)
                    if (sanitized != sentence.aiExplanation) {
                        dao.updateSentenceAiExplanation(sentence.id, sanitized)
                    }
                }
            }

            prefs.edit().putInt(KEY_APPLIED_SCHEMA_VERSION, PROMPT_SCHEMA_VERSION).apply()
        }
    }
}
