package com.smartspend.ai.ui.theme
import android.content.Context

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.smartspend.ai.data.Category

// Brand palette
val IndigoPrimary = Color(0xFF4F46E5)
val IndigoPrimaryDark = Color(0xFF818CF8)
val IndigoContainerLight = Color(0xFFEEF2FF)
val IndigoContainerDark = Color(0xFF312E81)
val OnIndigoContainerLight = Color(0xFF312E81)
val OnIndigoContainerDark = Color(0xFFE0E7FF)

// Background & Surface
val SlateBgLight = Color(0xFFF8FAFC)
val SlateBgDark = Color(0xFF0F172A)
val SlateSurfaceLight = Color(0xFFFFFFFF)
val SlateSurfaceDark = Color(0xFF1E293B)
val SlateSecondaryLight = Color(0xFF64748B)
val SlateSecondaryDark = Color(0xFF94A3B8)
val SlateTextLight = Color(0xFF0F172A)
val SlateTextDark = Color(0xFFF8FAFC)

// Financial Semantic Colors
val IncomeGreen = Color(0xFF16A34A)
val ExpenseRed = Color(0xFFDC2626)
val IncomeBg = Color(0xFFDCFCE7)
val ExpenseBg = Color(0xFFFEE2E2)

// Category Semantic Colors

fun getCategoryColor(context: Context, category: Category, customCategory: String?): Color {
    if (customCategory != null) {
        val prefs = context.getSharedPreferences("spendix_prefs", Context.MODE_PRIVATE)
        val spend = prefs.getString("custom_spend_categories", "") ?: ""
        val credit = prefs.getString("custom_credit_categories", "") ?: ""
        val all = (spend.split(",") + credit.split(",")).filter { it.isNotBlank() }
        val match = all.find { it.startsWith("$customCategory|") }
        if (match != null) {
            val hex = match.substringAfter("|")
            try {
                return Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {}
        }
    }
    return getCategoryColor(category)
}

fun getCategoryBgColor(context: Context, category: Category, customCategory: String?): Color = getCategoryColor(context, category, customCategory).copy(alpha = 0.15f)

fun getCategoryColor(category: Category): Color = when (category) {
    Category.FOOD -> Color(0xFFF97316)          // Vibrant Orange
    Category.GROCERIES -> Color(0xFF10B981)     // Emerald Green
    Category.TRAVEL -> Color(0xFF06B6D4)        // Cyan
    Category.SHOPPING -> Color(0xFF8B5CF6)      // Violet Purple
    Category.BILLS -> Color(0xFFF59E0B)         // Amber Gold
    Category.HEALTH -> Color(0xFFF43F5E)        // Rose Red
    Category.ENTERTAINMENT -> Color(0xFF6366F1) // Indigo
    Category.INVESTMENT -> Color(0xFF14B8A6)    // Teal
    Category.CASH -> Color(0xFF78716C)          // Stone Gray
    Category.TRANSFER -> Color(0xFF3B82F6)      // Bright Blue
    Category.INCOME -> Color(0xFF22C55E)        // Green
    Category.REFUND -> Color(0xFF2DD4BF)        // Mint Cyan
    Category.OTHER -> Color(0xFF94A3B8)         // Slate
}

// Category Light Backgrounds for Badges
fun getCategoryBgColor(category: Category): Color = getCategoryColor(category).copy(alpha = 0.15f)

// Vibrant Chart Palette
val ChartColors = listOf(
    Color(0xFFF97316), // Food
    Color(0xFF8B5CF6), // Shopping
    Color(0xFF10B981), // Groceries
    Color(0xFF06B6D4), // Travel
    Color(0xFFF59E0B), // Bills
    Color(0xFFF43F5E), // Health
    Color(0xFF6366F1), // Entertainment
    Color(0xFF14B8A6), // Investment
    Color(0xFF3B82F6), // Transfer
    Color(0xFFEC4899), // Pink
    Color(0xFF64748B)  // Slate
)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
)

val DarkPrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF312E81), Color(0xFF4C1D95))
)
