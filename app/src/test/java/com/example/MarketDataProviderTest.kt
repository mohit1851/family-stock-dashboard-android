package com.example

import com.example.data.CompositeMarketDataProvider
import com.example.data.MarketDataProvider
import com.example.data.MarketQuote
import com.example.data.SimulatedMarketDataProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataProviderTest {
    @Test
    fun simulatedProviderUsesFallbackPriceWhenLiveDataIsUnavailable() = runTest {
        val quote = SimulatedMarketDataProvider().getQuote(
            symbol = "RELIANCE",
            fallbackPrice = 2_400.0,
            fallbackChangePercentage = 1.0
        )

        assertNotNull(quote)
        assertEquals("RELIANCE", quote!!.symbol)
        assertFalse(quote.isLive)
        assertEquals("Verified Loop Simulator (Offline Fallback)", quote.source)
        assertTrue(quote.price in 2_390.0..2_410.0)
    }

    @Test
    fun compositeProviderStopsAtFirstSuccessfulProvider() = runTest {
        val quote = MarketQuote(
            symbol = "TCS",
            price = 3_900.0,
            changePercentage = 1.2,
            source = "Test Provider",
            isLive = true
        )
        val composite = CompositeMarketDataProvider(
            listOf(
                object : MarketDataProvider {
                    override suspend fun getQuote(symbol: String, fallbackPrice: Double?, fallbackChangePercentage: Double?) = null
                },
                object : MarketDataProvider {
                    override suspend fun getQuote(symbol: String, fallbackPrice: Double?, fallbackChangePercentage: Double?) = quote
                },
                SimulatedMarketDataProvider()
            )
        )

        assertEquals(quote, composite.getQuote("TCS"))
    }
}
