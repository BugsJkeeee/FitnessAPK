package com.bugsjkeeee.tempo.timer

/**
 * Компактная запись сценария для хранения в настройках:
 * «W40,R20x8;W300x1» — блоки через «;», интервалы через «,», число повторов после «x».
 */
object BlockCodec {
    fun encode(blocks: List<Block>): String = blocks.joinToString(";") { b ->
        b.intervals.joinToString(",") { (if (it.phase == Phase.REST) "R" else "W") + it.seconds } + "x" + b.repeats
    }

    fun decode(text: String): List<Block>? = runCatching {
        text.split(";").filter { it.isNotBlank() }.map { part ->
            val (body, repeats) = part.split("x").let { it[0] to it[1].toInt() }
            Block(
                intervals = body.split(",").map {
                    Interval(if (it[0] == 'R') Phase.REST else Phase.WORK, it.substring(1).toInt())
                },
                repeats = repeats,
            )
        }
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}
