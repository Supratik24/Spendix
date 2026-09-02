package com.smartspend.ai.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartspend.ai.data.Category
import com.smartspend.ai.ui.theme.getCategoryBgColor
import com.smartspend.ai.ui.theme.getCategoryColor
import kotlin.math.atan2
import kotlin.math.sqrt

@Composable
fun DonutChart(
    data: List<Pair<Category, Long>>,
    totalSpentPaise: Long,
    highlightedCategory: Category?,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.second }.toFloat().coerceAtLeast(1f)
    val animationProgress = remember { Animatable(0f) }
    val rotationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        rotationProgress.snapTo(-30f)
        launch {
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            rotationProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
            )
        }
    }

    val highlightedPair = data.firstOrNull { it.first == highlightedCategory }

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(data, total) {
                    detectTapGestures { offset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val dist = sqrt(dx * dx + dy * dy)
                        val outerRadius = size.width / 2f
                        val innerRadius = outerRadius - 40.dp.toPx()

                        // Only respond if tapped within donut ring or near center
                        if (dist >= innerRadius * 0.5f && dist <= outerRadius * 1.2f) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            // Normalize angle from -90 deg start
                            angle = (angle + 90f + 360f) % 360f

                            var currentAngle = 0f
                            for ((category, value) in data) {
                                val sweep = (value / total * 360f)
                                if (angle >= currentAngle && angle <= currentAngle + sweep) {
                                    onCategoryClick(category)
                                    break
                                }
                                currentAngle += sweep
                            }
                        }
                    }
                }
        ) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) - 16.dp.toPx()
            val startBase = -90f + rotationProgress.value

            // Draw subtle background track
            drawCircle(
                color = Color.Gray.copy(alpha = 0.08f),
                radius = baseRadius,
                center = centerOffset,
                style = Stroke(width = 18.dp.toPx())
            )

            var currentStart = startBase
            data.forEach { (category, value) ->
                val sweep = (value / total * 360f) * animationProgress.value
                val isHighlighted = category == highlightedCategory
                val sliceColor = getCategoryColor(category)
                val strokeWidth = if (isHighlighted) 24.dp.toPx() else 18.dp.toPx()
                val alpha = if (highlightedCategory == null || isHighlighted) 1f else 0.35f

                if (sweep > 0.5f) {
                    drawArc(
                        color = sliceColor.copy(alpha = alpha),
                        startAngle = currentStart,
                        sweepAngle = (sweep - 1.5f).coerceAtLeast(0.1f), // slight gap between segments
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                currentStart += sweep
            }
        }

        // Center Animated Readout
        AnimatedContent(
            targetState = highlightedPair,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
            label = "DonutCenterReadout"
        ) { selected ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                if (selected != null) {
                    val (category, amount) = selected
                    val percentage = (amount * 100 / totalSpentPaise.coerceAtLeast(1L))
                    Surface(
                        shape = CircleShape,
                        color = getCategoryBgColor(category),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category),
                            contentDescription = category.label(),
                            tint = getCategoryColor(category),
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Text(
                        text = category.label(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = getCategoryColor(category),
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = formatMoney(amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "$percentage% of total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 10.sp
                    )
                } else {
                    Text(
                        text = "TOTAL SPENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = formatMoney(totalSpentPaise),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${data.size} categories",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
