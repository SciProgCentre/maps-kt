package center.sciprog.maps.scheme

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import center.sciprog.maps.features.CoordinateSpace
import center.sciprog.maps.features.Rectangle
import center.sciprog.maps.features.ViewPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class XY(val x: Float, val y: Float)

internal data class XYRectangle(
    override val a: XY,
    override val b: XY,
) : Rectangle<XY> {

    override fun contains(point: XY): Boolean = point.x in a.x..b.x && point.y in a.y..b.y

//    companion object {
//        fun square(center: XY, height: Float, width: Float): XYRectangle = XYRectangle(
//            XY(center.x - width / 2, center.y + height / 2),
//            XY(center.x + width / 2, center.y - height / 2),
//        )
//    }
}

val Rectangle<XY>.top get() = max(a.y, b.y)
val Rectangle<XY>.bottom get() = min(a.y, b.y)

val Rectangle<XY>.right get() = max(a.x, b.x)
val Rectangle<XY>.left get() = min(a.x, b.x)

val Rectangle<XY>.width: Float get() = abs(a.x - b.x)
val Rectangle<XY>.height: Float get() = abs(a.y - b.y)

val Rectangle<XY>.center get() = XY((a.x + b.x) / 2, (a.y + b.y) / 2)

public val Rectangle<XY>.leftTop: XY get() = XY(left, top)
public val Rectangle<XY>.rightBottom: XY get() = XY(right, bottom)

internal val defaultCanvasSize = DpSize(512.dp, 512.dp)

data class XYViewPoint(
    override val focus: XY,
    override val zoom: Float = 1f,
) : ViewPoint<XY>

public fun CoordinateSpace<XY>.Rectangle(
    center: XY,
    height: Float,
    width: Float,
): Rectangle<XY> {
    val a = XY(
        center.x - (width / 2),
        center.y - (height / 2)
    )
    val b = XY(
        center.x + (width / 2),
        center.y + (height / 2)
    )
    return XYRectangle(a, b)
}