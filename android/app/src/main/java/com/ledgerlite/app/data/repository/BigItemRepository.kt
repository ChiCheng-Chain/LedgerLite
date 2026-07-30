package com.ledgerlite.app.data.repository

import com.ledgerlite.app.data.local.dao.BigItemDao
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.domain.model.BigItemStatus
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.flow.Flow

class BigItemRepository(private val dao: BigItemDao) {

    fun observeAll(): Flow<List<BigItem>> = dao.observeAll()

    fun observeActive(): Flow<List<BigItem>> = dao.observeByStatus(BigItemStatus.active)

    fun observeEnded(): Flow<List<BigItem>> = dao.observeByStatus(BigItemStatus.ended)

    suspend fun getById(id: Long): BigItem? = dao.getById(id)

    fun observeById(id: Long): Flow<BigItem?> = dao.observeById(id)

    suspend fun create(
        name: String,
        amount: Long,
        startDate: Long,
        categoryId: Long? = null,
        note: String = ""
    ): Long {
        require(name.isNotBlank()) { "资产名不能为空" }
        require(amount > 0) { "金额必须为正" }
        require(startDate > 0) { "开始日期无效" }
        val now = DateUtil.nowMillis()
        val item = BigItem(
            name = name,
            amount = amount,
            startDate = startDate,
            categoryId = categoryId,
            note = note,
            status = BigItemStatus.active,
            createdAt = now,
            updatedAt = now
        )
        return dao.insert(item)
    }

    suspend fun update(item: BigItem) {
        require(item.name.isNotBlank()) { "资产名不能为空" }
        require(item.amount > 0) { "金额必须为正" }
        require(item.startDate > 0) { "开始日期无效" }
        dao.update(item.copy(updatedAt = DateUtil.nowMillis()))
    }

    suspend fun endItem(id: Long) {
        val now = DateUtil.nowMillis()
        dao.endItem(id, BigItemStatus.ended, now, now)
    }
}
