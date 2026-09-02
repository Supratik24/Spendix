package com.smartspend.ai.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.data.TransactionSource
import com.smartspend.ai.data.TransactionType
import com.smartspend.ai.ui.theme.*

@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.type == TransactionType.CREDIT
    val context = androidx.compose.ui.platform.LocalContext.current
    val categoryColor = getCategoryColor(context, transaction.category, transaction.customCategory)
    val categoryBg = getCategoryBgColor(context, transaction.category, transaction.customCategory)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Surface(
                color = categoryBg,
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = getCategoryIcon(transaction.category),
                    contentDescription = transaction.category.label(),
                    modifier = Modifier.padding(10.dp),
                    tint = categoryColor
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.merchant ?: transaction.customCategory ?: transaction.category.label(),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge
                )
                val details = buildList {
                    if (!transaction.splitInfo.isNullOrBlank()) {
                        add("✂️ Split")
                    }
                    if (transaction.isCreditCard) {
                        add("💳 Credit Card")
                    } else if (!transaction.sender.isNullOrBlank()) {
                        add(transaction.sender.split("-").last())
                    }
                    add(transaction.category.label())
                    add(formatShortDate(transaction.occurredAt))
                    if (transaction.source == TransactionSource.MANUAL) {
                        add("Manual")
                    }
                    
                }
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (!transaction.tags.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        transaction.tags!!.split(",").forEach { tag ->
                            if (tag.isNotBlank()) {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = tag.trim(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = (if (isCredit) "+" else "−") + formatMoney(transaction.amountPaise),
                fontWeight = FontWeight.Bold,
                color = if (isCredit) IncomeGreen else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
