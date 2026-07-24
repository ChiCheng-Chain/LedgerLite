package com.ledgerlite.app.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerlite.app.ui.theme.AmountNumberStyle
import com.ledgerlite.app.util.MoneyUtil
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 日支出趋势折线图。带渐变填充 + 触点提示。
 * @param points 每天的支出，按时间正序。label 为日期标签。
 */
@Composable
fun TrendLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 180.dp
) {
    if (points.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = max(points.maxOf { it.value }, 1L)
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 800))
    }

    // 触点索引
    var touchedIndex by remember { mutableStateOf(-1) }

    Box(modifier = modifier.fillMaxWidth().height(height + 24.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        if (points.size >= 2) {
                            val w = size.width.toFloat()
                            val stepX = w / (points.size - 1)
                            val idx = (offset.x / stepX).toInt().coerceIn(0, points.size - 1)
                            touchedIndex = idx
                        } else if (points.size == 1) {
                            touchedIndex = 0
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val padBottom = 24f
            val chartH = h - padBottom
            val stepX = if (points.size > 1) w / (points.size - 1) else w / 2

            // 构建路径
            val path = Path()
            val fillPath = Path()
            val coords = points.mapIndexed { i, p ->
                val x = i * stepX
                val y = chartH - (p.value.toFloat() / maxVal) * chartH * progress.value
                Offset(x, y)
            }
            if (coords.isNotEmpty()) {
                path.moveTo(coords[0].x, coords[0].y)
                coords.drop(1).forEach { path.lineTo(it.x, it.y) }
                fillPath.addPath(path)
                fillPath.lineTo(coords.last().x, chartH)
                fillPath.lineTo(coords.first().x, chartH)
                fillPath.close()
            }

            // 渐变填充
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f)),
                    startY = 0f,
                    endY = chartH
                )
            )
            // 折线
            drawPath(path, color = lineColor, style = Stroke(width = 3f))

            // 触点高亮
            if (touchedIndex in points.indices) {
                val c = coords[touchedIndex]
                drawCircle(color = lineColor, radius = 6f, center = c)
                drawCircle(color = Color.White, radius = 3f, center = c)
            }
        }

        // 触点提示气泡 + 日期标签
        if (touchedIndex in points.indices) {
            val p = points[touchedIndex]
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${p.label}  ¥${MoneyUtil.centsToYuan(p.value)}",
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

data class ChartPoint(val label: String, val value: Long)
