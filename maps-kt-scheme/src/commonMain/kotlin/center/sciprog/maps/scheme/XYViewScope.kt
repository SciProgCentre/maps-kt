package center.sciprog.maps.scheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import center.sciprog.maps.features.*
import kotlin.math.min

public class XYViewScope(
    config: ViewConfig<XY>,
) : CoordinateViewScope<XY>(config) {
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

    override fun computeViewPoint(rectangle: Rectangle<XY>): ViewPoint<XY> {
        val scale: Float = min(
            canvasSize.width.value / rectangle.width,
            canvasSize.height.value / rectangle.height
        )
        return if(scale.isInfinite()){
            XYViewPoint(rectangle.center, 1f)
        } else {
            XYViewPoint(rectangle.center, scale)
        }
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

    public companion object{
        @Composable
        public fun remember(
            config: ViewConfig<XY> = ViewConfig(),
            initialViewPoint: ViewPoint<XY>? = null,
            initialRectangle: Rectangle<XY>? = null,
        ): XYViewScope = remember {
            XYViewScope(config).also { mapState->
                if (initialViewPoint != null) {
                    mapState.viewPoint = initialViewPoint
                } else if (initialRectangle != null) {
                    mapState.viewPoint = mapState.computeViewPoint(initialRectangle)
                }
            }
        }
    }
}

