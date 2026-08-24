package com.ledgerlite.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.data.repository.CategoryRepository
import com.ledgerlite.app.data.repository.ExpenseRepository
import com.ledgerlite.app.data.repository.SettingsRepository
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordUiState(
    val todayTotal: Long = 0,
    val monthTotal: Long = 0,
    val recentExpenses: List<ExpenseWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModel(
    private val expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository,
    /** 跨 0 点重发当前日，驱动「今日/本月」窗口跨天重算。测试可注入有限流。 */
    private val dayStartFlow: Flow<Long> = DateUtil.observeDayStart()
) : ViewModel() {

    val uiState: StateFlow<RecordUiState> =
        dayStartFlow.flatMapLatest {
            combine(
                settingsRepository.recentLimit,
                categoryRepository.observeAll()
            ) { limit, categories -> limit to categories }
                .flatMapLatest { (limit, categories) ->
                    combine(
                        expenseRepository.observeTodayTotal(),
                        expenseRepository.observeMonthTotal(),
                        expenseRepository.observeRecent(limit)
                    ) { today, month, recent ->
                        RecordUiState(
                            todayTotal = today,
                            monthTotal = month,
                            recentExpenses = recent,
                            categories = categories,
                            isLoading = false
                        )
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, RecordUiState())

    fun createExpense(amount: Long, categoryId: Long, note: String, occurredAt: Long) {
        viewModelScope.launch {
            expenseRepository.create(amount, categoryId, note, occurredAt)
        }
    }

    fun updateExpense(record: ExpenseRecord) {
        viewModelScope.launch {
            expenseRepository.update(record)
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.softDelete(id)
        }
    }

    class Factory(
        private val expenseRepository: ExpenseRepository,
        private val categoryRepository: CategoryRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecordViewModel(expenseRepository, categoryRepository, settingsRepository) as T
    }
}
