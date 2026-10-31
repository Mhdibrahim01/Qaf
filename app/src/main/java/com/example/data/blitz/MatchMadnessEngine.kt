package com.example.data.blitz

import androidx.compose.runtime.Immutable
import com.example.data.local.EntryEntity
import kotlin.math.roundToInt
import kotlin.random.Random

@Immutable
data class MatchCardItem(
    val id: String,
    val pairId: String,
    val text: String,
    val isEnglish: Boolean,
    val isMatched: Boolean = false,
    val isSelected: Boolean = false,
    val isError: Boolean = false
)

@Immutable
data class MatchPairItem(
    val id: String,
    val englishWord: String,
    val arabicMeaning: String,
    val pos: String = "",
    val level: String = "",
    val audioUrl: String = ""
)

@Immutable
data class MatchRoundState(
    val roundNumber: Int,
    val totalRounds: Int = 3,
    val leftCards: List<MatchCardItem>,
    val rightCards: List<MatchCardItem>,
    val matchedPairIds: Set<String> = emptySet(),
    val selectedLeftCardId: String? = null,
    val selectedRightCardId: String? = null
) {
    val isRoundComplete: Boolean
        get() = matchedPairIds.size >= leftCards.size && leftCards.isNotEmpty()
}

@Immutable
data class MatchMadnessSessionState(
    val rounds: List<MatchRoundState> = emptyList(),
    val allPairs: List<MatchPairItem> = emptyList(),
    val currentRoundIndex: Int = 0,
    val elapsedSeconds: Int = 0,
    val isTimerRunning: Boolean = true,
    val isFinished: Boolean = false,
    val mismatches: Int = 0,
    val isNewBestTime: Boolean = false
) {
    val currentRound: MatchRoundState?
        get() = rounds.getOrNull(currentRoundIndex)

    val totalPairsCount: Int
        get() = allPairs.size

    val matchedCountTotal: Int
        get() = rounds.sumOf { it.matchedPairIds.size }

    val accuracyPercent: Int
        get() {
            val totalAttempts = totalPairsCount + mismatches
            return if (totalAttempts > 0) {
                ((totalPairsCount.toDouble() / totalAttempts.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            } else 100
        }
}

sealed class MatchSelectionOutcome {
    data class CardSelected(val state: MatchMadnessSessionState) : MatchSelectionOutcome()
    data class MatchSuccess(
        val state: MatchMadnessSessionState,
        val pairId: String,
        val roundJustCompleted: Boolean,
        val gameJustCompleted: Boolean
    ) : MatchSelectionOutcome()
    data class MatchMismatch(
        val state: MatchMadnessSessionState,
        val leftCardId: String,
        val rightCardId: String
    ) : MatchSelectionOutcome()
}

object MatchMadnessEngine {
    const val PAIRS_PER_ROUND = 6
    const val TOTAL_ROUNDS = 3
    const val TOTAL_PAIRS = PAIRS_PER_ROUND * TOTAL_ROUNDS // 18 words

    private fun cleanArabic(raw: String): String {
        val singleLine = raw.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        return singleLine
            .split(Regex("[؛،,/]"))
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?: singleLine.trim()
    }

    fun createSession(
        entries: List<EntryEntity>,
        seed: Int = System.currentTimeMillis().toInt()
    ): MatchMadnessSessionState {
        val rng = Random(seed)
        val validPool = entries
            .filter { it.text.isNotBlank() && it.arabic.isNotBlank() }
            .distinctBy { it.text.trim().lowercase() }
            .ifEmpty { fallbackEntries() }

        val shuffled = validPool.shuffled(rng)
        val needed = TOTAL_PAIRS
        val selectedEntries = if (shuffled.size >= needed) {
            shuffled.take(needed)
        } else {
            // Repeat entries if pool is small
            val list = mutableListOf<EntryEntity>()
            while (list.size < needed) {
                list.addAll(shuffled.shuffled(rng))
            }
            list.take(needed)
        }

        val allPairs = selectedEntries.mapIndexed { idx, e ->
            MatchPairItem(
                id = "${e.id}_pair_$idx",
                englishWord = e.text.trim().replace("\r\n", " ").replace("\n", " "),
                arabicMeaning = cleanArabic(e.arabic),
                pos = e.pos.trim(),
                level = (e.effectiveLevel ?: e.level).trim().ifBlank { "B1" },
                audioUrl = e.audioUrl
            )
        }

        val rounds = (0 until TOTAL_ROUNDS).map { roundIdx ->
            val roundPairs = allPairs.subList(roundIdx * PAIRS_PER_ROUND, (roundIdx + 1) * PAIRS_PER_ROUND)
            val leftCards = roundPairs.map { pair ->
                MatchCardItem(
                    id = "${pair.id}_en",
                    pairId = pair.id,
                    text = pair.englishWord,
                    isEnglish = true
                )
            }.shuffled(rng)

            val rightCards = roundPairs.map { pair ->
                MatchCardItem(
                    id = "${pair.id}_ar",
                    pairId = pair.id,
                    text = pair.arabicMeaning,
                    isEnglish = false
                )
            }.shuffled(rng)

            MatchRoundState(
                roundNumber = roundIdx + 1,
                totalRounds = TOTAL_ROUNDS,
                leftCards = leftCards,
                rightCards = rightCards
            )
        }

        return MatchMadnessSessionState(
            rounds = rounds,
            allPairs = allPairs,
            currentRoundIndex = 0,
            elapsedSeconds = 0,
            isTimerRunning = true,
            isFinished = false,
            mismatches = 0
        )
    }

    /**
     * Handles tapping a card (either left English or right Arabic).
     */
    fun selectCard(
        state: MatchMadnessSessionState,
        cardId: String,
        previousBestTime: Int = 0
    ): MatchSelectionOutcome {
        if (state.isFinished) return MatchSelectionOutcome.CardSelected(state)
        val currentRound = state.currentRound ?: return MatchSelectionOutcome.CardSelected(state)

        // Find the card clicked
        val leftCard = currentRound.leftCards.find { it.id == cardId }
        val rightCard = currentRound.rightCards.find { it.id == cardId }

        if (leftCard != null) {
            if (leftCard.isMatched) return MatchSelectionOutcome.CardSelected(state)
            // If already selected, deselect
            if (currentRound.selectedLeftCardId == leftCard.id) {
                val updatedRound = currentRound.copy(
                    selectedLeftCardId = null,
                    leftCards = currentRound.leftCards.map { if (it.id == leftCard.id) it.copy(isSelected = false) else it }
                )
                return MatchSelectionOutcome.CardSelected(updateRound(state, updatedRound))
            }

            // Check if right card was already selected
            val otherRight = currentRound.rightCards.find { it.id == currentRound.selectedRightCardId }
            if (otherRight != null) {
                // We have both! Check match
                return evaluatePair(state, currentRound, leftCard, otherRight, previousBestTime)
            } else {
                // Select this left card
                val updatedRound = currentRound.copy(
                    selectedLeftCardId = leftCard.id,
                    leftCards = currentRound.leftCards.map { it.copy(isSelected = it.id == leftCard.id, isError = false) }
                )
                return MatchSelectionOutcome.CardSelected(updateRound(state, updatedRound))
            }
        } else if (rightCard != null) {
            if (rightCard.isMatched) return MatchSelectionOutcome.CardSelected(state)
            // If already selected, deselect
            if (currentRound.selectedRightCardId == rightCard.id) {
                val updatedRound = currentRound.copy(
                    selectedRightCardId = null,
                    rightCards = currentRound.rightCards.map { if (it.id == rightCard.id) it.copy(isSelected = false) else it }
                )
                return MatchSelectionOutcome.CardSelected(updateRound(state, updatedRound))
            }

            // Check if left card was already selected
            val otherLeft = currentRound.leftCards.find { it.id == currentRound.selectedLeftCardId }
            if (otherLeft != null) {
                // We have both! Check match
                return evaluatePair(state, currentRound, otherLeft, rightCard, previousBestTime)
            } else {
                // Select this right card
                val updatedRound = currentRound.copy(
                    selectedRightCardId = rightCard.id,
                    rightCards = currentRound.rightCards.map { it.copy(isSelected = it.id == rightCard.id, isError = false) }
                )
                return MatchSelectionOutcome.CardSelected(updateRound(state, updatedRound))
            }
        }

        return MatchSelectionOutcome.CardSelected(state)
    }

    private fun evaluatePair(
        state: MatchMadnessSessionState,
        currentRound: MatchRoundState,
        leftCard: MatchCardItem,
        rightCard: MatchCardItem,
        previousBestTime: Int
    ): MatchSelectionOutcome {
        val isMatch = leftCard.pairId == rightCard.pairId

        if (isMatch) {
            val newMatchedPairs = currentRound.matchedPairIds + leftCard.pairId
            val updatedLeft = currentRound.leftCards.map {
                if (it.id == leftCard.id) it.copy(isMatched = true, isSelected = false, isError = false) else it
            }
            val updatedRight = currentRound.rightCards.map {
                if (it.id == rightCard.id) it.copy(isMatched = true, isSelected = false, isError = false) else it
            }

            val updatedRound = currentRound.copy(
                leftCards = updatedLeft,
                rightCards = updatedRight,
                matchedPairIds = newMatchedPairs,
                selectedLeftCardId = null,
                selectedRightCardId = null
            )

            val roundJustCompleted = updatedRound.isRoundComplete
            val isFinalRound = state.currentRoundIndex >= state.rounds.size - 1
            val gameJustCompleted = roundJustCompleted && isFinalRound

            val isNewRecord = gameJustCompleted && (previousBestTime <= 0 || state.elapsedSeconds < previousBestTime)

            val nextState = updateRound(state, updatedRound).copy(
                isFinished = gameJustCompleted,
                isTimerRunning = !gameJustCompleted,
                isNewBestTime = isNewRecord
            )

            return MatchSelectionOutcome.MatchSuccess(
                state = nextState,
                pairId = leftCard.pairId,
                roundJustCompleted = roundJustCompleted,
                gameJustCompleted = gameJustCompleted
            )
        } else {
            // Mismatch
            val updatedLeft = currentRound.leftCards.map {
                if (it.id == leftCard.id) it.copy(isSelected = true, isError = true) else it
            }
            val updatedRight = currentRound.rightCards.map {
                if (it.id == rightCard.id) it.copy(isSelected = true, isError = true) else it
            }

            val updatedRound = currentRound.copy(
                leftCards = updatedLeft,
                rightCards = updatedRight,
                selectedLeftCardId = leftCard.id,
                selectedRightCardId = rightCard.id
            )

            val nextState = updateRound(state, updatedRound).copy(
                mismatches = state.mismatches + 1
            )

            return MatchSelectionOutcome.MatchMismatch(
                state = nextState,
                leftCardId = leftCard.id,
                rightCardId = rightCard.id
            )
        }
    }

    fun clearSelectionsAndErrors(state: MatchMadnessSessionState): MatchMadnessSessionState {
        val currentRound = state.currentRound ?: return state
        val updatedLeft = currentRound.leftCards.map { it.copy(isSelected = false, isError = false) }
        val updatedRight = currentRound.rightCards.map { it.copy(isSelected = false, isError = false) }
        val updatedRound = currentRound.copy(
            leftCards = updatedLeft,
            rightCards = updatedRight,
            selectedLeftCardId = null,
            selectedRightCardId = null
        )
        return updateRound(state, updatedRound)
    }

    fun advanceRound(state: MatchMadnessSessionState): MatchMadnessSessionState {
        if (state.currentRoundIndex + 1 < state.rounds.size) {
            return state.copy(currentRoundIndex = state.currentRoundIndex + 1)
        }
        return state.copy(isFinished = true, isTimerRunning = false)
    }

    fun tickTimer(state: MatchMadnessSessionState): MatchMadnessSessionState {
        if (!state.isTimerRunning || state.isFinished) return state
        return state.copy(elapsedSeconds = state.elapsedSeconds + 1)
    }

    private fun updateRound(
        state: MatchMadnessSessionState,
        newRound: MatchRoundState
    ): MatchMadnessSessionState {
        val newRounds = state.rounds.toMutableList()
        if (state.currentRoundIndex in newRounds.indices) {
            newRounds[state.currentRoundIndex] = newRound
        }
        return state.copy(rounds = newRounds)
    }

    private fun fallbackEntries(): List<EntryEntity> = listOf(
        EntryEntity(id = "fb_m1", type = "word", deck = "oxford5000", text = "abandon", pos = "v.", level = "B2", arabic = "يتخلى عن", definitionEn = "give up", noteEn = "", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_m2", type = "word", deck = "oxford5000", text = "capable", pos = "adj.", level = "B2", arabic = "قادر / كفء", definitionEn = "able to do", noteEn = "", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_m3", type = "word", deck = "oxford5000", text = "dedicate", pos = "v.", level = "B2", arabic = "يكرس / يخصص", definitionEn = "devote", noteEn = "", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_m4", type = "word", deck = "oxford5000", text = "efficient", pos = "adj.", level = "B2", arabic = "فعال / ذو كفاءة", definitionEn = "productive", noteEn = "", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_m5", type = "word", deck = "oxford5000", text = "generate", pos = "v.", level = "B2", arabic = "يولّد / يُنتج", definitionEn = "produce", noteEn = "", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = ""),
        EntryEntity(id = "fb_m6", type = "word", deck = "oxford5000", text = "hesitate", pos = "v.", level = "B2", arabic = "يتردد", definitionEn = "pause before saying or doing", noteEn = "", examplesEn = "", examplesAr = "", definitionUrl = "", audioUrl = "")
    )
}
