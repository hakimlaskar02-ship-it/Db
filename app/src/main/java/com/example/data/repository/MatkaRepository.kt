package com.example.data.repository

import com.example.data.local.MarketCategory
import com.example.data.local.MarketEntity
import com.example.data.local.MatkaDao
import com.example.data.local.RecordSource
import com.example.data.local.ResultRecordEntity
import com.example.data.parser.BackupSerializer
import com.example.data.parser.DpBossTextParser
import com.example.data.parser.ParsePreviewReport
import com.example.data.parser.ParsedImportRow
import com.example.data.parser.ParsedRowStatus
import com.example.data.remote.DpBossSyncService
import com.example.data.remote.LiveSyncResult
import com.example.domain.schedule.MarketScheduleConfig
import kotlinx.coroutines.flow.Flow

data class SyncMergeOutcome(
    val liveResult: LiveSyncResult,
    val newOrUpdatedCount: Int,
    val totalFetchedCount: Int
)

class MatkaRepository(
    private val dao: MatkaDao,
    private val syncService: DpBossSyncService = DpBossSyncService()
) {
    val allMarkets: Flow<List<MarketEntity>> = dao.observeAllMarkets()
    val allRecords: Flow<List<ResultRecordEntity>> = dao.observeAllRecords()

    fun observeRecordsForMarket(marketName: String): Flow<List<ResultRecordEntity>> =
        dao.observeRecordsForMarket(marketName)

    suspend fun ensureMarketExists(
        rawName: String,
        category: MarketCategory? = null
    ): String {
        val cleanName = rawName.trim().uppercase()
        require(cleanName.isNotEmpty()) { "Market name cannot be empty." }
        val resolvedCategory = category ?: MarketCategory.inferFromName(cleanName)
        dao.insertMarketIgnore(
            MarketEntity(
                name = cleanName,
                category = resolvedCategory.name
            )
        )
        return cleanName
    }

    suspend fun setMarketFavorite(marketName: String, isFavorite: Boolean) {
        dao.setMarketFavorite(marketName, isFavorite)
    }

    suspend fun updateMarketSchedule(marketName: String, schedule: MarketScheduleConfig) {
        dao.updateMarketSchedule(
            name = marketName,
            openMinutes = schedule.openTimeMinutes,
            closeMinutes = schedule.closeTimeMinutes,
            offDaysCsv = schedule.offDaysCsv()
        )
    }

    suspend fun deleteMarket(marketName: String) {
        dao.deleteMarketWithRecords(marketName)
    }

    suspend fun saveSingleRecord(record: ResultRecordEntity) {
        ensureMarketExists(record.marketName)
        dao.upsertRecord(record)
    }

    suspend fun setRecordVerified(id: Long, verified: Boolean) {
        dao.setRecordVerified(id, verified)
    }

    suspend fun deleteRecord(id: Long) {
        dao.deleteRecordById(id)
    }

    suspend fun clearAllData() {
        dao.replaceAllData(emptyList(), emptyList())
    }

    suspend fun previewBulkPaste(rawText: String, defaultMarket: String): ParsePreviewReport {
        val existing = dao.getAllRecordsOnce()
        return DpBossTextParser.parseText(
            rawText = rawText,
            defaultMarketName = defaultMarket,
            existingRecords = existing
        )
    }

    suspend fun commitParsedImport(
        rows: List<ParsedImportRow>,
        overwriteConflicts: Boolean
    ): Int {
        val toSave = rows.filter { row ->
            row.status == ParsedRowStatus.VALID_NEW ||
                row.status == ParsedRowStatus.OPEN_ONLY ||
                (overwriteConflicts && row.status == ParsedRowStatus.CONFLICT)
        }
        if (toSave.isEmpty()) return 0

        val now = System.currentTimeMillis()
        val distinctMarkets = toSave.map { it.marketName }.distinct()
        distinctMarkets.forEach { marketName ->
            ensureMarketExists(marketName)
            dao.updateMarketSyncTimestamp(marketName, now)
        }

        val entities = toSave.map { DpBossTextParser.toRecordEntity(it, RecordSource.PASTE_IMPORT) }
        dao.upsertRecords(entities)
        return entities.size
    }

    /**
     * Fetches latest results from DPBoss and merges only new or upgraded rows without duplicates.
     */
    suspend fun syncLiveFromWeb(): SyncMergeOutcome {
        val result = syncService.syncLatestResults()
        if (result is LiveSyncResult.Success && result.records.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val toUpsert = mutableListOf<ResultRecordEntity>()

            for (incoming in result.records) {
                val existing = dao.getRecordByMarketAndDate(incoming.marketName, incoming.dateIso)
                val isIdentical = existing != null &&
                    existing.openPana == incoming.openPana &&
                    existing.closePana == incoming.closePana &&
                    existing.jodi == incoming.jodi
                // Avoid overwriting a complete saved result with an open-only result
                val wouldDowngrade = existing != null && existing.isComplete && !incoming.isComplete
                if (!isIdentical && !wouldDowngrade) {
                    toUpsert.add(incoming)
                }
            }

            if (toUpsert.isNotEmpty()) {
                toUpsert.map { it.marketName }.distinct().forEach { marketName ->
                    ensureMarketExists(marketName)
                    dao.updateMarketSyncTimestamp(marketName, now)
                }
                dao.upsertRecords(toUpsert)
            }

            return SyncMergeOutcome(
                liveResult = result,
                newOrUpdatedCount = toUpsert.size,
                totalFetchedCount = result.records.size
            )
        }

        return SyncMergeOutcome(
            liveResult = result,
            newOrUpdatedCount = 0,
            totalFetchedCount = 0
        )
    }

    suspend fun exportBackupJson(): String {
        val markets = dao.getAllMarketsOnce()
        val records = dao.getAllRecordsOnce()
        return BackupSerializer.exportToJson(markets, records)
    }

    suspend fun restoreBackupJson(json: String, replaceExisting: Boolean): Result<Int> {
        return BackupSerializer.parseFromJson(json).map { (markets, records) ->
            if (replaceExisting) {
                val allMarketNames = (markets.map { it.name } + records.map { it.marketName }).distinct()
                val mergedMarkets = allMarketNames.map { name ->
                    markets.firstOrNull { it.name == name } ?: MarketEntity(
                        name = name,
                        category = MarketCategory.inferFromName(name).name
                    )
                }
                dao.replaceAllData(mergedMarkets, records)
            } else {
                markets.forEach { dao.insertMarketIgnore(it) }
                records.map { it.marketName }.distinct().forEach { ensureMarketExists(it) }
                dao.upsertRecords(records)
            }
            records.size
        }
    }

    suspend fun exportCsv(marketFilter: String?): String {
        val records = if (marketFilter.isNullOrBlank()) {
            dao.getAllRecordsOnce()
        } else {
            dao.getRecordsForMarketOnce(marketFilter)
        }
        return BackupSerializer.exportRecordsToCsv(records)
    }
}
