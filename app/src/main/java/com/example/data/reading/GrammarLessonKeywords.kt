package com.example.data.reading

import com.example.data.local.GrammarLessonEntity
import com.example.data.local.ReadingSentenceItem

/**
 * Deterministic keywords for all 90 grammar lessons (g001 to g090).
 * Used for both real-time UI highlighting and automated sentence grammar tag alignment.
 */
val GRAMMAR_LESSON_KEYWORDS: Map<String, Set<String>> = mapOf(
    "g001" to setOf("am", "is", "are", "'m", "'s", "'re"),
    "g002" to setOf("i", "you", "he", "she", "it", "we", "they", "my", "your", "his", "her", "its", "our", "their"),
    "g003" to setOf("a", "an", "the"),
    "g004" to setOf("patients", "walks", "hearts", "doctors", "minutes", "parks", "neighbors", "trees", "bottles", "backpacks", "habits", "thoughts", "adults", "bodies", "activities", "muscles"),
    "g005" to setOf("this", "that", "these", "those"),
    "g006" to setOf("there", "is", "are"),
    "g007" to setOf("wants", "reads", "discovers", "checks", "writes", "reviews", "does", "not", "sleeps"),
    "g008" to setOf("am", "is", "are", "studying", "helping", "typing", "falling", "reading", "enjoying"),
    "g009" to setOf("can", "can't", "cannot"),
    "g010" to setOf("what", "where", "when", "who", "why", "how", "which"),
    "g011" to setOf("in", "on", "at"),
    "g012" to setOf("some", "any"),
    "g013" to setOf("wear", "touch", "listen", "wash", "run", "consume", "follow", "not", "never", "always"),
    "g014" to setOf("was", "were", "wasn't", "weren't"),
    "g015" to setOf("did", "didn't", "went", "saw", "bought", "came", "made", "took", "had"),
    "g016" to setOf("was", "were", "working", "testing", "experiencing", "restarting", "analyzing", "collaborating"),
    "g017" to setOf("have", "has", "prepared", "researched", "helped", "improved", "answered", "created"),
    "g018" to setOf("warmer", "cozier", "delicious", "fuller", "more", "most", "stronger", "deeper", "than"),
    "g019" to setOf("will", "won't", "going"),
    "g020" to setOf("visiting", "installing", "hosting", "arriving", "launching", "meeting"),
    "g021" to setOf("must", "have", "to", "don't"),
    "g022" to setOf("should", "shouldn't"),
    "g023" to setOf("always", "usually", "often", "sometimes", "rarely", "never"),
    "g024" to setOf("patiently", "carefully", "quietly", "warmly", "politely", "gently", "skillfully"),
    "g025" to setOf("many", "much", "lot", "few", "little"),
    "g026" to setOf("it", "hers", "her", "him", "mine", "them", "their", "me", "us", "yours", "ours", "theirs"),
    "g027" to setOf("enjoys", "wants", "decided", "loves", "hopes", "drawing", "creating"),
    "g028" to setOf("if", "will", "is", "rains", "drops", "arrives", "remains"),
    "g029" to setOf("too", "enough"),
    "g030" to setOf("for", "since", "have", "has"),
    "g031" to setOf("have", "has", "did", "didn't", "since", "ago", "last"),
    "g032" to setOf("have", "has", "been"),
    "g033" to setOf("had", "hadn't"),
    "g034" to setOf("used", "would"),
    "g035" to setOf("if", "would", "had", "were"),
    "g036" to setOf("when", "unless", "as", "soon"),
    "g037" to setOf("is", "are", "was", "were"),
    "g038" to setOf("said", "told", "explained", "stated"),
    "g039" to setOf("who", "which", "that"),
    "g040" to setOf("might", "may", "could", "must", "can't"),
    "g041" to setOf("isn't", "aren't", "don't", "doesn't", "didn't", "haven't", "won't"),
    "g042" to setOf("up", "down", "off", "on", "out", "away"),
    "g043" to setOf("so", "such"),
    "g044" to setOf("both", "and"),
    "g045" to setOf("both", "either", "neither", "all", "none"),
    "g046" to setOf("will", "be"),
    "g047" to setOf("could", "tell", "wonder", "if", "whether"),
    "g048" to setOf("myself", "yourself", "himself", "herself", "itself", "ourselves", "themselves"),
    "g049" to setOf("the", "a", "an"),
    "g050" to setOf("about", "in", "of", "to", "for", "with", "on", "at"),
    "g051" to setOf("had", "been"),
    "g052" to setOf("wish", "if", "only", "had", "would", "could"),
    "g053" to setOf("must", "can't", "could", "might", "have"),
    "g054" to setOf("if", "had", "would", "have"),
    "g055" to setOf("if", "had", "would"),
    "g056" to setOf("having", "being"),
    "g057" to setOf("is", "are", "was", "were", "been", "being"),
    "g058" to setOf("have", "get"),
    "g059" to setOf("whether", "if"),
    "g060" to setOf("will", "have", "been"),
    "g061" to setOf("supposed", "bound", "due", "likely"),
    "g062" to setOf("whose", "whom", "where", "when", "why"),
    "g063" to setOf("whatever", "whoever", "whenever", "wherever", "however"),
    "g064" to setOf("having", "knowing", "seeing", "hearing", "feeling"),
    "g065" to setOf("although", "though", "even", "despite", "spite", "whereas", "while", "however", "therefore", "consequently"),
    "g066" to setOf("because", "since", "as", "due", "owing", "lead", "result"),
    "g067" to setOf("so", "such", "that", "order", "case"),
    "g068" to setOf("more", "most", "less", "least", "as", "than", "nowhere", "near"),
    "g069" to setOf("hardly", "scarcely", "barely", "no", "sooner"),
    "g070" to setOf("rather", "better", "prefer"),
    "g071" to setOf("little", "few", "hardly", "scarcely", "rarely", "seldom"),
    "g072" to setOf("hardly", "had", "never", "under", "circumstances", "rarely", "did"),
    "g073" to setOf("it", "was", "what", "is", "all", "that"),
    "g074" to setOf("not", "only", "also"),
    "g075" to setOf("used", "to", "be", "get"),
    "g076" to setOf("would", "always", "constantly", "forever"),
    "g077" to setOf("should", "had", "were"),
    "g078" to setOf("providing", "provided", "condition", "as", "long", "supposing"),
    "g079" to setOf("believed", "thought", "considered", "reported", "said", "understood"),
    "g080" to setOf("suggest", "recommend", "demand", "insist", "vital", "essential", "crucial"),
    "g081" to setOf("do", "so", "one", "ones", "not"),
    "g082" to setOf("having", "being", "with", "without"),
    "g083" to setOf("tend", "appear", "seem", "suggest", "indicate", "arguably", "likely"),
    "g084" to setOf("development", "implementation", "decision", "assumption", "establishment"),
    "g085" to setOf("needn't", "shall", "dare"),
    "g086" to setOf("was", "were", "about", "going"),
    "g087" to setOf("never", "rarely", "seldom", "little", "nowhere"),
    "g088" to setOf("namely", "specifically", "that", "is"),
    "g089" to setOf("anyway", "mind", "you", "fact", "indeed", "matter"),
    "g090" to setOf("whereas", "albeit", "notwithstanding")
)

private val TOKEN_CLEAN_REGEX = Regex("[^a-zA-Z0-9'_]+")
private val PAREN_REGEX = Regex("""\(([^)]+)\)""")
private val WHITESPACE_REGEX = Regex("\\s+")

/**
 * Extracts target grammar keywords from a [GrammarLessonEntity].
 * Uses the predefined map first, and dynamically derives keywords from lesson titles as a fallback.
 */
fun extractGrammarKeywords(lesson: GrammarLessonEntity?): Set<String> {
    if (lesson == null) return emptySet()
    val predefined = GRAMMAR_LESSON_KEYWORDS[lesson.id]
    if (!predefined.isNullOrEmpty()) return predefined

    val keywords = mutableSetOf<String>()
    val title = lesson.titleEn.lowercase()

    // 1. Text after colon: e.g. "Advanced linking: whereas, albeit, notwithstanding"
    if (title.contains(":")) {
        val afterColon = title.substringAfter(":")
        afterColon.split(',', '/', ';').forEach { raw ->
            raw.trim().split(" and ", " or ").forEach { word ->
                val clean = word.trim().replace(TOKEN_CLEAN_REGEX, "")
                if (clean.isNotBlank()) {
                    keywords.add(clean)
                }
            }
        }
    }

    // 2. Text inside parentheses: e.g. "Zero conditional and time clauses (when, as soon as, unless)"
    val parenMatch = PAREN_REGEX.find(title)
    if (parenMatch != null) {
        val inside = parenMatch.groupValues[1]
        inside.split(',', '/', ';').forEach { raw ->
            raw.trim().split(" and ", " or ").forEach { word ->
                val clean = word.trim().replace(TOKEN_CLEAN_REGEX, "")
                if (clean.isNotBlank()) {
                    keywords.add(clean)
                }
            }
        }
    }

    // 3. Known connector phrases in title
    val parts = title.replace(TOKEN_CLEAN_REGEX, " ").split(WHITESPACE_REGEX)
    for (w in parts) {
        if (w in listOf(
                "whereas", "albeit", "notwithstanding", "although", "though",
                "unless", "since", "until", "while", "whenever", "wherever",
                "provided", "providing", "supposing", "either", "neither", "both",
                "enough", "too", "would", "must", "can't", "should", "shouldn't",
                "could", "might", "may"
            )
        ) {
            keywords.add(w)
        }
    }

    if (title.contains("for and since")) {
        keywords.add("for")
        keywords.add("since")
    }
    if (title.contains("too and enough")) {
        keywords.add("too")
        keywords.add("enough")
    }
    if (title.contains("so and such")) {
        keywords.add("so")
        keywords.add("such")
    }
    if (title.contains("can and can't")) {
        keywords.add("can")
        keywords.add("can't")
    }

    return keywords
}

/**
 * Automatically tags sentences in an AI-generated reading passage with the target grammar lesson ID.
 * - Preserves any sentence already tagged with a grammar ID.
 * - For untagged sentences, inspects words against the lesson's deterministic keywords.
 * - If no sentences matched keywords, falls back to tagging the first 1-2 sentences to guarantee
 *   the target rule is linked in the story.
 */
fun autoTagGrammarSentences(
    sentences: List<ReadingSentenceItem>,
    targetGrammarLessonId: String?
): List<ReadingSentenceItem> {
    if (targetGrammarLessonId.isNullOrBlank() || sentences.isEmpty()) return sentences
    val keywords = GRAMMAR_LESSON_KEYWORDS[targetGrammarLessonId] ?: emptySet()

    var matchCount = 0
    val tagged = sentences.map { sentence ->
        if (!sentence.grammarId.isNullOrBlank()) {
            if (sentence.grammarId == targetGrammarLessonId) matchCount++
            sentence
        } else {
            val lowerEn = sentence.en.lowercase()
            val tokens = lowerEn.split(TOKEN_CLEAN_REGEX).filter { it.isNotBlank() }.toSet()
            val hasMatch = keywords.any { kw ->
                if (kw.contains(" ")) {
                    lowerEn.contains(kw)
                } else {
                    tokens.contains(kw)
                }
            }
            if (hasMatch) {
                matchCount++
                sentence.copy(grammarId = targetGrammarLessonId)
            } else {
                sentence
            }
        }
    }

    // If neither Gemini nor the keyword scanner tagged any sentence, tag the first sentence as fallback
    if (matchCount == 0 && tagged.isNotEmpty()) {
        return tagged.mapIndexed { index, sentence ->
            if (index == 0) sentence.copy(grammarId = targetGrammarLessonId) else sentence
        }
    }

    return tagged
}
