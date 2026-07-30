package com.ledgerlite.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 软删除一致性测试。
 *
 * 目的：验证软删除（设 deletedAt + updatedAt）后，所有读取/统计路径都正确过滤，
 * 且 deletedAt 与 updatedAt 同步更新。getById 仍可读到带 deletedAt 的记录（用于审计/撤销）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SoftDeleteConsistencyTest {

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
    fun `软删除后_区间总额排除已删记录`() = runTest {
        val now = 1700000000000L
        val idKeep = db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        val idDel = db.expenseDao().insert(
            ExpenseRecord(amount = 2000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        db.expenseDao().softDelete(idDel, now + 100)
        // 总额只剩 1000
        val total = db.expenseDao().observeTotalInRange(0, now + 1).first()
        assertEquals(1000, total)
        // 近期列表只剩保留的那条
        val records = db.expenseDao().observeRecent(10).first()
        assertEquals(1, records.size)
        assertEquals(idKeep, records[0].expense.id)
    }

    @Test
    fun `软删除后_按分类聚合排除已删记录`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        val idDel = db.expenseDao().insert(ExpenseRecord(amount = 2500, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().softDelete(idDel, now + 100)
        val sums = db.expenseDao().sumGroupByCategory(0, now + 1).first()
        assertEquals(1, sums.size)
        // 分类1只剩 1000
        assertEquals(1000, sums[0].total)
    }

    @Test
    fun `软删除同步更新deletedAt与updatedAt`() = runTest {
        val now = 1700000000000L
        val id = db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        val delTime = now + 5000
        db.expenseDao().softDelete(id, delTime)
        val rec = db.expenseDao().getById(id)
        assertNotNull(rec)
        assertEquals(delTime, rec!!.deletedAt)
        assertEquals(delTime, rec.updatedAt)
        // createdAt 保持不变
        assertEquals(now, rec.createdAt)
    }

    @Test
    fun `软删除后_getById仍可读到带deletedAt的记录`() = runTest {
        val now = 1700000000000L
        val id = db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        db.expenseDao().softDelete(id, now + 1)
        val rec = db.expenseDao().getById(id)
        assertNotNull(rec)
        assertNotNull(rec!!.deletedAt)
    }

    @Test
    fun `引用计数排除已软删除的流水`() = runTest {
        val now = 1700000000000L
        val id1 = db.expenseDao().insert(ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().insert(ExpenseRecord(amount = 2000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        // 软删一条
        db.expenseDao().softDelete(id1, now + 100)
        // referenceCount 只计未删除的，应只剩 1
        assertEquals(1, db.expenseDao().referenceCount(1))
    }

    @Test
    fun `重复软删除_以最后一次时间为准`() = runTest {
        val now = 1700000000000L
        val id = db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        db.expenseDao().softDelete(id, now + 100)
        db.expenseDao().softDelete(id, now + 200)
        val rec = db.expenseDao().getById(id)
        assertEquals(now + 200, rec!!.deletedAt)
        // 仍是软删除状态，不出现在查询里
        val records = db.expenseDao().observeRecent(10).first()
        assertTrue(records.isEmpty())
    }
}
