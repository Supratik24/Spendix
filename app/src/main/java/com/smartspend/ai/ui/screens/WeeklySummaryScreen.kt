package com.smartspend.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.data.TransactionType
import com.smartspend.ai.ui.components.TransactionRow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    transactions: List<Transaction>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Current week starts on Sunday
    var currentWeekStart by remember { 
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)))
    }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    
    val currentWeekEnd = currentWeekStart.plusDays(6)
    
    val weekFormatter = DateTimeFormatter.ofPattern("MMM d")
    val dateRangeText = "${currentWeekStart.format(weekFormatter)} - ${currentWeekEnd.format(weekFormatter)}"
    
    // Filter transactions for this week
    val weeklyTransactions = remember(transactions, currentWeekStart) {
        transactions.filter { 
            val txDate = java.time.Instant.ofEpochMilli(it.occurredAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            it.type == TransactionType.DEBIT &&
            !txDate.isBefore(currentWeekStart) && 
            !txDate.isAfter(currentWeekEnd)
        }.sortedByDescending { it.occurredAt }
    }
    
    // Calculate daily totals (Sun = 0, Sat = 6)
    val dailyTotals = remember(weeklyTransactions) {
        val totals = FloatArray(7) { 0f }
        weeklyTransactions.forEach { tx ->
            val txDate = java.time.Instant.ofEpochMilli(tx.occurredAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val dayOfWeek = txDate.dayOfWeek
            val index = if (dayOfWeek == DayOfWeek.SUNDAY) 0 else dayOfWeek.value
            totals[index] += (tx.amountPaise / 100f)
        }
        totals
    }
    
    val displayedTransactions = remember(weeklyTransactions, selectedDayIndex) {
        if (selectedDayIndex == null) {
            weeklyTransactions
        } else {
            weeklyTransactions.filter { tx ->
                val txDate = java.time.Instant.ofEpochMilli(tx.occurredAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val dayOfWeek = txDate.dayOfWeek
                val index = if (dayOfWeek == DayOfWeek.SUNDAY) 0 else dayOfWeek.value
                index == selectedDayIndex
            }
        }
    }
    
    val maxAmount = dailyTotals.maxOrNull()?.takeIf { it > 0 } ?: 1f
    val totalSpent = dailyTotals.sum()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Weekly Summary") },
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
            // Date Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    currentWeekStart = currentWeekStart.minusWeeks(1) 
                    selectedDayIndex = null
                }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Week")
                }
                Text(
                    text = dateRangeText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { 
                    currentWeekStart = currentWeekStart.plusWeeks(1) 
                    selectedDayIndex = null
                }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Week")
                }
            }
            
            // Total Spent
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Spent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹%.2f".format(totalSpent),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bar Chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                val baseBarColor = MaterialTheme.colorScheme.primary
                val unselectedBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    days.forEachIndexed { index, day ->
                        val heightFraction = dailyTotals[index] / maxAmount
                        val isSelected = selectedDayIndex == null || selectedDayIndex == index
                        val barColor = if (isSelected) baseBarColor else unselectedBarColor
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedDayIndex = if (selectedDayIndex == index) null else index
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(0.6f),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(heightFraction.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(barColor)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (selectedDayIndex == null) "Transactions" else "${listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")[selectedDayIndex!!]} Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (displayedTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expenses", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedTransactions, key = { it.id }) { txn ->
                        TransactionRow(transaction = txn, onClick = { /* View details? */ })
                    }
                }
            }
        }
    }
}
