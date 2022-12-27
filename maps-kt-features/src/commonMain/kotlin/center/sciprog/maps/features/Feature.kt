package center.sciprog.maps.features

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

public typealias DoubleRange = FloatRange
public typealias FloatRange = ClosedFloatingPointRange<Float>

public interface Feature<T : Any> {
    public interface Attribute<T>

    public val space: CoordinateSpace<T>

    public val zoomRange: FloatRange

    public val attributes: AttributeMap

    public fun getBoundingBox(zoom: Float): Rectangle<T>?
}

public interface PainterFeature<T:Any>: Feature<T> {
    @Composable
    public fun getPainter(): Painter
}

public interface SelectableFeature<T : Any> : Feature<T> {
    public operator fun contains(point: ViewPoint<T>): Boolean = getBoundingBox(point.zoom)?.let {
        point.focus in it
    } ?: false
}

public interface DraggableFeature<T : Any> : SelectableFeature<T> {
    public fun withCoordinates(newCoordinates: T): Feature<T>
}

/**
 * A draggable marker feature. Other features could be bound to this one.
 */
public interface MarkerFeature<T: Any>: DraggableFeature<T>{
    public val center: T
}

public fun <T : Any> Iterable<Feature<T>>.computeBoundingBox(
    space: CoordinateSpace<T>,
    zoom: Float,
): Rectangle<T>? = with(space) {
    mapNotNull { it.getBoundingBox(zoom) }.wrapRectangles()
}

//public fun Pair<Number, Number>.toCoordinates(): GeodeticMapCoordinates =
//    GeodeticMapCoordinates.ofDegrees(first.toDouble(), second.toDouble())


/**
 * A feature that decides what to show depending on the zoom value (it could change size of shape)
 */
public class FeatureSelector<T : Any>(
    override val space: CoordinateSpace<T>,
    override val zoomRange: FloatRange,
    override val attributes: AttributeMap = AttributeMap(),
    public val selector: (zoom: Float) -> Feature<T>, 
) : Feature<T> {


    override fun getBoundingBox(zoom: Float): Rectangle<T>? = selector(zoom).getBoundingBox(zoom)
}

public class PathFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val rectangle: Rectangle<T>,
    public val path: Path,
    public val brush: Brush,
    override val zoomRange: FloatRange,
    public val style: DrawStyle = Fill,
    public val targetRect: Rect = path.getBounds(),
    override val attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun withCoordinates(newCoordinates: T): Feature<T> = with(space) {
        PathFeature(
            space = space,
            rectangle = rectangle.withCenter(newCoordinates),
            path = path,
            brush = brush,
            style = style,
            targetRect = targetRect,
            zoomRange = zoomRange
        )
    }

    override fun getBoundingBox(zoom: Float): Rectangle<T> = rectangle

}

public class PointsFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val points: List<T>,
    override val zoomRange: FloatRange,
    public val stroke: Float = 2f,
    public val color: Color = Color.Red,
    public val pointMode: PointMode = PointMode.Points,
    override val attributes: AttributeMap = AttributeMap(),
) : Feature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T>? = with(space) {
        points.wrapPoints()
    }
}

public data class CircleFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    override val center: T,
    override val zoomRange: FloatRange,
    public val size: Dp = 5.dp,
    public val color: Color = Color.Red,
    override val attributes: AttributeMap = AttributeMap(),
) : MarkerFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> =
        space.Rectangle(center, zoom, DpSize(size, size))

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        CircleFeature(space, newCoordinates, zoomRange, size, color, attributes)
}

public class RectangleFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    override val center: T,
    override val zoomRange: FloatRange,
    public val size: DpSize = DpSize(5.dp, 5.dp),
    public val color: Color = Color.Red,
    override val attributes: AttributeMap = AttributeMap(),
) : MarkerFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> =
        space.Rectangle(center, zoom, size)

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        RectangleFeature(space, newCoordinates, zoomRange, size, color, attributes)
}

public class LineFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val a: T,
    public val b: T,
    override val zoomRange: FloatRange,
    public val color: Color = Color.Red,
    override val attributes: AttributeMap = AttributeMap(),
) : SelectableFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> =
        space.Rectangle(a, b)

    override fun contains(point: ViewPoint<T>): Boolean {
        return super.contains(point)
    }
}

/**
 * @param startAngle the angle from 3 o'clock downwards for the start of the arc in radians
 * @param arcLength arc length in radians
 */
public class ArcFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val oval: Rectangle<T>,
    public val startAngle: Float,
    public val arcLength: Float,
    override val zoomRange: FloatRange,
    public val color: Color = Color.Red,
    override val attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = oval

    override fun withCoordinates(newCoordinates: T): Feature<T> = with(space) {
        ArcFeature(space, oval.withCenter(newCoordinates), startAngle, arcLength, zoomRange, color, attributes)
    }
}

public data class DrawFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val position: T,
    override val zoomRange: FloatRange,
    override val attributes: AttributeMap = AttributeMap(),
    public val drawFeature: DrawScope.() -> Unit,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = space.Rectangle(position, position)

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(position = newCoordinates)
}

public data class BitmapImageFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    override val center: T,
    public val size: DpSize,
    public val image: ImageBitmap,
    override val zoomRange: FloatRange,
    override val attributes: AttributeMap = AttributeMap(),
) : MarkerFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = space.Rectangle(center, zoom, size)

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(center = newCoordinates)
}

public data class VectorImageFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    override val center: T,
    public val size: DpSize,
    public val image: ImageVector,
    override val zoomRange: FloatRange,
    override val attributes: AttributeMap = AttributeMap(),
) : MarkerFeature<T>, PainterFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = space.Rectangle(center, zoom, size)

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(center = newCoordinates)

    @Composable
    override fun getPainter(): VectorPainter = rememberVectorPainter(image)
}

/**
 * An image that is bound to coordinates and is scaled together with them
 *
 * @param rectangle the size of background in scheme size units. The screen units to scheme units ratio equals scale.
 */
public class ScalableImageFeature<T: Any>(
    override val space: CoordinateSpace<T>,
    public val rectangle: Rectangle<T>,
    override val zoomRange: FloatRange,
    override val attributes: AttributeMap = AttributeMap(),
    public val painter: @Composable () -> Painter,
) : Feature<T>, PainterFeature<T>{
    @Composable
    override fun getPainter(): Painter  = painter.invoke()

    override fun getBoundingBox(zoom: Float): Rectangle<T> =rectangle
}


/**
 * A group of other features
 */
public class FeatureGroup<T : Any>(
    override val space: CoordinateSpace<T>,
    public val children: Map<FeatureId<*>, Feature<T>>,
    override val zoomRange: FloatRange,
    override val attributes: AttributeMap = AttributeMap(),
) : Feature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T>? = with(space) {
        children.values.mapNotNull { it.getBoundingBox(zoom) }.wrapRectangles()
    }
}

public class TextFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val position: T,
    public val text: String,
    override val zoomRange: FloatRange,
    public val color: Color = Color.Black,
    override val attributes: AttributeMap = AttributeMap(),
    public val fontConfig: FeatureFont.() -> Unit,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = space.Rectangle(position, position)

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        TextFeature(space, newCoordinates, text, zoomRange, color, attributes, fontConfig)
}
