package com.smartspend.ai.ui.model

import com.smartspend.ai.data.Category
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.data.TransactionType
import com.smartspend.ai.data.SplitEntry
import com.smartspend.ai.data.parseSplitInfo
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

enum class MainTab { TRANSACTIONS, ANALYTICS, SPLITS }

data class SpendUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedMonth: YearMonth? = YearMonth.now(), // null = All Time
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val highlightedCategory: Category? = null, // for interactive pie chart focus
    val importStatus: ImportStatus = ImportStatus.Idle,
    val isDarkMode: Boolean? = null,
    val selectedTransaction: Transaction? = null,
    val currentTab: MainTab = MainTab.TRANSACTIONS
) {
    val availableMonths: List<YearMonth>
        get() {
            val zone = ZoneId.systemDefault()
            val months = transactions.map { t ->
                val date = Instant.ofEpochMilli(t.occurredAt).atZone(zone).toLocalDate()
                YearMonth.from(date)
            }.distinct().toMutableList()
            val current = YearMonth.now()
            if (!months.contains(current)) {
                months.add(0, current)
            }
            return months.sortedDescending()
        }

    val monthTransactions: List<Transaction>
        get() {
            val month = selectedMonth ?: return transactions
            val zone = ZoneId.systemDefault()
            return transactions.filter { t ->
                val date = Instant.ofEpochMilli(t.occurredAt).atZone(zone).toLocalDate()
                YearMonth.from(date) == month
            }
        }

    val filteredTransactions: List<Transaction>
        get() {
            return monthTransactions.filter { t ->
                val matchesQuery = if (searchQuery.isBlank()) true else {
                    val query = searchQuery.trim().lowercase()
                    (t.merchant?.lowercase()?.contains(query) == true) ||
                            t.category.name.lowercase().contains(query) ||
                            (t.sender?.lowercase()?.contains(query) == true)
                }
                val matchesCategory = (selectedCategory == null || t.category == selectedCategory) &&
                        (highlightedCategory == null || t.category == highlightedCategory)
                matchesQuery && matchesCategory
            }
        }

    val expenses: List<Transaction>
        get() = filteredTransactions.filter { it.type == TransactionType.DEBIT }

    val income: List<Transaction>
        get() = filteredTransactions.filter { it.type == TransactionType.CREDIT }

    // Bank Math (excludes Credit Cards since actual money isn't deducted yet)
    val spentPaise: Long
        get() = monthTransactions.filter { it.type == TransactionType.DEBIT && !it.isCreditCard }.sumOf { txn ->
            val splitTotal = parseSplitInfo(txn.splitInfo).sumOf { it.amountPaise }
            txn.amountPaise - splitTotal
        }

    val earnedPaise: Long
        get() = monthTransactions.filter { it.type == TransactionType.CREDIT && !it.isCreditCard }.sumOf { txn ->
            val splitTotal = parseSplitInfo(txn.splitInfo).sumOf { it.amountPaise }
            txn.amountPaise - splitTotal
        }

    val balancePaise: Long
        get() = earnedPaise - spentPaise

    // Expense Math (includes Credit Cards for pie charts and analytics)
    val totalCategorizedSpend: Long
        get() = monthTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { txn ->
            val splitTotal = parseSplitInfo(txn.splitInfo).sumOf { it.amountPaise }
            txn.amountPaise - splitTotal
        }

    val spendingByCategory: List<Pair<Category, Long>>
        get() = monthTransactions.filter { it.type == TransactionType.DEBIT }
            .groupBy { it.category }
            .mapValues { entry -> 
                entry.value.sumOf { txn ->
                    val splitTotal = parseSplitInfo(txn.splitInfo).sumOf { it.amountPaise }
                    txn.amountPaise - splitTotal
                }
            }
            .toList()
            .sortedByDescending { it.second }
}
