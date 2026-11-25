package com.example.ui.components

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.domain.analytics.OpenToClosePredictionResult
import com.example.domain.analytics.RankedPanaSuggestion
import com.example.domain.analytics.SuggestedCloseDigit
import com.example.domain.math.PanaValidationResult
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles

/**
 * Rule-based, data-driven Open-to-Close Analyzer card shared across Home & Predictions.
 *
 * Weights (0-100%, zero random numbers):
 * - 40% Historical Open→Close Correlation
 * - 25% Cut / Mirror & Family Logic (0↔5, 1↔6, 2↔7, 3↔8, 4↔9)
 * - 20% Hot & Due (Cold) Trend (last 15-30 draws)
 * - 15% Jodi Difference & Total Pattern
 */
@Composable
fun OpenToCloseAnalyzerCard(
    openPanaInput: String,
    validation: PanaValidationResult?,
    result: OpenToClosePredictionResult?,
    activeMarketLabel: String? = null,
    showFullClosePanas: Boolean = true,
    onInputChange: (String) -> Unit,
    onApplyCanonical: (String) -> Unit,
    onAnalyzeClose: () -> Unit,
    onClear: () -> Unit,
    onQuickPasteChart: (marketName: String, rawText: String) -> Unit,
    onEditHistory: () -> Unit,
    onInspectPana: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    var showQuickPasteDialog by remember { mutableStateOf(false) }

    GlassAnalyticsCard(
        glowAccent = colors.cyan,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier.testTag("open_to_close_analyzer_card")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.cyanSoft)
                        .border(0.8.dp, colors.cyan.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = colors.cyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.open_to_close_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (!activeMarketLabel.isNullOrBlank()) {
                            "Market: $activeMarketLabel • Real Chart Engine"
                        } else {
                            "40% Corr · 25% Cut · 20% Trend · 15% Jodi"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(
                label = "4 SP · 1 DP",
                color = colors.cyan,
                modifier = Modifier.testTag("o2c_quota_badge")
            )
        }

        Spacer(Modifier.height(10.dp))

        // Historical Chart Data Controls: [ Paste / Update Chart Data ] + [ Edit History ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { showQuickPasteDialog = true },
                shape = RoundedCornerShape(10.dp),
                color = colors.electricBlueSoft,
                border = BorderStroke(0.9.dp, colors.electricBlue.copy(alpha = 0.55f)),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("o2c_paste_update_chart_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = null,
                        tint = colors.cyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Paste / Update Chart Data",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                onClick = onEditHistory,
                shape = RoundedCornerShape(10.dp),
                color = colors.navy,
                border = BorderStroke(0.9.dp, colors.borderSubtle),
                modifier = Modifier
                    .height(40.dp)
                    .testTag("o2c_edit_history_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = "Edit stored historical records",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Edit Chart",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 3-Digit Open Panna Input
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
            keyboardActions = KeyboardActions(onDone = { onAnalyzeClose() }),
            trailingIcon = {
                if (openPanaInput.isNotEmpty()) {
                    IconButton(onClick = onClear) {
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

        // Real-time Open Single Digit Preview & Matka Validation
        when (validation) {
            is PanaValidationResult.Valid -> {
                val info = validation.info
                val digitSum = info.d1 + info.d2 + info.d3
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.emeraldSoft)
                        .border(0.9.dp, colors.emerald.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.emerald.copy(alpha = 0.22f))
                                .border(1.2.dp, colors.emerald, CircleShape)
                        ) {
                            Text(
                                text = info.ank.toString(),
                                style = MonoTextStyles.PanaCompact.copy(fontWeight = FontWeight.Bold),
                                color = colors.emerald
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Open Single Digit: ${info.ank}",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.emerald
                            )
                            Text(
                                text = "${info.d1} + ${info.d2} + ${info.d3} = $digitSum → ${info.ank} • ${validation.family.sizeTypeLabel}",
                                style = MonoTextStyles.MetricSmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    PanaCategoryBadge(category = info.category)
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

        // Action Buttons Row: [ Analyze Close ] + [ Clear ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAnalyzeClose,
                enabled = validation is PanaValidationResult.Valid,
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.electricBlue,
                    disabledContainerColor = colors.cardElevated
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("predict_5_close_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = if (validation is PanaValidationResult.Valid) Color.White else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.analyze_close_action),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (validation is PanaValidationResult.Valid) Color.White else colors.textMuted
                )
            }

            OutlinedButton(
                onClick = onClear,
                enabled = openPanaInput.isNotEmpty() || result != null,
                shape = RoundedCornerShape(13.dp),
                border = BorderStroke(
                    1.dp,
                    if (openPanaInput.isNotEmpty() || result != null) colors.borderSubtle else colors.borderSubtle.copy(alpha = 0.4f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = colors.navy,
                    contentColor = colors.textPrimary,
                    disabledContentColor = colors.textMuted
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("clear_open_pana_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = stringResource(R.string.cd_clear),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.clear_action),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Animated Real-Data Analysis Output
        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn() + expandVertically()
        ) {
            if (result != null) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // Open Summary + Data Source Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.navy)
                            .border(0.8.dp, colors.cyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = colors.cyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = "Open ${result.openPana} → Single ${result.openAnk} • Cut ${result.cutAnk}",
                                style = MonoTextStyles.MetricSmall.copy(fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                        }
                        StatusPill(
                            label = if (result.isPatternOnlyMode) "NO HISTORY" else "${result.totalAnalyzedCompleteDraws} DRAWS",
                            color = if (result.isPatternOnlyMode) colors.warning else colors.emerald
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = result.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.isPatternOnlyMode) colors.warning else colors.textSecondary
                    )

                    // Transparent 4-Factor Weight Legend
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Weights: 40% Open→Close History • 25% Cut/Family • 20% Hot/Due • 15% Jodi Diff/Total",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.textMuted
                    )

                    // TOP 5 SUGGESTED CLOSE NUMBERS (Single Digits 0-9)
                    if (result.top5CloseDigits.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOP 5 SUGGESTED CLOSE NUMBERS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.7.sp
                                ),
                                color = colors.cyan
                            )
                            Text(
                                text = "Weighted Score %",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Highlighted 5-Digit Neon Badge Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            result.top5CloseDigits.forEach { item ->
                                CloseDigitHighlightBadge(
                                    item = item,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Detailed Transparent Rows with 1-Line Reason + 4-Factor Breakdown + 2 Close Panas
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result.top5CloseDigits.forEach { item ->
                                CloseDigitDetailRow(
                                    item = item,
                                    onInspectPana = onInspectPana
                                )
                            }
                        }
                    }

                    // Strict 4 SP + 1 DP Close Panas Section
                    if (showFullClosePanas && result.top5ClosePanas.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOP 5 CLOSE PANAS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.7.sp
                                ),
                                color = colors.neonPurple
                            )
                            Text(
                                text = "Strict 4 SP + 1 DP",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            result.top5ClosePanas.forEach { closeItem ->
                                AnalyzerClosePanaRow(
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

    if (showQuickPasteDialog) {
        QuickChartPasteDialog(
            initialMarket = activeMarketLabel?.ifBlank { "KALYAN" } ?: "KALYAN",
            onDismiss = { showQuickPasteDialog = false },
            onSavePaste = { market, rawText ->
                onQuickPasteChart(market, rawText)
                showQuickPasteDialog = false
            },
            onOpenEditHistory = {
                showQuickPasteDialog = false
                onEditHistory()
            }
        )
    }
}

@Composable
private fun QuickChartPasteDialog(
    initialMarket: String,
    onDismiss: () -> Unit,
    onSavePaste: (marketName: String, rawText: String) -> Unit,
    onOpenEditHistory: () -> Unit
) {
    val colors = DpBossTheme.colors
    var marketText by remember(initialMarket) { mutableStateOf(initialMarket) }
    var chartText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.navy,
        title = {
            Column {
                Text("Paste / Update Chart Data", color = colors.textPrimary)
                Text(
                    text = "Real records power 40% Correlation, 20% Hot/Due & 15% Jodi rules",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = marketText,
                    onValueChange = { marketText = it.uppercase() },
                    label = { Text("Market Name", color = colors.textSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.cyan,
                        unfocusedBorderColor = colors.borderSubtle,
                        focusedContainerColor = colors.card,
                        unfocusedContainerColor = colors.card,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chartText,
                    onValueChange = { chartText = it },
                    label = { Text("Paste Results (Date, Open, Jodi, Close)", color = colors.textSecondary) },
                    placeholder = {
                        Text(
                            text = "10/02/2025 128-16-259\n11/02/2025 179-74-248\nor CSV / DPBoss panel chart",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    },
                    minLines = 5,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
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
                        .testTag("quick_paste_chart_input")
                )
                TextButton(
                    onClick = onOpenEditHistory,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Or edit existing records in History →", color = colors.cyan)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSavePaste(marketText, chartText) },
                enabled = chartText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.electricBlue),
                modifier = Modifier.testTag("quick_paste_save_button")
            ) {
                Text("Save & Analyze")
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
private fun CloseDigitHighlightBadge(
    item: SuggestedCloseDigit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    val accent = when (item.rank) {
        1 -> colors.cyan
        2 -> colors.emerald
        3 -> colors.electricBlue
        4 -> colors.neonPurple
        else -> colors.warning
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.22f), colors.navy)
                )
            )
            .border(
                width = if (item.rank == 1) 1.4.dp else 0.9.dp,
                color = accent.copy(alpha = if (item.rank == 1) 0.9f else 0.55f),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("close_digit_badge_${item.closeDigit}")
    ) {
        Text(
            text = "#${item.rank}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = accent
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.closeDigit.toString(),
            style = MonoTextStyles.HeroNumber.copy(fontSize = 24.sp),
            color = colors.textPrimary
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${item.confidencePercent}%",
                style = MonoTextStyles.MetricSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp
                ),
                color = accent
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CloseDigitDetailRow(
    item: SuggestedCloseDigit,
    onInspectPana: (String) -> Unit
) {
    val colors = DpBossTheme.colors
    val accent = when (item.rank) {
        1 -> colors.cyan
        2 -> colors.emerald
        3 -> colors.electricBlue
        4 -> colors.neonPurple
        else -> colors.warning
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(colors.navy)
            .border(0.8.dp, colors.borderSubtle, RoundedCornerShape(13.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("close_digit_detail_${item.closeDigit}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f))
                        .border(1.dp, accent.copy(alpha = 0.7f), CircleShape)
                ) {
                    Text(
                        text = item.closeDigit.toString(),
                        style = MonoTextStyles.PanaCompact.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${item.rank} Close ${item.closeDigit}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusPill(
                            label = "JODI ${item.jodi}",
                            color = colors.emerald
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.patternReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.confidencePercent}%",
                    style = MonoTextStyles.PanaCompact.copy(fontWeight = FontWeight.Bold),
                    color = accent
                )
                Text(
                    text = "Score",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = colors.textMuted
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (item.confidencePercent / 100f).coerceIn(0f, 1f) },
            color = accent,
            trackColor = colors.cardElevated,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Corr ${item.correlationScorePct}/40 • Cut/Fam ${item.cutFamilyScorePct}/25 • Hot/Due ${item.hotDueScorePct}/20 • Jodi ${item.jodiPatternScorePct}/15",
            style = MonoTextStyles.MetricSmall.copy(fontSize = 10.sp),
            color = colors.textMuted
        )

        if (item.matchingClosePanas.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Close Pannas:",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                item.matchingClosePanas.forEach { pana ->
                    Surface(
                        onClick = { onInspectPana(pana) },
                        shape = RoundedCornerShape(8.dp),
                        color = colors.cardElevated,
                        border = BorderStroke(0.8.dp, accent.copy(alpha = 0.55f)),
                        modifier = Modifier.testTag("close_digit_pana_chip_$pana")
                    ) {
                        Text(
                            text = pana,
                            style = MonoTextStyles.MetricSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyzerClosePanaRow(
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
            .clip(RoundedCornerShape(13.dp))
            .background(colors.navy)
            .border(
                width = if (item.rank == 1) 1.1.dp else 0.8.dp,
                color = if (item.rank == 1) colors.cyan.copy(alpha = 0.55f) else colors.borderSubtle,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("top5_pana_${item.pana}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
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
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.pana,
                    style = MonoTextStyles.PanaMedium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.width(7.dp))
                PanaCategoryBadge(category = item.category)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Ank ${item.ank}",
                    style = MonoTextStyles.MetricSmall,
                    color = colors.cyan
                )
                if (item.predictedJodi != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "• Jodi ${item.predictedJodi}",
                        style = MonoTextStyles.MetricSmall,
                        color = colors.emerald
                    )
                }
            }
            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${item.statisticalScore}%",
            style = MonoTextStyles.PanaCompact.copy(fontWeight = FontWeight.Bold),
            color = rankAccent
        )
    }
}
