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
fun SettingsAndEducationTab(viewModel: DashboardViewModel) {
    val stocks by viewModel.stockAssets.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentGroup by viewModel.currentGroup.collectAsStateWithLifecycle()



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Account & Profile",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentUser?.fullName ?: "Unknown User",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Username: @${currentUser?.username}",
                            fontSize = 12.sp,
                            color = TextSubtle
                        )
                        currentGroup?.let { g ->
                            Text(
                                text = "Family group: ${g.name}",
                                fontSize = 12.sp,
                                color = SoftGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed.copy(alpha = 0.1f), contentColor = SoftRed),
                        border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.testTag("settings_logout_btn")
                    ) {
                        Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        val groupMembers by viewModel.groupMembers.collectAsStateWithLifecycle()
        if (currentGroup != null) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Family Members",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BluePrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(SoftGreen.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Invite ${currentGroup?.inviteCode}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftGreen
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Members can record portfolio activity, maintain alerts, and view shared research notes on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtle,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    groupMembers.forEachIndexed { index, member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BluePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.fullName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = BluePrimary,
                                    fontSize = 14.sp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = member.fullName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    if (member.username == currentUser?.username) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(BluePrimary.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("You", fontSize = 9.sp, color = BluePrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "@${member.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSubtle
                                )
                            }

                            if (currentUser?.username == currentGroup?.ownerUsername && member.username != currentUser?.username) {
                                IconButton(
                                    onClick = { viewModel.deleteMember(member.username) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Member",
                                        tint = SoftRed.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (index < groupMembers.lastIndex) {
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), modifier = Modifier.padding(start = 46.dp))
                        }
                    }
                }
            }
        }

        val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Choose light mode, dark mode, or your device system theme.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtle
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        "LIGHT" to "Light",
                        "DARK" to "Dark",
                        "SYSTEM" to "Device"
                    )
                    themes.forEach { (key, label) ->
                        val isSelected = appTheme == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BluePrimary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.updateTheme(key) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextDark
                            )
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Portfolio Import",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Import an exported broker statement, such as Zerodha Kite, Groww, or Upstox CSV/XLSX. Import replaces this family group's current holdings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtle
                )
                Spacer(modifier = Modifier.height(14.dp))

                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { viewModel.importPortfolioFile(it) }
                }

                Button(
                    onClick = { filePickerLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select CSV/XLSX Statement", fontWeight = FontWeight.Bold)
                }
            }
        }



        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFFFD1CF))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SoftRed.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = SoftRed, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "API Key Handling",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "For local development, keep GEMINI_API_KEY and STOCK_INDIAN_API_KEY in the ignored .env file. Do not commit real keys. For production, route market and AI calls through a backend because keys packaged in an APK can be extracted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtle,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Deployment Notes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                val notes = listOf(
                    "Shared database" to "Room stores data on the current device. To sync across family devices, add a backend or managed database such as Firebase Firestore or Supabase with proper auth rules.",
                    "Backend proxy" to "Use a small backend service to call Gemini and IndianAPI with server-held keys. The Android app should request only the data it needs from that backend.",
                    "Private distribution" to "For family testing, share debug or release builds privately through a trusted channel or Firebase App Distribution. Use Play Console when you need managed releases and updates."
                )
                notes.forEachIndexed { index, (title, body) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(BluePrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                            Text(
                                text = body,
                                fontSize = 11.sp,
                                color = TextSubtle,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    if (index < notes.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

