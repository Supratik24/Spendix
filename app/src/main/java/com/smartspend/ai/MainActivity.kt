package com.smartspend.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.smartspend.ai.work.WeeklySummaryWorker

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartspend.ai.data.SmartSpendDatabase
import com.smartspend.ai.data.TransactionRepository
import com.smartspend.ai.ui.screens.MainScreen
import com.smartspend.ai.ui.screens.DashboardScreen
import com.smartspend.ai.ui.theme.SmartSpendTheme
import com.smartspend.ai.ui.viewmodel.SpendViewModel
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.os.Build

class MainActivity : ComponentActivity() {

    private val viewModel: SpendViewModel by viewModels {
        val database = SmartSpendDatabase.get(applicationContext)
        val repository = TransactionRepository(database.transactions())
        com.smartspend.ai.ui.viewmodel.SpendViewModel.factory(repository, getSharedPreferences("spendix_prefs", android.content.Context.MODE_PRIVATE))
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            viewModel.importSms(this)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        enableEdgeToEdge()
        
        val weeklyWorkRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WeeklySummaryNotification",
            ExistingPeriodicWorkPolicy.KEEP,
            weeklyWorkRequest
        )

        val prefs = getSharedPreferences("spendix_prefs", android.content.Context.MODE_PRIVATE)

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = state.isDarkMode ?: isSystemInDarkTheme()

            var showSplash by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
            var isAuthenticated by androidx.compose.runtime.remember { 
                androidx.compose.runtime.mutableStateOf(prefs.getBoolean("is_authenticated", false)) 
            }
            var showAccountScreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
            var userName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(prefs.getString("user_name", null)) }

            SmartSpendTheme(darkTheme = darkTheme) {
                if (showSplash) {
                    com.smartspend.ai.ui.screens.SplashScreen(
                        onSplashFinished = { showSplash = false }
                    )
                } else if (!isAuthenticated) {
                    com.smartspend.ai.ui.screens.LoginScreen(
                        onLoginSuccess = { email, name ->
                            prefs.edit()
                                .putBoolean("is_authenticated", true)
                                .putString("user_email", email)
                                .putString("user_name", name)
                                .apply()
                            userName = name
                            isAuthenticated = true

                            // Send Welcome Notification
                            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as NotificationManager
                            val channelId = "welcome_channel"
                            
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val channel = NotificationChannel(
                                    channelId,
                                    "Welcome",
                                    NotificationManager.IMPORTANCE_DEFAULT
                                )
                                notificationManager.createNotificationChannel(channel)
                            }

                            val notification = NotificationCompat.Builder(this@MainActivity, channelId)
                                .setSmallIcon(R.drawable.ic_notification_custom)
                                .setContentTitle("Welcome to Spendix! 🎉")
                                .setContentText("Hi ${name ?: "User"}, you're signed in successfully. Let's start tracking your expenses!")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setAutoCancel(true)
                                .build()

                            notificationManager.notify(1002, notification)
                        }
                    )
                } else if (state.selectedTransaction != null) {
                    com.smartspend.ai.ui.screens.TransactionDetailsScreen(
                        transaction = state.selectedTransaction!!,
                        viewModel = viewModel,
                        onBack = { viewModel.selectTransaction(null) }
                    )
                } else if (showAccountScreen) {
                    com.smartspend.ai.ui.screens.AccountScreen(
                        transactions = state.transactions,
                        userName = userName,
                        userEmail = prefs.getString("user_email", null),
                        isDarkMode = state.isDarkMode,
                        onToggleTheme = viewModel::toggleTheme,
                        onNameChanged = { newName ->
                            prefs.edit().putString("user_name", newName).apply()
                            userName = newName
                        },
                        onRescanSms = { 
                            android.widget.Toast.makeText(this@MainActivity, "Syncing transactions from SMS...", android.widget.Toast.LENGTH_SHORT).show()
                            viewModel.importSms(this@MainActivity)
                        },
                        onBack = { showAccountScreen = false },
                        onLogout = {
                            prefs.edit().putBoolean("is_authenticated", false).apply()
                            isAuthenticated = false
                            showAccountScreen = false
                        }
                    )
                } else {
                    MainScreen(
                        state = state,
                        viewModel = viewModel,
                        onRequestSmsPermission = ::requestSmsAccess,
                        userName = userName,
                        onOpenAccount = { showAccountScreen = true }
                    )
                }
            }
        }
    }

    private fun requestSmsAccess() {
        val permissions = mutableListOf(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}
