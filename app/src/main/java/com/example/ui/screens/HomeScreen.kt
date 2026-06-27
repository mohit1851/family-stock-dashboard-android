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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.ui.components.*
import com.example.ui.components.getStockColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun HomeDashboardTab(viewModel: DashboardViewModel) {
    val stocks by viewModel.stockAssets.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatMessages.collectAsStateWithLifecycle()
    val alertsList by viewModel.priceAlerts.collectAsStateWithLifecycle()
    val watchlistItems by viewModel.watchlistItems.collectAsStateWithLifecycle()
    val portfolioAnalysis by viewModel.portfolioAiSummary.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isPortfolioAiLoading.collectAsStateWithLifecycle()
    val verificationSources by viewModel.stockVerificationSources.collectAsStateWithLifecycle()
    val sortModeState by viewModel.portfolioSortMode.collectAsStateWithLifecycle()
    val hasLiveFeed = verificationSources.values.any { it.contains("Live") }
    var showFullscreenChat by remember { mutableStateOf(false) }

    val totalCurrentValue = stocks.sumOf { it.shares * it.currentPrice }
    val totalCostBasis = stocks.sumOf { it.shares * it.avgPrice }
    val netProfitLoss = totalCurrentValue - totalCostBasis
    val profitPercentage = if (totalCostBasis > 0) (netProfitLoss / totalCostBasis) * 100 else 0.0
    val activeAlertCount = alertsList.filter { it.isActive }.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Combined Wealth Banner Card
        item {
            val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
            val cardBrush = if (isDarkTheme) {
                Brush.horizontalGradient(colors = listOf(Color(0xFF1E2B3C), Color(0xFF111A24)))
            } else {
                Brush.horizontalGradient(colors = listOf(Color(0xFF005FAC), Color(0xFF1565C0)))
            }
            val gainColor = if (netProfitLoss >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350)
            val dotColor = if (hasLiveFeed) Color(0xFF4CAF50) else Color(0xFFFFB74D)

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(cardBrush)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Combined Portfolio Asset Value",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )

                        // Animated counting portfolio value
                        val animatedValue by animateFloatAsState(
                            targetValue = totalCurrentValue.toFloat(),
                            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                            label = "portfolio_value"
                        )
                        Text(
                            text = "₹" + String.format("%,.2f", animatedValue.toDouble()),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasLiveFeed) "Live market data" else "Offline fallback data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Gain: ${if (netProfitLoss >= 0) "+" else ""}₹${String.format("%,.0f", netProfitLoss)} (${String.format("%.2f", profitPercentage)}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = gainColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Portfolio Allocation Donut Chart
        item {
            if (stocks.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "PORTFOLIO ALLOCATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = BluePrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Donut Chart
                            PortfolioDonutChart(
                                stocks = stocks,
                                totalValue = totalCurrentValue,
                                modifier = Modifier.size(140.dp)
                            )

                            // Legend
                            Column(
                                modifier = Modifier.padding(start = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                stocks.sortedByDescending { it.shares * it.currentPrice }.forEach { stock ->
                                    val stockValue = stock.shares * stock.currentPrice
                                    val percentage = if (totalCurrentValue > 0) (stockValue / totalCurrentValue) * 100 else 0.0
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(getStockColor(stock.symbol))
                                        )
                                        Column {
                                            Text(
                                                text = stock.symbol,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                            Text(
                                                text = "${String.format("%.1f", percentage)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSubtle,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Portfolio Statement Importer
        item {
            PortfolioImportCard(onImport = { uri -> viewModel.importPortfolioFile(uri) })
        }

        // Action Status Row
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectTab(2) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Family Shared Watchlist",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSubtle,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${watchlistItems.size} Active Tracking Items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (watchlistItems.isNotEmpty()) BluePrimary else TextDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = TextSubtle,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Family Decisions Chat Card
        item {
            val chatScrollState = rememberScrollState()
            LaunchedEffect(chatHistory.size) {
                chatScrollState.animateScrollTo(chatScrollState.maxValue)
            }

            if (showFullscreenChat) {
                Dialog(
                    onDismissRequest = { showFullscreenChat = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = BackgroundGray,
                        border = BorderStroke(2.dp, BluePrimary)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Titlebar / window controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BluePrimary)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Family Discussion",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                IconButton(
                                    onClick = { showFullscreenChat = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close chat",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Toolbar
                            var searchQuery by remember { mutableStateOf("") }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search messages...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = TextDark),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BluePrimary,
                                        unfocusedBorderColor = BorderColor,
                                        focusedContainerColor = Color(0xFFF8F9FC),
                                        unfocusedContainerColor = Color(0xFFF8F9FC)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Shared messages",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }

                            HorizontalDivider(color = BorderColor)

                            val filteredChat = if (searchQuery.isEmpty()) {
                                chatHistory
                            } else {
                                chatHistory.filter { 
                                    it.sender.contains(searchQuery, ignoreCase = true) || 
                                    it.message.contains(searchQuery, ignoreCase = true) 
                                }
                            }

                            val dialogScrollState = rememberScrollState()
                            LaunchedEffect(filteredChat.size) {
                                dialogScrollState.animateScrollTo(dialogScrollState.maxValue)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(dialogScrollState)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (filteredChat.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isNotEmpty()) "No messages matching '$searchQuery'" else "No messages matching requirements yet.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSubtle
                                        )
                                    }
                                } else {
                                    for (chat in filteredChat) {
                                        ChatBubbleItem(chat, watchlistItems, onSystemClick = { viewModel.selectTab(2) })
                                    }
                                }
                            }

                            HorizontalDivider(color = BorderColor)

                            Box(modifier = Modifier.background(Color.White)) {
                                ChatInputBar(
                                    onSend = { text, recStock -> 
                                        viewModel.sendChatMessage("Me", text, recStock)
                                    },
                                    stocksList = stocks,
                                    watchlistList = watchlistItems
                                )
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Family Decisions Thread",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        IconButton(
                            onClick = { showFullscreenChat = true },
                            modifier = Modifier.size(28.dp).testTag("open_chat_new_window_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Open full chat",
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    // Scrollable Chat area containing seeded & users messages
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(chatScrollState)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (chat in chatHistory) {
                            ChatBubbleItem(chat, watchlistItems, onSystemClick = { viewModel.selectTab(2) })
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    ChatInputBar(
                        onSend = { text, recStock -> viewModel.sendChatMessage("Me", text, recStock) },
                        stocksList = stocks,
                        watchlistList = watchlistItems
                    )
                }
            }
        }

        // Portfolio holdings breakdown
        item {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    text = "PORTFOLIO HOLDINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                )

                stocks.forEach { stock ->
                    val stockValue = stock.shares * stock.currentPrice
                    val totalCost = stock.shares * stock.avgPrice
                    val profitLoss = stockValue - totalCost
                    val plPercentage = if (totalCost > 0) (profitLoss / totalCost) * 100 else 0.0
                    val isProfit = profitLoss >= 0

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(getStockColor(stock.symbol).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stock.symbol.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = getStockColor(stock.symbol)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stock.symbol,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "${stock.shares} Shares @ ₹${String.format("%,.2f", stock.avgPrice)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSubtle
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${String.format("%,.2f", stockValue)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isProfit) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = if (isProfit) SoftGreen else SoftRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${String.format("%.2f", plPercentage)}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProfit) SoftGreen else SoftRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    chat: ChatMessage,
    watchlistItems: List<WatchlistItem>,
    onSystemClick: (() -> Unit)? = null
) {
    val isMe = chat.sender == "Me"
    val isSystem = chat.sender.startsWith("System")
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    if (isSystem) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            val alertBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
            val alertBorder = if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
            val alertText = if (isDark) Color(0xFFB0B0B0) else Color(0xFF616161)

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = alertBg,
                border = BorderStroke(1.dp, alertBorder),
                modifier = Modifier
                    .then(
                        if (onSystemClick != null) {
                            Modifier.clickable { onSystemClick() }
                        } else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = alertText,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = chat.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = alertText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            if (!isMe) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BluePrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.sender.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    ),
                    color = if (isMe) BluePrimary else if (isDark) Color(0xFF2C2C2C) else Color(0xFFF0F0F0),
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = chat.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isMe) Color.White else TextDark
                        )

                        chat.recommendedStockSymbol?.let { rec ->
                            val match = watchlistItems.find { it.symbol.uppercase() == rec.uppercase() }
                            if (match != null) {
                                Card(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) Color.White.copy(alpha = 0.15f) else if (isDark) Color(0xFF1E1E1E) else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "WATCH: ${match.symbol}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMe) Color.White else BluePrimary
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (match.isTriggered) SoftGreen else if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFFFFF3E0))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (match.isTriggered) "HIT" else "WAITING",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (match.isTriggered || isMe) Color.White else Color(0xFFE65100)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "₹${String.format("%,.2f", match.currentPrice)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMe) Color.White else TextDark
                                            )
                                            Text(
                                                text = "→ ₹${String.format("%,.2f", match.targetPrice)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isMe) Color.White.copy(alpha = 0.8f) else Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    text = formatChatTimestamp(chat.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSubtle,
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp),
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    onSend: (String, String?) -> Unit,
    stocksList: List<StockAsset>,
    watchlistList: List<WatchlistItem>
) {
    var rawText by remember { mutableStateOf("") }
    var expandedRecommendation by remember { mutableStateOf(false) }
    var selectedRecSymbol by remember { mutableStateOf<String?>(null) }
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Column(
        modifier = Modifier
            .background(if (isDark) Color(0xFF1A1C1E) else Color(0xFFF1F3F9))
            .padding(8.dp)
    ) {
        if (selectedRecSymbol != null) {
            Surface(
                color = BluePrimary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📎 Attached Stock: ${selectedRecSymbol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { selectedRecSymbol = null },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Attachment", tint = BluePrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { expandedRecommendation = !expandedRecommendation },
                modifier = Modifier.background(BluePrimary.copy(alpha = 0.1f), CircleShape).size(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Attach", tint = BluePrimary, modifier = Modifier.size(20.dp))
            }

            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it },
                placeholder = { 
                    Text(
                        text = "Message family...",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtle
                    ) 
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDark) Color(0xFF2C2C2C) else Color.White,
                    unfocusedContainerColor = if (isDark) Color(0xFF2C2C2C) else Color.White,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
            )

            FloatingActionButton(
                onClick = {
                    if (rawText.isNotBlank()) {
                        onSend(rawText, selectedRecSymbol)
                        rawText = ""
                        selectedRecSymbol = null
                    }
                },
                containerColor = BluePrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
            }
        }

        DropdownMenu(
            expanded = expandedRecommendation,
            onDismissRequest = { expandedRecommendation = false }
        ) {
            DropdownMenuItem(
                text = { Text("No attachment") },
                onClick = {
                    selectedRecSymbol = null
                    expandedRecommendation = false
                }
            )
            stocksList.forEach { stock ->
                DropdownMenuItem(
                    text = { Text("Link Asset: ${stock.symbol}") },
                    onClick = {
                        selectedRecSymbol = stock.symbol
                        expandedRecommendation = false
                    }
                )
            }
            watchlistList.forEach { item ->
                DropdownMenuItem(
                    text = { Text("Link Watch: ${item.symbol}") },
                    onClick = {
                        selectedRecSymbol = item.symbol
                        expandedRecommendation = false
                    }
                )
            }
        }
    }
}

