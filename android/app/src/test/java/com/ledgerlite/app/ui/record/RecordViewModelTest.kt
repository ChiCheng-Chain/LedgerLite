package com.ledgerlite.app.ui.record

import com.ledgerlite.app.data.local.dao.CategoryDao
import com.ledgerlite.app.data.local.dao.ExpenseDao
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import com.ledgerlite.app.data.local.relation.CategorySum
import com.ledgerlite.app.data.local.relation.DaySum
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.data.repository.CategoryRepository
import com.ledgerlite.app.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeExpenseDao: FakeExpenseDao
    private lateinit var fakeCategoryDao: FakeCategoryDao
    private lateinit var vm: RecordViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseDao = FakeExpenseDao()
        fakeCategoryDao = FakeCategoryDao(listOf(
            Category(id = 1, name = "餐饮", createdAt = 0, updatedAt = 0),
            Category(id = 2, name = "交通", createdAt = 0, updatedAt = 0)
        ))
        vm = RecordViewModel(
            ExpenseRepository(fakeExpenseDao),
            CategoryRepository(fakeCategoryDao)
        )
    }

    @After
    fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `初始状态加载分类和最近记录`() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(2, state.categories.size)
        assertEquals("餐饮", state.categories[0].name)
    }

    @Test
    fun `create后状态更新`() = runTest(testDispatcher) {
        advanceUntilIdle()
        vm.createExpense(amount = 5000, categoryId = 1, note = "午饭", occurredAt = 1700000000000L)
        advanceUntilIdle()
        assertEquals(1, fakeExpenseDao.inserted.size)
        assertEquals(5000, fakeExpenseDao.inserted[0].amount)
        assertEquals("午饭", fakeExpenseDao.inserted[0].note)
        // createdAt 被 Repository 注入
        assertTrue(fakeExpenseDao.inserted[0].createdAt > 0)
    }
}

/** Fake ExpenseDao：MutableStateFlow 驱动，未用到的方法 throw NotImplementedError 占位。 */
private class FakeExpenseDao : ExpenseDao {
    val inserted = mutableListOf<ExpenseRecord>()
    private val records = MutableStateFlow<List<ExpenseRecord>>(emptyList())

    override fun observeRecent(limit: Int): Flow<List<ExpenseWithCategory>> =
        records.map { list -> list.take(limit).map { it.toWithCategory() } }

    override fun observeByDateRange(start: Long, end: Long): Flow<List<ExpenseWithCategory>> =
        records.map { list -> list.filter { it.occurredAt in start until end }.map { it.toWithCategory() } }

    override fun observeByDateRangeWithKeyword(start: Long, end: Long, keyword: String): Flow<List<ExpenseWithCategory>> =
        records.map { list -> list.filter { it.occurredAt in start until end && it.note.contains(keyword) }.map { it.toWithCategory() } }

    override fun observeByDateRangeWithCategory(start: Long, end: Long, categoryId: Long): Flow<List<ExpenseWithCategory>> =
        records.map { list -> list.filter { it.occurredAt in start until end && it.categoryId == categoryId }.map { it.toWithCategory() } }

    override fun observeTotalInRange(start: Long, end: Long): Flow<Long> =
        records.map { list -> list.filter { it.occurredAt in start until end }.sumOf { it.amount } }

    override fun sumGroupByCategory(start: Long, end: Long): Flow<List<CategorySum>> = flow { emit(emptyList()) }
    override fun sumGroupByCategoryFiltered(start: Long, end: Long, categoryId: Long): Flow<List<CategorySum>> = flow { emit(emptyList()) }
    override fun sumGroupByDay(start: Long, end: Long, tzOffset: Long): Flow<List<DaySum>> = flow { emit(emptyList()) }
    override suspend fun getById(id: Long): ExpenseRecord? = records.value.find { it.id == id }
    override fun observeById(id: Long): Flow<ExpenseRecord?> = records.map { list -> list.find { it.id == id } }

    override suspend fun insert(record: ExpenseRecord): Long {
        val newId = (records.value.maxOfOrNull { it.id } ?: 0) + 1
        val withId = record.copy(id = newId)
        records.value = records.value + withId
        inserted.add(withId)
        return newId
    }

    override suspend fun update(record: ExpenseRecord) {
        records.value = records.value.map { if (it.id == record.id) record else it }
    }

    override suspend fun softDelete(id: Long, now: Long) {
        records.value = records.value.map { if (it.id == id) it.copy(deletedAt = now) else it }
    }

    override suspend fun referenceCount(categoryId: Long): Int =
        records.value.count { it.categoryId == categoryId && it.deletedAt == null }

    private fun ExpenseRecord.toWithCategory() = ExpenseWithCategory(this, "测试", 0xFF3C6E71, "")
}

private class FakeCategoryDao(initial: List<Category>) : CategoryDao {
    private val categories = MutableStateFlow(initial.sortedBy { it.sortOrder })
    override fun observeAll(): Flow<List<Category>> = categories
    override fun observeRecent(limit: Int): Flow<List<Category>> = categories.map { it.take(limit) }
    override suspend fun getById(id: Long): Category? = categories.value.find { it.id == id }
    override suspend fun count(): Int = categories.value.size
    override suspend fun insert(category: Category): Long { TODO("Not yet implemented") }
    override suspend fun update(category: Category) { TODO("Not yet implemented") }
    override suspend fun updateAll(categories: List<Category>) { TODO("Not yet implemented") }
    override suspend fun delete(id: Long) { TODO("Not yet implemented") }
}
