package com.ledgerlite.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ledgerlite.app.util.MoneyUtil

/**
 * 金额输入底部弹窗。用于全屏页面（如资产编辑）的金额字段：点字段弹出，
 * 内含金额展示 + 计算器面板 + 确定按钮。确定后回填。
 * @param initial 初始金额串（元字符串，如 "12.34"）
 * @param onConfirm 确定回调，返回元字符串
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountInputSheet(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var input by remember { mutableStateOf(initial) }
    val cents = MoneyUtil.yuanToCents(input)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding()
        ) {
            AmountDisplay(text = input)
            Spacer(Modifier.height(16.dp))
            AmountKeypad(current = input, onKey = { input = it })
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onConfirm(MoneyUtil.centsToYuan(cents, withGrouping = false)) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = cents > 0,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("确定", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
