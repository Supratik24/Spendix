package com.smartspend.ai.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartspend.ai.data.Category
import com.smartspend.ai.data.TransactionType
import com.smartspend.ai.ui.components.label

@Composable
fun ManualTransactionDialog(
    onDismiss: () -> Unit,
    onSave: (amount: String, type: TransactionType, category: Category, merchant: String, customCategory: String?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.DEBIT) }
    var category by remember { mutableStateOf(Category.OTHER) }
    var customCategory by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant or note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TypeSelector(selected = type, onSelect = { type = it })
                CategoryDropdown(selected = category, selectedCustom = customCategory, isCredit = type == TransactionType.CREDIT, onSelect = { cat, custom -> category = cat; customCategory = custom })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(amount, type, category, merchant, customCategory) },
                enabled = amount.trim().replace(",", "").toBigDecimalOrNull()?.signum() == 1
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TypeSelector(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == TransactionType.DEBIT,
            onClick = { onSelect(TransactionType.DEBIT) },
            label = { Text("Expense") }
        )
        FilterChip(
            selected = selected == TransactionType.CREDIT,
            onClick = { onSelect(TransactionType.CREDIT) },
            label = { Text("Income") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selected: Category,
    selectedCustom: String? = null,
    isCredit: Boolean? = null,
    onSelect: (Category, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("spendix_prefs", android.content.Context.MODE_PRIVATE)
    
    val spendCustom = prefs.getString("custom_spend_categories", "") ?: ""
    val creditCustom = prefs.getString("custom_credit_categories", "") ?: ""
    
    val customCategories = remember(spendCustom, creditCustom, isCredit) {
        val list = if (isCredit == true) creditCustom.split(",") else if (isCredit == false) spendCustom.split(",") else (spendCustom.split(",") + creditCustom.split(","))
        list.filter { it.isNotBlank() }.map { it.substringBefore("|").trim() }.distinct()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCustom ?: selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 300.dp)
        ) {
            Category.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.label()) },
                    onClick = {
                        onSelect(cat, null)
                        expanded = false
                    }
                )
            }
            if (customCategories.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                customCategories.forEach { customName ->
                    DropdownMenuItem(
                        text = { Text(customName) },
                        onClick = {
                            onSelect(Category.OTHER, customName)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
