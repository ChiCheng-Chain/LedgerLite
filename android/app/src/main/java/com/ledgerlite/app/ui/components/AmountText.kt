package com.ledgerlite.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerlite.app.util.MoneyUtil

/**
 * 金额展示组件：符号 + 整数 + 小数，用 Row + 多 Text 组合。
 * 避免字体混排乱码。支出默认用 error 色（陶土橙）。
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
    showDecimals: Boolean = true,
    withGrouping: Boolean = true,
) {
    val negative = cents < 0
    val abs = if (negative) -cents else cents
    val yuanStr = MoneyUtil.centsToYuan(abs, withGrouping = withGrouping)
    val parts = yuanStr.split(".")
    val intPart = parts[0]
    val fracPart = if (parts.size > 1) parts[1] else "00"

    val sign = when {
        showSign && negative -> "−"
        showSign && !negative -> "+"
        else -> ""
    }

    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        if (sign.isNotEmpty()) {
            Text(
                text = sign,
                color = color,
                fontSize = (fontSize.value * 0.7f).sp,
                fontWeight = fontWeight
            )
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = currencySymbol,
            color = color,
            fontSize = (fontSize.value * 0.7f).sp,
            fontWeight = fontWeight
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = intPart,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
        if (showDecimals) {
            Text(
                text = ".$fracPart",
                color = color.copy(alpha = 0.75f),
                fontSize = (fontSize.value * 0.75f).sp,
                fontWeight = fontWeight
            )
        }
    }
}
