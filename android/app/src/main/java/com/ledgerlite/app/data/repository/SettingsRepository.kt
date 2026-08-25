package com.ledgerlite.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
 *
 * setter 直接在 edit 提供的 MutablePreferences 上赋值——
 * 不要套 toMutablePreferences().toPreferences()，那会切断 DataStore 的修改检测导致写入丢失。
 */
class SettingsRepository(
    context: Context? = null,
    /** 偏好数据源。默认 DataStore，测试可注入内存流。 */
    private val preferences: Flow<Preferences> =
        requireNotNull(context).applicationContext.appDataStore.data,
    private val editor: suspend (suspend (MutablePreferences) -> Unit) -> Unit = { mutate ->
        requireNotNull(context).applicationContext.appDataStore.edit { prefs -> mutate(prefs) }
    }
) {

    object Keys {
        val DEFAULT_HOME = stringPreferencesKey("default_home")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val SHOW_DECIMALS = booleanPreferencesKey("show_decimals")
        val DECIMAL_PLACES = intPreferencesKey("decimal_places")
        val RECENT_LIMIT = intPreferencesKey("recent_limit")
    }

    val defaultHome: Flow<String> = preferences.map { it[Keys.DEFAULT_HOME] ?: "record" }
    val currencySymbol: Flow<String> = preferences.map { it[Keys.CURRENCY_SYMBOL] ?: "¥" }
    val showDecimals: Flow<Boolean> = preferences.map { it[Keys.SHOW_DECIMALS] ?: true }
    val decimalPlaces: Flow<Int> = preferences.map { (it[Keys.DECIMAL_PLACES] ?: 2).coerceIn(1, 2) }
    val recentLimit: Flow<Int> = preferences.map { it[Keys.RECENT_LIMIT] ?: 5 }

    suspend fun setDefaultHome(value: String) {
        edit { prefs -> prefs[Keys.DEFAULT_HOME] = value }
    }

    suspend fun setCurrencySymbol(value: String) {
        edit { prefs -> prefs[Keys.CURRENCY_SYMBOL] = value }
    }

    suspend fun setShowDecimals(value: Boolean) {
        edit { prefs -> prefs[Keys.SHOW_DECIMALS] = value }
    }

    suspend fun setDecimalPlaces(value: Int) {
        edit { prefs -> prefs[Keys.DECIMAL_PLACES] = value.coerceIn(1, 2) }
    }

    suspend fun setRecentLimit(value: Int) {
        edit { prefs -> prefs[Keys.RECENT_LIMIT] = value.coerceIn(1, 20) }
    }

    private suspend fun edit(mutate: suspend (MutablePreferences) -> Unit) {
        editor(mutate)
    }
}
