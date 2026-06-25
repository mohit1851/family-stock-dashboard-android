package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class FiftyTwoWeekRange(
    val symbol: String,
    val companyName: String,
    val currentPrice: Double?,
    val yearHigh: Double?,
    val yearLow: Double?,
    val exchange: String
)

class IndianApiFiftyTwoWeekProvider(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String
) {
    suspend fun getFiftyTwoWeekRanges(): Map<String, FiftyTwoWeekRange> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_STOCK_INDIAN_API_KEY") return@withContext emptyMap()

        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment("fetch_52_week_high_low_data")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("X-Api-Key", apiKey)
            .header("User-Agent", "FamilyStockDashboard/1.0")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "IndianAPI 52-week HTTP fail: ${response.code}")
                    return@use emptyMap()
                }

                JSONObject(response.body?.string() ?: return@use emptyMap()).toRanges()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IndianAPI 52-week ranges: ${e.message}")
            emptyMap()
        }
    }

    private fun JSONObject.toRanges(): Map<String, FiftyTwoWeekRange> {
        val ranges = linkedMapOf<String, FiftyTwoWeekRange>()
        optJSONObject("NSE_52WeekHighLow")?.mergeExchangeRanges("NSE", ranges)
        optJSONObject("BSE_52WeekHighLow")?.mergeExchangeRanges("BSE", ranges)
        return ranges
    }

    private fun JSONObject.mergeExchangeRanges(exchange: String, ranges: MutableMap<String, FiftyTwoWeekRange>) {
        optJSONArray("high52Week").mergeRangeItems(exchange, true, ranges)
        optJSONArray("low52Week").mergeRangeItems(exchange, false, ranges)
    }

    private fun JSONArray?.mergeRangeItems(
        exchange: String,
        isHighList: Boolean,
        ranges: MutableMap<String, FiftyTwoWeekRange>
    ) {
        if (this == null) return

        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            val ticker = item.optString("ticker").takeIf { it.isNotBlank() } ?: continue
            val symbol = ticker.cleanSymbol()
            val current = item.opt("price").toNullableDouble()
            val high = if (isHighList) item.opt("52_week_high").toNullableDouble() else null
            val low = if (!isHighList) item.opt("52_week_low").toNullableDouble() else null
            val existing = ranges[symbol]
            ranges[symbol] = FiftyTwoWeekRange(
                symbol = symbol,
                companyName = item.optString("company").ifBlank { existing?.companyName ?: symbol },
                currentPrice = current ?: existing?.currentPrice,
                yearHigh = high ?: existing?.yearHigh,
                yearLow = low ?: existing?.yearLow,
                exchange = if (existing?.exchange == "NSE") existing.exchange else exchange
            )
        }
    }

    private fun String.cleanSymbol(): String = uppercase()
        .removeSuffix(".NS")
        .removeSuffix(".BO")
        .trim()

    private fun Any?.toNullableDouble(): Double? = when (this) {
        is Number -> toDouble()
        is String -> replace(",", "").trim().toDoubleOrNull()
        else -> null
    }

    private companion object {
        const val TAG = "IndianApi52Week"
        const val BASE_URL = "https://stock.indianapi.in"
    }
}
