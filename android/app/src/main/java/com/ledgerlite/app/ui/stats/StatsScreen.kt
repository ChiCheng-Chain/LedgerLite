package com.ledgerlite.app.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.ledgerlite.app.ui.components.AmountText
import com.ledgerlite.app.ui.components.charts.CategoryDonutChart
import com.ledgerlite.app.ui.components.charts.CategorySlice
import com.ledgerlite.app.ui.components.charts.ChartPoint
import com.ledgerlite.app.ui.components.charts.ComparisonBarChart
import com.ledgerlite.app.ui.components.charts.HeatmapCell
import com.ledgerlite.app.ui.components.charts.HeatmapChart
import com.ledgerlite.app.ui.components.charts.TrendLineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen() {
    val container = (LocalContext.current.applicationContext as LedgerLiteApp).container
    val vm: StatsViewModel = viewModel(
        factory = StatsViewModel.Factory(container.statisticsRepository, container.expenseRepository)
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val categories by container.categoryRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())

    var showCategoryMenu by remember { mutableStateOf(false) }
    var showCustomRange by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        // 筛选条
        FilterRow(
            state = state,
            categories = categories,
            onTimeFilter = { vm.setTimeFilter(it) },
            onCustomRange = { showCustomRange = true },
            onCategoryMenuToggle = { showCategoryMenu = true },
            showCategoryMenu = showCategoryMenu,
            onCategorySelect = { vm.setCategoryFilter(it); showCategoryMenu = false }
        )

        Spacer(Modifier.height(16.dp))

        // 总览指标卡
        SummaryCard(state = state)

        Spacer(Modifier.height(16.dp))

        // 1. 分类占比环形图 + 排行
        if (state.categoryShares.isNotEmpty()) {
            ChartCard(title = "分类占比", subtitle = state.rangeLabel) {
                val slices = state.categoryShares.map { sum ->
                    CategorySlice(
                        name = sum.categoryName ?: "未分类",
                        color = sum.categoryColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary,
                        value = sum.total
                    )
                }
                CategoryDonutChart(slices = slices, total = state.total)
            }
            Spacer(Modifier.height(16.dp))
        }

        // 2. 日支出趋势折线图（近 30 天连续，没支出的天填 0）
        val trendPoints = remember(state.trend50Days) {
            buildTrendPoints(state.trend50Days, days = 30)
        }
        ChartCard(title = "支出趋势", subtitle = "近 30 天") {
            TrendLineChart(points = trendPoints)
        }

        Spacer(Modifier.height(16.dp))

        // 3. 日支出热力图（最近 50 天，GitHub 风格铺满）
        ChartCard(title = "支出热力图", subtitle = "近 12 周") {
            val cells = state.trend50Days.map { day ->
                HeatmapCell(
                    label = SimpleDateFormat("M/d", Locale.getDefault()).format(Date(day.dayStart)),
                    value = day.total,
                    timestamp = day.dayStart
                )
            }
            HeatmapChart(cells = cells)
        }
        Spacer(Modifier.height(16.dp))

        // 6. 资产使用成本独立区
        if (state.bigItemDaily > 0 || state.bigItemWeekly > 0) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("资产使用成本", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
                    Text("非现金支出，与日常支出分开", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("日均", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            AmountText(cents = state.bigItemDaily, fontSize = 22.sp, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("周均", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            AmountText(cents = state.bigItemWeekly, fontSize = 22.sp, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showCustomRange) {
        CustomRangePicker(
            onDismiss = { showCustomRange = false },
            onConfirm = { s, e -> vm.setCustomRange(s, e); showCustomRange = false }
        )
    }
}

@Composable
private fun FilterRow(
    state: StatsUiState,
    categories: List<com.ledgerlite.app.data.local.entity.Category>,
    onTimeFilter: (StatsTimeFilter) -> Unit,
    onCustomRange: () -> Unit,
    onCategoryMenuToggle: () -> Unit,
    showCategoryMenu: Boolean,
    onCategorySelect: (Long?) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("MM-dd", Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        // 时间筛选
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(end = 12.dp)
        ) {
            items(listOf(StatsTimeFilter.WEEK, StatsTimeFilter.MONTH, StatsTimeFilter.CUSTOM)) { tf ->
                val label = when (tf) {
                    StatsTimeFilter.WEEK -> "本周"
                    StatsTimeFilter.MONTH -> "本月"
                    StatsTimeFilter.CUSTOM -> if (state.filter.time == StatsTimeFilter.CUSTOM && state.filter.customStart > 0)
                        "${dateFmt.format(Date(state.filter.customStart))}-${dateFmt.format(Date(state.filter.customEnd))}" else "自定义"
                    else -> ""
                }
                FilterChip(
                    selected = state.filter.time == tf,
                    onClick = {
                        if (tf == StatsTimeFilter.CUSTOM) onCustomRange() else onTimeFilter(tf)
                    },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // 分类筛选
        Box {
            AssistChip(
                onClick = onCategoryMenuToggle,
                label = { Text(state.filter.categoryId?.let { id -> categories.find { it.id == id }?.name ?: "分类" } ?: "全部分类") },
                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.padding(start = 4.dp)) }
            )
            DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { }) {
                DropdownMenuItem(text = { Text("全部分类") }, onClick = { onCategorySelect(null) })
                categories.forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.name) }, onClick = { onCategorySelect(cat.id) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: StatsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(state.rangeLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(4.dp))
            AmountText(cents = state.total, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("日均", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    AmountText(cents = state.dailyAvg, fontSize = 18.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                }
                state.topCategory?.let { top ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text("最多支出", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(top.categoryName ?: "未分类", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
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
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
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
                androidx.compose.material3.TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                androidx.compose.material3.Button(
                    onClick = {
                        val sel = dateState.selectedDateMillis
                        if (sel != null) {
                            if (pickingStart) { start = sel; pickingStart = false }
                            else {
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
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (pickingStart) "下一步" else "确定", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

/**
 * 把稀疏的 dailyTrend（仅有支出的天）补全成连续 N 天的折线点。
 * 没有支出的天填 0，保证折线连续不断点。
 */
private fun buildTrendPoints(
    dailyTrend: List<com.ledgerlite.app.data.local.relation.DaySum>,
    days: Int
): List<com.ledgerlite.app.ui.components.charts.ChartPoint> {
    val fmt = SimpleDateFormat("M/d", Locale.getDefault())
    val map = dailyTrend.associate { it.dayStart to it.total }
    val today = com.ledgerlite.app.util.DateUtil.startOfToday()
    val points = mutableListOf<com.ledgerlite.app.ui.components.charts.ChartPoint>()
    for (i in days - 1 downTo 0) {
        val dayStart = today - i * 86_400_000L
        points.add(
            com.ledgerlite.app.ui.components.charts.ChartPoint(
                label = fmt.format(Date(dayStart)),
                value = map[dayStart] ?: 0L
            )
        )
    }
    return points
}
