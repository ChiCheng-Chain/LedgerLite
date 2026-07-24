package com.ledgerlite.app.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.data.repository.ExpenseRepository
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** 一天的流水分组。 */
data class DayGroup(
    val dayStart: Long,
    val items: List<ExpenseWithCategory>,
    val dayTotal: Long
)

data class LedgerUiState(
    val groups: List<DayGroup> = emptyList(),
    val timeFilter: TimeFilter = TimeFilter.TODAY,
    val customStart: Long = 0,
    val customEnd: Long = 0,
    val selectedCategoryId: Long? = null, // null = 全部分类
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModel(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _timeFilter = MutableStateFlow(TimeFilter.TODAY)
    private val _customRange = MutableStateFlow(0L to 0L)
    private val _categoryId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<LedgerUiState> =
        combine(_timeFilter, _customRange, _categoryId) { filter, custom, catId ->
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
                    groups = groupByDay(items),
                    timeFilter = filter,
                    customStart = custom.first,
                    customEnd = custom.second,
                    selectedCategoryId = catId,
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

    fun delete(id: Long) {
        viewModelScope.launch { expenseRepository.softDelete(id) }
    }

    fun update(record: com.ledgerlite.app.data.local.entity.ExpenseRecord) {
        viewModelScope.launch { expenseRepository.update(record) }
    }

    suspend fun getById(id: Long) = expenseRepository.getById(id)

    /** 按本地日分组，同日内按时间倒序。 */
    private fun groupByDay(items: List<ExpenseWithCategory>): List<DayGroup> {
        return items
            .groupBy { DateUtil.startOfDay(it.expense.occurredAt) }
            .map { (dayStart, dayItems) ->
                DayGroup(
                    dayStart = dayStart,
                    items = dayItems.sortedByDescending { it.expense.occurredAt },
                    dayTotal = dayItems.sumOf { it.expense.amount }
                )
            }
            .sortedByDescending { it.dayStart }
    }

    class Factory(private val expenseRepository: ExpenseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LedgerViewModel(expenseRepository) as T
    }
}
