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
fun AlertsManagerTab(viewModel: DashboardViewModel) {
    val watchlistItems by viewModel.watchlistItems.collectAsStateWithLifecycle()
    val newsList by viewModel.stockNews.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- FAMILY SHARED WATCHLIST ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⭐ Family Shared Watchlist",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${watchlistItems.size} active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }

        if (watchlistItems.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Shared Watchlist Tickers",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add equities from Research to track target prices with the family.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSubtle,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        items(watchlistItems) { item ->
            val isDark = MaterialTheme.colorScheme.background.red < 0.5f
            val isTargetAbove = item.targetPrice >= item.currentPrice
            val percentToTarget = if (isTargetAbove) {
                if (item.targetPrice > 0) (item.currentPrice / item.targetPrice) * 100 else 0.0
            } else {
                if (item.currentPrice > 0) (item.targetPrice / item.currentPrice) * 100 else 0.0
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (item.isTriggered) {
                        if (isDark) Color(0xFF1B3E22) else Color(0xFFE8F5E9)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(1.dp, if (item.isTriggered) SoftGreen else BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = item.symbol, fontWeight = FontWeight.Bold, color = BluePrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                val pillBg = if (item.isTriggered) {
                                    if (isDark) Color(0xFF1E4624) else Color(0xFFC8E6C9)
                                } else {
                                    if (isDark) Color(0xFF4A3416) else Color(0xFFFFF3E0)
                                }
                                val pillText = if (item.isTriggered) {
                                    if (isDark) Color(0xFFC8E6C9) else SoftGreen
                                } else {
                                    if (isDark) Color(0xFFFFE0B2) else Color(0xFFE65100)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(pillBg)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (item.isTriggered) "TRIGGERED" else "ACTIVE WATCH",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pillText
                                    )
                                }
                            }
                            Text(text = item.name, style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    viewModel.sendChatMessage(
                                        sender = "System",
                                        messageText = "[Watchlist Update] ${item.symbol} current price is ₹${String.format("%,.2f", item.currentPrice)} and target watch goal is ₹${String.format("%,.2f", item.targetPrice)}.",
                                        recommendedSymbol = item.symbol
                                    )
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share item", tint = BluePrimary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.deleteWatchlistItem(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = TextSubtle, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current Price", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                            Text(
                                text = "₹" + String.format("%,.2f", item.currentPrice),
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Target Alert Price", style = MaterialTheme.typography.labelSmall, color = TextSubtle)
                            Text(
                                text = "₹" + String.format("%,.2f", item.targetPrice),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(BackgroundGray)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = (percentToTarget / 100.0).coerceIn(0.01..1.0).toFloat())
                                .clip(CircleShape)
                                .background(if (item.isTriggered) SoftGreen else Color(0xFFFF9800))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (item.isTriggered) "Milestone completed! 🎉" else "Watch progress: ${String.format("%.1f", percentToTarget.coerceAtMost(100.0))}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSubtle
                        )
                    }
                }
            }
        }

        // --- WATCHLIST NEWS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Family Asset News",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BluePrimary.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "From saved symbols",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }
            }
        }

        if (newsList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No recent news for saved symbols yet.", color = TextSubtle)
                    }
                }
            }
        }

        items(newsList) { news ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BluePrimary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = news.symbol,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = news.source,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSubtle,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (news.sentiment == "Bullish") {
                                        if (isDark) Color(0xFF1B3E22) else Color(0xFFE8F5E9)
                                    } else {
                                        if (isDark) Color(0xFF2C2C2C) else Color(0xFFECEFF1)
                                    }
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = news.sentiment,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (news.sentiment == "Bullish") SoftGreen else TextSubtle
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = news.title,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = news.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = news.timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSubtle
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "News source available",
                            tint = SoftGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

