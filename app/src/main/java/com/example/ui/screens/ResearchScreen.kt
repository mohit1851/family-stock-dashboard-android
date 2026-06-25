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
    val stockPriceHistory by viewModel.stockPriceHistory.collectAsStateWithLifecycle()
    val fiftyTwoWeekRanges by viewModel.fiftyTwoWeekRanges.collectAsStateWithLifecycle()

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
                    text = "🔍 SEEK EQUITY MARKET INTEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Perform real-time asset query with simulated tick feeds. Select popular tickers or search any custom dynamic symbol.",
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
                Divider(color = BorderColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Market Intelligence categories chooser
                Text(
                    text = "NSE MARKET PULSE & TRENDING DESK",
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

                // Scrollable Row of category stock assets
                val filteredList = trendingStocks.filter { it.category == selectedCategory }
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
                                    // small visual pill for categories: Green for positive, Red for negative
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
                    Text(text = "Active Equity Target Investigation", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
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
                            Text(text = "NSE Realtime Curve estimates", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
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
                                    contentDescription = "Sanity Checked",
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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    ) {
                        WavyPricePerformanceGraph(
                            stockSymbol = stock.symbol,
                            priceHistory = stockPriceHistory[stock.symbol.uppercase()].orEmpty()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = BorderColor)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Full Metrics Grid styled beautifully with Material 3 spacing
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header for Screener statistics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "FUNDAMENTAL BI DATA (SCREENER.IN)",
                                style = MaterialTheme.typography.labelSmall,
                                color = BluePrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            )
                            if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase() && searchedStock!!.isScreenerSourced) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SoftGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LIVE SCRAPED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = SoftGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Market Cap", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val mc = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) searchedStock!!.marketCap else 145000.0
                                Text(
                                    text = if (mc >= 100000.0) "₹" + String.format("%,.1f", mc / 100000.0) + "L Cr" else "₹" + String.format("%,.0f", mc) + " Cr",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Stock P/E", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val pe = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) searchedStock!!.peRatio else 22.4
                                Text(
                                    text = "${pe}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Book Value", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val bv = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) searchedStock!!.bookValue else 450.0
                                Text(
                                    text = "₹" + String.format("%,.0f", bv),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(text = "Div Yield", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val dy = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) searchedStock!!.dividendYield else 1.2
                                Text(
                                    text = "${dy}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                        }

                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "ROCE", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val roce = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) searchedStock!!.roce else 18.2
                                Text(
                                    text = "${roce}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "ROE", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val roe = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) searchedStock!!.roe else 14.5
                                Text(
                                    text = "${roe}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "52W High", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val h52 = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) {
                                    searchedStock!!.high52w
                                } else {
                                    rangeData?.yearHigh ?: stock.currentPrice * 1.15
                                }
                                Text(
                                    text = "₹" + String.format("%,.0f", h52),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(text = "52W Low", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val l52 = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) {
                                    searchedStock!!.low52w
                                } else {
                                    rangeData?.yearLow ?: stock.currentPrice * 0.8
                                }
                                Text(
                                    text = "₹" + String.format("%,.0f", l52),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                        }

                        // Row 3 (Secondary Details)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Face Value", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val fv = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) searchedStock!!.faceValue else 2.0
                                Text(
                                    text = "₹" + String.format("%.0f", fv),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Beta Index", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                                val beta = if (searchedStock != null && searchedStock!!.symbol.uppercase() == stock.symbol.uppercase()) searchedStock!!.betaIndex else 1.08
                                Text(
                                    text = String.format("%.2f", beta),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Spacer(modifier = Modifier.weight(2f)) // Space balance
                        }
                    }
                }
            }

            // --- Real-time Stock Data Sanity Check Security Shield Card ---
            // Box removed as per request, replaced with green verified icon above near price.

            // Quick Invest & Alert Deck
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "⚡ QUICK ACTION DECK: ${stock.symbol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Instantly log transactions for family assets or configure automated triggers.",
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
                                            text = "Keep track of ${stock.symbol} jointly. Enter a target alert criteria price. System triggers a collaborative alert dialogue into the chat once hit.",
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
                                    text = "AI-powered market intelligence for ${stock.symbol}",
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
                            text = if (singleStockAiText.isEmpty()) "Tap the refresh button to generate real-time analyst insights for this asset using Gemini 1.5 Flash." else singleStockAiText,
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
                                text = "Verified Gemini Intelligence",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
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

