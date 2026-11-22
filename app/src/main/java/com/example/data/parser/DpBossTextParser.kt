package com.example.data.parser

import com.example.data.local.RecordSource
import com.example.data.local.ResultRecordEntity
import com.example.domain.analytics.MatkaAnalyticsEngine
import com.example.domain.math.MatkaMathEngine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ParsedRowStatus {
    VALID_NEW,
    OPEN_ONLY,
    DUPLICATE,
    CONFLICT,
    INVALID
}

data class ParsedImportRow(
    val id: Int,
    val rawSnippet: String,
    val marketName: String,
    val dateIso: String,
    val dayOfWeek: Int,
    val openPana: String?,
    val openAnk: Int?,
    val jodi: String?,
    val closePana: String?,
    val closeAnk: Int?,
    val status: ParsedRowStatus,
    val message: String,
    val existingSummary: String? = null
)

data class ParsePreviewReport(
    val rows: List<ParsedImportRow>,
    val validNewCount: Int,
    val openOnlyCount: Int,
    val duplicateCount: Int,
    val conflictCount: Int,
    val invalidCount: Int
) {
    val saveableRows: List<ParsedImportRow>
        get() = rows.filter { it.status == ParsedRowStatus.VALID_NEW || it.status == ParsedRowStatus.OPEN_ONLY }
}

/**
 * Smart multi-format parser tailored for dpbosssattamatka.com copy-paste outputs:
 * 1. DPBoss Home inline results ("KALYAN\n259-67-449" or "KALYAN 259-67-449" or "259-6")
 * 2. DPBoss Vertical/Tabular Weekly Panel Charts ("10/02/2025 to 15/02/2025" with 3-line stacked Panas)
 * 3. Tabular / CSV date rows ("14/02/2025 179-74-248", "2025-02-14, 179, 74, 248")
 */
object DpBossTextParser {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

    // Matches 259-67-449, 259 - 67 - 449, 259-6*-***, 259-6
    private val fullResultRegex = Regex("""(?<!\d)(\d{3})\s*[-–—]\s*(\d{2})\s*[-–—]\s*(\d{3})(?!\d)""")
    private val openOnlyRegex = Regex("""(?<!\d)(\d{3})\s*[-–—]\s*(\d)(?:\*|\s*[-–—]\s*\*+)?(?!\d)""")
    private val csvThreePartsRegex = Regex("""(?<!\d)(\d{3})\s*[,|\t]\s*(\d{2})\s*[,|\t]\s*(\d{3})(?!\d)""")

    // Date formats: yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy, dd/MM/yy, dd-MM-yy
    private val dateRegex = Regex("""\b(\d{4}[-/]\d{1,2}[-/]\d{1,2}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4})\b""")
    private val dateRangeRegex = Regex(
        """(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})\s*(?:to|TO|To|-|–|—)\s*(\d{1,2}[-/]\d{1,2}[-/]\d{2,4})"""
    )

    // Known DPBoss market keywords to help detect market headers cleanly
    private val marketHeaderRegex = Regex("""^[A-Za-z][A-Za-z0-9\s&.'/-]{2,36}$""")
    private val ignoredHeaderWords = setOf(
        "PANEL CHART", "JODI CHART", "DATE", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN",
        "OPEN", "CLOSE", "JODI", "LOADING", "REFRESH", "LIVE RESULT", "DPBOSS", "SATTA MATKA",
        "HOME", "CHARTS", "RESULT", "RESULTS", "TO"
    )

    fun parseText(
        rawText: String,
        defaultMarketName: String,
        existingRecords: List<ResultRecordEntity>
    ): ParsePreviewReport {
        val cleanedText = rawText.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (cleanedText.isBlank()) {
            return ParsePreviewReport(emptyList(), 0, 0, 0, 0, 0)
        }

        val existingMap = existingRecords.associateBy { "${it.marketName.uppercase()}|${it.dateIso}" }
        val extracted = mutableListOf<CandidateRow>()

        // Detect if text contains DPBoss vertical weekly panel chart blocks
        val lines = cleanedText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val usedLineIndices = mutableSetOf<Int>()

        // Pass 1: Detect DPBoss Vertical Weekly Panel Chart blocks ("dd/MM/yyyy to dd/MM/yyyy")
        parseVerticalPanelBlocks(lines, defaultMarketName, usedLineIndices, extracted)

        // Pass 2: Parse remaining lines (Home inline, date-prefixed, CSV, Market header + result)
        val todayIso = todayDateIso()
        var activeMarket = defaultMarketName.trim().ifEmpty { "KALYAN" }.uppercase()
        var syntheticOffsetDays = 0

        for (i in lines.indices) {
            if (i in usedLineIndices) continue
            val line = lines[i]

            val dateMatch = dateRegex.find(line)
            val parsedDate = dateMatch?.let { normalizeDateToIso(it.value) }

            val fullMatches = fullResultRegex.findAll(line).toList()
            val csvMatches = if (fullMatches.isEmpty()) csvThreePartsRegex.findAll(line).toList() else emptyList()

            if (fullMatches.isNotEmpty() || csvMatches.isNotEmpty()) {
                val matches = if (fullMatches.isNotEmpty()) fullMatches else csvMatches
                // Check if market name appears before the first match on the same line
                val prefix = line.substring(0, matches.first().range.first)
                    .replace(dateRegex, "")
                    .replace(",", " ")
                    .replace("|", " ")
                    .replace(":", " ")
                    .trim()
                val lineMarket = extractMarketName(prefix) ?: activeMarket

                matches.forEachIndexed { mIdx, m ->
                    val openP = m.groupValues[1]
                    val jodi = m.groupValues[2]
                    val closeP = m.groupValues[3]
                    val rowDate = when {
                        parsedDate != null && matches.size == 1 -> parsedDate
                        parsedDate != null -> addDaysToIso(parsedDate, mIdx)
                        else -> addDaysToIso(todayIso, -syntheticOffsetDays)
                    }
                    if (parsedDate == null && matches.size > 1) {
                        syntheticOffsetDays++
                    }
                    extracted.add(
                        CandidateRow(
                            rawSnippet = line.take(72),
                            marketName = lineMarket,
                            dateIso = rowDate,
                            openPana = openP,
                            jodi = jodi,
                            closePana = closeP
                        )
                    )
                }
                continue
            }

            // Check Open-Only format ("259-6" or "259-6*-***")
            val openOnlyMatch = openOnlyRegex.find(line)
            if (openOnlyMatch != null) {
                val prefix = line.substring(0, openOnlyMatch.range.first)
                    .replace(dateRegex, "")
                    .replace(":", " ")
                    .trim()
                val lineMarket = extractMarketName(prefix) ?: activeMarket
                val rowDate = parsedDate ?: todayIso
                extracted.add(
                    CandidateRow(
                        rawSnippet = line.take(72),
                        marketName = lineMarket,
                        dateIso = rowDate,
                        openPana = openOnlyMatch.groupValues[1],
                        jodi = openOnlyMatch.groupValues[2],
                        closePana = null
                    )
                )
                continue
            }

            // Otherwise check if this line is a standalone Market Name header (e.g. "KALYAN", "MILAN DAY")
            val maybeMarket = extractMarketName(line)
            if (maybeMarket != null) {
                activeMarket = maybeMarket
            }
        }

        // Deduplicate within the same paste batch (keep latest occurrence per market+date) and validate strictly
        val seenBatch = mutableSetOf<String>()
        val finalRows = extracted.mapIndexed { idx, candidate ->
            evaluateCandidate(
                id = idx + 1,
                candidate = candidate,
                existingMap = existingMap,
                seenBatch = seenBatch
            )
        }

        return ParsePreviewReport(
            rows = finalRows,
            validNewCount = finalRows.count { it.status == ParsedRowStatus.VALID_NEW },
            openOnlyCount = finalRows.count { it.status == ParsedRowStatus.OPEN_ONLY },
            duplicateCount = finalRows.count { it.status == ParsedRowStatus.DUPLICATE },
            conflictCount = finalRows.count { it.status == ParsedRowStatus.CONFLICT },
            invalidCount = finalRows.count { it.status == ParsedRowStatus.INVALID }
        )
    }

    /**
     * Parses DPBoss vertical panel chart layout where a date range (e.g., "10/02/2025 to 15/02/2025")
     * is followed by 3 stacked lines representing Mon..Sat/Sun Open digits, Jodis, and Close digits.
     *
     * Example DPBoss Panel copy-paste:
     * 10/02/2025 to 15/02/2025
     * 1 2 3 4 ... (row 1: top digit of each open/close)
     * 2 54 6 8 32 9 ... (row 2: middle digit + 2-digit jodi + middle digit)
     * 9 0 7 8 ... (row 3: bottom digit of each open/close)
     */
    private fun parseVerticalPanelBlocks(
        lines: List<String>,
        defaultMarketName: String,
        usedIndices: MutableSet<Int>,
        out: MutableList<CandidateRow>
    ) {
        var activeMarket = defaultMarketName.trim().ifEmpty { "KALYAN" }.uppercase()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val rangeMatch = dateRangeRegex.find(line)
            if (rangeMatch == null) {
                extractMarketName(line)?.let { activeMarket = it }
                i++
                continue
            }

            val startIso = normalizeDateToIso(rangeMatch.groupValues[1])
            if (startIso == null) {
                i++
                continue
            }

            // Look ahead up to 3 lines for stacked panel digits
            if (i + 3 < lines.size) {
                val r1 = lines[i + 1].split(Regex("""[\s\t|]+""")).filter { it.isNotEmpty() }
                val r2 = lines[i + 2].split(Regex("""[\s\t|]+""")).filter { it.isNotEmpty() }
                val r3 = lines[i + 3].split(Regex("""[\s\t|]+""")).filter { it.isNotEmpty() }

                val looksStacked = r1.isNotEmpty() && r2.isNotEmpty() && r3.isNotEmpty() &&
                    r1.all { it.length == 1 && it[0].isDigit() || it == "*" } &&
                    r3.all { it.length == 1 && it[0].isDigit() || it == "*" } &&
                    r1.size % 2 == 0 && r3.size == r1.size &&
                    r2.size == (r1.size / 2) * 3

                if (looksStacked) {
                    val daysInWeek = r1.size / 2
                    for (dayIdx in 0 until daysInWeek) {
                        val o1 = r1[dayIdx * 2]
                        val c1 = r1[dayIdx * 2 + 1]
                        val o2 = r2[dayIdx * 3]
                        val jodi = r2[dayIdx * 3 + 1]
                        val c2 = r2[dayIdx * 3 + 2]
                        val o3 = r3[dayIdx * 2]
                        val c3 = r3[dayIdx * 2 + 1]

                        if (o1 == "*" || jodi.contains("*")) continue

                        val openPana = "$o1$o2$o3"
                        val closePana = if (c1 == "*" || c2 == "*" || c3 == "*") null else "$c1$c2$c3"
                        val dateIso = addDaysToIso(startIso, dayIdx)
                        out.add(
                            CandidateRow(
                                rawSnippet = "${rangeMatch.value} [Day ${dayIdx + 1}: $openPana-$jodi-${closePana ?: "***"}]",
                                marketName = activeMarket,
                                dateIso = dateIso,
                                openPana = openPana,
                                jodi = jodi,
                                closePana = closePana
                            )
                        )
                    }
                    usedIndices.add(i)
                    usedIndices.add(i + 1)
                    usedIndices.add(i + 2)
                    usedIndices.add(i + 3)
                    i += 4
                    continue
                }
            }
            i++
        }
    }

    private fun evaluateCandidate(
        id: Int,
        candidate: CandidateRow,
        existingMap: Map<String, ResultRecordEntity>,
        seenBatch: MutableSet<String>
    ): ParsedImportRow {
        val market = candidate.marketName.trim().uppercase().ifEmpty { "KALYAN" }
        val dow = isoDateToDayOfWeek(candidate.dateIso)
        val openP = candidate.openPana?.trim()?.takeIf { it.isNotEmpty() }
        val closeP = candidate.closePana?.trim()?.takeIf { it.isNotEmpty() }
        val rawJodi = candidate.jodi?.trim()?.takeIf { it.isNotEmpty() }

        val validationError = MatkaMathEngine.validateResultConsistency(openP, rawJodi, closeP)
        if (validationError != null) {
            return ParsedImportRow(
                id = id,
                rawSnippet = candidate.rawSnippet,
                marketName = market,
                dateIso = candidate.dateIso,
                dayOfWeek = dow,
                openPana = openP,
                openAnk = openP?.let { MatkaMathEngine.calculateAnk(it) },
                jodi = rawJodi,
                closePana = closeP,
                closeAnk = closeP?.let { MatkaMathEngine.calculateAnk(it) },
                status = ParsedRowStatus.INVALID,
                message = validationError
            )
        }

        val openAnk = openP?.let { MatkaMathEngine.calculateAnk(it) }
        val closeAnk = closeP?.let { MatkaMathEngine.calculateAnk(it) }
        val normalizedJodi = if (openAnk != null && closeAnk != null) {
            "$openAnk$closeAnk"
        } else {
            rawJodi?.takeIf { it.length == 2 }
        }

        val key = "$market|${candidate.dateIso}"
        val existing = existingMap[key]
        val isDuplicateInBatch = !seenBatch.add(key)

        val statusAndMsg = when {
            existing != null -> {
                val sameOpen = existing.openPana == openP
                val sameClose = existing.closePana == closeP
                if (sameOpen && sameClose) {
                    ParsedRowStatus.DUPLICATE to "Already saved (${existing.formattedResult})"
                } else {
                    ParsedRowStatus.CONFLICT to "Conflicts with saved ${existing.formattedResult}"
                }
            }
            isDuplicateInBatch -> ParsedRowStatus.DUPLICATE to "Duplicate date inside pasted block"
            closeP == null -> ParsedRowStatus.OPEN_ONLY to "Valid Open Pana (Close pending)"
            else -> ParsedRowStatus.VALID_NEW to "Verified Matka Pana & Jodi math"
        }

        return ParsedImportRow(
            id = id,
            rawSnippet = candidate.rawSnippet,
            marketName = market,
            dateIso = candidate.dateIso,
            dayOfWeek = dow,
            openPana = openP,
            openAnk = openAnk,
            jodi = normalizedJodi,
            closePana = closeP,
            closeAnk = closeAnk,
            status = statusAndMsg.first,
            message = statusAndMsg.second,
            existingSummary = existing?.formattedResult
        )
    }

    fun toRecordEntity(row: ParsedImportRow, source: RecordSource): ResultRecordEntity {
        return ResultRecordEntity(
            marketName = row.marketName,
            dateIso = row.dateIso,
            dayOfWeek = row.dayOfWeek,
            openPana = row.openPana,
            openAnk = row.openAnk,
            closePana = row.closePana,
            closeAnk = row.closeAnk,
            jodi = row.jodi,
            source = source.name,
            isVerified = row.closePana != null
        )
    }

    private fun extractMarketName(raw: String): String? {
        val cleaned = raw.replace(Regex("""[^A-Za-z0-9\s&.'/-]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (cleaned.length < 3 || cleaned.length > 36) return null
        if (!cleaned.any { it.isLetter() }) return null
        if (cleaned.all { it.isDigit() || it == '-' || it == '/' || it == ' ' }) return null
        val upper = cleaned.uppercase()
        if (upper in ignoredHeaderWords) return null
        if (upper.startsWith("HTTP") || upper.startsWith("WWW")) return null
        if (!marketHeaderRegex.matches(cleaned)) return null
        return upper
    }

    fun normalizeDateToIso(raw: String): String? {
        val normalized = raw.trim().replace('/', '-')
        val parts = normalized.split('-')
        if (parts.size != 3) return null
        val p0 = parts[0].toIntOrNull() ?: return null
        val p1 = parts[1].toIntOrNull() ?: return null
        val p2 = parts[2].toIntOrNull() ?: return null

        val (year, month, day) = when {
            parts[0].length == 4 -> Triple(p0, p1, p2) // yyyy-MM-dd
            else -> {
                val fullYear = if (p2 < 100) 2000 + p2 else p2
                Triple(fullYear, p1, p0) // dd-MM-yyyy (standard Indian DPBoss format)
            }
        }
        if (year !in 1990..2100 || month !in 1..12 || day !in 1..31) return null
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun todayDateIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }

    fun addDaysToIso(dateIso: String, deltaDays: Int): String {
        return try {
            val parsed = isoFormat.parse(dateIso) ?: return dateIso
            val cal = Calendar.getInstance().apply {
                time = parsed
                add(Calendar.DAY_OF_YEAR, deltaDays)
            }
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        } catch (_: Exception) {
            dateIso
        }
    }

    fun isoDateToDayOfWeek(dateIso: String): Int {
        return try {
            val parsed = isoFormat.parse(dateIso) ?: return 1
            val cal = Calendar.getInstance().apply { time = parsed }
            MatkaAnalyticsEngine.calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))
        } catch (_: Exception) {
            1
        }
    }

    private data class CandidateRow(
        val rawSnippet: String,
        val marketName: String,
        val dateIso: String,
        val openPana: String?,
        val jodi: String?,
        val closePana: String?
    )
}
