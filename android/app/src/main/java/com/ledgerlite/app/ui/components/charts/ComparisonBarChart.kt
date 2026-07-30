package com.ledgerlite.app.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerlite.app.ui.components.LocalDecimalConfig
import com.ledgerlite.app.ui.theme.AmountNumberStyle
import com.ledgerlite.app.util.MoneyUtil
import kotlin.math.max

/**
 * 对比柱状图。多根柱子横向排列，用于近 N 周/月支出对比。
 * @param bars 每根柱子，label 为周/月标签，value 为该周期支出。
 */
@Composable
fun ComparisonBarChart(
    bars: List<ChartPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 160.dp
) {
    if (bars.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = max(bars.maxOf { it.value }, 1L)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 700))
    }
    var touched by remember { mutableStateOf(-1) }

    Box(modifier = modifier.fillMaxWidth().height(height + 24.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(bars) {
                    detectTapGestures { offset ->
                        val barW = size.width / bars.size
                        val idx = (offset.x / barW).toInt().coerceIn(0, bars.size - 1)
                        touched = idx
                    }
                }
        ) {
            val padBottom = 24f
            val chartH = size.height - padBottom
            val barW = size.width / bars.size
            val barInnerW = barW * 0.5f
            val barGap = (barW - barInnerW) / 2f

            bars.forEachIndexed { i, bar ->
                val barH = (bar.value.toFloat() / maxVal) * chartH * progress.value
                val x = i * barW + barGap
                val y = chartH - barH
                drawRoundRect(
                    color = if (i == touched) barColor.copy(alpha = 1f) else barColor.copy(alpha = 0.7f),
                    topLeft = Offset(x, y),
                    size = Size(barInnerW, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
            }
        }

        // 触点提示
        if (touched in bars.indices) {
            val b = bars[touched]
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${b.label}  ¥${MoneyUtil.centsToYuan(b.value, decimalPlaces = LocalDecimalConfig.current.run { if (show) places else 0 })}",
                    style = AmountNumberStyle.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}
