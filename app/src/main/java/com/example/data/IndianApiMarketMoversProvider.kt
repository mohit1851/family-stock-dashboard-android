package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

data class IndianMarketMover(
    val symbol: String,
    val companyName: String,
    val currentPrice: Double,
    val changePercentage: Double,
    val volume: String,
    val category: String
)

class IndianApiMarketMoversProvider(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String
) {
    suspend fun getMarketMovers(): List<IndianMarketMover> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_STOCK_INDIAN_API_KEY") return@withContext emptyList()

        val movers = mutableListOf<IndianMarketMover>()
        fetchJsonObject("trending")?.optJSONObject("trending_stocks")?.let { trending ->
            movers += trending.optJSONArray("top_gainers").toMovers("gainer")
            movers += trending.optJSONArray("top_losers").toMovers("loser")
        }
        movers += fetchJsonArray("NSE_most_active").toMovers("traded")
        movers += fetchJsonArray("price_shockers").toMovers("volatile")

        movers.distinctBy { it.category to it.symbol }
    }

    private fun fetchJsonObject(path: String): JSONObject? = try {
        execute(path)?.let { JSONObject(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing IndianAPI market movers object $path: ${e.message}")
        null
    }

    private fun fetchJsonArray(path: String): JSONArray? = try {
        execute(path)?.let { JSONArray(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing IndianAPI market movers array $path: ${e.message}")
        null
    }

    private fun execute(path: String): String? {
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment(path)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("X-Api-Key", apiKey)
            .header("User-Agent", "FamilyStockDashboard/1.0")
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "IndianAPI market movers HTTP fail for $path: ${response.code}")
                    return@use null
                }
                response.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IndianAPI market movers $path: ${e.message}")
            null
        }
    }

    private fun JSONArray?.toMovers(category: String): List<IndianMarketMover> {
        if (this == null) return emptyList()

        return buildList {
            for (i in 0 until length()) {
                val item = optJSONObject(i) ?: continue
                val symbol = item.firstNonBlank("ticker_id", "ticker")?.cleanSymbol() ?: continue
                val price = item.firstNumber("price") ?: continue
                val change = item.firstNumber("percent_change", "percentage_change") ?: 0.0
                add(
                    IndianMarketMover(
                        symbol = symbol,
                        companyName = item.firstNonBlank("company_name", "company") ?: symbol,
                        currentPrice = price,
                        changePercentage = if (category == "loser") -abs(change) else change,
                        volume = item.firstVolume("volume"),
                        category = category
                    )
                )
            }
        }
    }

    private fun JSONObject.firstNonBlank(vararg names: String): String? {
        for (name in names) {
            val value = optString(name).takeIf { it.isNotBlank() }
            if (value != null) return value
        }
        return null
    }

    private fun JSONObject.firstNumber(vararg names: String): Double? {
        for (name in names) {
            opt(name).toNullableDouble()?.let { return it }
        }
        return null
    }

    private fun JSONObject.firstVolume(name: String): String {
        val value = opt(name)
        val numericValue = value.toNullableDouble()
        if (numericValue != null) return numericValue.toCompactVolume()
        return value?.toString()?.takeIf { it.isNotBlank() } ?: "-"
    }

    private fun String.cleanSymbol(): String = uppercase()
        .removeSuffix(".NS")
        .removeSuffix(".BO")
        .trim()

    private fun Any?.toNullableDouble(): Double? = when (this) {
        is Number -> toDouble()
        is String -> replace(",", "").replace("%", "").trim().toDoubleOrNull()
        else -> null
    }

    private fun Double.toCompactVolume(): String = when {
        this >= 10_000_000 -> "${roundOneDecimal(this / 1_000_000)}M"
        this >= 100_000 -> "${roundOneDecimal(this / 100_000)}L"
        this >= 1_000 -> "${roundOneDecimal(this / 1_000)}K"
        else -> toLong().toString()
    }

    private fun roundOneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

    private companion object {
        const val TAG = "IndianApiMarketMovers"
        const val BASE_URL = "https://stock.indianapi.in"
    }
}
