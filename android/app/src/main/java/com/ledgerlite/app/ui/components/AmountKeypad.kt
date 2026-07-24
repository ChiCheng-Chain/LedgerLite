package com.ledgerlite.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerlite.app.ui.theme.AmountNumberStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * 金额计算器面板。4×3 网格：7 8 9 / 4 5 6 / 1 2 3 / . 0 ⌫。
 * 纯输入（不做运算），小数点后最多两位。
 * @param current 当前输入串（用户视角的元字符串，如 "0.5" "152.3"）
 * @param onKey 按键回调，返回新串
 */
@Composable
fun AmountKeypad(
    current: String,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0", "backspace")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    KeypadKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = { onKey(handleKey(key, current)) }
                    )
                }
            }
        }
    }
}

/** 处理按键，返回新串。限制小数点后最多两位。 */
private fun handleKey(key: String, current: String): String = when (key) {
    "backspace" -> if (current.isNotEmpty()) current.dropLast(1) else current
    "." -> if (!current.contains('.')) current + "." else current
    else -> {
        val dotIndex = current.indexOf('.')
        if (dotIndex >= 0 && current.length - dotIndex > 2) current
        else if (current == "0") key
        else current + key
    }
}

@Composable
private fun KeypadKey(
    key: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isBackspace = key == "backspace"
    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isBackspace) {
            Icon(
                Icons.Outlined.Backspace,
                contentDescription = "退格",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = key,
                style = AmountNumberStyle.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * 金额展示行：¥ + 大字数字。被记账面板、编辑面板、资产金额字段复用。
 */
@Composable
fun AmountDisplay(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 48.sp,
    showTrailingDot: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = "¥",
            style = AmountNumberStyle.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (fontSize.value * 0.6f).sp
            )
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(end = 4.dp))
        Text(
            text = if (text.isEmpty()) "0" else text,
            style = AmountNumberStyle.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        )
        if (showTrailingDot && text.isNotEmpty() && !text.contains('.')) {
            Text(
                text = ".",
                style = AmountNumberStyle.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontSize = (fontSize.value * 0.85f).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
