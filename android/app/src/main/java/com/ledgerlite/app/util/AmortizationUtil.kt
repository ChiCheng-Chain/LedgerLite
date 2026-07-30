package com.ledgerlite.app.util

import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.domain.model.BigItemStatus

/**
 * 资产摊销计算。纯函数，可单测。
 * 使用天数按实际日历：
 * - active（使用中）：今天 - startDate + 1（含首尾，以本地时区 0 点计）
 * - ended（已结束）：endedAt - startDate + 1
 * 日均 = amount / 天数（整数除法向下取整）。
 * 周均 = 总额 / 已用周数（向上取整，不足 1 周算 1 周）：用了 1 天或 7 天都是 1 周，
 *   用 8 天是 2 周。这样反映「平均每周分摊多少」，而非把单日成本外推到整周。
 */
object AmortizationUtil {

    /** 资产总使用天数（含首尾）。active 用今天，ended 用 endedAt。 */
    fun totalDays(item: BigItem, now: Long = DateUtil.nowMillis()): Long {
        val start = DateUtil.startOfDay(item.startDate)
        val end = when (item.status) {
            BigItemStatus.active -> DateUtil.startOfToday()
            BigItemStatus.ended -> DateUtil.startOfDay(item.endedAt ?: item.startDate)
        }
        val days = (end - start) / 86_400_000L + 1
        return days.coerceAtLeast(1)
    }

    /** 日均成本（分），整数除法向下取整。 */
    fun dailyCost(item: BigItem, now: Long = DateUtil.nowMillis()): Long {
        val days = totalDays(item, now)
        if (days <= 0) return 0
        return item.amount / days
    }

    /** 已用周数（向上取整，不足 1 周算 1 周）。 */
    fun totalWeeks(item: BigItem, now: Long = DateUtil.nowMillis()): Long {
        val days = totalDays(item, now)
        return (days + 6) / 7
    }

    /** 周均成本（分）= 总额 / 已用周数（向上取整）。 */
    fun weeklyCost(item: BigItem, now: Long = DateUtil.nowMillis()): Long {
        val weeks = totalWeeks(item, now)
        if (weeks <= 0) return 0
        return item.amount / weeks
    }

    /** 是否计入当前使用成本（仅 active）。 */
    fun isActive(item: BigItem): Boolean = item.status == BigItemStatus.active

    /** 一组资产的日均总成本（只汇总 active）。 */
    fun totalDailyCost(items: List<BigItem>, now: Long = DateUtil.nowMillis()): Long =
        items.filter { isActive(it) }.sumOf { dailyCost(it, now) }

    /** 一组资产的周均总成本（只汇总 active）。 */
    fun totalWeeklyCost(items: List<BigItem>, now: Long = DateUtil.nowMillis()): Long =
        items.filter { isActive(it) }.sumOf { weeklyCost(it, now) }
}
