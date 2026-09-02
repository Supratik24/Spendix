package com.smartspend.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartspend.ai.data.Category
import com.smartspend.ai.ui.theme.getCategoryBgColor
import com.smartspend.ai.ui.theme.getCategoryColor

@Composable
fun SpendingBreakdown(
    spendingByCategory: List<Pair<Category, Long>>,
    totalSpentPaise: Long,
    highlightedCategory: Category?,
    onCategoryClick: (Category) -> Unit,
    onClearHighlight: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (spendingByCategory.isEmpty()) return

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap a slice or category to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                AnimatedVisibility(visible = highlightedCategory != null) {
                    SuggestionChip(
                        onClick = onClearHighlight,
                        label = { Text("Clear filter", fontSize = 11.sp) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Centered Donut Chart
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    data = spendingByCategory,
                    totalSpentPaise = totalSpentPaise,
                    highlightedCategory = highlightedCategory,
                    onCategoryClick = onCategoryClick
                )
            }

            Spacer(Modifier.height(20.dp))

            // Category progress cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                spendingByCategory.forEach { (category, amount) ->
                    CategoryProgressRow(
                        category = category,
                        amount = amount,
                        totalSpent = totalSpentPaise,
                        isHighlighted = category == highlightedCategory,
                        isDimmed = highlightedCategory != null && category != highlightedCategory,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryProgressRow(
    category: Category,
    amount: Long,
    totalSpent: Long,
    isHighlighted: Boolean,
    isDimmed: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = getCategoryColor(category)
    val percentage = if (totalSpent > 0) (amount.toFloat() / totalSpent.toFloat()) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 600),
        label = "ProgressAnimation"
    )

    val rowBgColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> categoryColor.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "RowBgColor"
    )

    val alpha = if (isDimmed) 0.4f else 1f

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = rowBgColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = getCategoryBgColor(category).copy(alpha = alpha * 0.2f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = category.label(),
                        tint = categoryColor.copy(alpha = alpha),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(amount),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    )
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor.copy(alpha = alpha),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Animated progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.Gray.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(categoryColor.copy(alpha = alpha))
                )
            }
        }
    }
}
