package com.ledgerlite.app.ui.bigitem

import com.ledgerlite.app.data.local.dao.BigItemDao
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.data.repository.BigItemRepository
import com.ledgerlite.app.domain.model.BigItemStatus
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * 资产列表排序测试：已结束的大件应垫底展示，不保留原位。
 *
 * 使用中按 createdAt 倒序在前；已结束按结束时间（endedAt，缺省 updatedAt）倒序垫底。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BigItemViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeBigItemDao
    private lateinit var vm: BigItemViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeBigItemDao()
        vm = BigItemViewModel(
            BigItemRepository(dao),
            dayStartFlow = MutableStateFlow(DateUtil.startOfToday())
        )
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    private fun item(
        id: Long,
        createdAt: Long,
        status: BigItemStatus = BigItemStatus.active,
        endedAt: Long? = null
    ) = BigItem(
        id = id,
        name = "资产$id",
        amount = 100000,
        startDate = createdAt,
        createdAt = createdAt,
        updatedAt = endedAt ?: createdAt,
        status = status,
        endedAt = endedAt
    )

    @Test
    fun `已结束资产垫底_使用中按创建时间倒序`() = runTest(testDispatcher) {
        dao.items.value = listOf(
            item(1, createdAt = 100),
            item(2, createdAt = 300, status = BigItemStatus.ended, endedAt = 500),
            item(3, createdAt = 200),
            item(4, createdAt = 50, status = BigItemStatus.ended, endedAt = 400)
        )
        advanceUntilIdle()

        val ids = vm.uiState.value.items.map { it.id }
        // 使用中按 createdAt 倒序（3→1），已结束按 endedAt 倒序（2→4）垫底
        assertEquals(listOf(3L, 1L, 2L, 4L), ids)
    }

    @Test
    fun `全部结束时按结束时间倒序`() = runTest(testDispatcher) {
        dao.items.value = listOf(
            item(1, createdAt = 100, status = BigItemStatus.ended, endedAt = 900),
            item(2, createdAt = 300, status = BigItemStatus.ended, endedAt = 800),
            item(3, createdAt = 200, status = BigItemStatus.ended, endedAt = 700)
        )
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), vm.uiState.value.items.map { it.id })
    }

    @Test
    fun `结束操作后资产立即沉底`() = runTest(testDispatcher) {
        dao.items.value = listOf(
            item(1, createdAt = 300),
            item(2, createdAt = 100),
            item(3, createdAt = 200)
        )
        advanceUntilIdle()
        assertEquals(listOf(1L, 3L, 2L), vm.uiState.value.items.map { it.id })

        // 结束最靠前的资产 1
        val now = 999L
        dao.items.value = dao.items.value.map {
            if (it.id == 1L) it.copy(status = BigItemStatus.ended, endedAt = now, updatedAt = now) else it
        }
        advanceUntilIdle()

        assertEquals(listOf(3L, 2L, 1L), vm.uiState.value.items.map { it.id })
    }

    /** 内存版 DAO：只实现 ViewModel 路径用到的方法。 */
    private class FakeBigItemDao : BigItemDao {
        val items = MutableStateFlow<List<BigItem>>(emptyList())

        override fun observeAll(): Flow<List<BigItem>> = items
        override fun observeByStatus(status: BigItemStatus): Flow<List<BigItem>> =
            items.apply { value = value.filter { it.status == status } }
        override suspend fun getById(id: Long): BigItem? = items.value.find { it.id == id }
        override fun observeById(id: Long): Flow<BigItem?> = MutableStateFlow(items.value.find { it.id == id })
        override suspend fun insert(item: BigItem): Long { TODO("Not yet implemented") }
        override suspend fun update(item: BigItem) { TODO("Not yet implemented") }
        override suspend fun endItem(id: Long, status: BigItemStatus, endedAt: Long, now: Long) { TODO("Not yet implemented") }
        override suspend fun getAll(): List<BigItem> = items.value
        override suspend fun insertAll(items: List<BigItem>) { TODO("Not yet implemented") }
        override suspend fun deleteAll() { TODO("Not yet implemented") }
    }
}
