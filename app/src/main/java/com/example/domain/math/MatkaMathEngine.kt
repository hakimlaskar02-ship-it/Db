package com.example.domain.math

/**
 * Pure deterministic Matka mathematical rules engine.
 *
 * Digit Order Rule (0 = 10):
 *   1 < 2 < 3 < 4 < 5 < 6 < 7 < 8 < 9 < 0
 *
 * Valid Pana Rule:
 *   A 3-digit string (d1, d2, d3) is valid iff rank(d1) <= rank(d2) <= rank(d3).
 *   Total valid Panas = C(10 + 3 - 1, 3) = 220:
 *     - Single Pana (SP): 120 (all 3 digits distinct)
 *     - Double Pana (DP): 90  (exactly 2 digits equal)
 *     - Triple Pana (TP): 10  (all 3 digits equal: 111..999, 000)
 *
 * Cut Digits:
 *   0 <-> 5, 1 <-> 6, 2 <-> 7, 3 <-> 8, 4 <-> 9  (i.e. (d + 5) % 10)
 */
enum class PanaCategory(val code: String, val fullLabel: String) {
    SP("SP", "Single Pana"),
    DP("DP", "Double Pana"),
    TP("TP", "Triple Pana")
}

data class PanaInfo(
    val pana: String,
    val d1: Int,
    val d2: Int,
    val d3: Int,
    val ank: Int,
    val category: PanaCategory,
    val familyKey: String
)

data class PanaFamily(
    val familyKey: String,
    val baseDigitsLabel: String,
    val members: List<PanaInfo>,
    val sizeTypeLabel: String // "8-Pana Family", "6-Pana Family", "4-Pana Family"
)

sealed interface PanaValidationResult {
    data class Valid(val info: PanaInfo, val family: PanaFamily) : PanaValidationResult
    data class Invalid(val reason: String, val suggestedCanonical: String? = null) : PanaValidationResult
}

object MatkaMathEngine {

    /** Strict Matka digit rank where 0 is the largest digit (rank 10). */
    fun digitRank(digit: Int): Int = if (digit == 0) 10 else digit

    /** Cut digit pair: 0↔5, 1↔6, 2↔7, 3↔8, 4↔9. */
    fun cutDigit(digit: Int): Int = (digit + 5) % 10

    /** Canonical cut-pair base digit in 1..5: {1,6}->1, {2,7}->2, {3,8}->3, {4,9}->4, {5,0}->5. */
    fun cutPairBase(digit: Int): Int {
        val mod = digit % 5
        return if (mod == 0) 5 else mod
    }

    /** Ank = (d1 + d2 + d3) % 10. */
    fun calculateAnk(d1: Int, d2: Int, d3: Int): Int = (d1 + d2 + d3) % 10

    fun calculateAnk(pana: String): Int? {
        if (pana.length != 3 || !pana.all { it.isDigit() }) return null
        return calculateAnk(pana[0] - '0', pana[1] - '0', pana[2] - '0')
    }

    /** Sorts any 3 digits into valid Matka order (1..9, 0). */
    fun canonicalizeDigits(d1: Int, d2: Int, d3: Int): String {
        val sorted = listOf(d1, d2, d3).sortedBy { digitRank(it) }
        return "${sorted[0]}${sorted[1]}${sorted[2]}"
    }

    /** Canonical family key derived from sorted cut-pair bases (each in 1..5). */
    fun familyKeyFor(d1: Int, d2: Int, d3: Int): String {
        val bases = listOf(cutPairBase(d1), cutPairBase(d2), cutPairBase(d3)).sorted()
        return "${bases[0]}${bases[1]}${bases[2]}"
    }

    /** Strictly validates a 3-digit Pana string against Matka digit ordering. */
    fun isValidPana(input: String): Boolean {
        if (input.length != 3 || !input.all { it.isDigit() }) return false
        val r1 = digitRank(input[0] - '0')
        val r2 = digitRank(input[1] - '0')
        val r3 = digitRank(input[2] - '0')
        return r1 <= r2 && r2 <= r3
    }

    fun categoryOf(d1: Int, d2: Int, d3: Int): PanaCategory = when {
        d1 == d2 && d2 == d3 -> PanaCategory.TP
        d1 == d2 || d2 == d3 || d1 == d3 -> PanaCategory.DP
        else -> PanaCategory.SP
    }

    /** Mathematically generated complete set of all 220 valid Matka Panas. */
    val allPanas: List<PanaInfo> by lazy {
        val orderedDigits = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 0)
        val result = ArrayList<PanaInfo>(220)
        for (i in orderedDigits.indices) {
            for (j in i until orderedDigits.size) {
                for (k in j until orderedDigits.size) {
                    val d1 = orderedDigits[i]
                    val d2 = orderedDigits[j]
                    val d3 = orderedDigits[k]
                    val panaStr = "$d1$d2$d3"
                    result.add(
                        PanaInfo(
                            pana = panaStr,
                            d1 = d1,
                            d2 = d2,
                            d3 = d3,
                            ank = calculateAnk(d1, d2, d3),
                            category = categoryOf(d1, d2, d3),
                            familyKey = familyKeyFor(d1, d2, d3)
                        )
                    )
                }
            }
        }
        result.toList()
    }

    val panaMap: Map<String, PanaInfo> by lazy {
        allPanas.associateBy { it.pana }
    }

    val singlePanas: List<PanaInfo> by lazy { allPanas.filter { it.category == PanaCategory.SP } } // 120
    val doublePanas: List<PanaInfo> by lazy { allPanas.filter { it.category == PanaCategory.DP } } // 90
    val triplePanas: List<PanaInfo> by lazy { allPanas.filter { it.category == PanaCategory.TP } } // 10
    val spAndDpPanas: List<PanaInfo> by lazy { allPanas.filter { it.category != PanaCategory.TP } } // 210

    val panasByAnk: Map<Int, List<PanaInfo>> by lazy {
        allPanas.groupBy { it.ank }
    }

    /** Complete mathematically partitioned Pana families (10×8-Pana, 20×6-Pana, 5×4-Pana = 220). */
    val allFamilies: List<PanaFamily> by lazy {
        allPanas.groupBy { it.familyKey }
            .entries
            .sortedBy { it.key }
            .map { (key, members) ->
                val sortedMembers = members.sortedWith(
                    compareBy<PanaInfo> { it.category.ordinal }
                        .thenBy { digitRank(it.d1) }
                        .thenBy { digitRank(it.d2) }
                        .thenBy { digitRank(it.d3) }
                )
                val sizeLabel = "${sortedMembers.size}-Pana Family"
                val pairsDesc = key.map { ch ->
                    val base = ch - '0'
                    val cut = cutDigit(base)
                    "{$base/$cut}"
                }.joinToString("·")
                PanaFamily(
                    familyKey = key,
                    baseDigitsLabel = pairsDesc,
                    members = sortedMembers,
                    sizeTypeLabel = sizeLabel
                )
            }
    }

    val familyMap: Map<String, PanaFamily> by lazy {
        allFamilies.associateBy { it.familyKey }
    }

    fun getPanaInfo(pana: String): PanaInfo? = panaMap[pana.trim()]

    fun getFamilyForPana(pana: String): PanaFamily? {
        val info = getPanaInfo(pana) ?: return null
        return familyMap[info.familyKey]
    }

    fun validatePana(rawInput: String): PanaValidationResult {
        val cleaned = rawInput.trim()
        if (cleaned.isEmpty()) {
            return PanaValidationResult.Invalid("Enter a 3-digit Pana")
        }
        if (cleaned.length != 3 || !cleaned.all { it.isDigit() }) {
            return PanaValidationResult.Invalid("Pana must be exactly 3 digits (0–9)")
        }
        val d1 = cleaned[0] - '0'
        val d2 = cleaned[1] - '0'
        val d3 = cleaned[2] - '0'
        val canonical = canonicalizeDigits(d1, d2, d3)
        if (!isValidPana(cleaned)) {
            return PanaValidationResult.Invalid(
                reason = "Invalid digit order ($cleaned). In Matka, 1 < 2 < … < 9 < 0 — valid form is $canonical.",
                suggestedCanonical = canonical
            )
        }
        val info = panaMap[cleaned]!!
        val family = familyMap[info.familyKey]!!
        return PanaValidationResult.Valid(info, family)
    }

    /** Cut-pair formatted string for a Pana (e.g., 179 -> 624 sorted -> 246). */
    fun directCutPana(pana: String): String? {
        val info = getPanaInfo(pana) ?: return null
        return canonicalizeDigits(cutDigit(info.d1), cutDigit(info.d2), cutDigit(info.d3))
    }

    /** Validates a full or open-only Matka result row for strict mathematical consistency. */
    fun validateResultConsistency(
        openPana: String?,
        jodi: String?,
        closePana: String?
    ): String? {
        val open = openPana?.trim()?.takeIf { it.isNotEmpty() }
        val close = closePana?.trim()?.takeIf { it.isNotEmpty() }
        val jodiStr = jodi?.trim()?.takeIf { it.isNotEmpty() }

        if (open == null) return "Open Pana is missing"
        when (val openCheck = validatePana(open)) {
            is PanaValidationResult.Invalid -> return "Open Pana error: ${openCheck.reason}"
            is PanaValidationResult.Valid -> {
                val openAnk = openCheck.info.ank
                if (close != null) {
                    when (val closeCheck = validatePana(close)) {
                        is PanaValidationResult.Invalid -> return "Close Pana error: ${closeCheck.reason}"
                        is PanaValidationResult.Valid -> {
                            val closeAnk = closeCheck.info.ank
                            val expectedJodi = "$openAnk$closeAnk"
                            if (jodiStr != null && jodiStr != expectedJodi) {
                                return "Jodi mismatch: $open (Ank $openAnk) + $close (Ank $closeAnk) = $expectedJodi, not $jodiStr"
                            }
                        }
                    }
                } else if (jodiStr != null) {
                    if (jodiStr.length == 1 && jodiStr[0].isDigit()) {
                        if (jodiStr[0] - '0' != openAnk) {
                            return "Open Ank mismatch: $open sums to $openAnk, not $jodiStr"
                        }
                    } else if (jodiStr.length == 2 && jodiStr.all { it.isDigit() }) {
                        if (jodiStr[0] - '0' != openAnk) {
                            return "Open Ank mismatch: $open sums to $openAnk, Jodi starts with ${jodiStr[0]}"
                        }
                    } else {
                        return "Invalid Jodi format: $jodiStr"
                    }
                }
            }
        }
        return null
    }
}
