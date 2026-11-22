package com.example.domain.schedule

import com.example.domain.analytics.MatkaAnalyticsEngine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class MarketLiveStatus(val badgeLabel: String) {
    LIVE_OPEN("LIVE / OPEN"),
    CLOSED_TODAY("CLOSED FOR TODAY"),
    OFF_TODAY("MARKET OFF TODAY")
}

data class MarketScheduleConfig(
    val openTimeMinutes: Int, // Minutes from midnight IST (0..1439)
    val closeTimeMinutes: Int, // Minutes from midnight IST (0..1439)
    val offDays: Set<Int> // ISO days: 1=Mon .. 7=Sun
) {
    val formattedTimingRange: String
        get() = "${MarketScheduleEngine.formatMinutes12h(openTimeMinutes)} – ${MarketScheduleEngine.formatMinutes12h(closeTimeMinutes)}"

    val offDaysSummary: String
        get() = if (offDays.isEmpty()) {
            "Open 7 Days"
        } else {
            val labels = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            offDays.sorted().mapNotNull { labels.getOrNull(it) }.joinToString(", ") + " Off"
        }

    fun offDaysCsv(): String = offDays.sorted().joinToString(",")
}

data class MarketScheduleEvaluation(
    val status: MarketLiveStatus,
    val targetDateIso: String,
    val targetDayOfWeek: Int, // 1=Mon .. 7=Sun
    val targetDayLabel: String,
    val isShiftedToNextActiveDay: Boolean,
    val shiftBannerMessage: String?,
    val istTimeLabel: String
)

/**
 * Market Timings, Weekly Off-Days, Live IST Status (Asia/Kolkata), and
 * Smart Next-Active-Day calculation engine.
 */
object MarketScheduleEngine {

    private val istTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val dayFullNames = listOf("", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun parseOffDaysCsv(csv: String): Set<Int> {
        if (csv.isBlank()) return emptySet()
        return csv.split(',')
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
    }

    fun defaultScheduleForMarket(marketName: String): MarketScheduleConfig {
        val u = marketName.trim().uppercase()
        return when {
            u.contains("MAIN BAZAR") -> MarketScheduleConfig(
                openTimeMinutes = hm(21, 35),
                closeTimeMinutes = hm(23, 55),
                offDays = setOf(6, 7) // Sat & Sun Off
            )
            u.contains("KALYAN NIGHT") -> MarketScheduleConfig(
                openTimeMinutes = hm(21, 25),
                closeTimeMinutes = hm(23, 30),
                offDays = setOf(7) // Sun Off
            )
            u.contains("KALYAN MORNING") -> MarketScheduleConfig(
                openTimeMinutes = hm(11, 0),
                closeTimeMinutes = hm(12, 2),
                offDays = emptySet() // Open 7 Days
            )
            u.contains("KALYAN") -> MarketScheduleConfig(
                openTimeMinutes = hm(15, 45),
                closeTimeMinutes = hm(17, 45),
                offDays = setOf(7) // Sun Off
            )
            u.contains("RAJDHANI NIGHT") -> MarketScheduleConfig(
                openTimeMinutes = hm(21, 30),
                closeTimeMinutes = hm(23, 45),
                offDays = setOf(7) // Sun Off
            )
            u.contains("RAJDHANI DAY") -> MarketScheduleConfig(
                openTimeMinutes = hm(15, 0),
                closeTimeMinutes = hm(17, 0),
                offDays = setOf(7) // Sun Off
            )
            u.contains("MILAN MORNING") -> MarketScheduleConfig(
                openTimeMinutes = hm(10, 15),
                closeTimeMinutes = hm(11, 15),
                offDays = emptySet()
            )
            u.contains("MILAN NIGHT") -> MarketScheduleConfig(
                openTimeMinutes = hm(21, 0),
                closeTimeMinutes = hm(23, 0),
                offDays = setOf(7)
            )
            u.contains("MILAN DAY") || u.contains("MILAN") -> MarketScheduleConfig(
                openTimeMinutes = hm(15, 0),
                closeTimeMinutes = hm(17, 0),
                offDays = setOf(7)
            )
            u.contains("SRIDEVI NIGHT") -> MarketScheduleConfig(
                openTimeMinutes = hm(19, 0),
                closeTimeMinutes = hm(20, 0),
                offDays = emptySet()
            )
            u.contains("SRIDEVI") -> MarketScheduleConfig(
                openTimeMinutes = hm(11, 35),
                closeTimeMinutes = hm(12, 35),
                offDays = emptySet()
            )
            u.contains("TIME BAZAR") -> MarketScheduleConfig(
                openTimeMinutes = hm(13, 0),
                closeTimeMinutes = hm(14, 0),
                offDays = emptySet()
            )
            u.contains("MADHUR MORNING") -> MarketScheduleConfig(
                openTimeMinutes = hm(11, 35),
                closeTimeMinutes = hm(12, 35),
                offDays = emptySet()
            )
            u.contains("MADHUR NIGHT") -> MarketScheduleConfig(
                openTimeMinutes = hm(20, 30),
                closeTimeMinutes = hm(22, 30),
                offDays = emptySet()
            )
            u.contains("MADHUR") -> MarketScheduleConfig(
                openTimeMinutes = hm(13, 30),
                closeTimeMinutes = hm(14, 30),
                offDays = emptySet()
            )
            u.contains("SUPREME NIGHT") -> MarketScheduleConfig(
                openTimeMinutes = hm(20, 45),
                closeTimeMinutes = hm(22, 45),
                offDays = setOf(7)
            )
            u.contains("SUPREME") -> MarketScheduleConfig(
                openTimeMinutes = hm(15, 35),
                closeTimeMinutes = hm(17, 35),
                offDays = setOf(7)
            )
            u.contains("MORNING") -> MarketScheduleConfig(
                openTimeMinutes = hm(9, 30),
                closeTimeMinutes = hm(10, 30),
                offDays = emptySet()
            )
            u.contains("NIGHT") -> MarketScheduleConfig(
                openTimeMinutes = hm(21, 0),
                closeTimeMinutes = hm(23, 0),
                offDays = setOf(7)
            )
            else -> MarketScheduleConfig(
                openTimeMinutes = hm(10, 0),
                closeTimeMinutes = hm(12, 0),
                offDays = emptySet()
            )
        }
    }

    /**
     * Evaluates a market's live status in IST (Asia/Kolkata) at [nowEpochMillis] and computes
     * the exact next active operating day for Day-Wise Pattern Analysis.
     */
    fun evaluateAt(
        schedule: MarketScheduleConfig,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): MarketScheduleEvaluation {
        val cal = Calendar.getInstance(istTimeZone).apply { timeInMillis = nowEpochMillis }
        val todayDow = MatkaAnalyticsEngine.calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val istTimeFmt = SimpleDateFormat("hh:mm a 'IST'", Locale.US).apply { timeZone = istTimeZone }
        val isoDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = istTimeZone }

        val status = when {
            todayDow in schedule.offDays -> MarketLiveStatus.OFF_TODAY
            currentMinutes >= schedule.closeTimeMinutes -> MarketLiveStatus.CLOSED_TODAY
            else -> MarketLiveStatus.LIVE_OPEN
        }

        val targetCal = (cal.clone() as Calendar)
        val shifted = status != MarketLiveStatus.LIVE_OPEN
        if (shifted) {
            // Advance at least 1 day and skip any scheduled off-days (guard against all-7-days-off)
            var guard = 0
            do {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
                val dow = MatkaAnalyticsEngine.calendarDayToIso(targetCal.get(Calendar.DAY_OF_WEEK))
                guard++
                if (dow !in schedule.offDays) break
            } while (guard < 8)
        }

        val targetDow = MatkaAnalyticsEngine.calendarDayToIso(targetCal.get(Calendar.DAY_OF_WEEK))
        val targetDayLabel = dayFullNames.getOrElse(targetDow) { "Next Day" }
        val targetDateIso = isoDateFmt.format(targetCal.time)

        val banner = when (status) {
            MarketLiveStatus.OFF_TODAY ->
                "Market Off Today — Showing Top 5 Predictions for $targetDayLabel"
            MarketLiveStatus.CLOSED_TODAY ->
                "Market Closed for Today — Showing Top 5 Predictions for $targetDayLabel"
            MarketLiveStatus.LIVE_OPEN -> null
        }

        return MarketScheduleEvaluation(
            status = status,
            targetDateIso = targetDateIso,
            targetDayOfWeek = targetDow,
            targetDayLabel = targetDayLabel,
            isShiftedToNextActiveDay = shifted,
            shiftBannerMessage = banner,
            istTimeLabel = istTimeFmt.format(cal.time)
        )
    }

    fun formatMinutes12h(totalMinutes: Int): String {
        val clamped = ((totalMinutes % 1440) + 1440) % 1440
        val hour24 = clamped / 60
        val min = clamped % 60
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, min, amPm)
    }

    fun hm(hour24: Int, minute: Int): Int = (hour24.coerceIn(0, 23) * 60) + minute.coerceIn(0, 59)
}
