package com.smartspend.ai.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("spendix_prefs", Context.MODE_PRIVATE)
    
    var customTagsStr by remember { mutableStateOf(prefs.getString("custom_tags", "") ?: "") }
    
    val tags = customTagsStr.split(",")
        .filter { it.isNotBlank() }
        .map { it.trim() }

    var showAddDialog by remember { mutableStateOf(false) }
    var newTagName by remember { mutableStateOf("") }
    
    var showEditDialog by remember { mutableStateOf<String?>(null) }
    var editTagName by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Tag")
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
            if (tags.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tags added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(tags) { tag ->
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
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Label,
                                    contentDescription = "Tag",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                showEditDialog = tag
                                editTagName = tag
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Tag") },
                text = {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("Tag Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newTagName.isNotBlank()) {
                            val newList = if (customTagsStr.isBlank()) newTagName.trim() else "$customTagsStr,${newTagName.trim()}"
                            prefs.edit().putString("custom_tags", newList).apply()
                            customTagsStr = newList
                            newTagName = ""
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
            val oldName = showEditDialog!!
            AlertDialog(
                onDismissRequest = { showEditDialog = null },
                title = { Text("Edit Tag") },
                text = {
                    OutlinedTextField(
                        value = editTagName,
                        onValueChange = { editTagName = it },
                        label = { Text("Tag Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (editTagName.isNotBlank()) {
                            val currentList = customTagsStr.split(",").filter { it.isNotBlank() }.toMutableList()
                            val index = currentList.indexOf(oldName)
                            if (index != -1) {
                                currentList[index] = editTagName.trim()
                                val newList = currentList.joinToString(",")
                                prefs.edit().putString("custom_tags", newList).apply()
                                customTagsStr = newList
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
                            val currentList = customTagsStr.split(",").filter { it.isNotBlank() }.toMutableList()
                            currentList.remove(oldName)
                            val newList = currentList.joinToString(",")
                            prefs.edit().putString("custom_tags", newList).apply()
                            customTagsStr = newList
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
