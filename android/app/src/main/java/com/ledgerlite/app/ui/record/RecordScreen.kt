package com.ledgerlite.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledgerlite.app.LedgerLiteApp
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.ui.components.AmountText
import com.ledgerlite.app.ui.components.CategoryIcon
import com.ledgerlite.app.ui.ledger.ExpenseEditSheet
import com.ledgerlite.app.ui.settings.CategoryManageScreen
import com.ledgerlite.app.util.DateUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordScreen(bottomInset: Dp = 0.dp) {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val vm: RecordViewModel = viewModel(
        factory = RecordViewModel.Factory(
            container.expenseRepository, container.categoryRepository, container.settingsRepository
        )
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showEntrySheet by remember { mutableStateOf(false) }
    var presetCategoryId by remember { mutableStateOf<Long?>(null) }
    var editing by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var pendingDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var showCategoryManage by remember { mutableStateOf(false) }

    RecordContent(
        state = state,
        bottomInset = bottomInset,
        onQuickEntry = { presetCategoryId = null; showEntrySheet = true },
        onCategoryClick = { id -> presetCategoryId = id; showEntrySheet = true },
        onRecentClick = { item -> editing = item },
        onOpenCategoryManage = { showCategoryManage = true }
    )

    if (showCategoryManage) {
        CategoryManageScreen(onBack = { showCategoryManage = false })
    }

    if (showEntrySheet) {
        QuickEntrySheet(
            categories = state.categories,
            onDismiss = { showEntrySheet = false },
            onSubmit = { amount, categoryId, note, occurredAt ->
                vm.createExpense(amount, categoryId, note, occurredAt)
            },
            presetCategoryId = presetCategoryId
        )
    }

    editing?.let { item ->
        ExpenseEditSheet(
            record = item.expense,
            categories = state.categories,
            onDismiss = { editing = null },
            onSave = { amount, categoryId, note, occurredAt ->
                vm.updateExpense(item.expense.copy(amount = amount, categoryId = categoryId, note = note, occurredAt = occurredAt))
                editing = null
            },
            onDelete = {
                pendingDelete = item
                editing = null
            }
        )
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除记录") },
            text = { Text("删除这条流水？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteExpense(item.expense.id)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun RecordContent(
    state: RecordUiState,
    bottomInset: Dp,
    onQuickEntry: () -> Unit,
    onCategoryClick: (Long) -> Unit,
    onRecentClick: (ExpenseWithCategory) -> Unit,
    onOpenCategoryManage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = 20.dp,
            bottom = 100.dp + bottomInset
        )
    ) {
        item { TodayCard(state.todayTotal) }
        item { Spacer(Modifier.height(8.dp)) }
        item { MonthCard(state.monthTotal) }

        if (state.categories.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "常用分类",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "管理",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onOpenCategoryManage)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            item { CategoryGrid(state.categories.take(8), onClick = onCategoryClick) }
        }

        item {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onQuickEntry,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("记一笔", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Text("最近记录", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
        }

        if (state.recentExpenses.isEmpty()) {
            item { EmptyRecent() }
        } else {
            items(state.recentExpenses, key = { it.expense.id }) { item ->
                RecentExpenseCard(item, onClick = { onRecentClick(item) })
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TodayCard(todayCents: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "今日支出",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            AmountText(
                cents = todayCents,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MonthCard(monthCents: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("本月支出", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(2.dp))
                AmountText(cents = monthCents, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CategoryGrid(categories: List<Category>, onClick: (Long) -> Unit) {
    // 每行 4 个分类色块
    val rows = categories.chunked(4)
    rows.forEach { row ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { category ->
                CategoryBlock(category, modifier = Modifier.weight(1f), onClick = { onClick(category.id) })
            }
            // 不足 4 个补占位
            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CategoryBlock(category: Category, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = Color(category.color)
    val iconVector = CategoryIcon.vector(category.icon)
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Box(modifier = Modifier.size(18.dp).background(accent, CircleShape))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}

@Composable
private fun EmptyRecent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("今天还没记账", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentExpenseCard(item: ExpenseWithCategory, onClick: () -> Unit = {}) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标 / 色点
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                val accent = item.categoryColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                val iconVector = item.categoryIcon?.let { CategoryIcon.vector(it) }
                Box(
                    modifier = Modifier.size(24.dp).background(accent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconVector != null) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Box(modifier = Modifier.size(10.dp).background(accent, CircleShape))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        item.categoryName ?: "未分类",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    val sub = buildString {
                        append(timeFormat.format(Date(item.expense.occurredAt)))
                        if (item.expense.note.isNotEmpty()) append("  ${item.expense.note}")
                    }
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AmountText(cents = item.expense.amount, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
