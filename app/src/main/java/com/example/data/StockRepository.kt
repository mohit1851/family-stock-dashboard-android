package com.example.data

import android.util.Log
import androidx.room.withTransaction
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class StockRepository(private val db: FamilyDatabase) {
    val allStocks: Flow<List<StockAsset>> = db.stockDao().getAllStocks()
    val allMessages: Flow<List<ChatMessage>> = db.chatDao().getAllMessages()
    val allAlerts: Flow<List<PriceAlert>> = db.alertDao().getAllAlerts()
    val allWatchlistItems: Flow<List<WatchlistItem>> = db.watchlistDao().getAllWatchlistItems()
    val allUsers: Flow<List<User>> = db.userDao().getAllUsers()

    // Map storing real-time validation sanity check sources: Symbol -> Source message
    private val _verificationSources = MutableStateFlow<Map<String, String>>(emptyMap())
    val verificationSources: StateFlow<Map<String, String>> = _verificationSources.asStateFlow()

    fun setVerificationSource(symbol: String, source: String) {
        _verificationSources.update { current ->
            current + (symbol.uppercase().trim() to source)
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val liveMarketDataProvider = CompositeMarketDataProvider(
        listOf(
            IndianApiMarketDataProvider(okHttpClient, BuildConfig.STOCK_INDIAN_API_KEY),
            YahooFinanceMarketDataProvider(okHttpClient)
        )
    )
    private val marketDataProvider = CompositeMarketDataProvider(
        listOf(liveMarketDataProvider, SimulatedMarketDataProvider())
    )
    private val stockDetailsProvider = IndianApiStockDetailsProvider(
        okHttpClient = okHttpClient,
        apiKey = BuildConfig.STOCK_INDIAN_API_KEY
    )
    private val marketMoversProvider = IndianApiMarketMoversProvider(
        okHttpClient = okHttpClient,
        apiKey = BuildConfig.STOCK_INDIAN_API_KEY
    )
    private val historicalDataProvider = IndianApiHistoricalDataProvider(
        okHttpClient = okHttpClient,
        apiKey = BuildConfig.STOCK_INDIAN_API_KEY
    )

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("StockRepository"))

    init {
        // Seed initial data if the database is completely empty
        repositoryScope.launch {
            try {
                seedInitialDataIfNeeded()
                // Start a simulation of real-time market price updates for Indian markets (NSE)
                startPriceSimulation()
            } catch (e: Exception) {
                Log.e("StockRepository", "Initialization error: ${e.message}")
            }
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        val existingStocks = allStocks.first()
        if (existingStocks.isEmpty()) {
            Log.d("StockRepository", "Seeding initial assets, chat messages, and alerts...")
            
            // 1. Seed Stocks matching the Sharma Family Portfolio (~ ₹42,85,240 value)
            val defaultStocks = listOf(
                StockAsset(
                    symbol = "RELIANCE",
                    name = "Reliance Industries Ltd",
                    shares = 800.0,
                    avgPrice = 2400.0,
                    currentPrice = 2450.00,
                    dailyChangePercentage = 1.25
                ),
                StockAsset(
                    symbol = "TCS",
                    name = "Tata Consultancy Services Ltd",
                    shares = 300.0,
                    avgPrice = 3800.0,
                    currentPrice = 3850.00,
                    dailyChangePercentage = 0.8
                ),
                StockAsset(
                    symbol = "HDFCBANK",
                    name = "HDFC Bank Ltd",
                    shares = 500.0,
                    avgPrice = 1600.0,
                    currentPrice = 1620.00,
                    dailyChangePercentage = -0.45
                ),
                StockAsset(
                    symbol = "TATAMOTORS",
                    name = "Tata Motors Ltd",
                    shares = 387.0,
                    avgPrice = 900.0,
                    currentPrice = 930.50,
                    dailyChangePercentage = 2.4
                )
            )
            for (stock in defaultStocks) {
                db.stockDao().insertStock(stock)
            }

            // 2. Seed Family Chat history
            val timeBasis = System.currentTimeMillis()
            val defaultMessages = listOf(
                ChatMessage(
                    sender = "Dad",
                    message = "TATA Motors looks strong at this dip. Shall we increase our position by 10%?",
                    timestamp = timeBasis - 15 * 60 * 1000 // 15 mins ago
                ),
                ChatMessage(
                    sender = "Me",
                    message = "Agreed. AI analysis also suggests a 'Strong Buy' for current levels. Placed the alert.",
                    timestamp = timeBasis - 12 * 60 * 1000 // 12 mins ago
                )
            )
            for (msg in defaultMessages) {
                db.chatDao().insertMessage(msg)
            }

            // 3. Seed Price Alerts
            val defaultAlerts = listOf(
                PriceAlert(
                    symbol = "RELIANCE",
                    targetPrice = 2500.0,
                    triggerType = "ABOVE",
                    isActive = true
                ),
                PriceAlert(
                    symbol = "TATAMOTORS",
                    targetPrice = 900.0,
                    triggerType = "BELOW",
                    isActive = true
                )
            )
            for (alert in defaultAlerts) {
                db.alertDao().insertAlert(alert)
            }
        }
    }

    suspend fun fetchLiveStockData(symbol: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        liveMarketDataProvider.getQuote(symbol)?.let { it.price to it.changePercentage }
    }

    suspend fun fetchStockDetails(symbol: String): IndianStockDetails? = withContext(Dispatchers.IO) {
        stockDetailsProvider.getStockDetails(symbol)
    }

    suspend fun fetchStockNews(symbols: List<String>): List<IndianStockNewsItem> = withContext(Dispatchers.IO) {
        symbols.distinct()
            .take(8)
            .flatMap { symbol -> stockDetailsProvider.getStockDetails(symbol)?.recentNews.orEmpty() }
            .distinctBy { it.id }
            .take(20)
    }

    suspend fun fetchMarketMovers(): List<IndianMarketMover> = withContext(Dispatchers.IO) {
        marketMoversProvider.getMarketMovers()
    }

    suspend fun fetchHistoricalPrices(symbol: String): List<HistoricalPricePoint> = withContext(Dispatchers.IO) {
        historicalDataProvider.getHistoricalPrices(symbol)
    }

    private suspend fun fetchBestEffortMarketQuote(
        symbol: String,
        fallbackPrice: Double,
        fallbackChangePercentage: Double
    ): MarketQuote? = withContext(Dispatchers.IO) {
        marketDataProvider.getQuote(symbol, fallbackPrice, fallbackChangePercentage)
    }

    // A coroutine loop that pulls prices in real time from Yahoo Finance,
    // with a highly robust simulated tick fallback when offline.
    private suspend fun startPriceSimulation() {
        while (currentCoroutineContext().isActive) {
            delay(12000) // Ticks every 12 seconds
            withContext(Dispatchers.IO) {
                try {
                    val stocks = db.stockDao().getAllStocks().first()
                    val alerts = db.alertDao().getAllAlerts().first()

                    val currentSources = _verificationSources.value.toMutableMap()

                    for (stock in stocks) {
                        var newPrice = stock.currentPrice
                        var newDailyChange = stock.dailyChangePercentage
                        var isReal = false

                        val quote = fetchBestEffortMarketQuote(
                            symbol = stock.symbol,
                            fallbackPrice = stock.currentPrice,
                            fallbackChangePercentage = stock.dailyChangePercentage
                        )
                        if (quote != null) {
                            newPrice = quote.price
                            newDailyChange = quote.changePercentage
                            isReal = quote.isLive
                            currentSources[stock.symbol.uppercase()] = quote.source
                        }

                        val updatedStock = stock.copy(
                            currentPrice = newPrice,
                            dailyChangePercentage = newDailyChange
                        )
                        db.stockDao().updateStock(updatedStock)

                        // Check price alerts!
                        for (alert in alerts) {
                            if (alert.isActive && !alert.isTriggered && alert.symbol.uppercase() == stock.symbol.uppercase()) {
                                val isTriggeredNow = when (alert.triggerType) {
                                    "ABOVE" -> newPrice >= alert.targetPrice
                                    "BELOW" -> newPrice <= alert.targetPrice
                                    else -> false
                                }
                                if (isTriggeredNow) {
                                    // Set alert to completed / triggered
                                    db.alertDao().updateAlert(alert.copy(isTriggered = true, isActive = false))
                                    
                                    // Automatically inject a system message in the chat thread!
                                    db.chatDao().insertMessage(
                                        ChatMessage(
                                            sender = "System Alert",
                                            message = "[ALERT HIT] ${stock.symbol} ${if (isReal) "Real-time" else "Simulated"} Price has crossed your threshold of ₹${alert.targetPrice}! Sync at ₹$newPrice.",
                                            timestamp = System.currentTimeMillis(),
                                            groupId = stock.groupId
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Update watchlist items!
                    val watchlistItems = db.watchlistDao().getAllWatchlistItems().first()
                    for (item in watchlistItems) {
                        var newPrice = item.currentPrice
                        var newDailyChange = item.dailyChangePercentage

                        val quote = fetchBestEffortMarketQuote(
                            symbol = item.symbol,
                            fallbackPrice = item.currentPrice,
                            fallbackChangePercentage = item.dailyChangePercentage
                        )
                        if (quote != null) {
                            newPrice = quote.price
                            newDailyChange = quote.changePercentage
                            currentSources[item.symbol.uppercase()] = quote.source
                        }

                        val updatedItem = item.copy(
                            currentPrice = newPrice,
                            dailyChangePercentage = newDailyChange
                        )

                        // Check if hit target price!
                        if (item.isActive && !item.isTriggered) {
                            val wasAbove = item.targetPrice >= item.currentPrice
                            val isHit = if (wasAbove) {
                                newPrice >= item.targetPrice
                            } else {
                                newPrice <= item.targetPrice
                            }

                            if (isHit) {
                                db.watchlistDao().updateWatchlistItem(
                                    updatedItem.copy(isTriggered = true, isActive = false)
                                )
                                // Alert the chat!
                                val targetFormatted = String.format("%,.2f", item.targetPrice)
                                val currentFormatted = String.format("%,.2f", newPrice)
                                db.chatDao().insertMessage(
                                    ChatMessage(
                                        sender = "System Watchlist Alert",
                                        message = "[WATCHLIST HIT] ${item.symbol} has hit your target of ₹${targetFormatted}! Current price is ₹${currentFormatted}.",
                                        recommendedStockSymbol = item.symbol,
                                        targetNotificationPrice = item.targetPrice,
                                        groupId = item.groupId
                                    )
                                )
                            } else {
                                db.watchlistDao().updateWatchlistItem(updatedItem)
                            }
                        } else {
                            db.watchlistDao().updateWatchlistItem(updatedItem)
                        }
                    }
                    _verificationSources.update { latest -> latest + currentSources }
                } catch (e: Exception) {
                    Log.e("StockRepository", "Error in real-time sync / simulation: ${e.message}")
                }
            }
        }
    }

    fun close() {
        repositoryScope.cancel()
    }

    suspend fun scrapeScreenerRatios(symbol: String): Map<String, Double> = withContext(Dispatchers.IO) {
        val cleanSymbol = symbol.uppercase().trim()
        val url = "https://www.screener.in/company/$cleanSymbol/"
        val r = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
            .build()
        val results = mutableMapOf<String, Double>()
        try {
            okHttpClient.newCall(r).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    
                    // Helper to parse double of a specific metric tag
                    fun parseMetric(htmlContent: String, metricLabel: String): Double? {
                        val idx = htmlContent.indexOf(metricLabel, ignoreCase = true)
                        if (idx == -1) return null
                        // Look for upcoming class="number"
                        var nrTag = htmlContent.indexOf("<span class=\"number\">", idx, ignoreCase = true)
                        if (nrTag == -1 || nrTag - idx > 600) {
                            // Try value class first
                            val valTag = htmlContent.indexOf("class=\"value\"", idx, ignoreCase = true)
                            if (valTag != -1 && valTag - idx < 600) {
                                nrTag = htmlContent.indexOf("<span class=\"number\">", valTag, ignoreCase = true)
                            }
                        }
                        if (nrTag == -1 || nrTag - idx > 1000) return null
                        val tagClose = htmlContent.indexOf(">", nrTag) + 1
                        val tagOpen = htmlContent.indexOf("<", tagClose)
                        if (tagClose > 0 && tagOpen > tagClose) {
                            val rawText = htmlContent.substring(tagClose, tagOpen).replace(",", "").replace("%", "").trim()
                            return rawText.toDoubleOrNull()
                        }
                        return null
                    }

                    parseMetric(html, "Market Cap")?.let { results["marketCap"] = it }
                    parseMetric(html, "Stock P/E")?.let { results["peRatio"] = it }
                    parseMetric(html, "Dividend Yield")?.let { results["dividendYield"] = it }
                    parseMetric(html, "ROCE")?.let { results["roce"] = it }
                    parseMetric(html, "ROE")?.let { results["roe"] = it }
                    parseMetric(html, "Book Value")?.let { results["bookValue"] = it }
                    parseMetric(html, "Face Value")?.let { results["faceValue"] = it }
                    
                    // Extract 52W High and Low
                    val hiLoIndex = html.indexOf("High / Low", ignoreCase = true)
                    if (hiLoIndex != -1) {
                        val firstNrTag = html.indexOf("<span class=\"number\">", hiLoIndex, ignoreCase = true)
                        if (firstNrTag != -1 && firstNrTag - hiLoIndex < 400) {
                            val tagClose = html.indexOf(">", firstNrTag) + 1
                            val tagOpen = html.indexOf("<", tagClose)
                            val highVal = html.substring(tagClose, tagOpen).replace(",", "").trim().toDoubleOrNull()
                            if (highVal != null) {
                                results["high52w"] = highVal
                            }
                            val secondNrTag = html.indexOf("<span class=\"number\">", firstNrTag + 20, ignoreCase = true)
                            if (secondNrTag != -1 && secondNrTag - firstNrTag < 400) {
                                val tagClose2 = html.indexOf(">", secondNrTag) + 1
                                val tagOpen2 = html.indexOf("<", tagClose2)
                                val lowVal = html.substring(tagClose2, tagOpen2).replace(",", "").trim().toDoubleOrNull()
                                if (lowVal != null) {
                                    results["low52w"] = lowVal
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StockRepository", "Screener scrape failed for $cleanSymbol: ${e.message}")
        }
        results
    }

    // --- Exposed Mutable functions for app operations ---

    suspend fun addStock(symbol: String, name: String, shares: Double, avgPrice: Double, currentPrice: Double, groupId: String = "SHARMA_GROUP") {
        withContext(Dispatchers.IO) {
            val formattedSymbol = symbol.uppercase().trim()
            val asset = StockAsset(
                symbol = formattedSymbol,
                name = name.trim(),
                shares = shares,
                avgPrice = avgPrice,
                currentPrice = currentPrice,
                dailyChangePercentage = 0.0,
                groupId = groupId
            )
            db.stockDao().insertStock(asset)
        }
    }

    suspend fun removeStockById(id: Int) {
        withContext(Dispatchers.IO) {
            db.stockDao().deleteStock(id)
        }
    }

    suspend fun replaceStocksForGroup(groupId: String, stocks: List<StockAsset>) {
        withContext(Dispatchers.IO) {
            db.withTransaction {
                db.stockDao().deleteStocksForGroup(groupId)
                stocks.forEach { stock ->
                    db.stockDao().insertStock(stock.copy(groupId = groupId))
                }
            }
        }
    }

    suspend fun insertChatMessage(sender: String, message: String, recommendedSymbol: String? = null, groupId: String = "SHARMA_GROUP") {
        withContext(Dispatchers.IO) {
            val chat = ChatMessage(
                sender = sender,
                message = message,
                recommendedStockSymbol = recommendedSymbol?.uppercase()?.trim(),
                groupId = groupId
            )
            db.chatDao().insertMessage(chat)
        }
    }

    suspend fun addPriceAlert(symbol: String, targetPrice: Double, triggerType: String, groupId: String = "SHARMA_GROUP") {
        withContext(Dispatchers.IO) {
            val alert = PriceAlert(
                symbol = symbol.uppercase().trim(),
                targetPrice = targetPrice,
                triggerType = triggerType,
                isActive = true,
                isTriggered = false,
                groupId = groupId
            )
            db.alertDao().insertAlert(alert)
        }
    }

    suspend fun toggleAlertActiveState(alert: PriceAlert) {
        withContext(Dispatchers.IO) {
            db.alertDao().updateAlert(alert.copy(isActive = !alert.isActive, isTriggered = false))
        }
    }

    suspend fun deleteAlertById(id: Int) {
        withContext(Dispatchers.IO) {
            db.alertDao().deleteAlert(id)
        }
    }

    suspend fun addWatchlistItem(symbol: String, name: String, targetPrice: Double, currentPrice: Double, dailyChange: Double, groupId: String = "SHARMA_GROUP") {
        withContext(Dispatchers.IO) {
            val item = WatchlistItem(
                symbol = symbol.uppercase().trim(),
                name = name,
                targetPrice = targetPrice,
                currentPrice = currentPrice,
                dailyChangePercentage = dailyChange,
                isActive = true,
                isTriggered = false,
                groupId = groupId
            )
            db.watchlistDao().insertWatchlistItem(item)
        }
    }

    suspend fun deleteWatchlistItemById(id: Int) {
        withContext(Dispatchers.IO) {
            db.watchlistDao().deleteWatchlistItem(id)
        }
    }

    suspend fun toggleWatchlistActiveState(id: Int) {
        withContext(Dispatchers.IO) {
            val items = db.watchlistDao().getAllWatchlistItems().first()
            val match = items.find { it.id == id } ?: return@withContext
            db.watchlistDao().updateWatchlistItem(match.copy(isActive = !match.isActive, isTriggered = false))
        }
    }

    // --- Secure User & Group Management ---

    suspend fun getUserByUsername(username: String): User? {
        return withContext(Dispatchers.IO) {
            db.userDao().getUserByUsername(username.lowercase().trim())
        }
    }

    suspend fun insertUser(user: User) {
        withContext(Dispatchers.IO) {
            db.userDao().insertUser(user)
        }
    }

    suspend fun updateUser(user: User) {
        withContext(Dispatchers.IO) {
            db.userDao().updateUser(user)
        }
    }

    suspend fun getGroupById(groupId: String): FamilyGroup? {
        return withContext(Dispatchers.IO) {
            db.groupDao().getGroupById(groupId)
        }
    }

    suspend fun getGroupByInviteCode(inviteCode: String): FamilyGroup? {
        return withContext(Dispatchers.IO) {
            db.groupDao().getGroupByInviteCode(inviteCode.uppercase().trim())
        }
    }

    suspend fun insertGroup(group: FamilyGroup) {
        withContext(Dispatchers.IO) {
            db.groupDao().insertGroup(group)
        }
    }
}
