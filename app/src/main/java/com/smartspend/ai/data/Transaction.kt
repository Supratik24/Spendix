package com.smartspend.ai.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "transactions", indices = [Index(value = ["fingerprint"], unique = true)])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountPaise: Long,
    val type: TransactionType,
    val category: Category,
    val customCategory: String? = null,
    val merchant: String?,
    val occurredAt: Long = java.time.Instant.now().toEpochMilli(),
    val source: TransactionSource = TransactionSource.SMS,
    val sender: String?,
    val fingerprint: String,
    val isCreditCard: Boolean = false,
    val refId: String? = null,
    val rawBody: String? = null,
    val notes: String? = null,
    val tags: String? = null,       // comma-separated tags e.g. "Rent,Office"
    val splitInfo: String? = null   // JSON-like: "name1:amount1,name2:amount2"
)

enum class TransactionType { DEBIT, CREDIT }
enum class TransactionSource { SMS, MANUAL }
enum class Category { FOOD, GROCERIES, TRAVEL, SHOPPING, BILLS, TRANSFER, HEALTH, ENTERTAINMENT, INVESTMENT, CASH, INCOME, REFUND, OTHER }

data class SplitEntry(val name: String, val amountPaise: Long)

fun parseSplitInfo(raw: String?): List<SplitEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(",").mapNotNull {
        val parts = it.split(":")
        if (parts.size == 2) SplitEntry(parts[0].trim(), parts[1].trim().toLongOrNull() ?: 0L)
        else null
    }
}
