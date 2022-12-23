package center.sciprog.maps.features

import androidx.compose.ui.unit.DpSize


public interface Area<T: Any>{
    public operator fun contains(point: T): Boolean
}

/**
 * A map coordinates rectangle. [a] and [b] represent opposing angles
 * of the rectangle without specifying which ones.
 */
public interface Rectangle<T : Any>: Area<T> {
    public val a: T
    public val b: T
}

/**
 * A context for map/scheme coordinates manipulation
 */
public interface CoordinateSpace<T : Any> {

    /**
     * Build a rectangle by two opposing corners
     */
    public fun buildRectangle(first: T, second: T): Rectangle<T>

    /**
     * Build a rectangle of visual size [size]
     */
    public fun buildRectangle(center: T, zoom: Double, size: DpSize): Rectangle<T>

    /**
     * Move given rectangle to be centered at [center]
     */
    public fun Rectangle<T>.withCenter(center: T): Rectangle<T>

    public fun Collection<Rectangle<T>>.wrapRectangles(): Rectangle<T>?

    public fun Collection<T>.wrapPoints(): Rectangle<T>?
}