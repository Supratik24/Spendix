package com.smartspend.ai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.CardTravel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import android.content.Context
import androidx.compose.material.icons.rounded.Category
import com.smartspend.ai.data.Category
import com.smartspend.ai.ui.components.getCategoryIcon
import com.smartspend.ai.ui.theme.getCategoryColor
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete

data class CategoryItem(val name: String, val icon: ImageVector, val color: Color, val isCustom: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendCategoriesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("spendix_prefs", Context.MODE_PRIVATE)
    
    // Built-in
    val builtin = Category.entries
        .filter { it != Category.INCOME && it != Category.REFUND }
        .map { 
            val name = it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            CategoryItem(name, getCategoryIcon(it), getCategoryColor(it), false) 
        }
        
    var customCategoriesStr by remember { mutableStateOf(prefs.getString("custom_spend_categories", "") ?: "") }
    
    val customCategories = customCategoriesStr.split(",")
        .filter { it.isNotBlank() }
        .map { 
            val parts = it.split("|")
            val name = parts[0].trim()
            val colorHex = if (parts.size > 1) parts[1] else "#94A3B8"
            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch(e: Exception) { Color(0xFF94A3B8) }
            CategoryItem(name, Icons.Rounded.Category, color, true)
        }

    val allCategories = builtin + customCategories
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    var selectedColor by remember { mutableStateOf("#94A3B8") }
    val availableColors = listOf("#EF4444", "#F97316", "#EAB308", "#22C55E", "#3B82F6", "#A855F7", "#EC4899", "#94A3B8")
    
    
    var showEditDialog by remember { mutableStateOf<CategoryItem?>(null) }
    var editCategoryName by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Spend categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Category")
                    }
                }
            )
        }
    ) { innerPadding ->
        Card(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            LazyColumn {
                items(allCategories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (category.isCustom) {
                                Text(text = category.name.take(1).uppercase(), color = category.color, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = category.name,
                                    tint = category.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (category.isCustom) {
                            IconButton(onClick = {
                                showEditDialog = category
                                editCategoryName = category.name
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                }
            }
        }
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Category") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Category Name") },
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Color", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableColors.take(4).forEach { colorHex ->
                                val c = Color(android.graphics.Color.parseColor(colorHex))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (selectedColor == colorHex) 2.dp else 0.dp,
                                            color = if (selectedColor == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = colorHex }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableColors.drop(4).forEach { colorHex ->
                                val c = Color(android.graphics.Color.parseColor(colorHex))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (selectedColor == colorHex) 2.dp else 0.dp,
                                            color = if (selectedColor == colorHex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = colorHex }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newCategoryName.isNotBlank()) {
                            val entry = "${newCategoryName.trim()}|$selectedColor"
                            val newList = if (customCategoriesStr.isBlank()) entry else "$customCategoriesStr,$entry"
                            prefs.edit().putString("custom_" + "spend_categories", newList).apply()
                            customCategoriesStr = newList
                            newCategoryName = ""
                        }
                        showAddDialog = false
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        if (showEditDialog != null) {
            val oldName = showEditDialog!!.name
            AlertDialog(
                onDismissRequest = { showEditDialog = null },
                title = { Text("Edit Category") },
                text = {
                    OutlinedTextField(
                        value = editCategoryName,
                        onValueChange = { editCategoryName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editCategoryName.isNotBlank()) {
                            val currentList = customCategoriesStr.split(",").filter { it.isNotBlank() }.toMutableList()
                            val index = currentList.indexOfFirst { it.startsWith("$oldName|") || it == oldName }
                            if (index != -1) {
                                val colorPart = currentList[index].substringAfter("|", "#94A3B8")
                                currentList[index] = "${editCategoryName.trim()}|$colorPart"
                                val newList = currentList.joinToString(",")
                                prefs.edit().putString("custom_spend_categories", newList).apply()
                                customCategoriesStr = newList
                            }
                        }
                        showEditDialog = null
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            val currentList = customCategoriesStr.split(",").filter { it.isNotBlank() }.toMutableList()
                            currentList.removeIf { it.startsWith("$oldName|") || it == oldName }
                            val newList = currentList.joinToString(",")
                            prefs.edit().putString("custom_spend_categories", newList).apply()
                            customCategoriesStr = newList
                            showEditDialog = null
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showEditDialog = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}
