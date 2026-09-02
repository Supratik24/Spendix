package com.smartspend.ai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("About Spendix") },
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
        ) {
            AboutRow(
                title = "Website",
                icon = Icons.Rounded.Language,
                onClick = {
                    dialogTitle = "Website"
                    dialogText = "Visit our official website for the latest updates, features, and community guides on how to make the most of Spendix.\n\nhttps://www.spendix.app"
                }
            )
            
            AboutRow(
                title = "Terms of Use",
                icon = Icons.Rounded.Gavel,
                onClick = {
                    dialogTitle = "Terms of Use"
                    dialogText = "1. ACCEPTANCE OF TERMS\nBy accessing and using Spendix, you accept and agree to be bound by the terms and provision of this agreement.\n\n2. PRIVACY AND DATA SECURITY\nYour use of Spendix is subject to our Privacy Policy. We do not transmit your SMS data to external servers.\n\n3. USER RESPONSIBILITIES\nYou are responsible for maintaining the confidentiality of your account information."
                }
            )
            
            AboutRow(
                title = "Privacy Policy",
                icon = Icons.Rounded.Security,
                onClick = {
                    dialogTitle = "Privacy Policy"
                    dialogText = "1. INFORMATION WE COLLECT\nSpendix operates locally on your device. We use an on-device AI engine to parse transactional SMS messages. We do not collect, store, or transmit your financial data to our servers.\n\n2. HOW WE USE YOUR INFORMATION\nThe parsed information is used exclusively to display your expenses, generate summaries, and provide insights within the app.\n\n3. DATA SECURITY\nAll data is stored securely in your device's protected storage."
                }
            )
            
            AboutRow(
                title = "Grievance Redressal Policy",
                icon = Icons.Rounded.Policy,
                onClick = {
                    dialogTitle = "Grievance Redressal Policy"
                    dialogText = "If you have any complaints, disputes, or issues with our app, please contact our Grievance Officer.\n\nEmail: grievances@spendix.app\nResponse Time: We aim to acknowledge all complaints within 48 hours and resolve them within 14 business days."
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Spendix v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 32.dp)
            )
        }

        if (dialogTitle != null) {
            AlertDialog(
                onDismissRequest = { dialogTitle = null },
                title = { Text(dialogTitle!!) },
                text = {
                    Text(
                        text = dialogText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    TextButton(onClick = { dialogTitle = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun AboutRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = "View",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
}
