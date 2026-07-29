package com.ledgerlite.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.TimeZone

/**
 * 全局日期选择面板：ModalBottomSheet 包 DatePicker，统一底部弹出风格。
 * @param initialSelectedDayStart 选中日期的本地 0 点 epoch；null 表示不预选。
 * @param title 顶部标题。
 * @param onConfirm 返回选中日期的本地 0 点 epoch；用户取消时 onDismiss。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerSheet(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    initialSelectedDayStart: Long? = null,
    title: String = "选择日期"
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tzOffset = TimeZone.getDefault().getOffset(0).toLong()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDayStart?.let { it - tzOffset }
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            DatePicker(state = state, showModeToggle = false)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("取消") }
                Button(
                    onClick = {
                        state.selectedDateMillis?.let { utc ->
                            onConfirm(utc + tzOffset)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = state.selectedDateMillis != null
                ) { Text("确定", color = MaterialTheme.colorScheme.onPrimary) }
            }
        }
    }
}
