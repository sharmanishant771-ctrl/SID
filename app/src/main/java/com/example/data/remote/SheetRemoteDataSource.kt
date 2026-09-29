package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SheetRemoteDataSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    companion object {
        private val SHEET_ID_PATTERN = Pattern.compile("/d/([a-zA-Z0-9-_]+)")

        /**
         * Normalizes any Google Sheet link or ID into a direct CSV export URL.
         */
        fun normalizeToCsvUrl(input: String): String {
            val trimmed = input.trim()

            // If it's already a direct CSV or gviz export URL
            if (trimmed.contains("export?format=csv") || trimmed.contains("gviz/tq?tqx=out:csv")) {
                return trimmed
            }

            // Extract Google Sheet ID
            val matcher = SHEET_ID_PATTERN.matcher(trimmed)
            if (matcher.find()) {
                val sheetId = matcher.group(1)
                // Check if gid is specified
                val gidMatcher = Pattern.compile("[#&?]gid=([0-9]+)").matcher(trimmed)
                val gid = if (gidMatcher.find()) gidMatcher.group(1) else "0"
                return "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&gid=$gid"
            }

            // If user passed just the sheet ID directly
            if (trimmed.matches(Regex("^[a-zA-Z0-9-_]{20,}$"))) {
                return "https://docs.google.com/spreadsheets/d/$trimmed/export?format=csv"
            }

            // Fallback to the trimmed URL as-is
            return trimmed
        }
    }

    suspend fun fetchCsvStream(sheetUrl: String): Result<InputStream> = withContext(Dispatchers.IO) {
        try {
            val finalUrl = normalizeToCsvUrl(sheetUrl)
            val request = Request.Builder()
                .url(finalUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch sheet (HTTP ${response.code}). Ensure sheet is shared as 'Anyone with the link can view'.")
                )
            }

            val body = response.body
                ?: return@withContext Result.failure(Exception("Empty response body received from Google Sheets."))

            // Note: Caller is responsible for closing the stream after parsing
            Result.success(body.byteStream())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
