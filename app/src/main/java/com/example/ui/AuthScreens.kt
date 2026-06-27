package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

@Composable
fun AuthenticationScreen(viewModel: DashboardViewModel) {
    var isLogin by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }

    val isDarkTheme = MaterialTheme.colorScheme.background.red < 0.5f
    val gradientStart = if (isDarkTheme) Color(0xFF1E2836) else Color(0xFFE8EFFC)
    val gradientEnd = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        gradientStart,
                        gradientEnd
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Branding Icon Section
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(width = 1.dp, color = BorderColor, shape = CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App lock",
                    tint = BluePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FAMILY CO-INVEST",
                    style = MaterialTheme.typography.labelSmall,
                    color = BluePrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Shared Family Portfolio",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextDark,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Track family holdings, watchlists, alerts, and research notes in one place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp)
                )
            }

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().testTag("auth_form_card")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern Tab Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabSelectionItem(
                            title = "Sign In",
                            isSelected = isLogin,
                            onClick = {
                                isLogin = true
                                // clear inputs on tab swap
                                username = ""
                                passcode = ""
                                fullName = ""
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TabSelectionItem(
                            title = "Register",
                            isSelected = !isLogin,
                            onClick = {
                                isLogin = false
                                username = ""
                                passcode = ""
                                fullName = ""
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!isLogin) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Your Full Name (e.g. Mohit Sharma)") },
                            leadingIcon = { Icon(Icons.Default.Face, contentDescription = null, tint = TextSubtle) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary),
                            modifier = Modifier.fillMaxWidth().testTag("auth_fullName_input")
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Select Username (e.g. mohit)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSubtle) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth().testTag("auth_username_input")
                    )

                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("Passcode (4+ digits)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSubtle) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth().testTag("auth_passcode_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (isLogin) {
                                viewModel.loginUser(username, passcode)
                            } else {
                                viewModel.registerNewUser(username, fullName, passcode)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.fillMaxWidth().testTag("auth_submit_btn")
                    ) {
                        Text(
                            text = if (isLogin) "Sign In" else "Create Account",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Quick access demo account option
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    }
                ),
                border = BorderStroke(1.dp, if (isDarkTheme) BluePrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Demo Account",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = BluePrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Testing the app? Use the Sharma demo account with sample family holdings, watchlists, alerts, and messages.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.loginUser("sharma", "1234")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, BluePrimary),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.fillMaxWidth().testTag("quick_sandbox_button")
                    ) {
                        Text("Use Demo Account", color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupSelectionScreen(viewModel: DashboardViewModel) {
    var groupName by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundGray)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(width = 1.dp, color = BorderColor, shape = CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Group Association",
                    tint = SoftGreen,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "FAMILY GROUP",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Link to a Family Circle",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextDark,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Logged in as @${currentUser?.username}. Create a family group or join one with an invite code.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSubtle,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Card 1: Create a family group
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Option A: Create a Family Group",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Text(
                        text = "Create a group and share the invite code with family members who should access the portfolio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtle,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Family Circle Name (e.g., Sharma Family)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary),
                        modifier = Modifier.fillMaxWidth().testTag("create_group_name_input")
                    )
                    Button(
                        onClick = {
                            viewModel.createFamilyGroup(groupName)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.fillMaxWidth().testTag("create_group_btn")
                    ) {
                        Text("Create Family Group", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 2: Join a family group
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Option B: Join a Family Group",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen
                    )
                    Text(
                        text = "Enter an invite code from another family member to access their shared portfolio and discussion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSubtle,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = { Text("Invitation Code (e.g., SHA-1234F)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoftGreen),
                        modifier = Modifier.fillMaxWidth().testTag("join_group_code_input")
                    )
                    Button(
                        onClick = {
                            viewModel.joinFamilyGroup(inviteCode)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.fillMaxWidth().testTag("join_group_btn")
                    ) {
                        Text("Join Family Group", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sign out action
            TextButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("← Log out from @${currentUser?.username}", color = SoftRed, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TabSelectionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextDark else TextSubtle
        )
    }
}
