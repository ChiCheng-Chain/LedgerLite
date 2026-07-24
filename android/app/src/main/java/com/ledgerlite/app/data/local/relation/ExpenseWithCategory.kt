package com.ledgerlite.app.data.local.relation

import androidx.room.Embedded
import com.ledgerlite.app.data.local.entity.ExpenseRecord

/**
 * 流水带分类信息（LEFT JOIN categories）。分类被删时 categoryName/color 可 null，UI 容错。
 */
data class ExpenseWithCategory(
    @Embedded val expense: ExpenseRecord,
    val categoryName: String?,
    val categoryColor: Long?,
    val categoryIcon: String?
)
