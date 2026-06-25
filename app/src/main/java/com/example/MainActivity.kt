package com.example

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
import com.example.ui.screens.*
import com.example.ui.components.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            val isDark = when (appTheme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = isDark) {
                MainContainer(viewModel)
            }
        }
    }
}

@Composable
fun MainContainer(viewModel: DashboardViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val userFeedback by viewModel.userFeedback.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentGroup by viewModel.currentGroup.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(userFeedback) {
        userFeedback?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    if (currentUser == null) {
        AuthenticationScreen(viewModel)
    } else if (currentGroup == null) {
        GroupSelectionScreen(viewModel)
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("app_main_scaffold"),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomNavigationBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                FamilyBrandHeader(viewModel)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (slideInHorizontally { fullWidth -> direction * fullWidth / 4 } + fadeIn(
                                animationSpec = tween(280)
                            )).togetherWith(
                                slideOutHorizontally { fullWidth -> -direction * fullWidth / 4 } + fadeOut(
                                    animationSpec = tween(200)
                                )
                            )
                        },
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            0 -> HomeDashboardTab(viewModel)
                            1 -> ResearchGraphsTab(viewModel)
                            2 -> AlertsManagerTab(viewModel)
                            3 -> SettingsAndEducationTab(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FamilyBrandHeader(viewModel: DashboardViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentGroup by viewModel.currentGroup.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = BorderColor)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = (currentGroup?.name ?: "").uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSubtle,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentUser?.fullName ?: "Shared Wealth",
                style = MaterialTheme.typography.titleLarge,
                color = BluePrimary,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            currentGroup?.let { group ->
                Text(
                    text = "Invite Code: ${group.inviteCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy((-10).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = if (!currentUser?.fullName.isNullOrBlank()) {
                currentUser!!.fullName.split(" ").filter { it.isNotBlank() }.take(2).map { it.take(1) }.joinToString("").uppercase()
            } else {
                "JD"
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentTab: Int, onTabSelected: (Int) -> Unit) {
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = TextSubtle,
        unselectedTextColor = TextSubtle
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .shadow(elevation = 12.dp, spotColor = Color.Black.copy(alpha = 0.08f))
            .border(1.dp, BorderColor)
    ) {
        data class NavItem(val index: Int, val icon: @Composable () -> Unit, val label: String)
        val items = listOf(
            NavItem(0, { Icon(if (currentTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") }, "Home"),
            NavItem(1, { Icon(if (currentTab == 1) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "Research") }, "Research"),
            NavItem(2, { Icon(if (currentTab == 2) Icons.Filled.Star else Icons.Outlined.Star, contentDescription = "Watchlist") }, "Watchlist"),
            NavItem(3, { Icon(if (currentTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") }, "Settings")
        )

        items.forEach { navItem ->
            NavigationBarItem(
                selected = currentTab == navItem.index,
                onClick = { onTabSelected(navItem.index) },
                icon = navItem.icon,
                label = {
                    Text(
                        text = navItem.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (currentTab == navItem.index) FontWeight.Bold else FontWeight.Medium
                        )
                    )
                },
                colors = navItemColors
            )
        }
    }
}

