package com.example.data.remote

import com.example.data.local.RecordSource
import com.example.data.local.ResultRecordEntity
import com.example.data.parser.DpBossTextParser
import com.example.domain.math.MatkaMathEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed interface LiveSyncResult {
    data class Success(
        val records: List<ResultRecordEntity>,
        val viaSourceLabel: String
    ) : LiveSyncResult

    data class BlockedOrFailed(
        val reason: String,
        val isCloudflareBlock: Boolean
    ) : LiveSyncResult
}

/**
 * Live Web Sync service tailored for https://dpbosssattamatka.com/
 *
 * Uses a strict 6-second timeout and attempts:
 * 1. Direct fetch to https://dpbosssattamatka.com/
 * 2. Sequential fallback via api.allorigins.win/raw?url=
 * 3. Sequential fallback via corsproxy.io/?
 *
 * Never fabricates or returns fake results if Cloudflare or network blocks access.
 */
class DpBossSyncService {

    companion object {
        const val TARGET_URL = "https://dpbosssattamatka.com/"
        private const val TIMEOUT_SECONDS = 6L
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun syncLatestResults(): LiveSyncResult = withContext(Dispatchers.IO) {
        val encodedUrl = URLEncoder.encode(TARGET_URL, "UTF-8")
        val endpoints = listOf(
            "Direct DPBoss" to TARGET_URL,
            "AllOrigins Proxy" to "https://api.allorigins.win/raw?url=$encodedUrl",
            "CorsProxy" to "https://corsproxy.io/?$encodedUrl"
        )

        var sawCloudflare = false
        var lastError = "Unable to connect to dpbosssattamatka.com"

        for ((label, url) in endpoints) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .build()

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    val body = response.body?.string().orEmpty()

                    val isCfChallenge = code == 403 || code == 503 ||
                        body.contains("cf-browser-verification", ignoreCase = true) ||
                        body.contains("Just a moment...", ignoreCase = true) ||
                        body.contains("Attention Required! | Cloudflare", ignoreCase = true)

                    if (isCfChallenge) {
                        sawCloudflare = true
                        lastError = "Blocked by Cloudflare security challenge ($label, HTTP $code)"
                        return@use
                    }

                    if (response.isSuccessful && body.isNotBlank()) {
                        val parsed = parseHtmlToRecords(body)
                        if (parsed.isNotEmpty()) {
                            return@withContext LiveSyncResult.Success(
                                records = parsed,
                                viaSourceLabel = label
                            )
                        } else {
                            lastError = "Page fetched via $label, but no live result blocks matched."
                        }
                    } else {
                        lastError = "HTTP $code via $label"
                    }
                }
            } catch (e: Exception) {
                lastError = "${e.javaClass.simpleName}: ${e.message ?: "Timeout after ${TIMEOUT_SECONDS}s"}"
            }
        }

        if (sawCloudflare) {
            LiveSyncResult.BlockedOrFailed(
                reason = "DPBoss is protected by Cloudflare right now. Copy results from your browser and use Quick Paste Importer.",
                isCloudflareBlock = true
            )
        } else {
            LiveSyncResult.BlockedOrFailed(
                reason = "Live sync timed out or unavailable ($lastError). Use Smart Copy-Paste Importer.",
                isCloudflareBlock = false
            )
        }
    }

    /**
     * Extracts real market names and XXX-XX-XXX or XXX-X results from DPBoss HTML markup.
     * Strips scripts/styles and validates every Pana strictly via MatkaMathEngine.
     */
    internal fun parseHtmlToRecords(html: String): List<ResultRecordEntity> {
        val stripped = html
            .replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<(?:br|p|div|h[1-6]|li|tr|td|span)[^>]*>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&#8211;", "-")
            .replace("&#8212;", "-")

        val lines = stripped.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val todayIso = DpBossTextParser.todayDateIso()
        val todayDow = DpBossTextParser.isoDateToDayOfWeek(todayIso)

        val fullRegex = Regex("""(?<!\d)(\d{3})\s*[-–—]\s*(\d{2})\s*[-–—]\s*(\d{3})(?!\d)""")
        val openOnlyRegex = Regex("""(?<!\d)(\d{3})\s*[-–—]\s*(\d)(?:\*|\s*[-–—]\s*\*+)?(?!\d)""")

        val resultsByMarket = linkedMapOf<String, ResultRecordEntity>()
        var lastHeading: String? = null

        for (line in lines) {
            val fullMatch = fullRegex.find(line)
            if (fullMatch != null && lastHeading != null) {
                val oP = fullMatch.groupValues[1]
                val jodi = fullMatch.groupValues[2]
                val cP = fullMatch.groupValues[3]
                if (MatkaMathEngine.validateResultConsistency(oP, jodi, cP) == null) {
                    val oAnk = MatkaMathEngine.calculateAnk(oP)
                    val cAnk = MatkaMathEngine.calculateAnk(cP)
                    resultsByMarket[lastHeading] = ResultRecordEntity(
                        marketName = lastHeading,
                        dateIso = todayIso,
                        dayOfWeek = todayDow,
                        openPana = oP,
                        openAnk = oAnk,
                        closePana = cP,
                        closeAnk = cAnk,
                        jodi = "$oAnk$cAnk",
                        source = RecordSource.WEB_SYNC.name,
                        isVerified = true
                    )
                }
                lastHeading = null
                continue
            }

            val openMatch = openOnlyRegex.find(line)
            if (openMatch != null && lastHeading != null) {
                val oP = openMatch.groupValues[1]
                val oAnkStr = openMatch.groupValues[2]
                if (MatkaMathEngine.validateResultConsistency(oP, oAnkStr, null) == null) {
                    val oAnk = MatkaMathEngine.calculateAnk(oP)
                    resultsByMarket.putIfAbsent(
                        lastHeading,
                        ResultRecordEntity(
                            marketName = lastHeading,
                            dateIso = todayIso,
                            dayOfWeek = todayDow,
                            openPana = oP,
                            openAnk = oAnk,
                            closePana = null,
                            closeAnk = null,
                            jodi = null,
                            source = RecordSource.WEB_SYNC.name,
                            isVerified = false
                        )
                    )
                }
                lastHeading = null
                continue
            }

            if (isLikelyMarketHeading(line)) {
                lastHeading = line.uppercase()
            }
        }

        return resultsByMarket.values.toList()
    }

    private fun isLikelyMarketHeading(line: String): Boolean {
        if (line.length !in 3..34) return false
        if (!line.all { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '&' }) return false
        if (!line.any { it.isLetter() }) return false
        val upper = line.uppercase()
        val blacklist = listOf(
            "DPBOSS", "LIVE RESULT", "LOADING", "REFRESH", "JODI", "PANEL",
            "CHART", "PRIVACY", "CONTACT", "DISCLAIMER", "SATTA MATKA", "WORLD"
        )
        if (blacklist.any { upper == it || upper.startsWith("$it ") }) return false
        return true
    }
}
