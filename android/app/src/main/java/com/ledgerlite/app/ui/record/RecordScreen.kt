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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledgerlite.app.LedgerLiteApp
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.ui.components.AmountText
import com.ledgerlite.app.util.DateUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordScreen() {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val vm: RecordViewModel = viewModel(
        factory = RecordViewModel.Factory(container.expenseRepository, container.categoryRepository)
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showEntrySheet by remember { mutableStateOf(false) }
    var presetCategoryId by remember { mutableStateOf<Long?>(null) }

    RecordContent(
        state = state,
        onQuickEntry = { presetCategoryId = null; showEntrySheet = true },
        onCategoryClick = { id -> presetCategoryId = id; showEntrySheet = true },
        onRecentClick = { /* 后续接流水编辑，MVP 先空 */ }
    )

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
}

@Composable
private fun RecordContent(
    state: RecordUiState,
    onQuickEntry: () -> Unit,
    onCategoryClick: (Long) -> Unit,
    onRecentClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))

        // 今日支出主卡片（主色容器大色块）
        TodayCard(state.todayTotal)

        Spacer(Modifier.height(12.dp))

        // 本月支出 次级卡片
        MonthCard(state.monthTotal)

        Spacer(Modifier.height(20.dp))

        // 常用分类（4 列网格色块）
        if (state.categories.isNotEmpty()) {
            Text(
                "常用分类",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            CategoryGrid(state.categories.take(8), onClick = onCategoryClick)
            Spacer(Modifier.height(20.dp))
        }

        // 记一笔主按钮
        Button(
            onClick = onQuickEntry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(8.dp))
            Text("记一笔", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))

        // 最近记录
        Text("最近记录", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        if (state.recentExpenses.isEmpty()) {
            EmptyRecent()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.recentExpenses, key = { it.expense.id }) { item ->
                    RecentExpenseCard(item)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
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
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "今日支出",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            AmountText(
                cents = todayCents,
                fontSize = 40.sp,
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
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
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun CategoryBlock(category: Category, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val accent = Color(category.color)
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(accent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(20.dp).background(accent, CircleShape))
        }
        Spacer(Modifier.height(6.dp))
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
private fun RecentExpenseCard(item: ExpenseWithCategory) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类色点
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(10.dp).background(
                        item.categoryColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
                )
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
