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

    // 统计：按日聚合。按本地时区日分组。
    @Query("""
        SELECT (occurredAt / 86400000 * 86400000) AS dayStart, SUM(amount) AS total
        FROM expense_records
        WHERE deletedAt IS NULL AND occurredAt >= :start AND occurredAt < :end
        GROUP BY dayStart
        ORDER BY dayStart ASC
    """)
    fun sumGroupByDay(start: Long, end: Long): Flow<List<DaySum>>

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

    // 引用计数：删分类前判断是否被引用
    @Query("SELECT COUNT(*) FROM expense_records WHERE categoryId = :categoryId AND deletedAt IS NULL")
    suspend fun referenceCount(categoryId: Long): Int
}
