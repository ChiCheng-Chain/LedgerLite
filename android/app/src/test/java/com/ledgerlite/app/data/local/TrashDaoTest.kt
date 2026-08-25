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
 * 回收站 DAO 测试（Robolectric 真内存库）。
 *
 * 覆盖：observeDeleted 只含软删记录、restore 恢复后回到正常查询、
 * hardDelete 只物理删已删记录、purgeOlderThan 按保留期阈值清理。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrashDaoTest {

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

    private fun record(amount: Long, occurredAt: Long) =
        ExpenseRecord(amount = amount, categoryId = 1, occurredAt = occurredAt, createdAt = occurredAt, updatedAt = occurredAt)

    @Test
    fun `observeDeleted 只返回软删记录`() = runTest {
        val now = 1700000000000L
        val idKeep = db.expenseDao().insert(record(1000, now))
        val idDel = db.expenseDao().insert(record(2000, now))
        db.expenseDao().softDelete(idDel, now + 100)

        val trash = db.expenseDao().observeDeleted().first()
        assertEquals(1, trash.size)
        assertEquals(idDel, trash[0].expense.id)
        assertEquals(idKeep, db.expenseDao().observeRecent(10).first()[0].expense.id)
    }

    @Test
    fun `restore 后记录回到正常查询`() = runTest {
        val now = 1700000000000L
        val id = db.expenseDao().insert(record(1000, now))
        db.expenseDao().softDelete(id, now + 100)

        db.expenseDao().restore(id, now + 200)

        val rec = db.expenseDao().getById(id)
        assertNull(rec!!.deletedAt)
        assertEquals(now + 200, rec.updatedAt)
        assertTrue(db.expenseDao().observeDeleted().first().isEmpty())
        assertEquals(1, db.expenseDao().observeRecent(10).first().size)
    }

    @Test
    fun `hardDelete 物理删除已删记录_未删记录不受影响`() = runTest {
        val now = 1700000000000L
        val idKeep = db.expenseDao().insert(record(1000, now))
        val idDel = db.expenseDao().insert(record(2000, now))
        db.expenseDao().softDelete(idDel, now + 100)

        db.expenseDao().hardDelete(idDel)

        assertNull(db.expenseDao().getById(idDel))
        assertNotNull(db.expenseDao().getById(idKeep))
    }

    @Test
    fun `hardDelete 对未删除记录不生效`() = runTest {
        val now = 1700000000000L
        val id = db.expenseDao().insert(record(1000, now))

        // 防呆：正常记录不会被回收站入口误删
        db.expenseDao().hardDelete(id)

        assertNotNull(db.expenseDao().getById(id))
    }

    @Test
    fun `purgeOlderThan 只清理过期记录`() = runTest {
        val now = 1700000000000L
        val dayMs = 24 * 60 * 60 * 1000L
        val idFresh = db.expenseDao().insert(record(1000, now))
        val idOld = db.expenseDao().insert(record(2000, now))
        val idNormal = db.expenseDao().insert(record(3000, now))
        // 刚删的和 31 天前删的
        db.expenseDao().softDelete(idFresh, now)
        db.expenseDao().softDelete(idOld, now - 31 * dayMs)

        val purged = db.expenseDao().purgeOlderThan(now - 30 * dayMs)

        assertEquals(1, purged)
        assertNull(db.expenseDao().getById(idOld))
        assertNotNull(db.expenseDao().getById(idFresh))
        assertNotNull(db.expenseDao().getById(idNormal))
        assertEquals(1, db.expenseDao().observeDeleted().first().size)
    }

    @Test
    fun `purgeOlderThan threshold 为 MAX 时清空全部软删记录`() = runTest {
        val now = 1700000000000L
        val id1 = db.expenseDao().insert(record(1000, now))
        val id2 = db.expenseDao().insert(record(2000, now))
        val idNormal = db.expenseDao().insert(record(3000, now))
        db.expenseDao().softDelete(id1, now)
        db.expenseDao().softDelete(id2, now)

        val purged = db.expenseDao().purgeOlderThan(Long.MAX_VALUE)

        assertEquals(2, purged)
        assertTrue(db.expenseDao().observeDeleted().first().isEmpty())
        assertNotNull(db.expenseDao().getById(idNormal))
    }
}
