package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AnimatedMetricNumber
import com.example.ui.components.EmptyAnalyticsState
import com.example.ui.components.GlassAnalyticsCard
import com.example.ui.components.LiveMarketStatusBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles
import com.example.viewmodel.DashboardStats
import com.example.viewmodel.MarketCardSummary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    stats: DashboardStats,
    marketSummaries: List<MarketCardSummary>,
    isSyncing: Boolean,
    onSyncLive: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenPredictions: () -> Unit,
    onOpenPanaChart: () -> Unit,
    onOpenMarketAnalysis: (String) -> Unit,
    onOpenMarketHistory: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAllMarkets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    var currentDateTime by remember { mutableStateOf(formatCurrentTimestamp()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = formatCurrentTimestamp()
            delay(30_000L)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Welcome Hero Banner with Generated Artwork
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(colors.electricBlue.copy(alpha = 0.6f), colors.cyan.copy(alpha = 0.25f))
                        ),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_analytics_hero_1795308203733),
                    contentDescription = stringResource(R.string.cd_hero_bg),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    colors.midnight.copy(alpha = 0.90f),
                                    colors.navy.copy(alpha = 0.72f),
                                    colors.midnight.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    StatusPill(
                        label = stats.lastSyncLabel,
                        color = if (isSyncing) colors.warning else colors.cyan
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.welcome_headline),
                        style = MaterialTheme.typography.displayMedium,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = currentDateTime,
                        style = MonoTextStyles.MetricSmall,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            onClick = onSyncLive,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            color = colors.electricBlue,
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("home_sync_live_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudSync,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isSyncing) "Syncing..." else "Sync Live",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            onClick = onOpenImport,
                            shape = RoundedCornerShape(12.dp),
                            color = colors.navy.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("home_quick_paste_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = null,
                                    tint = colors.cyan,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    text = "Quick Paste",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Four Animated KPI Stat Cards (2x2 Grid)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = "Total Markets",
                        value = stats.totalMarkets,
                        icon = Icons.Rounded.Storefront,
                        accent = colors.electricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Historical Records",
                        value = stats.totalRecords,
                        icon = Icons.Rounded.Dataset,
                        accent = colors.neonPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiStatCard(
                        title = "Today's Updated",
                        value = stats.todayUpdatedCount,
                        icon = Icons.Rounded.CalendarToday,
                        accent = colors.cyan,
                        modifier = Modifier.weight(1f)
                    )
                    KpiStatCard(
                        title = "Data Verified",
                        value = stats.verificationPercent,
                        suffix = "%",
                        icon = Icons.Rounded.VerifiedUser,
                        accent = colors.emerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Quick Tools Strip
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "PRECISION TOOLS",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = "Top 5 & Open→Close",
                        subtitle = "Live predictor",
                        icon = Icons.Rounded.AutoGraph,
                        accent = colors.electricBlue,
                        onClick = onOpenPredictions,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tool_predictions")
                    )
                    QuickActionTile(
                        title = "220 Pana Chart",
                        subtitle = "SP · DP · TP math",
                        icon = Icons.Rounded.GridView,
                        accent = colors.cyan,
                        onClick = onOpenPanaChart,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_tool_pana_chart")
                    )
                }
            }
        }

        // 4. Active Markets Overview or Empty State
        if (marketSummaries.isEmpty()) {
            item {
                EmptyAnalyticsState(
                    onPrimaryAction = onOpenImport,
                    secondaryActionLabel = "Explore 220 Pana Math Reference",
                    onSecondaryAction = onOpenPanaChart
                )
            }
        } else {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MARKET INTELLIGENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenAllMarkets)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "View All (${marketSummaries.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.electricBlue
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = colors.electricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            items(marketSummaries.take(5), key = { it.market.name }) { summary ->
                HomeMarketSnapshotCard(
                    summary = summary,
                    onAnalyze = { onOpenMarketAnalysis(summary.market.name) },
                    onHistory = { onOpenMarketHistory(summary.market.name) },
                    onToggleFavorite = { onToggleFavorite(summary.market.name) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    title: String,
    value: Int,
    suffix: String = "",
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    GlassAnalyticsCard(
        glowAccent = accent,
        contentPadding = PaddingValues(14.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.7f))
            )
        }
        Spacer(Modifier.height(12.dp))
        AnimatedMetricNumber(
            targetValue = value,
            suffix = suffix,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    GlassAnalyticsCard(
        glowAccent = accent,
        contentPadding = PaddingValues(14.dp),
        onClick = onClick,
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun HomeMarketSnapshotCard(
    summary: MarketCardSummary,
    onAnalyze: () -> Unit,
    onHistory: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    val latest = summary.latestRecord
    GlassAnalyticsCard(
        glowAccent = if (summary.isFavorite) colors.warning else null,
        onClick = onAnalyze,
        contentPadding = PaddingValues(15.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (summary.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (summary.isFavorite) "Unpin favorite" else "Pin favorite",
                    tint = if (summary.isFavorite) colors.warning else colors.textMuted,
                    modifier = Modifier.size(20.dp)
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
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${summary.schedule.formattedTimingRange} • ${summary.totalRecords} draws",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            LiveMarketStatusBadge(status = summary.scheduleEvaluation.status)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.navy)
                .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = latest?.formattedResult ?: "No Result Stored",
                style = MonoTextStyles.PanaMedium,
                color = if (latest != null) colors.cyan else colors.textMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onHistory,
                    shape = RoundedCornerShape(9.dp),
                    color = colors.cardElevated
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "History",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("History", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    }
                }
                Surface(
                    onClick = onAnalyze,
                    shape = RoundedCornerShape(9.dp),
                    color = colors.electricBlueSoft
                ) {
                    Text(
                        text = "Top 5",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.electricBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun formatCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.US)
    return sdf.format(Date())
}
