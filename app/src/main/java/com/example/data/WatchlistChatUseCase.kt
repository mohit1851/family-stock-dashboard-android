package com.example.data

class WatchlistChatUseCase(private val repository: StockRepository? = null) {
    suspend fun sendChatMessage(sender: String, messageText: String, recommendedSymbol: String?, groupId: String): WatchlistChatResult {
        val cleanMessage = messageText.trim()
        if (cleanMessage.isBlank()) return WatchlistChatResult.Ignored

        requireRepository().insertChatMessage(
            sender = sender,
            message = cleanMessage,
            recommendedSymbol = cleanRecommendedSymbol(recommendedSymbol),
            groupId = groupId
        )
        return WatchlistChatResult.Success(null)
    }

    suspend fun createPriceAlert(symbol: String, targetPrice: Double, triggerType: String, groupId: String): WatchlistChatResult {
        val cleanSymbol = cleanSymbol(symbol)
        if (cleanSymbol.isBlank() || targetPrice <= 0) return WatchlistChatResult.Ignored

        requireRepository().addPriceAlert(cleanSymbol, targetPrice, triggerType, groupId)
        return WatchlistChatResult.Success("Alert successfully added for $cleanSymbol!")
    }

    suspend fun addWatchlistItem(
        symbol: String,
        name: String,
        targetPrice: Double,
        currentPrice: Double,
        dailyChange: Double,
        groupId: String
    ): WatchlistChatResult {
        val cleanSymbol = cleanSymbol(symbol)
        if (cleanSymbol.isBlank() || targetPrice <= 0) return WatchlistChatResult.Ignored

        requireRepository().addWatchlistItem(cleanSymbol, name.trim(), targetPrice, currentPrice, dailyChange, groupId)
        return WatchlistChatResult.Success("Successfully added $cleanSymbol to the Shared Watchlist!")
    }

    suspend fun deleteWatchlistItem(id: Int) {
        requireRepository().deleteWatchlistItemById(id)
    }

    suspend fun toggleWatchlistItem(id: Int) {
        requireRepository().toggleWatchlistActiveState(id)
    }

    fun cleanSymbol(symbol: String): String = symbol.uppercase().trim()

    fun cleanRecommendedSymbol(symbol: String?): String? = symbol
        ?.uppercase()
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    private fun requireRepository(): StockRepository = requireNotNull(repository) {
        "Repository-backed WatchlistChatUseCase method called without a StockRepository."
    }
}

sealed class WatchlistChatResult {
    data class Success(val message: String?) : WatchlistChatResult()
    data object Ignored : WatchlistChatResult()
}
