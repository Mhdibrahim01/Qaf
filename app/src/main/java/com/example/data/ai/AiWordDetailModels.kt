package com.example.data.ai

import androidx.compose.runtime.Immutable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Accepts either a JSON Array of Strings (`["معنى ١", "معنى ٢"]`) or a single JSON String (`"معنى ١"`)
 * so that all 13,393 offline dictionary entries in `ai_word_details.json` deserialize without throwing.
 */
object FlexibleStringListSerializer : KSerializer<List<String>> {
    private val delegate = ListSerializer(String.serializer())
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder ?: return delegate.deserialize(decoder)
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }.filter { it.isNotBlank() }
            is JsonPrimitive -> {
                if (element is JsonNull) emptyList()
                else {
                    val raw = element.content.trim()
                    if (raw.isEmpty()) emptyList()
                    else raw.split(Regex("[،؛;]+")).map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf(raw) }
                }
            }
            else -> emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        delegate.serialize(encoder, value)
    }
}

/**
 * Accepts a JSON String, JSONNull, or JSON Array and coerces it cleanly to a String.
 */
object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonNull -> ""
            is JsonPrimitive -> element.content
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }.filter { it.isNotBlank() }.joinToString(" ؛ ")
            else -> ""
        }
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

@Immutable
@Serializable
data class AiExampleItem(
    @Serializable(with = FlexibleStringSerializer::class) val en: String = "",
    @Serializable(with = FlexibleStringSerializer::class) val ar: String = ""
)

@Immutable
@Serializable
data class AiMeaningItem(
    @Serializable(with = FlexibleStringSerializer::class) val pos: String = "",
    @Serializable(with = FlexibleStringListSerializer::class) val arabic: List<String> = emptyList(),
    @SerialName("definition_en") @Serializable(with = FlexibleStringSerializer::class) val definitionEn: String = "",
    @SerialName("definition_ar") @Serializable(with = FlexibleStringSerializer::class) val definitionAr: String = "",
    val example: AiExampleItem? = null
)

@Immutable
@Serializable
data class AiPhraseItem(
    @Serializable(with = FlexibleStringSerializer::class) val en: String = "",
    @Serializable(with = FlexibleStringListSerializer::class) val ar: List<String> = emptyList()
)

@Immutable
@Serializable
data class AiWordDetailJson(
    @Serializable(with = FlexibleStringSerializer::class) val syllables: String = "",
    @SerialName("ipa_us") @Serializable(with = FlexibleStringSerializer::class) val ipaUs: String = "",
    @SerialName("ipa_gb") @Serializable(with = FlexibleStringSerializer::class) val ipaGb: String = "",
    val meanings: List<AiMeaningItem> = emptyList(),
    val phrases: List<AiPhraseItem> = emptyList(),
    @SerialName("etymology_ar") @Serializable(with = FlexibleStringSerializer::class) val etymologyAr: String = "",
    val examples: List<AiExampleItem> = emptyList()
)

object AiWordDetailSanitizer {
    /**
     * Detects whether a cached [AiWordDetailJson] was polluted by synthetic fallback templates
     * (e.g., `"in terms of <word>"`, `"key <word>"`, `"<word> / <syn>"`, `"is used to describe"`).
     */
    fun hasSyntheticTemplates(word: String, data: AiWordDetailJson): Boolean {
        val w = word.trim().lowercase()
        val hasTemplatePhrase = data.phrases.any { p ->
            val en = p.en.trim().lowercase()
            en == "in terms of $w" ||
                en == "key $w" ||
                en == "important $w" ||
                en == "clear $w" ||
                en == "$w that matter" ||
                en == "all $w it" ||
                en.contains(" / ")
        }
        val hasTemplateExample = data.examples.any { ex ->
            ex.en.contains("is used to describe", ignoreCase = true)
        } || data.meanings.any { m ->
            m.example?.en?.contains("is used to describe", ignoreCase = true) == true
        }
        return hasTemplatePhrase || hasTemplateExample
    }

    /**
     * Strips synthetic template phrases and unnatural examples from [data].
     */
    fun sanitize(word: String, data: AiWordDetailJson): AiWordDetailJson {
        val w = word.trim().lowercase()
        val cleanPhrases = data.phrases.filterNot { p ->
            val en = p.en.trim().lowercase()
            en == "in terms of $w" ||
                en == "key $w" ||
                en == "important $w" ||
                en == "clear $w" ||
                en == "$w that matter" ||
                en == "all $w it" ||
                en == "think $w this" ||
                en == "talk $w the plan" ||
                en.contains(" / ")
        }
        val cleanExamples = data.examples.filterNot { ex ->
            ex.en.contains("is used to describe", ignoreCase = true)
        }
        return data.copy(
            phrases = cleanPhrases.ifEmpty { data.phrases.filterNot { it.en.contains("in terms of", ignoreCase = true) } },
            examples = cleanExamples
        )
    }
}

object AiWordDetailValidator {

    /**
     * Strict validation:
     * - 1 to 4 meanings
     * - each meaning has non-blank pos, non-empty arabic list, non-blank definitionEn,
     *   and if example is provided both en and ar must be non-blank
     * - 3 to 6 phrases, each with non-blank en and non-empty ar
     * - every example in root examples has non-blank en and ar
     */
    fun validate(data: AiWordDetailJson): Boolean {
        if (data.meanings.size !in 1..4) return false
        for (m in data.meanings) {
            if (m.pos.isBlank()) return false
            if (m.arabic.isEmpty() || m.arabic.all { it.isBlank() }) return false
            if (m.definitionEn.isBlank()) return false
            if (m.example != null) {
                if (m.example.en.isBlank() || m.example.ar.isBlank()) return false
            }
        }

        if (data.phrases.size !in 3..6) return false
        for (p in data.phrases) {
            if (p.en.isBlank()) return false
            if (p.ar.isEmpty() || p.ar.all { it.isBlank() }) return false
        }

        for (ex in data.examples) {
            if (ex.en.isBlank() || ex.ar.isBlank()) return false
        }

        return true
    }
}
