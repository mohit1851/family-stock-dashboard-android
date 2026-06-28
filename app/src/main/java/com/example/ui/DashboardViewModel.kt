package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import android.net.Uri
import android.provider.OpenableColumns

data class SearchedStock(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val dailyChangePercentage: Double,
    val peRatio: Double = 22.4,
    val betaIndex: Double = 1.12,
    val high52w: Double,
    val low52w: Double,
    val marketCap: Double = 145000.0,
    val dividendYield: Double = 1.2,
    val roce: Double = 18.2,
    val roe: Double = 14.5,
    val bookValue: Double = 450.0,
    val faceValue: Double = 2.0,
    val isScreenerSourced: Boolean = false
)

data class TrendingStock(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val changePercentage: Double,
    val volume: String = "1.2M",
    val category: String // "gainer", "loser", "volatile", "traded", "penny"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FamilyDatabase.getDatabase(application)
    private val repository = StockRepository(db)
    private val authUseCase = AuthUseCase(repository)
    private val researchUseCase = ResearchUseCase(repository)
    private val watchlistChatUseCase = WatchlistChatUseCase(repository)

    // User session states
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentGroup = MutableStateFlow<FamilyGroup?>(null)
    val currentGroup: StateFlow<FamilyGroup?> = _currentGroup.asStateFlow()

    // Data streams filtered reactively by current family group
    val stockAssets: StateFlow<List<StockAsset>> = combine(
        repository.allStocks,
        _currentGroup
    ) { stocks, group ->
        val gId = group?.groupId ?: ""
        stocks.filter { it.groupId == gId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = combine(
        repository.allMessages,
        _currentGroup
    ) { messages, group ->
        val gId = group?.groupId ?: ""
        messages.filter { it.groupId == gId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val priceAlerts: StateFlow<List<PriceAlert>> = combine(
        repository.allAlerts,
        _currentGroup
    ) { alerts, group ->
        val gId = group?.groupId ?: ""
        alerts.filter { it.groupId == gId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlistItems: StateFlow<List<WatchlistItem>> = combine(
        repository.allWatchlistItems,
        _currentGroup
    ) { items, group ->
        val gId = group?.groupId ?: ""
        items.filter { it.groupId == gId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupMembers: StateFlow<List<User>> = combine(
        repository.allUsers,
        _currentGroup
    ) { users, group ->
        val gId = group?.groupId ?: ""
        users.filter { it.groupId == gId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _stockNews = MutableStateFlow<List<NewsArticle>>(emptyList())
    val stockNews: StateFlow<List<NewsArticle>> = _stockNews.asStateFlow()

    private val _isStockNewsLoading = MutableStateFlow(false)
    val isStockNewsLoading: StateFlow<Boolean> = _isStockNewsLoading.asStateFlow()

    private val _stockNewsError = MutableStateFlow<String?>(null)
    val stockNewsError: StateFlow<String?> = _stockNewsError.asStateFlow()

    private val _stockPriceHistory = MutableStateFlow<Map<String, List<HistoricalPricePoint>>>(emptyMap())
    val stockPriceHistory: StateFlow<Map<String, List<HistoricalPricePoint>>> = _stockPriceHistory.asStateFlow()

    private val _isStockPriceHistoryLoading = MutableStateFlow(false)
    val isStockPriceHistoryLoading: StateFlow<Boolean> = _isStockPriceHistoryLoading.asStateFlow()

    private val _stockPriceHistoryError = MutableStateFlow<String?>(null)
    val stockPriceHistoryError: StateFlow<String?> = _stockPriceHistoryError.asStateFlow()

    private val _fiftyTwoWeekRanges = MutableStateFlow<Map<String, FiftyTwoWeekRange>>(emptyMap())
    val fiftyTwoWeekRanges: StateFlow<Map<String, FiftyTwoWeekRange>> = _fiftyTwoWeekRanges.asStateFlow()

    // UI State tabs: 0 - Home, 1 - Research, 2 - Alerts, 3 - Settings
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Portfolio sort mode: "value", "change", "return", "name"
    private val _portfolioSortMode = MutableStateFlow("value")
    val portfolioSortMode: StateFlow<String> = _portfolioSortMode.asStateFlow()

    fun cyclePortfolioSort() {
        _portfolioSortMode.value = when (_portfolioSortMode.value) {
            "value" -> "change"
            "change" -> "return"
            "return" -> "name"
            else -> "value"
        }
    }

    fun setPortfolioSort(mode: String) {
        _portfolioSortMode.value = mode
    }

    // Selected stock for research/detail
    private val _selectedStockSymbol = MutableStateFlow("TATAMOTORS")
    val selectedStockSymbol: StateFlow<String> = _selectedStockSymbol.asStateFlow()

    // Real-time server-side checking sources map
    val stockVerificationSources: StateFlow<Map<String, String>> = repository.verificationSources

    // Real-time stock search states
    private val _searchedStock = MutableStateFlow<SearchedStock?>(null)
    val searchedStock: StateFlow<SearchedStock?> = _searchedStock.asStateFlow()

    // Trending Stocks for the search/research deck
    private val _trendingStocks = MutableStateFlow<List<TrendingStock>>(researchUseCase.fallbackTrendingStocks)
    val trendingStocks: StateFlow<List<TrendingStock>> = _trendingStocks.asStateFlow()

    private val _isTrendingStocksLoading = MutableStateFlow(false)
    val isTrendingStocksLoading: StateFlow<Boolean> = _isTrendingStocksLoading.asStateFlow()

    private val _trendingStocksError = MutableStateFlow<String?>(null)
    val trendingStocksError: StateFlow<String?> = _trendingStocksError.asStateFlow()

    // AI summary and loading states
    private val _portfolioAiSummary = MutableStateFlow("")
    val portfolioAiSummary: StateFlow<String> = _portfolioAiSummary.asStateFlow()

    private val _isPortfolioAiLoading = MutableStateFlow(false)
    val isPortfolioAiLoading: StateFlow<Boolean> = _isPortfolioAiLoading.asStateFlow()

    private val _singleStockAiSummary = MutableStateFlow("")
    val singleStockAiSummary: StateFlow<String> = _singleStockAiSummary.asStateFlow()

    private val _isSingleStockAiLoading = MutableStateFlow(false)
    val isSingleStockAiLoading: StateFlow<Boolean> = _isSingleStockAiLoading.asStateFlow()

    // Status or messages feedback
    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    // App theme choice: "LIGHT", "DARK", "SYSTEM"
    private val prefs = application.getSharedPreferences("family_wealth_prefs", android.content.Context.MODE_PRIVATE)
    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    fun updateTheme(themeStr: String) {
        _appTheme.value = themeStr
        prefs.edit().putString("app_theme", themeStr).apply()
    }

    init {
        viewModelScope.launch {
            try {
                authUseCase.seedSandboxData()
            } catch (e: Exception) {
                _userFeedback.value = "DB Seeding failed: ${e.message}"
            }
            
            // Web scraper: Scrapes screener.in to update all metrics once the app is launched!
            updatePortfolioMetricsOnLaunch()
        }

        // Automatically update AI summary whenever portfolio contents change
        viewModelScope.launch {
            stockAssets.collect { assets ->
                if (assets.isNotEmpty()) {
                    runAIPortfolioSummary()
                }
            }
        }

        viewModelScope.launch {
            combine(stockAssets, watchlistItems) { assets, watchlists ->
                (assets.map { it.symbol.uppercase() } + watchlists.map { it.symbol.uppercase() }).distinct()
            }
                .distinctUntilChanged()
                .collect { symbols -> refreshStockNews(symbols) }
        }

        viewModelScope.launch {
            refreshTrendingStocks()
            while (currentCoroutineContext().isActive) {
                kotlinx.coroutines.delay(300000)
                refreshTrendingStocks()
            }
        }

        viewModelScope.launch {
            selectedStockSymbol.collect { symbol -> refreshStockPriceHistory(symbol) }
        }

        viewModelScope.launch {
            refreshFiftyTwoWeekRanges()
            while (currentCoroutineContext().isActive) {
                kotlinx.coroutines.delay(1800000)
                refreshFiftyTwoWeekRanges()
            }
        }

        // Start a continuous real-time price loop for the searched stock, with Screener updates
        viewModelScope.launch {
            while (currentCoroutineContext().isActive) {
                kotlinx.coroutines.delay(8000) // update searches every 8s to moderate network frequency
                val current = _searchedStock.value ?: continue
                
                val liveData = repository.fetchLiveStockData(current.symbol)
                if (liveData != null) {
                    val realPrice = Math.round(liveData.first * 100.0) / 100.0
                    val realChange = Math.round(liveData.second * 100.0) / 100.0
                    _searchedStock.value = current.copy(
                        currentPrice = realPrice,
                        dailyChangePercentage = realChange
                    )
                    repository.setVerificationSource(current.symbol, "Live Yahoo Finance API (NSE)")
                } else {
                    val percentageMovement = (kotlin.random.Random.nextDouble() * 0.3 - 0.15)
                    val originalPrice = current.currentPrice
                    val priceDelta = originalPrice * (percentageMovement / 100.0)
                    val newPrice = Math.round((originalPrice + priceDelta) * 100.0) / 100.0
                    val newDailyChange = current.dailyChangePercentage + percentageMovement
                    _searchedStock.value = current.copy(
                        currentPrice = newPrice,
                        dailyChangePercentage = Math.round(newDailyChange * 100.0) / 100.0
                    )
                    repository.setVerificationSource(current.symbol, "Offline fallback price model")
                }
            }
        }

    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }

    private fun updatePortfolioMetricsOnLaunch() {
        viewModelScope.launch {
            try {
                // Settle brief delay to let database settle
                kotlinx.coroutines.delay(3000)
                
                // Update trending stocks with real data
                val updatedTrending = _trendingStocks.value.map { trend ->
                    val live = repository.fetchLiveStockData(trend.symbol)
                    if (live != null) {
                        trend.copy(currentPrice = live.first, changePercentage = live.second)
                    } else trend
                }
                _trendingStocks.value = updatedTrending

                val stocks = stockAssets.value
                if (stocks.isNotEmpty()) {
                    for (stock in stocks) {
                        val screenerData = repository.scrapeScreenerRatios(stock.symbol)
                        if (screenerData.isNotEmpty()) {
                            val scrappedPrice = screenerData["peRatio"]?.let { pe ->
                                // If we succeeded in scraping screener, we got genuine live ratios!
                                // Let's also retrieve the current price if available, else retain Yahoo / DB
                                screenerData["high52w"]?.let { h ->
                                    screenerData["low52w"]?.let { l ->
                                        (h + l) / 2.0 // fallback estimation if current price field scraped didn't override
                                    }
                                }
                            } ?: stock.currentPrice

                            val updated = stock.copy(
                                currentPrice = Math.round(scrappedPrice * 100.0) / 100.0
                            )
                            db.stockDao().updateStock(updated)
                            repository.setVerificationSource(stock.symbol, "Screener.in Scraper (Updates Completed Live)")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Failed to run Screener setup scrape on launch: ${e.message}")
            }
        }
    }

    private suspend fun refreshStockNews(symbols: List<String>) {
        _isStockNewsLoading.value = true
        _stockNewsError.value = null
        val result = researchUseCase.fetchStockNews(symbols)
        _stockNews.value = result.articles
        _stockNewsError.value = result.errorMessage
        _isStockNewsLoading.value = false
    }

    private suspend fun refreshTrendingStocks() {
        _isTrendingStocksLoading.value = true
        _trendingStocksError.value = null
        val result = researchUseCase.fetchTrendingStocks()
        _trendingStocks.value = result.stocks
        _trendingStocksError.value = result.errorMessage
        _isTrendingStocksLoading.value = false
    }

    private suspend fun refreshStockPriceHistory(symbol: String) {
        _isStockPriceHistoryLoading.value = true
        _stockPriceHistoryError.value = null
        val result = researchUseCase.fetchPriceHistory(symbol)
        if (result.symbol.isNotBlank()) {
            _stockPriceHistory.value = _stockPriceHistory.value + (result.symbol to result.history)
            _stockPriceHistoryError.value = result.errorMessage
        }
        _isStockPriceHistoryLoading.value = false
    }

    private suspend fun refreshFiftyTwoWeekRanges() {
        val ranges = researchUseCase.fetchFiftyTwoWeekRanges()
        if (ranges.isNotEmpty()) {
            _fiftyTwoWeekRanges.value = ranges
        }
    }

    fun searchStock(query: String) {
        val initialResult = researchUseCase.createInitialSearchResult(query) ?: return
        val cleanQuery = initialResult.symbol
        _searchedStock.value = initialResult
        repository.setVerificationSource(cleanQuery, "Verifying live sources...")

        // Programmatically select it to load charts & initiate Gemini analytic hook
        selectStock(cleanQuery)

        // Async Live Fetch Overrides
        viewModelScope.launch {
            // 1. Fetch live price/changes from Yahoo Finance
            val liveData = repository.fetchLiveStockData(cleanQuery)
            if (liveData != null) {
                _searchedStock.value = researchUseCase.applyLiveQuote(_searchedStock.value ?: initialResult, liveData)
                repository.setVerificationSource(cleanQuery, "Live Yahoo Finance API (NSE)")
            }

            // 2. Fetch company details, fundamentals, and recent news from IndianAPI.
            val stockDetails = repository.fetchStockDetails(cleanQuery)
            if (stockDetails != null) {
                val currentFetched = _searchedStock.value ?: initialResult
                _searchedStock.value = researchUseCase.applyStockDetails(currentFetched, stockDetails)
                repository.setVerificationSource(stockDetails.symbol, stockDetails.source)
                if (stockDetails.recentNews.isNotEmpty()) {
                    _stockNews.value = stockDetails.recentNews.map {
                        NewsArticle(it.id, it.symbol, it.title, it.source, it.time, it.summary, it.sentiment)
                    }
                }
            }

            // 3. Fallback: fetch ratios scraped from screener.in where IndianAPI did not provide values.
            val screenerData = repository.scrapeScreenerRatios(cleanQuery)
            if (screenerData.isNotEmpty()) {
                val currentFetched = _searchedStock.value ?: initialResult
                _searchedStock.value = researchUseCase.applyScreenerData(currentFetched, screenerData)
                if (stockDetails == null) {
                    repository.setVerificationSource(cleanQuery, "Live Screener.in Web Scraper + Yahoo Finance API")
                }
            } else {
                if (liveData == null && stockDetails == null) {
                    repository.setVerificationSource(cleanQuery, "Offline fallback price model")
                }
            }
        }
    }

    // --- Secure User Authentication Actions ---
    fun registerNewUser(username: String, fullName: String, passcode: String) {
        viewModelScope.launch {
            try {
                applyAuthResult(authUseCase.registerUser(username, fullName, passcode))
            } catch (e: Exception) {
                _userFeedback.value = "Registration error: ${e.message}"
            }
        }
    }

    fun loginUser(username: String, passcode: String) {
        viewModelScope.launch {
            try {
                applyAuthResult(authUseCase.loginUser(username, passcode))
            } catch (e: Exception) {
                _userFeedback.value = "Login failed: ${e.message}"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentGroup.value = null
        _userFeedback.value = "Logged out successfully"
    }

    // --- Private Family Group Actions ---
    fun createFamilyGroup(groupName: String) {
        viewModelScope.launch {
            try {
                applyAuthResult(authUseCase.createFamilyGroup(_currentUser.value, groupName))
            } catch (e: Exception) {
                _userFeedback.value = "Failed to form group: ${e.message}"
            }
        }
    }

    fun joinFamilyGroup(inviteCode: String) {
        viewModelScope.launch {
            try {
                applyAuthResult(authUseCase.joinFamilyGroup(_currentUser.value, inviteCode))
            } catch (e: Exception) {
                _userFeedback.value = "Failed to join group: ${e.message}"
            }
        }
    }

    private fun applyAuthResult(result: AuthActionResult) {
        when (result) {
            is AuthActionResult.Success -> {
                _currentUser.value = result.user
                _currentGroup.value = result.group
                _userFeedback.value = result.message
                if (result.refreshPortfolioSummary) {
                    runAIPortfolioSummary()
                }
            }
            is AuthActionResult.Error -> {
                _userFeedback.value = result.message
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun selectStock(symbol: String) {
        _selectedStockSymbol.value = symbol.uppercase()
        _singleStockAiSummary.value = "" // clear previous AI summaries
        runSingleStockAiSummary(symbol)
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }

    // --- Chat Actions ---
    fun sendChatMessage(sender: String, messageText: String, recommendedSymbol: String? = null) {
        val currentG = _currentGroup.value?.groupId ?: "SHARMA_GROUP"
        viewModelScope.launch {
            watchlistChatUseCase.sendChatMessage(sender, messageText, recommendedSymbol, currentG)
        }
    }

    // --- Transactions (Buy / Sell) ---
    fun recordStockBuy(symbol: String, name: String, shares: Double, buyPrice: Double, currentPrice: Double) {
        val currentG = _currentGroup.value?.groupId ?: "SHARMA_GROUP"
        viewModelScope.launch {
            try {
                val formattedSymbol = symbol.uppercase().trim()
                val existing = stockAssets.value.find { it.symbol.uppercase() == formattedSymbol }
                if (existing != null) {
                    val totalShares = existing.shares + shares
                    val totalCost = (existing.shares * existing.avgPrice) + (shares * buyPrice)
                    val newAvgPrice = if (totalShares > 0) totalCost / totalShares else 0.0
                    val updated = existing.copy(
                        shares = totalShares,
                        avgPrice = Math.round(newAvgPrice * 100.0) / 100.0,
                        currentPrice = currentPrice
                    )
                    db.stockDao().updateStock(updated)
                } else {
                    repository.addStock(formattedSymbol, name, shares, buyPrice, currentPrice, currentG)
                }
                
                repository.insertChatMessage(
                    "System Transaction",
                    "Bought $shares shares of $formattedSymbol at ₹$buyPrice for the Combined Portfolio.",
                    groupId = currentG
                )
                
                _userFeedback.value = "Transaction recorded successfully!"
            } catch (e: Exception) {
                _userFeedback.value = "Error: ${e.message}"
            }
        }
    }

    fun importPortfolioFile(uri: Uri) {
        val context = getApplication<Application>()
        val contentResolver = context.contentResolver
        
        // Try to determine file type from name or mime
        var fileName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }
        
        val isExcel = fileName.endsWith(".xlsx", true) || fileName.endsWith(".xls", true)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    if (isExcel) {
                        performXlsxImport(inputStream)
                    } else {
                        val csvText = inputStream.bufferedReader().use { it.readText() }
                        performCsvImport(csvText)
                    }
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Failed to open input stream for $uri", e)
                _userFeedback.value = "Failed to open file: ${e.message}"
            }
        }
    }

    private suspend fun performCsvImport(csvText: String) {
        val currentG = _currentGroup.value?.groupId ?: "SHARMA_GROUP"
        try {
            val parsedAssets = PortfolioImportParser.parseCsv(csvText, currentG)
            finishPortfolioImport(parsedAssets, currentG, "statement CSV")
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error parsing CSV file", e)
            _userFeedback.value = "Failed to parse CSV: ${e.message}"
        }
    }

    fun importPortfolioCsv(csvText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            performCsvImport(csvText)
        }
    }

    private suspend fun performXlsxImport(inputStream: InputStream) {
        val currentG = _currentGroup.value?.groupId ?: "SHARMA_GROUP"
        try {
            val parsedAssets = PortfolioImportParser.parseXlsx(inputStream, currentG)
            finishPortfolioImport(parsedAssets, currentG, "Kite XLSX statement")
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error parsing Kite XLSX file", e)
            _userFeedback.value = "Failed to parse XLSX: ${e.message}"
        }
    }

    private suspend fun finishPortfolioImport(parsedAssets: List<StockAsset>, groupId: String, sourceLabel: String) {
        if (parsedAssets.isEmpty()) {
            _userFeedback.value = "No valid stock records found in $sourceLabel."
            return
        }

        repository.replaceStocksForGroup(groupId, parsedAssets)
        repository.insertChatMessage(
            "System Ledger",
            "Imported ${parsedAssets.size} assets from $sourceLabel successfully.",
            groupId = groupId
        )
        _userFeedback.value = "Successfully imported ${parsedAssets.size} stocks!"
        for (asset in parsedAssets) {
            viewModelScope.launch { repository.fetchLiveStockData(asset.symbol) }
        }
        runAIPortfolioSummary()
    }

    fun importPortfolioXlsx(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            performXlsxImport(inputStream)
        }
    }

    fun recordStockSell(symbol: String, shares: Double, sellPrice: Double) {
        val currentG = _currentGroup.value?.groupId ?: "SHARMA_GROUP"
        viewModelScope.launch {
            try {
                val formattedSymbol = symbol.uppercase().trim()
                val existing = stockAssets.value.find { it.symbol.uppercase() == formattedSymbol }
                if (existing == null) {
                    _userFeedback.value = "Cannot sell. Stock not found in holdings."
                    return@launch
                }
                if (existing.shares < shares) {
                    _userFeedback.value = "Insufficient shares to perform transaction."
                    return@launch
                }

                val remainingShares = existing.shares - shares
                if (remainingShares <= 0.0) {
                    repository.removeStockById(existing.id)
                } else {
                    val updated = existing.copy(shares = remainingShares)
                    db.stockDao().updateStock(updated)
                }

                repository.insertChatMessage(
                    "System Transaction",
                    "Sold $shares shares of $formattedSymbol at ₹$sellPrice. Capital unlocked for reinvestment.",
                    groupId = currentG
                )

                _userFeedback.value = "Sale recorded successfully!"
            } catch (e: Exception) {
                _userFeedback.value = "Error: ${e.message}"
            }
        }
    }

    fun deleteEntireAsset(id: Int) {
        viewModelScope.launch {
            repository.removeStockById(id)
        }
    }

    fun deleteMember(username: String) {
        viewModelScope.launch {
            authUseCase.removeMember(username)?.let { _userFeedback.value = it }
        }
    }

    // --- Price Alerts ---
    fun createPriceAlert(symbol: String, targetPrice: Double, triggerType: String) {
        val currentG = _currentGroup.value?.groupId ?: "SHARMA_GROUP"
        viewModelScope.launch {
            try {
                when (val result = watchlistChatUseCase.createPriceAlert(symbol, targetPrice, triggerType, currentG)) {
                    is WatchlistChatResult.Success -> _userFeedback.value = result.message
                    WatchlistChatResult.Ignored -> Unit
                }
            } catch (e: Exception) {
                _userFeedback.value = e.message
            }
        }
    }

    fun toggleAlertState(alert: PriceAlert) {
        viewModelScope.launch {
            repository.toggleAlertActiveState(alert)
        }
    }

    fun deleteAlert(id: Int) {
        viewModelScope.launch {
            repository.deleteAlertById(id)
        }
    }

    // --- Shared Watchlist ---
    fun addWatchlistItem(symbol: String, name: String, targetPrice: Double, currentPrice: Double, dailyChange: Double) {
        val currentG = _currentGroup.value?.groupId ?: "SHARMA_GROUP"
        viewModelScope.launch {
            try {
                when (val result = watchlistChatUseCase.addWatchlistItem(symbol, name, targetPrice, currentPrice, dailyChange, currentG)) {
                    is WatchlistChatResult.Success -> _userFeedback.value = result.message
                    WatchlistChatResult.Ignored -> Unit
                }
            } catch (e: Exception) {
                _userFeedback.value = e.message
            }
        }
    }

    fun deleteWatchlistItem(id: Int) {
        viewModelScope.launch {
            watchlistChatUseCase.deleteWatchlistItem(id)
        }
    }

    fun toggleWatchlistItem(id: Int) {
        viewModelScope.launch {
            watchlistChatUseCase.toggleWatchlistItem(id)
        }
    }

    // --- Gemini AI Triggers ---
    fun runAIPortfolioSummary() {
        viewModelScope.launch {
            _isPortfolioAiLoading.value = true
            try {
                val list = stockAssets.value
                val result = GeminiService.analyzePortfolio(list)
                _portfolioAiSummary.value = result
            } catch (e: Exception) {
                _portfolioAiSummary.value = "Analysis unavailable: ${e.message}"
            } finally {
                _isPortfolioAiLoading.value = false
            }
        }
    }

    fun runSingleStockAiSummary(symbol: String) {
        viewModelScope.launch {
            _isSingleStockAiLoading.value = true
            try {
                val result = GeminiService.analyzeSingleStock(symbol)
                _singleStockAiSummary.value = result
            } catch (e: Exception) {
                _singleStockAiSummary.value = "Market summary unavailable for $symbol: ${e.message}"
            } finally {
                _isSingleStockAiLoading.value = false
            }
        }
    }

    // Factory helper
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class NewsArticle(
    val id: String,
    val symbol: String,
    val title: String,
    val source: String,
    val timeStr: String,
    val summary: String,
    val sentiment: String
)
