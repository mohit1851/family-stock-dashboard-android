package com.example

import com.example.data.PortfolioImportParser
import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioImportParserTest {
    @Test
    fun parseCsvMapsCommonBrokerHeaders() {
        val csv = """
            Symbol,Company,Qty,Avg Price,Current Price
            RELIANCE,Reliance Industries Ltd,10,2400.50,2450.75
            TCS,Tata Consultancy Services Ltd,5,3800,3901.25
        """.trimIndent()

        val assets = PortfolioImportParser.parseCsv(csv, "FAMILY_1")

        assertEquals(2, assets.size)
        assertEquals("RELIANCE", assets[0].symbol)
        assertEquals("Reliance Industries Ltd", assets[0].name)
        assertEquals(10.0, assets[0].shares, 0.0)
        assertEquals(2400.50, assets[0].avgPrice, 0.0)
        assertEquals(2450.75, assets[0].currentPrice, 0.0)
        assertEquals("FAMILY_1", assets[0].groupId)
    }

    @Test
    fun parseCsvReturnsEmptyListForBlankFile() {
        assertEquals(emptyList<Any>(), PortfolioImportParser.parseCsv("", "FAMILY_1"))
    }
}
