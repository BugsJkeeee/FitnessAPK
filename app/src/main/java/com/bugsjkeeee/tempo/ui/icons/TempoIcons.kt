package com.bugsjkeeee.tempo.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Линейные монохромные иконки 24×24 в одном стиле (контур 2 dp, скруглённые концы).
 * Цвет задаётся через tint у Icon.
 */
object TempoIcons {
    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 -$r,$r h${-(w - 2 * r)} a$r,$r 0 0 1 -$r,-$r v${-(h - 2 * r)} a$r,$r 0 0 1 $r,-$r z"

    private fun icon(name: String, stroke: List<String>, fill: List<String> = emptyList()): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            stroke.forEach {
                addPath(
                    pathData = addPathNodes(it),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
            fill.forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        }.build()

    val Random = icon("random", listOf(rect(2f, 2f, 8f, 8f, 1.5f), rect(14f, 2f, 8f, 8f, 1.5f), rect(2f, 14f, 8f, 8f, 1.5f), rect(14f, 14f, 8f, 8f, 1.5f)))
    val Timer = icon("timer", listOf(circle(12f, 12f, 10f), "M12 6v6l4 2"))
    val Dumbbell = icon("dumbbell", listOf("M6.5 6.5v11", "M17.5 6.5v11", "M3 9.5v5", "M21 9.5v5", "M6.5 12h11", "M3 12h3.5", "M17.5 12h3.5"))
    val Journal = icon("journal", listOf("M4 19.5A2.5 2.5 0 0 1 6.5 17H20", "M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"))
    val Scale = icon("scale", listOf(rect(3f, 3f, 18f, 18f, 4f), "M7.5 10a4.5 4.5 0 0 1 9 0z", "M12 10l1.8-2.6"))
    val Settings = icon("settings", listOf(circle(12f, 12f, 3f), "M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"))

    val Add = icon("add", listOf("M12 5v14", "M5 12h14"))
    val Minus = icon("minus", listOf("M5 12h14"))
    val Close = icon("close", listOf("M18 6L6 18", "M6 6l12 12"))
    val Check = icon("check", listOf("M20 6L9 17l-5-5"))
    val Back = icon("back", listOf("M19 12H5", "M12 19l-7-7 7-7"))
    val ChevronRight = icon("chevron_right", listOf("M9 18l6-6-6-6"))
    val ChevronLeft = icon("chevron_left", listOf("M15 18l-6-6 6-6"))
    val ChevronDown = icon("chevron_down", listOf("M6 9l6 6 6-6"))
    val ChevronUp = icon("chevron_up", listOf("M18 15l-6-6-6 6"))
    val Search = icon("search", listOf(circle(11f, 11f, 8f), "M21 21l-4.35-4.35"))
    val Filter = icon("filter", listOf("M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3", "M1 14h6M9 8h6M17 16h6"))
    val Edit = icon("edit", listOf("M17 3a2.85 2.83 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5z"))
    val Trash = icon("trash", listOf("M3 6h18", "M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6", "M10 11v6M14 11v6", "M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"))
    val Heart = icon("heart", listOf("M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"))
    val HeartFilled = icon("heart_filled", emptyList(), listOf("M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"))
    val Ban = icon("ban", listOf(circle(12f, 12f, 10f), "M4.93 4.93l14.14 14.14"))
    val Eye = icon("eye", listOf("M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z", circle(12f, 12f, 3f)))
    val Calendar = icon("calendar", listOf(rect(3f, 4f, 18f, 18f, 2f), "M16 2v4M8 2v4M3 10h18"))
    val Volume = icon("volume", listOf("M11 5L6 9H2v6h4l5 4V5z", "M15.54 8.46a5 5 0 0 1 0 7.07", "M19.07 4.93a10 10 0 0 1 0 14.14"))
    val Refresh = icon("refresh", listOf("M1 4v6h6", "M23 20v-6h-6", "M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4l-4.64 4.36A9 9 0 0 1 3.51 15"))
    val Bolt = icon("bolt", listOf("M13 2L3 14h9l-1 8 10-12h-9l1-8z"))
    val Flame = icon("flame", listOf("M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"))
    val Format = icon("format", listOf("M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z", "M14 2v6h6", "M16 13H8M16 17H8M10 9H8"))
    val Muscle = icon("muscle", listOf("M20.24 12.24a6 6 0 0 0-8.49-8.49L5 10.5V19h8.5z", "M16 8L2 22", "M17.5 15H9"))
    val Clipboard = icon("clipboard", listOf("M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2", rect(8f, 2f, 8f, 4f, 1f)))
    val Bell = icon("bell", listOf("M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9", "M13.73 21a2 2 0 0 1-3.46 0"))
    val Flag = icon("flag", listOf("M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z", "M4 22v-7"))
    val Trophy = icon("trophy", listOf("M6 9H4.5a2.5 2.5 0 0 1 0-5H6", "M18 9h1.5a2.5 2.5 0 0 0 0-5H18", "M4 22h16", "M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22", "M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22", "M18 2H6v7a6 6 0 0 0 12 0V2z"))
    val Upload = icon("upload", listOf("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M17 8l-5-5-5 5", "M12 3v12"))
    val Download = icon("download", listOf("M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4", "M7 10l5 5 5-5", "M12 15V3"))
    val Sun = icon("sun", listOf(circle(12f, 12f, 4f), "M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"))
    val Target = icon("target", listOf(circle(12f, 12f, 10f), circle(12f, 12f, 6f), circle(12f, 12f, 2f)))
    val Youtube = icon("play_circle", listOf(circle(12f, 12f, 10f), "M10 8l6 4-6 4V8z"))

    val Play = icon("play", emptyList(), listOf("M6 3.5l14 8.5-14 8.5z"))
    val Pause = icon("pause", emptyList(), listOf(rect(6f, 4f, 4f, 16f, 1f), rect(14f, 4f, 4f, 16f, 1f)))
    val Stop = icon("stop", emptyList(), listOf(rect(6f, 6f, 12f, 12f, 1.5f)))
    val Skip = icon("skip", listOf("M5 4l10 8-10 8V4z", "M19 5v14"))
}

