package com.example

import com.example.data.local.RecordSource
import com.example.data.local.ResultRecordEntity
import com.example.data.parser.DpBossTextParser
import com.example.data.parser.ParsedRowStatus
import com.example.domain.analytics.MatkaAnalyticsEngine
import com.example.domain.math.MatkaMathEngine
import com.example.domain.math.PanaCategory
import com.example.domain.schedule.MarketLiveStatus
import com.example.domain.schedule.MarketScheduleEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MatkaEngineUnitTest {

    @Test
    fun `exact 220 valid Panas partitioned into 120 SP, 90 DP, 10 TP`() {
        assertEquals(220, MatkaMathEngine.allPanas.size)
        assertEquals(120, MatkaMathEngine.singlePanas.size)
        assertEquals(90, MatkaMathEngine.doublePanas.size)
        assertEquals(10, MatkaMathEngine.triplePanas.size)
        assertEquals(210, MatkaMathEngine.spAndDpPanas.size)

        for (ank in 0..9) {
            val group = MatkaMathEngine.panasByAnk[ank].orEmpty()
            assertEquals(22, group.size)
            assertEquals(12, group.count { it.category == PanaCategory.SP })
            assertEquals(9, group.count { it.category == PanaCategory.DP })
            assertEquals(1, group.count { it.category == PanaCategory.TP })
        }
    }

    @Test
    fun `strict Matka digit order and Ank calculation`() {
        assertTrue(MatkaMathEngine.isValidPana("179"))
        assertTrue(MatkaMathEngine.isValidPana("259"))
        assertTrue(MatkaMathEngine.isValidPana("449"))
        assertTrue(MatkaMathEngine.isValidPana("120"))
        assertTrue(MatkaMathEngine.isValidPana("000"))

        assertFalse(MatkaMathEngine.isValidPana("132"))
        assertFalse(MatkaMathEngine.isValidPana("015"))
        assertFalse(MatkaMathEngine.isValidPana("009"))

        assertEquals(7, MatkaMathEngine.calculateAnk("179"))
        assertEquals(6, MatkaMathEngine.calculateAnk("259"))
        assertEquals(7, MatkaMathEngine.calculateAnk("449"))
        assertEquals(0, MatkaMathEngine.calculateAnk("000"))
    }

    @Test
    fun `canonical families cleanly partition all 220 Panas`() {
        val totalMembers = MatkaMathEngine.allFamilies.sumOf { it.members.size }
        assertEquals(220, totalMembers)
        assertEquals(10, MatkaMathEngine.allFamilies.count { it.members.size == 8 })
        assertEquals(20, MatkaMathEngine.allFamilies.count { it.members.size == 6 })
        assertEquals(5, MatkaMathEngine.allFamilies.count { it.members.size == 4 })
    }

    @Test
    fun `empty market produces safe empty analytics with zero fake predictions`() {
        val snapshot = MatkaAnalyticsEngine.analyzeMarket("KALYAN", emptyList())
        assertFalse(snapshot.hasData)
        assertEquals(0, snapshot.totalRecords)
        assertTrue(snapshot.top5NextGamePanas.isEmpty())
        assertTrue(snapshot.openAnkSuggestions.isEmpty())
        assertTrue(snapshot.jodiSuggestions.isEmpty())
    }

    @Test
    fun `parser extracts inline and vertical DPBoss formats and flags invalid Panas`() {
        val samplePaste = """
            KALYAN
            10/02/2025 259-67-449
            11/02/2025 179-74-248
            12/02/2025 132-67-449
        """.trimIndent()

        val report = DpBossTextParser.parseText(samplePaste, "KALYAN", emptyList())
        assertEquals(3, report.rows.size)
        assertEquals(2, report.validNewCount)
        assertEquals(1, report.invalidCount)
        assertEquals(ParsedRowStatus.INVALID, report.rows[2].status)
    }

    @Test
    fun `strict 4 SP plus 1 DP quota enforced in BOTH Top 5 Next Game and Open-to-Close Predictor`() {
        val records = listOf(
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-10", dayOfWeek = 1, openPana = "179", openAnk = 7, closePana = "248", closeAnk = 4, jodi = "74", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-11", dayOfWeek = 2, openPana = "259", openAnk = 6, closePana = "449", closeAnk = 7, jodi = "67", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-12", dayOfWeek = 3, openPana = "179", openAnk = 7, closePana = "149", closeAnk = 4, jodi = "74", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-13", dayOfWeek = 4, openPana = "449", openAnk = 7, closePana = "338", closeAnk = 4, jodi = "74", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-14", dayOfWeek = 5, openPana = "223", openAnk = 7, closePana = "112", closeAnk = 4, jodi = "74", source = RecordSource.PASTE_IMPORT.name)
        )

        val snapshot = MatkaAnalyticsEngine.analyzeMarket("KALYAN", records)
        assertTrue(snapshot.hasData)
        assertEquals(5, snapshot.top5NextGamePanas.size)
        assertEquals(4, snapshot.top5NextGamePanas.count { it.category == PanaCategory.SP })
        assertEquals(1, snapshot.top5NextGamePanas.count { it.category == PanaCategory.DP })
        assertEquals(0, snapshot.top5NextGamePanas.count { it.category == PanaCategory.TP })
        // Verify sorted descending by statisticalScore (IDX)
        val top5Scores = snapshot.top5NextGamePanas.map { it.statisticalScore }
        assertEquals(top5Scores.sortedDescending(), top5Scores)

        val o2c = MatkaAnalyticsEngine.predictOpenToClose("179", records)
        assertNotNull(o2c)
        assertEquals(5, o2c!!.top5ClosePanas.size)
        assertEquals(4, o2c.top5ClosePanas.count { it.category == PanaCategory.SP })
        assertEquals(1, o2c.top5ClosePanas.count { it.category == PanaCategory.DP })
        assertEquals(0, o2c.top5ClosePanas.count { it.category == PanaCategory.TP })
        val o2cScores = o2c.top5ClosePanas.map { it.statisticalScore }
        assertEquals(o2cScores.sortedDescending(), o2cScores)
        assertTrue(o2c.top5ClosePanas.all { it.predictedJodi?.startsWith("7") == true && MatkaMathEngine.isValidPana(it.pana) })
    }

    @Test
    fun `IST live schedule detects OFF TODAY, CLOSED TODAY, and shifts to next active operating day`() {
        val ist = TimeZone.getTimeZone("Asia/Kolkata")

        // Sunday 2025-02-16 14:00 IST -> Kalyan is Sun Off -> shifts to Monday
        val sundayCal = Calendar.getInstance(ist).apply {
            set(2025, Calendar.FEBRUARY, 16, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val kalyanSchedule = MarketScheduleEngine.defaultScheduleForMarket("KALYAN")
        val sundayEval = MarketScheduleEngine.evaluateAt(kalyanSchedule, sundayCal.timeInMillis)
        assertEquals(MarketLiveStatus.OFF_TODAY, sundayEval.status)
        assertEquals(1, sundayEval.targetDayOfWeek) // Monday
        assertEquals("Monday", sundayEval.targetDayLabel)
        assertTrue(sundayEval.isShiftedToNextActiveDay)
        assertTrue(sundayEval.shiftBannerMessage!!.contains("Monday"))

        // Friday 2025-02-14 23:58 IST -> Main Bazar (Sat & Sun Off, closes 23:55) -> CLOSED FOR TODAY -> skips Sat & Sun to Monday!
        val fridayLateCal = Calendar.getInstance(ist).apply {
            set(2025, Calendar.FEBRUARY, 14, 23, 58, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val mainBazarSchedule = MarketScheduleEngine.defaultScheduleForMarket("MAIN BAZAR")
        val fridayEval = MarketScheduleEngine.evaluateAt(mainBazarSchedule, fridayLateCal.timeInMillis)
        assertEquals(MarketLiveStatus.CLOSED_TODAY, fridayEval.status)
        assertEquals(1, fridayEval.targetDayOfWeek) // Skips Sat (6) & Sun (7) -> Monday (1)
        assertEquals("Monday", fridayEval.targetDayLabel)
        assertTrue(fridayEval.shiftBannerMessage!!.contains("Monday"))

        // Sridevi at 11:00 AM IST on Sunday (Open 7 Days, closes 12:35 PM) -> LIVE / OPEN
        val sundayMorningCal = Calendar.getInstance(ist).apply {
            set(2025, Calendar.FEBRUARY, 16, 11, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val srideviSchedule = MarketScheduleEngine.defaultScheduleForMarket("SRIDEVI")
        val srideviEval = MarketScheduleEngine.evaluateAt(srideviSchedule, sundayMorningCal.timeInMillis)
        assertEquals(MarketLiveStatus.LIVE_OPEN, srideviEval.status)
        assertFalse(srideviEval.isShiftedToNextActiveDay)
        assertNull(srideviEval.shiftBannerMessage)
    }

    @Test
    fun `Open-to-Close Analyzer computes exact 4-factor weighted score (40-25-20-15) with zero fake percentages`() {
        // 1. With 0 historical records, Open Panna 128 -> Open Single Digit = 1, Cut = 6
        // Honest score MUST be strictly bounded to <= 25% (only Cut/Mirror & Family weight applies)
        val noHistoryResult = MatkaAnalyticsEngine.predictOpenToClose("128", emptyList())
        assertNotNull(noHistoryResult)
        assertTrue(noHistoryResult!!.isPatternOnlyMode)
        assertEquals(1, noHistoryResult.openAnk)
        assertEquals(6, noHistoryResult.cutAnk)
        assertEquals(5, noHistoryResult.top5CloseDigits.size)
        assertEquals(5, noHistoryResult.top5CloseDigits.map { it.closeDigit }.toSet().size)
        assertEquals(6, noHistoryResult.top5CloseDigits.first().closeDigit) // Cut of 1 is 6
        assertTrue(noHistoryResult.top5CloseDigits.all { it.confidencePercent in 0..25 })
        assertTrue(noHistoryResult.top5CloseDigits.all { it.correlationScorePct == 0 && it.hotDueScorePct == 0 && it.jodiPatternScorePct == 0 })
        assertEquals(
            noHistoryResult.top5CloseDigits.map { it.confidencePercent }.sortedDescending(),
            noHistoryResult.top5CloseDigits.map { it.confidencePercent }
        )
        assertTrue(noHistoryResult.top5CloseDigits.all { it.jodi == "1${it.closeDigit}" })
        assertTrue(noHistoryResult.top5CloseDigits.all { item ->
            item.matchingClosePanas.size == 2 &&
                item.matchingClosePanas.all { pana ->
                    MatkaMathEngine.isValidPana(pana) && MatkaMathEngine.calculateAnk(pana) == item.closeDigit
                }
        })
        // Strict 4 SP + 1 DP quota preserved
        assertEquals(5, noHistoryResult.top5ClosePanas.size)
        assertEquals(4, noHistoryResult.top5ClosePanas.count { it.category == PanaCategory.SP })
        assertEquals(1, noHistoryResult.top5ClosePanas.count { it.category == PanaCategory.DP })
        assertEquals(0, noHistoryResult.top5ClosePanas.count { it.category == PanaCategory.TP })

        // 2. With real historical chart data: Open 1 followed by Close 6 four times (and Jodi 16 diff +5, total 7)
        val history = listOf(
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-10", dayOfWeek = 1, openPana = "128", openAnk = 1, closePana = "259", closeAnk = 6, jodi = "16", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-11", dayOfWeek = 2, openPana = "128", openAnk = 1, closePana = "169", closeAnk = 6, jodi = "16", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-12", dayOfWeek = 3, openPana = "137", openAnk = 1, closePana = "240", closeAnk = 6, jodi = "16", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-13", dayOfWeek = 4, openPana = "146", openAnk = 1, closePana = "349", closeAnk = 6, jodi = "16", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-14", dayOfWeek = 5, openPana = "137", openAnk = 1, closePana = "149", closeAnk = 4, jodi = "14", source = RecordSource.PASTE_IMPORT.name),
            ResultRecordEntity(marketName = "KALYAN", dateIso = "2025-02-15", dayOfWeek = 6, openPana = "259", openAnk = 6, closePana = "128", closeAnk = 1, jodi = "61", source = RecordSource.PASTE_IMPORT.name)
        )
        val histResult = MatkaAnalyticsEngine.predictOpenToClose("128", history)
        assertNotNull(histResult)
        assertFalse(histResult!!.isPatternOnlyMode)
        assertEquals(5, histResult.top5CloseDigits.size)

        val top1 = histResult.top5CloseDigits.first()
        assertEquals(6, top1.closeDigit)
        assertEquals("16", top1.jodi)
        assertEquals(40, top1.correlationScorePct) // Top historical follower gets full 40%
        assertTrue(top1.cutFamilyScorePct > 0) // Direct cut of 1 is 6
        assertTrue(top1.hotDueScorePct > 0) // Hot in recent window
        assertTrue(top1.jodiPatternScorePct > 0) // Top Jodi diff +5 & total 7
        assertTrue(top1.confidencePercent >= 85)
        assertTrue(top1.patternReason.contains("4 times"))
        assertEquals(2, top1.matchingClosePanas.size)
        // Verify deterministic reproducibility (calling twice produces identical output)
        val repeatResult = MatkaAnalyticsEngine.predictOpenToClose("128", history)
        assertEquals(histResult.top5CloseDigits, repeatResult!!.top5CloseDigits)
    }
}
