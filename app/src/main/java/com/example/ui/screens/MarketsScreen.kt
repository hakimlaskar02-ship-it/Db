package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MarketCategory
import com.example.ui.components.EmptyAnalyticsState
import com.example.ui.components.GlassAnalyticsCard
import com.example.ui.components.LiveMarketStatusBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles
import com.example.viewmodel.MarketCardSummary

private enum class MarketSortOption(val label: String) {
    RECENTLY_UPDATED("Recently Updated"),
    ALPHABETICAL("A → Z"),
    MOST_RECORDS("Most Records")
}

@Composable
fun MarketsScreen(
    summaries: List<MarketCardSummary>,
    onViewAnalysis: (String) -> Unit,
    onViewHistory: (String) -> Unit,
    onImportForMarket: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onEditSchedule: (String) -> Unit,
    onAddMarket: (String, MarketCategory) -> Unit,
    onDeleteMarket: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var activeFilterTab by remember { mutableIntStateOf(0) } // 0 = All Markets, 1 = ★ Favorites
    var selectedCategory by remember { mutableStateOf<MarketCategory?>(null) }
    var sortOption by remember { mutableStateOf(MarketSortOption.RECENTLY_UPDATED) }
    var showAddDialog by remember { mutableStateOf(false) }
    var marketToDelete by remember { mutableStateOf<String?>(null) }

    val favoriteCount = remember(summaries) { summaries.count { it.isFavorite } }

    val (pinnedFavorites, otherMarkets) = remember(summaries, searchQuery, activeFilterTab, selectedCategory, sortOption) {
        val base = summaries.filter { item ->
            val nameMatch = searchQuery.isBlank() || item.market.name.contains(searchQuery.trim(), ignoreCase = true)
            val catMatch = selectedCategory == null || item.category == selectedCategory
            val tabMatch = activeFilterTab == 0 || item.isFavorite
            nameMatch && catMatch && tabMatch
        }
        val comparator = when (sortOption) {
            MarketSortOption.ALPHABETICAL -> compareBy<MarketCardSummary> { it.market.name }
            MarketSortOption.MOST_RECORDS -> compareByDescending<MarketCardSummary> { it.totalRecords }.thenBy { it.market.name }
            MarketSortOption.RECENTLY_UPDATED -> compareByDescending<MarketCardSummary> { it.latestRecord?.dateIso ?: "" }.thenBy { it.market.name }
        }
        val favs = base.filter { it.isFavorite }.sortedWith(comparator)
        val nonFavs = base.filter { !it.isFavorite }.sortedWith(comparator)
        favs to nonFavs
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header + Add Market + Search + [ ★ Favorites ] / [ All Markets ] Tabs
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Markets Directory",
                            style = MaterialTheme.typography.headlineLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${summaries.size} markets • ★ $favoriteCount pinned • Live IST",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Surface(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.electricBlue,
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("add_market_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Add Market", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        }
                    }
                }

                // Instant Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search market (e.g., Kalyan, Sridevi, Main Bazar)...", color = colors.textMuted) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_clear), tint = colors.textSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.electricBlue,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = colors.card,
                        unfocusedContainerColor = colors.card,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("markets_search_input")
                )

                // Quick Filter Tabs: [ ★ Favorites ] and [ All Markets ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.navy)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(13.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isFavTab = activeFilterTab == 1
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isFavTab) colors.warningSoft else Color.Transparent)
                            .border(
                                width = if (isFavTab) 1.dp else 0.dp,
                                color = if (isFavTab) colors.warning.copy(alpha = 0.7f) else Color.Transparent,
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable { activeFilterTab = 1 }
                            .padding(vertical = 9.dp)
                            .testTag("markets_tab_favorites")
                    ) {
                        Text(
                            text = "★ Favorites ($favoriteCount)",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isFavTab) colors.warning else colors.textSecondary
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (!isFavTab) colors.electricBlue else Color.Transparent)
                            .clickable { activeFilterTab = 0 }
                            .padding(vertical = 9.dp)
                            .testTag("markets_tab_all")
                    ) {
                        Text(
                            text = "All Markets (${summaries.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (!isFavTab) Color.White else colors.textSecondary
                        )
                    }
                }

                // Sort & Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarketSortOption.entries.forEach { opt ->
                        val selected = sortOption == opt
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) colors.electricBlueSoft else colors.card)
                                .border(0.9.dp, if (selected) colors.electricBlue else colors.borderSubtle, RoundedCornerShape(10.dp))
                                .clickable { sortOption = opt }
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = opt.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) colors.electricBlue else colors.textSecondary
                            )
                        }
                    }
                    FilterCategoryPill("All Types", selectedCategory == null) { selectedCategory = null }
                    MarketCategory.entries.forEach { cat ->
                        FilterCategoryPill(cat.label, selectedCategory == cat) { selectedCategory = cat }
                    }
                }
            }
        }

        if (pinnedFavorites.isEmpty() && otherMarkets.isEmpty()) {
            item {
                EmptyAnalyticsState(
                    title = when {
                        summaries.isEmpty() -> "No Markets Stored Yet"
                        activeFilterTab == 1 -> "No Favorite Markets Yet"
                        else -> "No Matching Markets"
                    },
                    subtitle = when {
                        summaries.isEmpty() -> "Import results from DPBoss or tap Add Market above to build your analytics vault."
                        activeFilterTab == 1 -> "Tap the ★ Star icon next to any market to pin it permanently under ★ My Markets."
                        else -> "Try clearing your search or switching category filters."
                    },
                    primaryActionLabel = when {
                        summaries.isEmpty() -> "IMPORT RESULTS"
                        activeFilterTab == 1 -> "VIEW ALL MARKETS"
                        else -> "RESET FILTERS"
                    },
                    onPrimaryAction = {
                        when {
                            summaries.isEmpty() -> onImportForMarket("KALYAN")
                            activeFilterTab == 1 -> activeFilterTab = 0
                            else -> {
                                searchQuery = ""
                                selectedCategory = null
                            }
                        }
                    }
                )
            }
        } else {
            // ★ My Markets Pinned Section (ALWAYS pinned at top)
            if (pinnedFavorites.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "★ MY MARKETS",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.warning
                        )
                    }
                }

                items(pinnedFavorites, key = { "fav_${it.market.name}" }) { summary ->
                    InteractiveMarketCard(
                        summary = summary,
                        onViewAnalysis = { onViewAnalysis(summary.market.name) },
                        onViewHistory = { onViewHistory(summary.market.name) },
                        onImportResults = { onImportForMarket(summary.market.name) },
                        onToggleFavorite = { onToggleFavorite(summary.market.name) },
                        onEditSchedule = { onEditSchedule(summary.market.name) },
                        onDeleteRequest = { marketToDelete = summary.market.name },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Remaining Markets Section
            if (otherMarkets.isNotEmpty()) {
                if (pinnedFavorites.isNotEmpty()) {
                    item {
                        Text(
                            text = "ALL MARKETS",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                }

                items(otherMarkets, key = { "all_${it.market.name}" }) { summary ->
                    InteractiveMarketCard(
                        summary = summary,
                        onViewAnalysis = { onViewAnalysis(summary.market.name) },
                        onViewHistory = { onViewHistory(summary.market.name) },
                        onImportResults = { onImportForMarket(summary.market.name) },
                        onToggleFavorite = { onToggleFavorite(summary.market.name) },
                        onEditSchedule = { onEditSchedule(summary.market.name) },
                        onDeleteRequest = { marketToDelete = summary.market.name },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMarketDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cat ->
                onAddMarket(name, cat)
                showAddDialog = false
            }
        )
    }

    marketToDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { marketToDelete = null },
            containerColor = colors.navy,
            title = { Text("Delete $name?", color = colors.textPrimary) },
            text = {
                Text(
                    "This permanently removes $name and all of its stored historical result records from this device.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMarket(name)
                        marketToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { marketToDelete = null }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun FilterCategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = DpBossTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.neonPurple else colors.navy)
            .border(0.8.dp, if (selected) colors.neonPurple else colors.borderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else colors.textSecondary
        )
    }
}

@Composable
private fun InteractiveMarketCard(
    summary: MarketCardSummary,
    onViewAnalysis: () -> Unit,
    onViewHistory: () -> Unit,
    onImportResults: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditSchedule: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    val latest = summary.latestRecord

    GlassAnalyticsCard(
        glowAccent = when {
            summary.isFavorite -> colors.warning
            summary.totalRecords > 0 -> colors.electricBlue
            else -> null
        },
        contentPadding = PaddingValues(15.dp),
        modifier = modifier
    ) {
        // Row 1: Star + Market Name + Edit Timing Gear + Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("favorite_market_${summary.market.name.lowercase().replace(" ", "_")}")
            ) {
                Icon(
                    imageVector = if (summary.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (summary.isFavorite) "Remove favorite" else "Favorite market",
                    tint = if (summary.isFavorite) colors.warning else colors.textMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.market.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = colors.cyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${summary.schedule.formattedTimingRange} • ${summary.schedule.offDaysSummary}",
                        style = MonoTextStyles.MetricSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(
                onClick = onEditSchedule,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("edit_schedule_${summary.market.name.lowercase().replace(" ", "_")}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Edit timing and off-days",
                    tint = colors.cyan,
                    modifier = Modifier.size(19.dp)
                )
            }
            IconButton(
                onClick = onDeleteRequest,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete market",
                    tint = colors.textMuted,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Row 2: Live IST Status Badge + Category + Record Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LiveMarketStatusBadge(status = summary.scheduleEvaluation.status)
            Text(
                text = "${summary.totalRecords} draws • ${latest?.dateIso ?: "No dates"}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }

        Spacer(Modifier.height(10.dp))

        // Open | Jodi | Close Structured Breakdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.navy)
                .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ResultMetricColumn("OPEN PANA", latest?.openPana ?: "---", "Ank ${latest?.openAnk ?: "-"}", colors.electricBlue)
            ResultMetricColumn("JODI", latest?.jodi ?: "--", if (latest?.isComplete == true) "Complete" else "Pending", colors.warning)
            ResultMetricColumn("CLOSE PANA", latest?.closePana ?: "---", "Ank ${latest?.closeAnk ?: "-"}", colors.cyan)
        }

        Spacer(Modifier.height(10.dp))

        // Three Quick Actions: Analysis, History, Import
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MarketActionButton(
                label = "Analysis",
                icon = Icons.Rounded.AutoGraph,
                accent = colors.electricBlue,
                onClick = onViewAnalysis,
                modifier = Modifier.weight(1f)
            )
            MarketActionButton(
                label = "History",
                icon = Icons.Rounded.History,
                accent = colors.cyan,
                onClick = onViewHistory,
                modifier = Modifier.weight(1f)
            )
            MarketActionButton(
                label = "Import",
                icon = Icons.Rounded.FileUpload,
                accent = colors.emerald,
                onClick = onImportResults,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ResultMetricColumn(
    label: String,
    value: String,
    sub: String,
    accent: Color
) {
    val colors = DpBossTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MonoTextStyles.PanaMedium, color = accent)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun MarketActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.4f)),
        modifier = modifier.height(40.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = accent,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AddMarketDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, MarketCategory) -> Unit
) {
    val colors = DpBossTheme.colors
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MarketCategory.MAIN) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.navy,
        title = { Text("Add Market", color = colors.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Market Name (e.g., KALYAN)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.electricBlue,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MarketCategory.entries.forEach { cat ->
                        FilterCategoryPill(cat.label, category == cat) { category = cat }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, category) },
                enabled = name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = colors.electricBlue)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
