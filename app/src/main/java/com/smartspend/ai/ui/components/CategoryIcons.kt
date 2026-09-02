package com.smartspend.ai.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.smartspend.ai.data.Category

fun getCategoryIcon(category: Category): ImageVector = when (category) {
    Category.FOOD -> Icons.Rounded.Restaurant
    Category.GROCERIES -> Icons.Rounded.LocalGroceryStore
    Category.TRAVEL -> Icons.Rounded.DirectionsCar
    Category.SHOPPING -> Icons.Rounded.ShoppingBag
    Category.BILLS -> Icons.Rounded.Receipt
    Category.HEALTH -> Icons.Rounded.MedicalServices
    Category.ENTERTAINMENT -> Icons.Rounded.Movie
    Category.INVESTMENT -> Icons.AutoMirrored.Rounded.TrendingUp
    Category.CASH -> Icons.Rounded.Atm
    Category.TRANSFER -> Icons.Rounded.SwapHoriz
    Category.INCOME -> Icons.Rounded.AccountBalanceWallet
    Category.REFUND -> Icons.Rounded.Replay
    Category.OTHER -> Icons.Rounded.Category
}
