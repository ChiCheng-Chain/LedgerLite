package com.ledgerlite.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ledgerlite.app.ui.bigitem.BigItemDetailScreen
import com.ledgerlite.app.ui.bigitem.BigItemEditScreen
import com.ledgerlite.app.ui.bigitem.BigItemScreen
import com.ledgerlite.app.ui.ledger.LedgerScreen
import com.ledgerlite.app.ui.record.RecordScreen
import com.ledgerlite.app.ui.settings.CategoryManageScreen
import com.ledgerlite.app.ui.settings.SettingsScreen
import com.ledgerlite.app.ui.stats.StatsScreen

private sealed class Tab(val route: String, val label: String, val icon: ImageVector)
private data object RecordTab : Tab("record", "记账", Icons.Outlined.Edit)
private data object LedgerTab : Tab("ledger", "流水", Icons.Outlined.Receipt)
private data object BigItemTab : Tab("bigitem", "资产", Icons.Outlined.Category)
private data object StatsTab : Tab("stats", "统计", Icons.Outlined.BarChart)

private val tabs = listOf(RecordTab, LedgerTab, BigItemTab, StatsTab)

object Routes {
    const val SETTINGS = "settings"
    const val CATEGORY_MANAGE = "category_manage"
    /** 一级 Tab 路由，供「默认首页」设置校验取值。 */
    val TAB_ROUTES = setOf("record", "ledger", "bigitem", "stats")
    const val BIG_ITEM_EDIT = "big_item_edit?itemId={itemId}"
    const val BIG_ITEM_DETAIL = "big_item_detail/{itemId}"
    fun bigItemEdit(itemId: Long? = null): String {
        val id = itemId?.takeIf { it > 0 } ?: -1L
        return "big_item_edit?itemId=$id"
    }
    fun bigItemDetail(itemId: Long): String = "big_item_detail/$itemId"
}

@Composable
fun AppRoot(startTabRoute: String = RecordTab.route) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // 仅在一级 Tab 显示底部栏
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startTabRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(RecordTab.route) {
                RecordTabScaffold(
                    innerPadding = innerPadding,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(LedgerTab.route) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    LedgerScreen()
                }
            }
            composable(BigItemTab.route) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    BigItemScreen(
                        onAdd = { navController.navigate(Routes.bigItemEdit()) },
                        onItemClick = { id -> navController.navigate(Routes.bigItemDetail(id)) }
                    )
                }
            }
            composable(StatsTab.route) {
                Box(modifier = Modifier.padding(innerPadding)) {
                    StatsScreen()
                }
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCategoryManage = { navController.navigate(Routes.CATEGORY_MANAGE) }
                )
            }
            composable(Routes.CATEGORY_MANAGE) {
                CategoryManageScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.BIG_ITEM_EDIT,
                arguments = listOf(navArgument("itemId") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("itemId") ?: -1L
                BigItemEditScreen(
                    itemId = id.takeIf { it > 0 },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.BIG_ITEM_DETAIL,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("itemId") ?: -1L
                BigItemDetailScreen(
                    itemId = id,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.bigItemEdit(it)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordTabScaffold(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onOpenSettings: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("LedgerLite") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            RecordScreen(bottomInset = innerPadding.calculateBottomPadding())
        }
    }
}
