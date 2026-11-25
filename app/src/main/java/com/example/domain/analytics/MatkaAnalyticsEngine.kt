package com.example.domain.analytics

import com.example.data.local.ResultRecordEntity
import com.example.domain.math.MatkaMathEngine
import com.example.domain.math.PanaCategory
import com.example.domain.math.PanaInfo
import com.example.domain.schedule.MarketScheduleConfig
import com.example.domain.schedule.MarketScheduleEngine
import com.example.domain.schedule.MarketScheduleEvaluation
import java.util.Calendar
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Pure deterministic statistical analysis engine.
 *
 * Strictly operates on real historical records stored in Room:
 * - Zero random numbers or fabricated predictions.
 * - Safe division everywhere (never outputs NaN or Infinity).
 * - Strict 4 SP + 1 DP quota in BOTH Top 5 Next Game Panas AND Live Open-to-Close Predictor
 *   (Top 4 highest-scoring Single Panas + single #1 highest-scoring Double Pana, all 5 ranked by IDX;
 *   Triple Panas excluded completely).
 * - Smart Next-Active-Day adjustment using live IST schedule when market is Off or Closed today.
 */
data class RankedAnkSuggestion(
    val rank: Int,
    val ank: Int,
    val historicalHits: Int,
    val hitRatePercent: Double,
    val currentGap: Int,
    val statisticalScore: Int, // 0..100 composite index strictly from historical factors
    val supportingNote: String
)

data class RankedPanaSuggestion(
    val rank: Int,
    val pana: String,
    val category: PanaCategory,
    val ank: Int,
    val historicalHits: Int,
    val currentGap: Int,
    val statisticalScore: Int, // 0..100 normalized statistical index
    val reason: String,
    val predictedJodi: String? = null // Populated for Open-to-Close Close predictions
)

data class RankedJodiSuggestion(
    val rank: Int,
    val jodi: String,
    val historicalHits: Int,
    val currentGap: Int,
    val statisticalScore: Int,
    val supportingNote: String
)

data class AnkFrequencyStat(
    val ank: Int,
    val totalHits: Int,
    val openHits: Int,
    val closeHits: Int,
    val sharePercent: Double
)

data class OverdueAnkStat(
    val ank: Int,
    val totalHits: Int,
    val currentGap: Int,
    val averageGap: Double,
    val longestGap: Int
)

data class DayWiseStat(
    val dayOfWeek: Int, // 1=Mon .. 7=Sun
    val dayShortLabel: String,
    val dayFullLabel: String,
    val totalDraws: Int,
    val ankHits: Map<Int, Int>,
    val topAnks: List<Pair<Int, Int>>,
    val topJodis: List<Pair<String, Int>>,
    val topPanas: List<Pair<String, Int>>
)

data class FamilyTrendStat(
    val familyKey: String,
    val baseDigitsLabel: String,
    val sizeTypeLabel: String,
    val totalHits: Int,
    val recentHits: Int,
    val drawsSinceLastHit: Int,
    val sampleMembers: List<String>
)

data class SuggestedCloseDigit(
    val rank: Int,
    val closeDigit: Int,
    val confidencePercent: Int, // Strictly calculated from weighted 40/25/20/15 score (0..100)
    val jodi: String,
    val matchingClosePanas: List<String>, // 2 matching valid Close Panas for this Close digit
    val patternReason: String, // Transparent 1-line human-readable reason
    val correlationScorePct: Int = 0, // 0..40
    val cutFamilyScorePct: Int = 0,   // 0..25
    val hotDueScorePct: Int = 0,      // 0..20
    val jodiPatternScorePct: Int = 0  // 0..15
)

data class OpenToClosePredictionResult(
    val openPana: String,
    val openAnk: Int,
    val openCategory: PanaCategory,
    val familyKey: String,
    val cutAnk: Int,
    val totalAnalyzedCompleteDraws: Int,
    val exactOpenPanaMatches: Int,
    val familyOpenMatches: Int,
    val openAnkMatches: Int,
    val cutAnkMatches: Int,
    val isPatternOnlyMode: Boolean = false,
    val top5CloseDigits: List<SuggestedCloseDigit> = emptyList(), // Top 5 suggested Close single digits (0-9)
    val top5ClosePanas: List<RankedPanaSuggestion>, // Strictly 4 SP + 1 DP ranked by IDX
    val rankedCloseAnks: List<RankedAnkSuggestion>,
    val spClosePanas: List<RankedPanaSuggestion>,
    val dpClosePanas: List<RankedPanaSuggestion>,
    val note: String
)

data class MarketAnalyticsSnapshot(
    val marketName: String,
    val totalRecords: Int,
    val completeRecords: Int,
    val verifiedRecords: Int,
    val dateRangeLabel: String,
    val nextDrawDateIso: String,
    val nextDrawDayOfWeek: Int,
    val nextDrawDayLabel: String,
    val scheduleEvaluation: MarketScheduleEvaluation,
    val analyzedAtMillis: Long,
    val top5NextGamePanas: List<RankedPanaSuggestion>, // Strictly 4 SP + 1 DP ranked by IDX
    val openAnkSuggestions: List<RankedAnkSuggestion>,
    val openSpSuggestions: List<RankedPanaSuggestion>,
    val openDpSuggestions: List<RankedPanaSuggestion>,
    val closeAnkSuggestions: List<RankedAnkSuggestion>,
    val closeSpSuggestions: List<RankedPanaSuggestion>,
    val closeDpSuggestions: List<RankedPanaSuggestion>,
    val jodiSuggestions: List<RankedJodiSuggestion>,
    val ankFrequencies: List<AnkFrequencyStat>,
    val topJodiFrequencies: List<Pair<String, Int>>,
    val topPanaFrequencies: List<Pair<String, Int>>,
    val overdueAnkStats: List<OverdueAnkStat>,
    val dayWiseStats: List<DayWiseStat>,
    val openToCloseMatrix: List<List<Int>>, // 10 x 10: [openAnk][closeAnk]
    val familyTrends: List<FamilyTrendStat>,
    val panaHitCounts: Map<String, Int>
) {
    val hasData: Boolean get() = totalRecords > 0
}

object MatkaAnalyticsEngine {

    private val dayShortNames = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val dayFullNames = listOf("", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    private fun safeDiv(numerator: Double, denominator: Double): Double {
        if (denominator <= 0.0 || numerator.isNaN() || denominator.isNaN()) return 0.0
        val value = numerator / denominator
        return if (value.isNaN() || value.isInfinite()) 0.0 else value
    }

    private fun clampScore(raw: Double): Int {
        if (raw.isNaN() || raw.isInfinite()) return 0
        return raw.roundToInt().coerceIn(1, 99)
    }

    fun analyzeMarket(
        marketName: String,
        rawRecords: List<ResultRecordEntity>,
        schedule: MarketScheduleConfig = MarketScheduleEngine.defaultScheduleForMarket(marketName),
        nowEpochMillis: Long = System.currentTimeMillis()
    ): MarketAnalyticsSnapshot {
        val scheduleEval = MarketScheduleEngine.evaluateAt(schedule, nowEpochMillis)
        val nextDateIso = scheduleEval.targetDateIso
        val nextDow = scheduleEval.targetDayOfWeek
        val nextDayLabel = scheduleEval.targetDayLabel

        if (rawRecords.isEmpty()) {
            return MarketAnalyticsSnapshot(
                marketName = marketName,
                totalRecords = 0,
                completeRecords = 0,
                verifiedRecords = 0,
                dateRangeLabel = "No records",
                nextDrawDateIso = nextDateIso,
                nextDrawDayOfWeek = nextDow,
                nextDrawDayLabel = nextDayLabel,
                scheduleEvaluation = scheduleEval,
                analyzedAtMillis = nowEpochMillis,
                top5NextGamePanas = emptyList(),
                openAnkSuggestions = emptyList(),
                openSpSuggestions = emptyList(),
                openDpSuggestions = emptyList(),
                closeAnkSuggestions = emptyList(),
                closeSpSuggestions = emptyList(),
                closeDpSuggestions = emptyList(),
                jodiSuggestions = emptyList(),
                ankFrequencies = emptyList(),
                topJodiFrequencies = emptyList(),
                topPanaFrequencies = emptyList(),
                overdueAnkStats = emptyList(),
                dayWiseStats = emptyList(),
                openToCloseMatrix = List(10) { List(10) { 0 } },
                familyTrends = emptyList(),
                panaHitCounts = emptyMap()
            )
        }

        // Chronological order: newest first (index 0 = most recent draw)
        val records = rawRecords.sortedByDescending { it.dateIso }
        val totalRecords = records.size
        val completeRecords = records.filter { it.isComplete }
        val verifiedCount = records.count { it.isVerified }
        val oldestDate = records.last().dateIso
        val newestDate = records.first().dateIso
        val dateRangeLabel = if (oldestDate == newestDate) newestDate else "$oldestDate → $newestDate"

        // --- 1. Aggregate real frequencies ---
        val openAnkCounts = IntArray(10)
        val closeAnkCounts = IntArray(10)
        val recentOpenAnkCounts = IntArray(10)
        val recentCloseAnkCounts = IntArray(10)
        val dayOpenAnkCounts = Array(8) { IntArray(10) }
        val dayCloseAnkCounts = Array(8) { IntArray(10) }

        val openPanaCounts = mutableMapOf<String, Int>()
        val closePanaCounts = mutableMapOf<String, Int>()
        val allPanaCounts = mutableMapOf<String, Int>()
        val recentPanaCounts = mutableMapOf<String, Int>()
        val dayPanaCounts = Array(8) { mutableMapOf<String, Int>() }

        val openPanaLastSeen = mutableMapOf<String, Int>()
        val closePanaLastSeen = mutableMapOf<String, Int>()
        val allPanaLastSeen = mutableMapOf<String, Int>()

        val familyTotalHits = mutableMapOf<String, Int>()
        val familyRecentHits = mutableMapOf<String, Int>()
        val familyLastSeen = mutableMapOf<String, Int>()

        val jodiCounts = mutableMapOf<String, Int>()
        val jodiLastSeen = mutableMapOf<String, Int>()
        val dayJodiCounts = Array(8) { mutableMapOf<String, Int>() }
        val dayTotalDraws = IntArray(8)

        val openToCloseMatrix = Array(10) { IntArray(10) }
        val recentWindow = min(25, totalRecords)

        records.forEachIndexed { index, rec ->
            val dow = rec.dayOfWeek.coerceIn(1, 7)
            dayTotalDraws[dow]++

            val oAnk = rec.openAnk
            val cAnk = rec.closeAnk
            val oPana = rec.openPana?.takeIf { MatkaMathEngine.isValidPana(it) }
            val cPana = rec.closePana?.takeIf { MatkaMathEngine.isValidPana(it) }
            val jodi = rec.jodi?.takeIf { it.length == 2 && it.all(Char::isDigit) }

            if (oAnk != null && oAnk in 0..9) {
                openAnkCounts[oAnk]++
                dayOpenAnkCounts[dow][oAnk]++
                if (index < recentWindow) recentOpenAnkCounts[oAnk]++
            }
            if (cAnk != null && cAnk in 0..9) {
                closeAnkCounts[cAnk]++
                dayCloseAnkCounts[dow][cAnk]++
                if (index < recentWindow) recentCloseAnkCounts[cAnk]++
            }
            if (oAnk != null && cAnk != null && oAnk in 0..9 && cAnk in 0..9) {
                openToCloseMatrix[oAnk][cAnk]++
            }
            if (oPana != null) {
                openPanaCounts[oPana] = (openPanaCounts[oPana] ?: 0) + 1
                allPanaCounts[oPana] = (allPanaCounts[oPana] ?: 0) + 1
                dayPanaCounts[dow][oPana] = (dayPanaCounts[dow][oPana] ?: 0) + 1
                if (index < recentWindow) recentPanaCounts[oPana] = (recentPanaCounts[oPana] ?: 0) + 1
                openPanaLastSeen.putIfAbsent(oPana, index)
                allPanaLastSeen.putIfAbsent(oPana, index)

                MatkaMathEngine.getPanaInfo(oPana)?.let { info ->
                    familyTotalHits[info.familyKey] = (familyTotalHits[info.familyKey] ?: 0) + 1
                    if (index < recentWindow) {
                        familyRecentHits[info.familyKey] = (familyRecentHits[info.familyKey] ?: 0) + 1
                    }
                    familyLastSeen.putIfAbsent(info.familyKey, index)
                }
            }
            if (cPana != null) {
                closePanaCounts[cPana] = (closePanaCounts[cPana] ?: 0) + 1
                allPanaCounts[cPana] = (allPanaCounts[cPana] ?: 0) + 1
                dayPanaCounts[dow][cPana] = (dayPanaCounts[dow][cPana] ?: 0) + 1
                if (index < recentWindow) recentPanaCounts[cPana] = (recentPanaCounts[cPana] ?: 0) + 1
                closePanaLastSeen.putIfAbsent(cPana, index)
                allPanaLastSeen.putIfAbsent(cPana, index)

                MatkaMathEngine.getPanaInfo(cPana)?.let { info ->
                    familyTotalHits[info.familyKey] = (familyTotalHits[info.familyKey] ?: 0) + 1
                    if (index < recentWindow) {
                        familyRecentHits[info.familyKey] = (familyRecentHits[info.familyKey] ?: 0) + 1
                    }
                    familyLastSeen.putIfAbsent(info.familyKey, index)
                }
            }
            if (jodi != null) {
                jodiCounts[jodi] = (jodiCounts[jodi] ?: 0) + 1
                dayJodiCounts[dow][jodi] = (dayJodiCounts[dow][jodi] ?: 0) + 1
                jodiLastSeen.putIfAbsent(jodi, index)
            }
        }

        val totalOpenDraws = openAnkCounts.sum().coerceAtLeast(1)
        val totalCloseDraws = closeAnkCounts.sum().coerceAtLeast(1)
        val totalAnkOccurrences = (openAnkCounts.sum() + closeAnkCounts.sum()).coerceAtLeast(1)

        // --- 2. Overdue stats per Ank ---
        val overdueAnkStats = (0..9).map { ank ->
            val hitIndices = mutableListOf<Int>()
            records.forEachIndexed { idx, rec ->
                if (rec.openAnk == ank || rec.closeAnk == ank) hitIndices.add(idx)
            }
            val currentGap = hitIndices.firstOrNull() ?: totalRecords
            val gaps = mutableListOf<Int>()
            for (i in 0 until hitIndices.size - 1) {
                gaps.add(hitIndices[i + 1] - hitIndices[i])
            }
            val avgGap = if (gaps.isNotEmpty()) {
                safeDiv(gaps.sum().toDouble(), gaps.size.toDouble())
            } else {
                safeDiv(totalRecords.toDouble(), (hitIndices.size + 1).toDouble())
            }
            val longestGap = (gaps + currentGap).maxOrNull() ?: totalRecords
            OverdueAnkStat(
                ank = ank,
                totalHits = hitIndices.size,
                currentGap = currentGap,
                averageGap = avgGap,
                longestGap = longestGap
            )
        }

        val openAnkGap = IntArray(10) { ank ->
            records.indexOfFirst { it.openAnk == ank }.let { if (it == -1) totalRecords else it }
        }
        val closeAnkGap = IntArray(10) { ank ->
            records.indexOfFirst { it.closeAnk == ank }.let { if (it == -1) totalRecords else it }
        }

        // --- 3. Score Open Anks & Close Anks (using Smart Next-Active-Day nextDow!) ---
        val lastRec = records.first()
        val lastOpenAnk = lastRec.openAnk
        val lastCloseAnk = lastRec.closeAnk

        fun buildAnkSuggestions(isOpen: Boolean): List<RankedAnkSuggestion> {
            val counts = if (isOpen) openAnkCounts else closeAnkCounts
            val recent = if (isOpen) recentOpenAnkCounts else recentCloseAnkCounts
            val dayCounts = if (isOpen) dayOpenAnkCounts[nextDow] else dayCloseAnkCounts[nextDow]
            val gaps = if (isOpen) openAnkGap else closeAnkGap
            val denom = if (isOpen) totalOpenDraws else totalCloseDraws
            val recentDenom = recent.sum().coerceAtLeast(1)

            val maxFreq = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
            val maxDayFreq = dayCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
            val maxGap = gaps.maxOrNull()?.coerceAtLeast(1) ?: 1

            val rawScored = (0..9).map { ank ->
                val freqRatio = safeDiv(counts[ank].toDouble(), maxFreq.toDouble())
                val recentRatio = safeDiv(recent[ank].toDouble(), recentDenom.toDouble()) * 3.0
                val dayRatio = safeDiv(dayCounts[ank].toDouble(), maxDayFreq.toDouble())
                val gapRatio = safeDiv(gaps[ank].toDouble(), maxGap.toDouble())
                val prevAnk = if (isOpen) lastCloseAnk ?: lastOpenAnk else lastOpenAnk
                val cutBonus = if (prevAnk != null && MatkaMathEngine.cutDigit(prevAnk) == ank) 0.14 else 0.0

                val composite = (
                    0.34 * freqRatio +
                        0.26 * gapRatio +
                        0.22 * dayRatio +
                        0.18 * min(1.0, recentRatio) +
                        cutBonus
                    )

                val reasons = mutableListOf<String>()
                if (dayCounts[ank] > 0) reasons.add("${dayShortNames[nextDow]} ×${dayCounts[ank]}")
                if (gaps[ank] >= 5) reasons.add("Gap ${gaps[ank]}d")
                if (counts[ank] > 0) reasons.add("${counts[ank]} hits")
                if (cutBonus > 0.0 && prevAnk != null) reasons.add("Cut of $prevAnk")
                if (reasons.isEmpty()) reasons.add("Baseline coverage")

                Triple(ank, composite, reasons.take(3).joinToString(" • "))
            }.sortedWith(compareByDescending<Triple<Int, Double, String>> { it.second }.thenBy { it.first })

            val topComposite = rawScored.firstOrNull()?.second?.takeIf { it > 0.0 } ?: 1.0
            return rawScored.mapIndexed { idx, (ank, comp, note) ->
                val normalized = clampScore(45.0 + 50.0 * safeDiv(comp, topComposite))
                RankedAnkSuggestion(
                    rank = idx + 1,
                    ank = ank,
                    historicalHits = counts[ank],
                    hitRatePercent = safeDiv(counts[ank] * 100.0, denom.toDouble()),
                    currentGap = gaps[ank],
                    statisticalScore = normalized,
                    supportingNote = note
                )
            }
        }

        val allOpenAnkRanked = buildAnkSuggestions(isOpen = true)
        val allCloseAnkRanked = if (closeAnkCounts.sum() > 0) buildAnkSuggestions(isOpen = false) else emptyList()
        val openAnkScoreMap = allOpenAnkRanked.associate { it.ank to it.statisticalScore / 100.0 }
        val closeAnkScoreMap = allCloseAnkRanked.associate { it.ank to it.statisticalScore / 100.0 }

        // --- 4. Score Valid SP & DP Panas (Exclude TP completely from predictions) ---
        val maxFamilyRecent = (familyRecentHits.values.maxOrNull() ?: 1).coerceAtLeast(1)
        val maxFamilyTotal = (familyTotalHits.values.maxOrNull() ?: 1).coerceAtLeast(1)

        fun scorePanaPool(
            isOpen: Boolean,
            candidates: List<PanaInfo>
        ): List<RankedPanaSuggestion> {
            val panaHitsMap = if (isOpen) openPanaCounts else closePanaCounts
            val panaGapMap = if (isOpen) openPanaLastSeen else closePanaLastSeen
            val ankScoreMap = if (isOpen) openAnkScoreMap else closeAnkScoreMap
            val dayAnkMap = if (isOpen) dayOpenAnkCounts[nextDow] else dayCloseAnkCounts[nextDow]
            val maxPanaHit = (panaHitsMap.values.maxOrNull() ?: 1).coerceAtLeast(1)
            val maxDayPanaHit = (dayPanaCounts[nextDow].values.maxOrNull() ?: 1).coerceAtLeast(1)

            val latestPana = if (isOpen) lastRec.openPana ?: lastRec.closePana else lastRec.closePana ?: lastRec.openPana
            val latestFamilyKey = latestPana?.let { MatkaMathEngine.getPanaInfo(it)?.familyKey }

            val scored = candidates.map { info ->
                val hits = panaHitsMap[info.pana] ?: 0
                val totalHitsBoth = allPanaCounts[info.pana] ?: 0
                val gap = panaGapMap[info.pana] ?: totalRecords
                val dayHits = dayPanaCounts[nextDow][info.pana] ?: 0
                val famHits = familyTotalHits[info.familyKey] ?: 0
                val famRec = familyRecentHits[info.familyKey] ?: 0
                val ankWeight = ankScoreMap[info.ank] ?: 0.35

                val freqComponent = safeDiv(hits.toDouble(), maxPanaHit.toDouble())
                val overdueComponent = if (hits > 0) {
                    min(1.0, safeDiv(gap.toDouble(), (totalRecords.coerceAtLeast(5) * 0.6)))
                } else {
                    0.35
                }
                val dayComponent = 0.55 * safeDiv(dayHits.toDouble(), maxDayPanaHit.toDouble()) +
                    0.45 * safeDiv(dayAnkMap[info.ank].toDouble(), (dayAnkMap.maxOrNull() ?: 1).coerceAtLeast(1).toDouble())
                val familyComponent = 0.55 * safeDiv(famRec.toDouble(), maxFamilyRecent.toDouble()) +
                    0.45 * safeDiv(famHits.toDouble(), maxFamilyTotal.toDouble())
                val repeatBonus = if (latestFamilyKey != null && info.familyKey == latestFamilyKey) 0.10 else 0.0

                val rawScore = (
                    0.34 * ankWeight +
                        0.23 * freqComponent +
                        0.18 * overdueComponent +
                        0.13 * dayComponent +
                        0.12 * familyComponent +
                        repeatBonus
                    )

                val reasonParts = mutableListOf<String>()
                if (dayAnkMap[info.ank] > 0) {
                    reasonParts.add("${dayShortNames[nextDow]} Ank ${info.ank} (×${dayAnkMap[info.ank]})")
                } else {
                    reasonParts.add("${dayShortNames[nextDow]} Ank ${info.ank}")
                }
                if (hits > 0) {
                    reasonParts.add("Hit ×$hits • Gap ${gap}d")
                } else if (totalHitsBoth > 0) {
                    reasonParts.add("Side hit ×$totalHitsBoth")
                } else if (famRec > 0) {
                    reasonParts.add("Active family (×$famRec)")
                } else {
                    reasonParts.add("Overdue ${info.category.code}")
                }
                if (latestFamilyKey != null && info.familyKey == latestFamilyKey) {
                    reasonParts.add("Family repeat")
                }

                Triple(info, rawScore, reasonParts.take(2).joinToString(" • "))
            }.sortedWith(
                compareByDescending<Triple<PanaInfo, Double, String>> { it.second }
                    .thenByDescending { allPanaCounts[it.first.pana] ?: 0 }
                    .thenBy { it.first.pana }
            )

            val maxRaw = scored.firstOrNull()?.second?.takeIf { it > 0.0 } ?: 1.0
            return scored.mapIndexed { idx, (info, raw, reason) ->
                val score = clampScore(42.0 + 54.0 * safeDiv(raw, maxRaw))
                RankedPanaSuggestion(
                    rank = idx + 1,
                    pana = info.pana,
                    category = info.category,
                    ank = info.ank,
                    historicalHits = panaHitsMap[info.pana] ?: 0,
                    currentGap = panaGapMap[info.pana] ?: totalRecords,
                    statisticalScore = score,
                    reason = reason
                )
            }
        }

        // Score unified SP+DP pool on the same scale so IDX scores are directly comparable across SP & DP!
        val unifiedOpenScored = scorePanaPool(isOpen = true, candidates = MatkaMathEngine.spAndDpPanas)
        val top5NextGame = enforceStrict4Sp1DpQuota(unifiedOpenScored)

        val openSp = unifiedOpenScored.filter { it.category == PanaCategory.SP }.take(6)
            .mapIndexed { i, item -> item.copy(rank = i + 1) }
        val openDp = unifiedOpenScored.filter { it.category == PanaCategory.DP }.take(6)
            .mapIndexed { i, item -> item.copy(rank = i + 1) }

        val hasCloseData = closePanaCounts.isNotEmpty() || closeAnkCounts.sum() > 0
        val unifiedCloseScored = if (hasCloseData) {
            scorePanaPool(isOpen = false, candidates = MatkaMathEngine.spAndDpPanas)
        } else {
            emptyList()
        }
        val closeSp = unifiedCloseScored.filter { it.category == PanaCategory.SP }.take(6)
            .mapIndexed { i, item -> item.copy(rank = i + 1) }
        val closeDp = unifiedCloseScored.filter { it.category == PanaCategory.DP }.take(6)
            .mapIndexed { i, item -> item.copy(rank = i + 1) }

        // --- 5. Jodi Analysis ---
        val jodiSuggestions = if (completeRecords.isNotEmpty()) {
            val maxJodiHit = (jodiCounts.values.maxOrNull() ?: 1).coerceAtLeast(1)
            val maxMatrixVal = openToCloseMatrix.maxOf { row -> row.maxOrNull() ?: 0 }.coerceAtLeast(1)
            val all100Jodis = (0..99).map { it.toString().padStart(2, '0') }

            val scoredJodis = all100Jodis.map { jodi ->
                val oDigit = jodi[0] - '0'
                val cDigit = jodi[1] - '0'
                val hits = jodiCounts[jodi] ?: 0
                val dayHits = dayJodiCounts[nextDow][jodi] ?: 0
                val gap = jodiLastSeen[jodi] ?: totalRecords
                val corrCount = openToCloseMatrix[oDigit][cDigit]

                val raw = 0.34 * safeDiv(hits.toDouble(), maxJodiHit.toDouble()) +
                    0.26 * safeDiv(corrCount.toDouble(), maxMatrixVal.toDouble()) +
                    0.20 * ((openAnkScoreMap[oDigit] ?: 0.3) * 0.55 + (closeAnkScoreMap[cDigit] ?: 0.3) * 0.45) +
                    0.12 * min(1.0, safeDiv(gap.toDouble(), totalRecords.coerceAtLeast(5).toDouble())) +
                    0.08 * if (dayHits > 0) 1.0 else 0.0

                val notes = mutableListOf<String>()
                if (hits > 0) notes.add("Hit ×$hits")
                if (corrCount > 0) notes.add("O$oDigit→C$cDigit ×$corrCount")
                if (dayHits > 0) notes.add("${dayShortNames[nextDow]} ×$dayHits")
                if (notes.isEmpty()) notes.add("Ank pair $oDigit→$cDigit")

                Triple(jodi, raw, notes.take(2).joinToString(" • "))
            }.sortedWith(compareByDescending<Triple<String, Double, String>> { it.second }.thenBy { it.first })

            val topRaw = scoredJodis.firstOrNull()?.second?.takeIf { it > 0.0 } ?: 1.0
            scoredJodis.take(8).mapIndexed { idx, (jodi, raw, note) ->
                RankedJodiSuggestion(
                    rank = idx + 1,
                    jodi = jodi,
                    historicalHits = jodiCounts[jodi] ?: 0,
                    currentGap = jodiLastSeen[jodi] ?: totalRecords,
                    statisticalScore = clampScore(40.0 + 55.0 * safeDiv(raw, topRaw)),
                    supportingNote = note
                )
            }
        } else {
            emptyList()
        }

        // --- 6. Hot & Cold Frequencies ---
        val ankFrequencies = (0..9).map { ank ->
            val total = openAnkCounts[ank] + closeAnkCounts[ank]
            AnkFrequencyStat(
                ank = ank,
                totalHits = total,
                openHits = openAnkCounts[ank],
                closeHits = closeAnkCounts[ank],
                sharePercent = safeDiv(total * 100.0, totalAnkOccurrences.toDouble())
            )
        }

        val topJodiFreqs = jodiCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(12)
            .map { it.key to it.value }

        val topPanaFreqs = allPanaCounts.entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(12)
            .map { it.key to it.value }

        // --- 7. Day-Wise Analysis (Monday..Sunday) ---
        val dayWiseStats = (1..7).map { dow ->
            val combinedAnkMap = (0..9).associateWith { ank ->
                dayOpenAnkCounts[dow][ank] + dayCloseAnkCounts[dow][ank]
            }
            val topAnks = combinedAnkMap.entries
                .filter { it.value > 0 }
                .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
                .take(5)
                .map { it.key to it.value }
            val topJodis = dayJodiCounts[dow].entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(5)
                .map { it.key to it.value }
            val topPanas = dayPanaCounts[dow].entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(5)
                .map { it.key to it.value }

            DayWiseStat(
                dayOfWeek = dow,
                dayShortLabel = dayShortNames[dow],
                dayFullLabel = dayFullNames[dow],
                totalDraws = dayTotalDraws[dow],
                ankHits = combinedAnkMap,
                topAnks = topAnks,
                topJodis = topJodis,
                topPanas = topPanas
            )
        }

        // --- 8. Family & Repeat Trends ---
        val familyTrends = MatkaMathEngine.allFamilies.map { fam ->
            FamilyTrendStat(
                familyKey = fam.familyKey,
                baseDigitsLabel = fam.baseDigitsLabel,
                sizeTypeLabel = fam.sizeTypeLabel,
                totalHits = familyTotalHits[fam.familyKey] ?: 0,
                recentHits = familyRecentHits[fam.familyKey] ?: 0,
                drawsSinceLastHit = familyLastSeen[fam.familyKey] ?: totalRecords,
                sampleMembers = fam.members.map { it.pana }
            )
        }.sortedWith(
            compareByDescending<FamilyTrendStat> { it.totalHits }
                .thenByDescending { it.recentHits }
                .thenBy { it.familyKey }
        )

        return MarketAnalyticsSnapshot(
            marketName = marketName,
            totalRecords = totalRecords,
            completeRecords = completeRecords.size,
            verifiedRecords = verifiedCount,
            dateRangeLabel = dateRangeLabel,
            nextDrawDateIso = nextDateIso,
            nextDrawDayOfWeek = nextDow,
            nextDrawDayLabel = nextDayLabel,
            scheduleEvaluation = scheduleEval,
            analyzedAtMillis = nowEpochMillis,
            top5NextGamePanas = top5NextGame,
            openAnkSuggestions = allOpenAnkRanked.take(5),
            openSpSuggestions = openSp,
            openDpSuggestions = openDp,
            closeAnkSuggestions = allCloseAnkRanked.take(5),
            closeSpSuggestions = closeSp,
            closeDpSuggestions = closeDp,
            jodiSuggestions = jodiSuggestions,
            ankFrequencies = ankFrequencies,
            topJodiFrequencies = topJodiFreqs,
            topPanaFrequencies = topPanaFreqs,
            overdueAnkStats = overdueAnkStats,
            dayWiseStats = dayWiseStats,
            openToCloseMatrix = openToCloseMatrix.map { it.toList() },
            familyTrends = familyTrends,
            panaHitCounts = allPanaCounts
        )
    }

    /**
     * Dedicated Live Open-to-Close Predictor.
     *
     * Strictly outputs EXACTLY 4 Single Panas (SP) + 1 Double Pana (DP) ranked by IDX score,
     * excluding Triple Pana (TP) completely.
     */
    fun predictOpenToClose(
        openPana: String,
        rawRecords: List<ResultRecordEntity>
    ): OpenToClosePredictionResult? {
        val openInfo = MatkaMathEngine.getPanaInfo(openPana) ?: return null
        val completeRecords = rawRecords
            .filter { it.isComplete }
            .sortedByDescending { it.dateIso }

        val cutAnk = MatkaMathEngine.cutDigit(openInfo.ank)
        val openFamily = MatkaMathEngine.getFamilyForPana(openInfo.pana)
        val directCutPana = MatkaMathEngine.directCutPana(openInfo.pana)
        val directCutPanaAnk = directCutPana?.let { MatkaMathEngine.calculateAnk(it) } ?: cutAnk

        // --- FACTOR 2: Cut / Mirror & Family Logic (25% weight) ---
        // Standard cut/mirror pairs: 0↔5, 1↔6, 2↔7, 3↔8, 4↔9
        val openPanaCutDigits = listOf(
            MatkaMathEngine.cutDigit(openInfo.d1),
            MatkaMathEngine.cutDigit(openInfo.d2),
            MatkaMathEngine.cutDigit(openInfo.d3)
        )
        val familyAnkCounts = IntArray(10)
        openFamily?.members?.forEach { m -> familyAnkCounts[m.ank]++ }
        val maxFamAnkCount = familyAnkCounts.maxOrNull()?.coerceAtLeast(1) ?: 1

        val cutFamilyNorm = DoubleArray(10)
        val cutFamilyReason = Array(10) { "" }
        for (c in 0..9) {
            var score = 0.0
            val matchedOpenCutCount = openPanaCutDigits.count { it == c }
            val isDirectOpenCut = (c == cutAnk)
            val isDirectCutPanaAnk = (c == directCutPanaAnk)
            val famHits = familyAnkCounts[c]

            if (isDirectOpenCut) {
                score += 0.65
            }
            if (matchedOpenCutCount > 0) {
                score += if (matchedOpenCutCount >= 2) 0.35 else 0.25
            }
            if (isDirectCutPanaAnk) {
                score += 0.20
            }
            if (famHits > 0) {
                score += 0.20 * safeDiv(famHits.toDouble(), maxFamAnkCount.toDouble())
            }
            cutFamilyNorm[c] = score.coerceIn(0.0, 1.0)

            cutFamilyReason[c] = when {
                isDirectOpenCut -> "Cut digit match (${openInfo.ank}↔$c)"
                matchedOpenCutCount > 0 -> {
                    val srcDigit = MatkaMathEngine.cutDigit(c)
                    "Cut of Open digit $srcDigit ($srcDigit↔$c)"
                }
                isDirectCutPanaAnk && directCutPana != null -> "Cut Panna $directCutPana Ank match"
                famHits > 0 -> "Open family (${openInfo.familyKey}) match"
                else -> ""
            }
        }

        // --- Aggregate Real Historical Data (Zero Fake / Zero Random Values) ---
        val exactCloseAnkHits = IntArray(10)
        val familyCloseAnkHits = IntArray(10)
        val ankCloseAnkHits = IntArray(10)
        val cutAnkCloseAnkHits = IntArray(10)
        val overallCloseAnkHits = IntArray(10)

        val exactClosePanaHits = mutableMapOf<String, Int>()
        val familyClosePanaHits = mutableMapOf<String, Int>()
        val ankClosePanaHits = mutableMapOf<String, Int>()
        val cutClosePanaHits = mutableMapOf<String, Int>()
        val overallClosePanaHits = mutableMapOf<String, Int>()
        val closePanaLastSeen = mutableMapOf<String, Int>()
        val closeFamilyHits = mutableMapOf<String, Int>()

        var exactMatches = 0
        var familyMatches = 0
        var openAnkMatches = 0
        var cutAnkMatches = 0

        completeRecords.forEachIndexed { idx, rec ->
            val recOpenPana = rec.openPana ?: return@forEachIndexed
            val recClosePana = rec.closePana ?: return@forEachIndexed
            val recOpenInfo = MatkaMathEngine.getPanaInfo(recOpenPana)
            val recCloseInfo = MatkaMathEngine.getPanaInfo(recClosePana)
            val recOpenAnk = rec.openAnk ?: recOpenInfo?.ank ?: return@forEachIndexed
            val recCloseAnk = rec.closeAnk ?: recCloseInfo?.ank ?: return@forEachIndexed

            overallCloseAnkHits[recCloseAnk]++
            overallClosePanaHits[recClosePana] = (overallClosePanaHits[recClosePana] ?: 0) + 1
            closePanaLastSeen.putIfAbsent(recClosePana, idx)
            if (recCloseInfo != null) {
                closeFamilyHits[recCloseInfo.familyKey] = (closeFamilyHits[recCloseInfo.familyKey] ?: 0) + 1
            }

            if (recOpenPana == openInfo.pana) {
                exactMatches++
                exactCloseAnkHits[recCloseAnk]++
                exactClosePanaHits[recClosePana] = (exactClosePanaHits[recClosePana] ?: 0) + 1
            }
            if (recOpenInfo != null && recOpenInfo.familyKey == openInfo.familyKey) {
                familyMatches++
                familyCloseAnkHits[recCloseAnk]++
                familyClosePanaHits[recClosePana] = (familyClosePanaHits[recClosePana] ?: 0) + 1
            }
            if (recOpenAnk == openInfo.ank) {
                openAnkMatches++
                ankCloseAnkHits[recCloseAnk]++
                ankClosePanaHits[recClosePana] = (ankClosePanaHits[recClosePana] ?: 0) + 1
            }
            if (recOpenAnk == cutAnk) {
                cutAnkMatches++
                cutAnkCloseAnkHits[recCloseAnk]++
                cutClosePanaHits[recClosePana] = (cutClosePanaHits[recClosePana] ?: 0) + 1
            }
        }

        val hasHistory = completeRecords.isNotEmpty()
        val totalComplete = completeRecords.size
        val recentWindowRecords = completeRecords.take(30) // Last 15-30 results for Hot/Due & Jodi patterns
        val recent15Records = completeRecords.take(15)

        // --- FACTOR 1: Historical Open-to-Close Correlation (40% weight) ---
        val maxAnkCorrHits = ankCloseAnkHits.maxOrNull() ?: 0
        val maxExactCorrHits = exactCloseAnkHits.maxOrNull() ?: 0
        val corrNorm = DoubleArray(10)
        for (c in 0..9) {
            if (openAnkMatches > 0 && maxAnkCorrHits > 0) {
                val baseCorr = safeDiv(ankCloseAnkHits[c].toDouble(), maxAnkCorrHits.toDouble())
                val exactBoost = if (exactMatches > 0 && maxExactCorrHits > 0) {
                    safeDiv(exactCloseAnkHits[c].toDouble(), maxExactCorrHits.toDouble())
                } else {
                    0.0
                }
                corrNorm[c] = if (exactMatches > 0 && maxExactCorrHits > 0) {
                    (0.82 * baseCorr + 0.18 * exactBoost).coerceIn(0.0, 1.0)
                } else {
                    baseCorr.coerceIn(0.0, 1.0)
                }
            } else {
                corrNorm[c] = 0.0
            }
        }

        // --- FACTOR 3: Hot & Due (Cold) Trend over last 15-30 draws (20% weight) ---
        val recent30CloseHits = IntArray(10)
        val recent15CloseHits = IntArray(10)
        recentWindowRecords.forEach { rec ->
            val cAnk = rec.closeAnk ?: rec.closePana?.let { MatkaMathEngine.calculateAnk(it) }
            if (cAnk != null && cAnk in 0..9) recent30CloseHits[cAnk]++
        }
        recent15Records.forEach { rec ->
            val cAnk = rec.closeAnk ?: rec.closePana?.let { MatkaMathEngine.calculateAnk(it) }
            if (cAnk != null && cAnk in 0..9) recent15CloseHits[cAnk]++
        }
        val closeAnkGap = IntArray(10) { c ->
            if (!hasHistory) 0
            else completeRecords.indexOfFirst {
                (it.closeAnk ?: it.closePana?.let { p -> MatkaMathEngine.calculateAnk(p) }) == c
            }.let { if (it == -1) totalComplete else it }
        }
        val maxRecent30Hits = recent30CloseHits.maxOrNull() ?: 0
        val maxRecent15Hits = recent15CloseHits.maxOrNull() ?: 0
        val maxCloseGap = closeAnkGap.maxOrNull() ?: 0

        val hotDueNorm = DoubleArray(10)
        val isDueDominant = BooleanArray(10)
        for (c in 0..9) {
            if (!hasHistory) {
                hotDueNorm[c] = 0.0
            } else {
                val hot30 = if (maxRecent30Hits > 0) safeDiv(recent30CloseHits[c].toDouble(), maxRecent30Hits.toDouble()) else 0.0
                val hot15 = if (maxRecent15Hits > 0) safeDiv(recent15CloseHits[c].toDouble(), maxRecent15Hits.toDouble()) else 0.0
                val hotScore = (0.65 * hot30 + 0.35 * hot15).coerceIn(0.0, 1.0)
                val dueScore = if (closeAnkGap[c] >= 3 && maxCloseGap > 0) {
                    safeDiv(closeAnkGap[c].toDouble(), maxCloseGap.toDouble()).coerceIn(0.0, 1.0)
                } else {
                    0.0
                }
                if (dueScore > hotScore) {
                    hotDueNorm[c] = dueScore
                    isDueDominant[c] = true
                } else {
                    hotDueNorm[c] = hotScore
                    isDueDominant[c] = false
                }
            }
        }

        // --- FACTOR 4: Jodi Difference & Total Pattern over recent chart (15% weight) ---
        val jodiTotalFreq = IntArray(10)
        val jodiDiffFreq = IntArray(10)
        recentWindowRecords.forEach { rec ->
            val o = rec.openAnk ?: rec.openPana?.let { MatkaMathEngine.calculateAnk(it) }
            val cl = rec.closeAnk ?: rec.closePana?.let { MatkaMathEngine.calculateAnk(it) }
            if (o != null && cl != null && o in 0..9 && cl in 0..9) {
                jodiTotalFreq[(o + cl) % 10]++
                jodiDiffFreq[((cl - o) % 10 + 10) % 10]++
            }
        }
        val maxJodiTotalFreq = jodiTotalFreq.maxOrNull() ?: 0
        val maxJodiDiffFreq = jodiDiffFreq.maxOrNull() ?: 0

        val jodiPatternNorm = DoubleArray(10)
        for (c in 0..9) {
            if (!hasHistory) {
                jodiPatternNorm[c] = 0.0
            } else {
                val candTotal = (openInfo.ank + c) % 10
                val candDiff = ((c - openInfo.ank) % 10 + 10) % 10
                val totalScore = if (maxJodiTotalFreq > 0) safeDiv(jodiTotalFreq[candTotal].toDouble(), maxJodiTotalFreq.toDouble()) else 0.0
                val diffScore = if (maxJodiDiffFreq > 0) safeDiv(jodiDiffFreq[candDiff].toDouble(), maxJodiDiffFreq.toDouble()) else 0.0
                jodiPatternNorm[c] = (0.50 * totalScore + 0.50 * diffScore).coerceIn(0.0, 1.0)
            }
        }

        // --- Combine Exact Weighted Score (40% + 25% + 20% + 15% = 100%) ---
        val weightedTotalPct = DoubleArray(10)
        val corrPts = IntArray(10)
        val cutPts = IntArray(10)
        val hotDuePts = IntArray(10)
        val jodiPts = IntArray(10)
        val transparentReasons = Array(10) { "" }

        for (c in 0..9) {
            val p1 = 40.0 * corrNorm[c]
            val p2 = 25.0 * cutFamilyNorm[c]
            val p3 = 20.0 * hotDueNorm[c]
            val p4 = 15.0 * jodiPatternNorm[c]
            corrPts[c] = p1.roundToInt()
            cutPts[c] = p2.roundToInt()
            hotDuePts[c] = p3.roundToInt()
            jodiPts[c] = p4.roundToInt()
            weightedTotalPct[c] = (p1 + p2 + p3 + p4).coerceIn(0.0, 100.0)

            // Build clear, factual 1-line reason strictly from the strongest contributing rule(s)
            val factorReasons = mutableListOf<Pair<Double, String>>()
            if (ankCloseAnkHits[c] > 0) {
                val timesWord = if (ankCloseAnkHits[c] == 1) "once" else "${ankCloseAnkHits[c]} times"
                factorReasons.add(p1 to "Followed Open ${openInfo.ank} $timesWord in history")
            }
            if (cutFamilyReason[c].isNotEmpty()) {
                factorReasons.add(p2 to cutFamilyReason[c])
            }
            if (hasHistory && hotDueNorm[c] > 0.0) {
                val trendLabel = if (isDueDominant[c]) {
                    "Due for ${closeAnkGap[c]} draws"
                } else {
                    "Hot: appeared ${recent30CloseHits[c]}× in last ${recentWindowRecords.size} draws"
                }
                factorReasons.add(p3 to trendLabel)
            }
            if (hasHistory && jodiPatternNorm[c] > 0.0) {
                val candTotal = (openInfo.ank + c) % 10
                val candDiff = ((c - openInfo.ank) % 10 + 10) % 10
                val tHits = jodiTotalFreq[candTotal]
                val dHits = jodiDiffFreq[candDiff]
                val jodiLabel = if (dHits >= tHits && dHits > 0) {
                    "Jodi diff +$candDiff hit ${dHits}× recently"
                } else if (tHits > 0) {
                    "Jodi total $candTotal hit ${tHits}× recently"
                } else {
                    ""
                }
                if (jodiLabel.isNotEmpty()) {
                    factorReasons.add(p4 to jodiLabel)
                }
            }

            val topReason = factorReasons
                .sortedByDescending { it.first }
                .firstOrNull()?.second
                ?: if (!hasHistory) "Paste chart data to score historical trends" else "Baseline coverage"
            transparentReasons[c] = topReason
        }

        // Rank all 10 Close digits strictly by weightedTotalPct descending (deterministic tie-breaks)
        val sortedDigits = (0..9).sortedWith(
            compareByDescending<Int> { weightedTotalPct[it] }
                .thenByDescending { ankCloseAnkHits[it] }
                .thenByDescending { cutFamilyNorm[it] }
                .thenByDescending { closeAnkGap[it] }
                .thenBy { it }
        )

        val denomAnk = if (openAnkMatches > 0) openAnkMatches else totalComplete.coerceAtLeast(1)
        val rankedCloseAnks = sortedDigits.mapIndexed { idx, cAnk ->
            val hitCount = if (openAnkMatches > 0) ankCloseAnkHits[cAnk] else overallCloseAnkHits[cAnk]
            RankedAnkSuggestion(
                rank = idx + 1,
                ank = cAnk,
                historicalHits = hitCount,
                hitRatePercent = if (hasHistory) safeDiv(hitCount * 100.0, denomAnk.toDouble()) else 0.0,
                currentGap = closeAnkGap[cAnk],
                statisticalScore = weightedTotalPct[cAnk].roundToInt().coerceIn(0, 100),
                supportingNote = transparentReasons[cAnk]
            )
        }

        // --- Score Valid 210 SP & DP Close Panas strictly from real history + weighted Close Ank score ---
        val maxOverallPana = (overallClosePanaHits.values.maxOrNull() ?: 1).coerceAtLeast(1)
        val maxCloseFam = (closeFamilyHits.values.maxOrNull() ?: 1).coerceAtLeast(1)
        val safeTotal = totalComplete.coerceAtLeast(1)

        val scoredAll = MatkaMathEngine.spAndDpPanas.map { info ->
            val closeDigitScore = weightedTotalPct[info.ank] / 100.0
            val exactHit = exactClosePanaHits[info.pana] ?: 0
            val famHit = familyClosePanaHits[info.pana] ?: 0
            val ankHit = ankClosePanaHits[info.pana] ?: 0
            val overallHit = overallClosePanaHits[info.pana] ?: 0
            val gap = closePanaLastSeen[info.pana] ?: safeTotal
            val isDirectCut = info.pana == directCutPana
            val isSameFamily = info.familyKey == openInfo.familyKey
            val famAffinity = if (hasHistory) safeDiv((closeFamilyHits[info.familyKey] ?: 0).toDouble(), maxCloseFam.toDouble()) else 0.0

            val raw = if (hasHistory) {
                0.45 * closeDigitScore +
                    0.20 * safeDiv(exactHit.toDouble(), exactMatches.coerceAtLeast(1).toDouble()) +
                    0.12 * safeDiv(ankHit.toDouble(), openAnkMatches.coerceAtLeast(1).toDouble()) +
                    0.09 * safeDiv(famHit.toDouble(), familyMatches.coerceAtLeast(1).toDouble()) +
                    0.07 * safeDiv(overallHit.toDouble(), maxOverallPana.toDouble()) +
                    0.04 * famAffinity +
                    0.03 * (if (isDirectCut) 1.0 else if (isSameFamily) 0.5 else 0.0)
            } else {
                0.65 * closeDigitScore +
                    0.20 * (if (isDirectCut) 1.0 else 0.0) +
                    0.15 * (if (isSameFamily) 1.0 else 0.0)
            }

            val jodi = "${openInfo.ank}${info.ank}"
            val reason = when {
                exactHit > 0 -> "Followed ${openInfo.pana} ${exactHit}× • Jodi $jodi"
                ankHit > 0 -> "Followed Open ${openInfo.ank} ${ankHit}× • Jodi $jodi"
                isDirectCut -> "Direct cut of ${openInfo.pana} • Jodi $jodi"
                famHit > 0 -> "Family match ×$famHit • Jodi $jodi"
                overallHit > 0 -> "Close hit ×$overallHit • Jodi $jodi"
                else -> "${transparentReasons[info.ank]} • Jodi $jodi"
            }

            Triple(info, raw, reason to jodi)
        }.sortedWith(
            compareByDescending<Triple<PanaInfo, Double, Pair<String, String>>> { it.second }
                .thenByDescending { overallClosePanaHits[it.first.pana] ?: 0 }
                .thenBy { it.first.pana }
        )

        val unifiedRanked = scoredAll.mapIndexed { idx, (info, raw, pair) ->
            val honestScore = if (hasHistory) {
                (raw * 100.0).roundToInt().coerceIn(1, 99)
            } else {
                // Strictly bounded by Cut/Mirror & Family weight when no historical chart exists
                weightedTotalPct[info.ank].roundToInt().coerceIn(0, 100)
            }
            RankedPanaSuggestion(
                rank = idx + 1,
                pana = info.pana,
                category = info.category,
                ank = info.ank,
                historicalHits = overallClosePanaHits[info.pana] ?: 0,
                currentGap = if (hasHistory) (closePanaLastSeen[info.pana] ?: totalComplete) else 0,
                statisticalScore = honestScore,
                reason = pair.first,
                predictedJodi = pair.second
            )
        }

        // Strict 4 SP + 1 DP quota enforced for Top 5 Close Panas (TP excluded)
        val top5Close = enforceStrict4Sp1DpQuota(unifiedRanked)
        val spClose = unifiedRanked.filter { it.category == PanaCategory.SP }.take(5)
            .mapIndexed { i, s -> s.copy(rank = i + 1) }
        val dpClose = unifiedRanked.filter { it.category == PanaCategory.DP }.take(5)
            .mapIndexed { i, s -> s.copy(rank = i + 1) }

        // Top 5 Suggested Close Numbers (0-9) with honest weighted % and 2 matching Close Panas each
        val top5Digits = sortedDigits.take(5).mapIndexed { idx, cAnk ->
            val matchingPanas = unifiedRanked
                .filter { it.ank == cAnk }
                .take(2)
                .map { it.pana }
            SuggestedCloseDigit(
                rank = idx + 1,
                closeDigit = cAnk,
                confidencePercent = weightedTotalPct[cAnk].roundToInt().coerceIn(0, 100),
                jodi = "${openInfo.ank}$cAnk",
                matchingClosePanas = matchingPanas,
                patternReason = transparentReasons[cAnk],
                correlationScorePct = corrPts[cAnk],
                cutFamilyScorePct = cutPts[cAnk],
                hotDueScorePct = hotDuePts[cAnk],
                jodiPatternScorePct = jodiPts[cAnk]
            )
        }

        val summaryNote = if (!hasHistory) {
            "0 historical draws stored — showing Cut/Mirror & Family matches only (25% max). Paste or update chart data to unlock full 100% correlation, Hot/Due & Jodi scoring."
        } else {
            "4-Factor Real Data Analysis across $totalComplete draws (Open ${openInfo.ank} seen ${openAnkMatches}× • Recent window: ${recentWindowRecords.size} draws)."
        }

        return OpenToClosePredictionResult(
            openPana = openInfo.pana,
            openAnk = openInfo.ank,
            openCategory = openInfo.category,
            familyKey = openInfo.familyKey,
            cutAnk = cutAnk,
            totalAnalyzedCompleteDraws = totalComplete,
            exactOpenPanaMatches = exactMatches,
            familyOpenMatches = familyMatches,
            openAnkMatches = openAnkMatches,
            cutAnkMatches = cutAnkMatches,
            isPatternOnlyMode = !hasHistory,
            top5CloseDigits = top5Digits,
            top5ClosePanas = top5Close,
            rankedCloseAnks = rankedCloseAnks.take(5),
            spClosePanas = spClose,
            dpClosePanas = dpClose,
            note = summaryNote
        )
    }

    /**
     * Strict 4 SP + 1 DP Quota:
     * - Picks the Top 4 highest-scoring Single Panas (SP)
     * - Picks the single #1 highest-scoring Double Pana (DP)
     * - Excludes Triple Pana (TP) completely
     * - Combines all 5 and sorts descending by statisticalScore (IDX)
     */
    private fun enforceStrict4Sp1DpQuota(rankedUnified: List<RankedPanaSuggestion>): List<RankedPanaSuggestion> {
        val top4Sp = rankedUnified
            .filter { it.category == PanaCategory.SP }
            .take(4)

        val top1Dp = rankedUnified
            .filter { it.category == PanaCategory.DP }
            .take(1)

        return (top4Sp + top1Dp)
            .sortedWith(
                compareByDescending<RankedPanaSuggestion> { it.statisticalScore }
                    .thenBy { it.rank }
                    .thenBy { it.pana }
            )
            .mapIndexed { idx, item -> item.copy(rank = idx + 1) }
    }

    fun calendarDayToIso(calendarDow: Int): Int = when (calendarDow) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}
