package com.smartspend.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.data.parseSplitInfo
import com.smartspend.ai.ui.components.TransactionRow
import com.smartspend.ai.ui.components.formatMoney
import com.smartspend.ai.ui.model.SpendUiState
import com.smartspend.ai.ui.viewmodel.SpendViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitsScreen(
    state: SpendUiState,
    viewModel: SpendViewModel,
    modifier: Modifier = Modifier
) {
    val splitTxns = state.transactions.filter { !it.splitInfo.isNullOrBlank() }
    
    // Calculate You Get (people owe you for your DEBITs) and You Pay (you owe people for their CREDITs, though not strictly handled yet)
    // Assuming for now, DEBIT splits mean friends owe you, CREDIT splits mean you owe friends.
    val youGet = splitTxns.filter { it.type == com.smartspend.ai.data.TransactionType.DEBIT }
        .sumOf { txn -> parseSplitInfo(txn.splitInfo).sumOf { it.amountPaise } }
        
    val youPay = splitTxns.filter { it.type == com.smartspend.ai.data.TransactionType.CREDIT }
        .sumOf { txn -> parseSplitInfo(txn.splitInfo).sumOf { it.amountPaise } }

    Scaffold(
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // You get card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("You get", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatMoney(youGet), style = MaterialTheme.typography.titleLarge, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            Icon(Icons.Rounded.CallReceived, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp).padding(start = 4.dp))
                        }
                    }
                }
                // You pay card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("You pay", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatMoney(youPay), style = MaterialTheme.typography.titleLarge, color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                            Icon(Icons.Rounded.CallMade, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(16.dp).padding(start = 4.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Split Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (splitTxns.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active splits", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(splitTxns, key = { it.id }) { txn ->
                        Card(
                            onClick = { viewModel.selectTransaction(txn) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                TransactionRow(
                                    transaction = txn,
                                    onClick = { viewModel.selectTransaction(txn) }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Shared with:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(
                                        onClick = { viewModel.saveSplitInfo(txn.id, null) },
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.height(24.dp)
                                    ) {
                                        Text("Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                val splits = parseSplitInfo(txn.splitInfo)
                                splits.forEach { split ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(split.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(split.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text(formatMoney(split.amountPaise), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (txn.type == com.smartspend.ai.data.TransactionType.DEBIT) Color(0xFF4CAF50) else Color(0xFFF44336))
                                        IconButton(
                                            onClick = {
                                                val newSplits = splits.filter { it.name != split.name }
                                                val encoded = newSplits.joinToString(",") { "${it.name}:${it.amountPaise}" }
                                                viewModel.saveSplitInfo(txn.id, encoded.ifBlank { null })
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
