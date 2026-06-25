package com.example.ui.components
import android.net.Uri
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

// HELPER COMPOSABLES & UTILITIES
// ============================================================

/**
 * Animated donut chart showing portfolio allocation by stock.
 */
@Composable
fun PortfolioDonutChart(
    stocks: List<StockAsset>,
    totalValue: Double,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(stocks) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val sortedStocks = stocks.sortedByDescending { it.shares * it.currentPrice }

    Canvas(modifier = modifier) {
        val strokeWidth = 28.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f
        sortedStocks.forEachIndexed { index, stock ->
            val value = stock.shares * stock.currentPrice
            val sweepAngle = if (totalValue > 0) (value / totalValue * 360f).toFloat() else 0f
            val animatedSweep = sweepAngle * animationProgress.value

            drawArc(
                color = getStockColorStatic(stock.symbol),
                startAngle = startAngle,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += animatedSweep
        }
    }
}

/**
 * Shimmer loading placeholder line — replaces spinners for a premium feel.
 */
@Composable
fun ShimmerLine(widthFraction: Float = 1f) {
    val shimmerColors = listOf(
        BorderColor.copy(alpha = 0.2f),
        BorderColor.copy(alpha = 0.5f),
        BorderColor.copy(alpha = 0.2f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(brush)
    )
}

/**
 * Format a timestamp into a human-readable relative time string.
 */
fun formatChatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        diff < TimeUnit.DAYS.toMillis(2) -> {
            "Yesterday " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        }
        else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Returns a consistent color for a stock symbol (Composable version).
 */
@Composable
fun getStockColor(symbol: String): Color {
    return getStockColorStatic(symbol)
}

/**
 * Returns a consistent color for a stock symbol (non-composable version for Canvas).
 */
fun getStockColorStatic(symbol: String): Color {
    val colorPalette = listOf(
        Color(0xFF2196F3),  // Blue
        Color(0xFF4CAF50),  // Green
        Color(0xFFFF9800),  // Orange
        Color(0xFFE91E63),  // Pink
        Color(0xFF9C27B0),  // Purple
        Color(0xFF00BCD4),  // Cyan
        Color(0xFFFF5722),  // Deep Orange
        Color(0xFF3F51B5),  // Indigo
        Color(0xFF009688),  // Teal
        Color(0xFFFFC107),  // Amber
    )
    val index = symbol.hashCode().let { if (it < 0) -it else it } % colorPalette.size
    return colorPalette[index]
}

/**
 * Returns a sector icon for a stock based on its symbol.
 */
@Composable
fun StockSectorIcon(symbol: String, modifier: Modifier = Modifier) {
    val (icon, tint) = when (symbol.uppercase()) {
        "HDFCBANK", "SBIN", "ICICIBANK", "KOTAKBANK", "AXISBANK" -> Icons.Filled.AccountBalance to Color(0xFF1565C0)
        "TCS", "INFY", "WIPRO", "HCLTECH", "TECHM" -> Icons.Filled.Computer to Color(0xFF00897B)
        "RELIANCE", "ONGC", "IOC", "BPCL" -> Icons.Filled.Bolt to Color(0xFFE65100)
        "TATAMOTORS", "MARUTI", "BAJAJ-AUTO", "HEROMOTOCO", "M&M" -> Icons.Filled.DirectionsCar to Color(0xFF6A1B9A)
        else -> Icons.Filled.ShowChart to Color(0xFF546E7A)
    }
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Sector icon",
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Reusable card for importing CSV or XLSX statements.
 */
@Composable
fun PortfolioImportCard(onImport: (Uri) -> Unit) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onImport(it) }
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (MaterialTheme.colorScheme.background.red < 0.5f) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                Color(0xFFE8F0FE)
            }
        ),
        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().testTag("import_portfolio_dashboard_card")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BluePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Upload Statement",
                    tint = BluePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "IMPORT BROKER STATEMENT (CSV/XLSX)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = BluePrimary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Parse statement (e.g. Zerodha Kite, Groww, Upstox) and load holdings instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.8f)
                )
            }
            Button(
                onClick = { filePickerLauncher.launch("*/*") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BluePrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(100.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Import File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
