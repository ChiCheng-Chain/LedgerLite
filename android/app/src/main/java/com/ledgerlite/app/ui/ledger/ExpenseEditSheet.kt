package com.ledgerlite.app.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerlite.app.data.local.entity.Category
import com.ledgerlite.app.data.local.entity.ExpenseRecord
import com.ledgerlite.app.ui.components.AmountDisplay
import com.ledgerlite.app.ui.components.AmountKeypad
import com.ledgerlite.app.ui.components.CategoryChip
import com.ledgerlite.app.util.MoneyUtil

/**
 * 编辑流水面板。与 QuickEntrySheet 同款的计算器数字面板，预填金额/分类/备注。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditSheet(
    record: ExpenseRecord,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (amount: Long, categoryId: Long, note: String, occurredAt: Long) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 预填金额：分转元字符串（不带千分位），如 1234 → "12.34"
    var amountInput by remember {
        mutableStateOf(MoneyUtil.centsToYuan(record.amount, withGrouping = false))
    }
    var selectedCategoryId by remember { mutableStateOf(record.categoryId) }
    var note by remember { mutableStateOf(record.note) }

    val cents = MoneyUtil.yuanToCents(amountInput)
    val canSubmit = cents > 0 && selectedCategoryId > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding()
        ) {
            // 金额展示区
            AmountDisplay(text = amountInput)

            // 分类 Chip 行
            if (categories.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { category ->
                        CategoryChip(
                            label = category.name,
                            selected = category.id == selectedCategoryId,
                            onClick = { selectedCategoryId = category.id },
                            accentColor = Color(category.color)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 4×3 数字网格
            AmountKeypad(current = amountInput, onKey = { amountInput = it })

            Spacer(Modifier.height(12.dp))

            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选）") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(Modifier.height(12.dp))

            // 删除 + 保存
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
                Button(
                    onClick = {
                        if (canSubmit) {
                            onSave(cents, selectedCategoryId, note, record.occurredAt)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = canSubmit,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("保存", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
