package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.analytics.MarketAnalyticsSnapshot
import com.example.ui.components.BarDatum
import com.example.ui.components.EmptyAnalyticsState
import com.example.ui.components.GlassAnalyticsCard
import com.example.ui.components.InteractiveGlowingBarChart
import com.example.ui.components.MarketSelectorStrip
import com.example.ui.components.OpenCloseHeatmapMatrix
import com.example.ui.components.OverdueGapComparisonList
import com.example.ui.components.StatusPill
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles
import com.example.viewmodel.MarketCardSummary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    marketSummaries: List<MarketCardSummary>,
    selectedMarket: String,
    snapshot: MarketAnalyticsSnapshot?,
    onSelectMarket: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenPanaChart: () -> Unit,
    onInspectPana: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    var activeSection by remember { mutableIntStateOf(0) }
    var selectedDayIndex by remember { mutableIntStateOf(0) } // 0=Mon .. 6=Sun

    val sectionLabels = listOf("Hot & Cold", "Overdue", "Day-Wise", "Open→Close", "Families")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 220 Pana Mathematical Chart Banner (always accessible even when DB is empty!)
        item {
            GlassAnalyticsCard(
                glowAccent = colors.cyan,
                onClick = onOpenPanaChart,
                contentPadding = PaddingValues(14.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("analytics_open_220_chart")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(colors.cyanSoft)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GridView,
                            contentDescription = null,
                            tint = colors.cyan
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Complete 220 Pana Reference Chart",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "120 SP (Blue) • 90 DP (Purple) • 10 TP (Cyan) • Live Validator",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = colors.cyan
                    )
                }
            }
        }

        if (marketSummaries.isNotEmpty()) {
            item {
                MarketSelectorStrip(
                    markets = marketSummaries,
                    selectedMarket = selectedMarket,
                    onSelectMarket = onSelectMarket,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }

        if (snapshot == null || !snapshot.hasData) {
            item {
                EmptyAnalyticsState(
                    title = "No Historical Data Available",
                    subtitle = "Import real historical market results to unlock Hot/Cold frequency charts, Overdue gap metrics, Day-Wise patterns, and the Open→Close matrix.",
                    onPrimaryAction = onNavigateToImport
                )
            }
            return@LazyColumn
        }

        // Analytics Section Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sectionLabels.forEachIndexed { idx, label ->
                    val selected = activeSection == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (selected) colors.neonPurple else colors.card)
                            .border(
                                1.dp,
                                if (selected) colors.neonPurple else colors.borderSubtle,
                                RoundedCornerShape(11.dp)
                            )
                            .clickable { activeSection = idx }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("analytics_section_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else colors.textSecondary
                        )
                    }
                }
            }
        }

        when (activeSection) {
            // A. Hot & Cold Numbers
            0 -> {
                item {
                    val sortedHot = snapshot.ankFrequencies.sortedByDescending { it.totalHits }
                    val sortedCold = snapshot.ankFrequencies.sortedBy { it.totalHits }
                    GlassAnalyticsCard(
                        glowAccent = colors.electricBlue,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "ANK FREQUENCY DISTRIBUTION (0–9)",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap any bar to inspect Open/Close share across ${snapshot.totalRecords} records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        InteractiveGlowingBarChart(
                            items = snapshot.ankFrequencies.map {
                                BarDatum(
                                    label = it.ank.toString(),
                                    value = it.totalHits,
                                    subtitle = "O:${it.openHits} / C:${it.closeHits} (${String.format("%.1f", it.sharePercent)}%)"
                                )
                            },
                            accentStart = colors.electricBlue,
                            accentEnd = colors.cyan
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.emeraldSoft)
                                    .padding(10.dp)
                            ) {
                                Text("HOTTEST ANKS", style = MaterialTheme.typography.labelSmall, color = colors.emerald)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = sortedHot.take(4).joinToString("  ") { "${it.ank}(${it.totalHits})" },
                                    style = MonoTextStyles.MetricSmall,
                                    color = colors.textPrimary
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.dangerSoft)
                                    .padding(10.dp)
                            ) {
                                Text("COLDEST ANKS", style = MaterialTheme.typography.labelSmall, color = colors.danger)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = sortedCold.take(4).joinToString("  ") { "${it.ank}(${it.totalHits})" },
                                    style = MonoTextStyles.MetricSmall,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }

                if (snapshot.topJodiFrequencies.isNotEmpty()) {
                    item {
                        GlassAnalyticsCard(
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "TOP JODI FREQUENCY",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.height(12.dp))
                            InteractiveGlowingBarChart(
                                items = snapshot.topJodiFrequencies.take(8).map { (jodi, count) ->
                                    BarDatum(label = jodi, value = count, subtitle = "Jodi $jodi")
                                },
                                accentStart = colors.neonPurple,
                                accentEnd = colors.warning
                            )
                        }
                    }
                }

                if (snapshot.topPanaFrequencies.isNotEmpty()) {
                    item {
                        GlassAnalyticsCard(
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "MOST FREQUENT PANAS",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                snapshot.topPanaFrequencies.forEach { (pana, hits) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.navy)
                                            .border(0.8.dp, colors.electricBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .clickable { onInspectPana(pana) }
                                            .padding(horizontal = 11.dp, vertical = 7.dp)
                                    ) {
                                        Text(pana, style = MonoTextStyles.PanaCompact, color = colors.textPrimary)
                                        Spacer(Modifier.width(6.dp))
                                        Text("×$hits", style = MonoTextStyles.MetricSmall, color = colors.cyan)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // B. Overdue Analysis
            1 -> {
                item {
                    GlassAnalyticsCard(
                        glowAccent = colors.warning,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "OVERDUE ANK GAP ANALYSIS",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Draws elapsed since each Ank last hit vs historical average gap (cyan) and max gap.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        OverdueGapComparisonList(stats = snapshot.overdueAnkStats)
                    }
                }
            }

            // C. Day-Wise Analysis (Mon..Sun)
            2 -> {
                item {
                    val dayStats = snapshot.dayWiseStats
                    val safeDayIdx = selectedDayIndex.coerceIn(0, (dayStats.size - 1).coerceAtLeast(0))
                    val activeDay = dayStats.getOrNull(safeDayIdx)

                    GlassAnalyticsCard(
                        glowAccent = colors.electricBlue,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "DAY-WISE PATTERN ENGINE (MON – SUN)",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            dayStats.forEachIndexed { idx, stat ->
                                val isSel = idx == safeDayIdx
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (isSel) colors.electricBlue else colors.navy)
                                        .border(
                                            0.8.dp,
                                            if (isSel) colors.electricBlue else colors.borderSubtle,
                                            RoundedCornerShape(9.dp)
                                        )
                                        .clickable { selectedDayIndex = idx }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = stat.dayShortLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSel) Color.White else colors.textSecondary
                                    )
                                }
                            }
                        }

                        if (activeDay != null) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${activeDay.dayFullLabel} Trends",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                                StatusPill(label = "${activeDay.totalDraws} DRAWS", color = colors.cyan)
                            }
                            Spacer(Modifier.height(12.dp))

                            if (activeDay.totalDraws == 0) {
                                Text(
                                    text = "No historical draws recorded on ${activeDay.dayFullLabel} yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            } else {
                                InteractiveGlowingBarChart(
                                    items = (0..9).map { ank ->
                                        BarDatum(
                                            label = ank.toString(),
                                            value = activeDay.ankHits[ank] ?: 0,
                                            subtitle = "${activeDay.dayShortLabel} Ank $ank"
                                        )
                                    },
                                    accentStart = colors.electricBlue,
                                    accentEnd = colors.emerald
                                )
                                if (activeDay.topJodis.isNotEmpty()) {
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Top ${activeDay.dayShortLabel} Jodis",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.textSecondary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        activeDay.topJodis.forEach { (jodi, count) ->
                                            StatusPill(label = "$jodi (×$count)", color = colors.warning)
                                        }
                                    }
                                }
                                if (activeDay.topPanas.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Top ${activeDay.dayShortLabel} Panas",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.textSecondary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        activeDay.topPanas.forEach { (pana, count) ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(colors.navy)
                                                    .border(0.8.dp, colors.neonPurple.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                                                    .clickable { onInspectPana(pana) }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "$pana ×$count",
                                                    style = MonoTextStyles.MetricSmall,
                                                    color = colors.textPrimary
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

            // D. Open-to-Close Correlation Matrix
            3 -> {
                item {
                    GlassAnalyticsCard(
                        glowAccent = colors.neonPurple,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "OPEN ANK → CLOSE ANK MATRIX",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Rows = Open Ank (0–9), Columns = Close Ank (0–9). Tap any cell to inspect transition frequency across ${snapshot.completeRecords} complete draws.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        if (snapshot.completeRecords == 0) {
                            Text(
                                text = "Import complete Open–Jodi–Close results to populate the correlation matrix.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        } else {
                            OpenCloseHeatmapMatrix(matrix = snapshot.openToCloseMatrix)
                        }
                    }
                }
            }

            // E. Pana Family & Cut-Digit Analysis
            else -> {
                item {
                    GlassAnalyticsCard(
                        glowAccent = colors.cyan,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "STANDARD CUT DIGIT PAIRS",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.cyan
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("0 ↔ 5", "1 ↔ 6", "2 ↔ 7", "3 ↔ 8", "4 ↔ 9").forEach { pair ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.navy)
                                        .border(0.8.dp, colors.cyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = pair,
                                        style = MonoTextStyles.MetricSmall.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    GlassAnalyticsCard(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "ACTIVE PANA FAMILIES (VERIFIED CUT SETS)",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Ranked by historical hits in ${snapshot.marketName}. Tap any Pana to open full family breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            snapshot.familyTrends.take(10).forEach { fam ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.navy)
                                        .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${fam.sizeTypeLabel} ${fam.baseDigitsLabel}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                        StatusPill(
                                            label = "×${fam.totalHits} • Gap ${fam.drawsSinceLastHit}d",
                                            color = if (fam.recentHits > 0) colors.emerald else colors.textSecondary
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        fam.sampleMembers.forEach { pana ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(7.dp))
                                                    .background(colors.cardElevated)
                                                    .clickable { onInspectPana(pana) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = pana,
                                                    style = MonoTextStyles.MetricSmall,
                                                    color = colors.cyan
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
        }
    }
}
