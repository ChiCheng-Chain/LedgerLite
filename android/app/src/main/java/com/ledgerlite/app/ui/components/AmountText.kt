package com.ledgerlite.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerlite.app.util.MoneyUtil

/**
 * 金额展示组件：符号+币种 + 整数 + 小数，用 Row + 多 Text 组合。
 * 各段按基线对齐，避免小数点随整数位数上飘/下飘。
 */
@Composable
fun AmountText(
    cents: Long,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.error,
    fontSize: TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    currencySymbol: String = "¥",
    showSign: Boolean = false,
    showDecimals: Boolean? = null,
    withGrouping: Boolean = true,
) {
    val cfg = LocalDecimalConfig.current
    val showDecimalsFinal = showDecimals ?: cfg.show
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val yuanStr = MoneyUtil.centsToYuan(abs, withGrouping = withGrouping, decimalPlaces = cfg.places)
    val parts = yuanStr.split(".")
    val intPart = parts[0]
    val fracPart = if (parts.size > 1) parts[1] else ""

    val sign = when {
        showSign && negative -> "−"
        showSign && !negative -> "+"
        else -> ""
    }

    val prefix = if (sign.isNotEmpty()) "$sign$currencySymbol" else currencySymbol

    Row(modifier = modifier) {
        Text(
            text = prefix,
            color = color,
            fontSize = (fontSize.value * 0.7f).sp,
            fontWeight = fontWeight,
            modifier = Modifier.alignByBaseline()
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = intPart,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            modifier = Modifier.alignByBaseline()
        )
        if (showDecimalsFinal && fracPart.isNotEmpty()) {
            Text(
                text = ".$fracPart",
                color = color.copy(alpha = 0.75f),
                fontSize = (fontSize.value * 0.75f).sp,
                fontWeight = fontWeight,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}
