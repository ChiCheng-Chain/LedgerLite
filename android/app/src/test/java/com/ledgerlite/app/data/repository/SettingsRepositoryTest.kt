package com.ledgerlite.app.data.repository

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 设置写入回归测试。
 *
 * 背景：曾因 setter 里套 toMutablePreferences().toPreferences() 修改副本并返回新对象，
 * 而 DataStore.edit 只保留对传入 MutablePreferences 的就地修改（返回值被丢弃），
 * 导致所有设置项点击后 UI 状态不变化、不持久化。
 * 此处用内存版 editor 模拟 DataStore.edit 的就地修改语义，验证 setter 链路真的会写入。
 */
class SettingsRepositoryTest {

    /** 内存版 DataStore：edit 语义与真实实现一致——只保留对传入 prefs 的就地修改。 */
    private class InMemoryStore {
        val state = MutableStateFlow<Preferences>(emptyPreferences())

        val preferences: Flow<Preferences> = state

        suspend fun edit(mutate: suspend (MutablePreferences) -> Unit) {
            val copy = state.value.toMutablePreferences()
            mutate(copy)
            state.value = copy.toPreferences()
        }
    }

    private fun repo(store: InMemoryStore) = SettingsRepository(
        preferences = store.preferences,
        editor = { mutate -> store.edit(mutate) }
    )

    @Test
    fun `setRecentLimit 写入后可读回`() = runTest {
        val store = InMemoryStore()
        val repository = repo(store)

        repository.setRecentLimit(10)

        assertEquals(10, store.state.value[SettingsRepository.Keys.RECENT_LIMIT])
    }

    @Test
    fun `setShowDecimals 写入后可读回`() = runTest {
        val store = InMemoryStore()
        val repository = repo(store)

        repository.setShowDecimals(false)

        assertEquals(false, store.state.value[SettingsRepository.Keys.SHOW_DECIMALS])
    }

    @Test
    fun `setCurrencySymbol 写入后可读回`() = runTest {
        val store = InMemoryStore()
        val repository = repo(store)

        repository.setCurrencySymbol("$")

        assertEquals("$", store.state.value[SettingsRepository.Keys.CURRENCY_SYMBOL])
    }

    @Test
    fun `setDefaultHome 写入后可读回`() = runTest {
        val store = InMemoryStore()
        val repository = repo(store)

        repository.setDefaultHome("stats")

        assertEquals("stats", store.state.value[SettingsRepository.Keys.DEFAULT_HOME])
    }

    @Test
    fun `setDecimalPlaces 超界值被钳制`() = runTest {
        val store = InMemoryStore()
        val repository = repo(store)

        repository.setDecimalPlaces(5)

        assertEquals(2, store.state.value[SettingsRepository.Keys.DECIMAL_PLACES])
    }

    @Test
    fun `多次写入保留其他键`() = runTest {
        val store = InMemoryStore()
        val repository = repo(store)

        repository.setRecentLimit(3)
        repository.setCurrencySymbol("€")

        val saved = store.state.value
        assertEquals(3, saved[SettingsRepository.Keys.RECENT_LIMIT])
        assertEquals("€", saved[SettingsRepository.Keys.CURRENCY_SYMBOL])
    }

    @Test
    fun `读取 Flow 反映写入后的值`() = runTest {
        val store = InMemoryStore()
        val repository = repo(store)

        repository.setRecentLimit(15)

        assertEquals(15, repository.recentLimit.first())
    }
}
