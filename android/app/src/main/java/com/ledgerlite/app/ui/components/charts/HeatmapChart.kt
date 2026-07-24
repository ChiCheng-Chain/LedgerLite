package com.ledgerlite.app.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerlite.app.ui.theme.AmountNumberStyle
import com.ledgerlite.app.util.MoneyUtil
import kotlin.math.max

/**
 * 日支出热力图。每格一天，颜色深浅代表支出多少。横向按周排列。
 * @param cells 按时间正序，每个 cell 一天。
 */
@Composable
fun HeatmapChart(
    cells: List<HeatmapCell>,
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.primary
) {
    if (cells.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
            Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(cells) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 600))
    }

    val maxVal = max(cells.maxOfOrNull { it.value } ?: 0L, 1L)
    var touched by remember { mutableStateOf<HeatmapCell?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(cells) {
                    detectTapGestures { offset ->
                        // 简单定位：估算点击的格子
                        val cols = (size.width / 26f).toInt().coerceAtLeast(1)
                        val rows = (cells.size + cols - 1) / cols
                        val cellW = size.width / cols
                        val cellH = size.height / rows
                        val col = (offset.x / cellW).toInt().coerceIn(0, cols - 1)
                        val row = (offset.y / cellH).toInt().coerceIn(0, rows - 1)
                        val idx = row * cols + col
                        touched = cells.getOrNull(idx)
                    }
                }
        ) {
            val cols = (size.width / 26f).toInt().coerceAtLeast(1)
            val rows = (cells.size + cols - 1) / cols
            val cellW = size.width / cols
            val cellH = size.height / rows
            val gap = 3f
            cells.forEachIndexed { i, cell ->
                val col = i % cols
                val row = i / cols
                val intensity = if (cell.value <= 0) 0f else (cell.value.toFloat() / maxVal)
                // 4 档颜色
                val alpha = when {
                    cell.value <= 0 -> 0.08f
                    intensity < 0.25f -> 0.25f
                    intensity < 0.5f -> 0.5f
                    intensity < 0.75f -> 0.75f
                    else -> 1f
                }
                drawRoundRect(
                    color = baseColor.copy(alpha = alpha * progress.value),
                    topLeft = Offset(col * cellW + gap / 2, row * cellH + gap / 2),
                    size = Size(cellW - gap, cellH - gap),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                )
            }
        }

        // 触点提示
        touched?.let { cell ->
            Text(
                text = "${cell.label}  ¥${MoneyUtil.centsToYuan(cell.value)}",
                style = AmountNumberStyle.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        // 图例
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("少", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(0.08f, 0.25f, 0.5f, 0.75f, 1f).forEach { a ->
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(baseColor.copy(alpha = a)))
            }
            Text("多", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class HeatmapCell(val label: String, val value: Long)
