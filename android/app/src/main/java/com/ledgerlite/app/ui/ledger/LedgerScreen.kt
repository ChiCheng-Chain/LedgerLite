package com.ledgerlite.app.ui.ledger

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.ui.components.AmountText
import com.ledgerlite.app.ui.components.CategoryIcon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LedgerScreen() {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val vm: LedgerViewModel = viewModel(factory = LedgerViewModel.Factory(container.expenseRepository))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val categories by container.categoryRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())

    var editing by remember { mutableStateOf<com.ledgerlite.app.data.local.entity.ExpenseRecord?>(null) }
    var pendingDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showCustomRange by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部筛选条
        FilterBar(
            state = state,
            categories = categories,
            onTimeFilter = { vm.setTimeFilter(it) },
            onCustomRange = { showCustomRange = true },
            onCategoryMenuToggle = { showCategoryMenu = true },
            showCategoryMenu = showCategoryMenu,
            onCategorySelect = { id -> vm.setCategoryFilter(id); showCategoryMenu = false }
        )

        LedgerContent(
            state = state,
            onItemClick = { item -> editing = item.expense },
            onSwipeDelete = { item -> pendingDelete = item }
        )
    }

    // 自定义日期范围选择
    if (showCustomRange) {
        CustomRangePicker(
            onDismiss = { showCustomRange = false },
            onConfirm = { start, end ->
                vm.setCustomRange(start, end)
                showCustomRange = false
            }
        )
    }

    // 编辑面板
    editing?.let { record ->
        ExpenseEditSheet(
            record = record,
            categories = categories,
            onDismiss = { editing = null },
            onSave = { amount, categoryId, note, occurredAt ->
                vm.update(record.copy(amount = amount, categoryId = categoryId, note = note, occurredAt = occurredAt))
            },
            onDelete = {
                pendingDelete = ExpenseWithCategory(record, null, null, null)
                editing = null
            }
        )
    }

    // 删除二次确认（Material You 卡片风格）
    pendingDelete?.let { item ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { pendingDelete = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "删除这条记录？",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "删除后可在回收站恢复（后续版本）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = { pendingDelete = null },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Button(
                            onClick = {
                                vm.delete(item.expense.id)
                                pendingDelete = null
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("删除", color = MaterialTheme.colorScheme.onError) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    state: LedgerUiState,
    categories: List<com.ledgerlite.app.data.local.entity.Category>,
    onTimeFilter: (TimeFilter) -> Unit,
    onCustomRange: () -> Unit,
    onCategoryMenuToggle: () -> Unit,
    showCategoryMenu: Boolean,
    onCategorySelect: (Long?) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        // 时间筛选行
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 12.dp)
        ) {
            item {
                FilterChip(
                    selected = state.timeFilter == TimeFilter.TODAY,
                    onClick = { onTimeFilter(TimeFilter.TODAY) },
                    label = { Text("今天") }
                )
            }
            item {
                FilterChip(
                    selected = state.timeFilter == TimeFilter.WEEK,
                    onClick = { onTimeFilter(TimeFilter.WEEK) },
                    label = { Text("本周") }
                )
            }
            item {
                FilterChip(
                    selected = state.timeFilter == TimeFilter.MONTH,
                    onClick = { onTimeFilter(TimeFilter.MONTH) },
                    label = { Text("本月") }
                )
            }
            item {
                AssistChip(
                    onClick = onCustomRange,
                    label = {
                        Text(
                            if (state.timeFilter == TimeFilter.CUSTOM && state.customStart > 0)
                                "${dateFmt.format(Date(state.customStart))} - ${dateFmt.format(Date(state.customEnd))}"
                            else "自定义"
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(start = 4.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (state.timeFilter == TimeFilter.CUSTOM) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 分类筛选行
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = onCategoryMenuToggle,
                    label = { Text(state.selectedCategoryId?.let { id -> categories.find { it.id == id }?.name ?: "分类" } ?: "全部分类") }
                )
            }
            DropdownMenu(
                expanded = showCategoryMenu,
                onDismissRequest = { /* 由 onCategorySelect 控制 */ }
            ) {
                DropdownMenuItem(
                    text = { Text("全部分类") },
                    onClick = { onCategorySelect(null) }
                )
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name) },
                        onClick = { onCategorySelect(cat.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedgerContent(
    state: LedgerUiState,
    onItemClick: (ExpenseWithCategory) -> Unit,
    onSwipeDelete: (ExpenseWithCategory) -> Unit
) {
    val dayFormat = remember { SimpleDateFormat("M月d日 EEEE", Locale.CHINA) }

    if (state.groups.isEmpty() && !state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("该筛选下没有记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "total-summary") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        state.rangeLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    AmountText(
                        cents = state.totalAmount,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        showDecimals = false
                    )
                }
            }
        }

        state.groups.forEach { group ->
            item(key = "header-${group.groupStart}") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        group.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AmountText(
                        cents = group.dayTotal,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        showDecimals = false
                    )
                }
            }
            items(group.items, key = { it.expense.id }) { item ->
                SwipeToDismissRow(
                    item = item,
                    showDate = state.multiDayGroups,
                    onClick = { onItemClick(item) },
                    onSwiped = { onSwipeDelete(item) }
                )
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangePicker(
    onDismiss: () -> Unit,
    onConfirm: (start: Long, end: Long) -> Unit
) {
    var pickingStart by remember { mutableStateOf(true) }
    var start by remember { mutableStateOf<Long?>(null) }
    var end by remember { mutableStateOf<Long?>(null) }

    val dateState = rememberDatePickerState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(
                if (pickingStart) "选择开始日期" else "选择结束日期",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (pickingStart) "第一步：选择范围起始日" else "第二步：选择范围结束日",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            DatePicker(state = dateState, showModeToggle = false)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Button(
                    onClick = {
                        val sel = dateState.selectedDateMillis
                        if (sel != null) {
                            if (pickingStart) {
                                start = sel
                                pickingStart = false
                            } else {
                                end = sel
                                if (start != null && end != null) {
                                    val (s, e) = if (start!! <= end!!) start!! to end!! else end!! to start!!
                                    onConfirm(s, e)
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        if (pickingStart) "下一步" else "确定",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissRow(
    item: ExpenseWithCategory,
    showDate: Boolean,
    onClick: () -> Unit,
    onSwiped: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwiped()
                // 返回 false 让 box 自动回弹，删除走二次确认对话框，避免卡在偏移态
                false
            } else false
        },
        positionalThreshold = { distance -> distance * 0.6f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text("删除", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.categoryName ?: "未分类",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.expense.note.isNotEmpty()) {
                        Text(
                            item.expense.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AmountText(cents = item.expense.amount, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
