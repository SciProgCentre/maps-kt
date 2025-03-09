package space.kscience.maps.features

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sqrt


public interface Area<T : Any> {
    public operator fun contains(point: T): Boolean
}

/**
 * A map coordinates rectangle. [a] and [b] represent opposing angles
 * of the rectangle without specifying which ones.
 */
public interface Rectangle<T : Any> : Area<T> {
    public val a: T
    public val b: T

    public val center: T
}

/**
 * A context for map/scheme coordinates manipulation
 */
public interface CoordinateSpace<T : Any> {

    /**
     * Build a rectangle by two opposing corners
     */
    public fun Rectangle(first: T, second: T): Rectangle<T>

    /**
     * Build a rectangle of visual size [size]
     */
    public fun Rectangle(center: T, zoom: Float, size: DpSize): Rectangle<T>

    /**
     * A view point used by default
     */
    public val defaultViewPoint: ViewPoint<T>

    /**
     * Create a [ViewPoint] associated with this coordinate space.
     */
    public fun ViewPoint(center: T, zoom: Float): ViewPoint<T>

    public fun ViewPoint<T>.moveBy(delta: T): ViewPoint<T>

    public fun ViewPoint<T>.zoomBy(
        zoomDelta: Float,
        invariant: T = focus,
    ): ViewPoint<T>

    /**
     * Move given rectangle to be centered at [center]
     */
    public fun Rectangle<T>.withCenter(center: T): Rectangle<T>

    public fun Collection<Rectangle<T>>.wrapRectangles(): Rectangle<T>?

    public fun Collection<T>.wrapPoints(): Rectangle<T>?

    public fun T.offsetTo(b: T, zoom: Float): DpOffset

    public fun T.distanceTo(b: T, zoom: Float): Dp {
        val offset: DpOffset = offsetTo(b, zoom)
        return sqrt(offset.x.value * offset.x.value + offset.y.value * offset.y.value).dp
    }

    public fun T.distanceToLine(a: T, b: T, zoom: Float): Dp {
        val d12 = a.offsetTo(b, zoom)
        val d01 = offsetTo(a, zoom)
        val distanceVale = abs(d12.x.value * d01.y.value - d12.y.value * d01.x.value) / a.distanceTo(b, zoom).value

        return distanceVale.dp
    }

    public fun T.isInsidePolygon(points: List<T>): Boolean
}

public fun <T : Any> CoordinateSpace<T>.Rectangle(viewPoint: ViewPoint<T>, size: DpSize): Rectangle<T> =
    Rectangle(viewPoint.focus, viewPoint.zoom, size)