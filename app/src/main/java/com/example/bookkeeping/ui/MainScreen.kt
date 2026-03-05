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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bookkeeping.R
import com.example.bookkeeping.ui.components.ConflictAlertDialog
import com.example.bookkeeping.ui.report.ReportScreen
import com.example.bookkeeping.ui.settings.AutoImportScreen
import com.example.bookkeeping.ui.settings.BudgetManagementScreen
import com.example.bookkeeping.ui.settings.CategoryManagementScreen
import com.example.bookkeeping.ui.settings.GeneralSettingsScreen
import com.example.bookkeeping.ui.settings.SettingsScreen

/** 底部导航路由。 */
private object Route {
    const val TRANSACTIONS = "transactions"
    const val REPORT       = "report"
    const val SETTINGS     = "settings"
    const val ADD_TRANSACTION = "add_transaction"
    const val EDIT_TRANSACTION = "edit_transaction/{transactionId}"
    const val CATEGORY_MANAGEMENT = "category_management"
    const val BUDGET_MANAGEMENT = "budget_management"
    const val AUTO_IMPORT = "auto_import"
    const val GENERAL_SETTINGS = "general_settings"
    
    fun editTransaction(transactionId: String) = "edit_transaction/$transactionId"
}

/**
 * 带底部导航栏的主容器。
 * - 记账：交易列表
 * - 报表：周 / 月 / 年报
 * - 设置：数据导入、应用信息
 */
@Composable
fun MainScreen(
    viewModel: MainScreenViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // ▶ 监听冲突事件（来自 PaymentAutoImporter）
    val conflictAlertState by viewModel.conflictAlertFlow.collectAsState(null)
    // 解引用以避免委托属性 smart cast 问题
    val conflictAlert = conflictAlertState

    val tabs = listOf(
        Triple(Route.TRANSACTIONS, stringResource(R.string.nav_transactions),  Icons.AutoMirrored.Filled.List),
        Triple(Route.REPORT,       stringResource(R.string.nav_report),  Icons.AutoMirrored.Filled.ShowChart),
        Triple(Route.SETTINGS,     stringResource(R.string.nav_settings),  Icons.Default.Settings),
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
        // ▶ 根级别显示冲突对话框（全局可见）
        if (conflictAlert != null) {
            ConflictAlertDialog(
                conflictAlert = conflictAlert,
                onKeep = {
                    // 用户选择"保留"：保留旧记录，不导入新记录
                    viewModel.onConflictKeepExisting()
                },
                onOverwrite = {
                    // 用户选择"覆盖"：删除旧记录并导入新记录
                    viewModel.onConflictOverwrite(conflictAlert.existingTxId)
                },
                onCancel = {
                    // 用户选择"取消"：放弃操作
                    viewModel.onConflictCancel()
                }
            )
        }

        NavHost(
            navController    = navController,
            startDestination = Route.TRANSACTIONS,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(Route.TRANSACTIONS) { 
                TransactionListScreen(
                    onAddClick = {
                        navController.navigate(Route.ADD_TRANSACTION)
                    },
                    onEditClick = { transactionId ->
                        navController.navigate(Route.editTransaction(transactionId))
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
                    onBudgetManage = {
                        navController.navigate(Route.BUDGET_MANAGEMENT)
                    },
                    onAutoImport = {
                        navController.navigate(Route.AUTO_IMPORT)
                    },
                    onGeneralSettings = {
                        navController.navigate(Route.GENERAL_SETTINGS)
                    },
                )
            }
            composable(Route.AUTO_IMPORT) {
                AutoImportScreen(
                    onBack = { navController.popBackStack() },
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
            composable(Route.EDIT_TRANSACTION) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
                AddTransactionScreen(
                    transactionId = transactionId,
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
            composable(Route.BUDGET_MANAGEMENT) {
                BudgetManagementScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
            composable(Route.GENERAL_SETTINGS) {
                GeneralSettingsScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
