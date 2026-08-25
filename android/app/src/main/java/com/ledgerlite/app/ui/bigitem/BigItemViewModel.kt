package com.ledgerlite.app.ui.bigitem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ledgerlite.app.data.local.entity.BigItem
import com.ledgerlite.app.data.repository.BigItemRepository
import com.ledgerlite.app.domain.model.BigItemStatus
import com.ledgerlite.app.util.AmortizationUtil
import com.ledgerlite.app.util.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BigItemListUiState(
    val items: List<BigItem> = emptyList(),
    val totalDailyCost: Long = 0,
    val totalWeeklyCost: Long = 0,
    val isLoading: Boolean = true
)

class BigItemViewModel(
    private val bigItemRepository: BigItemRepository,
    /** 跨 0 点重发当前日，驱动摊销天数跨天重算。测试可注入有限流。 */
    private val dayStartFlow: Flow<Long> = DateUtil.observeDayStart()
) : ViewModel() {

    val uiState: StateFlow<BigItemListUiState> =
        combine(bigItemRepository.observeAll(), dayStartFlow) { items, dayStart ->
            BigItemListUiState(
                // 使用中的按创建时间倒序在前，已结束的垫底（按结束时间倒序）
                items = items.sortedWith(
                    compareBy<BigItem> { it.status == BigItemStatus.ended }
                        .thenByDescending { if (it.status == BigItemStatus.ended) it.endedAt ?: it.updatedAt else it.createdAt }
                ),
                totalDailyCost = AmortizationUtil.totalDailyCost(items, dayStart),
                totalWeeklyCost = AmortizationUtil.totalWeeklyCost(items, dayStart),
                isLoading = false
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, BigItemListUiState())

    fun create(
        name: String,
        amount: Long,
        startDate: Long,
        note: String
    ) {
        viewModelScope.launch {
            bigItemRepository.create(name, amount, startDate, note = note)
        }
    }

    fun update(item: BigItem) {
        viewModelScope.launch { bigItemRepository.update(item) }
    }

    /** 编辑保存：取出现有资产，合并新字段后更新。 */
    fun updateExisting(
        id: Long,
        name: String,
        amount: Long,
        startDate: Long,
        note: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val existing = bigItemRepository.getById(id) ?: return@launch
            bigItemRepository.update(existing.copy(
                name = name, amount = amount, startDate = startDate, note = note
            ))
            onDone()
        }
    }

    fun endItem(id: Long) {
        viewModelScope.launch { bigItemRepository.endItem(id) }
    }

    suspend fun getById(id: Long): BigItem? = bigItemRepository.getById(id)

    class Factory(private val bigItemRepository: BigItemRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BigItemViewModel(bigItemRepository) as T
    }
}
