package com.smartspend.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    onRescanSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpendCategories by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showCreditCategories by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showTagsScreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showSpendCategories) {
        SpendCategoriesScreen(onBack = { showSpendCategories = false })
        return
    }
    if (showCreditCategories) {
        CreditCategoriesScreen(onBack = { showCreditCategories = false })
        return
    }
    if (showTagsScreen) {
        TagsScreen(onBack = { showTagsScreen = false })
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Preferences", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->


        LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Categorization",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column {
                            AccountMenuRow(
                                icon = Icons.Rounded.Category,
                                iconTint = MaterialTheme.colorScheme.onSurface,
                                text = "Spend Categories",
                                onClick = { showSpendCategories = true }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            AccountMenuRow(
                                icon = Icons.Rounded.CreditCard,
                                iconTint = MaterialTheme.colorScheme.onSurface,
                                text = "Credit Categories",
                                onClick = { showCreditCategories = true }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            AccountMenuRow(
                                icon = Icons.AutoMirrored.Rounded.Label,
                                iconTint = MaterialTheme.colorScheme.onSurface,
                                text = "Tags",
                                onClick = { showTagsScreen = true }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Data Sync",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        AccountMenuRow(
                            icon = Icons.Rounded.Sync,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            text = "Rescan SMS",
                            onClick = onRescanSms
                        )
                    }
                    Text(
                        text = "Manually trigger a deep scan of your SMS inbox to find missing transactions. This may take a few moments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
