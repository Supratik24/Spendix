package com.smartspend.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.ui.components.*
import com.smartspend.ai.ui.dialogs.ManualTransactionDialog
import com.smartspend.ai.ui.model.SpendUiState
import com.smartspend.ai.ui.viewmodel.SpendViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    state: SpendUiState,
    viewModel: SpendViewModel,
    onRequestSmsPermission: () -> Unit,
    userName: String?,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualEntryDialog by remember { mutableStateOf(false) }

    val monthLabel = state.selectedMonth?.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault())) ?: "All Time"
    val isFiltered = state.searchQuery.isNotBlank() || state.selectedCategory != null || state.highlightedCategory != null

    val context = androidx.compose.ui.platform.LocalContext.current
    val hasSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Header(
                userName = userName,
                onOpenAccount = onOpenAccount
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month navigation & All Time carousel
            item {
                MonthSelector(
                    selectedMonth = state.selectedMonth,
                    onPreviousMonth = viewModel::selectPreviousMonth,
                    onNextMonth = viewModel::selectNextMonth,
                    onCurrentMonth = viewModel::selectCurrentMonth,
                    onAllTime = viewModel::selectAllTime
                )
            }

            // Month balance overview
            item {
                BalanceCard(
                    balancePaise = state.balancePaise,
                    spentPaise = state.spentPaise,
                    earnedPaise = state.earnedPaise,
                    monthLabel = monthLabel
                )
            }

            // SMS Import status card
            if (!hasSmsPermission || state.importStatus is com.smartspend.ai.ui.model.ImportStatus.Importing) {
                item {
                    ImportCard(
                        status = state.importStatus,
                        onEnable = onRequestSmsPermission
                    )
                }
            }

            // Category Donut Breakdown & Insight
            if (state.expenses.isNotEmpty()) {
                item {
                    SpendingBreakdown(
                        spendingByCategory = state.spendingByCategory,
                        totalSpentPaise = state.totalCategorizedSpend,
                        highlightedCategory = state.highlightedCategory,
                        onCategoryClick = viewModel::setHighlightedCategory,
                        onClearHighlight = { viewModel.setHighlightedCategory(null) }
                    )
                }
                item {
                    InsightCard(spendingByCategory = state.spendingByCategory)
                }
            }

            // Search and Category filter
            item {
                SearchAndFilterBar(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = viewModel::setSelectedCategory
                )
            }


            // Activity list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFiltered) "Matching Transactions" else "Activity in $monthLabel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.filteredTransactions.size} total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                Button(
                    onClick = { showManualEntryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add manual transaction")
                }
            }

            // List or Empty state
            if (state.filteredTransactions.isEmpty()) {
                item {
                    EmptyTransactions(isFiltered = isFiltered)
                }
            } else {
                items(state.filteredTransactions, key = Transaction::id) { transaction ->
                    Box(modifier = Modifier.animateItem()) {
                        TransactionRow(
                            transaction = transaction,
                            onClick = { viewModel.selectTransaction(transaction) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showManualEntryDialog) {
        ManualTransactionDialog(
            onDismiss = { showManualEntryDialog = false },
            onSave = { amount, type, category, merchant, customCategory ->
                viewModel.addManual(amount, type, category, merchant, customCategory)
                showManualEntryDialog = false
            }
        )
    }
}
