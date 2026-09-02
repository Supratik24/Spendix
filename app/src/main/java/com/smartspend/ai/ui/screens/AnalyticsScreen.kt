package com.smartspend.ai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartspend.ai.data.Category
import com.smartspend.ai.data.TransactionType
import com.smartspend.ai.ui.components.formatMoney
import com.smartspend.ai.ui.model.SpendUiState
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

data class BudgetRecommendation(val icon: String, val title: String, val description: String, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(state: SpendUiState, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val zone = ZoneId.systemDefault()
    var selectedDay by remember { mutableStateOf<Int?>(null) }
    var chatInput by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    val focusManager = LocalFocusManager.current

    val dailyDebits = state.monthTransactions
        .filter { it.type == TransactionType.DEBIT && !it.isCreditCard }
        .groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate().dayOfMonth }
        .mapValues { it.value.sumOf { t -> t.amountPaise } }

    val dailyCredits = state.monthTransactions
        .filter { it.type == TransactionType.CREDIT && !it.isCreditCard }
        .groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zone).toLocalDate().dayOfMonth }
        .mapValues { it.value.sumOf { t -> t.amountPaise } }

    val daysInMonth = state.selectedMonth?.lengthOfMonth() ?: java.time.YearMonth.now().lengthOfMonth()
    val activeDays = (1..daysInMonth).toList()
    val maxAmount = (dailyDebits.values + dailyCredits.values).maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val totalSpent = state.spentPaise
    val totalEarned = state.earnedPaise
    val netBalance = totalEarned - totalSpent
    val savingsRate = if (totalEarned > 0) ((netBalance.toFloat() / totalEarned) * 100).roundToInt() else 0
    val topCategory = state.spendingByCategory.firstOrNull()
    
    // For average, it's better to divide by the days passed if it's the current month
    val currentYearMonth = java.time.YearMonth.now()
    val daysPassed = if (state.selectedMonth == currentYearMonth || state.selectedMonth == null) {
        java.time.LocalDate.now().dayOfMonth
    } else {
        daysInMonth
    }
    val avgDailySpend = if (daysPassed > 0) totalSpent / daysPassed else 0L
    val budgetRecs = buildBudgetRecommendations(state, savingsRate, topCategory)

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FinancialHealthCard(savingsRate, totalSpent, totalEarned, netBalance)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatCard(Modifier.weight(1f), "Spent", "Rs.${formatMoney(totalSpent)}", MaterialTheme.colorScheme.error, Icons.Rounded.TrendingDown)
                MiniStatCard(Modifier.weight(1f), "Earned", "Rs.${formatMoney(totalEarned)}", Color(0xFF4CAF50), Icons.Rounded.TrendingUp)
                MiniStatCard(Modifier.weight(1f), "Avg/Day", "Rs.${formatMoney(avgDailySpend)}", MaterialTheme.colorScheme.primary, Icons.Rounded.CalendarMonth)
            }

            if (activeDays.isNotEmpty()) {
                DailyBarChart(activeDays, dailyDebits, dailyCredits, maxAmount, selectedDay) { selectedDay = it }
            }

            if (budgetRecs.isNotEmpty()) {
                BudgetRecommendationsCard(budgetRecs)
            }

            AiAssistantCard(state, savingsRate, topCategory, avgDailySpend, chatInput, chatHistory,
                onInputChange = { chatInput = it },
                onSend = { q ->
                    val ans = generateAiResponse(context, q, state, savingsRate, topCategory, avgDailySpend)
                    chatHistory = chatHistory + Pair(q, ans)
                    chatInput = ""
                    focusManager.clearFocus()
                },
                onClear = { chatHistory = emptyList() }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FinancialHealthCard(savingsRate: Int, totalSpent: Long, totalEarned: Long, netBalance: Long) {
    val score = when {
        savingsRate >= 30 -> 90; savingsRate >= 20 -> 75; savingsRate >= 10 -> 55; savingsRate >= 0 -> 35; else -> 15
    }
    val label = when {
        savingsRate >= 30 -> "Excellent"; savingsRate >= 20 -> "Good"; savingsRate >= 10 -> "Fair"; savingsRate >= 0 -> "Needs Attention"; else -> "Critical"
    }
    val color = when {
        savingsRate >= 30 -> Color(0xFF4CAF50); savingsRate >= 20 -> Color(0xFF8BC34A); savingsRate >= 10 -> Color(0xFFFFC107); savingsRate >= 0 -> Color(0xFFFF9800); else -> Color(0xFFE53935)
    }
    val animScore by animateFloatAsState(targetValue = score.toFloat() / 100f, animationSpec = tween(800), label = "score")
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(color.copy(alpha = 0.15f), MaterialTheme.colorScheme.surface))).padding(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Favorite, null, tint = color, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Financial Health", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("$score", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = color)
                    Text("/100", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))) {
                    Box(modifier = Modifier.fillMaxWidth(animScore).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (netBalance >= 0) "Saving ${savingsRate}% of income this period" else "Spending exceeds income by Rs.${formatMoney(-netBalance)}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MiniStatCard(modifier: Modifier, label: String, value: String, color: Color, icon: ImageVector) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DailyBarChart(
    activeDays: List<Int>,
    dailyDebits: Map<Int, Long>,
    dailyCredits: Map<Int, Long>,
    maxAmount: Long,
    selectedDay: Int?,
    onDaySelected: (Int?) -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BarChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Daily Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${activeDays.size} active days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Tap a bar to see details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(visible = selectedDay != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                val day = selectedDay
                if (day != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Day $day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val d = dailyDebits[day] ?: 0L
                                val c = dailyCredits[day] ?: 0L
                                Text("Spent: Rs.${formatMoney(d)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (d > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Earned: Rs.${formatMoney(c)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (c > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDaySelected(null) }) { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }

            val maxBarH = 130.dp
            Row(modifier = Modifier.fillMaxWidth().height(maxBarH), horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Bottom) {
                activeDays.forEach { day ->
                    val debit = dailyDebits[day] ?: 0L
                    val credit = dailyCredits[day] ?: 0L
                    val isSel = selectedDay == day
                    val df by animateFloatAsState((debit.toFloat() / maxAmount).coerceIn(0f, 1f), tween(500), label = "d$day")
                    val cf by animateFloatAsState((credit.toFloat() / maxAmount).coerceIn(0f, 1f), tween(500), label = "c$day")
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { onDaySelected(if (isSel) null else day) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (credit > 0) {
                            Box(modifier = Modifier.fillMaxWidth(0.8f).height((maxBarH * cf).coerceAtLeast(3.dp)).clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)).background(if (isSel) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50).copy(alpha = 0.65f)))
                            Spacer(Modifier.height(2.dp))
                        }
                        if (debit > 0) {
                            Box(modifier = Modifier.fillMaxWidth(0.9f).height((maxBarH * df).coerceAtLeast(3.dp)).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error))
                        }
                        if (credit == 0L && debit == 0L) {
                            Box(modifier = Modifier.fillMaxWidth(0.8f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error)); Spacer(Modifier.width(4.dp)); Text("Spent", style = MaterialTheme.typography.labelSmall) }
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4CAF50).copy(alpha = 0.65f))); Spacer(Modifier.width(4.dp)); Text("Earned", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
private fun BudgetRecommendationsCard(recs: List<BudgetRecommendation>) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E).copy(alpha = 0.08f)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Budget Recommendations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            recs.forEach { rec ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(rec.color.copy(alpha = 0.08f)).padding(10.dp), verticalAlignment = Alignment.Top) {
                    Text(rec.icon, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(rec.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(rec.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TypewriterText(text: String, modifier: Modifier = Modifier) {
    var textToDisplay by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        textToDisplay = ""
        val words = text.split(" ")
        for (i in words.indices) {
            textToDisplay += words[i] + " "
            kotlinx.coroutines.delay(35)
        }
    }
    Text(textToDisplay.trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun AiAssistantCard(
    state: SpendUiState, savingsRate: Int, topCategory: Pair<Category, Long>?, avgDailySpend: Long,
    chatInput: String, chatHistory: List<Pair<String, String>>,
    onInputChange: (String) -> Unit, onSend: (String) -> Unit, onClear: () -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF455A64)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("AI Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Text("Powered by Spendix", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))

            if (chatHistory.isEmpty()) {
                Text("Suggested topics:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(10.dp))
                val q1 = "How are my spending habits?"
                val q2 = "Can you give me a budget plan?"
                val q3 = "What should I be careful about?"
                val q4 = "Any tips for saving taxes?"
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = { onSend(q1) },
                            label = { Text(q1, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = { onSend(q2) },
                            label = { Text(q2, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = { onSend(q3) },
                            label = { Text(q3, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = { onSend(q4) },
                            label = { Text(q4, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            chatHistory.forEachIndexed { index, (u, a) ->
                val isLast = index == chatHistory.lastIndex
                AnimatedVisibility(visible = true, enter = fadeIn(tween(300)) + expandVertically(tween(300))) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Box(modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)).background(MaterialTheme.colorScheme.primary).padding(14.dp, 10.dp)) {
                                Text(u, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF263238), Color(0xFF455A64)))), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(14.dp, 10.dp)) {
                                if (isLast) TypewriterText(a) else Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            if (chatHistory.isNotEmpty()) {
                TextButton(onClick = onClear, modifier = Modifier.align(Alignment.End)) { Text("Clear conversation", style = MaterialTheme.typography.labelSmall) }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = chatInput, onValueChange = onInputChange, modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about your finances...", style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(24.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (chatInput.isNotBlank()) onSend(chatInput) }),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(if (chatInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        .clickable(enabled = chatInput.isNotBlank()) { onSend(chatInput) },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Send, null, tint = if (chatInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}

private fun generateAiResponse(context: android.content.Context, question: String, state: SpendUiState, savingsRate: Int, topCategory: Pair<Category, Long>?, avgDailySpend: Long): String {
    val prefs = context.getSharedPreferences("spendix_prefs", android.content.Context.MODE_PRIVATE)
    val userName = prefs.getString("user_name", "there")?.split(" ")?.firstOrNull() ?: "there"
    val q = question.lowercase()
    val topCatName = topCategory?.first?.name?.lowercase()?.replaceFirstChar { it.uppercase() }?.replace("_", " ") ?: "N/A"
    val topCatAmt = topCategory?.second ?: 0L
    val totalSpent = state.spentPaise
    val totalEarned = state.earnedPaise
    val monthStr = state.selectedMonth?.format(java.time.format.DateTimeFormatter.ofPattern("MMMM")) ?: "All Time"

    val categoryKeywords = mapOf(
        "food" to Category.FOOD, "dining" to Category.FOOD, "restaurant" to Category.FOOD, 
        "zomato" to Category.FOOD, "swiggy" to Category.FOOD,
        "shopping" to Category.SHOPPING, "clothes" to Category.SHOPPING, "amazon" to Category.SHOPPING, "flipkart" to Category.SHOPPING,
        "travel" to Category.TRAVEL, "flight" to Category.TRAVEL, "train" to Category.TRAVEL,
        "cab" to Category.TRAVEL, "uber" to Category.TRAVEL, "ola" to Category.TRAVEL, 
        "transport" to Category.TRAVEL, "fuel" to Category.TRAVEL, "petrol" to Category.TRAVEL,
        "grocery" to Category.GROCERIES, "groceries" to Category.GROCERIES, "blinkit" to Category.GROCERIES, 
        "zepto" to Category.GROCERIES, "instamart" to Category.GROCERIES,
        "medical" to Category.HEALTH, "health" to Category.HEALTH, "medicine" to Category.HEALTH,
        "bills" to Category.BILLS, "utility" to Category.BILLS, "electricity" to Category.BILLS, 
        "water" to Category.BILLS, "recharge" to Category.BILLS,
        "rent" to Category.BILLS, "housing" to Category.BILLS,
        "entertainment" to Category.ENTERTAINMENT, "movie" to Category.ENTERTAINMENT, "netflix" to Category.ENTERTAINMENT,
        "investment" to Category.INVESTMENT, "mutual fund" to Category.INVESTMENT, "stocks" to Category.INVESTMENT,
        "education" to Category.OTHER, "fee" to Category.OTHER
    )

    // Check specific specific merchants first
    if (q.contains("how much") || q.contains("spend") || q.contains("spent") || q.contains("total")) {
        for ((keyword, cat) in categoryKeywords) {
            if (q.contains(keyword)) {
                val merchantSpent = state.filteredTransactions.filter { 
                    it.type == com.smartspend.ai.data.TransactionType.DEBIT && it.merchant?.lowercase()?.contains(keyword) == true
                }.sumOf { txn ->
                    val splitTotal = com.smartspend.ai.data.parseSplitInfo(txn.splitInfo).sumOf { it.amountPaise }
                    txn.amountPaise - splitTotal
                }
                
                val catSpent = state.filteredTransactions.filter { 
                    it.type == com.smartspend.ai.data.TransactionType.DEBIT && it.category == cat 
                }.sumOf { txn ->
                    val splitTotal = com.smartspend.ai.data.parseSplitInfo(txn.splitInfo).sumOf { it.amountPaise }
                    txn.amountPaise - splitTotal
                }

                if (merchantSpent > 0 && (keyword == "zomato" || keyword == "swiggy" || keyword == "amazon" || keyword == "flipkart" || keyword == "uber" || keyword == "ola" || keyword == "blinkit" || keyword == "zepto" || keyword == "instamart" || keyword == "netflix")) {
                     return "You've spent Rs.${formatMoney(merchantSpent)} specifically at ${keyword.replaceFirstChar{it.uppercase()}} in $monthStr."
                } else if (catSpent > 0) {
                     return "You've spent Rs.${formatMoney(catSpent)} on ${cat.name.lowercase().replace("_", " ")} in $monthStr."
                } else {
                     return "You haven't spent anything on $keyword in $monthStr."
                }
            }
        }
        
        if (q.contains("biggest") || q.contains("highest") || q.contains("maximum") || q.contains("most")) {
            if (topCatAmt > 0) {
                return "Your biggest expense category is $topCatName at Rs.${formatMoney(topCatAmt)}."
            }
        }

        if (q.contains("total") || (!q.contains("on") && !q.contains("at"))) {
            return "Your total spending in $monthStr is Rs.${formatMoney(totalSpent)}."
        }
    }

    if (q.contains("biggest") || q.contains("highest") || q.contains("maximum") || q.contains("most")) {
        if (topCatAmt > 0) {
            return "Your biggest expense category is $topCatName at Rs.${formatMoney(topCatAmt)}."
        }
    }

    return when {
        q.matches(Regex(".*\\b(hi|hello|hey|hii|helo|sup)\\b.*")) -> {
            "Hello $userName! \uD83D\uDC4B How can I assist you with your finances today? Try asking 'How much did I spend on food' or 'Give me a budget plan!'"
        }
        q.contains("habit") || q.contains("pattern") || q.contains("spending") -> {
            val pct = if (totalEarned > 0 && topCatAmt > 0) ((topCatAmt.toFloat() / totalEarned) * 100).roundToInt() else 0
            "Based on your recent transactions, your biggest expense is $topCatName, where you've spent Rs.${formatMoney(topCatAmt)} ($pct% of your income). You are spending about Rs.${formatMoney(avgDailySpend)} per day on average."
        }
        q.contains("careful") || q.contains("vulnerabilit") || q.contains("warning") || q.contains("alert") -> {
            if (savingsRate < 15) {
                "Your current savings rate is $savingsRate%, which is a bit low, $userName. This means you might not have enough buffer for emergencies. Try to cut back on non-essential spending for a while!"
            } else if (state.spendingByCategory.none { it.first == com.smartspend.ai.data.Category.INVESTMENT }) {
                "You're doing a great job saving ($savingsRate%!), but you don't have any investments recorded. Keeping all your savings in a bank account means inflation will slowly eat away at it. Consider looking into basic investments!"
            } else {
                "You're actually doing pretty well! Your cash flow is positive, you're saving money, and you have investments. Keep up the good work and stick to your budget."
            }
        }
        q.contains("budget") || q.contains("plan") || q.contains("allocate") -> {
            if (totalEarned > 0) {
                val needs = totalEarned * 50 / 100
                val wants = totalEarned * 30 / 100
                val savings = totalEarned * 20 / 100
                """Here is a simple 50/30/20 budget plan based on your income of Rs.${formatMoney(totalEarned)}:

• Needs (50%): Rs.${formatMoney(needs)}
• Wants (30%): Rs.${formatMoney(wants)}
• Savings (20%): Rs.${formatMoney(savings)}

You've spent Rs.${formatMoney(totalSpent)} so far, so you're ${if (totalSpent <= needs + wants) "doing great" else "slightly over budget"} for this period!"""
            } else "I need a bit more data! Once you have some verified income transactions, I can generate a personalized budget plan for you."
        }
        q.contains("tax") || q.contains("tips") || q.contains("saving") || q.contains("invest") -> {
            """Here are a few easy ways to save on taxes:

1. Section 80C: You can invest up to Rs. 1.5 Lakhs in things like ELSS, PPF, or EPF.
2. Section 80D: Health insurance premiums for you and your parents are tax-deductible.
3. NPS: You can get an extra Rs. 50,000 deduction under 80CCD(1B).

Try to plan these investments early in the year!"""
        }
        q.contains("how are you") || q.contains("who are you") || q.contains("what can you do") -> {
            "I am Spendix AI, your personal financial assistant! I can analyze your spending, answer questions like 'how much did I spend on food', and suggest budgets. What would you like to know?"
        }
        q.contains("joke") || q.contains("funny") -> {
            "Why did the banker break up with his girlfriend? He lost interest! \uD83D\uDE02 But seriously, let's look at your budget..."
        }
        q.contains("help") || q.contains("options") -> {
            """I can help with a lot of things, $userName! Try asking me:
• 'How much did I spend on food?'
• 'What is my biggest expense?'
• 'Give me a budget plan.'
• 'Any warnings about my finances?'
• 'Give me tax saving tips.'"""
        }
        else -> {
            val responses = listOf(
                "That's an interesting question, $userName! While I'm focused on your finances, I can tell you that you've saved $savingsRate% of your Rs.${formatMoney(totalEarned)} income so far. Want to see a budget plan?",
                "I'm here to help you manage your money! Feel free to ask me for a budget plan, a spending habits review, or how much you spent on a specific category.",
                "I'm still learning about that! But regarding your finances, you are currently spending about Rs.${formatMoney(avgDailySpend)} per day. Let me know if you want tips to reduce that."
            )
            responses.random()
        }
    }
}
private fun buildBudgetRecommendations(state: SpendUiState, savingsRate: Int, topCategory: Pair<Category, Long>?): List<BudgetRecommendation> {
    val recs = mutableListOf<BudgetRecommendation>()
    val totalEarned = state.earnedPaise
    val totalSpent = state.spentPaise

    if (savingsRate < 10) {
        recs.add(BudgetRecommendation("🚨", "Savings Alert", "You're saving only $savingsRate% this month. Try to hit at least 20% by cutting discretionary spend.", Color(0xFFE53935)))
    } else if (savingsRate >= 30) {
        recs.add(BudgetRecommendation("🏆", "Excellent Savings!", "You're saving $savingsRate% of income. Consider investing the surplus in index funds or FD.", Color(0xFF4CAF50)))
    }

    topCategory?.let { (cat, amt) ->
        if (totalEarned > 0) {
            val pct = ((amt.toFloat() / totalEarned) * 100).roundToInt()
            if (pct > 40) recs.add(BudgetRecommendation("⚠️", "${cat.name.lowercase().replaceFirstChar { it.uppercase() }} Overload", "${pct}% of income on ${cat.name.lowercase()}. Recommended max: 30%. Set a stricter budget.", Color(0xFFFF9800)))
        }
    }

    val foodSpend = state.spendingByCategory.find { it.first == Category.FOOD }?.second ?: 0L
    val entSpend = state.spendingByCategory.find { it.first == Category.ENTERTAINMENT }?.second ?: 0L
    if (totalEarned > 0 && (foodSpend + entSpend) > totalEarned * 35 / 100) {
        recs.add(BudgetRecommendation("🍽️", "Food & Entertainment", "You're spending ${(((foodSpend + entSpend).toFloat() / totalEarned) * 100).roundToInt()}% on food and fun. Try cooking at home 2-3 more days per week.", Color(0xFF7E57C2)))
    }

    if (totalEarned > 0 && savingsRate in 10..29) {
        val target = totalEarned * 20 / 100
        val current = totalEarned - totalSpent
        val gap = target - current
        if (gap > 0) recs.add(BudgetRecommendation("🎯", "Close the Savings Gap", "Cut Rs.${formatMoney(gap)} more per month to hit 20% savings target.", Color(0xFF039BE5)))
    }

    if (state.spendingByCategory.none { it.first == Category.INVESTMENT } && savingsRate >= 20) {
        recs.add(BudgetRecommendation("📈", "Start Investing", "You're saving well! No investments recorded yet. Consider starting a SIP of Rs.500–1000/month.", Color(0xFF00897B)))
    }
    return recs.take(3)
}