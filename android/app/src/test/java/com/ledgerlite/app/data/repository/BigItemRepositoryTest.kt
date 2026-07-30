package com.ledgerlite.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ledgerlite.app.data.local.LedgerDatabase
import com.ledgerlite.app.data.local.SeedCallback
import com.ledgerlite.app.domain.model.BigItemStatus
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
 * BigItemRepository 校验测试。验证 name/amount/startDate 的 require 校验。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BigItemRepositoryTest {

    private lateinit var db: LedgerDatabase
    private lateinit var repo: BigItemRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java
        ).allowMainThreadQueries().addCallback(SeedCallback()).build()
        repo = BigItemRepository(db.bigItemDao())
    }

    @After
    fun teardown() {
        db.close()
    }

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
    fun `空name_create抛异常`() = runTest {
        assertThrowsSuspend<IllegalArgumentException> {
            repo.create(name = "", amount = 600000, startDate = 1700000000000L)
        }
    }

    @Test
    fun `空白name_create抛异常`() = runTest {
        assertThrowsSuspend<IllegalArgumentException> {
            repo.create(name = "   ", amount = 600000, startDate = 1700000000000L)
        }
    }

    @Test
    fun `零金额_create抛异常`() = runTest {
        assertThrowsSuspend<IllegalArgumentException> {
            repo.create(name = "电脑", amount = 0, startDate = 1700000000000L)
        }
    }

    @Test
    fun `负金额_create抛异常`() = runTest {
        assertThrowsSuspend<IllegalArgumentException> {
            repo.create(name = "电脑", amount = -100, startDate = 1700000000000L)
        }
    }

    @Test
    fun `零开始日期_create抛异常`() = runTest {
        assertThrowsSuspend<IllegalArgumentException> {
            repo.create(name = "电脑", amount = 600000, startDate = 0)
        }
    }

    @Test
    fun `正常create落库`() = runTest {
        val id = repo.create(name = "笔记本电脑", amount = 600000, startDate = 1700000000000L)
        val item = db.bigItemDao().getById(id)
        assertEquals("笔记本电脑", item!!.name)
        assertEquals(600000, item.amount)
        assertEquals(BigItemStatus.active, item.status)
        assertTrue(item.createdAt > 0)
    }

    @Test
    fun `update_空name抛异常`() = runTest {
        val id = repo.create(name = "电脑", amount = 600000, startDate = 1700000000000L)
        val item = db.bigItemDao().getById(id)!!
        assertThrowsSuspend<IllegalArgumentException> {
            repo.update(item.copy(name = ""))
        }
    }
}
