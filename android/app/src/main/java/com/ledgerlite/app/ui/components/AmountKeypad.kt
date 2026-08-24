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
 * current 为带小数点的输入串（如 "5" "5." "5.3" "5.30"）。
 * 小数点后最多位数跟随全局 [LocalDecimalConfig]；不显示小数时禁用小数点键。
 * 展示由 AmountDisplay 按规则动态补零。
 * @param current 当前输入串
 * @param onKey 按键回调，返回新串
 */
@Composable
fun AmountKeypad(
    current: String,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = LocalDecimalConfig.current
    val keys = listOf("7", "8", "9", "4", "5", "6", "1", "2", "3", ".", "0", "backspace")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    KeypadKey(
                        key = key,
                        modifier = Modifier.weight(1f),
                        enabled = key != "." || cfg.show,
                        onClick = { onKey(handleKey(key, current, cfg)) }
                    )
                }
            }
        }
    }
}

/** 处理按键，返回新串。小数点后位数受 cfg.places 限制；cfg.show=false 时禁用小数点。 */
private fun handleKey(key: String, current: String, cfg: DecimalConfig): String = when (key) {
    "backspace" -> if (current.isNotEmpty()) current.dropLast(1) else current
    "." -> if (cfg.show && !current.contains('.')) current + "." else current
    else -> {
        val dotIndex = current.indexOf('.')
        if (dotIndex >= 0 && current.length - dotIndex > cfg.places) current
        else if (current == "0") key
        else current + key
    }
}

@Composable
private fun KeypadKey(
    key: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val isBackspace = key == "backspace"
    val contentColor = if (enabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .height(56.dp)
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
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
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * 金额展示行：¥ + 大字数字。被记账面板、编辑面板、资产金额字段复用。
 * text 为带小数点的输入串（如 "5" "5." "5.3" "5.30"），按规则动态补零展示：
 * 无小数点 → "整数.0"；点后 0 位 → "整数.00"；点后 1 位 → 补 "0"；点后 2 位 → 原样。
 */
@Composable
fun AmountDisplay(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 48.sp,
    showTrailingDot: Boolean = true
) {
    val cfg = LocalDecimalConfig.current
    val displayText = formatAmountText(text, cfg)
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = LocalCurrencySymbol.current,
            style = AmountNumberStyle.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = (fontSize.value * 0.6f).sp
            )
        )
        androidx.compose.foundation.layout.Spacer(Modifier.padding(end = 4.dp))
        Text(
            text = displayText,
            style = AmountNumberStyle.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * 输入串 → 展示串，按 [cfg] 动态补零。
 * show=false：只显示整数（5 → "5"）
 * places=1：5 → "5.0"；5. → "5.0"；5.3 → "5.3"
 * places=2：5 → "5.00"；5. → "5.00"；5.3 → "5.30"
 */
private fun formatAmountText(text: String, cfg: DecimalConfig): String {
    if (text.isEmpty()) return if (cfg.show) "0.${"0".repeat(cfg.places)}" else "0"
    val dotIndex = text.indexOf('.')
    if (!cfg.show) {
        return if (dotIndex < 0) text else text.substring(0, dotIndex).ifEmpty { "0" }
    }
    return if (dotIndex < 0) {
        "$text.${"0".repeat(cfg.places)}"
    } else {
        val intPart = text.substring(0, dotIndex).ifEmpty { "0" }
        val frac = text.substring(dotIndex + 1)
        val padded = (frac + "0".repeat(cfg.places)).take(cfg.places)
        "$intPart.$padded"
    }
}
