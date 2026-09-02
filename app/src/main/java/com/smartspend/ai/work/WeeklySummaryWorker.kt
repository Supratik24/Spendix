package com.smartspend.ai.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartspend.ai.R
import com.smartspend.ai.data.SmartSpendDatabase
import com.smartspend.ai.data.TransactionType
import java.util.Calendar

class WeeklySummaryWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences("spendix_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enable_notifications", true)) {
            return Result.success()
        }

        try {
            val db = SmartSpendDatabase.get(appContext)
            val transactions = db.transactions().getAll()

            val now = System.currentTimeMillis()
            val oneWeekAgo = now - 7 * 24 * 60 * 60 * 1000L

            val weeklyTxns = transactions.filter { it.occurredAt in oneWeekAgo..now }
            
            val totalSpent = weeklyTxns.filter { it.type == TransactionType.DEBIT }.sumOf { it.amountPaise }
            val totalEarned = weeklyTxns.filter { it.type == TransactionType.CREDIT }.sumOf { it.amountPaise }

            val spentStr = "Rs." + (totalSpent.toDouble() / 100.0)
            val earnedStr = "Rs." + (totalEarned.toDouble() / 100.0)

            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "weekly_summary"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Weekly Summary",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Weekly spending and earning summaries"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(R.drawable.ic_notification_custom)
                .setContentTitle("Your Weekly Summary \uD83D\uDCCA")
                .setContentText("You spent $spentStr and earned $earnedStr this week.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}
