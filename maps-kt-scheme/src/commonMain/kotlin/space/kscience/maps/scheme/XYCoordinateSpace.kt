package space.kscience.maps.scheme

import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import space.kscience.maps.features.CoordinateSpace
import space.kscience.maps.features.Rectangle
import space.kscience.maps.features.ViewPoint
import kotlin.math.abs
import kotlin.math.pow

public object XYCoordinateSpace : CoordinateSpace<XY> {
    override fun Rectangle(first: XY, second: XY): Rectangle<XY> =
        XYRectangle(first, second)

    override fun Rectangle(center: XY, zoom: Float, size: DpSize): Rectangle<XY> =
        Rectangle(center, (size.width.value / zoom), (size.height.value / zoom))

    override fun ViewPoint(center: XY, zoom: Float): ViewPoint<XY> =
        XYViewPoint(center, zoom)

    override fun ViewPoint<XY>.moveBy(delta: XY): ViewPoint<XY> =
        XYViewPoint(XY(focus.x + delta.x, focus.y + delta.y))

    override fun ViewPoint<XY>.zoomBy(
        zoomDelta: Float,
        invariant: XY,
    ): ViewPoint<XY> = if (invariant == focus) {
        XYViewPoint(focus, zoom = zoom * 2f.pow(zoomDelta))
    } else {
        val difScale = (1 - 2f.pow(-zoomDelta))
        val newCenter = XY(
            focus.x + (invariant.x - focus.x) * difScale,
            focus.y + (invariant.y - focus.y) * difScale
        )
        XYViewPoint(newCenter, zoom * 2f.pow(zoomDelta))
    }

    override fun Rectangle<XY>.withCenter(center: XY): Rectangle<XY> =
        Rectangle(center, width, height)

    override fun Collection<Rectangle<XY>>.wrapRectangles(): Rectangle<XY>? {
        if (isEmpty()) return null
        val minX = minOf { it.left }
        val maxX = maxOf { it.right }

        val minY = minOf { it.bottom }
        val maxY = maxOf { it.top }
        return XYRectangle(
            XY(minX, minY),
            XY(maxX, maxY)
        )
    }

    override fun Collection<XY>.wrapPoints(): Rectangle<XY>? {
        if (isEmpty()) return null
        val minX = minOf { it.x }
        val maxX = maxOf { it.x }

        val minY = minOf { it.y }
        val maxY = maxOf { it.y }
        return XYRectangle(
            XY(minX, minY),
            XY(maxX, maxY)
        )
    }

    override val defaultViewPoint: ViewPoint<XY> = XYViewPoint(XY(0f, 0f), 1f)

    override fun XY.offsetTo(b: XY, zoom: Float): DpOffset = DpOffset(
        (b.x - x).dp * zoom,
        (b.y - y).dp * zoom
    )

    override fun XY.isInsidePolygon(points: List<XY>): Boolean = points.zipWithNext().count { (left, right) ->
        val yRange = if(right.x >= left.x) {
            left.y..right.y
        } else {
            right.y..left.y
        }

        if(y !in yRange) return@count false

        val longitudeDelta = right.y - left.y

        left.x * abs((right.y - y) / longitudeDelta) +
                right.x * abs((y - left.y) / longitudeDelta) >= x
    } % 2 == 1
}