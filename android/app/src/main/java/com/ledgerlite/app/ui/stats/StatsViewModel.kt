package com.ledgerlite.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ledgerlite.app.data.local.relation.CategorySum
import com.ledgerlite.app.data.repository.ExpenseRepository
import com.ledgerlite.app.data.repository.StatisticsRepository
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class StatsTimeFilter { MONTH, WEEK, LAST_MONTH, LAST_30_DAYS, LAST_90_DAYS, CUSTOM }

data class StatsFilter(
    val time: StatsTimeFilter = StatsTimeFilter.MONTH,
    val customStart: Long = 0,
    val customEnd: Long = 0,
    val categoryId: Long? = null
)

data class StatsUiState(
    val filter: StatsFilter = StatsFilter(),
    val rangeLabel: String = "",
    val total: Long = 0,
    val dailyAvg: Long = 0,
    val topCategory: CategorySum? = null,
    val categoryShares: List<CategorySum> = emptyList(),
    val dailyTrend: List<com.ledgerlite.app.data.local.relation.DaySum> = emptyList(),
    val trend30Days: List<com.ledgerlite.app.data.local.relation.DaySum> = emptyList(),
    val bigItemDaily: Long = 0,
    val bigItemWeekly: Long = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val statisticsRepository: StatisticsRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(StatsFilter())

    val uiState: StateFlow<StatsUiState> =
        _filter.flatMapLatest { filter ->
            val (start, end, label) = timeRange(filter)
            val categoryFlow = if (filter.categoryId != null) {
                expenseRepository.sumGroupByCategoryFiltered(start, end, filter.categoryId)
            } else {
                expenseRepository.sumGroupByCategory(start, end)
            }
            // 主数据：4 路 combine（含近 28 天日数据用于趋势图）
            val mainFlow = combine(
                categoryFlow,
                expenseRepository.observeTotalInRange(start, end),
                expenseRepository.sumGroupByDay(start, end),
                expenseRepository.sumGroupByDay(DateUtil.startDaysAgo(29), DateUtil.startOfNextDay())
            ) { shares, total, daily, last30Days ->
                StatsPartial(shares, total, daily, last30Days, label, start, end)
            }
            // 再 combine 资产成本
            combine(
                mainFlow,
                statisticsRepository.observeBigItemCostSummary()
            ) { partial, bigItem ->
                val days = ((partial.end - partial.start) / 86_400_000L).coerceAtLeast(1)
                StatsUiState(
                    filter = filter,
                    rangeLabel = partial.label,
                    total = partial.total,
                    dailyAvg = if (partial.total > 0) partial.total / days else 0,
                    topCategory = partial.shares.firstOrNull(),
                    categoryShares = partial.shares,
                    dailyTrend = partial.daily,
                    trend30Days = partial.last30Days,
                    bigItemDaily = bigItem.totalDaily,
                    bigItemWeekly = bigItem.totalWeekly,
                    isLoading = false
                )
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())

    private data class StatsPartial(
        val shares: List<CategorySum>,
        val total: Long,
        val daily: List<com.ledgerlite.app.data.local.relation.DaySum>,
        val last30Days: List<com.ledgerlite.app.data.local.relation.DaySum>,
        val label: String,
        val start: Long,
        val end: Long
    )

    private fun timeRange(filter: StatsFilter): Triple<Long, Long, String> {
        val fmt = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
        val fmtMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        return when (filter.time) {
            StatsTimeFilter.MONTH -> {
                val s = DateUtil.startOfMonth()
                val e = DateUtil.startOfNextMonth()
                Triple(s, e, "本月 ${fmtMonth.format(java.util.Date(s))}")
            }
            StatsTimeFilter.WEEK -> {
                val s = DateUtil.startOfWeek()
                val e = DateUtil.startOfNextDay()
                Triple(s, e, "本周")
            }
            StatsTimeFilter.LAST_MONTH -> {
                val thisMonthStart = DateUtil.startOfMonth()
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = thisMonthStart; add(java.util.Calendar.MONTH, -1) }
                val s = cal.timeInMillis
                Triple(s, thisMonthStart, "上月 ${fmtMonth.format(java.util.Date(s))}")
            }
            StatsTimeFilter.LAST_30_DAYS -> {
                val e = DateUtil.startOfNextDay()
                val s = DateUtil.startDaysAgo(29)
                Triple(s, e, "近 30 天")
            }
            StatsTimeFilter.LAST_90_DAYS -> {
                val e = DateUtil.startOfNextDay()
                val s = DateUtil.startDaysAgo(89)
                Triple(s, e, "近 90 天")
            }
            StatsTimeFilter.CUSTOM -> {
                if (filter.customStart > 0 && filter.customEnd > 0) {
                    Triple(filter.customStart, filter.customEnd + 86_400_000L, "${fmt.format(java.util.Date(filter.customStart))} - ${fmt.format(java.util.Date(filter.customEnd))}")
                } else {
                    val s = DateUtil.startOfMonth(); val e = DateUtil.startOfNextMonth()
                    Triple(s, e, "本月")
                }
            }
        }
    }


    fun setTimeFilter(time: StatsTimeFilter) {
        _filter.value = _filter.value.copy(time = time)
    }

    fun setCustomRange(start: Long, end: Long) {
        _filter.value = _filter.value.copy(time = StatsTimeFilter.CUSTOM, customStart = start, customEnd = end)
    }

    fun setCategoryFilter(categoryId: Long?) {
        _filter.value = _filter.value.copy(categoryId = categoryId)
    }

    class Factory(
        private val statisticsRepository: StatisticsRepository,
        private val expenseRepository: ExpenseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StatsViewModel(statisticsRepository, expenseRepository) as T
    }
}
