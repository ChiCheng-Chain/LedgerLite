package com.ledgerlite.app.data.repository

import com.ledgerlite.app.data.local.dao.ExpenseDao
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import com.ledgerlite.app.data.local.relation.CategorySum
import com.ledgerlite.app.data.local.relation.DaySum
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * 薄包装 DAO。create 内部注入 createdAt/updatedAt。
 * 不在 Repository 切 Dispatcher（Room 自带 IO 调度）。
 */
class ExpenseRepository(private val dao: ExpenseDao) {

    fun observeRecent(limit: Int): Flow<List<ExpenseWithCategory>> = dao.observeRecent(limit)

    fun observeByDateRange(start: Long, end: Long): Flow<List<ExpenseWithCategory>> =
        dao.observeByDateRange(start, end)

    fun observeByDateRangeWithKeyword(start: Long, end: Long, keyword: String): Flow<List<ExpenseWithCategory>> =
        dao.observeByDateRangeWithKeyword(start, end, keyword)

    fun observeByDateRangeWithCategory(start: Long, end: Long, categoryId: Long): Flow<List<ExpenseWithCategory>> =
        dao.observeByDateRangeWithCategory(start, end, categoryId)

    fun observeTodayTotal(): Flow<Long> =
        dao.observeTotalInRange(DateUtil.startOfToday(), DateUtil.startOfNextDay())

    fun observeMonthTotal(): Flow<Long> =
        dao.observeTotalInRange(DateUtil.startOfMonth(), DateUtil.startOfNextMonth())

    fun observeTotalInRange(start: Long, end: Long): Flow<Long> =
        dao.observeTotalInRange(start, end).catch { emit(0L) }

    fun sumGroupByCategory(start: Long, end: Long): Flow<List<CategorySum>> =
        dao.sumGroupByCategory(start, end).catch { emit(emptyList()) }

    fun sumGroupByCategoryFiltered(start: Long, end: Long, categoryId: Long): Flow<List<CategorySum>> =
        dao.sumGroupByCategoryFiltered(start, end, categoryId).catch { emit(emptyList()) }

    fun sumGroupByDay(start: Long, end: Long): Flow<List<DaySum>> =
        dao.sumGroupByDay(start, end, DateUtil.tzOffsetMillis()).catch { emit(emptyList()) }

    suspend fun getById(id: Long): ExpenseRecord? = dao.getById(id)

    fun observeById(id: Long): Flow<ExpenseRecord?> = dao.observeById(id)

    suspend fun create(amount: Long, categoryId: Long, note: String, occurredAt: Long): Long {
        require(amount > 0) { "金额必须为正" }
        require(categoryId > 0) { "分类无效" }
        require(occurredAt > 0) { "发生时间无效" }
        val now = DateUtil.nowMillis()
        val record = ExpenseRecord(
            amount = amount,
            categoryId = categoryId,
            note = note,
            occurredAt = occurredAt,
            createdAt = now,
            updatedAt = now
        )
        return dao.insert(record)
    }

    suspend fun update(record: ExpenseRecord) {
        require(record.amount > 0) { "金额必须为正" }
        require(record.categoryId > 0) { "分类无效" }
        require(record.occurredAt > 0) { "发生时间无效" }
        val now = DateUtil.nowMillis()
        dao.update(record.copy(updatedAt = now))
    }

    suspend fun softDelete(id: Long) {
        dao.softDelete(id, DateUtil.nowMillis())
    }

    fun observeDeleted(): Flow<List<ExpenseWithCategory>> = dao.observeDeleted()

    suspend fun restore(id: Long) {
        dao.restore(id, DateUtil.nowMillis())
    }

    /** 物理删除单条已删记录（回收站里手动彻底删）。 */
    suspend fun hardDelete(id: Long) {
        dao.hardDelete(id)
    }

    /** 清空回收站：物理删除全部软删记录，返回删除条数。 */
    suspend fun emptyTrash(): Int = dao.purgeOlderThan(Long.MAX_VALUE)

    /**
     * 回收站 30 天保留期。软删超过 30 天的记录物理删除，返回删除条数。
     * 由 LedgerViewModel 在进入流水页时触发一次。
     */
    suspend fun purgeExpiredTrash(retentionMillis: Long = 30L * 24 * 60 * 60 * 1000): Int =
        dao.purgeOlderThan(DateUtil.nowMillis() - retentionMillis)

    /** 该分类被多少未删除流水引用。删分类前由 UI 层判断是否拦截。 */
    suspend fun referenceCount(categoryId: Long): Int = dao.referenceCount(categoryId)
}
