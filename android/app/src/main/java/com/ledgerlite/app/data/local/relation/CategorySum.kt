package com.ledgerlite.app.data.local.relation

/**
 * 按分类聚合的支出统计。categoryId 可能在历史数据里指向已删分类（LEFT JOIN 后为 null）。
 */
data class CategorySum(
    val categoryId: Long?,
    val categoryName: String?,
    val categoryColor: Long?,
    val total: Long
)
