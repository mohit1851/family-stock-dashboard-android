package com.example.data

import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory

object PortfolioImportParser {
    fun parseCsv(csvText: String, groupId: String): List<StockAsset> {
        val lines = csvText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.isEmpty()) return emptyList()

        val firstLine = lines.first()
        val delimiter = when {
            firstLine.contains(";") -> ";"
            firstLine.contains("\t") -> "\t"
            else -> ","
        }

        fun String.cleanCsvValue(): String = replace("\"", "").replace("'", "").trim()

        val headers = firstLine.split(delimiter).map { it.cleanCsvValue().lowercase() }
        var symbolIdx = -1
        var nameIdx = -1
        var sharesIdx = -1
        var avgPriceIdx = -1
        var currentPriceIdx = -1

        for ((index, header) in headers.withIndex()) {
            when {
                header.contains("symbol") || header.contains("ticker") || header.contains("stock") || header.contains("instrument") || header.contains("code") -> symbolIdx = index
                header == "name" || header.contains("company") || header.contains("description") || header.contains("security") -> nameIdx = index
                header.contains("share") || header.contains("quantity") || header.contains("qty") || header.contains("vol") || header.contains("size") || header.contains("units") -> sharesIdx = index
                (header.contains("buy") || header.contains("purchase") || header.contains("avg") || header.contains("cost") || header.contains("price") || header.contains("rate") || header.contains("entry"))
                    && !header.contains("current") && !header.contains("market") && !header.contains("last") && !header.contains("cmp") && !header.contains("live") -> avgPriceIdx = index
                header.contains("current") || header.contains("market") || header.contains("last") || header.contains("live") || header.contains("cmp") -> currentPriceIdx = index
            }
        }

        val hasHeaders = symbolIdx != -1 || sharesIdx != -1 || avgPriceIdx != -1
        val startLineIdx = if (hasHeaders) 1 else 0

        fun String.toCleanDouble(): Double? {
            val cleaned = replace(Regex("[^0-9.-]"), "")
            return cleaned.toDoubleOrNull()
        }

        return (startLineIdx until lines.size).mapNotNull { i ->
            val parts = lines[i].split(delimiter).map { it.cleanCsvValue() }
            if (parts.size < 2) return@mapNotNull null

            val symbol = if (hasHeaders && symbolIdx != -1 && symbolIdx < parts.size) {
                parts[symbolIdx].uppercase()
            } else {
                parts.getOrNull(0)?.uppercase() ?: ""
            }
            if (symbol.isBlank() || symbol.toDoubleOrNull() != null || symbol.lowercase() == "symbol" || symbol.lowercase() == "ticker") return@mapNotNull null

            val name = if (hasHeaders && nameIdx != -1 && nameIdx < parts.size) {
                parts[nameIdx]
            } else if (!hasHeaders && parts.size > 1) {
                parts.getOrNull(1) ?: "$symbol Corporation"
            } else {
                "$symbol Corporation"
            }

            val shares = if (hasHeaders && sharesIdx != -1 && sharesIdx < parts.size) {
                parts[sharesIdx].toCleanDouble() ?: 1.0
            } else {
                parts.getOrNull(2)?.toCleanDouble() ?: parts.getOrNull(1)?.toCleanDouble() ?: 1.0
            }

            val avgPrice = if (hasHeaders && avgPriceIdx != -1 && avgPriceIdx < parts.size) {
                parts[avgPriceIdx].toCleanDouble() ?: 100.0
            } else {
                parts.getOrNull(3)?.toCleanDouble() ?: parts.getOrNull(2)?.toCleanDouble() ?: 100.0
            }

            val currentPrice = if (hasHeaders && currentPriceIdx != -1 && currentPriceIdx < parts.size) {
                parts[currentPriceIdx].toCleanDouble() ?: avgPrice
            } else {
                parts.getOrNull(4)?.toCleanDouble() ?: avgPrice
            }

            StockAsset(
                symbol = symbol,
                name = name,
                shares = shares,
                avgPrice = avgPrice,
                currentPrice = currentPrice,
                dailyChangePercentage = 0.0,
                groupId = groupId
            )
        }
    }

    fun parseXlsx(inputStream: java.io.InputStream, groupId: String): List<StockAsset> {
        val workbook = WorkbookFactory.create(inputStream)
        workbook.use {
            val sheet = it.getSheetAt(0)
            val rows = sheet.iterator()
            val formatter = DataFormatter()

            if (!rows.hasNext()) return emptyList()

            var headerRow: org.apache.poi.ss.usermodel.Row? = null
            while (rows.hasNext()) {
                val row = rows.next()
                var hasSymbol = false
                var hasQty = false
                for (cell in row) {
                    val cellValue = formatter.formatCellValue(cell).lowercase()
                    if (cellValue.contains("symbol") || cellValue.contains("ticker")) hasSymbol = true
                    if (cellValue.contains("quantity") || cellValue.contains("qty")) hasQty = true
                }
                if (hasSymbol && hasQty) {
                    headerRow = row
                    break
                }
            }

            val header = headerRow ?: return emptyList()
            var symbolIdx = -1
            var nameIdx = -1
            var sharesIdx = -1
            var avgPriceIdx = -1
            var currentPriceIdx = -1

            for (i in 0 until header.lastCellNum) {
                val headerText = formatter.formatCellValue(header.getCell(i)).lowercase().trim()
                when {
                    headerText.contains("symbol") || headerText == "ticker" || headerText == "instrument" -> symbolIdx = i
                    headerText.contains("name") || headerText.contains("company") || headerText.contains("security") || headerText == "isin" -> if (nameIdx == -1) nameIdx = i
                    headerText.contains("quantity") || headerText.contains("qty") || headerText.contains("available") -> if (sharesIdx == -1) sharesIdx = i
                    headerText.contains("average") || headerText.contains("avg") || headerText.contains("buy") || headerText.contains("cost") || headerText == "rate" -> if (avgPriceIdx == -1) avgPriceIdx = i
                    headerText.contains("closing") || headerText.contains("last") || headerText.contains("cmp") || headerText.contains("close") || headerText.contains("market") -> if (currentPriceIdx == -1) currentPriceIdx = i
                }
            }

            fun String?.toCleanDouble(default: Double = 0.0): Double {
                if (this == null) return default
                val cleaned = replace(Regex("[^0-9.-]"), "")
                return cleaned.toDoubleOrNull() ?: default
            }

            val parsedAssets = mutableListOf<StockAsset>()
            while (rows.hasNext()) {
                val row = rows.next()
                fun getCleanString(idx: Int): String? {
                    if (idx == -1) return null
                    val cell = row.getCell(idx) ?: return null
                    return formatter.formatCellValue(cell).trim()
                }

                val symbol = getCleanString(symbolIdx)?.uppercase() ?: ""
                if (symbol.isBlank() || symbol == "SYMBOL" || symbol == "TICKER" || symbol.length > 20) continue
                if (symbol.toDoubleOrNull() != null) continue

                val shares = getCleanString(sharesIdx).toCleanDouble(0.0)
                if (shares <= 0) continue

                val avgPrice = getCleanString(avgPriceIdx).toCleanDouble(0.0)
                parsedAssets.add(
                    StockAsset(
                        symbol = symbol,
                        name = getCleanString(nameIdx) ?: "$symbol Corp",
                        shares = shares,
                        avgPrice = avgPrice,
                        currentPrice = getCleanString(currentPriceIdx).toCleanDouble(avgPrice),
                        dailyChangePercentage = 0.0,
                        groupId = groupId
                    )
                )
            }
            return parsedAssets
        }
    }
}
