package com.ledgerlite.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 日常支出流水。amount 为最小货币单位（分），整数避免浮点误差。
 * deletedAt 软删除，null=未删。查询一律带 WHERE deletedAt IS NULL。
 * 外键 onDelete=NO_ACTION：删分类前由 Repository 查引用计数拦截。
 *
 * type 字段 MVP 暂不入库（只有支出），后续加 ExpenseType 枚举 + type 列扩展收入。
 */
@Entity(
    tableName = "expense_records",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("categoryId"), Index("occurredAt"), Index("deletedAt")]
)
@Serializable
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    val categoryId: Long,
    val note: String = "",
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
