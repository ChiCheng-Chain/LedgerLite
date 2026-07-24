package com.ledgerlite.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 支出分类。默认分类由 SeedCallback 插入。
 */
@Entity(tableName = "categories", indices = [Index("sortOrder")])
@Serializable
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "",
    val color: Long = 0xFF3C6E71,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
