package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MarketCategory
import com.example.data.local.MarketEntity
import com.example.data.local.MatkaDatabase
import com.example.data.local.RecordSource
import com.example.data.local.ResultRecordEntity
import com.example.data.parser.DpBossTextParser
import com.example.data.parser.ParsePreviewReport
import com.example.data.remote.LiveSyncResult
import com.example.data.repository.MatkaRepository
import com.example.domain.analytics.MarketAnalyticsSnapshot
import com.example.domain.analytics.MatkaAnalyticsEngine
import com.example.domain.analytics.OpenToClosePredictionResult
import com.example.domain.math.MatkaMathEngine
import com.example.domain.math.PanaValidationResult
import com.example.domain.schedule.MarketScheduleConfig
import com.example.domain.schedule.MarketScheduleEngine
import com.example.domain.schedule.MarketScheduleEvaluation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab(val route: String, val label: String) {
    HOME("home", "Home"),
    PREDICTIONS("predictions", "Predictions"),
    ANALYTICS("analytics", "Analytics"),
    MARKETS("markets", "Markets"),
    DATA("data", "Data")
}

sealed interface SubScreen {
    data object None : SubScreen
    data class History(val initialMarket: String?) : SubScreen
    data object PanaChart : SubScreen
}

data class MarketCardSummary(
    val market: MarketEntity,
    val latestRecord: ResultRecordEntity?,
    val totalRecords: Int,
    val verifiedCount: Int,
    val category: MarketCategory,
    val isFavorite: Boolean,
    val schedule: MarketScheduleConfig,
    val scheduleEvaluation: MarketScheduleEvaluation
) {
    val verificationPercent: Int
        get() = if (totalRecords == 0) 0 else ((verifiedCount * 100) / totalRecords)
}

data class DashboardStats(
    val totalMarkets: Int = 0,
    val totalRecords: Int = 0,
    val todayUpdatedCount: Int = 0,
    val verificationPercent: Int = 0,
    val lastSyncLabel: String = "Auto-Sync: ON • Checking..."
)

data class AutoSyncUiState(
    val enabled: Boolean = true,
    val isSyncing: Boolean = false,
    val isPausedBlocked: Boolean = false,
    val lastUpdatedEpochMillis: Long? = null,
    val statusText: String = "Auto-Sync: ON • Initializing"
)

data class ToastMessage(
    val text: String,
    val isError: Boolean = false,
    val isWarning: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val PREFS_NAME = "dpboss_analytics_prefs"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_LAST_SYNC_EPOCH = "last_sync_epoch"
        private const val AUTO_SYNC_INTERVAL_MS = 15L * 60L * 1000L // 15 minutes
        private const val CLOCK_TICK_INTERVAL_MS = 30L * 1000L // 30 seconds for live IST & "Xm ago"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repository: MatkaRepository = MatkaRepository(
        dao = MatkaDatabase.getInstance(application).matkaDao()
    )

    private val _activeTab = MutableStateFlow(AppTab.HOME)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _subScreen = MutableStateFlow<SubScreen>(SubScreen.None)
    val subScreen: StateFlow<SubScreen> = _subScreen.asStateFlow()

    private val _selectedMarket = MutableStateFlow("")
    val selectedMarket: StateFlow<String> = _selectedMarket.asStateFlow()

    // Auto-Sync & Live IST clock states
    private val _autoSyncEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true))
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncPausedBlocked = MutableStateFlow(false)
    private val _lastSyncEpochMillis = MutableStateFlow<Long?>(
        prefs.getLong(KEY_LAST_SYNC_EPOCH, 0L).takeIf { it > 0L }
    )
    private val _nowTickMillis = MutableStateFlow(System.currentTimeMillis())

    val autoSyncState: StateFlow<AutoSyncUiState> = combine(
        _autoSyncEnabled,
        _isSyncing,
        _syncPausedBlocked,
        _lastSyncEpochMillis,
        _nowTickMillis
    ) { enabled, syncing, paused, lastEpoch, now ->
        val text = when {
            syncing -> "Auto-Sync: Syncing DPBoss..."
            !enabled -> "Auto-Sync: OFF • Manual paste ready"
            paused -> "Sync paused — Manual paste ready"
            lastEpoch != null -> "Auto-Sync: ON • Last updated: ${formatRelativeAge(now, lastEpoch)}"
            else -> "Auto-Sync: ON • Waiting for results"
        }
        AutoSyncUiState(
            enabled = enabled,
            isSyncing = syncing,
            isPausedBlocked = paused,
            lastUpdatedEpochMillis = lastEpoch,
            statusText = text
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AutoSyncUiState())

    // Live Open-to-Close Predictor state
    private val _openPanaInput = MutableStateFlow("")
    val openPanaInput: StateFlow<String> = _openPanaInput.asStateFlow()

    private val _openPanaValidation = MutableStateFlow<PanaValidationResult?>(null)
    val openPanaValidation: StateFlow<PanaValidationResult?> = _openPanaValidation.asStateFlow()

    private val _openToCloseResult = MutableStateFlow<OpenToClosePredictionResult?>(null)
    val openToCloseResult: StateFlow<OpenToClosePredictionResult?> = _openToCloseResult.asStateFlow()

    // Bulk Copy-Paste Importer state
    private val _pasteRawText = MutableStateFlow("")
    val pasteRawText: StateFlow<String> = _pasteRawText.asStateFlow()

    private val _importTargetMarket = MutableStateFlow("KALYAN")
    val importTargetMarket: StateFlow<String> = _importTargetMarket.asStateFlow()

    private val _parsePreviewReport = MutableStateFlow<ParsePreviewReport?>(null)
    val parsePreviewReport: StateFlow<ParsePreviewReport?> = _parsePreviewReport.asStateFlow()

    private val _overwriteConflicts = MutableStateFlow(false)
    val overwriteConflicts: StateFlow<Boolean> = _overwriteConflicts.asStateFlow()

    // Global modals
    private val _inspectedPana = MutableStateFlow<String?>(null)
    val inspectedPana: StateFlow<String?> = _inspectedPana.asStateFlow()

    private val _editingScheduleMarket = MutableStateFlow<MarketEntity?>(null)
    val editingScheduleMarket: StateFlow<MarketEntity?> = _editingScheduleMarket.asStateFlow()

    private val _toastEvents = MutableSharedFlow<ToastMessage>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<ToastMessage> = _toastEvents.asSharedFlow()

    val allMarkets: StateFlow<List<MarketEntity>> = repository.allMarkets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<ResultRecordEntity>> = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val marketSummaries: StateFlow<List<MarketCardSummary>> = combine(
        allMarkets,
        allRecords,
        _nowTickMillis
    ) { markets, records, now ->
        val grouped = records.groupBy { it.marketName }
        markets.map { market ->
            val mRecords = grouped[market.name].orEmpty().sortedByDescending { it.dateIso }
            val cat = runCatching { MarketCategory.valueOf(market.category) }
                .getOrElse { MarketCategory.inferFromName(market.name) }
            val schedule = market.resolvedSchedule()
            val evaluation = MarketScheduleEngine.evaluateAt(schedule, now)
            MarketCardSummary(
                market = market,
                latestRecord = mRecords.firstOrNull(),
                totalRecords = mRecords.size,
                verifiedCount = mRecords.count { it.isVerified },
                category = cat,
                isFavorite = market.isFavorite,
                schedule = schedule,
                scheduleEvaluation = evaluation
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = combine(
        allMarkets,
        allRecords,
        autoSyncState
    ) { markets, records, syncUi ->
        val todayIso = DpBossTextParser.todayDateIso()
        val total = records.size
        val verified = records.count { it.isVerified }
        val todayCount = records.count { it.dateIso == todayIso }
        val verifyPct = if (total == 0) 0 else ((verified * 100) / total)
        DashboardStats(
            totalMarkets = markets.size,
            totalRecords = total,
            todayUpdatedCount = todayCount,
            verificationPercent = verifyPct,
            lastSyncLabel = syncUi.statusText
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    /** Effective selected market name (prefers favorited market first if none selected). */
    val activeMarketName: StateFlow<String> = combine(
        _selectedMarket,
        marketSummaries
    ) { chosen, summaries ->
        when {
            chosen.isNotBlank() && summaries.any { it.market.name == chosen } -> chosen
            summaries.isNotEmpty() -> {
                val favWithData = summaries.filter { it.isFavorite && it.totalRecords > 0 }
                    .maxByOrNull { it.totalRecords }
                favWithData?.market?.name
                    ?: summaries.maxByOrNull { it.totalRecords }?.market?.name
                    ?: summaries.first().market.name
            }
            else -> ""
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val activeMarketSummary: StateFlow<MarketCardSummary?> = combine(
        activeMarketName,
        marketSummaries
    ) { name, list ->
        list.firstOrNull { it.market.name == name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Reactive deep statistical analytics snapshot recalculated automatically on any data or IST day change. */
    val analyticsSnapshot: StateFlow<MarketAnalyticsSnapshot?> = combine(
        activeMarketName,
        allMarkets,
        allRecords,
        _nowTickMillis
    ) { marketName, markets, records, now ->
        if (marketName.isBlank()) {
            null
        } else {
            val entity = markets.firstOrNull { it.name == marketName }
            val schedule = entity?.resolvedSchedule()
                ?: MarketScheduleEngine.defaultScheduleForMarket(marketName)
            val marketRecords = records.filter { it.marketName == marketName }
            MatkaAnalyticsEngine.analyzeMarket(
                marketName = marketName,
                rawRecords = marketRecords,
                schedule = schedule,
                nowEpochMillis = now
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // 1. Live IST clock ticker (every 30s) for live status badges & relative sync age
        viewModelScope.launch {
            while (true) {
                delay(CLOCK_TICK_INTERVAL_MS)
                _nowTickMillis.value = System.currentTimeMillis()
            }
        }

        // 2. Auto-Update engine: silent sync on app open + every 15 minutes while active
        viewModelScope.launch {
            if (_autoSyncEnabled.value) {
                performSync(isSilentBackground = true)
            }
            while (true) {
                delay(AUTO_SYNC_INTERVAL_MS)
                if (_autoSyncEnabled.value) {
                    performSync(isSilentBackground = true)
                }
            }
        }

        // 3. Auto-recalculate active Live Open-to-Close prediction whenever new records arrive!
        viewModelScope.launch {
            combine(activeMarketName, allRecords) { market, records ->
                market to records
            }.collect { (market, records) ->
                val currentOpen = _openPanaInput.value.trim()
                if (market.isNotBlank() && MatkaMathEngine.isValidPana(currentOpen) && _openToCloseResult.value != null) {
                    val marketRecords = records.filter { it.marketName == market }
                    _openToCloseResult.value = withContext(Dispatchers.Default) {
                        MatkaAnalyticsEngine.predictOpenToClose(currentOpen, marketRecords)
                    }
                }
            }
        }
    }

    // --- Auto-Sync Controls ---
    fun toggleAutoSync(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_SYNC_ENABLED, enabled).apply()
        if (enabled) {
            _syncPausedBlocked.value = false
            viewModelScope.launch { performSync(isSilentBackground = true) }
        }
    }

    fun syncLiveFromDpBoss() {
        viewModelScope.launch {
            performSync(isSilentBackground = false)
        }
    }

    private suspend fun performSync(isSilentBackground: Boolean) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        val outcome = repository.syncLiveFromWeb()
        val now = System.currentTimeMillis()
        _nowTickMillis.value = now

        when (val res = outcome.liveResult) {
            is LiveSyncResult.Success -> {
                _syncPausedBlocked.value = false
                _lastSyncEpochMillis.value = now
                prefs.edit().putLong(KEY_LAST_SYNC_EPOCH, now).apply()
                if (!isSilentBackground) {
                    val msg = if (outcome.newOrUpdatedCount > 0) {
                        "Synced ${outcome.newOrUpdatedCount} new/updated results via ${res.viaSourceLabel}."
                    } else {
                        "Up to date (${outcome.totalFetchedCount} live results checked via ${res.viaSourceLabel})."
                    }
                    emitToast(msg)
                }
            }
            is LiveSyncResult.BlockedOrFailed -> {
                _syncPausedBlocked.value = true
                // Background auto-sync MUST fail silently without annoying popups!
                if (!isSilentBackground) {
                    emitToast(res.reason, isWarning = true)
                }
            }
        }
        _isSyncing.value = false
    }

    // --- Navigation Actions ---
    fun selectTab(tab: AppTab) {
        _subScreen.value = SubScreen.None
        _activeTab.value = tab
    }

    fun openHistory(marketName: String? = null) {
        _subScreen.value = SubScreen.History(marketName ?: activeMarketName.value.ifBlank { null })
    }

    fun openPanaChart() {
        _subScreen.value = SubScreen.PanaChart
    }

    fun closeSubScreen() {
        _subScreen.value = SubScreen.None
    }

    fun selectMarket(marketName: String) {
        _selectedMarket.value = marketName
        val currentOpen = _openPanaInput.value.trim()
        if (MatkaMathEngine.isValidPana(currentOpen)) {
            runOpenToClosePrediction(currentOpen, marketName)
        } else {
            _openToCloseResult.value = null
        }
    }

    fun openMarketAnalysis(marketName: String) {
        selectMarket(marketName)
        selectTab(AppTab.PREDICTIONS)
    }

    fun openMarketImport(marketName: String) {
        _importTargetMarket.value = marketName
        selectTab(AppTab.DATA)
    }

    fun inspectPana(pana: String?) {
        _inspectedPana.value = pana
    }

    // --- Favorites & Schedule Editing ---
    fun toggleMarketFavorite(marketName: String) {
        val current = allMarkets.value.firstOrNull { it.name == marketName } ?: return
        val newFav = !current.isFavorite
        viewModelScope.launch {
            repository.setMarketFavorite(marketName, newFav)
            emitToast(if (newFav) "★ Pinned $marketName to My Markets" else "Removed $marketName from Favorites")
        }
    }

    fun openEditSchedule(marketName: String) {
        val entity = allMarkets.value.firstOrNull { it.name == marketName } ?: return
        _editingScheduleMarket.value = entity
    }

    fun dismissEditSchedule() {
        _editingScheduleMarket.value = null
    }

    fun saveMarketSchedule(marketName: String, schedule: MarketScheduleConfig) {
        viewModelScope.launch {
            repository.updateMarketSchedule(marketName, schedule)
            _nowTickMillis.value = System.currentTimeMillis()
            _editingScheduleMarket.value = null
            emitToast("Saved timing & off-days for $marketName.")
        }
    }

    // --- Live Open-to-Close Predictor ---
    fun updateOpenPanaInput(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }.take(3)
        _openPanaInput.value = digitsOnly
        if (digitsOnly.isEmpty()) {
            _openPanaValidation.value = null
            _openToCloseResult.value = null
        } else {
            val validation = MatkaMathEngine.validatePana(digitsOnly)
            _openPanaValidation.value = validation
            if (validation is PanaValidationResult.Invalid) {
                _openToCloseResult.value = null
            }
        }
    }

    fun applyCanonicalSuggestion(canonicalPana: String) {
        updateOpenPanaInput(canonicalPana)
        runOpenToClosePrediction(canonicalPana, activeMarketName.value)
    }

    fun triggerOpenToClosePrediction() {
        val input = _openPanaInput.value.trim()
        val validation = MatkaMathEngine.validatePana(input)
        _openPanaValidation.value = validation
        if (validation is PanaValidationResult.Invalid) {
            emitToast(validation.reason, isError = true)
            return
        }
        val market = activeMarketName.value
        if (market.isBlank()) {
            emitToast("Import market history first to run Open-to-Close analysis.", isWarning = true)
            return
        }
        runOpenToClosePrediction(input, market)
    }

    private fun runOpenToClosePrediction(openPana: String, marketName: String) {
        viewModelScope.launch {
            val marketRecords = allRecords.value.filter { it.marketName == marketName }
            val result = withContext(Dispatchers.Default) {
                MatkaAnalyticsEngine.predictOpenToClose(
                    openPana = openPana,
                    rawRecords = marketRecords
                )
            }
            _openToCloseResult.value = result
        }
    }

    // --- Bulk Copy-Paste Importer ---
    fun updatePasteText(text: String) {
        _pasteRawText.value = text
    }

    fun updateImportTargetMarket(name: String) {
        _importTargetMarket.value = name.uppercase()
    }

    fun toggleOverwriteConflicts(enabled: Boolean) {
        _overwriteConflicts.value = enabled
    }

    fun parseBulkPasteText() {
        val raw = _pasteRawText.value
        if (raw.isBlank()) {
            emitToast("Paste DPBoss inline results or panel chart text first.", isWarning = true)
            return
        }
        viewModelScope.launch {
            val targetMarket = _importTargetMarket.value.trim().ifEmpty {
                activeMarketName.value.ifEmpty { "KALYAN" }
            }
            val report = withContext(Dispatchers.Default) {
                repository.previewBulkPaste(raw, targetMarket)
            }
            _parsePreviewReport.value = report
            if (report.rows.isEmpty()) {
                emitToast(
                    "No Matka result patterns detected. Check format (e.g., 259-67-449 or Panel Chart).",
                    isWarning = true
                )
            }
        }
    }

    fun clearImportPreview() {
        _parsePreviewReport.value = null
        _pasteRawText.value = ""
    }

    fun confirmAndSaveParsedImport() {
        val report = _parsePreviewReport.value ?: return
        viewModelScope.launch {
            val savedCount = repository.commitParsedImport(
                rows = report.rows,
                overwriteConflicts = _overwriteConflicts.value
            )
            if (savedCount > 0) {
                val firstMarket = report.rows.firstOrNull {
                    it.status != com.example.data.parser.ParsedRowStatus.INVALID
                }?.marketName
                if (!firstMarket.isNullOrBlank()) {
                    _selectedMarket.value = firstMarket
                }
                _parsePreviewReport.value = null
                _pasteRawText.value = ""
                val now = System.currentTimeMillis()
                _lastSyncEpochMillis.value = now
                _nowTickMillis.value = now
                prefs.edit().putLong(KEY_LAST_SYNC_EPOCH, now).apply()
                emitToast("Merged $savedCount records — analytics & Top 5 recalculated.")
            } else {
                emitToast("No new valid rows to save. Toggle overwrite for conflicts if needed.", isWarning = true)
            }
        }
    }

    // --- Manual Entry & Market CRUD ---
    fun addMarket(rawName: String, category: MarketCategory) {
        val clean = rawName.trim().uppercase()
        if (clean.isEmpty()) {
            emitToast("Enter a valid market name.", isError = true)
            return
        }
        viewModelScope.launch {
            repository.ensureMarketExists(clean, category)
            _selectedMarket.value = clean
            emitToast("Market '$clean' added with default IST schedule.")
        }
    }

    fun deleteMarket(marketName: String) {
        viewModelScope.launch {
            repository.deleteMarket(marketName)
            if (_selectedMarket.value == marketName) {
                _selectedMarket.value = ""
            }
            emitToast("Deleted '$marketName' and its history.")
        }
    }

    fun saveManualResult(
        marketName: String,
        dateIso: String,
        openPana: String,
        closePana: String?
    ) {
        val cleanMarket = marketName.trim().uppercase()
        if (cleanMarket.isEmpty()) {
            emitToast("Select or enter a market name.", isError = true)
            return
        }
        val normalizedDate = DpBossTextParser.normalizeDateToIso(dateIso)
        if (normalizedDate == null) {
            emitToast("Enter a valid date (YYYY-MM-DD or DD/MM/YYYY).", isError = true)
            return
        }
        val cleanOpen = openPana.trim()
        val cleanClose = closePana?.trim()?.takeIf { it.isNotEmpty() }
        val error = MatkaMathEngine.validateResultConsistency(cleanOpen, null, cleanClose)
        if (error != null) {
            emitToast(error, isError = true)
            return
        }

        val openAnk = MatkaMathEngine.calculateAnk(cleanOpen)
        val closeAnk = cleanClose?.let { MatkaMathEngine.calculateAnk(it) }
        val jodi = if (openAnk != null && closeAnk != null) "$openAnk$closeAnk" else null
        val dow = DpBossTextParser.isoDateToDayOfWeek(normalizedDate)

        viewModelScope.launch {
            repository.saveSingleRecord(
                ResultRecordEntity(
                    marketName = cleanMarket,
                    dateIso = normalizedDate,
                    dayOfWeek = dow,
                    openPana = cleanOpen,
                    openAnk = openAnk,
                    closePana = cleanClose,
                    closeAnk = closeAnk,
                    jodi = jodi,
                    source = RecordSource.MANUAL.name,
                    isVerified = cleanClose != null
                )
            )
            _selectedMarket.value = cleanMarket
            emitToast("Saved $cleanMarket ($normalizedDate): $cleanOpen-${jodi ?: "${openAnk}*"}-${cleanClose ?: "***"}")
        }
    }

    fun toggleRecordVerification(record: ResultRecordEntity) {
        viewModelScope.launch {
            repository.setRecordVerified(record.id, !record.isVerified)
        }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
            emitToast("Record deleted.")
        }
    }

    fun clearAllDatabase() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedMarket.value = ""
            _openToCloseResult.value = null
            _parsePreviewReport.value = null
            emitToast("All stored markets and history cleared.")
        }
    }

    // --- Backup & Export ---
    suspend fun buildBackupJson(): String = withContext(Dispatchers.IO) {
        repository.exportBackupJson()
    }

    suspend fun buildCsvExport(marketFilter: String?): String = withContext(Dispatchers.IO) {
        repository.exportCsv(marketFilter)
    }

    fun restoreFromBackupJson(json: String, replaceExisting: Boolean) {
        viewModelScope.launch {
            val res = withContext(Dispatchers.IO) {
                repository.restoreBackupJson(json, replaceExisting)
            }
            res.onSuccess { count ->
                emitToast("Successfully restored $count verified records.")
            }.onFailure { err ->
                emitToast("JSON restore failed: ${err.message ?: "Invalid format"}", isError = true)
            }
        }
    }

    fun notifyToast(message: String, isError: Boolean = false, isWarning: Boolean = false) {
        emitToast(message, isError, isWarning)
    }

    private fun emitToast(text: String, isError: Boolean = false, isWarning: Boolean = false) {
        _toastEvents.tryEmit(ToastMessage(text, isError, isWarning))
    }

    private fun formatRelativeAge(nowMillis: Long, pastMillis: Long): String {
        val diffSec = ((nowMillis - pastMillis) / 1000L).coerceAtLeast(0L)
        return when {
            diffSec < 60L -> "just now"
            diffSec < 3600L -> "${diffSec / 60L}m ago"
            diffSec < 86400L -> "${diffSec / 3600L}h ago"
            else -> "${diffSec / 86400L}d ago"
        }
    }
}
