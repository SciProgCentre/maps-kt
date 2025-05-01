package space.kscience.maps.scheme

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import space.kscience.kmath.geometry.Vector2D
import space.kscience.maps.features.CoordinateSpace
import space.kscience.maps.features.Rectangle
import space.kscience.maps.features.ViewPoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A data class representing a 2D vector in a coordinate space with `Float` precision.
 *
 * @property x The x-coordinate of the vector.
 * @property y The y-coordinate of the vector.
 */
public data class XY(override val x: Float, override val y: Float) : Vector2D<Float>

/**
 * Creates an instance of the `XY` data class representing a two-dimensional vector with `x` and `y` coordinates
 * converted to `Float` values.
 *
 * @param x The*/
public fun XY(x: Number, y: Number): XY = XY(x.toFloat(), y.toFloat())

internal data class XYRectangle(
    override val a: XY,
    override val b: XY,
) : Rectangle<XY> {

    override fun contains(point: XY): Boolean = point.x in a.x..b.x && point.y in a.y..b.y

    override val center get() = XY((a.x + b.x) / 2, (a.y + b.y) / 2)

//    companion object {
//        fun square(center: XY, height: Float, width: Float): XYRectangle = XYRectangle(
//            XY(center.x - width / 2, center.y + height / 2),
//            XY(center.x + width / 2, center.y - height / 2),
//        )
//    }
}

public val Rectangle<XY>.top: Float get() = max(a.y, b.y)
public val Rectangle<XY>.bottom: Float get() = min(a.y, b.y)

public val Rectangle<XY>.right: Float get() = max(a.x, b.x)
public val Rectangle<XY>.left: Float get() = min(a.x, b.x)

public val Rectangle<XY>.width: Float get() = abs(a.x - b.x)
public val Rectangle<XY>.height: Float get() = abs(a.y - b.y)

public val Rectangle<XY>.leftTop: XY get() = XY(left, top)
public val Rectangle<XY>.rightBottom: XY get() = XY(right, bottom)

internal val defaultCanvasSize = DpSize(512.dp, 512.dp)

/**
 * A data class representing a viewpoint in a 2D coordinate space, defined by a focus point and
 * a zoom level.
 *
 * This class implements the [ViewPoint] interface using the [XY] coordinate system to define
 * spatial locations within a 2D space. The viewpoint is commonly used in visual representations
 * like maps or canvases to determine the focal area and zoom level.
 *
 * @property focus The central [XY] coordinate of the viewpoint.
 * @property zoom The magnification level of the viewpoint, where higher values indicate greater zoom-in.
 */
public data class XYViewPoint(
    override val focus: XY,
    override val zoom: Float = 1f,
) : ViewPoint<XY>

/**
 * Constructs a rectangle in the coordinate space defined by the center point,
 * width, and height.
 * The rectangle is created by determining two diagonal corners
 * based on the given dimensions and center.
 *
 * @param center The center point of the rectangle in the coordinate space.
 * @param height The height of the rectangle.
 * @param width The width of the rectangle.
 * @return A rectangle defined in the coordinate space with the specified parameters.
 */
@Suppress("UnusedReceiverParameter")
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