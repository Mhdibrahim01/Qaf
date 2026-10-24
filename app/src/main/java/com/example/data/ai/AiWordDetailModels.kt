package com.example.data.ai

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class AiExampleItem(
    val en: String = "",
    val ar: String = ""
)

@Immutable
@Serializable
data class AiMeaningItem(
    val pos: String = "",
    val arabic: List<String> = emptyList(),
    @SerialName("definition_en") val definitionEn: String = "",
    val example: AiExampleItem? = null
)

@Immutable
@Serializable
data class AiPhraseItem(
    val en: String = "",
    val ar: List<String> = emptyList()
)

@Immutable
@Serializable
data class AiWordDetailJson(
    val syllables: String = "",
    @SerialName("ipa_us") val ipaUs: String = "",
    @SerialName("ipa_gb") val ipaGb: String = "",
    val meanings: List<AiMeaningItem> = emptyList(),
    val phrases: List<AiPhraseItem> = emptyList(),
    @SerialName("etymology_ar") val etymologyAr: String = "",
    val examples: List<AiExampleItem> = emptyList()
)

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
