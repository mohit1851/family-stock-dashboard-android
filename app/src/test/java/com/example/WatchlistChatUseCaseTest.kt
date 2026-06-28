package com.example

import com.example.data.WatchlistChatUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchlistChatUseCaseTest {
    private val useCase = WatchlistChatUseCase()

    @Test
    fun cleanSymbolUppercasesAndTrimsInput() {
        assertEquals("RELIANCE", useCase.cleanSymbol(" reliance "))
    }

    @Test
    fun cleanRecommendedSymbolDropsBlankValues() {
        assertNull(useCase.cleanRecommendedSymbol("   "))
        assertNull(useCase.cleanRecommendedSymbol(null))
    }

    @Test
    fun cleanRecommendedSymbolNormalizesPresentValues() {
        assertEquals("TCS", useCase.cleanRecommendedSymbol(" tcs "))
    }
}
