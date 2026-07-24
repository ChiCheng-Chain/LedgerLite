package com.ledgerlite.app.data.repository

import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.data.local.relation.CategorySum
import com.ledgerlite.app.data.local.relation.DaySum
import com.ledgerlite.app.util.AmortizationUtil
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 只读聚合，跨 Expense + BigItem。不直接写库。
 * 窗口参数由调用方传入（或用默认本月/今日），保持与 Settings 解耦。
 */
class StatisticsRepository(
    private val expenseRepository: ExpenseRepository,
    private val bigItemRepository: BigItemRepository
) {

    fun observeMonthTotal(): Flow<Long> = expenseRepository.observeMonthTotal()

    fun observeMonthCategoryShare(): Flow<List<CategorySum>> {
        val start = DateUtil.startOfMonth()
        val end = DateUtil.startOfNextMonth()
        return expenseRepository.sumGroupByCategory(start, end)
    }

    fun observeDailyTrend(days: Int): Flow<List<DaySum>> {
        val end = DateUtil.startOfNextDay()
        val start = DateUtil.startDaysAgo(days - 1)
        return expenseRepository.sumGroupByDay(start, end)
    }

    /** 资产使用成本汇总：active 资产的日均/周均总成本。 */
    data class BigItemCostSummary(
        val activeItems: List<BigItem>,
        val totalDaily: Long,
        val totalWeekly: Long
    )

    fun observeBigItemCostSummary(): Flow<BigItemCostSummary> =
        bigItemRepository.observeActive().map { items ->
            BigItemCostSummary(
                activeItems = items,
                totalDaily = AmortizationUtil.totalDailyCost(items),
                totalWeekly = AmortizationUtil.totalWeeklyCost(items)
            )
        }
}
