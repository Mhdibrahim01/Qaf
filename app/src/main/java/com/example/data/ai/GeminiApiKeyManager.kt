package com.example.data.ai

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

enum class TranslationEngineMode(
    val id: String,
    val labelEn: String,
    val shortBadge: String,
    val descriptionEn: String
) {
    SMART_AUTO(
        id = "smart_auto",
        labelEn = "Smart Auto (AI + Free API)",
        shortBadge = "Smart Auto",
        descriptionEn = "Uses AI keys first and automatically switches to the Free Dictionary & Translation API on 429 rate limits or missing keys."
    ),
    FREE_API_ONLY(
        id = "free_api_only",
        labelEn = "Free API Only (Unlimited)",
        shortBadge = "Free API",
        descriptionEn = "Uses the Free Dictionary API (dictionaryapi.dev) + Google GTX / LibreTranslate / MyMemory with zero AI quota required."
    ),
    AI_KEYS_ONLY(
        id = "ai_keys_only",
        labelEn = "AI Keys Only (Strict)",
        shortBadge = "AI Keys Only",
        descriptionEn = "Strictly uses your configured Gemini, Groq, or OpenRouter API keys without free-engine fallback."
    );

    companion object {
        fun fromId(id: String?): TranslationEngineMode {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SMART_AUTO
        }
    }
}

enum class AiKeyProvider(val displayName: String) {
    GEMINI("Google Gemini"),
    GROQ("Groq (Llama 3.3)"),
    OPENROUTER("OpenRouter"),
    OPENAI("OpenAI"),
    OAUTH_OR_INVALID("Unsupported Token Format")
}

sealed class ApiKeyTestResult {
    data class Success(val message: String) : ApiKeyTestResult()
    data class RateLimited(val message: String) : ApiKeyTestResult()
    data class Error(val message: String) : ApiKeyTestResult()
}

object GeminiApiKeyManager {
    private const val PREFS_NAME = "wordbooks_gemini_api_prefs"
    private const val KEY_CUSTOM_KEY = "custom_gemini_api_key"
    private const val KEY_ENGINE_MODE = "translation_engine_mode"

    // Current active Gemini models (avoids retired 1.5/2.0 models and duplicate aliases)
    val ACTIVE_GEMINI_MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite-preview",
        "gemini-2.5-flash-lite"
    )

    private val GROQ_MODELS = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant"
    )

    private val OPENROUTER_MODELS = listOf(
        "google/gemini-2.5-flash",
        "meta-llama/llama-3.3-70b-instruct:free",
        "openai/gpt-4o-mini"
    )

    private val OPENAI_MODELS = listOf(
        "gpt-4o-mini"
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // Cooldown tracker: "keyHash::model" -> epochMillis when cooldown expires
    private val cooldownUntilMap = ConcurrentHashMap<String, Long>()

    private val _apiKeyUpdatedTrigger = MutableStateFlow(0)
    val apiKeyUpdatedTrigger: StateFlow<Int> = _apiKeyUpdatedTrigger.asStateFlow()

    /**
     * Cleans invisible whitespace, quotes, or "Bearer " prefixes from a pasted API key.
     */
    fun sanitizeKey(raw: String): String {
        return raw
            .trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim('"', '\'', ' ', '\t', '\r', '\n', '\u200B', '\u200E', '\u200F')
    }

    /**
     * Detects the provider type from a sanitized API key string.
     */
    fun detectProvider(key: String): AiKeyProvider {
        val clean = sanitizeKey(key)
        return when {
            (clean.startsWith("AIza") || clean.startsWith("AQ.")) && clean.length >= 25 -> AiKeyProvider.GEMINI
            clean.startsWith("gsk_") && clean.length >= 25 -> AiKeyProvider.GROQ
            clean.startsWith("sk-or-") && clean.length >= 25 -> AiKeyProvider.OPENROUTER
            clean.startsWith("sk-") && clean.length >= 25 -> AiKeyProvider.OPENAI
            clean.startsWith("ya29.") -> AiKeyProvider.OAUTH_OR_INVALID
            clean.length >= 30 && clean.all { it.isLetterOrDigit() || it == '_' || it == '-' } -> AiKeyProvider.GEMINI
            else -> AiKeyProvider.OAUTH_OR_INVALID
        }
    }

    fun getEngineMode(context: Context): TranslationEngineMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ENGINE_MODE, TranslationEngineMode.SMART_AUTO.id)
        return TranslationEngineMode.fromId(raw)
    }

    fun setEngineMode(context: Context, mode: TranslationEngineMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENGINE_MODE, mode.id)
            .apply()
        _apiKeyUpdatedTrigger.value += 1
    }

    /**
     * Returns the primary API key to use.
     */
    fun getApiKey(context: Context): String {
        val all = getAllConfiguredKeys(context)
        return all.firstOrNull().orEmpty()
    }

    /**
     * Returns all valid configured keys:
     * 1. Valid user custom keys (comma, semicolon, or newline separated) in order
     * 2. Built-in `BuildConfig.GEMINI_API_KEY` automatically kept at the end of the pool as a fallback
     *    so saving a custom key never breaks fallback connectivity!
     */
    fun getAllConfiguredKeys(context: Context): List<String> {
        val customRaw = getCustomKey(context)
        val customKeys = parseKeyList(customRaw)
            .filter { detectProvider(it) != AiKeyProvider.OAUTH_OR_INVALID }

        val buildKey = sanitizeKey(BuildConfig.GEMINI_API_KEY)
        val validBuildKey = buildKey.isNotBlank() &&
            buildKey != "null" &&
            buildKey != "YOUR_GEMINI_API_KEY" &&
            buildKey != "MY_GEMINI_API_KEY" &&
            detectProvider(buildKey) != AiKeyProvider.OAUTH_OR_INVALID

        val combined = LinkedHashSet<String>()
        combined.addAll(customKeys)
        if (validBuildKey) {
            combined.add(buildKey)
        }
        return combined.toList()
    }

    fun parseKeyList(raw: String): List<String> {
        return raw.split(Regex("[,;\\n]+"))
            .map { sanitizeKey(it) }
            .filter {
                it.isNotBlank() &&
                    it != "null" &&
                    it != "YOUR_GEMINI_API_KEY" &&
                    it != "MY_GEMINI_API_KEY"
            }
            .distinct()
    }

    fun hasCustomKey(context: Context): Boolean {
        val custom = parseKeyList(getCustomKey(context))
            .filter { detectProvider(it) != AiKeyProvider.OAUTH_OR_INVALID }
        return custom.isNotEmpty()
    }

    fun getCustomKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_KEY, "")?.trim().orEmpty()
    }

    fun setCustomApiKey(context: Context, key: String) {
        val cleanedList = parseKeyList(key)
        val normalized = cleanedList.joinToString(", ")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_KEY, normalized)
            .apply()
        cooldownUntilMap.clear()
        _apiKeyUpdatedTrigger.value += 1
    }

    fun clearCustomApiKey(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CUSTOM_KEY)
            .apply()
        cooldownUntilMap.clear()
        _apiKeyUpdatedTrigger.value += 1
    }

    fun isConfigured(context: Context): Boolean {
        if (getEngineMode(context) == TranslationEngineMode.FREE_API_ONLY) return true
        return getAllConfiguredKeys(context).isNotEmpty()
    }

    fun getKeySource(context: Context): String {
        val mode = getEngineMode(context)
        if (mode == TranslationEngineMode.FREE_API_ONLY) {
            return "Free API Mode (Unlimited)"
        }
        val customValid = parseKeyList(getCustomKey(context))
            .filter { detectProvider(it) != AiKeyProvider.OAUTH_OR_INVALID }
        val suffix = if (mode == TranslationEngineMode.SMART_AUTO) " + Free API" else ""
        return when {
            customValid.isNotEmpty() -> {
                val providers = customValid.map { detectProvider(it).displayName.substringBefore(" ") }.distinct().joinToString("/")
                if (customValid.size > 1) {
                    "Custom (${customValid.size} $providers keys)$suffix"
                } else {
                    "Custom ($providers key)$suffix"
                }
            }
            BuildConfig.GEMINI_API_KEY.isNotBlank() &&
                BuildConfig.GEMINI_API_KEY != "YOUR_GEMINI_API_KEY" &&
                BuildConfig.GEMINI_API_KEY != "null" -> "System Key$suffix"
            mode == TranslationEngineMode.SMART_AUTO -> "Free API Auto-Fallback"
            else -> "Not configured"
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Cooldown Management
    // ---------------------------------------------------------------------------------------------

    private fun cooldownKey(apiKey: String, model: String): String = "${apiKey.hashCode()}::$model"

    fun isModelOnCooldown(apiKey: String, model: String): Boolean {
        val until = cooldownUntilMap[cooldownKey(apiKey, model)] ?: return false
        if (System.currentTimeMillis() < until) return true
        cooldownUntilMap.remove(cooldownKey(apiKey, model))
        return false
    }

    fun markModelCooldown(apiKey: String, model: String, durationMs: Long) {
        cooldownUntilMap[cooldownKey(apiKey, model)] = System.currentTimeMillis() + durationMs
    }

    fun clearAllCooldowns() {
        cooldownUntilMap.clear()
    }

    // ---------------------------------------------------------------------------------------------
    // Unified Multi-Provider AI Execution (Gemini, Groq, OpenRouter, OpenAI) with Smart Cooldowns
    // ---------------------------------------------------------------------------------------------

    fun executeAiPrompt(
        prompt: String,
        keys: List<String>,
        expectsJson: Boolean = true,
        timeoutMs: Int = 30_000
    ): String {
        val validKeys = keys.map { sanitizeKey(it) }
            .filter {
                it.isNotBlank() &&
                    it != "YOUR_GEMINI_API_KEY" &&
                    it != "MY_GEMINI_API_KEY" &&
                    it != "null" &&
                    detectProvider(it) != AiKeyProvider.OAUTH_OR_INVALID
            }
            .distinct()

        if (validKeys.isEmpty()) {
            throw GeminiHttpException(
                401,
                "No valid AI API key configured. Enter a Gemini (AIza...), Groq (gsk_...), or OpenRouter (sk-or-...) key, or use Free API mode."
            )
        }

        var lastError: Exception? = null

        // Pass 1: Try models not currently in cooldown. Pass 2 (only if all were in cooldown): try primary model per key.
        for (ignoreCooldown in listOf(false, true)) {
            var attemptedAnyInPass = false
            for (apiKey in validKeys) {
                val provider = detectProvider(apiKey)
                val models = when (provider) {
                    AiKeyProvider.GEMINI -> if (ignoreCooldown) listOf(ACTIVE_GEMINI_MODELS.first()) else ACTIVE_GEMINI_MODELS
                    AiKeyProvider.GROQ -> if (ignoreCooldown) listOf(GROQ_MODELS.first()) else GROQ_MODELS
                    AiKeyProvider.OPENROUTER -> if (ignoreCooldown) listOf(OPENROUTER_MODELS.first()) else OPENROUTER_MODELS
                    AiKeyProvider.OPENAI -> OPENAI_MODELS
                    AiKeyProvider.OAUTH_OR_INVALID -> emptyList()
                }

                for (modelName in models) {
                    if (!ignoreCooldown && isModelOnCooldown(apiKey, modelName)) {
                        continue
                    }
                    attemptedAnyInPass = true
                    try {
                        return when (provider) {
                            AiKeyProvider.GEMINI -> callGeminiRest(apiKey, modelName, prompt, expectsJson, timeoutMs)
                            AiKeyProvider.GROQ -> callOpenAiCompatibleRest(
                                endpoint = "https://api.groq.com/openai/v1/chat/completions",
                                apiKey = apiKey,
                                modelName = modelName,
                                prompt = prompt,
                                expectsJson = expectsJson,
                                timeoutMs = timeoutMs
                            )
                            AiKeyProvider.OPENROUTER -> callOpenAiCompatibleRest(
                                endpoint = "https://openrouter.ai/api/v1/chat/completions",
                                apiKey = apiKey,
                                modelName = modelName,
                                prompt = prompt,
                                expectsJson = expectsJson,
                                timeoutMs = timeoutMs
                            )
                            AiKeyProvider.OPENAI -> callOpenAiCompatibleRest(
                                endpoint = "https://api.openai.com/v1/chat/completions",
                                apiKey = apiKey,
                                modelName = modelName,
                                prompt = prompt,
                                expectsJson = expectsJson,
                                timeoutMs = timeoutMs
                            )
                            AiKeyProvider.OAUTH_OR_INVALID -> throw IllegalStateException("Unsupported key format")
                        }
                    } catch (e: GeminiHttpException) {
                        lastError = e
                        when (e.statusCode) {
                            429 -> {
                                // Cooldown this (key, model) for 60 seconds and immediately move to next
                                markModelCooldown(apiKey, modelName, 60_000L)
                            }
                            404 -> {
                                // Model retired/unavailable: cooldown for 1 hour
                                markModelCooldown(apiKey, modelName, 3_600_000L)
                            }
                            400, 401, 403 -> {
                                // If key itself is invalid/unauthorized, cooldown all models on this key for 5 minutes and try next key
                                if (e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                                    e.statusCode == 401 || e.statusCode == 403
                                ) {
                                    models.forEach { m -> markModelCooldown(apiKey, m, 300_000L) }
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        lastError = e
                    }
                }
            }
            if (attemptedAnyInPass) break
        }

        throw lastError ?: GeminiHttpException(429, "All configured AI keys and models are currently rate-limited (429).")
    }

    private fun callGeminiRest(
        apiKey: String,
        modelName: String,
        prompt: String,
        expectsJson: Boolean,
        timeoutMs: Int
    ): String {
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$encodedKey"
        val payload = buildJsonObject {
            put(
                "contents",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put(
                                "parts",
                                buildJsonArray {
                                    add(buildJsonObject { put("text", prompt) })
                                }
                            )
                        }
                    )
                }
            )
            put(
                "generationConfig",
                buildJsonObject {
                    if (expectsJson) {
                        put("responseMimeType", "application/json")
                    }
                    put("temperature", 0.3)
                }
            )
        }.toString()

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
            }
            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $code"
            }

            if (code == 404 || body.contains("not found", ignoreCase = true) || body.contains("no longer available", ignoreCase = true)) {
                throw GeminiHttpException(404, "Model $modelName unavailable (404)")
            }
            if (code !in 200..299) {
                throw GeminiHttpException(code, "Model $modelName HTTP $code: ${extractCleanErrorMessage(body, code)}")
            }

            val root = json.parseToJsonElement(body).jsonObject
            val text = root["candidates"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content

            return text ?: throw IllegalStateException("Empty response from Gemini ($modelName)")
        } finally {
            conn.disconnect()
        }
    }

    private fun callOpenAiCompatibleRest(
        endpoint: String,
        apiKey: String,
        modelName: String,
        prompt: String,
        expectsJson: Boolean,
        timeoutMs: Int
    ): String {
        val payload = buildJsonObject {
            put("model", modelName)
            put("temperature", 0.3)
            if (expectsJson && !modelName.contains(":free")) {
                put(
                    "response_format",
                    buildJsonObject {
                        put("type", "json_object")
                    }
                )
            }
            put(
                "messages",
                buildJsonArray {
                    if (expectsJson) {
                        add(
                            buildJsonObject {
                                put("role", "system")
                                put("content", "You are an expert English-Arabic dictionary and language tutor. Always respond with valid raw JSON only.")
                            }
                        )
                    }
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    )
                }
            )
        }.toString()

        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = timeoutMs
            readTimeout = timeoutMs
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload)
            }
            val code = conn.responseCode
            val body = if (code in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $code"
            }

            if (code !in 200..299) {
                throw GeminiHttpException(code, "$modelName HTTP $code: ${extractCleanErrorMessage(body, code)}")
            }

            val root = json.parseToJsonElement(body).jsonObject
            val text = root["choices"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")
                ?.jsonObject?.get("content")
                ?.jsonPrimitive?.content

            return text ?: throw IllegalStateException("Empty response from $modelName")
        } finally {
            conn.disconnect()
        }
    }

    private fun extractCleanErrorMessage(rawBody: String, statusCode: Int): String {
        return try {
            val obj = json.parseToJsonElement(rawBody).jsonObject
            val errObj = obj["error"]?.jsonObject
            val msg = errObj?.get("message")?.jsonPrimitive?.content
            if (!msg.isNullOrBlank()) {
                msg.take(140)
            } else {
                rawBody.take(140)
            }
        } catch (_: Exception) {
            "HTTP $statusCode (${rawBody.take(100)})"
        }
    }

    /**
     * Tests one or multiple comma/newline-separated candidate keys (Gemini AIza..., Groq gsk_..., OpenRouter sk-or-..., OpenAI sk-...).
     */
    suspend fun testApiKey(candidateKeyInput: String): ApiKeyTestResult = withContext(Dispatchers.IO) {
        val candidates = parseKeyList(candidateKeyInput)
        if (candidates.isEmpty()) {
            return@withContext ApiKeyTestResult.Error("Please enter a valid API key to test.")
        }

        val summaries = mutableListOf<String>()
        var anySuccess = false
        var anyRateLimited = false

        for ((index, key) in candidates.withIndex()) {
            val label = if (candidates.size > 1) "Key ${index + 1}" else "Key"
            val provider = detectProvider(key)

            if (provider == AiKeyProvider.OAUTH_OR_INVALID) {
                val prefix = key.take(6)
                val hint = if (key.startsWith("AQ.") || key.startsWith("ya29.")) {
                    "$label starts with '$prefix' (OAuth/session token, not an API key). Create a Gemini key starting with 'AIzaSy...' at aistudio.google.com/apikey, or use a Groq ('gsk_...') key."
                } else {
                    "$label ('$prefix...') is not a recognized key format. Expected 'AIzaSy...' (Gemini), 'gsk_...' (Groq), or 'sk-...' (OpenRouter/OpenAI)."
                }
                summaries.add(hint)
                continue
            }

            when (val singleResult = testSingleProviderKey(key, provider)) {
                is ApiKeyTestResult.Success -> {
                    anySuccess = true
                    summaries.add("$label (${provider.displayName}): ${singleResult.message}")
                }
                is ApiKeyTestResult.RateLimited -> {
                    anyRateLimited = true
                    summaries.add("$label (${provider.displayName}): ${singleResult.message}")
                }
                is ApiKeyTestResult.Error -> {
                    summaries.add("$label (${provider.displayName}): ${singleResult.message}")
                }
            }
        }

        val combinedMessage = summaries.joinToString("\n")
        when {
            anySuccess -> ApiKeyTestResult.Success(combinedMessage)
            anyRateLimited -> ApiKeyTestResult.RateLimited(combinedMessage)
            else -> ApiKeyTestResult.Error(combinedMessage)
        }
    }

    private fun testSingleProviderKey(key: String, provider: AiKeyProvider): ApiKeyTestResult {
        return try {
            when (provider) {
                AiKeyProvider.GEMINI -> {
                    var lastCode = 0
                    var lastDetail = ""
                    for (modelName in ACTIVE_GEMINI_MODELS) {
                        val encodedKey = URLEncoder.encode(key, "UTF-8")
                        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$encodedKey"
                        var conn: HttpURLConnection? = null
                        try {
                            conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                                requestMethod = "POST"
                                connectTimeout = 10_000
                                readTimeout = 10_000
                                doOutput = true
                                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                                setRequestProperty("x-goog-api-key", key)
                            }
                            val payload = """{"contents":[{"parts":[{"text":"ok"}]}],"generationConfig":{"maxOutputTokens":4}}"""
                            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload) }

                            val code = conn.responseCode
                            val body = if (code in 200..299) {
                                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                            } else {
                                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $code"
                            }
                            lastCode = code
                            lastDetail = extractCleanErrorMessage(body, code)

                            if (code in 200..299) {
                                return ApiKeyTestResult.Success("Active & verified ($modelName • 200 OK)")
                            }
                            if (code == 400 || code == 401 || code == 403) {
                                if (body.contains("API_KEY_INVALID", ignoreCase = true) || code == 401 || code == 403) {
                                    return ApiKeyTestResult.Error("Rejected ($code): $lastDetail")
                                }
                            }
                        } finally {
                            conn?.disconnect()
                        }
                    }
                    if (lastCode == 429) {
                        ApiKeyTestResult.RateLimited("Valid key, but quota is currently rate-limited (429). Smart Auto will use Free API fallback.")
                    } else {
                        ApiKeyTestResult.Error("HTTP $lastCode: $lastDetail")
                    }
                }
                AiKeyProvider.GROQ, AiKeyProvider.OPENROUTER, AiKeyProvider.OPENAI -> {
                    val (endpoint, model) = when (provider) {
                        AiKeyProvider.GROQ -> "https://api.groq.com/openai/v1/chat/completions" to GROQ_MODELS.first()
                        AiKeyProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions" to OPENROUTER_MODELS.first()
                        else -> "https://api.openai.com/v1/chat/completions" to OPENAI_MODELS.first()
                    }
                    val text = callOpenAiCompatibleRest(
                        endpoint = endpoint,
                        apiKey = key,
                        modelName = model,
                        prompt = "Reply with {\"ok\":true}",
                        expectsJson = false,
                        timeoutMs = 10_000
                    )
                    if (text.isNotBlank()) {
                        ApiKeyTestResult.Success("Active & verified ($model • 200 OK)")
                    } else {
                        ApiKeyTestResult.Error("Empty response from ${provider.displayName}")
                    }
                }
                AiKeyProvider.OAUTH_OR_INVALID -> ApiKeyTestResult.Error("Unsupported key format.")
            }
        } catch (e: GeminiHttpException) {
            if (e.statusCode == 429) {
                ApiKeyTestResult.RateLimited("Valid key, currently rate-limited (429). Smart Auto will use Free API fallback.")
            } else {
                ApiKeyTestResult.Error("Rejected (${e.statusCode}): ${e.message}")
            }
        } catch (e: Exception) {
            ApiKeyTestResult.Error("Network error: ${e.message}")
        }
    }
}
