package com.smartspend.ai.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartspend.ai.ui.theme.ExpenseBg
import com.smartspend.ai.ui.theme.ExpenseRed
import com.smartspend.ai.ui.theme.IncomeBg
import com.smartspend.ai.ui.theme.IncomeGreen

@Composable
fun BalanceCard(
    balancePaise: Long,
    spentPaise: Long,
    earnedPaise: Long,
    monthLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = "$monthLabel BALANCE".uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            AnimatedContent(
                targetState = balancePaise,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "BalanceAmount"
            ) { targetBalance ->
                Text(
                    text = formatMoney(targetBalance),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPill(
                    label = "Income",
                    amountPaise = earnedPaise,
                    color = IncomeGreen,
                    bgColor = IncomeBg,
                    isIncome = true,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Spent",
                    amountPaise = spentPaise,
                    color = ExpenseRed,
                    bgColor = ExpenseBg,
                    isIncome = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    amountPaise: Long,
    color: Color,
    bgColor: Color,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                AnimatedContent(
                    targetState = amountPaise,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "StatAmount"
                ) { targetAmount ->
                    Text(
                        text = formatMoney(targetAmount),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                }
            }
        }
    }
}
