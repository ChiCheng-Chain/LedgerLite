package com.ledgerlite.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 进程级唯一 DataStore 实例（扩展属性，全局单例）。 */
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "ledger_settings")

/**
 * 本地偏好设置。封装 DataStore，每项一个 Flow + suspend setter。
 * DataStore 必须进程级唯一（由 Context.appDataStore 扩展属性保证），不能在 Composable 里反复构造。
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.appDataStore

    object Keys {
        val DEFAULT_HOME = stringPreferencesKey("default_home")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val SHOW_DECIMALS = booleanPreferencesKey("show_decimals")
        val RECENT_LIMIT = intPreferencesKey("recent_limit")
    }

    val defaultHome: Flow<String> = dataStore.data.map { it[Keys.DEFAULT_HOME] ?: "record" }
    val currencySymbol: Flow<String> = dataStore.data.map { it[Keys.CURRENCY_SYMBOL] ?: "¥" }
    val showDecimals: Flow<Boolean> = dataStore.data.map { it[Keys.SHOW_DECIMALS] ?: true }
    val recentLimit: Flow<Int> = dataStore.data.map { it[Keys.RECENT_LIMIT] ?: 5 }

    suspend fun setDefaultHome(value: String) {
        dataStore.edit { it[Keys.DEFAULT_HOME] = value }
    }

    suspend fun setCurrencySymbol(value: String) {
        dataStore.edit { it[Keys.CURRENCY_SYMBOL] = value }
    }

    suspend fun setShowDecimals(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_DECIMALS] = value }
    }

    suspend fun setRecentLimit(value: Int) {
        dataStore.edit { it[Keys.RECENT_LIMIT] = value.coerceIn(1, 20) }
    }
}
