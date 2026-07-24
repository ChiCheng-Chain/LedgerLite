package com.ledgerlite.app.data.local

import androidx.room.TypeConverter
import com.ledgerlite.app.domain.model.BigItemStatus

/**
 * 枚举统一存 Enum.name（String），不存 label。读时 valueOf 还原。
 * 未知值不静默兜底，valueOf 直接抛 IllegalArgumentException。
 */
class Converters {
    @TypeConverter
    fun fromBigItemStatus(value: BigItemStatus?): String? = value?.name

    @TypeConverter
    fun toBigItemStatus(value: String?): BigItemStatus? =
        value?.let { BigItemStatus.valueOf(it) }
}
