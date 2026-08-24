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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerlite.app.ui.components.LocalCurrencySymbol
import com.ledgerlite.app.ui.components.LocalDecimalConfig
import com.ledgerlite.app.ui.theme.AmountNumberStyle
import com.ledgerlite.app.util.MoneyUtil
import java.util.Calendar
import kotlin.math.max

/**
 * 日支出热力图，10 列 × 5 行 = 50 格网格，每格一天。
 * 范围内每一天都画方块：无支出用中性灰底，有支出按深浅染色。
 * @param cells 按时间正序，每个 cell 一天（timestamp 为本地 0 点 epoch）。
 */
@Composable
fun HeatmapChart(
    cells: List<HeatmapCell>,
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.primary,
    cols: Int = 14,
    rows: Int = 6
) {
    val days = cols * rows
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    val progress = remember { Animatable(0f) }
    LaunchedEffect(cells) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 600))
    }

    // 用 timestamp（本地 0 点 epoch）建索引。
    val dataMap = remember(cells) { cells.associate { it.timestamp to it } }

    // 窗口：从今天往回铺 days 天。todayStart 不缓存——上层跨 0 点重发数据时
    // 这里必须跟着移动，否则跨天后格子整体偏移一天。
    val todayStart = remember(cells) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val windowStart = todayStart - (days - 1) * 86_400_000L

    val maxVal = max(cells.maxOfOrNull { it.value } ?: 0L, 1L)
    var touched by remember { mutableStateOf<HeatmapCell?>(null) }

    // 方块按容器尺寸均分：宽=容器宽/列，高=容器高/行，整片铺满。
    val gapPx = with(androidx.compose.ui.platform.LocalDensity.current) { 3.dp.toPx() }
    val gridHeightDp = 150.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeightDp)
                .pointerInput(cells) {
                    detectTapGestures { offset ->
                        val cellW = (size.width - (cols - 1) * gapPx) / cols
                        val cellH = (size.height - (rows - 1) * gapPx) / rows
                        val col = (offset.x / (cellW + gapPx)).toInt()
                        val row = (offset.y / (cellH + gapPx)).toInt()
                        if (col in 0 until cols && row in 0 until rows) {
                            val slot = row * cols + col
                            val ts = windowStart + slot * 86_400_000L
                            val cell = dataMap[ts]
                            if (cell != null) touched = cell
                        }
                    }
                }
        ) {
            val cellW = (size.width - (cols - 1) * gapPx) / cols
            val cellH = (size.height - (rows - 1) * gapPx) / rows
            for (slot in 0 until days) {
                val ts = windowStart + slot * 86_400_000L
                val col = slot % cols
                val row = slot / cols
                val cell = dataMap[ts]
                val value = cell?.value ?: 0L
                val color = if (value <= 0) {
                    emptyColor.copy(alpha = 0.5f * progress.value)
                } else {
                    val intensity = value.toFloat() / maxVal
                    val alpha = when {
                        intensity < 0.25f -> 0.3f
                        intensity < 0.5f -> 0.55f
                        intensity < 0.75f -> 0.8f
                        else -> 1f
                    }
                    baseColor.copy(alpha = alpha * progress.value)
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(col * (cellW + gapPx), row * (cellH + gapPx)),
                    size = Size(cellW, cellH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        touched?.let { cell ->
            Text(
                text = "${cell.label}  ${LocalCurrencySymbol.current}${MoneyUtil.centsToYuan(cell.value, decimalPlaces = LocalDecimalConfig.current.run { if (show) places else 0 })}",
                style = AmountNumberStyle.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("少", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf(0.08f, 0.25f, 0.5f, 0.75f, 1f).forEach { a ->
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(baseColor.copy(alpha = a)))
            }
            Text("多", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class HeatmapCell(val label: String, val value: Long, val timestamp: Long = 0L)
