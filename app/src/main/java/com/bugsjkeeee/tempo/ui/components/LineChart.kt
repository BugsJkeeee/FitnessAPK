package com.bugsjkeeee.tempo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bugsjkeeee.tempo.ui.theme.LocalTempoStyle

/**
 * Линейный график по времени: основная серия с заливкой, необязательная линия тренда
 * и горизонтальная линия цели. [x] — любая монотонная шкала (дни, миллисекунды).
 */
@Composable
fun LineChart(
    points: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier,
    trend: List<Pair<Long, Double>> = emptyList(),
    goal: Double? = null,
    xLabel: (Long) -> String,
    yLabel: (Double) -> String,
) {
    if (points.isEmpty()) return
    val accent = MaterialTheme.colorScheme.primary
    val style = LocalTempoStyle.current
    val grid = style.tileBorder
    val muted = style.muted
    val trendColor = MaterialTheme.colorScheme.onSurface
    val goalColor = style.positive

    val ys = points.map { it.second } + trend.map { it.second } + listOfNotNull(goal)
    val minY = ys.min()
    val maxY = ys.max()
    val minX = points.minOf { it.first }
    val maxX = points.maxOf { it.first }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(yLabel(maxY), style = MaterialTheme.typography.labelSmall, color = muted)
            Text("мин. ${yLabel(minY)}", style = MaterialTheme.typography.labelSmall, color = muted)
        }
        Canvas(Modifier.fillMaxWidth().height(200.dp)) {
            val pad = 8.dp.toPx()
            val w = size.width - pad * 2
            val h = size.height - pad * 2
            val span = (maxY - minY).takeIf { it > 0 } ?: 1.0
            val lo = minY - span * 0.1
            val hi = maxY + span * 0.1
            val spanX = (maxX - minX).takeIf { it > 0 } ?: 1L
            fun at(x: Long, y: Double): Offset {
                val fx = if (maxX == minX) 0.5f else (x - minX).toFloat() / spanX
                val fy = ((y - lo) / (hi - lo)).toFloat()
                return Offset(pad + fx * w, pad + (1 - fy) * h)
            }
            for (i in 0..3) {
                val y = pad + h * i / 3f
                drawLine(grid, Offset(pad, y), Offset(pad + w, y), strokeWidth = 1.dp.toPx())
            }
            goal?.let {
                val y = at(minX, it).y
                drawLine(
                    goalColor, Offset(pad, y), Offset(pad + w, y), strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                )
            }
            val offsets = points.sortedBy { it.first }.map { at(it.first, it.second) }
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
                drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = 0.3f), Color.Transparent)))
                drawPath(line, accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            offsets.forEach { drawCircle(accent, radius = 3.dp.toPx(), center = it) }
            if (trend.size > 1) {
                val t = trend.sortedBy { it.first }.map { at(it.first, it.second) }
                val path = Path().apply {
                    moveTo(t.first().x, t.first().y)
                    t.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(path, trendColor.copy(alpha = 0.8f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(xLabel(minX), style = MaterialTheme.typography.labelSmall, color = muted)
            if (maxX != minX) Text(xLabel(maxX), style = MaterialTheme.typography.labelSmall, color = muted)
        }
    }
}
