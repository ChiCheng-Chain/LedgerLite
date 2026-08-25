package com.ledgerlite.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import com.ledgerlite.app.data.local.relation.CategorySum
import com.ledgerlite.app.data.local.relation.DaySum
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // 观察型：持续推流，UI 侧 collectAsStateWithLifecycle
    @Query("""
        SELECT e.*, c.name AS categoryName, c.color AS categoryColor, c.icon AS categoryIcon
        FROM expense_records e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.deletedAt IS NULL
        ORDER BY e.occurredAt DESC
        LIMIT :limit
    """)
    fun observeRecent(limit: Int): Flow<List<ExpenseWithCategory>>

    @Query("""
        SELECT e.*, c.name AS categoryName, c.color AS categoryColor, c.icon AS categoryIcon
        FROM expense_records e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.deletedAt IS NULL AND e.occurredAt >= :start AND e.occurredAt < :end
        ORDER BY e.occurredAt DESC
    """)
    fun observeByDateRange(start: Long, end: Long): Flow<List<ExpenseWithCategory>>

    @Query("""
        SELECT e.*, c.name AS categoryName, c.color AS categoryColor, c.icon AS categoryIcon
        FROM expense_records e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.deletedAt IS NULL AND e.occurredAt >= :start AND e.occurredAt < :end
          AND e.note LIKE '%' || :keyword || '%'
        ORDER BY e.occurredAt DESC
    """)
    fun observeByDateRangeWithKeyword(start: Long, end: Long, keyword: String): Flow<List<ExpenseWithCategory>>

    @Query("""
        SELECT e.*, c.name AS categoryName, c.color AS categoryColor, c.icon AS categoryIcon
        FROM expense_records e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.deletedAt IS NULL AND e.occurredAt >= :start AND e.occurredAt < :end
          AND e.categoryId = :categoryId
        ORDER BY e.occurredAt DESC
    """)
    fun observeByDateRangeWithCategory(start: Long, end: Long, categoryId: Long): Flow<List<ExpenseWithCategory>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM expense_records
        WHERE deletedAt IS NULL AND occurredAt >= :start AND occurredAt < :end
    """)
    fun observeTotalInRange(start: Long, end: Long): Flow<Long>

    // 统计：按分类聚合
    @Query("""
        SELECT e.categoryId AS categoryId, c.name AS categoryName, c.color AS categoryColor,
               SUM(e.amount) AS total
        FROM expense_records e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.deletedAt IS NULL AND e.occurredAt >= :start AND e.occurredAt < :end
        GROUP BY e.categoryId
        ORDER BY SUM(e.amount) DESC
    """)
    fun sumGroupByCategory(start: Long, end: Long): Flow<List<CategorySum>>

    @Query("""
        SELECT e.categoryId AS categoryId, c.name AS categoryName, c.color AS categoryColor,
               SUM(e.amount) AS total
        FROM expense_records e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.deletedAt IS NULL AND e.occurredAt >= :start AND e.occurredAt < :end
          AND e.categoryId = :categoryId
        GROUP BY e.categoryId
        ORDER BY SUM(e.amount) DESC
    """)
    fun sumGroupByCategoryFiltered(start: Long, end: Long, categoryId: Long): Flow<List<CategorySum>>

    // 统计：按日聚合。dayStart 按本地时区日界对齐：
    // (occurredAt + tzOffset) 对 86400000 取模得到当日已过的 UTC 毫秒，
    // 用 occurredAt 减去它得到本地 0 点 epoch。tzOffset 为本地时区相对 UTC 的毫秒偏移。
    @Query("""
        SELECT occurredAt - ((occurredAt + :tzOffset) % 86400000) AS dayStart, SUM(amount) AS total
        FROM expense_records
        WHERE deletedAt IS NULL AND occurredAt >= :start AND occurredAt < :end
        GROUP BY dayStart
        ORDER BY dayStart ASC
    """)
    fun sumGroupByDay(start: Long, end: Long, tzOffset: Long): Flow<List<DaySum>>

    // 一次性读取
    @Query("SELECT * FROM expense_records WHERE id = :id")
    suspend fun getById(id: Long): ExpenseRecord?

    @Query("SELECT * FROM expense_records WHERE id = :id")
    fun observeById(id: Long): Flow<ExpenseRecord?>

    // 写操作
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: ExpenseRecord): Long

    @Update
    suspend fun update(record: ExpenseRecord)

    @Query("UPDATE expense_records SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    // 回收站
    @Query("""
        SELECT e.*, c.name AS categoryName, c.color AS categoryColor, c.icon AS categoryIcon
        FROM expense_records e
        LEFT JOIN categories c ON e.categoryId = c.id
        WHERE e.deletedAt IS NOT NULL
        ORDER BY e.deletedAt DESC
    """)
    fun observeDeleted(): Flow<List<ExpenseWithCategory>>

    @Query("UPDATE expense_records SET deletedAt = NULL, updatedAt = :now WHERE id = :id")
    suspend fun restore(id: Long, now: Long)

    @Query("DELETE FROM expense_records WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun hardDelete(id: Long)

    /** 回收站保留期外（deletedAt 早于 threshold）的记录物理删除。 */
    @Query("DELETE FROM expense_records WHERE deletedAt IS NOT NULL AND deletedAt < :threshold")
    suspend fun purgeOlderThan(threshold: Long): Int

    // 引用计数：删分类前判断是否被引用
    @Query("SELECT COUNT(*) FROM expense_records WHERE categoryId = :categoryId AND deletedAt IS NULL")
    suspend fun referenceCount(categoryId: Long): Int

    // 备份恢复
    @Query("SELECT * FROM expense_records ORDER BY id ASC")
    suspend fun getAll(): List<ExpenseRecord>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(records: List<ExpenseRecord>)

    @Query("DELETE FROM expense_records")
    suspend fun deleteAll()
}
