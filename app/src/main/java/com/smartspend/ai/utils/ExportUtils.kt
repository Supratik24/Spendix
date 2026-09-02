package com.smartspend.ai.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.smartspend.ai.data.Transaction
import com.smartspend.ai.data.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun exportTransactionsToPdf(context: Context, transactions: List<Transaction>, title: String) {
        val fileName = "Spendix_Statement_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        
        // Save directly to the public Downloads directory
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir.mkdirs()
        val file = File(downloadsDir, fileName)

        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842 // A4 size

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var yPosition = 50f
            
            // Header
            canvas.drawText(title, 50f, yPosition, paint)
            yPosition += 40f
            
            // Table Header
            paint.textSize = 14f
            canvas.drawText("Date", 50f, yPosition, paint)
            canvas.drawText("Merchant", 150f, yPosition, paint)
            canvas.drawText("Category", 350f, yPosition, paint)
            canvas.drawText("Amount", 480f, yPosition, paint)
            yPosition += 10f
            canvas.drawLine(50f, yPosition, 545f, yPosition, linePaint)
            yPosition += 20f

            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            
            var totalDebit = 0L
            var totalCredit = 0L

            for (tx in transactions) {
                if (tx.type == TransactionType.DEBIT) {
                    totalDebit += tx.amountPaise
                } else {
                    totalCredit += tx.amountPaise
                }

                if (yPosition > pageHeight - 50f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = 50f
                }

                val date = dateFormat.format(Date(tx.occurredAt))
                val merchant = (tx.merchant ?: "Unknown").take(25)
                val category = (tx.customCategory ?: tx.category.name.lowercase().replaceFirstChar { it.uppercase() }).take(15)
                val amountStr = String.format(Locale.getDefault(), "%.2f", tx.amountPaise / 100.0)
                
                textPaint.color = if (tx.type == TransactionType.DEBIT) Color.DKGRAY else Color.parseColor("#388E3C") // Green for credit
                val prefix = if (tx.type == TransactionType.DEBIT) "-₹" else "+₹"

                canvas.drawText(date, 50f, yPosition, textPaint)
                canvas.drawText(merchant, 150f, yPosition, textPaint)
                canvas.drawText(category, 350f, yPosition, textPaint)
                canvas.drawText(prefix + amountStr, 480f, yPosition, textPaint)
                
                yPosition += 20f
            }

            // Summary
            if (yPosition > pageHeight - 100f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }
            
            yPosition += 10f
            canvas.drawLine(50f, yPosition, 545f, yPosition, linePaint)
            yPosition += 30f
            
            paint.textSize = 16f
            canvas.drawText("Summary", 50f, yPosition, paint)
            yPosition += 20f
            
            textPaint.color = Color.DKGRAY
            canvas.drawText("Total Spend: ₹${String.format(Locale.getDefault(), "%.2f", totalDebit / 100.0)}", 50f, yPosition, textPaint)
            yPosition += 20f
            
            textPaint.color = Color.parseColor("#388E3C")
            canvas.drawText("Total Income: ₹${String.format(Locale.getDefault(), "%.2f", totalCredit / 100.0)}", 50f, yPosition, textPaint)

            pdfDocument.finishPage(page)

            val os = FileOutputStream(file)
            pdfDocument.writeTo(os)
            pdfDocument.close()
            os.close()

            // Success feedback
            android.widget.Toast.makeText(context, "Saved to Downloads", android.widget.Toast.LENGTH_SHORT).show()
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Statement"))

        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
