package com.smartspend.ai.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

import android.provider.Telephony
import com.smartspend.ai.data.SmartSpendDatabase
import com.smartspend.ai.data.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) {
            pending.finish()
            return
        }
        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.displayMessageBody }
        val timestamp = messages[0].timestampMillis

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val txn = FinancialSmsParser.parse(sender, body, timestamp) ?: return@launch
                val repository = TransactionRepository(SmartSpendDatabase.get(context).transactions())

                val existingRefIds = repository.getAllSmsRefIds().toHashSet()
                val existingFingerprints = repository.getAllSmsFingerprints().toHashSet()

                // Check 1: Ref ID
                if (txn.refId != null && txn.refId in existingRefIds) return@launch
                
                // Check 2: Fingerprint
                if (txn.fingerprint in existingFingerprints) {
                    repository.updateSmsMetadata(txn.fingerprint, txn.rawBody, txn.refId)
                    return@launch
                }

                // Check 3: Account + Amount + Type + Day
                val tsDay = (timestamp / 86_400_000L) * 86_400_000L
                val acctSuffix = FinancialSmsParser.extractAccountSuffix(body)
                if (acctSuffix != null) {
                    val acctDayKeys = listOf(
                        "${acctSuffix}|${txn.amountPaise}|${txn.type}|$tsDay",
                        "${acctSuffix}|${txn.amountPaise}|${txn.type}|${tsDay - 86_400_000L}",
                        "${acctSuffix}|${txn.amountPaise}|${txn.type}|${tsDay + 86_400_000L}"
                    )
                    val existing = repository.getAll().filter { it.source == com.smartspend.ai.data.TransactionSource.SMS }
                    val existingKeys = existing.mapNotNull { eTxn ->
                        val suf = FinancialSmsParser.extractAccountSuffix(eTxn.rawBody ?: "") ?: return@mapNotNull null
                        val eDay = (eTxn.occurredAt / 86_400_000L) * 86_400_000L
                        "${suf}|${eTxn.amountPaise}|${eTxn.type}|$eDay"
                    }.toSet()
                    
                    if (acctDayKeys.any { it in existingKeys }) return@launch
                }

                repository.save(txn)
                
                val prefs = context.getSharedPreferences("spendix_prefs", Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean("enable_notifications", true)
                if (notificationsEnabled) {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channelId = "transaction_alerts"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val channel = NotificationChannel(channelId, "Transaction Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                            description = "Notifications for new credit and debit transactions"
                        }
                        notificationManager.createNotificationChannel(channel)
                    }

                    val typeStr = if (txn.type == com.smartspend.ai.data.TransactionType.CREDIT) "Credit" else "Debit"
                    val amountStr = "Rs." + (txn.amountPaise / 100.0).toString()
                    val notification = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(com.smartspend.ai.R.drawable.ic_notification_custom)
                        .setContentTitle("New $typeStr Transaction")
                        .setContentText("$typeStr of $amountStr recorded from ${txn.sender}")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()

                    notificationManager.notify(txn.fingerprint.hashCode(), notification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pending.finish()
            }
        }
    }
}
