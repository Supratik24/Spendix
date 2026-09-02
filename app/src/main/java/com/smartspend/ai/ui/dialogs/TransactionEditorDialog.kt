package com.smartspend.ai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartspend.ai.data.Category
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.data.TransactionType
import com.smartspend.ai.ui.components.formatDate
import com.smartspend.ai.ui.components.formatMoney
import com.smartspend.ai.ui.theme.ExpenseRed
import com.smartspend.ai.ui.theme.IncomeGreen

@Composable
fun TransactionEditorDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onSaveCategory: (Category, String?) -> Unit,
    onDelete: () -> Unit
) {
    var category by remember { mutableStateOf(transaction.category) }
    var customCategory by remember { mutableStateOf(transaction.customCategory) }
    val isCredit = transaction.type == TransactionType.CREDIT

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = transaction.merchant ?: "Transaction Details",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "${if (isCredit) "Credit" else "Debit"}: ${formatMoney(transaction.amountPaise)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) IncomeGreen else ExpenseRed
                )
                Text(
                    text = "Date: ${formatDate(transaction.occurredAt)} · Sender: ${transaction.sender ?: "Manual"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (transaction.sender != null) {
                    Text(
                        text = "Original SMS content is not stored for your privacy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                CategoryDropdown(
                    selected = category,
                    selectedCustom = customCategory,
                    onSelect = { cat, custom -> category = cat; customCategory = custom }
                )
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Delete transaction")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSaveCategory(category, customCategory) }) {
                Text("Save category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
