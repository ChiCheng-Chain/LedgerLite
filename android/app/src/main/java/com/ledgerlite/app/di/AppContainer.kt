package com.ledgerlite.app.di

import android.content.Context
import com.ledgerlite.app.data.local.LedgerDatabase
import com.ledgerlite.app.data.repository.BackupRepository
import com.ledgerlite.app.data.repository.BigItemRepository
import com.ledgerlite.app.data.repository.CategoryRepository
import com.ledgerlite.app.data.repository.ExpenseRepository
import com.ledgerlite.app.data.repository.SettingsRepository
import com.ledgerlite.app.data.repository.StatisticsRepository

/**
 * 进程级唯一组合根。所有依赖 by lazy 延迟构造。
 * Room Database 必须进程级唯一，不能在 Composable / Factory 里反复构造。
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: LedgerDatabase by lazy { LedgerDatabase.create(appContext) }

    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(database.expenseDao()) }
    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val bigItemRepository: BigItemRepository by lazy { BigItemRepository(database.bigItemDao()) }
    val statisticsRepository: StatisticsRepository by lazy {
        StatisticsRepository(expenseRepository, bigItemRepository)
    }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val backupRepository: BackupRepository by lazy { BackupRepository(database, settingsRepository) }
}
