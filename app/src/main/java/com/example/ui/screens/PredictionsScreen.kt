package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.analytics.MarketAnalyticsSnapshot
import com.example.domain.analytics.OpenToClosePredictionResult
import com.example.domain.analytics.RankedAnkSuggestion
import com.example.domain.analytics.RankedJodiSuggestion
import com.example.domain.analytics.RankedPanaSuggestion
import com.example.domain.math.PanaValidationResult
import com.example.ui.components.EmptyAnalyticsState
import com.example.ui.components.GlassAnalyticsCard
import com.example.ui.components.LiveMarketStatusBadge
import com.example.ui.components.MarketSelectorStrip
import com.example.ui.components.PanaCategoryBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles
import com.example.viewmodel.MarketCardSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PredictionsScreen(
    marketSummaries: List<MarketCardSummary>,
    selectedMarket: String,
    activeMarketSummary: MarketCardSummary?,
    snapshot: MarketAnalyticsSnapshot?,
    openPanaInput: String,
    openPanaValidation: PanaValidationResult?,
    openToCloseResult: OpenToClosePredictionResult?,
    onSelectMarket: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onEditSchedule: (String) -> Unit,
    onUpdateOpenPanaInput: (String) -> Unit,
    onApplyCanonicalPana: (String) -> Unit,
    onPredictOpenToClose: () -> Unit,
    onInspectPana: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    var detailSectionTab by remember { mutableIntStateOf(0) } // 0=Open, 1=Close, 2=Jodi

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                Spacer(Modifier.height(12.dp))
                EmptyAnalyticsState(
                    title = "No Historical Data Available",
                    subtitle = "Import real historical results for a market to unlock strict 4 SP + 1 DP Top-5 scoring and Live Open-to-Close predictions.",
                    onPrimaryAction = onNavigateToImport
                )
            }
            return@LazyColumn
        }

        // 1. Top Analysis Header Banner with Live IST Status & Timing
        item {
            GlassAnalyticsCard(
                glowAccent = colors.electricBlue,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = snapshot.marketName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (activeMarketSummary != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = colors.cyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${activeMarketSummary.schedule.formattedTimingRange} • ${activeMarketSummary.schedule.offDaysSummary}",
                                    style = MonoTextStyles.MetricSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { onEditSchedule(snapshot.marketName) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("pred_edit_schedule_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Edit market timing and off-days",
                            tint = colors.cyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveMarketStatusBadge(status = snapshot.scheduleEvaluation.status)
                    StatusPill(label = "${snapshot.totalRecords} DRAWS", color = colors.emerald)
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Target Draw: ${snapshot.nextDrawDayLabel}, ${snapshot.nextDrawDateIso} • ${snapshot.scheduleEvaluation.istTimeLabel}",
                    style = MonoTextStyles.MetricSmall,
                    color = colors.cyan
                )
                Text(
                    text = "Coverage: ${snapshot.dateRangeLabel} • Analyzed ${formatTimeShort(snapshot.analyzedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )

                // Smart Next-Active-Day Shift Notice when Market is Off or Closed Today
                snapshot.scheduleEvaluation.shiftBannerMessage?.let { shiftNote ->
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(colors.warningSoft)
                            .border(0.9.dp, colors.warning.copy(alpha = 0.6f), RoundedCornerShape(11.dp))
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                            .testTag("smart_next_day_shift_banner"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventRepeat,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = shiftNote,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.warning
                        )
                    }
                }
            }
        }

        // 2. LIVE OPEN-TO-CLOSE ANALYSIS (Strict 4 SP + 1 DP)
        item {
            LiveOpenToCloseCard(
                openPanaInput = openPanaInput,
                validation = openPanaValidation,
                result = openToCloseResult,
                onInputChange = onUpdateOpenPanaInput,
                onApplyCanonical = onApplyCanonicalPana,
                onPredict = onPredictOpenToClose,
                onInspectPana = onInspectPana,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // 3. TOP 5 NEXT GAME PANAS (Strict 4 SP · 1 DP Quota)
        item {
            GlassAnalyticsCard(
                glowAccent = colors.neonPurple,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = colors.neonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "TOP 5 NEXT GAME PANAS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                    }
                    StatusPill(
                        label = "4 SP · 1 DP",
                        color = colors.electricBlue,
                        modifier = Modifier.testTag("top5_quota_badge")
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Top 4 SP + #1 DP ranked by IDX • Weighted for ${snapshot.nextDrawDayLabel} trend + frequency + gap",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    snapshot.top5NextGamePanas.forEach { item ->
                        Top5PanaRowCard(
                            item = item,
                            onClick = { onInspectPana(item.pana) }
                        )
                    }
                }
            }
        }

        // 4. Deep Breakdown Tabs: Open Analysis | Close Analysis | Jodi Analysis
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.navy)
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Open Analysis", "Close Analysis", "Jodi Analysis").forEachIndexed { idx, label ->
                    val selected = detailSectionTab == idx
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colors.electricBlue else Color.Transparent)
                            .clickable { detailSectionTab = idx }
                            .padding(vertical = 10.dp)
                            .testTag("pred_subtab_$idx")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) Color.White else colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        item {
            when (detailSectionTab) {
                0 -> OpenCloseAnalysisBlock(
                    titlePrefix = "OPEN",
                    ankSuggestions = snapshot.openAnkSuggestions,
                    spSuggestions = snapshot.openSpSuggestions,
                    dpSuggestions = snapshot.openDpSuggestions,
                    onInspectPana = onInspectPana,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                1 -> if (snapshot.closeAnkSuggestions.isEmpty()) {
                    GlassAnalyticsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "No Close results recorded yet for ${snapshot.marketName}. Import complete Open–Jodi–Close rows to unlock Close analytics.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                } else {
                    OpenCloseAnalysisBlock(
                        titlePrefix = "CLOSE",
                        ankSuggestions = snapshot.closeAnkSuggestions,
                        spSuggestions = snapshot.closeSpSuggestions,
                        dpSuggestions = snapshot.closeDpSuggestions,
                        onInspectPana = onInspectPana,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                else -> JodiAnalysisBlock(
                    jodis = snapshot.jodiSuggestions,
                    marketName = snapshot.marketName,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveOpenToCloseCard(
    openPanaInput: String,
    validation: PanaValidationResult?,
    result: OpenToClosePredictionResult?,
    onInputChange: (String) -> Unit,
    onApplyCanonical: (String) -> Unit,
    onPredict: () -> Unit,
    onInspectPana: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors

    GlassAnalyticsCard(
        glowAccent = colors.cyan,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = colors.cyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.open_to_close_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
            }
            StatusPill(
                label = "4 SP · 1 DP",
                color = colors.cyan,
                modifier = Modifier.testTag("o2c_quota_badge")
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = openPanaInput,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.open_pana_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )
            },
            singleLine = true,
            textStyle = MonoTextStyles.PanaLarge.copy(color = colors.textPrimary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onPredict() }),
            trailingIcon = {
                if (openPanaInput.isNotEmpty()) {
                    IconButton(onClick = { onInputChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_clear),
                            tint = colors.textSecondary
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.cyan,
                unfocusedBorderColor = colors.borderSubtle,
                focusedContainerColor = colors.navy,
                unfocusedContainerColor = colors.navy,
                cursorColor = colors.cyan
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_pana_input")
        )

        when (validation) {
            is PanaValidationResult.Valid -> {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.emeraldSoft)
                        .border(0.8.dp, colors.emerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colors.emerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "${validation.info.pana} → Ank ${validation.info.ank} • ${validation.family.sizeTypeLabel}",
                            style = MonoTextStyles.MetricSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.emerald
                        )
                    }
                    PanaCategoryBadge(category = validation.info.category)
                }
            }
            is PanaValidationResult.Invalid -> {
                Spacer(Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.dangerSoft)
                        .border(0.8.dp, colors.danger.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = validation.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger
                        )
                    }
                    if (validation.suggestedCanonical != null) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            onClick = { onApplyCanonical(validation.suggestedCanonical) },
                            shape = RoundedCornerShape(8.dp),
                            color = colors.cardElevated,
                            border = BorderStroke(1.dp, colors.cyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "Use valid Matka Pana: ${validation.suggestedCanonical}",
                                style = MonoTextStyles.MetricSmall,
                                color = colors.cyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
            null -> Unit
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onPredict,
            enabled = validation is PanaValidationResult.Valid,
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.electricBlue,
                disabledContainerColor = colors.cardElevated
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("predict_5_close_button")
        ) {
            Text(
                text = stringResource(R.string.predict_5_close_action),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (validation is PanaValidationResult.Valid) Color.White else colors.textMuted
            )
        }

        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn() + expandVertically()
        ) {
            if (result != null) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = result.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))

                    if (result.top5ClosePanas.isEmpty()) {
                        Text(
                            text = "Import complete Open→Close history to generate Close Pana predictions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.warning
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result.top5ClosePanas.forEach { closeItem ->
                                Top5PanaRowCard(
                                    item = closeItem,
                                    onClick = { onInspectPana(closeItem.pana) }
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
private fun Top5PanaRowCard(
    item: RankedPanaSuggestion,
    onClick: () -> Unit
) {
    val colors = DpBossTheme.colors
    val rankAccent = when (item.rank) {
        1 -> colors.cyan
        2 -> colors.electricBlue
        3 -> colors.neonPurple
        else -> colors.textSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.navy)
            .border(
                width = if (item.rank == 1) 1.2.dp else 0.8.dp,
                color = if (item.rank == 1) colors.cyan.copy(alpha = 0.55f) else colors.borderSubtle,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
            .testTag("top5_pana_${item.pana}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(rankAccent.copy(alpha = 0.16f))
                .border(1.dp, rankAccent.copy(alpha = 0.5f), CircleShape)
        ) {
            Text(
                text = "#${item.rank}",
                style = MonoTextStyles.MetricSmall.copy(fontWeight = FontWeight.Bold),
                color = rankAccent
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.pana,
                    style = MonoTextStyles.PanaLarge,
                    color = colors.textPrimary
                )
                Spacer(Modifier.width(8.dp))
                PanaCategoryBadge(category = item.category)
                Spacer(Modifier.width(6.dp))
                StatusPill(label = "ANK ${item.ank}", color = colors.emerald)
                if (item.predictedJodi != null) {
                    Spacer(Modifier.width(6.dp))
                    StatusPill(label = "JODI ${item.predictedJodi}", color = colors.warning)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${item.statisticalScore}",
                style = MonoTextStyles.PanaMedium.copy(fontWeight = FontWeight.Bold),
                color = rankAccent
            )
            Text(
                text = "IDX",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = colors.textMuted
            )
        }
    }
}

@Composable
private fun OpenCloseAnalysisBlock(
    titlePrefix: String,
    ankSuggestions: List<RankedAnkSuggestion>,
    spSuggestions: List<RankedPanaSuggestion>,
    dpSuggestions: List<RankedPanaSuggestion>,
    onInspectPana: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GlassAnalyticsCard(glowAccent = colors.electricBlue, contentPadding = PaddingValues(16.dp)) {
            Text(
                text = "$titlePrefix ANK SUGGESTIONS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = colors.textPrimary
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ankSuggestions.take(5).forEach { ank ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(colors.electricBlue.copy(alpha = 0.35f), colors.navy)
                                    )
                                )
                                .border(1.5.dp, colors.electricBlue, CircleShape)
                        ) {
                            Text(
                                text = ank.ank.toString(),
                                style = MonoTextStyles.PanaLarge,
                                color = colors.textPrimary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Idx ${ank.statisticalScore}",
                            style = MonoTextStyles.MetricSmall.copy(fontSize = 11.sp),
                            color = colors.cyan
                        )
                        Text(
                            text = "×${ank.historicalHits}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ankSuggestions.take(5).forEach { ank ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.navy.copy(alpha = 0.7f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "#${ank.rank} Ank ${ank.ank} (${String.format("%.1f", ank.hitRatePercent)}%)",
                            style = MonoTextStyles.MetricSmall,
                            color = colors.textPrimary
                        )
                        Text(
                            text = ank.supportingNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        PanaCategorySuggestionCard(
            title = "$titlePrefix SINGLE PANA (SP)",
            accent = colors.electricBlue,
            items = spSuggestions,
            onInspectPana = onInspectPana
        )
        PanaCategorySuggestionCard(
            title = "$titlePrefix DOUBLE PANA (DP)",
            accent = colors.neonPurple,
            items = dpSuggestions,
            onInspectPana = onInspectPana
        )
    }
}

@Composable
private fun PanaCategorySuggestionCard(
    title: String,
    accent: Color,
    items: List<RankedPanaSuggestion>,
    onInspectPana: (String) -> Unit
) {
    val colors = DpBossTheme.colors
    GlassAnalyticsCard(contentPadding = PaddingValues(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = "Tap any Pana for family",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.navy)
                                .border(0.8.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                .clickable { onInspectPana(item.pana) }
                                .padding(vertical = 10.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = item.pana,
                                style = MonoTextStyles.PanaMedium,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Ank ${item.ank} • ${item.statisticalScore}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun JodiAnalysisBlock(
    jodis: List<RankedJodiSuggestion>,
    marketName: String,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    GlassAnalyticsCard(
        glowAccent = colors.warning,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
    ) {
        Text(
            text = "JODI STATISTICAL RANKINGS",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Derived from real historical Jodi hits and Open→Close transition matrix in $marketName.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(12.dp))

        if (jodis.isEmpty()) {
            Text(
                text = "Import complete Open–Jodi–Close results to compute Jodi rankings.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                jodis.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.navy)
                            .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${item.rank}",
                            style = MonoTextStyles.MetricSmall,
                            color = colors.textMuted,
                            modifier = Modifier.width(26.dp)
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.warningSoft)
                                .border(1.dp, colors.warning.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.jodi,
                                style = MonoTextStyles.PanaMedium,
                                color = colors.warning
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.supportingNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Gap ${item.currentGap} draws",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                        Text(
                            text = "IDX ${item.statisticalScore}",
                            style = MonoTextStyles.MetricSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.cyan
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeShort(millis: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.US).format(Date(millis))
}
