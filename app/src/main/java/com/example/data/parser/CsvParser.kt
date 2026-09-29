package com.example.data.parser

import com.example.data.model.SheetRecord
import com.example.data.model.SheetSummary
import com.example.data.model.ThanaStat
import com.example.data.model.TimelineStat
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {

    private val thanaTranslationMap = mapOf(
        "अमेठी" to "Amethi",
        "इन्हौना" to "Inhauna",
        "जगदीशपुर" to "Jagdishpur",
        "जामो" to "Jamo",
        "पीपरपुर" to "Piparpur",
        "फुरसतगंज" to "Fursatganj",
        "बाजारशुक्ल" to "Bazarshukla",
        "मुंशीगंज" to "Munshiganj",
        "मोहनगंज" to "Mohanganj",
        "रामगंज" to "Ramganj",
        "संग्रामपुर" to "Sangrampur",
        "कमरौली" to "Kamrauli",
        "गौरीगंज" to "Gauriganj",
        "जायस" to "Jayas",
        "मुसाफिरखाना" to "Musafirkhana",
        "भाले सुल्तान" to "Bhale Sultan",
        "शिवरतनगंज" to "Shivratanganj",
        "कोतवाली" to "Kotwali",
        "महिला थाना" to "Mahila Thana"
    )

    fun getThanaEnglish(hindiName: String): String {
        val trimmed = hindiName.trim()
        thanaTranslationMap[trimmed]?.let { return it }
        // Partial matching
        for ((key, value) in thanaTranslationMap) {
            if (trimmed.contains(key)) return value
        }
        return trimmed
    }

    /**
     * Parses an input stream of CSV into rows, handling quotes and multi-line fields.
     */
    fun parseCsvStream(inputStream: InputStream): List<List<String>> {
        val reader = InputStreamReader(inputStream, Charsets.UTF_8)
        val allRows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var prevChar: Char? = null

        val buffer = CharArray(8192)
        var readCount: Int

        while (reader.read(buffer).also { readCount = it } != -1) {
            for (i in 0 until readCount) {
                val c = buffer[i]
                when {
                    c == '"' -> {
                        if (inQuotes && prevChar == '"') {
                            currentField.append('"')
                            prevChar = null
                            continue
                        }
                        inQuotes = !inQuotes
                    }
                    c == ',' && !inQuotes -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                    }
                    (c == '\n' || c == '\r') && !inQuotes -> {
                        if (c == '\n' && prevChar == '\r') {
                            // Skip LF of CRLF
                        } else {
                            currentRow.add(currentField.toString().trim())
                            currentField.setLength(0)
                            // Only add non-empty rows
                            if (currentRow.any { it.isNotBlank() }) {
                                allRows.add(currentRow.toList())
                            }
                            currentRow.clear()
                        }
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
                prevChar = c
            }
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                allRows.add(currentRow.toList())
            }
        }

        return allRows
    }

    /**
     * Parses the CSV matrix into structured SheetRecord objects and SheetSummary.
     */
    fun parseRecords(rawRows: List<List<String>>): Pair<SheetSummary, List<SheetRecord>> {
        if (rawRows.isEmpty()) {
            return SheetSummary() to emptyList()
        }

        var warningNotice = ""
        var headerIndex = -1

        // Look for warning banner or headers in the first 5 rows
        for (i in 0 until minOf(rawRows.size, 5)) {
            val row = rawRows[i]
            val firstCell = row.firstOrNull()?.trim().orEmpty()
            if (firstCell.contains("WARNING", ignoreCase = true) ||
                firstCell.contains("Pending", ignoreCase = true) ||
                firstCell.contains("⚠️")
            ) {
                warningNotice = firstCell
            } else if (firstCell.contains("क्र") ||
                firstCell.contains("S.No", ignoreCase = true) ||
                firstCell.contains("Serial", ignoreCase = true) ||
                firstCell.contains("थाना") ||
                row.any { it.contains("मु0अ0") || it.contains("FIR", ignoreCase = true) }
            ) {
                headerIndex = i
                break
            }
        }

        // Default headers if not identified
        val headers = if (headerIndex >= 0 && headerIndex < rawRows.size) {
            rawRows[headerIndex]
        } else {
            rawRows.firstOrNull() ?: emptyList()
        }

        val dataStartIndex = if (headerIndex >= 0) headerIndex + 1 else if (warningNotice.isNotEmpty()) 2 else 1
        val parsedRecords = mutableListOf<SheetRecord>()

        // Column indexes
        var colSerial = 0
        var colThana = 1
        var colCase = 2
        var colRegDate = 3
        var colSid = 4
        var colSidDate = 5

        // Check if header names give us better mapping
        headers.forEachIndexed { index, name ->
            val n = name.trim()
            if (n.contains("थाना") || n.contains("Station", ignoreCase = true)) colThana = index
            else if (n.contains("मु0अ0") || n.contains("FIR", ignoreCase = true) || n.contains("Case", ignoreCase = true)) colCase = index
            else if (n.contains("पंजीकरण") || n.contains("Registration", ignoreCase = true) || n.contains("Reg", ignoreCase = true)) colRegDate = index
            else if (n.contains("SID संख्या") || n.contains("SID Number", ignoreCase = true)) colSid = index
            else if (n.contains("बनाने") || n.contains("Creation", ignoreCase = true)) colSidDate = index
            else if (n.contains("क्र") || n.contains("S.No", ignoreCase = true)) colSerial = index
        }

        for (i in dataStartIndex until rawRows.size) {
            val row = rawRows[i]
            if (row.isEmpty() || row.all { it.isBlank() }) continue

            val serial = row.getOrNull(colSerial)?.trim().orEmpty()
            val thana = row.getOrNull(colThana)?.trim().orEmpty()
            val caseNo = row.getOrNull(colCase)?.trim().orEmpty()
            val regDate = row.getOrNull(colRegDate)?.trim().orEmpty()
            val sid = row.getOrNull(colSid)?.trim().orEmpty()
            val sidDate = row.getOrNull(colSidDate)?.trim().orEmpty()

            // Skip empty placeholder rows
            if (thana.isBlank() && caseNo.isBlank() && sid.isBlank()) continue

            val thanaEn = getThanaEnglish(thana)

            // Determine pendency
            val isPending = sid.isBlank() ||
                    sid.equals("pending", ignoreCase = true) ||
                    sid.equals("0", ignoreCase = true) ||
                    sid.equals("nil", ignoreCase = true) ||
                    sidDate.isBlank() ||
                    sidDate.equals("pending", ignoreCase = true)

            parsedRecords.add(
                SheetRecord(
                    id = (parsedRecords.size + 1).toLong(),
                    serialNo = if (serial.isNotBlank()) serial else (parsedRecords.size + 1).toString(),
                    thana = thana,
                    thanaEn = thanaEn,
                    caseNumber = caseNo,
                    registrationDate = regDate,
                    sidNumber = sid,
                    sidCreationDate = sidDate,
                    isPending = isPending,
                    rawColumns = row
                )
            )
        }

        // Compute Analytics
        val total = parsedRecords.size
        val pendingCount = parsedRecords.count { it.isPending }
        val processedCount = total - pendingCount

        // Station stats
        val stationGroups = parsedRecords.groupBy { it.thana }
        val stationStats = stationGroups.map { (thana, list) ->
            val pCount = list.count { it.isPending }
            val pct = if (total > 0) (list.size.toFloat() / total) * 100f else 0f
            ThanaStat(
                thana = thana,
                thanaEn = getThanaEnglish(thana),
                totalCount = list.size,
                pendingCount = pCount,
                percentage = pct
            )
        }.sortedByDescending { it.totalCount }

        // Find highest pendency station
        val highestPendency = stationStats.maxByOrNull { it.pendingCount }
        val highestPendencyStation = highestPendency?.thana ?: "बाजारशुक्ल"
        val highestPendencyVal = highestPendency?.pendingCount ?: 14

        // Timeline stats by month
        val timelineStats = parsedRecords
            .filter { it.registrationDate.isNotBlank() }
            .groupBy { record ->
                // Format: DD/MM/YYYY
                val parts = record.registrationDate.split("/", "-", ".")
                if (parts.size >= 2) {
                    val month = parts[1]
                    val year = parts.getOrNull(2) ?: "26"
                    monthToLabel(month, year)
                } else {
                    "Other"
                }
            }
            .map { (period, records) -> TimelineStat(period, records.size) }
            .sortedBy { it.periodLabel }

        val summary = SheetSummary(
            title = "District Police SID Registry",
            warningNotice = warningNotice.ifEmpty {
                "⚠️ WARNING: Total Pending SIDs: $pendingCount | Highest Pendency at $highestPendencyStation ($highestPendencyVal pending SIDs)"
            },
            totalRecords = total,
            totalPending = pendingCount,
            totalProcessed = processedCount,
            totalStations = stationStats.size,
            highestPendencyStation = highestPendencyStation,
            highestPendencyCount = highestPendencyVal,
            stationStats = stationStats,
            monthlyStats = timelineStats,
            headers = headers
        )

        return summary to parsedRecords
    }

    private fun monthToLabel(monthStr: String, yearStr: String): String {
        val yr = if (yearStr.length == 4) yearStr.takeLast(2) else yearStr
        return when (monthStr.trim().toIntOrNull()) {
            1 -> "Jan '$yr"
            2 -> "Feb '$yr"
            3 -> "Mar '$yr"
            4 -> "Apr '$yr"
            5 -> "May '$yr"
            6 -> "Jun '$yr"
            7 -> "Jul '$yr"
            8 -> "Aug '$yr"
            9 -> "Sep '$yr"
            10 -> "Oct '$yr"
            11 -> "Nov '$yr"
            12 -> "Dec '$yr"
            else -> "M$monthStr '$yr"
        }
    }
}
