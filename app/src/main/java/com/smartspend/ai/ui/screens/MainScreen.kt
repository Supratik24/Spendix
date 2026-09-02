package com.smartspend.ai.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smartspend.ai.ui.model.MainTab
import com.smartspend.ai.ui.model.SpendUiState
import com.smartspend.ai.ui.viewmodel.SpendViewModel

import androidx.compose.foundation.layout.WindowInsets

@Composable
fun MainScreen(
    state: SpendUiState,
    viewModel: SpendViewModel,
    onRequestSmsPermission: () -> Unit,
    userName: String?,
    onOpenAccount: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Rounded.List, contentDescription = "Transactions") },
                    label = { Text("Activity") },
                    selected = state.currentTab == MainTab.TRANSACTIONS,
                    onClick = { viewModel.setTab(MainTab.TRANSACTIONS) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    selected = state.currentTab == MainTab.ANALYTICS,
                    onClick = { viewModel.setTab(MainTab.ANALYTICS) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.People, contentDescription = "Splits") },
                    label = { Text("Splits") },
                    selected = state.currentTab == MainTab.SPLITS,
                    onClick = { viewModel.setTab(MainTab.SPLITS) }
                )
            }
        }
    ) { innerPadding ->
        when (state.currentTab) {
            MainTab.TRANSACTIONS -> {
                DashboardScreen(
                    state = state,
                    viewModel = viewModel,
                    onRequestSmsPermission = onRequestSmsPermission,
                    userName = userName,
                    onOpenAccount = onOpenAccount,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainTab.ANALYTICS -> {
                AnalyticsScreen(
                    state = state,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainTab.SPLITS -> {
                SplitsScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
