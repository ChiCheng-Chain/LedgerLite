package com.ledgerlite.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ledgerlite.app.data.local.LedgerDatabase
import com.ledgerlite.app.data.local.SeedCallback
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
 * CategoryRepository 测试。重点验证 sortOrder 确定化：连建多个分类时单调递增。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CategoryRepositoryTest {

    private lateinit var db: LedgerDatabase
    private lateinit var repo: CategoryRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java
        ).allowMainThreadQueries().addCallback(SeedCallback()).build()
        repo = CategoryRepository(db.categoryDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `连建多个分类_sortOrder单调递增`() = runTest {
        val id1 = repo.create(name = "分类A", color = 0xFF112233)
        val id2 = repo.create(name = "分类B", color = 0xFF445566)
        val id3 = repo.create(name = "分类C", color = 0xFF778899)
        val all = repo.observeAll().first()
        // 种子分类 sortOrder 1..13，新建三个应为 14、15、16
        val newOnes = all.filter { it.id == id1 || it.id == id2 || it.id == id3 }
            .sortedBy { it.sortOrder }
        assertEquals(3, newOnes.size)
        assertEquals(14, newOnes[0].sortOrder)
        assertEquals(15, newOnes[1].sortOrder)
        assertEquals(16, newOnes[2].sortOrder)
    }

    @Test
    fun `第一个新分类sortOrder紧跟种子最大值`() = runTest {
        val id = repo.create(name = "新分类", color = 0xFF112233)
        val cat = repo.getById(id)!!
        // 种子最大 sortOrder=13，新分类应为 14
        assertEquals(14, cat.sortOrder)
        assertTrue(!cat.isDefault)
    }
}
