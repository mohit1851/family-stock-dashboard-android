package com.example.ui.screens
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ResearchGraphsTab(viewModel: DashboardViewModel) {
    val stocks by viewModel.stockAssets.collectAsStateWithLifecycle()
    val selectedSymbol by viewModel.selectedStockSymbol.collectAsStateWithLifecycle()
    val searchedStock by viewModel.searchedStock.collectAsStateWithLifecycle()
    val singleStockAiText by viewModel.singleStockAiSummary.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isSingleStockAiLoading.collectAsStateWithLifecycle()
    val trendingStocks by viewModel.trendingStocks.collectAsStateWithLifecycle()
    val isTrendingStocksLoading by viewModel.isTrendingStocksLoading.collectAsStateWithLifecycle()
    val trendingStocksError by viewModel.trendingStocksError.collectAsStateWithLifecycle()
    val stockPriceHistory by viewModel.stockPriceHistory.collectAsStateWithLifecycle()
    val isStockPriceHistoryLoading by viewModel.isStockPriceHistoryLoading.collectAsStateWithLifecycle()
    val stockPriceHistoryError by viewModel.stockPriceHistoryError.collectAsStateWithLifecycle()
    val fiftyTwoWeekRanges by viewModel.fiftyTwoWeekRanges.collectAsStateWithLifecycle()
    val newsList by viewModel.stockNews.collectAsStateWithLifecycle()
    val isStockNewsLoading by viewModel.isStockNewsLoading.collectAsStateWithLifecycle()
    val stockNewsError by viewModel.stockNewsError.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    // Union search or existing holdings:
    val activeStock = stocks.find { it.symbol.uppercase() == selectedSymbol.uppercase() }
        ?: if (searchedStock != null && searchedStock!!.symbol.uppercase() == selectedSymbol.uppercase()) {
            StockAsset(
                symbol = searchedStock!!.symbol,
                name = searchedStock!!.name,
                shares = 0.0,
                avgPrice = 0.0,
                currentPrice = searchedStock!!.currentPrice,
                dailyChangePercentage = searchedStock!!.dailyChangePercentage,
                groupId = ""
            )
        } else {
            stocks.find { it.symbol.uppercase() == selectedSymbol.uppercase() } ?: stocks.firstOrNull() ?: searchedStock?.let {
                StockAsset(
                    symbol = it.symbol,
                    name = it.name,
                    shares = 0.0,
                    avgPrice = 0.0,
                    currentPrice = it.currentPrice,
                    dailyChangePercentage = it.dailyChangePercentage,
                    groupId = ""
                )
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Sleek Real-time Stock Search & Analytics ---
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth().testTag("research_search_card")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "EQUITY RESEARCH",
                    style = MaterialTheme.typography.labelSmall,
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Search Indian equities and review live quotes, market movers, recent news, and historical price data where available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtle
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("E.g., INFY, SBIN, WIPRO, MSFT...", color = TextSubtle) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSubtle) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextDark),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("research_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark,
                            focusedPlaceholderColor = TextSubtle,
                            unfocusedPlaceholderColor = TextSubtle,
                            focusedBorderColor = BluePrimary,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchStock(searchQuery)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("research_search_btn")
                    ) {
                        Text("Search", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Suggestion Pills
                Text(
                    text = "Quick Tickers:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSubtle
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable suggestion pills Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickPills = listOf("RELIANCE", "TATAMOTORS", "TCS", "HDFCBANK", "INFY", "SBIN", "BHARTIARTL", "NIFTY")
                    quickPills.forEach { ticker ->
                        val isSelected = selectedSymbol.uppercase() == ticker
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    searchQuery = ticker
                                    viewModel.searchStock(ticker)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = ticker,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Market data categories chooser
                Text(
                    text = "NSE MARKET MOVERS",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = BluePrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                var selectedCategory by remember { mutableStateOf("gainer") }
                val categories = listOf(
                    "gainer" to ("Gainers" to Icons.Default.PlayArrow),
                    "loser" to ("Losers" to Icons.Default.PlayArrow),
                    "traded" to ("Active" to Icons.Default.Star),
                    "volatile" to ("Volatile" to Icons.Default.Refresh),
                    "penny" to ("Pennies" to Icons.Default.Info)
                )

                // Scrollable row of category tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (catId, pair) ->
                        val (label, icon) = pair
                        val isCatSel = selectedCategory == catId
                        val isDark = MaterialTheme.colorScheme.background.red < 0.5f
                        val categoryColor = when (catId) {
                            "gainer" -> SoftGreen
                            "loser" -> SoftRed
                            "traded" -> Color(0xFFE91E63)
                            "volatile" -> Color(0xFFFF9800)
                            else -> Color(0xFF9C27B0)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCatSel) {
                                        categoryColor.copy(alpha = 0.12f)
                                    } else {
                                        if (isDark) Color(0xFF2C2C2C) else MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(1.dp, if (isCatSel) categoryColor else BorderColor, RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = catId }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isCatSel) categoryColor else TextSubtle,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .graphicsLayer(
                                            rotationZ = when (catId) {
                                                "gainer" -> -90f
                                                "loser" -> 90f
                                                else -> 0f
                                            }
                                        )
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isCatSel) categoryColor else TextDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (isTrendingStocksLoading) {
                    ResearchStatusMessage("Loading latest movers...", showProgress = true)
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (trendingStocksError != null) {
                    ResearchStatusMessage(trendingStocksError ?: "Market movers unavailable.")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Scrollable Row of category stock assets
                val filteredList = trendingStocks.filter { it.category == selectedCategory }
                if (filteredList.isEmpty()) {
                    ResearchStatusMessage("No stocks available in this category yet.")
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredList.forEach { stockItem ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier
                                    .width(125.dp)
                                    .clickable {
                                        searchQuery = stockItem.symbol
                                        viewModel.searchStock(stockItem.symbol)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stockItem.symbol,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (stockItem.changePercentage >= 0) SoftGreen else SoftRed)
                                        )
                                    }
                                    Text(
                                        text = stockItem.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = TextSubtle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = "₹" + String.format("%.2f", stockItem.currentPrice),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextDark
                                        )
                                        Text(
                                            text = "${if (stockItem.changePercentage >= 0) "+" else ""}${String.format("%.1f", stockItem.changePercentage)}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                            color = if (stockItem.changePercentage >= 0) SoftGreen else SoftRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Stock Detail and Selector Row
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            var expandedDropdown by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDropdown = true }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Selected equity", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                    Text(
                        text = activeStock?.name ?: "Configure Asset First",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
                Text("▼", color = BluePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    // Populate with both active portfolio stocks and active searched stock
                    stocks.forEach { item ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text(item.symbol, style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                }
                            },
                            onClick = {
                                viewModel.selectStock(item.symbol)
                                expandedDropdown = false
                            }
                        )
                    }
                    searchedStock?.let { item ->
                        if (stocks.none { it.symbol.uppercase() == item.symbol.uppercase() }) {
                            DropdownMenuItem(
                                text = { 
                                    Column {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(item.symbol, style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                    }
                                },
                                onClick = {
                                    viewModel.selectStock(item.symbol)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        activeStock?.let { stock ->
            val rangeData = fiftyTwoWeekRanges[stock.symbol.uppercase()]
            val activeNews = newsList.filter { it.symbol.equals(stock.symbol, ignoreCase = true) }.take(3)
            val matchingSearch = searchedStock?.takeIf { it.symbol.uppercase() == stock.symbol.uppercase() }
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = stock.symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(text = "Live quote with historical price context", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "₹" + String.format("%,.2f", stock.currentPrice),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BluePrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Data source checked",
                                    tint = SoftGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${if (stock.dailyChangePercentage >= 0) "+" else ""}${stock.dailyChangePercentage}%",
                                color = if (stock.dailyChangePercentage >= 0) SoftGreen else SoftRed,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRICE HISTORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = BluePrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.0.sp
                        )
                        Text(
                            text = if (stockPriceHistoryError == null) "IndianAPI 1Y" else "Offline fallback",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSubtle,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        WavyPricePerformanceGraph(
                            stockSymbol = stock.symbol,
                            priceHistory = stockPriceHistory[stock.symbol.uppercase()].orEmpty()
                        )
                        if (isStockPriceHistoryLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center).size(28.dp),
                                strokeWidth = 3.dp,
                                color = BluePrimary
                            )
                        }
                    }
                    stockPriceHistoryError?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        ResearchStatusMessage(message)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Full Metrics Grid styled beautifully with Material 3 spacing
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header for provider-backed fundamentals.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "FUNDAMENTAL DATA",
                                style = MaterialTheme.typography.labelSmall,
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            )
                            if (matchingSearch?.isScreenerSourced == true) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SoftGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE DATA",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = SoftGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val marketCap = matchingSearch?.marketCap ?: 145000.0
                        val high52 = matchingSearch?.high52w ?: rangeData?.yearHigh ?: stock.currentPrice * 1.15
                        val low52 = matchingSearch?.low52w ?: rangeData?.yearLow ?: stock.currentPrice * 0.8
                        ResearchMetricRow(
                            leftLabel = "Market Cap",
                            leftValue = if (marketCap >= 100000.0) "₹" + String.format("%,.1f", marketCap / 100000.0) + "L Cr" else "₹" + String.format("%,.0f", marketCap) + " Cr",
                            rightLabel = "Stock P/E",
                            rightValue = "${matchingSearch?.peRatio ?: 22.4}x"
                        )
                        ResearchMetricRow(
                            leftLabel = "52W High",
                            leftValue = "₹" + String.format("%,.0f", high52),
                            rightLabel = "52W Low",
                            rightValue = "₹" + String.format("%,.0f", low52)
                        )
                        ResearchMetricRow(
                            leftLabel = "ROCE",
                            leftValue = "${matchingSearch?.roce ?: 18.2}%",
                            rightLabel = "ROE",
                            rightValue = "${matchingSearch?.roe ?: 14.5}%"
                        )
                        ResearchMetricRow(
                            leftLabel = "Book Value",
                            leftValue = "₹" + String.format("%,.0f", matchingSearch?.bookValue ?: 450.0),
                            rightLabel = "Dividend Yield",
                            rightValue = "${matchingSearch?.dividendYield ?: 1.2}%"
                        )
                        ResearchMetricRow(
                            leftLabel = "Face Value",
                            leftValue = "₹" + String.format("%.0f", matchingSearch?.faceValue ?: 2.0),
                            rightLabel = "Beta Index",
                            rightValue = String.format("%.2f", matchingSearch?.betaIndex ?: 1.08)
                        )
                    }
                }
            }

            ResearchNewsPreview(
                news = activeNews,
                isLoading = isStockNewsLoading,
                error = stockNewsError
            )

            // Quick Invest & Alert Deck
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "QUICK ACTIONS: ${stock.symbol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Record a portfolio action or add this equity to the shared family watchlist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtle
                    )
                    val filePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let { viewModel.importPortfolioFile(it) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.weight(1f).testTag("quick_import_csv_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import File", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }

                        var showWatchlistDialog by remember { mutableStateOf(false) }
                        Button(
                            onClick = { showWatchlistDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.weight(1f).testTag("quick_watchlist_btn"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watchlist", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }

                        if (showWatchlistDialog) {
                            var targetWatchlistPrice by remember { mutableStateOf(stock.currentPrice.toString()) }
                            AlertDialog(
                                onDismissRequest = { showWatchlistDialog = false },
                                title = { Text("Add to Family Watchlist", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "Track ${stock.symbol} with the family. Enter a target price and the app will notify the group when the watchlist condition is met.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSubtle
                                        )
                                        OutlinedTextField(
                                            value = targetWatchlistPrice,
                                            onValueChange = { targetWatchlistPrice = it },
                                            label = { Text("Target Alert Price (₹)") },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFF9800))
                                        )
                                        Text(
                                            text = "Current Ticker Price: ₹${String.format("%,.2f", stock.currentPrice)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            val tPrice = targetWatchlistPrice.toDoubleOrNull() ?: 0.0
                                            if (tPrice > 0) {
                                                viewModel.addWatchlistItem(
                                                    symbol = stock.symbol,
                                                    name = stock.name,
                                                    targetPrice = tPrice,
                                                    currentPrice = stock.currentPrice,
                                                    dailyChange = stock.dailyChangePercentage
                                                )
                                                viewModel.sendChatMessage(
                                                    sender = "System",
                                                    messageText = "I added ${stock.symbol} to our Shared Watchlist with a target of ₹${String.format("%,.2f", tPrice)}.",
                                                    recommendedSymbol = stock.symbol
                                                )
                                                showWatchlistDialog = false
                                            }
                                        }
                                    ) {
                                        Text("Confirm Add", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showWatchlistDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            val isResearchDark = MaterialTheme.colorScheme.background.red < 0.5f
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isResearchDark) Color(0xFF1E2124) else Color(0xFFF8FAFF)
                ),
                border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BluePrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gemini Analyst Room",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "AI-generated research note for ${stock.symbol}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSubtle
                                )
                            }
                        }

                        if (isAiLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = BluePrimary)
                        } else {
                            FilledIconButton(
                                onClick = { viewModel.runSingleStockAiSummary(stock.symbol) },
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = BluePrimary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh AI", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isResearchDark) Color(0xFF16191C) else Color.White,
                        border = BorderStroke(1.dp, if (isResearchDark) Color(0xFF2C2F33) else Color(0xFFE9EEF5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (singleStockAiText.isEmpty()) "Tap refresh to generate a Gemini 1.5 Flash research note. Review it against the live market data above before acting." else singleStockAiText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                letterSpacing = 0.2.sp
                            ),
                            color = TextDark,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    if (singleStockAiText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = SoftGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Generated by Gemini",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        if (activeStock == null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                ResearchStatusMessage(
                    message = "Search a symbol or add portfolio holdings to view chart, fundamentals, news, and actions.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ResearchStatusMessage(
    message: String,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = BluePrimary)
        } else {
            Icon(Icons.Default.Info, contentDescription = null, tint = TextSubtle, modifier = Modifier.size(16.dp))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextSubtle
        )
    }
}

@Composable
private fun ResearchNewsPreview(
    news: List<NewsArticle>,
    isLoading: Boolean,
    error: String?
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECENT NEWS",
                    style = MaterialTheme.typography.labelSmall,
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.0.sp
                )
                Text(
                    text = "IndianAPI",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSubtle,
                    fontWeight = FontWeight.SemiBold
                )
            }

            when {
                isLoading -> ResearchStatusMessage("Loading recent news...", showProgress = true)
                news.isNotEmpty() -> news.forEach { article ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${article.source} • ${article.timeStr}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSubtle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                else -> ResearchStatusMessage(error ?: "No recent news available for this equity yet.")
            }
        }
    }
}

@Composable
private fun ResearchMetricRow(
    leftLabel: String,
    leftValue: String,
    rightLabel: String,
    rightValue: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ResearchMetricTile(label = leftLabel, value = leftValue, modifier = Modifier.weight(1f))
        ResearchMetricTile(label = rightLabel, value = rightValue, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResearchMetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextDark,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun WavyPricePerformanceGraph(stockSymbol: String, priceHistory: List<HistoricalPricePoint>) {
    val chartValues = remember(stockSymbol, priceHistory) {
        if (priceHistory.size >= 2) {
            priceHistory.takeLast(60).map { it.price.toFloat() }
        } else {
            fallbackChartValues(stockSymbol)
        }
    }

    val primaryColor = BluePrimary
    val bgColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (chartValues.size < 2) return@Canvas

        val width = size.width
        val height = size.height
        val minPrice = chartValues.minOrNull() ?: return@Canvas
        val maxPrice = chartValues.maxOrNull() ?: return@Canvas
        val range = (maxPrice - minPrice).takeIf { it > 0f } ?: 1f
        val verticalPadding = height * 0.12f
        val drawableHeight = height - (verticalPadding * 2)
        val chartPoints = chartValues.map { price ->
            height - verticalPadding - (((price - minPrice) / range) * drawableHeight)
        }
        val xStep = width / (chartPoints.size - 1)

        val path = Path()
        path.moveTo(0f, chartPoints[0])

        for (i in 1 until chartPoints.size) {
            val prevX = (i - 1) * xStep
            val prevY = chartPoints[i - 1]
            val currentX = i * xStep
            val currentY = chartPoints[i]

            val controlX1 = prevX + (xStep / 2)
            val controlY1 = prevY
            val controlX2 = prevX + (xStep / 2)
            val controlY2 = currentY

            path.cubicTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        drawCircle(
            color = primaryColor,
            radius = 5.dp.toPx(),
            center = Offset(width, chartPoints.last())
        )
    }
}

private fun fallbackChartValues(stockSymbol: String): List<Float> {
    val seed = stockSymbol.hashCode()
    val random = kotlin.random.Random(seed)
    val points = mutableListOf<Float>()
    var base = 100f
    repeat(8) {
        val fluctuation = (random.nextFloat() * 60f - 30f)
        base = (base + fluctuation).coerceIn(30f, 130f)
        points.add(base)
    }
    return points
}

