package com.ledgerlite.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import com.ledgerlite.app.domain.model.BigItemStatus
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LedgerDatabaseTest {

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
    fun `种子数据插入13个默认分类`() = runTest {
        val categories = db.categoryDao().observeAll().first()
        assertEquals(13, categories.size)
        assertEquals("餐饮", categories[0].name)
        assertEquals("其他", categories.last().name)
        assertTrue(categories.all { it.isDefault })
    }

    @Test
    fun `TypeConverter往返_BigItemStatus`() = runTest {
        val now = 1700000000000L
        val item = BigItem(
            name = "笔记本电脑",
            amount = 600000,
            startDate = now,
            status = BigItemStatus.active,
            createdAt = now,
            updatedAt = now
        )
        val id = db.bigItemDao().insert(item)
        val loaded = db.bigItemDao().getById(id)
        assertNotNull(loaded)
        assertEquals(BigItemStatus.active, loaded!!.status)
    }

    @Test
    fun `软删除过滤_已删除流水不出现在查询`() = runTest {
        val now = 1700000000000L
        val id1 = db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        db.expenseDao().insert(
            ExpenseRecord(amount = 2000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now)
        )
        // 软删一条
        db.expenseDao().softDelete(id1, now + 1)
        val records = db.expenseDao().observeRecent(10).first()
        assertEquals(1, records.size)
        assertEquals(2000, records[0].expense.amount)
        // getById 仍可读到（含 deletedAt）
        val softDeleted = db.expenseDao().getById(id1)
        assertNotNull(softDeleted?.deletedAt)
    }

    @Test
    fun `按分类聚合查询`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().insert(ExpenseRecord(amount = 2500, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().insert(ExpenseRecord(amount = 3000, categoryId = 2, occurredAt = now, createdAt = now, updatedAt = now))
        val sums = db.expenseDao().sumGroupByCategory(0, now + 1).first()
        assertEquals(2, sums.size)
        // 按总额降序：分类1（3500）在前，分类2（3000）在后
        assertEquals(3500, sums[0].total)
        assertEquals(3000, sums[1].total)
    }

    @Test
    fun `引用计数_分类被流水引用`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(ExpenseRecord(amount = 1000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().insert(ExpenseRecord(amount = 2000, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        assertEquals(2, db.expenseDao().referenceCount(1))
        assertEquals(0, db.expenseDao().referenceCount(2))
    }
}
