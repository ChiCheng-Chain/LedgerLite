package com.ledgerlite.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 日期边界数据测试。
 *
 * 目的：验证 occurredAt 为未来时间戳、0、负时间戳时，按区间查询和按天聚合的行为。
 * 项目无任何日期校验，UI 也未禁止未来日期。这些测试记录"非法日期如何影响统计"，
 * 尤其是按天聚合的 dayStart 计算对边界值的稳健性。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DateBoundaryTest {

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
    fun `未来时间戳可落库_不在当前区间统计`() = runTest {
        val now = 1700000000000L
        val future = now + 10L * 365 * 24 * 3600 * 1000
        db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        db.expenseDao().insert(
            ExpenseRecord(amount = 2000, categoryId = 1, occurredAt = future, createdAt = now, updatedAt = now)
        )
        // 查询 [0, now+1) 区间：未来那笔被排除
        val total = db.expenseDao().observeTotalInRange(0, now + 1).first()
        assertEquals(1000, total)
        // 查询覆盖未来时则包含
        val totalAll = db.expenseDao().observeTotalInRange(0, future + 1).first()
        assertEquals(3000, totalAll)
    }

    @Test
    fun `零时间戳可落库_可被大区间查询命中`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(
            ExpenseRecord(amount = 500, categoryId = 1, occurredAt = 0, createdAt = now, updatedAt = now)
        )
        // occurredAt=0，用 start=0 的区间可命中
        val total = db.expenseDao().observeTotalInRange(0, now).first()
        assertEquals(500, total)
    }

    @Test
    fun `负时间戳可落库_按天聚合不崩溃`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(
            ExpenseRecord(amount = 800, categoryId = 1, occurredAt = -86400000L, createdAt = now, updatedAt = now)
        )
        // tzOffset=0 时，dayStart = occurredAt - (occurredAt % 86400000)
        // 负时间戳的取模结果在 SQLite 中为负或零，dayStart 计算应不抛异常
        val sums = db.expenseDao().sumGroupByDay(-86400000L * 2, now, 0).first()
        // 至少应返回一行，且 total=800，不崩溃
        assertTrue("按天聚合应返回结果", sums.isNotEmpty())
        assertEquals(800, sums[0].total)
    }

    @Test
    fun `同日不同时刻聚合到同一天`() = runTest {
        // 选一个对齐到 UTC 日界的时间戳作为当天 0 点，确保三个时刻落在同一个 dayStart
        val dayStart = 1700000000000L / 86400000L * 86400000L
        val tzOffset = 0L
        // 同一天内三个时刻
        val t1 = dayStart
        val t2 = dayStart + 3600 * 1000
        val t3 = dayStart + 7200 * 1000
        db.expenseDao().insert(ExpenseRecord(amount = 100, categoryId = 1, occurredAt = t1, createdAt = dayStart, updatedAt = dayStart))
        db.expenseDao().insert(ExpenseRecord(amount = 200, categoryId = 1, occurredAt = t2, createdAt = dayStart, updatedAt = dayStart))
        db.expenseDao().insert(ExpenseRecord(amount = 300, categoryId = 1, occurredAt = t3, createdAt = dayStart, updatedAt = dayStart))
        val sums = db.expenseDao().sumGroupByDay(0, t3 + 1, tzOffset).first()
        // 三条应聚合到同一个 dayStart
        assertEquals(1, sums.size)
        assertEquals(600, sums[0].total)
    }
}
