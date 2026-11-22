package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ResultRecordEntity
import com.example.data.parser.DpBossTextParser
import com.example.ui.components.EmptyAnalyticsState
import com.example.ui.components.StatusPill
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles
import kotlin.math.ceil

private enum class HistoryTypeFilter(val label: String) {
    ALL("All"),
    COMPLETE("Complete"),
    OPEN_ONLY("Open Only"),
    VERIFIED("Verified")
}

private enum class DateRangeFilter(val label: String, val days: Int?) {
    ALL("All Time", null),
    D7("7 Days", 7),
    D30("30 Days", 30),
    D90("90 Days", 90)
}

@Composable
fun HistoryScreen(
    initialMarket: String?,
    allMarkets: List<String>,
    allRecords: List<ResultRecordEntity>,
    onBack: () -> Unit,
    onToggleVerified: (ResultRecordEntity) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onExportCsv: (String?) -> Unit,
    onInspectPana: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val colors = DpBossTheme.colors
    var selectedMarket by remember(initialMarket) { mutableStateOf<String?>(initialMarket) }
    var searchQuery by remember { mutableStateOf("") }
    var sortDescending by remember { mutableStateOf(true) }
    var typeFilter by remember { mutableStateOf(HistoryTypeFilter.ALL) }
    var dateRange by remember { mutableStateOf(DateRangeFilter.ALL) }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 25

    val filtered = remember(allRecords, selectedMarket, searchQuery, sortDescending, typeFilter, dateRange) {
        val cutoffIso = dateRange.days?.let { days ->
            DpBossTextParser.addDaysToIso(DpBossTextParser.todayDateIso(), -days)
        }
        val list = allRecords.filter { rec ->
            val marketOk = selectedMarket == null || rec.marketName == selectedMarket
            val dateOk = cutoffIso == null || rec.dateIso >= cutoffIso
            val typeOk = when (typeFilter) {
                HistoryTypeFilter.ALL -> true
                HistoryTypeFilter.COMPLETE -> rec.isComplete
                HistoryTypeFilter.OPEN_ONLY -> !rec.isComplete
                HistoryTypeFilter.VERIFIED -> rec.isVerified
            }
            val q = searchQuery.trim()
            val queryOk = q.isEmpty() ||
                rec.dateIso.contains(q, ignoreCase = true) ||
                rec.marketName.contains(q, ignoreCase = true) ||
                rec.formattedResult.contains(q, ignoreCase = true)
            marketOk && dateOk && typeOk && queryOk
        }
        if (sortDescending) list.sortedByDescending { it.dateIso } else list.sortedBy { it.dateIso }
    }

    val totalPages = ceil(filtered.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
    val safePage = currentPage.coerceIn(0, totalPages - 1)
    val pageItems = filtered.drop(safePage * pageSize).take(pageSize)

    Column(modifier = modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("history_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back), tint = colors.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Historical Results", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Text("${filtered.size} matching records", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Surface(
                onClick = { onExportCsv(selectedMarket) },
                shape = RoundedCornerShape(10.dp),
                color = colors.emeraldSoft,
                border = BorderStroke(0.8.dp, colors.emerald.copy(alpha = 0.5f)),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag("history_export_csv_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("CSV", style = MaterialTheme.typography.labelLarge, color = colors.emerald)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Controls Block
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Market filter row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        HistoryChip("All Markets", selectedMarket == null) {
                            selectedMarket = null
                            currentPage = 0
                        }
                        allMarkets.forEach { m ->
                            HistoryChip(m, selectedMarket == m) {
                                selectedMarket = m
                                currentPage = 0
                            }
                        }
                    }

                    // Search + Sort
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                currentPage = 0
                            },
                            placeholder = { Text("Search date, Pana, Jodi...", color = colors.textMuted) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_clear), tint = colors.textSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.electricBlue,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = colors.card,
                                unfocusedContainerColor = colors.card,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = { sortDescending = !sortDescending },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Icon(Icons.Rounded.SwapVert, contentDescription = "Sort by date", tint = colors.cyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (sortDescending) "New" else "Old", style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
                            }
                        }
                    }

                    // Date Range + Type Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        DateRangeFilter.entries.forEach { rf ->
                            HistoryChip(rf.label, dateRange == rf, accent = colors.cyan) {
                                dateRange = rf
                                currentPage = 0
                            }
                        }
                        HistoryTypeFilter.entries.forEach { tf ->
                            HistoryChip(tf.label, typeFilter == tf, accent = colors.neonPurple) {
                                typeFilter = tf
                                currentPage = 0
                            }
                        }
                    }
                }
            }

            if (pageItems.isEmpty()) {
                item {
                    EmptyAnalyticsState(
                        title = "No Historical Records Found",
                        subtitle = "Import DPBoss results or clear active filters to view stored records.",
                        onPrimaryAction = onNavigateToImport
                    )
                }
            } else {
                itemsIndexed(pageItems, key = { _, rec -> rec.id }) { idx, rec ->
                    val rowBg = if (idx % 2 == 0) colors.card else colors.navy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(rowBg)
                            .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = rec.dateIso,
                                    style = MonoTextStyles.MetricSmall,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = rec.marketName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rec.openPana?.let { op ->
                                    Text(
                                        text = op,
                                        style = MonoTextStyles.PanaMedium,
                                        color = colors.electricBlue,
                                        modifier = Modifier.clickable { onInspectPana(op) }
                                    )
                                } ?: Text("---", style = MonoTextStyles.PanaMedium, color = colors.textMuted)

                                StatusPill(
                                    label = rec.jodi ?: "${rec.openAnk ?: "*"}*",
                                    color = colors.warning
                                )

                                rec.closePana?.let { cp ->
                                    Text(
                                        text = cp,
                                        style = MonoTextStyles.PanaMedium,
                                        color = colors.cyan,
                                        modifier = Modifier.clickable { onInspectPana(cp) }
                                    )
                                } ?: Text("***", style = MonoTextStyles.PanaMedium, color = colors.textMuted)
                            }
                        }

                        IconButton(onClick = { onToggleVerified(rec) }) {
                            Icon(
                                imageVector = if (rec.isVerified) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = if (rec.isVerified) "Verified" else "Mark verified",
                                tint = if (rec.isVerified) colors.emerald else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { onDeleteRecord(rec.id) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Delete record",
                                tint = colors.danger.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Pagination Bar
                if (totalPages > 1) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { if (safePage > 0) currentPage = safePage - 1 },
                                enabled = safePage > 0,
                                shape = RoundedCornerShape(10.dp),
                                color = colors.card,
                                border = BorderStroke(0.8.dp, colors.borderSubtle)
                            ) {
                                Text(
                                    text = "Previous",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (safePage > 0) colors.textPrimary else colors.textMuted,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                            Text(
                                text = "Page ${safePage + 1} / $totalPages",
                                style = MonoTextStyles.MetricSmall,
                                color = colors.textSecondary
                            )
                            Surface(
                                onClick = { if (safePage < totalPages - 1) currentPage = safePage + 1 },
                                enabled = safePage < totalPages - 1,
                                shape = RoundedCornerShape(10.dp),
                                color = colors.card,
                                border = BorderStroke(0.8.dp, colors.borderSubtle)
                            ) {
                                Text(
                                    text = "Next",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (safePage < totalPages - 1) colors.textPrimary else colors.textMuted,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryChip(
    label: String,
    selected: Boolean,
    accent: Color = DpBossTheme.colors.electricBlue,
    onClick: () -> Unit
) {
    val colors = DpBossTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) accent else colors.card)
            .border(0.8.dp, if (selected) accent else colors.borderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else colors.textSecondary
        )
    }
}
