package center.sciprog.maps.features

import androidx.compose.ui.unit.DpSize


public interface Rectangle<T : Any> {
    public val topLeft: T
    public val bottomRight: T

    public operator fun contains(point: T): Boolean
}

public interface CoordinateSpace<T : Any> {

    /**
     * Build a rectangle by two opposing corners
     */
    public fun buildRectangle(first: T, second: T): Rectangle<T>

    /**
     * Build a rectangle of visual size [size]
     */
    public fun buildRectangle(center: T, zoom: Double, size: DpSize): Rectangle<T>
    //GmcRectangle.square(center, (size.height.value / scale).radians, (size.width.value / scale).radians)

    /**
     * Move given rectangle to be centered at [center]
     */
    public fun Rectangle<T>.withCenter(center: T): Rectangle<T>

    public fun Iterable<Rectangle<T>>.computeRectangle(): Rectangle<T>?

    public fun Iterable<T>.computeRectangle(): Rectangle<T>?
}