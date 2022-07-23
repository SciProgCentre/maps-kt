package center.sciprog.compose.scheme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class SchemeCoordinates(val x: Float, val y: Float)

data class SchemeCoordinateBox(val a: SchemeCoordinates, val b: SchemeCoordinates)

val SchemeCoordinateBox.top get() = max(a.y, b.y)
val SchemeCoordinateBox.bottom get() = min(a.y, b.y)

val SchemeCoordinateBox.right get() = max(a.x, b.x)
val SchemeCoordinateBox.left get() = min(a.x, b.x)

val SchemeCoordinateBox.width get() = abs(a.x - b.x)
val SchemeCoordinateBox.height get() = abs(a.y - b.y)

val SchemeCoordinateBox.center get() = SchemeCoordinates((a.x + b.x) / 2, (a.y + b.y) / 2)


fun Collection<SchemeCoordinateBox>.wrapAll(): SchemeCoordinateBox? {
    if (isEmpty()) return null
    val minX = minOf { it.left }
    val maxX = maxOf { it.right }

    val minY = minOf { it.bottom }
    val maxY = maxOf { it.top }
    return SchemeCoordinateBox(
        SchemeCoordinates(minX, minY),
        SchemeCoordinates(maxX, maxY)
    )
}