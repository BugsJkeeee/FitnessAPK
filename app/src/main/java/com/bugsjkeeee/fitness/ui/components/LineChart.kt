package com.bugsjkeeee.fitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import com.bugsjkeeee.fitness.data.formatWeight

data class ChartPoint(val x: Long, val y: Double)

/**
 * Простой линейный график: точки по времени, заливка под линией,
 * подписи минимума/максимума по Y и первой/последней даты по X.
 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    unit: String = "кг",
) {
    if (points.isEmpty()) return
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "макс. ${formatWeight(maxY)} $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "мин. ${formatWeight(minY)} $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val pad = 8.dp.toPx()
            val w = size.width - pad * 2
            val h = size.height - pad * 2
            // Небольшой запас по Y, чтобы линия не прилипала к краям.
            val spanY = (maxY - minY).takeIf { it > 0 } ?: 1.0
            val lowY = minY - spanY * 0.1
            val highY = maxY + spanY * 0.1
            val spanX = (maxX - minX).takeIf { it > 0 } ?: 1L

            fun toOffset(p: ChartPoint): Offset {
                val fx = if (points.size == 1) 0.5f else (p.x - minX).toFloat() / spanX
                val fy = ((p.y - lowY) / (highY - lowY)).toFloat()
                return Offset(pad + fx * w, pad + (1 - fy) * h)
            }

            for (i in 0..3) {
                val y = pad + h * i / 3f
                drawLine(gridColor, Offset(pad, y), Offset(pad + w, y), strokeWidth = 1.dp.toPx())
            }

            val offsets = points.sortedBy { it.x }.map(::toOffset)
            if (offsets.size > 1) {
                val line = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(offsets.last().x, pad + h)
                    lineTo(offsets.first().x, pad + h)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)))
                drawPath(
                    line,
                    lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            offsets.forEach { drawCircle(lineColor, radius = 4.dp.toPx(), center = it) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatShortDate(minX), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (maxX != minX) {
                Text(formatShortDate(maxX), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
