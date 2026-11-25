package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.math.MatkaMathEngine
import com.example.domain.math.PanaCategory
import com.example.domain.schedule.MarketLiveStatus
import com.example.domain.schedule.MarketScheduleConfig
import com.example.domain.schedule.MarketScheduleEngine
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles
import com.example.viewmodel.AppTab
import com.example.viewmodel.AutoSyncUiState
import com.example.viewmodel.MarketCardSummary

@Composable
fun GlassAnalyticsCard(
    modifier: Modifier = Modifier,
    glowAccent: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = DpBossTheme.colors
    val borderBrush = if (glowAccent != null) {
        Brush.linearGradient(
            listOf(glowAccent.copy(alpha = 0.65f), colors.borderSubtle.copy(alpha = 0.45f))
        )
    } else {
        Brush.linearGradient(
            listOf(colors.borderSubtle.copy(alpha = 0.85f), colors.borderSubtle.copy(alpha = 0.35f))
        )
    }

    val bgBrush = if (glowAccent != null) {
        Brush.verticalGradient(
            listOf(
                glowAccent.copy(alpha = 0.11f),
                colors.card,
                colors.card
            )
        )
    } else {
        Brush.verticalGradient(listOf(colors.cardElevated.copy(alpha = 0.65f), colors.card))
    }

    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(1.dp, borderBrush, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun TopBrandHeader(
    autoSyncState: AutoSyncUiState,
    onToggleAutoSync: (Boolean) -> Unit,
    onShareApp: () -> Unit,
    onOpenPanaChart: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    val statusDotColor = when {
        autoSyncState.isSyncing -> colors.warning
        !autoSyncState.enabled -> colors.textMuted
        autoSyncState.isPausedBlocked -> colors.warning
        else -> colors.emerald
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_dpboss_brand_1795308204109),
                contentDescription = stringResource(R.string.cd_app_logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, colors.electricBlue.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.brand_full_title),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.85.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "4 SP · 1 DP ENGINE • IST SCHEDULED",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.cyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onShareApp,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("header_share_app_button")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.electricBlueSoft)
                        .border(0.9.dp, colors.electricBlue.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.cd_share_app),
                        tint = colors.cyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(
                onClick = onOpenPanaChart,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("header_pana_chart_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = stringResource(R.string.cd_pana_chart),
                    tint = colors.cyan
                )
            }
            IconButton(
                onClick = onOpenInfo,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("header_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = colors.textSecondary
                )
            }
        }

        // Subtle live Auto-Sync status bar with inline toggle switch
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.card.copy(alpha = 0.85f))
                .border(0.8.dp, colors.borderSubtle.copy(alpha = 0.8f), RoundedCornerShape(11.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(statusDotColor)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = autoSyncState.statusText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = if (autoSyncState.isPausedBlocked) colors.warning else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Switch(
                checked = autoSyncState.enabled,
                onCheckedChange = onToggleAutoSync,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.emerald,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.navy
                ),
                modifier = Modifier
                    .scale(0.75f)
                    .height(26.dp)
                    .testTag("header_auto_sync_switch")
            )
        }
    }
}

@Composable
fun LiveMarketStatusBadge(
    status: MarketLiveStatus,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    val (dotColor, bgColor, textColor) = when (status) {
        MarketLiveStatus.LIVE_OPEN -> Triple(colors.emerald, colors.emeraldSoft, colors.emerald)
        MarketLiveStatus.CLOSED_TODAY -> Triple(colors.danger, colors.dangerSoft, colors.danger)
        MarketLiveStatus.OFF_TODAY -> Triple(colors.textSecondary, colors.cardElevated, colors.textSecondary)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(0.8.dp, dotColor.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = status.badgeLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
fun PremiumBottomNavBar(
    activeTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    Surface(
        color = colors.navy.copy(alpha = 0.96f),
        border = BorderStroke(0.8.dp, colors.borderSubtle.copy(alpha = 0.8f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEach { tab ->
                val isSelected = tab == activeTab
                val (selectedIcon, unselectedIcon) = tabIcons(tab)
                val accentColor = if (isSelected) colors.electricBlue else colors.textMuted

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectTab(tab) }
                        .minimumInteractiveComponentSize()
                        .padding(vertical = 4.dp)
                        .testTag("nav_tab_${tab.route}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) colors.electricBlueSoft else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) colors.electricBlue.copy(alpha = 0.45f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) selectedIcon else unselectedIcon,
                            contentDescription = tab.label,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) colors.textPrimary else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun tabIcons(tab: AppTab): Pair<ImageVector, ImageVector> = when (tab) {
    AppTab.HOME -> Icons.Filled.Home to Icons.Outlined.Home
    AppTab.PREDICTIONS -> Icons.Filled.AutoGraph to Icons.Outlined.AutoGraph
    AppTab.ANALYTICS -> Icons.Filled.Analytics to Icons.Outlined.Analytics
    AppTab.MARKETS -> Icons.Filled.Storefront to Icons.Outlined.Storefront
    AppTab.DATA -> Icons.Filled.Storage to Icons.Outlined.Storage
}

@Composable
fun PanaCategoryBadge(
    category: PanaCategory,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    val (bg, fg) = when (category) {
        PanaCategory.SP -> colors.electricBlueSoft to colors.electricBlue
        PanaCategory.DP -> colors.neonPurpleSoft to colors.neonPurple
        PanaCategory.TP -> colors.cyanSoft to colors.cyan
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(0.8.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category.code,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fg
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .border(0.8.dp, color.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AnimatedMetricNumber(
    targetValue: Int,
    suffix: String = "",
    color: Color = DpBossTheme.colors.textPrimary,
    modifier: Modifier = Modifier
) {
    val animated by animateIntAsState(
        targetValue = targetValue,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "metric_counter"
    )
    Text(
        text = "$animated$suffix",
        style = MonoTextStyles.HeroNumber,
        color = color,
        maxLines = 1,
        modifier = modifier
    )
}

/**
 * Fast Searchable Market Selector with Favorites (★) pinning and quick filter tabs.
 */
@Composable
fun MarketSelectorStrip(
    markets: List<MarketCardSummary>,
    selectedMarket: String,
    onSelectMarket: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    if (markets.isEmpty()) return

    var searchQuery by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }

    val filtered = remember(markets, searchQuery, favoritesOnly) {
        markets
            .filter { item ->
                val favOk = !favoritesOnly || item.isFavorite
                val queryOk = searchQuery.isBlank() ||
                    item.market.name.contains(searchQuery.trim(), ignoreCase = true)
                favOk && queryOk
            }
            .sortedWith(
                compareByDescending<MarketCardSummary> { it.isFavorite }
                    .thenBy { it.market.name }
            )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Instant Search Bar + [ ★ Favorites ] / [ All ] Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search market...",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_clear),
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.electricBlue,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.card,
                    unfocusedContainerColor = colors.card
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("selector_market_search")
            )

            // Quick [ ★ ] toggle pill
            Surface(
                onClick = { favoritesOnly = !favoritesOnly },
                shape = RoundedCornerShape(12.dp),
                color = if (favoritesOnly) colors.warningSoft else colors.card,
                border = BorderStroke(
                    1.dp,
                    if (favoritesOnly) colors.warning else colors.borderSubtle
                ),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("selector_favorites_filter")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = if (favoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Filter Favorites",
                        tint = if (favoritesOnly) colors.warning else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (favoritesOnly) "★ Fav" else "All",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (favoritesOnly) colors.warning else colors.textSecondary
                    )
                }
            }
        }

        // Horizontal Pinned Market Chips with live status dot & star button
        if (filtered.isEmpty()) {
            Text(
                text = if (favoritesOnly) "No favorite markets pinned yet. Tap ★ on any market." else "No markets match \"$searchQuery\".",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtered.forEach { summary ->
                    val name = summary.market.name
                    val selected = name == selectedMarket
                    val statusDot = when (summary.scheduleEvaluation.status) {
                        MarketLiveStatus.LIVE_OPEN -> colors.emerald
                        MarketLiveStatus.CLOSED_TODAY -> colors.danger
                        MarketLiveStatus.OFF_TODAY -> colors.textSecondary
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.electricBlue else colors.card)
                            .border(
                                1.dp,
                                if (selected) colors.electricBlue else colors.borderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectMarket(name) }
                            .padding(start = 10.dp, end = 6.dp, top = 7.dp, bottom = 7.dp)
                            .testTag("market_chip_${name.lowercase().replace(" ", "_")}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusDot)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (selected) Color.White else colors.textSecondary,
                            maxLines = 1
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = if (summary.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (summary.isFavorite) "Unpin favorite" else "Pin favorite",
                            tint = when {
                                summary.isFavorite -> colors.warning
                                selected -> Color.White.copy(alpha = 0.75f)
                                else -> colors.textMuted
                            },
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { onToggleFavorite(name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditMarketScheduleDialog(
    marketName: String,
    initialSchedule: MarketScheduleConfig,
    onDismiss: () -> Unit,
    onSave: (MarketScheduleConfig) -> Unit
) {
    val colors = DpBossTheme.colors
    var openMinutes by remember(marketName) { mutableIntStateOf(initialSchedule.openTimeMinutes) }
    var closeMinutes by remember(marketName) { mutableIntStateOf(initialSchedule.closeTimeMinutes) }
    var offDays by remember(marketName) { mutableStateOf(initialSchedule.offDays) }

    val dayLabels = listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.navy,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.cyan)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Edit Market Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$marketName (IST • Asia/Kolkata)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TimeStepperRow(
                    label = "Open Time (IST)",
                    minutes = openMinutes,
                    accent = colors.electricBlue,
                    onAdjust = { delta ->
                        openMinutes = ((openMinutes + delta) % 1440 + 1440) % 1440
                    }
                )
                TimeStepperRow(
                    label = "Close Time (IST)",
                    minutes = closeMinutes,
                    accent = colors.cyan,
                    onAdjust = { delta ->
                        closeMinutes = ((closeMinutes + delta) % 1440 + 1440) % 1440
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Weekly Off Days (Tap to toggle)",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dayLabels.forEach { (dow, shortLabel) ->
                            val isOff = dow in offDays
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isOff) colors.dangerSoft else colors.cardElevated)
                                    .border(
                                        1.dp,
                                        if (isOff) colors.danger else colors.borderSubtle,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        offDays = if (isOff) offDays - dow else offDays + dow
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("schedule_off_day_$dow")
                            ) {
                                Text(
                                    text = shortLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOff) colors.danger else colors.textSecondary
                                )
                            }
                        }
                    }
                    Text(
                        text = if (offDays.isEmpty()) "Operates all 7 days (Mon–Sun)" else "Off: ${
                            dayLabels.filter { it.first in offDays }.joinToString(", ") { it.second }
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        MarketScheduleConfig(
                            openTimeMinutes = openMinutes,
                            closeTimeMinutes = closeMinutes,
                            offDays = offDays
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.electricBlue),
                modifier = Modifier.testTag("save_market_schedule_button")
            ) {
                Text("Save Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun TimeStepperRow(
    label: String,
    minutes: Int,
    accent: Color,
    onAdjust: (Int) -> Unit
) {
    val colors = DpBossTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.card)
            .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepChip("-1h") { onAdjust(-60) }
                StepChip("-5m") { onAdjust(-5) }
            }
            Text(
                text = MarketScheduleEngine.formatMinutes12h(minutes),
                style = MonoTextStyles.PanaCompact.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepChip("+5m") { onAdjust(5) }
                StepChip("+1h") { onAdjust(60) }
            }
        }
    }
}

@Composable
private fun StepChip(text: String, onClick: () -> Unit) {
    val colors = DpBossTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = colors.cardElevated,
        border = BorderStroke(0.8.dp, colors.borderSubtle)
    ) {
        Text(
            text = text,
            style = MonoTextStyles.MetricSmall,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun EmptyAnalyticsState(
    title: String = stringResource(R.string.empty_state_title),
    subtitle: String = stringResource(R.string.empty_state_subtitle),
    primaryActionLabel: String = stringResource(R.string.action_import_results),
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    GlassAnalyticsCard(
        glowAccent = colors.electricBlue,
        contentPadding = PaddingValues(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.electricBlueSoft, colors.navy)
                        )
                    )
                    .border(1.dp, colors.electricBlue.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.QueryStats,
                    contentDescription = null,
                    tint = colors.cyan,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onPrimaryAction,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.electricBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("empty_state_import_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = primaryActionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = onSecondaryAction,
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardElevated,
                    border = BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = secondaryActionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.cyan
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PanaDetailBottomSheet(
    pana: String,
    activeMarketName: String,
    marketHitCount: Int?,
    onDismiss: () -> Unit,
    onSelectRelatedPana: (String) -> Unit
) {
    val colors = DpBossTheme.colors
    val info = MatkaMathEngine.getPanaInfo(pana) ?: return
    val family = MatkaMathEngine.getFamilyForPana(pana)
    val cutPana = MatkaMathEngine.directCutPana(pana)
    val cutAnk = MatkaMathEngine.cutDigit(info.ank)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.navy,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = info.pana,
                    style = MonoTextStyles.HeroNumber,
                    color = colors.textPrimary
                )
                Spacer(Modifier.width(12.dp))
                PanaCategoryBadge(category = info.category)
                Spacer(Modifier.width(8.dp))
                StatusPill(label = "ANK ${info.ank}", color = colors.cyan)
                Spacer(Modifier.weight(1f))
                StatusPill(label = "CUT ANK $cutAnk", color = colors.warning)
            }

            Spacer(Modifier.height(14.dp))

            GlassAnalyticsCard(contentPadding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Digit Formula", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        Text(
                            text = "${info.d1} + ${info.d2} + ${info.d3} = ${info.d1 + info.d2 + info.d3} → Ank ${info.ank}",
                            style = MonoTextStyles.MetricSmall,
                            color = colors.textPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Direct Cut Pana", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        Text(
                            text = cutPana ?: "-",
                            style = MonoTextStyles.PanaCompact,
                            color = colors.neonPurple
                        )
                    }
                }
                if (activeMarketName.isNotBlank() && marketHitCount != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "$activeMarketName historical hits: $marketHitCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.emerald
                    )
                }
            }

            if (family != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${family.sizeTypeLabel} (${family.baseDigitsLabel})",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Key ${family.familyKey}",
                        style = MonoTextStyles.MetricSmall,
                        color = colors.textMuted
                    )
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    family.members.forEach { member ->
                        val isCurrent = member.pana == info.pana
                        val chipColor = when (member.category) {
                            PanaCategory.SP -> colors.electricBlue
                            PanaCategory.DP -> colors.neonPurple
                            PanaCategory.TP -> colors.cyan
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) chipColor else colors.cardElevated)
                                .border(1.dp, chipColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable { onSelectRelatedPana(member.pana) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = member.pana,
                                    style = MonoTextStyles.PanaCompact,
                                    color = if (isCurrent) Color.White else colors.textPrimary
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "(${member.ank})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) Color.White.copy(alpha = 0.85f) else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
