package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.schedule.MarketScheduleConfig
import com.example.domain.schedule.MarketScheduleEngine

enum class MarketCategory(val label: String) {
    MAIN("Main"),
    DAY("Day"),
    NIGHT("Night"),
    STARLINE("Starline"),
    OTHER("Other");

    companion object {
        fun inferFromName(name: String): MarketCategory {
            val upper = name.uppercase()
            return when {
                upper.contains("STARLINE") || upper.contains("JACKPOT") -> STARLINE
                upper.contains("NIGHT") || upper.contains("MAIN BAZAR") || upper.contains("RAJDHANI NIGHT") -> NIGHT
                upper.contains("DAY") || upper.contains("MORNING") || upper.contains("TIME BAZAR") -> DAY
                upper.contains("KALYAN") || upper.contains("MILAN") || upper.contains("SRIDEVI") ||
                    upper.contains("MADHUR") || upper.contains("SUPREME") || upper.contains("RAJDHANI") -> MAIN
                else -> OTHER
            }
        }
    }
}

enum class RecordSource(val label: String) {
    WEB_SYNC("Web Sync"),
    PASTE_IMPORT("Bulk Paste"),
    MANUAL("Manual Entry"),
    JSON_RESTORE("JSON Restore")
}

@Entity(
    tableName = "markets",
    indices = [Index(value = ["name"], unique = true)]
)
data class MarketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = MarketCategory.MAIN.name,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncedAt: Long? = null,
    val isFavorite: Boolean = false,
    /** Minutes from midnight IST (-1 means use built-in default schedule for this market). */
    val openTimeMinutes: Int = -1,
    val closeTimeMinutes: Int = -1,
    /** Comma-separated ISO off days (1=Mon..7=Sun), or "DEFAULT" to use built-in market schedule. */
    val offDaysCsv: String = "DEFAULT"
) {
    fun resolvedSchedule(): MarketScheduleConfig {
        val fallback = MarketScheduleEngine.defaultScheduleForMarket(name)
        return MarketScheduleConfig(
            openTimeMinutes = if (openTimeMinutes in 0..1439) openTimeMinutes else fallback.openTimeMinutes,
            closeTimeMinutes = if (closeTimeMinutes in 0..1439) closeTimeMinutes else fallback.closeTimeMinutes,
            offDays = if (offDaysCsv == "DEFAULT") fallback.offDays else MarketScheduleEngine.parseOffDaysCsv(offDaysCsv)
        )
    }
}

@Entity(
    tableName = "result_records",
    indices = [
        Index(value = ["marketName", "dateIso"], unique = true),
        Index(value = ["marketName"]),
        Index(value = ["dateIso"])
    ]
)
data class ResultRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val marketName: String,
    /** ISO-8601 date string yyyy-MM-dd */
    val dateIso: String,
    /** ISO day of week: 1 = Monday .. 7 = Sunday */
    val dayOfWeek: Int,
    val openPana: String? = null,
    val openAnk: Int? = null,
    val closePana: String? = null,
    val closeAnk: Int? = null,
    val jodi: String? = null,
    val source: String = RecordSource.PASTE_IMPORT.name,
    val isVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isComplete: Boolean
        get() = !openPana.isNullOrBlank() && !closePana.isNullOrBlank() && !jodi.isNullOrBlank()

    val formattedResult: String
        get() = when {
            isComplete -> "$openPana-$jodi-$closePana"
            !openPana.isNullOrBlank() && openAnk != null -> "$openPana-$openAnk*-***"
            else -> "***-**-***"
        }
}
