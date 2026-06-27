package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.round
import kotlin.random.Random

data class MarketQuote(
    val symbol: String,
    val price: Double,
    val changePercentage: Double,
    val source: String,
    val isLive: Boolean
)

interface MarketDataProvider {
    suspend fun getQuote(
        symbol: String,
        fallbackPrice: Double? = null,
        fallbackChangePercentage: Double? = null
    ): MarketQuote?
}

class CompositeMarketDataProvider(
    private val providers: List<MarketDataProvider>
) : MarketDataProvider {
    override suspend fun getQuote(
        symbol: String,
        fallbackPrice: Double?,
        fallbackChangePercentage: Double?
    ): MarketQuote? {
        for (provider in providers) {
            val quote = provider.getQuote(symbol, fallbackPrice, fallbackChangePercentage)
            if (quote != null) return quote
        }
        return null
    }
}

class YahooFinanceMarketDataProvider(
    private val okHttpClient: OkHttpClient
) : MarketDataProvider {
    override suspend fun getQuote(
        symbol: String,
        fallbackPrice: Double?,
        fallbackChangePercentage: Double?
    ): MarketQuote? = withContext(Dispatchers.IO) {
        val cleanSymbol = symbol.uppercase().trim()
        val yahooSymbol = when {
            cleanSymbol == "NIFTY" -> "^NSEI"
            cleanSymbol.contains(".") -> cleanSymbol
            else -> "$cleanSymbol.NS"
        }

        val request = Request.Builder()
            .url("https://query1.finance.yahoo.com/v8/finance/chart/$yahooSymbol?interval=1d&range=1d")
            .header("User-Agent", USER_AGENT)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Yahoo Finance HTTP fail for $yahooSymbol: ${response.code}")
                    return@use null
                }

                val bodyString = response.body?.string() ?: return@use null
                val result = JSONObject(bodyString)
                    .optJSONObject("chart")
                    ?.optJSONArray("result")
                    ?: return@use null
                if (result.length() == 0) return@use null

                val meta = result.getJSONObject(0).optJSONObject("meta") ?: return@use null
                val price = meta.optDouble("regularMarketPrice", Double.NaN)
                val previousClose = meta.optDouble("chartPreviousClose", price)
                if (price.isNaN()) return@use null

                val change = if (!previousClose.isNaN() && previousClose != 0.0) {
                    ((price - previousClose) / previousClose) * 100.0
                } else {
                    0.0
                }
                MarketQuote(
                    symbol = cleanSymbol,
                    price = price.roundToPaise(),
                    changePercentage = change.roundToPercent(),
                    source = "Live Yahoo Finance API (NSE)",
                    isLive = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Yahoo quote for $yahooSymbol: ${e.message}")
            null
        }
    }

    private companion object {
        const val TAG = "YahooMarketData"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
    }
}

class IndianApiMarketDataProvider(
    private val okHttpClient: OkHttpClient,
    private val apiKey: String
) : MarketDataProvider {
    override suspend fun getQuote(
        symbol: String,
        fallbackPrice: Double?,
        fallbackChangePercentage: Double?
    ): MarketQuote? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_STOCK_INDIAN_API_KEY") return@withContext null

        val cleanSymbol = symbol.uppercase().trim()
        val url = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegment("stock")
            .addQueryParameter("name", cleanSymbol)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("X-Api-Key", apiKey)
            .header("User-Agent", "FamilyStockDashboard/1.0")
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "IndianAPI HTTP fail for $cleanSymbol: ${response.code}")
                    return@use null
                }

                val json = JSONObject(response.body?.string() ?: return@use null)
                val price = json.optJSONObject("currentPrice")?.let { currentPrice ->
                    currentPrice.optNullableDouble("NSE") ?: currentPrice.optNullableDouble("BSE")
                } ?: return@use null
                val change = json.optNullableDouble("percentChange") ?: 0.0
                val ticker = json.optString("tickerId", cleanSymbol).ifBlank { cleanSymbol }

                MarketQuote(
                    symbol = ticker.uppercase(),
                    price = price.roundToPaise(),
                    changePercentage = change.roundToPercent(),
                    source = "Live IndianAPI Stock Market API",
                    isLive = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching IndianAPI quote for $cleanSymbol: ${e.message}")
            null
        }
    }

    private companion object {
        const val TAG = "IndianApiMarketData"
        const val BASE_URL = "https://stock.indianapi.in"
    }
}

class SimulatedMarketDataProvider : MarketDataProvider {
    override suspend fun getQuote(
        symbol: String,
        fallbackPrice: Double?,
        fallbackChangePercentage: Double?
    ): MarketQuote? {
        val cleanSymbol = symbol.uppercase().trim()
        val basePrice = fallbackPrice ?: defaultPriceFor(cleanSymbol) ?: return null
        val baseChange = fallbackChangePercentage ?: 0.0
        val movement = Random.nextDouble(-0.2, 0.2)
        val price = (basePrice * (1.0 + movement / 100.0)).coerceAtLeast(1.0)
        return MarketQuote(
            symbol = cleanSymbol,
            price = price.roundToPaise(),
            changePercentage = (baseChange + movement).roundToPercent(),
            source = "Offline fallback price model",
            isLive = false
        )
    }

    private fun defaultPriceFor(symbol: String): Double? = when (symbol) {
        "NIFTY" -> 23410.50
        "RELIANCE" -> 2420.40
        "TATAMOTORS" -> 962.15
        "TCS" -> 3954.80
        "HDFCBANK" -> 1582.40
        "INFY" -> 1452.10
        "ICICIBANK" -> 1112.50
        "SBIN" -> 824.60
        "BHARTIARTL" -> 1321.40
        "WIPRO" -> 461.30
        "ITC" -> 432.80
        "LTIM" -> 4751.90
        "SUZLON" -> 48.30
        "IDEA" -> 12.50
        "IRFC" -> 172.50
        "YESBANK" -> 21.40
        "GMRINFRA" -> 84.15
        else -> null
    }
}

private fun Double.roundToPaise(): Double = round(this * 100.0) / 100.0
private fun Double.roundToPercent(): Double = round(this * 100.0) / 100.0

private fun JSONObject.optNullableDouble(name: String): Double? {
    if (!has(name) || isNull(name)) return null
    return when (val value = opt(name)) {
        is Number -> value.toDouble()
        is String -> value.replace(",", "").toDoubleOrNull()
        null -> null
        else -> null
    }
}
