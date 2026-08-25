package com.ledgerlite.app.data.repository

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.ledgerlite.app.data.local.LedgerDatabase
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 备份文件解析或校验失败。 */
class BackupFormatException(message: String) : Exception(message)

/** 备份中的偏好设置，与 SettingsRepository.Keys 五个键一一对应。 */
@Serializable
data class BackupSettings(
    val defaultHome: String = "record",
    val currencySymbol: String = "¥",
    val showDecimals: Boolean = true,
    val decimalPlaces: Int = 2,
    val recentLimit: Int = 5
)

/**
 * 全量备份载体。backupVersion 是备份格式自身的版本（与 Room schema version 无关），
 * 未来格式变化时递增并在 parseBackupJson 中处理兼容。
 */
@Serializable
data class BackupData(
    val backupVersion: Int = 1,
    val createdAt: Long,
    val categories: List<Category>,
    val expenseRecords: List<ExpenseRecord>,
    val bigItems: List<BigItem>,
    val settings: BackupSettings
)

/** 恢复结果条数，供 UI 提示。 */
data class RestoreResult(
    val categoryCount: Int,
    val expenseCount: Int,
    val bigItemCount: Int
)

/**
 * 备份与恢复。核心三方法不依赖 ContentResolver，可用内存库直测；
 * SAF 文件 IO 由顶层 [writeTextToUri]/[readTextFromUri] 承担。
 */
class BackupRepository(
    private val database: LedgerDatabase,
    private val settingsRepository: SettingsRepository
) {
    // ignoreUnknownKeys + 字段默认值：旧版本备份在新版本应用中仍可解码
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    /** 读三表全量（含软删记录）+ 当前偏好，序列化为备份 JSON。 */
    suspend fun createBackupJson(): String {
        val backup = BackupData(
            createdAt = System.currentTimeMillis(),
            categories = database.categoryDao().getAll(),
            expenseRecords = database.expenseDao().getAll(),
            bigItems = database.bigItemDao().getAll(),
            settings = BackupSettings(
                defaultHome = settingsRepository.defaultHome.first(),
                currencySymbol = settingsRepository.currencySymbol.first(),
                showDecimals = settingsRepository.showDecimals.first(),
                decimalPlaces = settingsRepository.decimalPlaces.first(),
                recentLimit = settingsRepository.recentLimit.first()
            )
        )
        return json.encodeToString(BackupData.serializer(), backup)
    }

    /** 解析并做结构校验，不触碰数据库。失败抛 [BackupFormatException]。 */
    fun parseBackupJson(text: String): BackupData {
        val backup = try {
            json.decodeFromString(BackupData.serializer(), text)
        } catch (e: SerializationException) {
            throw BackupFormatException("备份文件格式无效")
        } catch (e: IllegalArgumentException) {
            throw BackupFormatException("备份文件格式无效")
        }
        if (backup.backupVersion > CURRENT_BACKUP_VERSION) {
            throw BackupFormatException("备份来自更新版本的 LedgerLite，请先升级应用")
        }
        // 引用完整性预检：避免用户确认后才撞外键约束
        val categoryIds = backup.categories.map { it.id }.toSet()
        val badExpense = backup.expenseRecords.any { it.categoryId !in categoryIds }
        val badItem = backup.bigItems.any { it.categoryId != null && it.categoryId !in categoryIds }
        if (badExpense || badItem) {
            throw BackupFormatException("备份中的记录引用了不存在的分类")
        }
        return backup
    }

    /**
     * 全量覆盖恢复：事务内清空三表后按外键顺序重插（保留原 id）。
     * 事务成功后才写偏好；任一步失败则库回滚、当前数据无损。
     */
    suspend fun restore(backup: BackupData): RestoreResult {
        database.withTransaction {
            // 先删子表后删父表（FK NO_ACTION），插入顺序反之
            database.expenseDao().deleteAll()
            database.bigItemDao().deleteAll()
            database.categoryDao().deleteAll()
            database.categoryDao().insertAll(backup.categories)
            database.expenseDao().insertAll(backup.expenseRecords)
            database.bigItemDao().insertAll(backup.bigItems)
        }
        with(settingsRepository) {
            setDefaultHome(backup.settings.defaultHome)
            setCurrencySymbol(backup.settings.currencySymbol)
            setShowDecimals(backup.settings.showDecimals)
            setDecimalPlaces(backup.settings.decimalPlaces)
            setRecentLimit(backup.settings.recentLimit)
        }
        return RestoreResult(
            categoryCount = backup.categories.size,
            expenseCount = backup.expenseRecords.size,
            bigItemCount = backup.bigItems.size
        )
    }

    companion object {
        const val CURRENT_BACKUP_VERSION = 1
    }
}

// ---- SAF 文件 IO 胶水（不参与单测，核心逻辑在 BackupRepository 内）----

/** "wt" = write + truncate：覆盖已存在文件时不留旧内容脏尾。 */
suspend fun writeTextToUri(context: Context, uri: Uri, text: String) {
    withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("无法打开备份文件写入")
    }
}

suspend fun readTextFromUri(context: Context, uri: Uri): String {
    return withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error("无法读取所选文件")
    }
}
