package com.ledgerlite.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 金额边界数据测试。
 *
 * 目的：验证数据层（DAO/SQL）对非法金额的处理。项目当前 Repository/DAO 零校验，
 * 仅靠 UI 按钮 cents>0 拦截。绕过 UI 直插时，这些测试记录"实际会落库什么"，
 * 并验证聚合查询不会因非法数据崩溃或静默吞掉结果。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AmountBoundaryTest {

    private lateinit var db: LedgerDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java
        ).allowMainThreadQueries().addCallback(SeedCallback()).build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `零金额可落库_聚合纳入计算`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(
            ExpenseRecord(amount = 0, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        val records = db.expenseDao().observeRecent(10).first()
        // 两条都应出现：零金额不被任何查询过滤
        assertEquals(2, records.size)
        // 区间总额应包含零金额（不报错，总额=1000）
        val total = db.expenseDao().observeTotalInRange(0, now + 1).first()
        assertEquals(1000, total)
    }

    @Test
    fun `负金额可落库_聚合会拉低总额`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        db.expenseDao().insert(
            ExpenseRecord(amount = -300, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        // 负数成功插入（数据层无校验），总额被拉低到 700
        val total = db.expenseDao().observeTotalInRange(0, now + 1).first()
        assertEquals(700, total)
        // 按分类聚合同样纳入负数
        val sums = db.expenseDao().sumGroupByCategory(0, now + 1).first()
        assertEquals(1, sums.size)
        assertEquals(700, sums[0].total)
    }

    @Test
    fun `Long最大值金额不溢出_单条`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(
            ExpenseRecord(amount = Long.MAX_VALUE, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        val total = db.expenseDao().observeTotalInRange(0, now + 1).first()
        assertEquals(Long.MAX_VALUE, total)
    }

    @Test
    fun `Long最大值求和溢出_聚合查询抛异常`() = runTest {
        val now = 1700000000000L
        // 两条 MAX_VALUE 求和超出 Long 范围，SQLite SUM 直接抛 integer overflow
        db.expenseDao().insert(
            ExpenseRecord(amount = Long.MAX_VALUE, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        db.expenseDao().insert(
            ExpenseRecord(amount = Long.MAX_VALUE, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        val thrown = try {
            db.expenseDao().observeTotalInRange(0, now + 1).first()
            null
        } catch (e: Exception) {
            e
        }
        // 真实 bug：金额累计溢出时，统计查询崩溃而非返回安全值。
        // 锁定此行为作为回归测试；修复后应改为断言"不抛异常 + 安全兜底"。
        assertNotNull(thrown)
        assertTrue(
            "应为 integer overflow，实际: ${thrown!!.message}",
            thrown.message?.contains("overflow") == true
        )
    }

    @Test
    fun `孤立分类ID插入_外键NO_ACTION抛异常`() = runTest {
        val now = 1700000000000L
        // categoryId=999 在 categories 表中不存在
        val thrown = try {
            db.expenseDao().insert(
                ExpenseRecord(amount = 1000, categoryId = 999, occurredAt = now, createdAt = now, updatedAt = now)
            )
            null
        } catch (e: Exception) {
            e
        }
        // 外键约束生效：NO_ACTION 在插入孤儿行时由 SQLite 外键检查拒绝
        assertNotNull(thrown)
    }
}
