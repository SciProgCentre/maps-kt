package center.sciprog.maps.scheme

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class SchemeCoordinates(val x: Float, val y: Float)

data class SchemeRectangle(
    val a: SchemeCoordinates,
    val b: SchemeCoordinates,
) {
    companion object {
        fun square(center: SchemeCoordinates, height: Float, width: Float): SchemeRectangle = SchemeRectangle(
            SchemeCoordinates(center.x - width / 2, center.y - height / 2),
            SchemeCoordinates(center.x + width / 2, center.y + height / 2),
        )
    }
}

val SchemeRectangle.top get() = max(a.y, b.y)
val SchemeRectangle.bottom get() = min(a.y, b.y)

val SchemeRectangle.right get() = max(a.x, b.x)
val SchemeRectangle.left get() = min(a.x, b.x)

val SchemeRectangle.width get() = abs(a.x - b.x)
val SchemeRectangle.height get() = abs(a.y - b.y)

val SchemeRectangle.center get() = SchemeCoordinates((a.x + b.x) / 2, (a.y + b.y) / 2)

public val SchemeRectangle.topLeft: SchemeCoordinates get() = SchemeCoordinates(top, left)
public val SchemeRectangle.bottomRight: SchemeCoordinates get() = SchemeCoordinates(bottom, right)

fun Collection<SchemeRectangle>.wrapAll(): SchemeRectangle? {
    if (isEmpty()) return null
    val minX = minOf { it.left }
    val maxX = maxOf { it.right }

    val minY = minOf { it.bottom }
    val maxY = maxOf { it.top }
    return SchemeRectangle(
        SchemeCoordinates(minX, minY),
        SchemeCoordinates(maxX, maxY)
    )
}

internal val defaultCanvasSize = DpSize(512.dp, 512.dp)

public fun SchemeRectangle.computeViewPoint(
    canvasSize: DpSize = defaultCanvasSize,
): SchemeViewPoint {
    val scale = min(
        canvasSize.width.value / width,
        canvasSize.height.value / height
    )

    return SchemeViewPoint(center, scale)
}