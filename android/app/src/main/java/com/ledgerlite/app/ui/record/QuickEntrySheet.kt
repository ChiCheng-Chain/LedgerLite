package com.ledgerlite.app.ui.record

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.ledgerlite.app.ui.components.AmountDisplay
import com.ledgerlite.app.ui.components.AmountKeypad
import com.ledgerlite.app.ui.components.CategoryChip
import com.ledgerlite.app.util.MoneyUtil

/**
 * 底部记账面板。UI 自带计算器式数字面板，用户直接点数字+小数点输入"0.5""152.3"。
 * 布局：金额展示 → 分类 Chip 行 → 4×3 数字网格(. 0 ⌫)→ 完成按钮。
 * 不做运算，纯输入。完成时用 yuanToCents 转成分存储。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntrySheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSubmit: (amountCents: Long, categoryId: Long, note: String, occurredAt: Long) -> Unit,
    presetCategoryId: Long? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 金额输入串，直接是用户视角的元字符串，如 "0.5" "152.3"
    var amountInput by remember { mutableStateOf("") }
    var selectedCategoryId by remember {
        mutableStateOf(presetCategoryId ?: categories.firstOrNull()?.id ?: 0L)
    }

    val cents = MoneyUtil.yuanToCents(amountInput)
    val canSubmit = cents > 0 && selectedCategoryId > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            // 金额展示区
            AmountDisplay(text = amountInput)

            // 分类 Chip 行（面板内）
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

            Spacer(Modifier.height(16.dp))

            // 4×3 数字网格
            AmountKeypad(current = amountInput, onKey = { amountInput = it })

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (canSubmit) {
                        onSubmit(cents, selectedCategoryId, "", System.currentTimeMillis())
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = canSubmit,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("完成", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
