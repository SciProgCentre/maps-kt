package center.sciprog.maps.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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

public typealias FloatRange = ClosedFloatingPointRange<Float>

public interface Feature<T : Any> {

    public val space: CoordinateSpace<T>

    public val attributes: Attributes

    public fun getBoundingBox(zoom: Float): Rectangle<T>?

    public fun withAttributes(modify: Attributes.() -> Attributes): Feature<T>
}

public val Feature<*>.color: Color? get() = attributes[ColorAttribute]

public val Feature<*>.zoomRange: FloatRange
    get() = attributes[ZoomRangeAttribute] ?: Float.NEGATIVE_INFINITY..Float.POSITIVE_INFINITY

public interface PainterFeature<T : Any> : Feature<T> {
    @Composable
    public fun getPainter(): Painter
}

public interface DomainFeature<T : Any> : Feature<T> {
    public operator fun contains(viewPoint: ViewPoint<T>): Boolean = getBoundingBox(viewPoint.zoom)?.let {
        viewPoint.focus in it
    } ?: false
}

public interface DraggableFeature<T : Any> : DomainFeature<T> {
    public fun withCoordinates(newCoordinates: T): Feature<T>
}

/**
 * A draggable marker feature. Other features could be bound to this one.
 */
public interface MarkerFeature<T : Any> : DraggableFeature<T> {
    public val center: T
}

public fun <T : Any> Iterable<Feature<T>>.computeBoundingBox(
    space: CoordinateSpace<T>,
    zoom: Float,
): Rectangle<T>? = with(space) {
    mapNotNull { it.getBoundingBox(zoom) }.wrapRectangles()
}

/**
 * A feature that decides what to show depending on the zoom value (it could change size of shape)
 */
@Stable
public data class FeatureSelector<T : Any>(
    override val space: CoordinateSpace<T>,
    override val attributes: Attributes = Attributes.EMPTY,
    public val selector: (zoom: Float) -> Feature<T>,
) : Feature<T> {

    override fun getBoundingBox(zoom: Float): Rectangle<T>? = selector(zoom).getBoundingBox(zoom)

    override fun withAttributes(modify: Attributes.() -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

@Stable
public data class PathFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val rectangle: Rectangle<T>,
    public val path: Path,
    public val brush: Brush,
    public val style: DrawStyle = Fill,
    public val targetRect: Rect = path.getBounds(),
    override val attributes: Attributes = Attributes.EMPTY,
) : DraggableFeature<T> {
    override fun withCoordinates(newCoordinates: T): Feature<T> = with(space) {
        PathFeature(
            space = space,
            rectangle = rectangle.withCenter(newCoordinates),
            path = path,
            brush = brush,
            style = style,
            targetRect = targetRect,
        )
    }

    override fun getBoundingBox(zoom: Float): Rectangle<T> = rectangle
    override fun withAttributes(modify: Attributes.() -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

@Stable
public data class PointsFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val points: List<T>,
    public val stroke: Float = 2f,
    public val pointMode: PointMode = PointMode.Points,
    override val attributes: Attributes = Attributes.EMPTY,
) : Feature<T> {

    private val boundingBox by lazy {
        with(space) { points.wrapPoints() }
    }

    override fun getBoundingBox(zoom: Float): Rectangle<T>? = boundingBox
    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

@Stable
public data class PolygonFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val points: List<T>,
    override val attributes: Attributes = Attributes.EMPTY,
) : DomainFeature<T> {

    private val boundingBox: Rectangle<T>? by lazy {
        with(space) { points.wrapPoints() }
    }

    override fun getBoundingBox(zoom: Float): Rectangle<T>? = boundingBox

    override fun contains(viewPoint: ViewPoint<T>): Boolean {
        val boundingBox = boundingBox ?: return false
        return viewPoint.focus in boundingBox && with(space) { viewPoint.focus.isInsidePolygon(points) }
    }

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

@Stable
public data class CircleFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    override val center: T,
    public val size: Dp = 5.dp,
    override val attributes: Attributes = Attributes.EMPTY,
) : MarkerFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> =
        space.Rectangle(center, zoom, DpSize(size, size))

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(center = newCoordinates)

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

@Stable
public data class RectangleFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    override val center: T,
    public val size: DpSize = DpSize(5.dp, 5.dp),
    override val attributes: Attributes = Attributes.EMPTY,
) : MarkerFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> =
        space.Rectangle(center, zoom, size)

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(center = newCoordinates)

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

@Stable
public data class LineFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val a: T,
    public val b: T,
    override val attributes: Attributes = Attributes.EMPTY,
) : DomainFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> =
        space.Rectangle(a, b)

    override fun contains(viewPoint: ViewPoint<T>): Boolean = with(space) {
        viewPoint.focus in getBoundingBox(viewPoint.zoom) && viewPoint.focus.distanceToLine(
            a,
            b,
            viewPoint.zoom
        ).value < 5f
    }

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

/**
 * @param startAngle the angle from 3 o'clock downwards for the start of the arc in radians
 * @param arcLength arc length in radians
 */
@Stable
public data class ArcFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val oval: Rectangle<T>,
    public val startAngle: Float,
    public val arcLength: Float,
    override val attributes: Attributes = Attributes.EMPTY,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = oval

    override fun withCoordinates(newCoordinates: T): Feature<T> =
        copy(oval = with(space) { oval.withCenter(newCoordinates) })

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

public data class DrawFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val position: T,
    override val attributes: Attributes = Attributes.EMPTY,
    public val drawFeature: DrawScope.() -> Unit,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = space.Rectangle(position, position)

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(position = newCoordinates)

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

@Stable
public data class BitmapImageFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    override val center: T,
    public val size: DpSize,
    public val image: ImageBitmap,
    override val attributes: Attributes = Attributes.EMPTY,
) : MarkerFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = space.Rectangle(center, zoom, size)

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(center = newCoordinates)

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}

@Stable
public data class VectorImageFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    override val center: T,
    public val size: DpSize,
    public val image: ImageVector,
    override val attributes: Attributes = Attributes.EMPTY,
) : MarkerFeature<T>, PainterFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = space.Rectangle(center, zoom, size)

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(center = newCoordinates)

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))

    @Composable
    override fun getPainter(): VectorPainter = rememberVectorPainter(image)
}

/**
 * An image that is bound to coordinates and is scaled together with them
 *
 * @param rectangle the size of background in scheme size units. The screen units to scheme units ratio equals scale.
 */
public data class ScalableImageFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val rectangle: Rectangle<T>,
    override val attributes: Attributes = Attributes.EMPTY,
    public val painter: @Composable () -> Painter,
) : Feature<T>, PainterFeature<T> {
    @Composable
    override fun getPainter(): Painter = painter.invoke()

    override fun getBoundingBox(zoom: Float): Rectangle<T> = rectangle

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}


public data class TextFeature<T : Any>(
    override val space: CoordinateSpace<T>,
    public val position: T,
    public val text: String,
    override val attributes: Attributes = Attributes.EMPTY,
    public val fontConfig: FeatureFont.() -> Unit,
) : DraggableFeature<T> {
    override fun getBoundingBox(zoom: Float): Rectangle<T> = space.Rectangle(position, position)

    override fun withCoordinates(newCoordinates: T): Feature<T> = copy(position = newCoordinates)

    override fun withAttributes(modify: (Attributes) -> Attributes): Feature<T> = copy(attributes = modify(attributes))
}
