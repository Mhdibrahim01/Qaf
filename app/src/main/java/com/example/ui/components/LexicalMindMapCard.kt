package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiWordDetailJson
import com.example.data.importer.PosSenseVariant
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.ArabicTypography
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldAccentBorder
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import com.example.ui.theme.bi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

enum class MindMapBranchType(
    val labelEn: String,
    val labelAr: String
) {
    COLLOCATION("Collocation", "متلازمة لفظية"),
    PREPOSITION("Preposition", "حرف جر / تركيب"),
    SYNONYM("Related / Meaning", "مرادف / معنى")
}

data class MindMapBranchNode(
    val labelEn: String,
    val labelAr: String,
    val type: MindMapBranchType,
    val pillLabelEn: String = labelEn
)

object LexicalMindMapExtractor {

    private val COMMON_PREPOSITIONS = setOf(
        "to", "for", "with", "on", "in", "at", "from", "of", "about", "by",
        "into", "over", "under", "between", "against", "through", "upon", "toward", "towards"
    )

    private val NEGATION_MARKERS = setOf(
        "not", "no", "never", "neither", "without", "lack", "lacking",
        "absence", "free", "unable", "impossible", "non", "fail", "failing", "refuse", "refusing"
    )

    /**
     * Dictionary metalanguage and generic framing words that appear in explanatory definitions
     * (e.g., "Not making any sound", "The action of manipulating...", "Very great in amount")
     * and must NEVER be mistaken for lexical synonyms.
     */
    private val DEFINITION_METALANGUAGE_STOP_WORDS = setOf(
        "the", "a", "an", "to", "of", "in", "for", "on", "with", "at", "by", "from", "or", "and",
        "be", "is", "are", "was", "were", "been", "being", "that", "this", "these", "those", "it",
        "its", "as", "if", "when", "where", "which", "who", "whom", "whose", "what", "how", "why",
        "someone", "somebody", "something", "anyone", "anybody", "anything", "everyone", "everybody",
        "everything", "nobody", "nothing", "person", "people", "thing", "things", "one", "ones",
        "other", "others", "another", "each", "every", "both", "either", "neither", "such", "own",
        "make", "makes", "made", "making", "have", "has", "had", "having", "do", "does", "did", "doing", "done",
        "cause", "causes", "caused", "causing", "show", "shows", "showed", "shown", "showing",
        "give", "gives", "gave", "given", "giving", "take", "takes", "took", "taken", "taking",
        "use", "uses", "used", "using", "get", "gets", "got", "getting", "keep", "keeps", "kept", "keeping",
        "put", "puts", "putting", "bring", "brings", "brought", "bringing", "come", "comes", "came", "coming",
        "go", "goes", "went", "gone", "going", "say", "says", "said", "saying", "look", "looks", "looking",
        "work", "works", "working", "move", "moves", "moving", "act", "acts", "acted", "acting",
        "action", "actions", "activity", "process", "state", "condition", "quality", "fact", "instance",
        "manner", "way", "ways", "method", "system", "practice", "form", "type", "kind", "sort",
        "part", "piece", "group", "number", "amount", "degree", "level", "extent", "point", "time",
        "period", "place", "area", "space", "side", "sense", "case", "result", "effect",
        "ability", "capacity", "tendency", "habit", "feeling", "character", "nature", "faculty", "mind",
        "very", "more", "most", "much", "many", "some", "any", "few", "little", "less", "least",
        "several", "all", "whole", "half", "full", "empty", "great", "large", "small", "high", "low",
        "good", "bad", "new", "old", "long", "short", "certain", "particular", "specific", "special",
        "general", "common", "usual", "normal", "regular", "typical", "especially", "usually", "often",
        "able", "unable", "capable", "likely", "possible", "impossible", "free", "lack", "without",
        "accompanied", "characterized", "consisting", "relating", "related", "connected", "involving",
        "involved", "concerned", "based", "intended", "designed", "meant", "produced", "created",
        "formed", "marked", "existing", "occurring", "happening", "becoming", "physical", "mental",
        "needed", "necessary", "official", "officially", "permanently", "deliberate", "specific",
        "clever", "unscrupulous", "organism", "typical", "self-contained", "vital", "various"
    )

    /**
     * Direct synonym lookup for common words whose dictionary definitions use negation or abstract framing.
     * Every entry pairs a verified English synonym/derivative with its OWN accurate Arabic translation.
     */
    private val CURATED_SYNONYMS_MAP = mapOf(
        "silent" to ("quiet" to "هادئ / صامت"),
        "memory" to ("recall" to "تذكّر / استحضار"),
        "manipulation" to ("influence" to "تأثير / توجيه"),
        "overwhelming" to ("intense" to "هائل / جارف"),
        "puncture" to ("pierce" to "يثقب / يخترق"),
        "psychology" to ("psychological" to "نفسي / سيكولوجي"),
        "into" to ("inside" to "إلى الداخل / بالداخل"),
        "onto" to ("upon" to "على / فوق"),
        "from" to ("away" to "بعيداً عن"),
        "with" to ("together" to "معاً / بصحبة"),
        "without" to ("lacking" to "بدون / يفتقر إلى"),
        "between" to ("among" to "بين / في وسط"),
        "through" to ("across" to "عبر / من خلال"),
        "under" to ("beneath" to "تحت / أسفل"),
        "over" to ("above" to "فوق / أعلى"),
        "absent" to ("away" to "غائب / بعيد"),
        "absence" to ("lack" to "غياب / افتقار"),
        "ability" to ("skill" to "مهارة / قدرة"),
        "able" to ("capable" to "قادر / كفء"),
        "abandon" to ("leave" to "يترك / يهجر"),
        "abolish" to ("repeal" to "يلغي / يبطل"),
        "absolute" to ("complete" to "تام / مطلق"),
        "abroad" to ("overseas" to "في الخارج"),
        "about" to ("approximately" to "تقريباً"),
        "above" to ("overhead" to "فوق / في الأعلى"),
        "optimism" to ("hopefulness" to "تفاؤل / أمل"),
        "optimistic" to ("hopeful" to "متفائل"),
        "option" to ("choice" to "خيار / بديل"),
        "oral" to ("spoken" to "شفهي / منطوق"),
        "order" to ("arrangement" to "ترتيب / نظام"),
        "ordinary" to ("normal" to "عادي / مألوف"),
        "organize" to ("arrange" to "ينظم / يرتب"),
        "organized" to ("structured" to "منظم / مرتب"),
        "origin" to ("source" to "أصل / مصدر"),
        "original" to ("authentic" to "أصلي / مبتكر"),
        "originally" to ("initially" to "في الأصل / مبدئياً")
    )

    /**
     * Verified grammatical dependent prepositions (with their exact Arabic meaning).
     * Replaces blind regex guessing on example sentences so locative adjuncts like
     * "studying psychology at the university" never produce false "+ at" nodes.
     */
    private val VERIFIED_DEPENDENT_PREPOSITIONS = mapOf(
        "depend" to ("on" to "يعتمد على"),
        "rely" to ("on" to "يعوّل على"),
        "focus" to ("on" to "يركز على"),
        "concentrate" to ("on" to "يركز على"),
        "insist" to ("on" to "يصرّ على"),
        "count" to ("on" to "يعتمد على"),
        "consist" to ("of" to "يتكوّن من"),
        "approve" to ("of" to "يوافق على"),
        "accuse" to ("of" to "يتهم بـ"),
        "manipulation" to ("of" to "تلاعب بـ"),
        "result" to ("in" to "يؤدي إلى"),
        "succeed" to ("in" to "ينجح في"),
        "participate" to ("in" to "يشارك في"),
        "engage" to ("in" to "ينخرط في"),
        "puncture" to ("in" to "ثقب في"),
        "lead" to ("to" to "يقود إلى"),
        "contribute" to ("to" to "يساهم في"),
        "belong" to ("to" to "ينتمي إلى"),
        "adapt" to ("to" to "يتكيّف مع"),
        "react" to ("to" to "يتفاعل مع"),
        "respond" to ("to" to "يستجيب لـ"),
        "refer" to ("to" to "يشير إلى"),
        "object" to ("to" to "يعترض على"),
        "suffer" to ("from" to "يعاني من"),
        "recover" to ("from" to "يتعافى من"),
        "prevent" to ("from" to "يمنع من"),
        "protect" to ("from" to "يحمي من"),
        "differ" to ("from" to "يختلف عن"),
        "apologize" to ("for" to "يعتذر عن"),
        "prepare" to ("for" to "يستعد لـ"),
        "apply" to ("for" to "يتقدم بطلب لـ"),
        "comply" to ("with" to "يمتثل لـ"),
        "cope" to ("with" to "يتأقلم مع"),
        "deal" to ("with" to "يتعامل مع"),
        "interfere" to ("with" to "يتدخل في")
    )

    @Volatile
    private var offlineRelatedMap: Map<String, List<Pair<String, String>>>? = null

    fun ensureRelatedMapLoaded(context: Context? = null) {
        if (offlineRelatedMap != null) return
        synchronized(this) {
            if (offlineRelatedMap != null) return
            val parsed = HashMap<String, List<Pair<String, String>>>(11000)
            try {
                val rawJson = when {
                    context != null -> context.assets.open("data_import/lexical_related_map.json")
                        .bufferedReader(Charsets.UTF_8).use { it.readText() }
                    else -> {
                        val f1 = File("app/src/main/assets/data_import/lexical_related_map.json")
                        val f2 = File("src/main/assets/data_import/lexical_related_map.json")
                        when {
                            f1.exists() -> f1.readText(Charsets.UTF_8)
                            f2.exists() -> f2.readText(Charsets.UTF_8)
                            else -> ""
                        }
                    }
                }
                if (rawJson.isNotBlank()) {
                    val root = Json.parseToJsonElement(rawJson) as? JsonObject
                    root?.forEach { (key, value) ->
                        val arr = value as? JsonArray ?: return@forEach
                        val pairs = arr.mapNotNull { item ->
                            val pairArr = item as? JsonArray ?: return@mapNotNull null
                            val en = pairArr.getOrNull(0)?.jsonPrimitive?.content?.trim().orEmpty()
                            val ar = pairArr.getOrNull(1)?.jsonPrimitive?.content?.trim().orEmpty()
                            if (en.isNotBlank() && ar.isNotBlank()) en to ar else null
                        }
                        if (pairs.isNotEmpty()) {
                            parsed[key] = pairs
                        }
                    }
                }
            } catch (_: Exception) {
            }
            offlineRelatedMap = parsed
        }
    }

    /**
     * Generates a compact, non-redundant label for the outer graph pill by stripping the repeated
     * headword (or its common inflections) while preserving the full compound phrase in `labelEn`
     * for the bottom detail card and TTS playback.
     */
    fun formatCompactPillLabel(
        headword: String,
        fullPhrase: String,
        type: MindMapBranchType
    ): String {
        val cleanHead = headword.trim()
        val cleanPhrase = fullPhrase.trim()
        if (cleanHead.isEmpty() || cleanPhrase.isEmpty()) return cleanPhrase

        val headVariants = buildHeadwordInflections(cleanHead)

        if (cleanPhrase.contains("+")) {
            val parts = cleanPhrase.split("+").map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size == 2) {
                if (parts[0].lowercase() in headVariants) return "+ ${parts[1]}"
                if (parts[1].lowercase() in headVariants) return "${parts[0]} +"
            }
        }

        val tokens = cleanPhrase.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size >= 2) {
            val firstClean = tokens.first().lowercase().trim(',', '.', ';', '!', '?')
            val lastClean = tokens.last().lowercase().trim(',', '.', ';', '!', '?')

            if (firstClean in headVariants) {
                val remainder = tokens.drop(1).joinToString(" ")
                return "+ $remainder"
            }
            if (lastClean in headVariants) {
                val prefix = tokens.dropLast(1).joinToString(" ")
                return "$prefix +"
            }
            val matchIdx = tokens.indexOfFirst { it.lowercase().trim(',', '.', ';') in headVariants }
            if (matchIdx in 1 until tokens.lastIndex) {
                val mutable = tokens.toMutableList()
                mutable[matchIdx] = "~"
                return mutable.joinToString(" ")
            }
        }

        return cleanPhrase
    }

    private fun buildHeadwordInflections(headword: String): Set<String> {
        val base = headword.trim().lowercase()
        if (base.isEmpty()) return emptySet()
        val set = mutableSetOf(
            base,
            "${base}s",
            "${base}es",
            "${base}ed",
            "${base}d",
            "${base}ing",
            "${base}ly"
        )
        if (base.endsWith("e") && base.length > 3) {
            val stem = base.dropLast(1)
            set.add("${stem}ing")
            set.add("${stem}ed")
            set.add("${stem}ion")
            set.add("${stem}ation")
        }
        if (base.endsWith("y") && base.length > 3) {
            val stem = base.dropLast(1)
            set.add("${stem}ies")
            set.add("${stem}ied")
        }
        if (base.endsWith("tion") && base.length > 5) {
            val stem = base.removeSuffix("tion")
            set.add("${stem}te")
            set.add("${stem}tes")
            set.add("${stem}ted")
            set.add("${stem}ting")
        }
        return set
    }

    private fun sharesStemWithHeadword(headword: String, candidate: String): Boolean {
        val h = headword.trim().lowercase()
        val c = candidate.trim().lowercase()
        if (h == c) return true
        if (c in buildHeadwordInflections(h) || h in buildHeadwordInflections(c)) return true
        val minPrefix = minOf(h.length, c.length, 6)
        if (minPrefix >= 5 && h.take(minPrefix) == c.take(minPrefix)) return true
        return false
    }

    private fun normalizeGerundToBase(word: String): String {
        val w = word.lowercase().trim()
        if (!w.endsWith("ing") || w.length < 6) return w
        val stem = w.removeSuffix("ing")
        return when {
            stem.endsWith("at") || stem.endsWith("ut") || stem.endsWith("iz") ||
                stem.endsWith("nc") || stem.endsWith("rc") || stem.endsWith("ag") ||
                stem.endsWith("ng") || stem.endsWith("lv") || stem.endsWith("rv") -> "${stem}e"
            stem.length >= 3 && stem.last() == stem[stem.length - 2] && stem.last() !in setOf('l', 's', 'f', 'z') ->
                stem.dropLast(1)
            else -> stem
        }
    }

    private fun isSyntheticTemplatePhrase(headLower: String, phraseEn: String): Boolean {
        val p = phraseEn.trim().lowercase()
        return p == "in terms of $headLower" ||
            p == "key $headLower" ||
            p == "important $headLower" ||
            p == "clear $headLower" ||
            p == "$headLower that matter" ||
            p == "all $headLower it" ||
            p == "think $headLower this" ||
            p == "talk $headLower the plan"
    }

    /**
     * Multi-tier linguistic extractor that finds a high-confidence Synonym / Related lexical node
     * while strictly blocking negated/non-sequitur tokens (like "silent" -> "making",
     * "psychology" -> "study", "into" -> "movement").
     */
    fun extractValidatedSynonym(
        headword: String,
        pos: String,
        definitionEn: String,
        etymologyAr: String = ""
    ): String? {
        val cleanHead = headword.trim().lowercase()
        if (cleanHead.isEmpty()) return null

        // Tier 0: Curated direct synonym map & verified offline dictionary map
        CURATED_SYNONYMS_MAP[cleanHead]?.let { return it.first }
        ensureRelatedMapLoaded()
        offlineRelatedMap?.get(cleanHead)?.firstOrNull()?.let { return it.first }

        val raw = definitionEn.trim()
        if (raw.isNotEmpty()) {
            val clauses = raw.split(";").map { it.trim() }.filter { it.isNotBlank() }

            // Pass 1: Explicit 1-2 word semicolon glosses (e.g. "; toughness" -> "toughness")
            for (clause in clauses.asReversed()) {
                val lowerClause = clause.lowercase()
                val clauseTokens = lowerClause
                    .split(Regex("\\s+"))
                    .map { it.replace(Regex("[^a-z-]"), "") }
                    .filter { it.isNotEmpty() }

                if (clauseTokens.isEmpty()) continue
                if (clauseTokens.any { it in NEGATION_MARKERS }) continue

                val strippedGloss = clause
                    .replace(Regex("^[Tt]o\\s+"), "")
                    .replace(Regex("^[Aa]n?\\s+"), "")
                    .replace(Regex("^[Tt]he\\s+"), "")
                    .trim()
                    .trimEnd('.', ',', ';', ':')

                val glossTokens = strippedGloss
                    .split(Regex("\\s+"))
                    .map { it.replace(Regex("[^A-Za-z-]"), "") }
                    .filter { it.isNotEmpty() }

                if (glossTokens.size in 1..2 && strippedGloss.length in 3..20) {
                    val contentWords = glossTokens.filter { it.lowercase() !in COMMON_PREPOSITIONS }
                    val allValid = contentWords.isNotEmpty() && contentWords.all { word ->
                        val wLower = word.lowercase()
                        wLower.length >= 3 &&
                            wLower !in DEFINITION_METALANGUAGE_STOP_WORDS &&
                            !sharesStemWithHeadword(cleanHead, wLower)
                    }
                    if (allValid) {
                        return glossTokens.joinToString(" ") { it.lowercase() }
                    }
                }
            }

            // Pass 2: Clean Verb genus ONLY ("To [adv] <verb> [prep] ...")
            for (clause in clauses) {
                val lowerClause = clause.lowercase()
                val clauseTokens = lowerClause.split(Regex("\\s+"))
                    .map { it.replace(Regex("[^a-z-]"), "") }
                    .filter { it.isNotBlank() }
                if (clauseTokens.take(3).any { it in NEGATION_MARKERS }) continue

                val isVerbPos = pos.trim().lowercase().startsWith("v") || clause.startsWith("To ")
                if (isVerbPos) {
                    val verbMatch = Regex(
                        "^To\\s+(?:[a-z]+ly\\s+)?([a-zA-Z]{3,15})(?:\\s+or\\s+([a-zA-Z]{3,15}))?(?:\\s+(to|for|with|on|in|at|from|of|about|into|upon|over))?\\b",
                        RegexOption.IGNORE_CASE
                    ).find(clause)

                    if (verbMatch != null) {
                        val v1 = verbMatch.groupValues[1].lowercase()
                        val v2 = verbMatch.groupValues.getOrNull(2)?.lowercase().orEmpty()
                        val prep = verbMatch.groupValues.getOrNull(3)?.lowercase().orEmpty()
                        val chosenVerb = when {
                            v1 !in DEFINITION_METALANGUAGE_STOP_WORDS && !sharesStemWithHeadword(cleanHead, v1) -> v1
                            v2.isNotBlank() && v2 !in DEFINITION_METALANGUAGE_STOP_WORDS && !sharesStemWithHeadword(cleanHead, v2) -> v2
                            else -> ""
                        }
                        if (chosenVerb.isNotBlank()) {
                            return if (prep.isNotBlank() && chosenVerb == v1 && v2.isEmpty() &&
                                prep in setOf("on", "in", "to", "for", "with", "from", "of", "about", "into", "over") &&
                                clause.lowercase().contains("$chosenVerb $prep ")
                            ) {
                                "$chosenVerb $prep"
                            } else {
                                chosenVerb
                            }
                        }
                    }
                }
            }
        }

        // Pass 3: Morphological Word-Family partner
        deriveWordFamilyPartner(cleanHead, etymologyAr)?.let { return it }

        return null
    }

    private fun deriveWordFamilyPartner(headword: String, etymologyAr: String): String? {
        val clean = headword.trim().lowercase()
        if (etymologyAr.isNotBlank()) {
            val rootMatch = Regex("[(\"'\\s]([a-zA-Z]{4,15})[\"')\\s]").findAll(etymologyAr)
            for (m in rootMatch) {
                val word = m.groupValues[1].lowercase()
                if (word != clean && word !in DEFINITION_METALANGUAGE_STOP_WORDS && !word.endsWith("io") && !word.endsWith("us") && !word.endsWith("ia")) {
                    return word
                }
            }
        }
        return when {
            clean.endsWith("ology") && clean.length > 6 -> "${clean.removeSuffix("ology")}ological"
            clean.endsWith("tion") && clean.length > 6 -> "${clean.removeSuffix("tion")}te"
            clean.endsWith("ment") && clean.length > 6 -> clean.removeSuffix("ment")
            clean.endsWith("ness") && clean.length > 6 -> clean.removeSuffix("ness")
            clean.endsWith("ity") && clean.length > 6 -> clean.removeSuffix("ity")
            clean.endsWith("ent") && clean.length > 5 -> "${clean.removeSuffix("ent")}ence"
            clean.endsWith("ant") && clean.length > 5 -> "${clean.removeSuffix("ant")}ance"
            clean.endsWith("ing") && clean.length > 6 -> normalizeGerundToBase(clean)
            clean.endsWith("ly") && clean.length > 5 -> clean.removeSuffix("ly")
            else -> null
        }
    }

    /**
     * Extracts up to 6 balanced lexical nodes (Collocations, Preposition Patterns, and Related/Synonyms)
     * from offline `AiWordDetailJson` and `PosSenseVariant` entries.
     * Reserves space so at least 1-2 purple `SYNONYM` (`Related`) nodes are always included with their
     * own accurate Arabic dictionary translations.
     */
    fun extractNodes(
        headword: String,
        aiDetail: AiWordDetailJson?,
        posVariants: List<PosSenseVariant>
    ): List<MindMapBranchNode> {
        val cleanHead = headword.trim()
        if (cleanHead.isEmpty()) return emptyList()
        val headLower = cleanHead.lowercase()
        ensureRelatedMapLoaded()

        val collocations = ArrayList<MindMapBranchNode>()
        val prepositions = ArrayList<MindMapBranchNode>()
        val synonyms = ArrayList<MindMapBranchNode>()

        val seenEn = HashSet<String>()
        val seenPills = HashSet<String>()
        seenEn.add(headLower)
        val headVariants = buildHeadwordInflections(cleanHead)
        val headIsPreposition = headLower in COMMON_PREPOSITIONS

        // 1. Extract Collocations & Preposition Patterns from aiDetail.phrases
        aiDetail?.phrases?.forEach { phrase ->
            val en = phrase.en.trim()
            if (en.isBlank() || isSyntheticTemplatePhrase(headLower, en)) return@forEach

            // Handle legacy "<headword> / <synonym>" entries by routing <synonym> to synonyms instead of collocations
            if (en.contains("/")) {
                val slashParts = en.split("/").map { it.trim() }.filter { it.isNotBlank() }
                if (slashParts.size == 2 && slashParts[0].lowercase() in headVariants) {
                    val synCandidate = slashParts[1].lowercase()
                    if (synCandidate.length in 3..18 && seenEn.add(synCandidate) && seenPills.add(synCandidate)) {
                        val ar = phrase.ar.firstOrNull { it.isNotBlank() }.orEmpty()
                        synonyms.add(
                            MindMapBranchNode(
                                labelEn = synCandidate,
                                pillLabelEn = synCandidate,
                                labelAr = ar,
                                type = MindMapBranchType.SYNONYM
                            )
                        )
                    }
                    return@forEach
                }
            }

            if (seenEn.add(en.lowercase())) {
                val ar = phrase.ar.firstOrNull { it.isNotBlank() }.orEmpty()
                val tokens = en.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
                val nonHeadTokens = tokens.filterNot { it in headVariants }
                val isPrepPattern = en.contains("+") || (
                    !headIsPreposition &&
                        nonHeadTokens.isNotEmpty() &&
                        nonHeadTokens.all { it in COMMON_PREPOSITIONS }
                    )
                val branchType = if (isPrepPattern) {
                    MindMapBranchType.PREPOSITION
                } else {
                    MindMapBranchType.COLLOCATION
                }
                val pillLabel = formatCompactPillLabel(cleanHead, en, branchType)
                if (seenPills.add(pillLabel.lowercase())) {
                    val node = MindMapBranchNode(
                        labelEn = en,
                        pillLabelEn = pillLabel,
                        labelAr = ar,
                        type = branchType
                    )
                    if (branchType == MindMapBranchType.PREPOSITION) {
                        prepositions.add(node)
                    } else {
                        collocations.add(node)
                    }
                }
            }
        }

        // 2. Add Verified Grammatical Dependent Preposition if applicable (never guess blindly from example sentences!)
        if (prepositions.isEmpty()) {
            val verifiedPrep = VERIFIED_DEPENDENT_PREPOSITIONS[headLower]
            if (verifiedPrep != null) {
                val (prep, prepAr) = verifiedPrep
                val patternEn = "$cleanHead + $prep"
                val pillLabel = "+ $prep"
                if (seenEn.add(patternEn.lowercase()) && seenPills.add(pillLabel.lowercase())) {
                    prepositions.add(
                        MindMapBranchNode(
                            labelEn = patternEn,
                            pillLabelEn = pillLabel,
                            labelAr = prepAr,
                            type = MindMapBranchType.PREPOSITION
                        )
                    )
                }
            }
        }

        // 3. Extract Verified Related Concepts / Synonyms with their OWN accurate Arabic translations
        val verifiedPairs = buildList {
            CURATED_SYNONYMS_MAP[headLower]?.let { add(it) }
            offlineRelatedMap?.get(headLower)?.forEach { pair ->
                if (none { it.first.equals(pair.first, ignoreCase = true) }) {
                    add(pair)
                }
            }
        }

        for ((synEn, synAr) in verifiedPairs) {
            if (synonyms.size >= 2) break
            if (seenEn.add(synEn.lowercase()) && seenPills.add(synEn.lowercase())) {
                synonyms.add(
                    MindMapBranchNode(
                        labelEn = synEn,
                        pillLabelEn = synEn,
                        labelAr = synAr,
                        type = MindMapBranchType.SYNONYM
                    )
                )
            }
        }

        // Fallback for custom/unindexed words (e.g. unit test custom definitions)
        if (synonyms.isEmpty()) {
            data class DefCandidate(val pos: String, val defEn: String, val defAr: String)
            val definitions = buildList {
                aiDetail?.meanings?.forEach { m ->
                    val ar = m.arabic.firstOrNull { it.isNotBlank() } ?: m.definitionAr
                    if (m.definitionEn.isNotBlank()) add(DefCandidate(m.pos, m.definitionEn, ar))
                }
                posVariants.forEach { v ->
                    if (v.definitionEn.isNotBlank()) {
                        add(DefCandidate(v.pos, v.definitionEn, v.arabicMeaning.ifBlank { v.definitionAr }))
                    }
                }
            }
            val etymologyAr = aiDetail?.etymologyAr.orEmpty()
            for (item in definitions) {
                if (synonyms.size >= 2) break
                val candidate = extractValidatedSynonym(cleanHead, item.pos, item.defEn, etymologyAr)
                if (!candidate.isNullOrBlank() &&
                    seenEn.add(candidate.lowercase()) &&
                    seenPills.add(candidate.lowercase())
                ) {
                    val candidateOwnAr = CURATED_SYNONYMS_MAP[candidate.lowercase()]?.second
                        ?: offlineRelatedMap?.get(candidate.lowercase())?.firstOrNull()?.second
                        ?: item.defAr
                    synonyms.add(
                        MindMapBranchNode(
                            labelEn = candidate,
                            pillLabelEn = candidate,
                            labelAr = candidateOwnAr,
                            type = MindMapBranchType.SYNONYM
                        )
                    )
                }
            }
        }

        // 4. Assemble up to 6 nodes, guaranteeing that Collocations, Prepositions, and Related/Synonyms are represented
        val combined = ArrayList<MindMapBranchNode>(6)
        combined.addAll(collocations.take(3))
        combined.addAll(prepositions.take(2))
        combined.addAll(synonyms.take(2))

        if (combined.size < 6) {
            for (extra in collocations.drop(3) + prepositions.drop(2) + synonyms.drop(2)) {
                if (combined.size >= 6) break
                if (extra !in combined) combined.add(extra)
            }
        }

        return combined.take(6)
    }

    /**
     * Computes 5-tier non-overlapping radial/diagonal offsets for `1..6` branch nodes:
     * - Tier 1 (Top Pole, `-90°`): `y = centerY - 0.39 * H`
     * - Tier 2 (Upper Diagonals, `-36°, -144°`): `y = centerY - 0.21 * H`, `x = centerX ± 0.30 * W`
     * - Tier 3 (Center Headword): `y = centerY`, `x = centerX`
     * - Tier 4 (Lower Diagonals, `+36°, +144°`): `y = centerY + 0.21 * H`, `x = centerX ± 0.30 * W`
     * - Tier 5 (Bottom Pole, `+90°`): `y = centerY + 0.39 * H`
     *
     * Every tier is vertically separated from adjacent tiers, preventing both center-node collisions
     * and top/bottom neighbor collisions.
     */
    fun computeRadialNodeOffsets(
        nodeCount: Int,
        widthPx: Float,
        heightPx: Float
    ): List<Offset> {
        if (nodeCount <= 0) return emptyList()
        val centerX = widthPx / 2f
        val centerY = heightPx / 2f

        val anglesDeg: List<Double> = when (nodeCount) {
            1 -> listOf(-90.0)
            2 -> listOf(-90.0, 90.0)
            3 -> listOf(-90.0, 42.0, 138.0)
            4 -> listOf(-45.0, 45.0, 135.0, -135.0) // 4 Diagonal Quadrants (NE, SE, SW, NW)
            5 -> listOf(-90.0, -38.0, 90.0, 142.0, -142.0) // Top, NE, Bottom, SW, NW (balanced poles!)
            else -> listOf(-90.0, -36.0, 36.0, 90.0, 144.0, -144.0) // Full 6-spoke hexagonal radial layout
        }

        val polarRadiusY = (heightPx * 0.39f).coerceAtLeast(74f)
        val diagonalOffsetX = (widthPx * 0.29f).coerceAtLeast(76f)
        val diagonalOffsetY = (heightPx * 0.22f).coerceAtLeast(48f)

        return anglesDeg.take(nodeCount).map { deg ->
            val rad = Math.toRadians(deg)
            val cosVal = cos(rad).toFloat()
            val sinVal = sin(rad).toFloat()

            if (abs(cosVal) < 0.15f) {
                Offset(
                    x = centerX,
                    y = centerY + sign(sinVal) * polarRadiusY
                )
            } else {
                Offset(
                    x = centerX + sign(cosVal) * diagonalOffsetX,
                    y = centerY + sign(sinVal) * diagonalOffsetY
                )
            }
        }
    }
}

// Distinct Slate-Indigo Palette for Center Headword Node so it never clashes with Amber Preposition nodes
private val CenterNodeContainerColor = Color(0xFF1E293B)
private val CenterNodeTextColor = Color(0xFFF8FAFC)

@Composable
fun VisualLexicalMindMapCard(
    headword: String,
    aiDetail: AiWordDetailJson?,
    posVariants: List<PosSenseVariant>,
    onSpeakText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val nodes = remember(headword, aiDetail, posVariants, context) {
        LexicalMindMapExtractor.ensureRelatedMapLoaded(context)
        LexicalMindMapExtractor.extractNodes(headword, aiDetail, posVariants)
    }

    if (nodes.isEmpty()) return

    var selectedNodeIndex by remember(headword, nodes) { mutableIntStateOf(0) }
    val safeSelectedIndex = selectedNodeIndex.coerceIn(0, nodes.size - 1)
    val selectedNode = nodes[safeSelectedIndex]

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lexical_mind_map_card"),
        color = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GoldAccentBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = bi("Lexical Mind-Map & Collocations", "الشبكة البصرية للمترادفات والمتلازمات"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldAccent
                        )
                        Text(
                            text = bi("Tap any node to hear pronunciation & view meaning", "اضغط على أي فرع لسماع النطق وعرض المعنى"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Legend Row (Disambiguated from Center Headword Node)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MindMapLegendDot(color = SkyAccent, label = bi("Headword", "الكلمة"))
                MindMapLegendDot(color = EmeraldAccent, label = bi("Collocation", "متلازمة"))
                MindMapLegendDot(color = AmberAccent, label = bi("Preposition", "حرف جر"))
                MindMapLegendDot(color = VioletAccent, label = bi("Related", "مرادف"))
            }

            // Radial Mind-Map Canvas + Interactive Node Chips (Locked to LTR for spatial geometry)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(254.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceElevatedDark)
                        .testTag("mind_map_canvas_container"),
                    contentAlignment = Alignment.Center
                ) {
                    val density = LocalDensity.current
                    val widthPx = with(density) { maxWidth.toPx() }
                    val heightPx = with(density) { maxHeight.toPx() }
                    val centerX = widthPx / 2f
                    val centerY = heightPx / 2f

                    val nodeOffsets = remember(nodes.size, widthPx, heightPx) {
                        LexicalMindMapExtractor.computeRadialNodeOffsets(
                            nodeCount = nodes.size,
                            widthPx = widthPx,
                            heightPx = heightPx
                        )
                    }

                    // Curved Bézier connector lines on Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("mind_map_canvas")
                    ) {
                        nodeOffsets.forEachIndexed { idx, target ->
                            val node = nodes[idx]
                            val branchColor = when (node.type) {
                                MindMapBranchType.COLLOCATION -> EmeraldAccent
                                MindMapBranchType.PREPOSITION -> AmberAccent
                                MindMapBranchType.SYNONYM -> VioletAccent
                            }
                            val isSelected = idx == safeSelectedIndex
                            val path = Path().apply {
                                moveTo(centerX, centerY)
                                val controlX = centerX + (target.x - centerX) * 0.35f
                                val controlY = target.y
                                quadraticTo(controlX, controlY, target.x, target.y)
                            }
                            drawPath(
                                path = path,
                                color = branchColor.copy(alpha = if (isSelected) 0.92f else 0.52f),
                                style = Stroke(
                                    width = if (isSelected) 4.0f else 2.5f,
                                    pathEffect = if (isSelected) null else PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                )
                            )
                        }
                    }

                    // Branch Nodes positioned on 5-tier radial spokes using opaque background so connector lines never bleed through
                    val basePillSurface = SurfaceDark
                    nodeOffsets.forEachIndexed { idx, pos ->
                        val node = nodes[idx]
                        val branchColor = when (node.type) {
                            MindMapBranchType.COLLOCATION -> EmeraldAccent
                            MindMapBranchType.PREPOSITION -> AmberAccent
                            MindMapBranchType.SYNONYM -> VioletAccent
                        }
                        val isSelected = idx == safeSelectedIndex
                        val offsetX = (pos.x - centerX).roundToInt()
                        val offsetY = (pos.y - centerY).roundToInt()
                        // 100% opaque surface fill prevents the underlying Canvas line from striking through the text
                        val pillContainerColor = if (isSelected) {
                            lerp(basePillSurface, branchColor, 0.24f)
                        } else {
                            basePillSurface
                        }

                        Surface(
                            modifier = Modifier
                                .offset { IntOffset(offsetX, offsetY) }
                                .widthIn(max = 144.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable {
                                    selectedNodeIndex = idx
                                    onSpeakText(node.labelEn.replace(" + ", " "))
                                }
                                .testTag("mind_map_node_$idx"),
                            color = pillContainerColor,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(if (isSelected) 1.8.dp else 1.2.dp, branchColor)
                        ) {
                            Text(
                                text = node.pillLabelEn,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 11.5.sp,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = if (isSelected) branchColor else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.5.dp)
                            )
                        }
                    }

                    // Center Headword Node (Neutral Slate-Indigo with Sky-Blue border to disambiguate from Amber Preposition)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onSpeakText(headword) }
                            .testTag("mind_map_center_node"),
                        color = CenterNodeContainerColor,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(2.dp, SkyAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak headword",
                                tint = SkyAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = headword,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = CenterNodeTextColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Selected Node Detail Bar (Shows full compound phrase + Arabic meaning)
            val activeColor = when (selectedNode.type) {
                MindMapBranchType.COLLOCATION -> EmeraldAccent
                MindMapBranchType.PREPOSITION -> AmberAccent
                MindMapBranchType.SYNONYM -> VioletAccent
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mind_map_selected_bar"),
                color = SurfaceElevatedDark,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, activeColor.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = activeColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = bi(selectedNode.type.labelEn, selectedNode.type.labelAr),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = activeColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = selectedNode.labelEn.replace(" + ", " "),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    textDirection = TextDirection.Ltr
                                ),
                                color = TextPrimary
                            )
                        }
                        if (selectedNode.labelAr.isNotBlank()) {
                            BidiText(
                                text = selectedNode.labelAr,
                                style = ArabicTypography.bodySmall,
                                color = TextSecondary,
                                fillMaxWidth = true
                            )
                        }
                    }

                    IconButton(
                        onClick = { onSpeakText(selectedNode.labelEn.replace(" + ", " ")) },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("mind_map_selected_speak_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak selected branch",
                            tint = activeColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MindMapLegendDot(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextMuted
        )
    }
}
