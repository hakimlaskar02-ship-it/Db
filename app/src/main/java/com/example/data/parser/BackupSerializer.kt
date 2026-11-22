package com.example.data.parser

import com.example.data.local.MarketCategory
import com.example.data.local.MarketEntity
import com.example.data.local.RecordSource
import com.example.data.local.ResultRecordEntity
import com.example.domain.math.MatkaMathEngine
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class BackupMarketDto(
    val name: String,
    val category: String = MarketCategory.MAIN.name,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null,
    val isFavorite: Boolean = false,
    val openTimeMinutes: Int = -1,
    val closeTimeMinutes: Int = -1,
    val offDaysCsv: String = "DEFAULT"
)

@JsonClass(generateAdapter = true)
data class BackupRecordDto(
    val marketName: String,
    val dateIso: String,
    val dayOfWeek: Int,
    val openPana: String? = null,
    val openAnk: Int? = null,
    val closePana: String? = null,
    val closeAnk: Int? = null,
    val jodi: String? = null,
    val source: String = RecordSource.JSON_RESTORE.name,
    val isVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class DatabaseBackupPayload(
    val app: String = "DPBoss Smart Analytics",
    val schemaVersion: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val markets: List<BackupMarketDto>,
    val records: List<BackupRecordDto>
)

object BackupSerializer {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(DatabaseBackupPayload::class.java).indent("  ")

    fun exportToJson(
        markets: List<MarketEntity>,
        records: List<ResultRecordEntity>
    ): String {
        val payload = DatabaseBackupPayload(
            markets = markets.map {
                BackupMarketDto(
                    name = it.name,
                    category = it.category,
                    createdAt = it.createdAt,
                    lastSyncedAt = it.lastSyncedAt,
                    isFavorite = it.isFavorite,
                    openTimeMinutes = it.openTimeMinutes,
                    closeTimeMinutes = it.closeTimeMinutes,
                    offDaysCsv = it.offDaysCsv
                )
            },
            records = records.map {
                BackupRecordDto(
                    marketName = it.marketName,
                    dateIso = it.dateIso,
                    dayOfWeek = it.dayOfWeek,
                    openPana = it.openPana,
                    openAnk = it.openAnk,
                    closePana = it.closePana,
                    closeAnk = it.closeAnk,
                    jodi = it.jodi,
                    source = it.source,
                    isVerified = it.isVerified,
                    createdAt = it.createdAt
                )
            }
        )
        return adapter.toJson(payload)
    }

    fun parseFromJson(json: String): Result<Pair<List<MarketEntity>, List<ResultRecordEntity>>> {
        return runCatching {
            val payload = adapter.fromJson(json)
                ?: throw IllegalArgumentException("Invalid JSON backup format.")

            val markets = payload.markets.map {
                MarketEntity(
                    name = it.name.trim().uppercase(),
                    category = it.category,
                    createdAt = it.createdAt,
                    lastSyncedAt = it.lastSyncedAt,
                    isFavorite = it.isFavorite,
                    openTimeMinutes = it.openTimeMinutes,
                    closeTimeMinutes = it.closeTimeMinutes,
                    offDaysCsv = it.offDaysCsv
                )
            }

            val validRecords = payload.records.mapNotNull { rec ->
                val openP = rec.openPana?.trim()?.takeIf { it.isNotEmpty() }
                val closeP = rec.closePana?.trim()?.takeIf { it.isNotEmpty() }
                if (openP != null && !MatkaMathEngine.isValidPana(openP)) return@mapNotNull null
                if (closeP != null && !MatkaMathEngine.isValidPana(closeP)) return@mapNotNull null

                val openAnk = openP?.let { MatkaMathEngine.calculateAnk(it) } ?: rec.openAnk
                val closeAnk = closeP?.let { MatkaMathEngine.calculateAnk(it) } ?: rec.closeAnk
                val jodi = if (openAnk != null && closeAnk != null) "$openAnk$closeAnk" else rec.jodi

                ResultRecordEntity(
                    marketName = rec.marketName.trim().uppercase(),
                    dateIso = rec.dateIso,
                    dayOfWeek = rec.dayOfWeek.coerceIn(1, 7),
                    openPana = openP,
                    openAnk = openAnk,
                    closePana = closeP,
                    closeAnk = closeAnk,
                    jodi = jodi,
                    source = RecordSource.JSON_RESTORE.name,
                    isVerified = rec.isVerified,
                    createdAt = rec.createdAt
                )
            }

            markets to validRecords
        }
    }

    fun exportRecordsToCsv(records: List<ResultRecordEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("Market,Date,DayOfWeek,OpenPana,OpenAnk,Jodi,ClosePana,CloseAnk,Source,Verified")
        records.forEach { r ->
            val fields = listOf(
                escapeCsv(r.marketName),
                escapeCsv(r.dateIso),
                r.dayOfWeek.toString(),
                r.openPana ?: "",
                r.openAnk?.toString() ?: "",
                r.jodi ?: "",
                r.closePana ?: "",
                r.closeAnk?.toString() ?: "",
                escapeCsv(r.source),
                if (r.isVerified) "Verified" else "Pending"
            )
            sb.appendLine(fields.joinToString(","))
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
