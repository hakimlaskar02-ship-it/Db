package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.math.MatkaMathEngine
import com.example.domain.math.PanaCategory
import com.example.domain.math.PanaValidationResult
import com.example.ui.components.GlassAnalyticsCard
import com.example.ui.components.PanaCategoryBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanaChartScreen(
    onBack: () -> Unit,
    onInspectPana: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val colors = DpBossTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PanaCategory?>(null) }
    var selectedAnk by remember { mutableStateOf<Int?>(null) }

    val cleanedQuery = searchQuery.filter { it.isDigit() }.take(3)
    val liveValidation = remember(cleanedQuery) {
        if (cleanedQuery.length == 3) MatkaMathEngine.validatePana(cleanedQuery) else null
    }

    val filteredByAnk = remember(cleanedQuery, selectedCategory, selectedAnk) {
        (0..9)
            .filter { ank -> selectedAnk == null || selectedAnk == ank }
            .mapNotNull { ank ->
                val list = MatkaMathEngine.panasByAnk[ank].orEmpty().filter { info ->
                    val catOk = selectedCategory == null || info.category == selectedCategory
                    val searchOk = cleanedQuery.isEmpty() || info.pana.contains(cleanedQuery)
                    catOk && searchOk
                }
                if (list.isEmpty()) null else ank to list
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Sub-screen AppBar
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
                    .testTag("pana_chart_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = colors.textPrimary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Complete 220 Pana Chart",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Mathematically generated • Order 1 < 2 < … < 9 < 0",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Counters + Live Validator Search Box
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChartSummaryChip("ALL", "220", colors.textPrimary, selectedCategory == null, Modifier.weight(1f)) {
                            selectedCategory = null
                        }
                        ChartSummaryChip("SP", "${MatkaMathEngine.singlePanas.size}", colors.electricBlue, selectedCategory == PanaCategory.SP, Modifier.weight(1f)) {
                            selectedCategory = PanaCategory.SP
                        }
                        ChartSummaryChip("DP", "${MatkaMathEngine.doublePanas.size}", colors.neonPurple, selectedCategory == PanaCategory.DP, Modifier.weight(1f)) {
                            selectedCategory = PanaCategory.DP
                        }
                        ChartSummaryChip("TP", "${MatkaMathEngine.triplePanas.size}", colors.cyan, selectedCategory == PanaCategory.TP, Modifier.weight(1f)) {
                            selectedCategory = PanaCategory.TP
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it.filter(Char::isDigit).take(3) },
                        placeholder = {
                            Text(
                                text = "Search or validate any 3-digit Pana (e.g., 179, 132)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textMuted
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.cyan)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_clear), tint = colors.textSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MonoTextStyles.PanaMedium.copy(color = colors.textPrimary),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.cyan,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = colors.card,
                            unfocusedContainerColor = colors.card
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pana_chart_search_input")
                    )

                    // Live Mathematical Validator Banner
                    when (liveValidation) {
                        is PanaValidationResult.Valid -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.emeraldSoft)
                                    .border(0.8.dp, colors.emerald.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                    .clickable { onInspectPana(liveValidation.info.pana) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Valid ${liveValidation.info.category.fullLabel} • Ank ${liveValidation.info.ank} • ${liveValidation.family.sizeTypeLabel}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = colors.emerald
                                    )
                                }
                                PanaCategoryBadge(category = liveValidation.info.category)
                            }
                        }
                        is PanaValidationResult.Invalid -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.dangerSoft)
                                    .border(0.8.dp, colors.danger.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = liveValidation.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.danger
                                )
                            }
                        }
                        null -> Unit
                    }

                    // Ank Filter Row (All, 0..9)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val allSelected = selectedAnk == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (allSelected) colors.electricBlue else colors.card)
                                .border(0.8.dp, if (allSelected) colors.electricBlue else colors.borderSubtle, RoundedCornerShape(10.dp))
                                .clickable { selectedAnk = null }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text("All Anks", style = MaterialTheme.typography.labelMedium, color = if (allSelected) Color.White else colors.textSecondary)
                        }
                        for (ank in 0..9) {
                            val isSel = selectedAnk == ank
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) colors.electricBlue else colors.card)
                                    .border(0.8.dp, if (isSel) colors.electricBlue else colors.borderSubtle, RoundedCornerShape(10.dp))
                                    .clickable { selectedAnk = ank }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text("Ank $ank", style = MonoTextStyles.MetricSmall, color = if (isSel) Color.White else colors.textSecondary)
                            }
                        }
                    }
                }
            }

            // Grouped Ank Cards
            items(filteredByAnk, key = { it.first }) { (ank, panas) ->
                GlassAnalyticsCard(
                    contentPadding = PaddingValues(14.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(label = "ANK $ank", color = colors.cyan)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Cut ${MatkaMathEngine.cutDigit(ank)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        }
                        Text(
                            text = "${panas.size} Panas",
                            style = MonoTextStyles.MetricSmall,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        panas.forEach { info ->
                            val tileColor = when (info.category) {
                                PanaCategory.SP -> colors.electricBlue
                                PanaCategory.DP -> colors.neonPurple
                                PanaCategory.TP -> colors.cyan
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(tileColor.copy(alpha = 0.14f))
                                    .border(0.9.dp, tileColor.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
                                    .clickable { onInspectPana(info.pana) }
                                    .padding(horizontal = 11.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = info.pana,
                                    style = MonoTextStyles.PanaCompact.copy(fontWeight = FontWeight.Bold),
                                    color = tileColor
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
private fun ChartSummaryChip(
    label: String,
    count: String,
    accent: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = DpBossTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else colors.card)
            .border(
                width = if (selected) 1.3.dp else 0.8.dp,
                color = if (selected) accent else colors.borderSubtle,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = count,
            style = MonoTextStyles.PanaMedium,
            color = if (selected) Color.White else accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else colors.textSecondary
        )
    }
}
