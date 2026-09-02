package com.smartspend.ai.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.data.TransactionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.OutputStream

object PdfGenerator {

    fun generateStatementForMonth(
        context: Context, 
        transactions: List<Transaction>, 
        year: Int, 
        month: Int, 
        outputStream: OutputStream
    ): Boolean {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()

        // --- Header ---
        paint.textSize = 24f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("Spendix Monthly Statement", 50f, 50f, paint)

        val monthDate = java.time.LocalDate.of(year, month, 1)
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        val monthStr = monthDate.format(monthFormatter)
        
        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("Period: $monthStr", 50f, 80f, paint)

        // Filter transactions for selected month
        val filteredTransactions = transactions.filter { txn ->
            val txDate = Instant.ofEpochMilli(txn.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate()
            txDate.year == year && txDate.monthValue == month
        }.sortedByDescending { it.occurredAt }
        
        val totalSpent = filteredTransactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amountPaise } / 100f

        paint.isFakeBoldText = true
        canvas.drawText("Total Spent: ₹%.2f".format(totalSpent), 400f, 80f, paint)

        // --- Table Header ---
        var yPosition = 120f
        paint.textSize = 12f
        paint.isFakeBoldText = true
        paint.color = Color.DKGRAY
        canvas.drawLine(50f, yPosition - 15f, 545f, yPosition - 15f, paint)
        
        canvas.drawText("Date", 50f, yPosition, paint)
        canvas.drawText("Merchant", 150f, yPosition, paint)
        canvas.drawText("Category", 350f, yPosition, paint)
        canvas.drawText("Amount", 480f, yPosition, paint)

        canvas.drawLine(50f, yPosition + 10f, 545f, yPosition + 10f, paint)
        yPosition += 30f

        // --- Table Rows ---
        paint.isFakeBoldText = false
        paint.color = Color.BLACK
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")

        for (txn in filteredTransactions) {
            // Check page overflow
            if (yPosition > 800f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }

            val dateStr = Instant.ofEpochMilli(txn.occurredAt).atZone(ZoneId.systemDefault()).format(dateFormatter)
            val merchantStr = (txn.merchant ?: txn.sender ?: "Unknown").take(25)
            val categoryStr = txn.category.name.take(15)
            val sign = if (txn.type == TransactionType.DEBIT) "-" else "+"
            val amountStr = "$sign₹%.2f".format(txn.amountPaise / 100f)

            canvas.drawText(dateStr, 50f, yPosition, paint)
            canvas.drawText(merchantStr, 150f, yPosition, paint)
            canvas.drawText(categoryStr, 350f, yPosition, paint)
            
            // Right align amount loosely by offsetting (basic implementation)
            canvas.drawText(amountStr, 480f, yPosition, paint)

            yPosition += 20f
        }

        if (filteredTransactions.isEmpty()) {
            canvas.drawText("No transactions for this month.", 50f, yPosition, paint)
        }

        pdfDocument.finishPage(page)

        // --- Save to File ---
        return try {
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            false
        }
    }
}
