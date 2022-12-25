package center.sciprog.maps.scheme

import androidx.compose.ui.unit.*
import center.sciprog.maps.features.*
import kotlin.math.min

class XYViewState(
    config: ViewConfig<XY>,
    canvasSize: DpSize,
    viewPoint: ViewPoint<XY>,
) : CoordinateViewState<XY>(config, canvasSize, viewPoint) {
    override val space: CoordinateSpace<XY>
        get() = XYCoordinateSpace

    override fun DpOffset.toCoordinates(): XY = XY(
        (x - canvasSize.width / 2).value / viewPoint.zoom + viewPoint.focus.x,
        (canvasSize.height / 2 - y).value / viewPoint.zoom + viewPoint.focus.y
    )

    override fun XY.toDpOffset(): DpOffset = DpOffset(
        (canvasSize.width / 2 + (x.dp - viewPoint.focus.x.dp) * viewPoint.zoom),
        (canvasSize.height / 2 + (viewPoint.focus.y.dp - y.dp) * viewPoint.zoom)
    )

    override fun viewPointFor(rectangle: Rectangle<XY>): ViewPoint<XY> {
        val scale = min(
            canvasSize.width.value / rectangle.width,
            canvasSize.height.value / rectangle.height
        )

        return XYViewPoint(rectangle.center, scale)
    }

    override fun ViewPoint<XY>.moveBy(x: Dp, y: Dp): ViewPoint<XY> {
        val newCoordinates = XY(focus.x + x.value / zoom, focus.y + y.value / zoom)
        return XYViewPoint(newCoordinates, zoom)
    }

    override fun Rectangle<XY>.toDpRect(): DpRect {
        val topLeft = leftTop.toDpOffset()
        val bottomRight = rightBottom.toDpOffset()
        return DpRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

}