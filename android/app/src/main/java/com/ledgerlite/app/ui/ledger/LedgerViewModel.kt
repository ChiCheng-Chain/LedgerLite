package com.ledgerlite.app.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.data.repository.ExpenseRepository
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 时间筛选类型。 */
enum class TimeFilter { TODAY, YESTERDAY, WEEK, MONTH, CUSTOM }

/** 流水分组。分组粒度由筛选口径决定：日/周/月/自定义。 */
data class DayGroup(
    val groupStart: Long,
    val label: String,
    val items: List<ExpenseWithCategory>,
    val dayTotal: Long
)

data class LedgerUiState(
    val groups: List<DayGroup> = emptyList(),
    val timeFilter: TimeFilter = TimeFilter.TODAY,
    val customStart: Long = 0,
    val customEnd: Long = 0,
    val selectedCategoryId: Long? = null, // null = 全部分类
    val totalAmount: Long = 0,
    val rangeLabel: String = "",
    val multiDayGroups: Boolean = false,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(
    private val expenseRepository: ExpenseRepository,
    /** 跨 0 点重发当前日，驱动「今日/昨日/本周」等相对窗口跨天重算。测试可注入有限流。 */
    private val dayStartFlow: Flow<Long> = DateUtil.observeDayStart()
) : ViewModel() {

    private val _timeFilter = MutableStateFlow(TimeFilter.TODAY)
    private val _customRange = MutableStateFlow(0L to 0L)
    private val _categoryId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<LedgerUiState> =
        combine(_timeFilter, _customRange, _categoryId, dayStartFlow) { filter, custom, catId, _ ->
            Triple(filter, custom, catId)
        }.flatMapLatest { (filter, custom, catId) ->
            val (start, end) = timeRange(filter, custom)
            val flow = if (catId != null) {
                expenseRepository.observeByDateRangeWithCategory(start, end, catId)
            } else {
                expenseRepository.observeByDateRange(start, end)
            }
            flow.map { items ->
                LedgerUiState(
                    groups = groupByRange(items, filter, custom),
                    timeFilter = filter,
                    customStart = custom.first,
                    customEnd = custom.second,
                    selectedCategoryId = catId,
                    totalAmount = items.sumOf { it.expense.amount },
                    rangeLabel = rangeLabel(filter),
                    multiDayGroups = false,
                    isLoading = false
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, LedgerUiState())

    /** 根据 TimeFilter 算出 [start, end) 时间范围。 */
    private fun timeRange(filter: TimeFilter, custom: Pair<Long, Long>): Pair<Long, Long> = when (filter) {
        TimeFilter.TODAY -> DateUtil.startOfToday() to DateUtil.startOfNextDay()
        TimeFilter.YESTERDAY -> {
            val today = DateUtil.startOfToday()
            (today - 86_400_000L) to today
        }
        TimeFilter.WEEK -> DateUtil.startOfWeek() to DateUtil.startOfNextDay()
        TimeFilter.MONTH -> DateUtil.startOfMonth() to DateUtil.startOfNextMonth()
        TimeFilter.CUSTOM -> {
            if (custom.first > 0 && custom.second > 0) {
                custom.first to (custom.second + 86_400_000L) // customEnd 为所选末日 0 点，开区间到次日
            } else {
                DateUtil.startOfToday() to DateUtil.startOfNextDay()
            }
        }
    }

    fun setTimeFilter(filter: TimeFilter) { _timeFilter.value = filter }

    fun setCustomRange(start: Long, end: Long) {
        _customRange.value = start to end
        _timeFilter.value = TimeFilter.CUSTOM
    }

    fun setCategoryFilter(categoryId: Long?) { _categoryId.value = categoryId }

    /** 顶部总合计卡片的标题。 */
    private fun rangeLabel(filter: TimeFilter): String = when (filter) {
        TimeFilter.TODAY -> "今日合计"
        TimeFilter.YESTERDAY -> "昨日合计"
        TimeFilter.WEEK -> "本周合计"
        TimeFilter.MONTH -> "本月合计"
        TimeFilter.CUSTOM -> "所选范围合计"
    }

    fun delete(id: Long) {
        viewModelScope.launch { expenseRepository.softDelete(id) }
    }

    fun update(record: com.ledgerlite.app.data.local.entity.ExpenseRecord) {
        viewModelScope.launch { expenseRepository.update(record) }
    }

    suspend fun getById(id: Long) = expenseRepository.getById(id)

    /** 按本地日分组，同日内按时间倒序。header 标签为「M月d日 星期几」。 */
    private fun groupByRange(
        items: List<ExpenseWithCategory>,
        filter: TimeFilter,
        @Suppress("UNUSED_PARAMETER") custom: Pair<Long, Long>
    ): List<DayGroup> {
        val dayFmt = java.text.SimpleDateFormat("M月d日 EEEE", java.util.Locale.CHINA)
        return items
            .groupBy { DateUtil.startOfDay(it.expense.occurredAt) }
            .map { (dayStart, dayItems) ->
                DayGroup(
                    groupStart = dayStart,
                    label = dayFmt.format(java.util.Date(dayStart)),
                    items = dayItems.sortedByDescending { it.expense.occurredAt },
                    dayTotal = dayItems.sumOf { it.expense.amount }
                )
            }
            .sortedByDescending { it.groupStart }
    }

    class Factory(private val expenseRepository: ExpenseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LedgerViewModel(expenseRepository) as T
    }
}
