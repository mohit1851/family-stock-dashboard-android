package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class HistoricalPricePoint(
    val date: String,
    val price: Double
)

class IndianApiHistoricalDataProvider(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String
) {
    suspend fun getHistoricalPrices(symbol: String): List<HistoricalPricePoint> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_STOCK_INDIAN_API_KEY") return@withContext emptyList()

        val cleanSymbol = symbol.uppercase().trim()
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment("historical_data")
            .addQueryParameter("stock_name", cleanSymbol)
            .addQueryParameter("period", "1yr")
            .addQueryParameter("filter", "price")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("X-Api-Key", apiKey)
            .header("User-Agent", "FamilyStockDashboard/1.0")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "IndianAPI historical data HTTP fail for $cleanSymbol: ${response.code}")
                    return@use emptyList()
                }

                val json = JSONObject(response.body?.string() ?: return@use emptyList())
                json.optJSONArray("datasets").priceDatasetValues().toHistoricalPricePoints()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IndianAPI historical prices for $cleanSymbol: ${e.message}")
            emptyList()
        }
    }

    private fun JSONArray?.priceDatasetValues(): JSONArray? {
        if (this == null) return null
        for (i in 0 until length()) {
            val dataset = optJSONObject(i) ?: continue
            val metric = dataset.optString("metric")
            val label = dataset.optString("label")
            if (metric.equals("Price", ignoreCase = true) || label.contains("price", ignoreCase = true)) {
                return dataset.optJSONArray("values")
            }
        }
        return optJSONObject(0)?.optJSONArray("values")
    }

    private fun JSONArray?.toHistoricalPricePoints(): List<HistoricalPricePoint> {
        if (this == null) return emptyList()

        return buildList {
            for (i in 0 until length()) {
                val value = optJSONArray(i) ?: continue
                val date = value.optString(0).takeIf { it.isNotBlank() } ?: continue
                val price = value.opt(1).toNullableDouble() ?: continue
                add(HistoricalPricePoint(date = date, price = price))
            }
        }
    }

    private fun Any?.toNullableDouble(): Double? = when (this) {
        is Number -> toDouble()
        is String -> replace(",", "").trim().toDoubleOrNull()
        else -> null
    }

    private companion object {
        const val TAG = "IndianApiHistoricalData"
        const val BASE_URL = "https://stock.indianapi.in"
    }
}
