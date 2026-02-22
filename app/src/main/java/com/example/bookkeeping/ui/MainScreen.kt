package com.example.bookkeeping.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bookkeeping.ui.report.ReportScreen
import com.example.bookkeeping.ui.settings.BudgetManagementScreen
import com.example.bookkeeping.ui.settings.CategoryManagementScreen
import com.example.bookkeeping.ui.settings.FeatureManagementScreen
import com.example.bookkeeping.ui.settings.SettingsScreen

/** 底部导航路由。 */
private object Route {
    const val TRANSACTIONS = "transactions"
    const val REPORT       = "report"
    const val SETTINGS     = "settings"
    const val ADD_TRANSACTION = "add_transaction"
    const val CATEGORY_MANAGEMENT = "category_management"
    const val FEATURE_MANAGEMENT = "feature_management"
    const val BUDGET_MANAGEMENT = "budget_management"
}

/**
 * 带底部导航栏的主容器。
 * - 记账：交易列表
 * - 报表：周 / 月 / 年报
 * - 设置：数据导入、应用信息
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        Triple(Route.TRANSACTIONS, "记账",  Icons.AutoMirrored.Filled.List),
        Triple(Route.REPORT,       "报表",  Icons.AutoMirrored.Filled.ShowChart),
        Triple(Route.SETTINGS,     "设置",  Icons.Default.Settings),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        icon     = { Icon(icon, contentDescription = label) },
                        label    = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                        onClick  = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Route.TRANSACTIONS,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(Route.TRANSACTIONS) { 
                TransactionListScreen(
                    onAddClick = {
                        navController.navigate(Route.ADD_TRANSACTION)
                    }
                )
            }
            composable(Route.REPORT)       { ReportScreen() }
            composable(Route.SETTINGS)     { 
                SettingsScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onCategoryManage = {
                        navController.navigate(Route.CATEGORY_MANAGEMENT)
                    },
                    onFeatureManage = {
                        navController.navigate(Route.FEATURE_MANAGEMENT)
                    },
                    onBudgetManage = {
                        navController.navigate(Route.BUDGET_MANAGEMENT)
                    },
                )
            }
            composable(Route.ADD_TRANSACTION) {
                AddTransactionScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onSuccess = {
                        navController.popBackStack()
                    },
                )
            }
            composable(Route.CATEGORY_MANAGEMENT) {
                CategoryManagementScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
            composable(Route.FEATURE_MANAGEMENT) {
                FeatureManagementScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
            composable(Route.BUDGET_MANAGEMENT) {
                BudgetManagementScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
