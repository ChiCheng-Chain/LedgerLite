package com.ledgerlite.app.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.relation.ExpenseWithCategory
import com.ledgerlite.app.data.repository.CategoryRepository
import com.ledgerlite.app.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecordUiState(
    val todayTotal: Long = 0,
    val monthTotal: Long = 0,
    val recentExpenses: List<ExpenseWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

class RecordViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<RecordUiState> =
        combine(
            expenseRepository.observeTodayTotal(),
            expenseRepository.observeMonthTotal(),
            expenseRepository.observeRecent(5),
            categoryRepository.observeAll()
        ) { today, month, recent, categories ->
            RecordUiState(
                todayTotal = today,
                monthTotal = month,
                recentExpenses = recent,
                categories = categories,
                isLoading = false
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, RecordUiState())

    fun createExpense(amount: Long, categoryId: Long, note: String, occurredAt: Long) {
        viewModelScope.launch {
            expenseRepository.create(amount, categoryId, note, occurredAt)
        }
    }

    class Factory(
        private val expenseRepository: ExpenseRepository,
        private val categoryRepository: CategoryRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecordViewModel(expenseRepository, categoryRepository) as T
    }
}
