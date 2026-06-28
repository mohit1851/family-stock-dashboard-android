package com.example.ui

import com.example.data.FiftyTwoWeekRange
import com.example.data.HistoricalPricePoint
import com.example.data.IndianMarketMover
import com.example.data.IndianStockDetails
import com.example.data.IndianStockNewsItem
import com.example.data.StockRepository
import kotlin.math.round
import kotlin.random.Random

class ResearchUseCase(private val repository: StockRepository? = null) {
    val fallbackTrendingStocks: List<TrendingStock> = defaultFallbackTrendingStocks()

    suspend fun fetchStockNews(symbols: List<String>): ResearchNewsResult {
        if (symbols.isEmpty()) return ResearchNewsResult(emptyList(), null)

        return try {
            val news = requireRepository().fetchStockNews(symbols).map { it.toNewsArticle() }
            ResearchNewsResult(
                articles = news,
                errorMessage = if (news.isEmpty()) "No recent news returned for saved symbols." else null
            )
        } catch (e: Exception) {
            ResearchNewsResult(emptyList(), "Unable to load recent news.")
        }
    }

    suspend fun fetchTrendingStocks(): ResearchTrendingResult {
        val liveMovers = try {
            requireRepository().fetchMarketMovers()
        } catch (e: Exception) {
            emptyList()
        }

        if (liveMovers.isEmpty()) {
            return ResearchTrendingResult(
                stocks = fallbackTrendingStocks,
                errorMessage = "Live movers unavailable; showing fallback list."
            )
        }

        val liveTrending = liveMovers.map { it.toTrendingStock() }
        val liveCategories = liveTrending.map { it.category }.toSet()
        val fallbackForMissingCategories = fallbackTrendingStocks.filter { it.category !in liveCategories }
        return ResearchTrendingResult(liveTrending + fallbackForMissingCategories, null)
    }

    suspend fun fetchPriceHistory(symbol: String): ResearchHistoryResult {
        val cleanSymbol = symbol.uppercase().trim()
        if (cleanSymbol.isBlank()) return ResearchHistoryResult(cleanSymbol, emptyList(), null)

        val liveHistory = try {
            requireRepository().fetchHistoricalPrices(cleanSymbol)
        } catch (e: Exception) {
            emptyList()
        }
        val hasLiveHistory = liveHistory.size >= 2
        return ResearchHistoryResult(
            symbol = cleanSymbol,
            history = if (hasLiveHistory) liveHistory else fallbackPriceHistory(cleanSymbol),
            errorMessage = if (hasLiveHistory) null else "Historical data unavailable; showing offline fallback chart."
        )
    }

    suspend fun fetchFiftyTwoWeekRanges(): Map<String, FiftyTwoWeekRange> = requireRepository().fetchFiftyTwoWeekRanges()

    fun createInitialSearchResult(query: String): SearchedStock? {
        val cleanQuery = cleanSymbol(query)
        if (cleanQuery.isBlank()) return null

        val random = Random(cleanQuery.hashCode())
        val currentPrice = roundTwo(basePriceFor(cleanQuery, random))
        val roce = roundOne(random.nextDouble(12.0, 42.0))
        val marketCap = when {
            cleanQuery == "RELIANCE" -> 1_650_420.0
            cleanQuery == "TCS" -> 1_324_050.0
            currentPrice > 1000.0 -> random.nextDouble(80_000.0, 500_000.0)
            currentPrice < 100.0 -> random.nextDouble(2_000.0, 15_000.0)
            else -> random.nextDouble(5_000.0, 80_000.0)
        }

        return SearchedStock(
            symbol = cleanQuery,
            name = popularStockNames[cleanQuery] ?: "$cleanQuery India Equity",
            currentPrice = currentPrice,
            dailyChangePercentage = roundTwo(random.nextDouble(-2.5, 4.0)),
            peRatio = roundOne(random.nextDouble(10.0, 55.0)),
            betaIndex = roundTwo(random.nextDouble(0.6, 1.8)),
            high52w = roundTwo(currentPrice * 1.18),
            low52w = roundTwo(currentPrice * 0.77),
            marketCap = roundTwo(marketCap),
            dividendYield = roundTwo(random.nextDouble(0.1, 2.5)),
            roce = roce,
            roe = roundOne(roce * 0.82),
            bookValue = roundTwo(currentPrice / random.nextDouble(1.5, 6.0)),
            faceValue = listOf(1.0, 2.0, 5.0, 10.0).random(random),
            isScreenerSourced = false
        )
    }

    fun applyLiveQuote(current: SearchedStock, liveData: Pair<Double, Double>?): SearchedStock {
        if (liveData == null) return current
        val finalPrice = roundTwo(liveData.first)
        return current.copy(
            currentPrice = finalPrice,
            dailyChangePercentage = roundTwo(liveData.second),
            high52w = roundTwo(finalPrice * 1.18),
            low52w = roundTwo(finalPrice * 0.77)
        )
    }

    fun applyStockDetails(current: SearchedStock, stockDetails: IndianStockDetails): SearchedStock {
        return current.copy(
            symbol = stockDetails.symbol,
            name = stockDetails.companyName,
            currentPrice = stockDetails.currentPrice?.let(::roundTwo) ?: current.currentPrice,
            dailyChangePercentage = stockDetails.percentChange?.let(::roundTwo) ?: current.dailyChangePercentage,
            peRatio = stockDetails.peRatio ?: current.peRatio,
            marketCap = stockDetails.marketCap ?: current.marketCap,
            dividendYield = stockDetails.dividendYield ?: current.dividendYield,
            roce = stockDetails.roce ?: current.roce,
            roe = stockDetails.roe ?: current.roe,
            bookValue = stockDetails.bookValue ?: current.bookValue,
            faceValue = stockDetails.faceValue ?: current.faceValue,
            high52w = stockDetails.yearHigh ?: current.high52w,
            low52w = stockDetails.yearLow ?: current.low52w,
            isScreenerSourced = true
        )
    }

    fun applyScreenerData(current: SearchedStock, screenerData: Map<String, Double>): SearchedStock {
        if (screenerData.isEmpty()) return current
        return current.copy(
            peRatio = screenerData["peRatio"] ?: current.peRatio,
            marketCap = screenerData["marketCap"] ?: current.marketCap,
            dividendYield = screenerData["dividendYield"] ?: current.dividendYield,
            roce = screenerData["roce"] ?: current.roce,
            roe = screenerData["roe"] ?: current.roe,
            bookValue = screenerData["bookValue"] ?: current.bookValue,
            faceValue = screenerData["faceValue"] ?: current.faceValue,
            high52w = screenerData["high52w"] ?: current.high52w,
            low52w = screenerData["low52w"] ?: current.low52w,
            isScreenerSourced = true
        )
    }

    fun fallbackPriceHistory(symbol: String): List<HistoricalPricePoint> {
        val cleanSymbol = cleanSymbol(symbol)
        val random = Random(cleanSymbol.hashCode())
        var base = basePriceFor(cleanSymbol, random)

        return List(30) { index ->
            val movement = random.nextDouble(-2.0, 2.0)
            base = (base * (1.0 + movement / 100.0)).coerceAtLeast(1.0)
            HistoricalPricePoint(
                date = "T-${29 - index}",
                price = roundTwo(base)
            )
        }
    }

    fun cleanSymbol(symbol: String): String = symbol.uppercase().trim().replace(" ", "")

    private fun requireRepository(): StockRepository = requireNotNull(repository) {
        "Repository-backed ResearchUseCase method called without a StockRepository."
    }

    private fun IndianStockNewsItem.toNewsArticle(): NewsArticle = NewsArticle(
        id = id,
        symbol = symbol,
        title = title,
        source = source,
        timeStr = time,
        summary = summary,
        sentiment = sentiment
    )

    private fun IndianMarketMover.toTrendingStock(): TrendingStock = TrendingStock(
        symbol = symbol,
        name = companyName,
        currentPrice = roundTwo(currentPrice),
        changePercentage = roundTwo(changePercentage),
        volume = volume,
        category = category
    )

    private fun basePriceFor(symbol: String, random: Random): Double = when (symbol) {
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
        else -> random.nextDouble(20.0, 6000.0)
    }

    private fun roundOne(value: Double): Double = round(value * 10.0) / 10.0

    private fun roundTwo(value: Double): Double = round(value * 100.0) / 100.0

    private fun defaultFallbackTrendingStocks(): List<TrendingStock> = listOf(
        TrendingStock("TCS", "Tata Consultancy Services Ltd", 3954.80, 2.45, "1.8M", "gainer"),
        TrendingStock("SBIN", "State Bank of India", 824.60, 2.95, "14.5M", "gainer"),
        TrendingStock("INFY", "Infosys Ltd", 1452.10, 2.10, "4.2M", "gainer"),
        TrendingStock("TATAMOTORS", "Tata Motors Ltd", 962.15, 2.40, "8.9M", "gainer"),
        TrendingStock("BHARTIARTL", "Bharti Airtel Ltd", 1321.40, 1.85, "3.1M", "gainer"),
        TrendingStock("HDFCBANK", "HDFC Bank Ltd", 1582.40, -1.80, "11.2M", "loser"),
        TrendingStock("WIPRO", "Wipro Ltd", 461.30, -1.45, "5.6M", "loser"),
        TrendingStock("ITC", "ITC Ltd", 432.80, -1.15, "12.0M", "loser"),
        TrendingStock("ICICIBANK", "ICICI Bank Ltd", 1112.50, -0.95, "6.8M", "loser"),
        TrendingStock("LTIM", "LTI Mindtree Ltd", 4751.90, -2.30, "0.9M", "loser"),
        TrendingStock("ADANIENT", "Adani Enterprises Ltd", 3140.0, 5.85, "4.5M", "volatile"),
        TrendingStock("YESBANK", "Yes Bank Ltd", 21.40, -4.50, "85.2M", "volatile"),
        TrendingStock("GMRINFRA", "GMR Airports Infrastructure", 84.15, 4.20, "24.1M", "volatile"),
        TrendingStock("NHPC", "NHPC Ltd", 96.50, -3.90, "32.4M", "volatile"),
        TrendingStock("RELIANCE", "Reliance Industries Ltd", 2420.40, 1.25, "18.2M", "traded"),
        TrendingStock("TCS", "Tata Consultancy Services Ltd", 3954.80, 2.45, "1.8M", "traded"),
        TrendingStock("INFY", "Infosys Ltd", 1452.10, 2.10, "4.2M", "traded"),
        TrendingStock("SUZLON", "Suzlon Energy Ltd", 48.30, 4.98, "40.5M", "penny"),
        TrendingStock("IDEA", "Vodafone Idea Ltd", 12.50, -2.10, "120.4M", "penny"),
        TrendingStock("IRFC", "Indian Railway Finance Corp", 172.50, 3.80, "18.9M", "penny")
    )

    private val popularStockNames = mapOf(
        "RELIANCE" to "Reliance Industries Ltd.",
        "TATAMOTORS" to "Tata Motors Ltd.",
        "TCS" to "Tata Consultancy Services Ltd.",
        "HDFCBANK" to "HDFC Bank Ltd.",
        "INFY" to "Infosys Ltd.",
        "ICICIBANK" to "ICICI Bank Ltd.",
        "SBIN" to "State Bank of India",
        "BHARTIARTL" to "Bharti Airtel Ltd.",
        "WIPRO" to "Wipro Ltd.",
        "ITC" to "ITC Ltd.",
        "LTIM" to "LTI Mindtree Ltd.",
        "NIFTY" to "NIFTY 50 Index",
        "SUZLON" to "Suzlon Energy Ltd.",
        "IDEA" to "Vodafone Idea Ltd.",
        "IRFC" to "Indian Railway Finance Corporation",
        "YESBANK" to "Yes Bank Ltd.",
        "GMRINFRA" to "GMR Airports Infrastructure Ltd."
    )
}

data class ResearchNewsResult(
    val articles: List<NewsArticle>,
    val errorMessage: String?
)

data class ResearchTrendingResult(
    val stocks: List<TrendingStock>,
    val errorMessage: String?
)

data class ResearchHistoryResult(
    val symbol: String,
    val history: List<HistoricalPricePoint>,
    val errorMessage: String?
)
