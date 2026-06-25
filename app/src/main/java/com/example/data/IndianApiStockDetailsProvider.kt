package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class IndianStockDetails(
    val symbol: String,
    val companyName: String,
    val industry: String?,
    val currentPrice: Double?,
    val percentChange: Double?,
    val yearHigh: Double?,
    val yearLow: Double?,
    val marketCap: Double?,
    val peRatio: Double?,
    val dividendYield: Double?,
    val roce: Double?,
    val roe: Double?,
    val bookValue: Double?,
    val faceValue: Double?,
    val recentNews: List<IndianStockNewsItem>,
    val source: String = "Live IndianAPI Stock Details API"
)

data class IndianStockNewsItem(
    val id: String,
    val symbol: String,
    val title: String,
    val source: String,
    val summary: String,
    val time: String,
    val url: String? = null,
    val sentiment: String = "Neutral"
)

class IndianApiStockDetailsProvider(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String
) {
    suspend fun getStockDetails(query: String): IndianStockDetails? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_STOCK_INDIAN_API_KEY") return@withContext null

        val cleanQuery = query.uppercase().trim()
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment("stock")
            .addQueryParameter("name", cleanQuery)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("X-Api-Key", apiKey)
            .header("User-Agent", "FamilyStockDashboard/1.0")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "IndianAPI stock details HTTP fail for $cleanQuery: ${response.code}")
                    return@use null
                }

                val json = JSONObject(response.body?.string() ?: return@use null)
                val symbol = json.optString("tickerId", cleanQuery).ifBlank { cleanQuery }.uppercase()
                val companyName = json.optString("companyName", symbol).ifBlank { symbol }
                val price = json.optJSONObject("currentPrice")?.let { currentPrice ->
                    currentPrice.optNullableDouble("NSE") ?: currentPrice.optNullableDouble("BSE")
                }

                IndianStockDetails(
                    symbol = symbol,
                    companyName = companyName,
                    industry = json.optString("industry").takeIf { it.isNotBlank() },
                    currentPrice = price,
                    percentChange = json.optNullableDouble("percentChange"),
                    yearHigh = json.optNullableDouble("yearHigh"),
                    yearLow = json.optNullableDouble("yearLow"),
                    marketCap = json.findFirstDouble("marketCap", "Market Cap", "Mkt Cap"),
                    peRatio = json.findFirstDouble("peRatio", "Stock P/E", "PE Ratio", "P/E"),
                    dividendYield = json.findFirstDouble("dividendYield", "Dividend Yield", "Div Yield"),
                    roce = json.findFirstDouble("ROCE", "Return on Capital Employed"),
                    roe = json.findFirstDouble("ROE", "Return on Equity"),
                    bookValue = json.findFirstDouble("Book Value", "bookValue"),
                    faceValue = json.findFirstDouble("Face Value", "faceValue"),
                    recentNews = json.optJSONArray("recentNews").toNewsItems(symbol)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IndianAPI stock details for $cleanQuery: ${e.message}")
            null
        }
    }

    private fun JSONArray?.toNewsItems(symbol: String): List<IndianStockNewsItem> {
        if (this == null) return emptyList()

        return buildList {
            for (i in 0 until length()) {
                val item = optJSONObject(i) ?: continue
                val title = item.firstNonBlank("title", "headline", "heading", "name") ?: continue
                val summary = item.firstNonBlank("summary", "description", "content", "desc") ?: ""
                add(
                    IndianStockNewsItem(
                        id = item.firstNonBlank("id", "url", "link") ?: "$symbol-$i",
                        symbol = symbol,
                        title = title,
                        source = item.firstNonBlank("source", "publisher", "provider", "site") ?: "IndianAPI",
                        summary = summary,
                        time = item.firstNonBlank("publishedAt", "pubDate", "date", "time") ?: "Recent",
                        url = item.firstNonBlank("url", "link"),
                        sentiment = item.firstNonBlank("sentiment") ?: "Neutral"
                    )
                )
            }
        }
    }

    private fun JSONObject.findFirstDouble(vararg possibleNames: String): Double? {
        for (name in possibleNames) {
            optNullableDouble(name)?.let { return it }
        }

        val targets = possibleNames.map { it.normalizedKey() }.toSet()
        val keys = keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = opt(key)
            if (key.normalizedKey() in targets) {
                value.toNullableDouble()?.let { return it }
            }
            when (value) {
                is JSONObject -> value.findFirstDouble(*possibleNames)?.let { return it }
                is JSONArray -> {
                    for (i in 0 until value.length()) {
                        val child = value.opt(i)
                        if (child is JSONObject) child.findFirstDouble(*possibleNames)?.let { return it }
                    }
                }
            }
        }
        return null
    }

    private fun JSONObject.firstNonBlank(vararg names: String): String? {
        for (name in names) {
            val value = optString(name).takeIf { it.isNotBlank() }
            if (value != null) return value
        }
        return null
    }

    private fun JSONObject.optNullableDouble(name: String): Double? {
        if (!has(name) || isNull(name)) return null
        return opt(name).toNullableDouble()
    }

    private fun Any?.toNullableDouble(): Double? = when (this) {
        is Number -> toDouble()
        is String -> replace(",", "").replace("%", "").trim().toDoubleOrNull()
        else -> null
    }

    private fun String.normalizedKey(): String = lowercase().filter { it.isLetterOrDigit() }

    private companion object {
        const val TAG = "IndianApiStockDetails"
        const val BASE_URL = "https://stock.indianapi.in"
    }
}
