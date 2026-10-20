package com.example.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.AiBatchSavePayload
import com.example.data.local.DeckArabicStats
import com.example.data.local.EntryEntity
import com.example.data.local.WordBooksDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

enum class BatchRunPhase {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    STOPPED,
    STOPPED_ERROR,
    BLOCKED
}

enum class BatchTargetOption(val count: Int, val label: String) {
    WORDS_25(25, "25"),
    WORDS_50(50, "50"),
    WORDS_100(100, "100"),
    WORDS_500(500, "500"),
    ALL_EMPTY(Int.MAX_VALUE, "All empty")
}

data class BatchTranslateUiState(
    val selectedDeck: String = "", // "" means All Decks
    val selectedTargetCount: Int = 25,
    val phase: BatchRunPhase = BatchRunPhase.IDLE,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val batchesCompleted: Int = 0,
    val totalBatches: Int = 0,
    val estimatedSecondsLeft: Long = 0L,
    val translatedCount: Int = 0,
    val skippedCount: Int = 0,
    val failedCount: Int = 0,
    val failedEntryIds: List<String> = emptyList(),
    val consecutiveFailures: Int = 0,
    val requestsUsedToday: Int = 0,
    val dailyRequestLimit: Int = DEFAULT_DAILY_REQUEST_LIMIT,
    val allowAi: Boolean = true,
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val deckStats: List<DeckArabicStats> = emptyList(),
    val totalEmptyAcrossDecks: Int = 0
) {
    val isRunning: Boolean get() = phase == BatchRunPhase.RUNNING
    val isPaused: Boolean get() = phase == BatchRunPhase.PAUSED
    val canResume: Boolean get() = phase == BatchRunPhase.PAUSED && doneCount < totalCount
    val showSummary: Boolean
        get() = phase == BatchRunPhase.COMPLETED ||
            phase == BatchRunPhase.STOPPED ||
            phase == BatchRunPhase.STOPPED_ERROR ||
            (phase == BatchRunPhase.PAUSED && doneCount > 0)

    companion object {
        const val BATCH_SIZE = 25
        const val DEFAULT_DAILY_REQUEST_LIMIT = 40
    }
}

/**
 * Coordinates resilient batch translation of empty Arabic entries in batches of 25:
 * - Orders by Favorites first, then CEFR level (A1..C2).
 * - Deduplicates shared `(text, pos)` so one translation is saved to all matching entries.
 * - Saves every batch permanently in a single `@Transaction` (`ar_source = "ai"`).
 * - Persists in-progress run state after every batch so closing and reopening the app
 *   allows seamless continuation without losing any progress.
 * - Enforces ~5s delay between requests, exponential backoff on HTTP 429 / 5xx,
 *   automatic stop after 5 consecutive failures, daily request limit (default 40),
 *   "Allow AI" switch, and offline check.
 */
class BatchTranslateManager(
    private val appContext: Context,
    private val dao: WordBooksDao,
    private val geminiService: GeminiDeckAndTranslationService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()

    @Volatile
    var interBatchDelayMs: Long = 7_000L

    @Volatile
    var baseBackoffMs: Long = 2_000L

    @Volatile
    var onlineCheckOverride: (() -> Boolean)? = null

    @Volatile
    var startForegroundServiceHook: Boolean = true

    @Volatile
    var onBatchSavedHook: (suspend (BatchTranslateUiState) -> Unit)? = null

    private var activeJob: Job? = null

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<BatchTranslateUiState> = _uiState.asStateFlow()

    init {
        InstanceHolder.register(this)
        scope.launch {
            refreshDeckCounts()
        }
    }

    private fun loadInitialState(): BatchTranslateUiState {
        val allowAi = prefs.getBoolean(KEY_ALLOW_AI, true)
        val dailyLimit = prefs.getInt(KEY_DAILY_LIMIT, BatchTranslateUiState.DEFAULT_DAILY_REQUEST_LIMIT)
        val usedToday = getPersistedRequestsUsedToday()
        val savedDeck = prefs.getString(KEY_SELECTED_DECK, "") ?: ""
        val savedTarget = prefs.getInt(KEY_SELECTED_TARGET, 25)
        val savedPhaseName = prefs.getString(KEY_RUN_PHASE, BatchRunPhase.IDLE.name) ?: BatchRunPhase.IDLE.name
        val rawPhase = runCatching { BatchRunPhase.valueOf(savedPhaseName) }.getOrDefault(BatchRunPhase.IDLE)

        val done = prefs.getInt(KEY_RUN_DONE, 0)
        val total = prefs.getInt(KEY_RUN_TOTAL, 0)
        val batchesDone = prefs.getInt(KEY_RUN_BATCHES_DONE, 0)
        val totalBatches = prefs.getInt(KEY_RUN_TOTAL_BATCHES, 0)
        val translated = prefs.getInt(KEY_RUN_TRANSLATED, 0)
        val skipped = prefs.getInt(KEY_RUN_SKIPPED, 0)
        val failed = prefs.getInt(KEY_RUN_FAILED, 0)
        val failedIdsRaw = prefs.getString(KEY_RUN_FAILED_IDS, "") ?: ""
        val failedIds = failedIdsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val statusMsg = prefs.getString(KEY_RUN_STATUS_MSG, "") ?: ""
        val errorMsg = prefs.getString(KEY_RUN_ERROR_MSG, null)

        // If the app was closed while RUNNING and reopened, transition to PAUSED so the user/test can continue seamlessly
        val restoredPhase = if (rawPhase == BatchRunPhase.RUNNING) {
            if (done < total && total > 0) BatchRunPhase.PAUSED else BatchRunPhase.COMPLETED
        } else {
            rawPhase
        }
        val restoredMsg = if (rawPhase == BatchRunPhase.RUNNING && restoredPhase == BatchRunPhase.PAUSED) {
            "Run paused after app restart ($done / $total done). Tap Resume to continue."
        } else {
            statusMsg
        }

        return BatchTranslateUiState(
            selectedDeck = savedDeck,
            selectedTargetCount = savedTarget,
            phase = restoredPhase,
            doneCount = done,
            totalCount = total,
            batchesCompleted = batchesDone,
            totalBatches = totalBatches,
            estimatedSecondsLeft = estimateSecondsRemaining(totalBatches - batchesDone),
            translatedCount = translated,
            skippedCount = skipped,
            failedCount = failed,
            failedEntryIds = failedIds,
            requestsUsedToday = usedToday,
            dailyRequestLimit = dailyLimit,
            allowAi = allowAi,
            statusMessage = restoredMsg,
            errorMessage = errorMsg
        )
    }

    suspend fun refreshDeckCounts() {
        val stats = dao.getDeckArabicStats()
        val totalEmpty = stats.sumOf { it.emptyCount }
        val usedToday = getPersistedRequestsUsedToday()
        _uiState.value = _uiState.value.copy(
            deckStats = stats,
            totalEmptyAcrossDecks = totalEmpty,
            requestsUsedToday = usedToday
        )
    }

    fun selectDeck(deckId: String) {
        prefs.edit().putString(KEY_SELECTED_DECK, deckId).apply()
        _uiState.value = _uiState.value.copy(selectedDeck = deckId)
    }

    fun selectTargetCount(targetCount: Int) {
        prefs.edit().putInt(KEY_SELECTED_TARGET, targetCount).apply()
        _uiState.value = _uiState.value.copy(selectedTargetCount = targetCount)
    }

    fun setAllowAi(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_AI, enabled).apply()
        val current = _uiState.value
        if (!enabled && current.isRunning) {
            activeJob?.cancel()
            updateAndPersistState(
                current.copy(
                    allowAi = false,
                    phase = BatchRunPhase.PAUSED,
                    statusMessage = "Paused because 'Allow AI' switch was turned off.",
                    errorMessage = "AI translation is disabled ('Allow AI' switch is off)."
                )
            )
            stopServiceNotification()
        } else {
            _uiState.value = current.copy(
                allowAi = enabled,
                errorMessage = if (enabled && current.phase == BatchRunPhase.BLOCKED) null else current.errorMessage,
                phase = if (enabled && current.phase == BatchRunPhase.BLOCKED) BatchRunPhase.IDLE else current.phase
            )
        }
    }

    fun setDailyRequestLimit(limit: Int) {
        val coerced = limit.coerceIn(1, 5_000)
        prefs.edit().putInt(KEY_DAILY_LIMIT, coerced).apply()
        _uiState.value = _uiState.value.copy(dailyRequestLimit = coerced)
    }

    fun isDeviceOnline(): Boolean {
        onlineCheckOverride?.let { return it() }
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    fun canMakeAiRequest(): Boolean {
        return _uiState.value.allowAi && isDeviceOnline() && getPersistedRequestsUsedToday() < _uiState.value.dailyRequestLimit
    }

    fun recordAiRequest() {
        incrementRequestsUsedToday()
        scope.launch {
            refreshDeckCounts()
        }
    }

    /**
     * Starts a new Batch Translate run from the selected deck and target count.
     */
    fun startBatchTranslate(
        deckFilter: String = _uiState.value.selectedDeck,
        targetCount: Int = _uiState.value.selectedTargetCount
    ) {
        scope.launch {
            startOrResumeInternal(
                deckFilter = deckFilter,
                targetCount = targetCount,
                isResuming = false,
                retryIdsOnly = null
            )
        }
    }

    /**
     * Resumes a paused or interrupted run (including after closing and reopening the app).
     */
    fun resumeBatchTranslate() {
        scope.launch {
            val current = _uiState.value
            if (current.doneCount >= current.totalCount || current.totalCount <= 0) {
                startOrResumeInternal(
                    deckFilter = current.selectedDeck,
                    targetCount = current.selectedTargetCount,
                    isResuming = false,
                    retryIdsOnly = null
                )
            } else {
                startOrResumeInternal(
                    deckFilter = current.selectedDeck,
                    targetCount = current.totalCount,
                    isResuming = true,
                    retryIdsOnly = null
                )
            }
        }
    }

    /**
     * Retries only the entries that failed validation or failed during the last run.
     */
    fun retryFailedEntries() {
        val failedIds = _uiState.value.failedEntryIds
        if (failedIds.isEmpty()) return
        scope.launch {
            startOrResumeInternal(
                deckFilter = _uiState.value.selectedDeck,
                targetCount = failedIds.size,
                isResuming = false,
                retryIdsOnly = failedIds
            )
        }
    }

    fun pauseBatchTranslate() {
        activeJob?.cancel()
        activeJob = null
        val current = _uiState.value
        if (current.phase == BatchRunPhase.RUNNING) {
            updateAndPersistState(
                current.copy(
                    phase = BatchRunPhase.PAUSED,
                    statusMessage = "Paused at ${current.doneCount} / ${current.totalCount} words."
                )
            )
            notifyServiceProgress()
        }
    }

    fun stopBatchTranslate() {
        activeJob?.cancel()
        activeJob = null
        val current = _uiState.value
        updateAndPersistState(
            current.copy(
                phase = BatchRunPhase.STOPPED,
                estimatedSecondsLeft = 0L,
                statusMessage = "Stopped by user. ${current.translatedCount} translated, ${current.skippedCount} skipped, ${current.failedCount} failed."
            )
        )
        stopServiceNotification()
    }

    /**
     * Suspending entry point for executing or continuing a run deterministically (used by both background job and tests).
     */
    suspend fun runBatchTranslateSuspend(
        deckFilter: String = _uiState.value.selectedDeck,
        targetCount: Int = _uiState.value.selectedTargetCount,
        isResuming: Boolean = false,
        retryIdsOnly: List<String>? = null
    ) {
        mutex.withLock {
            executeBatchLoop(
                deckFilter = deckFilter,
                targetCount = targetCount,
                isResuming = isResuming,
                retryIdsOnly = retryIdsOnly
            )
        }
    }

    private suspend fun startOrResumeInternal(
        deckFilter: String,
        targetCount: Int,
        isResuming: Boolean,
        retryIdsOnly: List<String>?
    ) {
        activeJob?.cancel()
        activeJob = scope.launch {
            runBatchTranslateSuspend(
                deckFilter = deckFilter,
                targetCount = targetCount,
                isResuming = isResuming,
                retryIdsOnly = retryIdsOnly
            )
        }
    }

    private suspend fun executeBatchLoop(
        deckFilter: String,
        targetCount: Int,
        isResuming: Boolean,
        retryIdsOnly: List<String>?
    ) {
        refreshDeckCounts()
        val current = _uiState.value

        // 1. Check "Allow AI" switch
        if (!current.allowAi) {
            updateAndPersistState(
                current.copy(
                    phase = BatchRunPhase.BLOCKED,
                    statusMessage = "AI features are disabled ('Allow AI' switch is off).",
                    errorMessage = "Turn on 'Allow AI' to run Batch Translate."
                )
            )
            return
        }

        // 2. Check offline status
        if (!isDeviceOnline()) {
            updateAndPersistState(
                current.copy(
                    phase = BatchRunPhase.BLOCKED,
                    statusMessage = "Device is offline. Batch Translate requires an internet connection.",
                    errorMessage = "You are currently offline. Connect to the internet and try again."
                )
            )
            return
        }

        val engineMode = GeminiApiKeyManager.getEngineMode(appContext)
        val hasTestTransport = geminiService.batchTransportOverride != null
        val useFreeEngineDirectly = !hasTestTransport && (
            engineMode == TranslationEngineMode.FREE_API_ONLY ||
                (engineMode == TranslationEngineMode.SMART_AUTO && !geminiService.isApiKeyConfigured())
            )

        // 3. Check API key (only when AI_KEYS_ONLY is active and no test transport is injected)
        if (!useFreeEngineDirectly && !geminiService.isApiKeyConfigured() && !hasTestTransport) {
            updateAndPersistState(
                current.copy(
                    phase = BatchRunPhase.BLOCKED,
                    statusMessage = "AI API key is not configured.",
                    errorMessage = "Add a Gemini/Groq/OpenRouter key or switch Engine Mode to Smart Auto / Free API."
                )
            )
            return
        }

        // 4. Check daily request limit (only blocks if AI_KEYS_ONLY or test transport, otherwise Smart Auto uses Free API)
        val usedTodayStart = getPersistedRequestsUsedToday()
        var switchToFreeForQuota = false
        if (usedTodayStart >= current.dailyRequestLimit) {
            if (hasTestTransport || engineMode == TranslationEngineMode.AI_KEYS_ONLY) {
                updateAndPersistState(
                    current.copy(
                        phase = BatchRunPhase.BLOCKED,
                        requestsUsedToday = usedTodayStart,
                        statusMessage = "Daily AI request limit reached ($usedTodayStart / ${current.dailyRequestLimit}).",
                        errorMessage = "Daily request limit (${current.dailyRequestLimit}) reached for today."
                    )
                )
                return
            } else {
                switchToFreeForQuota = true
            }
        }

        val remainingNeeded = if (isResuming) {
            max(0, current.totalCount - current.doneCount)
        } else {
            if (targetCount <= 0 || targetCount == Int.MAX_VALUE) 20_000 else targetCount
        }

        val candidateEntries: List<EntryEntity> = if (retryIdsOnly != null) {
            dao.getUntranslatedEntriesByIds(retryIdsOnly)
        } else {
            dao.getUntranslatedEntriesPrioritized(deckFilter, remainingNeeded)
        }

        if (candidateEntries.isEmpty()) {
            val doneNow = if (isResuming) current.totalCount else 0
            updateAndPersistState(
                current.copy(
                    selectedDeck = deckFilter,
                    selectedTargetCount = targetCount,
                    phase = BatchRunPhase.COMPLETED,
                    doneCount = doneNow,
                    totalCount = doneNow,
                    estimatedSecondsLeft = 0L,
                    statusMessage = "No empty words remaining to translate in ${if (deckFilter.isBlank()) "All Decks" else deckFilter}.",
                    errorMessage = null
                )
            )
            stopServiceNotification()
            return
        }

        // Step A: Reuse existing translations in DB for entries with the same (text, pos) without spending API requests
        val toReusePayloads = ArrayList<AiBatchSavePayload>()
        val remainingAfterDbReuse = ArrayList<EntryEntity>(candidateEntries.size)
        for (entry in candidateEntries) {
            val existingMatch = dao.findTranslatedEntryByTextAndPos(entry.text, entry.pos)
            if (existingMatch != null && existingMatch.arabic.isNotBlank()) {
                toReusePayloads.add(
                    AiBatchSavePayload(
                        id = entry.id,
                        text = entry.text,
                        pos = entry.pos,
                        arabic = existingMatch.arabic,
                        ipa = existingMatch.ipa,
                        examplesEn = existingMatch.examplesEn,
                        examplesAr = existingMatch.examplesAr
                    )
                )
            } else {
                remainingAfterDbReuse.add(entry)
            }
        }

        var reusedCount = 0
        if (toReusePayloads.isNotEmpty()) {
            dao.saveAiTranslatedBatchTransaction(toReusePayloads)
            reusedCount = toReusePayloads.size
        }

        // Group remaining entries by shared (LOWER(TRIM(text)), LOWER(TRIM(pos))) so we translate once and save to all
        val groupedByTextAndPos = LinkedHashMap<String, MutableList<EntryEntity>>()
        for (entry in remainingAfterDbReuse) {
            val key = "${entry.text.trim().lowercase()}::${entry.pos.trim().lowercase()}"
            groupedByTextAndPos.getOrPut(key) { ArrayList() }.add(entry)
        }

        val representatives = groupedByTextAndPos.values.map { it.first() }
        val batches = representatives.chunked(BatchTranslateUiState.BATCH_SIZE)

        val initialDone = if (isResuming) current.doneCount + reusedCount else reusedCount
        val plannedTotal = if (isResuming) current.totalCount else candidateEntries.size
        val initialBatchesDone = if (isResuming) current.batchesCompleted else 0
        val plannedTotalBatches = if (isResuming) {
            max(current.totalBatches, initialBatchesDone + batches.size)
        } else {
            batches.size
        }
        val initialTranslated = if (isResuming) current.translatedCount else 0
        val initialSkipped = (if (isResuming) current.skippedCount else 0) + reusedCount
        val initialFailed = if (isResuming) current.failedCount else 0
        val accumulatedFailedIds = LinkedHashSet<String>(
            if (isResuming && retryIdsOnly == null) current.failedEntryIds else emptyList()
        )

        updateAndPersistState(
            _uiState.value.copy(
                selectedDeck = deckFilter,
                selectedTargetCount = targetCount,
                phase = BatchRunPhase.RUNNING,
                doneCount = min(initialDone, plannedTotal),
                totalCount = plannedTotal,
                batchesCompleted = initialBatchesDone,
                totalBatches = plannedTotalBatches,
                estimatedSecondsLeft = estimateSecondsRemaining(plannedTotalBatches - initialBatchesDone),
                translatedCount = initialTranslated,
                skippedCount = initialSkipped,
                failedCount = initialFailed,
                failedEntryIds = accumulatedFailedIds.toList(),
                consecutiveFailures = 0,
                requestsUsedToday = getPersistedRequestsUsedToday(),
                statusMessage = if (batches.isEmpty()) {
                    "Completed via shared translations!"
                } else if (useFreeEngineDirectly || switchToFreeForQuota) {
                    "Translating ${plannedTotal - initialDone} words via Free Dictionary & Translation API (${batches.size} batches)..."
                } else {
                    "Translating ${plannedTotal - initialDone} words in ${batches.size} batch(es) of up to 25..."
                },
                errorMessage = null
            )
        )

        if (batches.isEmpty()) {
            refreshDeckCounts()
            updateAndPersistState(
                _uiState.value.copy(
                    phase = BatchRunPhase.COMPLETED,
                    doneCount = plannedTotal,
                    estimatedSecondsLeft = 0L,
                    statusMessage = "Batch Translate finished: ${_uiState.value.translatedCount} translated, ${_uiState.value.skippedCount} skipped."
                )
            )
            stopServiceNotification()
            return
        }

        if (startForegroundServiceHook) {
            BatchTranslateForegroundService.startOrUpdate(appContext)
        }

        var consecutiveFailures = 0

        for ((batchIndex, batchRepresentatives) in batches.withIndex()) {
            val latest = _uiState.value
            if (!latest.allowAi) {
                updateAndPersistState(
                    latest.copy(
                        phase = BatchRunPhase.PAUSED,
                        statusMessage = "Stopped: 'Allow AI' switch was disabled.",
                        errorMessage = "AI translation is turned off."
                    )
                )
                stopServiceNotification()
                return
            }

            if (!isDeviceOnline()) {
                updateAndPersistState(
                    latest.copy(
                        phase = BatchRunPhase.PAUSED,
                        statusMessage = "Paused: Device went offline.",
                        errorMessage = "Internet connection lost. Reconnect and tap Resume."
                    )
                )
                stopServiceNotification()
                return
            }

            val currentEngineMode = GeminiApiKeyManager.getEngineMode(appContext)
            val useFreeForBatch = !hasTestTransport && (
                useFreeEngineDirectly ||
                    switchToFreeForQuota ||
                    currentEngineMode == TranslationEngineMode.FREE_API_ONLY
                )

            val usedNow = getPersistedRequestsUsedToday()
            if (!useFreeForBatch && usedNow >= latest.dailyRequestLimit) {
                if (hasTestTransport || currentEngineMode == TranslationEngineMode.AI_KEYS_ONLY) {
                    updateAndPersistState(
                        latest.copy(
                            phase = BatchRunPhase.STOPPED_ERROR,
                            requestsUsedToday = usedNow,
                            estimatedSecondsLeft = 0L,
                            statusMessage = "Stopped: Daily request limit reached ($usedNow / ${latest.dailyRequestLimit}).",
                            errorMessage = "Daily request limit (${latest.dailyRequestLimit}) reached for today."
                        )
                    )
                    stopServiceNotification()
                    return
                } else {
                    switchToFreeForQuota = true
                }
            }

            // Delay between batches: fast 1.2s when using Free API, or configured interBatchDelayMs for AI
            if (batchIndex > 0 && interBatchDelayMs > 0L) {
                val effectiveDelay = if (useFreeForBatch || switchToFreeForQuota) min(1_200L, interBatchDelayMs) else interBatchDelayMs
                delay(effectiveDelay)
            }

            // Total entries covered by this batch (including duplicates sharing (text, pos))
            val batchCoveredEntries = batchRepresentatives.flatMap { rep ->
                val key = "${rep.text.trim().lowercase()}::${rep.pos.trim().lowercase()}"
                groupedByTextAndPos[key] ?: listOf(rep)
            }

            var batchSucceeded = false
            var rawResponse = ""
            var fallbackPayloads: List<AiBatchSavePayload>? = null

            while (!batchSucceeded) {
                if (!isDeviceOnline()) {
                    updateAndPersistState(
                        _uiState.value.copy(
                            phase = BatchRunPhase.PAUSED,
                            statusMessage = "Paused: Device is offline.",
                            errorMessage = "Device went offline during batch translation."
                        )
                    )
                    stopServiceNotification()
                    return
                }

                // Direct Free API path (when Free API mode is selected or AI quota reached in Smart Auto)
                if (useFreeForBatch || switchToFreeForQuota) {
                    val fb = geminiService.translateWithMyMemoryFallback(batchRepresentatives)
                    if (fb.isNotEmpty()) {
                        fallbackPayloads = fb
                        batchSucceeded = true
                        consecutiveFailures = 0
                        break
                    }
                }

                val currentUsed = getPersistedRequestsUsedToday()
                if (!useFreeForBatch && !switchToFreeForQuota && currentUsed >= _uiState.value.dailyRequestLimit) {
                    if (hasTestTransport || currentEngineMode == TranslationEngineMode.AI_KEYS_ONLY) {
                        updateAndPersistState(
                            _uiState.value.copy(
                                phase = BatchRunPhase.STOPPED_ERROR,
                                requestsUsedToday = currentUsed,
                                estimatedSecondsLeft = 0L,
                                statusMessage = "Daily request limit reached ($currentUsed / ${_uiState.value.dailyRequestLimit}).",
                                errorMessage = "Daily request limit (${_uiState.value.dailyRequestLimit}) reached."
                            )
                        )
                        stopServiceNotification()
                        return
                    } else {
                        switchToFreeForQuota = true
                        continue
                    }
                }

                try {
                    val updatedUsed = incrementRequestsUsedToday()
                    _uiState.value = _uiState.value.copy(requestsUsedToday = updatedUsed)

                    rawResponse = geminiService.requestBatchTranslationJson(batchRepresentatives)
                    batchSucceeded = true
                    consecutiveFailures = 0
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: GeminiHttpException) {
                    consecutiveFailures++

                    // In Smart Auto mode (when no test transport override is active), immediately fall back to Free API on first 429!
                    val allowImmediateFreeFallback = !hasTestTransport && currentEngineMode == TranslationEngineMode.SMART_AUTO
                    if ((e.statusCode == 429 && allowImmediateFreeFallback) || (e.statusCode == 429 && consecutiveFailures >= 2)) {
                        updateAndPersistState(
                            _uiState.value.copy(
                                statusMessage = "AI rate limit (429) reached. Switching to Free Dictionary & Translation API..."
                            )
                        )
                        val fb = geminiService.translateWithMyMemoryFallback(batchRepresentatives)
                        if (fb.isNotEmpty()) {
                            fallbackPayloads = fb
                            batchSucceeded = true
                            consecutiveFailures = 0
                            if (allowImmediateFreeFallback) {
                                switchToFreeForQuota = true
                            }
                            break
                        }
                    }

                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        val failedIdsNow = accumulatedFailedIds.apply {
                            addAll(batchCoveredEntries.map { it.id })
                        }.toList()
                        updateAndPersistState(
                            _uiState.value.copy(
                                phase = BatchRunPhase.STOPPED_ERROR,
                                consecutiveFailures = consecutiveFailures,
                                failedCount = _uiState.value.failedCount + batchCoveredEntries.size,
                                failedEntryIds = failedIdsNow,
                                estimatedSecondsLeft = 0L,
                                statusMessage = "Stopped automatically after $MAX_CONSECUTIVE_FAILURES consecutive failures.",
                                errorMessage = "Stopped after $MAX_CONSECUTIVE_FAILURES failures in a row (last error: HTTP ${e.statusCode})."
                            )
                        )
                        stopServiceNotification()
                        return
                    }

                    if (e.isRetryableRateLimitOrServer) {
                        val backoffMs = if (e.statusCode == 429) {
                            val base429Ms = 8_000L
                            (base429Ms * consecutiveFailures.coerceAtMost(3)) + (500L..1500L).random()
                        } else {
                            baseBackoffMs * (1L shl (consecutiveFailures - 1).coerceAtMost(5))
                        }
                        updateAndPersistState(
                            _uiState.value.copy(
                                consecutiveFailures = consecutiveFailures,
                                statusMessage = if (e.statusCode == 429) {
                                    "Rate limit (429). Backing off for ${(backoffMs / 1000).toInt()}s (retry $consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)..."
                                } else {
                                    "HTTP ${e.statusCode} received. Backing off for ${(backoffMs / 1000.0).toInt()}s (retry $consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)..."
                                }
                            )
                        )
                        if (backoffMs > 0L) {
                            delay(backoffMs)
                        }
                    } else {
                        val backoffMs = baseBackoffMs * (1L shl (consecutiveFailures - 1).coerceAtMost(4))
                        if (backoffMs > 0L) {
                            delay(backoffMs)
                        }
                    }
                } catch (e: Exception) {
                    consecutiveFailures++
                    if (!hasTestTransport && currentEngineMode == TranslationEngineMode.SMART_AUTO) {
                        val fb = geminiService.translateWithMyMemoryFallback(batchRepresentatives)
                        if (fb.isNotEmpty()) {
                            fallbackPayloads = fb
                            batchSucceeded = true
                            consecutiveFailures = 0
                            break
                        }
                    }
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        val failedIdsNow = accumulatedFailedIds.apply {
                            addAll(batchCoveredEntries.map { it.id })
                        }.toList()
                        updateAndPersistState(
                            _uiState.value.copy(
                                phase = BatchRunPhase.STOPPED_ERROR,
                                consecutiveFailures = consecutiveFailures,
                                failedCount = _uiState.value.failedCount + batchCoveredEntries.size,
                                failedEntryIds = failedIdsNow,
                                estimatedSecondsLeft = 0L,
                                statusMessage = "Stopped automatically after $MAX_CONSECUTIVE_FAILURES consecutive failures.",
                                errorMessage = "Stopped after $MAX_CONSECUTIVE_FAILURES failures in a row: ${e.message}"
                            )
                        )
                        stopServiceNotification()
                        return
                    }
                    val backoffMs = baseBackoffMs * (1L shl (consecutiveFailures - 1).coerceAtMost(4))
                    if (backoffMs > 0L) {
                        delay(backoffMs)
                    }
                }
            }

            // Validate every item in the batch response and discard invalid items without saving
            val validation = if (fallbackPayloads != null) {
                BatchValidationResult(
                    validPayloads = fallbackPayloads,
                    invalidEntryIds = emptyList()
                )
            } else {
                geminiService.parseAndValidateBatchResponse(
                    requestedEntries = batchRepresentatives,
                    rawResponse = rawResponse
                )
            }

            // Save valid items permanently in ONE database transaction
            if (validation.validPayloads.isNotEmpty()) {
                dao.saveAiTranslatedBatchTransaction(validation.validPayloads)
            }

            val validRepIds = validation.validPayloads.map { it.id }.toSet()
            var batchTranslatedEntriesCount = 0
            var batchSharedSkippedCount = 0
            var batchFailedEntriesCount = 0

            for (rep in batchRepresentatives) {
                val key = "${rep.text.trim().lowercase()}::${rep.pos.trim().lowercase()}"
                val group = groupedByTextAndPos[key] ?: listOf(rep)
                if (validRepIds.contains(rep.id)) {
                    batchTranslatedEntriesCount += group.size
                    if (group.size > 1) {
                        batchSharedSkippedCount += (group.size - 1)
                    }
                } else {
                    batchFailedEntriesCount += group.size
                    group.forEach { accumulatedFailedIds.add(it.id) }
                }
            }

            val prev = _uiState.value
            val newDone = min(prev.totalCount, prev.doneCount + batchCoveredEntries.size)
            val newBatchesDone = prev.batchesCompleted + 1
            val remainingBatches = max(0, prev.totalBatches - newBatchesDone)
            val newState = prev.copy(
                doneCount = newDone,
                batchesCompleted = newBatchesDone,
                estimatedSecondsLeft = estimateSecondsRemaining(remainingBatches),
                translatedCount = prev.translatedCount + batchTranslatedEntriesCount,
                skippedCount = prev.skippedCount + batchSharedSkippedCount,
                failedCount = prev.failedCount + batchFailedEntriesCount,
                failedEntryIds = accumulatedFailedIds.toList(),
                consecutiveFailures = consecutiveFailures,
                statusMessage = "Saved batch $newBatchesDone / ${prev.totalBatches} ($newDone / ${prev.totalCount} words)."
            )
            updateAndPersistState(newState)
            notifyServiceProgress()
            onBatchSavedHook?.invoke(newState)
        }

        refreshDeckCounts()
        val finalState = _uiState.value
        val completedState = finalState.copy(
            phase = BatchRunPhase.COMPLETED,
            doneCount = finalState.totalCount,
            estimatedSecondsLeft = 0L,
            statusMessage = "Batch translation complete: ${finalState.translatedCount} translated, ${finalState.skippedCount} skipped, ${finalState.failedCount} failed."
        )
        updateAndPersistState(completedState)
        stopServiceNotification()
    }

    private fun estimateSecondsRemaining(remainingBatches: Int): Long {
        if (remainingBatches <= 0) return 0L
        val perBatchSec = max(1L, (interBatchDelayMs / 1000L) + 3L)
        return remainingBatches * perBatchSec
    }

    private fun getPersistedRequestsUsedToday(): Int {
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_REQUESTS_DATE, "") ?: ""
        return if (savedDate == today) {
            prefs.getInt(KEY_REQUESTS_USED_TODAY, 0)
        } else {
            prefs.edit()
                .putString(KEY_REQUESTS_DATE, today)
                .putInt(KEY_REQUESTS_USED_TODAY, 0)
                .apply()
            0
        }
    }

    private fun incrementRequestsUsedToday(): Int {
        val today = LocalDate.now().toString()
        val current = getPersistedRequestsUsedToday()
        val updated = current + 1
        prefs.edit()
            .putString(KEY_REQUESTS_DATE, today)
            .putInt(KEY_REQUESTS_USED_TODAY, updated)
            .commit()
        return updated
    }

    private fun updateAndPersistState(newState: BatchTranslateUiState) {
        _uiState.value = newState
        prefs.edit()
            .putString(KEY_SELECTED_DECK, newState.selectedDeck)
            .putInt(KEY_SELECTED_TARGET, newState.selectedTargetCount)
            .putString(KEY_RUN_PHASE, newState.phase.name)
            .putInt(KEY_RUN_DONE, newState.doneCount)
            .putInt(KEY_RUN_TOTAL, newState.totalCount)
            .putInt(KEY_RUN_BATCHES_DONE, newState.batchesCompleted)
            .putInt(KEY_RUN_TOTAL_BATCHES, newState.totalBatches)
            .putInt(KEY_RUN_TRANSLATED, newState.translatedCount)
            .putInt(KEY_RUN_SKIPPED, newState.skippedCount)
            .putInt(KEY_RUN_FAILED, newState.failedCount)
            .putString(KEY_RUN_FAILED_IDS, newState.failedEntryIds.joinToString(","))
            .putString(KEY_RUN_STATUS_MSG, newState.statusMessage)
            .putString(KEY_RUN_ERROR_MSG, newState.errorMessage)
            .commit()
    }

    private fun notifyServiceProgress() {
        if (startForegroundServiceHook) {
            BatchTranslateForegroundService.startOrUpdate(appContext)
        }
    }

    private fun stopServiceNotification() {
        if (startForegroundServiceHook) {
            BatchTranslateForegroundService.stop(appContext)
        }
    }

    object InstanceHolder {
        @Volatile
        var current: BatchTranslateManager? = null
            private set

        fun register(manager: BatchTranslateManager) {
            current = manager
        }
    }

    companion object {
        const val PREFS_NAME = "wordbooks_batch_translate_prefs"
        const val MAX_CONSECUTIVE_FAILURES = 5

        private const val KEY_ALLOW_AI = "allow_ai_enabled"
        private const val KEY_DAILY_LIMIT = "daily_request_limit"
        private const val KEY_REQUESTS_DATE = "requests_used_date"
        private const val KEY_REQUESTS_USED_TODAY = "requests_used_today"
        private const val KEY_SELECTED_DECK = "selected_deck"
        private const val KEY_SELECTED_TARGET = "selected_target"
        private const val KEY_RUN_PHASE = "run_phase"
        private const val KEY_RUN_DONE = "run_done"
        private const val KEY_RUN_TOTAL = "run_total"
        private const val KEY_RUN_BATCHES_DONE = "run_batches_done"
        private const val KEY_RUN_TOTAL_BATCHES = "run_total_batches"
        private const val KEY_RUN_TRANSLATED = "run_translated"
        private const val KEY_RUN_SKIPPED = "run_skipped"
        private const val KEY_RUN_FAILED = "run_failed"
        private const val KEY_RUN_FAILED_IDS = "run_failed_ids"
        private const val KEY_RUN_STATUS_MSG = "run_status_msg"
        private const val KEY_RUN_ERROR_MSG = "run_error_msg"
    }
}
