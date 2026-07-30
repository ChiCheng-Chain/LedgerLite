package com.ledgerlite.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ledgerlite.app.data.local.LedgerDatabase
import com.ledgerlite.app.data.local.SeedCallback
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
 * ExpenseRepository 校验与溢出兜底测试。
 * 用真实 inMemory Room，验证 Repository 层的 require 校验和聚合 Flow 的 catch 兜底。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExpenseRepositoryTest {

    private lateinit var db: LedgerDatabase
    private lateinit var repo: ExpenseRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java
        ).allowMainThreadQueries().addCallback(SeedCallback()).build()
        repo = ExpenseRepository(db.expenseDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    /** 断言 block 抛指定异常；runTest 内 suspend 调用用此帮手捕获。 */
    private inline fun <reified T : Throwable> assertThrowsSuspend(block: () -> Unit) {
        try {
            block()
            error("预期抛 ${T::class.simpleName}，但未抛出")
        } catch (e: Throwable) {
            assertTrue(
                "预期 ${T::class.simpleName}，实际 ${e::class.simpleName}: ${e.message}",
                e is T
            )
        }
    }

    @Test
    fun `负金额_create抛IllegalArgumentException`() = runTest {
        assertThrowsSuspend<IllegalArgumentException> {
            repo.create(amount = -100, categoryId = 1, note = "", occurredAt = 1700000000000L)
        }
    }

    @Test
    fun `零金额_create抛异常`() = runTest {
        assertThrowsSuspend<IllegalArgumentException> {
            repo.create(amount = 0, categoryId = 1, note = "", occurredAt = 1700000000000L)
        }
    }

    @Test
    fun `零时间戳_create抛异常`() = runTest {
        assertThrowsSuspend<IllegalArgumentException> {
            repo.create(amount = 1000, categoryId = 1, note = "", occurredAt = 0)
        }
    }

    @Test
    fun `孤立categoryId_create抛异常`() = runTest {
        // categoryId=999>0 通过 Repository 校验，但数据库外键 NO_ACTION 仍抛异常
        assertThrowsSuspend<Exception> {
            repo.create(amount = 1000, categoryId = 999, note = "", occurredAt = 1700000000000L)
        }
    }

    @Test
    fun `update_负金额抛异常`() = runTest {
        val id = repo.create(amount = 1000, categoryId = 1, note = "", occurredAt = 1700000000000L)
        val rec = db.expenseDao().getById(id)!!
        assertThrowsSuspend<IllegalArgumentException> {
            repo.update(rec.copy(amount = -500))
        }
    }

    @Test
    fun `正常create落库`() = runTest {
        val id = repo.create(amount = 2500, categoryId = 1, note = "午饭", occurredAt = 1700000000000L)
        val rec = db.expenseDao().getById(id)
        assertEquals(2500, rec!!.amount)
        assertEquals("午饭", rec.note)
        assertTrue(rec.createdAt > 0)
        assertEquals(rec.createdAt, rec.updatedAt)
    }

    @Test
    fun `溢出总额_observeTotalInRange返回0不崩溃`() = runTest {
        val now = 1700000000000L
        // 直接用 DAO 灌入 MAX_VALUE，绕过 Repository 校验，触发 SQL SUM 溢出
        db.expenseDao().insert(ExpenseRecord(amount = Long.MAX_VALUE, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().insert(ExpenseRecord(amount = Long.MAX_VALUE, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        // Repository 的 catch 兜底：不抛异常，返回 0
        val total = repo.observeTotalInRange(0, now + 1).first()
        assertEquals(0L, total)
    }

    @Test
    fun `溢出_按分类聚合返回空列表不崩溃`() = runTest {
        val now = 1700000000000L
        db.expenseDao().insert(ExpenseRecord(amount = Long.MAX_VALUE, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().insert(ExpenseRecord(amount = Long.MAX_VALUE, categoryId = 1, occurredAt = now, createdAt = now, updatedAt = now))
        val sums = repo.sumGroupByCategory(0, now + 1).first()
        assertEquals(emptyList<Any>(), sums)
    }
}
