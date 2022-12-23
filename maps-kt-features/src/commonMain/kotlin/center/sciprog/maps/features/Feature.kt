package center.sciprog.maps.features

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.floor


public interface Feature<T : Any> {
    public interface Attribute<T>

    public val space: CoordinateSpace<T>

    public val zoomRange: ClosedFloatingPointRange<Double>

    public var attributes: AttributeMap

    public fun getBoundingBox(zoom: Double): Rectangle<T>?
}

public interface SelectableFeature<T : Any> : Feature<T> {
    public operator fun contains(point: ViewPoint<T>): Boolean = getBoundingBox(point.zoom)?.let {
        point.focus in it
    } ?: false
}

public interface DraggableFeature<T : Any> : SelectableFeature<T> {
    public fun withCoordinates(newCoordinates: T): Feature<T>
}

public fun <T : Any> Iterable<Feature<T>>.computeBoundingBox(
    space: CoordinateSpace<T>,
    zoom: Double,
): Rectangle<T>? = with(space) {
    mapNotNull { it.getBoundingBox(zoom) }.computeRectangle()
}

//public fun Pair<Number, Number>.toCoordinates(): GeodeticMapCoordinates =
//    GeodeticMapCoordinates.ofDegrees(first.toDouble(), second.toDouble())

internal val defaultZoomRange = 1.0..Double.POSITIVE_INFINITY

/**
 * A feature that decides what to show depending on the zoom value (it could change size of shape)
 */
public class FeatureSelector<T : Any>(
    override val space: CoordinateSpace<T>,
    override var attributes: AttributeMap = AttributeMap(),
    public val selector: (zoom: Int) -> Feature<T>,
) : Feature<T> {
    override val zoomRange: ClosedFloatingPointRange<Double> get() = defaultZoomRange

    override fun getBoundingBox(zoom: Double): Rectangle<T>? = selector(floor(zoom).toInt()).getBoundingBox(zoom)
}

public class DrawFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val rectangle: Rectangle<T>,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
    public val drawFeature: DrawScope.() -> Unit,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = rectangle

    override fun withCoordinates(newCoordinates: T): Feature<T> = with(space) {
        DrawFeature(space, rectangle.withCenter(newCoordinates), zoomRange, attributes, drawFeature)
    }
}

public class PathFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val rectangle: Rectangle<T>,
    public val path: Path,
    public val brush: Brush,
    public val style: DrawStyle = Fill,
    public val targetRect: Rect = path.getBounds(),
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
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

    override fun getBoundingBox(zoom: Double): Rectangle<T> = rectangle

}

public class PointsFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val points: List<T>,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val stroke: Float = 2f,
    public val color: Color = Color.Red,
    public val pointMode: PointMode = PointMode.Points,
    override var attributes: AttributeMap = AttributeMap(),
) : Feature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T>? = with(space) {
        points.computeRectangle()
    }
}

public data class CircleFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val center: T,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val size: Dp = 5.dp,
    public val color: Color = Color.Red,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> =
        space.buildRectangle(center, zoom, DpSize(size, size))

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        CircleFeature(space, newCoordinates, zoomRange, size, color, attributes)
}

public class RectangleFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val center: T,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val size: DpSize = DpSize(5.dp, 5.dp),
    public val color: Color = Color.Red,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> =
        space.buildRectangle(center, zoom, size)

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        RectangleFeature(space, newCoordinates, zoomRange, size, color, attributes)
}

public class LineFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val a: T,
    public val b: T,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val color: Color = Color.Red,
    override var attributes: AttributeMap = AttributeMap(),
) : SelectableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> =
        space.buildRectangle(a, b)

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
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val color: Color = Color.Red,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = oval

    override fun withCoordinates(newCoordinates: T): Feature<T> = with(space) {
        ArcFeature(space, oval.withCenter(newCoordinates), startAngle, arcLength, zoomRange, color, attributes)
    }
}

public class BitmapImageFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val rectangle: Rectangle<T>,
    public val image: ImageBitmap,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = rectangle

    override fun withCoordinates(newCoordinates: T): Feature<T> = with(space) {
        BitmapImageFeature(space, rectangle.withCenter(newCoordinates), image, zoomRange, attributes)
    }
}

public class VectorImageFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val rectangle: Rectangle<T>,
    public val image: ImageVector,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = rectangle

    override fun withCoordinates(newCoordinates: T): Feature<T> = with(space) {
        VectorImageFeature(space, rectangle.withCenter(newCoordinates), image, zoomRange, attributes)
    }

    @Composable
    public fun painter(): VectorPainter = rememberVectorPainter(image)
}

/**
 * A group of other features
 */
public class FeatureGroup<T : Any>(
    override val space: CoordinateSpace<T>,
    public val children: Map<FeatureId<*>, Feature<T>>,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    override var attributes: AttributeMap = AttributeMap(),
) : Feature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T>? = with(space) {
        children.values.mapNotNull { it.getBoundingBox(zoom) }.computeRectangle()
    }
}

public class TextFeature<T : Any>(
    public val position: T,
    public val text: String,
    override val zoomRange: ClosedFloatingPointRange<Double> = defaultZoomRange,
    public val color: Color = Color.Black,
    override var attributes: AttributeMap = AttributeMap(),
    public val fontConfig: FeatureFont.() -> Unit,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Double): Rectangle<T> = GmcRectangle(position, position)

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        TextFeature(newCoordinates, text, zoomRange, color, attributes, fontConfig)
}
