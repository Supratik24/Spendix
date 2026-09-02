package com.smartspend.ai.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartspend.ai.data.Category
import com.smartspend.ai.ui.theme.getCategoryColor

@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search transactions, merchants, sender…") },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(14.dp)
            )
            Category.entries.forEach { category ->
                val isSelected = selectedCategory == category
                val color = getCategoryColor(category)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onCategorySelected(if (isSelected) null else category)
                    },
                    label = { Text(category.label(), fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else color,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}
