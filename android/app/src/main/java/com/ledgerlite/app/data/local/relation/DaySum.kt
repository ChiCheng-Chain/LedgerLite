package com.ledgerlite.app.data.local.relation

/**
 * 按日聚合的支出统计。dayStart 为当日 0 点 epoch millis，便于 UI 排序与展示。
 */
data class DaySum(
    val dayStart: Long,
    val total: Long
)
