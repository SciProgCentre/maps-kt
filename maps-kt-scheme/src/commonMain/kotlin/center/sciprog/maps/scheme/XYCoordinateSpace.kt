package center.sciprog.maps.scheme

import androidx.compose.ui.unit.DpSize
import center.sciprog.maps.features.CoordinateSpace
import center.sciprog.maps.features.Rectangle
import center.sciprog.maps.features.ViewPoint
import kotlin.math.pow

object XYCoordinateSpace : CoordinateSpace<XY> {
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
}