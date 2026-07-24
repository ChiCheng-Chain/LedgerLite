package com.ledgerlite.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ledgerlite.app.domain.model.BigItemStatus
import kotlinx.serialization.Serializable

/**
 * 资产摊销项目。资产不生成 ExpenseRecord，与流水完全独立。
 * 使用天数按实际日历算：使用中 = 今天 - startDate + 1；已结束 = endedAt - startDate + 1。
 * 不再有 duration 字段（原 durationValue/durationUnit 已移除）。
 */
@Entity(
    tableName = "big_items",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("categoryId"), Index("status")]
)
@Serializable
data class BigItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val startDate: Long,
    val categoryId: Long? = null,
    val note: String = "",
    val status: BigItemStatus = BigItemStatus.active,
    val createdAt: Long,
    val updatedAt: Long,
    val endedAt: Long? = null
)
