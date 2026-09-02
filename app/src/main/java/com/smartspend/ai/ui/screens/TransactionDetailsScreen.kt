package com.smartspend.ai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.smartspend.ai.data.Category
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.data.TransactionType
import com.smartspend.ai.data.SplitEntry
import com.smartspend.ai.data.parseSplitInfo
import com.smartspend.ai.ui.components.formatMoney
import com.smartspend.ai.ui.components.getCategoryIcon
import com.smartspend.ai.ui.components.label
import com.smartspend.ai.ui.theme.*
import com.smartspend.ai.ui.viewmodel.SpendViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter



// Parses "Name:amount,Name2:amount2" stored in splitInfo


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transaction: Transaction,
    viewModel: SpendViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val isCredit = transaction.type == TransactionType.CREDIT
    var showMenu by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }

    // Notes state — editable
    var notesText by remember(transaction.id) { mutableStateOf(transaction.notes ?: "") }
    var notesFocused by remember { mutableStateOf(false) }

    // Tags state
    val currentTags = remember(transaction.tags) {
        transaction.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toMutableStateList()
            ?: mutableStateListOf()
    }
    
    

    // Split entries from splitInfo field
    val splitEntries = remember(transaction.splitInfo) { parseSplitInfo(transaction.splitInfo) }

    // Contacts permission
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
        if (granted) showSplitDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCredit) "Credit transaction" else "Debit transaction") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Change Category") },
                            onClick = { showMenu = false; showCategoryDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Transaction") },
                            onClick = {
                                showMenu = false
                                viewModel.delete(transaction.id)
                                onBack()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Header Card ─────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = transaction.merchant ?: transaction.category.label(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = (if (isCredit) "+" else "−") + "₹${formatMoney(transaction.amountPaise)}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCredit) IncomeGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = if (isCredit) IncomeGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (isCredit) "Credit" else "Debit",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = if (isCredit) IncomeGreen else MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    val dateStr = Instant.ofEpochMilli(transaction.occurredAt)
                        .atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy  •  h:mm a"))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (transaction.isCreditCard) {
                        Spacer(Modifier.height(6.dp))
                        Text("💳 Credit Card", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ── Sender / Category ────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(getCategoryColor(context, transaction.category, transaction.customCategory).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (transaction.customCategory != null) {
                            Text(transaction.customCategory.take(1).uppercase(), color = getCategoryColor(context, transaction.category, transaction.customCategory), style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        } else {
                            Icon(getCategoryIcon(transaction.category), contentDescription = null, tint = getCategoryColor(context, transaction.category, transaction.customCategory))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(transaction.sender ?: "Manual Entry", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(transaction.customCategory ?: transaction.category.label(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showCategoryDialog = true }) { Text("Edit") }
                }
            }

            // ── Notes ────────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Notes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        modifier = Modifier.fillMaxWidth().onFocusChanged { fs ->
                            if (notesFocused && !fs.isFocused) viewModel.saveNotes(transaction.id, notesText)
                            notesFocused = fs.isFocused
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (notesText.isEmpty()) Text("Tap to add a note...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                inner()
                            }
                        }
                    )
                    if (notesFocused) {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.saveNotes(transaction.id, notesText); notesFocused = false }) { Text("Save") }
                        }
                    }
                }
            }

            // ── Tags ─────────────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    val prefs = context.getSharedPreferences("spendix_prefs", android.content.Context.MODE_PRIVATE)
                    val customTagsStr = prefs.getString("custom_tags", "") ?: ""
                    val prefTags = customTagsStr.split(",").filter { it.isNotBlank() }.map { it.trim() }
                    val allTagOptions = (prefTags + currentTags).distinct()
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allTagOptions) { tag ->
                            val selected = tag in currentTags
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) currentTags.remove(tag) else currentTags.add(tag)
                                    viewModel.saveTags(transaction.id, currentTags.toList())
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                        
                    }
                }
            }

            // ── Split Expenses ────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Split Expenses", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        if (splitEntries.isNotEmpty()) {
                            TextButton(onClick = { viewModel.saveSplitInfo(transaction.id, null) }) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                            TextButton(onClick = {
                                if (hasContactsPermission) showSplitDialog = true
                                else contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }) { Text("Edit") }
                        }
                    }

                    if (splitEntries.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                if (hasContactsPermission) showSplitDialog = true
                                else contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Split with contacts")
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        val yourShare = if (splitEntries.isNotEmpty()) transaction.amountPaise / (splitEntries.size + 1) else 0L
                        // Your own share row
                        SplitPersonRow(name = "You (your share)", amountPaise = yourShare)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        splitEntries.forEach { entry ->
                            SplitPersonRow(name = entry.name, amountPaise = entry.amountPaise)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Total: ₹${formatMoney(transaction.amountPaise)} split among ${splitEntries.size + 1} people",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Other Info (Ref ID + SMS) ─────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text("Other info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    if (transaction.refId != null) {
                        Text("UPI Ref No", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(transaction.refId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (transaction.rawBody != null) {
                        Text("SMS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(transaction.rawBody, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    } else if (transaction.refId == null) {
                        Text("No additional info available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Category Dialog ─────────────────────────────────────────────────────
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Change Category") },
            text = {
                com.smartspend.ai.ui.dialogs.CategoryDropdown(
                    selected = transaction.category,
                    selectedCustom = transaction.customCategory,
                    isCredit = isCredit,
                    onSelect = { cat, custom -> viewModel.changeCategory(transaction.id, cat, custom); showCategoryDialog = false }
                )
            },
            confirmButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Split Dialog ─────────────────────────────────────────────────────────
    if (showSplitDialog) {
        val uiState by viewModel.uiState.collectAsState()
        SplitDialog(
            totalAmountPaise = transaction.amountPaise,
            existingSplits = splitEntries,
            allTransactions = uiState.transactions,
            context = context,
            onSave = { entries ->
                // Save as "Name:amountPaise,Name2:amount2" in splitInfo — NOT in notes
                val encoded = entries.joinToString(",") { "${it.name}:${it.amountPaise}" }
                viewModel.saveSplitInfo(transaction.id, encoded.ifBlank { null })
                showSplitDialog = false
            },
            onDismiss = { showSplitDialog = false }
        )
    }
}

@Composable
private fun SplitPersonRow(name: String, amountPaise: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text("₹${formatMoney(amountPaise)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SplitDialog(
    totalAmountPaise: Long,
    existingSplits: List<SplitEntry>,
    allTransactions: List<Transaction>,
    context: android.content.Context,
    onSave: (List<SplitEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    var splitMode by remember { mutableStateOf(SplitMode.EQUAL) }
    val selectedContacts = remember { mutableStateListOf<String>().also { it.addAll(existingSplits.map { e -> e.name }) } }
    
    // Store custom amounts as strings to allow typing
    val customAmounts = remember { mutableStateMapOf<String, String>().also { map ->
        existingSplits.forEach { map[it.name] = (it.amountPaise / 100).toString() }
    } }
    
    var contactSearch by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }

    // Load ALL device contacts
    val deviceContacts = remember {
        val list = mutableListOf<String>()
        try {
            val cursor = context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx)
                    if (name != null && name !in list) list.add(name)
                }
            }
        } catch (_: Exception) {}
        list
    }

    // Extract recent groups from transaction history
    val recentGroups = remember(allTransactions) {
        allTransactions.mapNotNull { txn ->
            if (txn.splitInfo.isNullOrBlank()) null
            else {
                val names = parseSplitInfo(txn.splitInfo).map { it.name }.sorted()
                if (names.isNotEmpty()) names else null
            }
        }
        .groupBy { it }
        .map { it.key to it.value.size }
        .sortedByDescending { it.second }
        .map { it.first }
        .take(5)
    }

    // Filtered contacts based on advanced search
    val filteredContacts by remember(contactSearch, selectedContacts.toList()) {
        derivedStateOf {
            val queryTokens = contactSearch.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            deviceContacts
                .filter { it !in selectedContacts }
                .filter { contact ->
                    if (queryTokens.isEmpty()) true
                    else {
                        val contactLower = contact.lowercase()
                        queryTokens.all { contactLower.contains(it) }
                    }
                }
                .sortedBy { contact ->
                    val contactLower = contact.lowercase()
                    val query = contactSearch.trim().lowercase()
                    when {
                        contactLower == query -> 0
                        contactLower.startsWith(query) -> 1
                        else -> 2
                    }
                }
        }
    }

    // Equal share calculation
    val totalPeople = selectedContacts.size + 1
    val equalShare = if (totalPeople > 0) totalAmountPaise / totalPeople else totalAmountPaise

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Split ₹${formatMoney(totalAmountPaise)}") },
        text = {
            Column(modifier = Modifier.heightIn(max = 500.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Split mode
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = splitMode == SplitMode.EQUAL, onClick = { splitMode = SplitMode.EQUAL }, label = { Text("Equal split") })
                    FilterChip(selected = splitMode == SplitMode.BY_AMOUNT, onClick = { splitMode = SplitMode.BY_AMOUNT }, label = { Text("Custom Amount") })
                }

                // Selected contacts
                if (selectedContacts.isNotEmpty()) {
                    if (splitMode == SplitMode.EQUAL) {
                        Text("${selectedContacts.size + 1} people — ₹${formatMoney(equalShare)} each", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(selectedContacts.toList()) { name ->
                                InputChip(
                                    selected = true,
                                    onClick = { selectedContacts.remove(name); customAmounts.remove(name) },
                                    label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    } else {
                        // Custom Amount View
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
                            selectedContacts.toList().forEach { name ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    OutlinedTextField(
                                        value = customAmounts[name] ?: "",
                                        onValueChange = { customAmounts[name] = it },
                                        modifier = Modifier.width(100.dp).height(50.dp),
                                        placeholder = { Text("₹0") },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                                    )
                                    IconButton(onClick = { selectedContacts.remove(name); customAmounts.remove(name) }) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // Recent Groups
                if (contactSearch.isBlank() && recentGroups.isNotEmpty() && selectedContacts.isEmpty()) {
                    Text("Recent Groups", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentGroups) { group ->
                            AssistChip(
                                onClick = { 
                                    selectedContacts.clear()
                                    selectedContacts.addAll(group)
                                },
                                label = { Text(group.joinToString(", ")) }
                            )
                        }
                    }
                    HorizontalDivider()
                }

                // Search bar
                OutlinedTextField(
                    value = contactSearch,
                    onValueChange = { contactSearch = it },
                    label = { Text("Search contacts") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Contact list (scrollable)
                Column(modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 200.dp)) {
                    if (filteredContacts.isEmpty() && contactSearch.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                val t = contactSearch.trim()
                                if (t.isNotBlank() && t !in selectedContacts) selectedContacts.add(t)
                                contactSearch = ""
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Add \"$contactSearch\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    filteredContacts.take(50).forEach { name ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedContacts.add(name); contactSearch = "" }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val entries = if (splitMode == SplitMode.EQUAL) {
                        selectedContacts.map { SplitEntry(name = it, amountPaise = equalShare) }
                    } else {
                        selectedContacts.map { name ->
                            val rupees = customAmounts[name]?.toLongOrNull() ?: 0L
                            SplitEntry(name = name, amountPaise = rupees * 100)
                        }
                    }
                    onSave(entries)
                },
                enabled = selectedContacts.isNotEmpty()
            ) { Text("Save Split") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

enum class SplitMode { EQUAL, BY_AMOUNT }
