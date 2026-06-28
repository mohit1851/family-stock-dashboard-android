package com.example

import com.example.ui.ResearchUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchUseCaseTest {
    private val useCase = ResearchUseCase()

    @Test
    fun createInitialSearchResultNormalizesKnownIndianSymbols() {
        val result = useCase.createInitialSearchResult(" tata motors ")

        requireNotNull(result)
        assertEquals("TATAMOTORS", result.symbol)
        assertEquals("Tata Motors Ltd.", result.name)
        assertEquals(962.15, result.currentPrice, 0.001)
        assertFalse(result.isScreenerSourced)
    }

    @Test
    fun applyLiveQuoteUpdatesPriceChangeAndDerivedRange() {
        val initial = requireNotNull(useCase.createInitialSearchResult("RELIANCE"))
        val updated = useCase.applyLiveQuote(initial, 2500.126 to -1.234)

        assertEquals(2500.13, updated.currentPrice, 0.001)
        assertEquals(-1.23, updated.dailyChangePercentage, 0.001)
        assertEquals(2950.15, updated.high52w, 0.001)
        assertEquals(1925.10, updated.low52w, 0.001)
    }

    @Test
    fun fallbackPriceHistoryIsStableAndNonEmpty() {
        val first = useCase.fallbackPriceHistory("INFY")
        val second = useCase.fallbackPriceHistory("INFY")

        assertEquals(30, first.size)
        assertEquals(first, second)
        assertTrue(first.all { it.price > 0.0 })
    }
}
