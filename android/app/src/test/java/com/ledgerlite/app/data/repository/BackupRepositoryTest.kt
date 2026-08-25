package com.ledgerlite.app.data.repository

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ledgerlite.app.data.local.LedgerDatabase
import com.ledgerlite.app.data.local.SeedCallback
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import com.ledgerlite.app.domain.model.BigItemStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 备份恢复测试（Robolectric 内存库，不经 ContentResolver）。
 *
 * 覆盖：备份→清库→恢复的数据一致性（含软删记录、大件状态、偏好）、
 * 原 id 与外键关系保留、覆盖语义、异常路径（坏 JSON / 版本过新 / 引用缺失）、幂等。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BackupRepositoryTest {

    /** 内存版偏好存储，复刻 DataStore.edit 就地修改语义（同 SettingsRepositoryTest）。 */
    private class InMemoryStore {
        val state = MutableStateFlow<Preferences>(emptyPreferences())
        val preferences: Flow<Preferences> = state
        suspend fun edit(mutate: suspend (MutablePreferences) -> Unit) {
            val copy = state.value.toMutablePreferences()
            mutate(copy)
            state.value = copy.toPreferences()
        }
    }

    private lateinit var db: LedgerDatabase
    private lateinit var store: InMemoryStore
    private lateinit var repo: BackupRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java
        ).allowMainThreadQueries().addCallback(SeedCallback()).build()
        store = InMemoryStore()
        repo = BackupRepository(
            db,
            SettingsRepository(preferences = store.preferences, editor = { mutate -> store.edit(mutate) })
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    private suspend fun seedData() {
        val now = 1700000000000L
        db.categoryDao().insert(Category(name = "自定义分类", color = 0xFF123456, sortOrder = 99, createdAt = now, updatedAt = now))
        db.expenseDao().insert(ExpenseRecord(amount = 1000, categoryId = 1, note = "午饭", occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().insert(ExpenseRecord(amount = 2000, categoryId = 2, note = "打车", occurredAt = now + 1000, createdAt = now, updatedAt = now))
        db.bigItemDao().insert(BigItem(name = "手机", amount = 599900, startDate = now, categoryId = 1, createdAt = now, updatedAt = now))
        db.bigItemDao().insert(
            BigItem(name = "旧电脑", amount = 899900, startDate = now - 86400000L, categoryId = null,
                status = BigItemStatus.ended, endedAt = now, createdAt = now, updatedAt = now)
        )
        // 一条软删记录（回收站）
        val idDel = db.expenseDao().insert(ExpenseRecord(amount = 500, categoryId = 3, note = "误删", occurredAt = now, createdAt = now, updatedAt = now))
        db.expenseDao().softDelete(idDel, now + 5000)
    }

    @Test
    fun `备份后清库再恢复_数据完全一致`() = runTest {
        seedData()
        store.edit { it[SettingsRepository.Keys.CURRENCY_SYMBOL] = "$" }
        store.edit { it[SettingsRepository.Keys.RECENT_LIMIT] = 10 }

        val json = repo.createBackupJson()
        val backup = repo.parseBackupJson(json)

        // 清空三表 + 偏好归零，模拟换机后的空库
        db.expenseDao().deleteAll()
        db.bigItemDao().deleteAll()
        db.categoryDao().deleteAll()
        store.state.value = emptyPreferences()

        repo.restore(backup)

        assertEquals(backup.categories, db.categoryDao().getAll())
        assertEquals(backup.expenseRecords, db.expenseDao().getAll())
        assertEquals(backup.bigItems, db.bigItemDao().getAll())
        // 软删记录随备份还原
        val restored = db.expenseDao().getAll()
        assertEquals(3, restored.size)
        assertTrue(restored.any { it.note == "误删" && it.deletedAt != null })
        // 大件状态还原
        val items = db.bigItemDao().getAll()
        assertEquals(BigItemStatus.ended, items.first { it.name == "旧电脑" }.status)
        // 偏好还原
        assertEquals("$", store.state.value[SettingsRepository.Keys.CURRENCY_SYMBOL])
        assertEquals(10, store.state.value[SettingsRepository.Keys.RECENT_LIMIT])
    }

    @Test
    fun `恢复保留原ID且外键关系正确`() = runTest {
        seedData()
        val backup = repo.parseBackupJson(repo.createBackupJson())

        db.expenseDao().deleteAll()
        db.bigItemDao().deleteAll()
        db.categoryDao().deleteAll()
        repo.restore(backup)

        val expenses = db.expenseDao().getAll()
        assertEquals(backup.expenseRecords.map { it.id }.toSet(), expenses.map { it.id }.toSet())
        // JOIN 查询能带出分类名，外键闭环
        val recent = db.expenseDao().observeRecent(10).first()
        assertTrue(recent.all { it.categoryName != null })
    }

    @Test
    fun `恢复覆盖备份之后新增的数据`() = runTest {
        seedData()
        val backup = repo.parseBackupJson(repo.createBackupJson())
        val countAtBackup = backup.expenseRecords.size

        // 备份后新增一条流水
        val now = 1700000000000L
        db.expenseDao().insert(ExpenseRecord(amount = 999, categoryId = 1, note = "备份后的新流水", occurredAt = now, createdAt = now, updatedAt = now))

        repo.restore(backup)

        val expenses = db.expenseDao().getAll()
        assertEquals(countAtBackup, expenses.size)
        assertTrue(expenses.none { it.note == "备份后的新流水" })
    }

    @Test
    fun `无效JSON抛BackupFormatException`() {
        assertThrows(BackupFormatException::class.java) {
            repo.parseBackupJson("not a json {")
        }
    }

    @Test
    fun `更高backupVersion抛BackupFormatException`() {
        val future = """
            {"backupVersion": 99, "createdAt": 0, "categories": [], "expenseRecords": [], "bigItems": [],
             "settings": {}}
        """.trimIndent()
        val ex = assertThrows(BackupFormatException::class.java) {
            repo.parseBackupJson(future)
        }
        assertTrue(ex.message!!.contains("升级"))
    }

    @Test
    fun `引用不存在分类的备份被拒`() {
        val bad = BackupData(
            createdAt = 0,
            categories = emptyList(),
            expenseRecords = listOf(
                ExpenseRecord(amount = 100, categoryId = 999, occurredAt = 1, createdAt = 1, updatedAt = 1)
            ),
            bigItems = emptyList(),
            settings = BackupSettings()
        )
        assertThrows(BackupFormatException::class.java) {
            repo.parseBackupJson(
                kotlinx.serialization.json.Json.encodeToString(BackupData.serializer(), bad)
            )
        }
    }

    @Test
    fun `连续恢复两次幂等`() = runTest {
        seedData()
        val backup = repo.parseBackupJson(repo.createBackupJson())

        repo.restore(backup)
        val first = db.expenseDao().getAll()
        repo.restore(backup)

        assertEquals(first, db.expenseDao().getAll())
    }

    @Test
    fun `空流水备份恢复成功`() = runTest {
        // 建库时 SeedCallback 已种 13 个分类，只清流水即得"空流水"备份
        val backup = repo.parseBackupJson(repo.createBackupJson())
        db.expenseDao().insert(
            ExpenseRecord(amount = 1000, categoryId = 1, note = "x", occurredAt = 1, createdAt = 1, updatedAt = 1)
        )

        val result = repo.restore(backup)

        assertEquals(0, result.expenseCount)
        assertEquals(0, db.expenseDao().getAll().size)
        assertEquals(13, result.categoryCount)
    }
}
