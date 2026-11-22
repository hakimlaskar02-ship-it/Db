package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.parser.DpBossTextParser
import com.example.ui.components.EditMarketScheduleDialog
import com.example.ui.components.PanaDetailBottomSheet
import com.example.ui.components.PremiumBottomNavBar
import com.example.ui.components.TopBrandHeader
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DataScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarketsScreen
import com.example.ui.screens.PanaChartScreen
import com.example.ui.screens.PredictionsScreen
import com.example.ui.theme.DpBossAnalyticsTheme
import com.example.ui.theme.DpBossTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SubScreen
import com.example.viewmodel.ToastMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            DpBossAnalyticsTheme {
                DpBossAnalyticsApp()
            }
        }
    }
}

@Composable
private fun DpBossAnalyticsApp(
    viewModel: MainViewModel = viewModel()
) {
    val colors = DpBossTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val subScreen by viewModel.subScreen.collectAsStateWithLifecycle()
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val autoSyncState by viewModel.autoSyncState.collectAsStateWithLifecycle()
    val marketSummaries by viewModel.marketSummaries.collectAsStateWithLifecycle()
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val activeMarketName by viewModel.activeMarketName.collectAsStateWithLifecycle()
    val activeMarketSummary by viewModel.activeMarketSummary.collectAsStateWithLifecycle()
    val analyticsSnapshot by viewModel.analyticsSnapshot.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val openPanaInput by viewModel.openPanaInput.collectAsStateWithLifecycle()
    val openPanaValidation by viewModel.openPanaValidation.collectAsStateWithLifecycle()
    val openToCloseResult by viewModel.openToCloseResult.collectAsStateWithLifecycle()
    val pasteRawText by viewModel.pasteRawText.collectAsStateWithLifecycle()
    val importTargetMarket by viewModel.importTargetMarket.collectAsStateWithLifecycle()
    val parsePreviewReport by viewModel.parsePreviewReport.collectAsStateWithLifecycle()
    val overwriteConflicts by viewModel.overwriteConflicts.collectAsStateWithLifecycle()
    val inspectedPana by viewModel.inspectedPana.collectAsStateWithLifecycle()
    val editingScheduleMarket by viewModel.editingScheduleMarket.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var lastToastStyle by remember { mutableStateOf(ToastMessage("")) }
    var showMethodologyDialog by remember { mutableStateOf(false) }
    var pendingCsvMarketFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            lastToastStyle = msg
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = msg.text,
                duration = SnackbarDuration.Short
            )
        }
    }

    // SAF Launcher: Export JSON Backup
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = viewModel.buildBackupJson()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Cannot write to selected file.")
                    }
                }.onSuccess {
                    viewModel.notifyToast("Database JSON backup exported successfully.")
                }.onFailure { err ->
                    viewModel.notifyToast("Export failed: ${err.message}", isError = true)
                }
            }
        }
    }

    // SAF Launcher: Restore JSON Backup
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Unable to read selected file.")
                    }
                }.onSuccess { jsonText ->
                    viewModel.restoreFromBackupJson(jsonText, replaceExisting = false)
                }.onFailure { err ->
                    viewModel.notifyToast("Read failed: ${err.message}", isError = true)
                }
            }
        }
    }

    // SAF Launcher: Export CSV
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val marketFilter = pendingCsvMarketFilter
            scope.launch {
                runCatching {
                    val csv = viewModel.buildCsvExport(marketFilter)
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(csv.toByteArray(Charsets.UTF_8))
                        } ?: error("Cannot write CSV file.")
                    }
                }.onSuccess {
                    viewModel.notifyToast("CSV history exported successfully.")
                }.onFailure { err ->
                    viewModel.notifyToast("CSV export failed: ${err.message}", isError = true)
                }
            }
        }
    }

    val marketNames = remember(marketSummaries) { marketSummaries.map { it.market.name } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.midnight, colors.navy.copy(alpha = 0.6f), colors.midnight)
                )
            )
    ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    val accent = when {
                        lastToastStyle.isError -> colors.danger
                        lastToastStyle.isWarning -> colors.warning
                        else -> colors.emerald
                    }
                    Snackbar(
                        containerColor = colors.cardElevated,
                        contentColor = colors.textPrimary,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = data.visuals.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = accent
                        )
                    }
                }
            },
            bottomBar = {
                PremiumBottomNavBar(
                    activeTab = activeTab,
                    onSelectTab = viewModel::selectTab
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                ) {
                    if (subScreen is SubScreen.None) {
                        TopBrandHeader(
                            autoSyncState = autoSyncState,
                            onToggleAutoSync = viewModel::toggleAutoSync,
                            onOpenPanaChart = viewModel::openPanaChart,
                            onOpenInfo = { showMethodologyDialog = true }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (val sub = subScreen) {
                            is SubScreen.PanaChart -> {
                                PanaChartScreen(
                                    onBack = viewModel::closeSubScreen,
                                    onInspectPana = viewModel::inspectPana
                                )
                            }
                            is SubScreen.History -> {
                                HistoryScreen(
                                    initialMarket = sub.initialMarket,
                                    allMarkets = marketNames,
                                    allRecords = allRecords,
                                    onBack = viewModel::closeSubScreen,
                                    onToggleVerified = viewModel::toggleRecordVerification,
                                    onDeleteRecord = viewModel::deleteRecord,
                                    onExportCsv = { filter ->
                                        pendingCsvMarketFilter = filter
                                        val stamp = DpBossTextParser.todayDateIso()
                                        val prefix = filter?.lowercase()?.replace(" ", "_") ?: "all_markets"
                                        exportCsvLauncher.launch("dpboss_${prefix}_$stamp.csv")
                                    },
                                    onInspectPana = viewModel::inspectPana,
                                    onNavigateToImport = { viewModel.selectTab(AppTab.DATA) }
                                )
                            }
                            is SubScreen.None -> {
                                Crossfade(
                                    targetState = activeTab,
                                    animationSpec = tween(durationMillis = 220),
                                    label = "main_tab_transition"
                                ) { tab ->
                                    when (tab) {
                                        AppTab.HOME -> HomeScreen(
                                            stats = dashboardStats,
                                            marketSummaries = marketSummaries,
                                            isSyncing = isSyncing,
                                            onSyncLive = viewModel::syncLiveFromDpBoss,
                                            onOpenImport = { viewModel.selectTab(AppTab.DATA) },
                                            onOpenPredictions = { viewModel.selectTab(AppTab.PREDICTIONS) },
                                            onOpenPanaChart = viewModel::openPanaChart,
                                            onOpenMarketAnalysis = viewModel::openMarketAnalysis,
                                            onOpenMarketHistory = { m -> viewModel.openHistory(m) },
                                            onToggleFavorite = viewModel::toggleMarketFavorite,
                                            onOpenAllMarkets = { viewModel.selectTab(AppTab.MARKETS) }
                                        )

                                        AppTab.PREDICTIONS -> PredictionsScreen(
                                            marketSummaries = marketSummaries,
                                            selectedMarket = activeMarketName,
                                            activeMarketSummary = activeMarketSummary,
                                            snapshot = analyticsSnapshot,
                                            openPanaInput = openPanaInput,
                                            openPanaValidation = openPanaValidation,
                                            openToCloseResult = openToCloseResult,
                                            onSelectMarket = viewModel::selectMarket,
                                            onToggleFavorite = viewModel::toggleMarketFavorite,
                                            onEditSchedule = viewModel::openEditSchedule,
                                            onUpdateOpenPanaInput = viewModel::updateOpenPanaInput,
                                            onApplyCanonicalPana = viewModel::applyCanonicalSuggestion,
                                            onPredictOpenToClose = viewModel::triggerOpenToClosePrediction,
                                            onInspectPana = viewModel::inspectPana,
                                            onNavigateToImport = { viewModel.selectTab(AppTab.DATA) }
                                        )

                                        AppTab.ANALYTICS -> AnalyticsScreen(
                                            marketSummaries = marketSummaries,
                                            selectedMarket = activeMarketName,
                                            snapshot = analyticsSnapshot,
                                            onSelectMarket = viewModel::selectMarket,
                                            onToggleFavorite = viewModel::toggleMarketFavorite,
                                            onOpenPanaChart = viewModel::openPanaChart,
                                            onInspectPana = viewModel::inspectPana,
                                            onNavigateToImport = { viewModel.selectTab(AppTab.DATA) }
                                        )

                                        AppTab.MARKETS -> MarketsScreen(
                                            summaries = marketSummaries,
                                            onViewAnalysis = viewModel::openMarketAnalysis,
                                            onViewHistory = { m -> viewModel.openHistory(m) },
                                            onImportForMarket = viewModel::openMarketImport,
                                            onToggleFavorite = viewModel::toggleMarketFavorite,
                                            onEditSchedule = viewModel::openEditSchedule,
                                            onAddMarket = viewModel::addMarket,
                                            onDeleteMarket = viewModel::deleteMarket
                                        )

                                        AppTab.DATA -> DataScreen(
                                            isSyncing = isSyncing,
                                            lastSyncStatus = autoSyncState.statusText,
                                            pasteText = pasteRawText,
                                            importTargetMarket = importTargetMarket,
                                            parsePreview = parsePreviewReport,
                                            overwriteConflicts = overwriteConflicts,
                                            totalRecords = dashboardStats.totalRecords,
                                            onSyncLive = viewModel::syncLiveFromDpBoss,
                                            onPasteTextChange = viewModel::updatePasteText,
                                            onTargetMarketChange = viewModel::updateImportTargetMarket,
                                            onParsePaste = viewModel::parseBulkPasteText,
                                            onToggleOverwriteConflicts = viewModel::toggleOverwriteConflicts,
                                            onConfirmSaveImport = viewModel::confirmAndSaveParsedImport,
                                            onClearPreview = viewModel::clearImportPreview,
                                            onSaveManualEntry = viewModel::saveManualResult,
                                            onExportJsonBackup = {
                                                val stamp = DpBossTextParser.todayDateIso()
                                                exportJsonLauncher.launch("dpboss_analytics_backup_$stamp.json")
                                            },
                                            onImportJsonBackup = {
                                                importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                            },
                                            onExportCsv = {
                                                pendingCsvMarketFilter = null
                                                val stamp = DpBossTextParser.todayDateIso()
                                                exportCsvLauncher.launch("dpboss_all_history_$stamp.csv")
                                            },
                                            onOpenFullHistory = { viewModel.openHistory(null) },
                                            onClearAllDatabase = viewModel::clearAllDatabase
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Global Pana Detail Modal Bottom Sheet
    inspectedPana?.let { pana ->
        PanaDetailBottomSheet(
            pana = pana,
            activeMarketName = activeMarketName,
            marketHitCount = analyticsSnapshot?.panaHitCounts?.get(pana),
            onDismiss = { viewModel.inspectPana(null) },
            onSelectRelatedPana = { related -> viewModel.inspectPana(related) }
        )
    }

    // Global Edit Market Schedule Dialog
    editingScheduleMarket?.let { marketEntity ->
        EditMarketScheduleDialog(
            marketName = marketEntity.name,
            initialSchedule = marketEntity.resolvedSchedule(),
            onDismiss = viewModel::dismissEditSchedule,
            onSave = { updated -> viewModel.saveMarketSchedule(marketEntity.name, updated) }
        )
    }

    // Methodology & Matka Rules Info Dialog
    if (showMethodologyDialog) {
        AlertDialog(
            onDismissRequest = { showMethodologyDialog = false },
            containerColor = colors.navy,
            title = { Text("DPBoss Smart Analytics Rules", color = colors.textPrimary) },
            text = {
                Column {
                    Text(
                        text = "• Strict 4 SP · 1 DP Quota: Both Top 5 Next Game Panas and Open→Close Predictor always select the Top 4 SP + #1 DP ranked by IDX (TP excluded).\n" +
                            "• Auto-Sync Engine: Automatically syncs dpbosssattamatka.com on app launch and every 15 minutes while active, failing silently if blocked.\n" +
                            "• Live IST Schedule: Tracks Open/Close timings & weekly off-days in Asia/Kolkata IST and shifts Day-Wise analysis to the next operating day when closed/off.\n" +
                            "• Matka Math: 1 < 2 < … < 9 < 0, Ank = (d1+d2+d3)%10, Cut Pairs 0↔5, 1↔6, 2↔7, 3↔8, 4↔9.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Tip: Tap ★ to pin markets under My Markets, and tap the gear icon on any market to customize timings or off-days.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.cyan
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodologyDialog = false }) {
                    Text("Got It", color = colors.electricBlue)
                }
            }
        )
    }
}
