package com.smartspend.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    transactions: List<com.smartspend.ai.data.Transaction> = emptyList(),
    userName: String?,
    userEmail: String?,
    isDarkMode: Boolean?,
    onToggleTheme: () -> Unit,
    onNameChanged: (String) -> Unit = {},
    onRescanSms: () -> Unit = {},
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditNameDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showPreferencesScreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showWeeklySummaryScreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showExportDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAboutScreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var notificationsEnabled by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            context.getSharedPreferences("spendix_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("enable_notifications", true)
        )
    }

    if (showPreferencesScreen) {
        PreferencesScreen(
            onBack = { showPreferencesScreen = false },
            onRescanSms = onRescanSms
        )
        return
    }

    if (showWeeklySummaryScreen) {
        WeeklySummaryScreen(
            transactions = transactions,
            onBack = { showWeeklySummaryScreen = false }
        )
        return
    }

    if (showExportDialog) {
        ExportStatementDialog(
            transactions = transactions,
            onDismiss = { showExportDialog = false }
        )
        return
    }

    if (showAboutScreen) {
        AboutScreen(onBack = { showAboutScreen = false })
        return
    }
    
    if (showEditNameDialog) {
        var tempName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(userName ?: "") }
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onNameChanged(tempName)
                    showEditNameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Account", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditNameDialog = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = userName ?: "User",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Contact Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = userEmail ?: "No email provided",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Expense Tracking Section
            Text(
                text = "Expense tracking",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    AccountMenuRow(
                        icon = Icons.Rounded.Settings,
                        iconTint = MaterialTheme.colorScheme.primary,
                        text = "Preferences",
                        onClick = { showPreferencesScreen = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    AccountMenuRow(
                        icon = Icons.Rounded.BarChart,
                        iconTint = MaterialTheme.colorScheme.primary,
                        text = "Weekly summary",
                        onClick = { showWeeklySummaryScreen = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    AccountMenuRow(
                        icon = Icons.Rounded.PictureAsPdf,
                        iconTint = MaterialTheme.colorScheme.primary,
                        text = "Export statement",
                        onClick = { showExportDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            
            Spacer(modifier = Modifier.height(8.dp))

            // Notifications Section
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    AccountMenuRow(
                        icon = Icons.Rounded.Notifications,
                        iconTint = MaterialTheme.colorScheme.primary,
                        text = "Transaction Alerts",
                        trailing = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = {
                                    notificationsEnabled = it
                                    context.getSharedPreferences("spendix_prefs", android.content.Context.MODE_PRIVATE)
                                        .edit().putBoolean("enable_notifications", it).apply()
                                }
                            )
                        },
                        onClick = {
                            notificationsEnabled = !notificationsEnabled
                            context.getSharedPreferences("spendix_prefs", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("enable_notifications", notificationsEnabled).apply()
                        }
                    )
                }
            }

            // 4. General Section
            Text(
                text = "General",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column {
                    AccountMenuRow(
                        icon = if (isDarkMode == true) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        text = "Theme",
                        trailing = {
                            Switch(
                                checked = isDarkMode == true,
                                onCheckedChange = { onToggleTheme() }
                            )
                        },
                        onClick = onToggleTheme
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    AccountMenuRow(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        iconTint = MaterialTheme.colorScheme.error,
                        text = "Logout",
                        textColor = MaterialTheme.colorScheme.error,
                        onClick = onLogout
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    AccountMenuRow(
                        icon = Icons.Rounded.Info,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = "About Spendix",
                        onClick = { showAboutScreen = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AccountMenuRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    text: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


