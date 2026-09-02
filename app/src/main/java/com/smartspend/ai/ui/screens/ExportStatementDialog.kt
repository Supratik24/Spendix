package com.smartspend.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.utils.ExportUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportStatementDialog(
    transactions: List<Transaction>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedMonthIndex by remember { mutableStateOf(0) }

    // Generate list of last 12 months
    val monthList = mutableListOf<String>()
    val monthRanges = mutableListOf<Pair<Long, Long>>()
    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    for (i in 0 until 12) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -i)
        
        // Month start
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        monthList.add(dateFormat.format(cal.time))

        // Month end
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val end = cal.timeInMillis

        monthRanges.add(Pair(start, end))
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Statement") },
        text = {
            Column {
                Text("Select a month to download as PDF:")
                Spacer(modifier = Modifier.height(16.dp))
                
                var expanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = monthList[selectedMonthIndex],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        monthList.forEachIndexed { index, monthStr ->
                            DropdownMenuItem(
                                text = { Text(monthStr) },
                                onClick = {
                                    selectedMonthIndex = index
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val range = monthRanges[selectedMonthIndex]
                    val filtered = transactions.filter { it.occurredAt in range.first..range.second }
                        .sortedBy { it.occurredAt }
                        
                    if (filtered.isEmpty()) {
                        android.widget.Toast.makeText(context, "No transactions found for this month.", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        val title = "Spendix Statement - ${monthList[selectedMonthIndex]}"
                        ExportUtils.exportTransactionsToPdf(context, filtered, title)
                        onDismiss()
                    }
                }
            ) {
                Text("Export PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
