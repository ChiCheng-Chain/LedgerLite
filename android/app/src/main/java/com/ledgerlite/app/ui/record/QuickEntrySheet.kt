package com.ledgerlite.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.ledgerlite.app.ui.components.AppDatePickerSheet
import com.ledgerlite.app.ui.components.CategoryChip
import com.ledgerlite.app.util.MoneyUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 底部记账面板。UI 自带计算器式数字面板，用户直接点数字+小数点输入"0.5""152.3"。
 * 布局：金额展示 → 分类 Chip 行 → 4×3 数字网格(. 0 ⌫)→ 完成按钮。
 * 不做运算，纯输入。完成时用 yuanToCents 转成分存储。
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
    var noteInput by remember { mutableStateOf("") }

    // 选中日期（本地 0 点 epoch），默认今天
    val cal = remember { Calendar.getInstance() }
    val todayStart = remember {
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    var selectedDayStart by remember { mutableStateOf(todayStart) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("yyyy/M/d", Locale.getDefault()) }

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
            // 日期（默认今天，可改为过去日期补记），点击切换
            Text(
                text = dateFmt.format(Date(selectedDayStart)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { showDatePicker = true }
                    .padding(vertical = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            // 金额展示区
            AmountDisplay(text = amountInput)

            // 分类区（自动换行，全部展示）
            if (categories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
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

            Spacer(Modifier.height(12.dp))

            // 备注
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("备注（可选）") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (canSubmit) {
                        // 选中日期的本地 0 点 + 当前时分
                        val now = Calendar.getInstance()
                        val occurredAt = selectedDayStart +
                            (now.get(Calendar.HOUR_OF_DAY) * 3600_000L) +
                            (now.get(Calendar.MINUTE) * 60_000L)
                        onSubmit(cents, selectedCategoryId, noteInput.trim(), occurredAt)
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

    if (showDatePicker) {
        AppDatePickerSheet(
            onDismiss = { showDatePicker = false },
            onConfirm = { dayStart ->
                selectedDayStart = dayStart
                showDatePicker = false
            },
            initialSelectedDayStart = selectedDayStart,
            title = "选择日期"
        )
    }
}
