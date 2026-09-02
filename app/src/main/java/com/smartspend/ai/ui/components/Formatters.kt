package com.smartspend.ai.ui.components

import com.smartspend.ai.data.Category
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatMoney(paise: Long): String {
    val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 2
    return formatter.format(paise / 100.0)
}

fun formatDate(time: Long): String {
    return SimpleDateFormat("d MMM, h:mm a", Locale("en", "IN")).format(Date(time))
}

fun formatShortDate(time: Long): String {
    return SimpleDateFormat("d MMM", Locale("en", "IN")).format(Date(time))
}

fun Category.label(): String {
    return name.lowercase().replaceFirstChar { it.titlecase() }
}
