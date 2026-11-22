package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.OverdueAnkStat
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles
import kotlin.math.max

data class BarDatum(
    val label: String,
    val value: Int,
    val subtitle: String = ""
)

@Composable
fun InteractiveGlowingBarChart(
    items: List<BarDatum>,
    accentStart: Color,
    accentEnd: Color,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    if (items.isEmpty()) return

    var selectedIndex by remember(items) { mutableIntStateOf(0) }
    var animateIn by remember(items) { mutableStateOf(false) }
    LaunchedEffect(items) { animateIn = true }

    val progress by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "bar_chart_reveal"
    )

    val maxVal = items.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
    val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val selected = items[safeIndex]

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip bar for selected item
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardElevated)
                .border(0.8.dp, accentStart.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Selected: ${selected.label}",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            Text(
                text = buildString {
                    append("${selected.value} hits")
                    if (selected.subtitle.isNotBlank()) append(" • ${selected.subtitle}")
                },
                style = MonoTextStyles.MetricSmall,
                color = accentEnd
            )
        }

        Spacer(Modifier.height(14.dp))

        // Bars + clickable columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEachIndexed { idx, datum ->
                val isSelected = idx == safeIndex
                val fraction = (datum.value.toFloat() / maxVal.toFloat()).coerceIn(0.06f, 1f) * progress

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIndex = idx }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = datum.value.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = if (isSelected) accentEnd else colors.textMuted,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .height(112.dp)
                        ) {
                            val barHeight = max(8f, size.height * fraction)
                            val topY = size.height - barHeight
                            // Background track
                            drawRoundRect(
                                color = colors.borderSubtle.copy(alpha = 0.35f),
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                            // Active bar
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = if (isSelected) {
                                        listOf(accentEnd, accentStart)
                                    } else {
                                        listOf(accentStart.copy(alpha = 0.72f), accentStart.copy(alpha = 0.28f))
                                    },
                                    startY = topY,
                                    endY = size.height
                                ),
                                topLeft = Offset(0f, topY),
                                size = Size(size.width, barHeight),
                                cornerRadius = CornerRadius(10f, 10f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = datum.label,
                        style = MonoTextStyles.MetricSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun OverdueGapComparisonList(
    stats: List<OverdueAnkStat>,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    if (stats.isEmpty()) return

    val maxGap = stats.maxOfOrNull { max(it.currentGap, it.longestGap) }?.coerceAtLeast(1) ?: 1

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.sortedByDescending { it.currentGap }.forEach { stat ->
            val currentRatio = (stat.currentGap.toFloat() / maxGap.toFloat()).coerceIn(0f, 1f)
            val avgRatio = (stat.averageGap.toFloat() / maxGap.toFloat()).coerceIn(0f, 1f)
            val isHotOverdue = stat.currentGap > stat.averageGap && stat.currentGap >= 4

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardElevated.copy(alpha = 0.7f))
                    .border(
                        0.8.dp,
                        if (isHotOverdue) colors.warning.copy(alpha = 0.45f) else colors.borderSubtle,
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isHotOverdue) colors.warningSoft else colors.electricBlueSoft)
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ANK ${stat.ank}",
                                style = MonoTextStyles.MetricSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isHotOverdue) colors.warning else colors.electricBlue
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Gap: ${stat.currentGap} draws",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                    }
                    Text(
                        text = "Avg ${String.format("%.1f", stat.averageGap)} • Max ${stat.longestGap}",
                        style = MonoTextStyles.MetricSmall,
                        color = colors.textSecondary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    drawRoundRect(
                        color = colors.borderSubtle.copy(alpha = 0.45f),
                        size = size,
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = colors.cyan.copy(alpha = 0.35f),
                        size = Size(size.width * avgRatio, size.height),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            if (isHotOverdue) listOf(colors.warning, colors.danger)
                            else listOf(colors.electricBlue, colors.cyan)
                        ),
                        size = Size(max(6f, size.width * currentRatio), size.height),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }
    }
}

@Composable
fun OpenCloseHeatmapMatrix(
    matrix: List<List<Int>>,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    if (matrix.size != 10) return

    val maxCell = matrix.maxOfOrNull { row -> row.maxOrNull() ?: 0 }?.coerceAtLeast(1) ?: 1
    var selectedPair by remember(matrix) { mutableStateOf(0 to 0) }
    val selOpen = selectedPair.first
    val selClose = selectedPair.second
    val selCount = matrix.getOrNull(selOpen)?.getOrNull(selClose) ?: 0
    val rowTotal = matrix.getOrNull(selOpen)?.sum()?.coerceAtLeast(1) ?: 1
    val pct = (selCount * 100.0) / rowTotal.toDouble()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardElevated)
                .border(0.8.dp, colors.neonPurple.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Open Ank $selOpen → Close Ank $selClose",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textPrimary
            )
            Text(
                text = "$selCount hits (${String.format("%.1f", pct)}%)",
                style = MonoTextStyles.MetricSmall,
                color = colors.cyan
            )
        }
        Spacer(Modifier.height(10.dp))

        // Column header (Close Ank 0..9)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "O\\C",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(24.dp)
            )
            for (c in 0..9) {
                Text(
                    text = c.toString(),
                    style = MonoTextStyles.MetricSmall.copy(fontSize = 10.sp),
                    color = colors.cyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        for (o in 0..9) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = o.toString(),
                    style = MonoTextStyles.MetricSmall.copy(fontSize = 10.sp),
                    color = colors.electricBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(24.dp)
                )
                for (c in 0..9) {
                    val value = matrix[o][c]
                    val intensity = (value.toFloat() / maxCell.toFloat()).coerceIn(0f, 1f)
                    val isSelected = o == selOpen && c == selClose
                    val cellBg = when {
                        value == 0 -> colors.navy
                        intensity > 0.7f -> colors.electricBlue.copy(alpha = 0.85f)
                        intensity > 0.35f -> colors.neonPurple.copy(alpha = 0.55f)
                        else -> colors.electricBlue.copy(alpha = 0.25f)
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 1.5.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(cellBg)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) colors.cyan else colors.borderSubtle.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(5.dp)
                            )
                            .clickable { selectedPair = o to c }
                    ) {
                        if (value > 0) {
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
