package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.parser.DpBossTextParser
import com.example.data.parser.ParsePreviewReport
import com.example.data.parser.ParsedRowStatus
import com.example.domain.math.MatkaMathEngine
import com.example.domain.math.PanaValidationResult
import com.example.ui.components.GlassAnalyticsCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.DpBossTheme
import com.example.ui.theme.MonoTextStyles

@Composable
fun DataScreen(
    isSyncing: Boolean,
    lastSyncStatus: String,
    pasteText: String,
    importTargetMarket: String,
    parsePreview: ParsePreviewReport?,
    overwriteConflicts: Boolean,
    totalRecords: Int,
    onSyncLive: () -> Unit,
    onPasteTextChange: (String) -> Unit,
    onTargetMarketChange: (String) -> Unit,
    onParsePaste: () -> Unit,
    onToggleOverwriteConflicts: (Boolean) -> Unit,
    onConfirmSaveImport: () -> Unit,
    onClearPreview: () -> Unit,
    onSaveManualEntry: (market: String, date: String, openPana: String, closePana: String?) -> Unit,
    onExportJsonBackup: () -> Unit,
    onImportJsonBackup: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenFullHistory: () -> Unit,
    onClearAllDatabase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = DpBossTheme.colors
    val clipboard = LocalClipboardManager.current
    var activeWorkspaceTab by remember { mutableIntStateOf(0) } // 0=Bulk & Sync, 1=Manual, 2=Backup
    var showClearDialog by remember { mutableStateOf(false) }

    // Manual entry form state
    var manualMarket by remember(importTargetMarket) { mutableStateOf(importTargetMarket.ifBlank { "KALYAN" }) }
    var manualDate by remember { mutableStateOf(DpBossTextParser.todayDateIso()) }
    var manualOpen by remember { mutableStateOf("") }
    var manualClose by remember { mutableStateOf("") }

    val openCheck = remember(manualOpen) {
        if (manualOpen.length == 3) MatkaMathEngine.validatePana(manualOpen) else null
    }
    val closeCheck = remember(manualClose) {
        if (manualClose.length == 3) MatkaMathEngine.validatePana(manualClose) else null
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Sub-workspace selector
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
                        Text("Data & Import Hub", style = MaterialTheme.typography.headlineLarge, color = colors.textPrimary)
                        Text("$totalRecords stored records • $lastSyncStatus", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                    Surface(
                        onClick = onOpenFullHistory,
                        shape = RoundedCornerShape(10.dp),
                        color = colors.card,
                        border = BorderStroke(0.8.dp, colors.borderSubtle),
                        modifier = Modifier.testTag("data_open_history_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("History", style = MaterialTheme.typography.labelLarge, color = colors.cyan)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.navy)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(13.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Sync & Paste", "Manual Entry", "Backup / CSV").forEachIndexed { idx, label ->
                        val selected = activeWorkspaceTab == idx
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) colors.electricBlue else Color.Transparent)
                                .clickable { activeWorkspaceTab = idx }
                                .padding(vertical = 9.dp)
                                .testTag("data_subtab_$idx")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        when (activeWorkspaceTab) {
            // TAB 0: Auto Sync + Smart Bulk Copy-Paste Importer
            0 -> {
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
                                Text("AUTO SYNC FROM DPBOSS", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text(
                                    text = "Direct fetch + AllOrigins & CorsProxy fallbacks (6s timeout).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            StatusPill(label = "LIVE WEB", color = colors.cyan)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onSyncLive,
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.electricBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("data_sync_live_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Syncing dpbosssattamatka.com...")
                            } else {
                                Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(19.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Sync Live Results", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                item {
                    GlassAnalyticsCard(
                        glowAccent = colors.cyan,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SMART BULK COPY-PASTE IMPORTER", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Surface(
                                onClick = {
                                    val clipText = clipboard.getText()?.text.orEmpty()
                                    if (clipText.isNotBlank()) onPasteTextChange(clipText)
                                },
                                shape = RoundedCornerShape(9.dp),
                                color = colors.cyanSoft,
                                border = BorderStroke(0.8.dp, colors.cyan.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("paste_clipboard_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentPaste, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Paste", style = MaterialTheme.typography.labelMedium, color = colors.cyan)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Auto-detects DPBoss Home inline results (259-67-449), 3-line Vertical Panel Charts, and date-prefixed rows.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = importTargetMarket,
                            onValueChange = onTargetMarketChange,
                            label = { Text("Default Market (for Panel Charts without header)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.electricBlue,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = colors.navy,
                                unfocusedContainerColor = colors.navy,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("import_market_input")
                        )

                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = onPasteTextChange,
                            placeholder = {
                                Text(
                                    text = "Paste directly from DPBoss:\nKALYAN\n14/02/2025 259-67-449\n15/02/2025 179-74-248\n— or paste weekly 3-line Panel Charts",
                                    style = MonoTextStyles.MetricSmall,
                                    color = colors.textMuted
                                )
                            },
                            minLines = 5,
                            maxLines = 9,
                            textStyle = MonoTextStyles.MetricSmall.copy(color = colors.textPrimary),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.cyan,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = colors.navy,
                                unfocusedContainerColor = colors.navy
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bulk_paste_textarea")
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onClearPreview,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .weight(0.38f)
                                    .height(48.dp)
                            ) {
                                Text("Clear", color = colors.textSecondary)
                            }
                            Button(
                                onClick = onParsePaste,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.electricBlue),
                                modifier = Modifier
                                    .weight(0.62f)
                                    .height(48.dp)
                                    .testTag("parse_data_button")
                            ) {
                                Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("Parse & Preview", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // Preview Table Card
                if (parsePreview != null && parsePreview.rows.isNotEmpty()) {
                    item {
                        GlassAnalyticsCard(
                            glowAccent = colors.emerald,
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text("IMPORT VALIDATION PREVIEW", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Spacer(Modifier.height(10.dp))

                            // Status Summary Counters (Green / Orange / Red)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                StatusPill(label = "VALID ${parsePreview.validNewCount + parsePreview.openOnlyCount}", color = colors.emerald, modifier = Modifier.weight(1f))
                                StatusPill(label = "DUP/CONF ${parsePreview.duplicateCount + parsePreview.conflictCount}", color = colors.warning, modifier = Modifier.weight(1f))
                                StatusPill(label = "INVALID ${parsePreview.invalidCount}", color = colors.danger, modifier = Modifier.weight(1f))
                            }

                            if (parsePreview.conflictCount > 0) {
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Overwrite ${parsePreview.conflictCount} conflicting dates",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.warning
                                    )
                                    Switch(
                                        checked = overwriteConflicts,
                                        onCheckedChange = onToggleOverwriteConflicts,
                                        colors = SwitchDefaults.colors(checkedTrackColor = colors.warning)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                parsePreview.rows.take(20).forEach { row ->
                                    val statusColor = when (row.status) {
                                        ParsedRowStatus.VALID_NEW -> colors.emerald
                                        ParsedRowStatus.OPEN_ONLY -> colors.cyan
                                        ParsedRowStatus.DUPLICATE, ParsedRowStatus.CONFLICT -> colors.warning
                                        ParsedRowStatus.INVALID -> colors.danger
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.navy)
                                            .border(0.8.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${row.marketName} • ${row.dateIso}",
                                                style = MonoTextStyles.MetricSmall,
                                                color = colors.textPrimary
                                            )
                                            StatusPill(label = row.status.name.replace("_", " "), color = statusColor)
                                        }
                                        Spacer(Modifier.height(3.dp))
                                        Text(
                                            text = "${row.openPana ?: "---"}-${row.jodi ?: "--"}-${row.closePana ?: "***"}  •  ${row.message}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = statusColor,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (parsePreview.rows.size > 20) {
                                    Text(
                                        text = "+ ${parsePreview.rows.size - 20} more parsed rows ready to save",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Button(
                                onClick = onConfirmSaveImport,
                                shape = RoundedCornerShape(13.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.emerald),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("confirm_save_import_button")
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.midnight)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Confirm & Save Valid Results",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = colors.midnight
                                )
                            }
                        }
                    }
                }
            }

            // TAB 1: Manual Result Entry
            1 -> {
                item {
                    GlassAnalyticsCard(
                        glowAccent = colors.neonPurple,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EditNote, contentDescription = null, tint = colors.neonPurple)
                            Spacer(Modifier.width(8.dp))
                            Text("MANUAL RESULT ENTRY", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Strictly enforces Matka digit order (1..9, 0) and auto-computes Open Ank, Close Ank, and Jodi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = manualMarket,
                            onValueChange = { manualMarket = it.uppercase() },
                            label = { Text("Market Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.electricBlue,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = colors.navy,
                                unfocusedContainerColor = colors.navy,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = manualDate,
                            onValueChange = { manualDate = it },
                            label = { Text("Result Date (YYYY-MM-DD or DD/MM/YYYY)") },
                            singleLine = true,
                            textStyle = MonoTextStyles.MetricSmall.copy(color = colors.textPrimary),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.electricBlue,
                                unfocusedBorderColor = colors.borderSubtle,
                                focusedContainerColor = colors.navy,
                                unfocusedContainerColor = colors.navy
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = manualOpen,
                                onValueChange = { manualOpen = it.filter(Char::isDigit).take(3) },
                                label = { Text("Open Pana") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MonoTextStyles.PanaMedium.copy(color = colors.textPrimary),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.electricBlue,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedContainerColor = colors.navy,
                                    unfocusedContainerColor = colors.navy
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_open_input")
                            )
                            OutlinedTextField(
                                value = manualClose,
                                onValueChange = { manualClose = it.filter(Char::isDigit).take(3) },
                                label = { Text("Close (Optional)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MonoTextStyles.PanaMedium.copy(color = colors.textPrimary),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.cyan,
                                    unfocusedBorderColor = colors.borderSubtle,
                                    focusedContainerColor = colors.navy,
                                    unfocusedContainerColor = colors.navy
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_close_input")
                            )
                        }

                        // Live auto-calculated Ank / Jodi preview
                        val openAnk = (openCheck as? PanaValidationResult.Valid)?.info?.ank
                        val closeAnk = (closeCheck as? PanaValidationResult.Valid)?.info?.ank
                        if (openCheck is PanaValidationResult.Invalid) {
                            Spacer(Modifier.height(8.dp))
                            Text(openCheck.reason, style = MaterialTheme.typography.bodySmall, color = colors.danger)
                        }
                        if (closeCheck is PanaValidationResult.Invalid) {
                            Spacer(Modifier.height(6.dp))
                            Text(closeCheck.reason, style = MaterialTheme.typography.bodySmall, color = colors.danger)
                        }
                        if (openAnk != null) {
                            Spacer(Modifier.height(10.dp))
                            val previewStr = if (closeAnk != null) {
                                "Verified Result: $manualOpen-$openAnk$closeAnk-$manualClose"
                            } else {
                                "Open Only: $manualOpen-${openAnk}*-***"
                            }
                            StatusPill(label = previewStr, color = colors.emerald)
                        }

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                onSaveManualEntry(manualMarket, manualDate, manualOpen, manualClose.ifBlank { null })
                                if (openCheck is PanaValidationResult.Valid && (manualClose.isBlank() || closeCheck is PanaValidationResult.Valid)) {
                                    manualOpen = ""
                                    manualClose = ""
                                }
                            },
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.neonPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_manual_entry_button")
                        ) {
                            Text("Save Verified Result", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // TAB 2: Backup, Restore & Export
            else -> {
                item {
                    GlassAnalyticsCard(
                        glowAccent = colors.emerald,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("DATABASE BACKUP & RESTORE", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Export your entire local Room database as portable JSON or CSV using Android's secure file picker.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = onExportJsonBackup,
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.electricBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_json_backup_button")
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export JSON Backup")
                        }
                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onImportJsonBackup,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, colors.cyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("import_json_backup_button")
                        ) {
                            Icon(Icons.Rounded.FileUpload, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Restore from JSON File", color = colors.cyan)
                        }
                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onExportCsv,
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, colors.emerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("export_csv_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export All History (CSV)", color = colors.emerald)
                        }
                    }
                }

                item {
                    GlassAnalyticsCard(
                        glowAccent = colors.danger,
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("DANGER ZONE", style = MaterialTheme.typography.titleSmall, color = colors.danger)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Wipe all locally stored markets and historical result records from this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.danger),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = colors.danger, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clear Local Database", color = colors.danger)
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = colors.navy,
            title = { Text("Clear Entire Database?", color = colors.textPrimary) },
            text = {
                Text(
                    "All stored markets and historical records will be permanently deleted. Consider exporting a JSON backup first.",
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllDatabase()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
